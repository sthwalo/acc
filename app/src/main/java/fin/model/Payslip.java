/*
 * FIN Financial Management System - Payroll Module
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 */

package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Payslip entity representing an employee's payslip for a specific period
 */
public class Payslip {
    private Long id;
    private Long companyId;
    private Long employeeId;
    private Long payrollPeriodId;
    private String payslipNumber;
    
    // Salary Information
    private BigDecimal basicSalary;
    private BigDecimal overtimeHours = BigDecimal.ZERO;
    private BigDecimal overtimeAmount = BigDecimal.ZERO;
    
    // Earnings
    private BigDecimal grossSalary;
    private BigDecimal housingAllowance = BigDecimal.ZERO;
    private BigDecimal transportAllowance = BigDecimal.ZERO;
    private BigDecimal medicalAllowance = BigDecimal.ZERO;
    private BigDecimal otherAllowances = BigDecimal.ZERO;
    private BigDecimal commission = BigDecimal.ZERO;
    private BigDecimal bonus = BigDecimal.ZERO;
    private BigDecimal totalEarnings;
    
    // Statutory Deductions
    private BigDecimal payeeTax = BigDecimal.ZERO;
    private BigDecimal uifEmployee = BigDecimal.ZERO;
    private BigDecimal uifEmployer = BigDecimal.ZERO;
    private BigDecimal sdlLevy = BigDecimal.ZERO;  // Skills Development Levy (1% of gross for employers)
    
    // Other Deductions
    private BigDecimal medicalAid = BigDecimal.ZERO;
    private BigDecimal pensionFund = BigDecimal.ZERO;
    private BigDecimal loanDeduction = BigDecimal.ZERO;
    private BigDecimal otherDeductions = BigDecimal.ZERO;
    private BigDecimal totalDeductions;
    
    // Net Pay
    private BigDecimal netPay;
    
    // Tax Certificate Info
    private BigDecimal annualGross;
    private BigDecimal annualPaye;
    private BigDecimal annualUif;
    
    // Status
    private PayslipStatus status = PayslipStatus.DRAFT;
    private PaymentMethod paymentMethod = PaymentMethod.EFT;
    private LocalDate paymentDate;
    private String paymentReference;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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
    
    public Payslip(Long initialCompanyId, Long initialEmployeeId, Long initialPayrollPeriodId, 
                  String initialPayslipNumber, BigDecimal initialBasicSalary) {
        this();
        this.companyId = initialCompanyId;
        this.employeeId = initialEmployeeId;
        this.payrollPeriodId = initialPayrollPeriodId;
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
        this.totalDeductions = (payeeTax != null ? payeeTax : BigDecimal.ZERO)
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
    
    public Long getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(Long newPayrollPeriodId) { this.payrollPeriodId = newPayrollPeriodId; }
    
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
    public BigDecimal getPayeeTax() { return payeeTax; }
    public void setPayeeTax(BigDecimal newPayeeTax) { 
        this.payeeTax = newPayeeTax;
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
    
    @Override
    public String toString() {
        return "Payslip{" +
                "id=" + id +
                ", payslipNumber='" + payslipNumber + '\'' +
                ", employeeId=" + employeeId +
                ", totalEarnings=" + totalEarnings +
                ", totalDeductions=" + totalDeductions +
                ", netPay=" + netPay +
                ", status=" + status +
                '}';
    }
}
