package fin.service;

import fin.TestConfiguration;
import org.junit.jupiter.api.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the InteractiveClassificationService
 */
public class TestInteractiveClassificationService {
    
    private InteractiveClassificationService classificationService;
    
    @BeforeAll
    static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();
    }
    
    @AfterAll 
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }
    
    @BeforeEach
    public void setUp() throws SQLException {
        // Create service with PostgreSQL test database
        classificationService = new TestableInteractiveClassificationService();
    }
    
    @Test
    @DisplayName("Test intelligent fallback suggestions for unknown patterns")
    public void testIntelligentFallbackSuggestions() throws SQLException {
        // Setup - ensure no rules exist for "bank fee"
        String pattern = "bank fee";
        
        // Execute - this should trigger fallback logic since no rules exist in test DB
        List<String> suggestions = classificationService.suggestAccountsForPattern(pattern);
        
        // Verify
        assertNotNull(suggestions, "Suggestions should not be null");
        assertTrue(suggestions.size() > 0, "Should return fallback suggestions");
        assertEquals("Bank Charges - Financial expenses", suggestions.get(0), "First suggestion should be bank charges for 'bank fee' pattern");
    }
    
    /**
     * Testable subclass that overrides database connection methods
     */
    private class TestableInteractiveClassificationService extends InteractiveClassificationService {
        @Override
        protected Connection getConnection() throws SQLException {
            // Use PostgreSQL test database instead of H2
            return DriverManager.getConnection(TestConfiguration.TEST_DB_URL);
        }
    }
}
