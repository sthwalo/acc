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
    
    /**
     * Load environment variables from .env file if it exists
     */
    private static void loadEnvironmentVariables() {
        try {
            java.io.File envFile = new java.io.File(".env");
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
    
    private static void initializeConfiguration() {
        loadConfiguration();
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
            databaseUser = System.getenv("TEST_DATABASE_USER");
            databasePassword = System.getenv("TEST_DATABASE_PASSWORD");
            System.out.println("🧪 DatabaseConfig using test database configuration");
            System.out.println("🔍 Test databaseUrl: " + databaseUrl);
            System.out.println("🔍 Test databaseUser: " + databaseUser);
            return;
        }
        
        // Read production database configuration from environment variables
        String dbUrl = System.getenv("DATABASE_URL");
        databaseUser = System.getenv("DATABASE_USER");
        databasePassword = System.getenv("DATABASE_PASSWORD");
        
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
}
