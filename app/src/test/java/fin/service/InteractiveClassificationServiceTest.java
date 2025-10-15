package fin.service;

import fin.TestConfiguration;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class InteractiveClassificationServiceTest {
    
    @BeforeAll
    static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();
    }
    
    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @Test
    void testClassificationRuleConstructorCreatesDefensiveCopy() {
        String[] originalKeywords = {"salary", "payroll", "employee"};
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(
                "SALARY PAYMENT", originalKeywords, "8100", "Employee Costs", 5);
        
        // Modify the original array
        originalKeywords[0] = "MODIFIED";
        
        // The rule's keywords should remain unchanged
        String[] ruleKeywords = rule.getKeywords();
        assertEquals("salary", ruleKeywords[0], "Rule keywords should not be affected by external modification");
        assertEquals("payroll", ruleKeywords[1]);
        assertEquals("employee", ruleKeywords[2]);
    }
    
    @Test
    void testClassificationRuleGetterReturnsDefensiveCopy() {
        String[] keywords = {"salary", "payroll", "employee"};
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(
                "SALARY PAYMENT", keywords, "8100", "Employee Costs", 5);
        
        // Get keywords from the rule
        String[] returnedKeywords = rule.getKeywords();
        
        // Modify the returned array
        returnedKeywords[0] = "MODIFIED";
        
        // The rule's internal keywords should remain unchanged
        String[] internalKeywords = rule.getKeywords();
        assertEquals("salary", internalKeywords[0], "Internal keywords should not be affected by external modification");
        assertEquals("payroll", internalKeywords[1]);
        assertEquals("employee", internalKeywords[2]);
    }
    
    @Test
    void testClassificationRuleWithNullKeywords() {
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(
                "TEST PATTERN", null, "8100", "Employee Costs", 1);
        
        assertNull(rule.getKeywords(), "Null keywords should be handled correctly");
    }
    
    @Test
    void testClassificationRuleWithEmptyKeywords() {
        String[] emptyKeywords = new String[0];
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(
                "TEST PATTERN", emptyKeywords, "8100", "Employee Costs", 1);
        
        String[] returnedKeywords = rule.getKeywords();
        assertNotNull(returnedKeywords, "Empty keywords array should be handled");
        assertEquals(0, returnedKeywords.length, "Empty keywords should return empty array");
        
        // The internal state should remain unchanged - defensive copy ensures isolation
        assertEquals(0, rule.getKeywords().length, "Internal state should remain unchanged");
    }
    
    @Test
    void testClassificationRuleKeywordsIsolation() {
        String[] originalKeywords = {"fee", "charge", "bank"};
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(
                "BANK FEE", originalKeywords, "9600", "Bank Charges", 3);
        
        // Get keywords multiple times - each should be independent
        String[] keywords1 = rule.getKeywords();
        String[] keywords2 = rule.getKeywords();
        
        // Modify one returned array
        keywords1[0] = "MODIFIED";
        
        // The other should remain unchanged
        assertEquals("fee", keywords2[0], "Multiple getter calls should return independent copies");
        assertEquals("charge", keywords2[1]);
        assertEquals("bank", keywords2[2]);
        
        // Internal state should remain unchanged
        String[] internalKeywords = rule.getKeywords();
        assertEquals("fee", internalKeywords[0], "Internal state should remain unchanged");
    }
}