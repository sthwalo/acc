package fin.integration;

import com.sun.jna.Pointer;

import fin.service.Libharu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for libharu JNA binding.
 * Tests PDF generation using libharu native library.
 * 
 * NOTE: Disabled in CI/CD because libharu native library is not available.
 * This test requires libharu to be installed on the system.
 */
@Disabled("Requires libharu native library installation - not available in CI/CD")
public class LibharuIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    public void testLibharuPdfGeneration() {
        // Create PDF document
        Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
        assertNotNull(pdf, "Failed to create PDF document");

        try {
            // Add a page
            Pointer page = Libharu.INSTANCE.HPDF_AddPage(pdf);
            assertNotNull(page, "Failed to add page");

            // Set page size to A4
            Libharu.INSTANCE.HPDF_Page_SetSize(page, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);

            // Get font
            Pointer font = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica", null);
            if (font != null) {
                // Set font and size
                Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 12);

                // Draw text
                Libharu.INSTANCE.HPDF_Page_BeginText(page);
                Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, 750, "Hello, libharu from JUnit!");
                Libharu.INSTANCE.HPDF_Page_EndText(page);
            }

            // Save PDF to temp directory
            File testFile = tempDir.resolve("test_libharu_integration.pdf").toFile();
            int result = Libharu.INSTANCE.HPDF_SaveToFile(pdf, testFile.getAbsolutePath());
            assertEquals(0, result, "Failed to save PDF");

            // Verify file was created and has content
            assertTrue(testFile.exists(), "PDF file was not created");
            assertTrue(testFile.length() > 0, "PDF file is empty");

        } finally {
            // Free resources
            Libharu.INSTANCE.HPDF_Free(pdf);
        }
    }

    @Test
    public void testLibharuPdfWithMultiplePages() {
        Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
        assertNotNull(pdf, "Failed to create PDF document");

        try {
            // Add multiple pages
            Pointer page1 = Libharu.INSTANCE.HPDF_AddPage(pdf);
            Pointer page2 = Libharu.INSTANCE.HPDF_AddPage(pdf);

            assertNotNull(page1, "Failed to add first page");
            assertNotNull(page2, "Failed to add second page");

            // Set sizes
            Libharu.INSTANCE.HPDF_Page_SetSize(page1, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);
            Libharu.INSTANCE.HPDF_Page_SetSize(page2, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);

            // Save PDF
            File testFile = tempDir.resolve("test_libharu_multipage.pdf").toFile();
            int result = Libharu.INSTANCE.HPDF_SaveToFile(pdf, testFile.getAbsolutePath());
            assertEquals(0, result, "Failed to save multi-page PDF");

            assertTrue(testFile.exists(), "Multi-page PDF file was not created");
            assertTrue(testFile.length() > 0, "Multi-page PDF file is empty");

        } finally {
            Libharu.INSTANCE.HPDF_Free(pdf);
        }
    }
}
