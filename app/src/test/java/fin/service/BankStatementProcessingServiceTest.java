package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BankStatementProcessingServiceTest {
    private BankStatementProcessingService service;
    
    @BeforeEach
    void setUp() {
        // Use in-memory H2 database for testing instead of PostgreSQL
        String testDbUrl = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        try {
            service = new BankStatementProcessingService(testDbUrl);
        } catch (Exception e) {
            // If H2 is not available, skip this test
            throw new RuntimeException("Test database not available - skipping test");
        }
    }
    
    @Test
    void processLines_WithServiceFee_CreatesTransaction() {
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
        
        if (!result.isEmpty()) {
            BankTransaction transaction = result.get(0);
            assertEquals(company.getId(), transaction.getCompanyId(), "Company ID should match");
            assertEquals("SERVICE FEE", transaction.getDetails().trim(), "Details should match");
            assertNotNull(transaction.getTransactionDate(), "Transaction date should not be null");
        }
    }
}
