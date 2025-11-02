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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a journal entry header in the double-entry accounting system.
 * Each journal entry contains multiple lines that must balance (debits = credits).
 */
public class JournalEntry {
    private Long id;
    private String reference;
    private LocalDate entryDate;
    private String description;
    private Long transactionTypeId;
    private Long fiscalPeriodId;
    private Long companyId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Associated journal entry lines
    private List<JournalEntryLine> lines;
    
    public JournalEntry() {
        this.lines = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public JournalEntry(String initialReference, LocalDate initialEntryDate, String initialDescription, 
                       Long initialFiscalPeriodId, Long initialCompanyId, String initialCreatedBy) {
        this();
        this.reference = initialReference;
        this.entryDate = initialEntryDate;
        this.description = initialDescription;
        this.fiscalPeriodId = initialFiscalPeriodId;
        this.companyId = initialCompanyId;
        this.createdBy = initialCreatedBy;
    }
    
    /**
     * Calculates the total debit amount from all lines
     */
    public BigDecimal getTotalDebits() {
        return lines.stream()
                .map(JournalEntryLine::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Calculates the total credit amount from all lines
     */
    public BigDecimal getTotalCredits() {
        return lines.stream()
                .map(JournalEntryLine::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Checks if the journal entry is balanced (debits = credits)
     */
    public boolean isBalanced() {
        return getTotalDebits().compareTo(getTotalCredits()) == 0;
    }
    
    /**
     * Adds a journal entry line to this entry
     */
    public void addLine(JournalEntryLine line) {
        line.setJournalEntryId(this.id);
        this.lines.add(line);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long newId) {
        this.id = newId;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String newReference) {
        this.reference = newReference;
    }
    
    public LocalDate getEntryDate() {
        return entryDate;
    }
    
    public void setEntryDate(LocalDate newEntryDate) {
        this.entryDate = newEntryDate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }
    
    public Long getTransactionTypeId() {
        return transactionTypeId;
    }
    
    public void setTransactionTypeId(Long newTransactionTypeId) {
        this.transactionTypeId = newTransactionTypeId;
    }
    
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }
    
    public void setFiscalPeriodId(Long newFiscalPeriodId) {
        this.fiscalPeriodId = newFiscalPeriodId;
    }
    
    public Long getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(Long newCompanyId) {
        this.companyId = newCompanyId;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String newCreatedBy) {
        this.createdBy = newCreatedBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
    }
    
    public List<JournalEntryLine> getLines() {
        return new ArrayList<>(lines);
    }
    
    public void setLines(List<JournalEntryLine> newLines) {
        this.lines = new ArrayList<>(newLines);
    }
    
    @Override
    public String toString() {
        return "JournalEntry{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", entryDate=" + entryDate +
                ", description='" + description + '\'' +
                ", totalDebits=" + getTotalDebits() +
                ", totalCredits=" + getTotalCredits() +
                ", balanced=" + isBalanced() +
                '}';
    }
}
