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

    // Display formatting constants
    private static final int REPORT_WIDTH = 130;
    private static final int DESCRIPTION_MAX_LENGTH = 48;
    private static final int DESCRIPTION_TRUNCATE_LENGTH = 45;
    private static final int TOTALS_LABEL_WIDTH = 77;
    private static final int HEADER_CENTER_WIDTH = 110;

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
        report.append("=".repeat(REPORT_WIDTH)).append("\n");

        // Opening balance row
        report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                "BF",
                "BALANCE BROUGHT FORWARD",
                "-",
                "-",
                formatCurrency(openingBalance)));
        report.append("-".repeat(REPORT_WIDTH)).append("\n");

        BigDecimal runningBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (BankTransaction transaction : transactions) {
            String date = transaction.getTransactionDate().format(dateFormatter);
            String reference = transaction.getId().toString();
            String description = transaction.getDetails() != null ? transaction.getDetails() : "";
            if (description.length() > DESCRIPTION_MAX_LENGTH) {
                description = description.substring(0, DESCRIPTION_TRUNCATE_LENGTH) + "...";
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
        report.append("=".repeat(REPORT_WIDTH)).append("\n");
        BigDecimal closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);
        report.append(String.format("%-" + TOTALS_LABEL_WIDTH + "s %-15s %-15s %-15s%n",
                "TOTALS:",
                formatCurrency(totalDebits),
                formatCurrency(totalCredits),
                formatCurrency(closingBalance)));

        return report.toString();
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, HEADER_CENTER_WIDTH)).append("\n");
        header.append(centerText("Company: " + company.getName(), HEADER_CENTER_WIDTH)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), HEADER_CENTER_WIDTH)).append("\n");
        header.append(centerText("Period: " + fiscalPeriod.getPeriodName() + " (" +
                fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")", HEADER_CENTER_WIDTH)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), HEADER_CENTER_WIDTH)).append("\n");
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