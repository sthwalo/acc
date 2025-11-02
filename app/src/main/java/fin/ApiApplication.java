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
import fin.license.LicenseManager;

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
                context.get(fin.service.BankStatementProcessingService.class)
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