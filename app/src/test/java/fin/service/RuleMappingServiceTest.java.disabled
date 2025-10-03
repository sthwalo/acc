package fin.service;

import fin.model.RuleMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development for RuleMappingService module
 * This module handles rule-based transaction classification from database
 *
 * STATUS: Service implemented, tests enabled
 * TODO: Verify integration with database and rule matching logic
 */
public class RuleMappingServiceTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private Statement mockStatement;

    private RuleMappingService ruleService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        ruleService = new TestableRuleMappingService("jdbc:test:db", mockConnection);
    }

    @Test
    void testLoadTransactionMappingRules_Success() throws SQLException {
        // Given
        when(mockResultSet.next())
            .thenReturn(true)  // First rule
            .thenReturn(true)  // Second rule
            .thenReturn(false); // No more rules

        when(mockResultSet.getString("match_value"))
            .thenReturn("INSURANCE PREMIUM")
            .thenReturn("SALARY PAYMENT");

        when(mockResultSet.getString("account_code"))
            .thenReturn("8800")
            .thenReturn("8100");

        when(mockResultSet.getString("account_name"))
            .thenReturn("Insurance")
            .thenReturn("Employee Costs");

        // When
        Map<String, RuleMapping> rules = ruleService.loadTransactionMappingRules(1L);

        // Then
        assertEquals(2, rules.size());
        assertTrue(rules.containsKey("INSURANCE PREMIUM"));
        assertTrue(rules.containsKey("SALARY PAYMENT"));

        RuleMapping insuranceRule = rules.get("INSURANCE PREMIUM");
        assertEquals("8800", insuranceRule.getAccountCode());
        assertEquals("Insurance", insuranceRule.getAccountName());
    }

    @Test
    void testLoadTransactionMappingRules_EmptyResult() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(false);

        // When
        Map<String, RuleMapping> rules = ruleService.loadTransactionMappingRules(1L);

        // Then
        assertTrue(rules.isEmpty());
    }

    @Test
    void testFindMatchingRule_ExactMatch() {
        // Given
        Map<String, RuleMapping> rules = Map.of(
            "INSURANCE PREMIUM", new RuleMapping("8800", "Insurance"),
            "SALARY PAYMENT", new RuleMapping("8100", "Employee Costs")
        );

        // When
        RuleMapping result = ruleService.findMatchingRule("INSURANCE PREMIUM PAYMENT TO DOTSURE", rules);

        // Then
        assertNotNull(result);
        assertEquals("8800", result.getAccountCode());
        assertEquals("Insurance", result.getAccountName());
    }

    @Test
    void testFindMatchingRule_NoMatch() {
        // Given
        Map<String, RuleMapping> rules = Map.of(
            "INSURANCE PREMIUM", new RuleMapping("8800", "Insurance")
        );

        // When
        RuleMapping result = ruleService.findMatchingRule("UNKNOWN TRANSACTION", rules);

        // Then
        assertNull(result);
    }

    @Test
    void testCreateStandardMappingRules() throws SQLException {
        // Given
        when(mockResultSet.next())
            .thenReturn(false) // No existing rules
            .thenReturn(true); // Account exists

        when(mockResultSet.getLong(1)).thenReturn(1000L); // Account ID

        // When
        int rulesCreated = ruleService.createStandardMappingRules(1L);

        // Then
        assertTrue(rulesCreated > 0); // Should create multiple rules
        verify(mockPreparedStatement, atLeast(8)).executeUpdate(); // At least 8 rules
    }

    @Test
    void testCreateMappingRule_NewRule() throws SQLException {
        // Given
        when(mockResultSet.next())
            .thenReturn(false) // Rule doesn't exist
            .thenReturn(true); // Account exists

        when(mockResultSet.getLong(1)).thenReturn(1000L); // Account ID

        // When
        ruleService.createMappingRule(1L, "BANK FEE", "9600", "Bank Charges");

        // Then
        verify(mockPreparedStatement, times(2)).executeQuery(); // Check existing + get account
        verify(mockPreparedStatement).executeUpdate(); // Insert rule
    }

    @Test
    void testCreateMappingRule_AccountNotFound() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(false); // Account doesn't exist

        // When & Then
        assertThrows(RuntimeException.class, () ->
            ruleService.createMappingRule(1L, "UNKNOWN", "9999", "Unknown Account")
        );
    }

    @Test
    void testClearExistingMappingRules() throws SQLException {
        // Given
        when(mockPreparedStatement.executeUpdate()).thenReturn(5); // 5 rules deleted

        // When
        ruleService.clearExistingMappingRules(1L);

        // Then
        verify(mockPreparedStatement).setLong(1, 1L);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    void testCreateTransactionMappingRulesTable_NewTable() throws SQLException {
        // Given
        when(mockConnection.getMetaData()).thenReturn(mock(java.sql.DatabaseMetaData.class));
        when(mockConnection.getMetaData().getTables(any(), any(), any(), any())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false); // Table doesn't exist

        // When
        ruleService.createTransactionMappingRulesTable();

        // Then
        verify(mockStatement, atLeast(3)).execute(anyString()); // CREATE TABLE + INDEXES
    }

    @Test
    void testCreateTransactionMappingRulesTable_ExistingTable() throws SQLException {
        // Given
        when(mockConnection.getMetaData()).thenReturn(mock(java.sql.DatabaseMetaData.class));
        when(mockConnection.getMetaData().getTables(any(), any(), any(), any())).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true); // Table exists

        // When
        ruleService.createTransactionMappingRulesTable();

        // Then
        verify(mockPreparedStatement, never()).executeUpdate(); // No table creation
    }

    // Helper methods
    private RuleMapping createRuleMapping(String accountCode, String accountName) {
        return new RuleMapping(accountCode, accountName);
    }
}

/**
 * Testable version of RuleMappingService that uses a mock connection
 */
class TestableRuleMappingService extends RuleMappingService {
    private final Connection mockConnection;

    public TestableRuleMappingService(String dbUrl, Connection mockConnection) {
        super(dbUrl);
        this.mockConnection = mockConnection;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return mockConnection;
    }
}