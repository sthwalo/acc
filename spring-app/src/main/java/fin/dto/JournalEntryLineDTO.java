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

/**
 * Data Transfer Object for individual Journal Entry Line item.
 * Represents a single debit or credit line within a journal entry.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
public class JournalEntryLineDTO {
    
    private Long id;
    private int lineNumber;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String description;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    /**
     * Default constructor for JSON deserialization
     */
    public JournalEntryLineDTO() {
    }

    /**
     * Full constructor for creating DTO with all fields
     */
    public JournalEntryLineDTO(Long id, int lineNumber, Long accountId, String accountCode,
                              String accountName, String description, BigDecimal debitAmount,
                              BigDecimal creditAmount) {
        this.id = id;
        this.lineNumber = lineNumber;
        this.accountId = accountId;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.description = description;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    @Override
    public String toString() {
        return "JournalEntryLineDTO{" +
                "id=" + id +
                ", lineNumber=" + lineNumber +
                ", accountId=" + accountId +
                ", accountCode='" + accountCode + '\'' +
                ", accountName='" + accountName + '\'' +
                ", description='" + description + '\'' +
                ", debitAmount=" + debitAmount +
                ", creditAmount=" + creditAmount +
                '}';
    }
}
