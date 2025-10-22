package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Classification UI Handler
 * 
 * SINGLE RESPONSIBILITY: Handle user interface for transaction classification
 * 
 * This service handles:
 * - Displaying transaction details
 * - Prompting for user input
 * - Showing classification suggestions
 * - Menu navigation for classification flows
 * - Formatting and presenting data to users
 * 
 * Created: October 11, 2025 - Extracted from InteractiveClassificationService
 */
public class ClassificationUIHandler {
    // Removed unused logger - UI handler doesn't need logging
    
    private final Scanner scanner;
    private final TransactionClassificationEngine classificationEngine;
    private final ClassificationRuleManager ruleManager;
    
    // Display constants
    @SuppressWarnings("MagicNumber")
    private static final int DISPLAY_LINE_WIDTH = 80;
    @SuppressWarnings("MagicNumber")
    private static final int ACCOUNT_NAME_DISPLAY_LENGTH = 35;
    @SuppressWarnings("MagicNumber")
    private static final int DESCRIPTION_DISPLAY_LENGTH = 50;
    @SuppressWarnings("MagicNumber")
    private static final int MAX_SUGGESTIONS = 5;
    @SuppressWarnings("MagicNumber")
    private static final int PERCENTAGE_MULTIPLIER = 100;
    @SuppressWarnings("MagicNumber")
    private static final int MENU_SEPARATOR_WIDTH = 60;
    @SuppressWarnings("MagicNumber")
    private static final int TRUNCATE_SUFFIX_LENGTH = 3;
    
    /**
     * User classification choice
     */
    public static class UserClassificationChoice {
        private final String accountCode;
        private final String accountName;
        private final boolean skipTransaction;
        private final boolean quitSession;
        
        private UserClassificationChoice(String initialAccountCode, String initialAccountName, boolean initialSkipTransaction, boolean initialQuitSession) {
            this.accountCode = initialAccountCode;
            this.accountName = initialAccountName;
            this.skipTransaction = initialSkipTransaction;
            this.quitSession = initialQuitSession;
        }
        
        public static UserClassificationChoice classify(String accountCode, String accountName) {
            return new UserClassificationChoice(accountCode, accountName, false, false);
        }
        
        public static UserClassificationChoice skip() {
            return new UserClassificationChoice(null, null, true, false);
        }
        
        public static UserClassificationChoice quit() {
            return new UserClassificationChoice(null, null, false, true);
        }
        
        // Getters
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public boolean isSkipTransaction() { return skipTransaction; }
        public boolean isQuitSession() { return quitSession; }
        public boolean isClassify() { return accountCode != null && accountName != null; }
    }
    
    @SuppressWarnings({"EI_EXPOSE_REP2"})
    public ClassificationUIHandler(TransactionClassificationEngine initialClassificationEngine, 
                                  ClassificationRuleManager initialRuleManager) {
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        this.classificationEngine = initialClassificationEngine;
        this.ruleManager = initialRuleManager;
    }
    
    /**
     * Display transaction details in a formatted way
     * 
     * @param transaction The transaction to display
     * @param index Current transaction index (for progress tracking)
     * @param total Total number of transactions
     */
    public void displayTransactionDetails(BankTransaction transaction, int index, int total) {
        System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
        if (index > 0 && total > 0) {
            System.out.println("TRANSACTION #" + index + " of " + total);
        } else {
            System.out.println("TRANSACTION DETAILS");
        }
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        
        System.out.println("📅 Date:        " + transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        System.out.println("📝 Description: " + transaction.getDetails());
        
        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("💸 Debit:       " + formatCurrency(transaction.getDebitAmount()));
        }
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("💵 Credit:      " + formatCurrency(transaction.getCreditAmount()));
        }
        
        if (transaction.getReference() != null) {
            System.out.println("🔗 Reference:   " + transaction.getReference());
        }
        
        // Show current classification status
        if (transaction.getAccountCode() != null) {
            System.out.println("🏷️  Current:     " + transaction.getAccountCode() + " - " + 
                             (transaction.getAccountName() != null ? transaction.getAccountName() : "Unknown"));
        } else {
            System.out.println("🏷️  Status:      UNCATEGORIZED");
        }
    }
    
    /**
     * Show auto-classification result and get user confirmation
     * 
     * @param result The classification result
     * @return true if user accepts, false if they want to classify manually
     */
    public boolean showAutoClassificationAndConfirm(TransactionClassificationEngine.ClassificationResult result) {
        if (result == null) {
            return false;
        }
        
        System.out.println("\n✅ AUTO-CLASSIFIED using standard rules:");
        System.out.println("   Account: " + result.getAccountCode() + " - " + result.getAccountName());
        System.out.println("   Confidence: " + String.format("%.1f%%", result.getConfidenceScore() * PERCENTAGE_MULTIPLIER));
        System.out.println("   Rule: " + result.getMatchingRule());
        System.out.println("   🧠 Intelligence: " + (result.isHighConfidence() ? "High confidence match" : "Medium confidence match"));
        
        // Ask for confirmation if not high confidence
        if (!result.isHighConfidence()) {
            System.out.print("\n❓ Accept this auto-classification? (y/n): ");
            String response = scanner.nextLine().trim().toLowerCase();
            return response.equals("y") || response.equals("yes");
        }
        
        return true; // High confidence - auto accept
    }
    
    /**
     * Show account suggestions to help user with classification
     * 
     * @param companyId The company ID
     * @param transaction The transaction being classified
     */
    public void showAccountSuggestions(Long companyId, BankTransaction transaction) {
        System.out.println("\n💡 ACCOUNT SUGGESTIONS:");
        System.out.println("-".repeat(MENU_SEPARATOR_WIDTH));
        
        // Show most used accounts from rules
        List<Map<String, Object>> mostUsed = ruleManager.getMostUsedAccounts(companyId, MAX_SUGGESTIONS);
        
        if (!mostUsed.isEmpty()) {
            System.out.println("📊 Most Used Accounts:");
            for (int i = 0; i < mostUsed.size(); i++) {
                Map<String, Object> account = mostUsed.get(i);
                System.out.printf("   %d. %s - %s (used %d times)%n", 
                    i + 1,
                    account.get("accountCode"),
                    truncateString((String) account.get("accountName"), ACCOUNT_NAME_DISPLAY_LENGTH),
                    account.get("usageCount"));
            }
        } else {
            System.out.println("📊 (No historical usage data available)");
        }
        
        // Show intelligent suggestions based on transaction content
        showIntelligentSuggestions(transaction);
        
        System.out.println("-".repeat(MENU_SEPARATOR_WIDTH));
    }
    
    /**
     * Show intelligent suggestions based on transaction analysis
     */
    private void showIntelligentSuggestions(BankTransaction transaction) {
        System.out.println("\n🎯 Smart Suggestions (based on transaction content):");
        
        String details = transaction.getDetails().toLowerCase();
        
        // Common patterns and their suggested accounts
        Map<String, String[]> suggestions = new LinkedHashMap<>();
        
        if (details.contains("fee") || details.contains("charge")) {
            suggestions.put("9600 - Bank Charges", new String[]{"Bank fees and transaction costs"});
        }
        if (details.contains("salary") || details.contains("wage")) {
            suggestions.put("8100 - Employee Costs", new String[]{"Salaries and wages"});
        }
        if (details.contains("insurance")) {
            suggestions.put("8800 - Insurance", new String[]{"Insurance premiums and costs"});
        }
        if (details.contains("fuel") || details.contains("petrol")) {
            suggestions.put("8200 - Travel & Vehicle", new String[]{"Fuel and vehicle expenses"});
        }
        if (details.contains("rent")) {
            suggestions.put("8500 - Office & Admin", new String[]{"Rent and office expenses"});
        }
        if (details.contains("telephone") || details.contains("internet") || details.contains("cell")) {
            suggestions.put("8300 - Communications", new String[]{"Telephone and internet costs"});
        }
        if (details.contains("electricity") || details.contains("water") || details.contains("utilities")) {
            suggestions.put("8400 - Utilities", new String[]{"Electricity, water, utilities"});
        }
        
        if (suggestions.isEmpty()) {
            System.out.println("   (No smart suggestions available for this transaction)");
        } else {
            int index = 1;
            for (Map.Entry<String, String[]> entry : suggestions.entrySet()) {
                System.out.printf("   %d. %s%n", index++, entry.getKey());
                System.out.printf("      %s%n", entry.getValue()[0]);
            }
        }
    }
    
    /**
     * Prompt user for transaction classification
     * 
     * @param transaction The transaction to classify
     * @param company The company context
     * @return User's classification choice
     */
    public UserClassificationChoice promptForClassification(BankTransaction transaction, Company company) {
        System.out.println("\n📝 Please classify this transaction for " + company.getName() + ":");
        System.out.println("💡 Enter account code and name (e.g., 8800 Insurance)");
        System.out.println("   or press ENTER to skip, or 'q' to quit");
        System.out.print("   Your choice: ");
        
        String input = scanner.nextLine().trim();
        
        if (input.equalsIgnoreCase("q") || input.equalsIgnoreCase("quit")) {
            return UserClassificationChoice.quit();
        } else if (input.isEmpty()) {
            return UserClassificationChoice.skip();
        } else {
            // Parse account code and name
            String[] parts = input.split("\\s+", 2);
            
            if (parts.length >= 2) {
                String accountCode = parts[0];
                String accountName = parts[1];
                
                // Validate account code format
                if (accountCode.matches("\\d{4}(-\\d{3})?")) {
                    return UserClassificationChoice.classify(accountCode, accountName);
                } else {
                    System.out.println("❌ Invalid account code format. Please use format like 8800 or 8800-001");
                    return promptForClassification(transaction, company); // Retry
                }
            } else {
                System.out.println("❌ Please provide both account code and name (e.g., 8800 Insurance)");
                return promptForClassification(transaction, company); // Retry
            }
        }
    }
    
    /**
     * Show similar transactions to help with classification
     * 
     * @param transaction The reference transaction
     * @param companyId The company ID
     */
    public void showSimilarTransactions(BankTransaction transaction, Long companyId) {
        List<BankTransaction> similar = classificationEngine.findSimilarUnclassifiedTransactions(
            transaction, companyId, MAX_SUGGESTIONS);
        
        if (!similar.isEmpty()) {
            System.out.println("\n🔍 Similar uncategorized transactions:");
            for (int i = 0; i < similar.size(); i++) {
                BankTransaction sim = similar.get(i);
                System.out.printf("   %d. %s - %s%n", 
                    i + 1,
                    sim.getTransactionDate().format(DateTimeFormatter.ofPattern("MM-dd")),
                    truncateString(sim.getDetails(), DESCRIPTION_DISPLAY_LENGTH));
            }
        }
    }
    
    /**
     * Display session summary
     * 
     * @param processedCount Number of transactions processed
     * @param classifiedCount Number of transactions classified
     * @param skippedCount Number of transactions skipped
     */
    public void showSessionSummary(int processedCount, int classifiedCount, int skippedCount) {
        System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("📋 SESSION SUMMARY");
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("📊 Transactions processed: " + processedCount);
        System.out.println("✅ Successfully classified: " + classifiedCount);
        System.out.println("⏭️  Skipped: " + skippedCount);
        
        if (processedCount > 0) {
            double successRate = ((double) classifiedCount * PERCENTAGE_MULTIPLIER) / (double) processedCount;
            System.out.println("📈 Success rate: " + String.format("%.1f%%", successRate));
        }
        
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
    }
    
    /**
     * Show main classification menu
     */
    public void showClassificationMenu() {
        System.out.println("\n" + "=".repeat(MENU_SEPARATOR_WIDTH));
        System.out.println("📋 TRANSACTION CLASSIFICATION MENU");
        System.out.println("=".repeat(MENU_SEPARATOR_WIDTH));
        System.out.println("1. Review Uncategorized Transactions");
        System.out.println("2. Auto-Classify All Transactions");
        System.out.println("3. Show Classification Summary");
        System.out.println("4. Manage Classification Rules");
        System.out.println("5. Back to Main Menu");
        System.out.println("=".repeat(MENU_SEPARATOR_WIDTH));
    }
    
    /**
     * Get user menu choice
     * 
     * @param prompt The prompt to display
     * @return User's input
     */
    public String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    /**
     * Confirm an action with the user
     * 
     * @param message The confirmation message
     * @return true if user confirms (y/yes), false otherwise
     */
    public boolean confirmAction(String message) {
        System.out.print(message + " (y/n): ");
        String response = scanner.nextLine().trim().toLowerCase();
        return response.equals("y") || response.equals("yes");
    }
    
    /**
     * Display error message
     * 
     * @param message The error message
     */
    public void showError(String message) {
        System.out.println("❌ " + message);
    }
    
    /**
     * Display success message
     * 
     * @param message The success message
     */
    public void showSuccess(String message) {
        System.out.println("✅ " + message);
    }
    
    /**
     * Display info message
     * 
     * @param message The info message
     */
    public void showInfo(String message) {
        System.out.println("ℹ️  " + message);
    }
    
    // Helper methods
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "R0.00";
        }
        return "R" + String.format("%,.2f", amount);
    }
    
    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - TRUNCATE_SUFFIX_LENGTH) + "...";
    }
}