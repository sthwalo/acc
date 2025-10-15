package fin.context;

import fin.config.DatabaseConfig;
import fin.controller.*;
import fin.repository.CompanyRepository;
import fin.service.*;
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
    
    public ApplicationContext() {
        // Check if we're in test mode and use test database URL
        String testDbUrl = System.getProperty("fin.database.test.url");
        if (testDbUrl != null && !testDbUrl.isEmpty()) {
            this.dbUrl = testDbUrl;
            System.out.println("🧪 Using test database: " + testDbUrl);
        } else {
            // Test database connection first
            if (!DatabaseConfig.testConnection()) {
                throw new RuntimeException("Failed to connect to database");
            }
            
            // Use URL with embedded credentials for compatibility with legacy code
            // that calls DriverManager.getConnection(url) directly
            this.dbUrl = DatabaseConfig.getDatabaseUrlWithCredentials();
            System.out.println("🔌 Application connected to: " + DatabaseConfig.getDatabaseType());
        }
        
        initializeServices();
        initializeUIComponents();
        initializeControllers();
        initializeApplicationController();
        
        System.out.println("✅ Application context initialized successfully");
    }
    
    /**
     * Initialize core business services
     * Replaces service instantiation from App.java constructor
     */
    private void initializeServices() {
        // Core services
        CompanyService companyService = new CompanyService(dbUrl);
        register(CompanyService.class, companyService);
        
        CsvImportService csvImportService = new CsvImportService(dbUrl, companyService);
        register(CsvImportService.class, csvImportService);
        
        ReportService reportService = new ReportService(dbUrl, csvImportService);
        register(ReportService.class, reportService);
        
        FinancialReportingService financialReportingService = new FinancialReportingService(dbUrl);
        register(FinancialReportingService.class, financialReportingService);
        
        PdfExportService pdfExportService = new PdfExportService();
        register(PdfExportService.class, pdfExportService);
        
        DataManagementService dataManagementService = new DataManagementService(
            dbUrl, companyService, csvImportService.getAccountService());
        register(DataManagementService.class, dataManagementService);
        
        BankStatementProcessingService bankStatementService = new BankStatementProcessingService(dbUrl);
        register(BankStatementProcessingService.class, bankStatementService);
        
        InteractiveClassificationService interactiveClassificationService = new InteractiveClassificationService();
        register(InteractiveClassificationService.class, interactiveClassificationService);
        
        // Transaction Classification Service (unified entry point)
        // Create specialized services needed for TransactionClassificationService
        CategoryManagementService categoryManagementService = new CategoryManagementService(dbUrl);
        register(CategoryManagementService.class, categoryManagementService);
        
        AccountManagementService accountManagementService = new AccountManagementService(dbUrl);
        register(AccountManagementService.class, accountManagementService);
        
        // CONSOLIDATION: Replace TransactionMappingRuleService with ClassificationRuleManager
        // ClassificationRuleManager handles both standard and company-specific learned rules
        ClassificationRuleManager classificationRuleManager = new ClassificationRuleManager();
        register(ClassificationRuleManager.class, classificationRuleManager);
        
        // Phase 4: ChartOfAccountsService removed - AccountClassificationService is single source of truth
        // AccountService now uses AccountClassificationService directly
        AccountClassificationService accountClassificationService = new AccountClassificationService(dbUrl);
        register(AccountClassificationService.class, accountClassificationService);
        
        // REMOVED: TransactionMappingService - consolidated into AccountClassificationService
        
        // NOTE: TransactionClassificationService uses AccountClassificationService as single source of truth
        TransactionClassificationService transactionClassificationService = new TransactionClassificationService(
            dbUrl,
            classificationRuleManager,  // Updated to use ClassificationRuleManager instead of TransactionMappingRuleService
            interactiveClassificationService
        );
        register(TransactionClassificationService.class, transactionClassificationService);
        
        CsvExportService csvExportService = new CsvExportService(companyService);
        register(CsvExportService.class, csvExportService);
        
        // Payroll service dependencies
        CompanyRepository companyRepository = new CompanyRepository(dbUrl);
        register(CompanyRepository.class, companyRepository);
        
        PayslipPdfService payslipPdfService = new PayslipPdfService(companyRepository);
        register(PayslipPdfService.class, payslipPdfService);
        
        // Payroll service
        PayrollService payrollService = new PayrollService(dbUrl, companyRepository, payslipPdfService, null);
        register(PayrollService.class, payrollService);
        
        // Payroll report service
        PayrollReportService payrollReportService = new PayrollReportService(dbUrl);
        register(PayrollReportService.class, payrollReportService);
        
        // Opening Balance service
        OpeningBalanceService openingBalanceService = new OpeningBalanceService(dbUrl);
        register(OpeningBalanceService.class, openingBalanceService);
        
        // Financial Data Repository (needed by GL and TB services)
        // Create DataSource for repository (same pattern as FinancialReportingService)
        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
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
        
        System.out.println("📦 Core services initialized");
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
        // Get dependencies
        ConsoleMenu menu = get(ConsoleMenu.class);
        InputHandler inputHandler = get(InputHandler.class);
        OutputFormatter outputFormatter = get(OutputFormatter.class);
        ApplicationState applicationState = get(ApplicationState.class);
        
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
        
        // Report controller
        ReportController reportController = new ReportController(
            get(FinancialReportingService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(ReportController.class, reportController);
        
        // Data management controller
        DataManagementController dataManagementController = new DataManagementController(
            get(DataManagementService.class),
            get(TransactionClassificationService.class),
            get(CsvExportService.class),
            get(CsvImportService.class),
            applicationState,
            menu,
            inputHandler,
            outputFormatter
        );
        register(DataManagementController.class, dataManagementController);
        
        // Payroll controller
        PayrollController payrollController = new PayrollController(
            get(PayrollService.class),
            get(PayrollReportService.class),  // <-- Add this missing parameter
            inputHandler,
            outputFormatter
        );
        register(PayrollController.class, payrollController);
        
        System.out.println("🎮 Domain controllers initialized");
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
            get(PayrollController.class)
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
