# 🚀 PostgreSQL Migration Guide

## Overview

Your FIN application is excellently positioned for PostgreSQL migration! The system uses standard JDBC connections with minimal SQLite-specific features, making the transition straightforward.

## 🎯 Migration Benefits

### Why PostgreSQL is Perfect for Your System:
- ✅ **Production-ready** - Better performance, concurrency, and reliability
- ✅ **ACID compliance** - Superior transaction integrity for financial data
- ✅ **Scalability** - Handles larger datasets and multiple concurrent users
- ✅ **Advanced features** - Better JSON support, window functions, and analytics
- ✅ **Security** - Row-level security, encrypted connections, and audit logging
- ✅ **Backup & Recovery** - Enterprise-grade backup and point-in-time recovery

## 📋 Current Architecture Assessment

### ✅ Migration-Friendly Features:
1. **JDBC-based architecture** - Easy database driver swap
2. **Standard SQL** - Most queries will work without modification
3. **Repository pattern** - Database logic isolated in services
4. **Connection string configuration** - Single point of change
5. **Clean entity models** - No database-specific dependencies

### ⚠️ Areas Requiring Attention:
1. **Auto-increment syntax** - SQLite `AUTOINCREMENT` → PostgreSQL `SERIAL`/`IDENTITY`
2. **Data types** - `TEXT` → `VARCHAR`/`TEXT`, `REAL` → `DECIMAL`
3. **Boolean handling** - SQLite integers → PostgreSQL boolean
4. **Date/time functions** - SQLite functions → PostgreSQL equivalents
5. **Upsert syntax** - `INSERT ... ON CONFLICT` differences

## 🔧 Step-by-Step Migration Plan

### Phase 1: Environment Setup (30 minutes)

#### 1.1 Update Dependencies
```gradle
// Update app/build.gradle.kts
dependencies {
    // Remove SQLite driver
    // implementation("org.xerial:sqlite-jdbc:3.36.0")
    
    // Add PostgreSQL driver
    implementation("org.postgresql:postgresql:42.7.3")
    
    // Add connection pooling (recommended)
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Existing dependencies remain the same
    implementation(libs.guava)
    implementation("org.apache.pdfbox:pdfbox:3.0.0")
    // ... rest of your dependencies
}
```

#### 1.2 Database Configuration
```java
// Update database URL in App.java
private static final String DB_URL = "jdbc:postgresql://localhost:5432/fin_database";
private static final String DB_USER = "fin_user";
private static final String DB_PASSWORD = "secure_password";

// Update connection creation throughout services
Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
```

#### 1.3 PostgreSQL Installation
```bash
# Install PostgreSQL (macOS)
brew install postgresql@15
brew services start postgresql@15

# Create database and user
createdb fin_database
psql fin_database -c "CREATE USER fin_user WITH PASSWORD 'secure_password';"
psql fin_database -c "GRANT ALL PRIVILEGES ON DATABASE fin_database TO fin_user;"
```

### Phase 2: Schema Migration (45 minutes)

#### 2.1 Create PostgreSQL Schema
```sql
-- Save as: app/src/main/resources/db/migration/V2__PostgreSQL_Schema.sql
-- Companies table
CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50),
    tax_number VARCHAR(50),
    address TEXT,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Fiscal periods table
CREATE TABLE fiscal_periods (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    period_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_closed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Account types table
CREATE TABLE account_types (
    id SERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    normal_balance CHAR(1) NOT NULL CHECK (normal_balance IN ('D', 'C')),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default account types
INSERT INTO account_types (code, name, normal_balance, description) VALUES
    ('A', 'Asset', 'D', 'Resources owned by the company'),
    ('L', 'Liability', 'C', 'Obligations of the company'),
    ('E', 'Equity', 'C', 'Owner''s claim on assets'),
    ('R', 'Revenue', 'C', 'Income from operations'),
    ('X', 'Expense', 'D', 'Costs incurred to generate revenue');

-- Account categories table
CREATE TABLE account_categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    account_type_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_type_id) REFERENCES account_types(id) ON DELETE RESTRICT,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Accounts table
CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    account_code VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    description TEXT,
    category_id INTEGER NOT NULL,
    parent_account_id INTEGER,
    company_id INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES account_categories(id) ON DELETE RESTRICT,
    FOREIGN KEY (parent_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    UNIQUE(company_id, account_code)
);

-- Transaction types table
CREATE TABLE transaction_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    company_id INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Journal entries table
CREATE TABLE journal_entries (
    id SERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL,
    entry_date DATE NOT NULL,
    description TEXT,
    transaction_type_id INTEGER,
    fiscal_period_id INTEGER NOT NULL,
    company_id INTEGER NOT NULL,
    created_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (transaction_type_id) REFERENCES transaction_types(id) ON DELETE SET NULL,
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Journal entry lines table
CREATE TABLE journal_entry_lines (
    id SERIAL PRIMARY KEY,
    journal_entry_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    debit_amount DECIMAL(15,2) DEFAULT 0.00,
    credit_amount DECIMAL(15,2) DEFAULT 0.00,
    description TEXT,
    reference VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (journal_entry_id) REFERENCES journal_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    CHECK ((debit_amount > 0 AND credit_amount = 0) OR (credit_amount > 0 AND debit_amount = 0))
);

-- Transaction mapping rules table
CREATE TABLE transaction_mapping_rules (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    description TEXT,
    match_type VARCHAR(20) NOT NULL CHECK (match_type IN ('CONTAINS', 'STARTS_WITH', 'ENDS_WITH', 'EQUALS', 'REGEX')),
    match_value TEXT NOT NULL,
    account_id INTEGER NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Bank transactions table
CREATE TABLE bank_transactions (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    bank_account_id INTEGER,
    fiscal_period_id INTEGER NOT NULL,
    transaction_date DATE NOT NULL,
    details TEXT,
    debit_amount DECIMAL(15,2),
    credit_amount DECIMAL(15,2),
    balance DECIMAL(15,2),
    service_fee BOOLEAN DEFAULT FALSE,
    account_number VARCHAR(50),
    statement_period VARCHAR(50),
    source_file VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT
);

-- Bank accounts table
CREATE TABLE bank_accounts (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    account_type VARCHAR(50),
    bank_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

-- Manual invoices table (from DataManagementService)
CREATE TABLE manual_invoices (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    invoice_number VARCHAR(100) NOT NULL,
    invoice_date DATE NOT NULL,
    description TEXT,
    amount DECIMAL(15,2) NOT NULL,
    debit_account_id INTEGER NOT NULL,
    credit_account_id INTEGER NOT NULL,
    fiscal_period_id INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(company_id, invoice_number),
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (debit_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    FOREIGN KEY (credit_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    FOREIGN KEY (fiscal_period_id) REFERENCES fiscal_periods(id) ON DELETE RESTRICT
);

-- Data corrections table
CREATE TABLE data_corrections (
    id SERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    transaction_id INTEGER NOT NULL,
    original_account_id INTEGER NOT NULL,
    new_account_id INTEGER NOT NULL,
    correction_reason TEXT NOT NULL,
    corrected_by VARCHAR(100) NOT NULL,
    corrected_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    FOREIGN KEY (transaction_id) REFERENCES bank_transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (original_account_id) REFERENCES accounts(id) ON DELETE RESTRICT,
    FOREIGN KEY (new_account_id) REFERENCES accounts(id) ON DELETE RESTRICT
);

-- Create indexes for performance
CREATE INDEX idx_accounts_company ON accounts(company_id, is_active);
CREATE INDEX idx_accounts_category ON accounts(category_id);
CREATE INDEX idx_accounts_parent ON accounts(parent_account_id) WHERE parent_account_id IS NOT NULL;
CREATE INDEX idx_transaction_types_company ON transaction_types(company_id, is_active);
CREATE INDEX idx_journal_entries_company_date ON journal_entries(company_id, entry_date);
CREATE INDEX idx_journal_entry_lines_account ON journal_entry_lines(account_id);
CREATE INDEX idx_transaction_mapping_rules_company ON transaction_mapping_rules(company_id, is_active, priority);
CREATE INDEX idx_bank_transactions_company_date ON bank_transactions(company_id, transaction_date);

-- Create triggers for updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_account_categories_updated_at 
    BEFORE UPDATE ON account_categories 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_accounts_updated_at 
    BEFORE UPDATE ON accounts 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_journal_entries_updated_at 
    BEFORE UPDATE ON journal_entries 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_transaction_mapping_rules_updated_at 
    BEFORE UPDATE ON transaction_mapping_rules 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_manual_invoices_updated_at 
    BEFORE UPDATE ON manual_invoices 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
```

### Phase 3: Code Updates (60 minutes)

#### 3.1 Database Connection Updates
```java
// Create new DatabaseConfig.java
package fin.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static HikariDataSource dataSource;
    
    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(System.getenv().getOrDefault("DATABASE_URL", 
            "jdbc:postgresql://localhost:5432/fin_database"));
        config.setUsername(System.getenv().getOrDefault("DATABASE_USER", "fin_user"));
        config.setPassword(System.getenv().getOrDefault("DATABASE_PASSWORD", "secure_password"));
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        dataSource = new HikariDataSource(config);
    }
    
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public static void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
```

#### 3.2 Service Updates Required

**Key Changes Needed:**

1. **Remove SQLite-specific syntax:**
   - `INTEGER PRIMARY KEY AUTOINCREMENT` → `SERIAL PRIMARY KEY`
   - `ON CONFLICT` → `ON CONFLICT` (PostgreSQL syntax slightly different)
   - `CURRENT_TIMESTAMP` → `CURRENT_TIMESTAMP` (same)

2. **Update data types:**
   - `REAL` → `DECIMAL(15,2)` for monetary values
   - `INTEGER` boolean → `BOOLEAN`
   - `TEXT` can remain `TEXT` or use `VARCHAR(n)`

3. **Connection handling:**
   ```java
   // Replace all instances of:
   Connection conn = DriverManager.getConnection(dbUrl);
   
   // With:
   Connection conn = DatabaseConfig.getConnection();
   ```

### Phase 4: Data Migration (30 minutes)

#### 4.1 Export Existing Data
```bash
# Export from SQLite
sqlite3 fin_database.db ".dump" > sqlite_export.sql

# Clean up SQLite-specific syntax for PostgreSQL
sed -i 's/AUTOINCREMENT/SERIAL/g' sqlite_export.sql
sed -i 's/datetime(\x27now\x27)/CURRENT_TIMESTAMP/g' sqlite_export.sql
```

#### 4.2 Import into PostgreSQL
```bash
# Create clean import script
psql -U fin_user -d fin_database -f sqlite_export_cleaned.sql
```

### Phase 5: Testing & Validation (45 minutes)

#### 5.1 Update Test Configuration
```java
// Update test database connections
public class TestDatabaseConfig {
    private static final String TEST_DB_URL = "jdbc:postgresql://localhost:5432/fin_test_database";
    // ... similar to DatabaseConfig but for testing
}
```

#### 5.2 Run Test Suite
```bash
./gradlew test
```

#### 5.3 Manual Testing Checklist
- [ ] Company creation and retrieval
- [ ] Fiscal period management
- [ ] Account and category operations
- [ ] Bank transaction import
- [ ] PDF processing
- [ ] Report generation
- [ ] Data corrections and manual invoices

## 🔒 Production Deployment Considerations

### Environment Variables
```bash
export DATABASE_URL="jdbc:postgresql://prod-server:5432/fin_production"
export DATABASE_USER="fin_prod_user"
export DATABASE_PASSWORD="very_secure_production_password"
```

### Security Enhancements
1. **SSL Connections:**
   ```java
   config.setJdbcUrl(DATABASE_URL + "?sslmode=require");
   ```

2. **Connection Encryption:**
   ```sql
   ALTER SYSTEM SET ssl = on;
   ```

3. **Role-based Access:**
   ```sql
   CREATE ROLE fin_readonly;
   GRANT SELECT ON ALL TABLES IN SCHEMA public TO fin_readonly;
   ```

## 📊 Performance Optimizations

### Database Tuning
```sql
-- Analyze tables after migration
ANALYZE;

-- Update PostgreSQL configuration
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1GB';
ALTER SYSTEM SET work_mem = '4MB';
SELECT pg_reload_conf();
```

### Connection Pooling Benefits
- Reduced connection overhead
- Better resource utilization
- Improved concurrent user handling
- Automatic connection recovery

## 🎯 Migration Timeline

| Phase | Duration | Key Activities |
|-------|----------|----------------|
| **Preparation** | 30 min | Dependencies, PostgreSQL setup |
| **Schema** | 45 min | Create tables, constraints, indexes |
| **Code Updates** | 60 min | Connection handling, data types |
| **Data Migration** | 30 min | Export/import existing data |
| **Testing** | 45 min | Comprehensive testing |
| **Total** | **3.5 hours** | Complete migration |

## ✅ Post-Migration Benefits

### Immediate Gains:
- ✅ **Better Performance** - Optimized query execution
- ✅ **Data Integrity** - ACID compliance for financial transactions
- ✅ **Concurrent Users** - Multiple users without blocking
- ✅ **Production Ready** - Enterprise-grade database

### Long-term Advantages:
- ✅ **Scalability** - Handle growing transaction volumes
- ✅ **Analytics** - Advanced reporting capabilities
- ✅ **Backup & Recovery** - Point-in-time recovery
- ✅ **Security** - Row-level security, audit logging

## 🚀 Ready to Migrate?

Your system is **perfectly positioned** for PostgreSQL migration. The clean architecture and standard JDBC usage make this a straightforward process with significant benefits.

**Recommendation:** Proceed with migration - the time investment (3.5 hours) will pay dividends in production reliability and performance.

---

*Next Steps: Would you like me to help implement any specific phase of this migration?*
