package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating trial balance reports.
 * Shows account balances with debit and credit totals.
 */
public class TrialBalanceService {

    private final FinancialDataRepository repository;

    public TrialBalanceService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate trial balance report content
     */
    public String generateTrialBalance(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        List<TrialBalanceEntry> entries = repository.getTrialBalanceEntries(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("TRIAL BALANCE REPORT", company, fiscalPeriod));
        report.append("\n");

        // Column headers for traditional trial balance
        report.append(String.format("%-15s %-50s %-20s %-20s%n",
                "Account Code", "Account Name", "Debit (ZAR)", "Credit (ZAR)"));
        report.append("=".repeat(105)).append("\n");

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (TrialBalanceEntry entry : entries) {
            BigDecimal closingBalance = entry.getClosingBalance();
            String normalBalance = entry.getNormalBalance(); // 'D' or 'C'

            // For trial balance, display based on account type normal balance
            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;

            if ("D".equals(normalBalance)) {
                // Debit normal balance accounts (Assets, Expenses)
                // Positive balance → Debit column, Negative balance → Credit column
                if (closingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    debitAmount = closingBalance;
                    totalDebits = totalDebits.add(debitAmount);
                } else if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
                    creditAmount = closingBalance.negate();
                    totalCredits = totalCredits.add(creditAmount);
                }
            } else {
                // Credit normal balance accounts (Liabilities, Equity, Revenue)
                // Positive balance → Credit column, Negative balance → Debit column
                if (closingBalance.compareTo(BigDecimal.ZERO) > 0) {
                    creditAmount = closingBalance;
                    totalCredits = totalCredits.add(creditAmount);
                } else if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
                    debitAmount = closingBalance.negate();
                    totalDebits = totalDebits.add(debitAmount);
                }
            }

            // Only show accounts with balances
            if (debitAmount.compareTo(BigDecimal.ZERO) != 0 || creditAmount.compareTo(BigDecimal.ZERO) != 0) {
                report.append(String.format("%-15s %-50s %-20s %-20s%n",
                        entry.getAccountCode(),
                        truncateString(entry.getAccountName(), 48),
                        formatCurrency(debitAmount),
                        formatCurrency(creditAmount)));
            }
        }

        // Totals
        report.append("=".repeat(105)).append("\n");
        report.append(String.format("%-65s %-20s %-20s%n",
                "TOTALS:",
                formatCurrency(totalDebits),
                formatCurrency(totalCredits)));

        // Check if trial balance balances
        BigDecimal difference = totalDebits.subtract(totalCredits);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            report.append("\n").append("*** TRIAL BALANCE DOES NOT BALANCE ***").append("\n");
            report.append(String.format("Difference: %s%n", formatCurrency(difference.abs())));
        } else {
            report.append("\n").append("*** TRIAL BALANCE BALANCES ***").append("\n");
        }

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, 130)).append("\n");
        header.append(centerText("Company: " + company.getName(), 130)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), 130)).append("\n");
        header.append(centerText("Period: " + fiscalPeriod.getPeriodName() + " (" +
                fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")", 130)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 130)).append("\n");
        return header.toString();
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return String.format("R %10.2f", amount);
    }
}