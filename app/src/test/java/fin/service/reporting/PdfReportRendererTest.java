package fin.service.reporting;

import fin.dto.AuditTrailDTO;
import fin.dto.AuditTrailLineDTO;
import fin.entity.Company;
import fin.entity.FiscalPeriod;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PdfReportRendererTest {

    @Test
    public void renderAuditTrailProducesReadablePdf() throws Exception {
        Company c = new Company("Test Company");
        FiscalPeriod p = new FiscalPeriod(1L, "FY2025-2026", LocalDate.of(2025,3,1), LocalDate.of(2026,2,28));

        AuditTrailLineDTO l1 = new AuditTrailLineDTO("1000","Cash","Payment received", new BigDecimal("0"), new BigDecimal("100.00"));
        AuditTrailLineDTO l2 = new AuditTrailLineDTO("2000","Revenue","Sale invoice number 12345", new BigDecimal("100.00"), new BigDecimal("0"));

        AuditTrailDTO entry = new AuditTrailDTO("INV-001", LocalDateTime.of(2025,11,3,0,0), "Invoice: Sale", "FIN", LocalDateTime.of(2025,11,5,0,0), List.of(l1, l2));

        byte[] pdf = PdfReportRenderer.renderAuditTrail(List.of(entry), c, p);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);

        // Validate basic PDF document metadata and content
        // Check raw bytes for fragments
        String raw = new String(pdf, java.nio.charset.StandardCharsets.ISO_8859_1);
        assertTrue(raw.contains("AUDIT TRAIL") || raw.contains("Audit Trail"), "PDF content should contain 'AUDIT TRAIL'");
        assertTrue(raw.contains("INV-001") || raw.contains("ENTRY: INV-001"), "PDF content should contain reference 'INV-001'");
        assertTrue(raw.contains("1000"), "PDF content should contain account code '1000'");

        // Validate metadata fields are present in raw PDF bytes (info dictionary is embedded in PDF)
        assertTrue(raw.contains("/Title (Audit Trail - Test Company)") || raw.contains("Audit Trail - Test Company"), "PDF should include title metadata");
        assertTrue(raw.contains("/Subject (Audit Trail for FY2025-2026)") || raw.contains("Audit Trail for FY2025-2026"), "PDF should include subject metadata");
        assertTrue(raw.contains("/Author (FIN Financial Management System)") || raw.contains("FIN Financial Management System"), "PDF should include author metadata");
    }
}
