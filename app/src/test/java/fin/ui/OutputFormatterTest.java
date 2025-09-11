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

class OutputFormatterTest {
    private OutputFormatter outputFormatter;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        outputFormatter = new OutputFormatter();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    void printInfo_PrintsWithInfoPrefix() {
        String message = "Test information message";
        
        outputFormatter.printInfo(message);
        
        String output = outputStream.toString();
        assertTrue(output.contains("ℹ️"), "Should contain info emoji");
        assertTrue(output.contains(message), "Should contain the message");
    }
    
    @Test
    void printSuccess_PrintsWithSuccessPrefix() {
        String message = "Operation successful";
        
        outputFormatter.printSuccess(message);
        
        String output = outputStream.toString();
        assertTrue(output.contains("✅"), "Should contain success emoji");
        assertTrue(output.contains(message), "Should contain the message");
    }
    
    @Test
    void printError_PrintsWithErrorPrefix() {
        String message = "Error occurred";
        
        outputFormatter.printError(message);
        
        String output = errorStream.toString(); // Check error stream instead
        assertTrue(output.contains("❌"), "Should contain error emoji");
        assertTrue(output.contains(message), "Should contain the message");
    }
    
    @Test
    void printWarning_PrintsWithWarningPrefix() {
        String message = "Warning message";
        
        outputFormatter.printWarning(message);
        
        String output = outputStream.toString();
        assertTrue(output.contains("⚠️"), "Should contain warning emoji");
        assertTrue(output.contains(message), "Should contain the message");
    }
    
    @Test
    void printHeader_PrintsFormattedHeader() {
        String title = "Test Header";
        
        outputFormatter.printHeader(title);
        
        String output = outputStream.toString();
        assertTrue(output.contains(title), "Should contain the title");
        assertTrue(output.contains("="), "Should contain header decoration");
    }
    
    @Test
    void printSeparator_PrintsSeparatorLine() {
        outputFormatter.printSeparator();
        
        String output = outputStream.toString();
        assertTrue(output.contains("─") || output.contains("-"), "Should contain separator characters");
    }
}
