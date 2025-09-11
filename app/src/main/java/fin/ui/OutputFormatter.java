package fin.ui;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Output formatting component
 * Extracted from monolithic App.java to separate output concerns
 */
public class OutputFormatter {
    
    private static final String SUCCESS_PREFIX = "✅ ";
    private static final String ERROR_PREFIX = "❌ ";
    private static final String WARNING_PREFIX = "⚠️ ";
    private static final String INFO_PREFIX = "ℹ️ ";
    private static final String PROCESS_PREFIX = "🔄 ";
    
    public void printSuccess(String message) {
        System.out.println(SUCCESS_PREFIX + message);
    }
    
    public void printError(String message) {
        System.err.println(ERROR_PREFIX + message);
    }
    
    public void printWarning(String message) {
        System.out.println(WARNING_PREFIX + message);
    }
    
    public void printInfo(String message) {
        System.out.println(INFO_PREFIX + message);
    }
    
    public void printProcessing(String message) {
        System.out.println(PROCESS_PREFIX + message);
    }
    
    public void printPlain(String message) {
        System.out.println(message);
    }
    
    public void printHeader(String title) {
        int totalWidth = 80;
        int titleLength = title.length();
        int paddingLength = (totalWidth - titleLength - 2) / 2;
        String padding = "=".repeat(Math.max(0, paddingLength));
        
        System.out.println("\n" + "=".repeat(totalWidth));
        System.out.println(padding + " " + title + " " + padding);
        System.out.println("=".repeat(totalWidth));
    }
    
    public void printSubHeader(String title) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println(title);
        System.out.println("-".repeat(50));
    }
    
    public void printSeparator() {
        System.out.println("-".repeat(80));
    }
    
    public void printDoubleSeparator() {
        System.out.println("=".repeat(80));
    }
    
    public void printProgress(String operation, int current, int total) {
        int percentage = (int) ((double) current / total * 100);
        String progressBar = createProgressBar(percentage, 20);
        System.out.printf("\r%s [%s] %d%% (%d/%d)", operation, progressBar, percentage, current, total);
        if (current == total) {
            System.out.println(); // New line when complete
        }
    }
    
    private String createProgressBar(int percentage, int width) {
        int filled = (int) (percentage / 100.0 * width);
        return "█".repeat(filled) + "░".repeat(width - filled);
    }
    
    public void printTable(List<String[]> data, String[] headers) {
        if (data.isEmpty()) {
            printInfo("No data to display");
            return;
        }
        
        // Calculate column widths
        int[] columnWidths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }
        
        for (String[] row : data) {
            for (int i = 0; i < row.length && i < columnWidths.length; i++) {
                if (row[i] != null) {
                    columnWidths[i] = Math.max(columnWidths[i], row[i].length());
                }
            }
        }
        
        // Print headers
        printTableRow(headers, columnWidths);
        printTableSeparator(columnWidths);
        
        // Print data rows
        for (String[] row : data) {
            printTableRow(row, columnWidths);
        }
    }
    
    private void printTableRow(String[] row, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnWidths.length; i++) {
            String cell = (i < row.length && row[i] != null) ? row[i] : "";
            sb.append(String.format("%-" + columnWidths[i] + "s", cell));
            if (i < columnWidths.length - 1) {
                sb.append(" | ");
            }
        }
        System.out.println(sb.toString());
    }
    
    private void printTableSeparator(int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < columnWidths.length; i++) {
            sb.append("-".repeat(columnWidths[i]));
            if (i < columnWidths.length - 1) {
                sb.append("-+-");
            }
        }
        System.out.println(sb.toString());
    }
    
    public void printTransactionSummary(List<BankTransaction> transactions) {
        if (transactions.isEmpty()) {
            printInfo("No transactions to display");
            return;
        }
        
        printSubHeader("Transaction Summary");
        System.out.printf("Total Transactions: %d%n", transactions.size());
        
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        
        for (BankTransaction tx : transactions) {
            if (tx.getDebitAmount() != null) {
                totalDebits = totalDebits.add(tx.getDebitAmount());
            }
            if (tx.getCreditAmount() != null) {
                totalCredits = totalCredits.add(tx.getCreditAmount());
            }
        }
        
        System.out.printf("Total Debits: %,.2f%n", totalDebits);
        System.out.printf("Total Credits: %,.2f%n", totalCredits);
        System.out.printf("Net Amount: %,.2f%n", totalCredits.subtract(totalDebits));
    }
    
    public void printCompanyDetails(Company company) {
        if (company == null) {
            printError("No company details to display");
            return;
        }
        
        printSubHeader("Company Details");
        System.out.println("ID: " + company.getId());
        System.out.println("Name: " + company.getName());
        System.out.println("Registration Number: " + 
                (company.getRegistrationNumber() != null ? company.getRegistrationNumber() : "N/A"));
        System.out.println("Tax Number: " + 
                (company.getTaxNumber() != null ? company.getTaxNumber() : "N/A"));
        System.out.println("Address: " + 
                (company.getAddress() != null ? company.getAddress() : "N/A"));
        System.out.println("Contact Email: " + 
                (company.getContactEmail() != null ? company.getContactEmail() : "N/A"));
        System.out.println("Contact Phone: " + 
                (company.getContactPhone() != null ? company.getContactPhone() : "N/A"));
    }
    
    public void printFiscalPeriodDetails(FiscalPeriod fiscalPeriod) {
        if (fiscalPeriod == null) {
            printError("No fiscal period details to display");
            return;
        }
        
        printSubHeader("Fiscal Period Details");
        System.out.println("ID: " + fiscalPeriod.getId());
        System.out.println("Name: " + fiscalPeriod.getPeriodName());
        System.out.println("Start Date: " + 
                fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        System.out.println("End Date: " + 
                fiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        System.out.println("Status: " + (fiscalPeriod.isClosed() ? "Closed" : "Open"));
    }
    
    public void printTransactionTable(List<BankTransaction> transactions) {
        if (transactions.isEmpty()) {
            printInfo("No transactions to display");
            return;
        }
        
        System.out.printf("%-12s %-40s %-15s %-15s %-15s%n", "Date", "Details", "Debit", "Credit", "Balance");
        printSeparator();
        
        for (BankTransaction transaction : transactions) {
            String debit = transaction.getDebitAmount() != null ? 
                    String.format("%,.2f", transaction.getDebitAmount()) : "";
            String credit = transaction.getCreditAmount() != null ? 
                    String.format("%,.2f", transaction.getCreditAmount()) : "";
            String balance = transaction.getBalance() != null ? 
                    String.format("%,.2f", transaction.getBalance()) : "";
            
            // Truncate details if too long
            String details = transaction.getDetails();
            if (details != null && details.length() > 38) {
                details = details.substring(0, 35) + "...";
            }
            
            System.out.printf("%-12s %-40s %-15s %-15s %-15s%n", 
                    transaction.getTransactionDate(), 
                    details, 
                    debit, 
                    credit, 
                    balance);
        }
    }
    
    public void printCurrentContext(Company company, FiscalPeriod fiscalPeriod) {
        if (company != null || fiscalPeriod != null) {
            printSubHeader("Current Context");
            if (company != null) {
                System.out.println("Company: " + company.getName());
            }
            if (fiscalPeriod != null) {
                System.out.println("Fiscal Period: " + fiscalPeriod.getPeriodName() + 
                          " (" + fiscalPeriod.getStartDate() + " to " + 
                          fiscalPeriod.getEndDate() + ")");
            }
        }
    }
    
    public void printFileLocation(String description, String filePath) {
        System.out.println("📁 " + description + ": " + filePath);
    }
}
