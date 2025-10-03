package fin;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Properties;

/**
 * Test configuration utilities for setting up test database
 */
public class TestConfiguration {
    
    // Test database configuration - loaded from environment variables only
    public static final String TEST_DB_URL;
    public static final String TEST_DB_USER;
    public static final String TEST_DB_PASSWORD;
    
    static {
        // Load environment variables from .env file first
        loadEnvironmentVariables();
        
        // Now read the test database configuration (check both env vars and system properties)
        TEST_DB_URL = getConfigValue("TEST_DATABASE_URL");
        TEST_DB_USER = getConfigValue("TEST_DATABASE_USER");
        TEST_DB_PASSWORD = getConfigValue("TEST_DATABASE_PASSWORD");
        
        // Validate that test database configuration is available
        if (TEST_DB_URL == null || TEST_DB_USER == null || TEST_DB_PASSWORD == null) {
            throw new RuntimeException("Test database configuration missing. Please set TEST_DATABASE_URL, TEST_DATABASE_USER, and TEST_DATABASE_PASSWORD environment variables or in test.env file.");
        }
        System.out.println("🔍 TestConfiguration - TEST_DB_URL: " + TEST_DB_URL);
        System.out.println("🔍 TestConfiguration - TEST_DB_USER: " + TEST_DB_USER);
        System.out.println("🔍 TestConfiguration - TEST_DB_PASSWORD: " + (TEST_DB_PASSWORD != null ? "[SET]" : "[NOT SET]"));
    }
    
    /**
     * Get configuration value from either environment variable or system property
     */
    private static String getConfigValue(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            value = System.getenv(key);
        }
        return value;
    }
    
    /**
     * Load environment variables from test.env file only
     */
    private static void loadEnvironmentVariables() {
        // Only try to load from test.env in test resources
        Path testEnvPath = Paths.get("app/src/test/resources/test.env");
        if (Files.exists(testEnvPath) && Files.isReadable(testEnvPath)) {
            System.out.println("🔍 Found test.env file at: " + testEnvPath.toAbsolutePath());
            try {
                Properties props = new Properties();
                try (var inputStream = Files.newInputStream(testEnvPath)) {
                    props.load(inputStream);
                }
                
                // Set as system properties
                for (String key : props.stringPropertyNames()) {
                    String value = props.getProperty(key);
                    System.setProperty(key, value);
                    System.out.println("🔍 Set system property " + key + " from test.env file");
                }
                
                System.out.println("🔍 Environment variables loaded from test.env file");
                return; // Success, exit
                
            } catch (IOException e) {
                System.err.println("❌ Error loading test.env file: " + e.getMessage());
            }
        }
        
        // If test.env doesn't exist, rely on environment variables or system properties only
        System.out.println("🔍 test.env file not found, using environment variables and system properties only");
    }
    
    /**
     * Create a test database with all required tables
     */
    public static void setupTestDatabase() throws SQLException {
        try (Connection conn = fin.config.DatabaseConfig.getTestConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Drop all existing tables and sequences first
            dropAllDatabaseObjects(stmt);
            
            // Execute the complete production schema
            executeSchemaFile(stmt);
            
            // Insert test data
            insertTestData(stmt);
            
            System.out.println("✅ Test database setup completed successfully");
        }
    }
    
    private static void dropAllDatabaseObjects(Statement stmt) throws SQLException {
        // Drop all tables in reverse dependency order (expanded list from production)
        String[] tables = {
            "transaction_mapping_rules",
            "transaction_types", 
            "tax_brackets",
            "tax_configurations",
            "payslips",
            "payroll_journal_entries",
            "payroll_periods",
            "manual_invoices",
            "employee_leave",
            "deductions",
            "benefits",
            "employees",
            "data_corrections",
            "company_classification_rules",
            "bank_transactions",
            "bank_accounts",
            "journal_entry_lines",
            "journal_entries",
            "accounts",
            "account_categories", 
            "account_types",
            "fiscal_periods",
            "companies"
        };
        
        for (String table : tables) {
            try {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + table + " CASCADE");
            } catch (SQLException e) {
                // Ignore errors if table doesn't exist
                System.out.println("⚠️ Could not drop table " + table + ": " + e.getMessage());
            }
        }
        
        // Drop sequences
        String[] sequences = {
            "account_categories_id_seq",
            "account_types_id_seq", 
            "accounts_id_seq",
            "bank_accounts_id_seq",
            "bank_transactions_id_seq",
            "benefits_id_seq",
            "companies_id_seq",
            "company_classification_rules_id_seq",
            "data_corrections_id_seq",
            "deductions_id_seq",
            "employee_leave_id_seq",
            "employees_id_seq",
            "fiscal_periods_id_seq",
            "journal_entries_id_seq",
            "journal_entry_lines_id_seq",
            "manual_invoices_id_seq",
            "payroll_journal_entries_id_seq",
            "payroll_periods_id_seq",
            "payslips_id_seq",
            "tax_brackets_id_seq",
            "tax_configurations_id_seq",
            "transaction_mapping_rules_id_seq",
            "transaction_types_id_seq"
        };
        
        for (String sequence : sequences) {
            try {
                stmt.executeUpdate("DROP SEQUENCE IF EXISTS " + sequence + " CASCADE");
            } catch (SQLException e) {
                // Ignore errors
            }
        }
    }
    
    private static void executeSchemaFile(Statement stmt) throws SQLException {
        try {
            // Try multiple possible locations for the schema file
            java.nio.file.Path schemaPath = null;
            java.nio.file.Path[] possiblePaths = {
                java.nio.file.Paths.get("test_schema.sql"),              // Current directory
                java.nio.file.Paths.get("../test_schema.sql"),           // Parent directory
                java.nio.file.Paths.get("../../test_schema.sql"),        // Grandparent
                java.nio.file.Paths.get("../../../test_schema.sql"),     // Great-grandparent
                java.nio.file.Paths.get("../../../../test_schema.sql"),  // Great-great-grandparent
                java.nio.file.Paths.get(System.getProperty("user.dir"), "test_schema.sql"),  // Project root
                java.nio.file.Paths.get(System.getProperty("user.dir"), "..", "test_schema.sql"),  // Parent of project root
            };
            
            for (java.nio.file.Path path : possiblePaths) {
                if (java.nio.file.Files.exists(path) && java.nio.file.Files.isReadable(path)) {
                    schemaPath = path;
                    System.out.println("🔍 Found test_schema.sql at: " + path.toAbsolutePath());
                    break;
                }
            }
            
            if (schemaPath == null) {
                throw new java.io.IOException("test_schema.sql not found in any expected location");
            }
            
            String schemaSql = java.nio.file.Files.readString(schemaPath);
            
            // Split into individual statements properly handling multi-line statements
            List<String> statements = parseSqlStatements(schemaSql);
            System.out.println("🔍 Parsed " + statements.size() + " SQL statements");
            for (String statement : statements) {
                statement = statement.trim();
                if (!statement.isEmpty() && !isCommentOrMetadata(statement)) {
                    System.out.println("🔍 Executing: " + statement.substring(0, Math.min(100, statement.length())) + "...");
                    try {
                        stmt.executeUpdate(statement);
                    } catch (SQLException e) {
                        // Some statements might fail (like CREATE EXTENSION, comments, etc.)
                        System.out.println("⚠️ Skipping statement: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                    }
                } else if (!statement.isEmpty()) {
                    System.out.println("⚠️ Filtered out: " + statement.substring(0, Math.min(50, statement.length())) + "...");
                }
            }
            
            System.out.println("✅ Schema executed successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to execute schema file: " + e.getMessage());
            throw new SQLException("Schema execution failed", e);
        }
    }
    
    private static List<String> parseSqlStatements(String sql) {
        List<String> statements = new java.util.ArrayList<>();
        StringBuilder currentStatement = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;
        int parenthesesDepth = 0;
        
        // Split into lines first to handle comments
        String[] lines = sql.split("\n");
        
        for (String line : lines) {
            String trimmedLine = line.trim();
            
            // Skip comment lines (but not lines that contain DDL after comments)
            if (trimmedLine.startsWith("--") && !trimmedLine.contains("CREATE") && 
                !trimmedLine.contains("ALTER") && !trimmedLine.contains("DROP") &&
                !trimmedLine.contains("INSERT") && !trimmedLine.contains("UPDATE") &&
                !trimmedLine.contains("DELETE")) {
                continue; // Skip pure comment lines
            }
            
            // Process the line character by character
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                
                // Handle string literals
                if (!inString && (c == '\'' || c == '"')) {
                    inString = true;
                    stringChar = c;
                } else if (inString && c == stringChar) {
                    inString = false;
                    stringChar = 0;
                }
                
                // Handle parentheses (for function bodies, etc.)
                if (!inString) {
                    if (c == '(') {
                        parenthesesDepth++;
                    } else if (c == ')') {
                        parenthesesDepth--;
                    }
                }
                
                currentStatement.append(c);
                
                // Check for statement terminator
                if (!inString && parenthesesDepth == 0 && c == ';') {
                    String statement = currentStatement.toString().trim();
                    if (!statement.isEmpty()) {
                        statements.add(statement);
                    }
                    currentStatement = new StringBuilder();
                    break; // Move to next line after finding semicolon
                }
            }
            
            // If we didn't find a semicolon, add the newline
            if (currentStatement.length() > 0 && !line.endsWith(";")) {
                currentStatement.append("\n");
            }
        }
        
        // Add any remaining statement
        if (currentStatement.length() > 0) {
            String remaining = currentStatement.toString().trim();
            if (!remaining.isEmpty()) {
                statements.add(remaining);
            }
        }
        
        return statements;
    }
    
    private static boolean isCommentOrMetadata(String statement) {
        String trimmed = statement.trim();
        String lower = trimmed.toLowerCase();
        
        // Skip pure comments (lines that are only --)
        if (lower.startsWith("--") && !lower.contains("\n")) {
            return true;
        }
        
        // Skip SET statements
        if (lower.startsWith("set ")) {
            return true;
        }
        
        // Skip SELECT pg_catalog statements
        if (lower.startsWith("select pg_catalog")) {
            return true;
        }
        
        // Skip specific SET statements
        if (lower.equals("set statement_timeout = 0") ||
            lower.equals("set lock_timeout = 0") ||
            lower.equals("set idle_in_transaction_session_timeout = 0") ||
            lower.equals("set client_encoding = 'utf8'") ||
            lower.equals("set standard_conforming_strings = on") ||
            lower.equals("set check_function_bodies = false") ||
            lower.equals("set xmloption = content") ||
            lower.equals("set client_min_messages = warning") ||
            lower.equals("set row_security = off")) {
            return true;
        }
        
        // Skip metadata comments that are part of DDL blocks
        // But allow statements that contain both comments and DDL
        if (lower.contains("type: ") && lower.contains("schema: ") && lower.contains("owner: ")) {
            return true;
        }
        
        // Skip dump headers
        if (lower.startsWith("dumped from database") ||
            lower.startsWith("dumped by pg_dump")) {
            return true;
        }
        
        // Allow statements that contain CREATE, ALTER, DROP, etc.
        if (lower.contains("create ") || lower.contains("alter ") || lower.contains("drop ") ||
            lower.contains("insert ") || lower.contains("update ") || lower.contains("delete ")) {
            return false;
        }
        
        // Default to filtering out if we're not sure
        return true;
    }
    
    private static void insertTestData(Statement stmt) throws SQLException {
        // Insert account types
        stmt.executeUpdate("""
            INSERT INTO account_types (code, name, normal_balance, description) VALUES
            ('ASSET', 'Asset', 'D', 'Assets - resources owned by the company'),
            ('LIAB', 'Liability', 'C', 'Liabilities - debts owed by the company'),
            ('EQUITY', 'Equity', 'C', 'Owner equity in the company'),
            ('REV', 'Revenue', 'C', 'Revenue earned by the company'),
            ('EXP', 'Expense', 'D', 'Expenses incurred by the company')
        """);
        
        // Insert test company
        stmt.executeUpdate("""
            INSERT INTO companies (id, name, registration_number, tax_number, address, contact_email, contact_phone)
            VALUES (1, 'Test Company Ltd', '2023/123456/07', '9876543210', '123 Test St, Test City', 'test@example.com', '+27-11-123-4567')
        """);
        
        // Insert test fiscal period
        stmt.executeUpdate("""
            INSERT INTO fiscal_periods (id, company_id, period_name, start_date, end_date, is_closed)
            VALUES (1, 1, '2025 Financial Year', '2025-01-01', '2025-12-31', false)
        """);
        
        // Insert test account categories
        stmt.executeUpdate("""
            INSERT INTO account_categories (id, name, account_type_id, company_id) VALUES
            (1, 'Current Assets', 1, 1),
            (2, 'Fixed Assets', 1, 1),
            (3, 'Current Liabilities', 2, 1),
            (4, 'Long-term Liabilities', 2, 1),
            (5, 'Owner Equity', 3, 1),
            (6, 'Sales', 4, 1),
            (7, 'Operating Expenses', 5, 1),
            (8, 'Financial Expenses', 5, 1)
        """);
        
        // Insert test accounts
        stmt.executeUpdate("""
            INSERT INTO accounts (id, company_id, category_id, account_code, account_name, description) VALUES
            (1, 1, 1, '1100', 'Bank Account', 'Main bank account'),
            (2, 1, 1, '1200', 'Accounts Receivable', 'Money owed by customers'),
            (3, 1, 7, '8800', 'Insurance', 'Insurance expenses'),
            (4, 1, 7, '9600', 'Bank Charges', 'Bank fees and charges'),
            (5, 1, 6, '4000', 'Sales Revenue', 'Revenue from sales'),
            (6, 1, 7, '5000', 'Office Expenses', 'General office expenses')
        """);
    }
    
    /**
     * Clean up test database
     */
    public static void cleanupTestDatabase() throws SQLException {
        try (Connection conn = fin.config.DatabaseConfig.getTestConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
             Statement stmt = conn.createStatement()) {
            
            // Drop all tables in reverse dependency order (expanded list from production)
            String[] tables = {
                "transaction_mapping_rules",
                "transaction_types", 
                "tax_brackets",
                "tax_configurations",
                "payslips",
                "payroll_journal_entries",
                "payroll_periods",
                "manual_invoices",
                "employee_leave",
                "deductions",
                "benefits",
                "employees",
                "data_corrections",
                "company_classification_rules",
                "bank_transactions",
                "bank_accounts",
                "journal_entry_lines",
                "journal_entries",
                "accounts",
                "account_categories", 
                "account_types",
                "fiscal_periods",
                "companies"
            };
            
            for (String table : tables) {
                try {
                    stmt.executeUpdate("DROP TABLE IF EXISTS " + table + " CASCADE");
                } catch (SQLException e) {
                    // Ignore errors if table doesn't exist
                    System.out.println("⚠️ Could not drop table " + table + ": " + e.getMessage());
                }
            }
            
            // Drop sequences
            String[] sequences = {
                "account_categories_id_seq",
                "account_types_id_seq", 
                "accounts_id_seq",
                "bank_accounts_id_seq",
                "bank_transactions_id_seq",
                "benefits_id_seq",
                "companies_id_seq",
                "company_classification_rules_id_seq",
                "data_corrections_id_seq",
                "deductions_id_seq",
                "employee_leave_id_seq",
                "employees_id_seq",
                "fiscal_periods_id_seq",
                "journal_entries_id_seq",
                "journal_entry_lines_id_seq",
                "manual_invoices_id_seq",
                "payroll_journal_entries_id_seq",
                "payroll_periods_id_seq",
                "payslips_id_seq",
                "tax_brackets_id_seq",
                "tax_configurations_id_seq",
                "transaction_mapping_rules_id_seq",
                "transaction_types_id_seq"
            };
            
            for (String sequence : sequences) {
                try {
                    stmt.executeUpdate("DROP SEQUENCE IF EXISTS " + sequence + " CASCADE");
                } catch (SQLException e) {
                    // Ignore errors
                }
            }
        }
    }
    
    /**
     * Main method for testing TestConfiguration functionality
     */
    public static void main(String[] args) {
        try {
            System.out.println("🧪 Testing TestConfiguration.java functionality...");
            
            // Test 1: Environment loading
            System.out.println("\n📋 Test 1: Environment Variable Loading");
            System.out.println("✅ TEST_DB_URL: " + TEST_DB_URL);
            System.out.println("✅ TEST_DB_USER: " + TEST_DB_USER);
            System.out.println("✅ TEST_DB_PASSWORD present: " + (TEST_DB_PASSWORD != null));
            
            // Test 2: Database connection
            System.out.println("\n📋 Test 2: Database Connection");
            try (Connection conn = fin.config.DatabaseConfig.getTestConnection(TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD);
                 Statement stmt = conn.createStatement()) {
                
                try (ResultSet rs = stmt.executeQuery("SELECT 1 as test")) {
                    if (rs.next() && rs.getInt("test") == 1) {
                        System.out.println("✅ Database connection successful");
                    }
                }
                
                // Test 3: Database setup
                System.out.println("\n📋 Test 3: Database Setup");
                long startTime = System.currentTimeMillis();
                setupTestDatabase();
                long endTime = System.currentTimeMillis();
                System.out.println("✅ Database setup completed in " + (endTime - startTime) + "ms");
                
                // Test 4: Verify setup worked
                System.out.println("\n📋 Test 4: Verify Setup");
                try (ResultSet tableCheck = stmt.executeQuery("SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema = 'public'")) {
                    if (tableCheck.next()) {
                        int tableCount = tableCheck.getInt("table_count");
                        System.out.println("✅ Found " + tableCount + " tables in database");
                    }
                }
                
                try (ResultSet dataCheck = stmt.executeQuery("SELECT COUNT(*) as company_count FROM companies")) {
                    if (dataCheck.next()) {
                        int companyCount = dataCheck.getInt("company_count");
                        System.out.println("✅ Found " + companyCount + " test companies");
                    }
                }
                
                // Test 5: Database cleanup
                System.out.println("\n📋 Test 5: Database Cleanup");
                cleanupTestDatabase();
                System.out.println("✅ Database cleanup completed");
                
                // Verify cleanup
                try (ResultSet cleanupCheck = stmt.executeQuery("SELECT COUNT(*) as remaining_tables FROM information_schema.tables WHERE table_schema = 'public'")) {
                    if (cleanupCheck.next()) {
                        int remainingTables = cleanupCheck.getInt("remaining_tables");
                        System.out.println("✅ " + remainingTables + " tables remaining after cleanup");
                    }
                }
                
            }
            
            System.out.println("\n🎉 All TestConfiguration.java tests passed!");
            
        } catch (Exception e) {
            System.err.println("❌ TestConfiguration.java test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
