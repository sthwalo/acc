package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for multi-part transactions that may contain both a main transaction and fees.
 */
public class MultiTransactionParser implements TransactionParser {
    private static final Pattern MAIN_AMOUNT_PATTERN = Pattern.compile("(\\d+,?\\d*\\.\\d{2})-");
    private static final Pattern FEE_PATTERN = Pattern.compile("FEE[:\\s-]+(?:ELECTRONIC\\s+PAYMENT\\s+)?(\\d+\\.\\d{2})-");

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null) return false;
        String upperLine = line.toUpperCase();
        
        // Must contain one of these transaction types
        boolean hasTransactionType = upperLine.contains("TRANSFER") ||
                                   upperLine.contains("PAYMENT") ||
                                   upperLine.contains("PURCHASE");
        
        // Must have FEE somewhere in the line, but not at the start
        // This prevents matching standalone fee transactions
        boolean hasFee = upperLine.indexOf("FEE") > 0;

        // Must have at least one amount in the format xxx.xx-
        boolean hasMainAmount = MAIN_AMOUNT_PATTERN.matcher(line).find();

        return hasTransactionType && hasFee && hasMainAmount;
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        // Extract the main transaction amount
        Matcher mainMatcher = MAIN_AMOUNT_PATTERN.matcher(line);
        if (!mainMatcher.find()) {
            throw new IllegalArgumentException("No main transaction amount found: " + line);
        }

        String mainAmountStr = mainMatcher.group(1).replace(",", "");
        BigDecimal mainAmount = new BigDecimal(mainAmountStr);
        
        // Description is everything up to the amount
        String mainDescription = line.substring(0, mainMatcher.start()).trim();

        // Optional: Extract the fee component (not used in current implementation)
        Matcher feeMatcher = FEE_PATTERN.matcher(line);
        if (feeMatcher.find()) {
            // Fee amount could be used in the future if needed
            String feeAmountStr = feeMatcher.group(1);
        }

        return new ParsedTransaction.Builder()
            .type(TransactionType.DEBIT)  // Multi-transactions are always debits
            .description(mainDescription)
            .amount(mainAmount)
            .date(context.getStatementDate())
            .build();
    }
}
