# FIN Application Modular Refactoring Plan

## Current Architecture Issues

### Problem Statement

The current `App.java` file (~1,280 lines) is a **monolithic class** that violates several SOLID principles and clean architecture patterns:

1. **Single Responsibility Principle (SRP) Violation**
   - Handles UI presentation logic
   - Manages business workflow coordination
   - Controls application state
   - Performs direct service orchestration

2. **Open/Closed Principle (OCP) Violation**
   - Adding new menu items requires modifying the main class
   - New workflows require changes to the core application logic

3. **High Coupling**
   - Tightly coupled to all service implementations
   - Direct dependency on Scanner for input handling
   - Mixed concerns of presentation and business logic

4. **Low Cohesion**
   - Methods handle vastly different responsibilities
   - UI formatting mixed with business logic
   - State management scattered throughout

5. **Testing Challenges**
   - Difficult to unit test individual components
   - Scanner dependencies make testing complex
   - Business logic intertwined with presentation

## Proposed Modular Architecture

### 1. Layered Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              ConsoleApplication                     │   │
│  │  • Application Bootstrap                            │   │
│  │  • Dependency Injection Setup                      │   │
│  │  • Main Method Only                                │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                 UI Package                          │   │
│  │  ┌─────────────┐ ┌──────────────┐ ┌──────────────┐ │   │
│  │  │ ConsoleMenu │ │ InputHandler │ │ OutputFormat │ │   │
│  │  │ • Display   │ │ • Validation │ │ • Pretty     │ │   │
│  │  │ • Navigate  │ │ • Type Conv. │ │   Print      │ │   │
│  │  └─────────────┘ └──────────────┘ └──────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                   CONTROLLER LAYER                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │            ApplicationController                    │   │
│  │  • Main Application Flow                           │   │
│  │  • Session State Management                        │   │
│  │  • Controller Coordination                         │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Domain Controllers                     │   │
│  │  ┌─────────────┐ ┌──────────────┐ ┌──────────────┐ │   │
│  │  │ Company     │ │ Import       │ │ Report       │ │   │
│  │  │ Controller  │ │ Controller   │ │ Controller   │ │   │
│  │  │ • Setup     │ │ • PDF & CSV  │ │ • Generation │ │   │
│  │  │ • Periods   │ │ • Validation │ │ • Export     │ │   │
│  │  └─────────────┘ └──────────────┘ └──────────────┘ │   │
│  │  ┌─────────────┐ ┌──────────────┐                  │   │
│  │  │ Data Mgmt   │ │ Verification │                  │   │
│  │  │ Controller  │ │ Controller   │                  │   │
│  │  │ • Entries   │ │ • Reconcile  │                  │   │
│  │  │ • Correct   │ │ • Validate   │                  │   │
│  │  └─────────────┘ └──────────────┘                  │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                   WORKFLOW LAYER                            │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Business Workflows                     │   │
│  │  ┌─────────────┐ ┌──────────────┐ ┌──────────────┐ │   │
│  │  │ Company     │ │ Import       │ │ Report Gen   │ │   │
│  │  │ Setup       │ │ Workflow     │ │ Workflow     │ │   │
│  │  │ Workflow    │ │ • Multi-step │ │ • Multi-     │ │   │
│  │  │ • Guided    │ │   Process    │ │   Format     │ │   │
│  │  │   Setup     │ │ • Validation │ │ • Batch      │ │   │
│  │  └─────────────┘ └──────────────┘ └──────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                               │
┌─────────────────────────────────────────────────────────────┐
│                     SERVICE LAYER                           │
│  │  [Existing Services - No Changes Required]              │
│  │  • CompanyService                                       │
│  │  • CsvImportService                                     │
│  │  • ReportService                                        │
│  │  • BankStatementProcessingService                       │
│  │  • DataManagementService                                │
│  │  • TransactionVerificationService                       │
└─────────────────────────────────────────────────────────────┘
```

### 2. Detailed Component Design

#### 2.1 Presentation Layer

##### ConsoleApplication.java (New Main Class)
```java
package fin;

public class ConsoleApplication {
    public static void main(String[] args) {
        // License validation
        if (!LicenseManager.checkLicenseCompliance()) {
            System.exit(1);
        }
        
        // Dependency injection setup
        ApplicationContext context = new ApplicationContext();
        
        // Start application
        ApplicationController controller = context.getApplicationController();
        controller.start();
    }
}
```

##### UI Package Components

**ConsoleMenu.java**
```java
package fin.ui;

public class ConsoleMenu {
    public void displayMainMenu();
    public void displayCompanyMenu();
    public void displayImportMenu();
    public void displayReportMenu();
    public void displayDataManagementMenu();
    
    public MenuChoice getUserChoice(MenuType menuType);
    public void displayHeader(String title);
    public void displayFooter();
}
```

**InputHandler.java**
```java
package fin.ui;

public class InputHandler {
    private final Scanner scanner;
    
    public String getString(String prompt);
    public String getString(String prompt, String defaultValue);
    public int getInteger(String prompt);
    public int getInteger(String prompt, int min, int max);
    public LocalDate getDate(String prompt);
    public boolean getBoolean(String prompt);
    public String getFilePath(String prompt, String extension);
    
    // Validation methods
    public boolean isValidFilePath(String path);
    public boolean isValidDate(String dateStr);
    public boolean isValidEmail(String email);
}
```

**OutputFormatter.java**
```java
package fin.ui;

public class OutputFormatter {
    public void printSuccess(String message);
    public void printError(String message);
    public void printWarning(String message);
    public void printInfo(String message);
    
    public void printTable(List<String[]> data, String[] headers);
    public void printReport(String reportContent);
    public void printSeparator();
    public void printProgress(String operation, int current, int total);
}
```

#### 2.2 Controller Layer

##### ApplicationController.java
```java
package fin.controller;

public class ApplicationController {
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    // Domain controllers
    private final CompanyController companyController;
    private final ImportController importController;
    private final ReportController reportController;
    private final DataManagementController dataManagementController;
    private final VerificationController verificationController;
    
    // Application state
    private ApplicationState state;
    
    public void start() {
        boolean exit = false;
        
        while (!exit) {
            MenuChoice choice = menu.getUserChoice(MenuType.MAIN);
            
            switch (choice) {
                case COMPANY_SETUP -> companyController.handleCompanySetup();
                case FISCAL_PERIOD -> companyController.handleFiscalPeriods();
                case IMPORT_BANK_STATEMENT -> importController.handleBankStatementImport();
                case IMPORT_CSV -> importController.handleCsvImport();
                case VIEW_DATA -> reportController.handleViewData();
                case GENERATE_REPORTS -> reportController.handleReportGeneration();
                case DATA_MANAGEMENT -> dataManagementController.handleDataManagement();
                case VERIFY_TRANSACTIONS -> verificationController.handleVerification();
                case EXIT -> exit = true;
            }
        }
    }
}
```

##### Domain Controllers

**CompanyController.java**
```java
package fin.controller;

public class CompanyController {
    private final CompanyService companyService;
    private final CompanySetupWorkflow companySetupWorkflow;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public void handleCompanySetup() {
        CompanySetupChoice choice = menu.getUserChoice(MenuType.COMPANY_SETUP);
        
        switch (choice) {
            case CREATE_COMPANY -> companySetupWorkflow.createNewCompany();
            case MANAGE_COMPANIES -> displayAndManageCompanies();
            case SELECT_COMPANY -> selectCurrentCompany();
        }
    }
    
    public void handleFiscalPeriods() {
        if (!hasCurrentCompany()) {
            outputFormatter.printError("Please select a company first");
            return;
        }
        
        FiscalPeriodChoice choice = menu.getUserChoice(MenuType.FISCAL_PERIOD);
        
        switch (choice) {
            case CREATE_PERIOD -> createFiscalPeriod();
            case MANAGE_PERIODS -> manageFiscalPeriods();
            case SELECT_PERIOD -> selectCurrentPeriod();
        }
    }
    
    private void createNewCompany() {
        // Delegate to workflow for complex multi-step process
        companySetupWorkflow.execute();
    }
}
```

**ImportController.java**
```java
package fin.controller;

public class ImportController {
    private final BankStatementProcessingService bankStatementService;
    private final CsvImportService csvImportService;
    private final ImportWorkflow importWorkflow;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public void handleBankStatementImport() {
        if (!hasRequiredContext()) {
            outputFormatter.printError("Please select company and fiscal period first");
            return;
        }
        
        String filePath = inputHandler.getFilePath("Enter PDF file path", ".pdf");
        
        importWorkflow.processBankStatement(filePath, getCurrentCompany(), getCurrentFiscalPeriod());
    }
    
    public void handleCsvImport() {
        if (!hasRequiredContext()) {
            outputFormatter.printError("Please select company and fiscal period first");
            return;
        }
        
        String filePath = inputHandler.getFilePath("Enter CSV file path", ".csv");
        
        importWorkflow.processCsvFile(filePath, getCurrentCompany(), getCurrentFiscalPeriod());
    }
}
```

**ReportController.java**
```java
package fin.controller;

public class ReportController {
    private final ReportService reportService;
    private final PdfExportService pdfExportService;
    private final ReportGenerationWorkflow reportWorkflow;
    private final ConsoleMenu menu;
    private final OutputFormatter outputFormatter;
    
    public void handleReportGeneration() {
        if (!hasCurrentFiscalPeriod()) {
            outputFormatter.printError("Please select a fiscal period first");
            return;
        }
        
        ReportChoice choice = menu.getUserChoice(MenuType.REPORTS);
        
        switch (choice) {
            case CASHBOOK -> generateCashbookReport();
            case GENERAL_LEDGER -> generateGeneralLedgerReport();
            case TRIAL_BALANCE -> generateTrialBalanceReport();
            case INCOME_STATEMENT -> generateIncomeStatementReport();
            case BALANCE_SHEET -> generateBalanceSheetReport();
            case ALL_REPORTS -> reportWorkflow.generateAllReports(getCurrentFiscalPeriod());
            case EXPORT_TO_PDF -> reportWorkflow.exportAllToPdf(getCurrentFiscalPeriod());
            case EXPORT_TO_EXCEL -> reportWorkflow.exportAllToExcel(getCurrentFiscalPeriod());
        }
    }
}
```

#### 2.3 Workflow Layer

**CompanySetupWorkflow.java**
```java
package fin.workflow;

public class CompanySetupWorkflow {
    private final CompanyService companyService;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public Company createNewCompany() {
        outputFormatter.printInfo("Creating new company...");
        
        // Step 1: Basic company information
        String name = inputHandler.getString("Company name");
        String registrationNumber = inputHandler.getString("Registration number (optional)", "");
        String taxNumber = inputHandler.getString("Tax number (optional)", "");
        
        // Step 2: Contact information
        String address = inputHandler.getString("Address (optional)", "");
        String email = inputHandler.getString("Contact email (optional)", "");
        String phone = inputHandler.getString("Contact phone (optional)", "");
        
        // Step 3: Create and save company
        Company company = Company.builder()
            .name(name)
            .registrationNumber(registrationNumber)
            .taxNumber(taxNumber)
            .address(address)
            .contactEmail(email)
            .contactPhone(phone)
            .build();
            
        Company savedCompany = companyService.createCompany(company);
        
        outputFormatter.printSuccess("Company created successfully: " + savedCompany.getName());
        
        // Step 4: Create initial fiscal period
        boolean createPeriod = inputHandler.getBoolean("Create initial fiscal period? (y/n)");
        if (createPeriod) {
            createInitialFiscalPeriod(savedCompany);
        }
        
        return savedCompany;
    }
    
    private void createInitialFiscalPeriod(Company company) {
        LocalDate startDate = inputHandler.getDate("Fiscal period start date (YYYY-MM-DD)");
        LocalDate endDate = inputHandler.getDate("Fiscal period end date (YYYY-MM-DD)");
        String periodName = inputHandler.getString("Period name", 
            String.format("FY%d-%d", startDate.getYear(), endDate.getYear()));
        
        FiscalPeriod period = FiscalPeriod.builder()
            .companyId(company.getId())
            .periodName(periodName)
            .startDate(startDate)
            .endDate(endDate)
            .isClosed(false)
            .build();
            
        companyService.createFiscalPeriod(period);
        outputFormatter.printSuccess("Fiscal period created: " + periodName);
    }
}
```

**ImportWorkflow.java**
```java
package fin.workflow;

public class ImportWorkflow {
    private final BankStatementProcessingService bankStatementService;
    private final CsvImportService csvImportService;
    private final OutputFormatter outputFormatter;
    
    public void processBankStatement(String filePath, Company company, FiscalPeriod fiscalPeriod) {
        try {
            outputFormatter.printInfo("Processing bank statement: " + filePath);
            
            // Step 1: Process PDF
            List<BankTransaction> transactions = bankStatementService.processStatementWithContext(
                filePath, company, fiscalPeriod);
            
            outputFormatter.printSuccess("Processed " + transactions.size() + " transactions");
            
            // Step 2: Display summary
            displayTransactionSummary(transactions);
            
            // Step 3: Optional CSV export
            boolean exportCsv = inputHandler.getBoolean("Export to CSV? (y/n)");
            if (exportCsv) {
                exportToCsv(transactions, company, fiscalPeriod);
            }
            
        } catch (Exception e) {
            outputFormatter.printError("Error processing bank statement: " + e.getMessage());
        }
    }
    
    public void processCsvFile(String filePath, Company company, FiscalPeriod fiscalPeriod) {
        try {
            outputFormatter.printInfo("Importing CSV file: " + filePath);
            
            // Step 1: Import and validate
            List<BankTransaction> transactions = csvImportService.importCsvFile(
                filePath, company.getId(), fiscalPeriod.getId());
            
            outputFormatter.printSuccess("Imported " + transactions.size() + " transactions");
            
            // Step 2: Display summary
            displayTransactionSummary(transactions);
            
        } catch (Exception e) {
            outputFormatter.printError("Error importing CSV: " + e.getMessage());
        }
    }
}
```

### 3. State Management

**ApplicationState.java**
```java
package fin.state;

public class ApplicationState {
    private Company currentCompany;
    private FiscalPeriod currentFiscalPeriod;
    private Map<String, Object> sessionData;
    
    // State management methods
    public void setCurrentCompany(Company company);
    public void setCurrentFiscalPeriod(FiscalPeriod fiscalPeriod);
    public boolean hasCurrentCompany();
    public boolean hasCurrentFiscalPeriod();
    
    // Session data management
    public void setSessionData(String key, Object value);
    public <T> T getSessionData(String key, Class<T> type);
    public void clearSession();
}
```

### 4. Dependency Injection

**ApplicationContext.java**
```java
package fin.context;

public class ApplicationContext {
    private final Map<Class<?>, Object> services = new HashMap<>();
    
    public ApplicationContext() {
        initializeServices();
        initializeControllers();
        initializeWorkflows();
    }
    
    private void initializeServices() {
        String dbUrl = DatabaseConfig.getDatabaseUrl();
        
        // Core services
        CompanyService companyService = new CompanyService(dbUrl);
        CsvImportService csvImportService = new CsvImportService(dbUrl, companyService);
        ReportService reportService = new ReportService(dbUrl, csvImportService);
        // ... other services
        
        register(CompanyService.class, companyService);
        register(CsvImportService.class, csvImportService);
        register(ReportService.class, reportService);
        // ... register other services
    }
    
    public <T> T get(Class<T> type) {
        return type.cast(services.get(type));
    }
    
    public ApplicationController getApplicationController() {
        return get(ApplicationController.class);
    }
}
```

### 5. Benefits of This Architecture

#### 5.1 SOLID Principles Compliance

1. **Single Responsibility**: Each class has one clear purpose
2. **Open/Closed**: Easy to add new menu items and workflows
3. **Liskov Substitution**: Controllers can be swapped/mocked
4. **Interface Segregation**: Focused interfaces for each concern
5. **Dependency Inversion**: Controllers depend on abstractions

#### 5.2 Clean Architecture Benefits

1. **Separation of Concerns**: UI, business logic, and data are separate
2. **Testability**: Each component can be unit tested independently
3. **Maintainability**: Changes in one layer don't affect others
4. **Flexibility**: Easy to replace console UI with web UI
5. **Reusability**: Controllers can be used by different interfaces

#### 5.3 Development Benefits

1. **Team Development**: Multiple developers can work on different controllers
2. **Code Review**: Smaller, focused classes are easier to review
3. **Debugging**: Issues are isolated to specific components
4. **Feature Addition**: New features follow established patterns
5. **Refactoring**: Easier to refactor individual components

### 6. Migration Strategy

#### Phase 1: Extract UI Components
1. Create `ConsoleMenu.java` with menu display logic
2. Create `InputHandler.java` with input validation
3. Create `OutputFormatter.java` with formatted output
4. Update `App.java` to use these components

#### Phase 2: Create Controller Layer
1. Create `ApplicationController.java` with main flow
2. Extract `CompanyController.java` from company-related methods
3. Extract `ImportController.java` from import-related methods
4. Extract `ReportController.java` from report-related methods

#### Phase 3: Implement Workflows
1. Create `CompanySetupWorkflow.java` for guided setup
2. Create `ImportWorkflow.java` for multi-step imports
3. Create `ReportGenerationWorkflow.java` for batch reporting

#### Phase 4: Add State Management
1. Create `ApplicationState.java` for session state
2. Create `ApplicationContext.java` for dependency injection
3. Update all controllers to use state management

#### Phase 5: Final Cleanup
1. Replace `App.java` with minimal `ConsoleApplication.java`
2. Remove all business logic from main class
3. Add comprehensive unit tests for all components

### 7. Testing Strategy

#### Unit Testing Structure
```
app/src/test/java/fin/
├── controller/
│   ├── ApplicationControllerTest.java
│   ├── CompanyControllerTest.java
│   ├── ImportControllerTest.java
│   └── ReportControllerTest.java
├── workflow/
│   ├── CompanySetupWorkflowTest.java
│   ├── ImportWorkflowTest.java
│   └── ReportGenerationWorkflowTest.java
├── ui/
│   ├── ConsoleMenuTest.java
│   ├── InputHandlerTest.java
│   └── OutputFormatterTest.java
└── state/
    └── ApplicationStateTest.java
```

#### Integration Testing
- Test complete workflows from UI to service layer
- Mock external dependencies (file system, database)
- Verify state management across controllers

### 8. Future Extensibility

#### Web Interface Support
The modular architecture supports adding a web interface:
```java
// Web controllers can reuse the same service layer
@RestController
public class CompanyWebController {
    private final CompanyController companyController;
    
    @PostMapping("/companies")
    public ResponseEntity<Company> createCompany(@RequestBody CompanyRequest request) {
        // Reuse existing controller logic
        return ResponseEntity.ok(companyController.createCompany(request.toCompany()));
    }
}
```

#### API Interface Support
Controllers can be adapted for REST API endpoints:
```java
public class CompanyApiController {
    private final CompanyService companyService;
    
    // Direct service usage for API responses
    public ApiResponse<Company> createCompany(CompanyApiRequest request) {
        Company company = companyService.createCompany(request.toEntity());
        return ApiResponse.success(company);
    }
}
```

### 9. Implementation Timeline

| Phase | Duration | Effort | Risk |
|-------|----------|--------|------|
| Phase 1: UI Components | 2-3 days | Medium | Low |
| Phase 2: Controller Layer | 3-4 days | High | Medium |
| Phase 3: Workflows | 2-3 days | Medium | Low |
| Phase 4: State Management | 1-2 days | Low | Low |
| Phase 5: Final Cleanup | 1 day | Low | Low |
| **Total** | **9-13 days** | **Medium** | **Low-Medium** |

### 10. Success Metrics

#### Code Quality Metrics
- **Cyclomatic Complexity**: Reduce from ~15-20 to ~3-5 per method
- **Class Size**: Reduce main class from 1,280 lines to <100 lines
- **Method Size**: Keep methods under 20 lines
- **Test Coverage**: Achieve 80%+ coverage for all new components

#### Maintainability Metrics
- **Time to Add Feature**: Reduce from 2-3 hours to 30-60 minutes
- **Time to Fix Bug**: Improve issue isolation and resolution
- **Code Review Time**: Reduce review time due to smaller, focused classes
- **Onboarding Time**: New developers can understand individual components

This refactoring plan provides a clear path to transform the monolithic `App.java` into a clean, modular architecture that follows SOLID principles and supports future extensibility.
