package fin.service;

import fin.model.BankTransaction;
import fin.model.JournalEntry;
import fin.model.JournalEntryLine;
import fin.model.Account;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Journal Entry Creation Service
 *
 * Handles the creation of double-entry journal entries based on transaction classification.
 * Implements detailed supplier account extraction and automatic account creation.
 * Follows the comprehensive account structure outlined in the detailed supplier account structure.
 */
public class JournalEntryCreationService {
    private static final Logger LOGGER = Logger.getLogger(JournalEntryCreationService.class.getName());
    private final String dbUrl;

    // Account code mappings for roll-up categories
    private static final Map<String, String> ROLLUP_CATEGORIES = Map.of(
        "8500", "Motor Vehicle Expenses",
        "8600", "Travel & Entertainment",
        "8700", "Professional Services",
        "8800", "Insurance",
        "8900", "Administrative Expenses",
        "9600", "Bank Charges"
    );

    public JournalEntryCreationService(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Creates a double-entry journal entry for a classified transaction
     */
    public void createJournalEntryForTransaction(BankTransaction transaction, String accountCode, String accountName) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            // Get or create the mapped account
            Account mappedAccount = getOrCreateAccount(conn, accountCode, accountName);

            // Get the bank account (assuming account code 1000 for bank)
            Account bankAccount = getBankAccount(conn);

            // Create journal entry
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setReference("JE-" + transaction.getId());
            journalEntry.setEntryDate(transaction.getTransactionDate());
            journalEntry.setDescription(transaction.getDetails());
            journalEntry.setCreatedAt(LocalDateTime.now());
            journalEntry.setCreatedBy("SYSTEM");

            // Insert journal entry
            long journalEntryId = insertJournalEntry(conn, journalEntry);

            // Create journal entry lines based on transaction type
            createJournalEntryLines(conn, journalEntryId, transaction, mappedAccount, bankAccount);

            conn.commit();
            LOGGER.info("Created journal entry for transaction: " + transaction.getId());

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating journal entry for transaction: " + transaction.getId(), e);
            throw new RuntimeException("Failed to create journal entry", e);
        }
    }

    /**
     * Extracts supplier information and maps to detailed account codes
     */
    public AccountMapping extractAccountMapping(String transactionDetails) {
        String upperDetails = transactionDetails.toUpperCase();

        // Vehicle & Tracking Suppliers
        if (upperDetails.contains("CARTRACK") || upperDetails.contains("CAR TRACK")) {
            return new AccountMapping("8500-001", "Cartrack Vehicle Tracking");
        }
        if (upperDetails.contains("NETSTAR")) {
            return new AccountMapping("8500-002", "Netstar Vehicle Tracking");
        }

        // Insurance Companies
        if (upperDetails.contains("KINGPRICE") || upperDetails.contains("KING PRICE")) {
            return new AccountMapping("8800-001", "King Price Insurance");
        }
        if (upperDetails.contains("DOTSURE")) {
            return new AccountMapping("8800-002", "DOTSURE Insurance");
        }
        if (upperDetails.contains("OUTSURANCE")) {
            return new AccountMapping("8800-003", "OUTSurance Insurance");
        }
        if (upperDetails.contains("MIWAY")) {
            return new AccountMapping("8800-004", "MIWAY Insurance");
        }
        if (upperDetails.contains("LIBERTY")) {
            return new AccountMapping("8800-005", "Liberty Insurance");
        }
        if (upperDetails.contains("BADGER")) {
            return new AccountMapping("8800-006", "Badger Insurance");
        }

        // Bank & Financial Services
        if (upperDetails.contains("STANDARD BANK") && upperDetails.contains("FEE")) {
            return new AccountMapping("9600-001", "Standard Bank Fees");
        }
        if (upperDetails.contains("CAPITEC") && upperDetails.contains("FEE")) {
            return new AccountMapping("9600-002", "Capitec Bank Fees");
        }
        if (upperDetails.contains("ATM") && upperDetails.contains("FEE")) {
            return new AccountMapping("9600-003", "ATM Fees");
        }
        if (upperDetails.contains("EFT") && upperDetails.contains("FEE")) {
            return new AccountMapping("9600-004", "EFT Fees");
        }

        // Fuel Stations
        if (upperDetails.contains("BP ")) {
            return new AccountMapping("8600-001", "Fuel Expenses - BP Stations");
        }
        if (upperDetails.contains("SHELL")) {
            return new AccountMapping("8600-002", "Fuel Expenses - Shell Stations");
        }
        if (upperDetails.contains("SASOL")) {
            return new AccountMapping("8600-003", "Fuel Expenses - Sasol Stations");
        }
        if (upperDetails.contains("ENGEN")) {
            return new AccountMapping("8600-004", "Fuel Expenses - Engen Stations");
        }

        // Communication Providers
        if (upperDetails.contains("TELKOM")) {
            return new AccountMapping("8400-001", "Telkom Communications");
        }
        if (upperDetails.contains("MTN")) {
            return new AccountMapping("8400-002", "MTN Communications");
        }
        if (upperDetails.contains("VODACOM")) {
            return new AccountMapping("8400-003", "Vodacom Communications");
        }

        // Default fallback - use broad categories
        return getFallbackMapping(upperDetails);
    }

    private AccountMapping getFallbackMapping(String upperDetails) {
        // Salary payments
        if (upperDetails.contains("SALARY") || upperDetails.contains("WAGES")) {
            return new AccountMapping("8100-999", "Salary Payments - Unspecified");
        }

        // Bank fees (general)
        if (upperDetails.contains("FEE")) {
            return new AccountMapping("9600-005", "Bank Fees - Other");
        }

        // Default expense account
        return new AccountMapping("8900-001", "General Expenses");
    }

    private Account getOrCreateAccount(Connection conn, String accountCode, String accountName) throws SQLException {
        // Check if account exists
        String selectSql = "SELECT id, account_code, account_name FROM accounts WHERE account_code = ?";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            stmt.setString(1, accountCode);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account account = new Account();
                    account.setId(rs.getLong("id"));
                    account.setAccountCode(rs.getString("account_code"));
                    account.setAccountName(rs.getString("account_name"));
                    return account;
                }
            }
        }

        // Account doesn't exist, create it
        String insertSql = """
            INSERT INTO accounts (account_code, account_name, account_type, category, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, accountCode);
            stmt.setString(2, accountName);
            stmt.setString(3, getAccountType(accountCode));
            stmt.setString(4, getAccountCategory(accountCode));
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Account account = new Account();
                    account.setId(rs.getLong(1));
                    account.setAccountCode(accountCode);
                    account.setAccountName(accountName);
                    LOGGER.info("Created new account: " + accountCode + " - " + accountName);
                    return account;
                }
            }
        }

        throw new SQLException("Failed to create account: " + accountCode);
    }

    private String getAccountType(String accountCode) {
        if (accountCode.startsWith("1")) return "ASSET";
        if (accountCode.startsWith("2")) return "LIABILITY";
        if (accountCode.startsWith("3")) return "EQUITY";
        if (accountCode.startsWith("4") || accountCode.startsWith("5") || accountCode.startsWith("6")) return "INCOME";
        if (accountCode.startsWith("7") || accountCode.startsWith("8") || accountCode.startsWith("9")) return "EXPENSE";
        return "EXPENSE"; // Default
    }

    private String getAccountCategory(String accountCode) {
        String mainCode = accountCode.substring(0, 4);
        return ROLLUP_CATEGORIES.getOrDefault(mainCode, "Other");
    }

    private Account getBankAccount(Connection conn) throws SQLException {
        String sql = "SELECT id, account_code, account_name FROM accounts WHERE account_code = '1100' LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                Account account = new Account();
                account.setId(rs.getLong("id"));
                account.setAccountCode(rs.getString("account_code"));
                account.setAccountName(rs.getString("account_name"));
                return account;
            }
        }
        throw new SQLException("Bank account (1100) not found");
    }

    private long insertJournalEntry(Connection conn, JournalEntry journalEntry) throws SQLException {
        String sql = """
            INSERT INTO journal_entries (reference, entry_date, description, created_by, created_at)
            VALUES (?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, journalEntry.getReference());
            stmt.setDate(2, Date.valueOf(journalEntry.getEntryDate()));
            stmt.setString(3, journalEntry.getDescription());
            stmt.setString(4, journalEntry.getCreatedBy());
            stmt.setTimestamp(5, Timestamp.valueOf(journalEntry.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to insert journal entry");
    }

    private void createJournalEntryLines(Connection conn, long journalEntryId, BankTransaction transaction,
                                       Account mappedAccount, Account bankAccount) throws SQLException {
        String sql = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, source_transaction_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        // Determine if this is income or expense based on transaction amounts
        boolean isIncome = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isExpense = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (isIncome) {
                // Income: Credit income account, Debit bank account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, mappedAccount.getId());
                stmt.setNull(3, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(4, transaction.getCreditAmount()); // credit_amount = transaction amount
                stmt.setString(5, "Income - " + transaction.getDetails());
                stmt.setLong(6, transaction.getId());
                stmt.executeUpdate();

                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, bankAccount.getId());
                stmt.setBigDecimal(3, transaction.getCreditAmount()); // debit_amount = transaction amount
                stmt.setNull(4, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(5, "Bank account - " + transaction.getDetails());
                stmt.setLong(6, transaction.getId());
                stmt.executeUpdate();

            } else if (isExpense) {
                // Expense: Debit expense account, Credit bank account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, mappedAccount.getId());
                stmt.setBigDecimal(3, transaction.getDebitAmount()); // debit_amount = transaction amount
                stmt.setNull(4, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(5, "Expense - " + transaction.getDetails());
                stmt.setLong(6, transaction.getId());
                stmt.executeUpdate();

                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, bankAccount.getId());
                stmt.setNull(3, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(4, transaction.getDebitAmount()); // credit_amount = transaction amount
                stmt.setString(5, "Bank account - " + transaction.getDetails());
                stmt.setLong(6, transaction.getId());
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Inner class for account mapping results
     */
    public static class AccountMapping {
        private final String accountCode;
        private final String accountName;

        public AccountMapping(String accountCode, String accountName) {
            this.accountCode = accountCode;
            this.accountName = accountName;
        }

        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
    }
}