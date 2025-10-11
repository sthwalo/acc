import java.util.List;
import fin.service.AccountClassificationService;
import fin.model.TransactionMappingRule;

public class TestRules {
    public static void main(String[] args) {
        try {
            AccountClassificationService service = new AccountClassificationService();
            List<TransactionMappingRule> rules = service.getStandardMappingRules();
            
            String[] testCases = {
                "PAYMENT TO INSURANCE CHAUKE",
                "IB TRANSFER TO SAVINGS ACCOUNT", 
                "XG SALARIES PAYMENT",
                "OUTSURANCE PREMIUM PAYMENT",
                "FEE: ELECTRONIC BANKING",
                "LYCEUM COLLEGE SCHOOL FEES"
            };
            
            System.out.println("Total rules: " + rules.size());
            
            for (String testCase : testCases) {
                System.out.println("\nTesting: " + testCase);
                TransactionMappingRule matchedRule = rules.stream()
                    .filter(r -> r.matches(testCase))
                    .findFirst()
                    .orElse(null);
                    
                if (matchedRule != null) {
                    System.out.println("  Matched: " + matchedRule.getRuleName());
                    System.out.println("  Description: " + matchedRule.getDescription());
                } else {
                    System.out.println("  No match found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
