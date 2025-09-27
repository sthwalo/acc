package fin;

import fin.config.TestDatabaseConfig;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Test configuration utilities for setting up test database
 */
public class TestConfiguration {
    
        // Test database configuration - loaded from environment variables only
    public static final String TEST_DB_URL = System.getenv("TEST_DATABASE_URL");
    public static final String TEST_DB_USER = System.getenv("TEST_DATABASE_USER");
    public static final String TEST_DB_PASSWORD = System.getenv("TEST_DATABASE_PASSWORD");
    
    static {
        // Validate that test database configuration is available
        if (TEST_DB_URL == null || TEST_DB_USER == null || TEST_DB_PASSWORD == null) {
            throw new RuntimeException("Test database configuration missing. Please set TEST_DATABASE_URL, TEST_DATABASE_USER, and TEST_DATABASE_PASSWORD in .env file.");
        }
        System.out.println("🔍 TestConfiguration - TEST_DB_URL: " + TEST_DB_URL);
        System.out.println("🔍 TestConfiguration - TEST_DB_USER: " + TEST_DB_USER);
        System.out.println("🔍 TestConfiguration - TEST_DB_PASSWORD: " + (TEST_DB_PASSWORD != null ? "[SET]" : "[NOT SET]"));
    }
    
    /**
     * Create a test database with all required tables
     */
    public static void setupTestDatabase() throws SQLException {
        try (Connection conn = TestDatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Drop existing tables in reverse dependency order
            dropExistingTables(stmt);
            
            // Create companies table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS companies (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    registration_number VARCHAR(100),
                    tax_number VARCHAR(100),
                    address TEXT,
                    email VARCHAR(255),
                    phone VARCHAR(50),
                    logo_path VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Create fiscal_periods table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS fiscal_periods (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    period_name VARCHAR(100) NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    is_closed BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id)
                )
            """);
            
            // Create account_types table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS account_types (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(50) NOT NULL UNIQUE,
                    description TEXT
                )
            """);
            
            // Create account_categories table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS account_categories (
                    id BIGSERIAL PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    account_type_id BIGINT NOT NULL,
                    company_id BIGINT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (account_type_id) REFERENCES account_types(id),
                    FOREIGN KEY (company_id) REFERENCES companies(id)
                )
            """);
            
            // Create accounts table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    category_id BIGINT NOT NULL,
                    account_code VARCHAR(20) NOT NULL,
                    account_name VARCHAR(255) NOT NULL,
                    description TEXT,
                    is_active BOOLEAN DEFAULT TRUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id),
                    FOREIGN KEY (category_id) REFERENCES account_categories(id),
                    UNIQUE(company_id, account_code)
                )
            """);
            
            // Create journal_entries table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS journal_entries (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    fiscal_period_id BIGINT NOT NULL,
                    entry_date DATE NOT NULL,
                    description TEXT,
                    reference VARCHAR(100),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id),
                    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
                )
            """);
            
            // Create journal_entry_lines table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS journal_entry_lines (
                    id BIGSERIAL PRIMARY KEY,
                    journal_entry_id BIGINT NOT NULL,
                    account_id BIGINT NOT NULL,
                    debit_amount DECIMAL(15,2),
                    credit_amount DECIMAL(15,2),
                    description TEXT,
                    FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id),
                    FOREIGN KEY (account_id) REFERENCES accounts(id)
                )
            """);
            
            // Create bank_transactions table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS bank_transactions (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    fiscal_period_id BIGINT,
                    transaction_date DATE NOT NULL,
                    details TEXT NOT NULL,
                    debit_amount DECIMAL(15,2),
                    credit_amount DECIMAL(15,2),
                    balance DECIMAL(15,2),
                    account_code VARCHAR(20),
                    account_name VARCHAR(255),
                    is_classified BOOLEAN DEFAULT FALSE,
                    source_file VARCHAR(500),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id),
                    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id)
                )
            """);
            
            // Create company_classification_rules table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS company_classification_rules (
                    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    pattern VARCHAR(1000) NOT NULL,
                    keywords VARCHAR(2000) NOT NULL,
                    account_code VARCHAR(20) NOT NULL,
                    account_name VARCHAR(255) NOT NULL,
                    usage_count INTEGER DEFAULT 1,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id)
                )
            """);
            
            // Insert test data
            insertTestData(stmt);
            
            System.out.println("✅ Test database setup completed successfully");
        }
    }
    
    private static void dropExistingTables(Statement stmt) throws SQLException {
        // Drop tables in reverse dependency order
        String[] tables = {
            "company_classification_rules",
            "bank_transactions", 
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
    }
    
    private static void insertTestData(Statement stmt) throws SQLException {
        // Insert account types
        stmt.executeUpdate("""
            INSERT INTO account_types (name, description) VALUES
            ('Asset', 'Assets - resources owned by the company'),
            ('Liability', 'Liabilities - debts owed by the company'),
            ('Equity', 'Owner equity in the company'),
            ('Revenue', 'Revenue earned by the company'),
            ('Expense', 'Expenses incurred by the company')
        """);
        
        // Insert test company
        stmt.executeUpdate("""
            INSERT INTO companies (id, name, registration_number, tax_number, address, email, phone)
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
        try (Connection conn = TestDatabaseConfig.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Drop all tables in reverse dependency order
            String[] tables = {
                "company_classification_rules",
                "bank_transactions", 
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
        }
    }
}
