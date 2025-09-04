package fin.service;

import fin.model.BankTransaction;
import fin.model.FiscalPeriod;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CsvExportService {
    private final CompanyService companyService;
    
    public CsvExportService(CompanyService companyService) {
        this.companyService = companyService;
    }
    
    public void exportTransactionsToCsv(List<BankTransaction> transactions, String outputPath, Long fiscalPeriodId) {
        FiscalPeriod fiscalPeriod = companyService.getFiscalPeriodById(fiscalPeriodId);
        if (fiscalPeriod == null) {
            throw new RuntimeException("Fiscal period not found: " + fiscalPeriodId);
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            // Write header
            writer.println("Date,Details,Debits,Credits,Balance,ServiceFee,AccountNumber,StatementPeriod,source_file,FiscalPeriod");
            
            // Format for currency values (no thousands separator, 2 decimal places)
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");
            
            // Write transactions
            for (BankTransaction transaction : transactions) {
                StringBuilder line = new StringBuilder();
                
                // Date (DD/MM format)
                line.append(transaction.getTransactionDate().format(dateFormatter)).append(",");
                
                // Details (escape quotes and commas)
                line.append(escapeField(transaction.getDetails())).append(",");
                
                // Debits
                line.append(formatAmount(transaction.getDebitAmount())).append(",");
                
                // Credits
                line.append(formatAmount(transaction.getCreditAmount())).append(",");
                
                // Balance
                line.append(formatAmount(transaction.getBalance())).append(",");
                
                // Service Fee
                line.append(transaction.isServiceFee() ? "Y" : "").append(",");
                
                // Account Number
                line.append(escapeField(transaction.getAccountNumber())).append(",");
                
                // Statement Period
                line.append(escapeField(transaction.getStatementPeriod())).append(",");
                
                // Source File
                line.append(escapeField(transaction.getSourceFile())).append(",");
                
                // Fiscal Period
                line.append(fiscalPeriod.getPeriodName());
                
                writer.println(line);
            }
            
            System.out.println("CSV export completed successfully.");
            System.out.println("Total transactions exported: " + transactions.size());
            
            // Calculate and display totals
            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;
            
            for (BankTransaction transaction : transactions) {
                if (transaction.getDebitAmount() != null) {
                    totalDebits = totalDebits.add(transaction.getDebitAmount());
                }
                if (transaction.getCreditAmount() != null) {
                    totalCredits = totalCredits.add(transaction.getCreditAmount());
                }
            }
            
            System.out.println("\nSummary:");
            System.out.println("Total Debits: " + formatAmount(totalDebits));
            System.out.println("Total Credits: " + formatAmount(totalCredits));
            
        } catch (IOException e) {
            System.err.println("Error exporting to CSV: " + e.getMessage());
            throw new RuntimeException("Failed to export transactions to CSV", e);
        }
    }
    
    private String escapeField(String field) {
        if (field == null) return "";
        // Replace quotes with double quotes and wrap in quotes if contains comma or newline
        String escaped = field.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "";
        return String.format(Locale.US, "%.2f", amount);
    }
}
