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
 */

package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Employee entity representing an employee in the payroll system
 * Fully integrated with the FIN financial management system
 */
public class Employee {
    private Long id;
    private Long companyId;
    private String employeeNumber;
    private String title;
    private String firstName;
    private String secondName;
    private String lastName;
    private String email;
    private String phone;
    private String position;
    private String department;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private boolean isActive = true;
    
    // Address Information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String province;
    private String postalCode;
    private String country = "ZA";
    
    // Banking Information
    private String bankName;
    private String accountHolderName;
    private String accountNumber;
    private String branchCode;
    private String accountType = "SAVINGS";
    
    // Employment Details
    private EmploymentType employmentType = EmploymentType.PERMANENT;
    private SalaryType salaryType = SalaryType.MONTHLY;
    private BigDecimal basicSalary;
    private BigDecimal overtimeRate = new BigDecimal("1.5");
    
    // Tax Information
    private String taxNumber;
    private String taxRebateCode = "A";
    private String uifNumber;
    private String medicalAidNumber;
    private String pensionFundNumber;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Enums
    public enum EmploymentType {
        PERMANENT, CONTRACT, TEMPORARY
    }
    
    public enum SalaryType {
        MONTHLY, WEEKLY, HOURLY, DAILY
    }
    
    // Constructors
    public Employee() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Employee(Long initialCompanyId, String initialEmployeeNumber, String initialFirstName, String initialLastName, 
                   String initialPosition, BigDecimal initialBasicSalary) {
        this();
        this.companyId = initialCompanyId;
        this.employeeNumber = initialEmployeeNumber;
        this.firstName = initialFirstName;
        this.lastName = initialLastName;
        this.position = initialPosition;
        this.basicSalary = initialBasicSalary;
    }
    
    // Calculated properties
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    public String getDisplayName() {
        return employeeNumber + " - " + getFullName();
    }
    
    public boolean isCurrentEmployee() {
        return isActive && terminationDate == null;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String newEmployeeNumber) { this.employeeNumber = newEmployeeNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String newTitle) { this.title = newTitle; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String newFirstName) { this.firstName = newFirstName; }
    
    public String getSecondName() { return secondName; }
    public void setSecondName(String newSecondName) { this.secondName = newSecondName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String newLastName) { this.lastName = newLastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String newEmail) { this.email = newEmail; }
    
    public String getPhone() { return phone; }
    public void setPhone(String newPhone) { this.phone = newPhone; }
    
    public String getPosition() { return position; }
    public void setPosition(String newPosition) { this.position = newPosition; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String newDepartment) { this.department = newDepartment; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate newHireDate) { this.hireDate = newHireDate; }
    
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate newTerminationDate) { this.terminationDate = newTerminationDate; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean newActive) { isActive = newActive; }
    
    // Address Information
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String newAddressLine1) { this.addressLine1 = newAddressLine1; }
    
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String newAddressLine2) { this.addressLine2 = newAddressLine2; }
    
    public String getCity() { return city; }
    public void setCity(String newCity) { this.city = newCity; }
    
    public String getProvince() { return province; }
    public void setProvince(String newProvince) { this.province = newProvince; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String newPostalCode) { this.postalCode = newPostalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String newCountry) { this.country = newCountry; }
    
    // Banking Information
    public String getBankName() { return bankName; }
    public void setBankName(String newBankName) { this.bankName = newBankName; }
    
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String newAccountHolderName) { this.accountHolderName = newAccountHolderName; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String newAccountNumber) { this.accountNumber = newAccountNumber; }
    
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String newBranchCode) { this.branchCode = newBranchCode; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String newAccountType) { this.accountType = newAccountType; }
    
    // Employment Details
    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType newEmploymentType) { this.employmentType = newEmploymentType; }
    
    public SalaryType getSalaryType() { return salaryType; }
    public void setSalaryType(SalaryType newSalaryType) { this.salaryType = newSalaryType; }
    
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal newBasicSalary) { this.basicSalary = newBasicSalary; }
    
    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal newOvertimeRate) { this.overtimeRate = newOvertimeRate; }
    
    // Tax Information
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String newTaxNumber) { this.taxNumber = newTaxNumber; }
    
    public String getTaxRebateCode() { return taxRebateCode; }
    public void setTaxRebateCode(String newTaxRebateCode) { this.taxRebateCode = newTaxRebateCode; }
    
    public String getUifNumber() { return uifNumber; }
    public void setUifNumber(String newUifNumber) { this.uifNumber = newUifNumber; }
    
    public String getMedicalAidNumber() { return medicalAidNumber; }
    public void setMedicalAidNumber(String newMedicalAidNumber) { this.medicalAidNumber = newMedicalAidNumber; }
    
    public String getPensionFundNumber() { return pensionFundNumber; }
    public void setPensionFundNumber(String newPensionFundNumber) { this.pensionFundNumber = newPensionFundNumber; }
    
    // Audit Fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime newUpdatedAt) { this.updatedAt = newUpdatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String newCreatedBy) { this.createdBy = newCreatedBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String newUpdatedBy) { this.updatedBy = newUpdatedBy; }
    
    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", employeeNumber='" + employeeNumber + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", position='" + position + '\'' +
                ", department='" + department + '\'' +
                ", basicSalary=" + basicSalary +
                ", isActive=" + isActive +
                '}';
    }
}
