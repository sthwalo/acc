package fin.test;

import fin.service.spring.SARSTaxCalculator;

public class TaxCalculationTest {

    public static void main(String[] args) {
        System.out.println("Starting TaxCalculationTest main method");
        try {
            // Create tax calculator directly (it will load tax tables in constructor)
            SARSTaxCalculator calculator = new SARSTaxCalculator();
            System.out.println("SARSTaxCalculator created");
            System.out.println("About to call initializeTaxTables...");
            try {
                calculator.initializeTaxTables(); // Manually initialize tax tables
                System.out.println("initializeTaxTables completed successfully.");
            } catch (Exception e) {
                System.err.println("initializeTaxTables failed: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("Tax brackets loaded: " + calculator.getTaxBrackets().size());
            
            // Print first few brackets to debug
            var brackets = calculator.getTaxBrackets();
            System.out.println("First 5 brackets:");
            for (int i = 0; i < Math.min(5, brackets.size()); i++) {
                System.out.println(brackets.get(i));
            }

            // Test gross salary of 10500
            double grossSalary = 10500.0;
            double totalCompanyPayroll = 500000.0; // Assume company payroll for SDL calculation

            System.out.println("=== TAX CALCULATION TEST ===");
            System.out.printf("Gross Salary: R%,.2f%n", grossSalary);
            System.out.printf("Total Company Payroll: R%,.2f%n", totalCompanyPayroll);
            System.out.println();

            // Calculate individual components
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

            // Show tax brackets loaded
            System.out.println("\n=== LOADED TAX BRACKETS ===");
            calculator.printAllBrackets();

        } catch (Exception e) {
            System.err.println("Error in tax calculation test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}