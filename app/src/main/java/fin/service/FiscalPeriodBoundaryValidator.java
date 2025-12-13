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

package fin.service;

import fin.entity.BankTransaction;
import fin.entity.FiscalPeriod;
import fin.repository.FiscalPeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Service for validating that bank transactions fall within fiscal period boundaries.
 * 
 * <p>Ensures data integrity by rejecting transactions with dates outside their
 * assigned fiscal period's start and end dates. This prevents:
 * <ul>
 *   <li>Incorrect financial reporting (transactions in wrong period)</li>
 *   <li>Data inconsistencies (transaction_date outside period boundaries)</li>
 *   <li>Accounting errors (period-end calculations include wrong transactions)</li>
 * </ul>
 * 
 * <p>Validation checks:
 * <ul>
 *   <li>Transaction date >= fiscal period start date</li>
 *   <li>Transaction date <= fiscal period end date</li>
 *   <li>Fiscal period exists and is valid</li>
 * </ul>
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
@Service
public class FiscalPeriodBoundaryValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(FiscalPeriodBoundaryValidator.class);
    
    private final FiscalPeriodRepository fiscalPeriodRepository;
    
    /**
     * Constructor with dependency injection.
     *
     * @param fiscalPeriodRepository Repository for fiscal period database operations
     */
    public FiscalPeriodBoundaryValidator(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }
    
    /**
     * Validate that transaction date falls within fiscal period boundaries.
     * 
     * <p>Checks if the transaction's date is within the start and end dates
     * of its assigned fiscal period. Returns true if valid, false if out of bounds.
     * 
     * @param transaction Transaction to validate
     * @return true if transaction date is within fiscal period boundaries, false otherwise
     */
    public boolean isWithinFiscalPeriod(BankTransaction transaction) {
        // Validate input
        if (transaction == null) {
            logger.warn("isWithinFiscalPeriod called with null transaction");
            return false;
        }
        
        if (transaction.getFiscalPeriodId() == null) {
            logger.warn("Transaction has null fiscal period ID");
            return false;
        }
        
        if (transaction.getTransactionDate() == null) {
            logger.warn("Transaction has null transaction date");
            return false;
        }
        
        // Fetch fiscal period
        Optional<FiscalPeriod> fiscalPeriodOpt = fiscalPeriodRepository.findById(transaction.getFiscalPeriodId());
        
        if (fiscalPeriodOpt.isEmpty()) {
            logger.error("Fiscal period not found: id={}", transaction.getFiscalPeriodId());
            return false;
        }
        
        FiscalPeriod fiscalPeriod = fiscalPeriodOpt.get();
        LocalDate transactionDate = transaction.getTransactionDate();
        LocalDate periodStart = fiscalPeriod.getStartDate();
        LocalDate periodEnd = fiscalPeriod.getEndDate();
        
        // Check boundaries
        boolean isAfterStart = !transactionDate.isBefore(periodStart);  // >= start
        boolean isBeforeEnd = !transactionDate.isAfter(periodEnd);      // <= end
        boolean isValid = isAfterStart && isBeforeEnd;
        
        if (!isValid) {
            logger.debug("Transaction date out of fiscal period bounds: date={}, period=[{} to {}], fiscal_period_id={}", 
                transactionDate,
                periodStart,
                periodEnd,
                transaction.getFiscalPeriodId());
        }
        
        return isValid;
    }
    
    /**
     * Get detailed validation error message for out-of-period transaction.
     * 
     * <p>Provides specific error message explaining why transaction is invalid,
     * including the transaction date, fiscal period boundaries, and suggested action.
     * 
     * @param transaction Transaction that failed validation
     * @return Detailed error message, or null if transaction is valid
     */
    public String getValidationErrorMessage(BankTransaction transaction) {
        // Validate input
        if (transaction == null) {
            return "Transaction is null";
        }
        
        if (transaction.getFiscalPeriodId() == null) {
            return "Transaction has no fiscal period assigned";
        }
        
        if (transaction.getTransactionDate() == null) {
            return "Transaction has no date";
        }
        
        // Fetch fiscal period
        Optional<FiscalPeriod> fiscalPeriodOpt = fiscalPeriodRepository.findById(transaction.getFiscalPeriodId());
        
        if (fiscalPeriodOpt.isEmpty()) {
            return String.format(
                "Fiscal period not found (ID: %d). Please verify the fiscal period exists.",
                transaction.getFiscalPeriodId()
            );
        }
        
        FiscalPeriod fiscalPeriod = fiscalPeriodOpt.get();
        LocalDate transactionDate = transaction.getTransactionDate();
        LocalDate periodStart = fiscalPeriod.getStartDate();
        LocalDate periodEnd = fiscalPeriod.getEndDate();
        
        // Check if valid
        if (!transactionDate.isBefore(periodStart) && !transactionDate.isAfter(periodEnd)) {
            return null;  // Valid - no error message
        }
        
        // Build detailed error message
        if (transactionDate.isBefore(periodStart)) {
            return String.format(
                "Transaction date %s is before fiscal period start date %s (Period: %s, ID: %d). " +
                "Please select a fiscal period that includes this transaction date.",
                transactionDate,
                periodStart,
                fiscalPeriod.getPeriodName(),
                fiscalPeriod.getId()
            );
        } else {
            return String.format(
                "Transaction date %s is after fiscal period end date %s (Period: %s, ID: %d). " +
                "Please select a fiscal period that includes this transaction date.",
                transactionDate,
                periodEnd,
                fiscalPeriod.getPeriodName(),
                fiscalPeriod.getId()
            );
        }
    }
    
    /**
     * Check if transaction is valid and get error message in one call.
     * 
     * <p>Convenience method that combines validation check and error message retrieval.
     * Returns ValidationResult with isValid flag and optional error message.
     * 
     * @param transaction Transaction to validate
     * @return ValidationResult with isValid flag and optional error message
     */
    public ValidationResult validateTransaction(BankTransaction transaction) {
        boolean isValid = isWithinFiscalPeriod(transaction);
        String errorMessage = isValid ? null : getValidationErrorMessage(transaction);
        return new ValidationResult(isValid, errorMessage);
    }
    
    /**
     * Simple validation result container.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
