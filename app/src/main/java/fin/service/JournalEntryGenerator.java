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

    // SQL Parameter indices for journal entry header insertion
    private static final int JOURNAL_ENTRY_INSERT_REFERENCE = 1;
    private static final int JOURNAL_ENTRY_INSERT_ENTRY_DATE = 2;
    private static final int JOURNAL_ENTRY_INSERT_DESCRIPTION = 3;
    private static final int JOURNAL_ENTRY_INSERT_COMPANY_ID = 4;
    private static final int JOURNAL_ENTRY_INSERT_FISCAL_PERIOD_ID = 5;
    private static final int JOURNAL_ENTRY_INSERT_CREATED_BY = 6;
    private static final int JOURNAL_ENTRY_INSERT_CREATED_AT = 7;

    // SQL Parameter indices for journal entry line insertion
    private static final int JOURNAL_LINE_INSERT_JOURNAL_ENTRY_ID = 1;
    private static final int JOURNAL_LINE_INSERT_ACCOUNT_ID = 2;
    private static final int JOURNAL_LINE_INSERT_DEBIT_AMOUNT = 3;
    private static final int JOURNAL_LINE_INSERT_CREDIT_AMOUNT = 4;
    private static final int JOURNAL_LINE_INSERT_DESCRIPTION = 5;
    private static final int JOURNAL_LINE_INSERT_SOURCE_TRANSACTION_ID = 6;

    // Account code constants
    private static final int PARENT_ACCOUNT_CODE_LENGTH = 4;

    public JournalEntryGenerator(String initialDbUrl, AccountRepository initialAccountRepository) {
        this.dbUrl = initialDbUrl;
        this.accountRepository = initialAccountRepository;
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
        return createJournalEntryForTransaction(transaction, classificationResult, false);
    }

    /**
     * Creates a journal entry for a classified transaction
     *
     * @param transaction The bank transaction to create an entry for
     * @param classificationResult The classification result containing account info
     * @param forceCreate If true, skips the existence check (used during regeneration)
     * @return true if the journal entry was created successfully
     */
    public boolean createJournalEntryForTransaction(BankTransaction transaction, ClassificationResult classificationResult, boolean forceCreate) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            // Check if journal entry already exists for this transaction (unless forceCreate is true)
            if (!forceCreate && journalEntryExistsForTransaction(conn, transaction)) {
                LOGGER.info("Journal entry already exists for transaction: " + transaction.getId() + " - skipping");
                return true;  // Return true as the entry already exists
            }

            // Get or create the classified account using AccountRepository
            Long classifiedAccountId = accountRepository.getOrCreateDetailedAccount(
                classificationResult.getAccountCode(),
                classificationResult.getAccountName(),
                getParentAccountCode(classificationResult.getAccountCode()),
                getAccountCategory(classificationResult.getAccountCode())
            );

            // Get the bank account ID
            Long bankAccountId = getBankAccountId(conn, transaction.getCompanyId());

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
        if (accountCode.length() >= PARENT_ACCOUNT_CODE_LENGTH) {
            return accountCode.substring(0, PARENT_ACCOUNT_CODE_LENGTH);
        }
        return accountCode;
    }

    private String getAccountCategory(String accountCode) {
        if (accountCode.startsWith("1")) {
            return "Assets";
        }
        if (accountCode.startsWith("2")) {
            return "Liabilities";
        }
        if (accountCode.startsWith("3")) {
            return "Equity";
        }
        if (accountCode.startsWith("4") || accountCode.startsWith("5") || accountCode.startsWith("6")) {
            return "Revenue";
        }
        if (accountCode.startsWith("7") || accountCode.startsWith("8") || accountCode.startsWith("9")) {
            return "Expenses";
        }
        return "Expenses"; // Default
    }

    private Long getBankAccountId(Connection conn, Long companyId) throws SQLException {
        String sql = "SELECT id FROM accounts WHERE account_code = '1100' AND company_id = ? LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        throw new SQLException("Bank account (1100) not found for company: " + companyId);
    }

    private long insertJournalEntry(Connection conn, JournalEntry journalEntry) throws SQLException {
        String sql = """
            INSERT INTO journal_entries (reference, entry_date, description, company_id, fiscal_period_id, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(JOURNAL_ENTRY_INSERT_REFERENCE, journalEntry.getReference());
            stmt.setDate(JOURNAL_ENTRY_INSERT_ENTRY_DATE, Date.valueOf(journalEntry.getEntryDate()));
            stmt.setString(JOURNAL_ENTRY_INSERT_DESCRIPTION, journalEntry.getDescription());
            stmt.setLong(JOURNAL_ENTRY_INSERT_COMPANY_ID, journalEntry.getCompanyId());
            stmt.setLong(JOURNAL_ENTRY_INSERT_FISCAL_PERIOD_ID, journalEntry.getFiscalPeriodId());
            stmt.setString(JOURNAL_ENTRY_INSERT_CREATED_BY, journalEntry.getCreatedBy());
            stmt.setTimestamp(JOURNAL_ENTRY_INSERT_CREATED_AT, Timestamp.valueOf(journalEntry.getCreatedAt()));

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
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, source_transaction_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        // Determine transaction type and create double-entry lines
        boolean isIncome = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isExpense = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (isIncome) {
                // Income: Credit income account, Debit bank account
                BigDecimal amount = transaction.getCreditAmount();

                // Credit the income account
                stmt.setLong(JOURNAL_LINE_INSERT_JOURNAL_ENTRY_ID, journalEntryId);
                stmt.setLong(JOURNAL_LINE_INSERT_ACCOUNT_ID, classifiedAccountId);
                stmt.setNull(JOURNAL_LINE_INSERT_DEBIT_AMOUNT, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(JOURNAL_LINE_INSERT_CREDIT_AMOUNT, amount); // credit_amount = transaction amount
                stmt.setString(JOURNAL_LINE_INSERT_DESCRIPTION, "Income - " + transaction.getDetails());
                stmt.setLong(JOURNAL_LINE_INSERT_SOURCE_TRANSACTION_ID, transaction.getId()); // source_transaction_id
                stmt.executeUpdate();

                // Debit the bank account
                stmt.setLong(JOURNAL_LINE_INSERT_JOURNAL_ENTRY_ID, journalEntryId);
                stmt.setLong(JOURNAL_LINE_INSERT_ACCOUNT_ID, bankAccountId);
                stmt.setBigDecimal(JOURNAL_LINE_INSERT_DEBIT_AMOUNT, amount); // debit_amount = transaction amount
                stmt.setNull(JOURNAL_LINE_INSERT_CREDIT_AMOUNT, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(JOURNAL_LINE_INSERT_DESCRIPTION, "Bank account - " + transaction.getDetails());
                stmt.setLong(JOURNAL_LINE_INSERT_SOURCE_TRANSACTION_ID, transaction.getId()); // source_transaction_id
                stmt.executeUpdate();

            } else if (isExpense) {
                // Expense: Debit expense account, Credit bank account
                BigDecimal amount = transaction.getDebitAmount();

                // Debit the expense account
                stmt.setLong(JOURNAL_LINE_INSERT_JOURNAL_ENTRY_ID, journalEntryId);
                stmt.setLong(JOURNAL_LINE_INSERT_ACCOUNT_ID, classifiedAccountId);
                stmt.setBigDecimal(JOURNAL_LINE_INSERT_DEBIT_AMOUNT, amount); // debit_amount = transaction amount
                stmt.setNull(JOURNAL_LINE_INSERT_CREDIT_AMOUNT, Types.NUMERIC); // credit_amount = NULL
                stmt.setString(JOURNAL_LINE_INSERT_DESCRIPTION, "Expense - " + transaction.getDetails());
                stmt.setLong(JOURNAL_LINE_INSERT_SOURCE_TRANSACTION_ID, transaction.getId()); // source_transaction_id
                stmt.executeUpdate();

                // Credit the bank account
                stmt.setLong(JOURNAL_LINE_INSERT_JOURNAL_ENTRY_ID, journalEntryId);
                stmt.setLong(JOURNAL_LINE_INSERT_ACCOUNT_ID, bankAccountId);
                stmt.setNull(JOURNAL_LINE_INSERT_DEBIT_AMOUNT, Types.NUMERIC); // debit_amount = NULL
                stmt.setBigDecimal(JOURNAL_LINE_INSERT_CREDIT_AMOUNT, amount); // credit_amount = transaction amount
                stmt.setString(JOURNAL_LINE_INSERT_DESCRIPTION, "Bank account - " + transaction.getDetails());
                stmt.setLong(JOURNAL_LINE_INSERT_SOURCE_TRANSACTION_ID, transaction.getId()); // source_transaction_id
                stmt.executeUpdate();
            }
        }
    }

    /**
     * Check if a journal entry already exists for this transaction
     * Uses source_transaction_id linkage for reliable duplicate detection
     */
    private boolean journalEntryExistsForTransaction(Connection conn, BankTransaction transaction) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM journal_entry_lines 
            WHERE source_transaction_id = ?
            """;
            
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, transaction.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }
}