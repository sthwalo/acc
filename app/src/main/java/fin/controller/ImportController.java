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

package fin.controller;

import fin.entity.BankTransaction;
import fin.dto.BankStatementUploadResponse;
import fin.service.upload.BankStatementProcessingService;
import fin.service.classification.TransactionClassificationService;
import fin.service.classification.engine.TransactionClassificationEngine.BatchClassificationResult;
import fin.service.classification.rules.TransactionMappingRuleService.ClassificationRule;
import fin.service.CompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Spring REST Controller for import operations (bank statements, transaction processing).
 */
@RestController
@RequestMapping("/api/v1/import")
public class ImportController {

    private final BankStatementProcessingService bankStatementService;
    private final TransactionClassificationService classificationService;
    private final CompanyService companyService;

    public ImportController(BankStatementProcessingService bankStatementService,
                                TransactionClassificationService classificationService,
                                CompanyService companyService) {
        this.bankStatementService = bankStatementService;
        this.classificationService = classificationService;
        this.companyService = companyService;
    }

    /**
     * Debug endpoint (POST) - extract text and metadata from an uploaded PDF file.
     * Intended for development use only.
     */
    @PostMapping("/debug/extract")
    public ResponseEntity<fin.dto.DocumentExtractionDebugResponse> debugExtractFromUpload(
            @RequestParam("file") MultipartFile file) {
        try {
            java.io.File tempFile = java.io.File.createTempFile("debug_extract_", ".pdf");
            file.transferTo(tempFile);
            fin.dto.DocumentExtractionDebugResponse resp = bankStatementService.debugExtractFromFile(tempFile);
            tempFile.delete();
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            fin.dto.DocumentExtractionDebugResponse resp = new fin.dto.DocumentExtractionDebugResponse(java.util.List.of(), null, null, null, null, java.util.List.of(e.getMessage()));
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Debug endpoint (GET) - extract text and metadata from a file path under input/std.
     * Only allows files inside the `input/std` directory for safety.
     */
    @GetMapping("/debug/extract")
    public ResponseEntity<fin.dto.DocumentExtractionDebugResponse> debugExtractFromPath(@RequestParam("path") String path) {
        try {
            // Basic safety check - only allow paths under input/std
            // Clean up relative path input to avoid duplicate 'input/std' prefixes when users pass full subpaths
            java.nio.file.Path baseDir = java.nio.file.Paths.get(System.getProperty("user.dir")).resolve("input").resolve("std").normalize();
            java.nio.file.Path requested;
            java.nio.file.Path candidate = java.nio.file.Path.of(path);
            if (candidate.isAbsolute()) {
                requested = candidate.normalize();
            } else {
                // Remove leading 'input/std' or 'input' if present to avoid duplication when resolving against baseDir
                String cleaned = path;
                if (cleaned.startsWith("input/std/") || cleaned.startsWith("input\\std\\")) {
                    cleaned = cleaned.substring("input/std/".length());
                } else if (cleaned.startsWith("input/") || cleaned.startsWith("input\\")) {
                    cleaned = cleaned.substring("input/".length());
                }
                requested = baseDir.resolve(cleaned).normalize();
            }
            if (!requested.startsWith(baseDir)) {
                return ResponseEntity.badRequest().body(new fin.dto.DocumentExtractionDebugResponse(java.util.List.of(), null, null, null, null, java.util.List.of("Only paths under input/std are allowed")));
            }
            java.io.File f = requested.toFile();
            if (!f.exists()) {
                return ResponseEntity.badRequest().body(new fin.dto.DocumentExtractionDebugResponse(java.util.List.of(), null, null, null, null, java.util.List.of("File not found: " + requested.toString())));
            }
            fin.dto.DocumentExtractionDebugResponse resp = bankStatementService.debugExtractFromFile(f);
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            fin.dto.DocumentExtractionDebugResponse resp = new fin.dto.DocumentExtractionDebugResponse(java.util.List.of(), null, null, null, null, java.util.List.of(e.getMessage()));
            return ResponseEntity.internalServerError().body(resp);
        }
    }

    /**
     * Upload and process a bank statement PDF
     */
    @PostMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/imports/bank-statement")
    public ResponseEntity<BankStatementUploadResponse> processBankStatement(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "debug", required = false, defaultValue = "false") boolean debug) {
        try {
            BankStatementProcessingService.StatementProcessingResult result =
                bankStatementService.processStatement(file, companyId, fiscalPeriodId);
            
            // Wrap result in enhanced response with detailed feedback
            BankStatementUploadResponse response = new BankStatementUploadResponse(result, debug);
            
            // Return appropriate HTTP status based on success
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                // Partial success or complete failure
                return ResponseEntity.status(207).body(response); // 207 Multi-Status
            }
        } catch (IllegalArgumentException e) {
            // Return structured error response for bad requests
            BankStatementUploadResponse errorResponse = new BankStatementUploadResponse(
                "Invalid request: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (fin.exception.PdfProcessingException e) {
            // PDF-specific processing failure - client should be informed and may retry with different file
            BankStatementUploadResponse errorResponse = new BankStatementUploadResponse(
                "PDF processing failed: " + e.getMessage()
            );
            return ResponseEntity.status(422).body(errorResponse);
        } catch (Exception e) {
            // Return structured error response for server errors
            BankStatementUploadResponse errorResponse = new BankStatementUploadResponse(
                "Failed to process bank statement: " + e.getMessage()
            );
            return ResponseEntity.internalServerError().body(errorResponse);
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
            BankTransaction classifiedTransaction = classificationService.classifyTransactionWithAccountId(transactionId, accountId);
            return ResponseEntity.ok(classifiedTransaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Auto-classify transactions for a company
     */
    @PostMapping("/companies/{companyId}/transactions/auto-classify")
    public ResponseEntity<BatchClassificationResult> autoClassifyTransactions(
            @PathVariable Long companyId) {
        try {
            BatchClassificationResult result =
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
    public ResponseEntity<List<ClassificationRule>> getClassificationRules(
            @PathVariable Long companyId) {
        try {
            List<ClassificationRule> rules =
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
    public ResponseEntity<ClassificationRule> addClassificationRule(
            @RequestBody ClassificationRule rule) {
        try {
            ClassificationRule createdRule =
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
    public ResponseEntity<ClassificationRule> updateClassificationRule(
            @PathVariable Long id,
            @RequestBody ClassificationRule rule) {
        try {
            ClassificationRule updatedRule =
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
}