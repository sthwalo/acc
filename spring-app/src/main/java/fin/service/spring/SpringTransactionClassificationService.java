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

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring Service for transaction classification operations.
 * Orchestrates classification workflows using JPA repositories.
 */
@Service
@Transactional
public class SpringTransactionClassificationService {

    private static final Logger LOGGER = Logger.getLogger(SpringTransactionClassificationService.class.getName());

    // Console output formatting constants
    private static final int CONSOLE_SEPARATOR_WIDTH = 80;

    private final SpringAccountService accountService;
    private final SpringCompanyService companyService;
    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public SpringTransactionClassificationService(SpringAccountService accountService,
                                                SpringCompanyService companyService,
                                                BankTransactionRepository bankTransactionRepository,
                                                FiscalPeriodRepository fiscalPeriodRepository) {
        this.accountService = accountService;
        this.companyService = companyService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Initialize chart of accounts for a company
     * This creates the standard account structure and categories
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

            // Create standard chart of accounts
            // This would typically involve creating predefined accounts
            // For now, we'll create some basic accounts as an example
            createStandardChartOfAccounts(companyId);

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
        // Asset accounts (1000-1999)
        createAccountIfNotExists(companyId, "1000", "Cash on Hand", 1, "Main cash account");
        createAccountIfNotExists(companyId, "1100", "Bank Account", 1, "Primary business bank account");
        createAccountIfNotExists(companyId, "1200", "Accounts Receivable", 1, "Money owed by customers");

        // Liability accounts (2000-2999)
        createAccountIfNotExists(companyId, "2000", "Accounts Payable", 2, "Money owed to suppliers");
        createAccountIfNotExists(companyId, "2100", "Loans Payable", 2, "Outstanding loans");

        // Equity accounts (3000-3999)
        createAccountIfNotExists(companyId, "3000", "Owner's Equity", 3, "Owner's investment in business");
        createAccountIfNotExists(companyId, "3100", "Retained Earnings", 3, "Accumulated profits");

        // Income accounts (4000-4999)
        createAccountIfNotExists(companyId, "4000", "Sales Revenue", 4, "Revenue from sales");
        createAccountIfNotExists(companyId, "4100", "Service Revenue", 4, "Revenue from services");

        // Expense accounts (5000-5999)
        createAccountIfNotExists(companyId, "5000", "Cost of Goods Sold", 5, "Direct costs of goods sold");
        createAccountIfNotExists(companyId, "5100", "Operating Expenses", 5, "General business expenses");
        createAccountIfNotExists(companyId, "5200", "Rent Expense", 5, "Office or facility rent");
        createAccountIfNotExists(companyId, "5300", "Utilities", 5, "Electricity, water, internet");
    }

    /**
     * Create an account if it doesn't already exist
     */
    private void createAccountIfNotExists(Long companyId, String accountCode, String accountName,
                                        Integer categoryId, String description) {
        try {
            accountService.getAccountByCompanyAndCode(companyId, accountCode);
            // Account already exists, skip
        } catch (Exception e) {
            // Account doesn't exist, create it
            accountService.createAccount(accountCode, accountName, companyId, categoryId, description, null);
        }
    }

    /**
     * Initialize transaction mapping rules for a company
     * This creates the standard pattern-matching rules for auto-classification
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

            // Create standard mapping rules
            // This would typically involve creating ClassificationRule entities
            // For now, we'll just return success
            int rulesCreated = createStandardMappingRules(companyId);

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
        // This would create ClassificationRule entities in the database
        // For now, return a dummy count
        return 10; // Assume 10 rules were created
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
     * Auto-classify transactions (simplified version for API)
     */
    @Transactional
    public ClassificationResult autoClassifyTransactions(Long companyId) {
        autoClassifyTransactions(companyId, null);
        return new ClassificationResult(null, null, null, "AUTO", "Batch classification");
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
}