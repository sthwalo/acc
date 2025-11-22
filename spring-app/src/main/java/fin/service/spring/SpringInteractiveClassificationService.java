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
import fin.repository.AccountRepository;
import fin.repository.BankTransactionRepository;
import fin.repository.CompanyRepository;
import fin.repository.FiscalPeriodRepository;
import fin.util.SpringDebugger;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Spring service for interactive transaction classification
 * Provides REST-based classification workflow instead of console-based interaction
 */
@Service
public class SpringInteractiveClassificationService {

    private final BankTransactionRepository bankTransactionRepository;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final SpringDebugger debugger;

    public SpringInteractiveClassificationService(BankTransactionRepository bankTransactionRepository,
                                                AccountRepository accountRepository,
                                                CompanyRepository companyRepository,
                                                FiscalPeriodRepository fiscalPeriodRepository,
                                                SpringDebugger debugger) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.debugger = debugger;
    }

    /**
     * Get uncategorized transactions for a company and fiscal period
     */
    public List<BankTransaction> getUncategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "getUncategorizedTransactions", companyId, fiscalPeriodId);

        List<BankTransaction> transactions = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

        debugger.logMethodExit("SpringInteractiveClassificationService", "getUncategorizedTransactions",
            String.format("Found %d uncategorized transactions", transactions.size()));

        return transactions;
    }

    /**
     * Get categorized transactions for a company and fiscal period
     */
    public List<BankTransaction> getCategorizedTransactions(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "getCategorizedTransactions", companyId, fiscalPeriodId);

        List<BankTransaction> transactions = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNotNull(companyId, fiscalPeriodId);

        debugger.logMethodExit("SpringInteractiveClassificationService", "getCategorizedTransactions",
            String.format("Found %d categorized transactions", transactions.size()));

        return transactions;
    }

    /**
     * Classify a single transaction
     */
    public BankTransaction classifyTransaction(Long transactionId, String accountCode, String accountName, String classifiedBy) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "classifyTransaction",
            transactionId, accountCode, accountName, classifiedBy);

        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Update transaction classification
        transaction.setAccountCode(accountCode);
        transaction.setAccountName(accountName);
        transaction.setClassificationDate(java.time.LocalDateTime.now());
        transaction.setClassifiedBy(classifiedBy);

        BankTransaction saved = bankTransactionRepository.save(transaction);

        debugger.logMethodExit("SpringInteractiveClassificationService", "classifyTransaction", saved.getId());
        return saved;
    }

    /**
     * Get account suggestions for a transaction based on description patterns
     */
    public List<AccountSuggestion> getAccountSuggestions(Long companyId, String transactionDescription) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "getAccountSuggestions", companyId, transactionDescription);

        List<Account> accounts = accountRepository.findByCompanyId(companyId);
        List<AccountSuggestion> suggestions = new ArrayList<>();

        String lowerDescription = transactionDescription.toLowerCase();

        // Simple keyword matching for suggestions
        for (Account account : accounts) {
            if (account.getAccountName() != null) {
                String accountName = account.getAccountName().toLowerCase();
                String[] accountWords = accountName.split(" ");

                for (String word : accountWords) {
                    if (word.length() > 3 && lowerDescription.contains(word)) {
                        suggestions.add(new AccountSuggestion(
                            account.getAccountCode(),
                            account.getAccountName(),
                            "Keyword match: " + word
                        ));
                        break;
                    }
                }

                if (suggestions.size() >= 5) { // Limit to 5 suggestions
                    break;
                }
            }
        }

        // Add fallback suggestions if no matches
        if (suggestions.isEmpty()) {
            suggestions.add(new AccountSuggestion("8100", "Employee Costs", "General expense account"));
            suggestions.add(new AccountSuggestion("4000", "Sales Revenue", "General revenue account"));
            suggestions.add(new AccountSuggestion("5000", "Operating Expenses", "General operating costs"));
        }

        debugger.logMethodExit("SpringInteractiveClassificationService", "getAccountSuggestions",
            String.format("Found %d suggestions", suggestions.size()));

        return suggestions;
    }

    /**
     * Get classification summary for a company and fiscal period
     */
    public ClassificationSummary getClassificationSummary(Long companyId, Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "getClassificationSummary", companyId, fiscalPeriodId);

        List<BankTransaction> allTransactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        List<BankTransaction> categorizedTransactions = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNotNull(companyId, fiscalPeriodId);

        int total = allTransactions.size();
        int categorized = categorizedTransactions.size();
        int uncategorized = total - categorized;
        double percentage = total > 0 ? ((double) categorized * 100) / total : 0;

        ClassificationSummary summary = new ClassificationSummary(total, categorized, uncategorized, percentage);

        debugger.logMethodExit("SpringInteractiveClassificationService", "getClassificationSummary", summary);
        return summary;
    }

    /**
     * Find similar uncategorized transactions based on description patterns
     */
    public List<BankTransaction> findSimilarTransactions(Long companyId, Long fiscalPeriodId, String pattern) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "findSimilarTransactions", companyId, fiscalPeriodId, pattern);

        List<BankTransaction> uncategorized = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

        String lowerPattern = pattern.toLowerCase();
        List<BankTransaction> similar = uncategorized.stream()
            .filter(tx -> tx.getDetails() != null && tx.getDetails().toLowerCase().contains(lowerPattern))
            .limit(10) // Limit results
            .collect(Collectors.toList());

        debugger.logMethodExit("SpringInteractiveClassificationService", "findSimilarTransactions",
            String.format("Found %d similar transactions", similar.size()));

        return similar;
    }

    /**
     * Batch classify multiple transactions
     */
    public int batchClassifyTransactions(List<Long> transactionIds, String accountCode, String accountName, String classifiedBy) {
        debugger.logMethodEntry("SpringInteractiveClassificationService", "batchClassifyTransactions",
            transactionIds.size(), accountCode, accountName, classifiedBy);

        int classified = 0;
        for (Long transactionId : transactionIds) {
            try {
                classifyTransaction(transactionId, accountCode, accountName, classifiedBy);
                classified++;
            } catch (Exception e) {
                debugger.logException("Batch Classification", "SpringInteractiveClassificationService",
                    "batchClassifyTransactions", e, transactionId);
            }
        }

        debugger.logMethodExit("SpringInteractiveClassificationService", "batchClassifyTransactions",
            String.format("Classified %d out of %d transactions", classified, transactionIds.size()));

        return classified;
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