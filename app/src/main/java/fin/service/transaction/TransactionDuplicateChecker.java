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

package fin.service.transaction;

import fin.entity.BankTransaction;
import fin.repository.BankTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Service for detecting duplicate bank transactions during upload.
 * 
 * <p>Prevents duplicate data from being saved when the same bank statement 
 * is uploaded multiple times. Uses 5-field matching criteria:
 * <ul>
 *   <li>company_id - same company</li>
 *   <li>transaction_date - exact date match</li>
 *   <li>debit_amount + credit_amount - exact amount match</li>
 *   <li>description - case-insensitive text match</li>
 *   <li>balance - running balance after transaction</li>
 * </ul>
 * 
 * <p>All 5 fields must match for a transaction to be considered a duplicate.
 * This ensures high accuracy while avoiding false positives.
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
@Service
public class TransactionDuplicateChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionDuplicateChecker.class);
    
    private final BankTransactionRepository transactionRepository;
    
    /**
     * Constructor with dependency injection.
     *
     * @param transactionRepository Repository for bank transaction database operations
     */
    public TransactionDuplicateChecker(BankTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }
    
    /**
     * Check if transaction already exists in database.
     * 
     * <p>Uses 5-field matching to determine if a transaction is a duplicate:
     * <ul>
     *   <li>Same company</li>
     *   <li>Same transaction date</li>
     *   <li>Same debit and credit amounts</li>
     *   <li>Same description (case-insensitive)</li>
     *   <li>Same balance after transaction</li>
     * </ul>
     * 
     * @param transaction Transaction to check for duplicates
     * @return true if duplicate found, false otherwise
     */
    public boolean isDuplicate(BankTransaction transaction) {
        // Validate input
        if (transaction == null) {
            logger.warn("isDuplicate called with null transaction");
            return false;
        }
        
        // Handle null amounts (convert to zero for matching)
        BigDecimal debitAmount = transaction.getDebitAmount() != null 
            ? transaction.getDebitAmount() 
            : BigDecimal.ZERO;
        
        BigDecimal creditAmount = transaction.getCreditAmount() != null 
            ? transaction.getCreditAmount() 
            : BigDecimal.ZERO;
        
        boolean exists = transactionRepository.existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            transaction.getCompanyId(),
            transaction.getTransactionDate(),
            debitAmount,
            creditAmount,
            transaction.getDetails(),
            transaction.getBalance()
        );
        
        if (exists) {
            logger.debug("Duplicate transaction detected: date={}, amount={}/{}, description={}", 
                transaction.getTransactionDate(),
                debitAmount,
                creditAmount,
                transaction.getDetails() != null && transaction.getDetails().length() > 50 
                    ? transaction.getDetails().substring(0, 50) + "..." 
                    : transaction.getDetails());
        }
        
        return exists;
    }
    
    /**
     * Get existing duplicate transaction details for reporting.
     * 
     * <p>Retrieves the existing transaction from the database that matches
     * all 5 duplicate detection fields. Used to provide detailed information
     * in rejection reports (e.g., transaction ID, upload date).
     * 
     * @param transaction Transaction to find duplicate for
     * @return Existing transaction if found, null otherwise
     */
    public BankTransaction findDuplicate(BankTransaction transaction) {
        // Validate input
        if (transaction == null) {
            logger.warn("findDuplicate called with null transaction");
            return null;
        }
        
        // Handle null amounts (convert to zero for matching)
        BigDecimal debitAmount = transaction.getDebitAmount() != null 
            ? transaction.getDebitAmount() 
            : BigDecimal.ZERO;
        
        BigDecimal creditAmount = transaction.getCreditAmount() != null 
            ? transaction.getCreditAmount() 
            : BigDecimal.ZERO;
        
        BankTransaction duplicate = transactionRepository
            .findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
                transaction.getCompanyId(),
                transaction.getTransactionDate(),
                debitAmount,
                creditAmount,
                transaction.getDetails(),
                transaction.getBalance()
            )
            .orElse(null);
        
        if (duplicate != null) {
            logger.debug("Found duplicate transaction: id={}, created_at={}", 
                duplicate.getId(),
                duplicate.getCreatedAt());
        }
        
        return duplicate;
    }
}
