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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.io.File;

/**
 * Input handling and validation component
 * Extracted from monolithic App.java to separate input concerns
 */
public class InputHandler {
    private final Scanner scanner;
    
    /**
     * Exception thrown when user cancels an input operation
     */
    public static class InputCancelledException extends RuntimeException {
        public InputCancelledException(String message) {
            super(message);
        }
    }
    
    public InputHandler(Scanner initialScanner) {
        this.scanner = initialScanner;
    }
    
    public String getString(String prompt) {
        System.out.print(prompt + ": ");
        
        // Check if there's input available before trying to read
        if (!scanner.hasNextLine()) {
            System.out.println("\n❌ No input available. Exiting application...");
            System.exit(0);
        }
        
        return scanner.nextLine().trim();
    }
    
    public String getString(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        
        // Check if there's input available before trying to read
        if (!scanner.hasNextLine()) {
            System.out.println("\n❌ No input available. Using default value: " + defaultValue);
            return defaultValue;
        }
        
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    public String getOptionalString(String prompt) {
        System.out.print(prompt + " (optional): ");
        
        // Check if there's input available before trying to read
        if (!scanner.hasNextLine()) {
            System.out.println("\n❌ No input available. Using empty value.");
            return null;
        }
        
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }
    
    public int getInteger(String prompt) {
        return getInteger(prompt, "cancel");
    }
    
    public int getInteger(String prompt, String cancelKeyword) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                
                // Check if there's input available before trying to read
                if (!scanner.hasNextLine()) {
                    System.out.println("\n❌ No input available. Exiting application...");
                    System.exit(0);
                }
                
                String input = scanner.nextLine().trim();
                
                // Check for cancellation
                if (cancelKeyword != null && cancelKeyword.equalsIgnoreCase(input)) {
                    throw new InputCancelledException("Operation cancelled by user");
                }
                
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number or '" + cancelKeyword + "' to cancel.");
            }
        }
    }
    
    public int getInteger(String prompt, int min, int max) {
        while (true) {
            int value = getInteger(prompt + " (" + min + "-" + max + ")");
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Value must be between " + min + " and " + max + ".");
        }
    }
    
    public LocalDate getDate(String prompt) {
        return getDate(prompt, "cancel");
    }
    
    public LocalDate getDate(String prompt, String cancelKeyword) {
        while (true) {
            try {
                System.out.print(prompt + " (DD/MM/YYYY): ");
                
                // Check if there's input available before trying to read
                if (!scanner.hasNextLine()) {
                    System.out.println("\n❌ No input available. Exiting application...");
                    System.exit(0);
                }
                
                String dateStr = scanner.nextLine().trim();
                
                // Check for cancellation
                if (cancelKeyword != null && cancelKeyword.equalsIgnoreCase(dateStr)) {
                    throw new InputCancelledException("Operation cancelled by user");
                }
                
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use DD/MM/YYYY or '" + cancelKeyword + "' to cancel.");
            }
        }
    }
    
    public LocalDate getDate(String prompt, LocalDate minDate) {
        while (true) {
            LocalDate date = getDate(prompt);
            if (minDate == null || !date.isBefore(minDate)) {
                return date;
            }
            System.out.println("Date must be after " + minDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
    }
    
    public BigDecimal getBigDecimal(String prompt) {
        return getBigDecimal(prompt, "cancel");
    }
    
    public BigDecimal getBigDecimal(String prompt, String cancelKeyword) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                
                // Check if there's input available before trying to read
                if (!scanner.hasNextLine()) {
                    System.out.println("\n❌ No input available. Exiting application...");
                    System.exit(0);
                }
                
                String input = scanner.nextLine().trim();
                
                // Check for cancellation
                if (cancelKeyword != null && cancelKeyword.equalsIgnoreCase(input)) {
                    throw new InputCancelledException("Operation cancelled by user");
                }
                
                return new BigDecimal(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number or '" + cancelKeyword + "' to cancel.");
            }
        }
    }
    
    public boolean getBoolean(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            
            // Check if there's input available before trying to read
            if (!scanner.hasNextLine()) {
                System.out.println("\n❌ No input available. Defaulting to 'no'.");
                return false;
            }
            
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.startsWith("y")) {
                return true;
            } else if (input.startsWith("n")) {
                return false;
            }
            System.out.println("Please enter 'y' for yes or 'n' for no.");
        }
    }
    
    public String getFilePath(String prompt, String extension) {
        while (true) {
            System.out.print(prompt + " (*" + extension + "): ");
            
            // Check if there's input available before trying to read
            if (!scanner.hasNextLine()) {
                System.out.println("\n❌ No input available. Exiting application...");
                System.exit(0);
            }
            
            String filePath = scanner.nextLine().trim();
            
            if (isValidFilePath(filePath, extension)) {
                return filePath;
            }
            
            System.out.println("File not found or invalid: " + filePath);
        }
    }
    
    public String getChoice(String prompt, String[] validChoices) {
        while (true) {
            System.out.print(prompt + ": ");
            String choice = scanner.nextLine().trim();
            
            for (String validChoice : validChoices) {
                if (validChoice.equalsIgnoreCase(choice)) {
                    return choice;
                }
            }
            
            System.out.println("Invalid choice. Valid options: " + String.join(", ", validChoices));
        }
    }
    
    public String getConfirmation(String prompt, String requiredText) {
        System.out.print(prompt + " (Type '" + requiredText + "' to confirm): ");
        return scanner.nextLine().trim();
    }
    
    // Validation methods
    public boolean isValidFilePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.isFile();
    }
    
    public boolean isValidFilePath(String path, String extension) {
        if (!isValidFilePath(path)) {
            return false;
        }
        return extension == null || path.toLowerCase().endsWith(extension.toLowerCase());
    }
    
    public boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    public boolean isValidEmail(String email) {
        return email != null && email.contains("@") && email.contains(".");
    }
    
    public boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public void waitForEnter() {
        System.out.println("\nPress Enter to continue...");
        
        // Check if there's input available before trying to read
        if (!scanner.hasNextLine()) {
            System.out.println("❌ No input available. Continuing...");
            return;
        }
        
        scanner.nextLine();
    }
    
    public void waitForEnter(String message) {
        System.out.println("\n" + message);
        System.out.println("Press Enter to continue...");
        
        // Check if there's input available before trying to read
        if (!scanner.hasNextLine()) {
            System.out.println("❌ No input available. Continuing...");
            return;
        }
        
        scanner.nextLine();
    }
}
