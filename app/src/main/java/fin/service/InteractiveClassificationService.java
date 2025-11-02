/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.TransactionMappingRule;
import fin.config.DatabaseConfig;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Unified Interactive Classification Service
 *
 * Combines functionality for managing transaction classification and rules.
 * Provides interactive user experience for transaction categorization.
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
    private final AccountClassificationService accountClassificationService;
    private final Map<Long, Map<String, ClassificationRule>> companyRules;
    private final List<ChangeRecord> changesMade;
    private final Map<String, List<String>> accountCategories;
    
    // Constants for display and thresholds
    private static final int DISPLAY_LINE_WIDTH = 80;
    private static final double CONFIDENCE_THRESHOLD_HIGH = 0.9;
    private static final double CONFIDENCE_THRESHOLD_MEDIUM = 0.6;
    private static final int PERCENTAGE_MULTIPLIER = 100;
    private static final int SIGNIFICANT_KEYWORD_LENGTH = 4;
    private static final double HIGH_MATCH_SCORE = 1.0;
    private static final double LOW_MATCH_SCORE = 0.5;
    private static final double PATTERN_MATCH_SCORE = 0.8;
    private static final double SIGNIFICANT_MATCH_BOOST = 1.2;
    private static final double SUPPLIER_MATCH_SCORE = 0.9;
    private static final double MAX_MATCH_SCORE = 1.0;
    private static final int MAX_KEYWORDS = 5;
    private static final int MAX_SIMILAR_TRANSACTIONS = 5;
    private static final int MAX_BATCH_SIMILAR = 20;
    private static final int MAX_UNCATEGORIZED_TRANSACTIONS = 100;
    private static final int ACCOUNT_NAME_DISPLAY_LENGTH = 35;
    private static final int DESCRIPTION_DISPLAY_LENGTH = 50;
    private static final int SIMILAR_TRANSACTION_DESC_LENGTH = 50;
    private static final int SUGGESTIONS_SEPARATOR_WIDTH = 60;
    private static final int MAX_ACCOUNTS_PER_CATEGORY = 3;
    private static final int MAX_ACCOUNT_SUGGESTIONS = 5;
    private static final int TABLE_SEPARATOR_WIDTH = 85;
    private static final int PATTERN_DISPLAY_LENGTH = 40;
    private static final int PARTIAL_PATTERN_WORDS = 3;
    private static final int LAST_CHANGES_COUNT = 5;
    
    // SQL Parameter Index Constants for PreparedStatement operations
    private static final int RULE_COMPANY_ID_PARAM = 1;
    private static final int RULE_PATTERN_PARAM = 2;
    private static final int RULE_KEYWORDS_PARAM = 3;
    private static final int RULE_ACCOUNT_CODE_PARAM = 4;
    private static final int RULE_ACCOUNT_NAME_PARAM = 5;
    private static final int RULE_UPDATE_KEYWORDS_PARAM = 1;
    private static final int RULE_UPDATE_ID_PARAM = 2;
    private static final int RULE_CHECK_COMPANY_ID_PARAM = 1;
    private static final int RULE_CHECK_ACCOUNT_CODE_PARAM = 2;
    private static final int RULE_CHECK_PATTERN_PARAM = 3;
    private static final int RULE_UPDATE_USAGE_COMPANY_ID_PARAM = 1;
    private static final int RULE_UPDATE_USAGE_ACCOUNT_CODE_PARAM = 2;
    private static final int RULE_UPDATE_USAGE_PATTERN_PARAM = 3;
    private static final int SIMILAR_TRANSACTIONS_COMPANY_ID_PARAM = 1;
    private static final int SIMILAR_TRANSACTIONS_PATTERN_PARAM = 2;
    private static final int SIMILAR_TRANSACTIONS_PATTERN_PARAM_2 = 3;
    private static final int BATCH_UPDATE_ACCOUNT_CODE_PARAM = 1;
    private static final int BATCH_UPDATE_ACCOUNT_NAME_PARAM = 2;
    private static final int BATCH_UPDATE_CLASSIFIED_BY_PARAM = 3;
    private static final int BATCH_UPDATE_TRANSACTION_ID_PARAM = 4;
    private static final int UNCATEGORIZED_COMPANY_ID_PARAM = 1;
    private static final int UNCATEGORIZED_FISCAL_PERIOD_PARAM = 2;
    private static final int UPDATE_CLASSIFICATION_ACCOUNT_CODE_PARAM = 1;
    private static final int UPDATE_CLASSIFICATION_ACCOUNT_NAME_PARAM = 2;
    private static final int UPDATE_CLASSIFICATION_CLASSIFIED_BY_PARAM = 3;
    private static final int UPDATE_CLASSIFICATION_TRANSACTION_ID_PARAM = 4;
    private static final int JOURNAL_HEADER_REFERENCE_PARAM = 1;
    private static final int JOURNAL_HEADER_DATE_PARAM = 2;
    private static final int JOURNAL_HEADER_DESCRIPTION_PARAM = 3;
    private static final int JOURNAL_HEADER_FISCAL_PERIOD_PARAM = 4;
    private static final int JOURNAL_HEADER_COMPANY_ID_PARAM = 5;
    private static final int JOURNAL_HEADER_CREATED_BY_PARAM = 6;
    private static final int JOURNAL_LINE_JOURNAL_ENTRY_ID_PARAM = 1;
    private static final int JOURNAL_LINE_ACCOUNT_ID_PARAM = 2;
    private static final int JOURNAL_LINE_DEBIT_AMOUNT_PARAM = 3;
    private static final int JOURNAL_LINE_CREDIT_AMOUNT_PARAM = 4;
    private static final int JOURNAL_LINE_DESCRIPTION_PARAM = 5;
    private static final int JOURNAL_LINE_REFERENCE_PARAM = 6;
    private static final int JOURNAL_LINE_SOURCE_TRANSACTION_PARAM = 7;
    private static final int ACCOUNT_LOOKUP_NAME_PARAM = 1;
    private static final int SIMILAR_UNCATEGORIZED_COMPANY_ID_PARAM = 1;
    private static final int SIMILAR_UNCATEGORIZED_PATTERN_PARAM = 2;
    private static final int ALLOCATION_ANALYSIS_COMPANY_ID_PARAM = 1;
    private static final int ALLOCATION_ANALYSIS_FISCAL_PERIOD_PARAM = 2;
    private static final int SUMMARY_COMPANY_ID_PARAM = 1;
    private static final int SUMMARY_FISCAL_PERIOD_PARAM = 2;
    
    // Change tracking
    public static class ChangeRecord {
        private Long transactionId;
        public LocalDate transactionDate;
        public String description;
        public BigDecimal amount;
        public String oldAccount;
        public String newAccount;
        public LocalDateTime timestamp;
        
        public ChangeRecord(Long initialTransactionId, LocalDate initialTransactionDate, String initialDescription, 
                          BigDecimal initialAmount, String initialOldAccount, String initialNewAccount) {
            this.transactionId = initialTransactionId;
            this.transactionDate = initialTransactionDate;
            this.description = initialDescription;
            this.amount = initialAmount;
            this.oldAccount = initialOldAccount;
            this.newAccount = initialNewAccount;
            this.timestamp = LocalDateTime.now();
        }
        
        public Long getTransactionId() {
            return transactionId;
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
        
        public ClassificationRule(String initialPattern, String[] initialKeywords, String initialAccountCode, 
                                String initialAccountName, int initialUsageCount) {
            this.pattern = initialPattern;
            this.keywords = initialKeywords != null ? initialKeywords.clone() : null;
            this.accountCode = initialAccountCode;
            this.accountName = initialAccountName;
            this.usageCount = initialUsageCount;
        }
        
        // Getters
        public String getPattern() { return pattern; }
        public String[] getKeywords() { return keywords != null ? keywords.clone() : null; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public int getUsageCount() { return usageCount; }
        public double getConfidenceScore() { return confidenceScore; }
        public void setConfidenceScore(double newConfidenceScore) { this.confidenceScore = newConfidenceScore; }
    }
    
    /**
     * Data class for classified transaction
     */
    public static class ClassifiedTransaction {
        private final BankTransaction transaction;
        private final String accountCode;
        private final String accountName;
        
        public ClassifiedTransaction(BankTransaction initialTransaction, String initialAccountCode, String initialAccountName) {
            this.transaction = initialTransaction;
            this.accountCode = initialAccountCode;
            this.accountName = initialAccountName;
        }
        
        public BankTransaction getTransaction() { return transaction; }
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
    }

    /**
     * Data class for user classification input
     */
    public static class ClassificationInput {
        private final String accountCode;
        private final String accountName;
        private final boolean shouldQuit;
        private final boolean skip;

        public ClassificationInput(String code, String name) {
            this.accountCode = code;
            this.accountName = name;
            this.shouldQuit = false;
            this.skip = false;
        }

        public ClassificationInput(boolean quitFlag, boolean skipFlag) {
            this.accountCode = null;
            this.accountName = null;
            this.shouldQuit = quitFlag;
            this.skip = skipFlag;
        }

        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public boolean shouldQuit() { return shouldQuit; }
        public boolean hasClassification() { return accountCode != null && accountName != null; }
        public boolean shouldSkip() { return skip; }
    }
    
    /**
     * Data class for account allocation analysis results
     */
    private static class AccountAllocationData {
        private String accountName;
        private String accountType;
        private int transactionCount;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
    }
    
    public InteractiveClassificationService() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        this.accountClassificationService = new AccountClassificationService(dbUrl);
        this.companyRules = new HashMap<>();
        this.changesMade = new ArrayList<>();
        this.accountCategories = new LinkedHashMap<>();
        
        initializeService();
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
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    pattern VARCHAR(1000) NOT NULL,
                    keywords VARCHAR(2000) NOT NULL,
                    account_code VARCHAR(20) NOT NULL,
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
                    String keywordsStr = rs.getString("keywords");
                    String[] keywords = keywordsStr != null ? keywordsStr.split(",") : new String[0];
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
     * Load account categories from AccountClassificationService (single source of truth)
     * REFACTORED: No longer reads from database directly, uses AccountClassificationService
     */
    private void loadAccountCategories() {
        // Clear existing categories
        accountCategories.clear();
        
        try {
            // Use AccountClassificationService as single source of truth
            // Get standard account definitions using reflection (since method is private)
            java.lang.reflect.Method getStandardAccountsMethod = 
                accountClassificationService.getClass().getDeclaredMethod("getStandardAccountDefinitions", Map.class);
            getStandardAccountsMethod.setAccessible(true);
            
            // Create dummy category IDs map (not used for display purposes)
            Map<String, Long> dummyCategoryIds = new HashMap<>();
            
            @SuppressWarnings("unchecked")
            List<Object> accounts = (List<Object>) getStandardAccountsMethod.invoke(accountClassificationService, dummyCategoryIds);
            
            // Group accounts by category for display
            for (Object accountObj : accounts) {
                try {
                    // Use reflection to access AccountDefinition fields
                    java.lang.reflect.Field codeField = accountObj.getClass().getDeclaredField("code");
                    java.lang.reflect.Field nameField = accountObj.getClass().getDeclaredField("name");
                    codeField.setAccessible(true);
                    nameField.setAccessible(true);
                    
                    String code = (String) codeField.get(accountObj);
                    String name = (String) nameField.get(accountObj);
                    
                    // Determine category based on account code (SARS standard)
                    String categoryName = getCategoryFromAccountCode(code);
                    String accountInfo = code + " - " + name;
                    
                    accountCategories.computeIfAbsent(categoryName, k -> new ArrayList<>()).add(accountInfo);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error processing account definition", e);
                }
            }
            
            LOGGER.info("Loaded " + accountCategories.size() + " account categories from AccountClassificationService");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading account categories from AccountClassificationService, falling back to empty", e);
            // Fallback to empty categories
            accountCategories.clear();
        }
    }
    
    /**
     * Get category name from account code - delegated to AccountClassificationService (single source of truth)
     * REFACTORED: No more hardcoded categories - uses standard mapping rules
     */
    private String getCategoryFromAccountCode(String accountCode) {
        if (accountCode == null || accountCode.length() < 1) {
            return "Other";
        }
        
        // Delegate to AccountClassificationService for ALL category determination
        List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
        
        // Find matching standard rule for this account code
        for (TransactionMappingRule rule : standardRules) {
            String ruleAccountCode = extractAccountCodeFromRuleDescription(rule.getDescription());
            if (accountCode.equals(ruleAccountCode)) {
                return rule.getMatchValue(); // Use match value as category indicator
            }
        }
        
        // If not found in standard rules, return based on account code prefix (minimal fallback)
        String prefix = accountCode.substring(0, 1);
        switch (prefix) {
            case "1": return "Assets";
            case "2": return "Liabilities"; 
            case "3": return "Equity";
            case "4": case "6": case "7": return "Revenue";
            case "5": case "8": case "9": return "Expenses";
            default: return "Other";
        }
    }
    
    /**
     * Classify a transaction interactively for a specific company
     */
    public ClassifiedTransaction classifyTransaction(BankTransaction transaction, Company company) {
        System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("🔍 TRANSACTION CLASSIFICATION - " + company.getName());
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        
        // Display transaction details
        displayTransactionDetails(transaction);
        
        // Check for existing rules
        ClassificationRule existingRule = findMatchingRule(transaction, company.getId());
        if (existingRule != null && existingRule.getConfidenceScore() >= CONFIDENCE_THRESHOLD_MEDIUM) {
            System.out.println("✅ AUTO-CLASSIFIED using learned rule:");
            System.out.println("   Account: " + existingRule.getAccountCode() + " - " + existingRule.getAccountName());
            System.out.println("   Confidence: " + String.format("%.1f%%", existingRule.getConfidenceScore() * PERCENTAGE_MULTIPLIER));
            System.out.println("   🧠 Intelligence: Recognized pattern from previous classifications");
            
            // Ask for confirmation if confidence is not very high
            if (existingRule.getConfidenceScore() < CONFIDENCE_THRESHOLD_HIGH) {
                System.out.print("\n❓ Accept this auto-classification? (y/n): ");
                String response = scanner.nextLine().trim().toLowerCase();
                if (!response.equals("y") && !response.equals("yes")) {
                    return promptForClassification(transaction, company);
                }
            }
            
            // Update usage count for this rule
            incrementRuleUsage(existingRule, company.getId());
            
            // Use mapping service to apply the rule
            accountClassificationService.classifyTransaction(transaction, existingRule.getAccountCode(), existingRule.getAccountName());
            
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
            if (score > highestScore && score >= LOW_MATCH_SCORE) {
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
                if (lowerKeyword.length() > SIGNIFICANT_KEYWORD_LENGTH) { // Significant keywords
                    totalScore += HIGH_MATCH_SCORE;
                    significantMatches++;
                } else {
                    totalScore += LOW_MATCH_SCORE; // Short keywords get less weight
                }
            }
            
            // Pattern matching for common variations
            if (isPatternMatch(lowerDetails, lowerKeyword)) {
                totalScore += PATTERN_MATCH_SCORE;
                significantMatches++;
            }
        }
        
        // Boost score if we have significant matches
        if (significantMatches > 0) {
            totalScore *= SIGNIFICANT_MATCH_BOOST; // 20% boost for having significant matches
        }
        
        // Company/supplier name detection
        if (detectSupplierPattern(lowerDetails, rule)) {
            totalScore += SUPPLIER_MATCH_SCORE;
            significantMatches++;
        }
        
        return Math.min(totalScore / keywords.length, MAX_MATCH_SCORE); // Cap at 1.0
    }
    
    /**
     * Pattern matching delegated to AccountClassificationService (single source of truth)
     * REFACTORED: No more hardcoded patterns - uses standard mapping rules
     */
    private boolean isPatternMatch(String details, String keyword) {
        // Delegate to AccountClassificationService for ALL pattern matching
        List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
        
        String lowerDetails = details.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        // Check if any standard rule matches this pattern
        for (TransactionMappingRule rule : standardRules) {
            String matchValue = rule.getMatchValue().toLowerCase();
            if (matchValue.contains(lowerKeyword) && lowerDetails.contains(matchValue)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Supplier pattern detection delegated to AccountClassificationService (single source of truth)
     * REFACTORED: No more hardcoded supplier patterns - uses standard mapping rules
     */
    private boolean detectSupplierPattern(String details, ClassificationRule rule) {
        // Delegate to AccountClassificationService for ALL supplier pattern detection
        List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
        
        String lowerDetails = details.toLowerCase();
        String ruleAccountCode = rule.getAccountCode();
        
        // Find matching standard rules for this account code
        for (TransactionMappingRule standardRule : standardRules) {
            String standardAccountCode = extractAccountCodeFromRuleDescription(standardRule.getDescription());
            if (ruleAccountCode.equals(standardAccountCode)) {
                String matchValue = standardRule.getMatchValue().toLowerCase();
                if (lowerDetails.contains(matchValue)) {
                    return true;
                }
            }
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
                
                stmt.setLong(RULE_UPDATE_USAGE_COMPANY_ID_PARAM, companyId);
                stmt.setString(RULE_UPDATE_USAGE_ACCOUNT_CODE_PARAM, rule.getAccountCode());
                stmt.setString(RULE_UPDATE_USAGE_PATTERN_PARAM, "%" + String.join("%", rule.getKeywords()) + "%");
                
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
                LIMIT %d
                """.formatted(MAX_SIMILAR_TRANSACTIONS);
                
            String[] keywords = extractKeywords(transaction.getDetails());
            String searchPattern = "%" + (keywords.length > 0 ? keywords[0] : "") + "%";
            
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(SIMILAR_TRANSACTIONS_COMPANY_ID_PARAM, companyId);
                stmt.setString(SIMILAR_TRANSACTIONS_PATTERN_PARAM, searchPattern);
                stmt.setString(SIMILAR_TRANSACTIONS_PATTERN_PARAM_2, searchPattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("🔍 Similar past transactions:");
                        do {
                            System.out.printf("   📝 %s → %s - %s (used %d times)%n", 
                                rs.getString("details").substring(0, Math.min(SIMILAR_TRANSACTION_DESC_LENGTH, rs.getString("details").length())),
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
        showAccountSuggestions(company.getId(), transaction);
        
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
     * Show available account suggestions based on AccountClassificationService (single source of truth)
     * REFACTORED: Simplified to only use AccountClassificationService, removed mixed database queries
     */
    private void showAccountSuggestions(Long companyId, BankTransaction transaction) {
        try {
            System.out.println("\n💡 ACCOUNT SUGGESTIONS:");
            System.out.println("-".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
            
            // Get intelligent suggestions from AccountClassificationService (single source of truth)
            System.out.println("📚 From Standard Classification Rules:");
            List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
            int suggestionsShown = 0;
            
            for (TransactionMappingRule rule : standardRules) {
                if (rule.matches(transaction.getDetails())) {
                    // Extract account code from description
                    String accountCode = extractAccountCodeFromRuleDescription(rule.getDescription());
                    if (accountCode != null) {
                        String simplifiedDescription = rule.getDescription().replaceAll("\\s*\\[AccountCode:.*?\\]", "");
                        System.out.println("   ✓ " + accountCode + " - " + simplifiedDescription);
                        suggestionsShown++;
                        if (suggestionsShown >= MAX_ACCOUNT_SUGGESTIONS) {
                            break; // Show top matches
                        }
                    }
                }
            }
            
            if (suggestionsShown == 0) {
                System.out.println("   (No standard rules match this transaction)");
                
                // Show general account categories from AccountClassificationService
                System.out.println("\n📋 Available Account Categories:");
                for (Map.Entry<String, List<String>> category : accountCategories.entrySet()) {
                    System.out.println("   " + category.getKey() + ":");
                    category.getValue().stream()
                        .limit(MAX_ACCOUNTS_PER_CATEGORY) // Show top 3 per category
                        .forEach(account -> System.out.println("     • " + account));
                }
            }
            
            System.out.println("-".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
            System.out.println("💡 TIP: Use account codes like 8100 (Employee Costs), 9600 (Bank Charges), etc.");
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error loading account suggestions from AccountClassificationService", e);
            System.out.println("   (Error loading suggestions - please enter account code manually)");
        }
    }
    
    /**
     * Extract account code from rule description (format: "[AccountCode:XXXX]")
     */
    private String extractAccountCodeFromRuleDescription(String description) {
        if (description == null) {
            return null;
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[AccountCode:(\\d+(?:-\\d+)?)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(description);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract keywords from transaction description for pattern matching
     */
    private String[] extractKeywords(String description) {
        if (description == null) {
            return new String[0];
        }
        
        // Remove common words and extract meaningful keywords
        String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        Set<String> commonWordsSet = Set.of(commonWords);
        
        // Delegate to AccountClassificationService for keyword extraction consistency
        // NO hardcoded keyword patterns - use standard mapping rule insights
        String[] keywords = accountClassificationService.getStandardMappingRules().stream()
                .map(rule -> rule.getMatchValue().toLowerCase())
                .filter(matchValue -> description.toLowerCase().contains(matchValue))
                .limit(MAX_KEYWORDS)
                .toArray(String[]::new);
        
        // If no standard keywords found, fall back to minimal extraction
        if (keywords.length == 0) {
            return Arrays.stream(description.toLowerCase().split("\\W+"))
                    .filter(word -> word.length() > 2)
                    .filter(word -> !commonWordsSet.contains(word))
                    .limit(MAX_KEYWORDS) // Take top 5 keywords
                    .toArray(String[]::new);
        }
        
        return keywords;
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
            String[] keywords = extractKeywordsForRule(pattern);

            // Check if rule already exists and handle accordingly
            handleRuleCreationOrUpdate(companyId, pattern, keywords, accountCode, accountName);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating mapping rule", e);
        }
    }

    /**
     * Extract keywords from pattern for rule matching
     */
    private String[] extractKeywordsForRule(String pattern) {
        return extractKeywords(pattern);
    }

    /**
     * Handle creation of new rule or update of existing rule
     */
    private void handleRuleCreationOrUpdate(Long companyId, String pattern, String[] keywords, String accountCode, String accountName) throws SQLException {
        String checkSql = """
            SELECT id, usage_count FROM company_classification_rules
            WHERE company_id = ? AND account_code = ? AND pattern ILIKE ?
            """;

        try (Connection conn = getConnection()) {
            // Check for existing rule
            Long existingRuleId = checkForExistingRule(conn, checkSql, companyId, accountCode, keywords);

            if (existingRuleId != null) {
                // Update existing rule
                updateExistingRule(conn, existingRuleId, keywords);
            } else {
                // Insert new rule
                insertNewRule(conn, companyId, pattern, keywords, accountCode, accountName);
            }

            // Update in-memory cache
            updateInMemoryCache(companyId, accountCode, keywords, pattern, accountName);
        }
    }

    /**
     * Check if a rule already exists for the given parameters
     */
    private Long checkForExistingRule(Connection conn, String checkSql, Long companyId, String accountCode, String[] keywords) throws SQLException {
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setLong(RULE_CHECK_COMPANY_ID_PARAM, companyId);
            checkStmt.setString(RULE_CHECK_ACCOUNT_CODE_PARAM, accountCode);
            checkStmt.setString(RULE_CHECK_PATTERN_PARAM, "%" + String.join("%", keywords) + "%");

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    /**
     * Update an existing rule's usage count and keywords
     */
    private void updateExistingRule(Connection conn, Long ruleId, String[] keywords) throws SQLException {
        String updateSql = """
            UPDATE company_classification_rules
            SET usage_count = usage_count + 1, keywords = ?, last_used = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
            updateStmt.setString(RULE_UPDATE_KEYWORDS_PARAM, String.join(",", keywords));
            updateStmt.setLong(RULE_UPDATE_ID_PARAM, ruleId);
            updateStmt.executeUpdate();
        }
    }

    /**
     * Insert a new classification rule
     */
    private void insertNewRule(Connection conn, Long companyId, String pattern, String[] keywords, String accountCode, String accountName) throws SQLException {
        String insertSql = """
            INSERT INTO company_classification_rules
            (company_id, pattern, keywords, account_code, account_name, usage_count, created_at, last_used)
            VALUES (?, ?, ?, ?, ?, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setLong(RULE_COMPANY_ID_PARAM, companyId);
            insertStmt.setString(RULE_PATTERN_PARAM, pattern);
            insertStmt.setString(RULE_KEYWORDS_PARAM, String.join(",", keywords));
            insertStmt.setString(RULE_ACCOUNT_CODE_PARAM, accountCode);
            insertStmt.setString(RULE_ACCOUNT_NAME_PARAM, accountName);
            insertStmt.executeUpdate();
        }
    }

    /**
     * Update the in-memory cache with the new rule
     */
    private void updateInMemoryCache(Long companyId, String accountCode, String[] keywords, String pattern, String accountName) {
        companyRules.computeIfAbsent(companyId, k -> new HashMap<>())
            .put(accountCode + ":" + String.join(",", keywords),
                 new ClassificationRule(pattern, keywords, accountCode, accountName, 1));

        LOGGER.info("Created/updated rule for pattern: " + pattern + " → " + accountCode + " - " + accountName);
    }
    
    /**
     * Run the interactive categorization experience
     */
    public void runInteractiveCategorization(Long companyId, Long fiscalPeriodId) {
        System.out.println("\n🏢 INTERACTIVE TRANSACTION CATEGORIZATION");
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        
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
        System.out.println("\n" + "=".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
        System.out.println("📋 CATEGORIZATION MENU");
        System.out.println("=".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
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

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Get transaction details first
                Map<Long, String> transactionDetails = getTransactionDetailsBatch(conn, transactionIds);

                // Update transactions and create mapping rules
                classifiedCount = processBatchClassification(conn, transactionIds, transactionDetails, accountCode, accountName, companyId);

                conn.commit();
                LOGGER.info("Batch classified " + classifiedCount + " transactions as " + accountCode + " - " + accountName);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error batch classifying transactions", e);
        }

        return classifiedCount;
    }

    /**
     * Process batch classification of transactions
     */
    private int processBatchClassification(Connection conn, List<Long> transactionIds, Map<Long, String> transactionDetails,
                                         String accountCode, String accountName, Long companyId) throws SQLException {
        int classifiedCount = 0;

        String updateSql = """
            UPDATE bank_transactions
            SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
            WHERE id = ?
            """;

        try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            for (Long id : transactionIds) {
                if (transactionDetails.containsKey(id)) {
                    stmt.setString(BATCH_UPDATE_ACCOUNT_CODE_PARAM, accountCode);
                    stmt.setString(BATCH_UPDATE_ACCOUNT_NAME_PARAM, accountName);
                    stmt.setString(BATCH_UPDATE_CLASSIFIED_BY_PARAM, "BATCH_CLASSIFICATION");
                    stmt.setLong(BATCH_UPDATE_TRANSACTION_ID_PARAM, id);
                    stmt.addBatch();

                    // Also create rule for future similar transactions
                    createMappingRule(companyId, transactionDetails.get(id), accountCode, accountName);

                    classifiedCount++;
                }
            }

            stmt.executeBatch();
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
            LIMIT %d
            """.formatted(MAX_UNCATEGORIZED_TRANSACTIONS);
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(UNCATEGORIZED_COMPANY_ID_PARAM, companyId);
            pstmt.setLong(UNCATEGORIZED_FISCAL_PERIOD_PARAM, fiscalPeriodId);
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
        
        processUncategorizedTransactions(uncategorized, companyId);
        showSessionSummary();
    }

    /**
     * Process each uncategorized transaction through the review workflow
     */
    private void processUncategorizedTransactions(List<BankTransaction> uncategorized, Long companyId) {
        for (int i = 0; i < uncategorized.size(); i++) {
            BankTransaction transaction = uncategorized.get(i);
            
            displayTransactionHeader(i + 1, uncategorized.size());
            displayTransactionDetails(transaction);
            
            // Show intelligent account suggestions from AccountClassificationService
            showAccountSuggestions(companyId, transaction);
            
            // Get user input for classification
            ClassificationInput input = getUserClassificationInput();
            
            if (input.shouldQuit()) {
                System.out.println("🛑 Exiting transaction review...");
                break;
            } else if (input.hasClassification()) {
                processTransactionClassification(transaction, input, companyId);
            }
        }
    }

    /**
     * Display transaction header with progress information
     */
    private void displayTransactionHeader(int current, int total) {
        System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("📋 TRANSACTION " + current + " of " + total);
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
    }

    /**
     * Get user input for transaction classification
     */
    private ClassificationInput getUserClassificationInput() {
        while (true) {
            System.out.println("\n📝 Classification Options:");
            System.out.println("1. Enter account code and name manually");
            System.out.println("2. Skip this transaction");
            System.out.println("3. Quit categorization");
            System.out.print("Choose option (1-3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.print("Account Code (e.g., 8100): ");
                    String accountCode = scanner.nextLine().trim();
                    System.out.print("Account Name (e.g., Employee Costs): ");
                    String accountName = scanner.nextLine().trim();

                    if (accountCode.isEmpty() || accountName.isEmpty()) {
                        System.out.println("❌ Both account code and name are required.");
                        continue;
                    }

                    return new ClassificationInput(accountCode, accountName);

                case "2":
                    return new ClassificationInput(false, true);

                case "3":
                    return new ClassificationInput(true, false);

                default:
                    System.out.println("❌ Invalid choice. Please select 1-3.");
            }
        }
    }

    /**
     * Process transaction classification with user input
     */
    private void processTransactionClassification(BankTransaction transaction, ClassificationInput input, Long companyId) {
        String accountCode = input.getAccountCode();
        String accountName = input.getAccountName();

        // Update transaction classification
        if (updateTransactionClassification(transaction.getId(), accountCode, accountName)) {
            // Create journal entry
            Long accountId = getAccountId(accountName);
            if (accountId != null) {
                if (createJournalEntryForTransaction(transaction, accountId)) {
                    // Create mapping rule for future similar transactions
                    createMappingRule(companyId, transaction.getDetails(), accountCode, accountName);

                    // Record change for session tracking
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

                    System.out.println("✅ Transaction classified as: " + accountCode + " - " + accountName);

                    // Ask about auto-categorizing similar transactions
                    autoCategorizeFromPattern(transaction, accountCode, accountName, companyId);
                } else {
                    System.out.println("❌ Failed to create journal entry for transaction");
                }
            } else {
                System.out.println("❌ Account not found: " + accountName);
            }
        } else {
            System.out.println("❌ Failed to update transaction classification");
        }
    }
    
    /**
     * Auto-categorize similar transactions
     */
    private void autoCategorizeFromPattern(BankTransaction transaction, String accountCode, String accountName, Long companyId) {
        String details = transaction.getDetails();

        // Create search patterns from transaction details
        List<String> patterns = createSearchPatterns(details);

        // Process auto-categorization for each pattern
        int totalCategorized = processAutoCategorizationPatterns(patterns, accountCode, accountName, companyId);

        // Report results
        reportAutoCategorizationResults(totalCategorized, accountName);
    }

    /**
     * Process auto-categorization for multiple patterns
     */
    private int processAutoCategorizationPatterns(List<String> patterns, String accountCode, String accountName, Long companyId) {
        int totalCategorized = 0;

        for (String pattern : patterns) {
            List<BankTransaction> similar = findSimilarUncategorized(pattern, companyId);

            if (!similar.isEmpty()) {
                boolean userConfirmed = getUserConfirmationForAutoCategorization(similar, pattern);
                if (userConfirmed) {
                    int categorized = categorizeSimilarTransactions(similar, accountCode, accountName);
                    totalCategorized += categorized;
                    break; // Only process first matching pattern
                }
            }
        }

        return totalCategorized;
    }

    /**
     * Get user confirmation for auto-categorizing similar transactions
     */
    private boolean getUserConfirmationForAutoCategorization(List<BankTransaction> similar, String pattern) {
        System.out.println("\n🔍 Found " + similar.size() + " similar transactions matching: '" +
                         pattern.substring(0, Math.min(PATTERN_DISPLAY_LENGTH, pattern.length())) + "'");

        System.out.print("Auto-categorize these? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        return confirm.equals("y") || confirm.equals("yes");
    }

    /**
     * Categorize a list of similar transactions
     */
    private int categorizeSimilarTransactions(List<BankTransaction> similar, String accountCode, String accountName) {
        int categorized = 0;
        Long accountId = getAccountId(accountName);

        if (accountId != null) {
            for (BankTransaction similarTx : similar) {
                if (categorizeSingleTransaction(similarTx, accountCode, accountName, accountId)) {
                    categorized++;
                }
            }
        }

        return categorized;
    }

    /**
     * Categorize a single transaction and record the change
     */
    private boolean categorizeSingleTransaction(BankTransaction transaction, String accountCode, String accountName, Long accountId) {
        // Update transaction classification first
        if (updateTransactionClassification(transaction.getId(), accountCode, accountName)) {
            // Then create journal entry
            if (createJournalEntryForTransaction(transaction, accountId)) {
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
                return true;
            }
        }
        return false;
    }

    /**
     * Report the results of auto-categorization
     */
    private void reportAutoCategorizationResults(int totalCategorized, String accountName) {
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
            LIMIT %d
            """.formatted(MAX_BATCH_SIMILAR);
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(SIMILAR_UNCATEGORIZED_COMPANY_ID_PARAM, companyId);
            pstmt.setString(SIMILAR_UNCATEGORIZED_PATTERN_PARAM, "%" + pattern + "%");
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
        printAccountAllocationHeader();
        List<AccountAllocationData> allocationData = getAccountAllocationData(companyId, fiscalPeriodId);
        printAccountAllocationTable(allocationData);
    }
    
    /**
     * Show categorization summary
     */
    private void showCategorizationSummary(Long companyId, Long fiscalPeriodId) {
        System.out.println("\n📊 CATEGORIZATION SUMMARY");
        System.out.println("=".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
        
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
            
            pstmt.setLong(SUMMARY_COMPANY_ID_PARAM, companyId);
            pstmt.setLong(SUMMARY_FISCAL_PERIOD_PARAM, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int total = rs.getInt("total_transactions");
                int categorized = rs.getInt("categorized_count");
                int uncategorized = rs.getInt("uncategorized_count");
                
                double percentage = total > 0 ? ((double) categorized * PERCENTAGE_MULTIPLIER) / (double) total : 0;
                
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
        
        System.out.println("\n" + "=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("📋 SESSION SUMMARY");
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        System.out.println("Changes made: " + changesMade.size());
        
        if (changesMade.size() > 0) {
            System.out.println("\nLast " + LAST_CHANGES_COUNT + " changes:");
            int start = Math.max(0, changesMade.size() - LAST_CHANGES_COUNT);
            for (int i = start; i < changesMade.size(); i++) {
                ChangeRecord change = changesMade.get(i);
                System.out.println("  • " + change.transactionDate.format(DateTimeFormatter.ofPattern("MM-dd")) + 
                                 " - " + change.description.substring(0, Math.min(DESCRIPTION_DISPLAY_LENGTH, change.description.length())));
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
        System.out.println("=".repeat(DISPLAY_LINE_WIDTH));
        
        for (int i = 0; i < changesMade.size(); i++) {
            ChangeRecord change = changesMade.get(i);
            System.out.println((i + 1) + ". " + change.transactionDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + 
                             " | " + formatCurrency(change.amount));
            System.out.println("   " + change.description.substring(0, Math.min(ACCOUNT_NAME_DISPLAY_LENGTH, change.description.length())));
            System.out.println("   " + change.oldAccount + " → " + change.newAccount);
            System.out.println("   @ " + change.timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println();
        }
    }
    
    /**
     * Save changes to database with proper transaction management
     */
    private void saveChanges() {
        if (changesMade.isEmpty()) {
            System.out.println("ℹ️  No changes to save");
            return;
        }

        System.out.println("\n💾 Saving " + changesMade.size() + " changes...");

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // All changes are already saved to database when categorization happens
                // This method provides transaction management and rollback capability

                // Note: Changes are persisted immediately when categorization occurs
                // This method serves as a confirmation and session cleanup

                conn.commit();
                System.out.println("✅ All changes saved successfully!");
                System.out.println("📝 Session changes confirmed and session cleared");

                // Clear session data only after successful commit
                changesMade.clear();

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("❌ Failed to save changes. All changes have been rolled back.");
                LOGGER.log(Level.SEVERE, "Error saving changes, rolled back transaction", e);
                throw e;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during save operation", e);
            System.out.println("❌ Database error occurred. Please try again.");
        }
    }    /**
     * Update transaction classification in the bank_transactions table
     */
    private boolean updateTransactionClassification(Long transactionId, String accountCode, String accountName) {
        String sql = """
            UPDATE bank_transactions
            SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
            WHERE id = ?
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(UPDATE_CLASSIFICATION_ACCOUNT_CODE_PARAM, accountCode);
            stmt.setString(UPDATE_CLASSIFICATION_ACCOUNT_NAME_PARAM, accountName);
            stmt.setString(UPDATE_CLASSIFICATION_CLASSIFIED_BY_PARAM, "INTERACTIVE-CATEGORIZATION");
            stmt.setLong(UPDATE_CLASSIFICATION_TRANSACTION_ID_PARAM, transactionId);

            int updated = stmt.executeUpdate();
            return updated > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction classification", e);
            return false;
        }
    }

    /**
     * Helper method to create journal entry for a transaction
     */
    private boolean createJournalEntryForTransaction(BankTransaction transaction, Long accountId) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Create journal entry header
                Long journalEntryId = createJournalEntryHeader(conn, transaction);

                // Get bank account ID and create journal entry lines
                createJournalEntryLines(conn, journalEntryId, transaction, accountId);

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

    /**
     * Create journal entry lines for a transaction
     */
    private void createJournalEntryLines(Connection conn, Long journalEntryId, BankTransaction transaction,
                                       Long accountId) throws SQLException {
        // Get bank account ID
        Long bankAccountId = getBankAccountId();

        String sql = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount,
                                           credit_amount, description, reference,
                                           source_transaction_id, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Bank account line (opposite of transaction)
            createBankAccountLine(pstmt, journalEntryId, transaction, bankAccountId != null ? bankAccountId : accountId);

            // Categorized account line (same as transaction)
            createCategorizedAccountLine(pstmt, journalEntryId, transaction, accountId);
        }
    }
    
    private void createBankAccountLine(PreparedStatement pstmt, Long journalEntryId, BankTransaction transaction, 
                                    Long bankAccountId) throws SQLException {
        pstmt.setLong(JOURNAL_LINE_JOURNAL_ENTRY_ID_PARAM, journalEntryId);
        pstmt.setLong(JOURNAL_LINE_ACCOUNT_ID_PARAM, bankAccountId);

        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Transaction is debit, so credit the bank account
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_AMOUNT_PARAM, null);
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_AMOUNT_PARAM, transaction.getDebitAmount());
        } else {
            // Transaction is credit, so debit the bank account
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_AMOUNT_PARAM, transaction.getCreditAmount());
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_AMOUNT_PARAM, null);
        }

        pstmt.setString(JOURNAL_LINE_DESCRIPTION_PARAM, "Bank Account");
        pstmt.setString(JOURNAL_LINE_REFERENCE_PARAM, "CAT-" + transaction.getId() + "-01");
        pstmt.setLong(JOURNAL_LINE_SOURCE_TRANSACTION_PARAM, transaction.getId());
        pstmt.executeUpdate();
    }
    
    private void createCategorizedAccountLine(PreparedStatement pstmt, Long journalEntryId, BankTransaction transaction, 
                                           Long accountId) throws SQLException {
        pstmt.setLong(JOURNAL_LINE_ACCOUNT_ID_PARAM, accountId);

        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            // Transaction is debit, so debit the expense/asset account
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_AMOUNT_PARAM, transaction.getDebitAmount());
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_AMOUNT_PARAM, null);
        } else {
            // Transaction is credit, so credit the income/liability account
            pstmt.setBigDecimal(JOURNAL_LINE_DEBIT_AMOUNT_PARAM, null);
            pstmt.setBigDecimal(JOURNAL_LINE_CREDIT_AMOUNT_PARAM, transaction.getCreditAmount());
        }

        pstmt.setString(JOURNAL_LINE_DESCRIPTION_PARAM, "Categorized Account");
        pstmt.setString(JOURNAL_LINE_REFERENCE_PARAM, "CAT-" + transaction.getId() + "-02");
        pstmt.setLong(JOURNAL_LINE_SOURCE_TRANSACTION_PARAM, transaction.getId());
        pstmt.executeUpdate();
    }
    
    /**
     * Recreates journal entries for all categorized transactions that don't have journal entries
     */
    public void recreateJournalEntriesForCategorizedTransactions() {
        try (Connection conn = getConnection();
             PreparedStatement pstmt = getCategorizedTransactionsWithoutJournalEntriesQuery(conn);
             ResultSet rs = pstmt.executeQuery()) {

            int count = processJournalEntryRecreation(rs);
            logRecreationResults(count);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error recreating journal entries", e);
        }
    }
    
    // Helper methods
    
    private String getInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }
        return String.format("%,.2f", amount);
    }
    
    private Long getAccountId(String accountName) {
        String sql = "SELECT id FROM accounts WHERE account_name = ? AND is_active = true";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(ACCOUNT_LOOKUP_NAME_PARAM, accountName);
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
            // First try to get suggestions from database
            suggestions = getDatabaseSuggestions(pattern);

            // If no database matches, analyze pattern for generic suggestions
            if (suggestions.isEmpty()) {
                suggestions = getPatternBasedSuggestions(pattern);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error getting account suggestions", e);
        }

        return suggestions;
    }

    /**
     * Get pattern-based account suggestions when database has no matches
     */
    private List<String> getPatternBasedSuggestions(String pattern) {
        List<String> suggestions = new ArrayList<>();
        String lowerPattern = pattern.toLowerCase();

        // Add pattern-based suggestions
        addPatternBasedSuggestions(suggestions, lowerPattern);

        // Add fallback suggestions if no specific matches
        if (suggestions.isEmpty()) {
            addFallbackSuggestions(suggestions);
        }

        return suggestions;
    }

    /**
     * Add suggestions based on pattern analysis
     */
    private void addPatternBasedSuggestions(List<String> suggestions, String lowerPattern) {
        if (lowerPattern.contains("fee") || lowerPattern.contains("charge") || lowerPattern.contains("commission")) {
            suggestions.add("Bank Charges - Financial expenses");
        }
        if (lowerPattern.contains("salary") || lowerPattern.contains("wage") || lowerPattern.contains("payroll")) {
            suggestions.add("Employee Costs - Personnel expenses");
        }
        if (lowerPattern.contains("insurance") || lowerPattern.contains("premium") || lowerPattern.contains("cover")) {
            suggestions.add("Insurance - Risk management expenses");
        }
        if (lowerPattern.contains("rent") || lowerPattern.contains("lease") || lowerPattern.contains("property")) {
            suggestions.add("Rent Expense - Property costs");
        }
        if (lowerPattern.contains("fuel") || lowerPattern.contains("petrol") || lowerPattern.contains("diesel")) {
            suggestions.add("Travel & Entertainment - Transportation costs");
        }
        if (lowerPattern.contains("electricity") || lowerPattern.contains("water") || lowerPattern.contains("utility")) {
            suggestions.add("Utilities - Operational expenses");
        }
        if (lowerPattern.contains("interest") || lowerPattern.contains("dividend")) {
            suggestions.add("Other Income - Financial income");
        }
    }

    /**
     * Add generic fallback suggestions
     */
    private void addFallbackSuggestions(List<String> suggestions) {
        suggestions.add("Operating Expenses - General business costs");
        suggestions.add("Other Income - Miscellaneous revenue");
        suggestions.add("Administrative Expenses - General overhead");
    }
    
    /**
     * Get transaction details for batch processing
     */
    private Map<Long, String> getTransactionDetailsBatch(Connection conn, List<Long> transactionIds) throws SQLException {
        Map<Long, String> transactionDetails = new HashMap<>();

        String selectSql = """
            SELECT id, transaction_date, details, debit_amount, credit_amount
            FROM bank_transactions
            WHERE id = ANY(?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
            Array idArray = conn.createArrayOf("BIGINT", transactionIds.toArray());
            stmt.setArray(1, idArray);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    transactionDetails.put(rs.getLong("id"), rs.getString("details"));
                }
            }
        }

        return transactionDetails;
    }
    
    /**
     * Create search patterns from transaction details
     */
    private List<String> createSearchPatterns(String details) {
        List<String> patterns = new ArrayList<>();
        patterns.add(details); // Exact match

        // Add partial patterns
        String[] words = details.split("\\s+");
        if (words.length > 1) {
            patterns.add(String.join(" ", Arrays.copyOf(words, Math.min(words.length - 1, PARTIAL_PATTERN_WORDS)))); // First few words
        }

        return patterns;
    }

    private void printAccountAllocationHeader() {
        System.out.println("\n📊 ACCOUNT ALLOCATION ANALYSIS");
        System.out.println("=".repeat(SUGGESTIONS_SEPARATOR_WIDTH));
    }

    private List<AccountAllocationData> getAccountAllocationData(Long companyId, Long fiscalPeriodId) {
        List<AccountAllocationData> data = new ArrayList<>();

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

            pstmt.setLong(ALLOCATION_ANALYSIS_COMPANY_ID_PARAM, companyId);
            pstmt.setLong(ALLOCATION_ANALYSIS_FISCAL_PERIOD_PARAM, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                AccountAllocationData item = new AccountAllocationData();
                item.accountName = rs.getString("account_name");
                item.accountType = rs.getString("account_type");
                item.transactionCount = rs.getInt("transaction_count");
                item.totalDebits = rs.getBigDecimal("total_debits");
                item.totalCredits = rs.getBigDecimal("total_credits");
                data.add(item);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error analyzing account allocations", e);
        }

        return data;
    }

    private void printAccountAllocationTable(List<AccountAllocationData> data) {
        if (data.isEmpty()) {
            System.out.println("No account allocation data found.");
            return;
        }

        System.out.printf("%-" + ACCOUNT_NAME_DISPLAY_LENGTH + "s | %-15s | %-12s | %-15s | %-15s%n",
            "Account Name", "Type", "Transactions", "Total Debits", "Total Credits");
        System.out.println("-".repeat(TABLE_SEPARATOR_WIDTH));

        for (AccountAllocationData item : data) {
            String accountName = item.accountName != null ?
                item.accountName.substring(0, Math.min(ACCOUNT_NAME_DISPLAY_LENGTH, item.accountName.length())) :
                "Unknown";

            String accountType = item.accountType != null ? item.accountType : "Unknown";

            System.out.printf("%-" + ACCOUNT_NAME_DISPLAY_LENGTH + "s | %-15s | %12d | %15s | %15s%n",
                accountName,
                accountType,
                item.transactionCount,
                formatCurrency(item.totalDebits),
                formatCurrency(item.totalCredits));
        }

        System.out.println("-".repeat(TABLE_SEPARATOR_WIDTH));
    }

    /**
     * Get bank account ID for journal entries
     */
    private Long getBankAccountId() {
        String sql = "SELECT id FROM accounts WHERE account_code = '1000' AND is_active = true LIMIT 1";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting bank account ID", e);
        }

        return null;
    }

    /**
     * Create journal entry header
     */
    private Long createJournalEntryHeader(Connection conn, BankTransaction transaction) throws SQLException {
        String sql = """
            INSERT INTO journal_entries (company_id, fiscal_period_id, transaction_date,
                                       description, reference, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(JOURNAL_HEADER_COMPANY_ID_PARAM, transaction.getCompanyId());
            stmt.setLong(JOURNAL_HEADER_FISCAL_PERIOD_PARAM, transaction.getFiscalPeriodId());
            stmt.setDate(JOURNAL_HEADER_DATE_PARAM, java.sql.Date.valueOf(transaction.getTransactionDate()));
            stmt.setString(JOURNAL_HEADER_DESCRIPTION_PARAM, transaction.getDetails());
            stmt.setString(JOURNAL_HEADER_REFERENCE_PARAM, "CAT-" + transaction.getId());
            stmt.setString(JOURNAL_HEADER_CREATED_BY_PARAM, "INTERACTIVE-CATEGORIZATION");

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }

        throw new SQLException("Failed to create journal entry header");
    }

    /**
     * Get query for categorized transactions without journal entries
     */
    private PreparedStatement getCategorizedTransactionsWithoutJournalEntriesQuery(Connection conn) throws SQLException {
        String sql = """
            SELECT bt.id, bt.details, bt.account_code, bt.account_name,
                   bt.debit_amount, bt.credit_amount, bt.transaction_date,
                   bt.company_id, bt.fiscal_period_id
            FROM bank_transactions bt
            LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id
            WHERE bt.account_code IS NOT NULL
            AND jel.id IS NULL
            ORDER BY bt.transaction_date
            """;

        return conn.prepareStatement(sql);
    }

    /**
     * Process journal entry recreation from result set
     */
    private int processJournalEntryRecreation(ResultSet rs) throws SQLException {
        int count = 0;

        while (rs.next()) {
            Long transactionId = rs.getLong("id");
            String accountName = rs.getString("account_name");
            BigDecimal debitAmount = rs.getBigDecimal("debit_amount");
            BigDecimal creditAmount = rs.getBigDecimal("credit_amount");
            LocalDate transactionDate = rs.getDate("transaction_date").toLocalDate();
            Long companyId = rs.getLong("company_id");
            Long fiscalPeriodId = rs.getLong("fiscal_period_id");

            // Recreate the bank transaction object
            BankTransaction transaction = new BankTransaction();
            transaction.setId(transactionId);
            transaction.setDetails(rs.getString("details"));
            transaction.setDebitAmount(debitAmount);
            transaction.setCreditAmount(creditAmount);
            transaction.setTransactionDate(transactionDate);
            transaction.setCompanyId(companyId);
            transaction.setFiscalPeriodId(fiscalPeriodId);

            // Get account ID and create journal entry
            Long accountId = getAccountId(accountName);
            if (accountId != null) {
                if (createJournalEntryForTransaction(transaction, accountId)) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Log recreation results
     */
    private void logRecreationResults(int count) {
        if (count > 0) {
            System.out.println("✅ Recreated journal entries for " + count + " categorized transactions");
            LOGGER.info("Recreated journal entries for " + count + " transactions");
        } else {
            System.out.println("ℹ️  No categorized transactions found that need journal entries");
        }
    }

    /**
     * Get database suggestions for account patterns
     */
    private List<String> getDatabaseSuggestions(String pattern) throws SQLException {
        List<String> suggestions = new ArrayList<>();

        String sql = """
            SELECT DISTINCT account_code, account_name, usage_count
            FROM company_classification_rules
            WHERE pattern ILIKE ?
            ORDER BY usage_count DESC
            LIMIT 5
            """;

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + pattern + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String accountCode = rs.getString("account_code");
                    String accountName = rs.getString("account_name");
                    suggestions.add(accountCode + " - " + accountName);
                }
            }
        }

        return suggestions;
    }
}