/*
 * Copyright 2025 Immaculate Nyoni
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
 * Database configuration and connection management
 * Supports PostgreSQL database connections
 */
public class DatabaseConfig {
    private static String databaseUrl;
    private static String databaseUser;
    private static String databasePassword;
    
    static {
        loadEnvironmentVariables();
        initializeConfiguration();
    }
    
    private static void initializeConfiguration() {
        loadConfiguration();
    }
    
    /**
     * Load environment variables from .env file if it exists
     */
    private static void loadEnvironmentVariables() {
        try {
            // Try current directory first (for production)
            java.io.File envFile = new java.io.File(".env");
            if (!envFile.exists()) {
                // Try parent directory (for tests running from app/ subdirectory)
                envFile = new java.io.File("../.env");
            }
            
            if (envFile.exists()) {
                java.util.Properties envProps = new java.util.Properties();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(envFile)) {
                    envProps.load(fis);
                }
                
                // Set environment variables if not already set
                for (String key : envProps.stringPropertyNames()) {
                    if (System.getenv(key) == null) {
                        System.setProperty(key, envProps.getProperty(key));
                        System.out.println("🔍 Loaded " + key + " from .env file");
                    }
                }
                System.out.println("🔍 Environment variables loaded from .env file");
            } else {
                System.out.println("🔍 .env file not found, using system environment variables only");
            }
        } catch (Exception e) {
            System.err.println("🔍 Failed to load .env file: " + e.getMessage());
            // Continue with system environment variables
        }
    }
    
    /**
     * Get configuration value from either system property or environment variable
     */
    private static String getConfigValue(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
    
    /**
     * Detect if we're running in a test context (JUnit, Gradle test task, or CI/CD)
     * This prevents TEST_DATABASE_* vars from being used during normal application runtime
     */
    private static boolean isRunningInTestContext() {
        // Check 1: JUnit is on the classpath and test classes are being executed
        try {
            // Check if JUnit's Test annotation is available
            Class.forName("org.junit.jupiter.api.Test");
            
            // Check the stack trace for test execution indicators
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                String methodName = element.getMethodName();
                
                // JUnit 5 test runner
                if (className.startsWith("org.junit.") || 
                    className.startsWith("org.gradle.api.internal.tasks.testing")) {
                    return true;
                }
                
                // Gradle test task
                if (className.contains("GradleWorkerMain") || 
                    className.contains("TestExecuter")) {
                    return true;
                }
                
                // Test class being executed (ends with Test)
                if (className.endsWith("Test") && !methodName.equals("getStackTrace")) {
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            // JUnit not on classpath - definitely not a test
            return false;
        }
        
        // Check 2: CI/CD environment variables (GitHub Actions, GitLab CI, etc.)
        String ciEnv = System.getenv("CI");
        String githubActions = System.getenv("GITHUB_ACTIONS");
        if ("true".equalsIgnoreCase(ciEnv) || "true".equalsIgnoreCase(githubActions)) {
            System.out.println("🔍 CI/CD environment detected (CI=" + ciEnv + ", GITHUB_ACTIONS=" + githubActions + ")");
            return true;
        }
        
        // Check 3: Gradle test system property
        String gradleTest = System.getProperty("gradle.test");
        if ("true".equalsIgnoreCase(gradleTest)) {
            return true;
        }
        
        // Not in test context
        return false;
    }
    
    /**
     * Load database configuration from environment variables
     * Called both in static initializer and can be called again to refresh
     */
    public static void loadConfiguration() {
        // Check if we're in test mode and use test database configuration
        String testDbUrl = System.getProperty("fin.database.test.url");
        if (testDbUrl != null && !testDbUrl.isEmpty()) {
            databaseUrl = testDbUrl;
            databaseUser = getConfigValue("TEST_DATABASE_USER");
            databasePassword = getConfigValue("TEST_DATABASE_PASSWORD");
            System.out.println("🧪 DatabaseConfig using test database configuration");
            System.out.println("🔍 Test databaseUrl: " + databaseUrl);
            System.out.println("🔍 Test databaseUser: " + databaseUser);
            return;
        }
        
        // Check if we're in test execution context (JUnit, Gradle test task, CI/CD)
        boolean isTestContext = isRunningInTestContext();
        
        // If in test context, check for TEST_DATABASE_* environment variables
        if (isTestContext) {
            String testEnvDbUrl = getConfigValue("TEST_DATABASE_URL");
            if (testEnvDbUrl != null && !testEnvDbUrl.isEmpty()) {
                System.out.println("🧪 Test execution context detected - using TEST_DATABASE_* environment variables");
                databaseUrl = testEnvDbUrl;
                databaseUser = getConfigValue("TEST_DATABASE_USER");
                databasePassword = getConfigValue("TEST_DATABASE_PASSWORD");
                
                if (databaseUser != null && databasePassword != null) {
                    System.out.println("✅ Test database configuration loaded from environment");
                    System.out.println("🔍 Test databaseUrl: " + databaseUrl);
                    System.out.println("🔍 Test databaseUser: " + databaseUser);
                    
                    // Validate test connection
                    try {
                        Class.forName("org.postgresql.Driver");
                        try (Connection testConn = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)) {
                            System.out.println("✅ Test PostgreSQL connection successful");
                        }
                    } catch (Exception e) {
                        System.err.println("⚠️ Test database connection failed: " + e.getMessage());
                        // Continue anyway - tests might set up database later
                    }
                    return;
                }
            }
        }
        
        // Read production database configuration from environment variables
        String dbUrl = getConfigValue("DATABASE_URL");
        databaseUser = getConfigValue("DATABASE_USER");
        databasePassword = getConfigValue("DATABASE_PASSWORD");
        
        System.out.println("🔍 DATABASE_URL from env: " + dbUrl);
        System.out.println("🔍 DATABASE_USER from env: " + databaseUser);
        System.out.println("🔍 DATABASE_PASSWORD from env: " + (databasePassword != null ? "[SET]" : "[NOT SET]"));
        
        // Require environment variables - no hardcoded defaults
        if (dbUrl == null || databaseUser == null || databasePassword == null) {
            throw new RuntimeException("Database configuration missing. Please set DATABASE_URL, DATABASE_USER, and DATABASE_PASSWORD environment variables in .env file or system environment.");
        }
        
        // Handle different URL formats
        if (dbUrl.startsWith("postgresql://")) {
            // Convert PostgreSQL URI to JDBC URL and extract credentials
            if (dbUrl.contains("@")) {
                // URL format: postgresql://user:password@host:port/database
                String urlPart = dbUrl.substring("postgresql://".length());
                int atIndex = urlPart.indexOf("@");
                String credentials = urlPart.substring(0, atIndex);
                String hostPortDb = urlPart.substring(atIndex + 1);
                
                if (credentials.contains(":")) {
                    String[] credParts = credentials.split(":", 2);
                    databaseUser = credParts[0];
                    databasePassword = credParts[1];
                }
                
                dbUrl = "jdbc:postgresql://" + hostPortDb;
                System.out.println("🔍 Extracted credentials from PostgreSQL URI");
                System.out.println("🔍 User: " + databaseUser);
                System.out.println("🔍 Host/Port/DB: " + hostPortDb);
            } else {
                // Convert PostgreSQL URI to JDBC URL
                dbUrl = "jdbc:" + dbUrl;
            }
            System.out.println("🔍 Converted PostgreSQL URI to JDBC URL: " + dbUrl);
        } else if (!dbUrl.startsWith("jdbc:")) {
            // If it doesn't start with jdbc: or postgresql://, assume it's a JDBC URL missing the prefix
            dbUrl = "jdbc:" + dbUrl;
            System.out.println("🔍 Added jdbc: prefix to URL: " + dbUrl);
        }
        
        databaseUrl = dbUrl;
        System.out.println("🔍 Final databaseUrl: " + databaseUrl);
        
        try {
            // Test PostgreSQL connection
            Class.forName("org.postgresql.Driver");
            System.out.println("✅ PostgreSQL driver loaded successfully");
            
            System.out.println("🔗 Attempting connection with:");
            System.out.println("   URL: " + dbUrl);
            System.out.println("   User: " + databaseUser);
            System.out.println("   Password: " + (databasePassword != null ? "[SET]" : "[NOT SET]"));
            
            try (Connection testConn = DriverManager.getConnection(dbUrl, databaseUser, databasePassword)) {
                System.out.println("✅ PostgreSQL connection successful");
                System.out.println("📊 Database: " + dbUrl);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to PostgreSQL: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Database connection failed: " + e.getMessage());
        }
    }
    
    /**
     * Get a database connection
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
    }
    
    /**
     * Get the current database URL
     */
    public static String getDatabaseUrl() {
        return databaseUrl;
    }
    
    /**
     * Get the database URL with embedded credentials for legacy code compatibility
     * This ensures that code using DriverManager.getConnection(url) directly will work
     * 
     * @return JDBC URL with user and password parameters embedded
     */
    public static String getDatabaseUrlWithCredentials() {
        if (databaseUrl == null) {
            return null;
        }
        // Add user and password as URL parameters
        String separator = databaseUrl.contains("?") ? "&" : "?";
        return databaseUrl + separator + "user=" + databaseUser + "&password=" + databasePassword;
    }
    
    /**
     * Check if using PostgreSQL
     */
    public static boolean isUsingPostgreSQL() {
        return true;
    }
    
    /**
     * Get database type for SQL syntax adjustments
     */
    public static String getDatabaseType() {
        return "postgresql";
    }
    
    /**
     * Close the connection pool (call on application shutdown)
     */
    public static void close() {
        // For direct JDBC connections, no connection pool to close
        System.out.println("🔒 Database connections closed");
    }
    
    /**
     * Test database connectivity
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            
            boolean success = rs.next() && rs.getInt(1) == 1;
            
            if (success) {
                System.out.println("✅ Database connection test successful");
                System.out.println("📊 Using: PostgreSQL");
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("❌ Database connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get appropriate SQL syntax for auto-increment
     */
    public static String getAutoIncrementSyntax() {
        return "SERIAL PRIMARY KEY";
    }
    
    /**
     * Get appropriate SQL syntax for boolean values
     */
    public static String getBooleanSyntax(boolean value) {
        return value ? "TRUE" : "FALSE";
    }
    
    /**
     * Get appropriate SQL syntax for current timestamp
     */
    public static String getCurrentTimestampSyntax() {
        return "CURRENT_TIMESTAMP";
    }
    
    /**
     * Get a test database connection with explicit credentials
     * Used by TestConfiguration to avoid interfering with production DatabaseConfig state
     */
    public static Connection getTestConnection(String url, String user, String password) throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
    
    /**
     * Get a test database connection with credentials embedded in URL
     * This is a convenience method for test classes
     */
    public static Connection getTestConnection(String url) throws SQLException {
        // If the URL already contains credentials, use it directly
        if (url.contains("user=") && url.contains("password=")) {
            return DriverManager.getConnection(url);
        }
        
        // Otherwise, try to get credentials from environment variables
        String testUser = System.getProperty("TEST_DATABASE_USER");
        if (testUser == null) {
            testUser = System.getenv("TEST_DATABASE_USER");
        }
        
        String testPassword = System.getProperty("TEST_DATABASE_PASSWORD");
        if (testPassword == null) {
            testPassword = System.getenv("TEST_DATABASE_PASSWORD");
        }
        
        // If we have credentials, add them to the URL
        if (testUser != null && testPassword != null) {
            String separator = url.contains("?") ? "&" : "?";
            String urlWithCreds = url + separator + "user=" + testUser + "&password=" + testPassword;
            return DriverManager.getConnection(urlWithCreds);
        }
        
        // Otherwise, fall back to using the URL directly (this will likely fail with auth error)
        return DriverManager.getConnection(url);
    }
}
