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

package fin.validation;

import fin.model.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validator for BankTransaction entities
 */
public class BankTransactionValidator implements ModelValidator<BankTransaction> {
    @Override
    public ValidationResult validate(BankTransaction transaction) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (transaction.getCompanyId() == null) {
            result.addError("companyId", "Company ID is required");
        }

        if (transaction.getFiscalPeriodId() == null) {
            result.addError("fiscalPeriodId", "Fiscal Period ID is required");
        }

        if (transaction.getTransactionDate() == null) {
            result.addError("transactionDate", "Transaction date is required");
        } else if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            result.addError("transactionDate", "Transaction date cannot be in the future");
        }

        if (transaction.getDetails() == null || transaction.getDetails().trim().isEmpty()) {
            result.addError("details", "Transaction details are required");
        }

        // Amount validation
        if (transaction.getDebitAmount() != null && transaction.getCreditAmount() != null) {
            if (transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 
                && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                result.addError("amount", "Transaction cannot have both debit and credit amounts");
            }
        }

        if (transaction.getDebitAmount() == null && transaction.getCreditAmount() == null) {
            result.addError("amount", "Either debit or credit amount must be specified");
        }

        // Amount sign validation
        if (transaction.getDebitAmount() != null 
            && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("debitAmount", "Debit amount cannot be negative");
        }

        if (transaction.getCreditAmount() != null 
            && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("creditAmount", "Credit amount cannot be negative");
        }

        // Balance validation - balance should be positive (running balance after transaction)
        if (transaction.getBalance() != null 
            && transaction.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            // Allow negative balances but warn
            // Some accounts can have overdrafts
        }

        return result;
    }
}
