package fin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Test configuration utilities for setting up test database
 */
public class TestConfiguration {
    
    public static final String TEST_DB_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL";
    
    /**
     * Create a test database with all required tables
     */
    public static void setupTestDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            
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
            
            // Create transaction_mapping_rules table
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS transaction_mapping_rules (
                    id BIGSERIAL PRIMARY KEY,
                    company_id BIGINT NOT NULL,
                    pattern VARCHAR(500) NOT NULL,
                    account_code VARCHAR(20) NOT NULL,
                    account_name VARCHAR(255) NOT NULL,
                    confidence_score DECIMAL(3,2) DEFAULT 1.0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (company_id) REFERENCES companies(id)
                )
            """);
            
            // Insert test data
            insertTestData(stmt);
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
        try (Connection conn = DriverManager.getConnection(TEST_DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // Drop all tables in reverse dependency order
            stmt.executeUpdate("DROP TABLE IF EXISTS transaction_mapping_rules");
            stmt.executeUpdate("DROP TABLE IF EXISTS bank_transactions");
            stmt.executeUpdate("DROP TABLE IF EXISTS journal_entry_lines");
            stmt.executeUpdate("DROP TABLE IF EXISTS journal_entries");
            stmt.executeUpdate("DROP TABLE IF EXISTS accounts");
            stmt.executeUpdate("DROP TABLE IF EXISTS account_categories");
            stmt.executeUpdate("DROP TABLE IF EXISTS account_types");
            stmt.executeUpdate("DROP TABLE IF EXISTS fiscal_periods");
            stmt.executeUpdate("DROP TABLE IF EXISTS companies");
        }
    }
}
