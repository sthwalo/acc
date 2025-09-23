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
            createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "6000", "Reversals & Adjustments");
            rulesCreated++;
            createMappingRule(companyId, "RTD-DEBIT AGAINST PAYERS AUTH", "6000", "Reversals & Adjustments");
            rulesCreated++;
            
            // Interest income
            createMappingRule(companyId, "INTEREST", "5000", "Other Income");
            rulesCreated++;
            createMappingRule(companyId, "EXCESS INTEREST", "5000", "Other Income");
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
            
            // Telephone - use existing account
            createMappingRule(companyId, "TELEPHONE", "8310-001", "Mobile Phone Payments");
            rulesCreated++;
            createMappingRule(companyId, "CELL", "8310-001", "Mobile Phone Payments");
            rulesCreated++;
            createMappingRule(companyId, "MOBILE", "8310-001", "Mobile Phone Payments");
            rulesCreated++;
            createMappingRule(companyId, "MTN", "8310-001", "Mobile Phone Payments");
            rulesCreated++;
            createMappingRule(companyId, "VOD", "8310-001", "Mobile Phone Payments");
            rulesCreated++;
            createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8310-001", "Mobile Phone Payments");
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

            // INVESTMENT INCOME - Non-operational (goes to Other Income 5000-5999)
            if (details.contains("STANLIB")) {
                return getOrCreateDetailedAccount("5000-001", "Stanlib Investment Income", "5000", "Other Income");
            }

            // INTEREST INCOME - Non-operational
            if (details.contains("INTEREST")) {
                if (details.contains("STANDARD BANK")) {
                    return getOrCreateDetailedAccount("5000-002", "Interest Income - Standard Bank", "5000", "Other Income");
                } else if (details.contains("CAPITEC")) {
                    return getOrCreateDetailedAccount("5000-003", "Interest Income - Capitec", "5000", "Other Income");
                } else {
                    return getOrCreateDetailedAccount("5000-004", "Interest Income - Other Banks", "5000", "Other Income");
                }
            }

            // EXCESS INTEREST - Non-operational income
            if (details.contains("EXCESS INTEREST")) {
                return getOrCreateDetailedAccount("5000-005", "Excess Interest Income", "5000", "Other Income");
            }

            // REFUNDS AND REVERSALS - Trace back to original transaction
            if (details.contains("REVERSAL") || details.contains("RETURNED") ||
                details.contains("NO AUTHORITY") || details.contains("CREDIT NOTE")) {

                // Salary reversals
                if (details.contains("SALARY") || details.contains("XG SALARIES")) {
                    return getOrCreateDetailedAccount("6000-001", "Salary Reversals", "6000", "Reversals & Adjustments");
                }

                // Insurance reversals
                if (details.contains("INSURANCE") || details.contains("PREMIUM")) {
                    return getOrCreateDetailedAccount("6000-002", "Insurance Reversals", "6000", "Reversals & Adjustments");
                }

                // Fee reversals
                if (details.contains("FEE") || details.contains("CHARGE")) {
                    return getOrCreateDetailedAccount("6000-003", "Fee Reversals", "6000", "Reversals & Adjustments");
                }

                // General reversals
                return getOrCreateDetailedAccount("6000-999", "General Reversals", "6000", "Reversals & Adjustments");
            }

            // RTD reversals (failed debits) - Handle separately from general reversals
            if (details.contains("RTD")) {
                if (details.contains("NOT PROVIDED FOR")) {
                    // Failed debits from insurance companies
                    if (details.contains("DOTSURE") || details.contains("MIWAY") ||
                        details.contains("LIBERTY") || details.contains("BADGER") ||
                        details.contains("CARTRACK") || details.contains("FAW")) {
                        return getOrCreateDetailedAccount("6000-005", "Insurance RTD Reversals", "6000", "Reversals & Adjustments");
                    }
                } else if (details.contains("DEBIT AGAINST PAYERS AUTH")) {
                    // Failed debit authorizations
                    if (details.contains("DOTSURE") || details.contains("MIWAY") ||
                        details.contains("LIBERTY") || details.contains("BADGER") ||
                        details.contains("CARTRACK") || details.contains("FAW")) {
                        return getOrCreateDetailedAccount("6000-006", "Insurance Debit Reversals", "6000", "Reversals & Adjustments");
                    }
                } else if (details.contains("AUTHORISATION CANCELLED")) {
                    // Cancelled authorizations
                    return getOrCreateDetailedAccount("6000-007", "Cancelled Authorizations", "6000", "Reversals & Adjustments");
                }
                // General RTD reversals
                return getOrCreateDetailedAccount("6000-008", "RTD Reversals", "6000", "Reversals & Adjustments");
            }

            // CASH DEPOSITS - Non-operational income
            if (details.contains("DEPOSIT")) {
                if (details.contains("STOKFELA")) {
                    return getOrCreateDetailedAccount("5000-005", "Cash Deposit Income - Stokvela", "5000", "Other Income");
                } else if (details.contains("TAU")) {
                    return getOrCreateDetailedAccount("5000-006", "Cash Deposit Income - Tau", "5000", "Other Income");
                } else if (details.contains("DAN")) {
                    return getOrCreateDetailedAccount("5000-007", "Cash Deposit Income - Dan", "5000", "Other Income");
                } else {
                    return getOrCreateDetailedAccount("5000-008", "Cash Deposit Income - Other", "5000", "Other Income");
                }
            }

            // INSURANCE CLAIM PAYMENTS - Non-operational income
            if (details.contains("PAYMENT OF INSURANCE CLAIMS")) {
                return getOrCreateDetailedAccount("5000-006", "Insurance Claim Payments", "5000", "Other Income");
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

            // SALARY PAYMENTS - Operational expenses
            if (details.contains("SALARIES") || details.contains("SALARY") ||
                details.contains("WAGES") || details.contains("XG SALARIES") ||
                (details.contains("IB PAYMENT TO") && (details.contains("XG SALARIES") || details.contains("WAGES")))) {

                // Extract employee name from payment description
                String employeeName = extractEmployeeName(details);
                if (employeeName != null) {
                    String accountCode = "8100-" + generateEmployeeCode(employeeName);
                    String accountName = "Salary - " + employeeName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8100", "Employee Costs");
                } else {
                    return getOrCreateDetailedAccount("8100-999", "Salary Payments - Unspecified", "8100", "Employee Costs");
                }
            }

            // TRUNCATED SALARY PAYMENTS - Handle incomplete descriptions
            if (details.equals("IB PAYMENT TO") || details.equals("IMMEDIATE PAYMENT")) {
                // These are salary payments with missing recipient details
                return getOrCreateDetailedAccount("8100-999", "Salary Payments - Unspecified", "8100", "Employee Costs");
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

            // PENSION FUND CONTRIBUTIONS - Operational expenses
            if (details.contains("PENSION") && details.contains("FUND") && details.contains("CONTRIBUTION")) {
                if (details.contains("FAW")) {
                    return getOrCreateDetailedAccount("8110-001", "Pension Contributions - FAW Fund", "8110", "Pension Contributions");
                } else {
                    return getOrCreateDetailedAccount("8110-999", "Pension Contributions - Other", "8110", "Pension Contributions");
                }
            }

            // TAX PAYMENTS - Operational expenses
            if (details.contains("SARS") || details.contains("PAYE") || details.contains("VAT")) {
                if (details.contains("PAYE") || details.contains("PAY-AS-YOU-EARN")) {
                    return getOrCreateDetailedAccount("3100-001", "PAYE Tax Payments", "3100", "Tax Liabilities");
                } else if (details.contains("VAT")) {
                    return getOrCreateDetailedAccount("3100-002", "VAT Payments", "3100", "Tax Liabilities");
                } else {
                    return getOrCreateDetailedAccount("3100-999", "Other Tax Payments", "3100", "Tax Liabilities");
                }
            }

            // UTILITIES - Operational expenses
            if (details.contains("ELECTRICITY")) {
                return getOrCreateDetailedAccount("8900-001", "Electricity Expenses", "8900", "Administrative Expenses");
            }
            if (details.contains("WATER")) {
                return getOrCreateDetailedAccount("8900-002", "Water Expenses", "8900", "Administrative Expenses");
            }
            if (details.contains("INTERNET") || details.contains("TELKOM")) {
                return getOrCreateDetailedAccount("8900-003", "Internet & Telephone", "8900", "Administrative Expenses");
            }

            // OFFICE SUPPLIES - Operational expenses
            if (details.contains("STATIONERY") || details.contains("PRINTING") || details.contains("OFFICE")) {
                return getOrCreateDetailedAccount("8900-004", "Office Supplies & Printing", "8900", "Administrative Expenses");
            }

            // PROFESSIONAL SERVICES - Operational expenses
            if (details.contains("LEGAL") || details.contains("ATTORNEY")) {
                return getOrCreateDetailedAccount("8700-001", "Legal Services", "8700", "Professional Services");
            }
            if (details.contains("ACCOUNTING") || details.contains("AUDIT")) {
                return getOrCreateDetailedAccount("8700-002", "Accounting & Audit Services", "8700", "Professional Services");
            }
            if (details.contains("CONSULTANT") || details.contains("CONSULTING")) {
                return getOrCreateDetailedAccount("8700-003", "Consulting Services", "8700", "Professional Services");
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

            // REIMBURSEMENTS - Special category for employee reimbursements
            if (details.contains("REIMBURSE")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8900-" + generateSupplierCode(supplierName);
                    String accountName = "Reimbursement - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8900", "Administrative Expenses");
                } else {
                    return getOrCreateDetailedAccount("8900-999", "Employee Reimbursements", "8900", "Administrative Expenses");
                }
            }

            // SCHOOL FEES - Education expenses
            if (details.contains("SCHOOL") || details.contains("COLLEGE") || details.contains("LYCEUM")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8900-" + generateSupplierCode(supplierName);
                    String accountName = "School Fees - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8900", "Administrative Expenses");
                } else {
                    return getOrCreateDetailedAccount("8900-005", "School Fees", "8900", "Administrative Expenses");
                }
            }

            // STOKFELA PAYMENTS - Special loans/payments to stokvela
            if (details.contains("STOKFELA") || details.contains("STOKVELA")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "2000-" + generateSupplierCode(supplierName);
                    String accountName = "Stokvela Loan - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "2000", "Long-term Liabilities");
                } else {
                    return getOrCreateDetailedAccount("2000-999", "Stokvela Loans", "2000", "Long-term Liabilities");
                }
            }

            // BOND REPAYMENTS - Liability reduction (mortgage payments)
            if (details.contains("STD BANK BOND REPAYMENT") || details.contains("BOND REPAYMENT")) {
                return getOrCreateDetailedAccount("2000-003", "Bond Liability", "2000", "Long-term Liabilities");
            }

            // CAR PURCHASES - Vehicle assets
            if (details.contains("CAR SALES") || details.contains("MERCEDES") ||
                details.contains("VEHICLE") || details.contains("AUTOMOTIVE")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "2000-" + generateSupplierCode(supplierName);
                    String accountName = "Vehicle Purchase - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "2000", "Non-Current Assets");
                } else {
                    return getOrCreateDetailedAccount("2000-002", "Vehicle Purchases", "2000", "Non-Current Assets");
                }
            }

            // COMMUNICATION & IT SERVICES - Two Way Technologies, etc.
            if (details.contains("TWO WAY TECHNOLOGIES") || details.contains("TECHNOLOGIES") ||
                details.contains("SOFTWARE") || details.contains("IT ")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8400-" + generateSupplierCode(supplierName);
                    String accountName = "IT Services - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8400", "Communication");
                } else {
                    return getOrCreateDetailedAccount("8400-999", "IT & Communication Services", "8400", "Communication");
                }
            }

            // LABOUR & HUMAN RESOURCES - Neo Entle Labour, etc.
            if (details.contains("LABOUR") || details.contains("HUMAN RESOUR") ||
                details.contains("RECRUITMENT") || details.contains("STAFFING")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8100-" + generateSupplierCode(supplierName);
                    String accountName = "Labour Services - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8100", "Employee Costs");
                } else {
                    return getOrCreateDetailedAccount("8100-998", "Labour & HR Services", "8100", "Employee Costs");
                }
            }

            // CONSTRUCTION & BUILDING - Modderfontein, Ellispark Stadium, etc.
            if (details.contains("STADIUM") || details.contains("CONSTRUCTION") ||
                details.contains("BUILDING") || details.contains("MODDERFONTEIN")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8200-" + generateSupplierCode(supplierName);
                    String accountName = "Construction Services - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8200", "Rent Expense");
                } else {
                    return getOrCreateDetailedAccount("8200-999", "Construction & Building Services", "8200", "Rent Expense");
                }
            }

            // TRANSPORT SERVICES - Mbhoni Miyambo Transport, etc.
            if (details.contains("TRANSPORT") || details.contains("LOGISTICS") ||
                details.contains("DELIVERY") || details.contains("COURIER")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null) {
                    String accountCode = "8600-" + generateSupplierCode(supplierName);
                    String accountName = "Transport Services - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "8600", "Travel & Entertainment");
                } else {
                    return getOrCreateDetailedAccount("8600-999", "Transport & Logistics Services", "8600", "Travel & Entertainment");
                }
            }

            // GENERAL SUPPLIER PAYMENTS - Catch all for other supplier payments
            if (details.contains("IB PAYMENT TO") && !details.contains("XG SALARIES") &&
                !details.contains("WAGES")) {
                String supplierName = extractSupplierName(details);
                if (supplierName != null && supplierName.length() > 2) {
                    String accountCode = "3000-" + generateSupplierCode(supplierName);
                    String accountName = "Supplier - " + supplierName;
                    return getOrCreateDetailedAccount(accountCode, accountName, "3000", "Accounts Payable");
                }
            }

            // ATM CASH WITHDRAWALS - Petty cash (current assets)
            if (details.contains("AUTOBANK CASH WITHDRAWAL")) {
                return getOrCreateDetailedAccount("1000-006", "Petty Cash Withdrawals", "1000", "Current Assets");
            }

            // MOBILE PHONE PAYMENTS - Communication expenses
            if (details.contains("PRE-PAID PAYMENT TO")) {
                if (details.contains("MTN") || details.contains("VOD")) {
                    return getOrCreateDetailedAccount("8310-001", "Mobile Phone Payments", "8310", "Telephone & Internet");
                }
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
     * Get the appropriate category_id for an account code
     */
    private int getCategoryIdForAccountCode(String accountCode) {
        if (accountCode == null) return 18; // Default to Operating Expenses

        String prefix = accountCode.split("-")[0];

        switch (prefix) {
            case "1000": return 11; // Current Assets (loan receivables)
            case "2000": return 13; // Current Liabilities (director loans)
            case "4000": return 16; // Operating Revenue
            case "5000": return 17; // Other Income
            case "6000": return 17; // Other Income (reversals/adjustments)
            case "8100": return 18; // Operating Expenses (salaries)
            case "8500": return 18; // Operating Expenses (vehicle expenses)
            case "8800": return 18; // Operating Expenses (insurance)
            case "9600": return 20; // Finance Costs (bank fees)
            case "3100": return 18; // Operating Expenses (VAT)
            default: return 18; // Default to Operating Expenses
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
