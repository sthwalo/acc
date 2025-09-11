package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the InteractiveClassificationService
 */
public class TestInteractiveClassificationService {
    
    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockPreparedStatement;
    
    @Mock
    private ResultSet mockResultSet;
    
    @Mock
    private TransactionMappingService mockMappingService;
    
    private InteractiveClassificationService classificationService;
    
    @BeforeEach
    public void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        
        // Mock database connection behavior
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        
        // Create a custom subclass for testing to avoid database connections
        classificationService = new TestableInteractiveClassificationService();
    }
    
    @Test
    @DisplayName("Test batch classification of transactions")
    public void testBatchClassification() throws SQLException {
        // Setup
        List<Long> transactionIds = List.of(1L, 2L, 3L);
        String accountCode = "8800";
        String accountName = "Insurance";
        Long companyId = 1L;
        
        // Configure mock behavior
        when(mockResultSet.next()).thenReturn(true, true, true, false); // Three transactions
        when(mockResultSet.getLong("id")).thenReturn(1L, 2L, 3L);
        when(mockResultSet.getString("details")).thenReturn(
            "Insurance Premium ABC123",
            "Monthly Insurance",
            "Outsurance Payment"
        );
        
        // Execute
        int result = classificationService.classifyTransactionsBatch(transactionIds, accountCode, accountName, companyId);
        
        // Verify
        assertEquals(3, result, "Should classify all three transactions");
        verify(mockPreparedStatement, times(1)).setArray(eq(1), any()); // For the SELECT query
        verify(mockPreparedStatement, times(3)).setString(eq(1), eq(accountCode)); // For each UPDATE
        verify(mockPreparedStatement, times(3)).setString(eq(2), eq(accountName)); // For each UPDATE
    }
    
    @Test
    @DisplayName("Test suggestion of accounts for transaction patterns")
    public void testSuggestAccountsForPattern() throws SQLException {
        // Setup
        String pattern = "insurance";
        
        // Configure mock behavior for rule-based suggestions
        when(mockResultSet.next()).thenReturn(true, true, false); // Two suggestions from rules
        when(mockResultSet.getString("account_code")).thenReturn("8800", "8850");
        when(mockResultSet.getString("account_name")).thenReturn("Insurance", "Medical Insurance");
        
        // Execute
        List<String> suggestions = classificationService.suggestAccountsForPattern(pattern);
        
        // Verify
        assertNotNull(suggestions, "Suggestions should not be null");
        assertEquals(2, suggestions.size(), "Should return two suggestions");
        assertEquals("8800 - Insurance", suggestions.get(0), "First suggestion should match");
        assertEquals("8850 - Medical Insurance", suggestions.get(1), "Second suggestion should match");
    }
    
    @Test
    @DisplayName("Test intelligent fallback suggestions for unknown patterns")
    public void testIntelligentFallbackSuggestions() throws SQLException {
        // Setup
        String pattern = "bank fee";
        
        // Configure mock behavior for no rule-based suggestions
        when(mockResultSet.next()).thenReturn(false); // No suggestions from rules
        
        // Execute
        List<String> suggestions = classificationService.suggestAccountsForPattern(pattern);
        
        // Verify
        assertNotNull(suggestions, "Suggestions should not be null");
        assertTrue(suggestions.size() > 0, "Should return fallback suggestions");
        assertEquals("9600 - Bank Charges", suggestions.get(0), "First suggestion should be bank charges for 'bank fee' pattern");
    }
    
    @Test
    @DisplayName("Test creation of mapping rule")
    public void testCreateMappingRule() throws SQLException {
        // Setup
        Long companyId = 1L;
        String pattern = "Monthly Insurance Premium";
        String accountCode = "8800";
        String accountName = "Insurance";
        
        // Configure mock behavior for checking existing rules
        when(mockResultSet.next()).thenReturn(false); // No existing rule
        
        // Execute
        classificationService.createMappingRule(companyId, pattern, accountCode, accountName);
        
        // Verify appropriate SQL execution
        verify(mockPreparedStatement, times(1)).setLong(eq(1), eq(companyId)); // For the check
        verify(mockPreparedStatement, times(1)).setString(eq(2), eq(accountCode)); // For the check
        verify(mockPreparedStatement, times(1)).setString(eq(3), any()); // For the pattern check
    }
    
    /**
     * Testable subclass that overrides database connection methods
     */
    private class TestableInteractiveClassificationService extends InteractiveClassificationService {
        @Override
        protected Connection getConnection() throws SQLException {
            return mockConnection;
        }
        
        // Override to provide our mock service
        @Override
        protected TransactionMappingService createMappingService() {
            return mockMappingService;
        }
    }
    
    /**
     * Helper method to create a sample bank transaction
     */
    private BankTransaction createSampleTransaction(Long id, String details, BigDecimal amount) {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(id);
        transaction.setDetails(details);
        transaction.setTransactionDate(LocalDate.now());
        
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            transaction.setDebitAmount(amount.abs());
        } else {
            transaction.setCreditAmount(amount);
        }
        
        return transaction;
    }
}
