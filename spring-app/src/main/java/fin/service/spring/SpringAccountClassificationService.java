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

import org.springframework.stereotype.Service;

/**
 * Spring wrapper for the AccountClassificationService.
 * Provides Spring bean integration for transaction classification.
 * 
 * DEPRECATED: Use AccountClassificationService directly instead.
 * This wrapper exists for backward compatibility during migration.
 */
@Service
@Deprecated
public class SpringAccountClassificationService {

    private final AccountClassificationService accountClassificationService;

    public SpringAccountClassificationService(AccountClassificationService accountClassificationService) {
        this.accountClassificationService = accountClassificationService;
    }

    /**
     * Initialize chart of accounts for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void initializeChartOfAccounts(Long companyId) {
        accountClassificationService.initializeChartOfAccounts(companyId);
    }

    /**
     * Initialize transaction mapping rules for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int initializeTransactionMappingRules(Long companyId) {
        return accountClassificationService.initializeTransactionMappingRules(companyId);
    }

    /**
     * Classify all unclassified transactions for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int classifyAllUnclassifiedTransactions(Long companyId, String username) {
        return accountClassificationService.classifyAllUnclassifiedTransactions(companyId, username);
    }

    /**
     * Reclassify all transactions for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int reclassifyAllTransactions(Long companyId, String username) {
        return accountClassificationService.reclassifyAllTransactions(companyId, username);
    }

    /**
     * Generate journal entries for classified transactions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int generateJournalEntriesForClassifiedTransactions(Long companyId, String createdBy) {
        return accountClassificationService.generateJournalEntriesForClassifiedTransactions(companyId, createdBy);
    }

    /**
     * Generate journal entries for unclassified transactions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        accountClassificationService.generateJournalEntriesForUnclassifiedTransactions(companyId, createdBy);
    }

    /**
     * Classify a single transaction
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public boolean classifyTransaction(fin.model.BankTransaction transaction, String accountCode, String accountName) {
        return accountClassificationService.classifyTransaction(transaction, accountCode, accountName);
    }

    /**
     * Generate account classification report
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void generateClassificationReport(Long companyId) {
        accountClassificationService.generateClassificationReport(companyId);
    }

    /**
     * Get account mapping suggestions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public java.util.Map<String, String> getAccountMappingSuggestions(Long companyId) {
        return accountClassificationService.getAccountMappingSuggestions(companyId);
    }

    /**
     * Get standard mapping rules
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public java.util.List<fin.model.TransactionMappingRule> getStandardMappingRules() {
        return accountClassificationService.getStandardMappingRules();
    }
}