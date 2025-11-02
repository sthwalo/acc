/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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