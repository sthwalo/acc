/*
 * Copyright (c) 2024 Immaculate Nyoni <sthwaloe@gmail.com>
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package fin.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes PDF bank statements to learn column positions and dimensions.
 * Uses PDFBox TextPosition to extract exact X,Y coordinates of text.
 * Generates ColumnProfile that can be used for coordinate-based parsing.
 */
public class PdfColumnAnalyzer {
    
    /**
     * Represents a column in the statement with its X-coordinate boundaries.
     */
    public static class ColumnProfile {
        private final String bankName;
        private final Map<String, ColumnBoundary> columns;
        private final float pageWidth;
        private final float pageHeight;
        
        public ColumnProfile(String bankName, float pageWidth, float pageHeight) {
            this.bankName = bankName;
            this.pageWidth = pageWidth;
            this.pageHeight = pageHeight;
            this.columns = new LinkedHashMap<>();
        }
        
        public void addColumn(String name, float startX, float endX) {
            columns.put(name, new ColumnBoundary(startX, endX));
        }
        
        public ColumnBoundary getColumn(String name) {
            return columns.get(name);
        }
        
        public String getBankName() {
            return bankName;
        }
        
        public float getPageWidth() {
            return pageWidth;
        }
        
        public float getPageHeight() {
            return pageHeight;
        }
        
        public Map<String, ColumnBoundary> getAllColumns() {
            return Collections.unmodifiableMap(columns);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("ColumnProfile{bank=").append(bankName)
              .append(", pageSize=").append(pageWidth).append("x").append(pageHeight)
              .append(", columns={\n");
            columns.forEach((name, boundary) -> 
                sb.append("  ").append(name).append(": ")
                  .append(boundary).append("\n"));
            sb.append("}}");
            return sb.toString();
        }
    }
    
    /**
     * Represents the X-coordinate boundaries of a column.
     */
    public static class ColumnBoundary {
        private final float startX;
        private final float endX;
        
        public ColumnBoundary(float startX, float endX) {
            this.startX = startX;
            this.endX = endX;
        }
        
        public float getStartX() {
            return startX;
        }
        
        public float getEndX() {
            return endX;
        }
        
        public float getWidth() {
            return endX - startX;
        }
        
        public boolean contains(float x) {
            return x >= startX && x <= endX;
        }
        
        @Override
        public String toString() {
            return String.format("[%.1f - %.1f] (width: %.1f)", startX, endX, getWidth());
        }
    }
    
    /**
     * Text element with its position and content.
     */
    private static class PositionedText {
        final String text;
        final float x;
        final float y;
        final float width;
        
        PositionedText(String text, float x, float y, float width) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
        }
        
        boolean isNumeric() {
            return text.matches("[\\d,\\s]+\\.\\d{2}.*");
        }
        
        boolean isDate() {
            return text.matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}.*");
        }
        
        @Override
        public String toString() {
            return String.format("'%s' @ x=%.1f y=%.1f width=%.1f", text, x, y, width);
        }
    }
    
    /**
     * Custom stripper that captures text positions.
     */
    private static class PositionCapturingStripper extends PDFTextStripper {
        private final List<PositionedText> positions = new ArrayList<>();
        
        PositionCapturingStripper() throws IOException {
            super();
            setSortByPosition(true);
        }
        
        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty() || text.trim().isEmpty()) {
                return;
            }
            
            TextPosition firstPos = textPositions.get(0);
            TextPosition lastPos = textPositions.get(textPositions.size() - 1);
            
            float x = firstPos.getXDirAdj();
            float y = firstPos.getYDirAdj();
            float width = (lastPos.getXDirAdj() + lastPos.getWidthDirAdj()) - x;
            
            positions.add(new PositionedText(text.trim(), x, y, width));
        }
        
        List<PositionedText> getPositions() {
            return positions;
        }
    }
    
    /**
     * Analyzes a PDF to learn column positions.
     * 
     * @param pdfPath Path to PDF file
     * @param bankName Name of bank (for profile identification)
     * @return ColumnProfile with learned column boundaries
     */
    public static ColumnProfile analyzeColumns(String pdfPath, String bankName) throws IOException {
        File file = new File(pdfPath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PDPage firstPage = document.getPage(0);
            float pageWidth = firstPage.getMediaBox().getWidth();
            float pageHeight = firstPage.getMediaBox().getHeight();
            
            PositionCapturingStripper stripper = new PositionCapturingStripper();
            stripper.setStartPage(0);
            stripper.setEndPage(1);
            stripper.getText(document);
            
            List<PositionedText> positions = stripper.getPositions();
            
            return detectColumns(bankName, pageWidth, pageHeight, positions);
        }
    }
    
    /**
     * Detects column boundaries by analyzing X-coordinate clusters.
     */
    private static ColumnProfile detectColumns(String bankName, float pageWidth, 
                                               float pageHeight, List<PositionedText> positions) {
        
        // Group positions by similar X coordinates (within 5 units tolerance)
        Map<Float, List<PositionedText>> xClusters = new TreeMap<>();
        for (PositionedText pos : positions) {
            Float clusterKey = findOrCreateCluster(xClusters.keySet(), pos.x, 5.0f);
            xClusters.computeIfAbsent(clusterKey, k -> new ArrayList<>()).add(pos);
        }
        
        // Identify column types by analyzing content patterns
        ColumnProfile profile = new ColumnProfile(bankName, pageWidth, pageHeight);
        
        List<Float> sortedXPositions = new ArrayList<>(xClusters.keySet());
        
        // Analyze each cluster to determine column type
        for (int i = 0; i < sortedXPositions.size(); i++) {
            Float xPos = sortedXPositions.get(i);
            List<PositionedText> cluster = xClusters.get(xPos);
            
            String columnType = identifyColumnType(cluster);
            if (columnType != null) {
                float startX = xPos;
                float endX = (i < sortedXPositions.size() - 1) 
                    ? sortedXPositions.get(i + 1) 
                    : pageWidth;
                
                profile.addColumn(columnType, startX, endX);
            }
        }
        
        return profile;
    }
    
    /**
     * Finds existing cluster or creates new one for X position.
     */
    private static Float findOrCreateCluster(Set<Float> existing, float x, float tolerance) {
        for (Float existingX : existing) {
            if (Math.abs(existingX - x) <= tolerance) {
                return existingX;
            }
        }
        return x;
    }
    
    /**
     * Identifies column type based on content patterns.
     */
    private static String identifyColumnType(List<PositionedText> texts) {
        if (texts.isEmpty()) {
            return null;
        }
        
        // Count different content types in this cluster
        long dateCount = texts.stream().filter(PositionedText::isDate).count();
        long numericCount = texts.stream().filter(PositionedText::isNumeric).count();
        
        float dateRatio = (float) dateCount / texts.size();
        float numericRatio = (float) numericCount / texts.size();
        
        if (dateRatio > 0.3) {
            return "date";
        } else if (numericRatio > 0.5) {
            // Check if amounts are small (service fees) or large (main amounts)
            double avgAmount = texts.stream()
                .filter(PositionedText::isNumeric)
                .mapToDouble(t -> {
                    try {
                        String clean = t.text.replaceAll("[^\\d.]", "");
                        return Double.parseDouble(clean);
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .average()
                .orElse(0);
            
            if (avgAmount < 100) {
                return "service_fee";
            } else if (avgAmount > 1000) {
                return "balance";
            } else {
                return "amount";
            }
        } else {
            return "description";
        }
    }
    
    /**
     * Extracts text from a specific column using learned boundaries.
     */
    public static List<String> extractColumnText(String pdfPath, ColumnBoundary column) 
            throws IOException {
        
        File file = new File(pdfPath);
        try (PDDocument document = Loader.loadPDF(file)) {
            PositionCapturingStripper stripper = new PositionCapturingStripper();
            stripper.getText(document);
            
            return stripper.getPositions().stream()
                .filter(pos -> column.contains(pos.x))
                .map(pos -> pos.text)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Main method for testing column analysis on sample PDFs.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: PdfColumnAnalyzer <pdf-path> <bank-name>");
            System.err.println("Example: PdfColumnAnalyzer input/Rock\\ Absa/4068820115.pdf ABSA");
            System.exit(1);
        }
        
        String pdfPath = args[0];
        String bankName = args[1];
        
        try {
            System.out.println("Analyzing PDF: " + pdfPath);
            System.out.println("Bank: " + bankName);
            System.out.println("─".repeat(80));
            
            ColumnProfile profile = analyzeColumns(pdfPath, bankName);
            System.out.println(profile);
            
            System.out.println("\n" + "─".repeat(80));
            System.out.println("Sample extractions:");
            
            for (Map.Entry<String, ColumnBoundary> entry : profile.getAllColumns().entrySet()) {
                String columnName = entry.getKey();
                ColumnBoundary boundary = entry.getValue();
                
                List<String> samples = extractColumnText(pdfPath, boundary);
                System.out.println("\n" + columnName.toUpperCase() + " column:");
                samples.stream().limit(5).forEach(text -> 
                    System.out.println("  - " + text));
            }
            
        } catch (IOException e) {
            System.err.println("Error analyzing PDF: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
