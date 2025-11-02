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