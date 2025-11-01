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