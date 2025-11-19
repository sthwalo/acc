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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document text extractor that wraps PdfTextExtractionService
 * Provides the interface expected by BankStatementProcessingService
 */
public class DocumentTextExtractor {
    private final PdfTextExtractionService pdfService;
    private String accountNumber;
    private String statementPeriod;

    public DocumentTextExtractor() {
        this.pdfService = new PdfTextExtractionService();
        this.accountNumber = null;
        this.statementPeriod = null;
    }

    /**
     * Parse document and extract text lines
     * @param file PDF file to parse
     * @return List of text lines
     * @throws RuntimeException if parsing fails
     */
    public List<String> parseDocument(File file) {
        try {
            List<String> lines = pdfService.extractTextLines(file.getAbsolutePath());

            // Extract account number and statement period from the text
            this.accountNumber = extractAccountNumber(lines);
            this.statementPeriod = extractStatementPeriod(lines);

            return lines;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse document: " + file.getName(), e);
        }
    }

    /**
     * Get the account number extracted from the document
     * @return Account number or null if not found
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Get the statement period extracted from the document
     * @return Statement period or null if not found
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
            line.toLowerCase().contains("statement no") ||
            line.toLowerCase().contains("vat reg") ||
            line.toLowerCase().contains("po box") ||
            line.toLowerCase().contains("these fees include")) {
            return false;
        }

        // Look for transaction patterns
        // 1. Lines with monetary amounts (digits followed by decimal point and 2 digits)
        if (line.matches(".*\\d+\\.\\d{2}.*")) {
            return true;
        }

        // 2. Lines with transaction keywords
        String[] transactionKeywords = {
            "payment", "fee", "charge", "deposit", "withdrawal",
            "debit", "credit", "atm", "eft", "salary", "interest", "dividend",
            "balance", "transfer", "ib payment"
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

    /**
     * Extract account number from text lines using regex patterns
     * @param lines Text lines from document
     * @return Account number or null if not found
     */
    private String extractAccountNumber(List<String> lines) {
        // Common patterns for account numbers in bank statements
        Pattern[] patterns = {
            Pattern.compile("Account\\s+Number\\s*:\\s*([0-9\\-]+)"),
            Pattern.compile("Account\\s*#\\s*:\\s*([0-9\\-]+)"),
            Pattern.compile("Account:\\s*([0-9\\-]+)"),
            Pattern.compile("Acc\\s+No\\s*:\\s*([0-9\\-]+)"),
            Pattern.compile("([0-9]{4}[\\-]?[0-9]{4}[\\-]?[0-9]{4}[\\-]?[0-9]{4})") // Standard bank account format
        };

        for (String line : lines) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }

        return null;
    }

    /**
     * Extract statement period from text lines using regex patterns
     * @param lines Text lines from document
     * @return Statement period or null if not found
     */
    private String extractStatementPeriod(List<String> lines) {
        // Common patterns for statement periods in bank statements
        Pattern[] patterns = {
            Pattern.compile("Statement\\s+Period\\s*:\\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s*to\\s*[0-9]{1,2}/[0-9]{1,2}/[0-9]{4})"),
            Pattern.compile("Period\\s*:\\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4}\\s*to\\s*[0-9]{1,2}/[0-9]{1,2}/[0-9]{4})"),
            Pattern.compile("From\\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})\\s*to\\s*([0-9]{1,2}/[0-9]{1,2}/[0-9]{4})"),
            Pattern.compile("([0-9]{1,2}\\s+[A-Za-z]+\\s+[0-9]{4}\\s*to\\s*[0-9]{1,2}\\s+[A-Za-z]+\\s+[0-9]{4})") // e.g., "01 Jan 2024 to 31 Jan 2024"
        };

        for (String line : lines) {
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }

        return null;
    }
}
