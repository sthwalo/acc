package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating income statement reports.
 * Shows revenue and expenses for the period.
 */
public class IncomeStatementService {

    private final FinancialDataRepository repository;

    public IncomeStatementService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate income statement report content
     */
    public String generateIncomeStatement(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);

        Map<String, BigDecimal> balances = repository.getAccountBalancesByType(companyId, fiscalPeriodId, null);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("INCOME STATEMENT", company, fiscalPeriod));
        report.append("\n");

        BigDecimal totalRevenue = balances.getOrDefault("REVENUE", BigDecimal.ZERO);
        BigDecimal totalExpenses = balances.getOrDefault("EXPENSES", BigDecimal.ZERO);
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);

        // Revenue section
        report.append("REVENUE\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Revenue", formatCurrency(totalRevenue)));
        report.append("\n");

        // Expenses section
        report.append("EXPENSES\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Expenses", formatCurrency(totalExpenses)));
        report.append("\n");

        // Net Income
        report.append("NET INCOME\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Net Income/(Loss)", formatCurrency(netIncome)));

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, 60)).append("\n");
        header.append(centerText("Company: " + company.getName(), 60)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), 60)).append("\n");
        header.append(centerText("Period: " + fiscalPeriod.getPeriodName() + " (" +
                fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")", 60)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 60)).append("\n");
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