package fin.service;

import fin.config.DatabaseConfig;
import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating cash flow statements.
 * Shows cash inflows and outflows from operating, investing, and financing activities.
 * Uses the indirect method: starts with net income, then adjusts for non-cash items.
 */
public class CashFlowService {

    private final FinancialDataRepository repository;
    private final IncomeStatementService incomeStatementService;

    public CashFlowService(FinancialDataRepository repository, IncomeStatementService incomeStatementService) {
        this.repository = repository;
        this.incomeStatementService = incomeStatementService;
    }

    /**
     * Data class to hold cash flow calculations
     */
    private static class CashFlowData {
        BigDecimal operatingCashFlow = BigDecimal.ZERO;
        BigDecimal investingCashFlow = BigDecimal.ZERO;
        BigDecimal financingCashFlow = BigDecimal.ZERO;
        BigDecimal assetPurchases = BigDecimal.ZERO;
        BigDecimal investments = BigDecimal.ZERO;
        BigDecimal loanProceeds = BigDecimal.ZERO;
        BigDecimal loanPayments = BigDecimal.ZERO;
    }

    /**
     * Get opening cash balance from opening balance journal entry
     */
    private BigDecimal getOpeningCashBalance(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT jel.debit_amount - COALESCE(jel.credit_amount, 0) as opening_balance
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? 
            AND a.account_code = '1100'
            AND je.description LIKE '%Opening Balance%'
            """;

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDatabaseUrl());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("opening_balance");
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate net income from revenue and expenses (same logic as IncomeStatementService)
     */
    private BigDecimal calculateNetIncome(int companyId, int fiscalPeriodId) throws SQLException {
        // Get account balances from General Ledger (same source as IncomeStatementService)
        GeneralLedgerService glService = new GeneralLedgerService(repository);
        var accountBalances = glService.getAccountClosingBalances(companyId, fiscalPeriodId);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (var entry : accountBalances.entrySet()) {
            var balance = entry.getValue();
            String accountCode = balance.getAccountCode();
            
            // Revenue accounts: 6000-7999
            if (accountCode.startsWith("6") || accountCode.startsWith("7")) {
                if (balance.getClosingBalance().compareTo(BigDecimal.ZERO) != 0) {
                    totalRevenue = totalRevenue.add(balance.getClosingBalance().abs());
                }
            }
            // Expense accounts: 8000-9999
            else if (accountCode.startsWith("8") || accountCode.startsWith("9")) {
                if (balance.getClosingBalance().compareTo(BigDecimal.ZERO) != 0) {
                    totalExpenses = totalExpenses.add(balance.getClosingBalance().abs());
                }
            }
        }
        
        return totalRevenue.subtract(totalExpenses);
    }

    /**
     * Get actual ending cash balance from bank account
     */
    private BigDecimal getActualCashBalance(int companyId, int fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT SUM(COALESCE(jel.debit_amount, 0) - COALESCE(jel.credit_amount, 0)) as balance
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ?
            AND a.account_code = '1100'
            AND (je.fiscal_period_id = ? OR je.fiscal_period_id IS NULL)
            """;

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDatabaseUrl());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("balance");
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * Calculate cash flows by category using the INDIRECT METHOD
     * 
     * INDIRECT METHOD APPROACH:
     * 1. Start with Net Income from income statement
     * 2. Add back non-cash expenses (none in this simple system)
     * 3. Adjust for changes in working capital (not tracked in this system)
     * 4. Therefore: Operating Cash Flow = Net Income
     * 5. Investing/Financing: Show actual cash movements from journal entries
     */
    private CashFlowData calculateCashFlows(int companyId, int fiscalPeriodId) throws SQLException {
        CashFlowData data = new CashFlowData();
        
        // For indirect method, operating cash flow starts with net income
        // Since we don't have non-cash expenses or working capital changes,
        // operating cash flow = net income (calculated elsewhere)
        
        // Calculate actual cash movements for investing and financing activities
        String sql = """
            SELECT 
                a.account_code,
                SUM(COALESCE(jel.debit_amount, 0)) as total_debits,
                SUM(COALESCE(jel.credit_amount, 0)) as total_credits
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            AND a.account_code != '1100'  -- Exclude bank account itself
            GROUP BY a.account_code
            HAVING SUM(COALESCE(jel.debit_amount, 0) - COALESCE(jel.credit_amount, 0)) != 0
            """;

        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getDatabaseUrl());
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, companyId);
            stmt.setInt(2, fiscalPeriodId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                String accountCode = rs.getString("account_code");
                BigDecimal totalDebits = rs.getBigDecimal("total_debits");
                BigDecimal totalCredits = rs.getBigDecimal("total_credits");
                BigDecimal netMovement = totalDebits.subtract(totalCredits);
                
                // Categorize by account code ranges for investing and financing activities
                if (accountCode.startsWith("2")) {
                    // Asset accounts (investing activities) - purchases are cash outflows
                    if (netMovement.compareTo(BigDecimal.ZERO) > 0) {
                        // Net debit to asset account = cash outflow for purchase
                        if (accountCode.equals("2000") || accountCode.startsWith("2000-")) {
                            data.assetPurchases = data.assetPurchases.add(netMovement);
                        } else if (accountCode.equals("2200") || accountCode.startsWith("2200-")) {
                            data.investments = data.investments.add(netMovement);
                        }
                    }
                } else if (accountCode.equals("4000") || accountCode.startsWith("4000-")) {
                    // Long-term loans (financing activity)
                    if (netMovement.compareTo(BigDecimal.ZERO) > 0) {
                        // Net debit to loan account = loan repayment (cash outflow)
                        data.loanPayments = data.loanPayments.add(netMovement);
                    } else {
                        // Net credit to loan account = loan proceeds (cash inflow)
                        data.loanProceeds = data.loanProceeds.add(netMovement.negate());
                    }
                }
            }
        }
        
        // For indirect method: Operating cash flow = Net Income (no adjustments needed)
        // Investing cash flows: Asset purchases are outflows
        data.investingCashFlow = data.assetPurchases.add(data.investments).negate();
        
        // Financing cash flows: Loan proceeds - loan payments
        data.financingCashFlow = data.loanProceeds.subtract(data.loanPayments);
        
        return data;
    }

    /**
     * Generate cash flow statement report content with ACTUAL cash movements
     */
    public String generateCashFlow(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);

        // Get opening cash balance (from opening balance journal entry)
        BigDecimal openingCashBalance = getOpeningCashBalance(companyId, fiscalPeriodId);
        
        // Calculate cash flows by category
        CashFlowData cashFlowData = calculateCashFlows(companyId, fiscalPeriodId);
        
        // Get actual ending cash balance
        BigDecimal actualEndingBalance = getActualCashBalance(companyId, fiscalPeriodId);

        // Calculate correct net income from income statement
        BigDecimal netIncome = calculateNetIncome(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("CASH FLOW STATEMENT", company, fiscalPeriod));
        report.append("\n");

        // Operating Activities - Start with net income, then adjust for non-cash items
        report.append("CASH FLOWS FROM OPERATING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Income/(Loss)", formatCurrency(netIncome)));
        report.append(String.format("%-45s %-15s%n", "Adjustments for non-cash items", formatCurrency(BigDecimal.ZERO)));
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash from Operating Activities", formatCurrency(netIncome)));
        report.append("\n");

        // Investing Activities
        report.append("CASH FLOWS FROM INVESTING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        if (cashFlowData.assetPurchases.compareTo(BigDecimal.ZERO) != 0) {
            report.append(String.format("%-45s %-15s%n", "Purchase of Property, Plant & Equipment", formatCurrency(cashFlowData.assetPurchases.negate())));
        }
        if (cashFlowData.investments.compareTo(BigDecimal.ZERO) != 0) {
            report.append(String.format("%-45s %-15s%n", "Purchase of Investments", formatCurrency(cashFlowData.investments.negate())));
        }
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash used in Investing Activities", formatCurrency(cashFlowData.investingCashFlow)));
        report.append("\n");

        // Financing Activities
        report.append("CASH FLOWS FROM FINANCING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        if (cashFlowData.loanProceeds.compareTo(BigDecimal.ZERO) != 0) {
            report.append(String.format("%-45s %-15s%n", "Proceeds from Long-term Loans", formatCurrency(cashFlowData.loanProceeds)));
        }
        if (cashFlowData.loanPayments.compareTo(BigDecimal.ZERO) != 0) {
            report.append(String.format("%-45s %-15s%n", "Repayment of Long-term Loans", formatCurrency(cashFlowData.loanPayments.negate())));
        }
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash from Financing Activities", formatCurrency(cashFlowData.financingCashFlow)));
        report.append("\n");

        // Net Change in Cash
        BigDecimal netCashChange = cashFlowData.operatingCashFlow
            .add(cashFlowData.investingCashFlow)
            .add(cashFlowData.financingCashFlow);
            
        report.append("NET CHANGE IN CASH\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Change in Cash", formatCurrency(netCashChange)));
        report.append(String.format("%-45s %-15s%n", "Cash at Beginning of Period", formatCurrency(openingCashBalance)));
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal calculatedEndingBalance = openingCashBalance.add(netCashChange);
        report.append(String.format("%-45s %-15s%n", "Cash at End of Period (calculated)", formatCurrency(calculatedEndingBalance)));
        report.append(String.format("%-45s %-15s%n", "Cash at End of Period (actual)", formatCurrency(actualEndingBalance)));
        
        // Reconciliation check
        BigDecimal difference = actualEndingBalance.subtract(calculatedEndingBalance);
        if (difference.abs().compareTo(new BigDecimal("0.01")) > 0) {
            report.append(String.format("%-45s %-15s%n", "⚠️ Reconciliation Difference", formatCurrency(difference)));
        } else {
            report.append("✅ Cash Flow Reconciled\n");
        }
        report.append("\n");

        report.append("NOTE: This cash flow statement uses the INDIRECT METHOD.\n");
        report.append("It starts with net income from the income statement and adjusts for non-cash items.\n");

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, 65)).append("\n");
        header.append(centerText("Company: " + company.getName(), 65)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), 65)).append("\n");
        header.append(centerText("Period: " + fiscalPeriod.getPeriodName() + " (" +
                fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")", 65)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 65)).append("\n");
        return header.toString();
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "R      0.00";
        }
        return String.format("R %10.2f", amount);
    }
}