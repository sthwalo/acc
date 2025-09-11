/*
 * Copyright 2025 Sthwalo Nyoni
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * FIN Financial Management System
 * TRANSITION VERSION - Delegating to modular architecture
 */
package fin;

import fin.context.ApplicationContext;
import fin.controller.ApplicationController;
import fin.license.LicenseManager;

/**
 * Transition version of App.java
 * This maintains backward compatibility while delegating to the new modular architecture
 * 
 * @deprecated Use ConsoleApplication instead. This class will be removed in future versions.
 */
@Deprecated
public class AppTransition {
    
    /**
     * Legacy greeting method for backward compatibility
     */
    public String getGreeting(String name) {
        if (name != null && !name.trim().isEmpty()) {
            return "Hello, " + name + "! Welcome to the FIN application.";
        }
        return "Hello World! Welcome to the FIN application.";
    }
    
    /**
     * Legacy main method - now delegates to modular architecture
     * 
     * @deprecated Use ConsoleApplication.main() instead
     */
    @Deprecated
    public static void main(String[] args) {
        System.out.println("⚠️  Using legacy App.main() - Consider migrating to ConsoleApplication");
        System.out.println("🔄 Delegating to modular architecture...");
        
        // License compliance check
        if (!LicenseManager.checkLicenseCompliance()) {
            System.exit(1);
        }
        
        // Check if API mode is requested
        if (args.length > 0 && "api".equals(args[0])) {
            // Start API server mode (legacy support)
            System.out.println("🚀 Starting FIN API Server...");
            System.out.println("📊 Financial Management REST API");
            System.out.println("🌐 CORS enabled for frontend at http://localhost:3000");
            System.out.println("===============================================");
            
            try {
                fin.api.ApiServer apiServer = new fin.api.ApiServer();
                apiServer.start();
                
                // Add shutdown hook for graceful shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n🛑 Shutting down API server...");
                    apiServer.stop();
                    System.out.println("✅ Server stopped successfully");
                }));
                
                // Keep main thread alive
                Thread.currentThread().join();
                
            } catch (Exception e) {
                System.err.println("❌ Failed to start API server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // Console application mode - delegate to new architecture
            try {
                System.out.println("🚀 Starting FIN Financial Management System");
                System.out.println("📊 Modular Architecture (via legacy App.main)");
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
                
                // Start the application using new modular architecture
                controller.start();
                
            } catch (Exception e) {
                System.err.println("❌ Failed to start application: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        }
    }
}
