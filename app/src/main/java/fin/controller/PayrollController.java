package fin.controller;

import fin.service.PayrollService;
import fin.service.PayrollReportService;
import fin.service.PdfPrintService;
import fin.model.Employee;
import fin.model.PayrollPeriod;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PayrollController {
    private final PayrollService payrollService;
    private final PayrollReportService payrollReportService;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;

    public PayrollController(PayrollService payrollService, PayrollReportService payrollReportService, 
                           InputHandler inputHandler, OutputFormatter outputFormatter) {
        this.payrollService = payrollService;
        this.payrollReportService = payrollReportService;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }

    public void handlePayrollManagement(Long companyId) {
        outputFormatter.printHeader("Payroll Management");
        boolean backToMain = false;
        while (!backToMain) {
            displayPayrollMenu();
            int choice = inputHandler.getInteger("Enter your choice", 1, 6);
            
            switch (choice) {
                case 1:
                    handleEmployeeManagement(companyId);
                    break;
                case 2:
                    handlePayrollPeriodManagement(companyId);
                    break;
                case 3:
                    handlePayrollProcessing(companyId);
                    break;
                case 4:
                    handlePayslipGeneration(companyId);
                    break;
                case 5:
                    handlePayrollReports(companyId);
                    break;
                case 6:
                    backToMain = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice");
            }
        }
    }

    private void displayPayrollMenu() {
        outputFormatter.printHeader("Payroll Management");
        System.out.println("1. Employee Management");
        System.out.println("2. Payroll Period Management");
        System.out.println("3. Process Payroll");
        System.out.println("4. Generate Payslips");
        System.out.println("5. Payroll Reports");
        System.out.println("6. Back to Main Menu");
        outputFormatter.printSeparator();
    }

    private void handleCleanupPayslipsForTesting() {
        outputFormatter.printHeader("Cleanup Payslips for Testing");
        outputFormatter.printWarning("⚠️  This will delete ALL payslip data and reset payroll periods!");
        outputFormatter.printWarning("This action cannot be undone.");
        
        if (inputHandler.getBoolean("Are you sure you want to proceed with cleanup?")) {
            try {
                payrollService.cleanupAllPayslipsForTesting();
                outputFormatter.printSuccess("✅ All payslips cleaned up successfully!");
                outputFormatter.printInfo("You can now re-run payroll processing with updated tax calculations.");
            } catch (Exception e) {
                outputFormatter.printError("❌ Cleanup failed: " + e.getMessage());
            }
        } else {
            outputFormatter.printInfo("Cleanup cancelled.");
        }
        
        inputHandler.waitForEnter("Press Enter to continue");
    }

    private void handleEmployeeManagement(Long companyId) {
        outputFormatter.printHeader("Employee Management");
        boolean back = false;
        while (!back) {
            outputFormatter.printPlain("1. List Employees");
            outputFormatter.printPlain("2. Create Employee");
            outputFormatter.printPlain("3. Update Employee");
            outputFormatter.printPlain("4. Delete Employee");
            outputFormatter.printPlain("5. Back to Payroll Management");
            int choice = inputHandler.getInteger("Enter your choice", 1, 5);
            switch (choice) {
                case 1:
                    List<Employee> employees = payrollService.getActiveEmployees(companyId);
                    outputFormatter.printPlain("Active Employees:");
                    for (Employee emp : employees) {
                        outputFormatter.printPlain(emp.getEmployeeNumber() + " - " + emp.getFullName());
                    }
                    inputHandler.waitForEnter();
                    break;
                case 2:
                    createEmployee(companyId);
                    break;
                case 3:
                    updateEmployee(companyId);
                    break;
                case 4:
                    deleteEmployee(companyId);
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice.");
            }
        }
    }

    private void createEmployee(Long companyId) {
        outputFormatter.printHeader("Create New Employee");

        try {
            Employee employee = new Employee();
            employee.setCompanyId(companyId);

            // Basic Information
            employee.setEmployeeNumber(inputHandler.getString("Enter employee number:"));
            employee.setFirstName(inputHandler.getString("Enter first name:"));
            employee.setLastName(inputHandler.getString("Enter last name:"));
            employee.setEmail(inputHandler.getString("Enter email:"));
            employee.setPhone(inputHandler.getString("Enter phone number:"));

            // Employment Details
            employee.setPosition(inputHandler.getString("Enter position:"));
            employee.setDepartment(inputHandler.getString("Enter department:"));

            // Hire Date
            outputFormatter.printPlain("Enter hire date:");
            int hireYear = inputHandler.getInteger("Year", 2000, 2030);
            int hireMonth = inputHandler.getInteger("Month", 1, 12);
            int hireDay = inputHandler.getInteger("Day", 1, 31);
            employee.setHireDate(LocalDate.of(hireYear, hireMonth, hireDay));

            // Salary Information
            BigDecimal basicSalary = inputHandler.getBigDecimal("Enter basic salary:");
            employee.setBasicSalary(basicSalary);

            // Employment Type
            outputFormatter.printPlain("Select employment type:");
            outputFormatter.printPlain("1. Permanent");
            outputFormatter.printPlain("2. Contract");
            outputFormatter.printPlain("3. Temporary");
            int empTypeChoice = inputHandler.getInteger("Enter choice", 1, 3);
            switch (empTypeChoice) {
                case 1:
                    employee.setEmploymentType(Employee.EmploymentType.PERMANENT);
                    break;
                case 2:
                    employee.setEmploymentType(Employee.EmploymentType.CONTRACT);
                    break;
                case 3:
                    employee.setEmploymentType(Employee.EmploymentType.TEMPORARY);
                    break;
            }

            // Salary Type
            outputFormatter.printPlain("Select salary type:");
            outputFormatter.printPlain("1. Monthly");
            outputFormatter.printPlain("2. Weekly");
            outputFormatter.printPlain("3. Hourly");
            outputFormatter.printPlain("4. Daily");
            int salaryTypeChoice = inputHandler.getInteger("Enter choice", 1, 4);
            switch (salaryTypeChoice) {
                case 1:
                    employee.setSalaryType(Employee.SalaryType.MONTHLY);
                    break;
                case 2:
                    employee.setSalaryType(Employee.SalaryType.WEEKLY);
                    break;
                case 3:
                    employee.setSalaryType(Employee.SalaryType.HOURLY);
                    break;
                case 4:
                    employee.setSalaryType(Employee.SalaryType.DAILY);
                    break;
            }

            // Tax Information
            employee.setTaxNumber(inputHandler.getString("Enter tax number:"));

            // Optional Banking Information
            String addBanking = inputHandler.getString("Add banking information? (y/n):");
            if (addBanking.toLowerCase().startsWith("y")) {
                employee.setBankName(inputHandler.getString("Enter bank name:"));
                employee.setAccountHolderName(inputHandler.getString("Enter account holder name:"));
                employee.setAccountNumber(inputHandler.getString("Enter account number:"));
                employee.setBranchCode(inputHandler.getString("Enter branch code:"));
            }

            employee.setCreatedBy("system");

            Employee created = payrollService.createEmployee(employee);
            outputFormatter.printSuccess("Employee created successfully: " + created.getEmployeeNumber() + " - " + created.getFullName());

        } catch (Exception e) {
            outputFormatter.printError("Failed to create employee: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    private void updateEmployee(Long companyId) {
        outputFormatter.printHeader("Update Employee");

        try {
            // Get list of employees to choose from
            List<Employee> employees = payrollService.getActiveEmployees(companyId);
            if (employees.isEmpty()) {
                outputFormatter.printWarning("No active employees found.");
                inputHandler.waitForEnter();
                return;
            }

            outputFormatter.printPlain("Select employee to update:");
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                outputFormatter.printPlain((i + 1) + ". " + emp.getEmployeeNumber() + " - " + emp.getFullName());
            }

            int choice = inputHandler.getInteger("Enter employee number", 1, employees.size());
            Employee employee = employees.get(choice - 1);

            outputFormatter.printPlain("Current employee details:");
            outputFormatter.printPlain("Name: " + employee.getFullName());
            outputFormatter.printPlain("Position: " + employee.getPosition());
            outputFormatter.printPlain("Department: " + employee.getDepartment());
            outputFormatter.printPlain("Basic Salary: " + employee.getBasicSalary());
            outputFormatter.printPlain("Email: " + employee.getEmail());

            // Update fields
            String updateFirstName = inputHandler.getString("Enter new first name (leave empty to keep current):");
            if (!updateFirstName.trim().isEmpty()) {
                employee.setFirstName(updateFirstName);
            }

            String updateLastName = inputHandler.getString("Enter new last name (leave empty to keep current):");
            if (!updateLastName.trim().isEmpty()) {
                employee.setLastName(updateLastName);
            }

            String updateEmail = inputHandler.getString("Enter new email (leave empty to keep current):");
            if (!updateEmail.trim().isEmpty()) {
                employee.setEmail(updateEmail);
            }

            String updatePosition = inputHandler.getString("Enter new position (leave empty to keep current):");
            if (!updatePosition.trim().isEmpty()) {
                employee.setPosition(updatePosition);
            }

            String updateDepartment = inputHandler.getString("Enter new department (leave empty to keep current):");
            if (!updateDepartment.trim().isEmpty()) {
                employee.setDepartment(updateDepartment);
            }

            String updateSalaryStr = inputHandler.getString("Enter new basic salary (leave empty to keep current):");
            if (!updateSalaryStr.trim().isEmpty()) {
                try {
                    BigDecimal newSalary = new BigDecimal(updateSalaryStr);
                    employee.setBasicSalary(newSalary);
                } catch (NumberFormatException e) {
                    outputFormatter.printWarning("Invalid salary format, keeping current salary.");
                }
            }

            employee.setUpdatedBy("system");

            Employee updated = payrollService.updateEmployee(employee);
            outputFormatter.printSuccess("Employee updated successfully: " + updated.getEmployeeNumber() + " - " + updated.getFullName());

        } catch (Exception e) {
            outputFormatter.printError("Failed to update employee: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    private void deleteEmployee(Long companyId) {
        outputFormatter.printHeader("Delete Employee");

        try {
            // Get list of employees to choose from
            List<Employee> employees = payrollService.getActiveEmployees(companyId);
            if (employees.isEmpty()) {
                outputFormatter.printWarning("No active employees found.");
                inputHandler.waitForEnter();
                return;
            }

            outputFormatter.printPlain("Select employee to delete:");
            for (int i = 0; i < employees.size(); i++) {
                Employee emp = employees.get(i);
                outputFormatter.printPlain((i + 1) + ". " + emp.getEmployeeNumber() + " - " + emp.getFullName());
            }

            int choice = inputHandler.getInteger("Enter employee number", 1, employees.size());
            Employee employee = employees.get(choice - 1);

            outputFormatter.printWarning("Are you sure you want to delete employee: " + employee.getFullName() + "?");
            outputFormatter.printWarning("This will mark the employee as inactive. They can be reactivated later if needed.");
            String confirm = inputHandler.getString("Type 'DELETE' to confirm:");

            if ("DELETE".equals(confirm.toUpperCase())) {
                payrollService.deleteEmployee(employee.getId(), companyId);
                outputFormatter.printSuccess("Employee deleted successfully: " + employee.getEmployeeNumber() + " - " + employee.getFullName());
            } else {
                outputFormatter.printPlain("Delete operation cancelled.");
            }

        } catch (Exception e) {
            outputFormatter.printError("Failed to delete employee: " + e.getMessage());
        }

        inputHandler.waitForEnter();
    }

    private void handlePayrollPeriodManagement(Long companyId) {
        outputFormatter.printHeader("Payroll Period Management");
        boolean back = false;
        while (!back) {
            outputFormatter.printPlain("1. List Payroll Periods");
            outputFormatter.printPlain("2. Create Payroll Period");
            outputFormatter.printPlain("3. Back to Payroll Management");
            int choice = inputHandler.getInteger("Enter your choice", 1, 3);
            switch (choice) {
                case 1:
                    List<PayrollPeriod> periods = payrollService.getPayrollPeriods(companyId);
                    outputFormatter.printPlain("Payroll Periods:");
                    for (PayrollPeriod period : periods) {
                        outputFormatter.printPlain(period.getPeriodName() + " (" + period.getStartDate() + " to " + period.getEndDate() + ") - Status: " + period.getStatus());
                    }
                    inputHandler.waitForEnter();
                    break;
                case 2:
                    createPayrollPeriod(companyId);
                    break;
                case 3:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice.");
            }
        }
    }

    private void createPayrollPeriod(Long companyId) {
        outputFormatter.printHeader("Create Payroll Period");
        
        String periodName = inputHandler.getString("Enter period name (e.g., October 2025):");
        
        outputFormatter.printPlain("Enter start date:");
        int startYear = inputHandler.getInteger("Year", 2020, 2030);
        int startMonth = inputHandler.getInteger("Month", 1, 12);
        int startDay = inputHandler.getInteger("Day", 1, 31);
        
        outputFormatter.printPlain("Enter end date:");
        int endYear = inputHandler.getInteger("Year", 2020, 2030);
        int endMonth = inputHandler.getInteger("Month", 1, 12);
        int endDay = inputHandler.getInteger("Day", 1, 31);
        
        outputFormatter.printPlain("Enter pay date:");
        int payYear = inputHandler.getInteger("Year", 2020, 2030);
        int payMonth = inputHandler.getInteger("Month", 1, 12);
        int payDay = inputHandler.getInteger("Day", 1, 31);
        
        try {
            PayrollPeriod period = new PayrollPeriod();
            period.setCompanyId(companyId);
            period.setPeriodName(periodName);
            period.setStartDate(LocalDate.of(startYear, startMonth, startDay));
            period.setEndDate(LocalDate.of(endYear, endMonth, endDay));
            period.setPayDate(LocalDate.of(payYear, payMonth, payDay));
            period.setCreatedBy("system");
            
            PayrollPeriod created = payrollService.createPayrollPeriod(period);
            outputFormatter.printSuccess("Payroll period created: " + created.getPeriodName());
            
        } catch (Exception e) {
            outputFormatter.printError("Failed to create payroll period: " + e.getMessage());
        }
        
        inputHandler.waitForEnter();
    }

    private void handlePayrollProcessing(Long companyId) {
        outputFormatter.printHeader("Payroll Processing");
        
        try {
            List<PayrollPeriod> allPeriods = payrollService.getPayrollPeriods(companyId);
            List<PayrollPeriod> processPeriods = allPeriods.stream()
                .filter(PayrollPeriod::canBeProcessed)
                .toList();
            
            if (processPeriods.isEmpty()) {
                outputFormatter.printWarning("No open payroll periods available for processing.");
                outputFormatter.printPlain("All existing periods have been processed.");
                outputFormatter.printPlain("Create a new payroll period to process payroll.");
                inputHandler.waitForEnter();
                return;
            }
            
            outputFormatter.printPlain("Select payroll period to process:");
            for (int i = 0; i < processPeriods.size(); i++) {
                outputFormatter.printPlain((i + 1) + ". " + processPeriods.get(i).getPeriodName());
            }
            int periodChoice = inputHandler.getInteger("Enter period number", 1, processPeriods.size());
            PayrollPeriod selectedPeriod = processPeriods.get(periodChoice - 1);
            payrollService.processPayroll(selectedPeriod.getId(), "system");
            outputFormatter.printSuccess("Payroll processed for period: " + selectedPeriod.getPeriodName());
            
        } catch (Exception e) {
            outputFormatter.printError("Failed to process payroll: " + e.getMessage());
        }
        
        inputHandler.waitForEnter();
    }

    private void handlePayslipGeneration(Long companyId) {
        outputFormatter.printHeader("Payslip Generation");
        
        try {
            List<PayrollPeriod> periods = payrollService.getPayrollPeriods(companyId);
            if (periods.isEmpty()) {
                outputFormatter.printWarning("No payroll periods found");
                inputHandler.waitForEnter();
                return;
            }
            
            outputFormatter.printPlain("Select payroll period to generate PDFs for:");
            for (int i = 0; i < periods.size(); i++) {
                outputFormatter.printPlain((i + 1) + ". " + periods.get(i).getPeriodName() + 
                                         " (" + periods.get(i).getStatus() + ")");
            }
            
            int periodChoice = inputHandler.getInteger("Enter period number", 1, periods.size()) - 1;
            
            if (periodChoice >= 0 && periodChoice < periods.size()) {
                PayrollPeriod selectedPeriod = periods.get(periodChoice);
                payrollService.generatePayslipPdfs(selectedPeriod.getId());
                outputFormatter.printSuccess("PDF generation completed");
            } else {
                outputFormatter.printError("Invalid period selection");
            }
            
        } catch (Exception e) {
            outputFormatter.printError("Failed to generate payslip PDFs: " + e.getMessage());
        }
        
        inputHandler.waitForEnter();
    }

    private void handlePayrollReports(Long companyId) {
        outputFormatter.printHeader("Payroll Reports");
        boolean back = false;
        while (!back) {
            outputFormatter.printPlain("1. Generate Payroll Summary Report");
            outputFormatter.printPlain("2. Generate Employee Payroll Report");
            outputFormatter.printPlain("3. Back to Payroll Management");
            int choice = inputHandler.getInteger("Enter your choice", 1, 3);
            switch (choice) {
                case 1:
                    try {
                        payrollReportService.generatePayrollSummaryReport(companyId);
                        outputFormatter.printSuccess("Payroll summary report generated.");
                    } catch (Exception e) {
                        outputFormatter.printError("Failed to generate payroll summary report: " + e.getMessage());
                    }
                    inputHandler.waitForEnter();
                    break;
                case 2:
                    try {
                        payrollReportService.generateEmployeePayrollReport(companyId);
                        outputFormatter.printSuccess("Employee payroll report generated.");
                    } catch (Exception e) {
                        outputFormatter.printError("Failed to generate employee payroll report: " + e.getMessage());
                    }
                    inputHandler.waitForEnter();
                    break;
                case 3:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice.");
            }
        }
    }

    private void handleTaxCalculations(Long companyId) {
        outputFormatter.printHeader("Tax Calculations");
        // TODO: Implement tax calculations logic
        outputFormatter.printInfo("Tax calculations feature is under development.");
        inputHandler.waitForEnter();
    }

    private void handlePayrollSettings(Long companyId) {
        outputFormatter.printHeader("Payroll Settings");
        // TODO: Implement payroll settings logic
        outputFormatter.printInfo("Payroll settings feature is under development.");
        inputHandler.waitForEnter();
    }

    public void generateSimplePayslipPdf(Long payslipId) {
        outputFormatter.printHeader("Generate Simple Payslip PDF");
        
        try {
            // Generate simple PDF for the given payslip ID
            PdfPrintService pdfPrintService = new PdfPrintService();
            pdfPrintService.generateSimplePayslipPdf(payslipId.intValue());
            outputFormatter.printSuccess("Simple payslip PDF generated successfully.");
        } catch (Exception e) {
            outputFormatter.printError("Failed to generate simple payslip PDF: " + e.getMessage());
        }
        
        inputHandler.waitForEnter();
    }
}
