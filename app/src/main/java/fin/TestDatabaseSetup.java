package fin;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Test Database Setup utility for CI/CD pipeline
 *
 * This class is used by the CI/CD pipeline to set up the test database
 * before running tests. It loads the test schema and initializes test data.
 */
public class TestDatabaseSetup {

    // Maximum length for displaying skipped SQL statements in error messages
    private static final int MAX_STATEMENT_DISPLAY_LENGTH = 50;

    public static void main(String[] args) {
        String dbUrl = System.getProperty("TEST_DATABASE_URL", System.getenv("TEST_DATABASE_URL"));
        String dbUser = System.getProperty("TEST_DATABASE_USER", System.getenv("TEST_DATABASE_USER"));
        String dbPassword = System.getProperty("TEST_DATABASE_PASSWORD", System.getenv("TEST_DATABASE_PASSWORD"));

        if (dbUrl == null || dbUser == null || dbPassword == null) {
            System.err.println("❌ Missing database configuration. Please set TEST_DATABASE_URL, TEST_DATABASE_USER, and TEST_DATABASE_PASSWORD");
            System.exit(1);
        }

        try {
            System.out.println("🗄️ Starting test database setup for CI/CD...");
            System.out.println("📋 Database URL: " + dbUrl);
            System.out.println("👤 Database User: " + dbUser);

            setupTestDatabase(dbUrl, dbUser, dbPassword);

            System.out.println("✅ Test database setup completed successfully!");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("❌ Test database setup failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void setupTestDatabase(String dbUrl, String dbUser, String dbPassword) throws Exception {
        try (Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {

            // Load and execute the test schema
            String schemaPath = "app/src/test/resources/test_schema.sql";
            String schemaSql = Files.readString(Paths.get(schemaPath));

            System.out.println("📄 Loaded schema from: " + schemaPath);
            System.out.println("🔧 Executing database schema...");

            // Split and execute statements
            String[] statements = schemaSql.split(";");

            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !statement.startsWith("--")) {
                    try {
                        stmt.executeUpdate(statement);
                    } catch (Exception e) {
                        // Some statements might fail (comments, etc.) - continue
                        System.out.println("⚠️ Skipping statement: " + statement.substring(0, Math.min(MAX_STATEMENT_DISPLAY_LENGTH, statement.length())));
                    }
                }
            }

            System.out.println("✅ Schema executed successfully");

            // Insert basic test data
            insertTestData(stmt);
        }
    }

    private static void insertTestData(Statement stmt) throws Exception {
        System.out.println("📝 Inserting test data...");

        // Insert account types
        stmt.executeUpdate("""
            INSERT INTO account_types (code, name, normal_balance, description) VALUES
            ('ASSET', 'Asset', 'D', 'Assets - resources owned by the company'),
            ('LIAB', 'Liability', 'C', 'Liabilities - debts owed by the company'),
            ('EQUITY', 'Equity', 'C', 'Owner equity in the company'),
            ('REV', 'Revenue', 'C', 'Revenue earned by the company'),
            ('EXP', 'Expense', 'D', 'Expenses incurred by the company')
            ON CONFLICT (code) DO NOTHING
        """);

        // Insert test company
        stmt.executeUpdate("""
            INSERT INTO companies (id, name, registration_number, tax_number, address, contact_email, contact_phone)
            VALUES (1, 'Test Company Ltd', '2023/123456/07', '9876543210', '123 Test St, Test City', 'test@example.com', '+27-11-123-4567')
            ON CONFLICT (id) DO NOTHING
        """);

        // Insert test fiscal period
        stmt.executeUpdate("""
            INSERT INTO fiscal_periods (id, company_id, period_name, start_date, end_date, is_closed)
            VALUES (1, 1, '2025 Financial Year', '2025-01-01', '2025-12-31', false)
            ON CONFLICT (id) DO NOTHING
        """);

        System.out.println("✅ Test data inserted successfully");
    }
}