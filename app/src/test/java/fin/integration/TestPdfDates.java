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

import fin.service.DocumentTextExtractor;
import java.io.File;
import java.util.List;

public class TestPdfDates {
    public static void main(String[] args) {
        try {
            String[] testFiles = {
                "input/xxxxx3753 (02).pdf",  // Likely Feb 2024 or Jan 2025?
                "input/xxxxx3753 (13).pdf",  // Likely Jan 2025?
                "input/xxxxx3753 (14).pdf"   // Likely Feb 2025?
            };
            
            for (String filePath : testFiles) {
                File file = new File(filePath);
                if (file.exists()) {
                    System.out.println("\n=== " + filePath + " ===");
                    
                    DocumentTextExtractor extractor = new DocumentTextExtractor();
                    List<String> lines = extractor.parseDocument(file);
                    
                    // Look for statement period
                    String statementPeriod = extractor.getStatementPeriod();
                    System.out.println("Statement Period: " + statementPeriod);
                    
                    // Look for dates in the first 50 lines
                    System.out.println("First 50 lines:");
                    for (int i = 0; i < Math.min(50, lines.size()); i++) {
                        String line = lines.get(i);
                        if (line.contains("2024") || line.contains("2025") || line.contains("Jan") || line.contains("Feb")) {
                            System.out.println((i+1) + ": " + line);
                        }
                    }
                    System.out.println("=".repeat(50));
                } else {
                    System.out.println("❌ File not found: " + filePath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
