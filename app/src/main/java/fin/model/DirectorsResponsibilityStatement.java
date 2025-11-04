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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Directors Responsibility Statement Model
 * Represents the statement of financial responsibility by directors
 */
public class DirectorsResponsibilityStatement {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String statementText;
    private LocalDate statementDate;
    private String approvedBy;
    private String approvedByTitle;
    private String witnessName;
    private String witnessTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public DirectorsResponsibilityStatement() {}
    
    public DirectorsResponsibilityStatement(Long companyId, Long fiscalPeriodId, String statementText) {
        this.companyId = companyId;
        this.fiscalPeriodId = fiscalPeriodId;
        this.statementText = statementText;
        this.statementDate = LocalDate.now();
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
    
    public String getStatementText() {
        return statementText;
    }
    
    public void setStatementText(String statementText) {
        this.statementText = statementText;
    }
    
    public LocalDate getStatementDate() {
        return statementDate;
    }
    
    public void setStatementDate(LocalDate statementDate) {
        this.statementDate = statementDate;
    }
    
    public String getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public String getApprovedByTitle() {
        return approvedByTitle;
    }
    
    public void setApprovedByTitle(String approvedByTitle) {
        this.approvedByTitle = approvedByTitle;
    }
    
    public String getWitnessName() {
        return witnessName;
    }
    
    public void setWitnessName(String witnessName) {
        this.witnessName = witnessName;
    }
    
    public String getWitnessTitle() {
        return witnessTitle;
    }
    
    public void setWitnessTitle(String witnessTitle) {
        this.witnessTitle = witnessTitle;
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
