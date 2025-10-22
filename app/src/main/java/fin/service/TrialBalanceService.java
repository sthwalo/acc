package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating trial balance reports.
 * Shows account balances with debit and credit totals.
 */
public class TrialBalanceService {

    // Display formatting constants
    private static final int REPORT_LINE_WIDTH = 105;
    private static final int ACCOUNT_NAME_MAX_LENGTH = 48;
    private static final int HEADER_CENTER_WIDTH = 130;
    private static final int TRUNCATE_SUFFIX_LENGTH = 3;

    private final FinancialDataRepository repository;
    private final GeneralLedgerService generalLedgerService;

    /**
     * Constructor with dependency injection.
     *
     * NOTE: EI_EXPOSE_REP warning is intentionally suppressed for this constructor.
     * This is an architectural design decision for Dependency Injection pattern:
     * - Services are injected as constructor parameters to enable loose coupling
     * - Allows for better testability through mock injection
     * - Enables proper accounting hierarchy (GL → Trial Balance → Financial Statements)
     * - Maintains separation between data access and reporting logic
     * - Suppressions are configured in config/spotbugs/exclude.xml for all service constructors
     *
     * @param initialRepository the financial data repository for database operations
     * @param initialGeneralLedgerService the general ledger service for account balance calculations
     */
    public TrialBalanceService(FinancialDataRepository initialRepository, GeneralLedgerService initialGeneralLedgerService) {
        this.repository = initialRepository;
        this.generalLedgerService = initialGeneralLedgerService;
    }

    /**
     * Generate trial balance report content
     * 
     * CORRECT ACCOUNTING FLOW:
     * This method reads closing balances FROM General Ledger Service.
     * Implements: Journal Entries → General Ledger → Trial Balance → Financial Statements
     */
    public String generateTrialBalance(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        
        // ✅ CORRECT FLOW: Get closing balances FROM General Ledger (not directly from journal entries)
        Map<String, AccountBalance> accountBalances = generalLedgerService.getAccountClosingBalances(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("TRIAL BALANCE REPORT", company, fiscalPeriod));
        report.append("\n");
        report.append("Source: General Ledger Account Balances (Double-Entry Bookkeeping)\n");
        report.append("Hierarchy: Journal Entries → General Ledger → Trial Balance → Financial Statements\n");
        report.append("\n");

                // Column headers for traditional trial balance
        report.append(String.format("%-15s %-50s %-20s %-20s%n",
                "Account Code", "Account Name", "Debit (ZAR)", "Credit (ZAR)"));
        report.append("=".repeat(REPORT_LINE_WIDTH)).append("\n");

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        // Process each account balance from General Ledger
        for (AccountBalance balance : accountBalances.values()) {
            // Use the AccountBalance methods to get proper TB debit/credit amounts
            BigDecimal debitAmount = balance.getTrialBalanceDebit();
            BigDecimal creditAmount = balance.getTrialBalanceCredit();

            // Add to totals
            totalDebits = totalDebits.add(debitAmount);
            totalCredits = totalCredits.add(creditAmount);

            // Only show accounts with balances
            if (debitAmount.compareTo(BigDecimal.ZERO) != 0 || creditAmount.compareTo(BigDecimal.ZERO) != 0) {
                report.append(String.format("%-15s %-50s %-20s %-20s%n",
                        balance.getAccountCode(),
                        truncateString(balance.getAccountName(), ACCOUNT_NAME_MAX_LENGTH),
                        formatCurrency(debitAmount),
                        formatCurrency(creditAmount)));
            }
        }

        // Totals
        report.append("=".repeat(REPORT_LINE_WIDTH)).append("\n");
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

    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - TRUNCATE_SUFFIX_LENGTH) + "...";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return String.format("R %10.2f", amount);
    }
}