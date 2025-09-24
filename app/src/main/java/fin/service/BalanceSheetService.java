package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating balance sheet reports.
 * Shows assets, liabilities, and equity positions.
 */
public class BalanceSheetService {

    private final FinancialDataRepository repository;

    public BalanceSheetService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate balance sheet report content
     */
    public String generateBalanceSheet(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);

        Map<String, BigDecimal> balances = repository.getAccountBalancesByType(companyId, fiscalPeriodId, null);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("BALANCE SHEET", company, fiscalPeriod));
        report.append("\n");

        BigDecimal totalAssets = balances.getOrDefault("ASSETS", BigDecimal.ZERO);
        BigDecimal totalLiabilities = balances.getOrDefault("LIABILITIES", BigDecimal.ZERO);
        BigDecimal totalEquity = balances.getOrDefault("EQUITY", BigDecimal.ZERO);

        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        // Assets section
        report.append("ASSETS\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Assets", formatCurrency(totalAssets)));
        report.append("\n");

        // Liabilities section
        report.append("LIABILITIES\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Liabilities", formatCurrency(totalLiabilities)));
        report.append("\n");

        // Equity section
        report.append("EQUITY\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Equity", formatCurrency(totalEquity)));
        report.append("\n");

        // Total Liabilities and Equity
        report.append("TOTAL LIABILITIES AND EQUITY\n");
        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %-15s%n", "Total Liabilities & Equity", formatCurrency(totalLiabilitiesAndEquity)));
        report.append("\n");

        // Balance check
        BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            report.append("BALANCE CHECK\n");
            report.append("-".repeat(50)).append("\n");
            report.append(String.format("%-40s %-15s%n", "Out of Balance", formatCurrency(difference)));
        }

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, 60)).append("\n");
        header.append(centerText("Company: " + company.getName(), 60)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), 60)).append("\n");
        header.append(centerText("As at: " + fiscalPeriod.getEndDate(), 60)).append("\n");
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