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

package fin.cli;

import fin.service.OpeningBalanceService;
import fin.config.DatabaseConfig;

import java.math.BigDecimal;

/**
 * CLI tool to create opening balance journal entries for fiscal periods.
 * Usage: java fin.cli.CreateOpeningBalance <companyId> <fiscalPeriodId>
 */
public class CreateOpeningBalance {
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java fin.cli.CreateOpeningBalance <companyId> <fiscalPeriodId>");
            System.err.println("Example: java fin.cli.CreateOpeningBalance 2 7");
            System.exit(1);
        }
        
        try {
            Long companyId = Long.parseLong(args[0]);
            Long fiscalPeriodId = Long.parseLong(args[1]);
            
            System.out.println("╔════════════════════════════════════════════════════════════════╗");
            System.out.println("║        Opening Balance Creation Tool - October 6, 2025        ║");
            System.out.println("╚════════════════════════════════════════════════════════════════╝");
            System.out.println();
            
            // Test database connection
            System.out.println("🔍 Testing database connection...");
            if (!DatabaseConfig.testConnection()) {
                System.err.println("❌ Failed to connect to database");
                System.exit(1);
            }
            System.out.println("✅ Database connection successful");
            System.out.println();
            
            // Create service
            String dbUrl = DatabaseConfig.getDatabaseUrlWithCredentials();
            OpeningBalanceService service = new OpeningBalanceService(dbUrl);
            
            // Display parameters
            System.out.println("📋 Parameters:");
            System.out.println("   Company ID: " + companyId);
            System.out.println("   Fiscal Period ID: " + fiscalPeriodId);
            System.out.println();
            
            // Calculate closing balance for verification
            BigDecimal closingBalance = service.calculateClosingBalance(companyId, fiscalPeriodId);
            if (closingBalance != null) {
                System.out.println("💰 Expected Closing Balance: R " + String.format("%,.2f", closingBalance));
                System.out.println();
            }
            
            // Create opening balance entry
            System.out.println("🚀 Creating opening balance journal entry...");
            boolean success = service.createOpeningBalanceEntry(companyId, fiscalPeriodId, "system");
            
            System.out.println();
            if (success) {
                System.out.println("╔════════════════════════════════════════════════════════════════╗");
                System.out.println("║                    ✅ SUCCESS                                   ║");
                System.out.println("╚════════════════════════════════════════════════════════════════╝");
                System.out.println();
                System.out.println("Opening balance journal entry created successfully!");
                System.out.println();
                System.out.println("Next steps:");
                System.out.println("1. Run Trial Balance report to verify bank balance");
                System.out.println("2. Check that Bank Account (1100) shows correct balance");
                System.out.println("3. Verify Retained Earnings (5100) has opening balance");
                System.out.println();
                System.exit(0);
            } else {
                System.err.println("╔════════════════════════════════════════════════════════════════╗");
                System.err.println("║                    ❌ FAILED                                    ║");
                System.err.println("╚════════════════════════════════════════════════════════════════╝");
                System.err.println();
                System.err.println("Failed to create opening balance journal entry.");
                System.err.println("Check logs for more details.");
                System.err.println();
                System.exit(1);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("❌ Error: Invalid number format for companyId or fiscalPeriodId");
            System.err.println("Both parameters must be integers");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
