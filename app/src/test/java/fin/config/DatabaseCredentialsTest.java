package fin.config;

import fin.TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that database connections work correctly with embedded credentials
 * This is especially important for CI/CD environments where the system user might not exist in PostgreSQL
 */
public class DatabaseCredentialsTest {

    @Test
    @DisplayName("Test that embedded credentials in URL work correctly with direct DriverManager")
    public void testEmbeddedCredentialsInUrl() throws SQLException {
        // This test confirms that our fix for CI/CD environments works properly
        
        // Get the credentials-embedded URL from TestConfiguration
        String urlWithCredentials = TestConfiguration.TEST_DB_URL_WITH_CREDENTIALS;
        
        // Verify the URL contains credentials
        assertTrue(urlWithCredentials.contains("user="), "URL should contain user parameter");
        assertTrue(urlWithCredentials.contains("password="), "URL should contain password parameter");
        
        // Try a direct connection with single parameter - this is what CI/CD uses
        try (Connection conn = DriverManager.getConnection(urlWithCredentials);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
            
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("test"), "Query should return 1");
        }
    }
    
    @Test
    @DisplayName("Test that DatabaseConfig getTestConnection(url) embeds credentials automatically")
    public void testGetTestConnectionWithSingleParam() throws SQLException {
        // This test verifies that our new getTestConnection(url) method works
        
        // Try connecting with a URL that doesn't have credentials
        try (Connection conn = DatabaseConfig.getTestConnection(TestConfiguration.TEST_DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
            
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("test"), "Query should return 1");
        }
    }
    
    @Test
    @DisplayName("Test that TestDatabaseConfig.getDatabaseUrlWithCredentials() works correctly")
    public void testTestDatabaseConfigCredentials() throws SQLException {
        // This test verifies that TestDatabaseConfig.getDatabaseUrlWithCredentials() works
        
        String urlWithCreds = TestDatabaseConfig.getDatabaseUrlWithCredentials();
        
        // Verify the URL contains credentials
        assertTrue(urlWithCreds.contains("user="), "URL should contain user parameter");
        assertTrue(urlWithCreds.contains("password="), "URL should contain password parameter");
        
        // Try a direct connection with this URL
        try (Connection conn = DriverManager.getConnection(urlWithCreds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
            
            assertTrue(rs.next(), "Result set should have at least one row");
            assertEquals(1, rs.getInt("test"), "Query should return 1");
        }
    }
    
    @Test
    @DisplayName("Test that DatabaseConfig.getDatabaseUrlWithCredentials() works for production connection")
    public void testDatabaseConfigCredentialsProduction() throws SQLException {
        // This test verifies that DatabaseConfig.getDatabaseUrlWithCredentials() works for production
        
        String urlWithCreds = DatabaseConfig.getDatabaseUrlWithCredentials();
        
        // Verify the URL contains credentials
        assertTrue(urlWithCreds.contains("user="), "URL should contain user parameter");
        assertTrue(urlWithCreds.contains("password="), "URL should contain password parameter");
        
        // Note: We don't actually test connecting with production credentials in unit tests
        // Just verify the URL format is correct
        System.out.println("✅ Production URL with embedded credentials has correct format");
    }
}