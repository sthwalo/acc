package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced parser for Standard Bank tabular format bank statements.
 * Uses position-based column extraction for maximum accuracy.
 * 
 * Standard Bank Column Structure:
 * - Details: positions 0-50
 * - Service Fee: positions 50-60 (## marker)
 * - Debits: positions 60-80 (amounts with trailing -)
 * - Credits: positions 80-95 (positive amounts)
 * - Date: positions 95-105 (MM DD format)
 * - Balance: positions 105+ (running balance)
 */
public class EnhancedStandardBankTabularParser implements TransactionParser {
    
    // Position-based column definitions (based on actual Standard Bank layout)
    private static final int DETAILS_START = 0;
    private static final int DETAILS_END = 78;
    private static final int SERVICE_FEE_START = 50;
    private static final int SERVICE_FEE_END = 78;
    private static final int DEBIT_START = 78;
    private static final int DEBIT_END = 100;
    private static final int CREDIT_START = 99;
    private static final int CREDIT_END = 110;
    private static final int DATE_START = 110;
    private static final int DATE_END = 120;
    private static final int BALANCE_START = 120;
    
    // Patterns for data validation
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([\\d,]+\\.\\d{2})-?");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{2})\\s+(\\d{2})");
    
    // Skip patterns for headers and footers
    private static final Pattern SKIP_PATTERN = Pattern.compile(
        "(?i)(details|service|fee|debits|credits|date|balance|" +
        "page \\d+|statement no|vat reg|month-end balance|" +
        "balance brought forward|please verify|standard bank|" +
        "authorised financial|banking association|ombudsman)"
    );

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // Skip header/footer lines
        if (SKIP_PATTERN.matcher(line).find()) {
            return false;
        }
        
        // Must be long enough to contain main columns
        if (line.length() < 80) {
            return false;
        }
        
        // Check if this is a NEW transaction line (has date pattern)
        String dateColumn = extractColumn(line, DATE_START, DATE_END);
        boolean hasDatePattern = DATE_PATTERN.matcher(dateColumn).find();
        
        if (hasDatePattern) {
            // This is a new transaction line - must have either debit or credit amount
            String debitColumn = extractColumn(line, DEBIT_START, DEBIT_END);
            String creditColumn = extractColumn(line, CREDIT_START, CREDIT_END);
            
            return AMOUNT_PATTERN.matcher(debitColumn).find() || 
                   AMOUNT_PATTERN.matcher(creditColumn).find();
        } else {
            // This might be a continuation line of a multi-line transaction
            // Don't parse it as a separate transaction - it will be handled
            // by the previous transaction line
            return false;
        }
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        // Extract data from columns
        String details = extractColumn(line, DETAILS_START, DETAILS_END).trim();
        String serviceFeeColumn = extractColumn(line, SERVICE_FEE_START, SERVICE_FEE_END);
        String debitColumn = extractColumn(line, DEBIT_START, DEBIT_END);
        String creditColumn = extractColumn(line, CREDIT_START, CREDIT_END);
        String dateColumn = extractColumn(line, DATE_START, DATE_END);
        String balanceColumn = extractColumn(line, BALANCE_START, line.length());

        // Parse amounts (only from their designated columns, not from description)
        BigDecimal debitAmount = extractAmount(debitColumn);
        BigDecimal creditAmount = extractAmount(creditColumn);
        BigDecimal balance = extractAmount(balanceColumn);
        
        // Determine transaction type and amount
        BigDecimal amount;
        TransactionType type;
        boolean hasServiceFee = serviceFeeColumn.contains("##");
        
        if (debitAmount != null) {
            amount = debitAmount;
            type = hasServiceFee ? TransactionType.SERVICE_FEE : TransactionType.DEBIT;
        } else if (creditAmount != null) {
            amount = creditAmount;
            type = TransactionType.CREDIT;
        } else {
            throw new IllegalArgumentException("No valid amount found in line: " + line);
        }

        // Parse date - ensure year is 2025 for current statements
        LocalDate transactionDate = parseTransactionDate(dateColumn, context);

        // Build transaction
        ParsedTransaction.Builder builder = new ParsedTransaction.Builder()
            .type(type)
            .amount(amount)
            .description(details)
            .date(transactionDate)
            .reference(extractReference(details));
            
        // Add balance if extracted
        if (balance != null) {
            builder.balance(balance);
        }
        
        // Add service fee flag
        builder.hasServiceFee(hasServiceFee);

        return builder.build();
    }

    private String extractColumn(String line, int start, int end) {
        if (start >= line.length()) {
            return "";
        }
        
        int actualEnd = Math.min(end, line.length());
        return line.substring(start, actualEnd).trim();
    }

    private BigDecimal extractAmount(String column) {
        if (column == null || column.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = AMOUNT_PATTERN.matcher(column);
        if (matcher.find()) {
            String amountStr = matcher.group(1).replace(",", "");
            try {
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }

    private LocalDate parseTransactionDate(String dateColumn, TransactionParsingContext context) {
        Matcher matcher = DATE_PATTERN.matcher(dateColumn);
        if (matcher.find()) {
            try {
                int month = Integer.parseInt(matcher.group(1));
                int day = Integer.parseInt(matcher.group(2));
                
                // For current financial year (2024-2025), use 2025 for most months
                // and handle year boundary properly
                LocalDate statementDate = context.getStatementDate();
                int year = 2025; // Default to 2025 for current statements
                
                // If statement date is provided, use it for better year determination
                if (statementDate != null) {
                    year = statementDate.getYear();
                    
                    // Handle financial year boundary
                    LocalDate candidateDate = LocalDate.of(year, month, day);
                    
                    // If the date is more than 6 months after statement date, 
                    // it's probably from the previous year
                    if (candidateDate.isAfter(statementDate.plusMonths(6))) {
                        candidateDate = LocalDate.of(year - 1, month, day);
                    }
                    // If the date is more than 6 months before statement date,
                    // it's probably from the next year
                    else if (candidateDate.isBefore(statementDate.minusMonths(6))) {
                        candidateDate = LocalDate.of(year + 1, month, day);
                    }
                    
                    return candidateDate;
                }
                
                // Fallback: use 2025 for current financial year
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                System.err.println("Failed to parse date from: " + dateColumn);
            }
        }
        
        // Fallback to statement date or current year
        return context.getStatementDate() != null ? 
               context.getStatementDate() : 
               LocalDate.of(2025, 1, 1);
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
}
