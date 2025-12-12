package fin.dto;

import java.util.List;

/**
 * Request DTO for bulk PDF generation
 */
public class BulkPdfRequest {
    private List<Long> payslipIds;

    public List<Long> getPayslipIds() { return payslipIds; }
    public void setPayslipIds(List<Long> payslipIds) { this.payslipIds = payslipIds; }
}