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

package fin.dto;

import fin.entity.BankTransaction;
import fin.service.upload.BankStatementProcessingService;

import java.util.List;

/**
 * Enhanced API response for bank statement upload processing.
 * 
 * <p>Provides comprehensive feedback about the upload operation including:
 * <ul>
 *   <li>Success status and summary message</li>
 *   <li>Detailed processing counts (valid, duplicates, out-of-period, validation errors)</li>
 *   <li>List of saved transactions</li>
 *   <li>List of rejected transactions with reasons</li>
 *   <li>Error messages for troubleshooting</li>
 * </ul>
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
public class BankStatementUploadResponse {
    
    private final boolean success;
    private final String message;
    private final ProcessingSummary summary;
    private final List<BankTransaction> savedTransactions;
    private final List<RejectedTransaction> rejectedTransactions;
    private final List<String> errors;
    
    /**
     * Constructor for upload response.
     *
     * @param result Processing result from BankStatementProcessingService
     */
    public BankStatementUploadResponse(BankStatementProcessingService.StatementProcessingResult result) {
        this.success = result.getInvalidTransactions() == 0 
            || result.getValidTransactions() > 0;
        this.message = buildSummaryMessage(result);
        this.summary = new ProcessingSummary(
            result.getProcessedLines(),
            result.getValidTransactions(),
            result.getDuplicateTransactions(),
            result.getOutOfPeriodTransactions(),
            result.getInvalidTransactions()
        );
        this.savedTransactions = result.getTransactions();
        this.rejectedTransactions = result.getRejectedTransactions();
        this.errors = result.getErrors();
    }
    
    /**
     * Constructor for error response (when processing fails completely).
     *
     * @param errorMessage Error message describing the failure
     */
    public BankStatementUploadResponse(String errorMessage) {
        this.success = false;
        this.message = errorMessage;
        this.summary = new ProcessingSummary(0, 0, 0, 0, 0);
        this.savedTransactions = List.of();
        this.rejectedTransactions = List.of();
        this.errors = List.of(errorMessage);
    }
    
    /**
     * Build human-readable summary message.
     */
    private String buildSummaryMessage(BankStatementProcessingService.StatementProcessingResult result) {
        int total = result.getValidTransactions() 
            + result.getDuplicateTransactions() 
            + result.getOutOfPeriodTransactions() 
            + result.getInvalidTransactions();
        
        if (result.getValidTransactions() == 0) {
            return String.format("Upload failed: No valid transactions found. " +
                "%d duplicates, %d out-of-period, %d validation errors.",
                result.getDuplicateTransactions(),
                result.getOutOfPeriodTransactions(),
                result.getInvalidTransactions());
        }
        
        if (result.getDuplicateTransactions() == 0 
            && result.getOutOfPeriodTransactions() == 0 
            && result.getInvalidTransactions() == 0) {
            return String.format("Upload successful: All %d transactions saved.",
                result.getValidTransactions());
        }
        
        return String.format("Upload completed: %d of %d transactions saved. " +
            "Skipped: %d duplicates, %d out-of-period, %d validation errors.",
            result.getValidTransactions(),
            total,
            result.getDuplicateTransactions(),
            result.getOutOfPeriodTransactions(),
            result.getInvalidTransactions());
    }
    
    // Getters
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ProcessingSummary getSummary() {
        return summary;
    }
    
    public List<BankTransaction> getSavedTransactions() {
        return savedTransactions;
    }
    
    public List<RejectedTransaction> getRejectedTransactions() {
        return rejectedTransactions;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Processing summary counts.
     */
    public static class ProcessingSummary {
        private final int totalLinesProcessed;
        private final int validTransactions;
        private final int duplicateTransactions;
        private final int outOfPeriodTransactions;
        private final int validationErrors;
        
        public ProcessingSummary(int totalLinesProcessed, int validTransactions,
                               int duplicateTransactions, int outOfPeriodTransactions,
                               int validationErrors) {
            this.totalLinesProcessed = totalLinesProcessed;
            this.validTransactions = validTransactions;
            this.duplicateTransactions = duplicateTransactions;
            this.outOfPeriodTransactions = outOfPeriodTransactions;
            this.validationErrors = validationErrors;
        }
        
        public int getTotalLinesProcessed() {
            return totalLinesProcessed;
        }
        
        public int getValidTransactions() {
            return validTransactions;
        }
        
        public int getDuplicateTransactions() {
            return duplicateTransactions;
        }
        
        public int getOutOfPeriodTransactions() {
            return outOfPeriodTransactions;
        }
        
        public int getValidationErrors() {
            return validationErrors;
        }
        
        public int getTotalRejected() {
            return duplicateTransactions + outOfPeriodTransactions + validationErrors;
        }
    }
}
