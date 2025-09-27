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
    
    public Payslip(Long companyId, Long employeeId, Long payrollPeriodId, 
                  String payslipNumber, BigDecimal basicSalary) {
        this();
        this.companyId = companyId;
        this.employeeId = employeeId;
        this.payrollPeriodId = payrollPeriodId;
        this.payslipNumber = payslipNumber;
        this.basicSalary = basicSalary;
        this.grossSalary = basicSalary;
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
    public void setId(Long id) { this.id = id; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    
    public Long getPayrollPeriodId() { return payrollPeriodId; }
    public void setPayrollPeriodId(Long payrollPeriodId) { this.payrollPeriodId = payrollPeriodId; }
    
    public String getPayslipNumber() { return payslipNumber; }
    public void setPayslipNumber(String payslipNumber) { this.payslipNumber = payslipNumber; }
    
    // Salary Information
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { 
        this.basicSalary = basicSalary;
        calculateTotals();
    }
    
    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { 
        this.overtimeHours = overtimeHours;
        calculateTotals();
    }
    
    public BigDecimal getOvertimeAmount() { return overtimeAmount; }
    public void setOvertimeAmount(BigDecimal overtimeAmount) { 
        this.overtimeAmount = overtimeAmount;
        calculateTotals();
    }
    
    // Earnings
    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal grossSalary) { 
        this.grossSalary = grossSalary;
        calculateTotals();
    }
    
    public BigDecimal getHousingAllowance() { return housingAllowance; }
    public void setHousingAllowance(BigDecimal housingAllowance) { 
        this.housingAllowance = housingAllowance;
        calculateTotals();
    }
    
    public BigDecimal getTransportAllowance() { return transportAllowance; }
    public void setTransportAllowance(BigDecimal transportAllowance) { 
        this.transportAllowance = transportAllowance;
        calculateTotals();
    }
    
    public BigDecimal getMedicalAllowance() { return medicalAllowance; }
    public void setMedicalAllowance(BigDecimal medicalAllowance) { 
        this.medicalAllowance = medicalAllowance;
        calculateTotals();
    }
    
    public BigDecimal getOtherAllowances() { return otherAllowances; }
    public void setOtherAllowances(BigDecimal otherAllowances) { 
        this.otherAllowances = otherAllowances;
        calculateTotals();
    }
    
    public BigDecimal getCommission() { return commission; }
    public void setCommission(BigDecimal commission) { 
        this.commission = commission;
        calculateTotals();
    }
    
    public BigDecimal getBonus() { return bonus; }
    public void setBonus(BigDecimal bonus) { 
        this.bonus = bonus;
        calculateTotals();
    }
    
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }
    
    // Deductions
    public BigDecimal getPayeeTax() { return payeeTax; }
    public void setPayeeTax(BigDecimal payeeTax) { 
        this.payeeTax = payeeTax;
        calculateTotals();
    }
    
    public BigDecimal getUifEmployee() { return uifEmployee; }
    public void setUifEmployee(BigDecimal uifEmployee) { 
        this.uifEmployee = uifEmployee;
        calculateTotals();
    }
    
    public BigDecimal getUifEmployer() { return uifEmployer; }
    public void setUifEmployer(BigDecimal uifEmployer) { this.uifEmployer = uifEmployer; }
    
    public BigDecimal getMedicalAid() { return medicalAid; }
    public void setMedicalAid(BigDecimal medicalAid) { 
        this.medicalAid = medicalAid;
        calculateTotals();
    }
    
    public BigDecimal getPensionFund() { return pensionFund; }
    public void setPensionFund(BigDecimal pensionFund) { 
        this.pensionFund = pensionFund;
        calculateTotals();
    }
    
    public BigDecimal getLoanDeduction() { return loanDeduction; }
    public void setLoanDeduction(BigDecimal loanDeduction) { 
        this.loanDeduction = loanDeduction;
        calculateTotals();
    }
    
    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal otherDeductions) { 
        this.otherDeductions = otherDeductions;
        calculateTotals();
    }
    
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }
    
    // Tax Certificate Info
    public BigDecimal getAnnualGross() { return annualGross; }
    public void setAnnualGross(BigDecimal annualGross) { this.annualGross = annualGross; }
    
    public BigDecimal getAnnualPaye() { return annualPaye; }
    public void setAnnualPaye(BigDecimal annualPaye) { this.annualPaye = annualPaye; }
    
    public BigDecimal getAnnualUif() { return annualUif; }
    public void setAnnualUif(BigDecimal annualUif) { this.annualUif = annualUif; }
    
    // Status and Payment
    public PayslipStatus getStatus() { return status; }
    public void setStatus(PayslipStatus status) { this.status = status; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }
    
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    
    // Audit Fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
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
