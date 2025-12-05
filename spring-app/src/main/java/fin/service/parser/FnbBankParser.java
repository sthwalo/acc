package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.StandardizedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Parser for First National Bank (FNB) statement format.
 * Handles "Cr" suffix detection and multiline description accumulation.
 *
 * CRITICAL FIXES:
 * - Detects "Cr" suffix for credits
 * - Handles multiline descriptions
 * - Prevents misclassification of credits as debits
 */
@Component
public class FnbBankParser extends AbstractMultilineTransactionParser {
    private static final Logger LOGGER = Logger.getLogger(FnbBankParser.class.getName());

    // Pattern to detect FNB bank statements
    private static final Pattern FNB_HEADER_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:FNB|First National Bank|FIRST NATIONAL BANK|Account Number|Business Account|Cheque Account).*$"
    );

    // Pattern to skip header/footer lines
    private static final Pattern SKIP_PATTERN = Pattern.compile(
        "^\\s*(?:Date|Transaction|Description|Amount|Balance|Reference|" +
        "First National Bank|FNB|Account Summary|Statement|Page \\d|Business Account|" +
        "Cheque Account|Savings Account|Account Number|Branch|VAT Number|" +
        "Customer Number|Statement Period|Generated|Opening Balance|Closing Balance|" +
        "Total Debits|Total Credits|Available Balance|Uncleared Funds|Hold Amount).*$",
        Pattern.CASE_INSENSITIVE
    );

    // Date formatter for FNB statements
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH);

    // State tracking
    private boolean isFnbStatement = false;
    private int statementYear = 2024; // Default fallback
    private LocalDate lastTransactionDate = null;

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        // Check if this is an FNB statement (first time detection)
        if (!isFnbStatement && FNB_HEADER_PATTERN.matcher(line).matches()) {
            isFnbStatement = true;
            return false; // Header line itself is not parseable
        }

        // Skip header/footer lines
        if (SKIP_PATTERN.matcher(line).matches()) {
            return false;
        }

        // Check if it's a bank charge line (starts with #)
        if (line.trim().startsWith("#")) {
            return true;
        }

        // Check if it looks like an FNB transaction line (starts with date)
        if (line.matches("^(?:\\d{1,2}/\\d{1,2}/\\d{4}|\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))\\s+.+")) {
            // Validate the date format
            try {
                String[] lineParts = line.trim().split("\\s+");
                String dateStr = lineParts[0];
                if (!dateStr.contains("/") && lineParts.length > 1 && !lineParts[1].matches("\\d{4}")) {
                    dateStr += " " + lineParts[1];
                }
                parseDate(dateStr, context != null && context.getStatementDate() != null ? context.getStatementDate().getYear() : 2024);
                return true;
            } catch (Exception e) {
                return false; // Invalid date format
            }
        }

        return false;
    }

    @Override
    protected boolean isContinuationLine(String line) {
        // FNB continuation lines don't start with dates or #
        String trimmed = line.trim();
        return !trimmed.startsWith("#") && 
               !line.matches("^\\d{1,2}/\\d{1,2}/\\d{4}\\s+.+") && 
               trimmed.length() > 0;
    }

    @Override
    protected StandardizedTransaction parseTransactionLine(String line) {
        try {
            // Check if this is a bank charge line (starts with #)
            if (line.trim().startsWith("#")) {
                return parseBankChargeLine(line);
            }

            // Extract date (first part until space, then month)
            // FNB dates are either "DD/MM/YYYY" or "DD MMM"
            String[] lineParts = line.trim().split("\\s+");
            String dateStr = lineParts[0];
            if (!dateStr.contains("/") && lineParts.length > 1 && !lineParts[1].matches("\\d{4}")) {
                // If not DD/MM/YYYY and second part is not year, it's "DD MMM"
                dateStr += " " + lineParts[1];
            }
            LocalDate transactionDate = parseDate(dateStr, statementYear);

            // Get the rest after date
            String rest = line.substring(line.indexOf(dateStr) + dateStr.length()).trim();

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
            boolean hasCr = false;

            for (int i = firstAmountIndex; i < parts.length; i++) {
                String part = parts[i];
                if (isNumeric(part)) {
                    if (currentAmount.length() > 0) currentAmount.append(" ");
                    currentAmount.append(part);

                    // Set hasCr only for transaction amounts, not balance (last amount)
                    if (part.endsWith("Cr") && i < parts.length - 1) {
                        hasCr = true;
                    }

                    // If this part has a decimal point or Cr, it's likely a complete amount
                    if (part.contains(".") || part.endsWith("Cr")) {
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

            if (amounts.size() >= 1) {
                // Last amount is always balance
                balance = amounts.get(amounts.size() - 1);
            }
            if (amounts.size() >= 2) {
                // Second to last is transaction amount
                amount = amounts.get(amounts.size() - 2);
            }

            // Determine transaction type based on "Cr" suffix and description
            boolean isCredit = hasCr || isLikelyCredit(description.toString());

            BigDecimal debitAmount = BigDecimal.ZERO;
            BigDecimal creditAmount = BigDecimal.ZERO;
            BigDecimal serviceFee = BigDecimal.ZERO;

            // Check if this is a service fee (contains "Fee" or similar)
            if (description.toString().toLowerCase().contains("fee") ||
                description.toString().toLowerCase().contains("charge")) {
                serviceFee = amount;
            } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
                // Negative amount indicates credit (refund)
                creditAmount = amount.negate();
            } else if (isCredit) {
                creditAmount = amount;
            } else {
                debitAmount = amount;
            }

            // Reformat description if it contains reference
            String finalDescription = description.toString().trim();
            if (finalDescription.contains("Ref:")) {
                // Extract the part before "Ref:" and the reference
                int refIndex = finalDescription.indexOf("Ref:");
                String beforeRef = finalDescription.substring(0, refIndex).trim();
                String refPart = finalDescription.substring(refIndex).trim(); // "Ref: 123456"
                finalDescription = beforeRef + " (" + refPart + ")";
            }

            // Track this transaction date for potential bank charges
            lastTransactionDate = transactionDate;

            return new StandardizedTransaction.Builder()
                    .date(transactionDate)
                    .description(finalDescription)
                    .serviceFee(serviceFee)
                    .debitAmount(debitAmount)
                    .creditAmount(creditAmount)
                    .balance(balance)
                    .build();

        } catch (Exception e) {
            LOGGER.warning("Failed to parse FNB transaction line: " + line + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * Parse bank charge line starting with # prefix.
     * Format: # Description Amount Balance
     * Example: # Service Fee 5.50 10294.50
     */
    private StandardizedTransaction parseBankChargeLine(String line) {
        try {
            // Remove # prefix and trim
            String content = line.substring(1).trim();
            
            // Split by spaces
            String[] parts = content.split("\\s+");
            
            // Find amounts from right to left
            // Last part is balance, second to last is fee amount
            if (parts.length < 3) {
                LOGGER.warning("Invalid bank charge line format: " + line);
                return null;
            }
            
            BigDecimal balance = parseAmount(parts[parts.length - 1]);
            BigDecimal serviceFee = parseAmount(parts[parts.length - 2]);
            
            // Description is everything before the amounts
            StringBuilder description = new StringBuilder();
            for (int i = 0; i < parts.length - 2; i++) {
                if (description.length() > 0) description.append(" ");
                description.append(parts[i]);
            }
            
            // Bank charges don't have their own date - they use the previous transaction's date
            LocalDate chargeDate = lastTransactionDate != null ? lastTransactionDate : LocalDate.of(statementYear, 1, 1);
            
            return new StandardizedTransaction.Builder()
                    .date(chargeDate)
                    .description(description.toString())
                    .serviceFee(serviceFee)
                    .debitAmount(BigDecimal.ZERO)
                    .creditAmount(BigDecimal.ZERO)
                    .balance(balance)
                    .build();
                    
        } catch (Exception e) {
            LOGGER.warning("Failed to parse FNB bank charge line: " + line + " - " + e.getMessage());
            return null;
        }
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        // Set statement year from context
        if (context != null && context.getStatementDate() != null) {
            statementYear = context.getStatementDate().getYear();
        }

        if (!canParse(line, context)) {
            return null;
        }

        StandardizedTransaction stdTx = parseTransactionLine(line);
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
        if (stdTx.getServiceFee().compareTo(BigDecimal.ZERO) > 0) {
            type = TransactionType.SERVICE_FEE;
            amount = stdTx.getServiceFee();
        } else if (stdTx.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            type = TransactionType.DEBIT;
            amount = stdTx.getDebitAmount();
        } else if (stdTx.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            type = TransactionType.CREDIT;
            amount = stdTx.getCreditAmount();
        } else {
            // Balance-only transaction
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
     * Parse date from FNB format (DD/MM/YYYY or DD MMM)
     */
    private LocalDate parseDate(String dateStr, int year) {
        try {
            if (dateStr.contains("/")) {
                // DD/MM/YYYY format
                return LocalDate.parse(dateStr, DATE_FORMATTER);
            } else {
                // DD MMM format, use provided year
                String[] parts = dateStr.split("\\s+");
                int day = Integer.parseInt(parts[0]);
                String monthStr = parts[1].toLowerCase();
                int month = switch (monthStr) {
                    case "jan" -> 1;
                    case "feb" -> 2;
                    case "mar" -> 3;
                    case "apr" -> 4;
                    case "may" -> 5;
                    case "jun" -> 6;
                    case "jul" -> 7;
                    case "aug" -> 8;
                    case "sep" -> 9;
                    case "oct" -> 10;
                    case "nov" -> 11;
                    case "dec" -> 12;
                    default -> throw new IllegalArgumentException("Invalid month: " + monthStr);
                };
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }

    /**
     * Parse amount, handling "Cr" suffix and spaces/commas
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Remove "Cr" suffix and clean
        String clean = amountStr.replace("Cr", "").replace(",", "").replace(" ", "").trim();

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
        return str != null && str.matches("[\\d,\\s]*\\d+\\.\\d{2}-?(?:Cr)?");
    }

    /**
     * Determine if transaction is likely a credit based on description keywords
     */
    private boolean isLikelyCredit(String description) {
        String lower = description.toLowerCase();
        return lower.contains("credit") ||
               lower.contains("deposit") ||
               lower.contains("transfer in") ||
               lower.contains("interest") ||
               lower.contains("dividend") ||
               lower.contains("salary") ||
               lower.contains("income");
    }

    /**
     * Reset parser state
     */
    @Override
    public void reset() {
        super.reset();
        isFnbStatement = false;
        lastTransactionDate = null;
    }
}