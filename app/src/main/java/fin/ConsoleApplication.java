/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
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
 */

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
            // Check for batch mode
            if (args.length > 0 && "--batch".equals(args[0])) {
                runBatchMode(args);
                return;
            }
            
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
    
    /**
     * Run application in batch mode for automated processing
     */
    private static void runBatchMode(String[] args) {
        try {
            System.out.println("🚀 Starting FIN Financial Management System - Batch Mode");
            System.out.println("📊 Automated Processing");
            System.out.println("===============================================");
            
            // License compliance check
            if (!LicenseManager.checkLicenseCompliance()) {
                System.err.println("❌ License compliance check failed");
                System.exit(1);
            }
            
            // Initialize application context
            ApplicationContext context = new ApplicationContext();
            
            // Parse batch arguments
            BatchProcessor processor = new BatchProcessor(context);
            processor.processBatchCommand(args);
            
            // Shutdown cleanly
            context.shutdown();
            System.out.println("✅ Batch processing completed successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Batch processing failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
