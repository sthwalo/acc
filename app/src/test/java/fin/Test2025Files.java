import fin.service.CompanyService;
import fin.service.BankStatementProcessingService;
import fin.model.Company;
import java.io.File;
import java.util.List;

public class Test2025Files {
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/drimacc_db?user=sthwalonyoni&password=Mapaya400151";
    
    public static void main(String[] args) {
        try {
            CompanyService companyService = new CompanyService(DB_URL);
            BankStatementProcessingService processor = new BankStatementProcessingService(DB_URL);
            
            List<Company> companies = companyService.getAllCompanies();
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found in database!");
                return;
            }
            Company company = companies.get(0); // Use first company
            
            String[] files2025 = {
                "../input/xxxxx3753 (13).pdf",  // Jan-Feb 2025
                "../input/xxxxx3753 (14).pdf"   // Feb-Mar 2025
            };
            
            for (String filePath : files2025) {
                File file = new File(filePath);
                if (file.exists()) {
                    System.out.println("\n🔄 Processing: " + file.getName());
                    
                    List transactions = processor.processStatement(file.getAbsolutePath(), company);
                    System.out.println("✅ Extracted " + transactions.size() + " transactions");
                } else {
                    System.out.println("❌ File not found: " + filePath);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
