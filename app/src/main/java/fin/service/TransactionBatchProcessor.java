package fin.service;

import fin.model.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * TransactionBatchProcessor - Handles batch processing of transactions
 *
 * This service orchestrates the complete transaction processing pipeline:
 * 1. Classifies transactions using rule-based mapping
 * 2. Generates journal entries for classified transactions
 * 3. Tracks processing statistics and results
 */
public class TransactionBatchProcessor {
    private static final Logger LOGGER = Logger.getLogger(TransactionBatchProcessor.class.getName());

    private final ClassificationRuleManager ruleManager;
    private final JournalEntryGenerator journalGenerator;

    public TransactionBatchProcessor(ClassificationRuleManager ruleManager,
                                   JournalEntryGenerator journalGenerator) {
        this.ruleManager = ruleManager;
        this.journalGenerator = journalGenerator;
    }

    /**
     * Process a batch of transactions
     *
     * @param transactions List of transactions to process
     * @param companyId Company ID for processing
     * @return BatchProcessingResult with processing statistics
     */
    public BatchProcessingResult processBatch(List<BankTransaction> transactions, Long companyId) {
        LOGGER.info("Starting batch processing of " + transactions.size() + " transactions for company: " + companyId);

        BatchProcessingResult result = new BatchProcessingResult();
        int processedCount = 0;
        int classifiedCount = 0;
        int failedCount = 0;

        // Load mapping rules for the company using ClassificationRuleManager
        List<ClassificationRuleManager.ClassificationRule> rules = ruleManager.getRulesByCompany(companyId);
        LOGGER.info("Loaded " + rules.size() + " mapping rules for company: " + companyId);

        for (BankTransaction transaction : transactions) {
            try {
                processedCount++;

                // Try rule-based classification using ClassificationRuleManager
                ClassificationRuleManager.ClassificationRule matchedRule = findMatchingRule(transaction.getDetails(), rules);
                ClassificationResult classification = null;

                if (matchedRule != null) {
                    classification = new ClassificationResult(
                        matchedRule.getAccountCode(),
                        matchedRule.getAccountName(),
                        "RULE_BASED"
                    );
                    LOGGER.fine("Transaction classified by rule: " + transaction.getDetails() + " -> " + matchedRule.getAccountCode());
                }
                // No fallback classification - transactions without matching rules remain unclassified

                if (classification != null) {
                    classifiedCount++;

                    // Generate journal entry for classified transaction
                    try {
                        journalGenerator.createJournalEntryForTransaction(transaction, classification);
                        LOGGER.fine("Journal entry created for transaction: " + transaction.getReference());
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to create journal entry for transaction: " + transaction.getReference(), e);
                        failedCount++;
                    }
                } else {
                    LOGGER.warning("Transaction could not be classified: " + transaction.getDetails());
                    failedCount++;
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing transaction: " + transaction.getReference(), e);
                failedCount++;
            }
        }

        result.setProcessedCount(processedCount);
        result.setClassifiedCount(classifiedCount);
        result.setFailedCount(failedCount);
        result.setSuccess(failedCount == 0);

        LOGGER.info(String.format("Batch processing completed. Processed: %d, Classified: %d, Failed: %d, Success: %b",
            processedCount, classifiedCount, failedCount, result.isSuccess()));

        return result;
    }

    /**
     * Process a batch of transactions and return detailed statistics
     *
     * @param transactions List of transactions to process
     * @param companyId Company ID for processing
     * @return BatchProcessingStatistics with detailed metrics
     */
    public BatchProcessingStatistics processBatchWithStatistics(List<BankTransaction> transactions, Long companyId) {
        BatchProcessingResult result = processBatch(transactions, companyId);

        return new BatchProcessingStatistics(
            result.getProcessedCount(),
            result.getClassifiedCount(),
            result.getProcessedCount() - result.getClassifiedCount()
        );
    }

    /**
     * Validate transactions before batch processing
     *
     * @param transactions List of transactions to validate
     * @return List of validation error messages (empty if all valid)
     */
    public List<String> validateTransactions(List<BankTransaction> transactions) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction transaction = transactions.get(i);

            if (transaction.getReference() == null || transaction.getReference().trim().isEmpty()) {
                errors.add("Transaction " + i + ": Missing reference number");
            }

            if (transaction.getDetails() == null || transaction.getDetails().trim().isEmpty()) {
                errors.add("Transaction " + i + ": Missing transaction details");
            }

            if (transaction.getDebitAmount() == null && transaction.getCreditAmount() == null) {
                errors.add("Transaction " + i + ": Missing transaction amount");
            }

            if (transaction.getTransactionDate() == null) {
                errors.add("Transaction " + i + ": Missing transaction date");
            }
        }

        return errors;
    }

    /**
     * Process transactions with validation
     *
     * @param transactions List of transactions to process
     * @param companyId Company ID for processing
     * @return BatchProcessingResult (throws exception if validation fails)
     * @throws IllegalArgumentException if transactions fail validation
     */
    public BatchProcessingResult processBatchValidated(List<BankTransaction> transactions, Long companyId) {
        List<String> validationErrors = validateTransactions(transactions);

        if (!validationErrors.isEmpty()) {
            throw new IllegalArgumentException("Transaction validation failed: " + String.join(", ", validationErrors));
        }

        return processBatch(transactions, companyId);
    }

    /**
     * Find a matching classification rule for the given transaction details
     */
    private ClassificationRuleManager.ClassificationRule findMatchingRule(String details, List<ClassificationRuleManager.ClassificationRule> rules) {
        if (details == null || rules == null) return null;
        
        String normalizedDetails = details.toLowerCase();
        
        for (ClassificationRuleManager.ClassificationRule rule : rules) {
            String pattern = rule.getPattern().toLowerCase();
            
            // Simple pattern matching - check if pattern is contained in details
            if (normalizedDetails.contains(pattern)) {
                return rule;
            }
        }
        return null;
    }
}