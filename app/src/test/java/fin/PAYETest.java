/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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