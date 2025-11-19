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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts text content from PDF documents for bank statement processing.
 * Uses Apache PDFBox for reliable PDF text extraction.
 */
@Service
public class DocumentTextExtractor {

    private String accountNumber;
    private String statementPeriod;

    /**
     * Parse a PDF document and extract text lines.
     */
    public List<String> parseDocument(File pdfFile) throws IOException {
        List<String> lines = new ArrayList<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            // Split into lines and clean up
            String[] rawLines = text.split("\\r?\\n");
            for (String line : rawLines) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                    // Extract metadata while processing
                    extractMetadata(line);
                }
            }
        }

        return lines;
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