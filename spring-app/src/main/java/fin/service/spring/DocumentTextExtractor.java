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
     * Enhanced with comprehensive quality assessment and better decision making.
     */
    public List<String> parseDocument(File pdfFile) throws IOException {
        logger.info("Starting text extraction from: {}", pdfFile.getName());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            List<String> lines = new ArrayList<>();

            // Strategy 1: Try PDFBox text extraction first (fast)
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            // Check extraction quality comprehensively
            String[] testLines = text.split("\\r?\\n");
            int shortLineCount = 0;
            int totalNonEmptyLines = 0;
            int dateLineCount = 0;
            int amountLineCount = 0;
            int financialTermCount = 0;

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
                    if (trimmed.matches(".*\\$?\\d+[,.]\\d{2}.*")) {
                        amountLineCount++;
                    }
                    // Check for financial terms
                    if (trimmed.matches("(?i).*\\b(credit|debit|deposit|withdrawal|transfer|payment|fee|balance|statement)\\b.*")) {
                        financialTermCount++;
                    }
                }
            }

            logger.info("PDF extraction quality: {} total lines, {} short lines ({}%), {} dates, {} amounts, {} financial terms",
                       totalNonEmptyLines, shortLineCount,
                       totalNonEmptyLines > 0 ? (shortLineCount * 100 / totalNonEmptyLines) : 0,
                       dateLineCount, amountLineCount, financialTermCount);

            // Enhanced decision logic for extraction method
            boolean needsOCR = false;
            String reason = "";

            if (totalNonEmptyLines == 0) {
                needsOCR = true;
                reason = "No text extracted by PDFBox";
            } else if (shortLineCount > (totalNonEmptyLines * 0.8)) {
                needsOCR = true;
                reason = "Over 80% of lines are very short (likely image-based PDF)";
            } else if (amountLineCount < 5 && financialTermCount < 3) {
                needsOCR = true;
                reason = "Insufficient financial content (likely watermark/stamp only)";
            } else if (shortLineCount > (totalNonEmptyLines * 0.6)) {
                // Text-based but heavily fragmented - try reconstruction first
                logger.info("Text-based PDF with heavy fragmentation, attempting reconstruction");
                List<String> reconstructedLines = reconstructLines(testLines);
                if (reconstructedLines.size() < totalNonEmptyLines * 0.5) {
                    needsOCR = true;
                    reason = "Reconstruction failed to improve quality";
                } else {
                    lines = reconstructedLines;
                    logger.info("Using reconstructed text extraction with {} lines", lines.size());
                }
            }

            if (needsOCR) {
                logger.info("PDFBox extraction insufficient ({}), falling back to OCR", reason);
                lines = extractWithOCR(document);
                logger.info("OCR extraction completed with {} lines", lines.size());
            } else if (lines.isEmpty()) {
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

            // Final validation
            if (lines.isEmpty()) {
                throw new IOException("No text could be extracted from PDF using either PDFBox or OCR");
            }

            logger.info("Text extraction completed successfully: {} lines total", lines.size());
            return lines;

        } catch (IOException e) {
            logger.error("Failed to extract text from PDF: {}", e.getMessage());
            throw e;
        }
    }    /**
     * Extract text from image-based PDF using Tesseract OCR.
     * Enhanced with better error handling and quality checks.
     */
    private List<String> extractWithOCR(PDDocument document) throws IOException {
        List<String> allLines = new ArrayList<>();

        // Create PDF renderer with higher DPI for better OCR accuracy
        PDFRenderer renderer = new PDFRenderer(document);
        int totalPages = document.getNumberOfPages();

        logger.info("Starting OCR extraction for {} pages", totalPages);

        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            try {
                logger.info("OCR processing page {}/{}", pageIndex + 1, totalPages);

                // Use higher DPI for better OCR quality (300 DPI is optimal for financial documents)
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, 300);

                // Apply image preprocessing if needed
                image = preprocessImageForOCR(image);

                // Perform OCR using utility
                String pageText = TesseractConfigUtil.performOCR(image);

                if (pageText == null || pageText.trim().isEmpty()) {
                    logger.warn("OCR returned empty text for page {}", pageIndex + 1);
                    continue;
                }

                // Split into lines and filter/process
                String[] lines = pageText.split("\\r?\\n");
                int validLines = 0;

                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && line.length() > 3) { // Filter out very short noise
                        // Clean up common OCR errors in financial text
                        line = cleanOCRErrors(line);
                        allLines.add(line);
                        extractMetadata(line);
                        validLines++;
                    }
                }

                logger.info("Page {}: {} valid lines extracted", pageIndex + 1, validLines);

            } catch (Exception e) {
                logger.error("Failed to OCR page {}: {}", pageIndex + 1, e.getMessage());
                // Continue with other pages rather than failing completely
            }
        }

        logger.info("OCR extraction completed: {} total lines extracted from {} pages",
                   allLines.size(), totalPages);

        return allLines;
    }

    /**
     * Preprocess image for better OCR results.
     * Applies basic image enhancements for financial documents.
     */
    private BufferedImage preprocessImageForOCR(BufferedImage image) {
        // For now, return the image as-is. Could add enhancements like:
        // - Contrast adjustment
        // - Noise reduction
        // - Binarization
        // - Deskewing
        return image;
    }

    /**
     * Clean up common OCR errors in financial text.
     * Package-private for testing.
     */
    String cleanOCRErrors(String text) {
        if (text == null) return "";

        // Common OCR corrections for financial documents
        // Fix individual characters first
        text = text.replaceAll("(?i)I", "1"); // Capital I -> 1
        text = text.replaceAll("(?i)l", "1"); // Lowercase l -> 1
        text = text.replaceAll("(?i)O", "0"); // Capital O -> 0
        text = text.replaceAll("(?i)zer", "0"); // "zer" -> "0"
        text = text.replaceAll("(?i)one", "1"); // "one" -> "1"
        text = text.replaceAll("(?i)tw", "2"); // "tw" -> "2"
        text = text.replaceAll("(?i)three", "3"); // "three" -> "3"
        text = text.replaceAll("(?i)four", "4"); // "four" -> "4"
        text = text.replaceAll("(?i)five", "5"); // "five" -> "5"
        text = text.replaceAll("(?i)six", "6"); // "six" -> "6"
        text = text.replaceAll("(?i)seven", "7"); // "seven" -> "7"
        text = text.replaceAll("(?i)eight", "8"); // "eight" -> "8"
        text = text.replaceAll("(?i)nine", "9"); // "nine" -> "9"

        // Fix date separator issues
        text = text.replaceAll("(\\d{1,2})\\s*[-—]\\s*(\\d{1,2})\\s*[-—]\\s*(\\d{4})", "$1/$2/$3");

        return text;
    }

    /**
     * Reconstruct logical lines from fragmented PDF text.
     * Enhanced with better heuristics for financial document reconstruction.
     * Package-private for testing.
     */
    List<String> reconstructLines(String[] fragmentedLines) {
        List<String> reconstructed = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();

        for (String fragment : fragmentedLines) {
            fragment = fragment.trim();
            if (fragment.isEmpty()) {
                continue;
            }

            // Enhanced heuristics for line breaks in financial documents
            boolean isNewLine = false;

            // 1. Date patterns (strongest indicator)
            if (fragment.matches("\\d{1,2}/\\d{1,2}/\\d{4}.*") ||
                fragment.matches("\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}.*") ||
                fragment.matches("\\d{4}-\\d{1,2}-\\d{1,2}.*")) {
                isNewLine = true;
            }

            // 2. Balance amounts (usually at end of transaction line)
            else if (fragment.matches("[\\d,]+\\.\\d{2}-?\\s*$") && currentLine.length() > 0) {
                // Check if current line already has a balance
                String current = currentLine.toString();
                if (!current.matches(".*\\s+[\\d,]+\\.\\d{2}-?\\s*$")) {
                    // Current line doesn't end with balance, so this fragment is the balance
                    currentLine.append(" ").append(fragment);
                    continue;
                } else {
                    // Current line already has balance, this is a new line
                    isNewLine = true;
                }
            }

            // 3. Transaction amount patterns
            else if (fragment.matches("[\\d,]+\\.\\d{2}-?\\s*$") && currentLine.length() > 10) {
                // This might be a transaction amount
                String current = currentLine.toString();
                // Count existing amounts in current line
                long amountCount = current.split("\\s+").length -
                    current.replaceAll("[\\d,]+\\.\\d{2}", "").split("\\s+").length;
                if (amountCount >= 2) { // Already has amount and balance
                    isNewLine = true;
                } else {
                    currentLine.append(" ").append(fragment);
                    continue;
                }
            }

            // 4. Keywords that typically start new lines
            else {
                String[] lineStarters = {
                    "Balance", "BALANCE", "Opening", "Closing", "Opening Balance",
                    "Transaction", "Date", "Description", "Amount", "Debit", "Credit",
                    "Transfer", "Payment", "Deposit", "Withdrawal", "Fee", "Charge",
                    "Interest", "Dividend", "Salary", "ATM", "EFT", "Cheque",
                    "Statement", "Account", "Branch", "VAT", "Page", "Total"
                };

                String lowerFragment = fragment.toLowerCase();
                for (String starter : lineStarters) {
                    if (lowerFragment.startsWith(starter.toLowerCase()) ||
                        lowerFragment.contains(starter.toLowerCase() + " ")) {
                        isNewLine = true;
                        break;
                    }
                }
            }

            // 5. Very long fragments (likely complete lines)
            if (!isNewLine && fragment.length() > 80) {
                isNewLine = true;
            }

            // Process line break
            if (isNewLine && currentLine.length() > 0) {
                String line = currentLine.toString().trim();
                if (!line.isEmpty() && isTransaction(line)) {
                    reconstructed.add(line);
                    extractMetadata(line);
                }
                currentLine = new StringBuilder(fragment);
            } else {
                if (currentLine.length() > 0) {
                    // Add space between fragments unless fragment starts with punctuation
                    if (!fragment.startsWith(",") && !fragment.startsWith(".") &&
                        !fragment.startsWith("-") && !fragment.startsWith(")")) {
                        currentLine.append(" ");
                    }
                }
                currentLine.append(fragment);
            }
        }

        // Add the last line
        if (currentLine.length() > 0) {
            String line = currentLine.toString().trim();
            if (!line.isEmpty() && isTransaction(line)) {
                reconstructed.add(line);
                extractMetadata(line);
            }
        }

        logger.info("Line reconstruction completed: {} fragments -> {} logical lines",
                   fragmentedLines.length, reconstructed.size());
        return reconstructed;
    }

    /**
     * Extract account number and statement period from document lines.
     * Package-private for testing.
     */
    void extractMetadata(String line) {
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