package fin.debug;

import fin.service.PayrollReportService;
import org.junit.jupiter.api.Test;

/**
 * Simple debug test to see PayrollReportService debugging output
 * This helps diagnose why payroll reports show R0.00 values for company 1
 */
public class PayrollReportDebugger {
    
    @Test
    public void debugPayrollReport() {
        try {
            System.out.println("🧪 Debugging PayrollReportService for Company 1 (Limelight Academy)");
            System.out.println("=".repeat(80));
            
            // Use production database URL
            String dbUrl = System.getenv("DATABASE_URL");
            if (dbUrl == null) {
                dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
            }
            
            // Add credentials
            String dbUser = System.getenv("DATABASE_USER");
            String dbPassword = System.getenv("DATABASE_PASSWORD");
            if (dbUser != null && dbPassword != null) {
                String separator = dbUrl.contains("?") ? "&" : "?";
                dbUrl = dbUrl + separator + "user=" + dbUser + "&password=" + dbPassword;
            }
            
            System.out.println("🔗 Database URL: " + dbUrl.replaceAll("password=[^&]*", "password=***"));
            
            // Initialize service
            PayrollReportService service = new PayrollReportService(dbUrl);
            
            System.out.println("📊 Testing payroll summary report generation...");
            System.out.println("-".repeat(80));
            
            // This should show the debugging output we added
            service.generatePayrollSummaryReport(1L);
            
            System.out.println("-".repeat(80));
            System.out.println("✅ Debug test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during debugging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}