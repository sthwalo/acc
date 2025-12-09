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

package fin.controller.spring;

import fin.entity.*;
import fin.service.reporting.SpringCsvExportService;
import fin.service.journal.SpringDataManagementService;
import fin.service.classification.SpringInteractiveClassificationService;
import fin.service.spring.SpringInvoicePdfService;
import fin.util.SpringDebugger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for data management operations.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class SpringDataManagementController {

    private final SpringDataManagementService dataManagementService;
    private final SpringInvoicePdfService invoicePdfService;
    private final SpringCsvExportService csvExportService;
    private final SpringInteractiveClassificationService classificationService;
    private final SpringDebugger debugger;

    public SpringDataManagementController(SpringDataManagementService dataManagementService,
                                        SpringInvoicePdfService invoicePdfService,
                                        SpringCsvExportService csvExportService,
                                        SpringInteractiveClassificationService classificationService,
                                        SpringDebugger debugger) {
        this.dataManagementService = dataManagementService;
        this.invoicePdfService = invoicePdfService;
        this.csvExportService = csvExportService;
        this.classificationService = classificationService;
        this.debugger = debugger;
    }

    /**
     * Reset company data
     */
    @PostMapping("/{companyId}/data-management/reset")
    public ResponseEntity<String> resetCompanyData(@PathVariable Long companyId,
                                                 @RequestParam(defaultValue = "true") boolean preserveMasterData) {
        debugger.logMethodEntry("SpringDataManagementController", "resetCompanyData", companyId, preserveMasterData);

        try {
            dataManagementService.resetCompanyData(companyId, preserveMasterData);
            debugger.logMethodExit("SpringDataManagementController", "resetCompanyData", "SUCCESS");
            return ResponseEntity.ok("Company data reset successfully");
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "resetCompanyData", "companyId", companyId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Company Data Reset", "SpringDataManagementController", "resetCompanyData", e, companyId, preserveMasterData);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Create a manual invoice
     */
    @PostMapping("/{companyId}/data-management/invoices")
    public ResponseEntity<ManualInvoice> createManualInvoice(@PathVariable Long companyId, @RequestBody ManualInvoice invoice, Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "FIN";
            ManualInvoice createdInvoice = dataManagementService.createManualInvoice(
                invoice.getCompanyId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDescription(),
                invoice.getAmount(),
                invoice.getDebitAccountId(),
                invoice.getCreditAccountId(),
                invoice.getFiscalPeriodId(),
                username
            );
            return ResponseEntity.ok(createdInvoice);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get manual invoice by ID
     */
    @GetMapping("/invoices/{id}")
    public ResponseEntity<ManualInvoice> getManualInvoiceById(@PathVariable Long id) {
        Optional<ManualInvoice> invoice = dataManagementService.getManualInvoiceById(id);
        return invoice.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all manual invoices for a company
     */
    @GetMapping("/{companyId}/invoices")
    public ResponseEntity<List<ManualInvoice>> getManualInvoicesByCompany(@PathVariable Long companyId) {
        try {
            List<ManualInvoice> invoices = dataManagementService.getManualInvoicesByCompany(companyId);
            return ResponseEntity.ok(invoices);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Sync invoice journal entries
     */
    @PostMapping("/{companyId}/data-management/sync-invoice-journal-entries")
    public ResponseEntity<String> syncInvoiceJournalEntries(@PathVariable Long companyId) {
        try {
            int syncedCount = dataManagementService.syncInvoiceJournalEntries(companyId);
            return ResponseEntity.ok("Synced " + syncedCount + " invoice journal entries");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a journal entry
     */
    @PostMapping("/{companyId}/data-management/journal-entries")
    public ResponseEntity<JournalEntry> createJournalEntry(@PathVariable Long companyId,
                                                         @RequestParam String entryNumber,
                                                         @RequestParam LocalDate entryDate,
                                                         @RequestParam String description,
                                                         @RequestParam Long fiscalPeriodId,
                                                         @RequestBody List<JournalEntryLine> lines,
                                                         Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "FIN";
            JournalEntry journalEntry = dataManagementService.createJournalEntry(
                companyId, entryNumber, entryDate, description, fiscalPeriodId, lines, username);
            return ResponseEntity.ok(journalEntry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get journal entries for a company
     */
    @GetMapping("/{companyId}/data-management/journal-entries")
    public ResponseEntity<List<JournalEntry>> getJournalEntriesByCompany(@PathVariable Long companyId) {
        try {
            List<JournalEntry> entries = dataManagementService.getJournalEntriesByCompany(companyId);
            return ResponseEntity.ok(entries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get journal entries for a fiscal period
     */
    @GetMapping("/{companyId}/data-management/journal-entries/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<JournalEntry>> getJournalEntriesByFiscalPeriod(@PathVariable Long companyId, @PathVariable Long fiscalPeriodId) {
        try {
            List<JournalEntry> entries = dataManagementService.getJournalEntriesByFiscalPeriod(fiscalPeriodId);
            return ResponseEntity.ok(entries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Correct transaction category
     */
    @PostMapping("/{companyId}/data-management/transactions/{transactionId}/correct-category")
    public ResponseEntity<DataCorrection> correctTransactionCategory(@PathVariable Long companyId, @PathVariable Long transactionId,
                                                                   @RequestParam Long originalAccountId,
                                                                   @RequestParam Long newAccountId,
                                                                   @RequestParam String reason,
                                                                   @RequestParam String correctedBy) {
        try {
            DataCorrection correction = dataManagementService.correctTransactionCategory(
                companyId, transactionId, originalAccountId, newAccountId, reason, correctedBy);
            return ResponseEntity.ok(correction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get transaction correction history
     */
    @GetMapping("/{companyId}/data-management/transactions/{transactionId}/correction-history")
    public ResponseEntity<List<DataCorrection>> getTransactionCorrectionHistory(@PathVariable Long companyId, @PathVariable Long transactionId) {
        try {
            List<DataCorrection> history = dataManagementService.getTransactionCorrectionHistory(transactionId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Validate data integrity for a company
     */
    @GetMapping("/{companyId}/data-management/integrity")
    public ResponseEntity<SpringDataManagementService.DataIntegrityReport> validateDataIntegrity(@PathVariable Long companyId) {
        debugger.logMethodEntry("SpringDataManagementController", "validateDataIntegrity", companyId);

        try {
            SpringDataManagementService.DataIntegrityReport report = dataManagementService.validateDataIntegrity(companyId);
            debugger.logMethodExit("SpringDataManagementController", "validateDataIntegrity", report);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "validateDataIntegrity", "companyId", companyId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Data Integrity Validation", "SpringDataManagementController", "validateDataIntegrity", e, companyId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate PDF for a manual invoice
     */
    @PostMapping("/{companyId}/data-management/invoices/{invoiceId}/generate-pdf")
    public ResponseEntity<String> generateInvoicePdf(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        debugger.logMethodEntry("SpringDataManagementController", "generateInvoicePdf", companyId, invoiceId);

        try {
            // Get company and fiscal period info
            Company company = dataManagementService.getCompanyById(companyId);
            if (company == null) {
                debugger.logValidationError("SpringDataManagementController", "generateInvoicePdf", "companyId", companyId.toString(), "Company not found");
                return ResponseEntity.notFound().build();
            }

            FiscalPeriod fiscalPeriod = dataManagementService.getCurrentFiscalPeriod(companyId);
            if (fiscalPeriod == null) {
                debugger.logValidationError("SpringDataManagementController", "generateInvoicePdf", "companyId", companyId.toString(), "No fiscal period found");
                return ResponseEntity.badRequest().build();
            }

            invoicePdfService.generateInvoicePdfBytes(invoiceId, company, fiscalPeriod);
            debugger.logMethodExit("SpringDataManagementController", "generateInvoicePdf", "PDF generated successfully");
            return ResponseEntity.ok("PDF generated successfully");
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "generateInvoicePdf", "invoiceId", invoiceId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Invoice PDF Generation", "SpringDataManagementController", "generateInvoicePdf", e, companyId, invoiceId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download PDF for a manual invoice
     */
    @GetMapping("/{companyId}/data-management/invoices/{invoiceId}/pdf")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Long companyId, @PathVariable Long invoiceId) {
        debugger.logMethodEntry("SpringDataManagementController", "downloadInvoicePdf", companyId, invoiceId);

        try {
            // Get company and fiscal period info
            Company company = dataManagementService.getCompanyById(companyId);
            if (company == null) {
                debugger.logValidationError("SpringDataManagementController", "downloadInvoicePdf", "companyId", companyId.toString(), "Company not found");
                return ResponseEntity.notFound().build();
            }

            FiscalPeriod fiscalPeriod = dataManagementService.getCurrentFiscalPeriod(companyId);
            if (fiscalPeriod == null) {
                debugger.logValidationError("SpringDataManagementController", "downloadInvoicePdf", "companyId", companyId.toString(), "No fiscal period found");
                return ResponseEntity.badRequest().build();
            }

            byte[] pdfBytes = invoicePdfService.generateInvoicePdfBytes(invoiceId, company, fiscalPeriod);
            debugger.logMethodExit("SpringDataManagementController", "downloadInvoicePdf", "PDF generated successfully");

            return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=\"invoice-" + invoiceId + ".pdf\"")
                .body(pdfBytes);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "downloadInvoicePdf", "invoiceId", invoiceId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Invoice PDF Download", "SpringDataManagementController", "downloadInvoicePdf", e, companyId, invoiceId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export transactions to CSV
     */
    @PostMapping("/{companyId}/data-management/export-csv")
    public ResponseEntity<byte[]> exportTransactionsToCsv(@PathVariable Long companyId, @RequestParam Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringDataManagementController", "exportTransactionsToCsv", companyId, fiscalPeriodId);

        try {
            byte[] csvBytes = csvExportService.exportTransactionsToCsvBytes(companyId, fiscalPeriodId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "transactions_" + companyId + "_" + fiscalPeriodId + ".csv");

            debugger.logMethodExit("SpringDataManagementController", "exportTransactionsToCsv", "CSV exported successfully");
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBytes);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "exportTransactionsToCsv", "fiscalPeriodId", fiscalPeriodId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("CSV Export", "SpringDataManagementController", "exportTransactionsToCsv", e, companyId, fiscalPeriodId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Download transactions as CSV file
     */
    @GetMapping("/{companyId}/data-management/csv-export")
    public ResponseEntity<byte[]> downloadTransactionsCsv(@PathVariable Long companyId, @RequestParam Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringDataManagementController", "downloadTransactionsCsv", companyId, fiscalPeriodId);

        try {
            byte[] csvBytes = csvExportService.exportTransactionsToCsvBytes(companyId, fiscalPeriodId);
            debugger.logMethodExit("SpringDataManagementController", "downloadTransactionsCsv", "CSV generated successfully");

            return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=\"transactions-" + companyId + "-" + fiscalPeriodId + ".csv\"")
                .body(csvBytes);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "downloadTransactionsCsv", "fiscalPeriodId", fiscalPeriodId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("CSV Download", "SpringDataManagementController", "downloadTransactionsCsv", e, companyId, fiscalPeriodId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get uncategorized transactions for interactive classification
     */
    @GetMapping("/{companyId}/data-management/classification/uncategorized")
    public ResponseEntity<List<BankTransaction>> getUncategorizedTransactions(@PathVariable Long companyId, @RequestParam Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringDataManagementController", "getUncategorizedTransactions", companyId, fiscalPeriodId);

        try {
            List<BankTransaction> transactions = classificationService.getUncategorizedTransactions(companyId, fiscalPeriodId);
            debugger.logMethodExit("SpringDataManagementController", "getUncategorizedTransactions",
                String.format("Found %d uncategorized transactions", transactions.size()));
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "getUncategorizedTransactions", "fiscalPeriodId", fiscalPeriodId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Get Uncategorized Transactions", "SpringDataManagementController", "getUncategorizedTransactions", e, companyId, fiscalPeriodId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get categorized transactions
     */
    @GetMapping("/{companyId}/data-management/classification/categorized")
    public ResponseEntity<List<BankTransaction>> getCategorizedTransactions(@PathVariable Long companyId, @RequestParam Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringDataManagementController", "getCategorizedTransactions", companyId, fiscalPeriodId);

        try {
            List<BankTransaction> transactions = classificationService.getCategorizedTransactions(companyId, fiscalPeriodId);
            debugger.logMethodExit("SpringDataManagementController", "getCategorizedTransactions",
                String.format("Found %d categorized transactions", transactions.size()));
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "getCategorizedTransactions", "fiscalPeriodId", fiscalPeriodId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Get Categorized Transactions", "SpringDataManagementController", "getCategorizedTransactions", e, companyId, fiscalPeriodId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Classify a single transaction
     */
    @PostMapping("/{companyId}/data-management/classification/transactions/{transactionId}")
    public ResponseEntity<BankTransaction> classifyTransaction(@PathVariable Long companyId, @PathVariable Long transactionId,
                                                             @RequestParam String accountCode,
                                                             @RequestParam String accountName,
                                                             @RequestParam String classifiedBy) {
        debugger.logMethodEntry("SpringDataManagementController", "classifyTransaction", transactionId, accountCode, accountName, classifiedBy);

        try {
            BankTransaction classified = classificationService.classifyTransaction(transactionId, accountCode, accountName, classifiedBy);
            debugger.logMethodExit("SpringDataManagementController", "classifyTransaction", classified.getId());
            return ResponseEntity.ok(classified);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "classifyTransaction", "transactionId", transactionId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Transaction Classification", "SpringDataManagementController", "classifyTransaction", e, transactionId, accountCode, accountName);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get account suggestions for a transaction
     */
    @GetMapping("/{companyId}/data-management/classification/suggestions")
    public ResponseEntity<List<SpringInteractiveClassificationService.AccountSuggestion>> getAccountSuggestions(
            @PathVariable Long companyId, @RequestParam String transactionDescription) {
        debugger.logMethodEntry("SpringDataManagementController", "getAccountSuggestions", companyId, transactionDescription);

        try {
            List<SpringInteractiveClassificationService.AccountSuggestion> suggestions =
                classificationService.getAccountSuggestions(companyId, transactionDescription);
            debugger.logMethodExit("SpringDataManagementController", "getAccountSuggestions",
                String.format("Found %d suggestions", suggestions.size()));
            return ResponseEntity.ok(suggestions);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "getAccountSuggestions", "transactionDescription", transactionDescription, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Get Account Suggestions", "SpringDataManagementController", "getAccountSuggestions", e, companyId, transactionDescription);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get classification summary
     */
    @GetMapping("/{companyId}/data-management/classification/summary")
    public ResponseEntity<SpringInteractiveClassificationService.ClassificationSummary> getClassificationSummary(
            @PathVariable Long companyId, @RequestParam Long fiscalPeriodId) {
        debugger.logMethodEntry("SpringDataManagementController", "getClassificationSummary", companyId, fiscalPeriodId);

        try {
            SpringInteractiveClassificationService.ClassificationSummary summary =
                classificationService.getClassificationSummary(companyId, fiscalPeriodId);
            debugger.logMethodExit("SpringDataManagementController", "getClassificationSummary", summary);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "getClassificationSummary", "fiscalPeriodId", fiscalPeriodId.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Get Classification Summary", "SpringDataManagementController", "getClassificationSummary", e, companyId, fiscalPeriodId);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Batch classify multiple transactions
     */
    @PostMapping("/{companyId}/data-management/classification/batch")
    public ResponseEntity<String> batchClassifyTransactions(@PathVariable Long companyId, @RequestBody BatchClassificationRequest request) {
        debugger.logMethodEntry("SpringDataManagementController", "batchClassifyTransactions",
            request.transactionIds.size(), request.accountCode, request.accountName, request.classifiedBy);

        try {
            int classified = classificationService.batchClassifyTransactions(
                request.transactionIds, request.accountCode, request.accountName, request.classifiedBy);
            debugger.logMethodExit("SpringDataManagementController", "batchClassifyTransactions",
                String.format("Classified %d transactions", classified));
            return ResponseEntity.ok(String.format("Successfully classified %d out of %d transactions",
                classified, request.transactionIds.size()));
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "batchClassifyTransactions",
                "transactionIds", request.transactionIds.toString(), e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Batch Classification", "SpringDataManagementController", "batchClassifyTransactions", e,
                request.transactionIds, request.accountCode, request.accountName);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Find similar uncategorized transactions
     */
    @GetMapping("/{companyId}/data-management/classification/similar")
    public ResponseEntity<List<BankTransaction>> findSimilarTransactions(@PathVariable Long companyId,
                                                                       @RequestParam Long fiscalPeriodId,
                                                                       @RequestParam String pattern) {
        debugger.logMethodEntry("SpringDataManagementController", "findSimilarTransactions", companyId, fiscalPeriodId, pattern);

        try {
            List<BankTransaction> similar = classificationService.findSimilarTransactions(companyId, fiscalPeriodId, pattern);
            debugger.logMethodExit("SpringDataManagementController", "findSimilarTransactions",
                String.format("Found %d similar transactions", similar.size()));
            return ResponseEntity.ok(similar);
        } catch (IllegalArgumentException e) {
            debugger.logValidationError("SpringDataManagementController", "findSimilarTransactions", "pattern", pattern, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            debugger.logException("Find Similar Transactions", "SpringDataManagementController", "findSimilarTransactions", e, companyId, fiscalPeriodId, pattern);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * DTO for batch classification requests
     */
    public static class BatchClassificationRequest {
        public List<Long> transactionIds;
        public String accountCode;
        public String accountName;
        public String classifiedBy;
    }
}