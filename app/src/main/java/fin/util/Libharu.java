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

package fin.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.io.IOException;

/**
 * Utility class for creating professional PDFs using libharu-inspired drawing techniques
 * Provides high-level PDF creation methods with advanced drawing capabilities
 */
public class Libharu {

    /**
     * Create a professional PDF using a drawing callback
     * @param outputPath Path where the PDF should be saved
     * @param callback Callback that performs the actual drawing
     * @param width Page width in points
     * @param height Page height in points
     * @throws IOException If PDF creation fails
     */
    public static void createProfessionalPdf(String outputPath, LibharuDrawingCallback callback,
                                           float width, float height) throws IOException {
        PDDocument document = new PDDocument();

        try {
            // Create a new page
            PDPage page = new PDPage();
            document.addPage(page);

            // Create content stream for drawing
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {
                // Call the callback to perform custom drawing
                callback.draw(contentStream, document);
            } finally {
                // Always close the content stream
                contentStream.close();
            }

            // Save the document
            document.save(outputPath);

        } finally {
            // Always close the document
            document.close();
        }
    }

    /**
     * Create a professional PDF with default A4 dimensions
     * @param outputPath Path where the PDF should be saved
     * @param callback Callback that performs the actual drawing
     * @throws IOException If PDF creation fails
     */
    public static void createProfessionalPdf(String outputPath, LibharuDrawingCallback callback) throws IOException {
        // Default A4 dimensions: 595.28 x 841.89 points
        createProfessionalPdf(outputPath, callback, 595.28f, 841.89f);
    }
}