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

package fin.service.classification;

import fin.entity.*;
import fin.repository.AccountRepository;
import fin.repository.BankTransactionRepository;
import fin.service.classification.engine.TransactionClassificationEngine;
import fin.util.Debugger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring service for interactive transaction classification
 * Provides REST-based classification workflow instead of console-based interaction
 */
@Service
public class InteractiveClassificationService {

    private static final int MAX_SUGGESTIONS = 5;
    private static final int MAX_SIMILAR_TRANSACTIONS = 10;
    private static final int MIN_KEYWORD_LENGTH = 3;

    private final BankTransactionRepository bankTransactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionClassificationEngine classificationEngine;
    private final Debugger debugger;

    public InteractiveClassificationService(BankTransactionRepository bankTransactionRepository,
                                                AccountRepository accountRepository,
                                                TransactionClassificationEngine classificationEngine,
                                                Debugger debugger) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.accountRepository = accountRepository;
        this.classificationEngine = classificationEngine;
        this.debugger = debugger;
    }

    /**
     * Get uncategorized transactions for a company and fiscal period
     */
    public List<BankTransaction> getUncategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("InteractiveClassificationService", "getUncategorizedTransactions", companyId, fiscalPeriodId);

        validateCompanyAndFiscalPeriodIds(companyId, fiscalPeriodId);

        List<BankTransaction> transactions = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

        debugger.logMethodExit("InteractiveClassificationService", "getUncategorizedTransactions",
            String.format("Found %d uncategorized transactions", transactions.size()));

        return transactions;
    }

    /**
     * Get categorized transactions for a company and fiscal period
     */
    public List<BankTransaction> getCategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("InteractiveClassificationService", "getCategorizedTransactions", companyId, fiscalPeriodId);

        validateCompanyAndFiscalPeriodIds(companyId, fiscalPeriodId);

        List<BankTransaction> allTransactions = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);

        List<BankTransaction> categorizedTransactions = allTransactions.stream()
            .filter(tx -> tx.getAccountCode() != null && !tx.getAccountCode().trim().isEmpty())
            .collect(Collectors.toList());

        debugger.logMethodExit("InteractiveClassificationService", "getCategorizedTransactions",
            String.format("Found %d categorized transactions", categorizedTransactions.size()));

        return categorizedTransactions;
    }

    /**
     * Validate company and fiscal period IDs
     */
    private void validateCompanyAndFiscalPeriodIds(Long companyId, Long fiscalPeriodId) {
        if (companyId == null || companyId <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive number");
        }
        if (fiscalPeriodId == null || fiscalPeriodId <= 0) {
            throw new IllegalArgumentException("Fiscal period ID must be a positive number");
        }
    }

    /**
     * Classify a single transaction
     */
    public BankTransaction classifyTransaction(Long transactionId, String accountCode, String accountName, String classifiedBy) {
        debugger.logMethodEntry("InteractiveClassificationService", "classifyTransaction",
            transactionId, accountCode, accountName, classifiedBy);

        // Input validation
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID cannot be null");
        }
        if (accountCode == null || accountCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Account code cannot be null or empty");
        }
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        if (classifiedBy == null || classifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Classified by cannot be null or empty");
        }

        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Update transaction classification
        transaction.setAccountCode(accountCode.trim());
        transaction.setAccountName(accountName.trim());
        // Persist a user-visible category value for reporting/audit trail; fallback to accountName
        transaction.setCategory(accountName.trim());
        transaction.setClassificationDate(java.time.LocalDateTime.now());
        transaction.setClassifiedBy(classifiedBy.trim());

        BankTransaction saved = bankTransactionRepository.save(transaction);

        // Ensure any lacking journal entries are created for this transaction
        try {
            classificationEngine.syncJournalEntries(saved.getCompanyId());
        } catch (Exception e) {
            // Don't fail classification if journal entry generation fails; log and continue
            debugger.logException("Interactive classification - generate journal entries", "InteractiveClassificationService", "classifyTransaction", e, transactionId);
        }

        debugger.logMethodExit("InteractiveClassificationService", "classifyTransaction", saved.getId());
        return saved;
    }

    /**
     * Get account suggestions for a transaction based on description patterns
     */
    public List<AccountSuggestion> getAccountSuggestions(Long companyId, String transactionDescription) {
        debugger.logMethodEntry("InteractiveClassificationService", "getAccountSuggestions", companyId, transactionDescription);

        if (transactionDescription == null || transactionDescription.trim().isEmpty()) {
            return getDefaultAccountSuggestions();
        }

        List<Account> accounts = accountRepository.findByCompanyId(companyId);
        List<AccountSuggestion> suggestions = new ArrayList<>();

        String lowerDescription = transactionDescription.toLowerCase().trim();

        for (Account account : accounts) {
            if (account.getAccountName() != null && suggestions.size() < MAX_SUGGESTIONS) {
                String accountName = account.getAccountName().toLowerCase();
                String matchedKeyword = findMatchingKeyword(lowerDescription, accountName);

                if (matchedKeyword != null) {
                    suggestions.add(new AccountSuggestion(
                        account.getAccountCode(),
                        account.getAccountName(),
                        "Keyword match: " + matchedKeyword
                    ));
                }
            }
        }

        return suggestions.isEmpty() ? getDefaultAccountSuggestions() : suggestions;
    }

    /**
     * Find matching keyword between transaction description and account name
     */
    private String findMatchingKeyword(String description, String accountName) {
        String[] accountWords = accountName.split("\\s+");

        for (String word : accountWords) {
            if (word.length() >= MIN_KEYWORD_LENGTH && description.contains(word)) {
                return word;
            }
        }
        return null;
    }

    /**
     * Get default account suggestions when no matches are found
     */
    private List<AccountSuggestion> getDefaultAccountSuggestions() {
        return Arrays.asList(
            new AccountSuggestion("8100", "Employee Costs", "General expense account"),
            new AccountSuggestion("4000", "Sales Revenue", "General revenue account"),
            new AccountSuggestion("5000", "Operating Expenses", "General operating costs")
        );
    }

    /**
     * Get classification summary for a company and fiscal period
     */
    public ClassificationSummary getClassificationSummary(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("InteractiveClassificationService", "getClassificationSummary", companyId, fiscalPeriodId);

        List<BankTransaction> allTransactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        List<BankTransaction> categorizedTransactions = allTransactions.stream()
            .filter(tx -> tx.getAccountCode() != null && !tx.getAccountCode().trim().isEmpty())
            .collect(Collectors.toList());

        int total = allTransactions.size();
        int categorized = categorizedTransactions.size();
        int uncategorized = total - categorized;
        double percentage = total > 0 ? ((double) categorized * 100) / total : 0;

        ClassificationSummary summary = new ClassificationSummary(total, categorized, uncategorized, percentage);

        debugger.logMethodExit("InteractiveClassificationService", "getClassificationSummary", summary);
        return summary;
    }

    /**
     * Find similar uncategorized transactions based on description patterns
     */
    public List<BankTransaction> findSimilarTransactions(Long companyId, Long fiscalPeriodId, String pattern) {
        debugger.logMethodEntry("InteractiveClassificationService", "findSimilarTransactions", companyId, fiscalPeriodId, pattern);

        if (pattern == null || pattern.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<BankTransaction> uncategorized = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

        String lowerPattern = pattern.toLowerCase().trim();
        List<BankTransaction> similar = uncategorized.stream()
            .filter(tx -> tx.getDetails() != null &&
                         tx.getDetails().toLowerCase().contains(lowerPattern))
            .limit(MAX_SIMILAR_TRANSACTIONS)
            .collect(Collectors.toList());

        debugger.logMethodExit("InteractiveClassificationService", "findSimilarTransactions",
            String.format("Found %d similar transactions", similar.size()));

        return similar;
    }

    /**
     * Batch classify multiple transactions
     */
    public int batchClassifyTransactions(List<Long> transactionIds, String accountCode, String accountName, String classifiedBy) {
        debugger.logMethodEntry("InteractiveClassificationService", "batchClassifyTransactions",
            transactionIds.size(), accountCode, accountName, classifiedBy);

        if (transactionIds == null || transactionIds.isEmpty()) {
            debugger.logMethodExit("InteractiveClassificationService", "batchClassifyTransactions",
                "No transaction IDs provided");
            return 0;
        }

        // Validate input parameters
        if (accountCode == null || accountCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Account code cannot be null or empty");
        }
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be null or empty");
        }
        if (classifiedBy == null || classifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Classified by cannot be null or empty");
        }

        int successful = 0;
        List<String> errors = new ArrayList<>();

        for (Long transactionId : transactionIds) {
            try {
                classifyTransaction(transactionId, accountCode, accountName, classifiedBy);
                successful++;
            } catch (Exception e) {
                String errorMsg = String.format("Failed to classify transaction %d: %s", transactionId, e.getMessage());
                errors.add(errorMsg);
                debugger.logException("Batch Classification", "InteractiveClassificationService",
                    "batchClassifyTransactions", e, transactionId);
            }
        }

        debugger.logMethodExit("InteractiveClassificationService", "batchClassifyTransactions",
            String.format("Batch classification completed: %d successful, %d failed out of %d total",
                successful, errors.size(), transactionIds.size()));

        return successful;
    }

    /**
     * DTO classes for the service
     */
    public static class AccountSuggestion {
        private final String accountCode;
        private final String accountName;
        private final String reason;

        public AccountSuggestion(String accountCode, String accountName, String reason) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.reason = reason;
        }

        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public String getReason() { return reason; }
    }

    public static class ClassificationSummary {
        private final int totalTransactions;
        private final int categorizedCount;
        private final int uncategorizedCount;
        private final double categorizedPercentage;

        public ClassificationSummary(int totalTransactions, int categorizedCount, int uncategorizedCount, double categorizedPercentage) {
            this.totalTransactions = totalTransactions;
            this.categorizedCount = categorizedCount;
            this.uncategorizedCount = uncategorizedCount;
            this.categorizedPercentage = categorizedPercentage;
        }

        public int getTotalTransactions() { return totalTransactions; }
        public int getCategorizedCount() { return categorizedCount; }
        public int getUncategorizedCount() { return uncategorizedCount; }
        public double getCategorizedPercentage() { return categorizedPercentage; }
    }
}