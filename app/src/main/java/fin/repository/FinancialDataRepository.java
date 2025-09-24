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
     * Get trial balance data from bank transactions
     */
    List<TrialBalanceEntry> getTrialBalanceEntries(int companyId, int fiscalPeriodId) throws SQLException;

    /**
     * Get comprehensive trial balance data with opening and closing balances
     */
    List<ComprehensiveTrialBalanceEntry> getComprehensiveTrialBalance(int companyId, int fiscalPeriodId) throws SQLException;

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
}