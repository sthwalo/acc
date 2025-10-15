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
package fin.ui;

import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleMenuTest {
    private ConsoleMenu menu;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        menu = new ConsoleMenu();
        System.setOut(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }
    
    @Test
    void displayMainMenu_PrintsAllOptions() {
        menu.displayMainMenu();
        
        String output = outputStream.toString();
        assertTrue(output.contains("FIN Application Menu"), "Should contain system title");
        assertTrue(output.contains("1. Company Setup"), "Should contain company setup option");
        assertTrue(output.contains("2. Fiscal Period Management"), "Should contain fiscal period option");
        assertTrue(output.contains("3. Import Bank Statement"), "Should contain import option");
        assertTrue(output.contains("11. Exit"), "Should contain exit option");
    }
    
    @Test
    void displayCompanyMenu_PrintsCompanyOptions() {
        menu.displayCompanyMenu();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Company Setup"), "Should contain company management title");
        assertTrue(output.contains("1. Create new company"), "Should contain create option");
        assertTrue(output.contains("2. Select existing company"), "Should contain select option");
        assertTrue(output.contains("3. View company details"), "Should contain manage option");
    }
    
    @Test
    void displayImportMenu_PrintsImportOptions() {
        menu.displayImportMenu();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Import Bank Statement"), "Should contain import title");
        assertTrue(output.contains("1. Import single bank statement"), "Should contain bank statement option");
        assertTrue(output.contains("2. Import multiple bank statements"), "Should contain CSV option");
    }
    
    @Test
    void displayReportMenu_PrintsReportOptions() {
        menu.displayReportMenu();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Financial Reports"), "Should contain report title");
        assertTrue(output.contains("1. Cashbook Report"), "Should contain financial statements option");
        assertTrue(output.contains("2. General Ledger Report"), "Should contain PDF export option");
    }
    
    @Test
    void displayDataManagementMenu_PrintsDataOptions() {
        menu.displayDataManagementMenu();
        
        String output = outputStream.toString();
        assertTrue(output.contains("Data Management"), "Should contain data management title");
        assertTrue(output.contains("3. Transaction Classification"), "Should contain classification option");
        assertTrue(output.contains("7. Export to CSV"), "Should contain export option");
    }
    
    @Test
    void displayHeader_PrintsFormattedHeader() {
        String title = "Test Header";
        
        menu.displayHeader(title);
        
        String output = outputStream.toString();
        assertTrue(output.contains(title), "Should contain the title");
        assertTrue(output.contains("="), "Should contain header formatting");
    }
    
    @Test
    void displaySeparator_PrintsSeparator() {
        menu.displaySeparator();
        
        String output = outputStream.toString();
        assertTrue(output.contains("-"), "Should contain separator formatting");
    }
}
