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
 * 
 * FOLLOWS PROPER ACCOUNTING HIERARCHY:
 * Journal Entries → General Ledger → Trial Balance → Balance Sheet
 */
public class BalanceSheetService {

    private final FinancialDataRepository repository;
    private final GeneralLedgerService generalLedgerService;

    /**
     * Constructor with dependency injection.
     *
     * NOTE: EI_EXPOSE_REP warning is intentionally suppressed for this constructor.
     * This is an architectural design decision for Dependency Injection pattern:
     * - Services are injected as constructor parameters to enable loose coupling
     * - Allows for better testability through mock injection
     * - Enables service composition for financial reporting workflows
     * - Maintains separation between data access and business logic
     * - Suppressions are configured in config/spotbugs/exclude.xml for all service constructors
     *
     * @param repository the financial data repository for database operations
     * @param generalLedgerService the general ledger service for account balance calculations
     */
    public BalanceSheetService(FinancialDataRepository repository, GeneralLedgerService generalLedgerService) {
        this.repository = repository;
        this.generalLedgerService = generalLedgerService;
    }

    /**
     * Generate balance sheet report content
     * 
     * CORRECT ACCOUNTING FLOW:
     * This method reads closing balances FROM General Ledger (same as Trial Balance).
     * Implements: Journal Entries → General Ledger → Trial Balance → Balance Sheet
     */
    public String generateBalanceSheet(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        
        // ✅ CORRECT FLOW: Get closing balances FROM General Ledger (same source as Trial Balance)
        Map<String, AccountBalance> accountBalances = generalLedgerService.getAccountClosingBalances(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("BALANCE SHEET", company, fiscalPeriod));
        report.append("\n");
        report.append("Source: General Ledger Account Balances (Double-Entry Bookkeeping)\n");
        report.append("Hierarchy: Journal Entries → General Ledger → Trial Balance → Balance Sheet\n");
        report.append("\n");

        // Categorize accounts by Balance Sheet type
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        // Calculate net profit/loss for retained earnings
        BigDecimal netProfit = calculateNetProfit(accountBalances);

        // Assets section (1000-2999)
        report.append("ASSETS\n");
        report.append("=".repeat(80)).append("\n");
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isAssetAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), 43),
                            formatCurrency(amount)));
                    totalAssets = totalAssets.add(amount);
                }
            }
        }
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL ASSETS", formatCurrency(totalAssets)));
        report.append("\n");

        // Liabilities section (3000-4999)
        report.append("LIABILITIES\n");
        report.append("=".repeat(80)).append("\n");
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isLiabilityAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), 43),
                            formatCurrency(amount)));
                    totalLiabilities = totalLiabilities.add(amount);
                }
            }
        }
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES", formatCurrency(totalLiabilities)));
        report.append("\n");

        // Equity section (5000-5999) + Retained Earnings
        report.append("EQUITY\n");
        report.append("=".repeat(80)).append("\n");
        
        // Show existing equity accounts first (opening balances)
        BigDecimal openingEquity = BigDecimal.ZERO;
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isEquityAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), 43),
                            formatCurrency(amount)));
                    openingEquity = openingEquity.add(amount);
                }
            }
        }
        
        // Add Retained Earnings calculation: Opening Equity + Net Profit/Loss
        BigDecimal retainedEarnings = openingEquity.add(netProfit);
        report.append(String.format("%-15s %-45s %15s%n",
                "RETAINED",
                "Retained Earnings (Opening + Net Profit)",
                formatCurrency(retainedEarnings)));
        totalEquity = retainedEarnings;
        report.append("\n");

        // Total Liabilities and Equity
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        report.append("TOTAL LIABILITIES AND EQUITY\n");
        report.append("=".repeat(80)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES & EQUITY", formatCurrency(totalLiabilitiesAndEquity)));
        report.append("\n");

        // Balance Sheet equation check: Assets = Liabilities + Equity
        BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            report.append("*** BALANCE SHEET DOES NOT BALANCE ***\n");
            report.append(String.format("Difference: %s%n", formatCurrency(difference.abs())));
            if (totalAssets.compareTo(totalLiabilitiesAndEquity) > 0) {
                report.append("Assets exceed Liabilities + Equity\n");
            } else {
                report.append("Liabilities + Equity exceed Assets\n");
            }
        } else {
            report.append("*** BALANCE SHEET BALANCES ***\n");
            report.append("Assets = Liabilities + Equity ✓\n");
        }

        return report.toString();
    }

    /**
     * Calculate net profit/loss for the period
     * Net Profit = Total Revenue - Total Expenses
     * Revenue accounts: 6000-7999
     * Expense accounts: 8000-9999
     */
    private BigDecimal calculateNetProfit(Map<String, AccountBalance> accountBalances) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            BigDecimal amount = balance.getClosingBalance();
            
            // Revenue accounts (6000-7999) - CREDIT normal balance
            if (isRevenueAccount(accountCode)) {
                totalRevenue = totalRevenue.add(amount);
            }
            // Expense accounts (8000-9999) - DEBIT normal balance  
            else if (isExpenseAccount(accountCode)) {
                totalExpenses = totalExpenses.add(amount);
            }
        }
        
        // Net Profit = Revenue - Expenses
        return totalRevenue.subtract(totalExpenses);
    }

    /**
     * Check if account code represents an Asset (1000-2999)
     */
    private boolean isAssetAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) return false;
        try {
            int code = Integer.parseInt(accountCode);
            return code >= 1000 && code <= 2999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents a Liability (3000-4999)
     */
    private boolean isLiabilityAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) return false;
        try {
            int code = Integer.parseInt(accountCode);
            return code >= 3000 && code <= 4999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents Equity (5000-5999)
     */
    private boolean isEquityAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) return false;
        try {
            int code = Integer.parseInt(accountCode);
            return code >= 5000 && code <= 5999;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if account code represents Revenue (6000-7999)
     */
    private boolean isRevenueAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) return false;
        // Handle account codes with dashes (e.g., "6100-001")
        String cleanCode = accountCode.split("-")[0];
        try {
            int code = Integer.parseInt(cleanCode);
            return code >= 6000 && code <= 7999;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents Expenses (8000-9999)
     */
    private boolean isExpenseAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < 4) return false;
        // Handle account codes with dashes (e.g., "8100-001")
        String cleanCode = accountCode.split("-")[0];
        try {
            int code = Integer.parseInt(cleanCode);
            return code >= 8000 && code <= 9999;
        } catch (NumberFormatException e) {
            return false;
        }
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

    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "R      0.00";
        }
        return String.format("R %10.2f", amount);
    }
}