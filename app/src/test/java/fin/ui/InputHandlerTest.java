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

import java.io.*;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class InputHandlerTest {
    private InputHandler inputHandler;
    
    @Test
    void getString_WithInput_ReturnsInput() {
        String testInput = "Hello World\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            String result = inputHandler.getString("Enter text: ");
            assertEquals("Hello World", result);
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void getString_WithDefault_ReturnsDefaultOnEmptyInput() {
        String testInput = "\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            String result = inputHandler.getString("Enter text: ", "default");
            assertEquals("default", result);
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void getInteger_WithValidInput_ReturnsInteger() {
        String testInput = "42\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            int result = inputHandler.getInteger("Enter number: ");
            assertEquals(42, result);
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void getInteger_WithRange_AcceptsValidRange() {
        String testInput = "5\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            int result = inputHandler.getInteger("Enter number (1-10): ", 1, 10);
            assertEquals(5, result);
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void getBoolean_WithYes_ReturnsTrue() {
        String testInput = "y\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            boolean result = inputHandler.getBoolean("Continue? ");
            assertTrue(result);
        } finally {
            System.setOut(originalOut);
        }
    }
    
    @Test
    void getBoolean_WithNo_ReturnsFalse() {
        String testInput = "n\n";
        InputStream inputStream = new ByteArrayInputStream(testInput.getBytes());
        Scanner scanner = new Scanner(inputStream);
        
        inputHandler = new InputHandler(scanner);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        try {
            boolean result = inputHandler.getBoolean("Continue? ");
            assertFalse(result);
        } finally {
            System.setOut(originalOut);
        }
    }
}
