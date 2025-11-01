package fin.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BudgetReportServiceIntegrationTest {

    @Test
    void testGenerateBudgetSummaryReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return; // Skip test if no .env file
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateBudgetSummaryReport(companyId),
            "Budget summary report generation should complete without throwing exceptions");

        System.out.println("✅ Budget summary report generation test passed (PDF generation skipped in test mode)");
    }

    @Test
    void testGenerateStrategicPlanReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return;
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateStrategicPlanReport(companyId),
            "Strategic plan report generation should complete without throwing exceptions");

        System.out.println("✅ Strategic plan report generation test passed (PDF generation skipped in test mode)");
    }

    @Test
    void testGenerateBudgetVsActualReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return;
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateBudgetVsActualReport(companyId),
            "Budget vs actual report generation should complete without throwing exceptions");

        System.out.println("✅ Budget vs actual report generation test passed (PDF generation skipped in test mode)");
    }
}