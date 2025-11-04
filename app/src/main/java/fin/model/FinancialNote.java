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

import java.time.LocalDateTime;

/**
 * Financial Note Model
 * Represents notes to financial statements (IFRS/IAS/GAAP compliant)
 */
public class FinancialNote {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String noteNumber; // e.g., "1", "2.1", "7.3"
    private String noteTitle; // e.g., "Accounting Policies", "Revenue"
    private NoteCategory noteCategory;
    private String noteContent; // Full text content
    private int displayOrder;
    private boolean isMandatory; // Required by IFRS/IAS
    private String standardReference; // e.g., "IAS 1", "IFRS 15"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum NoteCategory {
        ACCOUNTING_POLICIES,
        REVENUE,
        EXPENSES,
        ASSETS,
        LIABILITIES,
        EQUITY,
        CASH_FLOW,
        OTHER
    }
    
    // Constructors
    public FinancialNote() {}
    
    public FinancialNote(Long companyId, Long fiscalPeriodId, String noteNumber, 
                         String noteTitle, NoteCategory noteCategory, String noteContent) {
        this.companyId = companyId;
        this.fiscalPeriodId = fiscalPeriodId;
        this.noteNumber = noteNumber;
        this.noteTitle = noteTitle;
        this.noteCategory = noteCategory;
        this.noteContent = noteContent;
        this.displayOrder = 0;
        this.isMandatory = false;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getCompanyId() {
        return companyId;
    }
    
    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
    
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }
    
    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }
    
    public String getNoteNumber() {
        return noteNumber;
    }
    
    public void setNoteNumber(String noteNumber) {
        this.noteNumber = noteNumber;
    }
    
    public String getNoteTitle() {
        return noteTitle;
    }
    
    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }
    
    public NoteCategory getNoteCategory() {
        return noteCategory;
    }
    
    public void setNoteCategory(NoteCategory noteCategory) {
        this.noteCategory = noteCategory;
    }
    
    public String getNoteContent() {
        return noteContent;
    }
    
    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }
    
    public int getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }
    
    public boolean isMandatory() {
        return isMandatory;
    }
    
    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }
    
    public String getStandardReference() {
        return standardReference;
    }
    
    public void setStandardReference(String standardReference) {
        this.standardReference = standardReference;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
