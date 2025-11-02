/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.service;

import fin.model.BankTransaction;
import fin.model.ClassificationResult;
import fin.repository.AccountRepository;

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
 * Service for processing bank transactions - classification and journal entry generation.
 * This service handles the operational aspects of transaction processing that were
 * formerly in TransactionMappingService but are separate from account definitions.
 * 
 * MIGRATION NOTES:
 * - Extracted from TransactionMappingService as part of consolidation effort
 * - AccountClassificationService now handles chart of accounts and mapping rules
 * - This service focuses on transaction operations and journal entry generation
 * 
 * @author Sthwalo Nyoni
 * @version 1.0
 * @since 2025-10-11
 */
public class TransactionProcessingService {
    private static final Logger LOGGER = Logger.getLogger(TransactionProcessingService.class.getName());
    private final String dbUrl;
    private final AccountClassificationService accountClassificationService;
    private final JournalEntryGenerator journalEntryGenerator;
    
    // Database parameter indices for PreparedStatement operations
    private static final int PARAM_INDEX_ACCOUNT_CODE = 1;
    private static final int PARAM_INDEX_ACCOUNT_NAME = 2;
    private static final int PARAM_INDEX_USERNAME = 3;
    private static final int PARAM_INDEX_TRANSACTION_ID = 4;
    
    // Default account codes for unmapped transactions
    private static final String DEFAULT_BANK_ACCOUNT_CODE = "1100";
    
    // Cache for account mappings
    private Map<String, Long> accountCache = new HashMap<>();
    
    public TransactionProcessingService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
        this.accountClassificationService = new AccountClassificationService(initialDbUrl);
        this.journalEntryGenerator = new JournalEntryGenerator(initialDbUrl, new AccountRepository(initialDbUrl));
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
                                updateStmt.setString(PARAM_INDEX_ACCOUNT_CODE, matchedRule.accountCode);
                                updateStmt.setString(PARAM_INDEX_ACCOUNT_NAME, matchedRule.accountName);
                                updateStmt.setString(PARAM_INDEX_USERNAME, username);
                                updateStmt.setLong(PARAM_INDEX_TRANSACTION_ID, transaction.getId());
                                updateStmt.addBatch();
                                classifiedCount++;
                                
                                LOGGER.fine("Classified transaction ID: " + transaction.getId() + 
                                           " with account: " + matchedRule.accountCode + " - " + matchedRule.accountName);
                            }
                        }
                        
                        if (classifiedCount > 0) {
                            updateStmt.executeBatch();
                            conn.commit();
                            LOGGER.info("Successfully classified " + classifiedCount + " transactions");
                        }
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error classifying transactions for company ID: " + companyId, e);
            throw new RuntimeException("Failed to classify transactions", e);
        }
        
        return classifiedCount;
    }

    /**
     * Reclassify ALL transactions (including already classified) based on current mapping rules
     * 
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions reclassified
     */
    public int reclassifyAllTransactions(Long companyId, String username) {
        int reclassifiedCount = 0;
        
        try {
            // Get ALL transactions (not just unclassified)
            List<BankTransaction> allTransactions = getAllTransactions(companyId);
            
            if (allTransactions.isEmpty()) {
                LOGGER.info("No transactions found for company ID: " + companyId);
                return 0;
            }
            
            LOGGER.info("Found " + allTransactions.size() + " transactions for reclassification for company ID: " + companyId);
            
            // Load transaction mapping rules
            Map<String, RuleMapping> rules = loadTransactionMappingRules(companyId);
            LOGGER.info("Loaded " + rules.size() + " transaction mapping rules");
            
            // Apply classification rules to ALL transactions
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                conn.setAutoCommit(false);
                
                try {
                    String updateSql = """
                        UPDATE bank_transactions
                        SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
                        WHERE id = ?
                        """;
                    
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        for (BankTransaction transaction : allTransactions) {
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
                            
                            // Apply rule if found and different from current classification
                            if (matchedRule != null) {
                                String currentAccountCode = transaction.getAccountCode();
                                
                                // Only update if the classification has changed
                                if (currentAccountCode == null || !currentAccountCode.equals(matchedRule.accountCode)) {
                                    updateStmt.setString(PARAM_INDEX_ACCOUNT_CODE, matchedRule.accountCode);
                                    updateStmt.setString(PARAM_INDEX_ACCOUNT_NAME, matchedRule.accountName);
                                    updateStmt.setString(PARAM_INDEX_USERNAME, username);
                                    updateStmt.setLong(PARAM_INDEX_TRANSACTION_ID, transaction.getId());
                                    updateStmt.addBatch();
                                    reclassifiedCount++;
                                    
                                    LOGGER.fine("Reclassified transaction ID: " + transaction.getId() + 
                                               " from: " + currentAccountCode + " to: " + matchedRule.accountCode);
                                }
                            }
                        }
                        
                        if (reclassifiedCount > 0) {
                            updateStmt.executeBatch();
                            conn.commit();
                            LOGGER.info("Successfully reclassified " + reclassifiedCount + " transactions");
                        }
                    }
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reclassifying transactions for company ID: " + companyId, e);
            throw new RuntimeException("Failed to reclassify transactions", e);
        }
        
        return reclassifiedCount;
    }

    /**
     * Generate journal entries for unclassified transactions that have been classified
     * but don't yet have journal entries
     * 
     * @param companyId The company ID
     * @param createdBy The user creating the entries
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        try {
            // This method name is misleading - it actually generates entries for 
            // classified transactions that don't have journal entries yet
            generateJournalEntriesForClassifiedTransactions(companyId, createdBy);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating journal entries for unclassified transactions", e);
            throw new RuntimeException("Failed to generate journal entries", e);
        }
    }

    /**
     * Generate journal entries for all classified transactions that don't have journal entries yet
     * 
     * @param companyId The company ID
     * @param createdBy The user creating the entries
     * @return The number of journal entries generated
     */
    public int generateJournalEntriesForClassifiedTransactions(Long companyId, String createdBy) {
        int generatedCount = 0;
        
        try {
            // Get classified transactions without journal entries
            List<BankTransaction> transactionsNeedingJournalEntries = getClassifiedTransactionsWithoutJournalEntries(companyId);
            
            if (transactionsNeedingJournalEntries.isEmpty()) {
                LOGGER.info("No classified transactions need journal entries for company ID: " + companyId);
                return 0;
            }
            
            LOGGER.info("Found " + transactionsNeedingJournalEntries.size() + 
                       " classified transactions needing journal entries for company ID: " + companyId);
            
            // Generate journal entries for each transaction
            // JournalEntryGenerator handles its own transactions
            for (BankTransaction transaction : transactionsNeedingJournalEntries) {
                boolean success = generateJournalEntryForTransaction(transaction, createdBy, companyId);
                if (success) {
                    generatedCount++;
                }
            }
            
            if (generatedCount > 0) {
                LOGGER.info("Successfully generated " + generatedCount + " journal entries");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating journal entries for classified transactions", e);
            throw new RuntimeException("Failed to generate journal entries", e);
        }
        
        return generatedCount;
    }

    // ============================================================================
    // PRIVATE HELPER METHODS
    // ============================================================================

    /**
     * Load account cache for quick lookups
     */
    private void loadAccountCache() {
        String sql = "SELECT account_code, id FROM accounts";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                accountCache.put(rs.getString("account_code"), rs.getLong("id"));
            }
            
            LOGGER.info("Loaded " + accountCache.size() + " accounts into cache");
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load account cache", e);
        }
    }

    /**
     * Get unclassified transactions for a company
     */
    private List<BankTransaction> getUnclassifiedTransactions(Long companyId) {
        List<BankTransaction> transactions = new ArrayList<>();
        String sql = """
            SELECT id, transaction_date, details, debit_amount, credit_amount, 
                   account_code, account_name, company_id, fiscal_period_id
            FROM bank_transactions 
            WHERE company_id = ? AND account_code IS NULL
            ORDER BY transaction_date, id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = createTransactionFromResultSet(rs);
                    transactions.add(transaction);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting unclassified transactions", e);
            throw new RuntimeException("Failed to get unclassified transactions", e);
        }
        
        return transactions;
    }

    /**
     * Get ALL transactions for a company (for reclassification)
     */
    private List<BankTransaction> getAllTransactions(Long companyId) {
        List<BankTransaction> transactions = new ArrayList<>();
        String sql = """
            SELECT id, transaction_date, details, debit_amount, credit_amount, 
                   account_code, account_name, company_id, fiscal_period_id
            FROM bank_transactions 
            WHERE company_id = ?
            ORDER BY transaction_date, id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = createTransactionFromResultSet(rs);
                    transactions.add(transaction);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting all transactions", e);
            throw new RuntimeException("Failed to get all transactions", e);
        }
        
        return transactions;
    }

    /**
     * Get classified transactions that don't have journal entries yet
     */
    private List<BankTransaction> getClassifiedTransactionsWithoutJournalEntries(Long companyId) {
        List<BankTransaction> transactions = new ArrayList<>();
        String sql = """
            SELECT bt.id, bt.transaction_date, bt.details, bt.debit_amount, bt.credit_amount, 
                   bt.account_code, bt.account_name, bt.company_id, bt.fiscal_period_id
            FROM bank_transactions bt
            WHERE bt.company_id = ? 
            AND bt.account_code IS NOT NULL
            AND bt.id NOT IN (
                SELECT DISTINCT source_transaction_id 
                FROM journal_entry_lines 
                WHERE source_transaction_id IS NOT NULL
            )
            ORDER BY bt.transaction_date, bt.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BankTransaction transaction = createTransactionFromResultSet(rs);
                    transactions.add(transaction);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting classified transactions without journal entries", e);
            throw new RuntimeException("Failed to get classified transactions without journal entries", e);
        }
        
        return transactions;
    }

    /**
     * Create BankTransaction from ResultSet
     */
    private BankTransaction createTransactionFromResultSet(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setAccountCode(rs.getString("account_code"));
        transaction.setAccountName(rs.getString("account_name"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
        return transaction;
    }

    /**
     * Load transaction mapping rules for a company
     */
    private Map<String, RuleMapping> loadTransactionMappingRules(Long companyId) {
        Map<String, RuleMapping> rules = new HashMap<>();
        
        // Try the new schema first (pattern_text)
        String sql = """
            SELECT pattern_text, account_code, account_name 
            FROM transaction_mapping_rules 
            WHERE company_id = ? AND active = true
            ORDER BY priority DESC, id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, companyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String pattern = rs.getString("pattern_text");
                        String accountCode = rs.getString("account_code");
                        String accountName = rs.getString("account_name");
                        
                        if (pattern != null && accountCode != null) {
                            rules.put(pattern.toUpperCase(), new RuleMapping(accountCode, accountName));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load mapping rules with new schema, trying old schema", e);
            
            // Fall back to old schema (match_value)
            String oldSql = """
                SELECT match_value, account_code, account_name 
                FROM transaction_mapping_rules 
                WHERE company_id = ? AND active = true
                ORDER BY priority DESC, id
                """;
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(oldSql)) {
                
                stmt.setLong(1, companyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String pattern = rs.getString("match_value");
                        String accountCode = rs.getString("account_code");
                        String accountName = rs.getString("account_name");
                        
                        if (pattern != null && accountCode != null) {
                            rules.put(pattern.toUpperCase(), new RuleMapping(accountCode, accountName));
                        }
                    }
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Failed to load mapping rules with both schemas", ex);
            }
        }
        
        return rules;
    }

    /**
     * Find matching rule for transaction details
     */
    private RuleMapping findMatchingRule(String details, Map<String, RuleMapping> rules) {
        details = details.toUpperCase();
        
        // First try exact match
        if (rules.containsKey(details)) {
            return rules.get(details);
        }
        
        // Then try partial matches
        for (Map.Entry<String, RuleMapping> entry : rules.entrySet()) {
            String pattern = entry.getKey();
            if (details.contains(pattern)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    /**
     * Map transaction to account using built-in logic
     * This provides a default bank account for unmapped transactions
     */
    private Long mapTransactionToAccount(BankTransaction transaction) {
        try {
            // Use default bank account for unmapped transactions
            return getAccountIdFromCode(DEFAULT_BANK_ACCOUNT_CODE, transaction.getCompanyId()); // Default to bank account
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to map transaction to account", e);
            return null;
        }
    }

    /**
     * Get account code by ID
     */
    private String getAccountCodeById(Long accountId) {
        String sql = "SELECT code FROM accounts WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("code");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting account code by ID: " + accountId, e);
        }
        
        return null;
    }

    /**
     * Get account name by ID
     */
    private String getAccountNameById(Long accountId) {
        String sql = "SELECT name FROM accounts WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, accountId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting account name by ID: " + accountId, e);
        }
        
        return null;
    }

    /**
     * Get account ID by code for a specific company
     */
    private Long getAccountIdFromCode(String accountCode, Long companyId) {
        String sql = "SELECT id FROM accounts WHERE account_code = ? AND company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, accountCode);
            stmt.setLong(2, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting account ID for code: " + accountCode + ", company: " + companyId, e);
        }
        
        return null;
    }

    /**
     * Generate a proper double-entry journal entry for a transaction using JournalEntryGenerator
     */
    private boolean generateJournalEntryForTransaction(BankTransaction transaction, 
                                                     String createdBy, Long companyId) {
        try {
            // Create ClassificationResult from transaction data
            ClassificationResult classificationResult = new ClassificationResult(
                transaction.getAccountCode(),
                transaction.getAccountName(),
                "Auto-classified by " + createdBy
            );
            
            // Use the proper JournalEntryGenerator for complete double-entry accounting
            return journalEntryGenerator.createJournalEntryForTransaction(transaction, classificationResult);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating journal entry for transaction: " + transaction.getId(), e);
            return false;
        }
    }

    /**
     * Internal class for rule mapping
     */
    private static class RuleMapping {
        final String accountCode;
        final String accountName;
        
        RuleMapping(String valueAccountCode, String valueAccountName) {
            this.accountCode = valueAccountCode;
            this.accountName = valueAccountName;
        }
    }
}