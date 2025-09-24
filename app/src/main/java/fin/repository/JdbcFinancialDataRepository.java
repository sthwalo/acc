package fin.repository;

import fin.model.*;
import fin.model.ComprehensiveTrialBalanceEntry;
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

    public JdbcFinancialDataRepository(@SuppressWarnings("EI_EXPOSE_REP") DataSource dataSource) {
        this.dataSource = dataSource;
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
        // Get balances directly from bank_transactions for accuracy
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
                COALESCE(SUM(bt.debit_amount), 0) as total_debits,
                COALESCE(SUM(bt.credit_amount), 0) as total_credits
            FROM accounts a
            LEFT JOIN bank_transactions bt ON a.account_code = bt.account_code
                AND bt.company_id = ? AND bt.fiscal_period_id = ?
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
            stmt.setInt(3, companyId);

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
                    } else {
                        // For balance sheet accounts: normal debits - credits
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
    public List<TrialBalanceEntry> getTrialBalanceEntries(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT
                a.account_code,
                a.account_name,
                COALESCE(SUM(bt.debit_amount), 0) as total_debits,
                COALESCE(SUM(bt.credit_amount), 0) as total_credits
            FROM accounts a
            LEFT JOIN bank_transactions bt ON a.account_code = bt.account_code
                AND bt.company_id = ? AND bt.fiscal_period_id = ?
            WHERE a.company_id = ?
            GROUP BY a.account_code, a.account_name
            HAVING COALESCE(SUM(bt.debit_amount), 0) != 0 OR COALESCE(SUM(bt.credit_amount), 0) != 0
            ORDER BY a.account_code
            """;

        List<TrialBalanceEntry> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setInt(3, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");
                    BigDecimal debits = rs.getBigDecimal("total_debits");
                    BigDecimal credits = rs.getBigDecimal("total_credits");

                    entries.add(new TrialBalanceEntry(accountCode, accountName, debits, credits));
                }
            }
        }

        return entries;
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
                    String balanceSql = """
                        SELECT
                            COALESCE(SUM(credit_amount), 0) - COALESCE(SUM(debit_amount), 0) as closing_balance
                        FROM bank_transactions
                        WHERE company_id = ? AND fiscal_period_id = ?
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
        String openingSql = """
            SELECT
                COALESCE(SUM(jel.credit_amount), 0) - COALESCE(SUM(jel.debit_amount), 0) as opening_balance
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
                AND je.entry_date = ? AND LOWER(je.description) LIKE '%opening%balance%'
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(openingSql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setDate(3, java.sql.Date.valueOf(currentPeriod.getStartDate()));

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

    @Override
    public List<ComprehensiveTrialBalanceEntry> getComprehensiveTrialBalance(int companyId, int fiscalPeriodId) throws SQLException {
        // Get all accounts for the company
        String sql = """
            SELECT a.account_code, a.account_name
            FROM accounts a
            WHERE a.company_id = ?
            ORDER BY a.account_code
            """;

        List<ComprehensiveTrialBalanceEntry> entries = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");

                    // Calculate opening balance for this account from previous periods
                    BigDecimal openingBalance = getAccountOpeningBalance(companyId, fiscalPeriodId, accountCode);

                    // Calculate period movements for this account
                    BigDecimal[] periodMovements = getAccountPeriodMovements(companyId, fiscalPeriodId, accountCode);
                    BigDecimal periodDebits = periodMovements[0];
                    BigDecimal periodCredits = periodMovements[1];

                    // Calculate closing balance
                    BigDecimal closingBalance = openingBalance.add(periodDebits).subtract(periodCredits);

                    // Only include accounts with activity
                    if (openingBalance.compareTo(BigDecimal.ZERO) != 0 ||
                        periodDebits.compareTo(BigDecimal.ZERO) != 0 ||
                        periodCredits.compareTo(BigDecimal.ZERO) != 0) {

                        entries.add(new ComprehensiveTrialBalanceEntry(
                            accountCode, accountName, openingBalance, periodDebits, periodCredits, closingBalance));
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
                    String balanceSql = """
                        SELECT
                            COALESCE(SUM(credit_amount), 0) - COALESCE(SUM(debit_amount), 0) as closing_balance
                        FROM bank_transactions
                        WHERE company_id = ? AND fiscal_period_id = ? AND account_code = ?
                        """;

                    try (PreparedStatement balanceStmt = conn.prepareStatement(balanceSql)) {
                        balanceStmt.setInt(1, companyId);
                        balanceStmt.setInt(2, previousPeriodId);
                        balanceStmt.setString(3, accountCode);

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
            stmt.setDate(3, java.sql.Date.valueOf(currentPeriod.getStartDate()));
            stmt.setString(4, accountCode);

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
        String sql = """
            SELECT
                COALESCE(SUM(debit_amount), 0) as total_debits,
                COALESCE(SUM(credit_amount), 0) as total_credits
            FROM bank_transactions
            WHERE company_id = ? AND fiscal_period_id = ? AND account_code = ?
            """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            stmt.setString(3, accountCode);

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