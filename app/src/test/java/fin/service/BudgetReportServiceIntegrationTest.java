package fin.service;

import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
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

        try {
            service.generateBudgetSummaryReport(companyId);

            // Check if report was generated
            Path reportsDir = Paths.get("reports");
            if (Files.exists(reportsDir)) {
                File[] reportFiles = reportsDir.toFile().listFiles((dir, name) ->
                    name.startsWith("budget_summary_report_") && name.endsWith(".pdf"));

                assertNotNull(reportFiles, "Report files should be generated");
                assertTrue(reportFiles.length > 0, "At least one budget summary report should be generated");

                // Check file size (should be > 0 for valid PDF)
                File latestReport = reportFiles[reportFiles.length - 1];
                assertTrue(latestReport.length() > 1000, "Report file should have substantial content");

                System.out.println("✅ Budget summary report generated successfully: " + latestReport.getName());
                System.out.println("File size: " + latestReport.length() + " bytes");
            }

        } catch (Exception e) {
            System.err.println("❌ Budget summary report generation failed: " + e.getMessage());
            throw e;
        }
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

        try {
            service.generateStrategicPlanReport(companyId);

            // Check if report was generated
            Path reportsDir = Paths.get("reports");
            if (Files.exists(reportsDir)) {
                File[] reportFiles = reportsDir.toFile().listFiles((dir, name) ->
                    name.startsWith("strategic_plan_report_") && name.endsWith(".pdf"));

                assertNotNull(reportFiles, "Report files should be generated");
                assertTrue(reportFiles.length > 0, "At least one strategic plan report should be generated");

                // Check file size (should be > 0 for valid PDF)
                File latestReport = reportFiles[reportFiles.length - 1];
                assertTrue(latestReport.length() > 1000, "Report file should have substantial content");

                System.out.println("✅ Strategic plan report generated successfully: " + latestReport.getName());
                System.out.println("File size: " + latestReport.length() + " bytes");
            }

        } catch (Exception e) {
            System.err.println("❌ Strategic plan report generation failed: " + e.getMessage());
            throw e;
        }
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

        try {
            service.generateBudgetVsActualReport(companyId);

            // Check if report was generated
            Path reportsDir = Paths.get("reports");
            if (Files.exists(reportsDir)) {
                File[] reportFiles = reportsDir.toFile().listFiles((dir, name) ->
                    name.startsWith("budget_vs_actual_report_") && name.endsWith(".pdf"));

                assertNotNull(reportFiles, "Report files should be generated");
                assertTrue(reportFiles.length > 0, "At least one budget vs actual report should be generated");

                // Check file size (should be > 0 for valid PDF)
                File latestReport = reportFiles[reportFiles.length - 1];
                assertTrue(latestReport.length() > 1000, "Report file should have substantial content");

                System.out.println("✅ Budget vs actual report generated successfully: " + latestReport.getName());
                System.out.println("File size: " + latestReport.length() + " bytes");
            }

        } catch (Exception e) {
            System.err.println("❌ Budget vs actual report generation failed: " + e.getMessage());
            throw e;
        }
    }
}