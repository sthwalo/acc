package fin.controller;

import fin.model.Company;
import fin.service.CompanyService;
import fin.state.ApplicationState;
import fin.ui.ConsoleMenu;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.util.List;

/**
 * Company management controller
 * Extracted from monolithic App.java handleCompanySetup() and related methods
 */
public class CompanyController {
    private final CompanyService companyService;
    private final ApplicationState applicationState;
    private final ConsoleMenu menu;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;
    
    public CompanyController(CompanyService companyService, ApplicationState applicationState,
                           ConsoleMenu menu, InputHandler inputHandler, OutputFormatter outputFormatter) {
        this.companyService = companyService;
        this.applicationState = applicationState;
        this.menu = menu;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }
    
    public void handleCompanySetup() {
        boolean back = false;
        while (!back) {
            menu.displayCompanyMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, 6);
            
            switch (choice) {
                case 1:
                    createCompany();
                    break;
                case 2:
                    selectCompany();
                    break;
                case 3:
                    viewCompanyDetails();
                    break;
                case 4:
                    editCompany();
                    break;
                case 5:
                    deleteCompany();
                    break;
                case 6:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice. Please try again.");
            }
        }
    }
    
    public void createCompany() {
        outputFormatter.printHeader("Create New Company");
        
        try {
            String name = inputHandler.getString("Enter company name");
            String regNumber = inputHandler.getString("Enter registration number (optional)", "");
            String taxNumber = inputHandler.getString("Enter tax number (optional)", "");
            String address = inputHandler.getString("Enter address (optional)", "");
            String email = inputHandler.getString("Enter contact email (optional)", "");
            String phone = inputHandler.getString("Enter contact phone (optional)", "");
            
            Company company = new Company(name);
            company.setRegistrationNumber(regNumber.isEmpty() ? null : regNumber);
            company.setTaxNumber(taxNumber.isEmpty() ? null : taxNumber);
            company.setAddress(address.isEmpty() ? null : address);
            company.setContactEmail(email.isEmpty() ? null : email);
            company.setContactPhone(phone.isEmpty() ? null : phone);
            
            company = companyService.createCompany(company);
            applicationState.setCurrentCompany(company);
            
            outputFormatter.printSuccess("Company created successfully!");
            outputFormatter.printInfo("Company ID: " + company.getId());
            outputFormatter.printInfo("Company Name: " + company.getName());
            
        } catch (Exception e) {
            outputFormatter.printError("Error creating company: " + e.getMessage());
        }
    }
    
    public void selectCompany() {
        List<Company> companies = companyService.getAllCompanies();
        
        if (companies.isEmpty()) {
            outputFormatter.printWarning("No companies found. Please create a company first.");
            return;
        }
        
        outputFormatter.printHeader("Select Company");
        for (int i = 0; i < companies.size(); i++) {
            System.out.println((i + 1) + ". " + companies.get(i).getName());
        }
        
        int selection = inputHandler.getInteger("Enter company number", 1, companies.size());
        applicationState.setCurrentCompany(companies.get(selection - 1));
        
        outputFormatter.printSuccess("Selected company: " + applicationState.getCurrentCompany().getName());
    }
    
    public void viewCompanyDetails() {
        if (!applicationState.hasCurrentCompany()) {
            outputFormatter.printError("No company selected. Please select a company first.");
            return;
        }
        
        outputFormatter.printCompanyDetails(applicationState.getCurrentCompany());
    }
    
    public void editCompany() {
        if (!applicationState.hasCurrentCompany()) {
            outputFormatter.printError("No company selected. Please select a company first.");
            return;
        }
        
        Company currentCompany = applicationState.getCurrentCompany();
        outputFormatter.printHeader("Edit Company Details");
        outputFormatter.printInfo("Current values shown in [brackets]. Press Enter to keep current value.");
        
        try {
            String name = inputHandler.getString("Company name", currentCompany.getName());
            String regNumber = inputHandler.getString("Registration number", 
                    currentCompany.getRegistrationNumber() != null ? currentCompany.getRegistrationNumber() : "");
            String taxNumber = inputHandler.getString("Tax number", 
                    currentCompany.getTaxNumber() != null ? currentCompany.getTaxNumber() : "");
            String address = inputHandler.getString("Address", 
                    currentCompany.getAddress() != null ? currentCompany.getAddress() : "");
            String email = inputHandler.getString("Contact email", 
                    currentCompany.getContactEmail() != null ? currentCompany.getContactEmail() : "");
            String phone = inputHandler.getString("Contact phone", 
                    currentCompany.getContactPhone() != null ? currentCompany.getContactPhone() : "");
            String logoPath = inputHandler.getString("Logo path (full file path to company logo)", 
                    currentCompany.getLogoPath() != null ? currentCompany.getLogoPath() : "");
            
            currentCompany.setName(name);
            currentCompany.setRegistrationNumber(regNumber.isEmpty() ? null : regNumber);
            currentCompany.setTaxNumber(taxNumber.isEmpty() ? null : taxNumber);
            currentCompany.setAddress(address.isEmpty() ? null : address);
            currentCompany.setContactEmail(email.isEmpty() ? null : email);
            currentCompany.setContactPhone(phone.isEmpty() ? null : phone);
            currentCompany.setLogoPath(logoPath.isEmpty() ? null : logoPath);
            
            currentCompany = companyService.updateCompany(currentCompany);
            applicationState.setCurrentCompany(currentCompany);
            
            outputFormatter.printSuccess("Company updated successfully!");
            
        } catch (Exception e) {
            outputFormatter.printError("Error updating company: " + e.getMessage());
        }
    }
    
    public void deleteCompany() {
        if (!applicationState.hasCurrentCompany()) {
            outputFormatter.printError("No company selected. Please select a company first.");
            return;
        }
        
        Company currentCompany = applicationState.getCurrentCompany();
        
        outputFormatter.printHeader("Delete Company");
        outputFormatter.printWarning("This will permanently delete the company and all associated data.");
        outputFormatter.printInfo("Company to delete: " + currentCompany.getName() + " (ID: " + currentCompany.getId() + ")");
        
        String confirmation = inputHandler.getConfirmation("Type 'DELETE' to confirm", "DELETE");
        
        if ("DELETE".equals(confirmation)) {
            try {
                boolean deleted = companyService.deleteCompany(currentCompany.getId());
                if (deleted) {
                    outputFormatter.printSuccess("Company deleted successfully!");
                    applicationState.setCurrentCompany(null);
                } else {
                    outputFormatter.printError("Failed to delete company. It may have already been deleted or doesn't exist.");
                }
            } catch (Exception e) {
                outputFormatter.printError("Error deleting company: " + e.getMessage());
                outputFormatter.printInfo("This could be due to existing data that depends on this company.");
            }
        } else {
            outputFormatter.printInfo("Deletion cancelled.");
        }
    }
}
