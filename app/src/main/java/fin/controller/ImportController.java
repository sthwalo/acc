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
     * Upload and process a bank statement PDF
     */
    @PostMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/imports/bank-statement")
    public ResponseEntity<BankStatementUploadResponse> processBankStatement(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId,
            @RequestParam("file") MultipartFile file) {
        try {
            BankStatementProcessingService.StatementProcessingResult result =
                bankStatementService.processStatement(file, companyId, fiscalPeriodId);
            
            // Wrap result in enhanced response with detailed feedback
            BankStatementUploadResponse response = new BankStatementUploadResponse(result);
            
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
    public ResponseEntity<TransactionClassificationService.ClassificationResult> autoClassifyTransactions(
            @PathVariable Long companyId) {
        try {
            TransactionClassificationService.ClassificationResult result =
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
    public ResponseEntity<List<TransactionClassificationService.ClassificationRule>> getClassificationRules(
            @PathVariable Long companyId) {
        try {
            List<TransactionClassificationService.ClassificationRule> rules =
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
    public ResponseEntity<TransactionClassificationService.ClassificationRule> addClassificationRule(
            @RequestBody TransactionClassificationService.ClassificationRule rule) {
        try {
            TransactionClassificationService.ClassificationRule createdRule =
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
    public ResponseEntity<TransactionClassificationService.ClassificationRule> updateClassificationRule(
            @PathVariable Long id,
            @RequestBody TransactionClassificationService.ClassificationRule rule) {
        try {
            TransactionClassificationService.ClassificationRule updatedRule =
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