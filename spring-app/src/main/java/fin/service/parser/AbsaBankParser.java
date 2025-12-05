package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.StandardizedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Absa Bank statement format.
 * Handles OCR-extracted space-separated format with column-based parsing.
 * Supports multiline descriptions and various transaction types.
 *
 * CRITICAL FIXES:
 * - Regex-based amount extraction from right to left
 * - Handles space-delimited balances ("54 882.66")
 * - Prevents amounts from appearing in description field
 * - Accumulates multiline descriptions
 */
@Component
public class AbsaBankParser extends AbstractMultilineTransactionParser {
    
    /**
     * Helper class to track amount positions in the line for proper column assignment
     */
    private static class AmountMatch {
        final String amountStr;
        final int position;
        
        AmountMatch(String amountStr, int position) {
            this.amountStr = amountStr;
            this.position = position;
        }
    }
    private static final Logger LOGGER = Logger.getLogger(AbsaBankParser.class.getName());

    // Pattern to detect Absa bank statements
    private static final Pattern ABSA_HEADER_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:ABSA|Absa Bank|Account Number|Cheque Account).*$"
    );

    // Pattern to skip header/footer lines
    private static final Pattern SKIP_PATTERN = Pattern.compile(
        "^\\s*(?:Date|Transaction Description|Charge|Debit Amount|Credit Amount|Balance|" +
        "Your transactions|Account Type|Statement no|Client VAT|Account Summary|" +
        "Page \\d+ of \\d+|ABSA Bank Limited|Authorised Financial|Registration Number|" +
        "CSP\\d+|Return address|Private Bag|Cheque account statement|Issued on).*$",
        Pattern.CASE_INSENSITIVE
    );

    // State tracking
    private boolean isAbsaStatement = false;
    private BigDecimal previousBalance = null;

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        // Check if this is an Absa statement (first time detection)
        if (!isAbsaStatement && ABSA_HEADER_PATTERN.matcher(line).matches()) {
            isAbsaStatement = true;
            return false; // Header line itself is not parseable
        }

        // Skip header/footer lines
        if (SKIP_PATTERN.matcher(line).matches()) {
            return false;
        }

        // Check if it looks like an Absa transaction line (starts with date)
        if (line.matches("^\\d{1,2}/\\d{1,2}/\\d{4}\\s+.+")) {
            // Validate the date format
            try {
                String dateStr = line.substring(0, 10).trim();
                parseDateFromString(dateStr);
                return true;
            } catch (Exception e) {
                return false; // Invalid date format
            }
        }

        return false;
    }

    @Override
    protected boolean isContinuationLine(String line) {
        // Absa continuation lines start with spaces (no date)
        return line.matches("^\\s{5,}.+") && !line.matches("^\\d{1,2}/\\d{1,2}/\\d{4}\\s+.+");
    }

    @Override
    protected StandardizedTransaction parseTransactionLine(String line) {
        try {
            // Extract date (first 10 characters: "23/02/2023")
            String dateStr = line.substring(0, 10).trim();
            String rest = line.substring(10).trim();

            // Parse date
            LocalDate transactionDate = parseDateFromString(dateStr);

            // IMPROVED: Use regex to extract amounts from right to left
            // Pattern matches decimal numbers with optional spaces/commas as thousand separators
            // Example: "600.00", "54 882.66", "1,300.00", "10.00 T"
            Pattern amountPattern = Pattern.compile("([\\d\\s,]+\\.\\d{2})(?:\\s*[A-Z])?");
            Matcher matcher = amountPattern.matcher(rest);

            // Collect all amounts with their positions
            java.util.List<AmountMatch> matches = new java.util.ArrayList<>();
            while (matcher.find()) {
                String amountStr = matcher.group(1);
                int position = matcher.start();
                matches.add(new AmountMatch(amountStr, position));
            }

            // Sort matches by position (left to right)
            matches.sort(java.util.Comparator.comparingInt(m -> m.position));

            // Description is everything before the first amount
            int descriptionEndPos = matches.isEmpty() ? rest.length() : matches.get(0).position;
            String description = rest.substring(0, descriptionEndPos).trim();

            // Parse amounts from matches
            java.util.List<BigDecimal> amounts = new java.util.ArrayList<>();
            for (AmountMatch match : matches) {
                amounts.add(parseAmount(match.amountStr));
            }

            // Assign amounts based on Absa column structure (right to left):
            // 4 columns: [description] [charge] [debit] [credit] [balance]
            // Pattern 1: [desc] [charge] [debit] [credit] [balance] (4 amounts)
            // Pattern 2: [desc] [charge] [debit/credit] [balance] (3 amounts)
            // Pattern 3: [desc] [debit/credit] [balance] (2 amounts)
            // Pattern 4: [desc] [balance] (1 amount - opening balance)
            
            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;
            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal serviceFee = BigDecimal.ZERO;

            if (amounts.size() >= 1) {
                // Rightmost: Current balance
                currentBalance = amounts.get(amounts.size() - 1);
            }
            if (amounts.size() >= 2) {
                // Second from right: Could be credit amount OR debit amount
                BigDecimal secondAmount = amounts.get(amounts.size() - 2);
                
                // Determine if credit or debit by balance comparison
                if (previousBalance != null) {
                    BigDecimal balanceChange = currentBalance.subtract(previousBalance);
                    if (balanceChange.compareTo(BigDecimal.ZERO) > 0) {
                        // Balance increased → credit transaction
                        creditAmount = secondAmount;
                    } else if (balanceChange.compareTo(BigDecimal.ZERO) < 0) {
                        // Balance decreased → debit transaction
                        debitAmount = secondAmount;
                    }
                } else {
                    // No previous balance - use description keywords as hint
                    if (isLikelyDebit(description)) {
                        debitAmount = secondAmount;
                    } else {
                        creditAmount = secondAmount;
                    }
                }
            }
            if (amounts.size() >= 3) {
                // Third from right: Could be debit (if 2nd was credit) OR charge
                BigDecimal thirdAmount = amounts.get(amounts.size() - 3);
                
                // If we already have a credit, this might be a debit in same transaction
                // OR it's a service fee
                if (thirdAmount.compareTo(new BigDecimal("100")) < 0) {
                    // Small amount (< 100) is likely a service fee
                    serviceFee = thirdAmount;
                } else if (creditAmount.compareTo(BigDecimal.ZERO) > 0 && debitAmount.equals(BigDecimal.ZERO)) {
                    // We have credit but no debit - this could be the debit
                    debitAmount = thirdAmount;
                } else if (debitAmount.compareTo(BigDecimal.ZERO) > 0 && creditAmount.equals(BigDecimal.ZERO)) {
                    // We have debit but no credit - this could be the credit
                    creditAmount = thirdAmount;
                } else {
                    // Default to service fee
                    serviceFee = thirdAmount;
                }
            }
            if (amounts.size() >= 4) {
                // Fourth from right: Service fee/charge (leftmost amount column)
                serviceFee = amounts.get(amounts.size() - 4);
            }
            
            // Update previous balance for next transaction
            previousBalance = currentBalance;

            return new StandardizedTransaction.Builder()
                    .date(transactionDate)
                    .description(description.toString().trim())
                    .serviceFee(serviceFee)
                    .debitAmount(debitAmount)
                    .creditAmount(creditAmount)
                    .balance(currentBalance)
                    .build();

        } catch (Exception e) {
            LOGGER.warning("Failed to parse Absa transaction line: " + line + " - " + e.getMessage());
            return null;
        }
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        // Use multiline parsing logic
        StandardizedTransaction stdTx = handleMultilineParsing(line, canParse(line, context));

        // For single-line transactions, get the completed transaction
        if (stdTx == null) {
            stdTx = getCompletedTransaction();
        }

        if (stdTx == null) {
            return null;
        }

        // Convert StandardizedTransaction to ParsedTransaction
        return convertToParsedTransaction(stdTx);
    }

    /**
     * Convert StandardizedTransaction to ParsedTransaction for interface compatibility
     */
    private ParsedTransaction convertToParsedTransaction(StandardizedTransaction stdTx) {
        TransactionType type;
        BigDecimal amount;

        // Determine type and amount based on StandardizedTransaction
        if (stdTx.getServiceFee().compareTo(BigDecimal.ZERO) > 0 && 
            stdTx.getDebitAmount().equals(BigDecimal.ZERO) && 
            stdTx.getCreditAmount().equals(BigDecimal.ZERO)) {
            // Pure service fee transaction
            type = TransactionType.SERVICE_FEE;
            amount = stdTx.getServiceFee();
        } else if (stdTx.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            type = TransactionType.DEBIT;
            amount = stdTx.getDebitAmount();
        } else if (stdTx.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            type = TransactionType.CREDIT;
            amount = stdTx.getCreditAmount();
        } else {
            // Balance-only transaction (like opening balance)
            type = TransactionType.CREDIT;
            amount = BigDecimal.ZERO;
        }

        return new ParsedTransaction.Builder()
                .type(type)
                .description(stdTx.getDescription())
                .amount(amount)
                .date(stdTx.getDate())
                .balance(stdTx.getBalance())
                .hasServiceFee(stdTx.hasServiceFee())
                .build();
    }

    /**
     * Parse date from DD/MM/YYYY format
     */
    private LocalDate parseDateFromString(String dateStr) {
        String[] parts = dateStr.split("/");
        int day = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int year = Integer.parseInt(parts[2]);
        return LocalDate.of(year, month, day);
    }

    /**
     * Parse amount, handling both space and comma delimiters
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Handle both "54,882.66" and "54 882.66"
        String clean = amountStr.replace(",", "").replace(" ", "").trim();

        boolean isNegative = clean.endsWith("-");
        if (isNegative) {
            clean = clean.substring(0, clean.length() - 1);
        }

        BigDecimal amount = new BigDecimal(clean);
        return isNegative ? amount.negate() : amount;
    }

    /**
     * Determine if transaction is likely a debit based on description keywords
     * CRITICAL: ATM deposits are CREDITS, not debits!
     */
    private boolean isLikelyDebit(String description) {
        String lower = description.toLowerCase();
        
        // Explicit CREDIT indicators (take precedence)
        if (lower.contains("deposit") || 
            lower.contains("credit") ||
            lower.contains("payment fr") ||  // "Payment Fr" = payment FROM (incoming)
            lower.contains("acb credit") ||
            lower.contains("immediate trf cr") ||
            lower.contains("refund")) {
            return false; // This is a CREDIT
        }
        
        // Explicit DEBIT indicators
        return lower.contains("withdrawal") ||
               lower.contains("atm withdrawal") ||
               lower.contains("debit order") ||
               lower.contains("purchase") ||
               lower.contains("payment to") ||    // "Payment To" = payment TO someone (outgoing)
               lower.contains("payment dt") ||    // "Digital Payment Dt" = debit payment
               (lower.contains("payment") && !lower.contains("fr")); // Generic payment (not "from")
    }

    /**
     * Reset parser state
     */
    @Override
    public void reset() {
        super.reset();
        isAbsaStatement = false;
        previousBalance = null;
    }
}