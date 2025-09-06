/*
 * Direct PDF processor - processes all PDFs in input directory
 */
package fin;

import fin.service.BankStatementProcessingService;
import fin.service.CompanyService;
import fin.model.Company;
import fin.model.BankTransaction;
import java.io.File;
import java.util.List;

public class DirectPdfProcessor {
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/drimacc_db?user=sthwalonyoni&password=drimPro1823";
    
    public static void main(String[] args) {
        try {
            System.out.println("🏦 Direct PDF Processing");
            System.out.println("========================");
            
            // Initialize services
            CompanyService companyService = new CompanyService(DB_URL);
            BankStatementProcessingService bankService = new BankStatementProcessingService(DB_URL);
            
            // Get the first company
            List<Company> companies = companyService.getAllCompanies();
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found. Please create a company first.");
                return;
            }
            
            Company company = companies.get(0);
            System.out.println("✅ Using company: " + company.getName());
            
            // Find PDF files
            File inputDir = new File("../input");
            if (!inputDir.exists()) {
                System.out.println("❌ Input directory not found at: " + inputDir.getAbsolutePath());
                return;
            }
            
            File[] pdfFiles = inputDir.listFiles((dir, name) -> 
                name.toLowerCase().endsWith(".pdf"));
            
            if (pdfFiles == null || pdfFiles.length == 0) {
                System.out.println("❌ No PDF files found");
                return;
            }
            
            System.out.println("📄 Found " + pdfFiles.length + " PDF files");
            
            // Process each PDF
            int totalTransactions = 0;
            int successfulFiles = 0;
            
            for (File pdfFile : pdfFiles) {
                try {
                    System.out.println("\n🔄 Processing: " + pdfFile.getName());
                    
                    // Use the existing service method
                    List<BankTransaction> transactions = bankService.processStatement(
                        pdfFile.getAbsolutePath(), 
                        company
                    );
                    
                    totalTransactions += transactions.size();
                    successfulFiles++;
                    
                    System.out.println("✅ Extracted " + transactions.size() + " transactions");
                    
                } catch (Exception e) {
                    System.err.println("❌ Failed to process " + pdfFile.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            System.out.println("\n🎉 PROCESSING COMPLETE!");
            System.out.println("========================");
            System.out.println("📁 Files processed successfully: " + successfulFiles + "/" + pdfFiles.length);
            System.out.println("📊 Total transactions extracted: " + totalTransactions);
            
            // Verify database content
            System.out.println("\n🔍 Verifying database...");
            System.out.println("Run this query to see results:");
            System.out.println("psql -U sthwalonyoni -d drimacc_db -c \"SELECT COUNT(*) FROM bank_transactions;\"");
            
        } catch (Exception e) {
            System.err.println("❌ Processing failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
