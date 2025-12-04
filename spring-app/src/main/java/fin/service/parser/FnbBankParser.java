package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for First National Bank (FNB) statement format.
 * Handles various FNB statement formats including Business and Personal accounts.
 * Supports multiple transaction patterns and formats.
 */
@Component
public class FnbBankParser implements TransactionParser {
    private static final Logger LOGGER = Logger.getLogger(FnbBankParser.class.getName());

    // Pattern to detect FNB bank statements
    private static final Pattern FNB_HEADER_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:FNB|First National Bank|FIRST NATIONAL BANK|Account Number|Business Account|Cheque Account).*$"
    );

    // Pattern for FNB transaction lines with date, description, amount, balance
    // Format: DD/MM/YYYY Description Amount Balance
    // Example: "15/03/2024 ATM Withdrawal - Sandton 500.00 1,234.56"
    private static final Pattern TRANSACTION_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2}-?)\\s+([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern for FNB transactions with reference numbers
    // Format: DD/MM/YYYY Description Ref: XXX Amount Balance
    // Example: "15/03/2024 EFT Payment Ref: 123456 1,500.00 2,734.56"
    private static final Pattern TRANSACTION_WITH_REF_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+Ref:\\s*([^\\s]+)\\s+([\\d,]+\\.\\d{2}-?)\\s+([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern for FNB fee/service charge lines
    // Format: DD/MM/YYYY Service Fee Description Amount Balance
    // Example: "15/03/2024 Monthly Service Fee 150.00 2,584.56"
    private static final Pattern SERVICE_FEE_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?\\b(?:Fee|Charge|Cost)\\b.+?)\\s+([\\d,]+\\.\\d{2}-?)\\s+([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern for balance brought forward
    private static final Pattern BALANCE_BROUGHT_FORWARD_PATTERN = Pattern.compile(
        "(?i)^\\s*(\\d{1,2}/\\d{1,2}/\\d{4})\\s*Balance\\s+(?:Brought|B/F|Brought Forward).*?([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern for opening balance
    private static final Pattern OPENING_BALANCE_PATTERN = Pattern.compile(
        "(?i)^\\s*(\\d{1,2}/\\d{1,2}/\\d{4})\\s*Opening\\s+Balance.*?([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern to skip header/footer lines
    private static final Pattern SKIP_PATTERN = Pattern.compile(
        "^\\s*(?:Date|Transaction|Description|Amount|Balance|Reference|" +
        "First National Bank|FNB|Account Summary|Statement|Page \\d+|" +
        "Business Account|Cheque Account|Savings Account|Account Number|" +
        "Branch|VAT Number|Customer Number|Statement Period|Generated|" +
        "Opening Balance|Closing Balance|Total Debits|Total Credits|" +
        "Available Balance|Uncleared Funds|Hold Amount).*$",
        Pattern.CASE_INSENSITIVE
    );

    // Date formatter for FNB statements
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d/M/yyyy");

    // State tracking
    private boolean isFnbStatement = false;

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

        // Check if it matches any FNB transaction pattern
        return TRANSACTION_PATTERN.matcher(line).matches() ||
               TRANSACTION_WITH_REF_PATTERN.matcher(line).matches() ||
               SERVICE_FEE_PATTERN.matcher(line).matches() ||
               BALANCE_BROUGHT_FORWARD_PATTERN.matcher(line).matches() ||
               OPENING_BALANCE_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            return null;
        }

        try {
            // Try transaction with reference pattern
            Matcher matcher = TRANSACTION_WITH_REF_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseTransactionWithRef(matcher, context);
            }

            // Try service fee pattern
            matcher = SERVICE_FEE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseServiceFeeTransaction(matcher, context);
            }

            // Try standard transaction pattern
            matcher = TRANSACTION_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseStandardTransaction(matcher, context);
            }

            // Try balance brought forward
            matcher = BALANCE_BROUGHT_FORWARD_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseBalanceBroughtForward(matcher, context);
            }

            // Try opening balance
            matcher = OPENING_BALANCE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseOpeningBalance(matcher, context);
            }

        } catch (Exception e) {
            LOGGER.warning("Failed to parse FNB transaction line: " + line + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * Parse transaction with reference: DD/MM/YYYY Description Ref: XXX Amount Balance
     */
    private ParsedTransaction parseTransactionWithRef(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String reference = matcher.group(3);
        String amountStr = matcher.group(4);
        String balanceStr = matcher.group(5);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal amount = parseAmount(amountStr);
        BigDecimal balance = parseAmount(balanceStr);

        // Include reference in description
        String fullDescription = description + " (Ref: " + reference + ")";

        TransactionType type = determineTransactionType(fullDescription, amount);

        return new ParsedTransaction.Builder()
                .type(type)
                .description(fullDescription)
                .amount(amount.abs())
                .date(transactionDate)
                .balance(balance)
                .build();
    }

    /**
     * Parse service fee transaction
     */
    private ParsedTransaction parseServiceFeeTransaction(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String amountStr = matcher.group(3);
        String balanceStr = matcher.group(4);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal amount = parseAmount(amountStr);
        BigDecimal balance = parseAmount(balanceStr);

        return new ParsedTransaction.Builder()
                .type(TransactionType.SERVICE_FEE)
                .description(description)
                .amount(amount.abs())
                .date(transactionDate)
                .balance(balance)
                .hasServiceFee(true)
                .build();
    }

    /**
     * Parse standard transaction: DD/MM/YYYY Description Amount Balance
     */
    private ParsedTransaction parseStandardTransaction(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String amountStr = matcher.group(3);
        String balanceStr = matcher.group(4);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal amount = parseAmount(amountStr);
        BigDecimal balance = parseAmount(balanceStr);

        TransactionType type = determineTransactionType(description, amount);

        return new ParsedTransaction.Builder()
                .type(type)
                .description(description)
                .amount(amount.abs())
                .date(transactionDate)
                .balance(balance)
                .build();
    }

    /**
     * Parse balance brought forward
     */
    private ParsedTransaction parseBalanceBroughtForward(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String balanceStr = matcher.group(2);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal balance = parseAmount(balanceStr);

        return new ParsedTransaction.Builder()
                .type(TransactionType.CREDIT) // Opening balance
                .description("Balance Brought Forward")
                .amount(BigDecimal.ZERO)
                .date(transactionDate)
                .balance(balance)
                .build();
    }

    /**
     * Parse opening balance
     */
    private ParsedTransaction parseOpeningBalance(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String balanceStr = matcher.group(2);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal balance = parseAmount(balanceStr);

        return new ParsedTransaction.Builder()
                .type(TransactionType.CREDIT) // Opening balance
                .description("Opening Balance")
                .amount(BigDecimal.ZERO)
                .date(transactionDate)
                .balance(balance)
                .build();
    }

    /**
     * Parse date string to LocalDate
     */
    private LocalDate parseDateFromString(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            LOGGER.warning("Failed to parse date: " + dateStr);
            return LocalDate.now(); // fallback
        }
    }

    /**
     * Parse amount string to BigDecimal
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Remove commas and handle negative sign at end
        String cleanAmount = amountStr.replace(",", "").replace(" ", "");
        if (cleanAmount.endsWith("-")) {
            cleanAmount = "-" + cleanAmount.substring(0, cleanAmount.length() - 1);
        }

        try {
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            LOGGER.warning("Failed to parse amount: " + amountStr);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Determine transaction type based on description and amount
     */
    private TransactionType determineTransactionType(String description, BigDecimal amount) {
        String desc = description.toLowerCase();

        // Check for debit indicators
        if (desc.contains("withdrawal") || desc.contains("debit") ||
            desc.contains("payment") || desc.contains("transfer to") ||
            desc.contains("atm") || desc.contains("eft out") ||
            desc.contains("fee") || desc.contains("charge") ||
            amount.compareTo(BigDecimal.ZERO) < 0) {
            return TransactionType.DEBIT;
        }

        // Check for credit indicators
        if (desc.contains("deposit") || desc.contains("credit") ||
            desc.contains("salary") || desc.contains("transfer from") ||
            desc.contains("interest") || desc.contains("dividend") ||
            desc.contains("eft in") || desc.contains("refund") ||
            amount.compareTo(BigDecimal.ZERO) > 0) {
            return TransactionType.CREDIT;
        }

        // Default to debit for negative amounts, credit for positive
        return amount.compareTo(BigDecimal.ZERO) < 0 ? TransactionType.DEBIT : TransactionType.CREDIT;
    }
}