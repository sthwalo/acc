package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.config.DatabaseConfig;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Unified Interactive Classification Service
 * 
 * Combines the functionality of InteractiveTransactionClassifier and InteractiveCategorizationService
 * into a single cohesive service for managing transaction classification and rules.
 * 
 * Provides:
 * - Transaction classification
 * - Rule creation and management
 * - Pattern recognition
 * - Interactive user experience
 */
public class InteractiveClassificationService {
    private static final Logger LOGGER = Logger.getLogger(InteractiveClassificationService.class.getName());
    private final String dbUrl;
    private final Scanner scanner;
    private final TransactionMappingService mappingService;
    private final Map<Long, Map<String, ClassificationRule>> companyRules;
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
    
    /**
     * Data class for classification rules
     */
    public static class ClassificationRule {
        private final String pattern;
        private final String[] keywords;
        private final String accountCode;
        private final String accountName;
        private final int usageCount;
        private double confidenceScore;
        
        public ClassificationRule(String pattern, String[] keywords, String accountCode, 
                                String accountName, int usageCount) {
            this.pattern = pattern;
            this.keywords = keywords;
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.usageCount = usageCount;
        }
        
        // Getters
        public String getPattern() { return pattern; }
        public String[] getKeywords() { return keywords; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public int getUsageCount() { return usageCount; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double confidenceScore) { this.confidenceScore = confidenceScore; }
    }
    
    /**
     * Data class for classified transaction
     */
    public static class ClassifiedTransaction {
        private final BankTransaction transaction;
        private final String accountCode;
        private final String accountName;
        
        public ClassifiedTransaction(BankTransaction transaction, String accountCode, String accountName) {
            this.transaction = transaction;
            this.accountCode = accountCode;
            this.accountName = accountName;
        }
        
        public BankTransaction getTransaction() { return transaction; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
    }
    
    public InteractiveClassificationService() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.scanner = new Scanner(System.in);
        this.mappingService = createMappingService();
        this.companyRules = new HashMap<>();
        this.changesMade = new ArrayList<>();
        this.accountCategories = new LinkedHashMap<>();
        
        initializeService();
    }
    
    /**
     * Create the TransactionMappingService
     * This method can be overridden in tests to provide a mock service
     */
    protected TransactionMappingService createMappingService() {
        return new TransactionMappingService(dbUrl);
    }
    
    /**
     * Initialize service with database tables and load data
     */
    private void initializeService() {
        initializeDatabase();
        loadExistingRules();
        loadAccountCategories();
    }
    
    /**
     * Get a database connection
     * 
     * @return A Connection to the database
     * @throws SQLException if a database access error occurs
     */
    protected Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }
    
    /**
     * Initialize database tables for classification rules
     */
    private void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create company classification rules table
            String sql = """
                CREATE TABLE IF NOT EXISTS company_classification_rules (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    pattern TEXT NOT NULL,
                    keywords VARCHAR(255)[] NOT NULL,
                    account_code VARCHAR(10) NOT NULL,
                    account_name VARCHAR(255) NOT NULL,
                    usage_count INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id)
                )
                """;
            
            stmt.executeUpdate(sql);
            
            // Create index for faster lookups
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_classification_rules_company 
                ON company_classification_rules(company_id, account_code)
                """);
                
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error initializing classification database", e);
        }
    }
    
    /**
     * Load existing classification rules from database
     */
    private void loadExistingRules() {
        try {
            String sql = """
                SELECT company_id, pattern, keywords, account_code, account_name, usage_count
                FROM company_classification_rules
                ORDER BY usage_count DESC
                """;
                
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    Long companyId = rs.getLong("company_id");
                    String pattern = rs.getString("pattern");
                    Array keywordsArray = rs.getArray("keywords");
                    String[] keywords = (String[]) keywordsArray.getArray();
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");
                    int usageCount = rs.getInt("usage_count");
                    
                    ClassificationRule rule = new ClassificationRule(pattern, keywords, accountCode, accountName, usageCount);
                    
                    companyRules.computeIfAbsent(companyId, k -> new HashMap<>())
                        .put(accountCode + ":" + String.join(",", keywords), rule);
                }
                
                LOGGER.info("Loaded " + companyRules.values().stream()
                    .mapToInt(Map::size).sum() + " classification rules");
                    
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error loading classification rules", e);
        }
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
        
        try (Connection conn = getConnection();
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
     * Classify a transaction interactively for a specific company
     */
    public ClassifiedTransaction classifyTransaction(BankTransaction transaction, Company company) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔍 TRANSACTION CLASSIFICATION - " + company.getName());
        System.out.println("=".repeat(80));
        
        // Display transaction details
        displayTransactionDetails(transaction);
        
        // Check for existing rules
        ClassificationRule existingRule = findMatchingRule(transaction, company.getId());
        if (existingRule != null && existingRule.getConfidenceScore() >= 0.6) {
            System.out.println("✅ AUTO-CLASSIFIED using learned rule:");
            System.out.println("   Account: " + existingRule.getAccountCode() + " - " + existingRule.getAccountName());
            System.out.println("   Confidence: " + String.format("%.1f%%", existingRule.getConfidenceScore() * 100));
            System.out.println("   🧠 Intelligence: Recognized pattern from previous classifications");
            
            // Ask for confirmation if confidence is not very high
            if (existingRule.getConfidenceScore() < 0.9) {
                System.out.print("\n❓ Accept this auto-classification? (y/n): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    return promptForClassification(transaction, company);
                }
            }
            
            // Update usage count for this rule
            incrementRuleUsage(existingRule, company.getId());
            
            // Use mapping service to apply the rule
            mappingService.classifyTransaction(transaction, existingRule.getAccountCode(), existingRule.getAccountName());
            
            return new ClassifiedTransaction(transaction, existingRule.getAccountCode(), existingRule.getAccountName());
        }
        
        // Show similar transactions if any
        showSimilarTransactions(transaction, company.getId());
        
        // Prompt for classification
        return promptForClassification(transaction, company);
    }
    
    /**
     * Display transaction details for user review
     */
    private void displayTransactionDetails(BankTransaction transaction) {
        System.out.println("📋 Transaction Details:");
        System.out.println("   Date: " + transaction.getTransactionDate());
        System.out.println("   Description: " + transaction.getDetails());
        
        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("   Amount: -R" + String.format("%,.2f", transaction.getDebitAmount()) + " (Debit)");
        }
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("   Amount: +R" + String.format("%,.2f", transaction.getCreditAmount()) + " (Credit)");
        }
        
        if (transaction.getReference() != null) {
            System.out.println("   Reference: " + transaction.getReference());
        }
        System.out.println();
    }
    
    /**
     * Find matching classification rule for a transaction
     */
    private ClassificationRule findMatchingRule(BankTransaction transaction, Long companyId) {
        Map<String, ClassificationRule> rules = companyRules.get(companyId);
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        
        String details = transaction.getDetails().toLowerCase();
        ClassificationRule bestMatch = null;
        double highestScore = 0.0;
        
        for (ClassificationRule rule : rules.values()) {
            double score = calculateMatchScore(details, rule);
            if (score > highestScore && score >= 0.5) {
                highestScore = score;
                bestMatch = rule;
            }
        }
        
        if (bestMatch != null) {
            bestMatch.setConfidenceScore(highestScore);
        }
        
        return bestMatch;
    }
    
    /**
     * Calculate match score between transaction and rule
     */
    private double calculateMatchScore(String transactionDetails, ClassificationRule rule) {
        String[] keywords = rule.getKeywords();
        if (keywords == null || keywords.length == 0) {
            return 0.0;
        }
        
        String lowerDetails = transactionDetails.toLowerCase();
        double totalScore = 0.0;
        int significantMatches = 0;
        
        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            
            // Exact keyword match gets high score
            if (lowerDetails.contains(lowerKeyword)) {
                if (lowerKeyword.length() > 4) { // Significant keywords
                    totalScore += 1.0;
                    significantMatches++;
                } else {
                    totalScore += 0.5; // Short keywords get less weight
                }
            }
            
            // Pattern matching for common variations
            if (isPatternMatch(lowerDetails, lowerKeyword)) {
                totalScore += 0.8;
                significantMatches++;
            }
        }
        
        // Boost score if we have significant matches
        if (significantMatches > 0) {
            totalScore *= 1.2; // 20% boost for having significant matches
        }
        
        // Company/supplier name detection
        if (detectSupplierPattern(lowerDetails, rule)) {
            totalScore += 0.9;
            significantMatches++;
        }
        
        return Math.min(totalScore / keywords.length, 1.0); // Cap at 1.0
    }
    
    /**
     * Enhanced pattern matching for transaction types
     */
    private boolean isPatternMatch(String details, String keyword) {
        // Insurance patterns
        if (keyword.equals("insurance") && 
            (details.contains("premium") || details.contains("policy") || 
             details.contains("cover") || details.contains("santam") || 
             details.contains("outsurance") || details.contains("discovery"))) {
            return true;
        }
        
        // Salary patterns  
        if (keyword.equals("salary") && 
            (details.contains("payment to") || details.contains("salaries") || 
             details.contains("wage") || details.contains("employee"))) {
            return true;
        }
        
        // Bank fee patterns
        if (keyword.equals("fee") && 
            (details.contains("confirm") || details.contains("electronic") || 
             details.contains("immediate") || details.contains("charge"))) {
            return true;
        }
        
        // Rent/supplier patterns
        if (keyword.equals("rent") && 
            (details.contains("payment to") || details.contains("building") || 
             details.contains("property") || details.contains("stadium") || 
             details.contains("ellispark") || details.contains("johannesburg"))) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Detect supplier/company patterns for better classification
     */
    private boolean detectSupplierPattern(String details, ClassificationRule rule) {
        String accountName = rule.getAccountName().toLowerCase();
        
        // Known suppliers/patterns
        if (accountName.contains("rent") && 
            (details.contains("rent a dog") || details.contains("ellispark") || 
             details.contains("stadium") || details.contains("johannesburg"))) {
            return true;
        }
        
        if (accountName.contains("insurance") && 
            (details.contains("outsurance") || details.contains("santam") || 
             details.contains("premium") || details.contains("discovery"))) {
            return true;
        }
        
        if (accountName.contains("salary") && 
            (details.contains("john smith") || details.contains("jane doe") || 
             details.contains("sibongile") || details.contains("siyabulela"))) {
            return true;
        }
        
        // Customer patterns (for revenue classification)
        if (details.contains("corobrick") || details.contains("customer payment")) {
            return accountName.contains("revenue") || accountName.contains("sales");
        }
        
        return false;
    }
    
    /**
     * Increment usage count for a classification rule
     */
    private void incrementRuleUsage(ClassificationRule rule, Long companyId) {
        try {
            String sql = """
                UPDATE company_classification_rules 
                SET usage_count = usage_count + 1, last_used = CURRENT_TIMESTAMP
                WHERE company_id = ? AND account_code = ? AND pattern ILIKE ?
                """;
                
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                stmt.setString(2, rule.getAccountCode());
                stmt.setString(3, "%" + String.join("%", rule.getKeywords()) + "%");
                
                stmt.executeUpdate();
                
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error incrementing rule usage", e);
        }
    }
    
    /**
     * Show similar past transactions to help with classification
     */
    private void showSimilarTransactions(BankTransaction transaction, Long companyId) {
        try {
            String sql = """
                SELECT bt.details, cr.account_code, cr.account_name, cr.usage_count
                FROM bank_transactions bt
                JOIN company_classification_rules cr ON cr.company_id = bt.company_id
                WHERE bt.company_id = ? 
                AND cr.account_code IS NOT NULL
                AND (LOWER(bt.details) LIKE ? OR LOWER(cr.pattern) LIKE ?)
                ORDER BY cr.usage_count DESC
                LIMIT 5
                """;
                
            String[] keywords = extractKeywords(transaction.getDetails());
            String searchPattern = "%" + (keywords.length > 0 ? keywords[0] : "") + "%";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                stmt.setString(2, searchPattern);
                stmt.setString(3, searchPattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("🔍 Similar past transactions:");
                        do {
                            System.out.printf("   📝 %s → %s - %s (used %d times)%n", 
                                rs.getString("details").substring(0, Math.min(50, rs.getString("details").length())),
                                rs.getString("account_code"),
                                rs.getString("account_name"),
                                rs.getInt("usage_count"));
                        } while (rs.next());
                        System.out.println();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error finding similar transactions", e);
        }
    }
    
    /**
     * Prompt user for transaction classification
     */
    private ClassifiedTransaction promptForClassification(BankTransaction transaction, Company company) {
        System.out.println("💡 Available account suggestions for " + company.getName() + ":");
        showAccountSuggestions(company.getId());
        
        System.out.println("\n📝 Please classify this transaction:");
        System.out.print("   Account Code (e.g., 8800): ");
        String accountCode = scanner.nextLine().trim();
        
        System.out.print("   Account Name (e.g., Insurance): ");
        String accountName = scanner.nextLine().trim();
        
        // Validate input
        if (accountCode.isEmpty() || accountName.isEmpty()) {
            System.out.println("❌ Invalid input. Please provide both account code and name.");
            return promptForClassification(transaction, company);
        }
        
        // Save the classification rule
        createMappingRule(company.getId(), transaction.getDetails(), accountCode, accountName);
        
        System.out.println("✅ Classification saved and will be used for similar future transactions!");
        
        return new ClassifiedTransaction(transaction, accountCode, accountName);
    }
    
    /**
     * Show available account suggestions based on company's chart of accounts
     */
    private void showAccountSuggestions(Long companyId) {
        try {
            String sql = """
                SELECT account_code, account_name, usage_count 
                FROM company_classification_rules 
                WHERE company_id = ? 
                ORDER BY usage_count DESC, account_code ASC
                LIMIT 10
                """;
                
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("   Most used accounts:");
                        do {
                            System.out.printf("   • %s - %s (used %d times)%n",
                                rs.getString("account_code"),
                                rs.getString("account_name"),
                                rs.getInt("usage_count"));
                        } while (rs.next());
                    } else {
                        // Show standard account suggestions
                        System.out.println("   Standard account suggestions:");
                        System.out.println("   • 8800 - Insurance");
                        System.out.println("   • 8100 - Employee Costs");
                        System.out.println("   • 8200 - Rent Expense");
                        System.out.println("   • 9600 - Bank Charges");
                        System.out.println("   • 8300 - Utilities");
                        System.out.println("   • 8500 - Motor Vehicle Expenses");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error loading account suggestions", e);
        }
    }
    
    /**
     * Extract keywords from transaction description for pattern matching
     */
    private String[] extractKeywords(String description) {
        if (description == null) return new String[0];
        
        // Remove common words and extract meaningful keywords
        String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        Set<String> commonWordsSet = Set.of(commonWords);
        
        return Arrays.stream(description.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !commonWordsSet.contains(word))
                .limit(5) // Take top 5 keywords
                .toArray(String[]::new);
    }
    
    /**
     * Create a mapping rule for a transaction pattern
     * 
     * @param companyId The company ID
     * @param pattern The pattern to match
     * @param accountCode The account code to assign
     * @param accountName The account name to assign
     */
    public void createMappingRule(Long companyId, String pattern, String accountCode, String accountName) {
        try {
            // Extract keywords for future matching
            String[] keywords = extractKeywords(pattern);
            
            // Check if rule already exists
            String checkSql = """
                SELECT id, usage_count FROM company_classification_rules 
                WHERE company_id = ? AND account_code = ? AND pattern ILIKE ?
                """;
                
            String updateSql = """
                UPDATE company_classification_rules 
                SET usage_count = usage_count + 1, keywords = ?, last_used = CURRENT_TIMESTAMP
                WHERE id = ?
                """;
                
            String insertSql = """
                INSERT INTO company_classification_rules 
                (company_id, pattern, keywords, account_code, account_name, usage_count, created_at, last_used)
                VALUES (?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """;
            
            try (Connection conn = getConnection()) {
                // Check for existing rule
                try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                    checkStmt.setLong(1, companyId);
                    checkStmt.setString(2, accountCode);
                    checkStmt.setString(3, "%" + String.join("%", keywords) + "%");
                    
                    try (ResultSet rs = checkStmt.executeQuery()) {
                        if (rs.next()) {
                            // Update existing rule
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setArray(1, conn.createArrayOf("VARCHAR", keywords));
                                updateStmt.setLong(2, rs.getLong("id"));
                                updateStmt.executeUpdate();
                            }
                        } else {
                            // Insert new rule
                            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                                insertStmt.setLong(1, companyId);
                                insertStmt.setString(2, pattern);
                                insertStmt.setArray(3, conn.createArrayOf("VARCHAR", keywords));
                                insertStmt.setString(4, accountCode);
                                insertStmt.setString(5, accountName);
                                insertStmt.executeUpdate();
                            }
                        }
                    }
                }
                
                // Update in-memory cache
                companyRules.computeIfAbsent(companyId, k -> new HashMap<>())
                    .put(accountCode + ":" + String.join(",", keywords), 
                         new ClassificationRule(pattern, keywords, accountCode, accountName, 1));
                
                LOGGER.info("Created/updated rule for pattern: " + pattern + " → " + accountCode + " - " + accountName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating mapping rule", e);
        }
    }
    
    /**
     * Run the interactive categorization experience
     */
    public void runInteractiveCategorization(Long companyId, Long fiscalPeriodId) {
        System.out.println("\n🏢 INTERACTIVE TRANSACTION CATEGORIZATION");
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
    
    /**
     * Display the main menu
     */
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
     * Classify transactions in batch mode
     * 
     * @param transactionIds List of transaction IDs to classify
     * @param accountCode Account code to assign
     * @param accountName Account name to assign
     * @return Number of transactions classified
     */
    public int classifyTransactionsBatch(List<Long> transactionIds, String accountCode, String accountName, Long companyId) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return 0;
        }
        
        int classifiedCount = 0;
        
        try {
            // Get transaction details
            String selectSql = """
                SELECT id, transaction_date, details, debit_amount, credit_amount 
                FROM bank_transactions 
                WHERE id = ANY(?)
                """;
                
            String updateSql = """
                UPDATE bank_transactions
                SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
                WHERE id = ?
                """;
                
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    // Get transaction details first
                    Map<Long, String> transactionDetails = new HashMap<>();
                    try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                        Array idArray = conn.createArrayOf("BIGINT", transactionIds.toArray());
                        stmt.setArray(1, idArray);
                        
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                transactionDetails.put(rs.getLong("id"), rs.getString("details"));
                            }
                        }
                    }
                    
                    // Update transactions
                    try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                        for (Long id : transactionIds) {
                            if (transactionDetails.containsKey(id)) {
                                stmt.setString(1, accountCode);
                                stmt.setString(2, accountName);
                                stmt.setString(3, "BATCH_CLASSIFICATION");
                                stmt.setLong(4, id);
                                stmt.addBatch();
                                
                                // Also create rule for future similar transactions
                                createMappingRule(companyId, transactionDetails.get(id), accountCode, accountName);
                                
                                classifiedCount++;
                            }
                        }
                        
                        stmt.executeBatch();
                    }
                    
                    conn.commit();
                    LOGGER.info("Batch classified " + classifiedCount + " transactions as " + accountCode + " - " + accountName);
                    
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error batch classifying transactions", e);
        }
        
        return classifiedCount;
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
            AND bt.account_code IS NULL
            ORDER BY bt.transaction_date DESC, bt.id
            LIMIT 100
            """;
        
        try (Connection conn = getConnection();
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
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("TRANSACTION #" + (i + 1) + " of " + uncategorized.size());
            System.out.println("=".repeat(80));
            
            System.out.println("📅 Date:        " + transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            System.out.println("📝 Description: " + transaction.getDetails());
            
            if (transaction.getDebitAmount() != null) {
                System.out.println("💸 Debit:       " + formatCurrency(transaction.getDebitAmount()));
            }
            if (transaction.getCreditAmount() != null) {
                System.out.println("💵 Credit:      " + formatCurrency(transaction.getCreditAmount()));
            }
            
            System.out.println("🏷️  Status:      UNCATEGORIZED");
            
            // Show similar transactions for context
            showSimilarTransactions(transaction, companyId);
            
            // Get user input for classification
            System.out.println("\n🎯 Enter account code and name (e.g., 8800 Insurance)");
            System.out.print("   or press ENTER to skip, or 'q' to quit: ");
            String input = scanner.nextLine().trim();
            
            if (input.equalsIgnoreCase("q")) {
                System.out.println("🛑 Exiting transaction review...");
                break;
            } else if (!input.isEmpty()) {
                String[] parts = input.split("\\s+", 2);
                if (parts.length == 2) {
                    String accountCode = parts[0];
                    String accountName = parts[1];
                    
                    Long accountId = getAccountId(accountName);
                    if (accountId != null) {
                        createJournalEntryForTransaction(transaction, accountId);
                        
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
                        
                        // Create rule for future similar transactions
                        createMappingRule(companyId, transaction.getDetails(), accountCode, accountName);
                        
                        System.out.println("✅ Categorized as: " + accountCode + " - " + accountName);
                        
                        // Ask if user wants to apply to similar transactions
                        System.out.print("\n🔄 Auto-categorize similar transactions? (y/n): ");
                        String autocat = scanner.nextLine().trim().toLowerCase();
                        if (autocat.equals("y") || autocat.equals("yes")) {
                            autoCategorizeFromPattern(transaction, accountCode, accountName, companyId);
                        }
                    } else {
                        System.out.println("❌ Account not found: " + accountName);
                    }
                } else {
                    System.out.println("❌ Invalid format. Use 'code name' format (e.g., 8800 Insurance)");
                }
            }
        }
        
        showSessionSummary();
    }
    
    /**
     * Auto-categorize similar transactions
     */
    private void autoCategorizeFromPattern(BankTransaction transaction, String accountCode, String accountName, Long companyId) {
        String details = transaction.getDetails();
        
        // Create search patterns
        List<String> patterns = new ArrayList<>();
        patterns.add(details); // Exact match
        
        // Add partial patterns
        String[] words = details.split("\\s+");
        if (words.length > 1) {
            patterns.add(String.join(" ", Arrays.copyOf(words, Math.min(words.length - 1, 3)))); // First few words
        }
        
        // Find and categorize similar transactions
        int totalCategorized = 0;
        for (String pattern : patterns) {
            List<BankTransaction> similar = findSimilarUncategorized(pattern, companyId);
            
            if (!similar.isEmpty()) {
                System.out.println("\n🔍 Found " + similar.size() + " similar transactions matching: '" + 
                                 pattern.substring(0, Math.min(40, pattern.length())) + "'");
                
                System.out.print("Auto-categorize these? (y/n): ");
                String confirm = scanner.nextLine().trim().toLowerCase();
                if (confirm.equals("y") || confirm.equals("yes")) {
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
     * Find similar uncategorized transactions
     */
    private List<BankTransaction> findSimilarUncategorized(String pattern, Long companyId) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String sql = """
            SELECT bt.*
            FROM bank_transactions bt
            WHERE bt.company_id = ? 
            AND bt.details ILIKE ?
            AND bt.account_code IS NULL
            ORDER BY bt.transaction_date DESC
            LIMIT 20
            """;
        
        try (Connection conn = getConnection();
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
     * Analyze account allocations to show distribution of transactions
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
        
        try (Connection conn = getConnection();
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
        
        try (Connection conn = getConnection();
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
    
    /**
     * Helper method to create journal entry for a transaction
     */
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
        
        try (Connection conn = getConnection()) {
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
                    bankAccountId = getAccountId("Bank Account");
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
    
    // Helper methods
    
    private String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
    
    private Long getAccountId(String accountName) {
        String sql = "SELECT id FROM accounts WHERE account_name = ? AND is_active = true";
        
        try (Connection conn = getConnection();
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
    
    /**
     * Get suggestions for accounts based on a transaction pattern
     */
    public List<String> suggestAccountsForPattern(String pattern) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            String sql = """
                SELECT cr.account_code, cr.account_name, COUNT(*) as usage_count
                FROM company_classification_rules cr
                WHERE LOWER(cr.pattern) LIKE ?
                GROUP BY cr.account_code, cr.account_name
                ORDER BY COUNT(*) DESC
                LIMIT 5
                """;
                
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, "%" + pattern.toLowerCase() + "%");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        suggestions.add(rs.getString("account_code") + " - " + rs.getString("account_name"));
                    }
                }
            }
            
            // If no matches, add some standard suggestions
            if (suggestions.isEmpty()) {
                // Use pattern to make intelligent suggestions
                if (pattern.toLowerCase().contains("fee") || pattern.toLowerCase().contains("charge")) {
                    suggestions.add("9600 - Bank Charges");
                } else if (pattern.toLowerCase().contains("salary") || pattern.toLowerCase().contains("wage")) {
                    suggestions.add("8100 - Employee Costs");
                } else if (pattern.toLowerCase().contains("insurance") || pattern.toLowerCase().contains("premium")) {
                    suggestions.add("8800 - Insurance");
                } else if (pattern.toLowerCase().contains("rent") || pattern.toLowerCase().contains("lease")) {
                    suggestions.add("8200 - Rent Expense");
                } else {
                    // Add standard accounts as fallback
                    suggestions.add("9600 - Bank Charges");
                    suggestions.add("8100 - Employee Costs");
                    suggestions.add("8800 - Insurance");
                    suggestions.add("8200 - Rent Expense");
                    suggestions.add("6000 - Sales Revenue");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting account suggestions", e);
        }
        
        return suggestions;
    }
}
