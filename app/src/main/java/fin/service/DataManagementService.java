package fin.service;

import fin.model.*;
import java.sql.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.sql.Date; // Explicitly import SQL Date

/**
 * Service for managing data integrity, validation, and manual corrections.
 */
public class DataManagementService {
    private static final Logger LOGGER = Logger.getLogger(DataManagementService.class.getName());
    private final String dbUrl;
    private final CompanyService companyService;
    private final AccountService accountService;

    public DataManagementService(String dbUrl, CompanyService companyService, AccountService accountService) {
        this.dbUrl = dbUrl;
        this.companyService = companyService;
        this.accountService = accountService;
        initializeDatabase();
    }

    private void initializeDatabase() {
        String sql = 
            "CREATE TABLE IF NOT EXISTS manual_invoices (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "company_id INTEGER NOT NULL, " +
            "invoice_number TEXT NOT NULL, " +
            "invoice_date DATE NOT NULL, " +
            "description TEXT, " +
            "amount DECIMAL(15,2) NOT NULL, " +
            "debit_account_id INTEGER NOT NULL, " +
            "credit_account_id INTEGER NOT NULL, " +
            "fiscal_period_id INTEGER NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UNIQUE(company_id, invoice_number), " +
            "FOREIGN KEY(company_id) REFERENCES companies(id), " +
            "FOREIGN KEY(debit_account_id) REFERENCES accounts(id), " +
            "FOREIGN KEY(credit_account_id) REFERENCES accounts(id), " +
            "FOREIGN KEY(fiscal_period_id) REFERENCES fiscal_periods(id)" +
            "); " +
            
            "CREATE TABLE IF NOT EXISTS journal_entries (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "company_id INTEGER NOT NULL, " +
            "entry_number TEXT NOT NULL, " +
            "entry_date DATE NOT NULL, " +
            "description TEXT, " +
            "fiscal_period_id INTEGER NOT NULL, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "UNIQUE(company_id, entry_number), " +
            "FOREIGN KEY(company_id) REFERENCES companies(id), " +
            "FOREIGN KEY(fiscal_period_id) REFERENCES fiscal_periods(id)" +
            "); " +
            
            "CREATE TABLE IF NOT EXISTS journal_entry_lines (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "journal_entry_id INTEGER NOT NULL, " +
            "account_id INTEGER NOT NULL, " +
            "description TEXT, " +
            "debit_amount DECIMAL(15,2) DEFAULT 0, " +
            "credit_amount DECIMAL(15,2) DEFAULT 0, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(journal_entry_id) REFERENCES journal_entries(id), " +
            "FOREIGN KEY(account_id) REFERENCES accounts(id)" +
            "); " +
            
            "CREATE TABLE IF NOT EXISTS data_corrections (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "company_id INTEGER NOT NULL, " +
            "transaction_id INTEGER NOT NULL, " +
            "original_account_id INTEGER NOT NULL, " +
            "new_account_id INTEGER NOT NULL, " +
            "correction_reason TEXT NOT NULL, " +
            "corrected_by TEXT NOT NULL, " +
            "corrected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
            "FOREIGN KEY(company_id) REFERENCES companies(id), " +
            "FOREIGN KEY(transaction_id) REFERENCES bank_transactions(id), " +
            "FOREIGN KEY(original_account_id) REFERENCES accounts(id), " +
            "FOREIGN KEY(new_account_id) REFERENCES accounts(id)" +
            ");";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(sql);
            LOGGER.info("Data management tables created successfully");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing data management tables", e);
            throw new RuntimeException("Failed to initialize data management tables", e);
        }
    }

    /**
     * Resets all transactional data for a company while preserving master data.
     * This includes bank transactions, manual invoices, journal entries, etc.
     */
    public void resetCompanyData(Long companyId, boolean preserveMasterData) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // Delete transactional data
                // First delete journal entry lines (they link to journal entries)
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM journal_entry_lines WHERE journal_entry_id IN " +
                        "(SELECT id FROM journal_entries WHERE company_id = ?)")) {
                    pstmt.setLong(1, companyId);
                    pstmt.executeUpdate();
                }

                // Then delete from other tables that have company_id
                String[] tables = {
                    "data_corrections",
                    "journal_entries",
                    "manual_invoices",
                    "bank_transactions"
                };
                
                for (String table : tables) {
                    try (PreparedStatement pstmt = conn.prepareStatement(
                            "DELETE FROM " + table + " WHERE company_id = ?")) {
                        pstmt.setLong(1, companyId);
                        pstmt.executeUpdate();
                    }
                }
                
                // Optionally reset master data
                if (!preserveMasterData) {
                    String[] masterTables = {
                        "transaction_mapping_rules",
                        "accounts",
                        "account_categories",
                        "fiscal_periods"
                    };
                    
                    for (String table : masterTables) {
                        try (PreparedStatement pstmt = conn.prepareStatement(
                                "DELETE FROM " + table + " WHERE company_id = ?")) {
                            pstmt.setLong(1, companyId);
                            pstmt.executeUpdate();
                        }
                    }
                }
                
                conn.commit();
                LOGGER.info("Company data reset successful for company ID: " + companyId);
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error resetting company data", e);
            throw new RuntimeException("Failed to reset company data", e);
        }
    }

    /**
     * Creates a manual invoice with validation for duplicates.
     */
    public void createManualInvoice(Long companyId, String invoiceNumber, LocalDate invoiceDate,
                                  String description, BigDecimal amount, Long debitAccountId,
                                  Long creditAccountId, Long fiscalPeriodId) {
        // First check if invoice number already exists
        if (isInvoiceNumberExists(companyId, invoiceNumber)) {
            throw new IllegalArgumentException("Invoice number already exists: " + invoiceNumber);
        }
        
        String sql = 
            "INSERT INTO manual_invoices (company_id, invoice_number, invoice_date, " +
            "description, amount, debit_account_id, credit_account_id, fiscal_period_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setString(2, invoiceNumber);
            pstmt.setDate(3, Date.valueOf(invoiceDate));
            pstmt.setString(4, description);
            pstmt.setBigDecimal(5, amount);
            pstmt.setLong(6, debitAccountId);
            pstmt.setLong(7, creditAccountId);
            pstmt.setLong(8, fiscalPeriodId);
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating manual invoice", e);
            throw new RuntimeException("Failed to create manual invoice", e);
        }
    }

    private boolean isInvoiceNumberExists(Long companyId, String invoiceNumber) {
        String sql = "SELECT 1 FROM manual_invoices WHERE company_id = ? AND invoice_number = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setString(2, invoiceNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking invoice number existence", e);
            throw new RuntimeException("Failed to check invoice number", e);
        }
    }

    /**
     * Creates a journal entry with multiple lines. All lines must balance (debits = credits).
     */
    public void createJournalEntry(Long companyId, String entryNumber, LocalDate entryDate,
                                 String description, Long fiscalPeriodId,
                                 List<JournalEntryLine> lines) {
        // Validate that debits equal credits
        BigDecimal totalDebits = lines.stream()
            .map(JournalEntryLine::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalCredits = lines.stream()
            .map(JournalEntryLine::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException("Journal entry must balance. " +
                "Debits: " + totalDebits + ", Credits: " + totalCredits);
        }
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // First create the journal entry header
                Long journalEntryId = createJournalEntryHeader(conn, companyId, entryNumber,
                    entryDate, description, fiscalPeriodId);
                
                // Then create all the lines
                for (JournalEntryLine line : lines) {
                    createJournalEntryLine(conn, journalEntryId, line);
                }
                
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating journal entry", e);
            throw new RuntimeException("Failed to create journal entry", e);
        }
    }

    private Long createJournalEntryHeader(Connection conn, Long companyId, String entryNumber,
                                        LocalDate entryDate, String description,
                                        Long fiscalPeriodId) throws SQLException {
        String sql = 
            "INSERT INTO journal_entries (company_id, entry_number, entry_date, " +
            "description, fiscal_period_id) VALUES (?, ?, ?, ?, ?) " +
            "RETURNING id";
            
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setString(2, entryNumber);
            pstmt.setDate(3, Date.valueOf(entryDate));
            pstmt.setString(4, description);
            pstmt.setLong(5, fiscalPeriodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                throw new SQLException("Failed to create journal entry header");
            }
        }
    }

    private void createJournalEntryLine(Connection conn, Long journalEntryId,
                                      JournalEntryLine line) throws SQLException {
        String sql = 
            "INSERT INTO journal_entry_lines (journal_entry_id, account_id, description, " +
            "debit_amount, credit_amount) VALUES (?, ?, ?, ?, ?)";
            
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, line.getAccountId());
            pstmt.setString(3, line.getDescription());
            pstmt.setBigDecimal(4, line.getDebitAmount());
            pstmt.setBigDecimal(5, line.getCreditAmount());
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Records a correction to a transaction's categorization.
     */
    public void correctTransactionCategory(Long companyId, Long transactionId,
                                         Long originalAccountId, Long newAccountId,
                                         String reason, String correctedBy) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // First record the correction
                String correctionSql = 
                    "INSERT INTO data_corrections (company_id, transaction_id, " +
                    "original_account_id, new_account_id, correction_reason, corrected_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
                    
                try (PreparedStatement pstmt = conn.prepareStatement(correctionSql)) {
                    pstmt.setLong(1, companyId);
                    pstmt.setLong(2, transactionId);
                    pstmt.setLong(3, originalAccountId);
                    pstmt.setLong(4, newAccountId);
                    pstmt.setString(5, reason);
                    pstmt.setString(6, correctedBy);
                    
                    pstmt.executeUpdate();
                }
                
                // Then update the transaction
                String updateSql = 
                    "UPDATE bank_transactions SET account_id = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ? AND company_id = ?";
                    
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setLong(1, newAccountId);
                    pstmt.setLong(2, transactionId);
                    pstmt.setLong(3, companyId);
                    
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error correcting transaction category", e);
            throw new RuntimeException("Failed to correct transaction category", e);
        }
    }

    /**
     * Views correction history for a transaction.
     */
    public List<Map<String, Object>> getTransactionCorrectionHistory(Long transactionId) {
        String sql = 
            "SELECT c.*, " +
            "oa.account_code as original_account_code, " +
            "oa.account_name as original_account_name, " +
            "na.account_code as new_account_code, " +
            "na.account_name as new_account_name " +
            "FROM data_corrections c " +
            "JOIN accounts oa ON c.original_account_id = oa.id " +
            "JOIN accounts na ON c.new_account_id = na.id " +
            "WHERE c.transaction_id = ? " +
            "ORDER BY c.corrected_at DESC";
            
        List<Map<String, Object>> history = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, transactionId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> correction = new HashMap<>();
                    correction.put("correctedAt", rs.getTimestamp("corrected_at").toLocalDateTime());
                    correction.put("correctedBy", rs.getString("corrected_by"));
                    correction.put("reason", rs.getString("correction_reason"));
                    correction.put("originalAccountCode", rs.getString("original_account_code"));
                    correction.put("originalAccountName", rs.getString("original_account_name"));
                    correction.put("newAccountCode", rs.getString("new_account_code"));
                    correction.put("newAccountName", rs.getString("new_account_name"));
                    history.add(correction);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching transaction correction history", e);
            throw new RuntimeException("Failed to fetch correction history", e);
        }
        
        return history;
    }
}
