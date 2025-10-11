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
 * FIXED: Now calculates actual cash movements from journal entries.
 */
public class CashFlowService {

    private final FinancialDataRepository repository;

    public CashFlowService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Data class to hold cash flow calculations
     */
    private static class CashFlowData {
        BigDecimal operatingCashFlow = BigDecimal.ZERO;
        BigDecimal investingCashFlow = BigDecimal.ZERO;
        BigDecimal financingCashFlow = BigDecimal.ZERO;
        BigDecimal operatingExpenses = BigDecimal.ZERO;
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
     * Calculate cash flows by category
     */
    private CashFlowData calculateCashFlows(int companyId, int fiscalPeriodId) throws SQLException {
        CashFlowData data = new CashFlowData();
        
        String sql = """
            SELECT 
                a.account_code,
                SUM(COALESCE(jel.debit_amount, 0)) as total_debits,
                SUM(COALESCE(jel.credit_amount, 0)) as total_credits,
                SUM(COALESCE(jel.debit_amount, 0) - COALESCE(jel.credit_amount, 0)) as net_amount
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
                BigDecimal netAmount = rs.getBigDecimal("net_amount");
                
                // Categorize by account code ranges
                if (accountCode.startsWith("8") || accountCode.startsWith("9")) {
                    // Operating expenses (cash outflow)
                    data.operatingExpenses = data.operatingExpenses.add(netAmount);
                } else if (accountCode.startsWith("2")) {
                    // Asset purchases (investing outflow) 
                    if (accountCode.equals("2000")) {
                        data.assetPurchases = data.assetPurchases.add(netAmount);
                    } else if (accountCode.equals("2200")) {
                        data.investments = data.investments.add(netAmount);
                    }
                } else if (accountCode.equals("4000")) {
                    // Long-term loans (financing activity)
                    if (netAmount.compareTo(BigDecimal.ZERO) > 0) {
                        data.loanPayments = data.loanPayments.add(netAmount);
                    } else {
                        data.loanProceeds = data.loanProceeds.add(netAmount.negate());
                    }
                }
            }
        }
        
        // Calculate net cash flows by category
        data.operatingCashFlow = data.operatingExpenses.negate(); // Expenses are cash outflows
        data.investingCashFlow = data.assetPurchases.add(data.investments).negate(); // Asset purchases are outflows
        data.financingCashFlow = data.loanProceeds.subtract(data.loanPayments); // Proceeds - payments
        
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

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("CASH FLOW STATEMENT", company, fiscalPeriod));
        report.append("\n");

        // Operating Activities - Net loss plus operating expenses (actual cash outflows)
        BigDecimal netIncome = BigDecimal.ZERO.subtract(cashFlowData.operatingExpenses); // No revenue = expenses are net loss
        report.append("CASH FLOWS FROM OPERATING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Income/(Loss)", formatCurrency(netIncome)));
        report.append(String.format("%-45s %-15s%n", "Operating Expenses (cash outflows)", formatCurrency(cashFlowData.operatingExpenses.negate())));
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash used in Operating Activities", formatCurrency(cashFlowData.operatingCashFlow)));
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

        report.append("NOTE: This cash flow statement shows ACTUAL cash movements from journal entries.\n");
        report.append("Operating expenses represent actual cash outflows during the period.\n");

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