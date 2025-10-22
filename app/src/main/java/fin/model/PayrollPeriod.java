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
 * PayrollPeriod entity representing a payroll processing period
 */
public class PayrollPeriod {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String periodName;
    private LocalDate payDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodType periodType = PeriodType.MONTHLY;
    private PayrollStatus status = PayrollStatus.OPEN;
    private BigDecimal totalGrossPay = BigDecimal.ZERO;
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    private BigDecimal totalNetPay = BigDecimal.ZERO;
    private Integer employeeCount = 0;
    
    // Approval Workflow
    private LocalDateTime processedAt;
    private String processedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    
    // Audit Fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    // Enums
    public enum PeriodType {
        WEEKLY, MONTHLY, QUARTERLY
    }
    
    public enum PayrollStatus {
        OPEN, PROCESSED, APPROVED, PAID, CLOSED
    }
    
    // Constructors
    public PayrollPeriod() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public PayrollPeriod(Long initialCompanyId, String initialPeriodName, LocalDate initialStartDate, 
                        LocalDate initialEndDate, LocalDate initialPayDate) {
        this();
        this.companyId = initialCompanyId;
        this.periodName = initialPeriodName;
        this.startDate = initialStartDate;
        this.endDate = initialEndDate;
        this.payDate = initialPayDate;
    }
    
    // Business methods
    public boolean canBeProcessed() {
        return status == PayrollStatus.OPEN;
    }
    
    public boolean canBeApproved() {
        return status == PayrollStatus.PROCESSED;
    }
    
    public boolean isActive() {
        return status != PayrollStatus.CLOSED;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long newFiscalPeriodId) { this.fiscalPeriodId = newFiscalPeriodId; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String newPeriodName) { this.periodName = newPeriodName; }
    
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate newPayDate) { this.payDate = newPayDate; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate newStartDate) { this.startDate = newStartDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate newEndDate) { this.endDate = newEndDate; }
    
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType newPeriodType) { this.periodType = newPeriodType; }
    
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus newStatus) { this.status = newStatus; }
    
    public BigDecimal getTotalGrossPay() { return totalGrossPay; }
    public void setTotalGrossPay(BigDecimal newTotalGrossPay) { this.totalGrossPay = newTotalGrossPay; }
    
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal newTotalDeductions) { this.totalDeductions = newTotalDeductions; }
    
    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(BigDecimal newTotalNetPay) { this.totalNetPay = newTotalNetPay; }
    
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer newEmployeeCount) { this.employeeCount = newEmployeeCount; }
    
    // Approval fields
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime newProcessedAt) { this.processedAt = newProcessedAt; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String newProcessedBy) { this.processedBy = newProcessedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime newApprovedAt) { this.approvedAt = newApprovedAt; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String newApprovedBy) { this.approvedBy = newApprovedBy; }
    
    // Audit fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime newUpdatedAt) { this.updatedAt = newUpdatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String newCreatedBy) { this.createdBy = newCreatedBy; }
    
    @Override
    public String toString() {
        return "PayrollPeriod{" +
                "id=" + id +
                ", periodName='" + periodName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", payDate=" + payDate +
                ", status=" + status +
                ", totalNetPay=" + totalNetPay +
                ", employeeCount=" + employeeCount +
                '}';
    }
}
