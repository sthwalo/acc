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

import fin.service.spring.SpringTransactionClassificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Spring REST Controller for transaction classification operations.
 * Provides endpoints for initializing chart of accounts, classifying transactions,
 * and managing the journal entry generation process.
 */
@RestController
@RequestMapping("/api/v1/companies/{companyId}/classification")
public class SpringTransactionClassificationController {

    private final SpringTransactionClassificationService classificationService;

    public SpringTransactionClassificationController(SpringTransactionClassificationService classificationService) {
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
    public ResponseEntity<String> initializeTransactionMappingRules(@PathVariable Long companyId) {
        try {
            boolean success = classificationService.initializeTransactionMappingRules(companyId);
            if (success) {
                return ResponseEntity.ok("Transaction mapping rules initialized successfully");
            } else {
                return ResponseEntity.internalServerError().body("Failed to initialize transaction mapping rules");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
    public ResponseEntity<String> autoClassifyTransactions(@PathVariable Long companyId) {
        try {
            SpringTransactionClassificationService.ClassificationResult result =
                classificationService.autoClassifyTransactions(companyId);
            return ResponseEntity.ok(result.getRuleApplied());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
     * Get uncategorized transactions for a company
     */
    @GetMapping("/uncategorized")
    public ResponseEntity<String> getUncategorizedTransactions(@PathVariable Long companyId) {
        try {
            String transactions = classificationService.getUncategorizedTransactions(companyId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
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
            return ResponseEntity.badRequest().body(
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
            @RequestBody ClassificationUpdateRequest request) {
        try {
            // Validate accounts belong to company
            classificationService.updateTransactionClassification(
                companyId,
                transactionId,
                request.getDebitAccountId(),
                request.getCreditAccountId()
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
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to update transaction classification: " + e.getMessage(),
                    ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
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
