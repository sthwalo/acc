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

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling opening and closing balances for fiscal periods.
 * Creates journal entries to establish opening balances at the start of each fiscal period.
 */
public class OpeningBalanceService {
    private static final Logger LOGGER = Logger.getLogger(OpeningBalanceService.class.getName());
    private final String dbUrl;

    // SQL Parameter Constants
    private static final int JOURNAL_ENTRY_REFERENCE_PARAM = 1;
    private static final int JOURNAL_ENTRY_DATE_PARAM = 2;
    private static final int JOURNAL_ENTRY_DESCRIPTION_PARAM = 3;
    private static final int JOURNAL_ENTRY_FISCAL_PERIOD_PARAM = 4;
    private static final int JOURNAL_ENTRY_COMPANY_PARAM = 5;
    private static final int JOURNAL_ENTRY_CREATED_BY_PARAM = 6;

    private static final int JOURNAL_LINE_ENTRY_ID_PARAM = 1;
    private static final int JOURNAL_LINE_ACCOUNT_ID_PARAM = 2;
    private static final int JOURNAL_LINE_DEBIT_PARAM = 3;
    private static final int JOURNAL_LINE_CREDIT_PARAM = 4;
    private static final int JOURNAL_LINE_DESCRIPTION_PARAM = 5;
    private static final int JOURNAL_LINE_REFERENCE_PARAM = 6;

    public OpeningBalanceService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }

    /**
     * Calculate and create opening balance journal entry for a fiscal period.
     * 
     * @param companyId The company ID
     * @param fiscalPeriodId The fiscal period ID
     * @param createdBy The user creating the entry
     * @return true if successful, false otherwise
     */
    public boolean createOpeningBalanceEntry(Long companyId, Long fiscalPeriodId, String createdBy) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // Get fiscal period details
                FiscalPeriodInfo fiscalPeriod = getFiscalPeriodInfo(conn, fiscalPeriodId);
                if (fiscalPeriod == null) {
                    LOGGER.warning("Fiscal period not found: " + fiscalPeriodId);
                    return false;
                }
                
                // Calculate opening balance from first transaction
                BigDecimal openingBalance = calculateOpeningBalance(conn, companyId, fiscalPeriodId, fiscalPeriod.startDate);
                if (openingBalance == null || openingBalance.compareTo(BigDecimal.ZERO) == 0) {
                    LOGGER.info("No opening balance found for fiscal period: " + fiscalPeriodId);
                    return false;
                }
                
                LOGGER.info(String.format("📊 Calculated opening balance: R %,.2f for fiscal period %d (date: %s)", 
                    openingBalance, fiscalPeriodId, fiscalPeriod.startDate));
                
                // Check if opening balance entry already exists
                if (openingBalanceExists(conn, companyId, fiscalPeriodId)) {
                    LOGGER.info("⚠️  Opening balance entry already exists for fiscal period: " + fiscalPeriodId);
                    // Delete existing entry to recreate
                    deleteOpeningBalanceEntry(conn, companyId, fiscalPeriodId);
                }
                
                // Create journal entry
                Long journalEntryId = createJournalEntryHeader(conn, fiscalPeriod, createdBy);
                
                // Get account IDs
                Long bankAccountId = getAccountByCode(conn, companyId, "1100");  // Bank - Current Account
                Long openingBalanceEquityAccountId = getOpeningBalanceEquityAccount(conn, companyId);
                
                if (bankAccountId == null) {
                    throw new SQLException("Bank account (1100) not found for company: " + companyId);
                }
                if (openingBalanceEquityAccountId == null) {
                    throw new SQLException("Opening Balance Equity account (5300) not found for company: " + companyId);
                }
                
                // Create journal entry lines
                createJournalEntryLines(conn, journalEntryId, bankAccountId, openingBalanceEquityAccountId, 
                                      openingBalance, fiscalPeriod.reference);
                
                conn.commit();
                LOGGER.info(String.format("✅ Created opening balance entry: %s for R %,.2f", 
                    fiscalPeriod.reference, openingBalance));
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                LOGGER.log(Level.SEVERE, "❌ Error creating opening balance entry", e);
                throw e;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "❌ Database connection error", e);
            return false;
        }
    }

    /**
     * Calculate opening balance from the first transaction's balance field.
     * Opening balance = first transaction's balance + debit - credit
     */
    private BigDecimal calculateOpeningBalance(Connection conn, Long companyId, Long fiscalPeriodId, LocalDate startDate) throws SQLException {
        String sql = """
            SELECT balance, debit_amount, credit_amount
            FROM bank_transactions
            WHERE company_id = ? AND fiscal_period_id = ?
            ORDER BY transaction_date, id
            LIMIT 1
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                BigDecimal balance = rs.getBigDecimal("balance");
                BigDecimal debit = rs.getBigDecimal("debit_amount");
                BigDecimal credit = rs.getBigDecimal("credit_amount");
                
                // Opening balance = current balance + debit - credit
                if (balance != null) {
                    BigDecimal openingBalance = balance;
                    if (debit != null) {
                        openingBalance = openingBalance.add(debit);
                    }
                    if (credit != null) {
                        openingBalance = openingBalance.subtract(credit);
                    }
                    return openingBalance;
                }
            }
        }
        
        return null;
    }

    /**
     * Check if opening balance entry already exists.
     */
    private boolean openingBalanceExists(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT COUNT(*) as count
            FROM journal_entries
            WHERE company_id = ? AND fiscal_period_id = ?
            AND reference LIKE 'OB-%'
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") > 0;
            }
        }
        
        return false;
    }

    /**
     * Delete existing opening balance entry.
     */
    private void deleteOpeningBalanceEntry(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        // First delete journal entry lines
        String deleteLinesSQL = """
            DELETE FROM journal_entry_lines
            WHERE journal_entry_id IN (
                SELECT id FROM journal_entries
                WHERE company_id = ? AND fiscal_period_id = ?
                AND reference LIKE 'OB-%'
            )
            """;
        
        // Then delete journal entry header
        String deleteHeaderSQL = """
            DELETE FROM journal_entries
            WHERE company_id = ? AND fiscal_period_id = ?
            AND reference LIKE 'OB-%'
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(deleteLinesSQL)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            pstmt.executeUpdate();
        }
        
        try (PreparedStatement pstmt = conn.prepareStatement(deleteHeaderSQL)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            pstmt.executeUpdate();
        }
        
        LOGGER.info("Deleted existing opening balance entry for fiscal period: " + fiscalPeriodId);
    }

    /**
     * Get fiscal period information.
     */
    private FiscalPeriodInfo getFiscalPeriodInfo(Connection conn, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT id, company_id, period_name, start_date, end_date
            FROM fiscal_periods
            WHERE id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, fiscalPeriodId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                FiscalPeriodInfo info = new FiscalPeriodInfo();
                info.id = rs.getLong("id");
                info.companyId = rs.getLong("company_id");
                info.periodName = rs.getString("period_name");
                info.startDate = rs.getDate("start_date").toLocalDate();
                // endDate field removed - was assigned but never used
                info.reference = "OB-" + info.id;
                return info;
            }
        }
        
        return null;
    }

    /**
     * Create journal entry header.
     */
    private Long createJournalEntryHeader(Connection conn, FiscalPeriodInfo fiscalPeriod, String createdBy) throws SQLException {
        String description = "Opening Balance - " + fiscalPeriod.periodName;
        
        String sql = """
            INSERT INTO journal_entries (reference, entry_date, description, fiscal_period_id,
                                       company_id, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(JOURNAL_ENTRY_REFERENCE_PARAM, fiscalPeriod.reference);
            pstmt.setDate(JOURNAL_ENTRY_DATE_PARAM, java.sql.Date.valueOf(fiscalPeriod.startDate));
            pstmt.setString(JOURNAL_ENTRY_DESCRIPTION_PARAM, description);
            pstmt.setLong(JOURNAL_ENTRY_FISCAL_PERIOD_PARAM, fiscalPeriod.id);
            pstmt.setLong(JOURNAL_ENTRY_COMPANY_PARAM, fiscalPeriod.companyId);
            pstmt.setString(JOURNAL_ENTRY_CREATED_BY_PARAM, createdBy);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        
        throw new SQLException("Failed to create journal entry header");
    }

    /**
     * Create journal entry lines for opening balance.
     */
    private void createJournalEntryLines(Connection conn, Long journalEntryId, Long bankAccountId,
                                        Long openingBalanceEquityAccountId, BigDecimal openingBalance,
                                        String reference) throws SQLException {
        String sql = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount,
                                           credit_amount, description, reference, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Line 1: DEBIT Bank Account (asset increases on debit)
            pstmt.setLong(JOURNAL_LINE_ENTRY_ID_PARAM, journalEntryId);
            pstmt.setLong(JOURNAL_LINE_ACCOUNT_ID_PARAM, bankAccountId);
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_PARAM, openingBalance);  // DEBIT
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_PARAM, null);
            pstmt.setString(JOURNAL_LINE_DESCRIPTION_PARAM, "Bank - Current Account");
            pstmt.setString(JOURNAL_LINE_REFERENCE_PARAM, reference + "-L1");
            pstmt.executeUpdate();
            
            // Line 2: CREDIT Opening Balance Equity (temporary equity account - Cash Flow Statement only)
            pstmt.setLong(JOURNAL_LINE_ENTRY_ID_PARAM, journalEntryId);
            pstmt.setLong(JOURNAL_LINE_ACCOUNT_ID_PARAM, openingBalanceEquityAccountId);
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_PARAM, null);
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_PARAM, openingBalance);  // CREDIT
            pstmt.setString(JOURNAL_LINE_DESCRIPTION_PARAM, "Opening Balance Equity - Cash Flow Statement Only");
            pstmt.setString(JOURNAL_LINE_REFERENCE_PARAM, reference + "-L2");
            pstmt.executeUpdate();
        }
    }

    /**
     * Get account ID by account code.
     */
    private Long getAccountByCode(Connection conn, Long companyId, String accountCode) throws SQLException {
        String sql = """
            SELECT id FROM accounts
            WHERE company_id = ? AND account_code = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setString(2, accountCode);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        
        return null;
    }

    /**
     * Get Opening Balance Equity account for opening balance.
     * Tries account code 5300 first, then searches by name.
     */
    private Long getOpeningBalanceEquityAccount(Connection conn, Long companyId) throws SQLException {
        // Try 5300 first (Opening Balance Equity - created for this purpose)
        Long accountId = getAccountByCode(conn, companyId, "5300");
        if (accountId != null) {
            return accountId;
        }
        
        // Search by name containing "opening" AND "balance"
        String sql = """
            SELECT id FROM accounts
            WHERE company_id = ? 
            AND account_name ILIKE '%opening%'
            AND account_name ILIKE '%balance%'
            LIMIT 1
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        
        throw new SQLException("Opening Balance Equity account (5300) not found for company: " + companyId);
    }

    /**
     * Calculate closing balance for a fiscal period.
     */
    public BigDecimal calculateClosingBalance(Long companyId, Long fiscalPeriodId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String sql = """
                SELECT balance
                FROM bank_transactions
                WHERE company_id = ? AND fiscal_period_id = ?
                ORDER BY transaction_date DESC, id DESC
                LIMIT 1
                """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, companyId);
                pstmt.setLong(2, fiscalPeriodId);
                
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating closing balance", e);
        }
        
        return null;
    }

    /**
     * Inner class to hold fiscal period information.
     */
    private static class FiscalPeriodInfo {
        Long id;
        Long companyId;
        String periodName;
        LocalDate startDate;
        // endDate field removed - was assigned but never used
        String reference;
    }
}
