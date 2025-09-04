package fin.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;

/**
 * Service for extracting text from PDF bank statements
 * Handles pure PDF text extraction without any business logic
 */
public class PdfTextExtractionService {
    
    /**
     * Extract raw text from PDF file
     * @param pdfFilePath Path to the PDF file
     * @return Raw text content from PDF
     * @throws IOException if file cannot be read
     */
    public String extractTextFromPdf(String pdfFilePath) throws IOException {
        File pdfFile = new File(pdfFilePath);
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    /**
     * Extract text and save to file for debugging
     * @param pdfFilePath Path to the PDF file
     * @param outputTextFile Path to save extracted text
     * @throws IOException if files cannot be processed
     */
    public void extractTextToFile(String pdfFilePath, String outputTextFile) throws IOException {
        String extractedText = extractTextFromPdf(pdfFilePath);
        
        try (FileWriter writer = new FileWriter(outputTextFile)) {
            writer.write(extractedText);
        }
        
        System.out.println("✓ Text extracted from PDF: " + pdfFilePath);
        System.out.println("✓ Saved to: " + outputTextFile);
    }
    
    /**
     * Extract text and split into lines for processing
     * @param pdfFilePath Path to the PDF file
     * @return List of text lines from PDF
     * @throws IOException if file cannot be read
     */
    public List<String> extractTextLines(String pdfFilePath) throws IOException {
        String rawText = extractTextFromPdf(pdfFilePath);
        return Arrays.asList(rawText.split("\\r?\\n"));
    }
    
    /**
     * Get basic PDF information
     * @param pdfFilePath Path to the PDF file
     * @return Information about the PDF
     * @throws IOException if file cannot be read
     */
    public String getPdfInfo(String pdfFilePath) throws IOException {
        File pdfFile = new File(pdfFilePath);
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            int pageCount = document.getNumberOfPages();
            String text = new PDFTextStripper().getText(document);
            int lineCount = text.split("\\r?\\n").length;
            
            return String.format("PDF Info - Pages: %d, Lines: %d, Characters: %d", 
                               pageCount, lineCount, text.length());
        }
    }
}
