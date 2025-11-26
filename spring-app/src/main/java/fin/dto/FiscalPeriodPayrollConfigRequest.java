package fin.dto;

/**
 * Request DTO for fiscal period payroll configuration
 */
public class FiscalPeriodPayrollConfigRequest {
    private Long fiscalPeriodId;
    private java.time.LocalDate payDate;
    private fin.model.FiscalPeriod.PeriodType periodType;
    private fin.model.FiscalPeriod.PayrollStatus payrollStatus;

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public java.time.LocalDate getPayDate() { return payDate; }
    public void setPayDate(java.time.LocalDate payDate) { this.payDate = payDate; }

    public fin.model.FiscalPeriod.PeriodType getPeriodType() { return periodType; }
    public void setPeriodType(fin.model.FiscalPeriod.PeriodType periodType) { this.periodType = periodType; }

    public fin.model.FiscalPeriod.PayrollStatus getPayrollStatus() { return payrollStatus; }
    public void setPayrollStatus(fin.model.FiscalPeriod.PayrollStatus payrollStatus) { this.payrollStatus = payrollStatus; }
}