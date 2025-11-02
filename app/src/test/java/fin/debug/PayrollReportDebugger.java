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

package fin.debug;

import fin.service.PayrollReportService;
import org.junit.jupiter.api.Test;

/**
 * Simple debug test to see PayrollReportService debugging output
 * This helps diagnose why payroll reports show R0.00 values for company 1
 */
public class PayrollReportDebugger {
    
    @Test
    public void debugPayrollReport() {
        try {
            System.out.println("🧪 Debugging PayrollReportService for Company 1 (Limelight Academy)");
            System.out.println("=".repeat(80));
            
            // Use production database URL
            String dbUrl = System.getenv("DATABASE_URL");
            if (dbUrl == null) {
                dbUrl = "jdbc:postgresql://localhost:5432/drimacc_db";
            }
            
            // Add credentials
            String dbUser = System.getenv("DATABASE_USER");
            String dbPassword = System.getenv("DATABASE_PASSWORD");
            if (dbUser != null && dbPassword != null) {
                String separator = dbUrl.contains("?") ? "&" : "?";
                dbUrl = dbUrl + separator + "user=" + dbUser + "&password=" + dbPassword;
            }
            
            System.out.println("🔗 Database URL: " + dbUrl.replaceAll("password=[^&]*", "password=***"));
            
            // Initialize service
            PayrollReportService service = new PayrollReportService(dbUrl);
            
            System.out.println("📊 Testing payroll summary report generation...");
            System.out.println("-".repeat(80));
            
            // This should show the debugging output we added
            service.generatePayrollSummaryReport(1L);
            
            System.out.println("-".repeat(80));
            System.out.println("✅ Debug test completed!");
            
        } catch (Exception e) {
            System.err.println("❌ Error during debugging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}