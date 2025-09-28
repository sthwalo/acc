package fin;

import fin.service.SARSTaxCalculator;
import java.util.*;

public class TaxCalculatorTestApp {
    public static void main(String[] args) {
        try {
            SARSTaxCalculator calculator = new SARSTaxCalculator();

            // Load tax tables from your pdftotext output
            String pdfTextPath = "input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt";
            calculator.loadTaxTablesFromPDFText(pdfTextPath);

            // Test calculations with different salary amounts
            double[] testSalaries = {15000.0, 20000.0, 23204.0, 25000.0, 35000.0, 50000.0, 200000.0};

            for (double salary : testSalaries) {
                Map<String, Double> result = calculator.calculateNetPay(salary);
                calculator.printCalculation(result);
            }

            // Show first 10 brackets for verification
            System.out.println("\nFIRST 10 TAX BRACKETS:");
            System.out.println("=".repeat(50));
            List<SARSTaxCalculator.TaxBracket> brackets = calculator.getTaxBrackets();
            for (int i = 0; i < Math.min(10, brackets.size()); i++) {
                System.out.println(brackets.get(i));
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}