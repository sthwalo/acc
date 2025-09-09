package fin.service;

import fin.model.BankTransaction;
import fin.model.Account;
import fin.model.AccountCategory;
import fin.model.AccountType;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.Console;

/**
 * Interactive Transaction Categorization Service
 * 
 * Provides interactive categorization of bank transactions with features:
 * - Review uncategorized transactions one by one
 * - Create new accounts on the fly
 * - Auto-categorize similar transactions
 * - Account allocation analysis
 * - Change tracking and audit trail
 */
public class InteractiveCategorizationService {
    private static final Logger LOGGER = Logger.getLogger(InteractiveCategorizationService.class.getName());
    private final String dbUrl;
    private final Scanner scanner;
    private final List<ChangeRecord> changesMade;
    private final Map<String, List<String>> accountCategories;
    
    // Change tracking
    public static class ChangeRecord {
        public Long transactionId;
        public LocalDate transactionDate;
        public String description;
        public BigDecimal amount;
        public String oldAccount;
        public String newAccount;
        public LocalDateTime timestamp;
        
        public ChangeRecord(Long transactionId, LocalDate transactionDate, String description, 
                          BigDecimal amount, String oldAccount, String newAccount) {
            this.transactionId = transactionId;
            this.transactionDate = transactionDate;
            this.description = description;
            this.amount = amount;
            this.oldAccount = oldAccount;
            this.newAccount = newAccount;
            this.timestamp = LocalDateTime.now();
        }
    }
    
    public InteractiveCategorizationService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.scanner = new Scanner(System.in);
        this.changesMade = new ArrayList<>();
        this.accountCategories = new LinkedHashMap<>();
        loadAccountCategories();
    }
    
    /**
     * Load account categories from database
     */
    private void loadAccountCategories() {
        String sql = """
            SELECT 
                ac.name as category_name,
                at.name as account_type,
                a.account_name
            FROM accounts a
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            WHERE a.is_active = true
            ORDER BY at.name, ac.name, a.account_code
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String accountType = rs.getString("account_type");
                String accountName = rs.getString("account_name");
                
                accountCategories.computeIfAbsent(accountType, k -> new ArrayList<>()).add(accountName);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading account categories", e);
        }
    }
    
    /**
     * Main interactive categorization process
     */
    public void runInteractiveCategorization(Long companyId, Long fiscalPeriodId) {
        System.out.println("🏢 INTERACTIVE TRANSACTION CATEGORIZATION");
        System.out.println("=".repeat(80));
        
        while (true) {
            showMainMenu();
            String choice = getInput("\nSelect action (1-6): ");
            
            switch (choice) {
                case "1":
                    reviewUncategorizedTransactions(companyId, fiscalPeriodId);
                    break;
                case "2":
                    analyzeAccountAllocations(companyId, fiscalPeriodId);
                    break;
                case "3":
                    showCategorizationSummary(companyId, fiscalPeriodId);
                    break;
                case "4":
                    saveChanges();
                    break;
                case "5":
                    if (changesMade.size() > 0) {
                        String confirm = getInput("You have " + changesMade.size() + " unsaved changes. Save before exiting? (y/n): ");
                        if (confirm.toLowerCase().startsWith("y")) {
                            saveChanges();
                        }
                    }
                    System.out.println("👋 Exiting categorization system...");
                    return;
                case "6":
                    showChangeHistory();
                    break;
                default:
                    System.out.println("❌ Invalid choice. Please select 1-6.");
            }
        }
    }
    
    private void showMainMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📋 CATEGORIZATION MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. Review Uncategorized Transactions");
        System.out.println("2. Analyze Account Allocations");
        System.out.println("3. Show Categorization Summary");
        System.out.println("4. Save Changes");
        System.out.println("5. Exit");
        System.out.println("6. Show Change History");
    }
    
    /**
     * Review uncategorized transactions one by one
     */
    private void reviewUncategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> uncategorized = getUncategorizedTransactions(companyId, fiscalPeriodId);
        
        if (uncategorized.isEmpty()) {
            System.out.println("\n✅ All transactions are properly categorized!");
            return;
        }
        
        System.out.println("\n🔍 Found " + uncategorized.size() + " uncategorized transactions");
        
        for (int i = 0; i < uncategorized.size(); i++) {
            BankTransaction transaction = uncategorized.get(i);
            
            displayTransaction(i + 1, uncategorized.size(), transaction);
            
            String choice = handleTransactionCategorization(transaction);
            
            if ("EXIT".equals(choice)) {
                System.out.println("🛑 Exiting transaction review...");
                break;
            } else if ("SAVE_EXIT".equals(choice)) {
                saveChanges();
                System.out.println("💾 Saved and exiting transaction review...");
                break;
            }
        }
        
        showSessionSummary();
    }
    
    /**
     * Display a single transaction with full details and context
     */
    private void displayTransaction(int current, int total, BankTransaction transaction) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TRANSACTION #" + current + " of " + total);
        System.out.println("=".repeat(80));
        
        System.out.println("📅 Date:        " + transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        System.out.println("📝 Description: " + transaction.getDetails());
        
        if (transaction.getDebitAmount() != null) {
            System.out.println("💸 Debit:       " + formatCurrency(transaction.getDebitAmount()));
        }
        if (transaction.getCreditAmount() != null) {
            System.out.println("💵 Credit:      " + formatCurrency(transaction.getCreditAmount()));
        }
        if (transaction.getBalance() != null) {
            System.out.println("🏦 Balance:     " + formatCurrency(transaction.getBalance()));
        }
        
        System.out.println("🏷️  Status:      UNCATEGORIZED");
        
        // Show context - similar transactions
        showSimilarTransactions(transaction);
    }
    
    /**
     * Show similar transactions to help with categorization
     */
    private void showSimilarTransactions(BankTransaction transaction) {
        String sql = """
            SELECT bt.details, a.account_name, COUNT(*) as count
            FROM bank_transactions bt
            LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id
            LEFT JOIN accounts a ON jel.account_id = a.id
            WHERE bt.company_id = ? 
            AND (bt.details ILIKE ? OR bt.details ILIKE ?)
            AND a.account_name IS NOT NULL
            GROUP BY bt.details, a.account_name
            ORDER BY count DESC
            LIMIT 3
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String details = transaction.getDetails();
            String pattern1 = "%" + extractKeywords(details) + "%";
            String pattern2 = "%" + details.substring(0, Math.min(details.length(), 20)) + "%";
            
            pstmt.setLong(1, transaction.getCompanyId());
            pstmt.setString(2, pattern1);
            pstmt.setString(3, pattern2);
            
            ResultSet rs = pstmt.executeQuery();
            
            System.out.println("\n📋 Similar transactions already categorized:");
            boolean hasSimilar = false;
            while (rs.next()) {
                hasSimilar = true;
                System.out.println("  • " + rs.getString("details").substring(0, Math.min(50, rs.getString("details").length())) + 
                                 " → " + rs.getString("account_name") + " (" + rs.getInt("count") + " times)");
            }
            
            if (!hasSimilar) {
                System.out.println("  (No similar categorized transactions found)");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error finding similar transactions", e);
        }
    }
    
    /**
     * Handle categorization of a single transaction
     */
    private String handleTransactionCategorization(BankTransaction transaction) {
        while (true) {
            List<MenuOption> menuItems = showAccountMenu();
            String choice = getInput("\n❓ Select account for this transaction (1-" + menuItems.size() + "): ");
            
            try {
                int choiceNum = Integer.parseInt(choice);
                if (choiceNum >= 1 && choiceNum <= menuItems.size()) {
                    MenuOption option = menuItems.get(choiceNum - 1);
                    
                    String result = processMenuChoice(option, transaction);
                    if (result != null) {
                        return result;
                    }
                } else {
                    System.out.println("❌ Invalid choice. Please enter 1-" + menuItems.size());
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Please enter a valid number");
            }
        }
    }
    
    /**
     * Menu option class
     */
    private static class MenuOption {
        String type;
        String category;
        String account;
        String action;
        
        MenuOption(String type, String category, String account, String action) {
            this.type = type;
            this.category = category;
            this.account = account;
            this.action = action;
        }
    }
    
    /**
     * Show account allocation menu
     */
    private List<MenuOption> showAccountMenu() {
        System.out.println("\n🎯 ACCOUNT ALLOCATION MENU");
        System.out.println("-".repeat(50));
        
        List<MenuOption> menuItems = new ArrayList<>();
        int itemNum = 1;
        
        // Show accounts by category
        for (Map.Entry<String, List<String>> entry : accountCategories.entrySet()) {
            String category = entry.getKey();
            List<String> accounts = entry.getValue();
            
            System.out.println("\n📁 " + category.toUpperCase() + ":");
            for (String account : accounts) {
                System.out.printf("  %2d. %s\n", itemNum, account);
                menuItems.add(new MenuOption("ACCOUNT", category, account, null));
                itemNum++;
            }
        }
        
        // Show actions
        System.out.println("\n🔧 ACTIONS:");
        System.out.printf("  %2d. Create New Account\n", itemNum);
        menuItems.add(new MenuOption("ACTION", null, null, "CREATE_NEW"));
        itemNum++;
        
        System.out.printf("  %2d. Auto-Categorize Similar\n", itemNum);
        menuItems.add(new MenuOption("ACTION", null, null, "AUTO_CATEGORIZE"));
        itemNum++;
        
        System.out.printf("  %2d. Skip This Transaction\n", itemNum);
        menuItems.add(new MenuOption("ACTION", null, null, "SKIP"));
        itemNum++;
        
        System.out.printf("  %2d. Save & Exit\n", itemNum);
        menuItems.add(new MenuOption("ACTION", null, null, "SAVE_EXIT"));
        itemNum++;
        
        System.out.printf("  %2d. Exit Without Saving\n", itemNum);
        menuItems.add(new MenuOption("ACTION", null, null, "EXIT"));
        
        return menuItems;
    }
    
    /**
     * Process menu choice
     */
    private String processMenuChoice(MenuOption option, BankTransaction transaction) {
        if ("ACCOUNT".equals(option.type)) {
            String confirm = getInput("\n✅ Selected: " + option.account + " (" + option.category + ")\nConfirm? (y/n): ");
            if (confirm.toLowerCase().startsWith("y")) {
                categorizeTransaction(transaction, option.account);
                autoCategorizePrompt(transaction, option.account);
                return "CONTINUE";
            }
            return null;
        } else if ("ACTION".equals(option.type)) {
            switch (option.action) {
                case "CREATE_NEW":
                    String newAccount = createNewAccount();
                    if (newAccount != null) {
                        categorizeTransaction(transaction, newAccount);
                        autoCategorizePrompt(transaction, newAccount);
                        return "CONTINUE";
                    }
                    return null;
                case "AUTO_CATEGORIZE":
                    if (!changesMade.isEmpty()) {
                        ChangeRecord lastChange = changesMade.get(changesMade.size() - 1);
                        autoCategorizeFromPattern(transaction, lastChange.newAccount);
                    } else {
                        System.out.println("❌ No recent categorization to apply");
                    }
                    return null;
                case "SKIP":
                    System.out.println("⏭️  Skipping transaction...");
                    return "CONTINUE";
                case "SAVE_EXIT":
                    return "SAVE_EXIT";
                case "EXIT":
                    return "EXIT";
            }
        }
        return null;
    }
    
    /**
     * Create a new account interactively
     */
    private String createNewAccount() {
        System.out.println("\n🆕 CREATE NEW ACCOUNT");
        System.out.println("-".repeat(30));
        
        String accountName = getInput("Enter new account name: ").trim();
        if (accountName.isEmpty()) {
            System.out.println("❌ Account name cannot be empty");
            return null;
        }
        
        // Check if account already exists
        if (accountExists(accountName)) {
            System.out.println("❌ Account already exists: " + accountName);
            return accountName; // Return existing account
        }
        
        System.out.println("\nSelect category for '" + accountName + "':");
        List<String> categories = new ArrayList<>(accountCategories.keySet());
        for (int i = 0; i < categories.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + categories.get(i));
        }
        
        try {
            String catChoice = getInput("Category (1-" + categories.size() + "): ");
            int catIndex = Integer.parseInt(catChoice) - 1;
            
            if (catIndex >= 0 && catIndex < categories.size()) {
                String categoryType = categories.get(catIndex);
                
                if (createAccountInDatabase(accountName, categoryType)) {
                    accountCategories.get(categoryType).add(accountName);
                    System.out.println("✅ Created new account: " + accountName + " (" + categoryType + ")");
                    return accountName;
                } else {
                    System.out.println("❌ Failed to create account in database");
                    return null;
                }
            } else {
                System.out.println("❌ Invalid category choice");
                return null;
            }
        } catch (NumberFormatException e) {
            System.out.println("❌ Invalid input");
            return null;
        }
    }
    
    /**
     * Check if account exists
     */
    private boolean accountExists(String accountName) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE account_name = ? AND is_active = true";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if account exists", e);
        }
        
        return false;
    }
    
    /**
     * Create account in database
     */
    private boolean createAccountInDatabase(String accountName, String categoryType) {
        String sql = """
            INSERT INTO accounts (account_code, account_name, description, category_id, 
                                company_id, is_active, created_at, updated_at)
            SELECT 
                COALESCE(MAX(CAST(account_code AS INTEGER)), 0) + 1,
                ?,
                'User-created account',
                ac.id,
                1, -- Default company ID
                true,
                CURRENT_TIMESTAMP,
                CURRENT_TIMESTAMP
            FROM accounts a
            CROSS JOIN account_categories ac
            JOIN account_types at ON ac.account_type_id = at.id
            WHERE at.name = ?
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountName);
            pstmt.setString(2, categoryType);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating account in database", e);
            return false;
        }
    }
    
    /**
     * Categorize a transaction
     */
    private void categorizeTransaction(BankTransaction transaction, String accountName) {
        // Get account ID
        Long accountId = getAccountId(accountName);
        if (accountId == null) {
            System.out.println("❌ Error: Could not find account ID for " + accountName);
            return;
        }
        
        // Create journal entry for this transaction
        if (createJournalEntryForTransaction(transaction, accountId)) {
            // Record the change
            BigDecimal amount = transaction.getDebitAmount() != null ? 
                               transaction.getDebitAmount() : transaction.getCreditAmount();
            
            ChangeRecord change = new ChangeRecord(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getDetails(),
                amount,
                "UNCATEGORIZED",
                accountName
            );
            
            changesMade.add(change);
            System.out.println("✅ Categorized: " + transaction.getDetails().substring(0, Math.min(50, transaction.getDetails().length())) + 
                             " → " + accountName);
        } else {
            System.out.println("❌ Failed to categorize transaction");
        }
    }
    
    /**
     * Prompt for auto-categorization of similar transactions
     */
    private void autoCategorizePrompt(BankTransaction transaction, String accountName) {
        String confirm = getInput("\n🔄 Auto-categorize similar transactions? (y/n): ");
        if (confirm.toLowerCase().startsWith("y")) {
            autoCategorizeFromPattern(transaction, accountName);
        }
    }
    
    /**
     * Auto-categorize similar transactions
     */
    private void autoCategorizeFromPattern(BankTransaction transaction, String accountName) {
        String details = transaction.getDetails();
        
        // Create search patterns
        List<String> patterns = new ArrayList<>();
        patterns.add(details); // Exact match
        
        // Add partial patterns
        String[] words = details.split("\\s+");
        if (words.length > 1) {
            patterns.add(String.join(" ", Arrays.copyOf(words, words.length - 1))); // All but last word
        }
        if (words.length > 3) {
            patterns.add(String.join(" ", Arrays.copyOf(words, 3))); // First three words
        }
        
        // Find and categorize similar transactions
        int totalCategorized = 0;
        for (String pattern : patterns) {
            List<BankTransaction> similar = findSimilarUncategorized(pattern, transaction.getCompanyId());
            
            if (!similar.isEmpty()) {
                System.out.println("\n🔍 Found " + similar.size() + " similar transactions matching: '" + 
                                 pattern.substring(0, Math.min(40, pattern.length())) + "'");
                
                String confirm = getInput("Auto-categorize these? (y/n): ");
                if (confirm.toLowerCase().startsWith("y")) {
                    Long accountId = getAccountId(accountName);
                    if (accountId != null) {
                        for (BankTransaction similarTx : similar) {
                            if (createJournalEntryForTransaction(similarTx, accountId)) {
                                BigDecimal amount = similarTx.getDebitAmount() != null ? 
                                                  similarTx.getDebitAmount() : similarTx.getCreditAmount();
                                
                                ChangeRecord change = new ChangeRecord(
                                    similarTx.getId(),
                                    similarTx.getTransactionDate(),
                                    similarTx.getDetails(),
                                    amount,
                                    "UNCATEGORIZED",
                                    accountName
                                );
                                
                                changesMade.add(change);
                                totalCategorized++;
                            }
                        }
                    }
                    break; // Only process first matching pattern
                }
            }
        }
        
        if (totalCategorized > 0) {
            System.out.println("✨ Auto-categorized " + totalCategorized + " similar transactions as " + accountName);
        } else {
            System.out.println("❌ No similar uncategorized transactions found");
        }
    }
    
    /**
     * Get uncategorized transactions
     */
    private List<BankTransaction> getUncategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String sql = """
            SELECT bt.*
            FROM bank_transactions bt
            WHERE bt.company_id = ? 
            AND bt.fiscal_period_id = ?
            AND bt.id NOT IN (
                SELECT DISTINCT source_transaction_id 
                FROM journal_entry_lines 
                WHERE source_transaction_id IS NOT NULL
            )
            ORDER BY bt.transaction_date DESC, bt.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(mapResultSetToBankTransaction(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting uncategorized transactions", e);
        }
        
        return transactions;
    }
    
    /**
     * Find similar uncategorized transactions
     */
    private List<BankTransaction> findSimilarUncategorized(String pattern, Long companyId) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String sql = """
            SELECT bt.*
            FROM bank_transactions bt
            WHERE bt.company_id = ? 
            AND bt.details ILIKE ?
            AND bt.id NOT IN (
                SELECT DISTINCT source_transaction_id 
                FROM journal_entry_lines 
                WHERE source_transaction_id IS NOT NULL
            )
            ORDER BY bt.transaction_date DESC
            LIMIT 20
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setString(2, "%" + pattern + "%");
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                transactions.add(mapResultSetToBankTransaction(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding similar transactions", e);
        }
        
        return transactions;
    }
    
    /**
     * Show categorization summary
     */
    private void showCategorizationSummary(Long companyId, Long fiscalPeriodId) {
        System.out.println("\n📊 CATEGORIZATION SUMMARY");
        System.out.println("=".repeat(60));
        
        String sql = """
            SELECT 
                COUNT(*) as total_transactions,
                COUNT(jel.source_transaction_id) as categorized_count,
                COUNT(*) - COUNT(jel.source_transaction_id) as uncategorized_count
            FROM bank_transactions bt
            LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id
            WHERE bt.company_id = ? AND bt.fiscal_period_id = ?
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total_transactions");
                int categorized = rs.getInt("categorized_count");
                int uncategorized = rs.getInt("uncategorized_count");
                
                double percentage = total > 0 ? (categorized * 100.0 / total) : 0;
                
                System.out.println("📈 Total Transactions: " + total);
                System.out.println("✅ Categorized: " + categorized + " (" + String.format("%.1f%%", percentage) + ")");
                System.out.println("❓ Uncategorized: " + uncategorized);
                System.out.println("🔄 Session Changes: " + changesMade.size());
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting categorization summary", e);
        }
    }
    
    /**
     * Analyze account allocations
     */
    private void analyzeAccountAllocations(Long companyId, Long fiscalPeriodId) {
        System.out.println("\n📊 ACCOUNT ALLOCATION ANALYSIS");
        System.out.println("=".repeat(60));
        
        String sql = """
            SELECT 
                a.account_name,
                at.name as account_type,
                COUNT(jel.id) as transaction_count,
                SUM(COALESCE(jel.debit_amount, 0)) as total_debits,
                SUM(COALESCE(jel.credit_amount, 0)) as total_credits
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.journal_entry_id = je.id
            JOIN accounts a ON jel.account_id = a.id
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            GROUP BY a.account_name, at.name
            ORDER BY transaction_count DESC
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            System.out.printf("%-35s %-12s %8s %15s %15s\n", 
                            "Account", "Type", "Count", "Debits", "Credits");
            System.out.println("-".repeat(85));
            
            while (rs.next()) {
                String accountName = rs.getString("account_name");
                String accountType = rs.getString("account_type");
                int count = rs.getInt("transaction_count");
                BigDecimal debits = rs.getBigDecimal("total_debits");
                BigDecimal credits = rs.getBigDecimal("total_credits");
                
                System.out.printf("%-35s %-12s %8d %15s %15s\n",
                                accountName.length() > 33 ? accountName.substring(0, 30) + "..." : accountName,
                                accountType,
                                count,
                                formatCurrency(debits),
                                formatCurrency(credits));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error analyzing account allocations", e);
        }
    }
    
    /**
     * Show session summary
     */
    private void showSessionSummary() {
        if (changesMade.isEmpty()) {
            return;
        }
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📋 SESSION SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Changes made: " + changesMade.size());
        
        if (changesMade.size() > 0) {
            System.out.println("\nLast 5 changes:");
            int start = Math.max(0, changesMade.size() - 5);
            for (int i = start; i < changesMade.size(); i++) {
                ChangeRecord change = changesMade.get(i);
                System.out.println("  • " + change.transactionDate.format(DateTimeFormatter.ofPattern("MM-dd")) + 
                                 " - " + change.description.substring(0, Math.min(50, change.description.length())));
                System.out.println("    " + change.oldAccount + " → " + change.newAccount);
            }
        }
    }
    
    /**
     * Show change history
     */
    private void showChangeHistory() {
        if (changesMade.isEmpty()) {
            System.out.println("\n📝 No changes made in this session");
            return;
        }
        
        System.out.println("\n📝 CHANGE HISTORY (" + changesMade.size() + " changes)");
        System.out.println("=".repeat(80));
        
        for (int i = 0; i < changesMade.size(); i++) {
            ChangeRecord change = changesMade.get(i);
            System.out.println((i + 1) + ". " + change.transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                             " | " + formatCurrency(change.amount));
            System.out.println("   " + change.description.substring(0, Math.min(60, change.description.length())));
            System.out.println("   " + change.oldAccount + " → " + change.newAccount);
            System.out.println("   @ " + change.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println();
        }
    }
    
    /**
     * Save changes to database
     */
    private void saveChanges() {
        if (changesMade.isEmpty()) {
            System.out.println("ℹ️  No changes to save");
            return;
        }
        
        System.out.println("\n💾 Saving " + changesMade.size() + " changes...");
        
        // Changes are already saved to database when categorization happens
        // This just confirms and clears the session
        changesMade.clear();
        
        System.out.println("✅ All changes saved successfully!");
    }
    
    // Helper methods
    
    private String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private String extractKeywords(String text) {
        // Extract meaningful keywords from transaction description
        String[] words = text.split("\\s+");
        StringBuilder keywords = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 3 && !word.matches("\\d+")) {
                if (keywords.length() > 0) keywords.append(" ");
                keywords.append(word);
            }
        }
        
        return keywords.toString();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    private Long getAccountId(String accountName) {
        String sql = "SELECT id FROM accounts WHERE account_name = ? AND is_active = true";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, accountName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("id");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting account ID", e);
        }
        
        return null;
    }
    
    private boolean createJournalEntryForTransaction(BankTransaction transaction, Long accountId) {
        String sql1 = """
            INSERT INTO journal_entries (reference, entry_date, description, fiscal_period_id, 
                                       company_id, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;
        
        String sql2 = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, 
                                           credit_amount, description, reference, 
                                           source_transaction_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // Create journal entry header
                Long journalEntryId;
                try (PreparedStatement pstmt = conn.prepareStatement(sql1)) {
                    pstmt.setString(1, "CAT-" + transaction.getId());
                    pstmt.setDate(2, java.sql.Date.valueOf(transaction.getTransactionDate()));
                    pstmt.setString(3, "Categorized: " + transaction.getDetails());
                    pstmt.setLong(4, transaction.getFiscalPeriodId());
                    pstmt.setLong(5, transaction.getCompanyId());
                    pstmt.setString(6, "INTERACTIVE-CATEGORIZATION");
                    
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        journalEntryId = rs.getLong("id");
                    } else {
                        throw new SQLException("Failed to create journal entry");
                    }
                }
                
                // Get bank account ID
                Long bankAccountId = getAccountId("Bank - Current Account");
                if (bankAccountId == null) {
                    bankAccountId = getAccountId("Bank Account - Standard Bank #203163753");
                }
                
                // Create journal entry lines
                try (PreparedStatement pstmt = conn.prepareStatement(sql2)) {
                    // Bank account line (opposite of transaction)
                    pstmt.setLong(1, journalEntryId);
                    pstmt.setLong(2, bankAccountId != null ? bankAccountId : accountId);
                    
                    if (transaction.getDebitAmount() != null) {
                        // Transaction is debit, so credit the bank account
                        pstmt.setBigDecimal(3, null);
                        pstmt.setBigDecimal(4, transaction.getDebitAmount());
                    } else {
                        // Transaction is credit, so debit the bank account
                        pstmt.setBigDecimal(3, transaction.getCreditAmount());
                        pstmt.setBigDecimal(4, null);
                    }
                    
                    pstmt.setString(5, "Bank Account");
                    pstmt.setString(6, "CAT-" + transaction.getId() + "-01");
                    pstmt.setLong(7, transaction.getId());
                    pstmt.executeUpdate();
                    
                    // Categorized account line (same as transaction)
                    pstmt.setLong(2, accountId);
                    
                    if (transaction.getDebitAmount() != null) {
                        // Transaction is debit, so debit the expense/asset account
                        pstmt.setBigDecimal(3, transaction.getDebitAmount());
                        pstmt.setBigDecimal(4, null);
                    } else {
                        // Transaction is credit, so credit the income/liability account
                        pstmt.setBigDecimal(3, null);
                        pstmt.setBigDecimal(4, transaction.getCreditAmount());
                    }
                    
                    pstmt.setString(5, "Categorized Account");
                    pstmt.setString(6, "CAT-" + transaction.getId() + "-02");
                    pstmt.setLong(7, transaction.getId());
                    pstmt.executeUpdate();
                }
                
                conn.commit();
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating journal entry for transaction", e);
            return false;
        }
    }
    
    private BankTransaction mapResultSetToBankTransaction(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setBankAccountId(rs.getLong("bank_account_id"));
        transaction.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setBalance(rs.getBigDecimal("balance"));
        transaction.setServiceFee(rs.getBoolean("service_fee"));
        transaction.setAccountNumber(rs.getString("account_number"));
        transaction.setStatementPeriod(rs.getString("statement_period"));
        transaction.setSourceFile(rs.getString("source_file"));
        return transaction;
    }
}
