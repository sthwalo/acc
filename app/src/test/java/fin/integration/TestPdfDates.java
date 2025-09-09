package fin.integration;

import fin.service.DocumentTextExtractor;
import java.io.File;
import java.util.List;

public class TestPdfDates {
    public static void main(String[] args) {
        try {
            String[] testFiles = {
                "input/xxxxx3753 (02).pdf",  // Likely Feb 2024 or Jan 2025?
                "input/xxxxx3753 (13).pdf",  // Likely Jan 2025?
                "input/xxxxx3753 (14).pdf"   // Likely Feb 2025?
            };
            
            for (String filePath : testFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    System.out.println("\n=== " + filePath + " ===");
                    
                    DocumentTextExtractor extractor = new DocumentTextExtractor();
                    List<String> lines = extractor.parseDocument(file);
                    
                    // Look for statement period
                    String statementPeriod = extractor.getStatementPeriod();
                    System.out.println("Statement Period: " + statementPeriod);
                    
                    // Look for dates in the first 50 lines
                    System.out.println("First 50 lines:");
                    for (int i = 0; i < Math.min(50, lines.size()); i++) {
                        String line = lines.get(i);
                        if (line.contains("2024") || line.contains("2025") || line.contains("Jan") || line.contains("Feb")) {
                            System.out.println((i+1) + ": " + line);
                        }
                    }
                    System.out.println("=".repeat(50));
                } else {
                    System.out.println("❌ File not found: " + filePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
