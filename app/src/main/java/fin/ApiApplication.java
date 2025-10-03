package fin;

import fin.context.ApplicationContext;
import fin.license.LicenseManager;
import fin.service.*;

/**
 * API Server entry point for FIN Financial Management System
 * Uses the modular architecture with dependency injection
 */
public class ApiApplication {

    public static void main(String[] args) {
        try {
            // License compliance check
            if (!LicenseManager.checkLicenseCompliance()) {
                System.err.println("❌ License compliance check failed");
                System.exit(1);
            }

            System.out.println("🚀 Starting FIN API Server...");
            System.out.println("📊 Financial Management REST API - Modular Architecture");
            System.out.println("🌐 CORS enabled for frontend at http://localhost:3000");
            System.out.println("===============================================");

            // Initialize application context with dependency injection
            ApplicationContext context = new ApplicationContext();

            // Start API server with access to all services
            fin.api.ApiServer apiServer = new fin.api.ApiServer(
                context.get(fin.service.CompanyService.class),
                context.get(fin.service.CsvImportService.class),
                context.get(fin.service.ReportService.class),
                context.get(fin.service.FinancialReportingService.class),
                context.get(fin.service.PdfExportService.class),
                context.get(fin.service.DataManagementService.class),
                context.get(fin.service.BankStatementProcessingService.class),
                context.get(fin.service.TransactionVerificationService.class),
                context.get(fin.service.TransactionClassificationService.class),
                context.get(fin.service.PayrollService.class)
            );

            apiServer.start();

            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down API server...");
                try {
                    apiServer.stop();
                    context.shutdown();
                    System.out.println("✅ Server stopped successfully");
                } catch (Exception e) {
                    System.err.println("❌ Error during shutdown: " + e.getMessage());
                }
            }));

            // Keep main thread alive
            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ Failed to start API server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}