package fin.service;

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Service for generating general ledger reports.
 * Provides detailed transaction listings by account from journal entries.
 * Shows opening balance, all transactions with running balance, and closing balance.
 */
public class GeneralLedgerService {

    private final FinancialDataRepository repository;

    public GeneralLedgerService(FinancialDataRepository repository) {
        this.repository = repository;
    }

    /**
     * Generate general ledger report content for ALL accounts.
     * 
     * CORRECT ACCOUNTING FLOW:
     * This method reads DIRECTLY from journal entries (source of truth).
     * It does NOT depend on Trial Balance - GL is PRIMARY, TB is SECONDARY.
     */
    public String generateGeneralLedger(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        
        // ✅ CORRECT: Get all accounts from journal entries (NOT Trial Balance!)
        List<AccountInfo> accounts = repository.getActiveAccountsFromJournals(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("GENERAL LEDGER REPORT", company, fiscalPeriod));
        report.append("\n");
        report.append("Source: Journal Entries (Double-Entry Bookkeeping)\n");
        report.append("Hierarchy: Journal Entries → General Ledger → Trial Balance → Financial Statements\n");
        report.append("\n");

        // Generate ledger for each account with activity
        for (AccountInfo account : accounts) {
            String accountLedger = generateAccountLedger(
                companyId, 
                fiscalPeriodId, 
                account.getAccountCode(), 
                account.getAccountName(),
                account.getNormalBalance(),
                fiscalPeriod
            );
            report.append(accountLedger);
            report.append("\n\n");
        }

        return report.toString();
    }

    /**
     * Get account closing balances for Trial Balance posting.
     * 
     * CORRECT ACCOUNTING FLOW:
     * This method provides GL closing balances to Trial Balance Service.
     * Implements: Journal Entries → General Ledger → Trial Balance → Financial Statements
     */
    public Map<String, AccountBalance> getAccountClosingBalances(int companyId, int fiscalPeriodId) throws SQLException {
        List<AccountInfo> accounts = repository.getActiveAccountsFromJournals(companyId, fiscalPeriodId);
        
        Map<String, AccountBalance> accountBalances = new LinkedHashMap<>();
        
        for (AccountInfo account : accounts) {
            // Get account journal entry lines 
            List<JournalEntryLineDetail> ledgerLines = repository.getJournalEntryLinesForAccount(
                companyId, fiscalPeriodId, account.getAccountCode());
            
            // Get opening balance
            BigDecimal openingBalance = repository.getAccountOpeningBalanceForLedger(companyId, fiscalPeriodId, account.getAccountCode());
            
            // Calculate totals and closing balance
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            BigDecimal runningBalance = openingBalance;
            
            for (JournalEntryLineDetail line : ledgerLines) {
                BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
                BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
                
                totalDebits = totalDebits.add(debit);
                totalCredits = totalCredits.add(credit);
                
                // Update running balance based on account type
                if ("D".equals(account.getNormalBalance())) {
                    // DEBIT normal balance (Assets, Expenses): increase on debit, decrease on credit
                    runningBalance = runningBalance.add(debit).subtract(credit);
                } else {
                    // CREDIT normal balance (Liabilities, Equity, Revenue): increase on credit, decrease on debit
                    runningBalance = runningBalance.add(credit).subtract(debit);
                }
            }
            
            // Create account balance entry
            AccountBalance balance = new AccountBalance(
                account.getAccountCode(),
                account.getAccountName(), 
                account.getNormalBalance(),
                openingBalance,
                totalDebits,
                totalCredits,
                runningBalance
            );
            
            accountBalances.put(account.getAccountCode(), balance);
        }
        
        return accountBalances;
    }

    /**
     * Generate general ledger for a specific account
     */
    public String generateAccountLedger(int companyId, int fiscalPeriodId, String accountCode, 
                                       String accountName, String normalBalance, FiscalPeriod fiscalPeriod) throws SQLException {
        StringBuilder report = new StringBuilder();
        
        // Get opening balance for this account
        BigDecimal openingBalance = repository.getAccountOpeningBalanceForLedger(companyId, fiscalPeriodId, accountCode);
        
        // Get all journal entry lines for this account
        List<JournalEntryLineDetail> ledgerLines = repository.getJournalEntryLinesForAccount(companyId, fiscalPeriodId, accountCode);
        
        // Account header
        report.append("=".repeat(130)).append("\n");
        report.append(String.format("ACCOUNT: %s - %s%n", accountCode, accountName));
        report.append("=".repeat(130)).append("\n");
        
        // Opening balance
        String balanceIndicator = "D".equals(normalBalance) ? "DR" : "CR";
        report.append(String.format("Opening Balance (%s):                          %s %s%n%n",
            fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            formatCurrency(openingBalance.abs()),
            balanceIndicator
        ));
        
        // Column headers
        report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-20s%n",
                "Date", "Reference", "Description", "Debit", "Credit", "Balance"));
        report.append("=".repeat(130)).append("\n");
        
        // Calculate running balance
        BigDecimal runningBalance = openingBalance;
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        for (JournalEntryLineDetail line : ledgerLines) {
            BigDecimal debit = line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO;
            BigDecimal credit = line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO;
            
            totalDebits = totalDebits.add(debit);
            totalCredits = totalCredits.add(credit);
            
            // Update running balance based on account type
            if ("D".equals(normalBalance)) {
                // DEBIT normal balance (Assets, Expenses): increase on debit, decrease on credit
                runningBalance = runningBalance.add(debit).subtract(credit);
            } else {
                // CREDIT normal balance (Liabilities, Equity, Revenue): increase on credit, decrease on debit
                runningBalance = runningBalance.add(credit).subtract(debit);
            }
            
            // Format description
            String description = line.getDescription() != null ? line.getDescription() : "";
            if (description.length() > 48) {
                description = description.substring(0, 45) + "...";
            }
            
            // Determine balance display (DR or CR)
            String balanceDisplay = formatBalance(runningBalance, normalBalance);
            
            report.append(String.format("%-12s %-15s %-50s %-15s %-15s %-20s%n",
                    line.getEntryDate().format(dateFormatter),
                    line.getReference() != null ? line.getReference() : "N/A",
                    description,
                    formatCurrency(debit),
                    formatCurrency(credit),
                    balanceDisplay));
        }
        
        // Footer with totals
        report.append("=".repeat(130)).append("\n");
        report.append(String.format("Period Totals:                                       %-15s %-15s%n",
                formatCurrency(totalDebits),
                formatCurrency(totalCredits)));
        
        // Closing balance
        String closingBalanceIndicator = runningBalance.compareTo(BigDecimal.ZERO) >= 0 ? balanceIndicator : 
                                        ("D".equals(normalBalance) ? "CR" : "DR");
        report.append(String.format("Closing Balance (%s):                        %s %s%n",
            fiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
            formatCurrency(runningBalance.abs()),
            closingBalanceIndicator
        ));
        
        return report.toString();
    }
    
    /**
     * Format balance with DR/CR indicator
     */
    private String formatBalance(BigDecimal balance, String normalBalance) {
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return formatCurrency(BigDecimal.ZERO);
        }
        
        String indicator;
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            indicator = "D".equals(normalBalance) ? "DR" : "CR";
        } else {
            indicator = "D".equals(normalBalance) ? "CR" : "DR";
        }
        
        return String.format("%s %s", formatCurrency(balance.abs()), indicator);
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