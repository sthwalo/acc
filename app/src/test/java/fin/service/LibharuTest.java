package fin.service;

import com.sun.jna.Pointer;

/**
 * Test class for libharu integration
 * Creates a simple PDF to verify JNA binding works
 */
public class LibharuTest {

    public static void main(String[] args) {
        try {
            System.out.println("Testing libharu JNA integration...");

            // Create PDF document
            Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
            if (pdf == null) {
                System.err.println("❌ Failed to create PDF document");
                System.exit(1);
            }
            System.out.println("✅ PDF document created successfully");

            // Add a page
            Pointer page = Libharu.INSTANCE.HPDF_AddPage(pdf);
            if (page == null) {
                System.err.println("❌ Failed to add page");
                Libharu.INSTANCE.HPDF_Free(pdf);
                System.exit(1);
            }
            System.out.println("✅ Page added successfully");

            // Set page size to A4
            Libharu.INSTANCE.HPDF_Page_SetSize(page, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);

            // Get font
            Pointer font = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica", null);
            if (font != null) {
                // Set font and size
                Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 12);

                // Draw text
                Libharu.INSTANCE.HPDF_Page_BeginText(page);
                Libharu.INSTANCE.HPDF_Page_TextOut(page, 50, 750, "Hello, libharu from Java!");
                Libharu.INSTANCE.HPDF_Page_EndText(page);
                System.out.println("✅ Text drawn successfully");
            } else {
                System.out.println("⚠️  Font not found, skipping text drawing");
            }

            // Save PDF
            String testFile = "test_libharu_main.pdf";
            int result = Libharu.INSTANCE.HPDF_SaveToFile(pdf, testFile);
            if (result == 0) {
                System.out.println("✅ PDF saved successfully: " + testFile);
                java.io.File pdfFile = new java.io.File(testFile);
                System.out.println("📄 File size: " + pdfFile.length() + " bytes");
            } else {
                System.err.println("❌ Failed to save PDF, error code: " + result);
                Libharu.INSTANCE.HPDF_Free(pdf);
                System.exit(1);
            }

            // Free resources
            Libharu.INSTANCE.HPDF_Free(pdf);
            System.out.println("🎉 Libharu JNA integration test completed successfully!");

        } catch (Exception e) {
            System.err.println("❌ Error testing libharu: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}