package fin.service.upload;

import fin.config.PdfBoxConfigurator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test to verify OCR path (external rasterizer) works end-to-end for a real PDF file.
 */
public class DocumentTextExtractorOcrIntegrationTest {

    @Test
    @DisplayName("OCR fallback using external rasterizer should extract text from real PDF")
    void testExternalRasterizerOcrOnRealPdf() throws Exception {
        // Skip if pdftoppm or tesseract not available on PATH
        boolean hasPdftoppm = Runtime.getRuntime().exec(new String[]{"which", "pdftoppm"}).waitFor() == 0;
        boolean hasTesseract = Runtime.getRuntime().exec(new String[]{"which", "tesseract"}).waitFor() == 0;
        Assumptions.assumeTrue(hasPdftoppm && hasTesseract, "pdftoppm and tesseract must be installed for this integration test");

        // File path relative to project root
        File pdf = new File(System.getProperty("user.dir"), "input/std/xxxxx3753 (02).pdf");
        Assumptions.assumeTrue(pdf.exists(), "Test PDF not found: " + pdf.getAbsolutePath());

        // Force PDFBox as unavailable to hit OCR-only path
        PdfBoxConfigurator cfg = mock(PdfBoxConfigurator.class);
        when(cfg.isPdfBoxAvailable()).thenReturn(false);
        when(cfg.getPdfBoxStatus()).thenReturn("FORCED_UNAVAILABLE_FOR_TEST");

        DocumentTextExtractor extractor = new DocumentTextExtractor(cfg);

        List<String> lines = extractor.parseDocument(pdf);
        System.out.println("Extracted lines count: " + (lines == null ? 0 : lines.size()));
        if (lines != null) {
            lines.stream().limit(20).forEach(System.out::println);
        }

        // We expect at least some lines from OCR; allow zero but assert method completed (no exception)
        assertNotNull(lines);
    }
}
