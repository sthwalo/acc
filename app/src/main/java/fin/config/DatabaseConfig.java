/*
 * Copyright 2025 Immaculate Nyoni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you     public static void close() {
        // For direct JDBC connections, no connection pool to close
        System.out.println("🔒 Database connections closed");
    }t use this file except in compliance with the License.
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
import java.sql.SQLException;

/**
 * Database configuration and connection management
 * Supports both PostgreSQL (production) and SQLite (testing)
 */
public class DatabaseConfig {
    private static String databaseUrl;
    private static String databaseUser;
    private static String databasePassword;
    private static boolean usePostgreSQL = true;
    
    static {
        initializeConfiguration();
    }
    
    private static void initializeConfiguration() {
        // Check environment variables for PostgreSQL configuration
        String dbUrl = System.getenv("DATABASE_URL");
        databaseUser = System.getenv("DATABASE_USER");
        databasePassword = System.getenv("DATABASE_PASSWORD");
        
        System.out.println("🔍 Debug - DATABASE_URL from env: " + dbUrl);
        System.out.println("🔍 Debug - DATABASE_USER from env: " + databaseUser);
        System.out.println("🔍 Debug - DATABASE_PASSWORD from env: " + (databasePassword != null ? "[SET]" : "[NOT SET]"));
        
        // Default to your PostgreSQL configuration if not set
        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
            databaseUser = "sthwalonyoni";
            databasePassword = "drimPro1823";
            System.out.println("🔍 Using default PostgreSQL configuration");
        } else {
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
        }
        
        // Check if we should use SQLite for testing
        String useSqlite = System.getProperty("test.database", "false");
        if ("true".equals(useSqlite)) {
            usePostgreSQL = false;
            databaseUrl = "jdbc:sqlite:test.db";
            databaseUser = null;
            databasePassword = null;
            return;
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
            System.err.println("🔄 Falling back to SQLite for development...");
            
            usePostgreSQL = false;
            databaseUrl = "jdbc:sqlite:fin_database.db";
            databaseUser = null;
            databasePassword = null;
        }
    }
    
    /**
     * Get a database connection
     * Uses connection pool for PostgreSQL, direct connection for SQLite
     */
    public static Connection getConnection() throws SQLException {
        if (usePostgreSQL) {
            return DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
        } else {
            return DriverManager.getConnection(databaseUrl);
        }
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
        return usePostgreSQL;
    }
    
    /**
     * Get database type for SQL syntax adjustments
     */
    public static String getDatabaseType() {
        return usePostgreSQL ? "postgresql" : "sqlite";
    }
    
    /**
     * Close the connection pool (call on application shutdown)
     */
    public static void close() {
        // For direct JDBC connections, no connection pool to close
        System.out.println("� Database connections closed");
    }
    
    /**
     * Test database connectivity
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            String sql = usePostgreSQL ? "SELECT 1" : "SELECT 1";
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery(sql);
            boolean success = rs.next() && rs.getInt(1) == 1;
            
            if (success) {
                System.out.println("✅ Database connection test successful");
                System.out.println("📊 Using: " + (usePostgreSQL ? "PostgreSQL" : "SQLite"));
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
        return usePostgreSQL ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
    }
    
    /**
     * Get appropriate SQL syntax for boolean values
     */
    public static String getBooleanSyntax(boolean value) {
        if (usePostgreSQL) {
            return value ? "TRUE" : "FALSE";
        } else {
            return value ? "1" : "0";
        }
    }
    
    /**
     * Get appropriate SQL syntax for current timestamp
     */
    public static String getCurrentTimestampSyntax() {
        return usePostgreSQL ? "CURRENT_TIMESTAMP" : "datetime('now')";
    }
}
