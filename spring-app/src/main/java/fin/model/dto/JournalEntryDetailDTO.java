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

package fin.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Journal Entry detail view.
 * Contains full journal entry information including all line items.
 * Used for drill-down modal display.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
public class JournalEntryDetailDTO {
    
    private Long id;
    private String reference;
    private LocalDate entryDate;
    private String description;
    private String transactionType;
    private Long fiscalPeriodId;
    private String fiscalPeriodName;
    private Long companyId;
    private String companyName;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedAt;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private Integer lineCount;
    private List<JournalEntryLineDTO> lines;

    /**
     * Default constructor for JSON deserialization
     */
    public JournalEntryDetailDTO() {
        this.lines = new ArrayList<>();
    }

    /**
     * Full constructor for creating DTO with all fields
     */
    public JournalEntryDetailDTO(Long id, String reference, LocalDate entryDate, String description,
                                String transactionType, Long fiscalPeriodId, String fiscalPeriodName,
                                Long companyId, String companyName, String createdBy,
                                LocalDateTime createdAt, LocalDateTime updatedAt, String lastModifiedBy,
                                LocalDateTime lastModifiedAt, BigDecimal totalDebit, BigDecimal totalCredit,
                                Integer lineCount, List<JournalEntryLineDTO> lines) {
        this.id = id;
        this.reference = reference;
        this.entryDate = entryDate;
        this.description = description;
        this.transactionType = transactionType;
        this.fiscalPeriodId = fiscalPeriodId;
        this.fiscalPeriodName = fiscalPeriodName;
        this.companyId = companyId;
        this.companyName = companyName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastModifiedBy = lastModifiedBy;
        this.lastModifiedAt = lastModifiedAt;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.lineCount = lineCount;
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }

    public String getFiscalPeriodName() {
        return fiscalPeriodName;
    }

    public void setFiscalPeriodName(String fiscalPeriodName) {
        this.fiscalPeriodName = fiscalPeriodName;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public List<JournalEntryLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<JournalEntryLineDTO> lines) {
        this.lines = lines != null ? lines : new ArrayList<>();
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    public LocalDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public void setTotalDebit(BigDecimal totalDebit) {
        this.totalDebit = totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public void setTotalCredit(BigDecimal totalCredit) {
        this.totalCredit = totalCredit;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer lineCount) {
        this.lineCount = lineCount;
    }

    @Override
    public String toString() {
        return "JournalEntryDetailDTO{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", entryDate=" + entryDate +
                ", description='" + description + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", fiscalPeriodId=" + fiscalPeriodId +
                ", fiscalPeriodName='" + fiscalPeriodName + '\'' +
                ", companyId=" + companyId +
                ", companyName='" + companyName + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", lines=" + lines.size() + " lines" +
                '}';
    }
}
