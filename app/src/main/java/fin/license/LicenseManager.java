/*
 * Copyright 2025 Sthwalo Nyoni
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
package fin.license;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Scanner;

/**
 * License verification and enforcement for FIN Financial Management System
 * Ensures compliance with dual licensing model
 */
public class LicenseManager {
    
    private static final String PERSONAL_USE_DISCLAIMER = 
        "══════════════════════════════════════════════════════════════════════════════════\n" +
        "🏠 FIN FINANCIAL MANAGEMENT SYSTEM - PERSONAL USE VERSION\n" +
        "══════════════════════════════════════════════════════════════════════════════════\n" +
        "\n" +
        "This software is licensed for PERSONAL USE ONLY under Apache License 2.0.\n" +
        "\n" +
        "✅ ALLOWED:\n" +
        "   • Personal finance management\n" +
        "   • Educational use and research\n" +
        "   • Non-commercial open source development\n" +
        "\n" +
        "❌ NOT ALLOWED WITHOUT COMMERCIAL LICENSE:\n" +
        "   • Business financial management\n" +
        "   • Commercial or revenue-generating activities\n" +
        "   • Hosting for other users or customers\n" +
        "   • Integration into commercial products\n" +
        "\n" +
        "💼 NEED COMMERCIAL LICENSE?\n" +
        "   • Starter: $29/month (small business)\n" +
        "   • Professional: $99/month (growing business)\n" +
        "   • Enterprise: $299/month (large organization)\n" +
        "   • Contact: licensing@fin-software.com\n" +
        "\n" +
        "⚖️  COPYRIGHT NOTICE:\n" +
        "   Copyright 2025 Sthwalo Nyoni. All rights reserved.\n" +
        "   Unauthorized commercial use is strictly prohibited.\n" +
        "\n" +
        "══════════════════════════════════════════════════════════════════════════════════\n";
    
    private static final String COMMERCIAL_LICENSE_PATH = "commercial.license";
    
    public static boolean checkLicenseCompliance() {
        System.out.println(PERSONAL_USE_DISCLAIMER);
        
        // Check for commercial license file
        if (hasCommercialLicense()) {
            return validateCommercialLicense();
        }
        
        // Personal use confirmation
        return confirmPersonalUse();
    }
    
    private static boolean hasCommercialLicense() {
        Path licensePath = Paths.get(COMMERCIAL_LICENSE_PATH);
        return Files.exists(licensePath);
    }
    
    private static boolean validateCommercialLicense() {
        try {
            Path licensePath = Paths.get(COMMERCIAL_LICENSE_PATH);
            String licenseContent = Files.readString(licensePath);
            
            // Simple validation (in production, this would be cryptographically signed)
            if (licenseContent.contains("VALID_COMMERCIAL_LICENSE") && 
                licenseContent.contains("Sthwalo Nyoni")) {
                
                System.out.println("✅ Valid commercial license detected.");
                System.out.println("   Licensed for commercial use.");
                return true;
            } else {
                System.out.println("❌ Invalid commercial license file.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("❌ Error reading commercial license: " + e.getMessage());
            return false;
        }
    }
    
    private static boolean confirmPersonalUse() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("📋 LICENSE AGREEMENT CONFIRMATION:");
            System.out.println();
            System.out.println("Are you using FIN for PERSONAL USE ONLY?");
            System.out.println("(Personal finance, education, or non-commercial development)");
            System.out.println();
            System.out.print("Type 'yes' to confirm personal use only, or 'no' to exit: ");
            
            String response = scanner.nextLine().trim().toLowerCase();
            
            if ("yes".equals(response)) {
                System.out.println("✅ Personal use confirmed. Starting FIN...");
                logPersonalUse();
                return true;
            } else {
                System.out.println();
                System.out.println("🚫 Commercial use requires a commercial license.");
                System.out.println("   Please visit: https://fin-licensing.com");
                System.out.println("   Or contact: licensing@fin-software.com");
                System.out.println();
                System.out.println("   Exiting application.");
                return false;
            }
        }
    }
    
    private static void logPersonalUse() {
        try {
            Path logPath = Paths.get("personal_use.log");
            String logEntry = LocalDate.now() + " - Personal use confirmed\n";
            Files.write(logPath, logEntry.getBytes(), 
                       java.nio.file.StandardOpenOption.CREATE,
                       java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Log write failure is not critical for functionality
            System.out.println("Note: Could not write usage log.");
        }
    }
    
    public static void showCommercialInfo() {
        System.out.println("\n💼 COMMERCIAL LICENSING INFORMATION:");
        System.out.println("────────────────────────────────────────");
        System.out.println("🥉 Starter License - $29/month");
        System.out.println("   • Up to 3 companies, 1,000 transactions");
        System.out.println("   • Perfect for small businesses");
        System.out.println();
        System.out.println("🥈 Professional License - $99/month");
        System.out.println("   • Up to 10 companies, 10,000 transactions");
        System.out.println("   • API access and priority support");
        System.out.println();
        System.out.println("🥇 Enterprise License - $299/month");
        System.out.println("   • Unlimited usage and phone support");
        System.out.println("   • Custom features and compliance reporting");
        System.out.println();
        System.out.println("📞 Contact: licensing@fin-software.com");
        System.out.println("🌐 Website: https://fin-licensing.com");
        System.out.println();
    }
}
