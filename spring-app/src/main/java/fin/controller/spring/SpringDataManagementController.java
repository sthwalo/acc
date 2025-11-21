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

import fin.model.*;
import fin.service.spring.SpringDataManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for data management operations.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/data-management")
public class SpringDataManagementController {

    private final SpringDataManagementService dataManagementService;

    public SpringDataManagementController(SpringDataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }

    /**
     * Reset company data
     */
    @PostMapping("/reset")
    public ResponseEntity<String> resetCompanyData(@PathVariable Long companyId,
                                                 @RequestParam(defaultValue = "true") boolean preserveMasterData) {
        try {
            dataManagementService.resetCompanyData(companyId, preserveMasterData);
            return ResponseEntity.ok("Company data reset successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a manual invoice
     */
    @PostMapping("/invoices")
    public ResponseEntity<ManualInvoice> createManualInvoice(@RequestBody ManualInvoice invoice) {
        try {
            ManualInvoice createdInvoice = dataManagementService.createManualInvoice(
                invoice.getCompanyId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceDate(),
                invoice.getDescription(),
                invoice.getAmount(),
                invoice.getDebitAccountId(),
                invoice.getCreditAccountId(),
                invoice.getFiscalPeriodId()
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
    @GetMapping("/invoices")
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
    @PostMapping("/sync-invoice-journal-entries")
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
    @PostMapping("/journal-entries")
    public ResponseEntity<JournalEntry> createJournalEntry(@RequestParam Long companyId,
                                                         @RequestParam String entryNumber,
                                                         @RequestParam LocalDate entryDate,
                                                         @RequestParam String description,
                                                         @RequestParam Long fiscalPeriodId,
                                                         @RequestBody List<JournalEntryLine> lines) {
        try {
            JournalEntry journalEntry = dataManagementService.createJournalEntry(
                companyId, entryNumber, entryDate, description, fiscalPeriodId, lines);
            return ResponseEntity.ok(journalEntry);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get journal entries for a company
     */
    @GetMapping("/journal-entries")
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
    @GetMapping("/journal-entries/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<JournalEntry>> getJournalEntriesByFiscalPeriod(@PathVariable Long fiscalPeriodId) {
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
    @PostMapping("/transactions/{transactionId}/correct-category")
    public ResponseEntity<DataCorrection> correctTransactionCategory(@PathVariable Long transactionId,
                                                                   @RequestParam Long companyId,
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
    @GetMapping("/transactions/{transactionId}/correction-history")
    public ResponseEntity<List<DataCorrection>> getTransactionCorrectionHistory(@PathVariable Long transactionId) {
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
    @GetMapping("/integrity")
    public ResponseEntity<SpringDataManagementService.DataIntegrityReport> validateDataIntegrity(@PathVariable Long companyId) {
        try {
            SpringDataManagementService.DataIntegrityReport report = dataManagementService.validateDataIntegrity(companyId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}