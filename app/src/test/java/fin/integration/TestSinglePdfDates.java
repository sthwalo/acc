package fin.integration;

import fin.service.DocumentTextExtractor;
import java.io.File;
import java.util.List;

public class TestSinglePdfDates {
    public static void main(String[] args) {
        try {
            File file = new File("../input/xxxxx3753 (13).pdf");
            System.out.println("Testing file: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            
            if (file.exists()) {
                DocumentTextExtractor extractor = new DocumentTextExtractor();
                List<String> lines = extractor.parseDocument(file);
                
                System.out.println("Total lines: " + lines.size());
                System.out.println("Statement Period: " + extractor.getStatementPeriod());
                
                // Look for date patterns in first 20 lines
                System.out.println("\nFirst 20 lines:");
                for (int i = 0; i < Math.min(20, lines.size()); i++) {
                    System.out.println(i + ": " + lines.get(i));
                }
            } else {
                System.out.println("❌ File not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
