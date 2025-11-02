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

import fin.TestConfiguration;
import fin.model.BankTransaction;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tests for the InteractiveClassificationService
 */
public class TestInteractiveClassificationService {
    
    private InteractiveClassificationService classificationService;
    
    @BeforeAll
    static void setUpClass() throws Exception {
        // Skip database setup in CI/CD environments where it might not work
        String ciEnv = System.getenv("CI");
        String githubActions = System.getenv("GITHUB_ACTIONS");
        if ("true".equals(ciEnv) || "true".equals(githubActions)) {
            System.out.println("⏭️ Skipping TestConfiguration.setupTestDatabase() in CI/CD environment");
            return;
        }
        
        TestConfiguration.setupTestDatabase();
    }
    
    @AfterAll 
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @BeforeEach
    public void setUp() {
        classificationService = new InteractiveClassificationService();
    }
    
    @Test
    @DisplayName("Test service initialization")
    public void testServiceInitialization() {
        assertNotNull(classificationService, "Service should initialize");
    }
    
    @Test
    @DisplayName("Test account suggestions for pattern")
    public void testAccountSuggestionsForPattern() {
        String pattern = "FEE CONFIRM";
        List<String> suggestions = classificationService.suggestAccountsForPattern(pattern);
        
        assertNotNull(suggestions, "Suggestions should not be null");
        
        // Print suggestions for debugging
        System.out.println("Suggestions for pattern '" + pattern + "': " + suggestions);
        
        // The method should always provide suggestions, either from database or pattern analysis
        // For "FEE CONFIRM", it should detect "fee" and suggest bank charges
        assertFalse(suggestions.isEmpty(), "Should provide suggestions for fee pattern. Got: " + suggestions);
        
        // Should suggest bank charges for fee patterns (either from database or pattern analysis)
        boolean hasBankChargesSuggestion = suggestions.stream()
            .anyMatch(s -> s.toLowerCase().contains("bank") || s.toLowerCase().contains("charge") || 
                       s.toLowerCase().contains("financial expenses"));
        assertTrue(hasBankChargesSuggestion, "Should suggest bank charges for fee patterns. Suggestions: " + suggestions);
    }
    
    @Test
    @DisplayName("Test creating mapping rule")
    public void testCreateMappingRule() {
        Long companyId = 1L;
        String pattern = "TEST FEE PAYMENT";
        String accountCode = "9600";
        String accountName = "Bank Charges";
        
        assertDoesNotThrow(() -> {
            classificationService.createMappingRule(companyId, pattern, accountCode, accountName);
        }, "Creating mapping rule should not throw exception");
    }
    
    @Test
    @DisplayName("Test batch classification")
    public void testBatchClassification() {
        List<Long> transactionIds = List.of(999L, 998L); // Non-existent IDs for test
        String accountCode = "9600";
        String accountName = "Bank Charges";
        Long companyId = 1L;
        
        int result = classificationService.classifyTransactionsBatch(transactionIds, accountCode, accountName, companyId);
        
        // Should return 0 for non-existent transactions
        assertEquals(0, result, "Should return 0 for non-existent transactions");
    }
    
    @Test
    @DisplayName("Test recreate journal entries")
    public void testRecreateJournalEntries() {
        assertDoesNotThrow(() -> {
            classificationService.recreateJournalEntriesForCategorizedTransactions();
        }, "Recreating journal entries should not throw exception");
    }
    
    @Test
    @DisplayName("Test classification rule creation")
    public void testClassificationRuleCreation() {
        String pattern = "TEST PATTERN";
        String[] keywords = {"TEST", "PATTERN"};
        String accountCode = "8800";
        String accountName = "Insurance";
        int usageCount = 1;
        
        InteractiveClassificationService.ClassificationRule rule = 
            new InteractiveClassificationService.ClassificationRule(pattern, keywords, accountCode, accountName, usageCount);
        
        assertNotNull(rule, "Rule should be created");
        assertEquals(pattern, rule.getPattern(), "Pattern should match");
        assertEquals(accountCode, rule.getAccountCode(), "Account code should match");
        assertEquals(accountName, rule.getAccountName(), "Account name should match");
        assertEquals(usageCount, rule.getUsageCount(), "Usage count should match");
        assertArrayEquals(keywords, rule.getKeywords(), "Keywords should match");
    }
    
    @Test
    @DisplayName("Test classified transaction creation")
    public void testClassifiedTransactionCreation() {
        BankTransaction transaction = createTestTransaction();
        String accountCode = "9600";
        String accountName = "Bank Charges";
        
        InteractiveClassificationService.ClassifiedTransaction classified = 
            new InteractiveClassificationService.ClassifiedTransaction(transaction, accountCode, accountName);
        
        assertNotNull(classified, "Classified transaction should be created");
        assertEquals(transaction, classified.getTransaction(), "Transaction should match");
        assertEquals(accountCode, classified.getAccountCode(), "Account code should match");
        assertEquals(accountName, classified.getAccountName(), "Account name should match");
    }
    
    @Test
    @DisplayName("Test change record creation")
    public void testChangeRecordCreation() {
        Long transactionId = 123L;
        LocalDate transactionDate = LocalDate.of(2025, 10, 11);
        String description = "TEST FEE CONFIRM";
        BigDecimal amount = new BigDecimal("25.00");
        String oldAccount = "UNCATEGORIZED";
        String newAccount = "Bank Charges";
        
        InteractiveClassificationService.ChangeRecord change = 
            new InteractiveClassificationService.ChangeRecord(transactionId, transactionDate, description, amount, oldAccount, newAccount);
        
        assertNotNull(change, "Change record should be created");
        assertEquals(transactionId, change.getTransactionId(), "Transaction ID should match");
        assertEquals(transactionDate, change.transactionDate, "Transaction date should match");
        assertEquals(description, change.description, "Description should match");
        assertEquals(amount, change.amount, "Amount should match");
        assertEquals(oldAccount, change.oldAccount, "Old account should match");
        assertEquals(newAccount, change.newAccount, "New account should match");
        assertNotNull(change.timestamp, "Timestamp should be set");
    }
    
    private BankTransaction createTestTransaction() {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(123L);
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);
        transaction.setTransactionDate(LocalDate.of(2025, 10, 11));
        transaction.setDetails("TEST FEE CONFIRM");
        transaction.setDebitAmount(new BigDecimal("25.00"));
        transaction.setBalance(new BigDecimal("1000.00"));
        return transaction;
    }
}
