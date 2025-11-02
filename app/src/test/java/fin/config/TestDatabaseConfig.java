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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Test database configuration that mirrors DatabaseConfig but uses TEST_ environment variables
 * This allows tests to have their own database configuration separate from production
 */
public class TestDatabaseConfig {
    private static String testDatabaseUrl;
    private static String testDatabaseUser;
    private static String testDatabasePassword;

    static {
        loadEnvironmentVariables();
        initializeTestConfiguration();
    }

    /**
     * Load environment variables from test.env file ONLY if not already set
     * This ensures CI/CD environment variables take precedence over local test.env file
     */
    private static void loadEnvironmentVariables() {
        // Check if key environment variables are already set
        boolean envVarsAlreadySet = System.getenv("TEST_DATABASE_URL") != null 
                && System.getenv("TEST_DATABASE_USER") != null 
                && System.getenv("TEST_DATABASE_PASSWORD") != null;
        
        if (envVarsAlreadySet) {
            System.out.println("🧪 Using environment variables (CI/CD mode) - skipping test.env file");
            return;
        }
        
        // Try multiple possible locations for test.env file
        try {
            java.io.File envFile = null;
            
            // Location 1: app/src/test/resources/test.env (standard location)
            java.io.File location1 = new java.io.File("app/src/test/resources/test.env");
            if (location1.exists() && location1.canRead()) {
                envFile = location1;
            }
            
            // Location 2: test.env in working directory (fallback)
            if (envFile == null) {
                java.io.File location2 = new java.io.File("test.env");
                if (location2.exists() && location2.canRead()) {
                    envFile = location2;
                }
            }
            
            if (envFile != null) {
                System.out.println("🧪 Found test.env file at: " + envFile.getAbsolutePath());
                java.util.Properties envProps = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(envFile)) {
                    envProps.load(fis);
                }
                
                // Set as system properties
                for (String key : envProps.stringPropertyNames()) {
                    System.setProperty(key, envProps.getProperty(key));
                    System.out.println("🧪 Loaded " + key + " from test.env file");
                }
                System.out.println("🧪 Environment variables loaded from test.env file (local mode)");
            } else {
                System.out.println("🧪 test.env file not found, using environment variables only");
            }
        } catch (Exception e) {
            System.err.println("🧪 Failed to load test.env file: " + e.getMessage());
            // Continue with system environment variables
        }
    }

    private static void initializeTestConfiguration() {
        // Read test database configuration from system properties OR environment variables
        String dbUrl = System.getProperty("TEST_DATABASE_URL");
        if (dbUrl == null) {
            dbUrl = System.getenv("TEST_DATABASE_URL");
        }
        
        testDatabaseUser = System.getProperty("TEST_DATABASE_USER");
        if (testDatabaseUser == null) {
            testDatabaseUser = System.getenv("TEST_DATABASE_USER");
        }
        
        testDatabasePassword = System.getProperty("TEST_DATABASE_PASSWORD");
        if (testDatabasePassword == null) {
            testDatabasePassword = System.getenv("TEST_DATABASE_PASSWORD");
        }

        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_URL: " + dbUrl);
        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_USER: " + testDatabaseUser);
        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_PASSWORD: " + (testDatabasePassword != null ? "[SET]" : "[NOT SET]"));

        // Require environment variables - no hardcoded defaults
        if (dbUrl == null || testDatabaseUser == null || testDatabasePassword == null) {
            throw new RuntimeException("Test database configuration missing. Please set TEST_DATABASE_URL, TEST_DATABASE_USER, and TEST_DATABASE_PASSWORD environment variables.");
        }

        // Handle different URL formats (same as DatabaseConfig)
        if (dbUrl.startsWith("postgresql://")) {
            if (dbUrl.contains("@")) {
                String urlPart = dbUrl.substring("postgresql://".length());
                int atIndex = urlPart.indexOf("@");
                String credentials = urlPart.substring(0, atIndex);
                String hostPortDb = urlPart.substring(atIndex + 1);

                if (credentials.contains(":")) {
                    String[] credParts = credentials.split(":", 2);
                    testDatabaseUser = credParts[0];
                    testDatabasePassword = credParts[1];
                }

                dbUrl = "jdbc:postgresql://" + hostPortDb;
                System.out.println("🧪 Extracted test credentials from PostgreSQL URI");
            } else {
                dbUrl = "jdbc:" + dbUrl;
            }
        } else if (!dbUrl.startsWith("jdbc:")) {
            dbUrl = "jdbc:" + dbUrl;
        }

        testDatabaseUrl = dbUrl;
        System.out.println("🧪 Final test databaseUrl: " + testDatabaseUrl);

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ Test PostgreSQL driver loaded successfully");

            System.out.println("🧪 Testing test database connection with:");
            System.out.println("   URL: " + dbUrl);
            System.out.println("   User: " + testDatabaseUser);
            System.out.println("   Password: " + (testDatabasePassword != null ? "[SET]" : "[NOT SET]"));

            try (Connection testConn = DriverManager.getConnection(dbUrl, testDatabaseUser, testDatabasePassword)) {
                System.out.println("✅ Test database connection successful");
            }

        } catch (Exception e) {
            System.err.println("❌ Failed to connect to test database: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Test database connection failed: " + e.getMessage());
        }
    }

    /**
     * Get a test database connection
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(testDatabaseUrl, testDatabaseUser, testDatabasePassword);
    }

    /**
     * Get the test database URL
     */
    public static String getDatabaseUrl() {
        return testDatabaseUrl;
    }
    
    /**
     * Get the test database URL with embedded credentials for legacy code compatibility
     * This ensures that code using DriverManager.getConnection(url) directly will work
     * 
     * @return JDBC URL with user and password parameters embedded
     */
    public static String getDatabaseUrlWithCredentials() {
        if (testDatabaseUrl == null) {
            return null;
        }
        // Add user and password as URL parameters
        String separator = testDatabaseUrl.contains("?") ? "&" : "?";
        return testDatabaseUrl + separator + "user=" + testDatabaseUser + "&password=" + testDatabasePassword;
    }

    /**
     * Get the test database user
     */
    public static String getUsername() {
        return testDatabaseUser;
    }

    /**
     * Get the test database password
     */
    public static String getPassword() {
        return testDatabasePassword;
    }

    /**
     * Test test database connectivity
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {

            boolean success = rs.next() && rs.getInt(1) == 1;

            if (success) {
                System.out.println("✅ Test database connection test successful");
            }

            return success;

        } catch (SQLException e) {
            System.err.println("❌ Test database connection test failed: " + e.getMessage());
            return false;
        }
    }
}