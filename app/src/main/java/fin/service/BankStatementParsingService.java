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

import fin.model.BankTransaction;
import fin.model.Company;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing extracted bank statement text into structured transaction data
 * Handles business logic for transaction parsing, date conversion, and amount extraction
 */
public class BankStatementParsingService {
    
    private static final Pattern DATE_PATTERN = Pattern.compile("^(\\d{2})\\s+(\\d{2})");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\b(\\d{1,3}(?:,\\d{3})*(?:\\.\\d{2}))\\b");
    
    // Header/Footer patterns
    private static final Set<String> HEADER_FOOTER_PATTERNS = Set.of(
        "^\\s*BANK\\s+STATEMENT\\s*$",
        "^\\s*STATEMENT\\s+OF\\s+ACCOUNT\\s*$", 
        "^\\s*PAGE\\s+\\d+\\s*$",
        "^\\s*\\d+\\s*$",
        "^\\s*CONTINUED\\s*$",
        "^\\s*STATEMENT\\s+PERIOD\\s*:",
        "^\\s*ACCOUNT\\s+NUMBER\\s*:",
        "^\\s*BRANCH\\s+CODE\\s*:",
        "^\\s*Date\\s+Details\\s+Debits\\s+Credits\\s+Balance\\s*$",
        "^\\s*TOTAL\\s+(DEBITS|CREDITS)\\s*:",
        "^\\s*OPENING\\s+BALANCE\\s*:",
        "^\\s*CLOSING\\s+BALANCE\\s*:"
    );
    
    // Credit transaction patterns
    private static final Set<String> CREDIT_PATTERNS = Set.of(
        "CREDIT TRANSFER", "DEPOSIT", "IB PAYMENT FROM", "EXCESS INTEREST",
        "INTEREST CAPITALISED", "REVERSAL", "REFUND", "RTD-NOT PROVIDED FOR",
        "IB TRANSFER FROM"
    );
    
    // Debit transaction patterns  
    private static final Set<String> DEBIT_PATTERNS = Set.of(
        "SERVICE FEE", "WITHDRAWAL", "TRANSFER TO", "PAYMENT", "DEBIT ORDER",
        "ATM WITHDRAWAL", "CARD PURCHASE", "CHEQUE"
    );
    
    /**
     * Parse extracted text lines into bank transactions
     * @param textLines List of text lines from PDF
     * @param company Company information
     * @return List of parsed bank transactions
     */
    public List<BankTransaction> parseTransactions(List<String> textLines, Company company) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String currentDate = null;
        StringBuilder transactionDetails = new StringBuilder();
        BigDecimal debitAmount = null;
        BigDecimal creditAmount = null;
        BigDecimal balanceAmount = null;
        
        for (String line : textLines) {
            line = line.trim();
            
            if (line.isEmpty() || isHeaderOrFooterText(line)) {
                continue;
            }
            
            // Check if this is a new transaction line (starts with date)
            Matcher dateMatcher = DATE_PATTERN.matcher(line);
            
            if (dateMatcher.find()) {
                // Process previous transaction if exists
                if (currentDate != null && transactionDetails.length() > 0) {
                    BankTransaction transaction = createTransaction(
                        currentDate, transactionDetails.toString(), 
                        debitAmount, creditAmount, balanceAmount, company
                    );
                    if (transaction != null) {
                        transactions.add(transaction);
                    }
                }
                
                // Start new transaction
                TransactionData transactionData = parseTransactionLine(line);
                currentDate = transactionData.date;
                transactionDetails = new StringBuilder(transactionData.details);
                debitAmount = transactionData.debitAmount;
                creditAmount = transactionData.creditAmount;
                balanceAmount = transactionData.balanceAmount;
                
            } else if (currentDate != null) {
                // This is a continuation line
                
                // Clean and append continuation line
                String cleanedLine = cleanTransactionDetails(line);
                if (!cleanedLine.isEmpty()) {
                    if (transactionDetails.length() > 0) {
                        transactionDetails.append(" ");
                    }
                    transactionDetails.append(cleanedLine);
                    
                    // Check if this continuation line contains credit amounts
                    String combinedDetails = transactionDetails.toString();
                    BigDecimal extractedCredit = extractCreditAmountFromDetails(combinedDetails);
                    if (extractedCredit != null && extractedCredit.compareTo(BigDecimal.ZERO) > 0) {
                        creditAmount = extractedCredit;
                        debitAmount = null; // Override any debit amount from primary line
                    }
                }
            }
        }
        
        // Process last transaction
        if (currentDate != null && transactionDetails.length() > 0) {
            BankTransaction transaction = createTransaction(
                currentDate, transactionDetails.toString(),
                debitAmount, creditAmount, balanceAmount, company
            );
            if (transaction != null) {
                transactions.add(transaction);
            }
        }
        
        return transactions;
    }
    
    /**
     * Parse individual transaction line to extract date, details, and amounts
     */
    private TransactionData parseTransactionLine(String line) {
        TransactionData data = new TransactionData();
        
        // Extract date (MM DD format)
        Matcher dateMatcher = DATE_PATTERN.matcher(line);
        if (dateMatcher.find()) {
            String month = dateMatcher.group(1);
            String day = dateMatcher.group(2);
            data.date = String.format("%s/%s", day, month); // Convert to DD/MM
        }
        
        // Remove date from line to get details + amounts
        String remainingLine = line.replaceFirst("^\\d{2}\\s+\\d{2}\\s*", "").trim();
        
        // Extract all amounts from the line
        List<BigDecimal> amounts = extractAmountsFromLine(remainingLine);
        
        // Determine transaction type and amounts
        String upperLine = remainingLine.toUpperCase();
        boolean isCreditTransaction = CREDIT_PATTERNS.stream().anyMatch(upperLine::contains);
        boolean isDebitTransaction = DEBIT_PATTERNS.stream().anyMatch(upperLine::contains);
        boolean isBalanceOnly = upperLine.contains("BALANCE BROUGHT FORWARD");
        
        if (isBalanceOnly) {
            // Balance brought forward - only balance amount
            data.balanceAmount = amounts.isEmpty() ? BigDecimal.ZERO : amounts.get(amounts.size() - 1);
            data.details = cleanTransactionDetails(remainingLine);
        } else if (amounts.size() >= 2) {
            // Transaction with debit/credit and balance
            if (isCreditTransaction) {
                data.creditAmount = amounts.get(amounts.size() - 2);
                data.balanceAmount = amounts.get(amounts.size() - 1);
            } else if (isDebitTransaction || !isCreditTransaction) {
                data.debitAmount = amounts.get(amounts.size() - 2);
                data.balanceAmount = amounts.get(amounts.size() - 1);
            }
            data.details = extractDetailsFromLine(remainingLine, amounts);
        } else if (amounts.size() == 1) {
            // Single amount - could be balance only
            data.balanceAmount = amounts.get(0);
            data.details = extractDetailsFromLine(remainingLine, amounts);
        } else {
            // No amounts found - just details
            data.details = cleanTransactionDetails(remainingLine);
        }
        
        return data;
    }
    
    /**
     * Extract credit amounts from transaction details (for multiline transactions)
     */
    private BigDecimal extractCreditAmountFromDetails(String details) {
        String upperDetails = details.toUpperCase();
        
        // Look for credit patterns followed by amounts
        for (String creditPattern : CREDIT_PATTERNS) {
            if (upperDetails.contains(creditPattern)) {
                // Find amounts after the credit pattern
                int patternIndex = upperDetails.indexOf(creditPattern);
                String afterPattern = details.substring(patternIndex + creditPattern.length());
                
                Matcher amountMatcher = AMOUNT_PATTERN.matcher(afterPattern);
                if (amountMatcher.find()) {
                    String amountStr = amountMatcher.group(1).replace(",", "");
                    try {
                        return new BigDecimal(amountStr);
                    } catch (NumberFormatException e) {
                        // Continue searching
                    }
                }
            }
        }
        
        return null;
    }
    
    /**
     * Extract all amounts from a line
     */
    private List<BigDecimal> extractAmountsFromLine(String line) {
        List<BigDecimal> amounts = new ArrayList<>();
        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        
        while (matcher.find()) {
            String amountStr = matcher.group(1).replace(",", "");
            try {
                amounts.add(new BigDecimal(amountStr));
            } catch (NumberFormatException e) {
                // Skip invalid amounts
            }
        }
        
        return amounts;
    }
    
    /**
     * Extract transaction details by removing amounts from line
     */
    private String extractDetailsFromLine(String line, List<BigDecimal> amounts) {
        String details = line;
        
        // Remove amounts from the end of the line
        for (BigDecimal amount : amounts) {
            String amountStr = String.format("%.2f", amount).replace(".", "\\.");
            String amountPattern = "\\b" + amountStr.replace(",", ",?") + "\\b";
            details = details.replaceAll(amountPattern, "").trim();
        }
        
        return cleanTransactionDetails(details);
    }
    
    /**
     * Clean transaction details by removing header/footer contamination
     */
    private String cleanTransactionDetails(String details) {
        if (details == null) {
            return "";
        }
        
        // Remove aggressive truncation
        // Keep original length, just normalize spacing
        return details.replaceAll("\\s+", " ").trim();
    }
    
    /**
     * Check if line is header or footer text
     */
    private boolean isHeaderOrFooterText(String line) {
        String upperLine = line.toUpperCase().trim();
        
        return HEADER_FOOTER_PATTERNS.stream().anyMatch(pattern -> 
            upperLine.matches(pattern.replace("$", "").replace("^", ""))
        );
    }
    
    /**
     * Create BankTransaction from parsed data
     */
    private BankTransaction createTransaction(String date, String details, 
                                           BigDecimal debitAmount, BigDecimal creditAmount, 
                                           BigDecimal balanceAmount, Company company) {
        
        if (details.isEmpty()) {
            return null;
        }
        
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(company.getId());
        transaction.setTransactionDate(parseDate(date));
        transaction.setDetails(details);
        transaction.setDebitAmount(debitAmount != null ? debitAmount : BigDecimal.ZERO);
        transaction.setCreditAmount(creditAmount != null ? creditAmount : BigDecimal.ZERO);
        transaction.setBalance(balanceAmount != null ? balanceAmount : BigDecimal.ZERO);
        
        return transaction;
    }
    
    /**
     * Parse date from DD/MM format
     */
    private LocalDate parseDate(String dateStr) {
        try {
            // Assume current year for DD/MM format
            String fullDateStr = dateStr + "/" + LocalDate.now().getYear();
            return LocalDate.parse(fullDateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException e) {
            return LocalDate.now(); // Fallback
        }
    }
    
    /**
     * Helper class for transaction data
     */
    private static class TransactionData {
        String date;
        String details = "";
        BigDecimal debitAmount;
        BigDecimal creditAmount; 
        BigDecimal balanceAmount;
    }
}
