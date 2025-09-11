package fin;

import fin.context.ApplicationContext;
import fin.controller.ApplicationController;
import fin.license.LicenseManager;

/**
 * Minimal console application entry point
 * Replaces the monolithic App.java main method
 */
public class ConsoleApplication {
    
    public static void main(String[] args) {
        try {
            // License compliance check
            if (!LicenseManager.checkLicenseCompliance()) {
                System.err.println("❌ License compliance check failed");
                System.exit(1);
            }
            
            System.out.println("🚀 Starting FIN Financial Management System");
            System.out.println("📊 Modular Architecture - Console Mode");
            System.out.println("===============================================");
            
            // Initialize application context with dependency injection
            ApplicationContext context = new ApplicationContext();
            
            // Get main application controller
            ApplicationController controller = context.getApplicationController();
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down application...");
                try {
                    controller.shutdown();
                    context.shutdown();
                    System.out.println("✅ Application stopped successfully");
                } catch (Exception e) {
                    System.err.println("❌ Error during shutdown: " + e.getMessage());
                }
            }));
            
            // Start the application
            controller.start();
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
