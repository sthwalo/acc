package fin.service;

import fin.model.FiscalPeriod;
import fin.model.Company;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Enhanced Financial Reporting Service that generates comprehensive 
 * double-entry accounting reports with export functionality.
 */
public class FinancialReportingService {
    private static final Logger LOGGER = Logger.getLogger(FinancialReportingService.class.getName());
    private final String dbUrl;
    private final NumberFormat currencyFormat;
    
    public FinancialReportingService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }
    
    /**
     * Generates a comprehensive General Ledger report
     */
    public String generateGeneralLedger(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("GENERAL LEDGER", company, period));
        
        String sql = """
            SELECT 
                a.account_code,
                a.account_name,
                ac.name as category_name,
                jel.debit_amount,
                jel.credit_amount,
                jel.description,
                jel.reference,
                je.entry_date,
                je.description as journal_description
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            JOIN accounts a ON jel.account_id = a.id
            JOIN account_categories ac ON a.category_id = ac.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            ORDER BY a.account_code, je.entry_date, jel.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            String currentAccount = "";
            BigDecimal accountDebitTotal = BigDecimal.ZERO;
            BigDecimal accountCreditTotal = BigDecimal.ZERO;
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;
            
            while (rs.next()) {
                String accountCode = rs.getString("account_code");
                String accountName = rs.getString("account_name");
                String accountKey = accountCode + " - " + accountName;
                
                // New account section
                if (!accountKey.equals(currentAccount)) {
                    // Print totals for previous account
                    if (!currentAccount.isEmpty()) {
                        report.append(String.format("%-80s %15s %15s\n", 
                                "ACCOUNT TOTALS:", 
                                formatCurrency(accountDebitTotal),
                                formatCurrency(accountCreditTotal)));
                        
                        BigDecimal balance = accountDebitTotal.subtract(accountCreditTotal);
                        String balanceType = balance.compareTo(BigDecimal.ZERO) >= 0 ? "DR" : "CR";
                        report.append(String.format("%-80s %15s %s\n\n", 
                                "BALANCE:", 
                                formatCurrency(balance.abs()), 
                                balanceType));
                    }
                    
                    currentAccount = accountKey;
                    accountDebitTotal = BigDecimal.ZERO;
                    accountCreditTotal = BigDecimal.ZERO;
                    
                    // Account header
                    report.append("=".repeat(120)).append("\n");
                    report.append(String.format("ACCOUNT: %s (%s)\n", 
                            accountKey, rs.getString("category_name")));
                    report.append("=".repeat(120)).append("\n");
                    report.append(String.format("%-12s %-30s %-25s %15s %15s\n",
                            "Date", "Reference", "Description", "Debit", "Credit"));
                    report.append("-".repeat(120)).append("\n");
                }
                
                // Transaction line
                java.sql.Date entryDate = rs.getDate("entry_date");
                String reference = rs.getString("reference");
                String description = rs.getString("description");
                BigDecimal debitAmount = rs.getBigDecimal("debit_amount");
                BigDecimal creditAmount = rs.getBigDecimal("credit_amount");
                
                if (debitAmount != null) {
                    accountDebitTotal = accountDebitTotal.add(debitAmount);
                    grandTotalDebits = grandTotalDebits.add(debitAmount);
                }
                if (creditAmount != null) {
                    accountCreditTotal = accountCreditTotal.add(creditAmount);
                    grandTotalCredits = grandTotalCredits.add(creditAmount);
                }
                
                report.append(String.format("%-12s %-30s %-25s %15s %15s\n",
                        entryDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        reference != null ? reference : "",
                        description != null && description.length() > 23 ? 
                            description.substring(0, 20) + "..." : description,
                        debitAmount != null ? formatCurrency(debitAmount) : "",
                        creditAmount != null ? formatCurrency(creditAmount) : ""));
            }
            
            // Final account totals
            if (!currentAccount.isEmpty()) {
                report.append(String.format("%-80s %15s %15s\n", 
                        "ACCOUNT TOTALS:", 
                        formatCurrency(accountDebitTotal),
                        formatCurrency(accountCreditTotal)));
                
                BigDecimal balance = accountDebitTotal.subtract(accountCreditTotal);
                String balanceType = balance.compareTo(BigDecimal.ZERO) >= 0 ? "DR" : "CR";
                report.append(String.format("%-80s %15s %s\n\n", 
                        "BALANCE:", 
                        formatCurrency(balance.abs()), 
                        balanceType));
            }
            
            // Grand totals
            report.append("=".repeat(120)).append("\n");
            report.append(String.format("%-80s %15s %15s\n", 
                    "GRAND TOTALS:", 
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(120)).append("\n");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating general ledger", e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("general_ledger", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    /**
     * Generates a Trial Balance report
     */
    public String generateTrialBalance(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("TRIAL BALANCE", company, period));
        
        String sql = """
            SELECT 
                a.account_code,
                a.account_name,
                ac.name as category_name,
                at.name as account_type,
                SUM(COALESCE(jel.debit_amount, 0)) as total_debits,
                SUM(COALESCE(jel.credit_amount, 0)) as total_credits
            FROM accounts a
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id 
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ? AND a.is_active = true
            GROUP BY a.id, a.account_code, a.account_name, ac.name, at.name
            HAVING SUM(COALESCE(jel.debit_amount, 0)) + SUM(COALESCE(jel.credit_amount, 0)) > 0
            ORDER BY a.account_code
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            pstmt.setLong(3, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            // Headers
            report.append(String.format("%-10s %-35s %-20s %15s %15s\n",
                    "Code", "Account Name", "Category", "Debit", "Credit"));
            report.append("-".repeat(100)).append("\n");
            
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;
            
            while (rs.next()) {
                String accountCode = rs.getString("account_code");
                String accountName = rs.getString("account_name");
                String categoryName = rs.getString("category_name");
                BigDecimal totalDebits = rs.getBigDecimal("total_debits");
                BigDecimal totalCredits = rs.getBigDecimal("total_credits");
                
                BigDecimal balance = totalDebits.subtract(totalCredits);
                String debitColumn = "";
                String creditColumn = "";
                
                if (balance.compareTo(BigDecimal.ZERO) > 0) {
                    debitColumn = formatCurrency(balance);
                    grandTotalDebits = grandTotalDebits.add(balance);
                } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
                    creditColumn = formatCurrency(balance.abs());
                    grandTotalCredits = grandTotalCredits.add(balance.abs());
                }
                
                report.append(String.format("%-10s %-35s %-20s %15s %15s\n",
                        accountCode,
                        accountName.length() > 33 ? accountName.substring(0, 30) + "..." : accountName,
                        categoryName.length() > 18 ? categoryName.substring(0, 15) + "..." : categoryName,
                        debitColumn,
                        creditColumn));
            }
            
            // Totals
            report.append("-".repeat(100)).append("\n");
            report.append(String.format("%-66s %15s %15s\n", 
                    "TOTALS:", 
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(100)).append("\n");
            
            // Verify balance
            if (grandTotalDebits.compareTo(grandTotalCredits) == 0) {
                report.append("\n✓ TRIAL BALANCE IS IN BALANCE\n");
            } else {
                report.append("\n⚠ TRIAL BALANCE IS OUT OF BALANCE!\n");
                report.append("Difference: ").append(formatCurrency(grandTotalDebits.subtract(grandTotalCredits))).append("\n");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating trial balance", e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("trial_balance", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    /**
     * Generates an Income Statement (Profit & Loss)
     */
    public String generateIncomeStatement(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("INCOME STATEMENT", company, period));
        
        Map<String, BigDecimal> revenueAccounts = getAccountBalancesByType(companyId, fiscalPeriodId, "REVENUE");
        Map<String, BigDecimal> expenseAccounts = getAccountBalancesByType(companyId, fiscalPeriodId, "EXPENSE");
        
        // Revenue Section
        report.append("REVENUE\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : revenueAccounts.entrySet()) {
            BigDecimal amount = entry.getValue();
            totalRevenue = totalRevenue.add(amount);
            report.append(String.format("%-45s %14s\n", 
                    entry.getKey(), formatCurrency(amount)));
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL REVENUE", formatCurrency(totalRevenue)));
        report.append("\n");
        
        // Expenses Section
        report.append("EXPENSES\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : expenseAccounts.entrySet()) {
            BigDecimal amount = entry.getValue();
            totalExpenses = totalExpenses.add(amount);
            report.append(String.format("%-45s %14s\n", 
                    entry.getKey(), formatCurrency(amount)));
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL EXPENSES", formatCurrency(totalExpenses)));
        report.append("\n");
        
        // Net Income
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        report.append("=".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "NET INCOME", formatCurrency(netIncome)));
        report.append("=".repeat(60)).append("\n");
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("income_statement", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    /**
     * Generates a Balance Sheet
     */
    public String generateBalanceSheet(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("BALANCE SHEET", company, period));
        
        Map<String, BigDecimal> assets = getAccountBalancesByType(companyId, fiscalPeriodId, "ASSET");
        Map<String, BigDecimal> liabilities = getAccountBalancesByType(companyId, fiscalPeriodId, "LIABILITY");
        Map<String, BigDecimal> equity = getAccountBalancesByType(companyId, fiscalPeriodId, "EQUITY");
        
        // Assets Section
        report.append("ASSETS\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalAssets = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : assets.entrySet()) {
            BigDecimal amount = entry.getValue();
            totalAssets = totalAssets.add(amount);
            report.append(String.format("%-45s %14s\n", 
                    entry.getKey(), formatCurrency(amount)));
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL ASSETS", formatCurrency(totalAssets)));
        report.append("\n");
        
        // Liabilities Section
        report.append("LIABILITIES\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : liabilities.entrySet()) {
            BigDecimal amount = entry.getValue();
            totalLiabilities = totalLiabilities.add(amount);
            report.append(String.format("%-45s %14s\n", 
                    entry.getKey(), formatCurrency(amount)));
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL LIABILITIES", formatCurrency(totalLiabilities)));
        report.append("\n");
        
        // Equity Section
        report.append("OWNER'S EQUITY\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalEquity = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : equity.entrySet()) {
            BigDecimal amount = entry.getValue();
            totalEquity = totalEquity.add(amount);
            report.append(String.format("%-45s %14s\n", 
                    entry.getKey(), formatCurrency(amount)));
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL EQUITY", formatCurrency(totalEquity)));
        report.append("\n");
        
        // Balance Check
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        report.append("=".repeat(60)).append("\n");
        report.append(String.format("%-45s %14s\n", 
                "TOTAL LIABILITIES & EQUITY", formatCurrency(totalLiabilitiesAndEquity)));
        report.append("=".repeat(60)).append("\n");
        
        if (totalAssets.compareTo(totalLiabilitiesAndEquity) == 0) {
            report.append("\n✓ BALANCE SHEET IS IN BALANCE\n");
        } else {
            report.append("\n⚠ BALANCE SHEET IS OUT OF BALANCE!\n");
            report.append("Difference: ").append(formatCurrency(totalAssets.subtract(totalLiabilitiesAndEquity))).append("\n");
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("balance_sheet", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    /**
     * Generates a Cashbook report
     */
    public String generateCashbook(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("CASHBOOK", company, period));
        
        String sql = """
            SELECT
                bt.transaction_date,
                bt.details,
                jel.debit_amount as bank_debit,
                jel.credit_amount as bank_credit,
                bt.balance,
                COALESCE(bt.account_name, a.account_name) as account_name,
                COALESCE(bt.account_code, a.account_code) as account_code
            FROM bank_transactions bt
            LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id 
                AND jel.account_id = (SELECT id FROM accounts WHERE account_code = '1100' LIMIT 1)
            LEFT JOIN journal_entry_lines jel2 ON bt.id = jel2.source_transaction_id 
                AND jel2.account_id != (SELECT id FROM accounts WHERE account_code = '1100' LIMIT 1)
            LEFT JOIN accounts a ON jel2.account_id = a.id
            WHERE bt.company_id = ? AND bt.fiscal_period_id = ?
            ORDER BY bt.transaction_date, bt.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            // Headers
            report.append(String.format("%-12s %-35s %-20s %15s %15s %15s\n",
                    "Date", "Details", "Account", "Debit", "Credit", "Balance"));
            report.append("-".repeat(120)).append("\n");
            
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            
            while (rs.next()) {
                java.sql.Date transactionDate = rs.getDate("transaction_date");
                String details = rs.getString("details");
                String accountName = rs.getString("account_name");
                String accountCode = rs.getString("account_code");
                BigDecimal bankDebit = rs.getBigDecimal("bank_debit");
                BigDecimal bankCredit = rs.getBigDecimal("bank_credit");
                BigDecimal balance = rs.getBigDecimal("balance");
                
                // Use journal entry amounts for bank account (correct debit/credit)
                BigDecimal debitAmount = bankDebit;
                BigDecimal creditAmount = bankCredit;
                
                if (debitAmount != null) totalDebits = totalDebits.add(debitAmount);
                if (creditAmount != null) totalCredits = totalCredits.add(creditAmount);
                
                // Format account display - prefer account_name, fallback to account_code if name is null
                String accountDisplay = accountName != null ? accountName : 
                    (accountCode != null ? accountCode : "Unclassified");
                
                report.append(String.format("%-12s %-35s %-20s %15s %15s %15s\n",
                        transactionDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        details != null && details.length() > 33 ? details.substring(0, 30) + "..." : details,
                        accountDisplay.length() > 18 ? accountDisplay.substring(0, 15) + "..." : accountDisplay,
                        debitAmount != null ? formatCurrency(debitAmount) : "",
                        creditAmount != null ? formatCurrency(creditAmount) : "",
                        balance != null ? formatCurrency(balance) : ""));
            }
            
            // Totals
            report.append("-".repeat(120)).append("\n");
            report.append(String.format("%-68s %15s %15s\n", 
                    "TOTALS:", 
                    formatCurrency(totalDebits),
                    formatCurrency(totalCredits)));
            report.append("=".repeat(120)).append("\n");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating cashbook", e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("cashbook", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    /**
     * Generates an Audit Trail report showing all journal entries
     */
    public String generateAuditTrail(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("AUDIT TRAIL", company, period));
        
        String sql = """
            SELECT 
                je.reference,
                je.entry_date,
                je.description as journal_description,
                je.created_by,
                je.created_at,
                a.account_code,
                a.account_name,
                jel.debit_amount,
                jel.credit_amount,
                jel.description as line_description,
                jel.reference as line_reference
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            ORDER BY je.entry_date, je.id, jel.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            String currentEntry = "";
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;
            
            while (rs.next()) {
                String reference = rs.getString("reference");
                java.sql.Date entryDate = rs.getDate("entry_date");
                String journalDescription = rs.getString("journal_description");
                String createdBy = rs.getString("created_by");
                Timestamp createdAt = rs.getTimestamp("created_at");
                
                // New journal entry header
                if (!reference.equals(currentEntry)) {
                    currentEntry = reference;
                    
                    report.append("\n").append("=".repeat(120)).append("\n");
                    report.append(String.format("ENTRY: %-20s DATE: %-12s CREATED BY: %-20s\n",
                            reference,
                            entryDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            createdBy != null ? createdBy : "SYSTEM"));
                    report.append(String.format("DESCRIPTION: %s\n", journalDescription));
                    report.append(String.format("TIMESTAMP: %s\n", 
                            createdAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
                    report.append("-".repeat(120)).append("\n");
                    report.append(String.format("%-10s %-25s %-30s %15s %15s\n",
                            "Code", "Account", "Description", "Debit", "Credit"));
                    report.append("-".repeat(120)).append("\n");
                }
                
                // Journal entry line
                String accountCode = rs.getString("account_code");
                String accountName = rs.getString("account_name");
                String lineDescription = rs.getString("line_description");
                BigDecimal debitAmount = rs.getBigDecimal("debit_amount");
                BigDecimal creditAmount = rs.getBigDecimal("credit_amount");
                
                if (debitAmount != null) grandTotalDebits = grandTotalDebits.add(debitAmount);
                if (creditAmount != null) grandTotalCredits = grandTotalCredits.add(creditAmount);
                
                report.append(String.format("%-10s %-25s %-30s %15s %15s\n",
                        accountCode,
                        accountName.length() > 23 ? accountName.substring(0, 20) + "..." : accountName,
                        lineDescription != null && lineDescription.length() > 28 ? 
                            lineDescription.substring(0, 25) + "..." : lineDescription,
                        debitAmount != null ? formatCurrency(debitAmount) : "",
                        creditAmount != null ? formatCurrency(creditAmount) : ""));
            }
            
            // Grand totals
            report.append("\n").append("=".repeat(120)).append("\n");
            report.append(String.format("%-71s %15s %15s\n", 
                    "GRAND TOTALS:", 
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(120)).append("\n");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating audit trail", e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("audit_trail", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    // Helper methods
    
    private String generateReportHeader(String reportTitle, Company company, FiscalPeriod period) {
        StringBuilder header = new StringBuilder();
        header.append("=".repeat(120)).append("\n");
        header.append(String.format("%s\n", centerText(reportTitle, 120)));
        header.append("=".repeat(120)).append("\n");
        
        if (company != null) {
            header.append(String.format("Company: %s\n", company.getName()));
        }
        if (period != null) {
            header.append(String.format("Fiscal Period: %s (%s to %s)\n", 
                    period.getPeriodName(),
                    period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
        header.append(String.format("Generated: %s\n", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        header.append("-".repeat(120)).append("\n\n");
        
        return header.toString();
    }
    
    private Map<String, BigDecimal> getAccountBalancesByType(Long companyId, Long fiscalPeriodId, String accountType) {
        Map<String, BigDecimal> balances = new LinkedHashMap<>();
        
        String sql = """
            SELECT 
                a.account_code,
                a.account_name,
                SUM(COALESCE(jel.debit_amount, 0)) - SUM(COALESCE(jel.credit_amount, 0)) as balance
            FROM accounts a
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id 
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ? AND a.is_active = true AND at.name = ?
            GROUP BY a.id, a.account_code, a.account_name
            HAVING SUM(COALESCE(jel.debit_amount, 0)) + SUM(COALESCE(jel.credit_amount, 0)) > 0
            ORDER BY a.account_code
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            pstmt.setLong(3, companyId);
            pstmt.setString(4, accountType);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String accountCode = rs.getString("account_code");
                String accountName = rs.getString("account_name");
                BigDecimal balance = rs.getBigDecimal("balance");
                
                if (balance != null && balance.compareTo(BigDecimal.ZERO) != 0) {
                    balances.put(accountCode + " - " + accountName, balance.abs());
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting account balances by type", e);
        }
        
        return balances;
    }
    
    private void exportReport(String reportType, Company company, FiscalPeriod period, String content) {
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
            
            // Generate filename
            String filename = String.format("%s_%s_%s_%s.txt",
                    reportType,
                    company != null ? company.getName().replaceAll("\\s+", "_") : "company",
                    period != null ? period.getPeriodName().replaceAll("\\s+", "_") : "period",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
            
            Path filePath = reportsDir.resolve(filename);
            
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                writer.write(content);
            }
            
            LOGGER.info("Report exported to: " + filePath.toAbsolutePath());
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error exporting report", e);
        }
    }
    
    private FiscalPeriod getFiscalPeriod(Long fiscalPeriodId) {
        String sql = "SELECT * FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FiscalPeriod period = new FiscalPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStartDate(rs.getDate("start_date").toLocalDate());
                period.setEndDate(rs.getDate("end_date").toLocalDate());
                period.setClosed(rs.getBoolean("is_closed"));
                return period;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting fiscal period", e);
        }
        
        return null;
    }
    
    private Company getCompany(Long companyId) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                return company;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting company", e);
        }
        
        // Return default company if not found
        Company defaultCompany = new Company();
        defaultCompany.setId(companyId);
        defaultCompany.setName("Xinghizana Group");
        return defaultCompany;
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "";
        return currencyFormat.format(amount);
    }
    
    private String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }
}
