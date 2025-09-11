package fin.app;

import fin.service.CompanyService;
import fin.service.TransactionMappingService;
import fin.config.DatabaseConfig;
import fin.model.Company;

/**
 * Application to classify bank transactions using transaction mapping rules
 * This is a thin wrapper around TransactionMappingService
 */
public class TransactionClassifier {
    
    private final CompanyService companyService;
    private final TransactionMappingService mappingService;
    
    public TransactionClassifier() {
        String dbUrl = DatabaseConfig.getDatabaseUrl();
        this.companyService = new CompanyService(dbUrl);
        this.mappingService = new TransactionMappingService(dbUrl);
    }
    
    /**
     * Classify transactions based on the mapping rules
     * 
     * @param companyId The company ID
     * @param username The username for audit purposes
     */
    public void classifyTransactions(Long companyId, String username) {
        try {
            // Validate company exists
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                System.err.println("❌ Company not found with ID: " + companyId);
                return;
            }
            
            System.out.println("🏢 Classifying transactions for: " + company.getName());
            System.out.println("=".repeat(60));
            
            // Delegate to TransactionMappingService
            int classifiedCount = mappingService.classifyAllUnclassifiedTransactions(companyId, username);
            
            System.out.println("=".repeat(60));
            System.out.println("✅ Classification completed: " + classifiedCount + " transactions classified");
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("❌ Error classifying transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        try {
            TransactionClassifier classifier = new TransactionClassifier();
            
            Long companyId = 1L;
            String username = "system";
            
            // Parse command line arguments
            if (args.length > 0) {
                try {
                    companyId = Long.parseLong(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid company ID. Using default: 1");
                }
                
                if (args.length > 1) {
                    username = args[1];
                }
            }
            
            // Run classification
            classifier.classifyTransactions(companyId, username);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
