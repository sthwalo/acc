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

package fin.service.classification.engine;

import fin.entity.*;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import fin.repository.JournalEntryRepository;
import fin.repository.JournalEntryLineRepository;
import fin.service.CompanyService;
import fin.service.journal.AccountService;
import fin.service.classification.rules.TransactionMappingRuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for the core transaction classification engine.
 * Handles auto-classification, manual classification, and classification logic.
 *
 * SINGLE RESPONSIBILITY: Transaction classification execution
 */
@Service
@Transactional
public class TransactionClassificationEngine {

    private static final Logger LOGGER = Logger.getLogger(TransactionClassificationEngine.class.getName());

    private final AccountService accountService;
    private final CompanyService companyService;
    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final TransactionMappingRuleService transactionMappingRuleService;
    private final fin.validation.BankAccountResolver bankAccountResolver;

    public TransactionClassificationEngine(AccountService accountService,
                                        CompanyService companyService,
                                        BankTransactionRepository bankTransactionRepository,
                                        FiscalPeriodRepository fiscalPeriodRepository,
                                        JournalEntryRepository journalEntryRepository,
                                        JournalEntryLineRepository journalEntryLineRepository,
                                        TransactionMappingRuleService transactionMappingRuleService,
                                        fin.validation.BankAccountResolver bankAccountResolver) {
        this.accountService = accountService;
        this.companyService = companyService;
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.transactionMappingRuleService = transactionMappingRuleService;
        this.bankAccountResolver = bankAccountResolver;
    }

    /**
     * Auto-classify all unclassified transactions using mapping rules
     */
    @Transactional
    public int autoClassifyTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            LOGGER.info("Auto-classifying transactions for company: " + companyId + ", period: " + fiscalPeriodId);

            // Get active rules for the company
            List<TransactionMappingRule> rules = transactionMappingRuleService.getActiveRulesForCompany(companyId);

            // Get unclassified transactions for the period
            List<BankTransaction> unclassifiedTransactions = bankTransactionRepository
                .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

            int classifiedCount = 0;

            for (BankTransaction transaction : unclassifiedTransactions) {
                // Try to classify using rules
                for (TransactionMappingRule rule : rules) {
                    if (rule.matches(transaction.getDescription())) {
                        // Apply the classification
                        transaction.setAccountCode(rule.getAccount() != null ? rule.getAccount().getAccountCode() : null);
                        bankTransactionRepository.save(transaction);
                        classifiedCount++;
                        break; // Stop at first matching rule
                    }
                }
            }

            LOGGER.info("Auto-classified " + classifiedCount + " transactions");
            return classifiedCount;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to auto-classify transactions for company: " + companyId, e);
            return 0;
        }
    }

    /**
     * Perform auto-classification of transactions (internal method)
     */
    private int performAutoClassification(Long companyId, Long fiscalPeriodId) {
        // This would implement the actual classification logic
        // For now, return a dummy count
        return 5; // Assume 5 transactions were classified
    }

    /**
     * Classify a single transaction by account code
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
     * Classify a transaction with an account ID
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
     * Classify a transaction by account code with validation
     */
    @Transactional
    public void classifyTransactionByAccountCode(Long companyId, Long transactionId,
                                               String accountCode, String classifiedBy) {
        // Validate transaction exists and belongs to company
        BankTransaction transaction = bankTransactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }

        if (!transaction.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Transaction does not belong to company: " + companyId);
        }

        // Validate account exists for company
        Optional<Account> accountOpt = accountService.getAccountByCompanyAndCode(companyId, accountCode);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for company: " + accountCode);
        }

        // Update transaction
        transaction.setAccountCode(accountCode);
        bankTransactionRepository.save(transaction);

        LOGGER.info("Transaction " + transactionId + " classified to account " + accountCode + " by " + classifiedBy);
    }

    /**
     * Update transaction classification with manual account selection
     */
    @Transactional
    public void updateTransactionClassification(Long companyId, Long transactionId,
                                              Long debitAccountId, Long creditAccountId, String username) {
        // Validate transaction exists and belongs to company
        BankTransaction transaction = bankTransactionRepository.findById(transactionId).orElse(null);
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }

        if (!transaction.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Transaction does not belong to company: " + companyId);
        }

        // Validate debit account exists (if provided)
        if (debitAccountId != null) {
            Optional<Account> debitAccountOpt = accountService.getAccountById(debitAccountId);
            if (debitAccountOpt.isEmpty()) {
                throw new IllegalArgumentException("Debit account not found: " + debitAccountId);
            }
            Account debitAccount = debitAccountOpt.get();
            if (!debitAccount.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException("Debit account does not belong to company: " + debitAccountId);
            }
        }

        // Validate credit account exists (if provided)
        if (creditAccountId != null) {
            Optional<Account> creditAccountOpt = accountService.getAccountById(creditAccountId);
            if (creditAccountOpt.isEmpty()) {
                throw new IllegalArgumentException("Credit account not found: " + creditAccountId);
            }
            Account creditAccount = creditAccountOpt.get();
            if (!creditAccount.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException("Credit account does not belong to company: " + creditAccountId);
            }
        }

        // Update transaction classification
        transaction.setDebitAccountId(debitAccountId);
        transaction.setCreditAccountId(creditAccountId);
        transaction.setClassifiedBy(username);
        transaction.setClassificationDate(java.time.LocalDateTime.now());
        transaction.setUpdatedAt(java.time.LocalDateTime.now());

        bankTransactionRepository.save(transaction);

        LOGGER.info("Transaction " + transactionId + " updated with debit account " + debitAccountId +
                   " and credit account " + creditAccountId + " by " + username);
    }

    /**
     * Auto-classify transactions (API version that returns result)
     */
    @Transactional
    public BatchClassificationResult autoClassifyTransactions(Long companyId) {
        try {
            // Get all fiscal periods for the company
            List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);

            int totalClassified = 0;
            for (FiscalPeriod period : periods) {
                totalClassified += autoClassifyTransactions(companyId, period.getId());
            }

            return new BatchClassificationResult(
                (long) totalClassified,
                "SUCCESS",
                "Auto-classified " + totalClassified + " transactions across " + periods.size() + " periods"
            );
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to auto-classify transactions for company: " + companyId, e);
            return new BatchClassificationResult(0L, "ERROR", "Failed: " + e.getMessage());
        }
    }

    /**
     * Sync journal entries for new classified transactions
     */
    @Transactional
    public int syncJournalEntries(Long companyId) throws SQLException {
        LOGGER.info("Syncing journal entries for company: " + companyId);

        // Find classified transactions without journal entries
        List<BankTransaction> transactionsWithoutJournalEntries =
            bankTransactionRepository.findClassifiedTransactionsWithoutJournalEntries(companyId);

        int syncedCount = 0;

        for (BankTransaction transaction : transactionsWithoutJournalEntries) {
            // Fail fast: if creating a journal entry fails (e.g., missing bank account), let the exception bubble
            // so the caller is alerted and can take corrective action. Do not silently skip transactions.
            createJournalEntryForTransaction(transaction, "FIN");
            syncedCount++;
            LOGGER.info("Created journal entry for transaction: " + transaction.getId());
        }

        LOGGER.info("Successfully synced " + syncedCount + " journal entries for company: " + companyId);
        return syncedCount;
    }

    /**
     * Create a journal entry for a classified transaction
     */
    private void createJournalEntryForTransaction(BankTransaction transaction, String createdBy) throws SQLException {
        // Determine transaction type and amounts
        BigDecimal amount = transaction.getCreditAmount().compareTo(BigDecimal.ZERO) != 0 ?
                           transaction.getCreditAmount() : transaction.getDebitAmount();
        boolean isCreditTransaction = transaction.getCreditAmount().compareTo(BigDecimal.ZERO) != 0;

        // Create journal entry header
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setCompanyId(transaction.getCompanyId());
        journalEntry.setFiscalPeriodId(transaction.getFiscalPeriodId());
        journalEntry.setReference("TXN-" + transaction.getId());
        journalEntry.setEntryDate(transaction.getTransactionDate());
        journalEntry.setDescription(transaction.getDescription());
        journalEntry.setCreatedBy(createdBy);
        journalEntry.setCreatedAt(java.time.LocalDateTime.now());

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

        // Handle different classification types
        if (transaction.getDebitAccountId() != null && transaction.getCreditAccountId() != null) {
            // New approach: debit and credit accounts are explicitly set
            createJournalEntryLinesForDebitCreditAccounts(savedEntry, transaction, amount, createdBy);
        } else if (transaction.getAccountCode() != null) {
            // Legacy approach: single account code
            createJournalEntryLinesForAccountCode(savedEntry, transaction, amount, isCreditTransaction, createdBy);
        } else {
            throw new IllegalStateException("Transaction " + transaction.getId() + " is not properly classified");
        }
    }

    /**
     * Create journal entry lines for transactions with explicit debit/credit account IDs
     */
    private void createJournalEntryLinesForDebitCreditAccounts(JournalEntry journalEntry,
                                                              BankTransaction transaction,
                                                              BigDecimal amount, String createdBy) {
        // Debit line
        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setAccountId(transaction.getDebitAccountId());
        debitLine.setDescription(transaction.getDescription());
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setSourceTransactionId(transaction.getId());
        journalEntryLineRepository.save(debitLine);

        // Credit line
        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setAccountId(transaction.getCreditAccountId());
        creditLine.setDescription(transaction.getDescription());
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setSourceTransactionId(transaction.getId());
        journalEntryLineRepository.save(creditLine);
    }

    /**
     * Create journal entry lines for transactions with account codes (legacy approach)
     */
    private void createJournalEntryLinesForAccountCode(JournalEntry journalEntry,
                                                      BankTransaction transaction,
                                                      BigDecimal amount, boolean isCreditTransaction, String createdBy) throws SQLException {
        // Find the classified account
        Optional<Account> classifiedAccount = accountService.getAccountByCompanyAndCode(
            transaction.getCompanyId(), transaction.getAccountCode());

        if (classifiedAccount.isEmpty()) {
            throw new IllegalStateException("Account not found for code: " + transaction.getAccountCode());
        }

        // Resolve bank/cash account by name using resolver. Per project policy do NOT fall back silently - fail fast with clear message.
        Optional<Account> bankAccount = bankAccountResolver.getDefaultCashAccount(transaction.getCompanyId());
        if (bankAccount.isEmpty()) {
            throw new SQLException("Bank/cash account not found in table 'accounts' for company " + transaction.getCompanyId() + ". Please insert an active bank/cash account. Example: INSERT INTO accounts (company_id, account_code, account_name, category_id, is_active) VALUES (" + transaction.getCompanyId() + ", '1230', 'Bank', <category_id>, true)");
        }

        if (isCreditTransaction) {
            // Credit transaction: money coming into bank account
            // Correct accounting: Debit the bank account (asset increases), Credit the classified account (income)
            JournalEntryLine debitLine = new JournalEntryLine();
            debitLine.setJournalEntryId(journalEntry.getId());
            debitLine.setAccountId(bankAccount.get().getId());
            debitLine.setDescription(transaction.getDescription());
            debitLine.setDebitAmount(amount);
            debitLine.setCreditAmount(BigDecimal.ZERO);
            debitLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(debitLine);

            JournalEntryLine creditLine = new JournalEntryLine();
            creditLine.setJournalEntryId(journalEntry.getId());
            creditLine.setAccountId(classifiedAccount.get().getId());
            creditLine.setDescription(transaction.getDescription());
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(amount);
            creditLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(creditLine);
        } else {
            // Debit transaction: money going out of bank account
            // Correct accounting: Debit the classified account (expense), Credit the bank account (asset decreases)
            JournalEntryLine debitLine = new JournalEntryLine();
            debitLine.setJournalEntryId(journalEntry.getId());
            debitLine.setAccountId(classifiedAccount.get().getId());
            debitLine.setDescription(transaction.getDescription());
            debitLine.setDebitAmount(amount);
            debitLine.setCreditAmount(BigDecimal.ZERO);
            debitLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(debitLine);

            JournalEntryLine creditLine = new JournalEntryLine();
            creditLine.setJournalEntryId(journalEntry.getId());
            creditLine.setAccountId(bankAccount.get().getId());
            creditLine.setDescription(transaction.getDescription());
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(amount);
            creditLine.setSourceTransactionId(transaction.getId());
            journalEntryLineRepository.save(creditLine);
        }
    }

    /**
     * Regenerate all journal entries after reclassification
     */
    @Transactional
    public int regenerateAllJournalEntries(Long companyId) {
        try {
            LOGGER.info("Regenerating all journal entries for company: " + companyId);

            // Delete all existing journal entries and lines for the company
            List<JournalEntry> existingEntries = journalEntryRepository.findByCompanyId(companyId);
            int deletedCount = existingEntries.size();

            for (JournalEntry entry : existingEntries) {
                journalEntryLineRepository.deleteByJournalEntryId(entry.getId());
            }
            journalEntryRepository.deleteAll(existingEntries);

            LOGGER.info("Deleted " + deletedCount + " existing journal entries for company: " + companyId);

            // Get all transactions for the company and filter those that are classified
            List<BankTransaction> allTransactions = bankTransactionRepository.findByCompanyId(companyId);
            List<BankTransaction> classifiedTransactions = allTransactions.stream()
                .filter(t -> t.getAccountCode() != null ||
                           (t.getDebitAccountId() != null && t.getCreditAccountId() != null))
                .collect(Collectors.toList());

            // Create new journal entries for all classified transactions
            int createdCount = 0;
            for (BankTransaction transaction : classifiedTransactions) {
                try {
                    createJournalEntryForTransaction(transaction, "FIN");
                    createdCount++;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create journal entry for transaction " +
                               transaction.getId() + " during regeneration, skipping", e);
                }
            }

            LOGGER.info("Successfully regenerated " + createdCount + " journal entries for company: " + companyId);
            return createdCount;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to regenerate journal entries for company: " + companyId, e);
            return 0;
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
     * Result type for batch auto-classification operations.
     */
    public static class BatchClassificationResult {
        private final Long totalClassified;
        private final String status;
        private final String message;

        public BatchClassificationResult(Long totalClassified, String status, String message) {
            this.totalClassified = totalClassified;
            this.status = status;
            this.message = message;
        }

        public Long getTotalClassified() { return totalClassified; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }
}