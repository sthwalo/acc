package fin.dto;

import java.util.List;

/**
 * Request DTO for payslip generation
 */
public class PayslipGenerationRequest {
    private Long fiscalPeriodId;
    private List<Long> employeeIds;

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public List<Long> getEmployeeIds() { return employeeIds; }
    public void setEmployeeIds(List<Long> employeeIds) { this.employeeIds = employeeIds; }
}