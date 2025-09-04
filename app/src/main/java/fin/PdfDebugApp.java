package fin;

import fin.service.PdfTextExtractionService;

public class PdfDebugApp {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java fin.PdfDebugApp <pdf-file>");
            return;
        }
        
        String pdfFile = args[0];
        System.out.println("=== PDF Text Extraction Debug ===");
        System.out.println("Input: " + pdfFile);
        
        try {
            PdfTextExtractionService extractor = new PdfTextExtractionService();
            
            // Extract text to file
            String outputFile = pdfFile.replace(".pdf", "_extracted.txt");
            extractor.extractTextToFile(pdfFile, outputFile);
            
            // Show PDF info
            String pdfInfo = extractor.getPdfInfo(pdfFile);
            System.out.println(pdfInfo);
            
            // Show first 50 lines
            java.util.List<String> lines = extractor.extractTextLines(pdfFile);
            System.out.println("\n=== First 50 Lines ===");
            for (int i = 0; i < Math.min(50, lines.size()); i++) {
                System.out.printf("%3d: %s%n", i+1, lines.get(i));
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
