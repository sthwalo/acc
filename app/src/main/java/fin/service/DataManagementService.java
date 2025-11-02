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
@SuppressWarnings("unused") // companyService and accountService reserved for future validation features
public class DataManagementService {
    private static final Logger LOGGER = Logger.getLogger(DataManagementService.class.getName());
    private final String dbUrl;
    
    // TODO: Use for company existence validation, fiscal period validation, and audit logging enrichment
    // Future: Validate company exists/active before operations, check fiscal periods are open
    private final CompanyService companyService;
    
    // TODO: Use for account existence validation, ownership validation, type checking, and status verification
    // Future: Validate accounts exist/active/belong to company, enforce accounting rules (asset vs liability)
    private final AccountService accountService;

    // SQL Parameter indices for manual invoice creation
    private static final int MANUAL_INVOICE_PARAM_COMPANY_ID = 1;
    private static final int MANUAL_INVOICE_PARAM_INVOICE_NUMBER = 2;
    private static final int MANUAL_INVOICE_PARAM_INVOICE_DATE = 3;
    private static final int MANUAL_INVOICE_PARAM_DESCRIPTION = 4;
    private static final int MANUAL_INVOICE_PARAM_AMOUNT = 5;
    private static final int MANUAL_INVOICE_PARAM_DEBIT_ACCOUNT_ID = 6;
    private static final int MANUAL_INVOICE_PARAM_CREDIT_ACCOUNT_ID = 7;
    private static final int MANUAL_INVOICE_PARAM_FISCAL_PERIOD_ID = 8;

    // SQL Parameter indices for journal entry header creation
    private static final int JOURNAL_HEADER_PARAM_COMPANY_ID = 1;
    private static final int JOURNAL_HEADER_PARAM_ENTRY_NUMBER = 2;
    private static final int JOURNAL_HEADER_PARAM_ENTRY_DATE = 3;
    private static final int JOURNAL_HEADER_PARAM_DESCRIPTION = 4;
    private static final int JOURNAL_HEADER_PARAM_FISCAL_PERIOD_ID = 5;

    // SQL Parameter indices for journal entry line creation
    private static final int JOURNAL_LINE_PARAM_JOURNAL_ENTRY_ID = 1;
    private static final int JOURNAL_LINE_PARAM_ACCOUNT_ID = 2;
    private static final int JOURNAL_LINE_PARAM_DESCRIPTION = 3;
    private static final int JOURNAL_LINE_PARAM_DEBIT_AMOUNT = 4;
    private static final int JOURNAL_LINE_PARAM_CREDIT_AMOUNT = 5;

    // SQL Parameter indices for data correction recording
    private static final int CORRECTION_PARAM_COMPANY_ID = 1;
    private static final int CORRECTION_PARAM_TRANSACTION_ID = 2;
    private static final int CORRECTION_PARAM_ORIGINAL_ACCOUNT_ID = 3;
    private static final int CORRECTION_PARAM_NEW_ACCOUNT_ID = 4;
    private static final int CORRECTION_PARAM_REASON = 5;
    private static final int CORRECTION_PARAM_CORRECTED_BY = 6;

    // SQL Parameter indices for transaction update
    private static final int UPDATE_TRANSACTION_PARAM_NEW_ACCOUNT_ID = 1;
    private static final int UPDATE_TRANSACTION_PARAM_TRANSACTION_ID = 2;
    private static final int UPDATE_TRANSACTION_PARAM_COMPANY_ID = 3;

    public DataManagementService(String initialDbUrl, CompanyService initialCompanyService, AccountService initialAccountService) {
        this.dbUrl = initialDbUrl;
        this.companyService = initialCompanyService;
        this.accountService = initialAccountService;
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
            
            pstmt.setLong(MANUAL_INVOICE_PARAM_COMPANY_ID, companyId);
            pstmt.setString(MANUAL_INVOICE_PARAM_INVOICE_NUMBER, invoiceNumber);
            pstmt.setDate(MANUAL_INVOICE_PARAM_INVOICE_DATE, Date.valueOf(invoiceDate));
            pstmt.setString(MANUAL_INVOICE_PARAM_DESCRIPTION, description);
            pstmt.setBigDecimal(MANUAL_INVOICE_PARAM_AMOUNT, amount);
            pstmt.setLong(MANUAL_INVOICE_PARAM_DEBIT_ACCOUNT_ID, debitAccountId);
            pstmt.setLong(MANUAL_INVOICE_PARAM_CREDIT_ACCOUNT_ID, creditAccountId);
            pstmt.setLong(MANUAL_INVOICE_PARAM_FISCAL_PERIOD_ID, fiscalPeriodId);
            
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
            pstmt.setLong(JOURNAL_HEADER_PARAM_COMPANY_ID, companyId);
            pstmt.setString(JOURNAL_HEADER_PARAM_ENTRY_NUMBER, entryNumber);
            pstmt.setDate(JOURNAL_HEADER_PARAM_ENTRY_DATE, Date.valueOf(entryDate));
            pstmt.setString(JOURNAL_HEADER_PARAM_DESCRIPTION, description);
            pstmt.setLong(JOURNAL_HEADER_PARAM_FISCAL_PERIOD_ID, fiscalPeriodId);
            
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
            pstmt.setLong(JOURNAL_LINE_PARAM_JOURNAL_ENTRY_ID, journalEntryId);
            pstmt.setLong(JOURNAL_LINE_PARAM_ACCOUNT_ID, line.getAccountId());
            pstmt.setString(JOURNAL_LINE_PARAM_DESCRIPTION, line.getDescription());
            pstmt.setBigDecimal(JOURNAL_LINE_PARAM_DEBIT_AMOUNT, line.getDebitAmount());
            pstmt.setBigDecimal(JOURNAL_LINE_PARAM_CREDIT_AMOUNT, line.getCreditAmount());
            
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
                    pstmt.setLong(CORRECTION_PARAM_COMPANY_ID, companyId);
                    pstmt.setLong(CORRECTION_PARAM_TRANSACTION_ID, transactionId);
                    pstmt.setLong(CORRECTION_PARAM_ORIGINAL_ACCOUNT_ID, originalAccountId);
                    pstmt.setLong(CORRECTION_PARAM_NEW_ACCOUNT_ID, newAccountId);
                    pstmt.setString(CORRECTION_PARAM_REASON, reason);
                    pstmt.setString(CORRECTION_PARAM_CORRECTED_BY, correctedBy);
                    
                    pstmt.executeUpdate();
                }
                
                // Then update the transaction
                String updateSql = 
                    "UPDATE bank_transactions SET account_id = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ? AND company_id = ?";
                    
                try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                    pstmt.setLong(UPDATE_TRANSACTION_PARAM_NEW_ACCOUNT_ID, newAccountId);
                    pstmt.setLong(UPDATE_TRANSACTION_PARAM_TRANSACTION_ID, transactionId);
                    pstmt.setLong(UPDATE_TRANSACTION_PARAM_COMPANY_ID, companyId);
                    
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
