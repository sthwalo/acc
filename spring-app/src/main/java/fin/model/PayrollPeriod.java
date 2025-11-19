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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * PayrollPeriod entity representing a payroll processing period
 */
@Entity
@Table(name = "payroll_periods")
public class PayrollPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "company_id")
    private Long companyId;
    
    @Column(name = "fiscal_period_id")
    private Long fiscalPeriodId;
    
    @Column(name = "period_name")
    private String periodName;
    
    @Column(name = "pay_date")
    private LocalDate payDate;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private PeriodType periodType = PeriodType.MONTHLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PayrollStatus status = PayrollStatus.OPEN;
    
    @Column(name = "total_gross_pay")
    private BigDecimal totalGrossPay = BigDecimal.ZERO;
    
    @Column(name = "total_deductions")
    private BigDecimal totalDeductions = BigDecimal.ZERO;
    
    @Column(name = "total_net_pay")
    private BigDecimal totalNetPay = BigDecimal.ZERO;
    
    @Column(name = "employee_count")
    private Integer employeeCount = 0;
    
    // Approval Workflow
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "processed_by")
    private String processedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    // Audit Fields
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
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
    
    public LocalDate getPaymentDate() {
        return payDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.payDate = paymentDate;
    }

    public boolean isProcessed() {
        return status == PayrollStatus.PROCESSED || status == PayrollStatus.APPROVED || status == PayrollStatus.PAID || status == PayrollStatus.CLOSED;
    }

    public void setProcessed(boolean processed) {
        if (processed) {
            this.status = PayrollStatus.PROCESSED;
        } else {
            this.status = PayrollStatus.OPEN;
        }
    }
}
