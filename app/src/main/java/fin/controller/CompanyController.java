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
    
    // Menu option constants
    private static final int MENU_OPTION_CREATE_COMPANY = 1;
    private static final int MENU_OPTION_SELECT_COMPANY = 2;
    private static final int MENU_OPTION_VIEW_COMPANY_DETAILS = 3;
    private static final int MENU_OPTION_EDIT_COMPANY = 4;
    private static final int MENU_OPTION_DELETE_COMPANY = 5;
    private static final int MENU_OPTION_BACK_TO_MAIN = 6;
    private static final int COMPANY_MENU_MAX_OPTION = 6;
    
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
     * @param companyService the service for company management operations
     * @param applicationState the application state manager
     * @param menu the console menu for user interaction
     * @param inputHandler the input handler for user input
     * @param outputFormatter the output formatter for display formatting
     */
    public CompanyController(CompanyService initialCompanyService, ApplicationState initialApplicationState,
                           ConsoleMenu initialMenu, InputHandler initialInputHandler, OutputFormatter initialOutputFormatter) {
        this.companyService = initialCompanyService;
        this.applicationState = initialApplicationState;
        this.menu = initialMenu;
        this.inputHandler = initialInputHandler;
        this.outputFormatter = initialOutputFormatter;
    }
    
    public void handleCompanySetup() {
        boolean back = false;
        while (!back) {
            menu.displayCompanyMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, COMPANY_MENU_MAX_OPTION);
            
            switch (choice) {
                case MENU_OPTION_CREATE_COMPANY:
                    createCompany();
                    break;
                case MENU_OPTION_SELECT_COMPANY:
                    selectCompany();
                    break;
                case MENU_OPTION_VIEW_COMPANY_DETAILS:
                    viewCompanyDetails();
                    break;
                case MENU_OPTION_EDIT_COMPANY:
                    editCompany();
                    break;
                case MENU_OPTION_DELETE_COMPANY:
                    deleteCompany();
                    break;
                case MENU_OPTION_BACK_TO_MAIN:
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
            // Basic company information
            String name = inputHandler.getString("Enter company name");
            String regNumber = inputHandler.getString("Enter registration number (optional)", "");
            String taxNumber = inputHandler.getString("Enter tax number (optional)", "");
            String address = inputHandler.getString("Enter address (optional)", "");
            String email = inputHandler.getString("Enter contact email (optional)", "");
            String phone = inputHandler.getString("Enter contact phone (optional)", "");
            
            // Banking details
            outputFormatter.printInfo("\n📊 Banking Details");
            String bankName = inputHandler.getString("Enter bank name (optional)", "");
            String accountNumber = inputHandler.getString("Enter account number (optional)", "");
            String accountType = inputHandler.getString("Enter account type (e.g., Business Cheque) (optional)", "");
            String branchCode = inputHandler.getString("Enter branch code (optional)", "");
            
            // VAT registration
            outputFormatter.printInfo("\n💰 VAT Registration");
            boolean vatRegistered = inputHandler.getBoolean("Is company VAT registered? (y/n)");
            
            Company company = new Company(name);
            company.setRegistrationNumber(regNumber.isEmpty() ? null : regNumber);
            company.setTaxNumber(taxNumber.isEmpty() ? null : taxNumber);
            company.setAddress(address.isEmpty() ? null : address);
            company.setContactEmail(email.isEmpty() ? null : email);
            company.setContactPhone(phone.isEmpty() ? null : phone);
            company.setBankName(bankName.isEmpty() ? null : bankName);
            company.setAccountNumber(accountNumber.isEmpty() ? null : accountNumber);
            company.setAccountType(accountType.isEmpty() ? null : accountType);
            company.setBranchCode(branchCode.isEmpty() ? null : branchCode);
            company.setVatRegistered(vatRegistered);
            
            company = companyService.createCompany(company);
            applicationState.setCurrentCompany(company);
            
            outputFormatter.printSuccess("Company created successfully!");
            outputFormatter.printInfo("Company ID: " + company.getId());
            outputFormatter.printInfo("Company Name: " + company.getName());
            if (vatRegistered) {
                outputFormatter.printInfo("VAT Registered: Yes (15% VAT will be applied to invoices)");
            } else {
                outputFormatter.printInfo("VAT Registered: No (No VAT will be applied to invoices)");
            }
            
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
            
            // Banking Details Section
            outputFormatter.printInfo("\n📊 Banking Details");
            String bankName = inputHandler.getString("Bank name", 
                    currentCompany.getBankName() != null ? currentCompany.getBankName() : "");
            String accountNumber = inputHandler.getString("Account number", 
                    currentCompany.getAccountNumber() != null ? currentCompany.getAccountNumber() : "");
            String accountType = inputHandler.getString("Account type (e.g., Cheque, Savings)", 
                    currentCompany.getAccountType() != null ? currentCompany.getAccountType() : "");
            String branchCode = inputHandler.getString("Branch code", 
                    currentCompany.getBranchCode() != null ? currentCompany.getBranchCode() : "");
            
            // VAT Registration Section
            outputFormatter.printInfo("\n💰 VAT Registration");
            outputFormatter.printInfo("Current VAT status: " + 
                    (currentCompany.isVatRegistered() ? "Registered (15% VAT on invoices)" : "Not registered (0% VAT)"));
            boolean vatRegistered = inputHandler.getBoolean("Is company VAT registered?");
            
            currentCompany.setName(name);
            currentCompany.setRegistrationNumber(regNumber.isEmpty() ? null : regNumber);
            currentCompany.setTaxNumber(taxNumber.isEmpty() ? null : taxNumber);
            currentCompany.setAddress(address.isEmpty() ? null : address);
            currentCompany.setContactEmail(email.isEmpty() ? null : email);
            currentCompany.setContactPhone(phone.isEmpty() ? null : phone);
            currentCompany.setLogoPath(logoPath.isEmpty() ? null : logoPath);
            currentCompany.setBankName(bankName.isEmpty() ? null : bankName);
            currentCompany.setAccountNumber(accountNumber.isEmpty() ? null : accountNumber);
            currentCompany.setAccountType(accountType.isEmpty() ? null : accountType);
            currentCompany.setBranchCode(branchCode.isEmpty() ? null : branchCode);
            currentCompany.setVatRegistered(vatRegistered);
            
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
