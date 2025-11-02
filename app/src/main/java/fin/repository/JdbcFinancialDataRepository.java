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

package fin.repository;

import fin.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

/**
 * JDBC implementation of FinancialDataRepository.
 * Provides centralized database access for all financial reports.
 */
public class JdbcFinancialDataRepository implements FinancialDataRepository {

    @SuppressWarnings("EI_EXPOSE_REP") // DataSource is thread-safe and not exposed
    private final DataSource dataSource;
    
    // Prepared statement parameter indices
    private static final int PREPARED_STATEMENT_PARAM_1 = 1;
    private static final int PREPARED_STATEMENT_PARAM_2 = 2;
    private static final int PREPARED_STATEMENT_PARAM_3 = 3;
    private static final int PREPARED_STATEMENT_PARAM_4 = 4;

    public JdbcFinancialDataRepository(@SuppressWarnings("EI_EXPOSE_REP") DataSource initialDataSource) {
        this.dataSource = initialDataSource;
    }

    @Override
    public List<BankTransaction> getBankTransactions(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT id, company_id, fiscal_period_id, transaction_date, details,
                   debit_amount, credit_amount, balance, account_code, account_name
            FROM bank_transactions
            WHERE company_id = ? AND fiscal_period_id = ?
            ORDER BY transaction_date, id
            """;

        List<BankTransaction> transactions = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactions.add(mapToBankTransaction(rs));
                }
            }
        }

        return transactions;
    }

    @Override
    public Map<String, BigDecimal> getAccountBalancesByType(int companyId, int fiscalPeriodId, String accountType) throws SQLException {
        // FIXED: Get balances from journal_entry_lines for proper double-entry accounting
        String sql = """
            SELECT
                CASE
                    WHEN a.account_code LIKE '1%' THEN 'ASSETS'
                    WHEN a.account_code LIKE '2%' THEN 'LIABILITIES'
                    WHEN a.account_code LIKE '3%' THEN 'EQUITY'
                    WHEN a.account_code LIKE '4%' THEN 'REVENUE'
                    WHEN a.account_code LIKE '5%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%' THEN 'EXPENSES'
                    ELSE 'OTHER'
                END as account_type,
                COALESCE(SUM(jel.debit_amount), 0) as total_debits,
                COALESCE(SUM(jel.credit_amount), 0) as total_credits
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ?
            GROUP BY
                CASE
                    WHEN a.account_code LIKE '1%' THEN 'ASSETS'
                    WHEN a.account_code LIKE '2%' THEN 'LIABILITIES'
                    WHEN a.account_code LIKE '3%' THEN 'EQUITY'
                    WHEN a.account_code LIKE '4%' THEN 'REVENUE'
                    WHEN a.account_code LIKE '5%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%' THEN 'EXPENSES'
                    ELSE 'OTHER'
                END
            """;

        Map<String, BigDecimal> balances = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setInt(PREPARED_STATEMENT_PARAM_3, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String type = rs.getString("account_type");
                    BigDecimal debits = rs.getBigDecimal("total_debits");
                    BigDecimal credits = rs.getBigDecimal("total_credits");
                    
                    // Calculate net balance based on account type
                    BigDecimal netBalance;
                    if ("REVENUE".equals(type)) {
                        // For revenue: credits increase revenue, debits decrease revenue
                        netBalance = credits.subtract(debits);
                    } else if ("EXPENSES".equals(type)) {
                        // For expenses: debits increase expenses, credits decrease expenses
                        netBalance = debits.subtract(credits);
                    } else if ("ASSETS".equals(type)) {
                        // For assets: debits increase assets, credits decrease assets
                        netBalance = debits.subtract(credits);
                    } else if ("LIABILITIES".equals(type)) {
                        // For liabilities: credits increase liabilities, debits decrease liabilities
                        netBalance = credits.subtract(debits);
                    } else if ("EQUITY".equals(type)) {
                        // For equity: credits increase equity, debits decrease equity
                        netBalance = credits.subtract(debits);
                    } else {
                        // For other accounts: default to debits - credits
                        netBalance = debits.subtract(credits);
                    }

                    if (accountType == null || accountType.equals(type)) {
                        balances.put(type, netBalance);
                    }
                }
            }
        }

        return balances;
    }

    @Override
    public List<JournalEntry> getJournalEntries(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT je.id, je.entry_date, je.description, je.reference_number,
                   jel.account_code, jel.debit_amount, jel.credit_amount, jel.description as line_description
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            ORDER BY je.entry_date, je.id
            """;

        List<JournalEntry> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Map to JournalEntry - you'll need to implement this based on your JournalEntry model
                    // For now, returning empty list as audit trail might not be critical
                }
            }
        }

        return entries;
    }

    @Override
    public Company getCompany(int companyId) throws SQLException {
        String sql = "SELECT id, name, registration_number FROM companies WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Company company = new Company();
                    company.setId(rs.getLong("id"));
                    company.setName(rs.getString("name"));
                    company.setRegistrationNumber(rs.getString("registration_number"));
                    return company;
                }
            }
        }

        throw new SQLException("Company not found: " + companyId);
    }

    @Override
    public FiscalPeriod getFiscalPeriod(int fiscalPeriodId) throws SQLException {
        String sql = "SELECT id, company_id, period_name, start_date, end_date FROM fiscal_periods WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fiscalPeriodId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    FiscalPeriod period = new FiscalPeriod();
                    period.setId(rs.getLong("id"));
                    period.setCompanyId(rs.getLong("company_id"));
                    period.setPeriodName(rs.getString("period_name"));
                    period.setStartDate(rs.getDate("start_date").toLocalDate());
                    period.setEndDate(rs.getDate("end_date").toLocalDate());
                    return period;
                }
            }
        }

        throw new SQLException("Fiscal period not found: " + fiscalPeriodId);
    }

    @Override
    public BigDecimal getOpeningBalance(int companyId, int fiscalPeriodId) throws SQLException {
        // Get the current fiscal period to find opening balance entries
        FiscalPeriod currentPeriod = getFiscalPeriod(fiscalPeriodId);

        // First, try to find opening balance from previous fiscal period
        String sql = """
            SELECT id FROM fiscal_periods
            WHERE company_id = ? AND end_date < ?
            ORDER BY end_date DESC LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setDate(2, java.sql.Date.valueOf(currentPeriod.getStartDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int previousPeriodId = rs.getInt("id");

                    // Calculate the closing balance of the previous period
                    // FIXED: Read from journal_entry_lines for proper double-entry accounting
                    String balanceSql = """
                        SELECT
                            COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0) as closing_balance
                        FROM journal_entry_lines jel
                        JOIN journal_entries je ON jel.journal_entry_id = je.id
                        WHERE je.company_id = ? AND je.fiscal_period_id = ?
                        """;

                    try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                        balanceStmt.setInt(1, companyId);
                        balanceStmt.setInt(2, previousPeriodId);

                        try (ResultSet balanceRs = balanceStmt.executeQuery()) {
                            if (balanceRs.next()) {
                                BigDecimal balance = balanceRs.getBigDecimal("closing_balance");
                                if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
                                    return balance;
                                }
                            }
                        }
                    }
                }
            }
        }

        // If no previous period found or balance is zero, look for opening balance journal entries
        // Opening balance entries are typically dated at the start of the fiscal period
        // For cashbook, we want the debit amount to the bank account (1100) as the opening balance
        String openingSql = """
            SELECT jel.debit_amount as opening_balance
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
                AND je.entry_date = ? AND LOWER(je.description) LIKE '%opening%balance%'
                AND a.account_code = '1100' AND jel.debit_amount IS NOT NULL
            LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(openingSql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setDate(PREPARED_STATEMENT_PARAM_3, java.sql.Date.valueOf(currentPeriod.getStartDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("opening_balance");
                    if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
                        return balance;
                    }
                }
            }
        }

        // If still no opening balance found, return zero
        return BigDecimal.ZERO;
    }

    @Override
    public List<TrialBalanceEntry> getTrialBalanceEntries(int companyId, int fiscalPeriodId) throws SQLException {
        // Get all accounts for the company WITH their account type normal balance
        String sql = """
            SELECT a.account_code, a.account_name, at.normal_balance
            FROM accounts a
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            WHERE a.company_id = ?
            ORDER BY a.account_code
            """;

        List<TrialBalanceEntry> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");
                    String normalBalance = rs.getString("normal_balance"); // 'D' or 'C'

                    // Calculate opening balance for this account from previous periods
                    BigDecimal openingBalance = getAccountOpeningBalance(companyId, fiscalPeriodId, accountCode);

                    // Calculate period movements for this account
                    BigDecimal[] periodMovements = getAccountPeriodMovements(companyId, fiscalPeriodId, accountCode);
                    BigDecimal periodDebits = periodMovements[0];
                    BigDecimal periodCredits = periodMovements[1];

                    // Calculate closing balance based on account type normal balance
                    // Assets & Expenses (D): Closing = Opening + Debits - Credits
                    // Liabilities, Equity & Revenue (C): Closing = Opening + Credits - Debits
                    BigDecimal closingBalance;
                    if ("D".equals(normalBalance)) {
                        // Debit normal balance accounts (Assets, Expenses)
                        closingBalance = openingBalance.add(periodDebits).subtract(periodCredits);
                    } else {
                        // Credit normal balance accounts (Liabilities, Equity, Revenue)
                        closingBalance = openingBalance.add(periodCredits).subtract(periodDebits);
                    }

                    // Only include accounts with activity
                    if (openingBalance.compareTo(BigDecimal.ZERO) != 0 ||
                        periodDebits.compareTo(BigDecimal.ZERO) != 0 ||
                        periodCredits.compareTo(BigDecimal.ZERO) != 0) {

                        entries.add(new TrialBalanceEntry(
                            accountCode, accountName, normalBalance, openingBalance, periodDebits, periodCredits, closingBalance));
                    }
                }
            }
        }

        return entries;
    }

    private BigDecimal getAccountOpeningBalance(int companyId, int fiscalPeriodId, String accountCode) throws SQLException {
        // Get the current fiscal period to find the previous one
        FiscalPeriod currentPeriod = getFiscalPeriod(fiscalPeriodId);

        // First, try to find opening balance from previous fiscal period for this account
        String sql = """
            SELECT id FROM fiscal_periods
            WHERE company_id = ? AND end_date < ?
            ORDER BY end_date DESC LIMIT 1
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setDate(2, java.sql.Date.valueOf(currentPeriod.getStartDate()));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int previousPeriodId = rs.getInt("id");

                    // Calculate the closing balance of the previous period for this account
                    // FIXED: Read from journal_entry_lines for proper double-entry accounting
                    String balanceSql = """
                        SELECT
                            COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0) as closing_balance
                        FROM journal_entry_lines jel
                        JOIN accounts a ON jel.account_id = a.id
                        JOIN journal_entries je ON jel.journal_entry_id = je.id
                        WHERE je.company_id = ? 
                          AND je.fiscal_period_id = ? 
                          AND a.account_code = ?
                        """;

                    try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                        balanceStmt.setInt(1, companyId);
                        balanceStmt.setInt(2, previousPeriodId);
                        balanceStmt.setString(PREPARED_STATEMENT_PARAM_3, accountCode);

                        try (ResultSet balanceRs = balanceStmt.executeQuery()) {
                            if (balanceRs.next()) {
                                BigDecimal balance = balanceRs.getBigDecimal("closing_balance");
                                if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
                                    return balance;
                                }
                            }
                        }
                    }
                }
            }
        }

        // If no previous period found or balance is zero, look for opening balance journal entries for this account
        String openingSql = """
            SELECT
                COALESCE(SUM(jel.credit_amount), 0) - COALESCE(SUM(jel.debit_amount), 0) as opening_balance
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
                AND je.entry_date = ? AND LOWER(je.description) LIKE '%opening%balance%'
                AND a.account_code = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(openingSql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setDate(PREPARED_STATEMENT_PARAM_3, java.sql.Date.valueOf(currentPeriod.getStartDate()));
            stmt.setString(PREPARED_STATEMENT_PARAM_4, accountCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal balance = rs.getBigDecimal("opening_balance");
                    if (balance != null) {
                        return balance;
                    }
                }
            }
        }

        // If still no opening balance found, return zero
        return BigDecimal.ZERO;
    }

    private BigDecimal[] getAccountPeriodMovements(int companyId, int fiscalPeriodId, String accountCode) throws SQLException {
        // FIXED: Read from journal_entry_lines for proper double-entry accounting
        // EXCLUDE opening balance entries to avoid double-counting
        String sql = """
            SELECT
                COALESCE(SUM(jel.debit_amount), 0) as total_debits,
                COALESCE(SUM(jel.credit_amount), 0) as total_credits
            FROM journal_entry_lines jel
            JOIN accounts a ON jel.account_id = a.id
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            WHERE je.company_id = ? 
              AND je.fiscal_period_id = ? 
              AND a.account_code = ?
              AND NOT (LOWER(je.description) LIKE '%opening%balance%' OR je.reference LIKE 'OB-%')
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setString(PREPARED_STATEMENT_PARAM_3, accountCode);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal debits = rs.getBigDecimal("total_debits");
                    BigDecimal credits = rs.getBigDecimal("total_credits");
                    return new BigDecimal[]{debits, credits};
                }
            }
        }

        return new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }

    @Override
    public List<AccountInfo> getActiveAccountsFromJournals(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT DISTINCT
                a.account_code,
                a.account_name,
                at.normal_balance,
                at.name as account_type
            FROM journal_entry_lines jel
            JOIN accounts a ON jel.account_id = a.id
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            WHERE je.company_id = ?
              AND je.fiscal_period_id = ?
            ORDER BY a.account_code
            """;

        List<AccountInfo> accounts = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                AccountInfo account = new AccountInfo();
                account.setAccountCode(rs.getString("account_code"));
                account.setAccountName(rs.getString("account_name"));
                account.setNormalBalance(rs.getString("normal_balance"));
                account.setAccountType(rs.getString("account_type"));
                accounts.add(account);
            }
        }
        
        return accounts;
    }

    @Override
    public BigDecimal getAccountOpeningBalanceForLedger(int companyId, int fiscalPeriodId, String accountCode) throws SQLException {
        String sql = """
            SELECT 
                a.account_code,
                at.normal_balance,
                COALESCE(SUM(jel.debit_amount), 0) as total_debits,
                COALESCE(SUM(jel.credit_amount), 0) as total_credits,
                CASE 
                    WHEN at.normal_balance = 'D' THEN COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0)
                    WHEN at.normal_balance = 'C' THEN COALESCE(SUM(jel.credit_amount), 0) - COALESCE(SUM(jel.debit_amount), 0)
                    ELSE 0
                END as opening_balance
            FROM journal_entry_lines jel
            JOIN accounts a ON jel.account_id = a.id
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            WHERE je.company_id = ?
              AND je.fiscal_period_id = ?
              AND a.account_code = ?
              AND (LOWER(je.description) LIKE '%opening%balance%' OR je.reference LIKE 'OB-%')
            GROUP BY a.account_code, at.normal_balance
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setString(PREPARED_STATEMENT_PARAM_3, accountCode);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("opening_balance");
            }
            return BigDecimal.ZERO;
        }
    }

    @Override
    public List<JournalEntryLineDetail> getJournalEntryLinesForAccount(int companyId, int fiscalPeriodId, String accountCode) throws SQLException {
        String sql = """
            SELECT 
                jel.id as line_id,
                je.id as journal_entry_id,
                je.entry_date,
                je.reference,
                je.description,
                a.id as account_id,
                a.account_code,
                a.account_name,
                jel.debit_amount,
                jel.credit_amount
            FROM journal_entry_lines jel
            JOIN accounts a ON jel.account_id = a.id
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            WHERE je.company_id = ?
              AND je.fiscal_period_id = ?
              AND a.account_code = ?
              AND NOT (LOWER(je.description) LIKE '%opening%balance%' OR je.reference LIKE 'OB-%')
            ORDER BY je.entry_date, je.id, jel.id
            """;

        List<JournalEntryLineDetail> lines = new ArrayList<>();
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setString(PREPARED_STATEMENT_PARAM_3, accountCode);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                JournalEntryLineDetail line = new JournalEntryLineDetail();
                line.setLineId(rs.getLong("line_id"));
                line.setJournalEntryId(rs.getLong("journal_entry_id"));
                line.setEntryDate(rs.getDate("entry_date").toLocalDate());
                line.setReference(rs.getString("reference"));
                line.setDescription(rs.getString("description"));
                line.setAccountId(rs.getLong("account_id"));
                line.setAccountCode(rs.getString("account_code"));
                line.setAccountName(rs.getString("account_name"));
                line.setDebitAmount(rs.getBigDecimal("debit_amount"));
                line.setCreditAmount(rs.getBigDecimal("credit_amount"));
                lines.add(line);
            }
        }
        
        return lines;
    }

    private BankTransaction mapToBankTransaction(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setBalance(rs.getBigDecimal("balance"));
        transaction.setAccountCode(rs.getString("account_code"));
        transaction.setAccountName(rs.getString("account_name"));
        return transaction;
    }
}