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
 * Audit Report Model
 * Represents independent auditor's report on financial statements
 */
public class AuditReport {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private AuditOpinion auditOpinion;
    private String opinionText;
    private String basisForOpinion;
    private String keyAuditMatters;
    private String otherInformation;
    private String responsibilitiesManagement;
    private String responsibilitiesAuditor;
    private String auditorFirmName;
    private String auditorFirmRegistration; // IRBA number
    private String auditorPartnerName;
    private LocalDate auditDate;
    private LocalDate reportDate;
    private String auditStandard; // ISA, GAAS, etc.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public enum AuditOpinion {
        UNQUALIFIED("Unqualified"),
        QUALIFIED("Qualified"),
        ADVERSE("Adverse"),
        DISCLAIMER("Disclaimer of Opinion");
        
        private final String displayName;
        
        AuditOpinion(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    // Constructors
    public AuditReport() {
        this.auditStandard = "ISA"; // Default to International Standards on Auditing
    }
    
    public AuditReport(Long companyId, Long fiscalPeriodId, AuditOpinion opinion) {
        this();
        this.companyId = companyId;
        this.fiscalPeriodId = fiscalPeriodId;
        this.auditOpinion = opinion;
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
    
    public AuditOpinion getAuditOpinion() {
        return auditOpinion;
    }
    
    public void setAuditOpinion(AuditOpinion auditOpinion) {
        this.auditOpinion = auditOpinion;
    }
    
    public String getOpinionText() {
        return opinionText;
    }
    
    public void setOpinionText(String opinionText) {
        this.opinionText = opinionText;
    }
    
    public String getBasisForOpinion() {
        return basisForOpinion;
    }
    
    public void setBasisForOpinion(String basisForOpinion) {
        this.basisForOpinion = basisForOpinion;
    }
    
    public String getKeyAuditMatters() {
        return keyAuditMatters;
    }
    
    public void setKeyAuditMatters(String keyAuditMatters) {
        this.keyAuditMatters = keyAuditMatters;
    }
    
    public String getOtherInformation() {
        return otherInformation;
    }
    
    public void setOtherInformation(String otherInformation) {
        this.otherInformation = otherInformation;
    }
    
    public String getResponsibilitiesManagement() {
        return responsibilitiesManagement;
    }
    
    public void setResponsibilitiesManagement(String responsibilitiesManagement) {
        this.responsibilitiesManagement = responsibilitiesManagement;
    }
    
    public String getResponsibilitiesAuditor() {
        return responsibilitiesAuditor;
    }
    
    public void setResponsibilitiesAuditor(String responsibilitiesAuditor) {
        this.responsibilitiesAuditor = responsibilitiesAuditor;
    }
    
    public String getAuditorFirmName() {
        return auditorFirmName;
    }
    
    public void setAuditorFirmName(String auditorFirmName) {
        this.auditorFirmName = auditorFirmName;
    }
    
    public String getAuditorFirmRegistration() {
        return auditorFirmRegistration;
    }
    
    public void setAuditorFirmRegistration(String auditorFirmRegistration) {
        this.auditorFirmRegistration = auditorFirmRegistration;
    }
    
    public String getAuditorPartnerName() {
        return auditorPartnerName;
    }
    
    public void setAuditorPartnerName(String auditorPartnerName) {
        this.auditorPartnerName = auditorPartnerName;
    }
    
    public LocalDate getAuditDate() {
        return auditDate;
    }
    
    public void setAuditDate(LocalDate auditDate) {
        this.auditDate = auditDate;
    }
    
    public LocalDate getReportDate() {
        return reportDate;
    }
    
    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }
    
    public String getAuditStandard() {
        return auditStandard;
    }
    
    public void setAuditStandard(String auditStandard) {
        this.auditStandard = auditStandard;
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
