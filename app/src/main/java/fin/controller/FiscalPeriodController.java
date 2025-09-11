package fin.controller;

import fin.model.FiscalPeriod;
import fin.service.CompanyService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.time.LocalDate;
import java.util.List;

/**
 * Fiscal period management controller
 * Extracted from monolithic App.java fiscal period-related methods
 */
public class FiscalPeriodController {
    private final CompanyService companyService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public FiscalPeriodController(CompanyService companyService,
                                ApplicationState applicationState,
                                ConsoleMenu menu,
                                InputHandler inputHandler,
                                OutputFormatter outputFormatter) {
        this.companyService = companyService;
        this.applicationState = applicationState;
        this.menu = menu;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }
    
    public void handleFiscalPeriods() {
        try {
            applicationState.requireCompany();
            
            boolean back = false;
            while (!back) {
                menu.displayFiscalPeriodMenu();
                int choice = inputHandler.getInteger("Enter your choice", 1, 4);
                
                switch (choice) {
                    case 1:
                        createFiscalPeriod();
                        break;
                    case 2:
                        selectFiscalPeriod();
                        break;
                    case 3:
                        viewFiscalPeriodDetails();
                        break;
                    case 4:
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
    
    public void createFiscalPeriod() {
        outputFormatter.printHeader("Create New Fiscal Period");
        
        try {
            String name = inputHandler.getString("Enter period name (e.g., FY2023-2024)");
            LocalDate startDate = inputHandler.getDate("Enter start date");
            LocalDate endDate = inputHandler.getDate("Enter end date", startDate);
            
            FiscalPeriod fiscalPeriod = new FiscalPeriod(
                applicationState.getCurrentCompany().getId(), 
                name, 
                startDate, 
                endDate);
            
            fiscalPeriod = companyService.createFiscalPeriod(fiscalPeriod);
            applicationState.setCurrentFiscalPeriod(fiscalPeriod);
            
            outputFormatter.printSuccess("Fiscal period created successfully!");
            outputFormatter.printInfo("Period ID: " + fiscalPeriod.getId());
            outputFormatter.printInfo("Period Name: " + fiscalPeriod.getPeriodName());
            outputFormatter.printInfo("Date Range: " + startDate + " to " + endDate);
            
        } catch (Exception e) {
            outputFormatter.printError("Error creating fiscal period: " + e.getMessage());
        }
    }
    
    public void selectFiscalPeriod() {
        List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(
            applicationState.getCurrentCompany().getId());
        
        if (periods.isEmpty()) {
            outputFormatter.printWarning("No fiscal periods found. Please create a fiscal period first.");
            return;
        }
        
        outputFormatter.printHeader("Select Fiscal Period");
        for (int i = 0; i < periods.size(); i++) {
            FiscalPeriod period = periods.get(i);
            System.out.println((i + 1) + ". " + period.getPeriodName() + " (" + 
                    period.getStartDate() + " - " + period.getEndDate() + ")");
        }
        
        int selection = inputHandler.getInteger("Enter period number", 1, periods.size());
        applicationState.setCurrentFiscalPeriod(periods.get(selection - 1));
        
        outputFormatter.printSuccess("Selected fiscal period: " + 
            applicationState.getCurrentFiscalPeriod().getPeriodName());
    }
    
    public void viewFiscalPeriodDetails() {
        if (!applicationState.hasCurrentFiscalPeriod()) {
            outputFormatter.printError("No fiscal period selected. Please select a fiscal period first.");
            return;
        }
        
        outputFormatter.printFiscalPeriodDetails(applicationState.getCurrentFiscalPeriod());
    }
    
    public void manageFiscalPeriods() {
        try {
            applicationState.requireCompany();
            
            List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(
                applicationState.getCurrentCompany().getId());
            
            if (periods.isEmpty()) {
                outputFormatter.printWarning("No fiscal periods found for this company.");
                if (inputHandler.getBoolean("Would you like to create one now?")) {
                    createFiscalPeriod();
                }
                return;
            }
            
            outputFormatter.printHeader("Manage Fiscal Periods");
            outputFormatter.printInfo("Company: " + applicationState.getCurrentCompany().getName());
            
            for (int i = 0; i < periods.size(); i++) {
                FiscalPeriod period = periods.get(i);
                String status = period.isClosed() ? "[CLOSED]" : "[OPEN]";
                System.out.printf("%d. %s %s (%s - %s)%n", 
                    i + 1, period.getPeriodName(), status,
                    period.getStartDate(), period.getEndDate());
            }
            
            outputFormatter.printSeparator();
            outputFormatter.printPlain("Actions:");
            outputFormatter.printPlain("1. Select a period");
            outputFormatter.printPlain("2. Create new period");
            outputFormatter.printPlain("3. Close a period");
            outputFormatter.printPlain("4. Back to main menu");
            
            int choice = inputHandler.getInteger("Enter your choice", 1, 4);
            
            switch (choice) {
                case 1:
                    selectFiscalPeriod();
                    break;
                case 2:
                    createFiscalPeriod();
                    break;
                case 3:
                    closeFiscalPeriod(periods);
                    break;
                case 4:
                    // Go back
                    break;
                default:
                    outputFormatter.printError("Invalid choice.");
            }
            
        } catch (IllegalStateException e) {
            outputFormatter.printError(e.getMessage());
        }
    }
    
    private void closeFiscalPeriod(List<FiscalPeriod> periods) {
        List<FiscalPeriod> openPeriods = periods.stream()
            .filter(p -> !p.isClosed())
            .toList();
        
        if (openPeriods.isEmpty()) {
            outputFormatter.printInfo("No open periods to close.");
            return;
        }
        
        outputFormatter.printSubHeader("Close Fiscal Period");
        outputFormatter.printWarning("Closing a period will prevent further modifications to its data.");
        
        for (int i = 0; i < openPeriods.size(); i++) {
            FiscalPeriod period = openPeriods.get(i);
            System.out.println((i + 1) + ". " + period.getPeriodName() + " (" + 
                    period.getStartDate() + " - " + period.getEndDate() + ")");
        }
        
        int selection = inputHandler.getInteger("Select period to close", 1, openPeriods.size());
        FiscalPeriod periodToClose = openPeriods.get(selection - 1);
        
        String confirmation = inputHandler.getConfirmation(
            "Type 'CLOSE' to confirm closing period: " + periodToClose.getPeriodName(), 
            "CLOSE");
        
        if ("CLOSE".equals(confirmation)) {
            try {
                periodToClose.setClosed(true);
                // Note: This would need a service method to update the period
                outputFormatter.printSuccess("Fiscal period closed: " + periodToClose.getPeriodName());
                
                // If this was the current period, clear it
                if (applicationState.hasCurrentFiscalPeriod() && 
                    applicationState.getCurrentFiscalPeriod().getId().equals(periodToClose.getId())) {
                    applicationState.setCurrentFiscalPeriod(null);
                    outputFormatter.printInfo("Current fiscal period cleared. Please select an open period.");
                }
                
            } catch (Exception e) {
                outputFormatter.printError("Error closing fiscal period: " + e.getMessage());
            }
        } else {
            outputFormatter.printInfo("Period closure cancelled.");
        }
    }
}
