/*
 * Test single PDF processor - processes one PDF to debug
 */
package fin;

import fin.service.BankStatementProcessingService;
import fin.service.CompanyService;
import fin.model.Company;
import fin.model.BankTransaction;
import java.io.File;
import java.util.List;

public class TestSinglePdf {
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/drimacc_db?user=sthwalonyoni&password=drimPro1823";
    
    public static void main(String[] args) {
        try {
            System.out.println("🔍 Single PDF Test");
            System.out.println("==================");
            
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
            
            // Test a single PDF that contains transactions from March 2024 onwards
            File testFile = new File("../input/xxxxx3753 (06).pdf"); // Try March statement
            if (!testFile.exists()) {
                testFile = new File("../input/xxxxx3753 (07).pdf"); // Try April statement
            }
            if (!testFile.exists()) {
                testFile = new File("../input/xxxxx3753 (08).pdf"); // Try May statement
            }
            
            if (!testFile.exists()) {
                System.out.println("❌ Test file not found");
                return;
            }
            
            System.out.println("📄 Testing file: " + testFile.getName());
            
            // Process the PDF
            List<BankTransaction> transactions = bankService.processStatement(
                testFile.getAbsolutePath(), 
                company
            );
            
            System.out.println("✅ Successfully processed " + transactions.size() + " transactions");
            
            // Show some sample transactions
            if (!transactions.isEmpty()) {
                System.out.println("\n📊 Sample transactions:");
                for (int i = 0; i < Math.min(5, transactions.size()); i++) {
                    BankTransaction tx = transactions.get(i);
                    System.out.printf("  %s | %s | %.2f\n", 
                        tx.getTransactionDate(), 
                        tx.getDetails().substring(0, Math.min(50, tx.getDetails().length())),
                        tx.getDebitAmount() != null ? tx.getDebitAmount().doubleValue() : 
                        tx.getCreditAmount() != null ? tx.getCreditAmount().doubleValue() : 0.0);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
