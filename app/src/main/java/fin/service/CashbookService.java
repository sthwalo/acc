package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating cashbook reports.
 * Provides debit and credit transaction details from bank transactions.
 */
public class CashbookService {

    private final FinancialDataRepository repository;

    public CashbookService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate cashbook report content
     */
    public String generateCashbook(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        List<BankTransaction> transactions = repository.getBankTransactions(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("CASHBOOK REPORT", company, fiscalPeriod));
        report.append("\n");

                // Add opening balance row (Balance Brought Forward)
        BigDecimal openingBalance = repository.getOpeningBalance(companyId, fiscalPeriodId);
        
        // Column headers
        report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                "Date", "Reference", "Description", "Debit (ZAR)", "Credit (ZAR)", "Balance (ZAR)"));
        report.append("=".repeat(130)).append("\n");

        // Opening balance row
        report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "BF",
                "BALANCE BROUGHT FORWARD",
                "-",
                "-",
                formatCurrency(openingBalance)));
        report.append("-".repeat(130)).append("\n");

        BigDecimal runningBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (BankTransaction transaction : transactions) {
            String date = transaction.getTransactionDate().format(dateFormatter);
            String reference = transaction.getId().toString();
            String description = transaction.getDetails() != null ? transaction.getDetails() : "";
            if (description.length() > 48) {
                description = description.substring(0, 45) + "...";
            }

            BigDecimal debit = transaction.getDebitAmount() != null ? transaction.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = transaction.getCreditAmount() != null ? transaction.getCreditAmount() : BigDecimal.ZERO;

            // Update running balance
            runningBalance = runningBalance.add(credit).subtract(debit);
            
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);

            report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                    date,
                    reference,
                    description,
                    formatCurrency(debit),
                    formatCurrency(credit),
                    formatCurrency(runningBalance)));
        }

        // Totals
        report.append("=".repeat(130)).append("\n");
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);
        report.append(String.format("%-77s %-15s %-15s %-15s%n",
                "TOTALS:",
                formatCurrency(totalDebits),
                formatCurrency(totalCredits),
                formatCurrency(closingBalance)));

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, 110)).append("\n");
        header.append(centerText("Company: " + company.getName(), 110)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), 110)).append("\n");
        header.append(centerText("Period: " + fiscalPeriod.getPeriodName() + " (" +
                fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")", 110)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), 110)).append("\n");
        return header.toString();
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return String.format("R %10.2f", amount);
    }
}