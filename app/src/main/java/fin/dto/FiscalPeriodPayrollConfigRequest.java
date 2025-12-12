package fin.dto;

import fin.entity.FiscalPeriod;

/**
 * Request DTO for fiscal period payroll configuration
 */
public class FiscalPeriodPayrollConfigRequest {
    private Long fiscalPeriodId;
    private java.time.LocalDate payDate;
    private FiscalPeriod.PeriodType periodType;
    private FiscalPeriod.PayrollStatus payrollStatus;

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public java.time.LocalDate getPayDate() { return payDate; }
    public void setPayDate(java.time.LocalDate payDate) { this.payDate = payDate; }

    public FiscalPeriod.PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(FiscalPeriod.PeriodType periodType) { this.periodType = periodType; }

    public FiscalPeriod.PayrollStatus getPayrollStatus() { return payrollStatus; }
    public void setPayrollStatus(FiscalPeriod.PayrollStatus payrollStatus) { this.payrollStatus = payrollStatus; }
}