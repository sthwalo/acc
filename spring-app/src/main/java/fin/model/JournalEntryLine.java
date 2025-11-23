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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Represents a single line in a journal entry.
 * Each line contains either a debit or credit amount for a specific account.
 */
@Entity
@Table(name = "journal_entry_lines")
public class JournalEntryLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "journal_entry_id")
    private Long journalEntryId;
    
    @Column(name = "manual_invoice_id")
    private Long manualInvoiceId;
    
    @Column(name = "source_transaction_id")
    private Long sourceTransactionId;
    
    @Column(name = "line_number")
    private Integer lineNumber;
    
    @Column(name = "account_id")
    private Long accountId;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "debit_amount")
    private BigDecimal debitAmount;
    
    @Column(name = "credit_amount")
    private BigDecimal creditAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", insertable = false, updatable = false)
    private JournalEntry journalEntry;

    public JournalEntryLine() {
        this.debitAmount = BigDecimal.ZERO;
        this.creditAmount = BigDecimal.ZERO;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long newId) {
        this.id = newId;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long newJournalEntryId) {
        this.journalEntryId = newJournalEntryId;
    }

    public Long getManualInvoiceId() {
        return manualInvoiceId;
    }

    public void setManualInvoiceId(Long manualInvoiceId) {
        this.manualInvoiceId = manualInvoiceId;
    }

    public Long getSourceTransactionId() {
        return sourceTransactionId;
    }

    public void setSourceTransactionId(Long sourceTransactionId) {
        this.sourceTransactionId = sourceTransactionId;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long newAccountId) {
        this.accountId = newAccountId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
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

    public JournalEntry getJournalEntry() {
        return journalEntry;
    }

    public void setJournalEntry(JournalEntry journalEntry) {
        this.journalEntry = journalEntry;
        if (journalEntry != null) {
            this.journalEntryId = journalEntry.getId();
        }
    }

    public Account getAccount() {
        // TODO: Add relationship to Account
        return null;
    }

    public void setAccount(Account account) {
        if (account != null) {
            this.accountId = account.getId();
        }
    }

    public String getReference() {
        return description;
    }

    public void setReference(String reference) {
        this.description = reference;
    }
}