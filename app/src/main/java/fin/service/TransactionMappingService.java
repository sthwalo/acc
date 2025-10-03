package fin.service;

import fin.model.BankTransaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.math.BigDecimal;

/**
 * Service for automatically mapping bank transactions to appropriate accounts
 * based on transaction details and predefined mapping rules.
 */
public class TransactionMappingService {
    private static final Logger LOGGER = Logger.getLogger(TransactionMappingService.class.getName());
    private final String dbUrl;
    
    // Cache for account mappings
    private Map<String, Long> accountCache = new HashMap<>();
    
    public TransactionMappingService(String dbUrl) {
        this.dbUrl = dbUrl;
        loadAccountCache();
    }
    
    /**
     * Classify all unclassified transactions for a company based on mapping rules
     * 
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions classified
     */
    public int classifyAllUnclassifiedTransactions(Long companyId, String username) {
        int classifiedCount = 0;
        
        try {
            // Get unclassified transactions
            List<BankTransaction> unclassifiedTransactions = getUnclassifiedTransactions(companyId);
            
            if (unclassifiedTransactions.isEmpty()) {
                LOGGER.info("No unclassified transactions found for company ID: " + companyId);
                return 0;
            }
            
            LOGGER.info("Found " + unclassifiedTransactions.size() + " unclassified transactions for company ID: " + companyId);
            
            // Ensure transaction mapping rules table exists and is properly migrated
            createTransactionMappingRulesTable();
            
            // Load transaction mapping rules
            Map<String, RuleMapping> rules = loadTransactionMappingRules(companyId);
            LOGGER.info("Loaded " + rules.size() + " transaction mapping rules");
            
            // Apply classification rules
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                conn.setAutoCommit(false);
                
                try {
                    String updateSql = """
                        UPDATE bank_transactions
                        SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
                        WHERE id = ?
                        """;
                    
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        for (BankTransaction transaction : unclassifiedTransactions) {
                            String details = transaction.getDetails().toUpperCase();
                            
                            // First try direct rule matching
                            RuleMapping matchedRule = findMatchingRule(details, rules);
                            
                            // If no rule matched, try built-in logic
                            if (matchedRule == null) {
                                Long accountId = mapTransactionToAccount(transaction);
                                if (accountId != null) {
                                    String accountCode = getAccountCodeById(accountId);
                                    String accountName = getAccountNameById(accountId);
                                    
                                    matchedRule = new RuleMapping(accountCode, accountName);
                                }
                            }
                            
                            // Apply rule if found
                            if (matchedRule != null) {
                                updateStmt.setString(1, matchedRule.accountCode);
                                updateStmt.setString(2, matchedRule.accountName);
                                updateStmt.setString(3, username);
                                updateStmt.setLong(4, transaction.getId());
                                updateStmt.addBatch();
                                classifiedCount++;
                                
                                // Log for every 100 transactions
                                if (classifiedCount % 100 == 0) {
                                    LOGGER.info("Classified " + classifiedCount + " transactions so far...");
                                }
                            }
                        }
                        
                        // Execute batch update
                        updateStmt.executeBatch();
                    }
                    
                    conn.commit();
                    LOGGER.info("Successfully classified " + classifiedCount + " transactions");
                    
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error classifying transactions", e);
            throw new RuntimeException("Failed to classify transactions", e);
        }
        
        return classifiedCount;
    }
    
    /**
     * Find a matching rule for transaction details
     */
    private RuleMapping findMatchingRule(String details, Map<String, RuleMapping> rules) {
        for (Map.Entry<String, RuleMapping> entry : rules.entrySet()) {
            String pattern = entry.getKey();
            if (details.contains(pattern)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    /**
     * Load transaction mapping rules for a company
     */
    private Map<String, RuleMapping> loadTransactionMappingRules(Long companyId) throws SQLException {
        Map<String, RuleMapping> rules = new HashMap<>();
        
        String sql = """
            SELECT tmr.pattern_text, a.account_code, a.account_name 
            FROM transaction_mapping_rules tmr 
            JOIN accounts a ON tmr.account_id = a.id 
            WHERE tmr.company_id = ? AND tmr.is_active = true
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String pattern = rs.getString("pattern_text").toUpperCase();
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");
                    
                    rules.put(pattern, new RuleMapping(accountCode, accountName));
                }
            }
        }
        
        return rules;
    }
    
    /**
     * Get unclassified bank transactions
     */
    private List<BankTransaction> getUnclassifiedTransactions(Long companyId) throws SQLException {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String sql = """
            SELECT *
            FROM bank_transactions
            WHERE company_id = ? AND account_code IS NULL
            ORDER BY transaction_date
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = mapResultSetToBankTransaction(rs);
                    transactions.add(transaction);
                }
            }
        }
        
        return transactions;
    }
    
    /**
     * Helper class to hold rule mapping information
     */
    private static class RuleMapping {
        String accountCode;
        String accountName;
        
        RuleMapping(String accountCode, String accountName) {
            this.accountCode = accountCode;
            this.accountName = accountName;
        }
    }
    
    /**
     * Get a database connection
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }
    
    /**
     * Classify a single transaction with the provided account code and name
     * 
     * @param transaction The transaction to classify
     * @param accountCode The account code to assign
     * @param accountName The account name to assign
     * @return true if classification was successful
     */
    public boolean classifyTransaction(BankTransaction transaction, String accountCode, String accountName) {
        try {
            String sql = """
                UPDATE bank_transactions 
                SET account_code = ?, 
                    account_name = ?, 
                    classification_date = CURRENT_TIMESTAMP,
                    classified_by = ? 
                WHERE id = ?
                """;
                
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, accountCode);
                stmt.setString(2, accountName);
                stmt.setString(3, "INTERACTIVE-CLASSIFICATION");
                stmt.setLong(4, transaction.getId());
                
                int updated = stmt.executeUpdate();
                
                if (updated > 0) {
                    LOGGER.info("Classified transaction " + transaction.getId() + 
                             " as " + accountCode + " - " + accountName);
                    return true;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error classifying transaction " + transaction.getId(), e);
        }
        
        return false;
    }
    
    /**
     * Create standard mapping rules for a company
     * 
     * @param companyId The company ID to create rules for
     * @return Number of rules created
     */
    public int createStandardMappingRules(Long companyId) {
        int rulesCreated = 0;
        
        try {
            // Create transaction mapping rules table if it doesn't exist
            createTransactionMappingRulesTable();
            
            // Clear existing rules for this company
            clearExistingMappingRules(companyId);
            
            // Bank fees and charges
            createMappingRule(companyId, "FEE", "9600", "Bank Charges");
            rulesCreated++;
            createMappingRule(companyId, "CHARGE", "9600", "Bank Charges");
            rulesCreated++;
            createMappingRule(companyId, "ADMIN FEE", "9600", "Bank Charges");
            rulesCreated++;
            
            // Salaries
            createMappingRule(companyId, "SALARY", "8100", "Employee Costs");
            rulesCreated++;
            createMappingRule(companyId, "WAGE", "8100", "Employee Costs");
            rulesCreated++;
            createMappingRule(companyId, "XG SALARIES", "8100", "Employee Costs");
            rulesCreated++;
            
            // Insurance
            createMappingRule(companyId, "INSURANCE", "8800", "Insurance");
            rulesCreated++;
            createMappingRule(companyId, "INSURE", "8800", "Insurance");
            rulesCreated++;
            createMappingRule(companyId, "DOTSURE", "8800", "Insurance");
            rulesCreated++;
            createMappingRule(companyId, "MIWAY", "8800", "Insurance");
            rulesCreated++;
            createMappingRule(companyId, "KINGPRICE", "8800", "Insurance");
            rulesCreated++;
            
            // Insurance reversals and failed debits
            createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "7200", "Gain on Asset Disposal");
            rulesCreated++;
            createMappingRule(companyId, "RTD-DEBIT AGAINST PAYERS AUTH", "7200", "Gain on Asset Disposal");
            rulesCreated++;
            
            // Interest income
            createMappingRule(companyId, "INTEREST", "7000", "Interest Income");
            rulesCreated++;
            createMappingRule(companyId, "EXCESS INTEREST", "7000", "Interest Income");
            rulesCreated++;
            
            // Bank transfers
            createMappingRule(companyId, "IB TRANSFER TO", "1100", "Current Assets");
            rulesCreated++;
            createMappingRule(companyId, "IB TRANSFER FROM", "1100", "Current Assets");
            rulesCreated++;
            createMappingRule(companyId, "IB INSTANT MONEY CASH TO", "1100", "Current Assets");
            rulesCreated++;
            
            // Rent
            createMappingRule(companyId, "RENT", "8200", "Rent Expense");
            rulesCreated++;
            
            // Utilities
            createMappingRule(companyId, "ELECTRICITY", "8300", "Utilities");
            rulesCreated++;
            createMappingRule(companyId, "WATER", "8300", "Utilities");
            rulesCreated++;
            
            // Telephone - use Communication account (8400)
            createMappingRule(companyId, "TELEPHONE", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "CELL", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "MOBILE", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "MTN", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "VOD", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8400", "Communication");
            rulesCreated++;
            
            // Internet
            createMappingRule(companyId, "INTERNET", "8400", "Communication");
            rulesCreated++;
            createMappingRule(companyId, "TELKOM", "8400", "Communication");
            rulesCreated++;
            
            // Office expenses
            createMappingRule(companyId, "STATIONERY", "8400", "Office Expenses");
            rulesCreated++;
            createMappingRule(companyId, "PRINTING", "8400", "Office Expenses");
            rulesCreated++;
            createMappingRule(companyId, "OFFICE", "8400", "Office Expenses");
            rulesCreated++;
            
            // Fuel and vehicle expenses
            createMappingRule(companyId, "FUEL", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "PETROL", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "DIESEL", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "BP", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "SHELL", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "SASOL", "8600", "Travel & Entertainment");
            rulesCreated++;
            createMappingRule(companyId, "ENGEN", "8600", "Travel & Entertainment");
            rulesCreated++;
            
            // Vehicle tracking
            createMappingRule(companyId, "CARTRACK", "8500", "Motor Vehicle Expenses");
            rulesCreated++;
            createMappingRule(companyId, "NETSTAR", "8500", "Motor Vehicle Expenses");
            rulesCreated++;
            
            // Balance brought forward
            createMappingRule(companyId, "BALANCE BROUGHT FORWARD", "9500", "Interest Expense");
            rulesCreated++;
            
            LOGGER.info("Created " + rulesCreated + " standard mapping rules for company ID " + companyId);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating standard mapping rules", e);
        }
        
        return rulesCreated;
    }
    
    /**
     * Create transaction mapping rules table
     */
    private void createTransactionMappingRulesTable() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // First, check if the table exists
            boolean tableExists = false;
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "transaction_mapping_rules", null)) {
                tableExists = rs.next();
            }
            
            if (!tableExists) {
                // Create the table with the correct pattern_text column
                String sql = """
                    CREATE TABLE transaction_mapping_rules (
                        id BIGSERIAL PRIMARY KEY,
                        company_id BIGINT NOT NULL,
                        pattern_text TEXT NOT NULL,
                        account_code VARCHAR(20) NOT NULL,
                        account_name VARCHAR(100) NOT NULL,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (company_id) REFERENCES companies(id)
                    )
                    """;
                stmt.executeUpdate(sql);
                
                // Create indexes for faster lookups
                stmt.executeUpdate("CREATE INDEX idx_mapping_rules_company ON transaction_mapping_rules(company_id)");
                stmt.executeUpdate("CREATE INDEX idx_mapping_rules_pattern_text ON transaction_mapping_rules(pattern_text)");
                
                LOGGER.info("Created transaction_mapping_rules table with pattern_text column");
            } else {
                // Check if we need to migrate from 'pattern' to 'pattern_text'
                boolean hasPatternColumn = false;
                boolean hasPatternTextColumn = false;
                List<String> allColumns = new ArrayList<>();
                
                try (ResultSet rs = conn.getMetaData().getColumns(null, null, "transaction_mapping_rules", null)) {
                    while (rs.next()) {
                        String columnName = rs.getString("COLUMN_NAME");
                        allColumns.add(columnName);
                        if ("pattern".equals(columnName)) {
                            hasPatternColumn = true;
                        } else if ("pattern_text".equals(columnName)) {
                            hasPatternTextColumn = true;
                        }
                    }
                }
                
                LOGGER.info("Existing columns in transaction_mapping_rules: " + allColumns);
                LOGGER.info("Has pattern column: " + hasPatternColumn + ", Has pattern_text column: " + hasPatternTextColumn);
                
                if (hasPatternColumn && !hasPatternTextColumn) {
                    // Need to migrate from pattern to pattern_text
                    LOGGER.info("Migrating from 'pattern' to 'pattern_text' column");
                    
                    // Add pattern_text column
                    stmt.executeUpdate("ALTER TABLE transaction_mapping_rules ADD COLUMN pattern_text TEXT");
                    
                    // Copy data from pattern to pattern_text
                    stmt.executeUpdate("UPDATE transaction_mapping_rules SET pattern_text = pattern");
                    
                    // Make pattern_text non-null
                    stmt.executeUpdate("ALTER TABLE transaction_mapping_rules ALTER COLUMN pattern_text SET NOT NULL");
                    
                    // Create index on pattern_text
                    stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_mapping_rules_pattern_text ON transaction_mapping_rules(pattern_text)");
                    
                    // We keep the old column for backward compatibility, but all new code will use pattern_text
                    LOGGER.info("Migration to pattern_text completed successfully");
                } else if (!hasPatternTextColumn) {
                    // Table exists but doesn't have pattern_text - this could be an empty table or one with different schema
                    // Let's try to add the missing column
                    LOGGER.info("Table exists but missing pattern_text column. Adding it now...");
                    
                    try {
                        stmt.executeUpdate("ALTER TABLE transaction_mapping_rules ADD COLUMN pattern_text TEXT");
                        LOGGER.info("Added pattern_text column successfully");
                    } catch (SQLException e) {
                        LOGGER.warning("Could not add pattern_text column: " + e.getMessage());
                        // If we can't add the column, the table might have a different structure
                        // Let's check if this is a completely different table structure
                        if (allColumns.isEmpty()) {
                            LOGGER.info("Table appears to be empty or have no columns. Recreating...");
                            stmt.executeUpdate("DROP TABLE transaction_mapping_rules");
                            
                            // Recreate the table with correct structure
                            String sql = """
                                CREATE TABLE transaction_mapping_rules (
                                    id BIGSERIAL PRIMARY KEY,
                                    company_id BIGINT NOT NULL,
                                    pattern_text TEXT NOT NULL,
                                    account_code VARCHAR(20) NOT NULL,
                                    account_name VARCHAR(100) NOT NULL,
                                    is_active BOOLEAN DEFAULT TRUE,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (company_id) REFERENCES companies(id)
                                )
                                """;
                            stmt.executeUpdate(sql);
                            
                            // Create indexes
                            stmt.executeUpdate("CREATE INDEX idx_mapping_rules_company ON transaction_mapping_rules(company_id)");
                            stmt.executeUpdate("CREATE INDEX idx_mapping_rules_pattern_text ON transaction_mapping_rules(pattern_text)");
                            
                            LOGGER.info("Recreated transaction_mapping_rules table with correct structure");
                        } else {
                            throw new SQLException("transaction_mapping_rules table exists with unexpected structure: " + allColumns);
                        }
                    }
                } else {
                    LOGGER.info("transaction_mapping_rules table already has correct structure");
                }
            }
        }
    }
    
    /**
     * Clear existing mapping rules for a company
     */
    private void clearExistingMappingRules(Long companyId) throws SQLException {
        String sql = "DELETE FROM transaction_mapping_rules WHERE company_id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            int deleted = stmt.executeUpdate();
            
            if (deleted > 0) {
                LOGGER.info("Cleared " + deleted + " existing mapping rules for company ID " + companyId);
            }
        }
    }
    
    /**
     * Create a mapping rule for a specific pattern
     */
    private void createMappingRule(Long companyId, String pattern, String accountCode, 
                                 String accountName) throws SQLException {
        
        // First, get the account ID from the account code
        Long accountId = getAccountIdByCode(companyId, accountCode);
        if (accountId == null) {
            throw new SQLException("Account with code " + accountCode + " not found for company " + companyId);
        }
        
        // Check if rule already exists
        String ruleName = "Auto-" + pattern.replaceAll("[^A-Za-z0-9]", "_");
        String checkSql = "SELECT id FROM transaction_mapping_rules WHERE company_id = ? AND rule_name = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setLong(1, companyId);
            checkStmt.setString(2, ruleName);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    // Rule already exists, skip
                    return;
                }
            }
        }
        
        String sql = """
            INSERT INTO transaction_mapping_rules 
            (company_id, rule_name, description, match_type, match_value, pattern_text, account_id, is_active, priority)
            VALUES (?, ?, ?, 'CONTAINS', ?, ?, ?, true, 0)
            """;
            
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setString(2, ruleName);
            stmt.setString(3, "Auto-generated rule for " + pattern);
            stmt.setString(4, pattern);
            stmt.setString(5, pattern);
            stmt.setLong(6, accountId);
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Helper method to get account ID by account code
     */
    private Long getAccountIdByCode(Long companyId, String accountCode) throws SQLException {
        String sql = "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setString(2, accountCode);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                return null;
            }
        }
    }
    
    /**
     * Helper method to get standard account ID by SARS-compliant code.
     * This method looks up existing accounts without creating new ones.
     * 
     * Phase 2 Addition (2025-10-03): Supports SARS-compliant account structure.
     * 
     * @param accountCode Standard SARS account code (e.g., "8100", "7000", "2100")
     * @return Account ID or null if not found
     */
    private Long getStandardAccountId(String accountCode) {
        try {
            // Use company ID 2 (Xinghizana Group) - should be parameterized
            return getAccountIdByCode(2L, accountCode);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not find standard account: " + accountCode, e);
            return null;
        }
    }
    
    /**
     * Enhanced comprehensive transaction mapping that creates proper double-entry accounting
     * Distinguishes between operational revenue, non-operational income, and reversals/refunds
     * Now includes supplier extraction and detailed account creation
     */
    public Long mapTransactionToAccount(BankTransaction transaction) {
        String details = transaction.getDetails().toUpperCase();

        // =================================================================
        // INCOME TRANSACTIONS (CREDIT AMOUNTS) - PROPER DOUBLE-ENTRY CLASSIFICATION
        // =================================================================
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {

            // OPERATIONAL REVENUE - Service Income (goes to Revenue accounts 4000-4999)
            if (details.contains("COROBRIK")) {
                return getOrCreateDetailedAccount("4000-001", "Corobrik Service Revenue", "4000", "Revenue");
            }

            // NON-OPERATIONAL INCOME - Director Injections (Long-term loans - Liability accounts 2000-2999)
            if (details.contains("COMPANY ASSIST")) {
                return getOrCreateDetailedAccount("2000-001", "Director Loan - Company Assist", "2000", "Long-term Liabilities");
            }

            // LOAN REPAYMENTS - Short-term loans (Asset reduction - 1000-1999)
            if (details.contains("XINGHIZANA LOA")) {
                if (details.contains("TAU")) {
                    return getOrCreateDetailedAccount("1000-001", "Loan Receivable - Tau", "1000", "Current Assets");
                } else if (details.contains("MAPHOSA")) {
                    return getOrCreateDetailedAccount("1000-002", "Loan Receivable - Maposa", "1000", "Current Assets");
                } else {
                    return getOrCreateDetailedAccount("1000-003", "Loan Receivable - Other", "1000", "Current Assets");
                }
            }

            // INVESTMENT INCOME - Non-operational (SARS Code: 7000 - Interest Income)
            if (details.contains("STANLIB")) {
                return getStandardAccountId("7000"); // Interest Income
            }

            // INTEREST INCOME - Non-operational (SARS Code: 7000 - Interest Income)
            // All interest income goes to single standard account, details preserved in transaction
            if (details.contains("INTEREST")) {
                return getStandardAccountId("7000"); // Interest Income (all banks)
            }

            // EXCESS INTEREST - Non-operational income (SARS Code: 7000 - Interest Income)
            if (details.contains("EXCESS INTEREST")) {
                return getStandardAccountId("7000"); // Interest Income
            }

            // REFUNDS AND REVERSALS - Other Income (SARS Code: 7200 - Other Operating Income)
            // All reversals are income adjustments, use single standard account
            if (details.contains("REVERSAL") || details.contains("RETURNED") ||
                details.contains("NO AUTHORITY") || details.contains("CREDIT NOTE")) {
                return getStandardAccountId("7200"); // Other Operating Income (Adjustments/Refunds)
            }

            // RTD reversals (failed debits) - Other Income (SARS Code: 7200)
            // All RTD reversals are income adjustments
            if (details.contains("RTD")) {
                return getStandardAccountId("7200"); // Other Operating Income (Adjustments/Refunds)
            }

            // CASH DEPOSITS - Other Income (SARS Code: 7100 - Non-Operating Income)
            // All cash deposits classified as non-operating income
            if (details.contains("DEPOSIT")) {
                return getStandardAccountId("7100"); // Non-Operating Income
            }

            // INSURANCE CLAIM PAYMENTS - Other Income (SARS Code: 7100)
            if (details.contains("PAYMENT OF INSURANCE CLAIMS")) {
                return getStandardAccountId("7100"); // Non-Operating Income
            }
        }

        // =================================================================
        // EXPENSE TRANSACTIONS (DEBIT AMOUNTS) - PROPER DOUBLE-ENTRY CLASSIFICATION
        // =================================================================
        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {

            // VEHICLE TRACKING - Operational expenses
            if (details.contains("CARTRACK")) {
                return getOrCreateDetailedAccount("8500-001", "Cartrack Vehicle Tracking", "8500", "Motor Vehicle Expenses");
            }
            if (details.contains("NETSTAR")) {
                return getOrCreateDetailedAccount("8500-002", "Netstar Vehicle Tracking", "8500", "Motor Vehicle Expenses");
            }

            // INSURANCE PREMIUMS - Operational expenses
            if (details.contains("INSURANCE") || details.contains("PREMIUM")) {
                if (details.contains("KINGPRICE") || details.contains("KING PRICE")) {
                    return getOrCreateDetailedAccount("8800-001", "King Price Insurance Premiums", "8800", "Insurance");
                } else if (details.contains("DOTSURE")) {
                    return getOrCreateDetailedAccount("8800-002", "DOTSURE Insurance Premiums", "8800", "Insurance");
                } else if (details.contains("OUTSURANCE")) {
                    return getOrCreateDetailedAccount("8800-003", "OUTSurance Insurance Premiums", "8800", "Insurance");
                } else if (details.contains("MIWAY")) {
                    return getOrCreateDetailedAccount("8800-004", "MIWAY Insurance Premiums", "8800", "Insurance");
                } else if (details.contains("LIBERTY")) {
                    return getOrCreateDetailedAccount("8800-005", "Liberty Insurance Premiums", "8800", "Insurance");
                } else if (details.contains("BADGER")) {
                    return getOrCreateDetailedAccount("8800-006", "Badger Insurance Premiums", "8800", "Insurance");
                } else {
                    return getOrCreateDetailedAccount("8800-999", "Other Insurance Premiums", "8800", "Insurance");
                }
            }

            // FUEL EXPENSES - Operational expenses
            if (details.contains("FUEL") || details.contains("PETROL") || details.contains("DIESEL")) {
                if (details.contains("BP")) {
                    return getOrCreateDetailedAccount("8600-001", "Fuel Expenses - BP Stations", "8600", "Travel & Entertainment");
                } else if (details.contains("SHELL")) {
                    return getOrCreateDetailedAccount("8600-002", "Fuel Expenses - Shell Stations", "8600", "Travel & Entertainment");
                } else if (details.contains("SASOL")) {
                    return getOrCreateDetailedAccount("8600-003", "Fuel Expenses - Sasol Stations", "8600", "Travel & Entertainment");
                } else if (details.contains("ENGEN")) {
                    return getOrCreateDetailedAccount("8600-004", "Engen Fuel Expenses", "8600", "Travel & Entertainment");
                } else {
                    return getOrCreateDetailedAccount("8600-099", "Fuel Expenses - Other Stations", "8600", "Travel & Entertainment");
                }
            }

            // BANK FEES - Operational expenses
            if (details.contains("FEE") || details.contains("CHARGE")) {
                if (details.contains("STANDARD BANK")) {
                    return getOrCreateDetailedAccount("9600-001", "Standard Bank Fees", "9600", "Bank Charges");
                } else if (details.contains("CAPITEC")) {
                    return getOrCreateDetailedAccount("9600-002", "Capitec Bank Fees", "9600", "Bank Charges");
                } else if (details.contains("ATM")) {
                    return getOrCreateDetailedAccount("9600-003", "ATM Withdrawal Fees", "9600", "Bank Charges");
                } else if (details.contains("EFT")) {
                    return getOrCreateDetailedAccount("9600-004", "EFT Transaction Fees", "9600", "Bank Charges");
                } else if (details.contains("DEBIT ORDER")) {
                    return getOrCreateDetailedAccount("9600-005", "Debit Order Fees", "9600", "Bank Charges");
                } else {
                    return getOrCreateDetailedAccount("9600-999", "Other Bank Fees", "9600", "Bank Charges");
                }
            }

            // SALARY PAYMENTS - Operational expenses (SARS Code: 8100 - Employee Costs)
            // All salary payments to single standard account, employee name preserved in transaction details
            if (details.contains("SALARIES") || details.contains("SALARY") ||
                details.contains("WAGES") || details.contains("XG SALARIES") ||
                (details.contains("IB PAYMENT TO") && (details.contains("XG SALARIES") || details.contains("WAGES")))) {
                return getStandardAccountId("8100"); // Employee Costs (all employees)
            }

            // TRUNCATED SALARY PAYMENTS - Handle incomplete descriptions (SARS Code: 8100)
            if (details.equals("IB PAYMENT TO") || details.equals("IMMEDIATE PAYMENT")) {
                // These are salary payments with missing recipient details
                return getStandardAccountId("8100"); // Employee Costs
            }

            // TRUNCATED BANK TRANSFERS - Handle incomplete transfer descriptions
            if (details.equals("IB TRANSFER FROM")) {
                return getOrCreateDetailedAccount("1100-001", "Bank Transfers", "1100", "Current Assets");
            }

            // INTEREST EXPENSE - Handle debit interest transactions
            if (details.contains("EXCESS INTEREST")) {
                return getOrCreateDetailedAccount("9500-001", "Excess Interest Expense", "9500", "Interest Expense");
            }

            // EXCESS INTEREST - Already handled in income section above

            // PENSION FUND CONTRIBUTIONS - Operational expenses (SARS Code: 8100 - Employee Costs)
            // Pension contributions are part of employee costs
            if (details.contains("PENSION") && details.contains("FUND") && details.contains("CONTRIBUTION")) {
                return getStandardAccountId("8100"); // Employee Costs (includes pension)
            }

            // TAX PAYMENTS - Liabilities (SARS Code: 2100 - Current Tax Payable)
            // All tax payments reduce current tax liability
            if (details.contains("SARS") || details.contains("PAYE") || details.contains("VAT")) {
                return getStandardAccountId("2100"); // Current Tax Payable
            }

            // UTILITIES - Operational expenses (SARS Code: 8900 - Other Operating Expenses)
            if (details.contains("ELECTRICITY") || details.contains("WATER") || 
                details.contains("INTERNET") || details.contains("TELKOM")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // OFFICE SUPPLIES - Operational expenses (SARS Code: 8900)
            if (details.contains("STATIONERY") || details.contains("PRINTING") || details.contains("OFFICE")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // PROFESSIONAL SERVICES - Operational expenses (SARS Code: 8700 - Professional Fees)
            if (details.contains("LEGAL") || details.contains("ATTORNEY") ||
                details.contains("ACCOUNTING") || details.contains("AUDIT") ||
                details.contains("CONSULTANT") || details.contains("CONSULTING")) {
                return getStandardAccountId("8700"); // Professional Fees
            }

            // VEHICLE MAINTENANCE & PARTS - Operational expenses
            if (details.contains("MAINTENANCE") || details.contains("REPAIR") || details.contains("SERVICE")) {
                return getOrCreateDetailedAccount("8500-003", "Vehicle Maintenance & Repairs", "8500", "Motor Vehicle Expenses");
            }

            // TOLL FEES - Operational expenses
            if (details.contains("TOLL")) {
                return getOrCreateDetailedAccount("8600-005", "Toll Fees", "8600", "Travel & Entertainment");
            }

            // PARKING - Operational expenses
            if (details.contains("PARKING")) {
                return getOrCreateDetailedAccount("8600-006", "Parking Fees", "8600", "Travel & Entertainment");
            }

            // =================================================================
            // SUPPLIER PAYMENTS - DETAILED ACCOUNT STRUCTURE
            // =================================================================

            // REIMBURSEMENTS - Use standard Other Operating Expenses account (SARS Code: 8900)
            // Employee/supplier name preserved in transaction details field
            if (details.contains("REIMBURSE")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // SCHOOL FEES - Use standard Other Operating Expenses account (SARS Code: 8900)
            // School/institution name preserved in transaction details field
            if (details.contains("SCHOOL") || details.contains("COLLEGE") || details.contains("LYCEUM")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // STOKVELA PAYMENTS - Use standard Long-term Liabilities account (SARS Code: 2400)
            // Stokvela member name preserved in transaction details field
            if (details.contains("STOKFELA") || details.contains("STOKVELA")) {
                return getStandardAccountId("2400"); // Long-term Liabilities
            }

            // BOND REPAYMENTS - Liability reduction (mortgage payments)
            if (details.contains("STD BANK BOND REPAYMENT") || details.contains("BOND REPAYMENT")) {
                return getOrCreateDetailedAccount("2000-003", "Bond Liability", "2000", "Long-term Liabilities");
            }

            // VEHICLE PURCHASES - Use standard Property, Plant & Equipment (SARS Code: 2100)
            // Vehicle details and supplier name preserved in transaction details field
            if (details.contains("CAR SALES") || details.contains("MERCEDES") ||
                details.contains("VEHICLE") || details.contains("AUTOMOTIVE")) {
                return getStandardAccountId("2100"); // Property, Plant & Equipment
            }

            // IT SERVICES - Use standard Communication account (SARS Code: 8400)
            // Supplier name preserved in transaction details field
            if (details.contains("TWO WAY TECHNOLOGIES") || details.contains("TECHNOLOGIES") ||
                details.contains("SOFTWARE") || details.contains("IT ")) {
                return getStandardAccountId("8400"); // Communication
            }

            // LABOUR SERVICES - Use standard Employee Costs account (SARS Code: 8100)
            // Labour provider/agency name preserved in transaction details field
            if (details.contains("LABOUR") || details.contains("HUMAN RESOUR") ||
                details.contains("RECRUITMENT") || details.contains("STAFFING")) {
                return getStandardAccountId("8100"); // Employee Costs
            }

            // RENT/LEASE PAYMENTS - Ellispark Stadium (SARS Code: 8600 - Rent Paid)
            if (details.contains("ELLISPARK STADIUM")) {
                return getStandardAccountId("8600"); // Rent Paid
            }

            // DIRECTORS' REMUNERATION - Dan Nkuna (SARS Code: 8200)
            if (details.contains("MODDERFONTEIN") && details.contains("NKUNA")) {
                return getStandardAccountId("8200"); // Directors' Remuneration
            }

            // CONSTRUCTION & BUILDING - Operating expenses (SARS Code: 8900 - Other Operating Expenses)
            // Supplier details preserved in transaction description
            if (details.contains("CONSTRUCTION") || details.contains("BUILDING")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // TRANSPORT SERVICES - Operating expenses (SARS Code: 8900)
            if (details.contains("TRANSPORT") || details.contains("LOGISTICS") ||
                details.contains("DELIVERY") || details.contains("COURIER")) {
                return getStandardAccountId("8900"); // Other Operating Expenses
            }

            // GENERAL SUPPLIER PAYMENTS - Trade Payables (SARS Code: 2000 - Trade & Other Payables)
            // All supplier payments reduce trade payables liability
            if (details.contains("IB PAYMENT TO") && !details.contains("XG SALARIES") &&
                !details.contains("WAGES")) {
                return getStandardAccountId("2000"); // Trade & Other Payables
            }

            // ATM CASH WITHDRAWALS - Petty cash (current assets)
            if (details.contains("AUTOBANK CASH WITHDRAWAL")) {
                return getOrCreateDetailedAccount("1000-006", "Petty Cash Withdrawals", "1000", "Current Assets");
            }

            // MOBILE PHONE PAYMENTS - Communication expenses (SARS Code: 8400 - Communication)
            if (details.contains("PRE-PAID PAYMENT TO") || details.contains("MTN") || 
                details.contains("VOD") || details.contains("CELL C") || details.contains("TELKOM")) {
                return getStandardAccountId("8400"); // Communication
            }
        }

        // =================================================================
        // FALLBACK: If no specific pattern matches, return null to leave unclassified
        // =================================================================
        LOGGER.info("Transaction not classified: " + details);
        return null;
    }
    
    /**
     * Analyzes unclassified transactions and provides classification suggestions
     */
    public Map<String, List<BankTransaction>> analyzeUnclassifiedTransactions(Long companyId) {
        Map<String, List<BankTransaction>> groupedTransactions = new HashMap<>();
        
        String sql = """
            SELECT bt.* FROM bank_transactions bt
            WHERE bt.company_id = ?
            AND bt.id NOT IN (
                SELECT DISTINCT source_transaction_id 
                FROM journal_entry_lines 
                WHERE source_transaction_id IS NOT NULL
            )
            ORDER BY bt.transaction_date DESC
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                BankTransaction transaction = mapResultSetToBankTransaction(rs);
                Long accountId = mapTransactionToAccount(transaction);
                
                // Skip transactions that can't be classified
                if (accountId == null) {
                    continue;
                }
                
                String accountCode = getAccountCodeById(accountId);
                String accountName = getAccountNameById(accountId);
                String groupKey = accountCode + " - " + accountName;
                
                groupedTransactions.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(transaction);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error analyzing unclassified transactions", e);
        }
        
        return groupedTransactions;
    }
    
    /**
     * Creates automatic journal entries for all unclassified bank transactions
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        String sql = """
            SELECT bt.* FROM bank_transactions bt
            WHERE bt.company_id = ?
            AND bt.id NOT IN (
                SELECT DISTINCT source_transaction_id 
                FROM journal_entry_lines 
                WHERE source_transaction_id IS NOT NULL
            )
            ORDER BY bt.transaction_date
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, companyId);
                ResultSet rs = pstmt.executeQuery();
                
                int entryCount = 1;
                while (rs.next()) {
                    BankTransaction transaction = mapResultSetToBankTransaction(rs);
                    
                    // Skip transactions that can't be classified
                    Long mappedAccountId = mapTransactionToAccount(transaction);
                    if (mappedAccountId == null) {
                        continue;
                    }
                    
                    createJournalEntryForTransaction(conn, transaction, entryCount, mappedAccountId, createdBy);
                    entryCount++;
                }
                
                conn.commit();
                LOGGER.info("Generated journal entries for " + (entryCount - 1) + " transactions");
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating journal entries", e);
            throw new RuntimeException("Failed to generate journal entries", e);
        }
    }
    
    private void createJournalEntryForTransaction(Connection conn, BankTransaction transaction, 
                                                int entryNumber, Long mappedAccountId, String createdBy) throws SQLException {
        // Create journal entry header
        String reference = "AUTO-" + String.format("%05d", entryNumber);
        String description = "Auto-generated: " + transaction.getDetails();

        String insertJournalEntry = """
            INSERT INTO journal_entries (reference, entry_date, description, fiscal_period_id,
                                       company_id, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        Long journalEntryId;
        try (PreparedStatement pstmt = conn.prepareStatement(insertJournalEntry)) {
            pstmt.setString(1, reference);
            pstmt.setDate(2, java.sql.Date.valueOf(transaction.getTransactionDate()));
            pstmt.setString(3, description);
            pstmt.setLong(4, transaction.getFiscalPeriodId());
            pstmt.setLong(5, transaction.getCompanyId());
            pstmt.setString(6, createdBy);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                journalEntryId = rs.getLong("id");
            } else {
                throw new SQLException("Failed to create journal entry");
            }
        }

        // Get the mapped account (passed as parameter)
        Long bankAccountId = getAccountByCode("1100"); // Bank - Current Account

        // Create journal entry lines - CORRECT DOUBLE-ENTRY ACCOUNTING
        String insertLine = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount,
                                           credit_amount, description, reference,
                                           source_transaction_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(insertLine)) {
            // CORRECTED LOGIC: Bank account reflects actual bank transaction
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, bankAccountId);
            if (transaction.getDebitAmount() != null) {
                // Bank debit = money going out of bank account
                pstmt.setBigDecimal(3, transaction.getDebitAmount());
                pstmt.setBigDecimal(4, null);
            } else {
                // Bank credit = money coming into bank account
                pstmt.setBigDecimal(3, null);
                pstmt.setBigDecimal(4, transaction.getCreditAmount());
            }
            pstmt.setString(5, "Bank Account");
            pstmt.setString(6, reference + "-01");
            pstmt.setLong(7, transaction.getId());
            pstmt.executeUpdate();

            // Second line: Mapped account (opposite of bank transaction for double-entry)
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, mappedAccountId);
            if (transaction.getDebitAmount() != null) {
                // Bank debit (expense) = credit the expense account (increase expense)
                pstmt.setBigDecimal(3, null);
                pstmt.setBigDecimal(4, transaction.getDebitAmount());
            } else {
                // Bank credit (income) = credit the income account (increase income)
                pstmt.setBigDecimal(3, null);
                pstmt.setBigDecimal(4, transaction.getCreditAmount());
            }
            pstmt.setString(5, getAccountNameById(mappedAccountId));
            pstmt.setString(6, reference + "-02");
            pstmt.setLong(7, transaction.getId());
            pstmt.executeUpdate();
        }
    }
    
    private void loadAccountCache() {
        String sql = "SELECT id, account_code, account_name FROM accounts WHERE is_active = true";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String code = rs.getString("account_code");
                Long id = rs.getLong("id");
                accountCache.put(code, id);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading account cache", e);
        }
    }
    
    private Long getAccountByCode(String accountCode) {
        return accountCache.get(accountCode);
    }
    
    public String getAccountCodeById(Long accountId) {
        String sql = "SELECT account_code FROM accounts WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("account_code");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting account code", e);
        }
        
        return "UNKNOWN";
    }
    
    public String getAccountNameById(Long accountId) {
        String sql = "SELECT account_name FROM accounts WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, accountId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("account_name");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting account name", e);
        }
        
        return "Unknown Account";
    }
    
    private BankTransaction mapResultSetToBankTransaction(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setBankAccountId(rs.getLong("bank_account_id"));
        transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setBalance(rs.getBigDecimal("balance"));
        transaction.setServiceFee(rs.getBoolean("service_fee"));
        transaction.setAccountNumber(rs.getString("account_number"));
        transaction.setStatementPeriod(rs.getString("statement_period"));
        transaction.setSourceFile(rs.getString("source_file"));
        return transaction;
    }
    
    /**
     * Get or create a detailed account with automatic account creation
     * This ensures every transaction gets its own detailed account
     */
    private Long getOrCreateDetailedAccount(String detailedCode, String detailedName,
                                           String rollupCode, String rollupName) {
        try {
            // First try to get existing account
            Long existingAccountId = getAccountIdByCode(1L, detailedCode); // Use company ID 1 for Xinghizana
            if (existingAccountId != null) {
                return existingAccountId;
            }

            // Account doesn't exist, create it
            return createDetailedAccount(detailedCode, detailedName, rollupCode, rollupName);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting/creating detailed account: " + detailedCode, e);
            return null;
        }
    }

    /**
     * Create a new detailed account in the database
     */
    private Long createDetailedAccount(String accountCode, String accountName,
                                     String rollupCode, String rollupName) throws SQLException {
        // For now, we'll assume company_id = 1 (Xinghizana Group)
        // In a real implementation, this should be passed as a parameter
        Long companyId = 1L;

        // Determine category_id based on account code prefix
        int categoryId = getCategoryIdForAccountCode(accountCode);

        // First, find the parent account ID by code
        Long parentAccountId = null;
        if (rollupCode != null) {
            String parentSql = "SELECT id FROM accounts WHERE company_id = ? AND account_code = ?";
            try (Connection conn = getConnection();
                 PreparedStatement parentStmt = conn.prepareStatement(parentSql)) {
                parentStmt.setLong(1, companyId);
                parentStmt.setString(2, rollupCode);
                ResultSet parentRs = parentStmt.executeQuery();
                if (parentRs.next()) {
                    parentAccountId = parentRs.getLong("id");
                }
            }
        }

        String sql = """
            INSERT INTO accounts (company_id, account_code, account_name, category_id, parent_account_id, is_active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            stmt.setString(2, accountCode);
            stmt.setString(3, accountName);
            stmt.setInt(4, categoryId);
            if (parentAccountId != null) {
                stmt.setLong(5, parentAccountId);
            } else {
                stmt.setNull(5, java.sql.Types.INTEGER);
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Long newAccountId = rs.getLong("id");
                // Update cache
                accountCache.put(accountCode, newAccountId);
                LOGGER.info("Created new detailed account: " + accountCode + " - " + accountName);
                return newAccountId;
            }
        }

        return null;
    }

    /**
     * Get the appropriate category_id for an account code based on SARS-compliant structure.
     * Maps account codes (1000-9999) to actual category IDs in database.
     * 
     * Updated: 2025-10-03 - Fixed to use actual category IDs (7-16) instead of non-existent IDs (18-20)
     */
    private int getCategoryIdForAccountCode(String accountCode) {
        if (accountCode == null) return 14; // Default to Operating Expenses (ID 14)

        String prefix = accountCode.split("-")[0];

        // Map SARS account code ranges to actual category IDs in database
        switch (prefix) {
            // Assets (1000-1999)
            case "1000":
            case "1100":
            case "1200":
            case "1300":
            case "1400":
            case "1500":
                return 7; // Current Assets
            
            case "1600":
            case "1700":
            case "1800":
            case "1900":
                return 8; // Non-Current Assets
            
            // Liabilities (2000-2999)
            case "2000":
            case "2100":
            case "2200":
            case "2300":
                return 9; // Current Liabilities
            
            case "2400":
            case "2500":
            case "2600":
            case "2700":
            case "2800":
            case "2900":
                return 10; // Non-Current Liabilities
            
            // Equity (3000-3999) - Note: 3000-3999 for payables is technically liabilities
            case "3000":
            case "3100":
            case "3200":
                return 9; // Current Liabilities (trade payables)
            
            case "3300":
            case "3400":
            case "3500":
            case "3600":
            case "3700":
            case "3800":
            case "3900":
                return 11; // Owner's Equity
            
            // Revenue (4000-6999)
            case "4000":
            case "4100":
            case "4200":
            case "4300":
            case "4400":
            case "4500":
            case "4600":
            case "4700":
            case "4800":
            case "4900":
            case "5000":
            case "5100":
            case "5200":
            case "5300":
            case "5400":
            case "5500":
                return 12; // Operating Revenue
            
            case "5600":
            case "5700":
            case "5800":
            case "5900":
            case "6000":
            case "6100":
            case "6200":
            case "6300":
            case "6400":
            case "6500":
            case "6600":
            case "6700":
            case "6800":
            case "6900":
                return 13; // Other Income
            
            // Revenue from 7000-7999
            case "7000":
            case "7100":
            case "7200":
            case "7300":
            case "7400":
            case "7500":
            case "7600":
            case "7700":
            case "7800":
            case "7900":
                return 13; // Other Income (Interest, Dividends, etc.)
            
            // Expenses (8000-8999)
            case "8000":
            case "8100": // Employee Costs
            case "8200": // Directors' Remuneration
            case "8300": // Depreciation
            case "8400": // Communication
            case "8500": // Vehicle Expenses
            case "8600": // Repairs & Maintenance
            case "8700": // Professional Fees
            case "8800": // Insurance
            case "8900": // Other Operating Expenses
                return 14; // Operating Expenses
            
            // Finance Costs (9000-9999)
            case "9000":
            case "9100":
            case "9200":
            case "9300":
            case "9400":
            case "9500":
            case "9600": // Bank Charges
            case "9700":
            case "9800":
            case "9900":
                return 16; // Finance Costs
            
            default:
                return 14; // Default to Operating Expenses (ID 14)
        }
    }

    /**
     * Generate journal entries for all classified transactions that don't have journal entries yet
     *
     * @param companyId The company ID
     * @return The number of journal entries created
     */
    public int generateJournalEntriesForUnclassifiedTransactions(Long companyId) {
        int journalEntriesCreated = 0;

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            // Get classified transactions that don't have journal entries
            String sql = """
                SELECT bt.id, bt.transaction_date, bt.details, bt.debit_amount, bt.credit_amount,
                       bt.account_code, bt.account_name
                FROM bank_transactions bt
                LEFT JOIN journal_entries je ON je.reference = CONCAT('JE-', bt.id)
                WHERE bt.company_id = ?
                AND bt.account_code IS NOT NULL
                AND je.id IS NULL
                ORDER BY bt.transaction_date, bt.id
                """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, companyId);

                try (ResultSet rs = stmt.executeQuery()) {
                    JournalEntryCreationService journalService = new JournalEntryCreationService(dbUrl);

                    while (rs.next()) {
                        // Create BankTransaction object
                        BankTransaction transaction = new BankTransaction();
                        transaction.setId(rs.getLong("id"));
                        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                        transaction.setDetails(rs.getString("details"));
                        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
                        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
                        transaction.setAccountCode(rs.getString("account_code"));
                        transaction.setAccountName(rs.getString("account_name"));

                        // Create journal entry
                        try {
                            journalService.createJournalEntryForTransaction(
                                transaction,
                                transaction.getAccountCode(),
                                transaction.getAccountName()
                            );
                            journalEntriesCreated++;

                            // Log progress every 100 entries
                            if (journalEntriesCreated % 100 == 0) {
                                LOGGER.info("Created " + journalEntriesCreated + " journal entries so far...");
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Failed to create journal entry for transaction ID: " + transaction.getId(), e);
                        }
                    }
                }
            }

            LOGGER.info("Journal entry generation completed. Created " + journalEntriesCreated + " journal entries.");
            return journalEntriesCreated;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating journal entries", e);
            throw new RuntimeException("Failed to generate journal entries", e);
        }
    }

    /**
     * Extract employee name from salary payment description
     */
    private String extractEmployeeName(String details) {
        // Common patterns: "IB PAYMENT TO [NAME] XG SALARIES"
        if (details.contains("IB PAYMENT TO") && details.contains("XG SALARIES")) {
            int start = details.indexOf("IB PAYMENT TO") + "IB PAYMENT TO".length();
            int end = details.indexOf("XG SALARIES");
            if (start < end) {
                String namePart = details.substring(start, end).trim();
                // Clean up common prefixes/suffixes
                namePart = namePart.replaceAll("^(MR|MRS|MS|DR|PROF)\\s+", "");
                return namePart;
            }
        }
        return null;
    }

    /**
     * Generate a unique code for employee salary accounts
     */
    private String generateEmployeeCode(String employeeName) {
        if (employeeName == null || employeeName.trim().isEmpty()) {
            return "999";
        }
        
        // Create a simple hash-like code from the employee name
        String cleanName = employeeName.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleanName.length() >= 3) {
            return cleanName.substring(0, 3);
        } else {
            return String.format("%03d", cleanName.hashCode() % 1000);
        }
    }

    /**
     * Extract supplier name from payment description
     */
    private String extractSupplierName(String details) {
        // Common patterns for supplier payments
        if (details.contains("IB PAYMENT TO")) {
            int start = details.indexOf("IB PAYMENT TO") + "IB PAYMENT TO".length();
            // Look for common endings
            String[] endings = {"LOAN REPAYM", "REIMBURSE", "XG SALARIES", "VEHICL", "CLEANING", "TRANSPORT", "TECHNOLOGIES", "LABOUR"};
            int earliestEnd = details.length();
            
            for (String ending : endings) {
                int endPos = details.indexOf(ending);
                if (endPos > start && endPos < earliestEnd) {
                    earliestEnd = endPos;
                }
            }
            
            if (earliestEnd > start) {
                String supplierPart = details.substring(start, earliestEnd).trim();
                // Clean up common prefixes/suffixes
                supplierPart = supplierPart.replaceAll("^(MR|MRS|MS|DR|PROF)\\s+", "");
                return supplierPart;
            }
        }
        
        // For other patterns, try to extract reasonable supplier names
        if (details.contains("PAYMENT TO")) {
            int start = details.indexOf("PAYMENT TO") + "PAYMENT TO".length();
            // Take up to next space or common delimiter
            int end = details.indexOf(" ", start + 1);
            if (end == -1) end = details.length();
            if (end > start) {
                return details.substring(start, end).trim();
            }
        }
        
        return null;
    }

    /**
     * Generate a unique code for supplier accounts
     */
    private String generateSupplierCode(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            return "999";
        }
        
        // Create a simple hash-like code from the supplier name
        String cleanName = supplierName.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (cleanName.length() >= 3) {
            return cleanName.substring(0, 3);
        } else {
            return String.format("%03d", Math.abs(cleanName.hashCode()) % 1000);
        }
    }
}
