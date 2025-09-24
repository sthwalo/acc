package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for generating general ledger reports.
 * Provides detailed transaction listings by account.
 */
public class GeneralLedgerService {

    private final FinancialDataRepository repository;

    public GeneralLedgerService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate general ledger report content
     */
    public String generateGeneralLedger(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        List<BankTransaction> transactions = repository.getBankTransactions(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("GENERAL LEDGER REPORT", company, fiscalPeriod));
        report.append("\n");

        // Group transactions by account
        var transactionsByAccount = transactions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        BankTransaction::getAccountCode,
                        java.util.LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (var entry : transactionsByAccount.entrySet()) {
            String accountCode = entry.getKey();
            List<BankTransaction> accountTransactions = entry.getValue();

            if (accountTransactions.isEmpty()) continue;

            String accountName = accountTransactions.get(0).getAccountName();

            // Account header
            report.append(String.format("Account: %s - %s%n", accountCode, accountName));
            report.append("-".repeat(130)).append("\n");

            // Column headers
            report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                    "Date", "Reference", "Description", "Debit (ZAR)", "Credit (ZAR)", "Balance (ZAR)"));
            report.append("=".repeat(130)).append("\n");

            BigDecimal accountDebits = BigDecimal.ZERO;
            BigDecimal accountCredits = BigDecimal.ZERO;

            for (BankTransaction transaction : accountTransactions) {
                String date = transaction.getTransactionDate().format(dateFormatter);
                String reference = transaction.getId().toString();
                String description = transaction.getDetails() != null ? transaction.getDetails() : "";
                if (description.length() > 48) {
                    description = description.substring(0, 45) + "...";
                }

                BigDecimal debit = transaction.getDebitAmount() != null ? transaction.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal credit = transaction.getCreditAmount() != null ? transaction.getCreditAmount() : BigDecimal.ZERO;

                accountDebits = accountDebits.add(debit);
                accountCredits = accountCredits.add(credit);

                // For each transaction, show the cumulative balance up to that point
                BigDecimal transactionBalance = accountDebits.subtract(accountCredits);

                report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-15s%n",
                        date,
                        reference,
                        description,
                        formatCurrency(debit),
                        formatCurrency(credit),
                        formatCurrency(transactionBalance)));
            }

            // Account totals with net balance
            BigDecimal netBalance = accountDebits.subtract(accountCredits);
            report.append("=".repeat(130)).append("\n");
            report.append(String.format("%-77s %-15s %-15s %-15s%n",
                    "Period Total:",
                    formatCurrency(accountDebits),
                    formatCurrency(accountCredits),
                    formatCurrency(netBalance)));
            report.append("\n\n");
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

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return String.format("R %10.2f", amount);
    }
}