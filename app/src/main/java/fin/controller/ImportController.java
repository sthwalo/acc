package fin.controller;

import fin.model.BankTransaction;
import fin.service.BankStatementProcessingService;
import fin.service.CsvImportService;
import fin.service.PdfExportService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Import management controller
 * Extracted from monolithic App.java import-related methods
 */
public class ImportController {
    private final BankStatementProcessingService bankStatementService;
    private final CsvImportService csvImportService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public ImportController(BankStatementProcessingService bankStatementService,
                          CsvImportService csvImportService,
                          ApplicationState applicationState,
                          ConsoleMenu menu,
                          InputHandler inputHandler,
                          OutputFormatter outputFormatter) {
        this.bankStatementService = bankStatementService;
        this.csvImportService = csvImportService;
        this.applicationState = applicationState;
        this.menu = menu;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }
    
    public void handleBankStatementImport() {
        try {
            applicationState.requireContext();
            
            boolean back = false;
            while (!back) {
                menu.displayImportMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, 3);
                
                switch (choice) {
                    case 1:
                        handleSingleBankStatementImport();
                        break;
                    case 2:
                        handleBatchBankStatementImport();
                        break;
                    case 3:
                        back = true;
                        break;
                    default:
                        outputFormatter.printError("Invalid choice. Please try again.");
                }
            }
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    public void handleSingleBankStatementImport() {
        outputFormatter.printHeader("Import Single Bank Statement");
        
        try {
            String filePath = inputHandler.getFilePath("Enter bank statement file path", ".pdf");
            
            // Process the bank statement
            outputFormatter.printProcessing("Processing bank statement: " + filePath);
            List<BankTransaction> transactions = bankStatementService.processStatement(
                filePath, applicationState.getCurrentCompany());
            
            outputFormatter.printSuccess("Successfully processed " + transactions.size() + " transactions");
            
            // Display transaction summary
            outputFormatter.printTransactionSummary(transactions);
            
            // Ask for CSV export
            if (inputHandler.getBoolean("Would you like to export the processed transactions to CSV?")) {
                exportTransactionsToCSV(transactions, "single");
            }
            
        } catch (Exception e) {
            outputFormatter.printError("Error processing bank statement: " + e.getMessage());
        }
    }
    
    public void handleBatchBankStatementImport() {
        outputFormatter.printHeader("Import Multiple Bank Statements");
        outputFormatter.printInfo("Enter file paths (one per line)");
        outputFormatter.printInfo("Enter an empty line when done");
        
        List<String> filePaths = new ArrayList<>();
        while (true) {
            String filePath = inputHandler.getString("File path (or empty to finish)", "");
            
            if (filePath.isEmpty()) {
                break;
            }
            
            if (inputHandler.isValidFilePath(filePath, ".pdf")) {
                filePaths.add(filePath);
                outputFormatter.printSuccess("Added: " + filePath);
            } else {
                outputFormatter.printError("File not found: " + filePath);
            }
        }
        
        if (filePaths.isEmpty()) {
            outputFormatter.printWarning("No valid files provided.");
            return;
        }
        
        try {
            List<BankTransaction> allTransactions = new ArrayList<>();
            
            // Process each bank statement with progress
            for (int i = 0; i < filePaths.size(); i++) {
                String filePath = filePaths.get(i);
                outputFormatter.printProgress("Processing files", i + 1, filePaths.size());
                
                List<BankTransaction> transactions = bankStatementService.processStatement(
                    filePath, applicationState.getCurrentCompany());
                allTransactions.addAll(transactions);
            }
            
            outputFormatter.printSuccess("Successfully processed " + allTransactions.size() + 
                    " transactions from " + filePaths.size() + " files");
            
            // Display transaction summary
            outputFormatter.printTransactionSummary(allTransactions);
            
            // Ask for CSV export
            if (inputHandler.getBoolean("Would you like to export the processed transactions to CSV?")) {
                exportTransactionsToCSV(allTransactions, "batch");
            }
            
        } catch (Exception e) {
            outputFormatter.printError("Error processing bank statements: " + e.getMessage());
        }
    }
    
    public void handleCsvImport() {
        try {
            applicationState.requireContext();
            
            outputFormatter.printHeader("Import CSV Data");
            String filePath = inputHandler.getFilePath("Enter CSV file path", ".csv");
            
            outputFormatter.printProcessing("Importing CSV file: " + filePath);
            List<BankTransaction> transactions = csvImportService.importCsvFile(
                filePath, 
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId());
            
            outputFormatter.printSuccess("Successfully imported " + transactions.size() + " transactions");
            outputFormatter.printTransactionSummary(transactions);
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error importing CSV file: " + e.getMessage());
        }
    }
    
    public void handleViewImportedData() {
        try {
            applicationState.requireContext();
            
            outputFormatter.printHeader("Imported Transactions");
            outputFormatter.printCurrentContext(
                applicationState.getCurrentCompany(), 
                applicationState.getCurrentFiscalPeriod());
            
            List<BankTransaction> transactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId());
            
            if (transactions.isEmpty()) {
                outputFormatter.printInfo("No transactions found for the selected company and fiscal period.");
                return;
            }
            
            outputFormatter.printInfo("Found " + transactions.size() + " transactions.");
            outputFormatter.printSeparator();
            outputFormatter.printPlain("Options:");
            outputFormatter.printPlain("1. View transactions in terminal");
            outputFormatter.printPlain("2. Export transactions to PDF");
            outputFormatter.printPlain("3. Back to main menu");
            
            int choice = inputHandler.getInteger("Enter your choice", 1, 3);
            
            switch (choice) {
                case 1:
                    outputFormatter.printTransactionTable(transactions);
                    inputHandler.waitForEnter();
                    break;
                case 2:
                    try {
                        PdfExportService pdfService = new PdfExportService();
                        String pdfPath = pdfService.exportTransactionsToPdf(
                            transactions, 
                            applicationState.getCurrentCompany(), 
                            applicationState.getCurrentFiscalPeriod());
                        outputFormatter.printSuccess("PDF exported successfully: " + pdfPath);
                    } catch (Exception e) {
                        outputFormatter.printError("PDF export failed: " + e.getMessage());
                    }
                    break;
                case 3:
                    return;
                default:
                    outputFormatter.printError("Invalid choice. Returning to main menu.");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error viewing imported data: " + e.getMessage());
        }
    }
    
    private void exportTransactionsToCSV(List<BankTransaction> transactions, String type) {
        try {
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = String.format("processed_statements_%s_%s_%s_%s.csv",
                type,
                applicationState.getCurrentCompany().getName().toLowerCase().replace(" ", "_"),
                applicationState.getCurrentFiscalPeriod().getPeriodName().toLowerCase(),
                timestamp);
            
            exportTransactions(transactions, filename);
            outputFormatter.printSuccess("Transactions exported to: " + filename);
            outputFormatter.printFileLocation("Export location", new File(filename).getAbsolutePath());
            
        } catch (IOException e) {
            outputFormatter.printError("Error exporting transactions: " + e.getMessage());
        }
    }
    
    @SuppressWarnings("FS")
    private void exportTransactions(List<BankTransaction> transactions, String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath, java.nio.charset.StandardCharsets.UTF_8)) {
            // Write header
            writer.write("Date,Details,Debit Amount,Credit Amount,Balance,Account Number%n");
            
            // Write transactions
            for (BankTransaction tx : transactions) {
                String formattedLine = String.format("%s,\"%s\",%s,%s,%s,%s%n",
                    tx.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    tx.getDetails() != null ? tx.getDetails().replace("\"", "\"\"") : "",
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : "",
                    tx.getCreditAmount() != null ? tx.getCreditAmount() : "",
                    tx.getBalance() != null ? tx.getBalance() : "",
                    tx.getAccountNumber() != null ? tx.getAccountNumber() : ""
                );
                writer.write(formattedLine);
            }
        }
    }
}
