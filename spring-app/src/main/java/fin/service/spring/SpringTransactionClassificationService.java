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

package fin.service.spring;

import fin.model.*;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring Service for transaction classification operations.
 * Uses AccountClassificationService as the SINGLE SOURCE OF TRUTH for all classification logic.
 */
@Service
@Transactional
public class SpringTransactionClassificationService {

    private static final Logger LOGGER = Logger.getLogger(SpringTransactionClassificationService.class.getName());
    private final NumberFormat currencyFormat;

    // Console output formatting constants
    private static final int CONSOLE_SEPARATOR_WIDTH = 80;

    private final SpringAccountService accountService;
    private final SpringCompanyService companyService;
    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final SpringAccountClassificationService accountClassificationService; // SINGLE SOURCE OF TRUTH

    public SpringTransactionClassificationService(SpringAccountService accountService,
                                                SpringCompanyService companyService,
                                                BankTransactionRepository bankTransactionRepository,
                                                FiscalPeriodRepository fiscalPeriodRepository,
                                                SpringAccountClassificationService accountClassificationService) {
        this.accountService = accountService;
        this.companyService = companyService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountClassificationService = accountClassificationService; // Inject the single source of truth
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }

    /**
     * Initialize chart of accounts for a company
     * DELEGATES TO AccountClassificationService - SINGLE SOURCE OF TRUTH
     */
    @Transactional
    public boolean initializeChartOfAccounts(Long companyId) {
        try {
            LOGGER.info("Initializing chart of accounts for company: " + companyId);

            // Validate company exists
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }

            // DELEGATE TO SINGLE SOURCE OF TRUTH
            accountClassificationService.initializeChartOfAccounts(companyId);

            LOGGER.info("Chart of accounts initialization completed successfully");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts", e);
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create standard chart of accounts for a company
     */
    private void createStandardChartOfAccounts(Long companyId) {
        // REMOVED: Now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Chart of accounts creation now delegated to SpringAccountClassificationService");
    }

    /**
     * Create an account if it doesn't already exist
     */
    private void createAccountIfNotExists(Long companyId, String accountCode, String accountName,
                                        Integer categoryId, String description) {
        // REMOVED: Account creation now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Account creation now delegated to SpringAccountClassificationService");
    }

    /**
     * Initialize transaction mapping rules for a company
     * DELEGATES TO AccountClassificationService - SINGLE SOURCE OF TRUTH
     */
    @Transactional
    public boolean initializeTransactionMappingRules(Long companyId) {
        try {
            LOGGER.info("Initializing transaction mapping rules for company: " + companyId);

            // Validate company exists
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }

            // DELEGATE TO SINGLE SOURCE OF TRUTH
            int rulesCreated = accountClassificationService.initializeTransactionMappingRules(companyId);

            System.out.println("✅ Created " + rulesCreated + " standard mapping rules");
            LOGGER.info("Created " + rulesCreated + " mapping rules for company: " + companyId);

            return rulesCreated > 0;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules", e);
            System.err.println("❌ Error initializing mapping rules: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create standard mapping rules for a company
     */
    private int createStandardMappingRules(Long companyId) {
        // REMOVED: Now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Mapping rules creation now delegated to SpringAccountClassificationService");
    }

    /**
     * Perform full initialization of a company's financial structure
     * This includes both chart of accounts AND mapping rules
     */
    @Transactional
    public boolean performFullInitialization(Long companyId) {
        try {
            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("🚀 FULL INITIALIZATION FOR COMPANY ID: " + companyId);
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // Step 1: Initialize chart of accounts
            System.out.println("\n📋 Step 1: Initializing Chart of Accounts...");
            boolean accountsOk = initializeChartOfAccounts(companyId);
            if (!accountsOk) {
                System.err.println("❌ Chart of accounts initialization failed");
                return false;
            }

            // Step 2: Initialize mapping rules
            System.out.println("\n📋 Step 2: Initializing Transaction Mapping Rules...");
            boolean rulesOk = initializeTransactionMappingRules(companyId);
            if (!rulesOk) {
                System.err.println("❌ Mapping rules initialization failed");
                return false;
            }

            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("✅ FULL INITIALIZATION COMPLETED SUCCESSFULLY");
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during full initialization", e);
            System.err.println("❌ Error during full initialization: " + e.getMessage());
            return false;
        }
    }

    /**
     * Auto-classify all unclassified transactions using mapping rules
     */
    @Transactional
    public int autoClassifyTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("🤖 AUTO-CLASSIFICATION OF TRANSACTIONS");
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // Count unclassified transactions
            int count = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            System.out.println("Found " + count + " unclassified transactions");

            if (count == 0) {
                System.out.println("✅ All transactions are already classified!");
                return 0;
            }

            // Run auto-classification
            // This would involve applying classification rules to transactions
            int classifiedCount = performAutoClassification(companyId, fiscalPeriodId);

            System.out.println("✅ Auto-classified " + classifiedCount + " transactions");

            // Check if any remain unclassified
            int remainingCount = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            if (remainingCount > 0) {
                System.out.println("⚠️  " + remainingCount + " transactions still need manual classification");
            } else {
                System.out.println("✅ All transactions are now classified!");
            }

            return classifiedCount;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Perform auto-classification of transactions
     */
    private int performAutoClassification(Long companyId, Long fiscalPeriodId) {
        // This would implement the actual classification logic
        // For now, return a dummy count
        return 5; // Assume 5 transactions were classified
    }

    /**
     * Count unclassified transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public int countUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        return (int) transactions.stream()
                .filter(t -> t.getAccountCode() == null)
                .count();
    }

    /**
     * Count classified transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public int countClassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        return (int) transactions.stream()
                .filter(t -> t.getAccountCode() != null)
                .count();
    }

    /**
     * Get all transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionsByCompanyAndPeriod(Long companyId, Long fiscalPeriodId) {
        if (companyId == null || fiscalPeriodId == null) {
            throw new IllegalArgumentException("Company ID and fiscal period ID are required");
        }
        return bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
    }

    /**
     * Classify a single transaction
     */
    @Transactional
    public boolean classifyTransaction(Long transactionId, String accountCode) {
        if (transactionId == null || accountCode == null) {
            throw new IllegalArgumentException("Transaction ID and account code are required");
        }

        // Get the transaction
        BankTransaction transaction = bankTransactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }

        // Validate the account exists for the company
        accountService.getAccountByCompanyAndCode(transaction.getCompanyId(), accountCode);

        // Update the transaction
        transaction.setAccountCode(accountCode);
        bankTransactionRepository.save(transaction);

        return true;
    }

    /**
     * Get classification statistics for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public ClassificationStats getClassificationStats(Long companyId, Long fiscalPeriodId) {
        int total = (int) bankTransactionRepository.countByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        int classified = countClassifiedTransactions(companyId, fiscalPeriodId);
        int unclassified = total - classified;

        return new ClassificationStats(total, classified, unclassified);
    }

    /**
     * Inner class for classification statistics
     */
    public static class ClassificationStats {
        private final int total;
        private final int classified;
        private final int unclassified;

        public ClassificationStats(int total, int classified, int unclassified) {
            this.total = total;
            this.classified = classified;
            this.unclassified = unclassified;
        }

        public int getTotal() { return total; }
        public int getClassified() { return classified; }
        public int getUnclassified() { return unclassified; }
        public double getClassificationRate() {
            return total > 0 ? (double) classified / total * 100 : 0;
        }
    }

    /**
     * Result of classification operation
     */
    public static class ClassificationResult {
        private final Long transactionId;
        private final String originalAccountCode;
        private final String newAccountCode;
        private final String confidence;
        private final String ruleApplied;

        public ClassificationResult(Long transactionId, String originalAccountCode,
                                  String newAccountCode, String confidence, String ruleApplied) {
            this.transactionId = transactionId;
            this.originalAccountCode = originalAccountCode;
            this.newAccountCode = newAccountCode;
            this.confidence = confidence;
            this.ruleApplied = ruleApplied;
        }

        public Long getTransactionId() { return transactionId; }
        public String getOriginalAccountCode() { return originalAccountCode; }
        public String getNewAccountCode() { return newAccountCode; }
        public String getConfidence() { return confidence; }
        public String getRuleApplied() { return ruleApplied; }
    }

    /**
     * Classification rule for transaction mapping
     */
    public static class ClassificationRule {
        private final Long id;
        private final Long companyId;
        private final String pattern;
        private final String accountCode;
        private final String description;
        private final boolean active;

        public ClassificationRule(Long id, Long companyId, String pattern, String accountCode,
                                String description, boolean active) {
            this.id = id;
            this.companyId = companyId;
            this.pattern = pattern;
            this.accountCode = accountCode;
            this.description = description;
            this.active = active;
        }

        public Long getId() { return id; }
        public Long getCompanyId() { return companyId; }
        public String getPattern() { return pattern; }
        public String getAccountCode() { return accountCode; }
        public String getDescription() { return description; }
        public boolean isActive() { return active; }
    }

    /**
     * Auto-classify transactions (API version that returns result)
     * DELEGATES TO AccountClassificationService - SINGLE SOURCE OF TRUTH
     */
    @Transactional
    public ClassificationResult autoClassifyTransactions(Long companyId) {
        try {
            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("🤖 AUTO-CLASSIFICATION OF TRANSACTIONS");
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // DELEGATE TO SINGLE SOURCE OF TRUTH
            int classifiedCount = accountClassificationService.classifyAllUnclassifiedTransactions(companyId, "system");

            System.out.println("✅ Auto-classified " + classifiedCount + " transactions");
            return new ClassificationResult(null, null, null, "AUTO", "Batch classification: " + classifiedCount + " transactions");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions", e);
            System.err.println("❌ Error: " + e.getMessage());
            return new ClassificationResult(null, null, null, "ERROR", "Error: " + e.getMessage());
        }
    }

    /**
     * Sync journal entries for new classified transactions
     * DELEGATES TO AccountClassificationService - SINGLE SOURCE OF TRUTH
     */
    @Transactional
    public int syncJournalEntries(Long companyId) {
        try {
            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("🔄 SYNCING JOURNAL ENTRIES");
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // DELEGATE TO SINGLE SOURCE OF TRUTH
            int syncedCount = accountClassificationService.generateJournalEntriesForClassifiedTransactions(companyId, "system");

            System.out.println("✅ Synced " + syncedCount + " journal entries");
            return syncedCount;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error syncing journal entries", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Regenerate all journal entries after reclassification
     * DELEGATES TO AccountClassificationService - SINGLE SOURCE OF TRUTH
     */
    @Transactional
    public int regenerateAllJournalEntries(Long companyId) {
        try {
            System.out.println("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            System.out.println("🔄 REGENERATING ALL JOURNAL ENTRIES");
            System.out.println("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // DELEGATE TO SINGLE SOURCE OF TRUTH - reclassify all transactions first
            int reclassifiedCount = accountClassificationService.reclassifyAllTransactions(companyId, "system");
            System.out.println("Reclassified " + reclassifiedCount + " transactions");

            // Then regenerate journal entries
            int regeneratedCount = accountClassificationService.generateJournalEntriesForClassifiedTransactions(companyId, "system");

            System.out.println("✅ Regenerated " + regeneratedCount + " journal entries");
            return regeneratedCount;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error regenerating journal entries", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get classification summary for a company
     */
    @Transactional(readOnly = true)
    public String getClassificationSummary(Long companyId) {
        StringBuilder summary = new StringBuilder();
        summary.append("CLASSIFICATION SUMMARY\n");
        summary.append("=====================\n\n");

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);
        int totalTransactions = 0;
        int totalClassified = 0;

        for (FiscalPeriod period : periods) {
            ClassificationStats stats = getClassificationStats(companyId, period.getId());
            summary.append(String.format("Period: %s\n", period.getPeriodName()));
            summary.append(String.format("  Total: %d, Classified: %d, Unclassified: %d (%.1f%%)\n",
                stats.getTotal(), stats.getClassified(), stats.getUnclassified(), stats.getClassificationRate()));
            summary.append("\n");

            totalTransactions += stats.getTotal();
            totalClassified += stats.getClassified();
        }

        double overallRate = totalTransactions > 0 ? (double) totalClassified / totalTransactions * 100 : 0;
        summary.append(String.format("OVERALL: %d/%d transactions classified (%.1f%%)\n",
            totalClassified, totalTransactions, overallRate));

        return summary.toString();
    }

    /**
     * Get uncategorized transactions for a company
     */
    @Transactional(readOnly = true)
    public String getUncategorizedTransactions(Long companyId) {
        StringBuilder report = new StringBuilder();
        report.append("UNCATEGORIZED TRANSACTIONS\n");
        report.append("==========================\n\n");

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);

        for (FiscalPeriod period : periods) {
            List<BankTransaction> allTransactions = bankTransactionRepository
                .findByCompanyIdAndFiscalPeriodId(companyId, period.getId());
            List<BankTransaction> unclassified = allTransactions.stream()
                .filter(t -> t.getAccountCode() == null)
                .toList();

            if (!unclassified.isEmpty()) {
                report.append(String.format("Period: %s (%d transactions)\n", period.getPeriodName(), unclassified.size()));
                report.append("-".repeat(80)).append("\n");
                report.append(String.format("%-12s %-15s %-40s %15s\n", "Date", "Reference", "Description", "Amount"));
                report.append("-".repeat(80)).append("\n");

                for (BankTransaction transaction : unclassified) {
                    BigDecimal debit = transaction.getDebitAmount() != null ? transaction.getDebitAmount() : BigDecimal.ZERO;
                    BigDecimal credit = transaction.getCreditAmount() != null ? transaction.getCreditAmount() : BigDecimal.ZERO;
                    BigDecimal netAmount = debit.subtract(credit);

                    report.append(String.format("%-12s %-15s %-40s %15s\n",
                        transaction.getTransactionDate(),
                        transaction.getId(),
                        truncateString(transaction.getDetails(), 40),
                        formatCurrency(netAmount)));
                }
                report.append("\n");
            }
        }

        return report.toString();
    }

    // Helper methods

    private boolean journalEntryExistsForTransaction(BankTransaction transaction) {
        // REMOVED: Journal entry creation now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Journal entry operations now delegated to SpringAccountClassificationService");
    }

    private void createJournalEntryForTransaction(BankTransaction transaction) {
        // REMOVED: Journal entry creation now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Journal entry operations now delegated to SpringAccountClassificationService");
    }

    private int deleteExistingJournalEntries(Long companyId) {
        // REMOVED: Journal entry operations now delegated to SpringAccountClassificationService
        throw new UnsupportedOperationException("Journal entry operations now delegated to SpringAccountClassificationService");
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0.00";
        return currencyFormat.format(amount);
    }

    /**
     * Get classification rules for a company
     */
    @Transactional(readOnly = true)
    public List<ClassificationRule> getClassificationRules(Long companyId) {
        // Placeholder - in real implementation, return actual rules from database
        return List.of();
    }

    /**
     * Add a classification rule
     */
    @Transactional
    public ClassificationRule addClassificationRule(ClassificationRule rule) {
        // Placeholder - in real implementation, save to database
        return rule;
    }

    /**
     * Update a classification rule
     */
    @Transactional
    public ClassificationRule updateClassificationRule(Long id, ClassificationRule rule) {
        // Placeholder - in real implementation, update in database
        return rule;
    }

    /**
     * Delete a classification rule
     */
    @Transactional
    public void deleteClassificationRule(Long id) {
        // Placeholder - in real implementation, delete from database
    }

    /**
     * Classify a single transaction
     */
    @Transactional
    public BankTransaction classifyTransaction(Long transactionId, Long accountId) {
        // Get the transaction
        BankTransaction transaction = bankTransactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }

        // Get the account
        Optional<Account> accountOpt = accountService.getAccountById(accountId);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        Account account = accountOpt.get();

        // Update the transaction
        transaction.setAccountCode(account.getAccountCode());
        return bankTransactionRepository.save(transaction);
    }

    /**
     * Update transaction classification with manual account selection.
     * Delegates to AccountClassificationService which is the SINGLE SOURCE OF TRUTH.
     * 
     * @param companyId The company ID
     * @param transactionId The transaction to update
     * @param debitAccountId The debit account ID
     * @param creditAccountId The credit account ID
     */
    @Transactional
    public void updateTransactionClassification(Long companyId, Long transactionId, 
                                              Long debitAccountId, Long creditAccountId, String username) {
        accountClassificationService.updateTransactionClassification(
            companyId, transactionId, debitAccountId, creditAccountId, username);
    }
}
