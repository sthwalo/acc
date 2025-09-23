package fin.controller;

import fin.model.Account;
import fin.model.BankTransaction;
import fin.model.JournalEntryLine;
import fin.service.ClassificationIntegrationService;
import fin.service.CsvExportService;
import fin.service.CsvImportService;
import fin.service.DataManagementService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Data management controller
 * Extracted from monolithic App.java data management-related methods
 */
public class DataManagementController {
    private final DataManagementService dataManagementService;
    private final ClassificationIntegrationService classificationService;
    private final CsvExportService csvExportService;
    private final CsvImportService csvImportService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public DataManagementController(DataManagementService dataManagementService,
                                  ClassificationIntegrationService classificationService,
                                  CsvExportService csvExportService,
                                  CsvImportService csvImportService,
                                  ApplicationState applicationState,
                                  ConsoleMenu menu,
                                  InputHandler inputHandler,
                                  OutputFormatter outputFormatter) {
        this.dataManagementService = dataManagementService;
        this.classificationService = classificationService;
        this.csvExportService = csvExportService;
        this.csvImportService = csvImportService;
        this.applicationState = applicationState;
        this.menu = menu;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }
    
    public void handleDataManagement() {
        try {
            applicationState.requireCompany();
            
            boolean back = false;
            while (!back) {
                menu.displayDataManagementMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, 9);
                
                switch (choice) {
                    case 1:
                        handleManualInvoiceCreation();
                        break;
                    case 2:
                        handleJournalEntryCreation();
                        break;
                    case 3:
                        handleTransactionClassification();
                        break;
                    case 4:
                        handleTransactionCorrection();
                        break;
                    case 5:
                        handleTransactionHistory();
                        break;
                    case 6:
                        handleDataReset();
                        break;
                    case 7:
                        handleExportToCSV();
                        break;
                    case 8:
                        handleInitializeMappingRules();
                        break;
                    case 9:
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
    
    public void handleManualInvoiceCreation() {
        outputFormatter.printHeader("Create Manual Invoice");
        
        try {
            applicationState.requireFiscalPeriod();
            
            String invoiceNumber = inputHandler.getString("Enter invoice number");
            LocalDate invoiceDate = inputHandler.getDate("Enter invoice date");
            String description = inputHandler.getString("Enter description");
            BigDecimal amount = inputHandler.getBigDecimal("Enter amount");
            
            // Account selection
            outputFormatter.printSubHeader("Select Debit Account");
            Long debitAccountId = selectAccount();
            
            outputFormatter.printSubHeader("Select Credit Account");
            Long creditAccountId = selectAccount();
            
            if (debitAccountId != null && creditAccountId != null) {
                dataManagementService.createManualInvoice(
                    applicationState.getCurrentCompany().getId(),
                    invoiceNumber,
                    invoiceDate,
                    description,
                    amount,
                    debitAccountId,
                    creditAccountId,
                    applicationState.getCurrentFiscalPeriod().getId());
                
                outputFormatter.printSuccess("Invoice created successfully!");
            } else {
                outputFormatter.printError("Account selection required for both debit and credit accounts");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error creating invoice: " + e.getMessage());
        }
    }
    
    public void handleJournalEntryCreation() {
        outputFormatter.printHeader("Create Journal Entry");
        
        try {
            applicationState.requireFiscalPeriod();
            
            String entryNumber = inputHandler.getString("Enter entry number");
            LocalDate entryDate = inputHandler.getDate("Enter entry date");
            String description = inputHandler.getString("Enter description");
            
            List<JournalEntryLine> lines = new ArrayList<>();
            boolean addMoreLines = true;
            
            while (addMoreLines) {
                outputFormatter.printSubHeader("Add Journal Entry Line");
                JournalEntryLine line = new JournalEntryLine();
                
                outputFormatter.printInfo("Select account:");
                line.setAccountId(selectAccount());
                
                if (line.getAccountId() == null) {
                    outputFormatter.printError("Account selection is required");
                    continue;
                }
                
                line.setDescription(inputHandler.getString("Enter line description"));
                
                boolean isDebit = inputHandler.getBoolean("Is this a debit entry?");
                BigDecimal amount = inputHandler.getBigDecimal("Enter amount");
                
                if (isDebit) {
                    line.setDebitAmount(amount);
                } else {
                    line.setCreditAmount(amount);
                }
                
                lines.add(line);
                
                addMoreLines = inputHandler.getBoolean("Add another line?");
            }
            
            if (!lines.isEmpty()) {
                dataManagementService.createJournalEntry(
                    applicationState.getCurrentCompany().getId(),
                    entryNumber,
                    entryDate,
                    description,
                    applicationState.getCurrentFiscalPeriod().getId(),
                    lines);
                
                outputFormatter.printSuccess("Journal entry created successfully!");
            } else {
                outputFormatter.printWarning("No journal entry lines were added");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error creating journal entry: " + e.getMessage());
        }
    }
    
    public void handleTransactionClassification() {
        try {
            applicationState.requireContext();
            
            boolean back = false;
            while (!back) {
                menu.displayTransactionClassificationMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, 5);
                
                switch (choice) {
                    case 1:
                        classificationService.runInteractiveClassification(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        break;
                    case 2:
                        int classifiedCount = classificationService.autoClassifyTransactions(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        if (classifiedCount > 0) {
                            outputFormatter.printSuccess("Auto-classified " + classifiedCount + " transactions");
                        }
                        break;
                    case 3:
                        handleChartOfAccountsInitialization();
                        break;
                    case 4:
                        int syncCount = classificationService.synchronizeJournalEntries(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        if (syncCount > 0) {
                            outputFormatter.printSuccess("Synchronized " + syncCount + " journal entries");
                        }
                        break;
                    case 5:
                        back = true;
                        break;
                    default:
                        outputFormatter.printError("Invalid choice");
                }
            }
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    private void handleChartOfAccountsInitialization() {
        outputFormatter.printSubHeader("Chart of Accounts Initialization");
        
        boolean back = false;
        while (!back) {
            menu.displayAccountInitializationMenu();
            int choice = inputHandler.getInteger("Enter choice", 1, 4);
            
            switch (choice) {
                case 1:
                    boolean success = classificationService.initializeChartOfAccounts(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Chart of Accounts initialized successfully");
                    }
                    break;
                case 2:
                    success = classificationService.initializeTransactionMappingRules(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Transaction Mapping Rules initialized successfully");
                    }
                    break;
                case 3:
                    success = classificationService.performFullInitialization(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Full initialization completed successfully");
                    }
                    break;
                case 4:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice");
            }
        }
    }
    
    public void handleTransactionCorrection() {
        outputFormatter.printHeader("Correct Transaction Categorization");
        
        try {
            applicationState.requireFiscalPeriod();
            
            List<BankTransaction> transactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            
            if (transactions.isEmpty()) {
                outputFormatter.printInfo("No transactions found to correct.");
                return;
            }
            
            outputFormatter.printSubHeader("Recent Transactions");
            for (int i = 0; i < Math.min(transactions.size(), 20); i++) {
                BankTransaction tx = transactions.get(i);
                System.out.printf("%d. [%s] %s - Amount: %s%n", i + 1,
                    tx.getTransactionDate(), 
                    tx.getDetails().length() > 50 ? tx.getDetails().substring(0, 47) + "..." : tx.getDetails(),
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
            }
            
            int txIndex = inputHandler.getInteger("Select transaction number to correct", 1, 
                Math.min(transactions.size(), 20)) - 1;
            
            BankTransaction tx = transactions.get(txIndex);
            
            outputFormatter.printSubHeader("Select New Account for Categorization");
            Long newAccountId = selectAccount();
            
            if (newAccountId != null) {
                String reason = inputHandler.getString("Enter reason for correction");
                String correctedBy = inputHandler.getString("Enter your name");
                
                dataManagementService.correctTransactionCategory(
                    applicationState.getCurrentCompany().getId(),
                    tx.getId(),
                    tx.getBankAccountId(),
                    newAccountId,
                    reason,
                    correctedBy);
                
                outputFormatter.printSuccess("Transaction categorization corrected successfully!");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error correcting transaction: " + e.getMessage());
        }
    }
    
    public void handleTransactionHistory() {
        outputFormatter.printHeader("View Transaction History");
        
        try {
            applicationState.requireFiscalPeriod();
            
            List<BankTransaction> transactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            
            if (transactions.isEmpty()) {
                outputFormatter.printInfo("No transactions found.");
                return;
            }
            
            outputFormatter.printSubHeader("Recent Transactions");
            for (int i = 0; i < Math.min(transactions.size(), 10); i++) {
                BankTransaction tx = transactions.get(i);
                System.out.printf("%d. [%s] %s - Amount: %s%n", i + 1,
                    tx.getTransactionDate(), 
                    tx.getDetails().length() > 50 ? tx.getDetails().substring(0, 47) + "..." : tx.getDetails(),
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
            }
            
            int txIndex = inputHandler.getInteger("Select transaction number to view history", 1, 
                Math.min(transactions.size(), 10)) - 1;
            
            BankTransaction tx = transactions.get(txIndex);
            List<Map<String, Object>> history = 
                dataManagementService.getTransactionCorrectionHistory(tx.getId());
            
            if (history.isEmpty()) {
                outputFormatter.printInfo("No correction history found for this transaction.");
            } else {
                outputFormatter.printSubHeader("Correction History");
                for (Map<String, Object> correction : history) {
                    System.out.printf("Date: %s%n", correction.get("correctedAt"));
                    System.out.printf("By: %s%n", correction.get("correctedBy"));
                    System.out.printf("From: %s (%s)%n", 
                        correction.get("originalAccountName"),
                        correction.get("originalAccountCode"));
                    System.out.printf("To: %s (%s)%n",
                        correction.get("newAccountName"),
                        correction.get("newAccountCode"));
                    System.out.printf("Reason: %s%n", correction.get("reason"));
                    outputFormatter.printSeparator();
                }
            }
            
            inputHandler.waitForEnter();
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error viewing transaction history: " + e.getMessage());
        }
    }
    
    public void handleDataReset() {
        outputFormatter.printHeader("Reset Company Data");
        outputFormatter.printWarning("This will delete transaction data for the current company.");
        
        outputFormatter.printPlain("Reset options:");
        outputFormatter.printPlain("1. Reset only transaction data");
        outputFormatter.printPlain("2. Reset all data (including chart of accounts)");
        outputFormatter.printPlain("3. Cancel");
        
        int choice = inputHandler.getInteger("Enter choice", 1, 3);
        
        if (choice == 3) {
            outputFormatter.printInfo("Reset operation cancelled");
            return;
        }
        
        String confirmation = inputHandler.getConfirmation("Type 'CONFIRM' to proceed", "CONFIRM");
        
        if ("CONFIRM".equals(confirmation)) {
            try {
                boolean preserveMasterData = (choice == 1);
                dataManagementService.resetCompanyData(
                    applicationState.getCurrentCompany().getId(), 
                    preserveMasterData);
                
                outputFormatter.printSuccess("Data reset completed successfully!");
                
            } catch (Exception e) {
                outputFormatter.printError("Error resetting data: " + e.getMessage());
            }
        } else {
            outputFormatter.printInfo("Reset operation cancelled");
        }
    }
    
    public void handleExportToCSV() {
        try {
            applicationState.requireContext();
            
            List<BankTransaction> transactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            
            if (transactions.isEmpty()) {
                outputFormatter.printInfo("No transactions found to export.");
                return;
            }
            
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = String.format("exported_transactions_%s_%s_%s.csv",
                applicationState.getCurrentCompany().getName().toLowerCase().replace(" ", "_"),
                applicationState.getCurrentFiscalPeriod().getPeriodName().toLowerCase(),
                timestamp);
            
            outputFormatter.printProcessing("Exporting " + transactions.size() + " transactions to CSV...");
            csvExportService.exportTransactionsToCsv(transactions, filename, 
                applicationState.getCurrentFiscalPeriod().getId());
            
            outputFormatter.printSuccess("Transactions exported successfully");
            outputFormatter.printFileLocation("Export file", new File(filename).getAbsolutePath());
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error exporting transactions: " + e.getMessage());
        }
    }
    
    private void handleInitializeMappingRules() {
        outputFormatter.printHeader("Initialize Mapping Rules");
        
        try {
            boolean success = classificationService.initializeTransactionMappingRules(
                applicationState.getCurrentCompany().getId());
            
            if (success) {
                outputFormatter.printSuccess("Transaction Mapping Rules initialized successfully");
                
                // Optionally run classification
                int classified = classificationService.autoClassifyTransactions(
                    applicationState.getCurrentCompany().getId(),
                    applicationState.getCurrentFiscalPeriod().getId());
                outputFormatter.printSuccess("Auto-classified " + classified + " transactions");
            } else {
                outputFormatter.printError("Failed to initialize mapping rules");
            }
            
        } catch (Exception e) {
            outputFormatter.printError("Failed to initialize mapping rules: " + e.getMessage());
        }
        
        outputFormatter.printInfo("Press Enter to continue...");
        inputHandler.waitForEnter();
    }
    
    private Long selectAccount() {
        List<Account> accounts = csvImportService.getAccountService()
            .getAccountsByCompany(applicationState.getCurrentCompany().getId());
        
        if (accounts.isEmpty()) {
            outputFormatter.printWarning("No accounts found.");
            return null;
        }
        
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. [%s] %s%n", i + 1, account.getAccountCode(), account.getAccountName());
        }
        
        int selection = inputHandler.getInteger("Select account number", 1, accounts.size());
        return accounts.get(selection - 1).getId();
    }
}
