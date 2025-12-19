package fin.service.upload;

import fin.config.PdfBoxConfigurator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Run OCR fallback runner test")
public class DocumentTextExtractorOcrRunnerTest {

    @Test
    void runOcrFallback() throws Exception {
        File pdf = new File(System.getProperty("user.dir"), "input/std/xxxxx3753 (02).pdf");
        if (!pdf.exists()) {
            System.out.println("Skipping: test PDF not found: " + pdf.getAbsolutePath());
            return;
        }

        // Confirm external tools
        boolean hasPdftoppm = Runtime.getRuntime().exec(new String[]{"which", "pdftoppm"}).waitFor() == 0;
        boolean hasTesseract = Runtime.getRuntime().exec(new String[]{"which", "tesseract"}).waitFor() == 0;
        if (!hasPdftoppm || !hasTesseract) {
            System.out.println("Skipping: pdftoppm/tesseract not available on PATH");
            return;
        }

        PdfBoxConfigurator cfg = new PdfBoxConfigurator();
        cfg.markPdfBoxUnavailable("forced for test");

        DocumentTextExtractor extractor = new DocumentTextExtractor(cfg);

        List<String> lines = extractor.parseDocument(pdf);
        System.out.println("Lines extracted: " + (lines == null ? 0 : lines.size()));
        if (lines != null) {
            lines.stream().limit(50).forEach(System.out::println);
        }

        // Should at least complete without throwing; lines may be empty but method ran
        assertNotNull(lines);
    }
}
