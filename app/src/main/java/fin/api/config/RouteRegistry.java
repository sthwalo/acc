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

package fin.api.config;

import com.google.gson.Gson;
import fin.api.controllers.*;
import fin.api.routes.*;
import fin.api.middleware.AuthMiddleware;

/**
 * Route Registry class
 * Central coordinator for registering all API routes
 * Ensures proper ordering of middleware, auth, and feature routes
 */
public class RouteRegistry {
    private final Gson gson;
    private final AuthMiddleware authMiddleware;
    private final AuthController authController;
    private final CompanyController companyController;
    private final FiscalPeriodController fiscalPeriodController;
    private final TransactionController transactionController;
    private final UploadController uploadController;
    private final ReportController reportController;
    private final BudgetController budgetController;
    private final PayrollController payrollController;
    private final ClassificationController classificationController;
    private final DataManagementController dataManagementController;
    private final AccountController accountController;
    private final DepreciationController depreciationController;
    private final PlanController planController;

    /**
     * Constructor with dependency injection for all controllers
     */
    public RouteRegistry(
        AuthController authController,
        CompanyController companyController,
        FiscalPeriodController fiscalPeriodController,
        TransactionController transactionController,
        UploadController uploadController,
        ReportController reportController,
        BudgetController budgetController,
        PayrollController payrollController,
        ClassificationController classificationController,
        DataManagementController dataManagementController,
        AccountController accountController,
        DepreciationController depreciationController,
        PlanController planController
    ) {
        this.gson = new Gson();
        this.authMiddleware = new AuthMiddleware(null); // TODO: Remove after SparkJava migration
        this.authController = authController;
        this.companyController = companyController;
        this.fiscalPeriodController = fiscalPeriodController;
        this.transactionController = transactionController;
        this.uploadController = uploadController;
        this.reportController = reportController;
        this.budgetController = budgetController;
        this.payrollController = payrollController;
        this.classificationController = classificationController;
        this.dataManagementController = dataManagementController;
        this.accountController = accountController;
        this.depreciationController = depreciationController;
        this.planController = planController;
    }

    /**
     * Registers all API routes in the correct order
     * Order is important: system routes first, then auth, then features
     */
    public void register() {
        System.out.println("🔗 Registering API routes...");

        // Register authentication routes (login, register, logout)
        AuthRoutes.register(gson, authController, authMiddleware);

        // Register plan routes (for user registration)
        PlanRoutes.register(gson, planController);

        // Register company routes
        CompanyRoutes.register(gson, companyController, authMiddleware);

        // Register fiscal period routes
        FiscalPeriodRoutes.register(gson, fiscalPeriodController, authMiddleware);

        // Register transaction routes
        TransactionRoutes.register(gson, transactionController, authMiddleware);

        // Register upload routes
        UploadRoutes.register(gson, uploadController, authMiddleware);

        // Register report routes
        ReportRoutes.register(gson, reportController, authMiddleware);

        // Register budget routes
        BudgetRoutes.register(gson, budgetController, authMiddleware);

        // Register payroll routes
        PayrollRoutes.register(gson, payrollController, authMiddleware);

        // Register classification routes
        ClassificationRoutes.register(gson, classificationController, authMiddleware);

        // Register data management routes
        DataManagementRoutes.register(gson, dataManagementController, authMiddleware);

        // Register account routes
        AccountRoutes.register(gson, accountController, authMiddleware);

        // Register depreciation routes
        DepreciationRoutes.register(gson, depreciationController, authMiddleware);

        System.out.println("✅ API routes registered successfully");
    }
}