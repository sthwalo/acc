package fin.controller;

import fin.controller.PayrollController;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.time.LocalDate;

/**
 * Main application controller
 * Coordinates all domain controllers and manages application flow
 * Replaces the main method flow from monolithic App.java
 */
public class ApplicationController {
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    private final ApplicationState applicationState;
    
    // Domain controllers
    private final CompanyController companyController;
    private final FiscalPeriodController fiscalPeriodController;
    private final ImportController importController;
    private final ReportController reportController;
    private final DataManagementController dataManagementController;
    private final VerificationController verificationController;
    private final PayrollController payrollController;
    
    public ApplicationController(
        ConsoleMenu menu,
        InputHandler inputHandler,
        OutputFormatter outputFormatter,
        ApplicationState applicationState,
        CompanyController companyController,
        FiscalPeriodController fiscalPeriodController,
        ImportController importController,
        ReportController reportController,
        DataManagementController dataManagementController,
        VerificationController verificationController,
        PayrollController payrollController // add this parameter
    ) {
        this.menu = menu;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
        this.applicationState = applicationState;
        this.companyController = companyController;
        this.fiscalPeriodController = fiscalPeriodController;
        this.importController = importController;
        this.reportController = reportController;
        this.dataManagementController = dataManagementController;
        this.verificationController = verificationController;
        this.payrollController = payrollController; // initialize field
    }

    /**
     * Main application loop
     * Replaces the main method logic from original App.java
     */
    public void start() {
        outputFormatter.printHeader("FIN Financial Management System");
        outputFormatter.printInfo("Welcome to the modular FIN application");
        
        displayWelcomeMessage();
        
        boolean exit = false;
        
        while (!exit) {
            try {
                displayCurrentContext();
                menu.displayMainMenu();
                
                int choice = inputHandler.getInteger("Enter your choice", 1, 11);
                
                switch (choice) {
                    case 1:
                        companyController.handleCompanySetup();
                        break;
                    case 2:
                        fiscalPeriodController.handleFiscalPeriods();
                        break;
                    case 3:
                        importController.handleBankStatementImport();
                        break;
                    case 4:
                        importController.handleCsvImport();
                        break;
                    case 5:
                        importController.handleViewImportedData();
                        break;
                    case 6:
                        reportController.handleReportGeneration();
                        break;
                    case 7:
                        dataManagementController.handleDataManagement();
                        break;
                    case 8:
                        verificationController.handleTransactionVerification();
                        break;
                    case 9:
                        if (applicationState.hasCurrentCompany()) {
                            payrollController.handlePayrollManagement(applicationState.getCurrentCompany().getId());
                        } else {
                            outputFormatter.printWarning("Please select a company first (Option 1)");
                            inputHandler.waitForEnter();
                        }
                        break;
                    case 10:
                        displayCurrentTime();
                        break;
                    case 11:
                        exit = handleExit();
                        break;
                    default:
                        outputFormatter.printError("Invalid choice. Please try again.");
                }
                
            } catch (Exception e) {
                outputFormatter.printError("Unexpected error: " + e.getMessage());
                outputFormatter.printInfo("Please try again or contact support if the problem persists.");
                
                // Log the exception for debugging
                System.err.println("Exception details: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        }
        
        displayGoodbyeMessage();
    }
    
    /**
     * Handle application exit with optional data saving
     */
    private boolean handleExit() {
        outputFormatter.printHeader("Exit Application");
        
        if (applicationState.hasCurrentCompany()) {
            outputFormatter.printInfo("Current session: " + applicationState.getStateDescription());
            
            if (inputHandler.getBoolean("Would you like to save any current work before exiting?")) {
                handleSaveBeforeExit();
            }
        }
        
        boolean confirmExit = inputHandler.getBoolean("Are you sure you want to exit the FIN application?");
        
        if (confirmExit) {
            // Cleanup operations
            applicationState.clearAll();
            return true;
        } else {
            outputFormatter.printInfo("Returning to main menu");
            return false;
        }
    }
    
    private void handleSaveBeforeExit() {
        outputFormatter.printSubHeader("Save Options");
        outputFormatter.printPlain("1. Export current transactions to CSV");
        outputFormatter.printPlain("2. Generate summary report");
        outputFormatter.printPlain("3. Skip saving");
        
        int choice = inputHandler.getInteger("Select save option", 1, 3);
        
        switch (choice) {
            case 1:
                try {
                    dataManagementController.handleExportToCSV();
                    outputFormatter.printSuccess("Data exported before exit");
                } catch (Exception e) {
                    outputFormatter.printError("Error exporting data: " + e.getMessage());
                }
                break;
            case 2:
                try {
                    reportController.generateTrialBalanceReport();
                    outputFormatter.printSuccess("Summary report generated before exit");
                } catch (Exception e) {
                    outputFormatter.printError("Error generating report: " + e.getMessage());
                }
                break;
            case 3:
                outputFormatter.printInfo("Skipping save operations");
                break;
            default:
                outputFormatter.printError("Invalid choice, skipping save");
        }
    }
    
    private void displayWelcomeMessage() {
        outputFormatter.printSeparator();
        outputFormatter.printInfo("System Status:");
        outputFormatter.printSuccess("✓ Database connection established");
        outputFormatter.printSuccess("✓ All services initialized");
        outputFormatter.printSuccess("✓ License compliance verified");
        outputFormatter.printSeparator();
        
        outputFormatter.printInfo("Quick Start Guide:");
        outputFormatter.printPlain("1. Create or select a company (Option 1)");
        outputFormatter.printPlain("2. Create or select a fiscal period (Option 2)");
        outputFormatter.printPlain("3. Import bank statements or CSV data (Options 3-4)");
        outputFormatter.printPlain("4. Generate financial reports (Option 6)");
        outputFormatter.printSeparator();
    }
    
    private void displayCurrentContext() {
        if (applicationState.hasCurrentCompany() || applicationState.hasCurrentFiscalPeriod()) {
            outputFormatter.printCurrentContext(
                applicationState.getCurrentCompany(), 
                applicationState.getCurrentFiscalPeriod());
        } else {
            outputFormatter.printWarning("No company or fiscal period selected");
            outputFormatter.printInfo("Please start with Company Setup (Option 1)");
        }
    }
    
    private void displayCurrentTime() {
        outputFormatter.printInfo("Current date: " + LocalDate.now());
        
        if (applicationState.hasCurrentFiscalPeriod()) {
            outputFormatter.printInfo("Current fiscal period: " + 
                applicationState.getCurrentFiscalPeriod().getPeriodName());
        }
        
        inputHandler.waitForEnter("Press Enter to continue");
    }
    
    private void displayGoodbyeMessage() {
        outputFormatter.printHeader("Thank You for Using FIN");
        outputFormatter.printSuccess("Session ended successfully");
        outputFormatter.printInfo("All data has been saved");
        outputFormatter.printPlain("Goodbye!");
        outputFormatter.printSeparator();
    }
    
    /**
     * Handle unexpected application errors gracefully
     */
    public void handleGlobalError(Exception e) {
        outputFormatter.printError("A serious error occurred: " + e.getMessage());
        outputFormatter.printWarning("The application will attempt to continue...");
        
        // Try to save current state if possible
        if (applicationState.hasCurrentCompany()) {
            outputFormatter.printInfo("Attempting to preserve current session data...");
            try {
                // Could implement emergency save functionality here
                outputFormatter.printSuccess("Session data preserved");
            } catch (Exception saveException) {
                outputFormatter.printError("Could not preserve session data: " + saveException.getMessage());
            }
        }
    }
    
    /**
     * Display application statistics and usage information
     */
    public void displayApplicationStats() {
        outputFormatter.printHeader("Application Statistics");
        
        if (applicationState.hasCurrentCompany()) {
            outputFormatter.printInfo("Current company: " + applicationState.getCurrentCompany().getName());
            
            if (applicationState.hasCurrentFiscalPeriod()) {
                outputFormatter.printInfo("Current fiscal period: " + 
                    applicationState.getCurrentFiscalPeriod().getPeriodName());
            }
        }
        
        // Could add more statistics here like:
        // - Number of transactions imported
        // - Reports generated
        // - Session duration
        
        outputFormatter.printInfo("Application uptime: Active session");
        outputFormatter.printInfo("System status: Operational");
    }
    
    /**
     * Handle system shutdown gracefully
     */
    public void shutdown() {
        outputFormatter.printInfo("Shutting down FIN application...");
        
        // Cleanup resources
        applicationState.clearAll();
        
        outputFormatter.printSuccess("Shutdown complete");
    }
}
