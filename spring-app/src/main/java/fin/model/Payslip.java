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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Payslip entity representing an employee's payslip for a specific period
 */
@Entity
@Table(name = "payslips")
@Access(AccessType.FIELD)
public class Payslip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "employee_id")
    private Long employeeId;
    @Column(name = "payroll_period_id")
    private Long fiscalPeriodId;
    @Column(name = "payslip_number")
    private String payslipNumber;
    
    // Salary Information
    @Column(name = "basic_salary")
    private BigDecimal basicSalary;
    @Column(name = "overtime_hours")
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    @Column(name = "overtime_amount")
    private BigDecimal overtimeAmount = BigDecimal.ZERO;
    
    // Earnings
    @Column(name = "gross_salary")
    private BigDecimal grossSalary;
    @Column(name = "housing_allowance")
    private BigDecimal housingAllowance = BigDecimal.ZERO;
    @Column(name = "transport_allowance")
    private BigDecimal transportAllowance = BigDecimal.ZERO;
    @Column(name = "medical_allowance")
    private BigDecimal medicalAllowance = BigDecimal.ZERO;
    @Column(name = "other_allowances")
    private BigDecimal otherAllowances = BigDecimal.ZERO;
    @Column(name = "commission")
    private BigDecimal commission = BigDecimal.ZERO;
    @Column(name = "bonus")
    private BigDecimal bonus = BigDecimal.ZERO;
    @Column(name = "total_earnings")
    private BigDecimal totalEarnings;
    
    // Statutory Deductions
    @Column(name = "paye_tax")
    private BigDecimal payeTax = BigDecimal.ZERO;
    @Column(name = "uif_employee")
    private BigDecimal uifEmployee = BigDecimal.ZERO;
    @Column(name = "uif_employer")
    private BigDecimal uifEmployer = BigDecimal.ZERO;
    @Column(name = "sdl_levy")
    private BigDecimal sdlLevy = BigDecimal.ZERO;  // Skills Development Levy (1% of gross for employers)
    
    // Other Deductions
    @Column(name = "medical_aid")
    private BigDecimal medicalAid = BigDecimal.ZERO;
    @Column(name = "pension_fund")
    private BigDecimal pensionFund = BigDecimal.ZERO;
    @Column(name = "loan_deduction")
    private BigDecimal loanDeduction = BigDecimal.ZERO;
    @Column(name = "other_deductions")
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;
    
    // Net Pay
    @Column(name = "net_pay")
    private BigDecimal netPay;
    
    // Tax Certificate Info
    @Column(name = "annual_gross")
    private BigDecimal annualGross;
    @Column(name = "annual_paye")
    private BigDecimal annualPaye;
    @Column(name = "annual_uif")
    private BigDecimal annualUif;
    
    // Status
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PayslipStatus status = PayslipStatus.DRAFT;
    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod = PaymentMethod.EFT;
    @Column(name = "payment_date")
    private LocalDate paymentDate;
    @Column(name = "payment_reference")
    private String paymentReference;
    
    // Audit Fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "created_by")
    private String createdBy;
    
    // Enums
    public enum PayslipStatus {
        DRAFT, APPROVED, PAID, EXPORTED
    }
    
    public enum PaymentMethod {
        EFT, CASH, CHEQUE
    }
    
    // Constructors
    public Payslip() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Payslip(Long initialCompanyId, Long initialEmployeeId, Long initialFiscalPeriodId, 
                  String initialPayslipNumber, BigDecimal initialBasicSalary) {
        this();
        this.companyId = initialCompanyId;
        this.employeeId = initialEmployeeId;
        this.fiscalPeriodId = initialFiscalPeriodId;
        this.payslipNumber = initialPayslipNumber;
        this.basicSalary = initialBasicSalary;
        this.grossSalary = initialBasicSalary;
        calculateTotals();
    }
    
    // Business methods
    public void calculateTotals() {
        // Calculate total earnings - handle null grossSalary
        BigDecimal baseSalary = (grossSalary != null) ? grossSalary : BigDecimal.ZERO;
        this.totalEarnings = baseSalary
            .add(housingAllowance != null ? housingAllowance : BigDecimal.ZERO)
            .add(transportAllowance != null ? transportAllowance : BigDecimal.ZERO)
            .add(medicalAllowance != null ? medicalAllowance : BigDecimal.ZERO)
            .add(otherAllowances != null ? otherAllowances : BigDecimal.ZERO)
            .add(commission != null ? commission : BigDecimal.ZERO)
            .add(bonus != null ? bonus : BigDecimal.ZERO)
            .add(overtimeAmount != null ? overtimeAmount : BigDecimal.ZERO);
        
        // Calculate total deductions
        this.totalDeductions = (payeTax != null ? payeTax : BigDecimal.ZERO)
            .add(uifEmployee != null ? uifEmployee : BigDecimal.ZERO)
            .add(medicalAid != null ? medicalAid : BigDecimal.ZERO)
            .add(pensionFund != null ? pensionFund : BigDecimal.ZERO)
            .add(loanDeduction != null ? loanDeduction : BigDecimal.ZERO)
            .add(otherDeductions != null ? otherDeductions : BigDecimal.ZERO);
        
        // Calculate net pay
        this.netPay = totalEarnings.subtract(totalDeductions);
    }
    
    public boolean canBeApproved() {
        return status == PayslipStatus.DRAFT;
    }
    
    public boolean canBePaid() {
        return status == PayslipStatus.APPROVED;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long newEmployeeId) { this.employeeId = newEmployeeId; }
    
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long newFiscalPeriodId) { this.fiscalPeriodId = newFiscalPeriodId; }
    
    public String getPayslipNumber() { return payslipNumber; }
    public void setPayslipNumber(String newPayslipNumber) { this.payslipNumber = newPayslipNumber; }
    
    // Salary Information
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal newBasicSalary) { 
        this.basicSalary = newBasicSalary;
        calculateTotals();
    }
    
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal newOvertimeHours) { 
        this.overtimeHours = newOvertimeHours;
        calculateTotals();
    }
    
    public BigDecimal getOvertimeAmount() { return overtimeAmount; }
    public void setOvertimeAmount(BigDecimal newOvertimeAmount) { 
        this.overtimeAmount = newOvertimeAmount;
        calculateTotals();
    }
    
    // Earnings
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal newGrossSalary) { 
        this.grossSalary = newGrossSalary;
        calculateTotals();
    }
    
    public BigDecimal getHousingAllowance() { return housingAllowance; }
    public void setHousingAllowance(BigDecimal newHousingAllowance) { 
        this.housingAllowance = newHousingAllowance;
        calculateTotals();
    }
    
    public BigDecimal getTransportAllowance() { return transportAllowance; }
    public void setTransportAllowance(BigDecimal newTransportAllowance) { 
        this.transportAllowance = newTransportAllowance;
        calculateTotals();
    }
    
    public BigDecimal getMedicalAllowance() { return medicalAllowance; }
    public void setMedicalAllowance(BigDecimal newMedicalAllowance) { 
        this.medicalAllowance = newMedicalAllowance;
        calculateTotals();
    }
    
    public BigDecimal getOtherAllowances() { return otherAllowances; }
    public void setOtherAllowances(BigDecimal newOtherAllowances) { 
        this.otherAllowances = newOtherAllowances;
        calculateTotals();
    }
    
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal newCommission) { 
        this.commission = newCommission;
        calculateTotals();
    }
    
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal newBonus) { 
        this.bonus = newBonus;
        calculateTotals();
    }
    
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal newTotalEarnings) { this.totalEarnings = newTotalEarnings; }
    
    // Deductions
    public BigDecimal getPayeeTax() { return payeTax; }
    public void setPayeeTax(BigDecimal newPayeeTax) { 
        this.payeTax = newPayeeTax;
        calculateTotals();
    }
    
    public BigDecimal getUifEmployee() { return uifEmployee; }
    public void setUifEmployee(BigDecimal newUifEmployee) { 
        this.uifEmployee = newUifEmployee;
        calculateTotals();
    }
    
    public BigDecimal getUifEmployer() { return uifEmployer; }
    public void setUifEmployer(BigDecimal newUifEmployer) { this.uifEmployer = newUifEmployer; }
    
    public BigDecimal getSdlLevy() { return sdlLevy; }
    public void setSdlLevy(BigDecimal newSdlLevy) { this.sdlLevy = newSdlLevy; }
    
    public BigDecimal getMedicalAid() { return medicalAid; }
    public void setMedicalAid(BigDecimal newMedicalAid) { 
        this.medicalAid = newMedicalAid;
        calculateTotals();
    }
    
    public BigDecimal getPensionFund() { return pensionFund; }
    public void setPensionFund(BigDecimal newPensionFund) { 
        this.pensionFund = newPensionFund;
        calculateTotals();
    }
    
    public BigDecimal getLoanDeduction() { return loanDeduction; }
    public void setLoanDeduction(BigDecimal newLoanDeduction) { 
        this.loanDeduction = newLoanDeduction;
        calculateTotals();
    }
    
    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal newOtherDeductions) { 
        this.otherDeductions = newOtherDeductions;
        calculateTotals();
    }
    
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal newTotalDeductions) { this.totalDeductions = newTotalDeductions; }
    
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal newNetPay) { this.netPay = newNetPay; }
    
    // Tax Certificate Info
    public BigDecimal getAnnualGross() { return annualGross; }
    public void setAnnualGross(BigDecimal newAnnualGross) { this.annualGross = newAnnualGross; }
    
    public BigDecimal getAnnualPaye() { return annualPaye; }
    public void setAnnualPaye(BigDecimal newAnnualPaye) { this.annualPaye = newAnnualPaye; }
    
    public BigDecimal getAnnualUif() { return annualUif; }
    public void setAnnualUif(BigDecimal newAnnualUif) { this.annualUif = newAnnualUif; }
    
    // Status and Payment
    public PayslipStatus getStatus() { return status; }
    public void setStatus(PayslipStatus newStatus) { this.status = newStatus; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod newPaymentMethod) { this.paymentMethod = newPaymentMethod; }
    
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate newPaymentDate) { this.paymentDate = newPaymentDate; }
    
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String newPaymentReference) { this.paymentReference = newPaymentReference; }
    
    // Audit Fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime newUpdatedAt) { this.updatedAt = newUpdatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String newCreatedBy) { this.createdBy = newCreatedBy; }
    
    public BigDecimal getPaye() {
        return payeTax;
    }

    public void setPaye(BigDecimal paye) {
        this.payeTax = paye;
        calculateTotals();
    }

    public BigDecimal getUif() {
        return uifEmployee;
    }

    public void setUif(BigDecimal uif) {
        this.uifEmployee = uif;
        calculateTotals();
    }

    public BigDecimal getSdl() {
        return sdlLevy;
    }

    public void setSdl(BigDecimal sdl) {
        this.sdlLevy = sdl;
    }

    public BigDecimal getNetSalary() {
        return netPay;
    }

    public void setNetSalary(BigDecimal netSalary) {
        this.netPay = netSalary;
    }
}
