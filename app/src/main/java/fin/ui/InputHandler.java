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
    
    public InputHandler(Scanner scanner) {
        this.scanner = scanner;
    }
    
    public String getString(String prompt) {
        System.out.print(prompt + ": ");
        return scanner.nextLine().trim();
    }
    
    public String getString(String prompt, String defaultValue) {
        System.out.print(prompt + " [" + defaultValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? defaultValue : input;
    }
    
    public String getOptionalString(String prompt) {
        System.out.print(prompt + " (optional): ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? null : input;
    }
    
    public int getInteger(String prompt) {
        while (true) {
            try {
                System.out.print(prompt + ": ");
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
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
        while (true) {
            try {
                System.out.print(prompt + " (DD/MM/YYYY): ");
                String dateStr = scanner.nextLine().trim();
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use DD/MM/YYYY.");
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
        while (true) {
            try {
                System.out.print(prompt + ": ");
                return new BigDecimal(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            }
        }
    }
    
    public boolean getBoolean(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
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
        scanner.nextLine();
    }
    
    public void waitForEnter(String message) {
        System.out.println("\n" + message);
        System.out.println("Press Enter to continue...");
        scanner.nextLine();
    }
}
