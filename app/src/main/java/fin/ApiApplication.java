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

            // Get API controllers from dependency injection context
            fin.api.controllers.AuthController authController = context.get(fin.api.controllers.AuthController.class);
            fin.api.controllers.CompanyController companyController = context.get(fin.api.controllers.CompanyController.class);
            fin.api.controllers.FiscalPeriodController fiscalPeriodController = context.get(fin.api.controllers.FiscalPeriodController.class);
            fin.api.controllers.TransactionController transactionController = context.get(fin.api.controllers.TransactionController.class);
            fin.api.controllers.UploadController uploadController = context.get(fin.api.controllers.UploadController.class);
            fin.api.controllers.ReportController reportController = context.get(fin.api.controllers.ReportController.class);
            fin.api.controllers.BudgetController budgetController = context.get(fin.api.controllers.BudgetController.class);
            fin.api.controllers.PayrollController payrollController = context.get(fin.api.controllers.PayrollController.class);
            fin.api.controllers.ClassificationController classificationController = context.get(fin.api.controllers.ClassificationController.class);
            fin.api.controllers.DataManagementController dataManagementController = context.get(fin.api.controllers.DataManagementController.class);
            fin.api.controllers.AccountController accountController = context.get(fin.api.controllers.AccountController.class);
            fin.api.controllers.DepreciationController depreciationController = context.get(fin.api.controllers.DepreciationController.class);
            fin.api.controllers.PlanController planController = context.get(fin.api.controllers.PlanController.class);

            // Start API server with access to all services and controllers
            fin.api.ApiServer apiServer = new fin.api.ApiServer(
                context.get(fin.service.UserService.class),
                context.get(fin.service.CompanyService.class),
                context.get(fin.service.BankStatementProcessingService.class),
                context.get(fin.service.TransactionClassificationService.class),
                context.get(fin.service.FinancialReportingService.class),
                context.get(fin.service.BudgetService.class),
                context.get(fin.service.PayrollService.class),
                context.get(fin.service.DataManagementService.class),
                context.get(fin.service.AccountManagementService.class),
                context.get(fin.service.DepreciationService.class),
                context.get(fin.service.JwtService.class),
                authController,
                companyController,
                fiscalPeriodController,
                transactionController,
                uploadController,
                reportController,
                budgetController,
                payrollController,
                classificationController,
                dataManagementController,
                accountController,
                depreciationController,
                planController
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