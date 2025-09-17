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
    
    public PayrollPeriod(Long companyId, String periodName, LocalDate startDate, 
                        LocalDate endDate, LocalDate payDate) {
        this();
        this.companyId = companyId;
        this.periodName = periodName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.payDate = payDate;
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
    public void setId(Long id) { this.id = id; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate payDate) { this.payDate = payDate; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType periodType) { this.periodType = periodType; }
    
    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }
    
    public BigDecimal getTotalGrossPay() { return totalGrossPay; }
    public void setTotalGrossPay(BigDecimal totalGrossPay) { this.totalGrossPay = totalGrossPay; }
    
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal totalDeductions) { this.totalDeductions = totalDeductions; }
    
    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(BigDecimal totalNetPay) { this.totalNetPay = totalNetPay; }
    
    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer employeeCount) { this.employeeCount = employeeCount; }
    
    // Approval fields
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    
    // Audit fields
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
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
