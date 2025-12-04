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

package fin.service.spring;

import fin.util.TesseractConfigUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts text content from PDF documents for bank statement processing.
 * Uses Apache PDFBox for text-based PDFs with OCR fallback for image-based PDFs.
 * Implements hybrid extraction strategy for comprehensive document support.
 */
@Service
public class DocumentTextExtractor {

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextExtractor.class);

    private String accountNumber;
    private String statementPeriod;

    /**
     * Parse a PDF document and extract text lines.
     * Uses hybrid extraction strategy: PDFBox first, then OCR for image-based PDFs.
     */
    public List<String> parseDocument(File pdfFile) throws IOException {
        List<String> lines = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            // Strategy 1: Try PDFBox text extraction first (fast)
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            
            // Check extraction quality
            String[] testLines = text.split("\\r?\\n");
            int shortLineCount = 0;
            int totalNonEmptyLines = 0;
            int dateLineCount = 0;
            int amountLineCount = 0;
            
            for (String line : testLines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    totalNonEmptyLines++;
                    if (trimmed.length() < 10) {
                        shortLineCount++;
                    }
                    // Check for date patterns
                    if (trimmed.matches(".*\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}.*") ||
                        trimmed.matches(".*\\d{4}[/\\-]\\d{2}[/\\-]\\d{2}.*")) {
                        dateLineCount++;
                    }
                    // Check for monetary amounts
                    if (trimmed.matches(".*\\d+\\.\\d{2}.*")) {
                        amountLineCount++;
                    }
                }
            }
            
            logger.info("PDF extraction quality: {} total lines, {} short lines ({}%), {} dates, {} amounts",
                       totalNonEmptyLines, shortLineCount, 
                       totalNonEmptyLines > 0 ? (shortLineCount * 100 / totalNonEmptyLines) : 0,
                       dateLineCount, amountLineCount);
            
            // Determine if we need OCR:
            // - More than 70% of lines are very short (fragmented)
            // - AND less than 10 monetary amounts found (likely watermark/stamp only)
            // The key indicator is lack of monetary data, not dates (watermarks can have dates)
            boolean needsOCR = totalNonEmptyLines > 0 && 
                               (shortLineCount > (totalNonEmptyLines * 0.7)) &&
                               amountLineCount < 10;
            
            if (needsOCR) {
                logger.info("PDFBox extraction insufficient, falling back to OCR for image-based PDF");
                lines = extractWithOCR(document);
            } else if (shortLineCount > (totalNonEmptyLines * 0.5)) {
                // Text-based but needs reconstruction
                logger.info("Text-based PDF with fragmentation, reconstructing lines");
                lines = reconstructLines(testLines);
            } else {
                // Good text extraction, use as-is
                logger.info("Using PDFBox text extraction");
                for (String line : testLines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        lines.add(line);
                        extractMetadata(line);
                    }
                }
            }
        }

        return lines;
    }

    /**
     * Extract text from image-based PDF using Tesseract OCR.
     * Renders each PDF page as an image and performs OCR on it.
     */
    private List<String> extractWithOCR(PDDocument document) throws IOException {
        List<String> allLines = new ArrayList<>();
        
        // Create PDF renderer
        PDFRenderer renderer = new PDFRenderer(document);
        
        // Process each page
        int pageCount = document.getNumberOfPages();
        logger.info("Processing {} pages with OCR", pageCount);
        
        for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            logger.info("OCR processing page {}/{}", pageIndex + 1, pageCount);
            
            // Render page as image at 150 DPI for good OCR quality with faster processing
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 150);
            
            // Perform OCR using utility
            String pageText = TesseractConfigUtil.performOCR(image);
            
            // Split into lines and add to results
            String[] lines = pageText.split("\\r?\\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    allLines.add(line);
                    extractMetadata(line);
                }
            }
        }
        
        logger.info("OCR extraction completed: {} lines extracted", allLines.size());
        
        return allLines;
    }

    /**
     * Reconstruct logical lines from fragmented PDF text.
     * Merges short lines together based on heuristics.
     */
    private List<String> reconstructLines(String[] fragmentedLines) {
        List<String> reconstructed = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        
        for (String fragment : fragmentedLines) {
            fragment = fragment.trim();
            if (fragment.isEmpty()) {
                continue;
            }
            
            // Check if this fragment might be the start of a new logical line
            boolean isNewLine = false;
            
            // Heuristics for line breaks:
            // 1. Line starts with a date pattern
            if (fragment.matches("\\d{2}/\\d{2}/\\d{4}.*") || 
                fragment.matches("\\d{2}\\s+[A-Za-z]{3}\\s+\\d{4}.*")) {
                isNewLine = true;
            }
            
            // 2. Previous line ended with an amount or balance
            if (currentLine.length() > 0 && 
                currentLine.toString().matches(".*\\d+\\.\\d{2}\\s*$")) {
                isNewLine = true;
            }
            
            // 3. Line contains transaction keywords at the start
            String[] lineStarters = {"Balance", "BALANCE", "Opening", "Closing", 
                                     "Transaction", "Date", "Description"};
            for (String starter : lineStarters) {
                if (fragment.startsWith(starter)) {
                    isNewLine = true;
                    break;
                }
            }
            
            if (isNewLine && currentLine.length() > 0) {
                String line = currentLine.toString().trim();
                if (!line.isEmpty()) {
                    reconstructed.add(line);
                    extractMetadata(line);
                }
                currentLine = new StringBuilder(fragment);
            } else {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(fragment);
            }
        }
        
        // Add the last line
        if (currentLine.length() > 0) {
            String line = currentLine.toString().trim();
            if (!line.isEmpty()) {
                reconstructed.add(line);
                extractMetadata(line);
            }
        }
        
        return reconstructed;
    }

    /**
     * Extract account number and statement period from document lines.
     */
    private void extractMetadata(String line) {
        // Extract account number - look for patterns like "Account: 1234567890" or "Account Number: 1234567890"
        if (accountNumber == null) {
            Pattern accountPattern = Pattern.compile("Account(?:\\s+Number)?\\s*:\\s*([0-9\\s-]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = accountPattern.matcher(line);
            if (matcher.find()) {
                accountNumber = matcher.group(1).replaceAll("\\s+", "");
            }
        }

        // Extract statement period - look for date ranges
        if (statementPeriod == null) {
            Pattern periodPattern = Pattern.compile("(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})\\s+to\\s+(\\d{1,2}\\s+[A-Za-z]+\\s+\\d{4})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = periodPattern.matcher(line);
            if (matcher.find()) {
                statementPeriod = matcher.group(1) + " to " + matcher.group(2);
            }
        }
    }

    /**
     * Get the extracted account number.
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Get the extracted statement period.
     */
    public String getStatementPeriod() {
        return statementPeriod;
    }

    /**
     * Determine if a line represents a bank transaction.
     * Transactions typically contain amounts, dates, or transaction keywords.
     */
    public boolean isTransaction(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        line = line.trim();

        // Skip page headers, footers, and metadata
        if (line.toLowerCase().contains("page") ||
            line.toLowerCase().contains("statement") ||
            line.toLowerCase().contains("account summary") ||
            line.toLowerCase().contains("opening balance") ||
            line.toLowerCase().contains("closing balance")) {
            return false;
        }

        // Look for transaction patterns
        // 1. Lines with monetary amounts (digits followed by decimal point and 2 digits)
        if (line.matches(".*\\d+\\.\\d{2}.*")) {
            return true;
        }

        // 2. Lines with transaction keywords
        String[] transactionKeywords = {
            "transfer", "payment", "fee", "charge", "deposit", "withdrawal",
            "debit", "credit", "atm", "eft", "salary", "interest", "dividend"
        };

        String lowerLine = line.toLowerCase();
        for (String keyword : transactionKeywords) {
            if (lowerLine.contains(keyword)) {
                return true;
            }
        }

        // 3. Lines that look like transaction descriptions with amounts at the end
        if (line.matches(".*\\s+\\d+\\.\\d{2}-?$")) {
            return true;
        }

        return false;
    }
}