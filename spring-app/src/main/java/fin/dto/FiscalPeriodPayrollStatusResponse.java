package fin.dto;

import fin.model.FiscalPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for fiscal period payroll status list
 */
public class FiscalPeriodPayrollStatusResponse {
    private Long id;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate payDate;
    private FiscalPeriod.PayrollStatus payrollStatus;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private Integer employeeCount;
    private boolean isClosed;

    // Constructor from FiscalPeriod
    public FiscalPeriodPayrollStatusResponse(FiscalPeriod fiscalPeriod) {
        this.id = fiscalPeriod.getId();
        this.periodName = fiscalPeriod.getPeriodName();
        this.startDate = fiscalPeriod.getStartDate();
        this.endDate = fiscalPeriod.getEndDate();
        this.payDate = fiscalPeriod.getPayDate();
        this.payrollStatus = fiscalPeriod.getPayrollStatus();
        this.totalGrossPay = fiscalPeriod.getTotalGrossPay();
        this.totalNetPay = fiscalPeriod.getTotalNetPay();
        this.employeeCount = fiscalPeriod.getEmployeeCount();
        this.isClosed = fiscalPeriod.isClosed();
    }

    // Getters
    public Long getId() { return id; }
    public String getPeriodName() { return periodName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public LocalDate getPayDate() { return payDate; }
    public FiscalPeriod.PayrollStatus getPayrollStatus() { return payrollStatus; }
    public BigDecimal getTotalGrossPay() { return totalGrossPay; }
    public BigDecimal getTotalNetPay() { return totalNetPay; }
    public Integer getEmployeeCount() { return employeeCount; }
    public boolean isClosed() { return isClosed; }
}