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

import org.springframework.stereotype.Service;

import fin.entity.BankTransaction;
import fin.entity.TransactionMappingRule;
import fin.entity.JournalEntry;
import fin.entity.JournalEntryLine;
import fin.entity.Account;
import fin.repository.BankTransactionRepository;
import fin.repository.AccountRepository;
import fin.repository.JournalEntryRepository;
import fin.repository.JournalEntryLineRepository;
import java.math.BigDecimal;
import java.util.List;

/**
 * Spring wrapper for the AccountClassificationService.
 * Provides Spring bean integration for transaction classification.
 * 
 * DEPRECATED: Use AccountClassificationService directly instead.
 * This wrapper exists for backward compatibility during migration.
 */
@Service
@Deprecated
public class AccountClassificationService {

    private final BankTransactionRepository bankTransactionRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public AccountClassificationService(BankTransactionRepository bankTransactionRepository,
                                       AccountRepository accountRepository,
                                       JournalEntryRepository journalEntryRepository,
                                       JournalEntryLineRepository journalEntryLineRepository) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }

    /**
     * Initialize chart of accounts for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void initializeChartOfAccounts(Long companyId) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Initialize transaction mapping rules for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int initializeTransactionMappingRules(Long companyId) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Classify all unclassified transactions for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int classifyAllUnclassifiedTransactions(Long companyId, String username) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Reclassify all transactions for a company
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int reclassifyAllTransactions(Long companyId, String username) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Generate journal entries for classified transactions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public int generateJournalEntriesForClassifiedTransactions(Long companyId, String createdBy) {
        List<BankTransaction> transactions = bankTransactionRepository.findClassifiedTransactionsWithoutJournalEntries(companyId);
        int count = 0;
        for (BankTransaction transaction : transactions) {
            // Create journal entry
            JournalEntry journalEntry = new JournalEntry();
            journalEntry.setCompanyId(companyId);
            journalEntry.setFiscalPeriodId(transaction.getFiscalPeriodId());
            journalEntry.setReference(transaction.getReference());
            journalEntry.setDescription(transaction.getReference() + " - " + transaction.getAccountName());
            journalEntry.setCreatedBy(createdBy);
            journalEntry.setCreatedAt(java.time.LocalDateTime.now());
            JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

            // Create debit line
            Account debitAccount = accountRepository.findByCompanyIdAndAccountCode(companyId, transaction.getAccountCode()).orElseThrow();
            JournalEntryLine debitLine = new JournalEntryLine();
            debitLine.setJournalEntryId(savedEntry.getId());
            debitLine.setAccountId(debitAccount.getId());
            debitLine.setDebitAmount(transaction.getCreditAmount()); // Assuming credit transaction
            debitLine.setCreditAmount(BigDecimal.ZERO);
            debitLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(debitLine);

            // Create credit line for bank account
            Account creditAccount = accountRepository.findByCompanyIdAndAccountCode(companyId, "1000").orElseThrow();
            JournalEntryLine creditLine = new JournalEntryLine();
            creditLine.setJournalEntryId(savedEntry.getId());
            creditLine.setAccountId(creditAccount.getId());
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(transaction.getCreditAmount());
            creditLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(creditLine);

            count++;
        }
        return count;
    }

    /**
     * Generate journal entries for unclassified transactions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Classify a single transaction
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public boolean classifyTransaction(BankTransaction transaction, String accountCode, String accountName) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Generate account classification report
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public void generateClassificationReport(Long companyId) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Get account mapping suggestions
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public java.util.Map<String, String> getAccountMappingSuggestions(Long companyId) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Get standard mapping rules
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     */
    public java.util.List<TransactionMappingRule> getStandardMappingRules() {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }

    /**
     * Update transaction classification with manual account selection.
     * DELEGATES TO SINGLE SOURCE OF TRUTH
     * 
     * @param companyId The company ID
     * @param transactionId The transaction to update
     * @param debitAccountId The debit account ID
     * @param creditAccountId The credit account ID
     */
    public void updateTransactionClassification(Long companyId, Long transactionId, 
                                              Long debitAccountId, Long creditAccountId, String username) {
        throw new UnsupportedOperationException("Use TransactionClassificationService instead");
    }
}
