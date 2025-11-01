package fin.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import java.io.IOException;

/**
 * Callback interface for drawing operations in PDF generation
 * Allows custom drawing logic to be injected into PDF creation process
 */
public interface LibharuDrawingCallback {

    /**
     * Draw custom content on the PDF page
     * @param contentStream The PDFBox content stream for drawing
     * @param document The PDF document being created
     * @throws IOException If drawing operations fail
     */
    void draw(PDPageContentStream contentStream, PDDocument document) throws IOException;
}