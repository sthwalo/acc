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

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Table(name = "fiscal_periods")
public class FiscalPeriod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "name", nullable = false)
    private String periodName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed;

    // ===== PAYROLL-SPECIFIC FIELDS =====
    @Column(name = "pay_date")
    private LocalDate payDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type")
    private PeriodType periodType = PeriodType.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "payroll_status")
    private PayrollStatus payrollStatus = PayrollStatus.OPEN;

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

    // ===== AUDIT FIELDS =====
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    // ===== ENUMS =====
    public enum PeriodType {
        WEEKLY, MONTHLY, QUARTERLY
    }

    public enum PayrollStatus {
        OPEN, PROCESSED, APPROVED, PAID, CLOSED
    }

    // Constructors, getters, and setters
    public FiscalPeriod() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public FiscalPeriod(Long initialCompanyId, String initialPeriodName, LocalDate initialStartDate, LocalDate initialEndDate) {
        this();
        this.companyId = initialCompanyId;
        this.periodName = initialPeriodName;
        this.startDate = initialStartDate;
        this.endDate = initialEndDate;
        this.isClosed = false;
    }

    /**
     * Copy constructor for defensive copying.
     * Creates a deep copy of all FiscalPeriod fields to prevent external modification.
     */
    public FiscalPeriod(FiscalPeriod other) {
        if (other == null) {
            return;
        }

        this.id = other.id;
        this.companyId = other.companyId;
        this.periodName = other.periodName;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.isClosed = other.isClosed;
        this.payDate = other.payDate;
        this.periodType = other.periodType;
        this.payrollStatus = other.payrollStatus;
        this.totalGrossPay = other.totalGrossPay;
        this.totalDeductions = other.totalDeductions;
        this.totalNetPay = other.totalNetPay;
        this.employeeCount = other.employeeCount;
        this.processedAt = other.processedAt;
        this.processedBy = other.processedBy;
        this.approvedAt = other.approvedAt;
        this.approvedBy = other.approvedBy;
        this.createdAt = other.createdAt;
        this.updatedAt = other.updatedAt;
        this.createdBy = other.createdBy;
    }

    // ===== BUSINESS METHODS =====

    /**
     * Check if a given date falls within this fiscal period
     */
    public boolean containsDate(LocalDate date) {
        return (date.isEqual(startDate) || date.isAfter(startDate)) &&
               (date.isEqual(endDate) || date.isBefore(endDate));
    }

    /**
     * Check if this period can be used for payroll processing
     */
    public boolean canBeProcessed() {
        return (payrollStatus == PayrollStatus.OPEN || payrollStatus == PayrollStatus.PROCESSED) && !isClosed;
    }

    /**
     * Check if this period can be reprocessed (already processed but not closed)
     */
    public boolean canBeReprocessed() {
        return payrollStatus == PayrollStatus.PROCESSED && !isClosed;
    }

    /**
     * Check if this period can be approved
     */
    public boolean canBeApproved() {
        return payrollStatus == PayrollStatus.PROCESSED;
    }

    /**
     * Check if payroll is active for this period
     */
    public boolean isPayrollActive() {
        return payrollStatus != PayrollStatus.CLOSED;
    }

    /**
     * Check if payroll has been processed
     */
    public boolean isPayrollProcessed() {
        return payrollStatus == PayrollStatus.PROCESSED ||
               payrollStatus == PayrollStatus.APPROVED ||
               payrollStatus == PayrollStatus.PAID ||
               payrollStatus == PayrollStatus.CLOSED;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }

    public String getPeriodName() { return periodName; }
    public void setPeriodName(String newPeriodName) { this.periodName = newPeriodName; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate newStartDate) { this.startDate = newStartDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate newEndDate) { this.endDate = newEndDate; }

    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }

    // Payroll-specific getters and setters
    public LocalDate getPayDate() { return payDate; }
    public void setPayDate(LocalDate newPayDate) { this.payDate = newPayDate; }

    public PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(PeriodType newPeriodType) { this.periodType = newPeriodType; }

    public PayrollStatus getPayrollStatus() { return payrollStatus; }
    public void setPayrollStatus(PayrollStatus newPayrollStatus) { this.payrollStatus = newPayrollStatus; }

    public BigDecimal getTotalGrossPay() { return totalGrossPay; }
    public void setTotalGrossPay(BigDecimal newTotalGrossPay) { this.totalGrossPay = newTotalGrossPay; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal newTotalDeductions) { this.totalDeductions = newTotalDeductions; }

    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public void setTotalNetPay(BigDecimal newTotalNetPay) { this.totalNetPay = newTotalNetPay; }

    public Integer getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(Integer newEmployeeCount) { this.employeeCount = newEmployeeCount; }

    // Approval workflow
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

    // Legacy compatibility methods
    public LocalDate getPaymentDate() { return payDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.payDate = paymentDate; }

    public boolean isProcessed() { return isPayrollProcessed(); }
    public void setProcessed(boolean processed) {
        if (processed) {
            this.payrollStatus = PayrollStatus.PROCESSED;
        } else {
            this.payrollStatus = PayrollStatus.OPEN;
        }
    }

    @Override
    public String toString() {
        return "FiscalPeriod{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", periodName='" + periodName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", payDate=" + payDate +
                ", isClosed=" + isClosed +
                ", payrollStatus=" + payrollStatus +
                ", totalGrossPay=" + totalGrossPay +
                ", employeeCount=" + employeeCount +
                '}';
    }
}
