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