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

import fin.service.upload.DocumentTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test PDF text extraction to debug Absa bank statement parsing issues.
 * This test helps identify why 0 transactions are being extracted from 324 processed lines.
 */
class PdfTextExtractionTest {

    @Test
    void testAbsaPdfTextExtraction() throws Exception {
        // Path to the Absa PDF file
        File pdfFile = new File("../input/Rock Absa/4068820115.pdf");
        
        if (!pdfFile.exists()) {
            System.err.println("⚠️  PDF file not found at: " + pdfFile.getAbsolutePath());
            System.err.println("Please ensure the file exists before running this test.");
            return;
        }

        System.out.println("📄 Testing PDF: " + pdfFile.getName());
        System.out.println("📍 Path: " + pdfFile.getAbsolutePath());
        System.out.println("==========================================\n");

        // Extract text using DocumentTextExtractor
        DocumentTextExtractor extractor = new DocumentTextExtractor();
        List<String> lines = extractor.parseDocument(pdfFile);

        System.out.println("✅ Extraction completed!");
        System.out.println("📊 Total lines extracted: " + lines.size());
        System.out.println("🏦 Account Number: " + extractor.getAccountNumber());
        System.out.println("📅 Statement Period: " + extractor.getStatementPeriod());
        
        // Analyze line lengths
        int shortLines = 0;
        int mediumLines = 0;
        int longLines = 0;
        for (String line : lines) {
            int len = line.trim().length();
            if (len < 10) shortLines++;
            else if (len < 50) mediumLines++;
            else longLines++;
        }
        System.out.println("\n📏 Line Length Analysis:");
        System.out.println("  Short (<10 chars): " + shortLines);
        System.out.println("  Medium (10-50 chars): " + mediumLines);
        System.out.println("  Long (>50 chars): " + longLines);
        System.out.println("  Percentage short: " + (100.0 * shortLines / lines.size()) + "%");
        
        System.out.println("\n==========================================\n");

        // Display first 50 lines
        System.out.println("📋 FIRST 100 LINES:\n");
        for (int i = 0; i < Math.min(100, lines.size()); i++) {
            String line = lines.get(i);
            // Truncate very long lines for display
            if (line.length() > 150) {
                System.out.printf("%3d: %s... (truncated, total length: %d)%n", i + 1, line.substring(0, 150), line.length());
            } else {
                System.out.printf("%3d: %s%n", i + 1, line);
            }
        }

        // Display lines with dates (transaction candidates)
        System.out.println("\n==========================================");
        System.out.println("📅 LINES WITH DATES (DD/MM/YYYY format):\n");
        int dateLineCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches(".*\\d{2}/\\d{2}/\\d{4}.*")) {
                System.out.printf("%3d: %s%n", i + 1, line);
                dateLineCount++;
            }
        }
        System.out.println("\n📊 Total lines with dates: " + dateLineCount);

        // Display lines with monetary amounts
        System.out.println("\n==========================================");
        System.out.println("💰 LINES WITH AMOUNTS (pattern: digits.digits):\n");
        int amountLineCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches(".*\\d{1,3}(,?\\d{3})*\\.\\d{2}.*") && line.length() > 5 && line.length() < 200) {
                System.out.printf("%3d: %s%n", i + 1, line);
                amountLineCount++;
                if (amountLineCount >= 30) {
                    System.out.println("... (showing first 30 amount lines)");
                    break;
                }
            }
        }
        System.out.println("\n📊 Total lines with amounts: " + amountLineCount);

        // Analyze line patterns for Absa format
        System.out.println("\n==========================================");
        System.out.println("🔍 ANALYZING ABSA FORMAT PATTERNS:\n");
        System.out.println("Expected Absa format: Date | Transaction Description | Charge | Debit Amount | Credit Amount | Balance");
        System.out.println("\nLooking for pipe-separated lines:\n");
        int pipeLineCount = 0;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("|")) {
                System.out.printf("%3d: %s%n", i + 1, line);
                pipeLineCount++;
                if (pipeLineCount >= 10) {
                    System.out.println("... (showing first 10 pipe-separated lines)");
                    break;
                }
            }
        }
        System.out.println("\n📊 Total pipe-separated lines: " + pipeLineCount);

        // Look for "Balance Brought Forward" pattern
        System.out.println("\n==========================================");
        System.out.println("🔍 SEARCHING FOR 'BALANCE BROUGHT FORWARD':\n");
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).toLowerCase();
            if (line.contains("balance") && (line.contains("brought") || line.contains("forward") || line.contains("opening"))) {
                System.out.printf("%3d: %s%n", i + 1, lines.get(i));
            }
        }

        System.out.println("\n==========================================");
        System.out.println("✅ Test completed! Review the output above to identify the actual PDF format.");
        System.out.println("==========================================\n");

        // Assertions
        assertNotNull(lines, "Extracted lines should not be null");
        assertTrue(lines.size() > 0, "Should extract at least one line");
    }

    @Test
    void testAbsaParserPatternMatching() {
        System.out.println("\n🧪 TESTING ABSA PARSER REGEX PATTERNS:\n");
        System.out.println("==========================================\n");

        // Test patterns from AbsaBankParser
        String[] testLines = {
            "01/11/2024 | SALARY DEPOSIT | 0.00 | 0.00 | 15000.00 | 15000.00",
            "02/11/2024 | ATM WITHDRAWAL | 10.00 | 500.00 | 0.00 | 14490.00",
            "Balance Brought Forward | 1000.00",
            "01/11/2024|SALARY DEPOSIT|0.00|0.00|15000.00|15000.00",
            "01 Nov 2024 SALARY DEPOSIT 0.00 0.00 15000.00 15000.00"
        };

        // Pattern 1: Pipe-separated with date
        String pattern1 = "\\d{2}/\\d{2}/\\d{4}\\s*\\|.*";
        System.out.println("Pattern 1 (Pipe-separated with date): " + pattern1);
        for (String line : testLines) {
            boolean matches = line.matches(pattern1);
            System.out.printf("  %s: %s%n", matches ? "✅" : "❌", line);
        }

        // Pattern 2: Balance Brought Forward
        String pattern2 = "(?i)balance\\s+brought\\s+forward.*";
        System.out.println("\nPattern 2 (Balance Brought Forward): " + pattern2);
        for (String line : testLines) {
            boolean matches = line.matches(pattern2);
            System.out.printf("  %s: %s%n", matches ? "✅" : "❌", line);
        }

        // Pattern 3: Space-separated with date
        String pattern3 = "\\d{2}\\s+[A-Za-z]{3}\\s+\\d{4}\\s+.*";
        System.out.println("\nPattern 3 (Space-separated with date): " + pattern3);
        for (String line : testLines) {
            boolean matches = line.matches(pattern3);
            System.out.printf("  %s: %s%n", matches ? "✅" : "❌", line);
        }

        System.out.println("\n==========================================\n");
    }
}
