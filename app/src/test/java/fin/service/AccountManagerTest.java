package fin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development for AccountManager module
 * This module handles account creation, lookup, and management
 */
public class AccountManagerTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    private AccountManager accountManager;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        // Setup mock connection
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        // Create a testable AccountManager that uses the mock connection
        accountManager = new TestableAccountManager("jdbc:test:db", mockConnection);
    }

    @Test
    void testGetOrCreateDetailedAccount_ExistingAccount() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("id")).thenReturn(1001L);

        // When
        Long accountId = accountManager.getOrCreateDetailedAccount(
            "2000-001", "Director Loan - Company Assist", "2000", "Long-term Liabilities");

        // Then
        assertEquals(1001L, accountId);
        verify(mockPreparedStatement).setLong(1, 1L); // companyId
        verify(mockPreparedStatement).setString(2, "2000-001");
    }

    @Test
    void testGetOrCreateDetailedAccount_NewAccount() throws SQLException {
        // Given - account doesn't exist
        when(mockResultSet.next()).thenReturn(false).thenReturn(true); // First query returns no result, second returns generated key
        when(mockResultSet.getLong(1)).thenReturn(2001L); // getLong(1) for generated keys
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Mock successful insert

        // When
        Long accountId = accountManager.getOrCreateDetailedAccount(
            "8800-002", "DOTSURE Insurance Premiums", "8800", "Insurance");

        // Then
        assertEquals(2001L, accountId);
        verify(mockPreparedStatement, times(1)).executeQuery(); // One for lookup
        verify(mockPreparedStatement, times(1)).executeUpdate(); // One for insert
    }

    @Test
    void testCreateDetailedAccount_Success() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong(1)).thenReturn(3001L); // getLong(1) for generated keys
        when(mockPreparedStatement.executeUpdate()).thenReturn(1); // Mock successful insert

        // When
        Long accountId = accountManager.createDetailedAccount(
            "8100-JOH", "Salary - John Doe", "8100", "Employee Costs");

        // Then
        assertEquals(3001L, accountId);
        verify(mockPreparedStatement).setLong(1, 1L); // company_id
        verify(mockPreparedStatement).setString(2, "8100-JOH");
        verify(mockPreparedStatement).setString(3, "Salary - John Doe");
        verify(mockPreparedStatement).setString(4, "8100");
        verify(mockPreparedStatement).setInt(5, 18); // Operating Expenses category
    }

    @Test
    void testGetAccountIdByCode_ExistingAccount() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("id")).thenReturn(1100L);

        // When
        Long accountId = accountManager.getAccountIdByCode(1L, "1100");

        // Then
        assertEquals(1100L, accountId);
        verify(mockPreparedStatement).setLong(1, 1L);
        verify(mockPreparedStatement).setString(2, "1100");
    }

    @Test
    void testGetAccountIdByCode_NonExistingAccount() throws SQLException {
        // Given
        when(mockResultSet.next()).thenReturn(false);

        // When
        Long accountId = accountManager.getAccountIdByCode(1L, "9999");

        // Then
        assertNull(accountId);
    }

    @Test
    void testGetCategoryIdForAccountCode_CurrentAssets() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("1000-001");

        // Then
        assertEquals(11, categoryId); // Current Assets
    }

    @Test
    void testGetCategoryIdForAccountCode_LongTermLiabilities() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("2000-001");

        // Then
        assertEquals(13, categoryId); // Current Liabilities (mapped to director loans)
    }

    @Test
    void testGetCategoryIdForAccountCode_OperatingRevenue() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("4000-001");

        // Then
        assertEquals(16, categoryId); // Operating Revenue
    }

    @Test
    void testGetCategoryIdForAccountCode_OtherIncome() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("5000-001");

        // Then
        assertEquals(17, categoryId); // Other Income
    }

    @Test
    void testGetCategoryIdForAccountCode_OperatingExpenses() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("8100-JOH");

        // Then
        assertEquals(18, categoryId); // Operating Expenses
    }

    @Test
    void testGetCategoryIdForAccountCode_BankFees() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("9600-001");

        // Then
        assertEquals(20, categoryId); // Finance Costs
    }

    @Test
    void testGetCategoryIdForAccountCode_Default() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode("9999-999");

        // Then
        assertEquals(18, categoryId); // Default to Operating Expenses
    }

    @Test
    void testGetCategoryIdForAccountCode_NullInput() {
        // When
        int categoryId = accountManager.getCategoryIdForAccountCode(null);

        // Then
        assertEquals(18, categoryId); // Default to Operating Expenses
    }
}

/**
 * Testable version of AccountManager that uses a mock connection
 */
class TestableAccountManager extends AccountManager {
    private final Connection mockConnection;

    public TestableAccountManager(String jdbcUrl, Connection mockConnection) {
        super(jdbcUrl);
        this.mockConnection = mockConnection;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return mockConnection;
    }
}