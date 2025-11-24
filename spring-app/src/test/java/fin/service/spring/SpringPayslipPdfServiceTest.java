package fin.service.spring;

import fin.model.Company;
import fin.model.Employee;
import fin.model.FiscalPeriod;
import fin.model.Payslip;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class SpringPayslipPdfServiceTest {

    @Test
    public void generatePayslipPdf_withUnicodeCharacters_shouldReturnPdfBytes() {
        SpringPayslipPdfService service = new SpringPayslipPdfService();

        Payslip payslip = new Payslip(1L, 1L, 1L, "PSL-TEST-UNICODE", new BigDecimal("1000"));
        Employee employee = new Employee(1L, "EMP-1", "José", "Müller", "Developer", new BigDecimal("1000"));
        Company company = new Company();
        company.setName("Acme's Café – München");
        FiscalPeriod fiscalPeriod = new FiscalPeriod();
        // intentionally leave payDate null to check N/A behavior

        byte[] pdfBytes = service.generatePayslipPdf(payslip, employee, company, fiscalPeriod);
        assertNotNull(pdfBytes, "PDF bytes should not be null");
        assertTrue(pdfBytes.length > 0, "PDF bytes should have positive length");
        String header = new String(pdfBytes, 0, Math.min(8, pdfBytes.length));
        assertTrue(header.startsWith("%PDF-"), "PDF header missing: " + header);
    }
}
