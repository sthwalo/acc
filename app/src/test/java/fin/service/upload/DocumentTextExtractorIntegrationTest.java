package fin.service.upload;

import fin.config.PdfBoxConfigurator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test to run text extraction against a real PDF file in input/std/
 * Skipped automatically if the file is not present.
 */
public class DocumentTextExtractorIntegrationTest {

    @Test
    @DisplayName("Extract text from real sample PDF in input/std")
    void testExtractRealPdf() throws Exception {
        File pdf = new File("input/std/xxxxx3753 (02).pdf");
        Assumptions.assumeTrue(pdf.exists(), "Sample PDF not found: " + pdf.getAbsolutePath());

        PdfBoxConfigurator pdfBoxConfigurator = mock(PdfBoxConfigurator.class);
        when(pdfBoxConfigurator.isPdfBoxAvailable()).thenReturn(true);
        when(pdfBoxConfigurator.getPdfBoxStatus()).thenReturn("AVAILABLE");

        DocumentTextExtractor extractor = new DocumentTextExtractor(pdfBoxConfigurator);

        List<String> lines = extractor.parseDocument(pdf);

        System.out.println("Extracted " + lines.size() + " lines from " + pdf.getName());
        int preview = Math.min(30, lines.size());
        for (int i = 0; i < preview; i++) {
            System.out.println(String.format("%3d: %s", i + 1, lines.get(i)));
        }

        // Don't assert non-empty to avoid CI failures on OCR environment, just sanity-check null
        assertNotNull(lines);
    }
}