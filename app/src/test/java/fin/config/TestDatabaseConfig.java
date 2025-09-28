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
                        System.out.println("🧪 Loaded " + key + " from .env file");
                    }
                }
                System.out.println("🧪 Environment variables loaded from .env file");
            } else {
                System.out.println("🧪 .env file not found, using system environment variables only");
            }
        } catch (Exception e) {
            System.err.println("🧪 Failed to load .env file: " + e.getMessage());
            // Continue with system environment variables
        }
    }

    private static void initializeTestConfiguration() {
        // Read test database configuration from system properties (set by TestConfiguration)
        String dbUrl = System.getProperty("TEST_DATABASE_URL");
        testDatabaseUser = System.getProperty("TEST_DATABASE_USER");
        testDatabasePassword = System.getProperty("TEST_DATABASE_PASSWORD");

        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_URL from system property: " + dbUrl);
        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_USER from system property: " + testDatabaseUser);
        System.out.println("🧪 TestDatabaseConfig - TEST_DATABASE_PASSWORD from system property: " + (testDatabasePassword != null ? "[SET]" : "[NOT SET]"));

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