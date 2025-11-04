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
 * Company Extended Info Model
 * Additional company information required for comprehensive financial reports
 */
public class CompanyExtendedInfo {
    private Long id;
    private Long companyId;
    private String natureOfBusiness;
    private String directorsNames;
    private String companySecretary;
    private String businessAddress;
    private String postalAddress;
    private String auditorsName;
    private String auditorsAddress;
    private String auditorsRegistration; // IRBA registration
    private String bankersName;
    private String bankersBranch;
    private String legalAdvisors;
    private Integer yearOfIncorporation;
    private String countryOfIncorporation;
    private String accountingFramework; // IFRS, IFRS for SMEs, GAAP
    private String functionalCurrency;
    private String presentationCurrency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public CompanyExtendedInfo() {
        this.countryOfIncorporation = "South Africa";
        this.accountingFramework = "IFRS";
        this.functionalCurrency = "ZAR";
        this.presentationCurrency = "ZAR";
    }
    
    public CompanyExtendedInfo(Long companyId) {
        this();
        this.companyId = companyId;
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
    
    public String getNatureOfBusiness() {
        return natureOfBusiness;
    }
    
    public void setNatureOfBusiness(String natureOfBusiness) {
        this.natureOfBusiness = natureOfBusiness;
    }
    
    public String getDirectorsNames() {
        return directorsNames;
    }
    
    public void setDirectorsNames(String directorsNames) {
        this.directorsNames = directorsNames;
    }
    
    public String getCompanySecretary() {
        return companySecretary;
    }
    
    public void setCompanySecretary(String companySecretary) {
        this.companySecretary = companySecretary;
    }
    
    public String getBusinessAddress() {
        return businessAddress;
    }
    
    public void setBusinessAddress(String businessAddress) {
        this.businessAddress = businessAddress;
    }
    
    public String getPostalAddress() {
        return postalAddress;
    }
    
    public void setPostalAddress(String postalAddress) {
        this.postalAddress = postalAddress;
    }
    
    public String getAuditorsName() {
        return auditorsName;
    }
    
    public void setAuditorsName(String auditorsName) {
        this.auditorsName = auditorsName;
    }
    
    public String getAuditorsAddress() {
        return auditorsAddress;
    }
    
    public void setAuditorsAddress(String auditorsAddress) {
        this.auditorsAddress = auditorsAddress;
    }
    
    public String getAuditorsRegistration() {
        return auditorsRegistration;
    }
    
    public void setAuditorsRegistration(String auditorsRegistration) {
        this.auditorsRegistration = auditorsRegistration;
    }
    
    public String getBankersName() {
        return bankersName;
    }
    
    public void setBankersName(String bankersName) {
        this.bankersName = bankersName;
    }
    
    public String getBankersBranch() {
        return bankersBranch;
    }
    
    public void setBankersBranch(String bankersBranch) {
        this.bankersBranch = bankersBranch;
    }
    
    public String getLegalAdvisors() {
        return legalAdvisors;
    }
    
    public void setLegalAdvisors(String legalAdvisors) {
        this.legalAdvisors = legalAdvisors;
    }
    
    public Integer getYearOfIncorporation() {
        return yearOfIncorporation;
    }
    
    public void setYearOfIncorporation(Integer yearOfIncorporation) {
        this.yearOfIncorporation = yearOfIncorporation;
    }
    
    public String getCountryOfIncorporation() {
        return countryOfIncorporation;
    }
    
    public void setCountryOfIncorporation(String countryOfIncorporation) {
        this.countryOfIncorporation = countryOfIncorporation;
    }
    
    public String getAccountingFramework() {
        return accountingFramework;
    }
    
    public void setAccountingFramework(String accountingFramework) {
        this.accountingFramework = accountingFramework;
    }
    
    public String getFunctionalCurrency() {
        return functionalCurrency;
    }
    
    public void setFunctionalCurrency(String functionalCurrency) {
        this.functionalCurrency = functionalCurrency;
    }
    
    public String getPresentationCurrency() {
        return presentationCurrency;
    }
    
    public void setPresentationCurrency(String presentationCurrency) {
        this.presentationCurrency = presentationCurrency;
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
