package fin.service;

import fin.model.BankTransaction;
import fin.model.TransactionMappingRule;
import fin.config.DatabaseConfig;

import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Core Transaction Classification Engine
 * 
 * SINGLE RESPONSIBILITY: Classify individual transactions using rules from AccountClassificationService
 * 
 * This service is the CORE classification engine that:
 * - Uses AccountClassificationService as single source of truth
 * - Applies pattern matching algorithms
 * - Returns classification results with confidence scores
 * - Does NOT handle UI, menus, or user interaction
 * 
 * Created: October 11, 2025 - Extracted from InteractiveClassificationService
 */
public class TransactionClassificationEngine {
    private static final Logger LOGGER = Logger.getLogger(TransactionClassificationEngine.class.getName());
    
    private final String dbUrl;
    private final AccountClassificationService accountClassificationService;
    
    // Classification confidence thresholds
    @SuppressWarnings("MagicNumber")
    private static final double CONFIDENCE_THRESHOLD_HIGH = 0.9;
    @SuppressWarnings("MagicNumber")
    private static final double CONFIDENCE_THRESHOLD_MEDIUM = 0.6;
    @SuppressWarnings("MagicNumber")
    private static final double HIGH_MATCH_SCORE = 1.0;
    @SuppressWarnings("MagicNumber")
    private static final double LOW_MATCH_SCORE = 0.5;
    @SuppressWarnings("MagicNumber")
    private static final double PATTERN_MATCH_SCORE = 0.8;
    @SuppressWarnings("MagicNumber")
    private static final double MAX_MATCH_SCORE = 1.0;
    @SuppressWarnings("MagicNumber")
    private static final int SIGNIFICANT_KEYWORD_LENGTH = 4;
    @SuppressWarnings("MagicNumber")
    private static final int MAX_KEYWORDS_TO_EXTRACT = 5;
    
    // Database parameter indices for similar transaction queries
    private static final int PARAM_COMPANY_ID = 1;
    private static final int PARAM_TRANSACTION_ID = 2;
    private static final int PARAM_PATTERN_EXACT = 3;
    private static final int PARAM_PATTERN_KEYWORD1 = 4;
    private static final int PARAM_PATTERN_KEYWORD2 = 5;
    private static final int PARAM_MAX_RESULTS = 6;
    
    /**
     * Classification result with confidence score
     */
    public static class ClassificationResult {
        private final String accountCode;
        private final String accountName;
        private final double confidenceScore;
        private final String matchingRule;
        private final boolean isAutoClassified;
        
        public ClassificationResult(String valueAccountCode, String valueAccountName, double valueConfidenceScore, 
                                  String valueMatchingRule, boolean valueIsAutoClassified) {
            this.accountCode = valueAccountCode;
            this.accountName = valueAccountName;
            this.confidenceScore = valueConfidenceScore;
            this.matchingRule = valueMatchingRule;
            this.isAutoClassified = valueIsAutoClassified;
        }
        
        // Getters
        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public double getConfidenceScore() { return confidenceScore; }
        public String getMatchingRule() { return matchingRule; }
        public boolean isAutoClassified() { return isAutoClassified; }
        public boolean isHighConfidence() { return confidenceScore >= CONFIDENCE_THRESHOLD_HIGH; }
        public boolean isMediumConfidence() { return confidenceScore >= CONFIDENCE_THRESHOLD_MEDIUM; }
    }
    
    public TransactionClassificationEngine() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.accountClassificationService = new AccountClassificationService(dbUrl);
    }
    
    /**
     * Classify a transaction using AccountClassificationService rules (single source of truth)
     * 
     * @param transaction The transaction to classify
     * @param companyId The company ID for context
     * @return ClassificationResult or null if no match found
     */
    public ClassificationResult classifyTransaction(BankTransaction transaction, Long companyId) {
        if (transaction == null || transaction.getDetails() == null) {
            return null;
        }
        
        // ✅ SINGLE SOURCE OF TRUTH: Use AccountClassificationService for all rules
        List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
        
        String transactionDetails = transaction.getDetails().toLowerCase();
        ClassificationResult bestMatch = null;
        double highestScore = 0.0;
        
        // Apply each rule and find the best match
        for (TransactionMappingRule rule : standardRules) {
            if (rule.matches(transactionDetails)) {
                double confidence = calculateConfidenceScore(transactionDetails, rule);
                
                if (confidence > highestScore && confidence >= LOW_MATCH_SCORE) {
                    String accountCode = extractAccountCodeFromRuleDescription(rule.getDescription());
                    if (accountCode != null) {
                        bestMatch = new ClassificationResult(
                            accountCode,
                            getAccountNameFromRule(rule),
                            confidence,
                            rule.getRuleName(),
                            confidence >= CONFIDENCE_THRESHOLD_MEDIUM
                        );
                        highestScore = confidence;
                    }
                }
            }
        }
        
        return bestMatch;
    }
    
    /**
     * Calculate confidence score for a rule match
     */
    private double calculateConfidenceScore(String transactionDetails, TransactionMappingRule rule) {
        String matchValue = rule.getMatchValue().toLowerCase();
        
        // Exact match gets highest score
        if (transactionDetails.contains(matchValue)) {
            if (matchValue.length() > SIGNIFICANT_KEYWORD_LENGTH) {
                return HIGH_MATCH_SCORE;
            } else {
                return PATTERN_MATCH_SCORE;
            }
        }
        
        // Partial match based on rule type
        switch (rule.getMatchType()) {
            case CONTAINS:
                if (transactionDetails.contains(matchValue)) {
                    return PATTERN_MATCH_SCORE;
                }
                break;
            case STARTS_WITH:
                if (transactionDetails.startsWith(matchValue)) {
                    return HIGH_MATCH_SCORE;
                }
                break;
            case ENDS_WITH:
                if (transactionDetails.endsWith(matchValue)) {
                    return PATTERN_MATCH_SCORE;
                }
                break;
            case EQUALS:
                if (transactionDetails.equals(matchValue)) {
                    return HIGH_MATCH_SCORE;
                }
                break;
            case REGEX:
                if (transactionDetails.matches(matchValue)) {
                    return HIGH_MATCH_SCORE;
                }
                break;
        }
        
        return 0.0;
    }
    
    /**
     * Extract account code from rule description (format: "[AccountCode:XXXX]")
     */
    private String extractAccountCodeFromRuleDescription(String description) {
        if (description == null) return null;
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[AccountCode:(\\d+(?:-\\d+)?)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(description);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract account name from rule description or use rule name as fallback
     */
    private String getAccountNameFromRule(TransactionMappingRule rule) {
        // Try to extract from description first
        String description = rule.getDescription();
        if (description != null && description.contains(" - ")) {
            String[] parts = description.split(" - ");
            if (parts.length > 1) {
                return parts[1].replace("[AccountCode:" + extractAccountCodeFromRuleDescription(description) + "]", "").trim();
            }
        }
        
        // Fallback to rule name
        return rule.getRuleName();
    }
    
    /**
     * Classify multiple transactions in batch
     * 
     * @param transactions List of transactions to classify
     * @param companyId Company ID for context
     * @return Map of transaction ID to classification result
     */
    public Map<Long, ClassificationResult> classifyTransactionsBatch(List<BankTransaction> transactions, Long companyId) {
        Map<Long, ClassificationResult> results = new HashMap<>();
        
        if (transactions == null || transactions.isEmpty()) {
            return results;
        }
        
        // Load rules once for efficiency (for future enhancements)
        // List<TransactionMappingRule> standardRules = accountClassificationService.getStandardMappingRules();
        
        for (BankTransaction transaction : transactions) {
            ClassificationResult result = classifyTransaction(transaction, companyId);
            if (result != null) {
                results.put(transaction.getId(), result);
            }
        }
        
        LOGGER.info("Classified " + results.size() + " out of " + transactions.size() + " transactions");
        return results;
    }
    
    /**
     * Find similar transactions based on pattern matching
     * 
     * @param transaction The reference transaction
     * @param companyId Company ID for context
     * @param maxResults Maximum number of similar transactions to return
     * @return List of similar unclassified transactions
     */
    public List<BankTransaction> findSimilarUnclassifiedTransactions(BankTransaction transaction, Long companyId, int maxResults) {
        List<BankTransaction> similarTransactions = new ArrayList<>();
        
        if (transaction == null || transaction.getDetails() == null) {
            return similarTransactions;
        }
        
        String sql = """
            SELECT bt.*
            FROM bank_transactions bt
            WHERE bt.company_id = ? 
            AND bt.account_code IS NULL
            AND bt.id != ?
            AND (
                LOWER(bt.details) LIKE ? OR
                LOWER(bt.details) LIKE ? OR
                LOWER(bt.details) LIKE ?
            )
            ORDER BY bt.transaction_date DESC
            LIMIT ?
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String details = transaction.getDetails().toLowerCase();
            String[] keywords = extractKeywords(details);
            
            pstmt.setLong(PARAM_COMPANY_ID, companyId);
            pstmt.setLong(PARAM_TRANSACTION_ID, transaction.getId());
            
            // Create search patterns
            String pattern1 = "%" + details + "%"; // Exact description match
            String pattern2 = keywords.length > 0 ? "%" + keywords[0] + "%" : ""; // First keyword
            String pattern3 = keywords.length > 1 ? "%" + keywords[1] + "%" : ""; // Second keyword
            
            pstmt.setString(PARAM_PATTERN_EXACT, pattern1);
            pstmt.setString(PARAM_PATTERN_KEYWORD1, pattern2);
            pstmt.setString(PARAM_PATTERN_KEYWORD2, pattern3);
            pstmt.setInt(PARAM_MAX_RESULTS, maxResults);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                similarTransactions.add(mapResultSetToBankTransaction(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error finding similar transactions", e);
        }
        
        return similarTransactions;
    }
    
    /**
     * Extract meaningful keywords from transaction description
     */
    private String[] extractKeywords(String description) {
        if (description == null) return new String[0];
        
        // Remove common words and extract meaningful keywords
        String[] commonWords = {"the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by"};
        Set<String> commonWordsSet = Set.of(commonWords);
        
        return Arrays.stream(description.toLowerCase().split("\\W+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !commonWordsSet.contains(word))
                .limit(MAX_KEYWORDS_TO_EXTRACT) // Take top N keywords
                .toArray(String[]::new);
    }
    
    /**
     * Map ResultSet to BankTransaction
     */
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