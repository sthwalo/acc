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
    
    public Employee(Long companyId, String employeeNumber, String firstName, String lastName, 
                   String position, BigDecimal basicSalary) {
        this();
        this.companyId = companyId;
        this.employeeNumber = employeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.position = position;
        this.basicSalary = basicSalary;
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
    public void setId(Long id) { this.id = id; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    
    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getSecondName() { return secondName; }
    public void setSecondName(String secondName) { this.secondName = secondName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    
    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    // Address Information
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    // Banking Information
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    
    public String getAccountHolderName() { return accountHolderName; }
    public void setAccountHolderName(String accountHolderName) { this.accountHolderName = accountHolderName; }
    
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    
    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    
    // Employment Details
    public EmploymentType getEmploymentType() { return employmentType; }
    public void setEmploymentType(EmploymentType employmentType) { this.employmentType = employmentType; }
    
    public SalaryType getSalaryType() { return salaryType; }
    public void setSalaryType(SalaryType salaryType) { this.salaryType = salaryType; }
    
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    
    public BigDecimal getOvertimeRate() { return overtimeRate; }
    public void setOvertimeRate(BigDecimal overtimeRate) { this.overtimeRate = overtimeRate; }
    
    // Tax Information
    public String getTaxNumber() { return taxNumber; }
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }
    
    public String getTaxRebateCode() { return taxRebateCode; }
    public void setTaxRebateCode(String taxRebateCode) { this.taxRebateCode = taxRebateCode; }
    
    public String getUifNumber() { return uifNumber; }
    public void setUifNumber(String uifNumber) { this.uifNumber = uifNumber; }
    
    public String getMedicalAidNumber() { return medicalAidNumber; }
    public void setMedicalAidNumber(String medicalAidNumber) { this.medicalAidNumber = medicalAidNumber; }
    
    public String getPensionFundNumber() { return pensionFundNumber; }
    public void setPensionFundNumber(String pensionFundNumber) { this.pensionFundNumber = pensionFundNumber; }
    
    // Audit Fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    
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
