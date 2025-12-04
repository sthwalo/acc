package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Absa Bank statement format.
 * Handles OCR-extracted space-separated format: Date Description Charge DebitAmount CreditAmount Balance
 * Example: "23/02/2023 Atm Payment Fr Killarney 600.00 54 882.66"
 * Supports multi-line descriptions and various transaction types.
 */
@Component
public class AbsaBankParser implements TransactionParser {
    private static final Logger LOGGER = Logger.getLogger(AbsaBankParser.class.getName());

    // Pattern to detect Absa bank statements
    private static final Pattern ABSA_HEADER_PATTERN = Pattern.compile(
        "(?i)^\\s*(?:ABSA|Absa Bank|Account Number|Cheque Account).*$"
    );

    // Pattern for Absa transaction lines with balance at end
    // Format: DD/MM/YYYY Description Amount Balance (where Amount can be debit or credit)
    // Example: "23/02/2023 Atm Payment Fr Killarney 600.00 54 882.66"
    private static final Pattern TRANSACTION_WITH_BALANCE_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s*$"
    );

    // Pattern for transactions with charge column
    // Format: DD/MM/YYYY Description Charge Amount Balance
    // Example: "23/02/2023 Digital Payment Dt Settlement 10.00 T 1 300.00 53 582.66"
    private static final Pattern TRANSACTION_WITH_CHARGE_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s+[TA]\\s+([\\d,]+\\.\\d{2})\\s+([\\d,]+\\.\\d{2})\\s*$"
    );

    // Pattern for simple transactions (Bal Brought Forward, etc.)
    // Format: DD/MM/YYYY Description Balance
    // Example: "20/02/2023 Bal Brought Forward 54 282.66"
    private static final Pattern SIMPLE_TRANSACTION_PATTERN = Pattern.compile(
        "^(\\d{1,2}/\\d{1,2}/\\d{4})\\s+(.+?)\\s+([\\d,]+\\.\\d{2})\\s*$"
    );

    // Pattern for Balance Brought Forward line (without date)
    private static final Pattern BALANCE_BROUGHT_FORWARD_PATTERN = Pattern.compile(
        "(?i)^\\s*Balance\\s+(?:Brought|B/F|Brought Forward).*?([\\d,]+\\.\\d{2})\\s*$"
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

        // Check if it matches any Absa transaction pattern
        return TRANSACTION_WITH_BALANCE_PATTERN.matcher(line).matches() ||
               TRANSACTION_WITH_CHARGE_PATTERN.matcher(line).matches() ||
               SIMPLE_TRANSACTION_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            // Check for Balance Brought Forward
            Matcher bbfMatcher = BALANCE_BROUGHT_FORWARD_PATTERN.matcher(line);
            if (bbfMatcher.matches()) {
                LOGGER.info("Balance Brought Forward detected: " + bbfMatcher.group(1));
            }
            return null;
        }

        try {
            // Try transaction with charge pattern (most complete format)
            Matcher matcher = TRANSACTION_WITH_CHARGE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseTransactionWithCharge(matcher, context);
            }

            // Try transaction with balance pattern (standard format)
            matcher = TRANSACTION_WITH_BALANCE_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseTransactionWithBalance(matcher, context);
            }

            // Try simple transaction pattern (like Balance Brought Forward)
            matcher = SIMPLE_TRANSACTION_PATTERN.matcher(line);
            if (matcher.matches()) {
                return parseSimpleTransaction(matcher, context);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Absa transaction line: " + line + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * Parse transaction with charge: DD/MM/YYYY Description Charge Amount Balance
     * Example: "23/02/2023 Digital Payment Dt Settlement 10.00 T 1 300.00 53 582.66"
     */
    private ParsedTransaction parseTransactionWithCharge(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String chargeStr = matcher.group(3);
        String amountStr = matcher.group(4);
        String balanceStr = matcher.group(5);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal charge = parseAmount(chargeStr);
        BigDecimal amount = parseAmount(amountStr);
        BigDecimal balance = parseAmount(balanceStr);

        // Determine if debit or credit based on balance change
        TransactionType type = determineTransactionType(description, true); // Assume debit if charge present
        
        return new ParsedTransaction.Builder()
                .type(type)
                .description(description)
                .amount(amount)
                .date(transactionDate)
                .balance(balance)
                .hasServiceFee(charge.compareTo(BigDecimal.ZERO) > 0)
                .build();
    }

    /**
     * Parse transaction with balance: DD/MM/YYYY Description Amount Balance
     * Example: "23/02/2023 Atm Payment Fr Killarney 600.00 54 882.66"
     */
    private ParsedTransaction parseTransactionWithBalance(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String amountStr = matcher.group(3);
        String balanceStr = matcher.group(4);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal amount = parseAmount(amountStr);
        BigDecimal balance = parseAmount(balanceStr);

        // Determine transaction type from description
        boolean isDebit = description.toLowerCase().contains("payment") ||
                         description.toLowerCase().contains("debit") ||
                         description.toLowerCase().contains("atm") ||
                         description.toLowerCase().contains("fee");
        
        TransactionType type = determineTransactionType(description, isDebit);
        
        return new ParsedTransaction.Builder()
                .type(type)
                .description(description)
                .amount(amount)
                .date(transactionDate)
                .balance(balance)
                .hasServiceFee(false)
                .build();
    }

    /**
     * Parse simple transaction: DD/MM/YYYY Description Balance
     * Example: "20/02/2023 Bal Brought Forward 54 282.66"
     */
    private ParsedTransaction parseSimpleTransaction(Matcher matcher, TransactionParsingContext context) {
        String dateStr = matcher.group(1);
        String description = matcher.group(2).trim();
        String balanceStr = matcher.group(3);

        LocalDate transactionDate = parseDateFromString(dateStr);
        BigDecimal balance = parseAmount(balanceStr);

        // For Balance Brought Forward, amount is 0 (it's just setting the opening balance)
        TransactionType type = description.toLowerCase().contains("bal brought forward") ||
                               description.toLowerCase().contains("balance brought forward")
                               ? TransactionType.CREDIT  // Opening balance counts as initial credit
                               : determineTransactionType(description, false);
        
        return new ParsedTransaction.Builder()
                .type(type)
                .description(description)
                .amount(BigDecimal.ZERO)
                .date(transactionDate)
                .balance(balance)
                .hasServiceFee(false)
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

    private BigDecimal parseAmount(String amountStr) {
        // Remove currency symbols, commas, and trailing minus signs
        String cleaned = amountStr.replace(",", "")
                                  .replace("R", "")
                                  .replace(" ", "")
                                  .trim();

        boolean isNegative = cleaned.endsWith("-");
        if (isNegative) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }

        BigDecimal amount = new BigDecimal(cleaned);
        return isNegative ? amount.negate() : amount;
    }

    private TransactionType determineTransactionType(String description, boolean isDebit) {
        String lowerDesc = description.toLowerCase();

        // Service fees
        if (lowerDesc.contains("fee") || lowerDesc.contains("charge") || lowerDesc.contains("commission")) {
            return TransactionType.SERVICE_FEE;
        }

        // ATM transactions
        if (lowerDesc.contains("atm") || lowerDesc.contains("cash withdrawal")) {
            return TransactionType.DEBIT;
        }

        // Payments
        if (lowerDesc.contains("payment") || lowerDesc.contains("debit order") || lowerDesc.contains("debit")) {
            return TransactionType.DEBIT;
        }

        // Deposits/Credits
        if (lowerDesc.contains("deposit") || lowerDesc.contains("credit") || lowerDesc.contains("transfer in")) {
            return TransactionType.CREDIT;
        }

        // Default based on amount sign
        return isDebit ? TransactionType.DEBIT : TransactionType.CREDIT;
    }

    /**
     * Reset parser state
     */
    public void reset() {
        isAbsaStatement = false;
    }
}
