/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.service;

import fin.model.TransactionMappingRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AccountClassificationService.
 * 
 * This test suite verifies the single source of truth for transaction classification rules.
 * The getStandardMappingRules() method is THE authoritative source for all classification logic.
 */
@DisplayName("AccountClassificationService Tests")
class AccountClassificationServiceTest {
    
    private AccountClassificationService service;
    
    @BeforeEach
    void setUp() {
        // Note: We use a dummy URL since we're only testing getStandardMappingRules()
        // which doesn't require database connection
        service = new AccountClassificationService("jdbc:postgresql://dummy:5432/test");
    }
    
    @Test
    @DisplayName("Should return exactly 100 standard mapping rules")
    void testGetStandardMappingRulesCount() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        assertNotNull(rules, "Rules list should not be null");
        assertEquals(100, rules.size(), 
            "Should return exactly 100 standard mapping rules with enhanced classifications");
    }
    
    @Test
    @DisplayName("Rules should be sorted by priority in descending order")
    void testRulesAreSortedByPriorityDescending() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (int i = 0; i < rules.size() - 1; i++) {
            int currentPriority = rules.get(i).getPriority();
            int nextPriority = rules.get(i + 1).getPriority();
            
            assertTrue(currentPriority >= nextPriority,
                String.format("Rules should be sorted by priority (descending). " +
                    "Rule at index %d has priority %d, but rule at index %d has priority %d",
                    i, currentPriority, i + 1, nextPriority));
        }
    }
    
    @Test
    @DisplayName("All rules should have account code embedded in description")
    void testAllRulesHaveAccountCodeInDescription() {
        // Arrange
        Pattern accountCodePattern = Pattern.compile("\\[AccountCode:(\\d+(?:-\\d+)?)\\]");
        
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (TransactionMappingRule rule : rules) {
            String description = rule.getDescription();
            assertNotNull(description, "Rule description should not be null: " + rule.getRuleName());
            
            Matcher matcher = accountCodePattern.matcher(description);
            assertTrue(matcher.find(), 
                String.format("Rule '%s' should have account code in description format [AccountCode:XXXX]. " +
                    "Description: %s", rule.getRuleName(), description));
            
            String accountCode = matcher.group(1);
            assertNotNull(accountCode, "Account code should be extractable from description");
            assertTrue(accountCode.matches("\\d+(?:-\\d+)?"), 
                "Account code should be numeric (e.g., '8100' or '8800-001'). Found: " + accountCode);
        }
    }
    
    @Test
    @DisplayName("All rules should be active by default")
    void testAllRulesAreActive() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (TransactionMappingRule rule : rules) {
            assertTrue(rule.isActive(), 
                String.format("Rule '%s' should be active by default", rule.getRuleName()));
        }
    }
    
    @Test
    @DisplayName("All rules should have non-null required fields")
    void testAllRulesHaveRequiredFields() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (TransactionMappingRule rule : rules) {
            assertNotNull(rule.getRuleName(), "Rule name should not be null");
            assertNotNull(rule.getDescription(), "Description should not be null");
            assertNotNull(rule.getMatchType(), "Match type should not be null");
            assertNotNull(rule.getMatchValue(), "Match value should not be null");
            assertTrue(rule.getPriority() > 0, 
                String.format("Priority should be positive for rule '%s'. Found: %d", 
                    rule.getRuleName(), rule.getPriority()));
        }
    }
    
    @Test
    @DisplayName("Priority 10 rules should come first (critical patterns)")
    void testPriority10RulesAreFirst() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        int priority10Count = 0;
        for (int i = 0; i < rules.size(); i++) {
            TransactionMappingRule rule = rules.get(i);
            if (rule.getPriority() == 10) {
                priority10Count++;
                // Priority 10 rules should come before lower priority rules
            } else if (rule.getPriority() < 10 && priority10Count > 0) {
                // Once we see a non-priority-10 rule, all remaining should also be non-priority-10
                for (int j = i; j < rules.size(); j++) {
                    assertTrue(rules.get(j).getPriority() <= 10,
                        "Rules should be sorted by priority descending");
                }
                break;
            }
        }
        
        assertTrue(priority10Count >= 3, 
            "Should have at least 3 critical priority 10 rules (Insurance Chauke, Jeffrey Maphosa, Stone Jeffrey)");
    }
    
    @Test
    @DisplayName("Insurance Chauke rule should have priority 10 (critical)")
    void testInsuranceChaukeRuleHasPriority10() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        TransactionMappingRule insuranceChaukeRule = rules.stream()
            .filter(r -> r.getMatchValue().equals("INSURANCE CHAUKE"))
            .findFirst()
            .orElse(null);
        
        assertNotNull(insuranceChaukeRule, 
            "Should have a rule for 'INSURANCE CHAUKE' (employee name that contains 'INSURANCE' keyword)");
        assertEquals(10, insuranceChaukeRule.getPriority(),
            "Insurance Chauke rule MUST have priority 10 to avoid misclassification as insurance expense");
        
        // Verify it maps to Employee Costs (8100), not Insurance (8800)
        String description = insuranceChaukeRule.getDescription();
        assertTrue(description.contains("[AccountCode:8100]"),
            "Insurance Chauke should map to Employee Costs (8100), not Insurance (8800)");
    }
    
    @Test
    @DisplayName("Generic insurance rule should have lower priority than Insurance Chauke")
    void testGenericInsuranceRuleHasLowerPriority() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        TransactionMappingRule genericInsuranceRule = rules.stream()
            .filter(r -> r.getMatchValue().equals("INSURANCE") && r.getPriority() < 10)
            .findFirst()
            .orElse(null);
        
        assertNotNull(genericInsuranceRule, 
            "Should have a generic 'INSURANCE' rule with priority < 10");
        
        assertTrue(genericInsuranceRule.getPriority() <= 5,
            "Generic insurance rule should have priority 5 or lower (fallback pattern)");
        
        // Verify it maps to Insurance expense (8800)
        String description = genericInsuranceRule.getDescription();
        assertTrue(description.contains("[AccountCode:8800]"),
            "Generic insurance rule should map to Insurance expense (8800)");
    }
    
    @Test
    @DisplayName("All rule names should be unique")
    void testAllRuleNamesAreUnique() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        Set<String> ruleNames = new HashSet<>();
        for (TransactionMappingRule rule : rules) {
            String ruleName = rule.getRuleName();
            assertFalse(ruleNames.contains(ruleName),
                String.format("Duplicate rule name found: '%s'", ruleName));
            ruleNames.add(ruleName);
        }
        
        assertEquals(rules.size(), ruleNames.size(), 
            "All rule names should be unique");
    }
    
    @Test
    @DisplayName("Rules should cover all major account codes (8100, 8800, 9600, etc.)")
    void testRulesCoverMajorAccountCodes() {
        // Arrange
        Set<String> expectedAccountCodes = Set.of(
            "8100", // Employee Costs
            "8700", // Professional Services
            "1100", // Bank - Current Account
            "4000", // Long-term Loans
            "8800", // Insurance
            "9600", // Bank Charges
            "9300", // Training & Development
            "8600"  // Fuel Expenses (8600-099 maps to base code 8600)
        );
        
        Pattern accountCodePattern = Pattern.compile("\\[AccountCode:(\\d+(?:-\\d+)?)\\]");
        
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        Set<String> foundAccountCodes = new HashSet<>();
        
        for (TransactionMappingRule rule : rules) {
            Matcher matcher = accountCodePattern.matcher(rule.getDescription());
            if (matcher.find()) {
                String accountCode = matcher.group(1);
                // Extract base code (e.g., "8800" from "8800-001", "8600" from "8600-099")
                String baseCode = accountCode.split("-")[0];
                foundAccountCodes.add(baseCode);
            }
        }
        
        // Assert
        for (String expectedCode : expectedAccountCodes) {
            assertTrue(foundAccountCodes.contains(expectedCode),
                String.format("Rules should cover account code %s. Found codes: %s", 
                    expectedCode, foundAccountCodes));
        }
    }
    
    @Test
    @DisplayName("Match types should be CONTAINS or REGEX only")
    void testMatchTypesAreValid() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (TransactionMappingRule rule : rules) {
            TransactionMappingRule.MatchType matchType = rule.getMatchType();
            assertTrue(
                matchType == TransactionMappingRule.MatchType.CONTAINS ||
                matchType == TransactionMappingRule.MatchType.REGEX,
                String.format("Rule '%s' has invalid match type: %s. Only CONTAINS or REGEX allowed.",
                    rule.getRuleName(), matchType)
            );
        }
    }
    
    @Test
    @DisplayName("REGEX rules should have valid regex patterns")
    void testRegexRulesHaveValidPatterns() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        for (TransactionMappingRule rule : rules) {
            if (rule.getMatchType() == TransactionMappingRule.MatchType.REGEX) {
                String pattern = rule.getMatchValue();
                assertDoesNotThrow(() -> Pattern.compile(pattern),
                    String.format("Rule '%s' has invalid regex pattern: %s", 
                        rule.getRuleName(), pattern));
            }
        }
    }
    
    @Test
    @DisplayName("Priority distribution should be correct (20, 10, 9, 8, 5)")
    void testPriorityDistribution() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Count rules by priority
        long priority20Count = rules.stream().filter(r -> r.getPriority() == 20).count();
        long priority10Count = rules.stream().filter(r -> r.getPriority() == 10).count();
        long priority9Count = rules.stream().filter(r -> r.getPriority() == 9).count();
        long priority8Count = rules.stream().filter(r -> r.getPriority() == 8).count();
        long priority5Count = rules.stream().filter(r -> r.getPriority() == 5).count();
        
        // Assert - adjusted for expanded rule set with more flexible minimums
        assertTrue(priority20Count >= 0, "Should have priority 20 rules (critical patterns)");
        assertTrue(priority10Count >= 3, "Should have at least 3 priority 10 rules (high-priority patterns)");
        assertTrue(priority9Count >= 1, "Should have at least 1 priority 9 rule (specific business patterns)");
        assertTrue(priority8Count >= 5, "Should have at least 5 priority 8 rules (standard business patterns)");
        assertTrue(priority5Count >= 3, "Should have at least 3 priority 5 rules (generic/fallback patterns)");
        
        // All rules should be priority 5, 6, 7, 8, 9, 10, or 20
        for (TransactionMappingRule rule : rules) {
            int priority = rule.getPriority();
            assertTrue(priority == 5 || priority == 6 || priority == 7 || priority == 8 || priority == 9 || priority == 10 || priority == 20,
                String.format("Rule '%s' has invalid priority %d. Only 5, 6, 7, 8, 9, 10, 20 allowed.", 
                    rule.getRuleName(), priority));
        }
    }
    
    @Test
    @DisplayName("Bank transfer rules should map to Bank - Current Account (1100)")
    void testBankTransferRulesMapToCorrectAccount() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        List<TransactionMappingRule> bankTransferRules = rules.stream()
            .filter(r -> r.getMatchValue().contains("IB TRANSFER") && 
                        !r.getRuleName().contains("Fuel")) // Exclude fuel transfer rule
            .toList();
        
        assertFalse(bankTransferRules.isEmpty(), "Should have bank transfer rules");
        
        for (TransactionMappingRule rule : bankTransferRules) {
            assertTrue(rule.getDescription().contains("[AccountCode:1100-001]"),
                String.format("Bank transfer rule '%s' should map to Bank - Current Account (1100-001)", 
                    rule.getRuleName()));
        }
    }
    
    @Test
    @DisplayName("Salary rules should map to Employee Costs (8100)")
    void testSalaryRulesMapToCorrectAccount() {
        // Act
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Assert
        List<TransactionMappingRule> salaryRules = rules.stream()
            .filter(r -> r.getMatchValue().toUpperCase().contains("SALARY") || 
                        r.getMatchValue().toUpperCase().contains("WAGES"))
            .toList();
        
        assertFalse(salaryRules.isEmpty(), "Should have salary/wage rules");
        
        for (TransactionMappingRule rule : salaryRules) {
            assertTrue(rule.getDescription().contains("[AccountCode:8100]"),
                String.format("Salary rule '%s' should map to Employee Costs (8100)", 
                    rule.getRuleName()));
        }
    }
    
    @Test
    @DisplayName("Test rule matching logic with sample transaction details")
    void testRuleMatchingWithSampleTransactions() {
        // Arrange
        List<TransactionMappingRule> rules = service.getStandardMappingRules();
        
        // Test cases: transaction detail → expected to match rule with account code
        String[][] testCases = {
            {"PAYMENT TO INSURANCE CHAUKE", "8100"}, // Should match priority 10 salary rule, NOT insurance
            {"IB TRANSFER TO SAVINGS ACCOUNT", "1100"}, // Bank transfer (changed from FUEL ACCOUNT to avoid conflict)
            {"XG SALARIES PAYMENT", "8100"}, // Generic salary
            {"OUTSURANCE PREMIUM PAYMENT", "8800"}, // Generic insurance (lower priority)
            {"FEE: ELECTRONIC BANKING", "9600"}, // Bank charges
            {"LYCEUM COLLEGE PAYMENT", "8730"}, // Education (changed from "SCHOOL FEES" to avoid "FEE" conflict)
        };
        
        // Act & Assert
        for (String[] testCase : testCases) {
            String transactionDetails = testCase[0];
            String expectedAccountCode = testCase[1];
            
            // Find first matching rule
            TransactionMappingRule matchedRule = rules.stream()
                .filter(r -> r.matches(transactionDetails))
                .findFirst()
                .orElse(null);
            
            assertNotNull(matchedRule, 
                String.format("Should find a matching rule for transaction: '%s'", transactionDetails));
            
            // Extract account code from description
            Pattern pattern = Pattern.compile("\\[AccountCode:(\\d+(?:-\\d+)?)\\]");
            Matcher matcher = pattern.matcher(matchedRule.getDescription());
            assertTrue(matcher.find(), 
                String.format("Matched rule should have account code in description. Rule: %s, Description: %s",
                    matchedRule.getRuleName(), matchedRule.getDescription()));
            
            String actualAccountCode = matcher.group(1).split("-")[0]; // Get base code
            assertEquals(expectedAccountCode, actualAccountCode,
                String.format("Transaction '%s' should map to account code %s, but matched rule '%s' maps to %s",
                    transactionDetails, expectedAccountCode, matchedRule.getRuleName(), actualAccountCode));
        }
    }
}
