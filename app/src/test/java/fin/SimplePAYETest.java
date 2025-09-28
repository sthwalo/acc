package fin;
import fin.service.SARSTaxCalculator;
import java.io.IOException;

public class SimplePAYETest {
    public static void main(String[] args) {
        try {
            SARSTaxCalculator calculator = new SARSTaxCalculator();
            calculator.loadTaxTablesFromPDFText("input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt");

            // Print some brackets to debug
            calculator.printAllBrackets();

            // Test the specific example from user: R25,000 gross should show which bracket it falls into
            double grossSalary = 25000.00;
            double paye = calculator.findPAYE(grossSalary);

            System.out.printf("PAYE for R%,.2f gross salary: R%,.2f%n", grossSalary, paye);

            if (Math.abs(paye - 3018.00) < 0.01) {
                System.out.println("✅ PAYE calculation is CORRECT!");
            } else {
                System.out.println("❌ PAYE calculation is INCORRECT!");
            }

            // Also test the full net pay calculation
            var netPayResult = calculator.calculateNetPay(grossSalary);
            calculator.printCalculation(netPayResult);

        } catch (IOException e) {
            System.err.println("Error loading tax tables: " + e.getMessage());
        }
    }
}