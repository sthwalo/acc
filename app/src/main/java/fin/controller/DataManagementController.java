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
    // Menu choice constants
    private static final int MENU_CHOICE_MANUAL_INVOICE = 1;
    private static final int MENU_CHOICE_JOURNAL_ENTRY = 2;
    private static final int MENU_CHOICE_TRANSACTION_CLASSIFICATION = 3;
    private static final int MENU_CHOICE_TRANSACTION_CORRECTION = 4;
    private static final int MENU_CHOICE_TRANSACTION_HISTORY = 5;
    private static final int MENU_CHOICE_DATA_RESET = 6;
    private static final int MENU_CHOICE_EXPORT_CSV = 7;
    private static final int MENU_CHOICE_BACK = 8;
    
    // Transaction classification menu choices
    private static final int CLASSIFICATION_CHOICE_INTERACTIVE = 1;
    private static final int CLASSIFICATION_CHOICE_AUTO_CLASSIFY = 2;
    private static final int CLASSIFICATION_CHOICE_RECLASSIFY_ALL = 3;
    private static final int CLASSIFICATION_CHOICE_INIT_CHART = 4;
    private static final int CLASSIFICATION_CHOICE_SYNC_JOURNAL = 5;
    private static final int CLASSIFICATION_CHOICE_REGENERATE_JOURNAL = 6;
    private static final int CLASSIFICATION_CHOICE_BACK = 7;
    
    // Account initialization menu choices
    private static final int INIT_CHOICE_CHART_OF_ACCOUNTS = 1;
    private static final int INIT_CHOICE_MAPPING_RULES = 2;
    private static final int INIT_CHOICE_FULL_INIT = 3;
    private static final int INIT_CHOICE_BACK = 4;
    
    // Transaction correction filter choices
    private static final int FILTER_CHOICE_ALL = 1;
    private static final int FILTER_CHOICE_UNCATEGORIZED = 2;
    private static final int FILTER_CHOICE_CATEGORIZED = 3;
    private static final int FILTER_CHOICE_BACK = 4;
    
    // Data reset choices
    private static final int RESET_CHOICE_TRANSACTIONS_ONLY = 1;
    private static final int RESET_CHOICE_ALL_DATA = 2;
    private static final int RESET_CHOICE_CANCEL = 3;
    
    // Display constants
    private static final int TRANSACTIONS_PER_PAGE = 50;
    private static final int MAX_DESCRIPTION_LENGTH = 50;
    private static final int TRUNCATED_DESCRIPTION_LENGTH = 47;
    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_SIMILAR_TRANSACTIONS_DISPLAY = 5;
    private static final int MAX_RECENT_TRANSACTIONS = 10;
    private static final int MIN_KEYWORD_LENGTH = 3;
    private static final int MAX_SIMILAR_TRANSACTIONS = 20;
    
    // Menu bounds constants
    private static final int MAX_FILTER_CHOICE = 4;
    private static final int MAX_RESET_CHOICE = 3;
    
    // Pattern extraction constants
    private static final int MIN_WORD_LENGTH = 3;
    private static final int MAX_DESCRIPTION_PATTERN_LENGTH = 10;
    
    private final DataManagementService dataManagementService;
    private final TransactionClassificationService classificationService;
    private final CsvExportService csvExportService;
    private final CsvImportService csvImportService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public DataManagementController(DataManagementService initialDataManagementService,
                                  TransactionClassificationService initialClassificationService,
                                  CsvExportService initialCsvExportService,
                                  CsvImportService initialCsvImportService,
                                  ApplicationState initialApplicationState,
                                  ConsoleMenu initialMenu,
                                  InputHandler initialInputHandler,
                                  OutputFormatter initialOutputFormatter) {
        this.dataManagementService = initialDataManagementService;
        this.classificationService = initialClassificationService;
        this.csvExportService = initialCsvExportService;
        this.csvImportService = initialCsvImportService;
        this.applicationState = initialApplicationState;
        this.menu = initialMenu;
        this.inputHandler = initialInputHandler;
        this.outputFormatter = initialOutputFormatter;
    }
    
    /**
     * Handles the data management menu and user interactions.
     * This method is designed for extension by subclasses that need to customize
     * the data management workflow while maintaining the core menu structure.
     * Subclasses can override individual handler methods to customize behavior.
     */
    public void handleDataManagement() {
        try {
            applicationState.requireCompany();
            
            boolean back = false;
            while (!back) {
                menu.displayDataManagementMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, MENU_CHOICE_BACK);
                
                switch (choice) {
                    case MENU_CHOICE_MANUAL_INVOICE:
                        handleManualInvoiceCreation();
                        break;
                    case MENU_CHOICE_JOURNAL_ENTRY:
                        handleJournalEntryCreation();
                        break;
                    case MENU_CHOICE_TRANSACTION_CLASSIFICATION:
                        handleTransactionClassification();
                        break;
                    case MENU_CHOICE_TRANSACTION_CORRECTION:
                        handleTransactionCorrection();
                        break;
                    case MENU_CHOICE_TRANSACTION_HISTORY:
                        handleTransactionHistory();
                        break;
                    case MENU_CHOICE_DATA_RESET:
                        handleDataReset();
                        break;
                    case MENU_CHOICE_EXPORT_CSV:
                        handleExportToCSV();
                        break;
                    case MENU_CHOICE_BACK:
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
    
    /**
     * Handles manual invoice creation process.
     * This method is designed for extension by subclasses that need to customize
     * invoice creation while maintaining the core validation and service calls.
     */
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
    
    /**
     * Handles journal entry creation process.
     * This method is designed for extension by subclasses that need to customize
     * journal entry creation while maintaining the core validation and service calls.
     */
    public void handleJournalEntryCreation() {
        outputFormatter.printHeader("Create Journal Entry");
        
        try {
            applicationState.requireFiscalPeriod();
            performJournalEntryCreation();
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error creating journal entry: " + e.getMessage());
        }
    }

    /**
     * Performs the core journal entry creation logic.
     * This method can be overridden by subclasses to customize the creation process.
     */
    protected void performJournalEntryCreation() throws Exception {
        JournalEntryData entryData = collectJournalEntryDetails();
        List<JournalEntryLine> lines = collectJournalEntryLines();
        
        if (!lines.isEmpty()) {
            createJournalEntryFromData(entryData, lines);
            outputFormatter.printSuccess("Journal entry created successfully!");
        } else {
            outputFormatter.printWarning("No journal entry lines were added");
        }
    }
    
    private JournalEntryData collectJournalEntryDetails() {
        JournalEntryData data = new JournalEntryData();
        data.entryNumber = inputHandler.getString("Enter entry number");
        data.entryDate = inputHandler.getDate("Enter entry date");
        data.description = inputHandler.getString("Enter description");
        return data;
    }
    
    private List<JournalEntryLine> collectJournalEntryLines() {
        List<JournalEntryLine> lines = new ArrayList<>();
        boolean addMoreLines = true;
        
        while (addMoreLines) {
            JournalEntryLine line = collectSingleJournalEntryLine();
            if (line != null) {
                lines.add(line);
            }
            addMoreLines = inputHandler.getBoolean("Add another line?");
        }
        
        return lines;
    }
    
    private JournalEntryLine collectSingleJournalEntryLine() {
        outputFormatter.printSubHeader("Add Journal Entry Line");
        JournalEntryLine line = new JournalEntryLine();
        
        outputFormatter.printInfo("Select account:");
        line.setAccountId(selectAccount());
        
        if (line.getAccountId() == null) {
            outputFormatter.printError("Account selection is required");
            return null;
        }
        
        line.setDescription(inputHandler.getString("Enter line description"));
        
        boolean isDebit = inputHandler.getBoolean("Is this a debit entry?");
        BigDecimal amount = inputHandler.getBigDecimal("Enter amount");
        
        if (isDebit) {
            line.setDebitAmount(amount);
        } else {
            line.setCreditAmount(amount);
        }
        
        return line;
    }
    
    private void createJournalEntryFromData(JournalEntryData entryData, List<JournalEntryLine> lines) throws Exception {
        dataManagementService.createJournalEntry(
            applicationState.getCurrentCompany().getId(),
            entryData.entryNumber,
            entryData.entryDate,
            entryData.description,
            applicationState.getCurrentFiscalPeriod().getId(),
            lines);
    }
    
    private static class JournalEntryData {
        String entryNumber;
        LocalDate entryDate;
        String description;
    }
    
    /**
     * Handles transaction classification menu and operations.
     * This method is designed for extension by subclasses that need to customize
     * the classification workflow while maintaining the core menu structure.
     */
    public void handleTransactionClassification() {
        try {
            applicationState.requireContext();
            runClassificationMenu();
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }

    /**
     * Runs the classification menu loop.
     * This method can be overridden by subclasses to customize menu behavior.
     */
    protected void runClassificationMenu() {
        boolean back = false;
        while (!back) {
            menu.displayTransactionClassificationMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, CLASSIFICATION_CHOICE_BACK);
            back = processClassificationChoice(choice);
        }
    }

    private boolean processClassificationChoice(int choice) {
        switch (choice) {
            case CLASSIFICATION_CHOICE_INTERACTIVE:
                handleInteractiveClassification();
                return false;
            case CLASSIFICATION_CHOICE_AUTO_CLASSIFY:
                handleAutoClassification();
                return false;
            case CLASSIFICATION_CHOICE_RECLASSIFY_ALL:
                handleReclassification();
                return false;
            case CLASSIFICATION_CHOICE_INIT_CHART:
                handleChartOfAccountsInitialization();
                return false;
            case CLASSIFICATION_CHOICE_SYNC_JOURNAL:
                handleJournalSync();
                return false;
            case CLASSIFICATION_CHOICE_REGENERATE_JOURNAL:
                handleJournalRegeneration();
                return false;
            case CLASSIFICATION_CHOICE_BACK:
                return true;
            default:
                outputFormatter.printError("Invalid choice");
                return false;
        }
    }

    private void handleInteractiveClassification() {
        classificationService.runInteractiveClassification(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());
    }

    private void handleAutoClassification() {
        int classifiedCount = classificationService.autoClassifyTransactions(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());
        if (classifiedCount > 0) {
            outputFormatter.printSuccess("Auto-classified " + classifiedCount + " transactions");
        }
    }

    private void handleReclassification() {
        int reclassifiedCount = classificationService.reclassifyAllTransactions(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());
        if (reclassifiedCount > 0) {
            outputFormatter.printSuccess("Reclassified " + reclassifiedCount + " transactions with updated rules");
        }
    }

    private void handleJournalSync() {
        int syncCount = classificationService.synchronizeJournalEntries(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());
        if (syncCount > 0) {
            outputFormatter.printSuccess("Generated " + syncCount + " journal entries");
        }
    }

    private void handleJournalRegeneration() {
        System.out.println("\n⚠️  WARNING: This will delete and regenerate ALL journal entries!");
        System.out.print("Are you sure? (yes/no): ");
        String confirm = inputHandler.getString("Confirm");
        if (confirm.equalsIgnoreCase("yes")) {
            int regeneratedCount = classificationService.regenerateAllJournalEntries(
                applicationState.getCurrentCompany().getId(),
                applicationState.getCurrentFiscalPeriod().getId());
            if (regeneratedCount > 0) {
                outputFormatter.printSuccess("Regenerated " + regeneratedCount + " journal entries");
                outputFormatter.printInfo("Journal entries now reflect current transaction classifications");
            }
        } else {
            outputFormatter.printInfo("Operation cancelled");
        }
    }
    
    private void handleChartOfAccountsInitialization() {
        outputFormatter.printSubHeader("Chart of Accounts Initialization");
        
        boolean back = false;
        while (!back) {
            menu.displayAccountInitializationMenu();
            int choice = inputHandler.getInteger("Enter choice", 1, INIT_CHOICE_BACK);
            
            switch (choice) {
                case INIT_CHOICE_CHART_OF_ACCOUNTS:
                    boolean success = classificationService.initializeChartOfAccounts(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Chart of Accounts initialized successfully");
                    }
                    break;
                case INIT_CHOICE_MAPPING_RULES:
                    success = classificationService.initializeTransactionMappingRules(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Transaction Mapping Rules initialized successfully");
                    }
                    break;
                case INIT_CHOICE_FULL_INIT:
                    success = classificationService.performFullInitialization(
                        applicationState.getCurrentCompany().getId());
                    if (success) {
                        outputFormatter.printSuccess("Full initialization completed successfully");
                    }
                    break;
                case INIT_CHOICE_BACK:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice");
            }
        }
    }
    
    /**
     * Handles transaction correction process with filtering and pagination.
     * This method is designed for extension by subclasses that need to customize
     * transaction correction while maintaining the core workflow.
     */
    public void handleTransactionCorrection() {
        outputFormatter.printHeader("Correct Transaction Categorization");

        try {
            applicationState.requireFiscalPeriod();
            performTransactionCorrection();
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error correcting transaction: " + e.getMessage());
        }
    }

    /**
     * Performs the core transaction correction logic.
     * This method can be overridden by subclasses to customize the correction process.
     */
    protected void performTransactionCorrection() throws Exception {
        List<BankTransaction> allTransactions = csvImportService.getTransactions(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());

        if (allTransactions.isEmpty()) {
            outputFormatter.printInfo("No transactions found to correct.");
            return;
        }

        int filterChoice = setupTransactionFiltering(allTransactions.size());
        if (filterChoice == FILTER_CHOICE_BACK) {
            return;
        }

        List<BankTransaction> transactions = filterTransactions(allTransactions, filterChoice);

        if (transactions.isEmpty()) {
            outputFormatter.printInfo("No transactions match the selected filter.");
            return;
        }

        handlePaginationAndCorrection(transactions, allTransactions, filterChoice);
    }

    private int setupTransactionFiltering(int totalTransactions) {
        System.out.println("\n📊 Total Transactions: " + totalTransactions);
        System.out.println("\nFilter Options:");
        System.out.println("1. Show All Transactions");
        System.out.println("2. Show Uncategorized Only");
        System.out.println("3. Show Categorized Only");
        System.out.println("4. Back to Data Management");

        return inputHandler.getInteger("Select filter option", 1, MAX_FILTER_CHOICE);
    }

    private void handlePaginationAndCorrection(List<BankTransaction> transactions,
                                            List<BankTransaction> allTransactions,
                                            int filterChoice) throws Exception {
        final int transactionsPerPage = 50;
        int totalPages = (int) Math.ceil((double) transactions.size() / transactionsPerPage);
        int currentPage = 1;

        while (true) {
            displayTransactionPage(transactions, currentPage, totalPages, transactionsPerPage);

            String input = getUserNavigationInput(currentPage, totalPages, transactions.size());

            if (input.equalsIgnoreCase("0")) {
                return;
            } else if (input.equalsIgnoreCase("P") && currentPage > 1) {
                currentPage--;
                continue;
            } else if (input.equalsIgnoreCase("N") && currentPage < totalPages) {
                currentPage++;
                continue;
            }

            int txIndex = processTransactionSelection(input, transactions);
            if (txIndex != -1) {
                correctSingleTransaction(transactions.get(txIndex));
                refreshTransactionData(allTransactions, filterChoice, currentPage, totalPages);
                transactions = filterTransactions(allTransactions, filterChoice);
                totalPages = (int) Math.ceil((double) transactions.size() / transactionsPerPage);

                currentPage = adjustCurrentPageAfterRefresh(currentPage, totalPages);
            }
        }
    }

    private void displayTransactionPage(List<BankTransaction> transactions, int currentPage,
                                      int totalPages, int transactionsPerPage) {
        outputFormatter.printSubHeader("Transactions (Page " + currentPage + "/" + totalPages + ")");

        int startIdx = (currentPage - 1) * transactionsPerPage;
        int endIdx = Math.min(startIdx + transactionsPerPage, transactions.size());

        for (int i = startIdx; i < endIdx; i++) {
            BankTransaction tx = transactions.get(i);
            String status = (tx.getAccountCode() != null && !tx.getAccountCode().isEmpty()) ? "✓" : "⚠️";
            System.out.printf("%d. %s [%s] %s - Amount: %s%n",
                i + 1,
                status,
                tx.getTransactionDate(),
                tx.getDetails().length() > MAX_DESCRIPTION_LENGTH ? tx.getDetails().substring(0, TRUNCATED_DESCRIPTION_LENGTH) + "..." : tx.getDetails(),
                tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
        }
    }

    private String getUserNavigationInput(int currentPage, int totalPages, int totalTransactions) {
        System.out.println("\nNavigation:");
        System.out.println("0. Go back");
        if (currentPage > 1) {
            System.out.println("P. Previous page");
        }
        if (currentPage < totalPages) {
            System.out.println("N. Next page");
        }
        System.out.println("Or enter transaction number to correct (1-" + totalTransactions + ")");

        return inputHandler.getString("Your choice");
    }

    private int processTransactionSelection(String input, List<BankTransaction> transactions) {
        try {
            int txIndex = Integer.parseInt(input) - 1;
            if (txIndex >= 0 && txIndex < transactions.size()) {
                return txIndex;
            } else {
                outputFormatter.printError("Invalid transaction number");
                return -1;
            }
        } catch (NumberFormatException e) {
            outputFormatter.printError("Invalid input. Please enter a number or P/N/0");
            return -1;
        }
    }

    private void refreshTransactionData(List<BankTransaction> allTransactions, int filterChoice,
                                      int currentPage, int totalPages) throws Exception {
        allTransactions.clear();
        allTransactions.addAll(csvImportService.getTransactions(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId()));
    }

    private int adjustCurrentPageAfterRefresh(int currentPage, int newTotalPages) {
        if (currentPage > newTotalPages) {
            return newTotalPages > 0 ? newTotalPages : 1;
        }
        return currentPage;
    }
    
    private List<BankTransaction> filterTransactions(List<BankTransaction> transactions, int filterChoice) {
        switch (filterChoice) {
            case FILTER_CHOICE_UNCATEGORIZED: // Uncategorized only
                return transactions.stream()
                    .filter(tx -> tx.getAccountCode() == null || tx.getAccountCode().isEmpty())
                    .collect(java.util.stream.Collectors.toList());
            case FILTER_CHOICE_CATEGORIZED: // Categorized only
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
                        if (word.length() > MIN_KEYWORD_LENGTH && description.contains(word)) {
                            suggestions.add(account);
                            break;
                        }
                    }
                    
                    if (suggestions.size() >= MAX_SUGGESTIONS) {
                        break;
                    }
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
                .limit(MAX_SIMILAR_TRANSACTIONS)
                .collect(java.util.stream.Collectors.toList());
            
            if (!similarTransactions.isEmpty()) {
                System.out.println("\n📋 Found " + similarTransactions.size() + " similar uncategorized transactions:");
                for (int i = 0; i < Math.min(MAX_SIMILAR_TRANSACTIONS_DISPLAY, similarTransactions.size()); i++) {
                    BankTransaction similar = similarTransactions.get(i);
                    System.out.printf("  %d. [%s] %s - Amount: %s%n",
                        i + 1,
                        similar.getTransactionDate(),
                        similar.getDetails().length() > MAX_DESCRIPTION_LENGTH ? similar.getDetails().substring(0, TRUNCATED_DESCRIPTION_LENGTH) + "..." : similar.getDetails(),
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
                if (word.length() > MIN_WORD_LENGTH) {
                    return word;
                }
            }
            return words[0];
        }
        return description.length() > MAX_DESCRIPTION_PATTERN_LENGTH ? description.substring(0, MAX_DESCRIPTION_PATTERN_LENGTH) : description;
    }
    
    /**
     * Handles transaction history viewing process.
     * This method is designed for extension by subclasses that need to customize
     * history viewing while maintaining the core workflow.
     */
    public void handleTransactionHistory() {
        outputFormatter.printHeader("View Transaction History");
        
        try {
            applicationState.requireFiscalPeriod();
            performTransactionHistoryViewing();
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error viewing transaction history: " + e.getMessage());
        }
    }

    /**
     * Performs the core transaction history viewing logic.
     * This method can be overridden by subclasses to customize history display.
     */
    protected void performTransactionHistoryViewing() throws Exception {
        List<BankTransaction> transactions = csvImportService.getTransactions(
            applicationState.getCurrentCompany().getId(),
            applicationState.getCurrentFiscalPeriod().getId());
        
        if (transactions.isEmpty()) {
            outputFormatter.printInfo("No transactions found.");
            return;
        }
        
        displayRecentTransactions(transactions);
        BankTransaction selectedTransaction = selectTransactionForHistory(transactions);
        displayTransactionCorrectionHistory(selectedTransaction);
        
        inputHandler.waitForEnter();
    }
    
    private void displayRecentTransactions(List<BankTransaction> transactions) {
        outputFormatter.printSubHeader("Recent Transactions");
        for (int i = 0; i < Math.min(transactions.size(), MAX_RECENT_TRANSACTIONS); i++) {
            BankTransaction tx = transactions.get(i);
            System.out.printf("%d. [%s] %s - Amount: %s%n", i + 1,
                tx.getTransactionDate(), 
                tx.getDetails().length() > MAX_DESCRIPTION_LENGTH ? tx.getDetails().substring(0, TRUNCATED_DESCRIPTION_LENGTH) + "..." : tx.getDetails(),
                tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
        }
    }
    
    private BankTransaction selectTransactionForHistory(List<BankTransaction> transactions) {
        int txIndex = inputHandler.getInteger("Select transaction number to view history", 1, 
            Math.min(transactions.size(), MAX_RECENT_TRANSACTIONS)) - 1;
        return transactions.get(txIndex);
    }
    
    private void displayTransactionCorrectionHistory(BankTransaction tx) throws Exception {
        List<Map<String, Object>> history = 
            dataManagementService.getTransactionCorrectionHistory(tx.getId());
        
        if (history.isEmpty()) {
            outputFormatter.printInfo("No correction history found for this transaction.");
        } else {
            displayCorrectionHistoryDetails(history);
        }
    }
    
    private void displayCorrectionHistoryDetails(List<Map<String, Object>> history) {
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
    
    /**
     * Handles data reset operations for the company.
     * This method is designed for extension by subclasses that need to customize
     * data reset behavior while maintaining the core confirmation and service calls.
     */
    public void handleDataReset() {
        outputFormatter.printHeader("Reset Company Data");
        outputFormatter.printWarning("This will delete transaction data for the current company.");
        
        outputFormatter.printPlain("Reset options:");
        outputFormatter.printPlain("1. Reset only transaction data");
        outputFormatter.printPlain("2. Reset all data (including chart of accounts)");
        outputFormatter.printPlain("3. Cancel");
        
        int choice = inputHandler.getInteger("Enter choice", 1, MAX_RESET_CHOICE);
        
        if (choice == RESET_CHOICE_CANCEL) {
            outputFormatter.printInfo("Reset operation cancelled");
            return;
        }
        
        String confirmation = inputHandler.getConfirmation("Type 'CONFIRM' to proceed", "CONFIRM");
        
        if ("CONFIRM".equals(confirmation)) {
            try {
                boolean preserveMasterData = (choice == RESET_CHOICE_TRANSACTIONS_ONLY);
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
    
    /**
     * Handles CSV export of transactions.
     * This method is designed for extension by subclasses that need to customize
     * export behavior while maintaining the core file generation and service calls.
     */
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
