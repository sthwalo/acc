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

public class TestSinglePdfDates {
    public static void main(String[] args) {
        try {
            File file = new File("../input/xxxxx3753 (13).pdf");
            System.out.println("Testing file: " + file.getAbsolutePath());
            System.out.println("File exists: " + file.exists());
            
            if (file.exists()) {
                DocumentTextExtractor extractor = new DocumentTextExtractor();
                List<String> lines = extractor.parseDocument(file);
                
                System.out.println("Total lines: " + lines.size());
                System.out.println("Statement Period: " + extractor.getStatementPeriod());
                
                // Look for date patterns in first 20 lines
                System.out.println("\nFirst 20 lines:");
                for (int i = 0; i < Math.min(20, lines.size()); i++) {
                    System.out.println(i + ": " + lines.get(i));
                }
            } else {
                System.out.println("❌ File not found!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
