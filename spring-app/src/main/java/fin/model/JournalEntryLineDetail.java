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

package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Detailed view of a journal entry line with parent journal entry information.
 * Used for general ledger reports where we need both line details and entry metadata.
 */
public class JournalEntryLineDetail {
    private Long lineId;
    private Long journalEntryId;
    private LocalDate entryDate;
    private String reference;
    private String description;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;

    public JournalEntryLineDetail() {
        this.debitAmount = BigDecimal.ZERO;
        this.creditAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long newLineId) {
        this.lineId = newLineId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long newJournalEntryId) {
        this.journalEntryId = newJournalEntryId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate newEntryDate) {
        this.entryDate = newEntryDate;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String newReference) {
        this.reference = newReference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long newAccountId) {
        this.accountId = newAccountId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String newAccountCode) {
        this.accountCode = newAccountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String newAccountName) {
        this.accountName = newAccountName;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal newDebitAmount) {
        this.debitAmount = newDebitAmount != null ? newDebitAmount : BigDecimal.ZERO;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal newCreditAmount) {
        this.creditAmount = newCreditAmount != null ? newCreditAmount : BigDecimal.ZERO;
    }
}