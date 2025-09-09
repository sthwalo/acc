package fin.app;

import fin.config.DatabaseConfig;

import java.sql.*;
import java.util.Scanner;

/**
 * Database Cleanup Tool for Fresh Company Setup
 * Cleans all data except audit trails to start with a clean slate
 */
public class DatabaseCleanupTool {
    
    private final String dbUrl;
    private final Scanner scanner;
    
    public DatabaseCleanupTool() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.scanner = new Scanner(System.in);
    }
    
    public void performCleanup() {
        System.out.println("🧹 DATABASE CLEANUP TOOL");
        System.out.println("=".repeat(50));
        System.out.println("This will clean all data except audit trails");
        System.out.println("to create a fresh start for company setup.");
        System.out.println("=".repeat(50));
        
        // Show what will be cleaned
        showCleanupPlan();
        
        System.out.print("\n❓ Proceed with cleanup? (yes/no): ");
        String response = scanner.nextLine().trim().toLowerCase();
        
        if (!response.equals("yes")) {
            System.out.println("⏹️ Cleanup cancelled");
            return;
        }
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            // Clean data in proper order (respecting foreign keys)
            cleanTransactionData(conn);
            cleanCompanyClassificationRules(conn);
            cleanReportsData(conn);
            cleanCompanyData(conn);
            cleanBankAccounts(conn);
            cleanFiscalPeriods(conn);
            
            conn.commit();
            
            System.out.println("\n✅ DATABASE CLEANUP COMPLETED!");
            System.out.println("🎯 Ready for fresh company setup");
            
        } catch (SQLException e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Show what will be cleaned
     */
    private void showCleanupPlan() {
        System.out.println("\n📋 CLEANUP PLAN:");
        System.out.println("✅ WILL BE KEPT:");
        System.out.println("   • Audit trail records");
        System.out.println("   • Database schema structure");
        System.out.println("   • Account type definitions");
        System.out.println();
        System.out.println("🗑️ WILL BE DELETED:");
        System.out.println("   • All company data");
        System.out.println("   • All bank transactions");
        System.out.println("   • All classification rules");
        System.out.println("   • All fiscal periods");
        System.out.println("   • All bank accounts");
        System.out.println("   • Generated reports (file system)");
    }
    
    /**
     * Clean transaction-related data
     */
    private void cleanTransactionData(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning transaction data...");
        
        String[] tables = {
            "bank_transactions",
            "journal_entries",
            "ledger_entries"
        };
        
        for (String table : tables) {
            try {
                String sql = "DELETE FROM " + table;
                try (Statement stmt = conn.createStatement()) {
                    int deleted = stmt.executeUpdate(sql);
                    System.out.println("   📊 Deleted " + deleted + " records from " + table);
                }
            } catch (SQLException e) {
                if (!e.getMessage().contains("does not exist")) {
                    System.out.println("   ⚠️ Warning cleaning " + table + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clean company classification rules
     */
    private void cleanCompanyClassificationRules(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning classification rules...");
        
        try {
            String sql = "DELETE FROM company_classification_rules";
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(sql);
                System.out.println("   🧠 Deleted " + deleted + " classification rules");
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("does not exist")) {
                System.out.println("   ⚠️ Warning cleaning classification rules: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean reports data (but keep audit trails)
     */
    private void cleanReportsData(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning reports data...");
        
        // Note: We're not deleting audit trails, just other report-related data
        // If there are specific report tables, they would be cleaned here
        
        System.out.println("   📋 Audit trails preserved");
    }
    
    /**
     * Clean company data
     */
    private void cleanCompanyData(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning company data...");
        
        String[] tables = {
            "accounts",
            "account_categories", 
            "companies"
        };
        
        for (String table : tables) {
            try {
                String sql = "DELETE FROM " + table;
                try (Statement stmt = conn.createStatement()) {
                    int deleted = stmt.executeUpdate(sql);
                    System.out.println("   🏢 Deleted " + deleted + " records from " + table);
                }
            } catch (SQLException e) {
                if (!e.getMessage().contains("does not exist")) {
                    System.out.println("   ⚠️ Warning cleaning " + table + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Clean bank accounts
     */
    private void cleanBankAccounts(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning bank accounts...");
        
        try {
            String sql = "DELETE FROM bank_accounts";
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(sql);
                System.out.println("   🏦 Deleted " + deleted + " bank accounts");
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("does not exist")) {
                System.out.println("   ⚠️ Warning cleaning bank accounts: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean fiscal periods
     */
    private void cleanFiscalPeriods(Connection conn) throws SQLException {
        System.out.println("\n🔄 Cleaning fiscal periods...");
        
        try {
            String sql = "DELETE FROM fiscal_periods";
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(sql);
                System.out.println("   📅 Deleted " + deleted + " fiscal periods");
            }
        } catch (SQLException e) {
            if (!e.getMessage().contains("does not exist")) {
                System.out.println("   ⚠️ Warning cleaning fiscal periods: " + e.getMessage());
            }
        }
    }
    
    /**
     * Clean generated report files
     */
    public void cleanReportFiles() {
        System.out.println("\n🗂️ CLEANING REPORT FILES");
        System.out.println("-".repeat(30));
        
        java.io.File reportsDir = new java.io.File("reports");
        if (!reportsDir.exists()) {
            System.out.println("   ℹ️ No reports directory found");
            return;
        }
        
        // Clean generated reports but keep audit trails
        java.io.File[] files = reportsDir.listFiles();
        if (files != null) {
            int deletedCount = 0;
            int keptCount = 0;
            
            for (java.io.File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName().toLowerCase();
                    
                    // Keep audit trail files
                    if (fileName.contains("audit_trail")) {
                        System.out.println("   📋 Keeping: " + file.getName());
                        keptCount++;
                    } else {
                        if (file.delete()) {
                            System.out.println("   🗑️ Deleted: " + file.getName());
                            deletedCount++;
                        } else {
                            System.out.println("   ⚠️ Failed to delete: " + file.getName());
                        }
                    }
                }
            }
            
            System.out.println("\n   📊 Summary:");
            System.out.println("      Deleted: " + deletedCount + " files");
            System.out.println("      Kept: " + keptCount + " audit trail files");
        }
        
        // Clean generated subdirectory
        java.io.File generatedDir = new java.io.File("reports/generated");
        if (generatedDir.exists()) {
            java.io.File[] generatedFiles = generatedDir.listFiles();
            if (generatedFiles != null) {
                for (java.io.File file : generatedFiles) {
                    if (file.isFile() && file.delete()) {
                        System.out.println("   🗑️ Deleted generated: " + file.getName());
                    }
                }
            }
        }
    }
    
    /**
     * Verify cleanup results
     */
    public void verifyCleanup() {
        System.out.println("\n🔍 VERIFYING CLEANUP RESULTS");
        System.out.println("-".repeat(35));
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            
            String[] tables = {
                "companies", "bank_transactions", "company_classification_rules",
                "fiscal_periods", "bank_accounts", "accounts", "account_categories"
            };
            
            for (String table : tables) {
                try {
                    String sql = "SELECT COUNT(*) FROM " + table;
                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            if (count == 0) {
                                System.out.println("   ✅ " + table + ": " + count + " records");
                            } else {
                                System.out.println("   ⚠️ " + table + ": " + count + " records remaining");
                            }
                        }
                    }
                } catch (SQLException e) {
                    System.out.println("   ❓ " + table + ": table not found (OK)");
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error verifying cleanup: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            DatabaseCleanupTool tool = new DatabaseCleanupTool();
            tool.performCleanup();
            tool.cleanReportFiles();
            tool.verifyCleanup();
            
            System.out.println("\n🎉 CLEANUP COMPLETE!");
            System.out.println("Ready for fresh company setup and intelligent classification training.");
            
        } catch (Exception e) {
            System.err.println("❌ Error during cleanup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
