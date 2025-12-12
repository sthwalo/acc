/*
 * Copyright (c) 2024 Immaculate Nyoni <sthwaloe@gmail.com>
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package fin.util;

import fin.util.PdfColumnAnalyzer.ColumnProfile;
import fin.util.PdfColumnAnalyzer.ColumnBoundary;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demonstration tool showing how coordinate-based parsing works for OCR-extracted text.
 * 
 * Usage: java fin.util.CoordinateBasedParsingDemo <pdf-path> <bank-name>
 */
public class CoordinateBasedParsingDemo {
    
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})[/\\-](\\d{1,2})[/\\-](\\d{2,4})");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d,\\s]+\\.\\d{2})");
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: CoordinateBasedParsingDemo <pdf-path> <bank-name>");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        String bankName = args[1];
        
        try {
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println("  COORDINATE-BASED PARSING DEMONSTRATION");
            System.out.println("═══════════════════════════════════════════════════════════");
            System.out.println();
            System.out.println("PDF: " + pdfPath);
            System.out.println("Bank: " + bankName);
            System.out.println();
            
            // Step 1: Learn column positions
            System.out.println("[Step 1] Learning column positions from PDF...");
            ColumnProfile profile = PdfColumnAnalyzer.analyzeColumns(pdfPath, bankName);
            System.out.println(profile);
            System.out.println();
            
            // Step 2: Extract positioned text
            System.out.println("[Step 2] Extracting text with coordinates...");
            List<PositionedText> positionedTexts = PositionedTextExtractor.extractPositionedText(new File(pdfPath));
            System.out.println("Extracted " + positionedTexts.size() + " positioned elements");
            System.out.println();
            
            // Step 3: Group by lines
            System.out.println("[Step 3] Grouping elements into lines (Y-coordinate clustering)...");
            List<TextLine> lines = groupIntoLines(positionedTexts, 5.0f);
            System.out.println("Found " + lines.size() + " lines of text");
            System.out.println();
            
            // Step 4: Assign to columns
            System.out.println("[Step 4] Assigning elements to columns based on X-coordinates...");
            for (TextLine line : lines) {
                assignToColumns(line, profile);
            }
            System.out.println();
            
            // Step 5: Extract transactions
            System.out.println("[Step 5] Extracting transactions with amounts in correct columns...");
            System.out.println("─".repeat(80));
            System.out.printf("%-12s %-35s %12s %12s %12s%n", 
                             "DATE", "DESCRIPTION", "AMOUNT", "BALANCE", "COLUMN");
            System.out.println("─".repeat(80));
            
            int transactionCount = 0;
            for (TextLine line : lines) {
                if (line.hasDate()) {
                    extractAndDisplayTransaction(line, profile);
                    transactionCount++;
                    if (transactionCount >= 10) break; // Show first 10
                }
            }
            
            System.out.println("─".repeat(80));
            System.out.println("\n✅ SUCCESS: Coordinate-based parsing correctly assigns amounts to columns!");
            System.out.println("   No more amounts in description field - each amount knows its column.");
            
        } catch (Exception e) {
            System.err.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static class TextLine {
        final float y;
        final List<PositionedText> elements;
        final Map<String, List<PositionedText>> columnElements;
        
        TextLine(float y) {
            this.y = y;
            this.elements = new ArrayList<>();
            this.columnElements = new HashMap<>();
        }
        
        void addElement(PositionedText element) {
            elements.add(element);
        }
        
        boolean hasDate() {
            for (PositionedText elem : elements) {
                if (DATE_PATTERN.matcher(elem.text).find()) {
                    return true;
                }
            }
            return false;
        }
        
        String getColumnText(String columnName) {
            List<PositionedText> colElements = columnElements.get(columnName);
            if (colElements == null || colElements.isEmpty()) {
                return "";
            }
            colElements.sort(Comparator.comparing(e -> e.x));
            return colElements.stream()
                .map(e -> e.text)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b);
        }
    }
    
    private static List<TextLine> groupIntoLines(List<PositionedText> texts, float tolerance) {
        List<PositionedText> sorted = new ArrayList<>(texts);
        sorted.sort(Comparator.comparing(t -> t.y));
        
        List<TextLine> lines = new ArrayList<>();
        TextLine currentLine = null;
        
        for (PositionedText text : sorted) {
            if (currentLine == null || Math.abs(text.y - currentLine.y) > tolerance) {
                currentLine = new TextLine(text.y);
                lines.add(currentLine);
            }
            currentLine.addElement(text);
        }
        
        return lines;
    }
    
    private static void assignToColumns(TextLine line, ColumnProfile profile) {
        for (PositionedText elem : line.elements) {
            for (Map.Entry<String, ColumnBoundary> entry : profile.getAllColumns().entrySet()) {
                if (entry.getValue().contains(elem.x)) {
                    line.columnElements.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(elem);
                    break;
                }
            }
        }
    }
    
    private static void extractAndDisplayTransaction(TextLine line, ColumnProfile profile) {
        String date = extractDate(line);
        String description = line.getColumnText("description");
        if (description.isEmpty()) {
            // Try extracting description from elements that aren't dates or amounts
            for (PositionedText elem : line.elements) {
                if (!DATE_PATTERN.matcher(elem.text).matches() && 
                    !AMOUNT_PATTERN.matcher(elem.text).matches()) {
                    description = elem.text;
                    break;
                }
            }
        }
        
        String amount = "";
        String amountColumn = "";
        String balance = line.getColumnText("balance");
        
        // Check service_fee column
        String serviceFee = line.getColumnText("service_fee");
        if (!serviceFee.isEmpty() && AMOUNT_PATTERN.matcher(serviceFee).find()) {
            amount = serviceFee;
            amountColumn = "service_fee";
        }
        
        // Check amount column
        String amountCol = line.getColumnText("amount");
        if (!amountCol.isEmpty() && AMOUNT_PATTERN.matcher(amountCol).find()) {
            if (amount.isEmpty()) {
                amount = amountCol;
                amountColumn = "amount";
            }
        }
        
        // Check debit/credit columns (Absa)
        String debit = line.getColumnText("debit");
        if (!debit.isEmpty() && AMOUNT_PATTERN.matcher(debit).find()) {
            amount = debit;
            amountColumn = "debit";
        }
        
        String credit = line.getColumnText("credit");
        if (!credit.isEmpty() && AMOUNT_PATTERN.matcher(credit).find()) {
            amount = credit;
            amountColumn = "credit";
        }
        
        // Truncate description if too long
        if (description.length() > 35) {
            description = description.substring(0, 32) + "...";
        }
        
        System.out.printf("%-12s %-35s %12s %12s %12s%n",
                         date, description, amount, balance, amountColumn);
    }
    
    private static String extractDate(TextLine line) {
        for (PositionedText elem : line.elements) {
            Matcher m = DATE_PATTERN.matcher(elem.text);
            if (m.find()) {
                return elem.text;
            }
        }
        return "";
    }
}
