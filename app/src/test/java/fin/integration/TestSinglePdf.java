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

/*
 * Test single PDF processor - processes one PDF to debug
 */
package fin.integration;

import fin.TestConfiguration;
import fin.service.BankStatementProcessingService;
import fin.service.CompanyService;
import fin.model.Company;
import fin.model.BankTransaction;
import java.io.File;
import java.util.List;

public class TestSinglePdf {
    
    public static void main(String[] args) {
        try {
            // Setup test database
            TestConfiguration.setupTestDatabase();

            System.out.println("🔍 Single PDF Test");
            System.out.println("==================");
            
            // Initialize services
            CompanyService companyService = new CompanyService(TestConfiguration.TEST_DB_URL + "?user=" + TestConfiguration.TEST_DB_USER + "&password=" + TestConfiguration.TEST_DB_PASSWORD);
            BankStatementProcessingService bankService = new BankStatementProcessingService(TestConfiguration.TEST_DB_URL + "?user=" + TestConfiguration.TEST_DB_USER + "&password=" + TestConfiguration.TEST_DB_PASSWORD);
            
            // Get the first company
            List<Company> companies = companyService.getAllCompanies();
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found. Please create a company first.");
                return;
            }
            
            Company company = companies.get(0);
            System.out.println("✅ Using company: " + company.getName());
            
            // Test a single PDF that contains transactions from March 2024 onwards
            File testFile = new File("../input/xxxxx3753 (06).pdf"); // Try March statement
            if (!testFile.exists()) {
                testFile = new File("../input/xxxxx3753 (07).pdf"); // Try April statement
            }
            if (!testFile.exists()) {
                testFile = new File("../input/xxxxx3753 (08).pdf"); // Try May statement
            }
            
            if (!testFile.exists()) {
                System.out.println("❌ Test file not found");
                return;
            }
            
            System.out.println("📄 Testing file: " + testFile.getName());
            
            // Process the PDF
            List<BankTransaction> transactions = bankService.processStatement(
                testFile.getAbsolutePath(), 
                company
            );
            
            System.out.println("✅ Successfully processed " + transactions.size() + " transactions");
            
            // Show some sample transactions
            if (!transactions.isEmpty()) {
                System.out.println("\n📊 Sample transactions:");
                for (int i = 0; i < Math.min(5, transactions.size()); i++) {
                    BankTransaction tx = transactions.get(i);
                    System.out.printf("  %s | %s | %.2f%n", 
                        tx.getTransactionDate(), 
                        tx.getDetails().substring(0, Math.min(50, tx.getDetails().length())),
                        tx.getDebitAmount() != null ? tx.getDebitAmount().doubleValue() : 
                        tx.getCreditAmount() != null ? tx.getCreditAmount().doubleValue() : 0.0);
                }
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }

        // Cleanup test database
        try {
            TestConfiguration.cleanupTestDatabase();
        } catch (Exception e) {
            System.err.println("Error cleaning up test database: " + e.getMessage());
        }
    }
}
