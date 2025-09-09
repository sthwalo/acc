package fin.app;

import fin.config.DatabaseConfig;

import java.sql.*;

/**
 * Database Schema Updater for Account Classification Enhancement
 * Adds necessary fields and tables for company-specific classification
 */
public class DatabaseSchemaUpdater {
    
    private final String dbUrl;
    
    public DatabaseSchemaUpdater() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
    }
    
    public void updateSchema() {
        System.out.println("🔧 UPDATING DATABASE SCHEMA FOR ACCOUNT CLASSIFICATION");
        System.out.println("=".repeat(60));
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            
            // Update bank_transactions table
            updateBankTransactionsTable(conn);
            
            // Create company_classification_rules table
            createClassificationRulesTable(conn);
            
            // Create indexes for performance
            createIndexes(conn);
            
            System.out.println("✅ Database schema update completed successfully!");
            
        } catch (SQLException e) {
            System.err.println("❌ Error updating database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Add account classification fields to bank_transactions table
     */
    private void updateBankTransactionsTable(Connection conn) throws SQLException {
        System.out.println("📊 Updating bank_transactions table...");
        
        // Add account_code column
        try {
            String sql1 = "ALTER TABLE bank_transactions ADD COLUMN account_code VARCHAR(10)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql1);
                System.out.println("   ✅ Added account_code column");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate column")) {
                System.out.println("   ℹ️ account_code column already exists");
            } else {
                throw e;
            }
        }
        
        // Add account_name column
        try {
            String sql2 = "ALTER TABLE bank_transactions ADD COLUMN account_name VARCHAR(255)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql2);
                System.out.println("   ✅ Added account_name column");
            }
        } catch (SQLException e) {
            if (e.getMessage().contains("already exists") || e.getMessage().contains("duplicate column")) {
                System.out.println("   ℹ️ account_name column already exists");
            } else {
                throw e;
            }
        }
    }
    
    /**
     * Create company_classification_rules table
     */
    private void createClassificationRulesTable(Connection conn) throws SQLException {
        System.out.println("📋 Creating company_classification_rules table...");
        
        String sql = """
            CREATE TABLE IF NOT EXISTS company_classification_rules (
                id BIGSERIAL PRIMARY KEY,
                company_id BIGINT NOT NULL,
                pattern TEXT NOT NULL,
                keywords VARCHAR(255)[] NOT NULL,
                account_code VARCHAR(10) NOT NULL,
                account_name VARCHAR(255) NOT NULL,
                usage_count INTEGER DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_used TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT fk_classification_company 
                    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("   ✅ company_classification_rules table created/verified");
        }
    }
    
    /**
     * Create indexes for better performance
     */
    private void createIndexes(Connection conn) throws SQLException {
        System.out.println("🔍 Creating indexes for performance...");
        
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_bank_transactions_account ON bank_transactions(account_code, company_id)",
            "CREATE INDEX IF NOT EXISTS idx_classification_rules_company ON company_classification_rules(company_id, account_code)",
            "CREATE INDEX IF NOT EXISTS idx_classification_rules_pattern ON company_classification_rules(company_id, pattern)",
            "CREATE INDEX IF NOT EXISTS idx_classification_rules_usage ON company_classification_rules(company_id, usage_count DESC)"
        };
        
        try (Statement stmt = conn.createStatement()) {
            for (String indexSql : indexes) {
                try {
                    stmt.executeUpdate(indexSql);
                    System.out.println("   ✅ Created index");
                } catch (SQLException e) {
                    if (e.getMessage().contains("already exists")) {
                        System.out.println("   ℹ️ Index already exists");
                    } else {
                        System.err.println("   ⚠️ Error creating index: " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Verify schema changes
     */
    public void verifySchema() {
        System.out.println("\n🔍 VERIFYING SCHEMA CHANGES");
        System.out.println("-".repeat(40));
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            
            // Check bank_transactions columns
            System.out.println("📊 bank_transactions table:");
            verifyTableColumns(conn, "bank_transactions", new String[]{"account_code", "account_name"});
            
            // Check company_classification_rules table
            System.out.println("\n📋 company_classification_rules table:");
            verifyTableExists(conn, "company_classification_rules");
            
            System.out.println("\n✅ Schema verification completed!");
            
        } catch (SQLException e) {
            System.err.println("❌ Error verifying schema: " + e.getMessage());
        }
    }
    
    /**
     * Verify specific columns exist in a table
     */
    private void verifyTableColumns(Connection conn, String tableName, String[] expectedColumns) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        
        for (String columnName : expectedColumns) {
            try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    System.out.println("   ✅ Column '" + columnName + "' exists");
                } else {
                    System.out.println("   ❌ Column '" + columnName + "' missing");
                }
            }
        }
    }
    
    /**
     * Verify table exists
     */
    private void verifyTableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            if (rs.next()) {
                System.out.println("   ✅ Table '" + tableName + "' exists");
                
                // Count columns
                try (ResultSet colRs = meta.getColumns(null, null, tableName, null)) {
                    int columnCount = 0;
                    while (colRs.next()) {
                        columnCount++;
                    }
                    System.out.println("   📊 Contains " + columnCount + " columns");
                }
            } else {
                System.out.println("   ❌ Table '" + tableName + "' missing");
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            DatabaseSchemaUpdater updater = new DatabaseSchemaUpdater();
            updater.updateSchema();
            updater.verifySchema();
        } catch (Exception e) {
            System.err.println("❌ Error during schema update: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
