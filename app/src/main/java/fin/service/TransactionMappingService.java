package fin.service;

import fin.model.BankTransaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
     * Maps a bank transaction to the appropriate account based on its details
     */
    public Long mapTransactionToAccount(BankTransaction transaction) {
        String details = transaction.getDetails().toUpperCase();
        
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
        
        // Computer expenses
        if (details.contains("SOFTWARE") || details.contains("COMPUTER") || 
            details.contains("IT") || details.contains("TECHNOLOGY")) {
            return getAccountByCode("9100"); // Computer Expenses
        }
        
        // Marketing
        if (details.contains("MARKETING") || details.contains("ADVERTISING") || 
            details.contains("PROMOTION")) {
            return getAccountByCode("9200"); // Marketing & Advertising
        }
        
        // Interest payments
        if (details.contains("INTEREST") && transaction.getDebitAmount() != null) {
            return getAccountByCode("9500"); // Interest Expense
        }
        
        // Interest income
        if (details.contains("INTEREST") && transaction.getCreditAmount() != null) {
            return getAccountByCode("7000"); // Interest Income
        }
        
        // Sales revenue (deposits/credits)
        if (transaction.getCreditAmount() != null && !details.contains("TRANSFER") && 
            !details.contains("LOAN") && !details.contains("CAPITAL")) {
            return getAccountByCode("6100"); // Service Revenue
        }
        
        // Loans from directors/shareholders
        if (details.contains("DIRECTOR") || details.contains("SHAREHOLDER") || 
            details.contains("OWNER")) {
            if (transaction.getCreditAmount() != null) {
                // Money coming in from directors - liability
                return getAccountByCode("4000"); // Long-term Loans
            } else {
                // Money going out to directors - asset
                return getAccountByCode("1200"); // Accounts Receivable
            }
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
