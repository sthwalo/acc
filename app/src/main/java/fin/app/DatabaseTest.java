package fin.app;

import java.sql.*;

/**
 * Simple Database Test to verify connection and basic queries
 */
public class DatabaseTest {
    
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/drimacc_db";
    private static final String DB_USER = "sthwalonyoni";
    private static final String DB_PASSWORD = System.getenv().getOrDefault("DB_PASSWORD", "password");
    
    public static void main(String[] args) {
        System.out.println("🔍 DATABASE CONNECTION TEST");
        System.out.println("===========================");
        
        try {
            System.out.println("📊 Connecting to database...");
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                System.out.println("✅ Database connection successful!");
                
                // Test company query
                System.out.println("📋 Testing company query...");
                String companySQL = "SELECT id, name, registration_number, address FROM companies WHERE id = 1";
                try (PreparedStatement pstmt = conn.prepareStatement(companySQL)) {
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        System.out.println("✅ Company found: " + rs.getString("name"));
                        System.out.println("   Registration: " + rs.getString("registration_number"));
                        System.out.println("   Address: " + rs.getString("address"));
                    }
                }
                
                // Test fiscal period query
                System.out.println("📅 Testing fiscal period query...");
                String periodSQL = "SELECT id, period_name, start_date, end_date FROM fiscal_periods WHERE id = 1";
                try (PreparedStatement pstmt = conn.prepareStatement(periodSQL)) {
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        System.out.println("✅ Fiscal period found: " + rs.getString("period_name"));
                        System.out.println("   From: " + rs.getDate("start_date"));
                        System.out.println("   To: " + rs.getDate("end_date"));
                    }
                }
                
                // Test accounts query
                System.out.println("💰 Testing accounts query...");
                String accountsSQL = """
                    SELECT a.account_code, a.account_name, at.name as account_type
                    FROM accounts a
                    JOIN account_categories ac ON a.category_id = ac.id
                    JOIN account_types at ON ac.account_type_id = at.id
                    WHERE a.company_id = ? AND at.name = ?
                    ORDER BY a.account_code
                    LIMIT 5
                """;
                try (PreparedStatement pstmt = conn.prepareStatement(accountsSQL)) {
                    pstmt.setLong(1, 1L);
                    pstmt.setString(2, "Asset");
                    
                    ResultSet rs = pstmt.executeQuery();
                    System.out.println("✅ Asset accounts found:");
                    while (rs.next()) {
                        System.out.println("   " + rs.getString("account_code") + " - " + rs.getString("account_name"));
                    }
                }
                
                // Test account balance query
                System.out.println("🧮 Testing account balance query...");
                String balanceSQL = """
                    SELECT a.account_code, a.account_name, at.name as account_type,
                           COALESCE(SUM(CASE WHEN jel.debit_amount IS NOT NULL THEN jel.debit_amount ELSE -jel.credit_amount END), 0) as balance
                    FROM accounts a
                    JOIN account_categories ac ON a.category_id = ac.id
                    JOIN account_types at ON ac.account_type_id = at.id
                    LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
                    LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
                    WHERE a.company_id = ? AND at.name = ?
                    AND (je.fiscal_period_id = ? OR je.fiscal_period_id IS NULL)
                    GROUP BY a.id, a.account_code, a.account_name, at.name
                    HAVING COALESCE(SUM(CASE WHEN jel.debit_amount IS NOT NULL THEN jel.debit_amount ELSE -jel.credit_amount END), 0) != 0
                    ORDER BY a.account_code
                    LIMIT 5
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(balanceSQL)) {
                    pstmt.setLong(1, 1L);
                    pstmt.setString(2, "Asset");
                    pstmt.setLong(3, 1L);
                    
                    ResultSet rs = pstmt.executeQuery();
                    System.out.println("✅ Asset balances found:");
                    while (rs.next()) {
                        System.out.printf("   %s - %s: R %.2f%n", 
                            rs.getString("account_code"), 
                            rs.getString("account_name"), 
                            rs.getDouble("balance"));
                    }
                }
                
                System.out.println();
                System.out.println("🎉 All database tests passed!");
                System.out.println("   Ready to generate Excel reports with real data!");
                
            }
            
        } catch (Exception e) {
            System.err.println("❌ Database test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
