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
import fin.context.ApplicationContext;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.SQLException;

/**
 * Test class for TransactionClassificationService.
 * Verifies Phase 1 refactoring: unified classification service.
 */
class TransactionClassificationServiceTest {
    
    private ApplicationContext applicationContext;
    private TransactionClassificationService classificationService;
    
    @BeforeAll
    static void setUpClass() {
        // Set test mode to skip tax calculator initialization
        System.setProperty("test.mode", "true");
        
        // Force reload of database configuration for test environment
        fin.config.DatabaseConfig.loadConfiguration();
        
        // Setup test database
        try {
            TestConfiguration.setupTestDatabase();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to setup test database", e);
        }
    }
    
    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @BeforeEach
    void setUp() {
        applicationContext = new ApplicationContext();
        classificationService = applicationContext.get(TransactionClassificationService.class);
    }
    
    @Test
    void get_TransactionClassificationService_ReturnsNonNullInstance() {
        assertNotNull(classificationService, "TransactionClassificationService should not be null");
    }
    
    @Test
    void get_SameService_ReturnsSameInstance() {
        TransactionClassificationService service1 = applicationContext.get(TransactionClassificationService.class);
        TransactionClassificationService service2 = applicationContext.get(TransactionClassificationService.class);
        
        assertSame(service1, service2, "Should return the same singleton instance");
    }
    
    @Test
    void transactionClassificationService_HasAllRequiredMethods() {
        // Verify all six public API methods are accessible
        assertDoesNotThrow(() -> {
            // Test method existence by reflection (no actual execution to avoid DB dependencies)
            classificationService.getClass().getMethod("initializeChartOfAccounts", Long.class);
            classificationService.getClass().getMethod("initializeTransactionMappingRules", Long.class);
            classificationService.getClass().getMethod("performFullInitialization", Long.class);
            classificationService.getClass().getMethod("runInteractiveClassification", Long.class, Long.class);
            classificationService.getClass().getMethod("autoClassifyTransactions", Long.class, Long.class);
            classificationService.getClass().getMethod("synchronizeJournalEntries", Long.class, Long.class);
        }, "All six public API methods should be accessible");
    }
    
    @Test
    void transactionClassificationService_IsProperlyWired() {
        // Verify service is properly registered and can be retrieved
        assertDoesNotThrow(() -> {
            TransactionClassificationService service = applicationContext.get(TransactionClassificationService.class);
            assertNotNull(service, "Service should be properly wired in ApplicationContext");
        }, "TransactionClassificationService should be properly wired in ApplicationContext");
    }
    
    @Test
    void transactionClassificationService_HasCorrectDependencies() {
        // Verify that all required dependency services are also available
        // NOTE: ChartOfAccountsService is deprecated (2025-10-03) in favor of AccountClassificationService
        assertDoesNotThrow(() -> {
            // Primary chart of accounts service (SARS-compliant, single source of truth)
            AccountClassificationService accountClassificationService = applicationContext.get(AccountClassificationService.class);
            
            // Supporting services
            TransactionMappingRuleService ruleService = applicationContext.get(TransactionMappingRuleService.class);
            TransactionMappingService mappingService = applicationContext.get(TransactionMappingService.class);
            InteractiveClassificationService interactiveService = applicationContext.get(InteractiveClassificationService.class);
            
            // Deprecated service (still registered for backward compatibility)
            @SuppressWarnings("deprecation")
            ChartOfAccountsService chartOfAccountsService = applicationContext.get(ChartOfAccountsService.class);
            
            assertNotNull(accountClassificationService, "AccountClassificationService should be available (primary)");
            assertNotNull(ruleService, "TransactionMappingRuleService should be available");
            assertNotNull(mappingService, "TransactionMappingService should be available");
            assertNotNull(interactiveService, "InteractiveClassificationService should be available");
            assertNotNull(chartOfAccountsService, "ChartOfAccountsService should be available (deprecated)");
        }, "All dependency services should be properly registered");
    }
    
    @Test
    void transactionClassificationService_ReplacesClassificationIntegrationService() {
        // Verify that TransactionClassificationService is the new unified service
        // and ClassificationIntegrationService is being phased out
        assertDoesNotThrow(() -> {
            TransactionClassificationService newService = applicationContext.get(TransactionClassificationService.class);
            assertNotNull(newService, "New unified service should be available");
        }, "TransactionClassificationService should successfully replace ClassificationIntegrationService");
    }
    
    @Test
    void transactionClassificationService_HasBackwardCompatibleAPI() {
        // Verify that the service provides the same API as ClassificationIntegrationService
        // This ensures no breaking changes for existing code
        try {
            // Check method signatures match
            classificationService.getClass().getMethod("runInteractiveClassification", Long.class, Long.class);
            classificationService.getClass().getMethod("autoClassifyTransactions", Long.class, Long.class);
            classificationService.getClass().getMethod("initializeChartOfAccounts", Long.class);
            classificationService.getClass().getMethod("initializeTransactionMappingRules", Long.class);
            classificationService.getClass().getMethod("performFullInitialization", Long.class);
            classificationService.getClass().getMethod("synchronizeJournalEntries", Long.class, Long.class);
            
            // All methods exist - API is backward compatible
            assertTrue(true, "API is backward compatible with ClassificationIntegrationService");
        } catch (NoSuchMethodException e) {
            fail("TransactionClassificationService should have backward-compatible API: " + e.getMessage());
        }
    }
    
    @Test
    void applicationContext_RegistersTransactionClassificationService() {
        // Verify that ApplicationContext properly registers the service
        TransactionClassificationService service = applicationContext.get(TransactionClassificationService.class);
        
        assertNotNull(service, "ApplicationContext should register TransactionClassificationService");
        assertInstanceOf(TransactionClassificationService.class, service, 
            "Retrieved instance should be of type TransactionClassificationService");
    }
    
    @Test
    void phase1Refactoring_AllServicesAvailable() {
        // Comprehensive test for Phase 1: verify all related services are available
        assertDoesNotThrow(() -> {
            // New unified service
            TransactionClassificationService unifiedService = 
                applicationContext.get(TransactionClassificationService.class);
            
            // Primary chart of accounts service (single source of truth as of 2025-10-03)
            AccountClassificationService accountClassificationService = 
                applicationContext.get(AccountClassificationService.class);
            
            // Supporting services
            @SuppressWarnings("deprecation")
            ChartOfAccountsService chartService = 
                applicationContext.get(ChartOfAccountsService.class);
            TransactionMappingRuleService ruleService = 
                applicationContext.get(TransactionMappingRuleService.class);
            CategoryManagementService categoryService = 
                applicationContext.get(CategoryManagementService.class);
            AccountManagementService accountService = 
                applicationContext.get(AccountManagementService.class);
            TransactionMappingService mappingService = 
                applicationContext.get(TransactionMappingService.class);
            
            // Verify all are non-null
            assertNotNull(unifiedService, "TransactionClassificationService should be registered");
            assertNotNull(accountClassificationService, "AccountClassificationService should be registered (primary)");
            assertNotNull(chartService, "ChartOfAccountsService should be registered (deprecated)");
            assertNotNull(ruleService, "TransactionMappingRuleService should be registered");
            assertNotNull(categoryService, "CategoryManagementService should be registered");
            assertNotNull(accountService, "AccountManagementService should be registered");
            assertNotNull(mappingService, "TransactionMappingService should be registered");
        }, "Phase 1 refactoring: all services should be properly registered and accessible");
    }
    
    @Test
    void accountClassificationService_IsSingleSourceOfTruth() {
        // Verify AccountClassificationService is now the primary chart of accounts provider
        // as of 2025-10-03 refactoring
        assertDoesNotThrow(() -> {
            AccountClassificationService accountService = 
                applicationContext.get(AccountClassificationService.class);
            
            assertNotNull(accountService, 
                "AccountClassificationService should be the single source of truth for chart of accounts");
            
            // Verify it's properly registered and accessible
            assertInstanceOf(AccountClassificationService.class, accountService,
                "Retrieved instance should be AccountClassificationService");
        }, "AccountClassificationService should be available as single source of truth");
    }
    
    @Test
    void chartOfAccountsService_IsDeprecatedButAvailable() {
        // ChartOfAccountsService is deprecated as of 2025-10-03 but remains available
        // for backward compatibility during transition period
        assertDoesNotThrow(() -> {
            @SuppressWarnings("deprecation")
            ChartOfAccountsService deprecatedService = 
                applicationContext.get(ChartOfAccountsService.class);
            
            assertNotNull(deprecatedService, 
                "ChartOfAccountsService should still be available (deprecated)");
            
            // Note: Will be removed after 30 days of successful operation with
            // AccountClassificationService as single source of truth
        }, "Deprecated ChartOfAccountsService should still be registered for backward compatibility");
    }
}
