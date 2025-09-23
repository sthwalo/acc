package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.config.DatabaseConfig;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/**
 * Interactive Transaction Classifier that learns company-specific account mappings
 * and builds intelligence over time for automatic classification
 */
public class InteractiveTransactionClassifier {
    
    private final String dbUrl;
    private final Scanner scanner;
    private final Map<Long, Map<String, ClassificationRule>> companyRules;
    
    public InteractiveTransactionClassifier() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.scanner = new Scanner(System.in);
        this.companyRules = new HashMap<>();
        initializeDatabase();
        loadExistingRules();
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
        if (existingRule != null && existingRule.getConfidenceScore() >= 0.6) { // Lowered from 0.8 to 0.6
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
            if (score > highestScore && score >= 0.5) { // Lowered from 0.7 to 0.5
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
                
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                stmt.setString(2, rule.getAccountCode());
                stmt.setString(3, "%" + String.join("%", rule.getKeywords()) + "%");
                
                stmt.executeUpdate();
                
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error incrementing rule usage: " + e.getMessage());
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
                
            String searchPattern = "%" + extractKeywords(transaction.getDetails())[0] + "%";
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
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
            System.err.println("⚠️ Error finding similar transactions: " + e.getMessage());
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
        
        // Extract keywords for future matching
        String[] keywords = extractKeywords(transaction.getDetails());
        
        // Save the classification rule
        saveClassificationRule(company.getId(), transaction.getDetails(), keywords, accountCode, accountName);
        
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
                
            try (Connection conn = DriverManager.getConnection(dbUrl);
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
                        System.out.println("   • Insurance - Risk management expenses");
                        System.out.println("   • Employee Costs - Personnel expenses");
                        System.out.println("   • Rent Expense - Property costs");
                        System.out.println("   • Bank Charges - Financial expenses");
                        System.out.println("   • Utilities - Operational expenses");
                        System.out.println("   • Motor Vehicle Expenses - Transportation costs");
                        System.out.println("   • Interest Income - Financial income");
                        System.out.println("   • Interest Expense - Financial costs");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error loading account suggestions: " + e.getMessage());
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
     * Save classification rule for future use
     */
    private void saveClassificationRule(Long companyId, String originalPattern, String[] keywords, 
                                      String accountCode, String accountName) {
        try {
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
            
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
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
                                insertStmt.setString(2, originalPattern);
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
                         new ClassificationRule(originalPattern, keywords, accountCode, accountName, 1));
                         
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error saving classification rule: " + e.getMessage());
        }
    }
    
    /**
     * Initialize database tables for classification rules
     */
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(dbUrl);
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
            System.err.println("⚠️ Error initializing classification database: " + e.getMessage());
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
                
            try (Connection conn = DriverManager.getConnection(dbUrl);
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
                
                System.out.println("📚 Loaded " + companyRules.values().stream()
                    .mapToInt(Map::size).sum() + " classification rules");
                    
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Error loading classification rules: " + e.getMessage());
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
            this.keywords = keywords != null ? keywords.clone() : new String[0];
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.usageCount = usageCount;
        }
        
        // Getters
        public String getPattern() { return pattern; }
        public String[] getKeywords() { return keywords != null ? keywords.clone() : new String[0]; }
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
}
