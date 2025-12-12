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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for rejected bank transactions during upload processing.
 * 
 * <p>Provides detailed information about why a transaction was rejected,
 * including the rejection reason and transaction details for reporting.
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
public class RejectedTransaction {
    
    private final LocalDate transactionDate;
    private final String description;
    private final BigDecimal debitAmount;
    private final BigDecimal creditAmount;
    private final BigDecimal balance;
    private final RejectionReason reason;
    private final String reasonDetail;
    
    /**
     * Constructor for rejected transaction.
     *
     * @param transactionDate Transaction date
     * @param description Transaction description
     * @param debitAmount Debit amount (can be null)
     * @param creditAmount Credit amount (can be null)
     * @param balance Account balance after transaction
     * @param reason Rejection reason code
     * @param reasonDetail Detailed explanation of rejection
     */
    public RejectedTransaction(LocalDate transactionDate, String description,
                              BigDecimal debitAmount, BigDecimal creditAmount,
                              BigDecimal balance, RejectionReason reason, String reasonDetail) {
        this.transactionDate = transactionDate;
        this.description = description;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.balance = balance;
        this.reason = reason;
        this.reasonDetail = reasonDetail;
    }
    
    // Getters
    
    public LocalDate getTransactionDate() {
        return transactionDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getDebitAmount() {
        return debitAmount;
    }
    
    public BigDecimal getCreditAmount() {
        return creditAmount;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public RejectionReason getReason() {
        return reason;
    }
    
    public String getReasonDetail() {
        return reasonDetail;
    }
    
    /**
     * Rejection reason codes.
     */
    public enum RejectionReason {
        /** Transaction already exists in database (duplicate) */
        DUPLICATE,
        
        /** Transaction date outside fiscal period boundaries */
        OUT_OF_PERIOD,
        
        /** Transaction failed validation rules */
        VALIDATION_ERROR
    }
    
    @Override
    public String toString() {
        return String.format("RejectedTransaction{date=%s, description='%s', debit=%s, credit=%s, balance=%s, reason=%s, detail='%s'}",
            transactionDate, 
            description != null && description.length() > 50 ? description.substring(0, 50) + "..." : description,
            debitAmount, creditAmount, balance, reason, reasonDetail);
    }
}
