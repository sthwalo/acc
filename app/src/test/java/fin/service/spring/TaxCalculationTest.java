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

package fin.service.spring;

import java.io.IOException;

public class TaxCalculationTest {
    public static void main(String[] args) {
        System.out.println("=== SARS Tax Calculator Test ===");
        System.out.println("Testing UIF calculation for R25,000 gross salary");

        SARSTaxCalculator calculator = new SARSTaxCalculator();

        try {
            // Load tax tables
            String pdfTextPath = "../input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt";
            calculator.loadTaxTablesFromPDFText(pdfTextPath);

            // Test R25,000 salary
            double grossSalary = 25000.0;
            double totalCompanyPayroll = 50000.0; // Below SDL threshold

            double paye = calculator.findPAYE(grossSalary);
            double uif = calculator.calculateUIF(grossSalary);
            double sdl = calculator.calculateSDL(grossSalary, totalCompanyPayroll);
            double netPay = grossSalary - paye - uif - sdl;

            System.out.println("\n=== UIF CALCULATION DETAILS ===");
            System.out.printf("Gross Salary: R%,.2f%n", grossSalary);
            System.out.println("UIF Threshold: R17,712.00");
            System.out.println("UIF Rate: 1%");
            System.out.println("UIF Cap: R177.12");
            System.out.println();
            System.out.println("Since R25,000 > R17,712, UIF is capped at maximum:");
            System.out.printf("UIF: R%,.2f (CAPPED)%n", uif);

            System.out.println("\n=== COMPLETE TAX CALCULATION RESULTS ===");
            System.out.printf("Gross Salary: R%,.2f%n", grossSalary);
            System.out.printf("PAYE Tax: R%,.2f%n", paye);
            System.out.printf("UIF: R%,.2f%n", uif);
            System.out.printf("SDL: R%,.2f%n", sdl);
            System.out.printf("Net Pay: R%,.2f%n", netPay);

        } catch (IOException e) {
            System.err.println("Failed to load tax tables: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Calculation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}