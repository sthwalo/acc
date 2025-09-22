package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for service fee transactions.
 * Handles both standard service fees (##) and electronic payment fees.
 */
public class ServiceFeeParser implements TransactionParser {
    private static final Pattern FEE_PATTERN = Pattern.compile(".*?\\s*(\\d+\\.\\d{2})-\\s*(?:##)?|.*?\\s*##\\s*(\\d+\\.\\d{2})-?");

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null) return false;
        
        // Must contain either ## or FEE, but not be a table header
        boolean containsFeeMarkers = line.contains("##") || line.toUpperCase().contains("FEE");
        
        // Exclude table headers - lines that contain multiple column keywords
        boolean isTableHeader = line.matches(".*(?:Fee|Debits|Credits|Date|Balance).*(?:Fee|Debits|Credits|Date|Balance).*") ||
                               line.trim().matches("^\\s*(?:Date|Details?|Description|Amount|Debit|Credit|Balance|Reference|Fee)(?:\\s+(?:Date|Details?|Description|Amount|Debit|Credit|Balance|Reference|Fee))*\\s*$");
        
        // Must have an amount pattern for actual transactions
        boolean hasAmountPattern = line.matches(".*\\d+\\.\\d{2}-?\\s*(?:##)?\\s*$") || 
                                  line.matches(".*##\\s*\\d+\\.\\d{2}-?\\s*$");
        
        return containsFeeMarkers && !isTableHeader && hasAmountPattern;
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        Matcher feeMatcher = FEE_PATTERN.matcher(line);
        if (!feeMatcher.find()) {
            throw new IllegalArgumentException("Invalid service fee format: " + line);
        }

        String amountStr = feeMatcher.group(1) != null ? feeMatcher.group(1) : feeMatcher.group(2);
        BigDecimal feeAmount = new BigDecimal(amountStr);
        
        // Extract details by removing amount, dashes, and ## symbols from the end
        String details = line.replaceAll("\\s*\\d+\\.\\d{2}-\\s*(?:##)?\\s*$|\\s*##\\s*\\d+\\.\\d{2}-?\\s*$", "").trim();
            
        return new ParsedTransaction.Builder()
            .type(TransactionType.SERVICE_FEE)
            .description(details)
            .amount(feeAmount)
            .date(context.getStatementDate())
            .reference("FEE")
            .build();
    }
}
