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