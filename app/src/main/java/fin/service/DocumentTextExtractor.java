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

package fin.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Unified document text extraction service that handles multiple document types.
 * This consolidates the functionality from:
 * - PdfTextExtractor
 * - PdfTextExtractionService
 * - TextExtractor
 */
public class DocumentTextExtractor {
    private static final Pattern STATEMENT_DATE_PATTERN = Pattern.compile("Statement from\\s+(.+)");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("Account number:\\s*(\\d+[\\s\\d]*\\d+)");
    private String accountNumber;
    private String statementPeriod;
    private boolean isStandardBankFormat = false;

    /**
     * Parses a document and extracts its content line by line.
     * 
     * @param file The document file to parse
     * @return List of text lines from the document
     * @throws IOException if there's an error reading the file
     */
    public List<String> parseDocument(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            // First pass: detect bank format
            PDFTextStripper firstPassStripper = createStripper();
            String firstPassText = firstPassStripper.getText(document);
            detectBankFormat(firstPassText);
            
            // Second pass: extract with appropriate format
            PDFTextStripper stripper = createStripper();
            
            // Use layout preservation for Standard Bank tabular format
            if (isStandardBankFormat) {
                stripper.setLineSeparator("\n");
                stripper.setWordSeparator(" ");
                stripper.setArticleStart("");
                stripper.setArticleEnd("");
            }
            
            String text = stripper.getText(document);
            List<String> lines = Arrays.stream(text.split("\\r?\\n"))
                                     .map(line -> isStandardBankFormat ? line : line.trim()) // Preserve spacing for Standard Bank
                                     .filter(line -> !line.isEmpty())
                                     .collect(Collectors.toList());
            
            // Extract metadata while processing lines
            extractMetadata(lines);
            
            return lines;
        }
    }

    /**
     * Detects the bank format from the PDF text
     */
    private void detectBankFormat(String text) {
        // Check for Standard Bank indicators
        if (text.contains("STANDARD BANK") || 
            text.contains("standardbank.co.za") ||
            text.contains("BizDirect Contact Centre") ||
            (text.contains("Service") && text.contains("Fee") && 
             text.contains("Debits") && text.contains("Credits") && 
             text.contains("Date") && text.contains("Balance"))) {
            isStandardBankFormat = true;
            System.out.println("📊 Detected Standard Bank tabular format");
        }
    }

    /**
     * Returns true if this document is in Standard Bank tabular format
     */
    public boolean isStandardBankFormat() {
        return isStandardBankFormat;
    }

    private void extractMetadata(List<String> lines) {
        for (String line : lines) {
            // Try to find account number
            Matcher accountMatcher = ACCOUNT_NUMBER_PATTERN.matcher(line);
            if (accountMatcher.find()) {
                this.accountNumber = accountMatcher.group(1).replaceAll("\\s+", "");
                continue;
            }
            
            // Try to find statement period
            Matcher dateMatcher = STATEMENT_DATE_PATTERN.matcher(line);
            if (dateMatcher.find()) {
                this.statementPeriod = dateMatcher.group(1).trim();
                continue;
            }
        }
    }

    /**
     * Returns the account number extracted from the document.
     * Must call parseDocument first.
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Returns the statement period extracted from the document.
     * Must call parseDocument first.
     */
    public String getStatementPeriod() {
        return statementPeriod;
    }

    /**
     * Determines if a line represents a transaction.
     */
    public boolean isTransaction(String line) {
        // Skip common header/footer lines
        if (isHeaderOrFooterLine(line)) {
            return false;
        }
        
        // Check if line has amount format or common transaction indicators
        return line.matches(".*\\d+\\.\\d{2}-?\\s*[DR]?\\s*$") || // Matches amount at end
               line.matches(".*\\d+\\.\\d{2}-?\\s*##\\s*$") ||    // Matches amount with ##
               line.startsWith("IB PAYMENT") ||
               line.startsWith("DEBIT ORDER") ||
               line.startsWith("CREDIT ORDER") ||
               line.startsWith("BALANCE") ||
               line.contains("FEE") ||
               line.contains("TRANSFER");
    }
    
    private boolean isHeaderOrFooterLine(String line) {
        return line.contains("Page") ||
               line.contains("VAT Reg") ||
               line.contains("Statement No") ||
               line.contains("PO BOX") ||
               line.startsWith("##") ||
               line.contains("Please verify") ||
               // Table headers
               line.matches(".*(?:Fee|Debits|Credits|Date|Balance).*(?:Fee|Debits|Credits|Date|Balance).*") ||
               line.trim().equals("Fee Debits Credits Date Balance") ||
               line.trim().equals("Date Details Debit Credit Balance") ||
               // Common table header patterns
               line.matches("^\\s*(?:Date|Details?|Description|Amount|Debit|Credit|Balance|Reference)(?:\\s+(?:Date|Details?|Description|Amount|Debit|Credit|Balance|Reference))*\\s*$");
    }

    private PDFTextStripper createStripper() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        return stripper;
    }
}
