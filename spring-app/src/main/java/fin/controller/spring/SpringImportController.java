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

import fin.model.BankTransaction;
import fin.service.spring.BankStatementProcessingService;
import fin.service.spring.SpringTransactionClassificationService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.List;

/**
 * Spring REST Controller for import operations (bank statements, transaction processing).
 */
@RestController
@RequestMapping("/api/v1/import")
public class SpringImportController {

    private final BankStatementProcessingService bankStatementService;
    private final SpringTransactionClassificationService classificationService;

    public SpringImportController(BankStatementProcessingService bankStatementService,
                                SpringTransactionClassificationService classificationService) {
        this.bankStatementService = bankStatementService;
        this.classificationService = classificationService;
    }

    /**
     * Upload and process a bank statement PDF
     */
    @PostMapping("/bank-statement")
    public ResponseEntity<BankStatementProcessingService.StatementProcessingResult> processBankStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") Long companyId,
            @RequestParam("fiscalPeriodId") Long fiscalPeriodId) {
        try {
            BankStatementProcessingService.StatementProcessingResult result =
                bankStatementService.processStatement(file, companyId, fiscalPeriodId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get transactions for a company and fiscal period
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions")
    public ResponseEntity<List<BankTransaction>> getTransactionsByCompanyAndPeriod(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period - handle null fiscalPeriodId values
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unclassified transactions for a company
     */
    @GetMapping("/companies/{companyId}/transactions/unclassified")
    public ResponseEntity<List<BankTransaction>> getUnclassifiedTransactions(@PathVariable Long companyId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getUnclassifiedTransactions(companyId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Classify a single transaction
     */
    @PostMapping("/transactions/{transactionId}/classify")
    public ResponseEntity<BankTransaction> classifyTransaction(@PathVariable Long transactionId,
                                                             @RequestParam Long accountId) {
        try {
            BankTransaction classifiedTransaction = classificationService.classifyTransaction(transactionId, accountId);
            return ResponseEntity.ok(classifiedTransaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Auto-classify transactions for a company
     */
    @PostMapping("/companies/{companyId}/transactions/auto-classify")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationResult> autoClassifyTransactions(
            @PathVariable Long companyId) {
        try {
            SpringTransactionClassificationService.ClassificationResult result =
                classificationService.autoClassifyTransactions(companyId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Initialize chart of accounts for a company
     */
    @PostMapping("/chart-of-accounts/company/{companyId}/initialize")
    public ResponseEntity<String> initializeChartOfAccounts(@PathVariable Long companyId) {
        try {
            classificationService.initializeChartOfAccounts(companyId);
            return ResponseEntity.ok("Chart of accounts initialized successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get classification rules for a company
     */
    @GetMapping("/classification-rules/company/{companyId}")
    public ResponseEntity<List<SpringTransactionClassificationService.ClassificationRule>> getClassificationRules(
            @PathVariable Long companyId) {
        try {
            List<SpringTransactionClassificationService.ClassificationRule> rules =
                classificationService.getClassificationRules(companyId);
            return ResponseEntity.ok(rules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a classification rule
     */
    @PostMapping("/classification-rules")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationRule> addClassificationRule(
            @RequestBody SpringTransactionClassificationService.ClassificationRule rule) {
        try {
            SpringTransactionClassificationService.ClassificationRule createdRule =
                classificationService.addClassificationRule(rule);
            return ResponseEntity.ok(createdRule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a classification rule
     */
    @PutMapping("/classification-rules/{id}")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationRule> updateClassificationRule(
            @PathVariable Long id,
            @RequestBody SpringTransactionClassificationService.ClassificationRule rule) {
        try {
            SpringTransactionClassificationService.ClassificationRule updatedRule =
                classificationService.updateClassificationRule(id, rule);
            return ResponseEntity.ok(updatedRule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a classification rule
     */
    @DeleteMapping("/classification-rules/{id}")
    public ResponseEntity<Void> deleteClassificationRule(@PathVariable Long id) {
        try {
            classificationService.deleteClassificationRule(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get transaction processing statistics
     */
    @GetMapping("/statistics/company/{companyId}")
    public ResponseEntity<BankStatementProcessingService.ProcessingStatistics> getProcessingStatistics(
            @PathVariable Long companyId) {
        try {
            BankStatementProcessingService.ProcessingStatistics stats =
                bankStatementService.getProcessingStatistics(companyId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export transactions to PDF
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions/export/pdf")
    public ResponseEntity<ByteArrayResource> exportTransactionsToPdf(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();

            byte[] pdfBytes = generateTransactionPdf(transactions, companyId, fiscalPeriodId);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions_" + companyId + "_" + fiscalPeriodId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple PDF generation for transactions (placeholder - would use PDFBox or similar)
     */
    private byte[] generateTransactionPdf(List<BankTransaction> transactions, Long companyId, Long fiscalPeriodId) throws IOException {
        // This is a placeholder implementation
        // In a real implementation, you would use Apache PDFBox or similar library
        // to generate a proper PDF with transaction data

        StringBuilder content = new StringBuilder();
        content.append("FINANCIAL TRANSACTIONS\n");
        content.append("Company ID: ").append(companyId).append("\n");
        content.append("Fiscal Period ID: ").append(fiscalPeriodId).append("\n");
        content.append("Total Transactions: ").append(transactions.size()).append("\n\n");

        content.append(String.format("%-12s %-50s %-15s %-15s %-20s\n",
                "Date", "Details", "Debit", "Credit", "Reference"));
        content.append("-".repeat(120)).append("\n");

        for (BankTransaction transaction : transactions) {
            String details = transaction.getDetails() != null ? transaction.getDetails() : "";
            content.append(String.format("%-12s %-50s %-15s %-15s %-20s\n",
                    transaction.getTransactionDate(),
                    details.length() > 47 ? details.substring(0, 47) + "..." : details,
                    transaction.getDebitAmount() != null ? transaction.getDebitAmount().toString() : "",
                    transaction.getCreditAmount() != null ? transaction.getCreditAmount().toString() : "",
                    transaction.getReference() != null ? transaction.getReference() : ""));
        }

        // For now, return the text content as bytes (not a real PDF)
        // TODO: Implement proper PDF generation using PDFBox
        return content.toString().getBytes();
    }
}