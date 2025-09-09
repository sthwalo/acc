package fin.app;

import fin.service.TransactionMappingService;
import fin.service.AccountClassificationService;
import fin.service.CompanyService;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.model.BankTransaction;
import fin.config.DatabaseConfig;

import java.util.List;
import java.util.Map;

/**
 * Tool to analyze current account classification status and provide recommendations
 * for improving transaction classification accuracy
 */
public class AccountClassificationAnalyzer {
    
    private TransactionMappingService mappingService;
    private AccountClassificationService classificationService;
    private CompanyService companyService;
    private String dbUrl;
    
    public AccountClassificationAnalyzer() {
        if (!DatabaseConfig.testConnection()) {
            throw new RuntimeException("Failed to connect to database");
        }
        
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.companyService = new CompanyService(dbUrl);
        this.mappingService = new TransactionMappingService(dbUrl);
        this.classificationService = new AccountClassificationService(dbUrl);
    }
    
    public void analyzeClassificationAccuracy() {
        System.out.println("🔍 ACCOUNT CLASSIFICATION ACCURACY ANALYSIS");
        System.out.println("=".repeat(60));
        
        // Get company and fiscal period
        List<Company> companies = companyService.getAllCompanies();
        if (companies.isEmpty()) {
            System.err.println("❌ No companies found in database");
            return;
        }
        
        Company company = companies.get(0);
        List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(company.getId());
        if (periods.isEmpty()) {
            System.err.println("❌ No fiscal periods found for company");
            return;
        }
        
        FiscalPeriod period = periods.get(0);
        
        System.out.println("🏢 Company: " + company.getName());
        System.out.println("📅 Period: " + period.getPeriodName());
        System.out.println();
        
        // Analyze unclassified transactions
        analyzeUnclassifiedTransactions(company.getId());
        
        // Show classification suggestions
        showClassificationSuggestions(company.getId());
        
        // Provide recommendations
        provideRecommendations();
    }
    
    private void analyzeUnclassifiedTransactions(Long companyId) {
        System.out.println("📊 UNCLASSIFIED TRANSACTIONS ANALYSIS");
        System.out.println("-".repeat(40));
        
        Map<String, List<BankTransaction>> unclassified = mappingService.analyzeUnclassifiedTransactions(companyId);
        
        if (unclassified.isEmpty()) {
            System.out.println("✅ All transactions are properly classified!");
            return;
        }
        
        System.out.println("Found " + unclassified.size() + " different account classifications:");
        System.out.println();
        
        int totalTransactions = 0;
        for (Map.Entry<String, List<BankTransaction>> entry : unclassified.entrySet()) {
            String account = entry.getKey();
            List<BankTransaction> transactions = entry.getValue();
            totalTransactions += transactions.size();
            
            System.out.printf("📋 %s (%d transactions)%n", account, transactions.size());
            
            // Show sample transactions
            transactions.stream()
                .limit(3)
                .forEach(t -> System.out.printf("   📝 %s: %s%n", 
                    t.getTransactionDate(), 
                    t.getDetails().length() > 50 ? 
                        t.getDetails().substring(0, 47) + "..." : 
                        t.getDetails()));
            
            if (transactions.size() > 3) {
                System.out.println("   ... and " + (transactions.size() - 3) + " more");
            }
            System.out.println();
        }
        
        System.out.println("📈 Total unclassified transactions: " + totalTransactions);
        System.out.println();
    }
    
    private void showClassificationSuggestions(Long companyId) {
        System.out.println("💡 ACCOUNT MAPPING SUGGESTIONS");
        System.out.println("-".repeat(40));
        
        Map<String, String> suggestions = classificationService.getAccountMappingSuggestions(companyId);
        
        for (Map.Entry<String, String> entry : suggestions.entrySet()) {
            System.out.printf("%-25s → %s%n", entry.getKey(), entry.getValue());
        }
        System.out.println();
    }
    
    private void provideRecommendations() {
        System.out.println("🎯 RECOMMENDATIONS FOR IMPROVING CLASSIFICATION");
        System.out.println("-".repeat(50));
        
        System.out.println("1. 📥 EXTRACTION LEVEL IMPROVEMENTS:");
        System.out.println("   ✅ Pros:");
        System.out.println("      • Transactions are classified at source");
        System.out.println("      • All future reports automatically use correct accounts");
        System.out.println("      • Data integrity maintained across all systems");
        System.out.println("      • Journal entries can be auto-generated correctly");
        System.out.println("   ⚠️  Cons:");
        System.out.println("      • Requires updating extraction/import logic");
        System.out.println("      • May need to reprocess historical data");
        System.out.println("      • Changes affect multiple components");
        System.out.println();
        
        System.out.println("2. 📊 EXPORT LEVEL IMPROVEMENTS:");
        System.out.println("   ✅ Pros:");
        System.out.println("      • Quick fix for immediate reporting needs");
        System.out.println("      • No changes to core data structures");
        System.out.println("      • Can be customized per report type");
        System.out.println("   ⚠️  Cons:");
        System.out.println("      • Data inconsistency between raw data and reports");
        System.out.println("      • Must be applied to every export format");
        System.out.println("      • Source data remains incorrectly classified");
        System.out.println("      • Journal entries still use wrong accounts");
        System.out.println();
        
        System.out.println("🏆 RECOMMENDED APPROACH:");
        System.out.println("   1. 🔧 IMMEDIATE: Fix extraction level for proper data classification");
        System.out.println("   2. 🔄 ENHANCEMENT: Improve TransactionMappingService rules");
        System.out.println("   3. 📝 VERIFICATION: Update reports to show proper account details");
        System.out.println("   4. 🧹 CLEANUP: Reclassify existing historical transactions");
        System.out.println();
        
        System.out.println("💪 IMPLEMENTATION STRATEGY:");
        System.out.println("   Phase 1: Enhance transaction mapping rules");
        System.out.println("   Phase 2: Add account classification to BankTransaction model");
        System.out.println("   Phase 3: Update extraction to apply classification during import");
        System.out.println("   Phase 4: Regenerate reports with proper account information");
        System.out.println("   Phase 5: Create reclassification tool for historical data");
    }
    
    public static void main(String[] args) {
        try {
            AccountClassificationAnalyzer analyzer = new AccountClassificationAnalyzer();
            analyzer.analyzeClassificationAccuracy();
        } catch (Exception e) {
            System.err.println("❌ Error during classification analysis: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
