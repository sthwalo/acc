package fin.controller;

import fin.service.PayrollService;
import fin.model.Employee;
import fin.model.PayrollPeriod;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;

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
            outputFormatter.printPlain("3. Process Payroll");
            outputFormatter.printPlain("4. Back to main menu");
            int choice = inputHandler.getInteger("Enter your choice", 1, 4);
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
                        outputFormatter.printPlain(period.getPeriodName() + " (" + period.getStartDate() + " to " + period.getEndDate() + ")");
                    }
                    inputHandler.waitForEnter();
                    break;
                case 3:
                    List<PayrollPeriod> processPeriods = payrollService.getPayrollPeriods(companyId);
                    if (processPeriods.isEmpty()) {
                        outputFormatter.printWarning("No payroll periods found.");
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
                case 4:
                    back = true;
                    break;
                default:
                    outputFormatter.printError("Invalid choice.");
            }
        }
    }
}
