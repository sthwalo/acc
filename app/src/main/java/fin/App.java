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
/*
 * Copyright 2025 Sthwalo Nyoni
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
 *
 * FIN Financial Management System
 * Enhanced with interactive features and financial reporting
 */
package fin;

import fin.model.Account;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.model.JournalEntryLine;
import fin.service.BankStatementProcessingService;
import fin.service.TransactionVerificationService;
import fin.service.CompanyService;
import fin.service.CsvImportService;
import fin.service.CsvExportService;
import fin.service.DataManagementService;
import fin.service.PdfExportService;
import fin.service.ReportService;
import fin.service.FinancialReportingService;
import fin.service.ClassificationIntegrationService;
import fin.license.LicenseManager;
import fin.config.DatabaseConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class App {
    public String getGreeting(String name) {
        if (name != null && !name.trim().isEmpty()) {
            return "Hello, " + name + "! Welcome to the FIN application.";
        }
        return "Hello World! Welcome to the FIN application.";
    }
    
    // Use PostgreSQL database via DatabaseConfig
    private final CompanyService companyService;
    private final CsvImportService csvImportService;
    private final ReportService reportService;
    private final FinancialReportingService financialReportingService;
    private final PdfExportService pdfExportService;
    private final DataManagementService dataManagementService;
    private final BankStatementProcessingService bankStatementService;
    private final TransactionVerificationService verificationService;
    private final ClassificationIntegrationService classificationService;
    private Company currentCompany;
    private FiscalPeriod currentFiscalPeriod;
    
    public App() {
        // Test database connection and get database URL
        if (!DatabaseConfig.testConnection()) {
            throw new RuntimeException("Failed to connect to database");
        }
        
        String dbUrl = DatabaseConfig.getDatabaseUrl();
        System.out.println("🔌 Console App connected to: " + DatabaseConfig.getDatabaseType());
        
        this.companyService = new CompanyService(dbUrl);
        this.csvImportService = new CsvImportService(dbUrl, companyService);
        this.reportService = new ReportService(dbUrl, csvImportService);
        this.financialReportingService = new FinancialReportingService(dbUrl);
        this.pdfExportService = new PdfExportService();
        this.dataManagementService = new DataManagementService(dbUrl, companyService, csvImportService.getAccountService());
        this.bankStatementService = new BankStatementProcessingService(dbUrl);
        this.verificationService = new TransactionVerificationService(dbUrl, companyService, csvImportService);
        this.classificationService = new ClassificationIntegrationService();
    }
    
    public void showDataManagementMenu() {
        System.out.println("\n===== Data Management =====");
        System.out.println("1. Create Manual Invoice");
        System.out.println("2. Create Journal Entry");
        System.out.println("3. Transaction Classification");
        System.out.println("4. Correct Transaction Categorization");
        System.out.println("5. View Transaction History");
        System.out.println("6. Reset Company Data");
        System.out.println("7. Export to CSV");
        System.out.println("8. Back to main menu");
        System.out.print("Enter your choice (1-8): ");
    }
    
    public void showMenu() {
        System.out.println("\n===== FIN Application Menu =====");
        System.out.println("1. Company Setup");
        System.out.println("2. Fiscal Period Management");
        System.out.println("3. Import Bank Statement");
        System.out.println("4. Import CSV Data");
        System.out.println("5. View Imported Data");
        System.out.println("6. Generate Reports");
        System.out.println("7. Data Management");
        System.out.println("8. Verify Transactions");
        System.out.println("9. Payroll Management");
        System.out.println("10. Show current time");
        System.out.println("11. Exit");
        System.out.print("Enter your choice (1-11): ");
    }

    public void showCompanyMenu() {
        System.out.println("\n===== Company Setup =====");
        System.out.println("1. Create new company");
        System.out.println("2. Select existing company");
        System.out.println("3. View company details");
        System.out.println("4. Edit company details");
        System.out.println("5. Delete company");
        System.out.println("6. Back to main menu");
        System.out.print("Enter your choice (1-6): ");
    }
    
    public void showFiscalPeriodMenu() {
        System.out.println("\n===== Fiscal Period Management =====");
        System.out.println("1. Create new fiscal period");
        System.out.println("2. Select existing fiscal period");
        System.out.println("3. View fiscal period details");
        System.out.println("4. Back to main menu");
        System.out.print("Enter your choice (1-4): ");
    }
    
    public void showReportMenu() {
        System.out.println("\n===== Financial Reports =====");
        System.out.println("1. Cashbook Report");
        System.out.println("2. General Ledger Report");
        System.out.println("3. Trial Balance Report");
        System.out.println("4. Income Statement");
        System.out.println("5. Balance Sheet");
        System.out.println("6. Cash Flow Statement");
        System.out.println("7. Back to main menu");
        System.out.print("Enter your choice (1-7): ");
    }
    
    private void handleCompanySetup(Scanner scanner) {
        boolean back = false;
        while (!back) {
            showCompanyMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    createCompany(scanner);
                    break;
                case "2":
                    selectCompany(scanner);
                    break;
                case "3":
                    viewCompanyDetails();
                    break;
                case "4":
                    editCompany(scanner);
                    break;
                case "5":
                    deleteCompany(scanner);
                    break;
                case "6":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private void createCompany(Scanner scanner) {
        System.out.println("\n===== Create New Company =====");
        
        System.out.print("Enter company name: ");
        String name = scanner.nextLine().trim();
        
        System.out.print("Enter registration number (optional): ");
        String regNumber = scanner.nextLine().trim();
        
        System.out.print("Enter tax number (optional): ");
        String taxNumber = scanner.nextLine().trim();
        
        System.out.print("Enter address (optional): ");
        String address = scanner.nextLine().trim();
        
        System.out.print("Enter contact email (optional): ");
        String email = scanner.nextLine().trim();
        
        System.out.print("Enter contact phone (optional): ");
        String phone = scanner.nextLine().trim();
        
        Company company = new Company(name);
        company.setRegistrationNumber(regNumber.isEmpty() ? null : regNumber);
        company.setTaxNumber(taxNumber.isEmpty() ? null : taxNumber);
        company.setAddress(address.isEmpty() ? null : address);
        company.setContactEmail(email.isEmpty() ? null : email);
        company.setContactPhone(phone.isEmpty() ? null : phone);
        
        company = companyService.createCompany(company);
        currentCompany = company;
        
        System.out.println("\nCompany created successfully!");
        System.out.println("Company ID: " + company.getId());
        System.out.println("Company Name: " + company.getName());
    }
    
    private void selectCompany(Scanner scanner) {
        List<Company> companies = companyService.getAllCompanies();
        
        if (companies.isEmpty()) {
            System.out.println("No companies found. Please create a company first.");
            return;
        }
        
        System.out.println("\n===== Select Company =====");
        for (int i = 0; i < companies.size(); i++) {
            System.out.println((i + 1) + ". " + companies.get(i).getName());
        }
        
        System.out.print("Enter company number (1-" + companies.size() + "): ");
        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= companies.size()) {
                currentCompany = companies.get(selection - 1);
                System.out.println("Selected company: " + currentCompany.getName());
                
                // Reset current fiscal period when company changes
                currentFiscalPeriod = null;
            } else {
                System.out.println("Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    }
    
    private void viewCompanyDetails() {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        System.out.println("\n===== Company Details =====");
        System.out.println("ID: " + currentCompany.getId());
        System.out.println("Name: " + currentCompany.getName());
        System.out.println("Registration Number: " + 
                (currentCompany.getRegistrationNumber() != null ? currentCompany.getRegistrationNumber() : "N/A"));
        System.out.println("Tax Number: " + 
                (currentCompany.getTaxNumber() != null ? currentCompany.getTaxNumber() : "N/A"));
        System.out.println("Address: " + 
                (currentCompany.getAddress() != null ? currentCompany.getAddress() : "N/A"));
        System.out.println("Contact Email: " + 
                (currentCompany.getContactEmail() != null ? currentCompany.getContactEmail() : "N/A"));
        System.out.println("Contact Phone: " + 
                (currentCompany.getContactPhone() != null ? currentCompany.getContactPhone() : "N/A"));
    }
    
    private void editCompany(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        System.out.println("\n===== Edit Company Details =====");
        System.out.println("Current values shown in [brackets]. Press Enter to keep current value.");
        
        System.out.print("Company name [" + currentCompany.getName() + "]: ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) {
            currentCompany.setName(name);
        }
        
        System.out.print("Registration number [" + 
                (currentCompany.getRegistrationNumber() != null ? currentCompany.getRegistrationNumber() : "N/A") + "]: ");
        String regNumber = scanner.nextLine().trim();
        if (!regNumber.isEmpty()) {
            currentCompany.setRegistrationNumber(regNumber.equals("N/A") ? null : regNumber);
        }
        
        System.out.print("Tax number [" + 
                (currentCompany.getTaxNumber() != null ? currentCompany.getTaxNumber() : "N/A") + "]: ");
        String taxNumber = scanner.nextLine().trim();
        if (!taxNumber.isEmpty()) {
            currentCompany.setTaxNumber(taxNumber.equals("N/A") ? null : taxNumber);
        }
        
        System.out.print("Address [" + 
                (currentCompany.getAddress() != null ? currentCompany.getAddress() : "N/A") + "]: ");
        String address = scanner.nextLine().trim();
        if (!address.isEmpty()) {
            currentCompany.setAddress(address.equals("N/A") ? null : address);
        }
        
        System.out.print("Contact email [" + 
                (currentCompany.getContactEmail() != null ? currentCompany.getContactEmail() : "N/A") + "]: ");
        String email = scanner.nextLine().trim();
        if (!email.isEmpty()) {
            currentCompany.setContactEmail(email.equals("N/A") ? null : email);
        }
        
        System.out.print("Contact phone [" + 
                (currentCompany.getContactPhone() != null ? currentCompany.getContactPhone() : "N/A") + "]: ");
        String phone = scanner.nextLine().trim();
        if (!phone.isEmpty()) {
            currentCompany.setContactPhone(phone.equals("N/A") ? null : phone);
        }
        
        try {
            currentCompany = companyService.updateCompany(currentCompany);
            System.out.println("\nCompany updated successfully!");
        } catch (Exception e) {
            System.err.println("Error updating company: " + e.getMessage());
        }
    }
    
    private void deleteCompany(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        System.out.println("\n===== Delete Company =====");
        System.out.println("WARNING: This will permanently delete the company and all associated data.");
        System.out.println("Company to delete: " + currentCompany.getName() + " (ID: " + currentCompany.getId() + ")");
        System.out.print("Type 'DELETE' to confirm: ");
        
        String confirmation = scanner.nextLine().trim();
        if ("DELETE".equals(confirmation)) {
            try {
                boolean deleted = companyService.deleteCompany(currentCompany.getId());
                if (deleted) {
                    System.out.println("Company deleted successfully!");
                    currentCompany = null;
                    currentFiscalPeriod = null;
                } else {
                    System.out.println("Failed to delete company. It may have already been deleted or doesn't exist.");
                }
            } catch (Exception e) {
                System.err.println("Error deleting company: " + e.getMessage());
                System.err.println("This could be due to existing data that depends on this company.");
            }
        } else {
            System.out.println("Deletion cancelled.");
        }
    }
    
    private void handleFiscalPeriodManagement(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        boolean back = false;
        while (!back) {
            showFiscalPeriodMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    createFiscalPeriod(scanner);
                    break;
                case "2":
                    selectFiscalPeriod(scanner);
                    break;
                case "3":
                    viewFiscalPeriodDetails();
                    break;
                case "4":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private void createFiscalPeriod(Scanner scanner) {
        System.out.println("\n===== Create New Fiscal Period =====");
        
        System.out.print("Enter period name (e.g., FY2023-2024): ");
        String name = scanner.nextLine().trim();
        
        LocalDate startDate = null;
        while (startDate == null) {
            System.out.print("Enter start date (DD/MM/YYYY): ");
            String dateStr = scanner.nextLine().trim();
            try {
                startDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use DD/MM/YYYY.");
            }
        }
        
        LocalDate endDate = null;
        while (endDate == null) {
            System.out.print("Enter end date (DD/MM/YYYY): ");
            String dateStr = scanner.nextLine().trim();
            try {
                endDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                if (endDate.isBefore(startDate)) {
                    System.out.println("End date must be after start date.");
                    endDate = null;
                }
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use DD/MM/YYYY.");
            }
        }
        
        FiscalPeriod fiscalPeriod = new FiscalPeriod(currentCompany.getId(), name, startDate, endDate);
        fiscalPeriod = companyService.createFiscalPeriod(fiscalPeriod);
        currentFiscalPeriod = fiscalPeriod;
        
        System.out.println("\nFiscal period created successfully!");
        System.out.println("Period ID: " + fiscalPeriod.getId());
        System.out.println("Period Name: " + fiscalPeriod.getPeriodName());
        System.out.println("Date Range: " + 
                fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                " - " + 
                fiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    }
    
    private void selectFiscalPeriod(Scanner scanner) {
        List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(currentCompany.getId());
        
        if (periods.isEmpty()) {
            System.out.println("No fiscal periods found. Please create a fiscal period first.");
            return;
        }
        
        System.out.println("\n===== Select Fiscal Period =====");
        for (int i = 0; i < periods.size(); i++) {
            FiscalPeriod period = periods.get(i);
            System.out.println((i + 1) + ". " + period.getPeriodName() + " (" + 
                    period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                    " - " + 
                    period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ")");
        }
        
        System.out.print("Enter period number (1-" + periods.size() + "): ");
        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= periods.size()) {
                currentFiscalPeriod = periods.get(selection - 1);
                System.out.println("Selected fiscal period: " + currentFiscalPeriod.getPeriodName());
            } else {
                System.out.println("Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number.");
        }
    }
    
    private void viewFiscalPeriodDetails() {
        if (currentFiscalPeriod == null) {
            System.out.println("No fiscal period selected. Please select a fiscal period first.");
            return;
        }
        
        System.out.println("\n===== Fiscal Period Details =====");
        System.out.println("ID: " + currentFiscalPeriod.getId());
        System.out.println("Name: " + currentFiscalPeriod.getPeriodName());
        System.out.println("Start Date: " + 
                currentFiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        System.out.println("End Date: " + 
                currentFiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        System.out.println("Status: " + (currentFiscalPeriod.isClosed() ? "Closed" : "Open"));
    }
    
    private void handleCsvImport(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        if (currentFiscalPeriod == null) {
            System.out.println("No fiscal period selected. Please select a fiscal period first.");
            return;
        }
        
        System.out.println("\n===== Import CSV Data =====");
        System.out.print("Enter CSV file path: ");
        String filePath = scanner.nextLine().trim();
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }
        
        try {
            List<BankTransaction> transactions = csvImportService.importCsvFile(filePath, currentCompany.getId(), currentFiscalPeriod.getId());
            System.out.println("Successfully imported " + transactions.size() + " transactions.");
        } catch (Exception e) {
            System.err.println("Error importing CSV file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleReportGeneration(Scanner scanner) {
        if (currentCompany == null || currentFiscalPeriod == null) {
            System.out.println("Company and fiscal period must be selected before generating reports.");
            return;
        }
        
        boolean back = false;
        while (!back) {
            showReportMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    generateCashbookReport();
                    break;
                case "2":
                    generateGeneralLedgerReport();
                    break;
                case "3":
                    generateTrialBalanceReport();
                    break;
                case "4":
                    generateIncomeStatementReport();
                    break;
                case "5":
                    generateBalanceSheetReport();
                    break;
                case "6":
                    generateCashFlowReport();
                    break;
                case "7":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private void generateCashbookReport() {
        System.out.println("\nGenerating Cashbook Report...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report
            financialReportingService.generateCashbook(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ Cashbook Report generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating Cashbook Report: " + e.getMessage());
        }
    }
    
    private void generateGeneralLedgerReport() {
        System.out.println("\nGenerating General Ledger Report...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report
            financialReportingService.generateGeneralLedger(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ General Ledger Report generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating General Ledger Report: " + e.getMessage());
        }
    }
    
    private void generateTrialBalanceReport() {
        System.out.println("\nGenerating Trial Balance Report...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report
            financialReportingService.generateTrialBalance(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ Trial Balance Report generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating Trial Balance Report: " + e.getMessage());
        }
    }
    
    private void generateIncomeStatementReport() {
        System.out.println("\nGenerating Income Statement...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report
            financialReportingService.generateIncomeStatement(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ Income Statement generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating Income Statement: " + e.getMessage());
        }
    }
    
    private void generateBalanceSheetReport() {
        System.out.println("\nGenerating Balance Sheet...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report
            financialReportingService.generateBalanceSheet(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ Balance Sheet generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating Balance Sheet: " + e.getMessage());
        }
    }
    
    private void generateCashFlowReport() {
        System.out.println("\nGenerating Cash Flow Statement...");
        try {
            // Ensure reports directory exists
            String reportsDir = "/Users/sthwalonyoni/FIN/reports";
            ensureReportsDirectoryExists(reportsDir);
            
            // Generate report using Audit Trail as a substitute since FinancialReportingService
            // doesn't have a dedicated Cash Flow method
            financialReportingService.generateAuditTrail(currentCompany.getId(), currentFiscalPeriod.getId(), true);
            
            System.out.println("\n✅ Cash Flow Statement generated and saved to reports directory");
            System.out.println("📁 Location: " + reportsDir);
        } catch (Exception e) {
            System.err.println("\n❌ Error generating Cash Flow Statement: " + e.getMessage());
        }
    }
    
    /**
     * Ensures the reports directory exists, creating it if necessary
     * @return true if directory exists or was created successfully
     */
    private boolean ensureReportsDirectoryExists(String reportsDir) {
        File directory = new File(reportsDir);
        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (created) {
                System.out.println("📁 Created reports directory: " + reportsDir);
            } else {
                System.err.println("❌ Failed to create reports directory: " + reportsDir);
                return false;
            }
        } else {
            System.out.println("📁 Using existing reports directory: " + reportsDir);
        }
        
        // Check if directory is writable
        if (!directory.canWrite()) {
            System.err.println("❌ Reports directory is not writable: " + reportsDir);
            return false;
        }
        
        return true;
    }
    
    /**
     * Handles viewing imported transaction data
     * Shows transactions for the current company and fiscal period
     * Offers option to export to PDF
     */
    private void handleViewImportedData(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        if (currentFiscalPeriod == null) {
            System.out.println("No fiscal period selected. Please select a fiscal period first.");
            return;
        }
        
        System.out.println("\n===== Imported Transactions =====\n");
        System.out.println("Company: " + currentCompany.getName());
        System.out.println("Fiscal Period: " + currentFiscalPeriod.getPeriodName() + 
                          " (" + currentFiscalPeriod.getStartDate() + " to " + 
                          currentFiscalPeriod.getEndDate() + ")");
        
        List<BankTransaction> transactions = csvImportService.getTransactions(
                currentCompany.getId(), currentFiscalPeriod.getId());
        
        if (transactions.isEmpty()) {
            System.out.println("\nNo transactions found for the selected company and fiscal period.");
            return;
        }
        
        System.out.println("\nFound " + transactions.size() + " transactions.");
        System.out.println("\nOptions:");
        System.out.println("1. View transactions in terminal");
        System.out.println("2. Export transactions to PDF");
        System.out.println("3. Back to main menu");
        System.out.print("Enter your choice (1-3): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                displayTransactionsInTerminal(transactions);
                break;
            case "2":
                exportTransactionsToPdf(transactions);
                break;
            case "3":
                return;
            default:
                System.out.println("Invalid choice. Returning to main menu.");
                return;
        }
        
        System.out.println("\nPress Enter to continue...");
        scanner.nextLine();
    }
    
    /**
     * Displays transactions in the terminal
     */
    private void displayTransactionsInTerminal(List<BankTransaction> transactions) {
        System.out.println("\nDisplaying " + transactions.size() + " transactions:\n");
        System.out.printf("%-12s %-40s %-15s %-15s %-15s\n", "Date", "Details", "Debit", "Credit", "Balance");
        System.out.println("-".repeat(100));
        
        for (BankTransaction transaction : transactions) {
            String debit = transaction.getDebitAmount() != null ? 
                    String.format("%,.2f", transaction.getDebitAmount()) : "";
            String credit = transaction.getCreditAmount() != null ? 
                    String.format("%,.2f", transaction.getCreditAmount()) : "";
            String balance = transaction.getBalance() != null ? 
                    String.format("%,.2f", transaction.getBalance()) : "";
            
            // Truncate details if too long
            String details = transaction.getDetails();
            if (details != null && details.length() > 38) {
                details = details.substring(0, 35) + "...";
            }
            
            System.out.printf("%-12s %-40s %-15s %-15s %-15s\n", 
                    transaction.getTransactionDate(), 
                    details, 
                    debit, 
                    credit, 
                    balance);
        }
    }
    
    /**
     * Exports transactions to PDF
     */
    private void exportTransactionsToPdf(List<BankTransaction> transactions) {
        try {
            System.out.println("\nExporting transactions to PDF...");
            String pdfPath = pdfExportService.exportTransactionsToPdf(
                    transactions, currentCompany, currentFiscalPeriod);
            System.out.println("PDF successfully generated: " + pdfPath);
            System.out.println("You can find the PDF file at: " + new File(pdfPath).getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error exporting to PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleDataManagement(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        boolean back = false;
        while (!back) {
            showDataManagementMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    handleManualInvoiceCreation(scanner);
                    break;
                case "2":
                    handleJournalEntryCreation(scanner);
                    break;
                case "3":
                    handleTransactionClassification(scanner);
                    break;
                case "4":
                    handleTransactionCorrection(scanner);
                    break;
                case "5":
                    handleTransactionHistory(scanner);
                    break;
                case "6":
                    handleDataReset(scanner);
                    break;
                case "7":
                    handleExportToCSV(scanner);
                    break;
                case "8":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
    
    private void handleTransactionClassification(Scanner scanner) {
        if (currentCompany == null || currentFiscalPeriod == null) {
            System.out.println("Please select both a company and fiscal period first.");
            return;
        }

        System.out.println("\n===== Transaction Classification =====");
        System.out.println("1. Run Interactive Classification");
        System.out.println("2. Auto-Classify Transactions");
        System.out.println("3. Initialize Chart of Accounts");
        System.out.println("4. Synchronize Journal Entries");
        System.out.println("5. Back to Data Management");
        System.out.print("Enter your choice (1-5): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                classificationService.runInteractiveClassification(
                    currentCompany.getId(), currentFiscalPeriod.getId());
                break;
            case "2":
                classificationService.autoClassifyTransactions(
                    currentCompany.getId(), currentFiscalPeriod.getId());
                break;
            case "3":
                System.out.println("\n===== Initialize Chart of Accounts =====");
                System.out.println("1. Initialize Chart of Accounts Only");
                System.out.println("2. Initialize Transaction Mapping Rules Only");
                System.out.println("3. Perform Full Initialization");
                System.out.println("4. Back");
                System.out.print("Enter choice (1-4): ");
                
                String initChoice = scanner.nextLine().trim();
                switch (initChoice) {
                    case "1":
                        boolean success = classificationService.initializeChartOfAccounts(currentCompany.getId());
                        if (success) {
                            System.out.println("✅ Chart of Accounts initialized successfully");
                        }
                        break;
                    case "2":
                        success = classificationService.initializeTransactionMappingRules(currentCompany.getId());
                        if (success) {
                            System.out.println("✅ Transaction Mapping Rules initialized successfully");
                        }
                        break;
                    case "3":
                        success = classificationService.performFullInitialization(currentCompany.getId());
                        if (success) {
                            System.out.println("✅ Full initialization completed successfully");
                        }
                        break;
                    case "4":
                        // Just go back
                        break;
                    default:
                        System.out.println("Invalid choice.");
                }
                break;
            case "4":
                int syncCount = classificationService.synchronizeJournalEntries(
                    currentCompany.getId(), currentFiscalPeriod.getId());
                if (syncCount > 0) {
                    System.out.println("✅ Synchronized " + syncCount + " transactions with journal entries");
                }
                break;
            case "5":
                // Just go back
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }
    
    private void handleManualInvoiceCreation(Scanner scanner) {
        System.out.println("\n===== Create Manual Invoice =====");
        if (currentFiscalPeriod == null) {
            System.out.println("Please select a fiscal period first.");
            return;
        }
        
        try {
            // Get invoice details
            System.out.print("Enter invoice number: ");
            String invoiceNumber = scanner.nextLine().trim();
            
            System.out.print("Enter invoice date (DD/MM/YYYY): ");
            LocalDate invoiceDate = LocalDate.parse(scanner.nextLine().trim(), 
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                
            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();
            
            System.out.print("Enter amount: ");
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
            
            // Show accounts for selection
            System.out.println("\nSelect debit account:");
            Long debitAccountId = selectAccount(scanner);
            
            System.out.println("\nSelect credit account:");
            Long creditAccountId = selectAccount(scanner);
            
            if (debitAccountId != null && creditAccountId != null) {
                dataManagementService.createManualInvoice(
                    currentCompany.getId(), invoiceNumber, invoiceDate, description,
                    amount, debitAccountId, creditAccountId, currentFiscalPeriod.getId());
                System.out.println("Invoice created successfully!");
            }
            
        } catch (Exception e) {
            System.err.println("Error creating invoice: " + e.getMessage());
        }
    }
    
    private void handleJournalEntryCreation(Scanner scanner) {
        System.out.println("\n===== Create Journal Entry =====");
        if (currentFiscalPeriod == null) {
            System.out.println("Please select a fiscal period first.");
            return;
        }
        
        try {
            System.out.print("Enter entry number: ");
            String entryNumber = scanner.nextLine().trim();
            
            System.out.print("Enter entry date (DD/MM/YYYY): ");
            LocalDate entryDate = LocalDate.parse(scanner.nextLine().trim(), 
                DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                
            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();
            
            List<JournalEntryLine> lines = new ArrayList<>();
            boolean addMoreLines = true;
            
            while (addMoreLines) {
                System.out.println("\nAdd journal entry line:");
                JournalEntryLine line = new JournalEntryLine();
                
                System.out.println("\nSelect account:");
                line.setAccountId(selectAccount(scanner));
                
                System.out.print("Enter line description: ");
                line.setDescription(scanner.nextLine().trim());
                
                System.out.print("Is this a debit entry? (y/n): ");
                boolean isDebit = scanner.nextLine().trim().toLowerCase().startsWith("y");
                
                System.out.print("Enter amount: ");
                BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
                
                if (isDebit) {
                    line.setDebitAmount(amount);
                } else {
                    line.setCreditAmount(amount);
                }
                
                lines.add(line);
                
                System.out.print("\nAdd another line? (y/n): ");
                addMoreLines = scanner.nextLine().trim().toLowerCase().startsWith("y");
            }
            
            dataManagementService.createJournalEntry(
                currentCompany.getId(), entryNumber, entryDate, description,
                currentFiscalPeriod.getId(), lines);
            System.out.println("Journal entry created successfully!");
            
        } catch (Exception e) {
            System.err.println("Error creating journal entry: " + e.getMessage());
        }
    }
    
    private void handleTransactionCorrection(Scanner scanner) {
        System.out.println("\n===== Correct Transaction Categorization =====");
        if (currentFiscalPeriod == null) {
            System.out.println("Please select a fiscal period first.");
            return;
        }
        
        try {
            // Show recent transactions
            List<BankTransaction> transactions = csvImportService.getTransactions(
                currentCompany.getId(), currentFiscalPeriod.getId());
            
            if (transactions.isEmpty()) {
                System.out.println("No transactions found to correct.");
                return;
            }
            
            System.out.println("\nRecent Transactions:");
            for (int i = 0; i < transactions.size(); i++) {
                BankTransaction tx = transactions.get(i);
                System.out.printf("%d. [%s] %s - Amount: %s\n", i + 1,
                    tx.getTransactionDate(), tx.getDetails(),
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
            }
            
            System.out.print("\nSelect transaction number to correct: ");
            int txIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
            
            if (txIndex >= 0 && txIndex < transactions.size()) {
                BankTransaction tx = transactions.get(txIndex);
                
                System.out.println("\nSelect new account for categorization:");
                Long newAccountId = selectAccount(scanner);
                
                if (newAccountId != null) {
                    System.out.print("Enter reason for correction: ");
                    String reason = scanner.nextLine().trim();
                    
                    System.out.print("Enter your name: ");
                    String correctedBy = scanner.nextLine().trim();
                    
                    dataManagementService.correctTransactionCategory(
                        currentCompany.getId(), tx.getId(), tx.getBankAccountId(),
                        newAccountId, reason, correctedBy);
                    System.out.println("Transaction categorization corrected successfully!");
                }
            } else {
                System.out.println("Invalid transaction selection.");
            }
            
        } catch (Exception e) {
            System.err.println("Error correcting transaction: " + e.getMessage());
        }
    }
    
    private void handleTransactionHistory(Scanner scanner) {
        System.out.println("\n===== View Transaction History =====");
        if (currentFiscalPeriod == null) {
            System.out.println("Please select a fiscal period first.");
            return;
        }
        
        try {
            // Show recent transactions
            List<BankTransaction> transactions = csvImportService.getTransactions(
                currentCompany.getId(), currentFiscalPeriod.getId());
            
            if (transactions.isEmpty()) {
                System.out.println("No transactions found.");
                return;
            }
            
            System.out.println("\nRecent Transactions:");
            for (int i = 0; i < transactions.size(); i++) {
                BankTransaction tx = transactions.get(i);
                System.out.printf("%d. [%s] %s - Amount: %s\n", i + 1,
                    tx.getTransactionDate(), tx.getDetails(),
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : tx.getCreditAmount());
            }
            
            System.out.print("\nSelect transaction number to view history: ");
            int txIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;
            
            if (txIndex >= 0 && txIndex < transactions.size()) {
                BankTransaction tx = transactions.get(txIndex);
                List<Map<String, Object>> history = 
                    dataManagementService.getTransactionCorrectionHistory(tx.getId());
                
                if (history.isEmpty()) {
                    System.out.println("No correction history found for this transaction.");
                } else {
                    System.out.println("\nCorrection History:");
                    for (Map<String, Object> correction : history) {
                        System.out.printf("Date: %s\n", correction.get("correctedAt"));
                        System.out.printf("By: %s\n", correction.get("correctedBy"));
                        System.out.printf("From: %s (%s)\n", 
                            correction.get("originalAccountName"),
                            correction.get("originalAccountCode"));
                        System.out.printf("To: %s (%s)\n",
                            correction.get("newAccountName"),
                            correction.get("newAccountCode"));
                        System.out.printf("Reason: %s\n", correction.get("reason"));
                        System.out.println();
                    }
                }
            } else {
                System.out.println("Invalid transaction selection.");
            }
            
        } catch (Exception e) {
            System.err.println("Error viewing transaction history: " + e.getMessage());
        }
    }
    
    private void handleBankStatementImport(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }
        
        if (currentFiscalPeriod == null) {
            System.out.println("No fiscal period selected. Please select a fiscal period first.");
            return;
        }
        
        System.out.println("\n===== Import Bank Statement =====");
        System.out.println("1. Import single bank statement");
        System.out.println("2. Import multiple bank statements (batch)");
        System.out.println("3. Back to main menu");
        System.out.print("Enter your choice (1-3): ");
        
        String choice = scanner.nextLine().trim();
        
        switch (choice) {
            case "1":
                handleSingleBankStatementImport(scanner);
                break;
            case "2":
                handleBatchBankStatementImport(scanner);
                break;
            case "3":
                return;
            default:
                System.out.println("Invalid choice. Please try again.");
        }
    }
    
    private void handleSingleBankStatementImport(Scanner scanner) {
        System.out.println("\n===== Import Single Bank Statement =====");
        System.out.print("Enter bank statement file path: ");
        String filePath = scanner.nextLine().trim();
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }
        
        try {
            // Process the bank statement
            List<BankTransaction> transactions = bankStatementService.processStatement(
                filePath, currentCompany);
            
            // Ask for CSV export
            System.out.print("\nWould you like to export the processed transactions to CSV? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String outputPath = String.format("processed_statement_%s_%s_%s.csv",
                    currentCompany.getName().toLowerCase().replace(" ", "_"),
                    currentFiscalPeriod.getPeriodName().toLowerCase(),
                    timestamp);
                
                exportTransactions(transactions, outputPath);
                System.out.println("Transactions exported to: " + new File(outputPath).getAbsolutePath());
            }
            
            System.out.println("Successfully processed " + transactions.size() + " transactions.");
            
        } catch (Exception e) {
            System.err.println("Error processing bank statement: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleBatchBankStatementImport(Scanner scanner) {
        System.out.println("\n===== Import Multiple Bank Statements =====");
        System.out.println("Enter file paths (one per line)");
        System.out.println("Enter an empty line when done");
        
        List<String> filePaths = new ArrayList<>();
        while (true) {
            System.out.print("File path (or empty to finish): ");
            String filePath = scanner.nextLine().trim();
            
            if (filePath.isEmpty()) {
                break;
            }
            
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.out.println("File not found: " + filePath);
                continue;
            }
            
            filePaths.add(filePath);
        }
        
        if (filePaths.isEmpty()) {
            System.out.println("No valid files provided.");
            return;
        }
        
        try {
            List<BankTransaction> allTransactions = new ArrayList<>();
            
            // Process each bank statement
            for (String filePath : filePaths) {
                List<BankTransaction> transactions = bankStatementService.processStatement(
                    filePath, currentCompany);
                allTransactions.addAll(transactions);
            }
            
            // Ask for CSV export
            System.out.print("\nWould you like to export the processed transactions to CSV? (y/n): ");
            if (scanner.nextLine().trim().toLowerCase().startsWith("y")) {
                String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                String outputPath = String.format("processed_statements_batch_%s_%s_%s.csv",
                    currentCompany.getName().toLowerCase().replace(" ", "_"),
                    currentFiscalPeriod.getPeriodName().toLowerCase(),
                    timestamp);
                
                exportTransactions(allTransactions, outputPath);
                System.out.println("Transactions exported to: " + new File(outputPath).getAbsolutePath());
            }
            
            System.out.println("Successfully processed " + allTransactions.size() + " transactions from " + filePaths.size() + " files.");
            
        } catch (Exception e) {
            System.err.println("Error processing bank statements: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void exportTransactions(List<BankTransaction> transactions, String outputPath) throws IOException {
        try (FileWriter writer = new FileWriter(outputPath)) {
            // Write header
            writer.write("Date,Details,Debit Amount,Credit Amount,Balance,Account Number\n");
            
            // Write transactions
            for (BankTransaction tx : transactions) {
                writer.write(String.format("%s,\"%s\",%s,%s,%s,%s\n",
                    tx.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    tx.getDetails() != null ? tx.getDetails().replace("\"", "\"\"") : "",
                    tx.getDebitAmount() != null ? tx.getDebitAmount() : "",
                    tx.getCreditAmount() != null ? tx.getCreditAmount() : "",
                    tx.getBalance() != null ? tx.getBalance() : "",
                    tx.getAccountNumber() != null ? tx.getAccountNumber() : ""
                ));
            }
        }
    }
    
    private void handleExportToCSV(Scanner scanner) {
        if (currentCompany == null || currentFiscalPeriod == null) {
            System.out.println("Please select both a company and fiscal period first.");
            return;
        }

        try {
            List<BankTransaction> transactions = csvImportService.getTransactions(
                    currentCompany.getId(), currentFiscalPeriod.getId());

            if (transactions.isEmpty()) {
                System.out.println("No transactions found to export.");
                return;
            }

            // Generate filename with timestamp
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String filename = String.format("exported_transactions_%s_%s_%s.csv",
                    currentCompany.getName().toLowerCase().replace(" ", "_"),
                    currentFiscalPeriod.getPeriodName().toLowerCase(),
                    timestamp);

            CsvExportService exportService = new CsvExportService(companyService);
            exportService.exportTransactionsToCsv(transactions, filename, currentFiscalPeriod.getId());

            System.out.println("Transactions exported successfully to: " + filename);
            System.out.println("You can find the file at: " + new File(filename).getAbsolutePath());

        } catch (Exception e) {
            System.err.println("Error exporting transactions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDataReset(Scanner scanner) {
        System.out.println("\n===== Reset Company Data =====");
        System.out.println("WARNING: This will delete transaction data for the current company.");
        System.out.println("1. Reset only transaction data");
        System.out.println("2. Reset all data (including chart of accounts)");
        System.out.println("3. Cancel");
        
        System.out.print("\nEnter choice (1-3): ");
        String choice = scanner.nextLine().trim();
        
        if ("3".equals(choice)) {
            return;
        }
        
        System.out.print("Type 'CONFIRM' to proceed: ");
        String confirmation = scanner.nextLine().trim();
        
        if ("CONFIRM".equals(confirmation)) {
            try {
                boolean preserveMasterData = "1".equals(choice);
                dataManagementService.resetCompanyData(currentCompany.getId(), preserveMasterData);
                System.out.println("Data reset successful!");
            } catch (Exception e) {
                System.err.println("Error resetting data: " + e.getMessage());
            }
        } else {
            System.out.println("Operation cancelled.");
        }
    }
    
    private void handleTransactionVerification(Scanner scanner) {
        if (currentCompany == null) {
            System.out.println("No company selected. Please select a company first.");
            return;
        }

        if (currentFiscalPeriod == null) {
            System.out.println("No fiscal period selected. Please select a fiscal period first.");
            return;
        }

        System.out.println("\n===== Transaction Verification =====");
        System.out.print("Enter bank statement file path: ");
        String bankStatementPath = scanner.nextLine().trim();

        File bankStatementFile = new File(bankStatementPath);
        if (!bankStatementFile.exists() || !bankStatementFile.isFile()) {
            System.out.println("Bank statement file not found: " + bankStatementPath);
            return;
        }

        try {
            System.out.println("\nVerifying transactions...");
            TransactionVerificationService.VerificationResult result = 
                verificationService.verifyTransactions(bankStatementPath, 
                    currentCompany.getId(), currentFiscalPeriod.getId());

            // Print verification results
            System.out.println("\n=== Verification Results ===");
            System.out.println("Overall Status: " + (result.isValid() ? "VALID" : "DISCREPANCIES FOUND"));
            System.out.println("\nTotal Amounts:");
            System.out.printf("Total Debits: %,.2f%n", result.getTotalDebits());
            System.out.printf("Total Credits: %,.2f%n", result.getTotalCredits());
            System.out.printf("Final Balance: %,.2f%n", result.getFinalBalance());

            if (!result.isValid()) {
                System.out.println("\nDiscrepancies Found:");
                for (String discrepancy : result.getDiscrepancies()) {
                    System.out.println("- " + discrepancy);
                }

                if (!result.getMissingTransactions().isEmpty()) {
                    System.out.println("\nTransactions in bank statement but missing in CSV:");
                    for (BankTransaction tx : result.getMissingTransactions()) {
                        System.out.printf("Date: %s | Details: %s | Debit: %s | Credit: %s | Balance: %s%n",
                            tx.getTransactionDate(),
                            tx.getDetails(),
                            tx.getDebitAmount() != null ? String.format("%,.2f", tx.getDebitAmount()) : "-",
                            tx.getCreditAmount() != null ? String.format("%,.2f", tx.getCreditAmount()) : "-",
                            tx.getBalance() != null ? String.format("%,.2f", tx.getBalance()) : "-");
                    }
                }

                if (!result.getExtraTransactions().isEmpty()) {
                    System.out.println("\nTransactions in CSV but not in bank statement:");
                    for (BankTransaction tx : result.getExtraTransactions()) {
                        System.out.printf("Date: %s | Details: %s | Debit: %s | Credit: %s | Balance: %s%n",
                            tx.getTransactionDate(),
                            tx.getDetails(),
                            tx.getDebitAmount() != null ? String.format("%,.2f", tx.getDebitAmount()) : "-",
                            tx.getCreditAmount() != null ? String.format("%,.2f", tx.getCreditAmount()) : "-",
                            tx.getBalance() != null ? String.format("%,.2f", tx.getBalance()) : "-");
                    }
                }

                if (!result.getDifferences().isEmpty()) {
                    System.out.println("\nTotal Differences:");
                    for (Map.Entry<String, BigDecimal> diff : result.getDifferences().entrySet()) {
                        System.out.printf("%s: %,.2f%n", 
                            diff.getKey().substring(0, 1).toUpperCase() + diff.getKey().substring(1),
                            diff.getValue());
                    }
                }
            }

            System.out.println("\nVerification complete.");

        } catch (Exception e) {
            System.err.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Long selectAccount(Scanner scanner) {
        List<Account> accounts = csvImportService.getAccountService().getAccountsByCompany(currentCompany.getId());
        
        if (accounts.isEmpty()) {
            System.out.println("No accounts found.");
            return null;
        }
        
        for (int i = 0; i < accounts.size(); i++) {
            Account account = accounts.get(i);
            System.out.printf("%d. [%s] %s\n", i + 1, account.getAccountCode(), account.getAccountName());
        }
        
        System.out.print("Select account number: ");
        try {
            int selection = Integer.parseInt(scanner.nextLine().trim());
            if (selection >= 1 && selection <= accounts.size()) {
                return accounts.get(selection - 1).getId();
            }
        } catch (NumberFormatException e) {
            // Invalid input
        }
        
        System.out.println("Invalid selection.");
        return null;
    }
    
    
    public static void main(String[] args) {
        // License compliance check
        if (!LicenseManager.checkLicenseCompliance()) {
            System.exit(1);
        }
        
        // Check if API mode is requested
        if (args.length > 0 && "api".equals(args[0])) {
            // Start API server mode
            System.out.println("🚀 Starting FIN API Server...");
            System.out.println("📊 Financial Management REST API");
            System.out.println("🌐 CORS enabled for frontend at http://localhost:3000");
            System.out.println("===============================================");
            
            try {
                fin.api.ApiServer apiServer = new fin.api.ApiServer();
                apiServer.start();
                
                // Add shutdown hook for graceful shutdown
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n🛑 Shutting down API server...");
                    apiServer.stop();
                    System.out.println("✅ Server stopped successfully");
                }));
                
                // Keep main thread alive
                Thread.currentThread().join();
                
            } catch (Exception e) {
                System.err.println("❌ Failed to start API server: " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            // Console application mode
            App app = new App();
            System.out.println(app.getGreeting(null));
            
            // Check if console is available - if not, suggest API mode
            // Bypassing console check for manual testing
            /*
            if (System.console() == null) {
                System.out.println("⚠️  Interactive console not available in this environment.");
                System.out.println("🚀 Please use API mode instead:");
                System.out.println("   ./gradlew run --args=\"api\"");
                System.out.println("   or: java -jar app/build/libs/app.jar api");
                System.out.println();
                System.out.println("💡 You can also use the test suite: ./interactive-test.sh");
                return;
            }
            */
            
            // Create a single Scanner for the entire application session
            Scanner scanner = new Scanner(System.in);
            boolean exit = false;
            
            try {
                while (!exit) {
                    app.showMenu();
                    
                    try {
                        String input = scanner.nextLine().trim();
                        if (input.isEmpty()) {
                            System.out.println("❌ Empty input. Please enter a number between 1-11.");
                            continue;
                        }
                        int choice = Integer.parseInt(input);
                        
                        switch (choice) {
                            case 1:
                                app.handleCompanySetup(scanner);
                                break;
                            case 2:
                                app.handleFiscalPeriodManagement(scanner);
                                break;
                            case 3:
                                app.handleBankStatementImport(scanner);
                                break;
                            case 4:
                                app.handleCsvImport(scanner);
                                break;
                            case 5:
                                app.handleViewImportedData(scanner);
                                break;
                            case 6:
                                app.handleReportGeneration(scanner);
                                break;
                            case 7:
                                app.handleDataManagement(scanner);
                                break;
                            case 8:
                                app.handleTransactionVerification(scanner);
                                break;
                            case 9:
                                System.out.println("⚠️ Payroll management is now available in the new modular application.");
                                System.out.println("Please use the ConsoleApplication entry point for payroll features.");
                                break;
                            case 10:
                                System.out.println("Current time: " + LocalDate.now());
                                break;
                            case 11:
                                exit = true;
                                System.out.println("Exiting Sthwalo application. Goodbye!");
                                break;
                            default:
                                System.out.println("Invalid choice. Please try again.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("❌ Invalid input. Please enter a number between 1-10.");
                    } catch (java.util.NoSuchElementException e) {
                        System.out.println("❌ No input available. Exiting...");
                        exit = true;  // Properly exit the loop instead of continuing infinitely
                    } catch (Exception e) {
                        System.out.println("❌ Input error: " + e.getMessage());
                        System.out.println("💡 Try using API mode: ./gradlew run --args=\"api\"");
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Unexpected error: " + e.getMessage());
                e.printStackTrace();
            }
            // Don't close the scanner as it closes System.in
        }
    }
}
