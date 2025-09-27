package fin.controller;

import fin.service.PayrollService;
import fin.service.PdfPrintService;
import fin.model.Employee;
import fin.model.PayrollPeriod;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

import java.time.LocalDate;
import java.util.List;

public class PayrollController {
    private final PayrollService payrollService;
    private final InputHandler inputHandler;
    private final OutputFormatter outputFormatter;

    public PayrollController(PayrollService payrollService, InputHandler inputHandler, OutputFormatter outputFormatter) {
        this.payrollService = payrollService;
        this.inputHandler = inputHandler;
        this.outputFormatter = outputFormatter;
    }

    public void handlePayrollManagement(Long companyId) {
        outputFormatter.printHeader("Payroll Management");
        boolean back = false;
        while (!back) {
            outputFormatter.printPlain("1. List Employees");
            outputFormatter.printPlain("2. List Payroll Periods");
            outputFormatter.printPlain("3. Create Payroll Period");
            outputFormatter.printPlain("4. Process Payroll");
            outputFormatter.printPlain("5. Generate Payslip PDFs");
            outputFormatter.printPlain("6. Back to main menu");
            int choice = inputHandler.getInteger("Enter your choice", 1, 6);
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
                    List<PayrollPeriod> periods = payrollService.getPayrollPeriods(companyId);
                    outputFormatter.printPlain("Payroll Periods:");
                    for (PayrollPeriod period : periods) {
                        outputFormatter.printPlain(period.getPeriodName() + " (" + period.getStartDate() + " to " + period.getEndDate() + ") - Status: " + period.getStatus());
                    }
                    inputHandler.waitForEnter();
                    break;
                case 3:
                    createPayrollPeriod(companyId);
                    break;
                case 4:
                    List<PayrollPeriod> allPeriods = payrollService.getPayrollPeriods(companyId);
                    List<PayrollPeriod> processPeriods = allPeriods.stream()
                        .filter(PayrollPeriod::canBeProcessed)
                        .toList();
                    
                    if (processPeriods.isEmpty()) {
                        outputFormatter.printWarning("No open payroll periods available for processing.");
                        outputFormatter.printPlain("All existing periods have been processed.");
                        outputFormatter.printPlain("Create a new payroll period to process payroll.");
                        inputHandler.waitForEnter();
                        break;
                    }
                    outputFormatter.printPlain("Select payroll period to process:");
                    for (int i = 0; i < processPeriods.size(); i++) {
                        outputFormatter.printPlain((i + 1) + ". " + processPeriods.get(i).getPeriodName());
                    }
                    int periodChoice = inputHandler.getInteger("Enter period number", 1, processPeriods.size());
                    PayrollPeriod selectedPeriod = processPeriods.get(periodChoice - 1);
                    payrollService.processPayroll(selectedPeriod.getId(), "system");
                    outputFormatter.printSuccess("Payroll processed for period: " + selectedPeriod.getPeriodName());
                    inputHandler.waitForEnter();
                    break;
                case 5:
                    generatePayslipPdfs(companyId);
                    break;
                case 6:
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

    private void generatePayslipPdfs(Long companyId) {
        outputFormatter.printHeader("Generate Payslip PDFs");
        
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
