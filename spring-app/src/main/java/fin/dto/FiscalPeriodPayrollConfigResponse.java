package fin.dto;

import fin.model.FiscalPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for fiscal period payroll configuration
 */
public class FiscalPeriodPayrollConfigResponse {
    private Long fiscalPeriodId;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private FiscalPeriod.PeriodType periodType;
    private FiscalPeriod.PayrollStatus payrollStatus;
    private BigDecimal totalGrossPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalNetPay;
    private Integer employeeCount;
    private boolean isClosed;

    // Constructor from FiscalPeriod
    public FiscalPeriodPayrollConfigResponse(FiscalPeriod fiscalPeriod) {
        this.fiscalPeriodId = fiscalPeriod.getId();
        this.periodName = fiscalPeriod.getPeriodName();
        this.startDate = fiscalPeriod.getStartDate();
        this.endDate = fiscalPeriod.getEndDate();
        this.payDate = fiscalPeriod.getPayDate();
        this.periodType = fiscalPeriod.getPeriodType();
        this.payrollStatus = fiscalPeriod.getPayrollStatus();
        this.totalGrossPay = fiscalPeriod.getTotalGrossPay();
        this.totalDeductions = fiscalPeriod.getTotalDeductions();
        this.totalNetPay = fiscalPeriod.getTotalNetPay();
        this.employeeCount = fiscalPeriod.getEmployeeCount();
        this.isClosed = fiscalPeriod.isClosed();
    }

    // Getters
    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public String getPeriodName() { return periodName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getPayDate() { return payDate; }
    public FiscalPeriod.PeriodType getPeriodType() { return periodType; }
    public FiscalPeriod.PayrollStatus getPayrollStatus() { return payrollStatus; }
    public BigDecimal getTotalGrossPay() { return totalGrossPay; }
    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public Integer getEmployeeCount() { return employeeCount; }
    public boolean isClosed() { return isClosed; }
}