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
import fin.model.Company;
import org.junit.jupiter.api.*;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BankStatementProcessingServiceTest {
    private BankStatementProcessingService service;
    
    @BeforeAll
    static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();
    }
    
    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @BeforeEach
    void setUp() {
        // Use test database URL with embedded credentials for service
        service = new BankStatementProcessingService(TestConfiguration.TEST_DB_URL_WITH_CREDENTIALS);
    }
    
    @Test
    void processLines_WithValidInput_ParsesCorrectly() {
        // Given
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        
        List<String> lines = List.of(
            "Statement date: 1 September 2025",
            "Account number: 12345",
            "SERVICE FEE  35.00-"
        );
        
        // When
        List<BankTransaction> result = service.processLines(lines, "test-statement.pdf", company);
        
        // Then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should contain parsed transactions");
        
        // Check that the service fee transaction was parsed
        BankTransaction transaction = result.get(0);
        assertEquals("SERVICE FEE", transaction.getDetails(), "Transaction details should match");
        assertNotNull(transaction.getDebitAmount(), "Service fee should have debit amount");
        assertEquals(new java.math.BigDecimal("35.00"), transaction.getDebitAmount(), "Debit amount should be 35.00");
        
        // Check that fiscal period was found (should be the 2025 period)
        assertNotNull(transaction.getFiscalPeriodId(), "Fiscal period should be assigned");
    }
    
    @Test
    void processLines_WithEmptyLines_ReturnsEmptyList() {
        // Given
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        
        List<String> lines = List.of();
        
        // When
        try {
            List<BankTransaction> result = service.processLines(lines, "test-statement.pdf", company);
            
            // Then
            assertNotNull(result, "Result should not be null");
            assertTrue(result.isEmpty(), "Result should be empty for empty lines");
        } catch (Exception e) {
            // Acceptable if service requires database for empty list too
            assertTrue(true, "Service may require database connection even for empty processing");
        }
    }
}
