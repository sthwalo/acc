package fin.app;

import fin.service.AccountClassificationService;
import fin.service.CompanyService;
import fin.service.TransactionMappingService;
import fin.config.DatabaseConfig;
import fin.model.Company;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Thin wrapper around AccountClassificationService for initializing 
 * the chart of accounts and transaction mapping rules for a company.
 * 
 * This class has been refactored to delegate most functionality to
 * specialized service classes, reducing duplication and improving maintainability.
 */
public class ChartOfAccountsInitializer {
    
    private static final Logger LOGGER = Logger.getLogger(ChartOfAccountsInitializer.class.getName());
    private final AccountClassificationService accountService;
    private final CompanyService companyService;
    private final TransactionMappingService mappingService;
    private final String dbUrl;
    
    public ChartOfAccountsInitializer() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.accountService = new AccountClassificationService(dbUrl);
        this.companyService = new CompanyService(dbUrl);
        this.mappingService = new TransactionMappingService(dbUrl);
    }
    
    /**
     * Initialize the chart of accounts for a company
     * 
     * This is now a thin wrapper around AccountClassificationService's implementation
     * 
     * @param companyId The ID of the company to initialize accounts for
     */
    public void initializeChartOfAccounts(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                LOGGER.log(Level.SEVERE, "Company not found with ID: " + companyId);
                return;
            }
            
            LOGGER.info("Initializing chart of accounts for: " + company.getName());
            System.out.println("🏢 Initializing chart of accounts for: " + company.getName());
            
            // Delegate to the account service
            accountService.initializeChartOfAccounts(companyId);
            
            // Generate a report of the accounts
            accountService.generateClassificationReport(companyId);
            
            LOGGER.info("Chart of accounts initialized successfully for " + company.getName());
            System.out.println("✅ Chart of accounts initialized successfully!");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts", e);
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
        }
    }
    
    /**
     * Initialize standard transaction mapping rules for a company
     * 
     * @param companyId The ID of the company to create rules for
     */
    public void initializeTransactionMappingRules(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                LOGGER.log(Level.SEVERE, "Company not found with ID: " + companyId);
                System.err.println("❌ Company not found with ID: " + companyId);
                return;
            }
            
            LOGGER.info("Initializing transaction mapping rules for: " + company.getName());
            System.out.println("\n📝 Creating Transaction Mapping Rules for: " + company.getName());
            System.out.println("=".repeat(80));
            
            // Delegate to the mapping service to create standard rules
            int rulesCreated = mappingService.createStandardMappingRules(companyId);
            
            LOGGER.info("Transaction mapping rules initialized successfully for " + company.getName());
            System.out.println("✅ Created " + rulesCreated + " standard mapping rules");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules", e);
            System.err.println("❌ Error initializing transaction mapping rules: " + e.getMessage());
        }
    }
    
    /**
     * Perform full initialization of a company's financial structure
     * 
     * @param companyId The ID of the company to initialize
     */
    public void performFullInitialization(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                LOGGER.log(Level.SEVERE, "Company not found with ID: " + companyId);
                System.err.println("❌ Company not found with ID: " + companyId);
                return;
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🏢 FULL FINANCIAL INITIALIZATION FOR: " + company.getName());
            System.out.println("=".repeat(80));
            
            // Step 1: Initialize chart of accounts
            System.out.println("\n📊 Step 1: Initializing Chart of Accounts");
            accountService.initializeChartOfAccounts(companyId);
            
            // Step 2: Create standard transaction mapping rules
            System.out.println("\n🔄 Step 2: Creating Transaction Mapping Rules");
            mappingService.createStandardMappingRules(companyId);
            
            // Step 3: Process any unclassified transactions
            System.out.println("\n📝 Step 3: Classifying Existing Transactions");
            int classifiedCount = mappingService.classifyAllUnclassifiedTransactions(companyId, "SYSTEM");
            System.out.println("✅ Classified " + classifiedCount + " transactions");
            
            // Step 4: Generate classification report
            System.out.println("\n📋 Step 4: Generating Classification Report");
            accountService.generateClassificationReport(companyId);
            
            System.out.println("\n✨ Full initialization completed successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during full initialization: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Error during full initialization", e);
        }
    }
    
    /**
     * Main entry point for the initializer
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp app.jar fin.app.ChartOfAccountsInitializer <command> <companyId>");
            System.out.println("Commands:");
            System.out.println("  init-accounts    - Initialize chart of accounts");
            System.out.println("  init-rules       - Initialize transaction mapping rules");
            System.out.println("  full-init        - Perform full initialization");
            return;
        }
        
        String command = args[0];
        Long companyId = Long.parseLong(args[1]);
        
        ChartOfAccountsInitializer initializer = new ChartOfAccountsInitializer();
        
        switch (command) {
            case "init-accounts":
                initializer.initializeChartOfAccounts(companyId);
                break;
            case "init-rules":
                initializer.initializeTransactionMappingRules(companyId);
                break;
            case "full-init":
                initializer.performFullInitialization(companyId);
                break;
            default:
                System.out.println("Unknown command: " + command);
                break;
        }
    }
}
