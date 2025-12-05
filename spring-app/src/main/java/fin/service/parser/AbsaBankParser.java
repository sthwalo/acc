package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.StandardizedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Parser for Absa Bank statement format.
 * Handles OCR-extracted space-separated format with column-based parsing.
 * Supports multiline descriptions and various transaction types.
 *
 * CRITICAL FIXES:
 * - Column-based parsing instead of greedy regex
 * - Handles space-delimited balances ("54 882.66")
 * - Prevents amounts from appearing in description field
 * - Accumulates multiline descriptions
 */
@Component
public class AbsaBankParser extends AbstractMultilineTransactionParser {
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

            // Split by single spaces to get all parts
            String[] parts = rest.split("\\s+");

            // Find the transition from description to amounts
            // Work from right to left, first non-numeric marks the boundary
            int firstAmountIndex = parts.length; // Default to end if all numeric
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!isNumeric(parts[i])) {
                    firstAmountIndex = i + 1; // Amounts start after this non-numeric part
                    break;
                }
            }

            // Build description from parts before amounts
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < firstAmountIndex; i++) {
                if (description.length() > 0) description.append(" ");
                description.append(parts[i]);
            }

            // Parse amounts - group consecutive numeric parts intelligently
            java.util.List<BigDecimal> amounts = new java.util.ArrayList<>();
            StringBuilder currentAmount = new StringBuilder();

            for (int i = firstAmountIndex; i < parts.length; i++) {
                String part = parts[i];
                if (isNumeric(part)) {
                    if (currentAmount.length() > 0) currentAmount.append(" ");
                    currentAmount.append(part);

                    // If this part has a decimal point, it's likely a complete amount
                    if (part.contains(".")) {
                        amounts.add(parseAmount(currentAmount.toString()));
                        currentAmount = new StringBuilder();
                    }
                } else {
                    // If we have a current amount, add it
                    if (currentAmount.length() > 0) {
                        amounts.add(parseAmount(currentAmount.toString()));
                        currentAmount = new StringBuilder();
                    }
                }
            }

            // Add the last amount if any
            if (currentAmount.length() > 0) {
                amounts.add(parseAmount(currentAmount.toString()));
            }

            // Assign amounts based on count
            BigDecimal balance = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            BigDecimal serviceFee = BigDecimal.ZERO;

            if (amounts.size() >= 1) {
                // Last amount is always balance
                balance = amounts.get(amounts.size() - 1);
            }
            if (amounts.size() >= 2) {
                // Second to last is transaction amount
                amount = amounts.get(amounts.size() - 2);
            }
            if (amounts.size() >= 3) {
                // Third to last is service fee
                serviceFee = amounts.get(amounts.size() - 3);
            }

            // Determine transaction type
            boolean isDebit = isLikelyDebit(description.toString());
            boolean isPureServiceFee = description.toString().toLowerCase().contains("fee") ||
                                      description.toString().toLowerCase().contains("charge");

            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;

            if (isPureServiceFee && amounts.size() == 2) {
                // Pure service fee transaction (fee amount + balance)
                serviceFee = amount;
                amount = BigDecimal.ZERO;
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                // Negative amount indicates credit (refund)
                creditAmount = amount.negate();
            } else if (isDebit) {
                debitAmount = amount;
            } else {
                creditAmount = amount;
            }

            return new StandardizedTransaction.Builder()
                    .date(transactionDate)
                    .description(description.toString().trim())
                    .serviceFee(serviceFee)
                    .debitAmount(debitAmount)
                    .creditAmount(creditAmount)
                    .balance(balance)
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
     * Check if string represents a numeric amount (including partial amounts)
     */
    private boolean isNumeric(String str) {
        return str != null && str.matches("[\\d,\\s]+(?:\\.\\d{2})?-?");
    }

    /**
     * Determine if transaction is likely a debit based on description keywords
     */
    private boolean isLikelyDebit(String description) {
        String lower = description.toLowerCase();
        return lower.contains("withdrawal") ||
               lower.contains("atm") ||
               lower.contains("debit") ||
               lower.contains("purchase") ||
               lower.contains("debit order") ||
               lower.contains("payment") && (lower.contains("to") || lower.contains("out"));
    }

    /**
     * Reset parser state
     */
    @Override
    public void reset() {
        super.reset();
        isAbsaStatement = false;
    }
}