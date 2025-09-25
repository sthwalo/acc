package fin.test;

import fin.service.PayrollService;

/**
 * Simple test class to run payroll processing with PDF generation and email sending
 * for Limelight Academy employees.
 */
public class PayrollTest {

    public static void main(String[] args) {
        System.out.println("🏢 FIN PAYROLL TEST - PDF & EMAIL GENERATION");
        System.out.println("============================================");

        try {
            // Database connection URL
            String dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";

            // Create PayrollService
            PayrollService payrollService = new PayrollService(dbUrl);

            System.out.println("🏫 Processing payroll for Limelight Academy (Company ID: 3)");
            System.out.println("📅 Payroll Period: March 2024 (Period ID: 1)");

            // Process payroll - this should generate PDFs and send emails
            System.out.println("📊 Processing payroll with PDF generation and email sending...");
            payrollService.processPayroll(1L, "PayrollTest");

            System.out.println("✅ Payroll processing completed!");
            System.out.println("📄 PDFs should be generated in reports/payslips/ directory");
            System.out.println("📧 Emails should be sent to employee email addresses");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}