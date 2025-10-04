package fin.controller;

import fin.model.Account;
import fin.model.BankTransaction;
import fin.model.JournalEntryLine;
import fin.service.TransactionClassificationService;
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
    private final TransactionClassificationService classificationService;
    private final CsvExportService csvExportService;
    private final CsvImportService csvImportService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public DataManagementController(DataManagementService dataManagementService,
                                  TransactionClassificationService classificationService,
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
                int choice = inputHandler.getInteger("Enter your choice", 1, 7);
                
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
                        handleTransactionHistory();
                        break;
                    case 5:
                        handleDataReset();
                        break;
                    case 6:
                        handleExportToCSV();
                        break;
                    case 7:
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
                int choice = inputHandler.getInteger("Enter your choice", 1, 7);
                
                switch (choice) {
                    case 1:
                        // Interactive Classification (new transactions)
                        classificationService.runInteractiveClassification(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        break;
                    case 2:
                        // Auto-Classify Transactions
                        int classifiedCount = classificationService.autoClassifyTransactions(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        if (classifiedCount > 0) {
                            outputFormatter.printSuccess("Auto-classified " + classifiedCount + " transactions");
                        }
                        break;
                    case 3:
                        // Re-classify Transactions (fix existing) - moved from top-level Option 4
                        handleTransactionCorrection();
                        break;
                    case 4:
                        // Initialize Chart of Accounts
                        handleChartOfAccountsInitialization();
                        break;
                    case 5:
                        // Initialize Mapping Rules - moved from top-level Option 8
                        handleInitializeMappingRules();
                        break;
                    case 6:
                        // Generate Journal Entries (renamed from Synchronize)
                        int syncCount = classificationService.synchronizeJournalEntries(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        if (syncCount > 0) {
                            outputFormatter.printSuccess("Generated " + syncCount + " journal entries");
                        }
                        break;
                    case 7:
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
            
            List<BankTransaction> allTransactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            
            if (allTransactions.isEmpty()) {
                outputFormatter.printInfo("No transactions found to correct.");
                return;
            }
            
            // Enhanced: Show filtering options
            System.out.println("\n📊 Total Transactions: " + allTransactions.size());
            System.out.println("\nFilter Options:");
            System.out.println("1. Show All Transactions");
            System.out.println("2. Show Uncategorized Only");
            System.out.println("3. Show Categorized Only");
            System.out.println("4. Back to Data Management");
            
            int filterChoice = inputHandler.getInteger("Select filter option", 1, 4);
            
            if (filterChoice == 4) {
                return;
            }
            
            List<BankTransaction> transactions = filterTransactions(allTransactions, filterChoice);
            
            if (transactions.isEmpty()) {
                outputFormatter.printInfo("No transactions match the selected filter.");
                return;
            }
            
            // Enhanced: Pagination for all transactions
            final int TRANSACTIONS_PER_PAGE = 50;
            int totalPages = (int) Math.ceil((double) transactions.size() / TRANSACTIONS_PER_PAGE);
            int currentPage = 1;
            
            while (true) {
                outputFormatter.printSubHeader("Transactions (Page " + currentPage + "/" + totalPages + ")");
                
                int startIdx = (currentPage - 1) * TRANSACTIONS_PER_PAGE;
                int endIdx = Math.min(startIdx + TRANSACTIONS_PER_PAGE, transactions.size());
                
                for (int i = startIdx; i < endIdx; i++) {
                    BankTransaction tx = transactions.get(i);
                    String status = (tx.getAccountCode() != null && !tx.getAccountCode().isEmpty()) ? "✓" : "⚠️";
                    System.out.printf("%d. %s [%s] %s - Amount: %s%n", 
                        i + 1,
                        status,
                        tx.getTransactionDate(), 
                        tx.getDetails().length() > 50 ? tx.getDetails().substring(0, 47) + "..." : tx.getDetails(),
                        tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
                }
                
                System.out.println("\nNavigation:");
                System.out.println("0. Go back");
                if (currentPage > 1) {
                    System.out.println("P. Previous page");
                }
                if (currentPage < totalPages) {
                    System.out.println("N. Next page");
                }
                System.out.println("Or enter transaction number to correct (1-" + transactions.size() + ")");
                
                String input = inputHandler.getString("Your choice");
                
                if (input.equalsIgnoreCase("0")) {
                    return;
                } else if (input.equalsIgnoreCase("P") && currentPage > 1) {
                    currentPage--;
                    continue;
                } else if (input.equalsIgnoreCase("N") && currentPage < totalPages) {
                    currentPage++;
                    continue;
                }
                
                try {
                    int txIndex = Integer.parseInt(input) - 1;
                    if (txIndex >= 0 && txIndex < transactions.size()) {
                        correctSingleTransaction(transactions.get(txIndex));
                        
                        // Refresh transactions after correction
                        allTransactions = csvImportService.getTransactions(
                            applicationState.getCurrentCompany().getId(),
                            applicationState.getCurrentFiscalPeriod().getId());
                        transactions = filterTransactions(allTransactions, filterChoice);
                        totalPages = (int) Math.ceil((double) transactions.size() / TRANSACTIONS_PER_PAGE);
                        
                        // Stay on same page if possible
                        if (currentPage > totalPages) {
                            currentPage = totalPages > 0 ? totalPages : 1;
                        }
                    } else {
                        outputFormatter.printError("Invalid transaction number");
                    }
                } catch (NumberFormatException e) {
                    outputFormatter.printError("Invalid input. Please enter a number or P/N/0");
                }
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error correcting transaction: " + e.getMessage());
        }
    }
    
    private List<BankTransaction> filterTransactions(List<BankTransaction> transactions, int filterChoice) {
        switch (filterChoice) {
            case 2: // Uncategorized only
                return transactions.stream()
                    .filter(tx -> tx.getAccountCode() == null || tx.getAccountCode().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            case 3: // Categorized only
                return transactions.stream()
                    .filter(tx -> tx.getAccountCode() != null && !tx.getAccountCode().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            default: // All transactions
                return transactions;
        }
    }
    
    private void correctSingleTransaction(BankTransaction tx) throws Exception {
        outputFormatter.printSubHeader("Correcting Transaction");
        System.out.println("Date: " + tx.getTransactionDate());
        System.out.println("Description: " + tx.getDetails());
        System.out.println("Amount: " + (tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount()));
        if (tx.getAccountCode() != null && !tx.getAccountCode().isEmpty()) {
            System.out.println("Current Classification: [" + tx.getAccountCode() + "] " + tx.getAccountName());
        }
        
        // Enhanced: Show intelligent suggestions from AccountClassificationService
        showIntelligentSuggestions(tx);
        
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
            
            // Enhanced: Ask about similar transactions
            askAboutSimilarTransactions(tx, newAccountId, reason, correctedBy);
        }
    }
    
    private void showIntelligentSuggestions(BankTransaction tx) {
        System.out.println("\n💡 Intelligent Suggestions:");
        
        try {
            // Get all accounts via the service
            List<Account> accounts = csvImportService.getAccountService()
                .getAccountsByCompany(applicationState.getCurrentCompany().getId());
            
            // Simple keyword matching for suggestions
            String description = tx.getDetails().toLowerCase();
            List<Account> suggestions = new ArrayList<>();
            
            // Match keywords in description with account names
            for (Account account : accounts) {
                if (account.getAccountName() != null) {
                    String accountName = account.getAccountName().toLowerCase();
                    String[] accountWords = accountName.split(" ");
                    
                    for (String word : accountWords) {
                        if (word.length() > 3 && description.contains(word)) {
                            suggestions.add(account);
                            break;
                        }
                    }
                    
                    if (suggestions.size() >= 5) break;
                }
            }
            
            if (!suggestions.isEmpty()) {
                System.out.println("Top matches based on description:");
                for (int i = 0; i < suggestions.size(); i++) {
                    Account account = suggestions.get(i);
                    System.out.printf("  %d. [%s] %s%n", 
                        i + 1, 
                        account.getAccountCode(), 
                        account.getAccountName());
                }
            } else {
                System.out.println("  No automatic suggestions - select from full account list");
            }
        } catch (Exception e) {
            System.out.println("  (Suggestions unavailable: " + e.getMessage() + ")");
        }
    }
    
    private void askAboutSimilarTransactions(BankTransaction tx, Long newAccountId, String reason, String correctedBy) {
        try {
            List<BankTransaction> allTransactions = csvImportService.getTransactions(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            
            // Find similar uncategorized transactions
            String pattern = extractKeyPattern(tx.getDetails());
            List<BankTransaction> similarTransactions = allTransactions.stream()
                .filter(t -> !t.getId().equals(tx.getId()))
                .filter(t -> t.getAccountCode() == null || t.getAccountCode().isEmpty())
                .filter(t -> t.getDetails().toLowerCase().contains(pattern.toLowerCase()))
                .limit(20)
                .collect(java.util.stream.Collectors.toList());
            
            if (!similarTransactions.isEmpty()) {
                System.out.println("\n📋 Found " + similarTransactions.size() + " similar uncategorized transactions:");
                for (int i = 0; i < Math.min(5, similarTransactions.size()); i++) {
                    BankTransaction similar = similarTransactions.get(i);
                    System.out.printf("  %d. [%s] %s - Amount: %s%n",
                        i + 1,
                        similar.getTransactionDate(),
                        similar.getDetails().length() > 50 ? similar.getDetails().substring(0, 47) + "..." : similar.getDetails(),
                        similar.getDebitAmount() != null ? similar.getDebitAmount() : similar.getCreditAmount());
                }
                
                String bulkChoice = inputHandler.getString("Apply this classification to all similar transactions? (y/n)");
                if (bulkChoice.equalsIgnoreCase("y")) {
                    int count = 0;
                    for (BankTransaction similar : similarTransactions) {
                        try {
                            dataManagementService.correctTransactionCategory(
                                applicationState.getCurrentCompany().getId(),
                                similar.getId(),
                                similar.getBankAccountId(),
                                newAccountId,
                                reason + " (Bulk correction)",
                                correctedBy);
                            count++;
                        } catch (Exception e) {
                            System.out.println("  ⚠️ Failed to correct transaction " + similar.getId() + ": " + e.getMessage());
                        }
                    }
                    outputFormatter.printSuccess("Bulk corrected " + count + " similar transactions!");
                }
            }
        } catch (Exception e) {
            System.out.println("  (Bulk correction unavailable: " + e.getMessage() + ")");
        }
    }
    
    private String extractKeyPattern(String description) {
        // Extract key pattern from transaction description
        String[] words = description.split("\\s+");
        if (words.length > 0) {
            // Return first meaningful word (longer than 3 characters)
            for (String word : words) {
                if (word.length() > 3) {
                    return word;
                }
            }
            return words[0];
        }
        return description.length() > 10 ? description.substring(0, 10) : description;
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
