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
import fin.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Spring Service for data management, integrity validation, and manual corrections.
 * Handles manual invoices, journal entries, data corrections, and company data resets.
 */
@Service
@Transactional
public class SpringDataManagementService {

    private static final Logger LOGGER = Logger.getLogger(SpringDataManagementService.class.getName());

    // Dependencies
    private final SpringCompanyService companyService;
    private final SpringAccountService accountService;
    private final AccountRepository accountRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ManualInvoiceRepository manualInvoiceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final DataCorrectionRepository dataCorrectionRepository;

    public SpringDataManagementService(SpringCompanyService companyService,
                                     SpringAccountService accountService,
                                     AccountRepository accountRepository,
                                     FiscalPeriodRepository fiscalPeriodRepository,
                                     BankTransactionRepository bankTransactionRepository,
                                     ManualInvoiceRepository manualInvoiceRepository,
                                     JournalEntryRepository journalEntryRepository,
                                     JournalEntryLineRepository journalEntryLineRepository,
                                     DataCorrectionRepository dataCorrectionRepository) {
        this.companyService = companyService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.manualInvoiceRepository = manualInvoiceRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.dataCorrectionRepository = dataCorrectionRepository;
    }

    /**
     * Resets all transactional data for a company while optionally preserving master data.
     */
    @Transactional
    public void resetCompanyData(Long companyId, boolean preserveMasterData) {
        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        try {
            // Delete transactional data in correct order (due to foreign keys)

            // Delete journal entry lines first
            List<JournalEntry> journalEntries = journalEntryRepository.findByCompanyId(companyId);
            for (JournalEntry je : journalEntries) {
                journalEntryLineRepository.deleteByJournalEntryId(je.getId());
            }

            // Delete other transactional data
            dataCorrectionRepository.deleteByCompanyId(companyId);
            journalEntryRepository.deleteByCompanyId(companyId);
            manualInvoiceRepository.deleteByCompanyId(companyId);
            bankTransactionRepository.deleteByCompanyId(companyId);

            // Optionally reset master data
            if (!preserveMasterData) {
                accountRepository.deleteByCompanyId(companyId);
                fiscalPeriodRepository.deleteByCompanyId(companyId);
            }

            LOGGER.info("Company data reset successful for company ID: " + companyId);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error resetting company data", e);
            throw new RuntimeException("Failed to reset company data", e);
        }
    }

    /**
     * Creates a manual invoice with validation and automatic journal entry creation.
     */
    @Transactional
    public ManualInvoice createManualInvoice(Long companyId, String invoiceNumber, LocalDate invoiceDate,
                                           String description, BigDecimal amount, Long debitAccountId,
                                           Long creditAccountId, Long fiscalPeriodId) {
        // Validate inputs
        if (companyId == null || invoiceNumber == null || invoiceDate == null ||
            description == null || amount == null || debitAccountId == null ||
            creditAccountId == null || fiscalPeriodId == null) {
            throw new IllegalArgumentException("All invoice parameters are required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Validate accounts exist and belong to company
        Account debitAccount = accountService.getAccountById(debitAccountId).orElse(null);
        if (debitAccount == null || !debitAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Invalid debit account: " + debitAccountId);
        }

        Account creditAccount = accountService.getAccountById(creditAccountId).orElse(null);
        if (creditAccount == null || !creditAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Invalid credit account: " + creditAccountId);
        }

        // Check if invoice number already exists
        if (manualInvoiceRepository.existsByCompanyIdAndInvoiceNumber(companyId, invoiceNumber)) {
            throw new IllegalArgumentException("Invoice number already exists: " + invoiceNumber);
        }

        // Create manual invoice
        ManualInvoice invoice = new ManualInvoice();
        invoice.setCompanyId(companyId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(invoiceDate);
        invoice.setDescription(description);
        invoice.setAmount(amount);
        invoice.setDebitAccountId(debitAccountId);
        invoice.setCreditAccountId(creditAccountId);
        invoice.setFiscalPeriodId(fiscalPeriodId);

        ManualInvoice savedInvoice = manualInvoiceRepository.save(invoice);

        // Create corresponding journal entry
        createInvoiceJournalEntry(savedInvoice);

        LOGGER.info("Manual invoice and journal entry created successfully: " + invoiceNumber);
        return savedInvoice;
    }

    /**
     * Creates the journal entry for a manual invoice
     */
    private void createInvoiceJournalEntry(ManualInvoice invoice) {
        // Create journal entry header
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setCompanyId(invoice.getCompanyId());
        journalEntry.setReference("INV-" + invoice.getInvoiceNumber());
        journalEntry.setEntryDate(invoice.getInvoiceDate());
        journalEntry.setDescription("Invoice: " + invoice.getDescription());
        journalEntry.setFiscalPeriodId(invoice.getFiscalPeriodId());
        journalEntry.setCreatedBy("FIN");
        journalEntry.setCreatedAt(LocalDateTime.now());

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

        // Create debit line (Accounts Receivable)
        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setJournalEntry(savedEntry);
        debitLine.setAccountId(invoice.getDebitAccountId());
        debitLine.setDescription(invoice.getDescription());
        debitLine.setDebitAmount(invoice.getAmount());
        debitLine.setCreditAmount(BigDecimal.ZERO);
        debitLine.setReference("INV-" + invoice.getInvoiceNumber() + "-DR");
        journalEntryLineRepository.save(debitLine);

        // Create credit line (Sales Revenue)
        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setJournalEntry(savedEntry);
        creditLine.setAccountId(invoice.getCreditAccountId());
        creditLine.setDescription(invoice.getDescription());
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(invoice.getAmount());
        creditLine.setReference("INV-" + invoice.getInvoiceNumber() + "-CR");
        journalEntryLineRepository.save(creditLine);
    }

    /**
     * Syncs journal entries for invoices that don't have them yet.
     */
    @Transactional
    public int syncInvoiceJournalEntries(Long companyId) {
        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        List<ManualInvoice> invoicesWithoutJournalEntries = manualInvoiceRepository
            .findInvoicesWithoutJournalEntries(companyId);

        int syncedCount = 0;

        for (ManualInvoice invoice : invoicesWithoutJournalEntries) {
            try {
                createInvoiceJournalEntry(invoice);
                syncedCount++;
                LOGGER.info("Synced journal entry for invoice: " + invoice.getInvoiceNumber());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to sync invoice " + invoice.getInvoiceNumber() + ", skipping", e);
            }
        }

        return syncedCount;
    }

    /**
     * Creates a journal entry with multiple lines. All lines must balance (debits = credits).
     */
    @Transactional
    public JournalEntry createJournalEntry(Long companyId, String entryNumber, LocalDate entryDate,
                                         String description, Long fiscalPeriodId,
                                         List<JournalEntryLine> lines) {
        // Validate inputs
        if (companyId == null || entryNumber == null || entryDate == null ||
            description == null || fiscalPeriodId == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("All journal entry parameters are required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Validate all accounts in lines belong to company
        for (JournalEntryLine line : lines) {
            Account account = accountService.getAccountById(line.getAccountId()).orElse(null);
            if (account == null || !account.getCompanyId().equals(companyId)) {
                throw new IllegalArgumentException("Invalid account in journal entry: " + line.getAccountId());
            }
        }

        // Validate that debits equal credits
        BigDecimal totalDebits = lines.stream()
            .map(line -> line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = lines.stream()
            .map(line -> line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalArgumentException("Journal entry must balance. " +
                "Debits: " + totalDebits + ", Credits: " + totalCredits);
        }

        // Create journal entry header
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setCompanyId(companyId);
        journalEntry.setReference(entryNumber);
        journalEntry.setEntryDate(entryDate);
        journalEntry.setDescription(description);
        journalEntry.setFiscalPeriodId(fiscalPeriodId);
        journalEntry.setCreatedBy("FIN");
        journalEntry.setCreatedAt(LocalDateTime.now());

        JournalEntry savedEntry = journalEntryRepository.save(journalEntry);

        // Create all the lines
        for (JournalEntryLine line : lines) {
            line.setJournalEntry(savedEntry);
            line.setReference("JE-" + savedEntry.getId() + "-L" + System.currentTimeMillis());
            journalEntryLineRepository.save(line);
        }

        return savedEntry;
    }

    /**
     * Records a correction to a transaction's categorization.
     */
    @Transactional
    public DataCorrection correctTransactionCategory(Long companyId, Long transactionId,
                                                   Long originalAccountId, Long newAccountId,
                                                   String reason, String correctedBy) {
        // Validate inputs
        if (companyId == null || transactionId == null || originalAccountId == null ||
            newAccountId == null || reason == null || correctedBy == null) {
            throw new IllegalArgumentException("All correction parameters are required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Validate accounts exist and belong to company
        Account originalAccount = accountService.getAccountById(originalAccountId).orElse(null);
        if (originalAccount == null || !originalAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Invalid original account: " + originalAccountId);
        }

        Account newAccount = accountService.getAccountById(newAccountId).orElse(null);
        if (newAccount == null || !newAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Invalid new account: " + newAccountId);
        }

        // Get the transaction
        Optional<BankTransaction> transactionOpt = bankTransactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found: " + transactionId);
        }

        BankTransaction transaction = transactionOpt.get();
        if (!transaction.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Transaction does not belong to company: " + transactionId);
        }

        // Record the correction
        DataCorrection correction = new DataCorrection();
        correction.setCompanyId(companyId);
        correction.setTransactionId(transactionId);
        correction.setOriginalAccountId(originalAccountId);
        correction.setNewAccountId(newAccountId);
        correction.setCorrectionReason(reason);
        correction.setCorrectedBy(correctedBy);
        correction.setCorrectedAt(LocalDateTime.now());

        DataCorrection savedCorrection = dataCorrectionRepository.save(correction);

        // Update the transaction
        transaction.setAccountId(newAccountId);
        bankTransactionRepository.save(transaction);

        LOGGER.info("Transaction correction recorded: " + transactionId + " by " + correctedBy);
        return savedCorrection;
    }

    /**
     * Gets correction history for a transaction.
     */
    @Transactional(readOnly = true)
    public List<DataCorrection> getTransactionCorrectionHistory(Long transactionId) {
        return dataCorrectionRepository.findByTransactionIdOrderByCorrectedAtDesc(transactionId);
    }

    /**
     * Gets all manual invoices for a company
     */
    @Transactional(readOnly = true)
    public List<ManualInvoice> getManualInvoicesByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return manualInvoiceRepository.findByCompanyId(companyId);
    }

    /**
     * Gets a manual invoice by ID
     */
    @Transactional(readOnly = true)
    public Optional<ManualInvoice> getManualInvoiceById(Long id) {
        return manualInvoiceRepository.findById(id);
    }

    /**
     * Gets journal entries for a company
     */
    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return journalEntryRepository.findByCompanyId(companyId);
    }

    /**
     * Gets journal entries for a fiscal period
     */
    @Transactional(readOnly = true)
    public List<JournalEntry> getJournalEntriesByFiscalPeriod(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }
        return journalEntryRepository.findByFiscalPeriodId(fiscalPeriodId);
    }

    /**
     * Validates data integrity for a company
     */
    @Transactional(readOnly = true)
    public DataIntegrityReport validateDataIntegrity(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        DataIntegrityReport report = new DataIntegrityReport();
        report.setCompanyId(companyId);

        // Check journal entries balance
        List<JournalEntry> journalEntries = journalEntryRepository.findByCompanyId(companyId);
        int unbalancedEntries = 0;

        for (JournalEntry je : journalEntries) {
            List<JournalEntryLine> lines = journalEntryLineRepository.findByJournalEntryId(je.getId());

            BigDecimal totalDebits = lines.stream()
                .map(line -> line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredits = lines.stream()
                .map(line -> line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalDebits.compareTo(totalCredits) != 0) {
                unbalancedEntries++;
            }
        }

        report.setUnbalancedJournalEntries(unbalancedEntries);

        // Check for transactions without accounts
        long transactionsWithoutAccounts = bankTransactionRepository
            .findByCompanyIdAndBankAccountIdIsNull(companyId).size();
        report.setTransactionsWithoutAccounts((int) transactionsWithoutAccounts);

        // Check for orphaned journal entry lines
        // This would require a custom query, but for now we'll set it to 0
        report.setOrphanedJournalEntryLines(0);

        report.setTotalIssues(unbalancedEntries + (int) transactionsWithoutAccounts);

        return report;
    }

    /**
     * Inner class for data integrity reports
     */
    public static class DataIntegrityReport {
        private Long companyId;
        private int unbalancedJournalEntries;
        private int transactionsWithoutAccounts;
        private int orphanedJournalEntryLines;
        private int totalIssues;

        // Getters and setters
        public Long getCompanyId() { return companyId; }
        public void setCompanyId(Long companyId) { this.companyId = companyId; }

        public int getUnbalancedJournalEntries() { return unbalancedJournalEntries; }
        public void setUnbalancedJournalEntries(int unbalancedJournalEntries) { this.unbalancedJournalEntries = unbalancedJournalEntries; }

        public int getTransactionsWithoutAccounts() { return transactionsWithoutAccounts; }
        public void setTransactionsWithoutAccounts(int transactionsWithoutAccounts) { this.transactionsWithoutAccounts = transactionsWithoutAccounts; }

        public int getOrphanedJournalEntryLines() { return orphanedJournalEntryLines; }
        public void setOrphanedJournalEntryLines(int orphanedJournalEntryLines) { this.orphanedJournalEntryLines = orphanedJournalEntryLines; }

        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
    }
}