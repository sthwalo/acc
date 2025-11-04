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

package fin.context;

import fin.config.DatabaseConfig;
import fin.controller.ApplicationController;
import fin.controller.BudgetController;
import fin.controller.CompanyController;
import fin.controller.DataManagementController;
import fin.controller.DepreciationController;
import fin.controller.FiscalPeriodController;
import fin.controller.ImportController;
import fin.controller.PayrollController;
import fin.controller.ReportController;
import fin.repository.CompanyRepository;
import fin.repository.DepreciationRepository;
import fin.service.AccountClassificationService;
import fin.service.AccountManagementService;
import fin.service.BalanceSheetService;
import fin.service.BankStatementProcessingService;
import fin.service.BudgetReportService;
import fin.service.BudgetService;
import fin.service.CategoryManagementService;
import fin.service.ClassificationRuleManager;
import fin.service.CompanyLogoService;
import fin.service.CompanyService;
import fin.service.CsvExportService;
import fin.service.CsvImportService;
import fin.service.DataManagementService;
import fin.service.DepreciationService;
import fin.service.FinancialReportingService;
import fin.service.InvoicePdfService;
import fin.service.GeneralLedgerService;
import fin.service.IncomeStatementService;
import fin.service.InteractiveClassificationService;
import fin.service.OpeningBalanceService;
import fin.service.PayrollReportService;
import fin.service.PayrollService;
import fin.service.PayslipPdfService;
import fin.service.PdfBrandingService;
import fin.service.PdfExportService;
import fin.service.ReportService;
import fin.service.TextReportToPdfService;
import fin.service.StrategicPlanningService;
import fin.service.TransactionClassificationService;
import fin.service.TrialBalanceService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.nio.charset.StandardCharsets;

/**
 * Application context for dependency injection
 * Replaces manual service instantiation from monolithic App.java constructor
 */
public class ApplicationContext {
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final String dbUrl;
    
    // Database connection pool configuration
    private static final int DATABASE_MAX_POOL_SIZE = 5;
    private static final int DATABASE_MIN_IDLE = 2;
    
    public ApplicationContext() {
        // Determine database URL first (validation before assignment)
        String databaseUrl;
        
        // Check if we're in test mode and use test database URL
        String testDbUrl = System.getProperty("fin.database.test.url");
        if (testDbUrl != null && !testDbUrl.isEmpty()) {
            databaseUrl = testDbUrl;
            System.out.println("🧪 Using test database: " + testDbUrl);
        } else {
            // Use URL with embedded credentials for compatibility with legacy code
            // that calls DriverManager.getConnection(url) directly
            databaseUrl = DatabaseConfig.getDatabaseUrlWithCredentials();
            System.out.println("🔌 Application connected to: " + DatabaseConfig.getDatabaseType());
        }
        
        // Initialize all services BEFORE assigning fields (secure constructor pattern)
        try {
            initializeServices(databaseUrl);
            initializeUIComponents();
            initializeControllers();
            initializeApplicationController();
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize application components: " + e.getMessage() + " - continuing with partial initialization");
            // Don't throw exception - allow constructor to complete
        }
        
        // Only assign field after initialization attempts
        this.dbUrl = databaseUrl;
        
        System.out.println("✅ Application context initialized successfully");
    }
    
    /**
     * Initialize core business services
     * Replaces service instantiation from App.java constructor
     */
    private void initializeServices(String initialDbUrl) {
        try {
            initializeCoreServices(initialDbUrl);
            initializeTransactionClassificationServices(initialDbUrl);
            initializePayrollServices(initialDbUrl);
            initializeFinancialServices(initialDbUrl);
            initializeNewFeatureServices(initialDbUrl);
            initializeDepreciationServices(initialDbUrl);
            
            System.out.println("📦 Core services initialized");
        } catch (Exception e) {
            System.out.println("❌ Failed to initialize services: " + e.getMessage() + " - continuing with partial initialization");
            // Don't throw exception - allow constructor to complete
        }
    }
    
    /**
     * Initialize core business services
     */
    private void initializeCoreServices(String initialDbUrl) {
        // Core services
        CompanyService companyService = new CompanyService(initialDbUrl);
        register(CompanyService.class, companyService);
        
        // Company logo service (depends on CompanyService)
        CompanyLogoService companyLogoService = new CompanyLogoService(initialDbUrl);
        register(CompanyLogoService.class, companyLogoService);
        
        CsvImportService csvImportService = new CsvImportService(initialDbUrl, companyService);
        register(CsvImportService.class, csvImportService);
        
        ReportService reportService = new ReportService(initialDbUrl, csvImportService);
        register(ReportService.class, reportService);
        
        PdfExportService pdfExportService = new PdfExportService();
        register(PdfExportService.class, pdfExportService);
        
        // PDF Branding Service for report headers/footers
        PdfBrandingService pdfBrandingService = new PdfBrandingService();
        register(PdfBrandingService.class, pdfBrandingService);
        
        // Text-to-PDF conversion service for financial reports
        TextReportToPdfService textReportToPdfService = new TextReportToPdfService();
        register(TextReportToPdfService.class, textReportToPdfService);
        
        // Financial Reporting Service with PDF export capability
        FinancialReportingService financialReportingService = new FinancialReportingService(initialDbUrl, textReportToPdfService);
        register(FinancialReportingService.class, financialReportingService);
        
        DataManagementService dataManagementService = new DataManagementService(
            initialDbUrl, companyService, csvImportService.getAccountService());
        register(DataManagementService.class, dataManagementService);
        
        BankStatementProcessingService bankStatementService = new BankStatementProcessingService(initialDbUrl);
        register(BankStatementProcessingService.class, bankStatementService);
        
        InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService();
        register(InteractiveClassificationService.class, interactiveClassificationService);
    }
    
    /**
     * Initialize transaction classification services
     */
    private void initializeTransactionClassificationServices(String initialDbUrl) {
        // Transaction Classification Service (unified entry point)
        // Create specialized services needed for TransactionClassificationService
        CategoryManagementService categoryManagementService = new CategoryManagementService(initialDbUrl);
        register(CategoryManagementService.class, categoryManagementService);
        
        AccountManagementService accountManagementService = new AccountManagementService(initialDbUrl);
        register(AccountManagementService.class, accountManagementService);
        
        // CONSOLIDATION: Replace TransactionMappingRuleService with ClassificationRuleManager
        // ClassificationRuleManager handles both standard and company-specific learned rules
        ClassificationRuleManager classificationRuleManager = new ClassificationRuleManager();
        register(ClassificationRuleManager.class, classificationRuleManager);
        
        // Phase 4: ChartOfAccountsService removed - AccountClassificationService is single source of truth
        // AccountService now uses AccountClassificationService directly
        AccountClassificationService accountClassificationService = new AccountClassificationService(initialDbUrl);
        register(AccountClassificationService.class, accountClassificationService);
        
        // REMOVED: TransactionMappingService - consolidated into AccountClassificationService
        
        // NOTE: TransactionClassificationService uses AccountClassificationService as single source of truth
        TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
            initialDbUrl,
            classificationRuleManager,  // Updated to use ClassificationRuleManager instead of TransactionMappingRuleService
            get(InteractiveClassificationService.class)
        );
        register(TransactionClassificationService.class, transactionClassificationService);
        
        CsvExportService csvExportService = new CsvExportService(get(CompanyService.class));
        register(CsvExportService.class, csvExportService);
    }
    
    /**
     * Initialize payroll services
     */
    private void initializePayrollServices(String initialDbUrl) {
        // Payroll service dependencies
        CompanyRepository companyRepository = new CompanyRepository(initialDbUrl);
        register(CompanyRepository.class, companyRepository);
        
        PayslipPdfService payslipPdfService = new PayslipPdfService();
        register(PayslipPdfService.class, payslipPdfService);
        
        // Payroll service
        PayrollService payrollService = new PayrollService(initialDbUrl, companyRepository, payslipPdfService, null);
        register(PayrollService.class, payrollService);
        
        // Payroll report service
        PayrollReportService payrollReportService = new PayrollReportService(initialDbUrl);
        register(PayrollReportService.class, payrollReportService);
        
        // Opening Balance service
        OpeningBalanceService openingBalanceService = new OpeningBalanceService(initialDbUrl);
        register(OpeningBalanceService.class, openingBalanceService);
    }
    
    /**
     * Initialize financial services
     */
    private void initializeFinancialServices(String initialDbUrl) {
        // Financial Data Repository (needed by GL and TB services)
        // Create DataSource for repository (same pattern as FinancialReportingService)
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(initialDbUrl);
        config.setMaximumPoolSize(DATABASE_MAX_POOL_SIZE);
        config.setMinimumIdle(DATABASE_MIN_IDLE);
        com.zaxxer.hikari.HikariDataSource repositoryDataSource = new com.zaxxer.hikari.HikariDataSource(config);
        
        fin.repository.FinancialDataRepository financialDataRepository = new fin.repository.JdbcFinancialDataRepository(repositoryDataSource);
        register(fin.repository.FinancialDataRepository.class, financialDataRepository);
        
        // General Ledger Service
        GeneralLedgerService generalLedgerService = new GeneralLedgerService(financialDataRepository);
        register(GeneralLedgerService.class, generalLedgerService);
        
        // Trial Balance Service (depends on General Ledger)
        TrialBalanceService trialBalanceService = new TrialBalanceService(financialDataRepository, generalLedgerService);
        register(TrialBalanceService.class, trialBalanceService);
        
        // Income Statement Service (depends on General Ledger for TB data)
        IncomeStatementService incomeStatementService = new IncomeStatementService(financialDataRepository, generalLedgerService);
        register(IncomeStatementService.class, incomeStatementService);
        
        // Balance Sheet Service (depends on General Ledger for TB data)
        BalanceSheetService balanceSheetService = new BalanceSheetService(financialDataRepository, generalLedgerService);
        register(BalanceSheetService.class, balanceSheetService);
    }
    
    /**
     * Initialize new feature services
     */
    private void initializeNewFeatureServices(String initialDbUrl) {
        // Invoice PDF Service (new feature for TASK 6.3)
        InvoicePdfService invoicePdfService = new InvoicePdfService(initialDbUrl);
        register(InvoicePdfService.class, invoicePdfService);
        
        // Strategic Planning Service (new feature for TASK 6.1)
        StrategicPlanningService strategicPlanningService = new StrategicPlanningService(initialDbUrl, this);
        register(StrategicPlanningService.class, strategicPlanningService);
        
        // Budget Service (new feature for TASK 6.1)
        BudgetService budgetService = new BudgetService(initialDbUrl, this);
        register(BudgetService.class, budgetService);

        BudgetReportService budgetReportService = new BudgetReportService(initialDbUrl);
        register(BudgetReportService.class, budgetReportService);
    }
    
    /**
     * Initialize depreciation services
     */
    private void initializeDepreciationServices(String initialDbUrl) {
        // Depreciation Service (new feature)
        DepreciationRepository depreciationRepository = new DepreciationRepository(initialDbUrl);
        register(DepreciationRepository.class, depreciationRepository);
        
        DepreciationService depreciationService = new DepreciationService(depreciationRepository);
        register(DepreciationService.class, depreciationService);
    }
    
    /**
     * Initialize UI components
     * Replaces UI logic scattered throughout App.java
     */
    private void initializeUIComponents() {
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        
        ConsoleMenu menu = new ConsoleMenu();
        register(ConsoleMenu.class, menu);
        
        InputHandler inputHandler = new InputHandler(scanner);
        register(InputHandler.class, inputHandler);
        
        OutputFormatter outputFormatter = new OutputFormatter();
        register(OutputFormatter.class, outputFormatter);
        
        ApplicationState applicationState = new ApplicationState();
        register(ApplicationState.class, applicationState);
        
        System.out.println("🖥️  UI components initialized");
    }
    
    /**
     * Initialize domain controllers
     * Replaces handler methods from App.java
     */
    private void initializeControllers() {
        // Get common dependencies
        ConsoleMenu menu = get(ConsoleMenu.class);
        InputHandler inputHandler = get(InputHandler.class);
        OutputFormatter outputFormatter = get(OutputFormatter.class);
        ApplicationState applicationState = get(ApplicationState.class);

        // Initialize controllers by domain
        initializeCompanyRelatedControllers(menu, inputHandler, outputFormatter, applicationState);
        initializeImportRelatedControllers(menu, inputHandler, outputFormatter, applicationState);
        initializeReportRelatedControllers(menu, inputHandler, outputFormatter, applicationState);
        initializeDataManagementControllers(menu, inputHandler, outputFormatter, applicationState);
        initializePayrollControllers(inputHandler, outputFormatter);
        initializeDepreciationControllers(menu, inputHandler, outputFormatter, applicationState);

        System.out.println("🎮 Domain controllers initialized");
    }

    /**
     * Initialize company and fiscal period related controllers
     */
    private void initializeCompanyRelatedControllers(ConsoleMenu menu, InputHandler inputHandler,
                                                   OutputFormatter outputFormatter, ApplicationState applicationState) {
        // Company controller
        CompanyController companyController = new CompanyController(
            get(CompanyService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(CompanyController.class, companyController);

        // Fiscal period controller
        FiscalPeriodController fiscalPeriodController = new FiscalPeriodController(
            get(CompanyService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(FiscalPeriodController.class, fiscalPeriodController);
    }

    /**
     * Initialize import related controllers
     */
    private void initializeImportRelatedControllers(ConsoleMenu menu, InputHandler inputHandler,
                                                  OutputFormatter outputFormatter, ApplicationState applicationState) {
        // Import controller
        ImportController importController = new ImportController(
            get(BankStatementProcessingService.class),
            get(CsvImportService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(ImportController.class, importController);
    }

    /**
     * Initialize report related controllers
     */
    private void initializeReportRelatedControllers(ConsoleMenu menu, InputHandler inputHandler,
                                                  OutputFormatter outputFormatter, ApplicationState applicationState) {
        // Report controller
        ReportController reportController = new ReportController(
            get(FinancialReportingService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(ReportController.class, reportController);
    }

    /**
     * Initialize data management controllers
     */
    private void initializeDataManagementControllers(ConsoleMenu menu, InputHandler inputHandler,
                                                   OutputFormatter outputFormatter, ApplicationState applicationState) {
        // Data management controller
        DataManagementController dataManagementController = new DataManagementController(
            get(DataManagementService.class),
            get(TransactionClassificationService.class),
            get(CsvExportService.class),
            get(CsvImportService.class),
            get(InvoicePdfService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(DataManagementController.class, dataManagementController);
        
        // Budget controller (new feature for TASK 6.1)
        BudgetController budgetController = new BudgetController(
            get(BudgetService.class),
            get(StrategicPlanningService.class),
            get(BudgetReportService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(BudgetController.class, budgetController);
    }

    /**
     * Initialize payroll controllers
     */
    private void initializePayrollControllers(InputHandler inputHandler, OutputFormatter outputFormatter) {
        // Payroll controller
        PayrollController payrollController = new PayrollController(
            get(PayrollService.class),
            get(PayrollReportService.class),
            inputHandler,
            outputFormatter
        );
        register(PayrollController.class, payrollController);
    }
    
    /**
     * Initialize depreciation controllers
     */
    private void initializeDepreciationControllers(ConsoleMenu menu, InputHandler inputHandler,
                                                 OutputFormatter outputFormatter, ApplicationState applicationState) {
        // Depreciation controller
        DepreciationController depreciationController = new DepreciationController(
            get(DepreciationService.class),
            applicationState,
            inputHandler
        );
        register(DepreciationController.class, depreciationController);
    }
    
    /**
     * Initialize main application controller
     * Replaces main method logic from App.java
     */
    private void initializeApplicationController() {
        ApplicationController applicationController = new ApplicationController(
            get(ConsoleMenu.class),
            get(InputHandler.class),
            get(OutputFormatter.class),
            get(ApplicationState.class),
            get(CompanyController.class),
            get(FiscalPeriodController.class),
            get(ImportController.class),
            get(ReportController.class),
            get(DataManagementController.class),
            get(PayrollController.class),
            get(BudgetController.class),
            get(DepreciationController.class)
        );
        register(ApplicationController.class, applicationController);
        
        System.out.println("🚀 Application controller initialized");
    }
    
    /**
     * Register a service in the context
     */
    private <T> void register(Class<T> type, T instance) {
        services.put(type, instance);
    }
    
    /**
     * Get a service from the context
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + type.getName());
        }
        return (T) service;
    }
    
    /**
     * Check if a service is registered
     */
    public boolean has(Class<?> type) {
        return services.containsKey(type);
    }
    
    /**
     * Get the main application controller
     * Entry point for starting the application
     */
    public ApplicationController getApplicationController() {
        return get(ApplicationController.class);
    }
    
    /**
     * Get application state for external access
     */
    public ApplicationState getApplicationState() {
        return get(ApplicationState.class);
    }
    
    /**
     * Get the interactive classification service
     */
    public InteractiveClassificationService getInteractiveClassificationService() {
        return get(InteractiveClassificationService.class);
    }
    
    /**
     * Get the payroll report service
     */
    public PayrollReportService getPayrollReportService() {
        return new PayrollReportService(DatabaseConfig.getDatabaseUrlWithCredentials());
    }

    /**
     * Get the payroll service
     */
    public PayrollService getPayrollService() {
        return get(PayrollService.class);
    }
    
    /**
     * Get the payroll controller
     */
    public PayrollController getPayrollController() {
        return new PayrollController(
            getPayrollService(),
            getPayrollReportService(),
            getInputHandler(),
            getOutputFormatter()
        );
    }
    
    /**
     * Get the input handler
     */
    public InputHandler getInputHandler() {
        return get(InputHandler.class);
    }

    /**
     * Get the output formatter
     */
    public OutputFormatter getOutputFormatter() {
        return get(OutputFormatter.class);
    }
    
    /**
     * Shutdown the application context
     */
    public void shutdown() {
        System.out.println("🔄 Shutting down application context...");
        
        // Cleanup operations could go here
        // For example, closing database connections, saving state, etc.
        
        services.clear();
        System.out.println("✅ Application context shutdown complete");
    }
    
    /**
     * Get context information for debugging
     */
    public String getContextInfo() {
        StringBuilder info = new StringBuilder();
        info.append("ApplicationContext Information:\n");
        info.append("Database URL: ").append(dbUrl).append("\n");
        info.append("Registered services: ").append(services.size()).append("\n");
        
        for (Class<?> serviceType : services.keySet()) {
            info.append("  - ").append(serviceType.getSimpleName()).append("\n");
        }
        
        return info.toString();
    }
}
