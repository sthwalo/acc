/*
 * Copyright (c) 2024 Immaculate Nyoni <sthwaloe@gmail.com>
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package fin.util;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Extracts text from OCR-based PDFs with X,Y coordinates using Tesseract.
 * Returns positioned text that can be used for coordinate-based parsing.
 */
public class OcrCoordinateExtractor {
    
    /**
     * Represents text with its bounding box coordinates.
     */
    public static class PositionedText {
        private final String text;
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        
        public PositionedText(String text, float x, float y, float width, float height) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public String getText() {
            return text;
        }
        
        public float getX() {
            return x;
        }
        
        public float getY() {
            return y;
        }
        
        public float getWidth() {
            return width;
        }
        
        public float getHeight() {
            return height;
        }
        
        public boolean isInColumn(PdfColumnAnalyzer.ColumnBoundary column) {
            return column.contains(x);
        }
        
        public boolean isNumeric() {
            return text.matches("[\\d,\\s]+\\.\\d{2}.*");
        }
        
        public boolean isDate() {
            return text.matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}.*");
        }
        
        @Override
        public String toString() {
            return String.format("'%s' @ x=%.1f y=%.1f (%.1fx%.1f)", text, x, y, width, height);
        }
    }
    
    /**
     * Represents a line of text with multiple words at similar Y positions.
     */
    public static class TextLine {
        private final float y;
        private final List<PositionedText> words;
        
        public TextLine(float y) {
            this.y = y;
            this.words = new ArrayList<>();
        }
        
        public void addWord(PositionedText word) {
            words.add(word);
        }
        
        public float getY() {
            return y;
        }
        
        public List<PositionedText> getWords() {
            return words;
        }
        
        public String getFullText() {
            return words.stream()
                .sorted(Comparator.comparing(PositionedText::getX))
                .map(PositionedText::getText)
                .collect(Collectors.joining(" "));
        }
        
        /**
         * Gets text from a specific column range.
         */
        public String getColumnText(PdfColumnAnalyzer.ColumnBoundary column) {
            return words.stream()
                .filter(w -> w.isInColumn(column))
                .sorted(Comparator.comparing(PositionedText::getX))
                .map(PositionedText::getText)
                .collect(Collectors.joining(" "));
        }
        
        /**
         * Gets all numeric text (amounts) in order.
         */
        public List<String> getAmounts() {
            return words.stream()
                .filter(PositionedText::isNumeric)
                .sorted(Comparator.comparing(PositionedText::getX))
                .map(PositionedText::getText)
                .collect(Collectors.toList());
        }
        
        @Override
        public String toString() {
            return String.format("Line @ y=%.1f: %s", y, getFullText());
        }
    }
    
    /**
     * Extracts positioned text from PDF using Tesseract OCR.
     * 
     * @param pdfPath Path to PDF file
     * @param pageNum Page number (0-indexed)
     * @return List of positioned text elements
     */
    public static List<PositionedText> extractWithPositions(String pdfPath, int pageNum) throws Exception {
        File pdfFile = new File(pdfPath);
        
        // Convert PDF page to image
        BufferedImage image;
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            image = renderer.renderImageWithDPI(pageNum, 300); // 300 DPI for good OCR
        }
        
        // Run Tesseract with word-level detection
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/share/tessdata"); // macOS Homebrew path
        tesseract.setLanguage("eng");
        
        // Get words with bounding boxes
        List<Word> words = tesseract.getWords(image, 0); // 0 = page number
        
        // Convert to PositionedText
        List<PositionedText> positions = new ArrayList<>();
        for (Word word : words) {
            java.awt.Rectangle bbox = word.getBoundingBox();
            positions.add(new PositionedText(
                word.getText(),
                bbox.x,
                bbox.y,
                bbox.width,
                bbox.height
            ));
        }
        
        return positions;
    }
    
    /**
     * Groups positioned text into lines based on Y coordinate proximity.
     */
    public static List<TextLine> groupIntoLines(List<PositionedText> positions) {
        if (positions.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Sort by Y position
        List<PositionedText> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparing(PositionedText::getY));
        
        List<TextLine> lines = new ArrayList<>();
        TextLine currentLine = null;
        float lastY = -1;
        float yTolerance = 10; // Words within 10 pixels are on same line
        
        for (PositionedText pos : sorted) {
            if (currentLine == null || Math.abs(pos.getY() - lastY) > yTolerance) {
                // Start new line
                if (currentLine != null) {
                    lines.add(currentLine);
                }
                currentLine = new TextLine(pos.getY());
                lastY = pos.getY();
            }
            currentLine.addWord(pos);
        }
        
        // Add last line
        if (currentLine != null) {
            lines.add(currentLine);
        }
        
        return lines;
    }
    
    /**
     * Main method for testing OCR coordinate extraction.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: OcrCoordinateExtractor <pdf-path> [page-num]");
            System.err.println("Example: OcrCoordinateExtractor 'input/Rock Absa/4068820115.pdf' 0");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        int pageNum = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        
        try {
            System.out.println("Extracting text with OCR from: " + pdfPath);
            System.out.println("Page: " + pageNum);
            System.out.println("─".repeat(80));
            
            List<PositionedText> positions = extractWithPositions(pdfPath, pageNum);
            System.out.println("Found " + positions.size() + " text elements");
            
            // Group into lines
            List<TextLine> lines = groupIntoLines(positions);
            System.out.println("Grouped into " + lines.size() + " lines");
            
            System.out.println("\n" + "─".repeat(80));
            System.out.println("Sample lines (first 20):");
            lines.stream().limit(20).forEach(line -> {
                System.out.printf("Y=%.1f: %s\n", line.getY(), line.getFullText());
                
                // Show amounts on this line
                List<String> amounts = line.getAmounts();
                if (!amounts.isEmpty()) {
                    System.out.println("  → Amounts: " + String.join(", ", amounts));
                }
            });
            
            // Analyze X-coordinate distribution to find columns
            System.out.println("\n" + "─".repeat(80));
            System.out.println("X-coordinate distribution (for column detection):");
            
            Map<Integer, Long> xDistribution = positions.stream()
                .filter(p -> !p.getText().trim().isEmpty())
                .collect(Collectors.groupingBy(
                    p -> Math.round(p.getX() / 10) * 10, // Round to nearest 10
                    Collectors.counting()
                ));
            
            xDistribution.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(e -> e.getValue() > 5) // Only show significant clusters
                .forEach(e -> System.out.printf("X≈%d: %d words\n", e.getKey(), e.getValue()));
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
