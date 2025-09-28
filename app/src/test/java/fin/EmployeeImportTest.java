package fin;

import fin.context.ApplicationContext;
import fin.model.Company;
import fin.model.PayrollPeriod;
import fin.service.PayrollService;
import fin.service.CompanyService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Simple test program to import employees and generate payslips
 */
public class EmployeeImportTest {

    public static void main(String[] args) {
        try {
            System.out.println("🚀 Starting Employee Import and Payslip Generation Test");
            System.out.println("===============================================");

            // Initialize application context
            ApplicationContext context = new ApplicationContext();

            // Get services
            CompanyService companyService = context.get(fin.service.CompanyService.class);
            PayrollService payrollService = context.get(PayrollService.class);

            // Get or create a company
            Company company = getOrCreateCompany(companyService);
            System.out.println("✅ Using company: " + company.getName() + " (ID: " + company.getId() + ")");

            // Import employees from TXT file
            String employeeFilePath = "input/EmployeesData.txt";
            System.out.println("📥 Importing employees from: " + employeeFilePath);
            int importedCount = payrollService.importEmployeesFromFile(employeeFilePath, company.getId());
            System.out.println("✅ Imported " + importedCount + " employees");

            // Create payroll period for current month with timestamp to avoid duplicates
            PayrollPeriod payrollPeriod = createCurrentMonthPayrollPeriod(payrollService, company.getId());
            System.out.println("✅ Created payroll period: " + payrollPeriod.getPeriodName() + " (ID: " + payrollPeriod.getId() + ")");

            // Process payroll
            System.out.println("💰 Processing payroll...");
            payrollService.processPayroll(payrollPeriod.getId(), "system");

            // Generate payslip PDFs
            System.out.println("📄 Generating payslip PDFs...");
            payrollService.generatePayslipPdfs(payrollPeriod.getId());

            System.out.println("🎉 Employee import and payslip generation completed successfully!");

            // Shutdown context
            context.shutdown();

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Company getOrCreateCompany(CompanyService companyService) {
        // Try to get existing company
        var companies = companyService.getAllCompanies();
        if (!companies.isEmpty()) {
            return companies.get(0);
        }

        // Create new company if none exists
        Company company = new Company("Test Company");
        return companyService.createCompany(company);
    }

    private static PayrollPeriod createCurrentMonthPayrollPeriod(PayrollService payrollService, Long companyId) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        // Include timestamp to make period name unique
        String timestamp = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        String periodName = now.format(DateTimeFormatter.ofPattern("MMMM yyyy")) + " " + timestamp;
        LocalDate payDate = endOfMonth; // Pay on the last day of the month

        PayrollPeriod period = new PayrollPeriod();
        period.setCompanyId(companyId);
        period.setFiscalPeriodId(4L); // Use Fiscal Period 3 - 2025 (2025-03-01 to 2026-02-28)
        period.setPeriodName(periodName);
        period.setStartDate(startOfMonth);
        period.setEndDate(endOfMonth);
        period.setPayDate(payDate);
        period.setPeriodType(PayrollPeriod.PeriodType.MONTHLY);
        period.setCreatedBy("system");

        return payrollService.createPayrollPeriod(period);
    }
}