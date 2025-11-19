package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced parser for Standard Bank tabular format bank statements.
 * Handles the column structure for Standard Bank statements.
 */
@Component
public class StandardBankTabularParser implements TransactionParser {
    private static final Logger LOGGER = Logger.getLogger(StandardBankTabularParser.class.getName());

    // Pattern to identify transaction lines
    private static final Pattern TRANSACTION_LINE_PATTERN = Pattern.compile(
        "^[A-Z][A-Z\\s\\-:]+.*\\s+(\\d{2})\\s+(\\d{2})\\s+([\\d,]+\\.\\d{2}-?)\\s*$"
    );

    // Pattern to identify header/footer lines to skip
    private static final Pattern SKIP_PATTERN = Pattern.compile(
        "^\\s*(?:Details\\s+Service\\s+Fee|DEBITS\\s+CREDITS\\s+DATE|" +
        "BRAAMFONTEIN|MARSHALLTOWN|BIZLAUNCH|Account Number|" +
        "Statement from|Statement No|VAT Reg|Page \\d|Month-end Balance|" +
        "PO BOX|DOORNFONTEIN|XINGHIZANA GROUP\\(PTY\\)LTD|" +
        "BizDirect Contact Centre|e-mail:|\\d+\\s+\\w+\\s+\\d{4}|" +
        "MONTHLY EMAIL VAT|Statement Frequency:|BANK STATEMENT).*$"
    );

    // State for multi-line transactions
    private ParsedTransaction pendingTransaction = null;
    private List<String> currentDescriptionLines = new ArrayList<>();
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private boolean lastLineWasTransaction = false;

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }

        // Skip header/footer lines
        if (SKIP_PATTERN.matcher(line).matches()) {
            return false;
        }

        // Check if it matches transaction pattern
        return TRANSACTION_LINE_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            return null;
        }

        try {
            Matcher matcher = TRANSACTION_LINE_PATTERN.matcher(line);
            if (matcher.find()) {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));
                String balanceStr = matcher.group(3);

                // Extract description (everything before the date columns)
                String description = line.substring(0, line.lastIndexOf(String.valueOf(month))).trim();

                // Parse balance
                BigDecimal balance = parseAmount(balanceStr);

                // Determine transaction type and amounts from description
                TransactionData data = parseTransactionData(description, balance);

                // Create transaction date
                LocalDate transactionDate = createTransactionDate(month, day, context);

                return new ParsedTransaction.Builder()
                    .type(data.isServiceFee ? TransactionType.SERVICE_FEE :
                          (data.debitAmount != null && data.debitAmount.compareTo(BigDecimal.ZERO) > 0) ?
                          TransactionType.DEBIT : TransactionType.CREDIT)
                    .description(data.details)
                    .amount(data.debitAmount != null && data.debitAmount.compareTo(BigDecimal.ZERO) > 0 ?
                           data.debitAmount : data.creditAmount)
                    .date(transactionDate)
                    .balance(balance)
                    .hasServiceFee(data.isServiceFee)
                    .build();
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to parse Standard Bank transaction line: " + line + " - " + e.getMessage());
        }

        return null;
    }

    /**
     * Finalize any pending transactions
     */
    public ParsedTransaction finalizeParsing() {
        ParsedTransaction result = pendingTransaction;
        pendingTransaction = null;
        currentDescriptionLines.clear();
        return result;
    }

    /**
     * Reset parser state
     */
    public void reset() {
        currentDescriptionLines.clear();
        pendingTransaction = null;
        statementStartDate = null;
        statementEndDate = null;
        lastLineWasTransaction = false;
    }

    private TransactionData parseTransactionData(String description, BigDecimal balance) {
        TransactionData data = new TransactionData();
        data.details = description;
        data.balance = balance;
        data.isServiceFee = description.toLowerCase().contains("fee") ||
                           description.toLowerCase().contains("charge");

        // Parse amounts from description if present
        Pattern amountPattern = Pattern.compile("(\\d+(?:,\\d{3})*(?:\\.\\d{2}))");
        Matcher matcher = amountPattern.matcher(description);

        List<BigDecimal> amounts = new ArrayList<>();
        while (matcher.find()) {
            amounts.add(new BigDecimal(matcher.group(1).replace(",", "")));
        }

        if (!amounts.isEmpty()) {
            // Assume the last amount is the transaction amount
            BigDecimal amount = amounts.get(amounts.size() - 1);
            // Determine if debit or credit based on balance change
            // This is a simplified logic - in practice, you'd need more sophisticated analysis
            data.debitAmount = amount;
            data.creditAmount = BigDecimal.ZERO;
        }

        return data;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Remove commas and handle negative sign at end
            String cleanAmount = amountStr.replace(",", "").replace("-", "");
            BigDecimal amount = new BigDecimal(cleanAmount);

            // If original had minus sign at end, make it negative
            if (amountStr.endsWith("-")) {
                amount = amount.negate();
            }

            return amount;
        } catch (NumberFormatException e) {
            LOGGER.warning("Failed to parse amount: " + amountStr);
            return BigDecimal.ZERO;
        }
    }

    private LocalDate createTransactionDate(int month, int day, TransactionParsingContext context) {
        int year = 2024; // Default year

        if (context != null && context.getStatementDate() != null) {
            year = context.getStatementDate().getYear();
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception e) {
            LOGGER.warning("Invalid date: " + year + "-" + month + "-" + day + ", using statement date");
            return context != null && context.getStatementDate() != null ?
                   context.getStatementDate() : LocalDate.now();
        }
    }

    /**
     * Internal class to hold transaction data
     */
    private static class TransactionData {
        String details;
        BigDecimal debitAmount;
        BigDecimal creditAmount;
        BigDecimal balance;
        boolean isServiceFee;
    }
}