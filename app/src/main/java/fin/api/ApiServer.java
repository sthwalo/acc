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

package fin.api;

import fin.api.config.*;
import fin.api.controllers.*;
import fin.api.routes.*;
import fin.api.middleware.AuthMiddleware;
import fin.api.util.ApiConstants;
import fin.service.*;
import spark.Spark;
import com.google.gson.Gson;

/**
 * Modular REST API Server for FIN Financial Management System.
 *
 * This server provides REST endpoints for all backend console functionalities:
 * - Company management and fiscal periods
 * - Transaction processing and classification
 * - Financial reporting (trial balance, income statement, balance sheet, etc.)
 * - Budget management and variance analysis
 * - Payroll processing with SARS compliance
 * - Data management and audit trails
 * - Account management and depreciation
 * - System logs and time tracking
 *
 * Architecture: Modular design with separate packages for config, controllers,
 * routes, middleware, DTOs, and utilities. Follows JAR-first development workflow.
 */
public class ApiServer {

    // Core services
    private final UserService userService;
    private final CompanyService companyService;
    private final BankStatementProcessingService bankStatementProcessingService;
    private final TransactionClassificationService transactionClassificationService;
    private final FinancialReportingService financialReportingService;
    private final BudgetService budgetService;
    private final PayrollService payrollService;
    private final DataManagementService dataManagementService;
    private final AccountManagementService accountManagementService;
    private final DepreciationService depreciationService;
    private final JwtService jwtService;

    // API components
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
     * Constructor with dependency injection for all services and API components.
     * Follows secure constructor pattern with final fields.
     */
    public ApiServer(
            UserService userService,
            CompanyService companyService,
            BankStatementProcessingService bankStatementProcessingService,
            TransactionClassificationService transactionClassificationService,
            FinancialReportingService financialReportingService,
            BudgetService budgetService,
            PayrollService payrollService,
            DataManagementService dataManagementService,
            AccountManagementService accountManagementService,
            DepreciationService depreciationService,
            JwtService jwtService,
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
            PlanController planController) {

        this.userService = userService;
        this.companyService = companyService;
        this.bankStatementProcessingService = bankStatementProcessingService;
        this.transactionClassificationService = transactionClassificationService;
        this.financialReportingService = financialReportingService;
        this.budgetService = budgetService;
        this.payrollService = payrollService;
        this.dataManagementService = dataManagementService;
        this.accountManagementService = accountManagementService;
        this.depreciationService = depreciationService;
        this.jwtService = jwtService;

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
     * Starts the API server with modular configuration and route registration.
     * Uses ApiConfig for server setup, CorsConfig for CORS, ExceptionConfig for
     * exception handling, and individual route classes for modular route registration.
     */
    public void start() {
        System.out.println("🚀 Starting FIN API Server...");

        // Configure server settings
        ApiConfig.configure();

        // Configure CORS for frontend integration
        CorsConfig.configure();

        // Configure exception handling
        ExceptionConfig.configure();

        // Initialize common dependencies
        Gson gson = new Gson();
        AuthMiddleware authMiddleware = new AuthMiddleware(jwtService);

        // Register all routes through individual route classes
        AuthRoutes.register(gson, authController, authMiddleware);
        PlanRoutes.register(gson, planController);
        CompanyRoutes.register(gson, companyController, authMiddleware);
        FiscalPeriodRoutes.register(gson, fiscalPeriodController, authMiddleware);
        TransactionRoutes.register(gson, transactionController, authMiddleware);
        UploadRoutes.register(gson, uploadController, authMiddleware);
        ReportRoutes.register(gson, reportController, authMiddleware);
        BudgetRoutes.register(gson, budgetController, authMiddleware);
        PayrollRoutes.register(gson, payrollController, authMiddleware);
        ClassificationRoutes.register(gson, classificationController, authMiddleware);
        DataManagementRoutes.register(gson, dataManagementController, authMiddleware);
        AccountRoutes.register(gson, accountController, authMiddleware);
        DepreciationRoutes.register(gson, depreciationController, authMiddleware);

        System.out.println("✅ FIN API Server started on port " + ApiConstants.SERVER_PORT);
        System.out.println("🌐 Health check: http://localhost:" + ApiConstants.SERVER_PORT + "/api/v1/health");
    }    /**
     * Stops the API server and closes database connections.
     */
    public void stop() {
        Spark.stop();
        System.out.println("🛑 FIN API Server stopped");
    }
}