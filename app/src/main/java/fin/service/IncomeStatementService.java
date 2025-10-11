package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Service for generating income statement reports.
 * Shows revenue and expenses for the period.
 * 
 * CORRECT ACCOUNTING FLOW:
 * This service reads FROM Trial Balance data (which reads from General Ledger).
 * Implements: Journal Entries → General Ledger → Trial Balance → Income Statement
 */
public class IncomeStatementService {

    private final FinancialDataRepository repository;
    private final GeneralLedgerService generalLedgerService;

    public IncomeStatementService(FinancialDataRepository repository, GeneralLedgerService generalLedgerService) {
        this.repository = repository;
        this.generalLedgerService = generalLedgerService;
    }

    /**
     * Generate income statement report content
     * 
     * ACCOUNTING PRINCIPLES:
     * - Revenue accounts (6000-7999): Increase with CREDIT, decrease with DEBIT
     * - Expense accounts (8000-9999): Increase with DEBIT, decrease with CREDIT
     * - Net Income = Revenue - Expenses
     */
    public String generateIncomeStatement(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        
        // ✅ CORRECT FLOW: Get closing balances FROM General Ledger (same source as Trial Balance)
        Map<String, AccountBalance> accountBalances = generalLedgerService.getAccountClosingBalances(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("INCOME STATEMENT", company, fiscalPeriod));
        report.append("\n");
        report.append("Source: Trial Balance Account Balances\n");
        report.append("Hierarchy: Journal Entries → General Ledger → Trial Balance → Income Statement\n");
        report.append("\n");

        // Separate revenue and expense accounts
        Map<String, AccountBalance> revenueAccounts = new LinkedHashMap<>();
        Map<String, AccountBalance> expenseAccounts = new LinkedHashMap<>();
        
        for (Map.Entry<String, AccountBalance> entry : accountBalances.entrySet()) {
            AccountBalance balance = entry.getValue();
            String accountCode = balance.getAccountCode();
            
            // Revenue accounts: 6000-7999
            if (accountCode.startsWith("6") || accountCode.startsWith("7")) {
                if (balance.getClosingBalance().compareTo(BigDecimal.ZERO) != 0) {
                    revenueAccounts.put(entry.getKey(), balance);
                }
            }
            // Expense accounts: 8000-9999
            else if (accountCode.startsWith("8") || accountCode.startsWith("9")) {
                if (balance.getClosingBalance().compareTo(BigDecimal.ZERO) != 0) {
                    expenseAccounts.put(entry.getKey(), balance);
                }
            }
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        // REVENUE SECTION
        report.append("REVENUE\n");
        report.append("=".repeat(80)).append("\n");
        report.append(String.format("%-15s %-45s %-15s%n", "Account Code", "Account Name", "Amount (ZAR)"));
        report.append("-".repeat(80)).append("\n");
        
        if (revenueAccounts.isEmpty()) {
            report.append("No revenue accounts with balances found.\n");
        } else {
            for (AccountBalance balance : revenueAccounts.values()) {
                // Revenue accounts have CREDIT normal balance - show as positive amounts
                BigDecimal revenueAmount = balance.getClosingBalance().abs();
                totalRevenue = totalRevenue.add(revenueAmount);
                
                report.append(String.format("%-15s %-45s %-15s%n",
                        balance.getAccountCode(),
                        truncateString(balance.getAccountName(), 43),
                        formatCurrency(revenueAmount)));
            }
        }
        
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-60s %-15s%n", "TOTAL REVENUE:", formatCurrency(totalRevenue)));
        report.append("\n\n");

        // EXPENSES SECTION
        report.append("EXPENSES\n");
        report.append("=".repeat(80)).append("\n");
        report.append(String.format("%-15s %-45s %-15s%n", "Account Code", "Account Name", "Amount (ZAR)"));
        report.append("-".repeat(80)).append("\n");
        
        if (expenseAccounts.isEmpty()) {
            report.append("No expense accounts with balances found.\n");
        } else {
            for (AccountBalance balance : expenseAccounts.values()) {
                // Expense accounts have DEBIT normal balance - show as positive amounts
                BigDecimal expenseAmount = balance.getClosingBalance().abs();
                totalExpenses = totalExpenses.add(expenseAmount);
                
                report.append(String.format("%-15s %-45s %-15s%n",
                        balance.getAccountCode(),
                        truncateString(balance.getAccountName(), 43),
                        formatCurrency(expenseAmount)));
            }
        }
        
        report.append("-".repeat(80)).append("\n");
        report.append(String.format("%-60s %-15s%n", "TOTAL EXPENSES:", formatCurrency(totalExpenses)));
        report.append("\n\n");

        // NET INCOME CALCULATION
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        
        report.append("NET INCOME\n");
        report.append("=".repeat(80)).append("\n");
        report.append(String.format("%-60s %-15s%n", "Total Revenue:", formatCurrency(totalRevenue)));
        report.append(String.format("%-60s %-15s%n", "Less: Total Expenses:", formatCurrency(totalExpenses)));
        report.append("-".repeat(80)).append("\n");
        
        String netIncomeLabel = netIncome.compareTo(BigDecimal.ZERO) >= 0 ? "NET PROFIT:" : "NET LOSS:";
        report.append(String.format("%-60s %-15s%n", netIncomeLabel, formatCurrency(netIncome.abs())));
        
        report.append("=".repeat(80)).append("\n");

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

    private String truncateString(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength - 3) + "...";
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "R      0.00";
        }
        return String.format("R %10.2f", amount);
    }
}