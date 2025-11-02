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

package fin.service;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class BudgetReportServiceIntegrationTest {

    @Test
    void testGenerateBudgetSummaryReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return; // Skip test if no .env file
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateBudgetSummaryReport(companyId),
            "Budget summary report generation should complete without throwing exceptions");

        System.out.println("✅ Budget summary report generation test passed (PDF generation skipped in test mode)");
    }

    @Test
    void testGenerateStrategicPlanReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return;
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateStrategicPlanReport(companyId),
            "Strategic plan report generation should complete without throwing exceptions");

        System.out.println("✅ Strategic plan report generation test passed (PDF generation skipped in test mode)");
    }

    @Test
    void testGenerateBudgetVsActualReportWithRealData() throws Exception {
        // Load environment variables
        Properties env = new Properties();
        try {
            env.load(Files.newBufferedReader(Paths.get(".env")));
        } catch (IOException e) {
            System.out.println("Warning: Could not load .env file: " + e.getMessage());
            return;
        }

        String dbUrl = env.getProperty("DATABASE_URL");
        if (dbUrl == null) {
            System.out.println("Skipping test: DATABASE_URL not found in .env");
            return;
        }

        BudgetReportService service = new BudgetReportService(dbUrl);

        // Test with company ID 1 (Limelight Academy)
        Long companyId = 1L;

        // In test mode, PDF generation is skipped - just verify method completes without exception
        assertDoesNotThrow(() -> service.generateBudgetVsActualReport(companyId),
            "Budget vs actual report generation should complete without throwing exceptions");

        System.out.println("✅ Budget vs actual report generation test passed (PDF generation skipped in test mode)");
    }
}