package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating cash flow statements.
 * Shows cash inflows and outflows from operating, investing, and financing activities.
 */
public class CashFlowService {

    private final FinancialDataRepository repository;

    public CashFlowService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate cash flow statement report content
     * Note: This is a simplified version. A full cash flow statement would require
     * comparative period data and more detailed classification of cash flows.
     */
    public String generateCashFlow(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);

        // Get balances for the period
        Map<String, BigDecimal> balances = repository.getAccountBalancesByType(companyId, fiscalPeriodId, null);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("CASH FLOW STATEMENT", company, fiscalPeriod));
        report.append("\n");

        // Operating Activities (simplified - net income minus non-cash items)
        BigDecimal netIncome = balances.getOrDefault("REVENUE", BigDecimal.ZERO)
                .subtract(balances.getOrDefault("EXPENSES", BigDecimal.ZERO));

        report.append("CASH FLOWS FROM OPERATING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Income", formatCurrency(netIncome)));
        report.append(String.format("%-45s %-15s%n", "Adjustments for non-cash items", formatCurrency(BigDecimal.ZERO)));
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash from Operating Activities", formatCurrency(netIncome)));
        report.append("\n");

        // Investing Activities (simplified)
        report.append("CASH FLOWS FROM INVESTING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash from Investing Activities", formatCurrency(BigDecimal.ZERO)));
        report.append("\n");

        // Financing Activities (simplified)
        report.append("CASH FLOWS FROM FINANCING ACTIVITIES\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Cash from Financing Activities", formatCurrency(BigDecimal.ZERO)));
        report.append("\n");

        // Net Change in Cash
        BigDecimal netChangeInCash = netIncome; // Simplified
        report.append("NET CHANGE IN CASH\n");
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Net Change in Cash", formatCurrency(netChangeInCash)));
        report.append(String.format("%-45s %-15s%n", "Cash at Beginning of Period", formatCurrency(BigDecimal.ZERO)));
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %-15s%n", "Cash at End of Period", formatCurrency(netChangeInCash)));
        report.append("\n");

        report.append("NOTE: This is a simplified cash flow statement. A complete cash flow statement\n");
        report.append("requires comparative period data and detailed classification of cash flows.\n");

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