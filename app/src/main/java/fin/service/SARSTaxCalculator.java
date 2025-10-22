package fin.service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class SARSTaxCalculator {
    // SARS Tax Constants (2024/25 Tax Year)
    private static final double TAX_FREE_THRESHOLD = 5586.0;        // R5,586 annual tax-free allowance
    private static final double ANNUAL_SDL_THRESHOLD = 500000.0;    // R500,000 annual payroll threshold for SDL
    private static final double MONTHLY_SDL_THRESHOLD = ANNUAL_SDL_THRESHOLD / 12; // R41,666.67 monthly threshold
    private static final double SDL_RATE = 0.01;                    // 1% SDL levy rate

    // UIF Constants
    private static final double UIF_THRESHOLD = 17712.0;
    private static final double UIF_CAP = 177.12;
    private static final double MIN_SALARY_RANGE = 5500.0;
    private static final double MAX_SALARY_RANGE = 30000.0;
    private static final double BASE_TAX_HIGHEST_BRACKET = 54481.0;
    private static final double THRESHOLD_HIGHEST_BRACKET = 156328.0;
    private static final double ADDITIONAL_TAX_RATE = 0.45;
    private static final double UIF_RATE = 0.01;

    // Regex group indices for tax bracket parsing
    private static final int REGEX_GROUP_LOWER_1 = 1;
    private static final int REGEX_GROUP_UPPER_1 = 2;
    private static final int REGEX_GROUP_TAX_1 = 3;
    private static final int REGEX_GROUP_LOWER_2 = 4;
    private static final int REGEX_GROUP_UPPER_2 = 5;
    private static final int REGEX_GROUP_TAX_2 = 6;

    // Rounding constants for financial calculations
    private static final double ROUNDING_FACTOR = 100.0;

    // Display formatting constants
    private static final int HEADER_WIDTH = 60;
    private static final int SECTION_WIDTH = 40;
    private static final int BRACKET_LIST_WIDTH = 50;

    private List<TaxBracket> taxBrackets = new ArrayList<>();

    public static class TaxBracket {
        private double lower;
        private double upper;
        private double tax;

        public TaxBracket(double valueLower, double valueUpper, double valueTax) {
            this.lower = valueLower;
            this.upper = valueUpper;
            this.tax = valueTax;
        }

        public double getLower() {
            return lower;
        }

        public double getUpper() {
            return upper;
        }

        public double getTax() {
            return tax;
        }

        @Override
        public String toString() {
            return String.format("R%.0f - R%.0f: Tax R%.0f", lower, upper, tax);
        }
    }

    public void loadTaxTablesFromPDFText(String pdfTextPath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(pdfTextPath)), java.nio.charset.StandardCharsets.UTF_8);
        parseTaxBrackets(content);
        System.out.println("Loaded " + taxBrackets.size() + " tax brackets");
    }

    private void parseTaxBrackets(String pdfText) {
        // Updated regex to capture both brackets per line, similar to RegexTest.java
        // Pattern matches: R lower1 - R upper1 R ... R tax1 R ... R ... R lower2 - R upper2 R ... R tax2
        Pattern pattern = Pattern.compile(
            "R\\s*(\\d+[,\\d]*)\\s*-\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*\\d+[,\\d]*\\s*" +
            "R\\s*(\\d+[,\\d]*)\\s*-\\s*R\\s*(\\d+[,\\d]*)\\s*R\\s*\\d+[,\\d]*\\s*R\\s*(\\d+[,\\d]*)"
        );

        String[] lines = pdfText.split("\n");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                try {
                    // Parse first bracket
                    double lower1 = parseNumber(matcher.group(REGEX_GROUP_LOWER_1));
                    double upper1 = parseNumber(matcher.group(REGEX_GROUP_UPPER_1));
                    double tax1 = parseNumber(matcher.group(REGEX_GROUP_TAX_1));
                    
                    // Validate first bracket is within R5,500-R30,000 range
                    if (lower1 >= MIN_SALARY_RANGE && upper1 <= MAX_SALARY_RANGE) {
                        taxBrackets.add(new TaxBracket(lower1, upper1, tax1));
                        System.out.printf("✓ Added first bracket: R%.0f - R%.0f → Tax R%.0f%n", lower1, upper1, tax1);
                    }
                    
                    // Parse second bracket
                    double lower2 = parseNumber(matcher.group(REGEX_GROUP_LOWER_2));
                    double upper2 = parseNumber(matcher.group(REGEX_GROUP_UPPER_2));
                    double tax2 = parseNumber(matcher.group(REGEX_GROUP_TAX_2));
                    
                    // Validate second bracket is within R5,500-R30,000 range
                    if (lower2 >= MIN_SALARY_RANGE && upper2 <= MAX_SALARY_RANGE) {
                        taxBrackets.add(new TaxBracket(lower2, upper2, tax2));
                        System.out.printf("✓ Added second bracket: R%.0f - R%.0f → Tax R%.0f%n", lower2, upper2, tax2);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line + " - " + e.getMessage());
                }
            }
        }

        // Sort by lower bound to ensure proper searching
        taxBrackets.sort(Comparator.comparingDouble(b -> b.lower));
        
        // Remove duplicates if any (though unlikely)
        Set<String> seen = new HashSet<>();
        taxBrackets.removeIf(bracket -> !seen.add(bracket.lower + "-" + bracket.upper));
        
        System.out.println("Final tax brackets loaded: " + taxBrackets.size() + " (filtered for R5,500-R30,000 range)");
    }

    private double parseNumber(String numberStr) {
        // Remove commas and spaces, convert to double
        return Double.parseDouble(numberStr.replaceAll("[,\\s]", ""));
    }

    public double calculateUIF(double grossSalary) {
        if (grossSalary <= UIF_THRESHOLD) {
            return Math.round(grossSalary * UIF_RATE * ROUNDING_FACTOR) / ROUNDING_FACTOR;
        } else {
            return UIF_CAP;
        }
    }

    /**
     * Calculate SDL (Skills Development Levy)
     * SDL is 1% of total payroll for companies with annual payroll > R500,000
     * For monthly payroll, this is calculated per employee but only if total payroll > ~R41,667/month
     */
    public double calculateSDL(double grossSalary, double totalCompanyPayroll) {
        // SDL is only applicable if total company payroll > R500,000 per year
        // Monthly threshold: R500,000 / 12 = R41,666.67
        
        if (totalCompanyPayroll > MONTHLY_SDL_THRESHOLD) {
            return Math.round(grossSalary * SDL_RATE * ROUNDING_FACTOR) / ROUNDING_FACTOR;
        }
        return 0.0;
    }

    public double findPAYE(double grossSalary) {
        // Debug: Print the actual salary value
        System.out.printf("DEBUG: findPAYE called with grossSalary = %.2f%n", grossSalary);
        
        // Handle salaries below the tax threshold (below R5,586 based on loaded brackets)
        if (grossSalary < TAX_FREE_THRESHOLD) {
            System.out.printf("✓ Salary R%.2f is below tax threshold, no PAYE tax%n", grossSalary);
            return 0.0;
        }
        
        // Check if salary is within our loaded range
        if (grossSalary < MIN_SALARY_RANGE || grossSalary > MAX_SALARY_RANGE) {
            throw new IllegalArgumentException("Salary R" + grossSalary + " is outside the supported range (R5,500-R30,000)");
        }
        
        for (TaxBracket bracket : taxBrackets) {
            if (grossSalary >= bracket.lower && grossSalary <= bracket.upper) {
                System.out.printf("✓ Found bracket: %s for gross R%.2f%n", bracket, grossSalary);
                return bracket.tax;
            }
        }

        // If salary exceeds highest bracket, use the formula from page 15
        if (!taxBrackets.isEmpty() && grossSalary > taxBrackets.get(taxBrackets.size()-1).upper) {
            System.out.println("⚠ Salary exceeds table range, using 45% formula");
            return calculateAboveTablePAYE(grossSalary);
        }

        throw new IllegalArgumentException("No tax bracket found for gross salary: R" + grossSalary);
    }

    private double calculateAboveTablePAYE(double grossSalary) {
        // Formula from page 15: Add base tax + 45% of excess over R156,328
        double baseTax = BASE_TAX_HIGHEST_BRACKET; // Tax for highest bracket
        double threshold = THRESHOLD_HIGHEST_BRACKET;
        double excess = grossSalary - threshold;
        double additionalTax = excess * ADDITIONAL_TAX_RATE;

        return baseTax + additionalTax;
    }

    public Map<String, Double> calculateNetPay(double grossSalary) {
        System.out.println("\n" + "=".repeat(HEADER_WIDTH));
        System.out.printf("CALCULATING NET PAY FOR GROSS SALARY: R%.2f%n", grossSalary);
        System.out.println("=".repeat(HEADER_WIDTH));

        double uif = calculateUIF(grossSalary);
        double paye = findPAYE(grossSalary);
        double netPay = grossSalary - uif - paye;

        Map<String, Double> result = new LinkedHashMap<>();
        result.put("gross", grossSalary);
        result.put("uif", uif);
        result.put("paye", paye);
        result.put("net", netPay);

        return result;
    }

    public void printCalculation(Map<String, Double> calculation) {
        System.out.println("\nRESULTS:");
        System.out.println("-".repeat(SECTION_WIDTH));
        System.out.printf("Gross Salary: R%,.2f%n", calculation.get("gross"));
        System.out.printf("UIF Deduction: R%,.2f%n", calculation.get("uif"));
        System.out.printf("PAYE Tax: R%,.2f%n", calculation.get("paye"));
        System.out.printf("NET PAY: R%,.2f%n", calculation.get("net"));
        System.out.println("-".repeat(SECTION_WIDTH));
    }

    public void printAllBrackets() {
        System.out.println("\nLOADED TAX BRACKETS:");
        System.out.println("=".repeat(BRACKET_LIST_WIDTH));
        for (TaxBracket bracket : taxBrackets) {
            System.out.println(bracket);
        }
    }
    
    public List<TaxBracket> getTaxBrackets() {
        return new ArrayList<>(taxBrackets);
    }
}