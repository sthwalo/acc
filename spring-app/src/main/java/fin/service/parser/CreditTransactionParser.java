package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for credit transactions (deposits, transfers in, etc.)
 */
@Component
public class CreditTransactionParser implements TransactionParser {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\s*(\\d+,?\\d*\\.\\d{2})\\s*$");
    private static final String[] CREDIT_KEYWORDS = {
        "CREDIT TRANSFER",
        "DEPOSIT",
        "PAYMENT FROM",
        "TRANSFER FROM",
        "REAL TIME CREDIT",
        "REVERSAL",
        "REFUND"
    };

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null) return false;
        String upperLine = line.toUpperCase();
        return !upperLine.contains("SERVICE FEE") && // Not a service fee
               !upperLine.contains("##") && // Not a marked fee
               java.util.Arrays.stream(CREDIT_KEYWORDS)
                   .anyMatch(keyword -> upperLine.contains(keyword));
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No amount found in line: " + line);
        }

        String amountStr = matcher.group(1).replace(",", "");
        BigDecimal amount = new BigDecimal(amountStr);

        // Extract description (everything before the amount)
        String description = line.substring(0, matcher.start()).trim();

        return new ParsedTransaction.Builder()
                .type(TransactionType.CREDIT)
                .description(description)
                .amount(amount)
                .date(context.getStatementDate())
                .build();
    }
}