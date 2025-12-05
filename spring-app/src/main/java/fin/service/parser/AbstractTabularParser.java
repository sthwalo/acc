package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.StandardizedTransaction;
import fin.model.parser.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;

/**
 * Abstract base class for tabular bank statement parsers.
 * Provides common utility methods for parsing amounts, dates, and descriptions
 * that are shared across different bank formats.
 */
public abstract class AbstractTabularParser implements TransactionParser {
    protected static final Logger LOGGER = Logger.getLogger(AbstractTabularParser.class.getName());

    // Common date formats used across banks
    protected static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    protected static final DateTimeFormatter DD_MM_YY = DateTimeFormatter.ofPattern("dd/MM/yy");
    protected static final DateTimeFormatter MM_DD_YYYY = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    protected static final DateTimeFormatter YYYY_MM_DD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Parse amount string into BigDecimal.
     * Handles commas, parentheses for negatives, and currency symbols.
     *
     * @param amountStr The amount string (e.g., "1,234.56", "(123.45)", "R 123.45-")
     * @return BigDecimal amount, null if parsing fails
     */
    protected BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }

        try {
            String cleaned = amountStr.trim()
                    .replaceAll(",", "")  // Remove commas
                    .replaceAll("R\\s*", "")  // Remove currency symbol
                    .replaceAll("\\$", "")  // Remove dollar sign
                    .replaceAll("\\(.*\\)", "")  // Remove parentheses content
                    .trim();

            // Handle negative indicators
            boolean isNegative = cleaned.endsWith("-") || cleaned.startsWith("(") || cleaned.endsWith(")");
            if (isNegative) {
                cleaned = cleaned.replaceAll("[-()]", "").trim();
            }

            BigDecimal amount = new BigDecimal(cleaned);

            return isNegative ? amount.negate() : amount;
        } catch (NumberFormatException e) {
            LOGGER.warning("Failed to parse amount: " + amountStr);
            return null;
        }
    }

    /**
     * Parse date string into LocalDate.
     * Tries multiple common formats.
     *
     * @param dateStr The date string (e.g., "15/12/2023", "15 Dec 2023")
     * @return LocalDate, null if parsing fails
     */
    protected LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        String cleaned = dateStr.trim();

        // Try different formats
        DateTimeFormatter[] formats = {DD_MM_YYYY, DD_MM_YY, MM_DD_YYYY, YYYY_MM_DD};

        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(cleaned, format);
            } catch (DateTimeParseException e) {
                // Try next format
            }
        }

        // Try parsing with month names (e.g., "15 Dec 2023")
        try {
            // Simple pattern for "dd MMM yyyy"
            if (cleaned.matches("\\d{1,2}\\s+[A-Za-z]{3}\\s+\\d{4}")) {
                String[] parts = cleaned.split("\\s+");
                int day = Integer.parseInt(parts[0]);
                String month = parts[1].substring(0, 3).toUpperCase();
                int year = Integer.parseInt(parts[2]);

                return LocalDate.of(year, monthNameToNumber(month), day);
            }
        } catch (Exception e) {
            // Ignore
        }

        LOGGER.warning("Failed to parse date: " + dateStr);
        return null;
    }

    /**
     * Convert month name to number.
     */
    private int monthNameToNumber(String month) {
        switch (month) {
            case "JAN": return 1;
            case "FEB": return 2;
            case "MAR": return 3;
            case "APR": return 4;
            case "MAY": return 5;
            case "JUN": return 6;
            case "JUL": return 7;
            case "AUG": return 8;
            case "SEP": return 9;
            case "OCT": return 10;
            case "NOV": return 11;
            case "DEC": return 12;
            default: throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

    /**
     * Clean and normalize transaction description.
     * Removes extra spaces, normalizes case, etc.
     *
     * @param description The raw description
     * @return Cleaned description
     */
    protected String cleanDescription(String description) {
        if (description == null) {
            return null;
        }

        return description.trim()
                .replaceAll("\\s+", " ")  // Normalize spaces
                .replaceAll("[\\r\\n\\t]", " ")  // Remove line breaks
                .trim();
    }

    /**
     * Determine transaction type based on amounts and description.
     * This is a fallback method; parsers should override for bank-specific logic.
     *
     * @param debitAmount Debit amount
     * @param creditAmount Credit amount
     * @param description Transaction description
     * @return TransactionType
     */
    protected TransactionType determineTransactionType(BigDecimal debitAmount, BigDecimal creditAmount, String description) {
        // Service fees
        if (description != null && (description.toLowerCase().contains("fee") ||
                                   description.toLowerCase().contains("charge"))) {
            return TransactionType.SERVICE_FEE;
        }

        // Based on amounts
        if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            return TransactionType.DEBIT;
        }
        if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            return TransactionType.CREDIT;
        }

        // Default
        return TransactionType.CREDIT;
    }

    /**
     * Build ParsedTransaction with validation.
     *
     * @param date Transaction date
     * @param description Transaction description
     * @param debitAmount Debit amount
     * @param creditAmount Credit amount
     * @param balance Running balance
     * @param serviceFee Service fee amount
     * @param reference Transaction reference
     * @return ParsedTransaction or null if invalid
     */
    protected ParsedTransaction buildTransaction(LocalDate date, String description,
                                                    BigDecimal debitAmount, BigDecimal creditAmount,
                                                    BigDecimal balance, BigDecimal serviceFee,
                                                    String reference) {
        if (date == null || description == null || description.trim().isEmpty()) {
            return null;
        }

        // Determine transaction type
        TransactionType type = determineTransactionType(debitAmount, creditAmount, description);
        BigDecimal amount = BigDecimal.ZERO;
        boolean hasServiceFee = false;

        if (serviceFee != null && serviceFee.compareTo(BigDecimal.ZERO) > 0) {
            amount = serviceFee;
            type = TransactionType.SERVICE_FEE;
            hasServiceFee = true;
        } else if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            amount = debitAmount;
            type = TransactionType.DEBIT;
        } else if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            amount = creditAmount;
            type = TransactionType.CREDIT;
        }

        try {
            return new ParsedTransaction.Builder()
                    .date(date)
                    .description(description)
                    .amount(amount)
                    .balance(balance)
                    .reference(reference)
                    .type(type)
                    .hasServiceFee(hasServiceFee)
                    .build();
        } catch (Exception e) {
            LOGGER.warning("Failed to build transaction: " + e.getMessage());
            return null;
        }
    }
}