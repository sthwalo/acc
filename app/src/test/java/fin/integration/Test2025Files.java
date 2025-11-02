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

package fin.integration;

import fin.TestConfiguration;
import fin.service.CompanyService;
import fin.service.BankStatementProcessingService;
import fin.model.Company;
import fin.model.BankTransaction;
import java.io.File;
import java.util.List;

public class Test2025Files {
    
    public static void main(String[] args) {
        try {
            // Setup test database
            TestConfiguration.setupTestDatabase();

            String dbUrl = TestConfiguration.TEST_DB_URL + "?user=" + TestConfiguration.TEST_DB_USER + "&password=" + TestConfiguration.TEST_DB_PASSWORD;
            CompanyService companyService = new CompanyService(dbUrl);
            BankStatementProcessingService processor = new BankStatementProcessingService(dbUrl);
            
            List<Company> companies = companyService.getAllCompanies();
            if (companies.isEmpty()) {
                System.out.println("❌ No companies found in database!");
                return;
            }
            Company company = companies.get(0); // Use first company
            
            String[] files2025 = {
                "../input/xxxxx3753 (13).pdf",  // Jan-Feb 2025
                "../input/xxxxx3753 (14).pdf"   // Feb-Mar 2025
            };
            
            for (String filePath : files2025) {
                File file = new File(filePath);
                if (file.exists()) {
                    System.out.println("\n🔄 Processing: " + file.getName());
                    
                    List<BankTransaction> transactions = processor.processStatement(file.getAbsolutePath(), company);
                    System.out.println("✅ Extracted " + transactions.size() + " transactions");
                } else {
                    System.out.println("❌ File not found: " + filePath);
                }
            }
            
        } catch (Exception e) {
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
