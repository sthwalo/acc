package fin.dto;

import java.util.List;

/**
 * Request DTO for sending payslips by email
 */
public class EmailPayslipsRequest {
    private List<Long> payslipIds;

    public List<Long> getPayslipIds() { return payslipIds; }
    public void setPayslipIds(List<Long> payslipIds) { this.payslipIds = payslipIds; }
}