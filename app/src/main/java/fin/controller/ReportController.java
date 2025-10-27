package fin.controller;

import fin.service.FinancialReportingService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.io.File;

/**
 * Report generation controller
 * Extracted from monolithic App.java report-related methods
 */
public class ReportController {
    private final FinancialReportingService financialReportingService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    private final String reportsDir;

    // Controller Constants
    private static final int MAX_REPORT_MENU_CHOICE = 8;
    private static final int REPORT_MENU_BACK_CHOICE = 8;
    private static final int MAX_CUSTOM_REPORT_CHOICE = 4;
    private static final int CUSTOM_REPORT_BACK_CHOICE = 4;
    private static final int REPORT_TYPE_CASHBOOK_INDEX = 0;
    private static final int REPORT_TYPE_GENERAL_LEDGER_INDEX = 1;
    private static final int REPORT_TYPE_TRIAL_BALANCE_INDEX = 2;
    private static final int REPORT_TYPE_INCOME_STATEMENT_INDEX = 3;
    private static final int REPORT_TYPE_BALANCE_SHEET_INDEX = 4;
    private static final int REPORT_TYPE_AUDIT_TRAIL_INDEX = 5;
    private static final int REPORT_TYPE_CASH_FLOW_INDEX = 6;
    // Menu choice constants
    private static final int REPORT_MENU_CASHBOOK = 1;
    private static final int REPORT_MENU_GENERAL_LEDGER = 2;
    private static final int REPORT_MENU_TRIAL_BALANCE = 3;
    private static final int REPORT_MENU_INCOME_STATEMENT = 4;
    private static final int REPORT_MENU_BALANCE_SHEET = 5;
    private static final int REPORT_MENU_AUDIT_TRAIL = 6;
    private static final int REPORT_MENU_CASH_FLOW = 7;
    private static final int CUSTOM_REPORT_DATE_RANGE = 1;
    private static final int CUSTOM_REPORT_ACCOUNT_SPECIFIC = 2;
    private static final int CUSTOM_REPORT_TRANSACTION_TYPE = 3;
    
    /**
     * Constructor with dependency injection.
     *
     * NOTE: EI_EXPOSE_REP warning is intentionally suppressed for this constructor.
     * This is an architectural design decision for Dependency Injection pattern:
     * - Services and UI components are injected as constructor parameters to enable loose coupling
     * - Allows for better testability through mock injection
     * - Enables separation between business logic, UI, and application state
     * - Maintains single responsibility principle in MVC architecture
     * - Suppressions are configured in config/spotbugs/exclude.xml for all controller constructors
     *
     * @param initialFinancialReportingService the service for generating financial reports
     * @param initialApplicationState the application state manager
     * @param initialMenu the console menu for user interaction
     * @param initialInputHandler the input handler for user input
     * @param initialOutputFormatter the output formatter for display formatting
     */
    public ReportController(FinancialReportingService initialFinancialReportingService,
                          ApplicationState initialApplicationState,
                          ConsoleMenu initialMenu,
                          InputHandler initialInputHandler,
                          OutputFormatter initialOutputFormatter) {
        this.financialReportingService = initialFinancialReportingService;
        this.applicationState = initialApplicationState;
        this.menu = initialMenu;
        this.inputHandler = initialInputHandler;
        this.outputFormatter = initialOutputFormatter;
        this.reportsDir = "/Users/sthwalonyoni/FIN/reports";
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize the report generation menu flow.
     * When overriding, ensure that:
     * - Application state context is validated before proceeding with report operations
     * - User input is properly validated and sanitized
     * - Error handling is maintained for invalid choices and system errors
     * - The menu loop respects user navigation choices (back/exit)
     * - All report generation calls maintain proper error handling and user feedback
     */
    public void handleReportGeneration() {
        try {
            applicationState.requireContext();
            
            boolean back = false;
            while (!back) {
                menu.displayReportMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, MAX_REPORT_MENU_CHOICE);
                
                switch (choice) {
                    case REPORT_MENU_CASHBOOK:
                        generateCashbookReport();
                        break;
                    case REPORT_MENU_GENERAL_LEDGER:
                        generateGeneralLedgerReport();
                        break;
                    case REPORT_MENU_TRIAL_BALANCE:
                        generateTrialBalanceReport();
                        break;
                    case REPORT_MENU_INCOME_STATEMENT:
                        generateIncomeStatementReport();
                        break;
                    case REPORT_MENU_BALANCE_SHEET:
                        generateBalanceSheetReport();
                        break;
                    case REPORT_MENU_AUDIT_TRAIL:
                        generateAuditTrailReport();
                        break;
                    case REPORT_MENU_CASH_FLOW:
                        generateCashFlowReport();
                        break;
                    case REPORT_MENU_BACK_CHOICE:
                        back = true;
                        break;
                    default:
                        outputFormatter.printError("Invalid choice. Please try again.");
                }
            }
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize cashbook report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateCashbookReport() {
        outputFormatter.printHeader("Generating Cashbook Report");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating cashbook report...");
            financialReportingService.generateCashbook(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Cashbook Report generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Cashbook Report: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize general ledger report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateGeneralLedgerReport() {
        outputFormatter.printHeader("Generating General Ledger Report");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating general ledger report...");
            financialReportingService.generateGeneralLedger(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("General Ledger Report generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating General Ledger Report: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize trial balance report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateTrialBalanceReport() {
        outputFormatter.printHeader("Generating Trial Balance Report");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating trial balance report...");
            financialReportingService.generateTrialBalance(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Trial Balance Report generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Trial Balance Report: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize income statement report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateIncomeStatementReport() {
        outputFormatter.printHeader("Generating Income Statement");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating income statement...");
            financialReportingService.generateIncomeStatement(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Income Statement generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Income Statement: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize balance sheet report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateBalanceSheetReport() {
        outputFormatter.printHeader("Generating Balance Sheet");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating balance sheet...");
            financialReportingService.generateBalanceSheet(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Balance Sheet generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Balance Sheet: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize cash flow report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateCashFlowReport() {
        outputFormatter.printHeader("Generating Cash Flow Statement");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating cash flow statement...");
            financialReportingService.generateCashFlowStatement(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Cash Flow Statement generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Cash Flow Statement: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize audit trail report generation.
     * When overriding, ensure that:
     * - Reports directory existence is verified before file operations
     * - Company and fiscal period context is properly validated
     * - Error handling is maintained for file system and service failures
     * - User feedback is provided for both success and failure scenarios
     * - File location information is displayed to the user after successful generation
     */
    public void generateAuditTrailReport() {
        outputFormatter.printHeader("Generating Audit Trail");
        
        try {
            ensureReportsDirectoryExists();
            
            outputFormatter.printProcessing("Generating audit trail...");
            financialReportingService.generateAuditTrail(
                applicationState.getCurrentCompany().getId(), 
                applicationState.getCurrentFiscalPeriod().getId(), 
                true);
            
            outputFormatter.printSuccess("Audit Trail generated successfully");
            outputFormatter.printFileLocation("Report location", reportsDir);
            
        } catch (Exception e) {
            outputFormatter.printError("Error generating Audit Trail: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize batch report generation.
     * When overriding, ensure that:
     * - Application state context is validated before batch operations
     * - Reports directory existence is verified before file operations
     * - User confirmation is obtained before proceeding with batch generation
     * - Individual report failures don't stop the entire batch process
     * - Progress feedback is provided during batch processing
     * - Success/failure counts are tracked and reported to the user
     * - Error handling is maintained for both individual report failures and system errors
     * - File location information is displayed after batch completion
     */
    public void generateAllReports() {
        outputFormatter.printHeader("Generating All Financial Reports");
        
        try {
            applicationState.requireContext();
            ensureReportsDirectoryExists();
            
            outputFormatter.printInfo("This will generate all available financial reports");
            if (!inputHandler.getBoolean("Continue with batch report generation?")) {
                outputFormatter.printInfo("Batch report generation cancelled");
                return;
            }
            
            String[] reportTypes = {
                "Cashbook", "General Ledger", "Trial Balance", 
                "Income Statement", "Balance Sheet", "Audit Trail", "Cash Flow Statement"
            };
            
            int successCount = 0;
            
            for (int i = 0; i < reportTypes.length; i++) {
                try {
                    outputFormatter.printProgress("Generating reports", i + 1, reportTypes.length);
                    
                    switch (i) {
                        case REPORT_TYPE_CASHBOOK_INDEX: financialReportingService.generateCashbook(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_GENERAL_LEDGER_INDEX: financialReportingService.generateGeneralLedger(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_TRIAL_BALANCE_INDEX: financialReportingService.generateTrialBalance(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_INCOME_STATEMENT_INDEX: financialReportingService.generateIncomeStatement(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_BALANCE_SHEET_INDEX: financialReportingService.generateBalanceSheet(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_AUDIT_TRAIL_INDEX: financialReportingService.generateAuditTrail(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        case REPORT_TYPE_CASH_FLOW_INDEX: financialReportingService.generateCashFlowStatement(
                                applicationState.getCurrentCompany().getId(), 
                                applicationState.getCurrentFiscalPeriod().getId(), true); break;
                        default:
                            outputFormatter.printError("Unknown report type index: " + i);
                            break;
                    }
                    successCount++;
                } catch (Exception e) {
                    outputFormatter.printError("Failed to generate " + reportTypes[i] + ": " + e.getMessage());
                }
            }
            
            outputFormatter.printSuccess("Generated " + successCount + " out of " + reportTypes.length + " reports");
            outputFormatter.printFileLocation("All reports saved to", reportsDir);
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        } catch (Exception e) {
            outputFormatter.printError("Error during batch report generation: " + e.getMessage());
        }
    }
    
    /**
     * This method is designed for extension.
     * Subclasses can override this method to customize custom report generation menu and options.
     * When overriding, ensure that:
     * - Application state context is validated before custom report operations
     * - Available custom report options are clearly presented to the user
     * - User input is properly validated and sanitized
     * - Error handling is maintained for invalid choices and system errors
     * - Delegation to specific report generation methods maintains proper error handling
     * - Menu navigation (back/exit) is properly handled
     */
    public void generateCustomReport() {
        outputFormatter.printHeader("Generate Custom Report");
        
        try {
            applicationState.requireContext();
            
            outputFormatter.printPlain("Available custom report options:");
            outputFormatter.printPlain("1. Date range report");
            outputFormatter.printPlain("2. Account-specific report");
            outputFormatter.printPlain("3. Transaction type report");
            outputFormatter.printPlain("4. Back to reports menu");
            
            int choice = inputHandler.getInteger("Select custom report type", 1, MAX_CUSTOM_REPORT_CHOICE);
            
            switch (choice) {
                case CUSTOM_REPORT_DATE_RANGE:
                    generateDateRangeReport();
                    break;
                case CUSTOM_REPORT_ACCOUNT_SPECIFIC:
                    generateAccountSpecificReport();
                    break;
                case CUSTOM_REPORT_TRANSACTION_TYPE:
                    generateTransactionTypeReport();
                    break;
                case CUSTOM_REPORT_BACK_CHOICE:
                    // Go back
                    break;
                default:
                    outputFormatter.printError("Invalid choice");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    private void generateDateRangeReport() {
        outputFormatter.printSubHeader("Date Range Report");
        outputFormatter.printInfo("Generate reports for a specific date range within the fiscal period");
        
        // This would need additional service methods to support date range filtering
        outputFormatter.printInfo("Date range reporting functionality would be implemented here");
    }
    
    private void generateAccountSpecificReport() {
        outputFormatter.printSubHeader("Account-Specific Report");
        outputFormatter.printInfo("Generate reports for specific accounts");
        
        // This would need account selection and filtering
        outputFormatter.printInfo("Account-specific reporting functionality would be implemented here");
    }
    
    private void generateTransactionTypeReport() {
        outputFormatter.printSubHeader("Transaction Type Report");
        outputFormatter.printInfo("Generate reports filtered by transaction type");
        
        // This would need transaction type filtering
        outputFormatter.printInfo("Transaction type reporting functionality would be implemented here");
    }
    
    private boolean ensureReportsDirectoryExists() {
        File directory = new File(reportsDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                outputFormatter.printInfo("Created reports directory: " + reportsDir);
                return true;
            } else {
                outputFormatter.printError("Failed to create reports directory: " + reportsDir);
                return false;
            }
        }
        
        if (!directory.canWrite()) {
            outputFormatter.printError("Reports directory is not writable: " + reportsDir);
            return false;
        }
        
        return true;
    }
}
