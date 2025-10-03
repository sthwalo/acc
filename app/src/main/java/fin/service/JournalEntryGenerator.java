package fin.service;

import fin.model.BankTransaction;
import fin.model.JournalEntry;
import fin.model.ClassificationResult;
import fin.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JournalEntryGenerator - Creates journal entries for classified transactions
 *
 * This service handles the creation of double-entry journal entries based on
 * transaction classification results. It integrates with AccountRepository to
 * ensure proper account management and uses ClassificationResult objects
 * from transaction classification services.
 */
public class JournalEntryGenerator {
    private static final Logger LOGGER = Logger.getLogger(JournalEntryGenerator.class.getName());

    private final String dbUrl;
    private final AccountRepository accountRepository;

    public JournalEntryGenerator(String dbUrl, AccountRepository accountRepository) {
        this.dbUrl = dbUrl;
        this.accountRepository = accountRepository;
    }

    /**
     * Get database connection - can be overridden for testing
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Creates a journal entry for a classified transaction
     *
     * @param transaction The bank transaction to create an entry for
     * @param classificationResult The classification result containing account info
     * @return true if the journal entry was created successfully
     */
    public boolean createJournalEntryForTransaction(BankTransaction transaction, ClassificationResult classificationResult) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // Get or create the classified account using AccountRepository
            Long classifiedAccountId = accountRepository.getOrCreateDetailedAccount(
                classificationResult.getAccountCode(),
                classificationResult.getAccountName(),
                getParentAccountCode(classificationResult.getAccountCode()),
                getAccountCategory(classificationResult.getAccountCode())
            );

            // Get the bank account ID
            Long bankAccountId = getBankAccountId(conn);

            // Create journal entry header
            JournalEntry journalEntry = createJournalEntryHeader(transaction);

            // Insert journal entry and get ID
            long journalEntryId = insertJournalEntry(conn, journalEntry);

            // Create journal entry lines
            createJournalEntryLines(conn, journalEntryId, transaction, classifiedAccountId, bankAccountId);

            conn.commit();
            LOGGER.info("Created journal entry " + journalEntryId + " for transaction: " + transaction.getId());
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating journal entry for transaction: " + transaction.getId(), e);
            return false;
        }
    }

    /**
     * Generates journal entries for all unclassified transactions in a company
     *
     * @param companyId The company ID
     * @return The number of journal entries created
     */
    public int generateJournalEntriesForUnclassifiedTransactions(Long companyId) {
        // This would typically integrate with TransactionBatchProcessor
        // For now, return 0 as this is a placeholder for batch processing
        LOGGER.info("Batch journal entry generation not yet implemented");
        return 0;
    }

    private JournalEntry createJournalEntryHeader(BankTransaction transaction) {
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setReference(generateReference(transaction));
        journalEntry.setEntryDate(transaction.getTransactionDate());
        journalEntry.setDescription(transaction.getDetails());
        journalEntry.setCompanyId(transaction.getCompanyId());
        journalEntry.setFiscalPeriodId(transaction.getFiscalPeriodId());
        journalEntry.setCreatedBy("SYSTEM");
        journalEntry.setCreatedAt(LocalDateTime.now());
        return journalEntry;
    }

    private String generateReference(BankTransaction transaction) {
        return "JE-" + transaction.getId() + "-" + System.currentTimeMillis();
    }

    private String getParentAccountCode(String accountCode) {
        // Extract parent code (first 4 digits for main categories)
        if (accountCode.length() >= 4) {
            return accountCode.substring(0, 4);
        }
        return accountCode;
    }

    private String getAccountCategory(String accountCode) {
        if (accountCode.startsWith("1")) return "Assets";
        if (accountCode.startsWith("2")) return "Liabilities";
        if (accountCode.startsWith("3")) return "Equity";
        if (accountCode.startsWith("4") || accountCode.startsWith("5") || accountCode.startsWith("6")) return "Revenue";
        if (accountCode.startsWith("7") || accountCode.startsWith("8") || accountCode.startsWith("9")) return "Expenses";
        return "Expenses"; // Default
    }

    private Long getBankAccountId(Connection conn) throws SQLException {
        String sql = "SELECT id FROM accounts WHERE account_code = '1100' LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        throw new SQLException("Bank account (1100) not found");
    }

    private long insertJournalEntry(Connection conn, JournalEntry journalEntry) throws SQLException {
        String sql = """
            INSERT INTO journal_entries (reference, entry_date, description, company_id, fiscal_period_id, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, journalEntry.getReference());
            stmt.setDate(2, Date.valueOf(journalEntry.getEntryDate()));
            stmt.setString(3, journalEntry.getDescription());
            stmt.setLong(4, journalEntry.getCompanyId());
            stmt.setLong(5, journalEntry.getFiscalPeriodId());
            stmt.setString(6, journalEntry.getCreatedBy());
            stmt.setTimestamp(7, Timestamp.valueOf(journalEntry.getCreatedAt()));

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
                                       Long classifiedAccountId, Long bankAccountId) throws SQLException {
        String sql = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description)
            VALUES (?, ?, ?, ?, ?)
            """;

        // Determine transaction type and create double-entry lines
        boolean isIncome = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isExpense = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (isIncome) {
                // Income: Credit income account, Debit bank account
                BigDecimal amount = transaction.getCreditAmount();

                // Credit the income account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, classifiedAccountId);
                stmt.setNull(3, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(4, amount); // credit_amount = transaction amount
                stmt.setString(5, "Income - " + transaction.getDetails());
                stmt.executeUpdate();

                // Debit the bank account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, bankAccountId);
                stmt.setBigDecimal(3, amount); // debit_amount = transaction amount
                stmt.setNull(4, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(5, "Bank account - " + transaction.getDetails());
                stmt.executeUpdate();

            } else if (isExpense) {
                // Expense: Debit expense account, Credit bank account
                BigDecimal amount = transaction.getDebitAmount();

                // Debit the expense account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, classifiedAccountId);
                stmt.setBigDecimal(3, amount); // debit_amount = transaction amount
                stmt.setNull(4, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(5, "Expense - " + transaction.getDetails());
                stmt.executeUpdate();

                // Credit the bank account
                stmt.setLong(1, journalEntryId);
                stmt.setLong(2, bankAccountId);
                stmt.setNull(3, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(4, amount); // credit_amount = transaction amount
                stmt.setString(5, "Bank account - " + transaction.getDetails());
                stmt.executeUpdate();
            }
        }
    }
}