import fin.service.spring.SARSTaxCalculator;
import java.util.Map;

public class TaxTest {
    public static void main(String[] args) {
        try {
            SARSTaxCalculator calculator = new SARSTaxCalculator();

            // Initialize tax tables
            calculator.initializeTaxTables();

            double grossSalary = 25000;
            double totalCompanyPayroll = 50000.0; // For SDL calculation

            System.out.println("=== TAX CALCULATION TEST ===");
            System.out.printf("Gross Salary: R%,.2f%n", grossSalary);
            System.out.printf("Total Company Payroll: R%,.2f%n", totalCompanyPayroll);
            System.out.println();

            // Calculate components
            double paye = calculator.findPAYE(grossSalary);
            double uif = calculator.calculateUIF(grossSalary);
            double sdl = calculator.calculateSDL(grossSalary, totalCompanyPayroll);

            double totalDeductions = paye + uif + sdl;
            double netSalary = grossSalary - totalDeductions;

            System.out.println("=== TAX BREAKDOWN ===");
            System.out.printf("PAYE (Income Tax): R%,.2f%n", paye);
            System.out.printf("UIF (Unemployment Insurance): R%,.2f%n", uif);
            System.out.printf("SDL (Skills Development Levy): R%,.2f%n", sdl);
            System.out.println();
            System.out.printf("Total Deductions: R%,.2f%n", totalDeductions);
            System.out.printf("Net Salary: R%,.2f%n", netSalary);

            // Show loaded brackets
            System.out.println("\n=== TAX BRACKETS LOADED ===");
            System.out.println("Number of brackets: " + calculator.getTaxBrackets().size());

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}