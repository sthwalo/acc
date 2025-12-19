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

package fin.service.upload;

import fin.util.TesseractConfigUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fin.config.PdfBoxConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final PdfBoxConfigurator pdfBoxConfigurator;

    @Autowired
    public DocumentTextExtractor(PdfBoxConfigurator pdfBoxConfigurator) {
        this.pdfBoxConfigurator = pdfBoxConfigurator;
    }

    private static final Logger logger = LoggerFactory.getLogger(DocumentTextExtractor.class);



    private String accountNumber;
    private String statementPeriod;

    /**
     * Parse a PDF document and extract text lines.
     * Uses hybrid extraction strategy: PDFBox first, then OCR for image-based PDFs.
     * Enhanced with comprehensive quality assessment and better decision making.
     * OPTIMIZED: Added timeout protection and better performance monitoring.
     */
    public List<String> parseDocument(File pdfFile) throws IOException {
        logger.info("Starting text extraction from: {} (size: {} bytes)", pdfFile.getName(), pdfFile.length());

        long startTime = System.currentTimeMillis();
        final int MAX_PROCESSING_TIME_MS = 240000; // 4 minutes max processing time

        // Check PDFBox health before attempting extraction
        if (!pdfBoxConfigurator.isPdfBoxAvailable()) {
            logger.warn("PDFBox is not available ({}), falling back to OCR extraction only", pdfBoxConfigurator.getPdfBoxStatus());
            // Use OCR fallback directly
            try (PDDocument document = Loader.loadPDF(pdfFile)) {
                return extractWithOCR(document, pdfFile, startTime, MAX_PROCESSING_TIME_MS);
            }
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            List<String> lines = new ArrayList<>();
            try {
                // Strategy 1: Try PDFBox text extraction first (fast)
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String text = stripper.getText(document);

                // ...existing code for quality assessment and fallback logic...
                String[] testLines = text.split("\r?\n");
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
                    // Check timeout before starting expensive OCR
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime > MAX_PROCESSING_TIME_MS) {
                        throw new IOException("PDF processing timeout: exceeded " + MAX_PROCESSING_TIME_MS + "ms during quality assessment");
                    }

                    logger.info("PDFBox extraction insufficient ({}), falling back to OCR", reason);
                    lines = extractWithOCR(document, pdfFile, startTime, MAX_PROCESSING_TIME_MS);
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

                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Text extraction completed successfully: {} lines total in {}ms", lines.size(), totalTime);

                // Log first 20 lines for debugging
                int logCount = Math.min(20, lines.size());
                logger.info("First {} extracted lines: {}", logCount, lines.subList(0, logCount));

                return lines;

            } catch (IOException e) {
                logger.error("Failed to extract text from PDF: {}", e.getMessage());
                throw e;
            } catch (Throwable t) {
                // If this is a fatal VM error, rethrow
                if (t instanceof VirtualMachineError || t instanceof ThreadDeath) {
                    throw t;
                }
                logger.error("PDFBox failed during extraction ({}), falling back to OCR: {}", t.getClass().getSimpleName(), t.getMessage());
                // Fallback to OCR (prefer external rasterizer if renderer is broken)
                try {
                    return extractWithOCR(document, pdfFile, startTime, MAX_PROCESSING_TIME_MS);
                } catch (IOException ioe) {
                    logger.error("OCR fallback also failed: {}", ioe.getMessage());
                    throw ioe;
                }
            }
        }
    }

    /**
     * Extract text from image-based PDF using Tesseract OCR.
     * Enhanced with better error handling and quality checks.
     * OPTIMIZED: Reduced DPI from 300 to 200 for faster processing while maintaining accuracy.
     * Added timeout protection to prevent infinite processing.
     */
    List<String> extractWithOCR(PDDocument document, File pdfFile, long startTime, int maxProcessingTimeMs) throws IOException {
        List<String> allLines = new ArrayList<>();

        try {
            // Create PDF renderer with optimized DPI for better performance/accuracy balance
            PDFRenderer renderer = new PDFRenderer(document);
            int totalPages = document.getNumberOfPages();

            logger.info("Starting OCR extraction for {} pages (optimized 200 DPI)", totalPages);

            boolean sawFontProviderError = false;
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                // Check timeout before processing each page
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime > maxProcessingTimeMs) {
                    throw new IOException("OCR processing timeout: exceeded " + maxProcessingTimeMs + "ms while processing page " + (pageIndex + 1) + " of " + totalPages);
                }

                try {
                    logger.info("OCR processing page {}/{} (elapsed: {}ms)", pageIndex + 1, totalPages, elapsedTime);

                    // OPTIMIZED: Use 200 DPI instead of 300 for faster processing
                    // 200 DPI provides good accuracy for financial documents while being ~2x faster
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, 200);

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

                } catch (Throwable t) {
                    logger.error("Failed to OCR page {}: {} ({})", pageIndex + 1, t.getMessage(), t.getClass().getSimpleName());
                    // If the page failed due to the PDFBox font provider (known issue), mark flag and attempt external rasterizer immediately
                    if (isFontProviderError(t)) {
                        sawFontProviderError = true;
                        logger.warn("Detected PDFBox font-provider error - aborting per-page rendering and attempting external rasterizer fallback");
                        try {
                            List<String> external = ocrUsingExternalRasterizer(pdfFile, startTime, maxProcessingTimeMs);
                            if (external != null && !external.isEmpty()) {
                                return external;
                            }
                        } catch (IOException ioe) {
                            logger.error("External rasterizer fallback failed: {}", ioe.getMessage());
                        }
                        break;
                    }
                    // Continue with other pages rather than failing completely for other errors
                }
            }

            // If no lines were extracted and we saw the PDFBox font-provider error, attempt external rasterizer fallback
            if (allLines.isEmpty() && sawFontProviderError) {
                logger.info("No OCR lines extracted and detected PDFBox font-provider failures; trying external rasterizer (pdftoppm)");
                List<String> external = ocrUsingExternalRasterizer(pdfFile, startTime, maxProcessingTimeMs);
                if (external != null && !external.isEmpty()) {
                    return external;
                }
            }

            logger.info("OCR extraction completed: {} total lines extracted from {} pages",
                       allLines.size(), totalPages);

            return allLines;
        } catch (Throwable t) {
            // If renderer construction or other fatal errors occur, try external rasterizer-based OCR as a fallback
            logger.error("OCR extraction failed during initialization: {} ({}). Attempting external rasterizer fallback", t.getMessage(), t.getClass().getSimpleName());
            try {
                List<String> external = ocrUsingExternalRasterizer(pdfFile, startTime, maxProcessingTimeMs);
                if (external != null && !external.isEmpty()) {
                    return external;
                }
            } catch (IOException ioe) {
                logger.error("External rasterizer OCR fallback failed: {}", ioe.getMessage());
            }
            // If everything fails, return what we have (possibly empty)
            return allLines; // empty
        }
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
     * Detect whether the throwable chain contains a PDFBox font-provider related error (NoClassDefFoundError or message containing FontMapperImpl/FileSystemFontProvider)
     */
    private boolean isFontProviderError(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof java.lang.NoClassDefFoundError) return true;
            String msg = cur.getMessage();
            if (msg != null && (msg.contains("FontMapperImpl") || msg.contains("DefaultFontProvider") || msg.contains("FileSystemFontProvider"))) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * Check if an external command is available on PATH.
     */
    private boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process p = pb.start();
            boolean exited = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return exited && p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Attempt OCR by rasterizing the PDF using an external tool (pdftoppm) and running Tesseract on the images.
     * Returns empty list on failure.
     */
    private List<String> ocrUsingExternalRasterizer(File pdfFile, long startTime, int maxProcessingTimeMs) throws IOException {
        List<String> allLines = new ArrayList<>();

        if (!isCommandAvailable("pdftoppm")) {
            logger.warn("pdftoppm not available on PATH. External rasterizer fallback is not possible.");
            return allLines;
        }

        java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("pdf_raster_");
        String outPrefix = tempDir.resolve("page").toString();

        // Run pdftoppm -jpeg -r 200 input.pdf outPrefix
        ProcessBuilder pb = new ProcessBuilder("pdftoppm", "-jpeg", "-r", "200", pdfFile.getAbsolutePath(), outPrefix);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try {
            boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                String pdftoppmOut = new String(process.getInputStream().readAllBytes());
                logger.debug("pdftoppm stdout/stderr: {}", pdftoppmOut);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("pdftoppm timed out");
                }
                if (process.exitValue() != 0) {
                    logger.error("pdftoppm failed (exit {}): {}", process.exitValue(), pdftoppmOut);
                    throw new IOException("pdftoppm failed: exit " + process.exitValue());
                }

                // Collect generated images (match both jpg and png variants)
                java.nio.file.Path dir = tempDir;
                java.util.List<java.nio.file.Path> found = new java.util.ArrayList<>();
                try (java.nio.file.DirectoryStream<java.nio.file.Path> s1 = java.nio.file.Files.newDirectoryStream(dir, "page*.jpg")) {
                    for (java.nio.file.Path pth : s1) found.add(pth);
                } catch (Throwable ignored) {}
                try (java.nio.file.DirectoryStream<java.nio.file.Path> s2 = java.nio.file.Files.newDirectoryStream(dir, "page*.png")) {
                    for (java.nio.file.Path pth : s2) found.add(pth);
                } catch (Throwable ignored) {}

                logger.info("External rasterizer produced {} image(s): {}", found.size(), found);

                for (java.nio.file.Path imgPath : found) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    if (elapsedTime > maxProcessingTimeMs) {
                        throw new IOException("External OCR timeout exceeded");
                    }
                    try {
                        BufferedImage img = javax.imageio.ImageIO.read(imgPath.toFile());
                        if (img == null) {
                            logger.warn("ImageIO could not read generated image {}", imgPath);
                            continue;
                        }
                        String pageText = TesseractConfigUtil.performOCR(img);
                        if (pageText == null || pageText.trim().isEmpty()) {
                            logger.warn("External OCR returned empty text for image {}", imgPath);
                            continue;
                        }
                        String[] lines = pageText.split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty() && line.length() > 3) {
                                line = cleanOCRErrors(line);
                                allLines.add(line);
                                extractMetadata(line);
                            }
                        }
                } catch (Throwable t) {
                    logger.error("External OCR failed for {}: {}", imgPath, t.getMessage());
                }
            }

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("pdftoppm interrupted", ie);
        } finally {
            // Cleanup
            try {
                java.nio.file.Files.walk(tempDir)
                    .map(java.nio.file.Path::toFile)
                    .sorted((a,b) -> -a.getAbsolutePath().compareTo(b.getAbsolutePath()))
                    .forEach(java.io.File::delete);
            } catch (Throwable ignored) {}
        }

        logger.info("External rasterizer OCR completed: {} lines", allLines.size());
        return allLines;
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
            Pattern periodPattern = Pattern.compile("(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})\\s+to\\s+(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE);
            Matcher matcher = periodPattern.matcher(line);
            if (matcher.find()) {
                statementPeriod = matcher.group(1) + " to " + matcher.group(2);
            }
        }
    }

    /**
     * Parse a statement period string (e.g., "16 February 2024 to 18 March 2024")
     * into start and end LocalDate values. Returns null if parsing fails.
     */
    public StatementPeriod parseStatementPeriod(String raw) {
        if (raw == null || raw.isBlank()) return null;
        // Normalize whitespace and common OCR noise
        String cleaned = raw.replaceAll("[\u2018\u2019\u201C\u201D]", "").trim();
        cleaned = cleaned.replaceAll("\u00A0", " "); // non-breaking spaces
        cleaned = cleaned.replaceAll("\\s+to\\s+", " to ");

        // Match two date parts
        Pattern p = Pattern.compile("(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})\\s+to\\s+(\\d{1,2}\\s+[A-Za-z]{3,9}\\s+\\d{4})", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(cleaned);
        if (!m.find()) {
            // Try numeric formats like 01/02/2024 to 28/02/2024 or 2024-02-01 to 2024-02-28
            Pattern alt = Pattern.compile("(\\d{1,2}[\\/\\-]\\d{1,2}[\\/\\-]\\d{4})\\s+to\\s+(\\d{1,2}[\\/\\-]\\d{1,2}[\\/\\-]\\d{4})");
            Matcher m2 = alt.matcher(cleaned);
            if (m2.find()) {
                return parseTwoDates(m2.group(1), m2.group(2));
            }
            return null;
        }

        String d1 = m.group(1);
        String d2 = m.group(2);
        return parseTwoDates(d1, d2);
    }

    private StatementPeriod parseTwoDates(String d1, String d2) {
        java.time.LocalDate start = tryParseDate(d1);
        java.time.LocalDate end = tryParseDate(d2);
        if (start == null || end == null) return null;
        return new StatementPeriod(start, end);
    }

    private java.time.LocalDate tryParseDate(String rawDate) {
        rawDate = rawDate.trim();
        // Possible format patterns
        java.time.format.DateTimeFormatter[] formatters = new java.time.format.DateTimeFormatter[]{
            java.time.format.DateTimeFormatter.ofPattern("d MMMM uuuu"),
            java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu"),
            java.time.format.DateTimeFormatter.ofPattern("d/MM/uuuu"),
            java.time.format.DateTimeFormatter.ofPattern("d-M-uuuu"),
            java.time.format.DateTimeFormatter.ofPattern("uuuu-M-d"),
            java.time.format.DateTimeFormatter.ofPattern("uuuu/MM/d")
        };

        // Try lower/upper case normalization and common OCR fixes
        String normalized = rawDate.replaceAll("[^A-Za-z0-9/\\-\\s]", " ").replaceAll("\s+", " ");
        // Fix some common OCR month errors (e.g., 'Fruary' -> 'February') - minimal set
        normalized = normalized.replaceAll("(?i)f?ruary", "February");
        normalized = normalized.replaceAll("(?i)mar(en|ch)", "March");

        for (java.time.format.DateTimeFormatter fmt : formatters) {
            try {
                return java.time.LocalDate.parse(normalized, fmt);
            } catch (Exception ignored) { }
        }
        // Last resort try ISO parse
        try {
            return java.time.LocalDate.parse(normalized);
        } catch (Exception ignored) { }
        return null;
    }

    /**
     * Simple holder for parsed statement period dates.
     */
    public static class StatementPeriod {
        private final java.time.LocalDate start;
        private final java.time.LocalDate end;
        public StatementPeriod(java.time.LocalDate start, java.time.LocalDate end) {
            this.start = start; this.end = end;
        }
        public java.time.LocalDate getStart() { return start; }
        public java.time.LocalDate getEnd() { return end; }
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