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
            
            // Insurance
            createMappingRule(companyId, "INSURANCE", "8800", "Insurance");
            rulesCreated++;
            createMappingRule(companyId, "INSURE", "8800", "Insurance");
            rulesCreated++;
            
            // Rent
            createMappingRule(companyId, "RENT", "8200", "Rent Expense");
            rulesCreated++;
            
            // Utilities
            createMappingRule(companyId, "ELECTRICITY", "8300", "Utilities");
            rulesCreated++;
            createMappingRule(companyId, "WATER", "8300", "Utilities");
            rulesCreated++;
            
            // Telephone
            createMappingRule(companyId, "TELEPHONE", "8310", "Telephone & Internet");
            rulesCreated++;
            createMappingRule(companyId, "CELL", "8310", "Telephone & Internet");
            rulesCreated++;
            createMappingRule(companyId, "INTERNET", "8310", "Telephone & Internet");
            rulesCreated++;
            
            // Office expenses
            createMappingRule(companyId, "STATIONERY", "8400", "Office Expenses");
            rulesCreated++;
            createMappingRule(companyId, "PRINTING", "8400", "Office Expenses");
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
     * Maps a bank transaction to the appropriate account based on its details
     */
    public Long mapTransactionToAccount(BankTransaction transaction) {
        String details = transaction.getDetails().toUpperCase();
        
        // PRIORITY: Check revenue transactions first for credit amounts
        if (transaction.getCreditAmount() != null) {
            // Sales revenue (deposits/credits) - check this early for credit transactions
            if (!details.contains("LOAN") && !details.contains("CAPITAL") && 
                !details.contains("BOND") && !details.contains("INTEREST") && 
                !details.contains("REFUND") && !details.contains("REVERSAL") && 
                !details.contains("RTD-DEBIT")) {
                // Allow CREDIT TRANSFER as revenue if it doesn't match other patterns
                if (details.contains("CREDIT TRANSFER") || details.contains("DEPOSIT") || 
                    details.contains("PAYMENT") || !details.contains("TRANSFER")) {
                    return getAccountByCode("6100"); // Service Revenue
                }
            }
            
            // Interest income
            if (details.contains("INTEREST")) {
                return getAccountByCode("7000"); // Interest Income
            }
            
            // Refunds (credit transactions that are actually refunds)
            if (details.contains("REFUND") || details.contains("RTD-DEBIT") || 
                details.contains("REVERSAL")) {
                return getAccountByCode("6200"); // Refunds
            }
            
            // Director/Shareholder transfers (credits)
            if (details.contains("DIRECTOR") || details.contains("SHAREHOLDER") || 
                details.contains("OWNER") || (details.contains("IB TRANSFER") && details.contains("FROM"))) {
                return getAccountByCode("4000"); // Long-term Loans
            }
        }
        
        // EXPENSE PATTERNS (debit transactions or specific patterns)
        // Bank charges and fees
        if (details.contains("FEE")) {
            return getAccountByCode("9600"); // Bank Charges
        }
        
        // Salaries and wages
        if (details.contains("SALARIES") || details.contains("SALARY") || 
            details.contains("WAGES") || details.contains("PAYROLL")) {
            return getAccountByCode("8100"); // Employee Costs
        }
        
        // Insurance payments
        if (details.contains("INSURANCE") || details.contains("ASSURANCE")) {
            return getAccountByCode("8800"); // Insurance
        }
        
        // Rent payments
        if (details.contains("RENT") || details.contains("LEASE")) {
            return getAccountByCode("8200"); // Rent Expense
        }
        
        // Communication expenses
        if (details.contains("TELEPHONE") || details.contains("INTERNET") || 
            details.contains("COMMUNICATION") || details.contains("CELL")) {
            return getAccountByCode("8400"); // Communication
        }
        
        // Vehicle expenses
        if (details.contains("FUEL") || details.contains("VEHICLE") || 
            details.contains("MOTOR") || details.contains("CAR")) {
            return getAccountByCode("8500"); // Motor Vehicle Expenses
        }
        
        // Utilities
        if (details.contains("ELECTRICITY") || details.contains("WATER") || 
            details.contains("MUNICIPAL") || details.contains("UTILITIES")) {
            return getAccountByCode("8300"); // Utilities
        }
        
        // Professional services
        if (details.contains("LEGAL") || details.contains("ACCOUNTING") || 
            details.contains("PROFESSIONAL") || details.contains("CONSULTANT")) {
            return getAccountByCode("8700"); // Professional Services
        }
        
        // Travel and entertainment
        if (details.contains("TRAVEL") || details.contains("HOTEL") || 
            details.contains("ENTERTAINMENT") || details.contains("MEALS")) {
            return getAccountByCode("8600"); // Travel & Entertainment
        }
        
        // Office supplies
        if (details.contains("STATIONERY") || details.contains("OFFICE") || 
            details.contains("SUPPLIES")) {
            return getAccountByCode("9000"); // Office Supplies
        }
        
        // Computer expenses - check this after revenue to avoid false matches
        if (details.contains("SOFTWARE") || details.contains("COMPUTER") || 
            details.contains("IT") || details.contains("TECHNOLOGY")) {
            return getAccountByCode("9100"); // Computer Expenses
        }
        
        // Marketing
        if (details.contains("MARKETING") || details.contains("ADVERTISING") || 
            details.contains("PROMOTION")) {
            return getAccountByCode("9200"); // Marketing & Advertising
        }
        
        // Bond repayments (debit transactions)
        if (details.contains("BOND") && details.contains("REPAYMENT") && transaction.getDebitAmount() != null) {
            return getAccountByCode("9500"); // Interest Expense
        }
        
        // Interest payments (debits)
        if (details.contains("INTEREST") && transaction.getDebitAmount() != null) {
            return getAccountByCode("9500"); // Interest Expense
        }
        
        // Director/Shareholder transfers (debits)
        if ((details.contains("DIRECTOR") || details.contains("SHAREHOLDER") || 
             details.contains("OWNER") || details.contains("IB TRANSFER")) && 
            transaction.getDebitAmount() != null) {
            return getAccountByCode("1200"); // Accounts Receivable
        }
        
        // VAT payments
        if (details.contains("VAT") || details.contains("SARS")) {
            return getAccountByCode("3100"); // VAT Output
        }
        
        // PAYE/UIF/SDL payments
        if (details.contains("PAYE")) {
            return getAccountByCode("3200"); // PAYE Payable
        }
        if (details.contains("UIF")) {
            return getAccountByCode("3300"); // UIF Payable
        }
        if (details.contains("SDL")) {
            return getAccountByCode("3400"); // SDL Payable
        }
        
        // Default mappings based on debit/credit
        if (transaction.getDebitAmount() != null) {
            // Debit transactions - typically expenses
            return getAccountByCode("8000"); // Cost of Goods Sold (generic expense)
        } else {
            // Credit transactions - typically income
            return getAccountByCode("6200"); // Other Operating Revenue
        }
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
                    createJournalEntryForTransaction(conn, transaction, entryCount, createdBy);
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
                                                int entryNumber, String createdBy) throws SQLException {
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
        
        // Get the mapped account
        Long mappedAccountId = mapTransactionToAccount(transaction);
        Long bankAccountId = getAccountByCode("1100"); // Bank - Current Account
        
        // Create journal entry lines
        String insertLine = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, 
                                           credit_amount, description, reference, 
                                           source_transaction_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertLine)) {
            // First line: Bank account (opposite of transaction)
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, bankAccountId);
            if (transaction.getDebitAmount() != null) {
                // Transaction is debit, so credit the bank account
                pstmt.setBigDecimal(3, null);
                pstmt.setBigDecimal(4, transaction.getDebitAmount());
            } else {
                // Transaction is credit, so debit the bank account
                pstmt.setBigDecimal(3, transaction.getCreditAmount());
                pstmt.setBigDecimal(4, null);
            }
            pstmt.setString(5, "Bank Account");
            pstmt.setString(6, reference + "-01");
            pstmt.setLong(7, transaction.getId());
            pstmt.executeUpdate();
            
            // Second line: Mapped account (same as transaction)
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, mappedAccountId);
            if (transaction.getDebitAmount() != null) {
                // Transaction is debit, so debit the expense/asset account
                pstmt.setBigDecimal(3, transaction.getDebitAmount());
                pstmt.setBigDecimal(4, null);
            } else {
                // Transaction is credit, so credit the income/liability account
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
    
    private String getAccountCodeById(Long accountId) {
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
    
    private String getAccountNameById(Long accountId) {
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
}
