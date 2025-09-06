package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Standard Bank tabular format bank statements.
 * Handles the layout-preserved format with separate columns for:
 * Details | Service Fee | Debits | Credits | Date | Balance
 */
public class StandardBankTabularParser implements TransactionParser {
    
    // Pattern to match transaction lines with amounts in the right columns
    // Based on actual Standard Bank format analysis from PDF
    private static final Pattern TRANSACTION_LINE_PATTERN = Pattern.compile(
        "^(.{0,95}?)" +                                 // Group 1: Details column (0-95 chars)
        "\\s*(##)?\\s*" +                              // Group 2: Service fee marker (optional)
        "([\\d,]+\\.\\d{2}-)?" +                       // Group 3: Debit amount (with trailing -)
        "\\s*([\\d,]+\\.\\d{2})?" +                    // Group 4: Credit amount  
        "\\s+(\\d{2}\\s\\d{2})" +                     // Group 5: Date (MM DD) - required
        "\\s+([\\d,]+\\.\\d{2}-?)" +                  // Group 6: Balance (required, may be negative)
        "\\s*$"
    );
    
    // Pattern to identify potential transaction description lines
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile(
        "^[A-Z][A-Z\\s\\d\\.\\-\\*#\\(\\)/:]+$"
    );
    
    // Date formatter for MM DD format (assuming current year)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM dd");
    
    private String currentDescription = "";
    private boolean collectingDescription = false;

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // Skip header lines
        if (isHeaderLine(line)) {
            return false;
        }
        
        // Check if this line has transaction data (amounts in the right columns)
        Matcher matcher = TRANSACTION_LINE_PATTERN.matcher(line);
        if (matcher.matches()) {
            String debit = matcher.group(3) != null ? matcher.group(3).trim() : "";
            String credit = matcher.group(4) != null ? matcher.group(4).trim() : "";
            String date = matcher.group(5) != null ? matcher.group(5).trim() : "";
            
            // Must have at least one amount and a date
            return (!debit.isEmpty() || !credit.isEmpty()) && !date.isEmpty();
        }
        
        // Check if it's a description line that might be part of a multi-line transaction
        return DESCRIPTION_PATTERN.matcher(line).matches() && 
               !line.contains("BALANCE BROUGHT FORWARD");
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        Matcher matcher = TRANSACTION_LINE_PATTERN.matcher(line);
        if (!matcher.matches()) {
            // This is a description line, collect it for the next transaction
            if (collectingDescription) {
                currentDescription += " " + line.trim();
            } else {
                currentDescription = line.trim();
                collectingDescription = true;
            }
            return null; // Return null for description-only lines
        }

        // Extract data from the matched line and trim whitespace
        String details = matcher.group(1) != null ? matcher.group(1).trim() : "";
        String serviceFeeStr = matcher.group(2) != null ? matcher.group(2).trim() : "";
        String debitStr = matcher.group(3) != null ? matcher.group(3).trim() : "";
        String creditStr = matcher.group(4) != null ? matcher.group(4).trim() : "";
        String dateStr = matcher.group(5) != null ? matcher.group(5).trim() : "";
        String balanceStr = matcher.group(6) != null ? matcher.group(6).trim() : "";

        // Determine transaction type and amount
        BigDecimal amount;
        TransactionType type;
        
        if (!debitStr.isEmpty()) {
            // Remove the trailing "-" from debit amounts
            String amountStr = debitStr.endsWith("-") ? debitStr.substring(0, debitStr.length() - 1) : debitStr;
            amount = new BigDecimal(amountStr.replace(",", ""));
            type = !serviceFeeStr.isEmpty() ? TransactionType.SERVICE_FEE : TransactionType.DEBIT;
        } else if (!creditStr.isEmpty()) {
            amount = new BigDecimal(creditStr.replace(",", ""));
            type = TransactionType.CREDIT;
        } else {
            throw new IllegalArgumentException("No amount found in transaction line: " + line);
        }

        // Build description from collected lines
        String fullDescription = currentDescription.trim();
        if (!details.isEmpty()) {
            if (fullDescription.isEmpty()) {
                fullDescription = details;
            } else {
                fullDescription = fullDescription + " " + details;
            }
        }

        // Parse date
        LocalDate transactionDate = parseDate(dateStr, context);

        // Reset description collection
        currentDescription = "";
        collectingDescription = false;

        return new ParsedTransaction.Builder()
            .type(type)
            .amount(amount)
            .description(fullDescription)
            .date(transactionDate)
            .reference(extractReference(fullDescription))
            .build();
    }

    private LocalDate parseDate(String dateStr, TransactionParsingContext context) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            // Use statement date as fallback
            return context.getStatementDate();
        }

        try {
            // Parse MM DD format and use current year from context
            String[] parts = dateStr.trim().split("\\s+");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[0]);
                int day = Integer.parseInt(parts[1]);
                int year = context.getStatementDate().getYear();
                
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse date: " + dateStr + ", using statement date");
        }

        return context.getStatementDate();
    }

    private String extractReference(String description) {
        // Extract reference numbers from descriptions
        Pattern refPattern = Pattern.compile("(\\d{8,})");
        Matcher matcher = refPattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private boolean isHeaderLine(String line) {
        String upperLine = line.toUpperCase();
        return upperLine.contains("DETAILS") && upperLine.contains("SERVICE") && upperLine.contains("FEE") ||
               upperLine.contains("DEBITS") && upperLine.contains("CREDITS") && upperLine.contains("DATE") ||
               upperLine.contains("BALANCE BROUGHT FORWARD") ||
               upperLine.contains("MONTH-END BALANCE") ||
               upperLine.contains("PAGE") ||
               upperLine.contains("STATEMENT NO") ||
               upperLine.contains("VAT REG") ||
               line.trim().matches("^\\s*$");
    }

    /**
     * Reset the parser state (useful when processing multiple statements)
     */
    public void reset() {
        currentDescription = "";
        collectingDescription = false;
    }
}
