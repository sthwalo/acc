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

import fin.service.classification.TransactionClassificationService;
import fin.service.classification.reporting.TransactionClassificationReportingService.UnclassifiedTransaction;
import fin.service.classification.engine.TransactionClassificationEngine.BatchClassificationResult;
import fin.exception.ErrorCode;
import fin.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Spring REST Controller for transaction classification operations.
 * Provides endpoints for initializing chart of accounts, classifying transactions,
 * and managing the journal entry generation process.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/classification")
public class TransactionClassificationController {

    private final TransactionClassificationService classificationService;

    public TransactionClassificationController(TransactionClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    /**
     * Initialize chart of accounts for a company
     */
    @PostMapping("/initialize-chart-of-accounts")
    public ResponseEntity<String> initializeChartOfAccounts(@PathVariable Long companyId) {
        try {
            boolean success = classificationService.initializeChartOfAccounts(companyId);
            if (success) {
                return ResponseEntity.ok("Chart of accounts initialized successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to initialize chart of accounts");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Initialize transaction mapping rules for a company
     */
    @PostMapping("/initialize-mapping-rules")
    public ResponseEntity<ApiResponse<String>> initializeTransactionMappingRules(@PathVariable Long companyId) {
        try {
            boolean success = classificationService.initializeTransactionMappingRules(companyId);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success(
                    "Transaction mapping rules initialized successfully",
                    "Rules have been created from industry templates"
                ));
            } else {
                return ResponseEntity.internalServerError().body(ApiResponse.error(
                    "Failed to initialize transaction mapping rules",
                    ErrorCode.CLASSIFICATION_RULE_NOT_FOUND.getCode()
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                "Invalid request",
                ErrorCode.VALIDATION_ERROR.getCode()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error(
                "Database error",
                ErrorCode.DATABASE_ERROR.getCode()
            ));
        }
    }

    /**
     * Perform full initialization (chart of accounts + mapping rules)
     */
    @PostMapping("/full-initialization")
    public ResponseEntity<String> performFullInitialization(@PathVariable Long companyId) {
        try {
            boolean success = classificationService.performFullInitialization(companyId);
            if (success) {
                return ResponseEntity.ok("Full initialization completed successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to complete full initialization");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Auto-classify unclassified transactions for a company
     */
    @PostMapping("/auto-classify")
    public ResponseEntity<BatchClassificationResult> autoClassifyTransactions(@PathVariable Long companyId) {
        try {
            BatchClassificationResult result =
                classificationService.autoClassifyTransactions(companyId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Sync journal entries for new transactions
     */
    @PostMapping("/sync-journal-entries")
    public ResponseEntity<String> syncJournalEntries(@PathVariable Long companyId) {
        try {
            int syncedCount = classificationService.syncJournalEntries(companyId);
            return ResponseEntity.ok("Synced " + syncedCount + " journal entries");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Regenerate all journal entries after reclassification
     */
    @PostMapping("/regenerate-journal-entries")
    public ResponseEntity<String> regenerateAllJournalEntries(@PathVariable Long companyId) {
        try {
            int regeneratedCount = classificationService.regenerateAllJournalEntries(companyId);
            return ResponseEntity.ok("Regenerated " + regeneratedCount + " journal entries");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get classification summary for a company
     */
    @GetMapping("/summary")
    public ResponseEntity<String> getClassificationSummary(@PathVariable Long companyId) {
        try {
            String summary = classificationService.getClassificationSummary(companyId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Get unclassified transactions for a specific fiscal period.
     * Used by the frontend TransactionClassificationReview component.
     */
    @GetMapping("/unclassified/{fiscalPeriodId}")
    public ResponseEntity<ApiResponse<List<UnclassifiedTransaction>>> getUnclassifiedTransactions(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<UnclassifiedTransaction> transactions =
                classificationService.getUnclassifiedTransactions(companyId, fiscalPeriodId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Unclassified transactions retrieved successfully",
                transactions
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request: " + e.getMessage(),
                    ErrorCode.VALIDATION_ERROR.getCode())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve unclassified transactions: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Get all transaction mapping rules.
     * Used by UI for displaying classification rules and suggestions.
     */
    @GetMapping("/mapping-rules")
    public ResponseEntity<ApiResponse<String>> getMappingRules(@PathVariable Long companyId) {
        try {
            String message = "Mapping rules endpoint - implementation pending";
            return ResponseEntity.ok(ApiResponse.success(
                "Mapping rules retrieved successfully",
                message
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to retrieve mapping rules: " + e.getMessage(), 
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Update transaction classification with manual account selection.
     * Creates or updates journal entry with selected debit and credit accounts.
     */
    @PutMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<String>> updateTransactionClassification(
            @PathVariable Long companyId,
            @PathVariable Long transactionId,
            @RequestBody ClassificationUpdateRequest request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "FIN";
            // Validate accounts belong to company
            classificationService.updateTransactionClassification(
                companyId,
                transactionId,
                request.getDebitAccountId(),
                request.getCreditAccountId(),
                username
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transaction classification updated successfully",
                "Journal entry created/updated for transaction " + transactionId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request: " + e.getMessage(),
                    ErrorCode.VALIDATION_ERROR.getCode())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to update transaction classification: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Create a new transaction mapping rule for a company.
     * Used by the frontend TransactionClassificationReview component.
     */
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<String>> createClassificationRule(
            @PathVariable Long companyId,
            @RequestBody CreateRuleRequest request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "FIN";
            
            classificationService.createTransactionMappingRule(
                companyId,
                request.getRuleName(),
                request.getMatchType(),
                request.getMatchValue(),
                request.getAccountCode(),
                request.getAccountName(),
                request.getPriority(),
                username
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Classification rule created successfully",
                "Rule '" + request.getRuleName() + "' created for company " + companyId
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request: " + e.getMessage(),
                    ErrorCode.VALIDATION_ERROR.getCode())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to create classification rule: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Classify a transaction with an account code.
     * Used by the frontend TransactionClassificationReview component for manual classification.
     */
    @PostMapping("/transactions/{transactionId}/classify")
    public ResponseEntity<ApiResponse<String>> classifyTransaction(
            @PathVariable Long companyId,
            @PathVariable Long transactionId,
            @RequestBody ClassifyTransactionRequest request,
            Principal principal) {
        try {
            String username = principal != null ? principal.getName() : "FIN";
            
            classificationService.classifyTransactionByAccountCode(
                companyId,
                transactionId,
                request.getAccountCode(),
                username
            );
            
            return ResponseEntity.ok(ApiResponse.success(
                "Transaction classified successfully",
                "Transaction " + transactionId + " classified with account " + request.getAccountCode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request: " + e.getMessage(),
                    ErrorCode.VALIDATION_ERROR.getCode())
            );
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ApiResponse.error("Failed to classify transaction: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Request body for creating a classification rule.
     */
    public static class CreateRuleRequest {
        private String ruleName;
        private String matchType;
        private String matchValue;
        private String accountCode;
        private String accountName;
        private Integer priority;

        public String getRuleName() { return ruleName; }
        public void setRuleName(String ruleName) { this.ruleName = ruleName; }

        public String getMatchType() { return matchType; }
        public void setMatchType(String matchType) { this.matchType = matchType; }

        public String getMatchValue() { return matchValue; }
        public void setMatchValue(String matchValue) { this.matchValue = matchValue; }

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
    }

    /**
     * Request body for classifying a transaction.
     */
    public static class ClassifyTransactionRequest {
        private String accountCode;

        public String getAccountCode() { return accountCode; }
        public void setAccountCode(String accountCode) { this.accountCode = accountCode; }
    }

    /**
     * Request body for classification update.
     */
    public static class ClassificationUpdateRequest {
        private Long debitAccountId;
        private Long creditAccountId;

        /**
         * Gets the debit account ID.
         *
         * @return the debit account ID
         */
        public Long getDebitAccountId() {
            return debitAccountId;
        }

        /**
         * Sets the debit account ID.
         *
         * @param debitAccountId the debit account ID to set
         */
        public void setDebitAccountId(Long debitAccountId) {
            this.debitAccountId = debitAccountId;
        }

        /**
         * Gets the credit account ID.
         *
         * @return the credit account ID
         */
        public Long getCreditAccountId() {
            return creditAccountId;
        }

        /**
         * Sets the credit account ID.
         *
         * @param creditAccountId the credit account ID to set
         */
        public void setCreditAccountId(Long creditAccountId) {
            this.creditAccountId = creditAccountId;
        }
    }
}
