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

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import java.util.List;

@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "registration_number")
    private String registrationNumber;
    
    @Column(name = "tax_number")
    private String taxNumber;
    
    @Column(name = "address")
    private String address;
    
    @Column(name = "contact_email")
    private String contactEmail;
    
    @Column(name = "contact_phone")
    private String contactPhone;
    
    @Column(name = "logo_path")
    private String logoPath;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Banking details
    @Column(name = "bank_name")
    private String bankName;
    
    @Column(name = "account_number")
    private String accountNumber;
    
    @Column(name = "account_type")
    private String accountType;
    
    @Column(name = "branch_code")
    private String branchCode;
    
    // VAT registration
    @Column(name = "vat_registered")
    private boolean vatRegistered;
    
    // Relationships
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCompany> userCompanies;
    
    // Constructors, getters, and setters
    public Company() {}
    
    public Company(String initialName) {
        this.name = initialName;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
    /**
     * Creates a deep copy of all Company fields to prevent external modification.
     */
    public Company(Company other) {
        if (other == null) {
            return;
        }
        
        this.id = other.id;
        this.name = other.name;
        this.registrationNumber = other.registrationNumber;
        this.taxNumber = other.taxNumber;
        this.address = other.address;
        this.contactEmail = other.contactEmail;
        this.contactPhone = other.contactPhone;
        this.logoPath = other.logoPath;
        this.createdAt = other.createdAt;
        this.bankName = other.bankName;
        this.accountNumber = other.accountNumber;
        this.accountType = other.accountType;
        this.branchCode = other.branchCode;
        this.vatRegistered = other.vatRegistered;
        this.userCompanies = other.userCompanies; // Reference copy for relationships
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public String getName() { return name; }
    public void setName(String newName) { this.name = newName; }
    
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String newRegistrationNumber) { this.registrationNumber = newRegistrationNumber; }
    
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String newTaxNumber) { this.taxNumber = newTaxNumber; }
    
    public String getAddress() { return address; }
    public void setAddress(String newAddress) { this.address = newAddress; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String newContactEmail) { this.contactEmail = newContactEmail; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String newContactPhone) { this.contactPhone = newContactPhone; }
    
    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String newLogoPath) { this.logoPath = newLogoPath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    // Banking details getters and setters
    public String getBankName() { return bankName; }
    public void setBankName(String newBankName) { this.bankName = newBankName; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String newAccountNumber) { this.accountNumber = newAccountNumber; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String newAccountType) { this.accountType = newAccountType; }
    
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String newBranchCode) { this.branchCode = newBranchCode; }
    
    // VAT registration getters and setters
    public boolean isVatRegistered() { return vatRegistered; }
    public void setVatRegistered(boolean newVatRegistered) { this.vatRegistered = newVatRegistered; }
    
    // Relationship getters and setters
    public List<UserCompany> getUserCompanies() { return userCompanies; }
    public void setUserCompanies(List<UserCompany> userCompanies) { this.userCompanies = userCompanies; }
    
    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", taxNumber='" + taxNumber + '\'' +
                ", vatRegistered=" + vatRegistered +
                '}';
    }
}
