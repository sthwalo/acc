package fin.dto;

/**
 * Request DTO for payroll processing
 */
public class PayrollProcessRequest {
    private Long fiscalPeriodId;
    private boolean reprocess;

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public boolean isReprocess() { return reprocess; }
    public void setReprocess(boolean reprocess) { this.reprocess = reprocess; }
}