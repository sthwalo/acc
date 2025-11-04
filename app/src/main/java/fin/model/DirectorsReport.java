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
 * Directors Report Model
 * Represents the directors' report on company activities and financial performance
 */
public class DirectorsReport {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String natureOfBusiness;
    private String reviewOfOperations;
    private String directorsNames;
    private String directorsInterests;
    private String shareCapitalInfo;
    private String dividendsInfo;
    private String propertyPlantEquipment;
    private String eventsAfterReportingDate;
    private String goingConcernAssessment;
    private String secretaryInfo;
    private String auditorsInfo;
    private String specialResolutions;
    private LocalDate reportDate;
    private String approvedBy;
    private String approvedByTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public DirectorsReport() {}
    
    public DirectorsReport(Long companyId, Long fiscalPeriodId) {
        this.companyId = companyId;
        this.fiscalPeriodId = fiscalPeriodId;
        this.reportDate = LocalDate.now();
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
    
    public String getNatureOfBusiness() {
        return natureOfBusiness;
    }
    
    public void setNatureOfBusiness(String natureOfBusiness) {
        this.natureOfBusiness = natureOfBusiness;
    }
    
    public String getReviewOfOperations() {
        return reviewOfOperations;
    }
    
    public void setReviewOfOperations(String reviewOfOperations) {
        this.reviewOfOperations = reviewOfOperations;
    }
    
    public String getDirectorsNames() {
        return directorsNames;
    }
    
    public void setDirectorsNames(String directorsNames) {
        this.directorsNames = directorsNames;
    }
    
    public String getDirectorsInterests() {
        return directorsInterests;
    }
    
    public void setDirectorsInterests(String directorsInterests) {
        this.directorsInterests = directorsInterests;
    }
    
    public String getShareCapitalInfo() {
        return shareCapitalInfo;
    }
    
    public void setShareCapitalInfo(String shareCapitalInfo) {
        this.shareCapitalInfo = shareCapitalInfo;
    }
    
    public String getDividendsInfo() {
        return dividendsInfo;
    }
    
    public void setDividendsInfo(String dividendsInfo) {
        this.dividendsInfo = dividendsInfo;
    }
    
    public String getPropertyPlantEquipment() {
        return propertyPlantEquipment;
    }
    
    public void setPropertyPlantEquipment(String propertyPlantEquipment) {
        this.propertyPlantEquipment = propertyPlantEquipment;
    }
    
    public String getEventsAfterReportingDate() {
        return eventsAfterReportingDate;
    }
    
    public void setEventsAfterReportingDate(String eventsAfterReportingDate) {
        this.eventsAfterReportingDate = eventsAfterReportingDate;
    }
    
    public String getGoingConcernAssessment() {
        return goingConcernAssessment;
    }
    
    public void setGoingConcernAssessment(String goingConcernAssessment) {
        this.goingConcernAssessment = goingConcernAssessment;
    }
    
    public String getSecretaryInfo() {
        return secretaryInfo;
    }
    
    public void setSecretaryInfo(String secretaryInfo) {
        this.secretaryInfo = secretaryInfo;
    }
    
    public String getAuditorsInfo() {
        return auditorsInfo;
    }
    
    public void setAuditorsInfo(String auditorsInfo) {
        this.auditorsInfo = auditorsInfo;
    }
    
    public String getSpecialResolutions() {
        return specialResolutions;
    }
    
    public void setSpecialResolutions(String specialResolutions) {
        this.specialResolutions = specialResolutions;
    }
    
    public LocalDate getReportDate() {
        return reportDate;
    }
    
    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
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
