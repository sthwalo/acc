package fin.service;

import fin.model.BankTransaction;
import fin.model.FiscalPeriod;

import java.math.BigDecimal;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ReportService {
    private final String dbUrl;
    private final CsvImportService csvImportService;
    
    public ReportService(String dbUrl, CsvImportService csvImportService) {
        this.dbUrl = dbUrl;
        this.csvImportService = csvImportService;
    }
    
    /**
     * Generates a cashbook report for the specified fiscal period
     */
    public String generateCashbookReport(Long fiscalPeriodId) {
        List<BankTransaction> transactions = csvImportService.getTransactionsByFiscalPeriod(fiscalPeriodId);
        
        if (transactions.isEmpty()) {
            return "No transactions found for the specified fiscal period.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("CASHBOOK REPORT\n");
        report.append("==============\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("Date Range: ").append(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                  .append(" - ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        // Sort transactions by date
        transactions.sort(Comparator.comparing(BankTransaction::getTransactionDate));
        
        // Format and add transactions to report
        report.append(String.format("%-12s %-40s %15s %15s %15s%n", 
                "Date", "Description", "Debit", "Credit", "Balance"));
        report.append("-".repeat(100)).append("\n");
        
        for (BankTransaction transaction : transactions) {
            String date = transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String description = transaction.getDetails();
            if (description != null && description.length() > 38) {
                description = description.substring(0, 35) + "...";
            }
            
            String debit = transaction.getDebitAmount() != null ? 
                    String.format("%,.2f", transaction.getDebitAmount()) : "";
            String credit = transaction.getCreditAmount() != null ? 
                    String.format("%,.2f", transaction.getCreditAmount()) : "";
            String balance = transaction.getBalance() != null ? 
                    String.format("%,.2f", transaction.getBalance()) : "";
            
            report.append(String.format("%-12s %-40s %15s %15s %15s%n", 
                    date, description, debit, credit, balance));
        }
        
        // Calculate totals
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getDebitAmount() != null)
                .map(BankTransaction::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getCreditAmount() != null)
                .map(BankTransaction::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        report.append("-".repeat(100)).append("\n");
        report.append(String.format("%-53s %15s %15s%n", 
                "TOTALS", String.format("%,.2f", totalDebits), String.format("%,.2f", totalCredits)));
        
        return report.toString();
    }
    
    /**
     * Generates a general ledger report for the specified fiscal period
     */
    public String generateGeneralLedgerReport(Long fiscalPeriodId) {
        List<BankTransaction> transactions = csvImportService.getTransactionsByFiscalPeriod(fiscalPeriodId);
        
        if (transactions.isEmpty()) {
            return "No transactions found for the specified fiscal period.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("GENERAL LEDGER REPORT\n");
        report.append("====================\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("Date Range: ").append(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                  .append(" - ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        // Group transactions by account (for now, we'll use a simplified approach)
        Map<String, List<BankTransaction>> accountTransactions = new HashMap<>();
        
        // For this simplified version, we'll categorize based on transaction details
        for (BankTransaction transaction : transactions) {
            String accountName = categorizeTransaction(transaction);
            accountTransactions.computeIfAbsent(accountName, k -> new ArrayList<>()).add(transaction);
        }
        
        // Generate report for each account
        for (Map.Entry<String, List<BankTransaction>> entry : accountTransactions.entrySet()) {
            String accountName = entry.getKey();
            List<BankTransaction> accountTxns = entry.getValue();
            
            report.append("\nAccount: ").append(accountName).append("\n");
            report.append("-".repeat(80)).append("\n");
            report.append(String.format("%-12s %-40s %12s %12s%n", 
                    "Date", "Description", "Debit", "Credit"));
            report.append("-".repeat(80)).append("\n");
            
            BigDecimal accountDebitTotal = BigDecimal.ZERO;
            BigDecimal accountCreditTotal = BigDecimal.ZERO;
            
            for (BankTransaction txn : accountTxns) {
                String date = txn.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String description = txn.getDetails();
                if (description != null && description.length() > 38) {
                    description = description.substring(0, 35) + "...";
                }
                
                String debit = txn.getDebitAmount() != null ? 
                        String.format("%,.2f", txn.getDebitAmount()) : "";
                String credit = txn.getCreditAmount() != null ? 
                        String.format("%,.2f", txn.getCreditAmount()) : "";
                
                report.append(String.format("%-12s %-40s %12s %12s%n", 
                        date, description, debit, credit));
                
                if (txn.getDebitAmount() != null) {
                    accountDebitTotal = accountDebitTotal.add(txn.getDebitAmount());
                }
                if (txn.getCreditAmount() != null) {
                    accountCreditTotal = accountCreditTotal.add(txn.getCreditAmount());
                }
            }
            
            report.append("-".repeat(80)).append("\n");
            report.append(String.format("%-53s %12s %12s%n", 
                    "Account Totals", String.format("%,.2f", accountDebitTotal), 
                    String.format("%,.2f", accountCreditTotal)));
            
            // Calculate account balance
            BigDecimal accountBalance = accountDebitTotal.subtract(accountCreditTotal);
            String balanceType = accountBalance.compareTo(BigDecimal.ZERO) >= 0 ? "DR" : "CR";
            accountBalance = accountBalance.abs();
            
            report.append(String.format("%-53s %12s %s%n", 
                    "Account Balance", String.format("%,.2f", accountBalance), balanceType));
            report.append("\n");
        }
        
        return report.toString();
    }
    
    /**
     * Generates a trial balance report for the specified fiscal period
     */
    public String generateTrialBalanceReport(Long fiscalPeriodId) {
        List<BankTransaction> transactions = csvImportService.getTransactionsByFiscalPeriod(fiscalPeriodId);
        
        if (transactions.isEmpty()) {
            return "No transactions found for the specified fiscal period.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("TRIAL BALANCE REPORT\n");
        report.append("===================\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("Date Range: ").append(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                  .append(" - ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        // Group transactions by account
        Map<String, BigDecimal> accountBalances = new HashMap<>();
        
        // For this simplified version, we'll categorize based on transaction details
        for (BankTransaction transaction : transactions) {
            String accountName = categorizeTransaction(transaction);
            BigDecimal amount = BigDecimal.ZERO;
            
            if (transaction.getDebitAmount() != null) {
                amount = amount.add(transaction.getDebitAmount());
            }
            if (transaction.getCreditAmount() != null) {
                amount = amount.subtract(transaction.getCreditAmount());
            }
            
            accountBalances.merge(accountName, amount, BigDecimal::add);
        }
        
        // Generate trial balance
        report.append(String.format("%-40s %15s %15s%n", 
                "Account", "Debit", "Credit"));
        report.append("-".repeat(72)).append("\n");
        
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (Map.Entry<String, BigDecimal> entry : accountBalances.entrySet()) {
            String accountName = entry.getKey();
            BigDecimal balance = entry.getValue();
            
            String debit = "";
            String credit = "";
            
            if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                debit = String.format("%,.2f", balance);
                totalDebits = totalDebits.add(balance);
            } else {
                credit = String.format("%,.2f", balance.abs());
                totalCredits = totalCredits.add(balance.abs());
            }
            
            report.append(String.format("%-40s %15s %15s%n", 
                    accountName, debit, credit));
        }
        
        report.append("-".repeat(72)).append("\n");
        report.append(String.format("%-40s %15s %15s%n", 
                "TOTALS", String.format("%,.2f", totalDebits), String.format("%,.2f", totalCredits)));
        
        return report.toString();
    }
    
    /**
     * Generates an income statement for the specified fiscal period
     */
    public String generateIncomeStatementReport(Long fiscalPeriodId) {
        List<BankTransaction> transactions = csvImportService.getTransactionsByFiscalPeriod(fiscalPeriodId);
        
        if (transactions.isEmpty()) {
            return "No transactions found for the specified fiscal period.";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("INCOME STATEMENT\n");
        report.append("===============\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("Date Range: ").append(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                  .append(" - ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        // Categorize transactions into revenue and expenses
        Map<String, BigDecimal> revenueAccounts = new HashMap<>();
        Map<String, BigDecimal> expenseAccounts = new HashMap<>();
        
        for (BankTransaction transaction : transactions) {
            String category = categorizeTransaction(transaction);
            BigDecimal amount = BigDecimal.ZERO;
            
            if (transaction.getDebitAmount() != null) {
                amount = transaction.getDebitAmount();
                // Simplified: assume all debits are expenses
                expenseAccounts.merge(category, amount, BigDecimal::add);
            }
            
            if (transaction.getCreditAmount() != null) {
                amount = transaction.getCreditAmount();
                // Simplified: assume all credits are revenue
                revenueAccounts.merge(category, amount, BigDecimal::add);
            }
        }
        
        // Generate income statement
        report.append("REVENUE\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : revenueAccounts.entrySet()) {
            report.append(String.format("%-40s %20s%n", 
                    entry.getKey(), String.format("%,.2f", entry.getValue())));
            totalRevenue = totalRevenue.add(entry.getValue());
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-40s %20s%n", 
                "Total Revenue", String.format("%,.2f", totalRevenue)));
        report.append("\n");
        
        report.append("EXPENSES\n");
        report.append("-".repeat(60)).append("\n");
        
        BigDecimal totalExpenses = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : expenseAccounts.entrySet()) {
            report.append(String.format("%-40s %20s%n", 
                    entry.getKey(), String.format("%,.2f", entry.getValue())));
            totalExpenses = totalExpenses.add(entry.getValue());
        }
        
        report.append("-".repeat(60)).append("\n");
        report.append(String.format("%-40s %20s%n", 
                "Total Expenses", String.format("%,.2f", totalExpenses)));
        report.append("\n");
        
        // Calculate net income
        BigDecimal netIncome = totalRevenue.subtract(totalExpenses);
        report.append("=".repeat(60)).append("\n");
        report.append(String.format("%-40s %20s%n", 
                "NET INCOME", String.format("%,.2f", netIncome)));
        
        return report.toString();
    }
    
    /**
     * Generates a balance sheet for the specified fiscal period
     */
    public String generateBalanceSheetReport(Long fiscalPeriodId) {
        // This would be a more complex implementation in a real system
        // For now, we'll return a placeholder
        
        StringBuilder report = new StringBuilder();
        report.append("BALANCE SHEET\n");
        report.append("============\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("As of: ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        report.append("Note: Balance sheet generation requires proper account classification.\n");
        report.append("This is a placeholder for the balance sheet report.\n");
        
        return report.toString();
    }
    
    /**
     * Generates a cash flow statement for the specified fiscal period
     */
    public String generateCashFlowReport(Long fiscalPeriodId) {
        // This would be a more complex implementation in a real system
        // For now, we'll return a placeholder
        
        StringBuilder report = new StringBuilder();
        report.append("CASH FLOW STATEMENT\n");
        report.append("===================\n\n");
        
        // Get fiscal period details
        FiscalPeriod period = getFiscalPeriodById(fiscalPeriodId);
        if (period != null) {
            report.append("Fiscal Period: ").append(period.getPeriodName()).append("\n");
            report.append("Date Range: ").append(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                  .append(" - ").append(period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n\n");
        }
        
        report.append("Note: Cash flow statement generation requires proper transaction categorization.\n");
        report.append("This is a placeholder for the cash flow statement report.\n");
        
        return report.toString();
    }
    
    private FiscalPeriod getFiscalPeriodById(Long fiscalPeriodId) {
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
            } else {
                return null;
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting fiscal period: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Simple categorization logic for transactions
     * In a real system, this would be more sophisticated with proper account mapping
     */
    private String categorizeTransaction(BankTransaction transaction) {
        String details = transaction.getDetails() != null ? transaction.getDetails().toLowerCase() : "";
        
        // Very simple categorization based on keywords
        if (details.contains("salary") || details.contains("salaries") || details.contains("wage")) {
            return "Salaries and Wages";
        } else if (details.contains("rent")) {
            return "Rent Expense";
        } else if (details.contains("insurance")) {
            return "Insurance Expense";
        } else if (details.contains("payment to") || details.contains("payment from")) {
            return "General Payments";
        } else if (details.contains("fee")) {
            return "Bank Fees";
        } else if (details.contains("transfer")) {
            return "Transfers";
        } else if (details.contains("withdrawal")) {
            return "Cash Withdrawals";
        } else {
            return "Miscellaneous";
        }
    }
}
