package fin;

import fin.service.SARSTaxCalculator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PAYETest {
    public static void main(String[] args) {
        try {
            SARSTaxCalculator calculator = new SARSTaxCalculator();
            calculator.loadTaxTablesFromPDFText("input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt");

            calculator.printAllBrackets();

            // Read employee data from file
            List<String> lines = Files.readAllLines(Paths.get("input/EmployeesData.txt"));
            boolean isFirstLine = true;
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("CALCULATING PAYE FOR ALL EMPLOYEES");
            System.out.println("=".repeat(80));
            
            for (String line : lines) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip header
                    continue;
                }
                
                if (line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\t");
                if (parts.length >= 45) { // Fixed Salary is the last column
                    try {
                        String employeeCode = parts[0];
                        String surname = parts[1];
                        String firstName = parts[6];
                        String salaryStr = parts[parts.length - 1].trim();
                        
                        // Handle South African number format: remove spaces, replace comma with dot
                        salaryStr = salaryStr.replaceAll("\\s+", "").replace(",", ".");
                        
                        if (!salaryStr.isEmpty()) {
                            double grossSalary = Double.parseDouble(salaryStr);
                            
                            System.out.println("\n" + "-".repeat(60));
                            System.out.printf("Employee: %s %s %s (Code: %s)%n", firstName, parts[7], surname, employeeCode);
                            
                            var result = calculator.calculateNetPay(grossSalary);
                            calculator.printCalculation(result);
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing salary from line: " + line);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Error loading tax tables: " + e.getMessage());
        }
    }
}