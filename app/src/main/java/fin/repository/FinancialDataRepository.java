package fin.repository;

import fin.model.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * Repository interface for financial data access.
 * Centralizes all database operations for financial reports to ensure data integrity.
 */
public interface FinancialDataRepository {

    /**
     * Get all bank transactions for a company and fiscal period
     */
    List<BankTransaction> getBankTransactions(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get account balances by type for balance sheet and income statement
     */
    Map<String, BigDecimal> getAccountBalancesByType(int companyId, int fiscalPeriodId, String accountType) throws SQLException;

    /**
     * Get trial balance data with opening balances, period movements, and closing balances
     */
    List<TrialBalanceEntry> getTrialBalanceEntries(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get journal entries for audit trail
     */
    List<JournalEntry> getJournalEntries(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get company information
     */
    Company getCompany(int companyId) throws SQLException;

    /**
     * Get fiscal period information
     */
    FiscalPeriod getFiscalPeriod(int fiscalPeriodId) throws SQLException;

    /**
     * Calculate opening balance (Balance Brought Forward) from previous fiscal period
     */
    BigDecimal getOpeningBalance(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get all accounts that have journal entry activity in a fiscal period.
     * This is the PRIMARY method for General Ledger - reads directly from journal entries.
     * Returns account metadata including code, name, and normal balance type.
     */
    List<AccountInfo> getActiveAccountsFromJournals(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get opening balance for a specific account in the general ledger.
     * For balance sheet accounts (assets, liabilities, equity), this is the previous period's closing balance.
     * For income statement accounts (revenue, expenses), this is always 0.
     * For the first fiscal period, checks for opening balance journal entries (OB-* reference).
     */
    BigDecimal getAccountOpeningBalanceForLedger(int companyId, int fiscalPeriodId, String accountCode) throws SQLException;

    /**
     * Get all journal entry lines for a specific account with entry details.
     * Returns lines in chronological order with parent journal entry information.
     * Excludes opening balance entries (reference pattern 'OB-%') to avoid duplication.
     */
    List<JournalEntryLineDetail> getJournalEntryLinesForAccount(int companyId, int fiscalPeriodId, String accountCode) throws SQLException;
}