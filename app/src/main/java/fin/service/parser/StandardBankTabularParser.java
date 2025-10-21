package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced parser for Standard Bank tabular format bank statements.
 * Handles ONLY the column structure:
 * Details (0-55) | Service Fee (55-70) | Debits (70-95) | Credits (95-115) | Date (115-125) | Balance (125-135)
 * 
 * CRITICAL: Address lines from document headers should NEVER appear in transaction details!
 */
public class StandardBankTabularParser implements TransactionParser {
    
    // Regex group indices for transaction parsing
    private static final int BALANCE_GROUP_INDEX = 3;
    
    // Default year for transaction date parsing when no context is available
    private static final int DEFAULT_YEAR = 2024;
    
    // Pattern to identify transaction lines - they start with description and have date/balance in columns
    // Format: "DESCRIPTION [amounts in middle columns] MM DD [balance]"
    private static final Pattern TRANSACTION_LINE_PATTERN = Pattern.compile(
        "^[A-Z][A-Z\\s\\-:]+.*\\s+(\\d{2})\\s+(\\d{2})\\s+([\\d,]+\\.\\d{2}-?)\\s*$"
    );
    
    // Pattern to extract amounts from compact format lines
    private static final Pattern COMPACT_AMOUNT_PATTERN = Pattern.compile(
        "([\\d,]+\\.\\d{2}-?)\\s+\\d{2}\\s+\\d{2}\\s+[\\d,]+\\.\\d{2}-?\\s*$"
    );
    
    // Pattern to extract debit amounts (with trailing -)
    private static final Pattern DEBIT_PATTERN = Pattern.compile(
        "([\\d,]+\\.\\d{2})-"
    );
    
    // Pattern to extract credit amounts (without trailing -)  
    private static final Pattern CREDIT_PATTERN = Pattern.compile(
        "([\\d,]+\\.\\d{2})(?!-)"
    );
    
    // Pattern to detect service fee marker
    private static final Pattern SERVICE_FEE_PATTERN = Pattern.compile(
        "\\s+##\\s+"
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
    
    // Column positions for tabular format
    private static final int DETAILS_START = 0;
    private static final int DETAILS_END = 55;
    private static final int SERVICE_FEE_START = 55;
    private static final int SERVICE_FEE_END = 70;
    private static final int DEBITS_START = 70;
    private static final int DEBITS_END = 95;
    private static final int CREDITS_START = 95;
    private static final int CREDITS_END = 115;
    private static final int DATE_START = 115;
    private static final int DATE_END = 125;
    private static final int BALANCE_START = 125;
    private static final int BALANCE_END = 135;
    
    // State for multi-line transactions
    private ParsedTransaction pendingTransaction = null;
    private List<String> currentDescriptionLines = new ArrayList<>();
    private LocalDate statementStartDate;
    private LocalDate statementEndDate;
    private boolean lastLineWasTransaction = false;
    
    // Pattern to extract statement period from header
    private static final Pattern STATEMENT_PERIOD_PATTERN = Pattern.compile(
        "Statement from (\\d{1,2} \\w+ \\d{4}) to (\\d{1,2} \\w+ \\d{4})"
    );

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        // Extract statement period from header if found
        extractStatementPeriod(line);
        
        // Initialize statement dates from context if not set and no period extracted yet
        if (statementStartDate == null && context != null) {
            statementStartDate = context.getStatementDate();
            // Estimate end date (statements are typically monthly)
            statementEndDate = statementStartDate.plusMonths(1);
        }
        
        // Skip header/footer lines and address information
        if (SKIP_PATTERN.matcher(line).matches()) {
            lastLineWasTransaction = false;
            return false;
        }
        
        // Check if this is a NEW transaction line (has date + balance pattern at end)
        if (isTransactionLine(line)) {
            lastLineWasTransaction = true;
            return true;
        }
        
        // Only accept description continuation lines that immediately follow a transaction
        if (lastLineWasTransaction && isDescriptionLine(line)) {
            return true;
        }
        
        // Reset flag for non-continuation lines
        lastLineWasTransaction = false;
        return false;
    }

    /**
     * Determines if a line is a NEW transaction line by checking for date + balance pattern
     */
    private boolean isTransactionLine(String line) {
        return TRANSACTION_LINE_PATTERN.matcher(line).matches();
    }
    
    /**
     * Determines if a line is a valid description continuation line
     * MUST immediately follow a transaction line and NOT start with a transaction pattern
     */
    private boolean isDescriptionLine(String line) {
        String trimmed = line.trim();
        
        // Must not be empty
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Must NOT be a transaction line itself (doesn't start with transaction pattern)
        if (isTransactionLine(line)) {
            return false;
        }
        
        // CRITICAL: Reject any line containing address/header information
        if (trimmed.matches(".*(?:PO BOX|MARSHALLTOWN|DOORNFONTEIN|BRAAMFONTEIN|XINGHIZANA GROUP).*")) {
            return false;
        }
        
        // Must be valid continuation text - alphanumeric with common punctuation
        // Continuation lines often contain reference numbers, company names, etc.
        // Fixed pattern - allows continuation lines starting with '*' or alphanumeric
        return trimmed.matches("^[A-Z0-9*][A-Z0-9\\s\\*\\-\\(\\)\\.:/#\\+]+$");
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        // Check if this is a transaction line or continuation line
        if (isTransactionLine(line)) {
            // We found a new transaction line
            
            // If we have a pending transaction, finalize it with collected continuation lines
            if (pendingTransaction != null) {
                ParsedTransaction completedTransaction = finalizePendingTransaction();
                
                // Now create new pending transaction from current line
                TransactionData data = extractTransactionData(line);
                if (data != null) {
                    createPendingTransaction(data, context);
                }
                
                // Return the completed transaction (with continuation lines)
                return completedTransaction;
            } else {
                // This is the first transaction - create pending transaction
                TransactionData data = extractTransactionData(line);
                if (data != null) {
                    createPendingTransaction(data, context);
                }
                return null; // No completed transaction to return yet
            }
            
        } else if (isDescriptionLine(line)) {
            // This is a continuation line - collect it
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                currentDescriptionLines.add(trimmed);
            }
            return null; // Continue collecting
        }

        return null; // Skip other lines
    }

    /**
     * Helper method to create a new pending transaction
     */
    private void createPendingTransaction(TransactionData data, TransactionParsingContext context) {
        // Determine transaction type
        TransactionType type;
        if (data.isServiceFee) {
            type = TransactionType.SERVICE_FEE;
        } else if (data.debitAmount != null) {
            type = TransactionType.DEBIT;
        } else {
            type = TransactionType.CREDIT;
        }

        LocalDate transactionDate = parseTransactionDate(data.month, data.day, context);

        pendingTransaction = new ParsedTransaction.Builder()
            .type(type)
            .amount(data.debitAmount != null ? data.debitAmount : data.creditAmount)
            .description(data.details != null ? data.details.trim() : "")
            .date(transactionDate)
            .balance(data.balance)
            .hasServiceFee(data.isServiceFee)
            .reference(extractReference(data.details != null ? data.details.trim() : ""))
            .build();
        
        // Clear continuation lines for the new transaction
        currentDescriptionLines.clear();
    }

    /**
     * Finalize the pending transaction by appending collected continuation lines
     */
    private ParsedTransaction finalizePendingTransaction() {
        if (pendingTransaction == null) {
            return null;
        }

        // Build the complete description
        StringBuilder fullDescription = new StringBuilder();
        fullDescription.append(pendingTransaction.getDescription());
        
        // Append continuation lines
        for (String continuationLine : currentDescriptionLines) {
            if (fullDescription.length() > 0) {
                fullDescription.append(" ");
            }
            fullDescription.append(continuationLine);
        }

        // Create final transaction with complete description
        ParsedTransaction result = new ParsedTransaction.Builder()
            .type(pendingTransaction.getType())
            .amount(pendingTransaction.getAmount())
            .description(fullDescription.toString())
            .date(pendingTransaction.getDate())
            .balance(pendingTransaction.getBalance())
            .hasServiceFee(pendingTransaction.hasServiceFee())
            .reference(extractReference(fullDescription.toString()))
            .build();

        // Clear the pending transaction
        pendingTransaction = null;
        currentDescriptionLines.clear();

        return result;
    }

    /**
     * Call this at the end of parsing to finalize any remaining pending transaction
     */
    public ParsedTransaction finalizeParsing() {
        return finalizePendingTransaction();
    }
    
    /**
     * Extracts transaction data from a transaction line
     * Since the PDF text extraction gives us space-separated format rather than fixed columns,
     * we need to parse: "DESCRIPTION [AMOUNT] MM DD BALANCE"
     */
    private TransactionData extractTransactionData(String line) {
        Matcher dateMatcher = TRANSACTION_LINE_PATTERN.matcher(line);
        if (!dateMatcher.matches()) {
            return null;
        }
        
        TransactionData data = new TransactionData();
        data.month = Integer.parseInt(dateMatcher.group(1));
        data.day = Integer.parseInt(dateMatcher.group(2));
        data.balance = parseAmount(dateMatcher.group(BALANCE_GROUP_INDEX));
        
        // Extract the description part by removing the trailing "AMOUNT MM DD BALANCE" pattern
        // The pattern captures everything before the final "MM DD BALANCE"
        String fullLine = line.trim();
        
        // Find the last occurrence of the date + balance pattern
        String dateBalancePattern = "\\s+" + String.format("%02d", data.month) + "\\s+" + String.format("%02d", data.day) + "\\s+[\\d,]+\\.\\d{2}-?\\s*$";
        String withoutDateBalance = fullLine.replaceAll(dateBalancePattern, "");
        
        // Now extract amounts and description
        // Check if this line has a debit amount (ending with -)
        Pattern debitPattern = Pattern.compile("(.+?)\\s+([\\d,]+\\.\\d{2}-)\\s*$");
        Matcher debitMatcher = debitPattern.matcher(withoutDateBalance);
        
        if (debitMatcher.matches()) {
            // This is a debit transaction
            data.details = debitMatcher.group(1).trim();
            data.debitAmount = parseAmount(debitMatcher.group(2));
            
            // Check for service fee marker in the details
            data.isServiceFee = data.details.contains("##");
        } else {
            // Check if this is a credit transaction
            Pattern creditPattern = Pattern.compile("(.+?)\\s+([\\d,]+\\.\\d{2})\\s*$");
            Matcher creditMatcher = creditPattern.matcher(withoutDateBalance);
            
            if (creditMatcher.matches()) {
                data.details = creditMatcher.group(1).trim();
                data.creditAmount = parseAmount(creditMatcher.group(2));
            } else {
                // No amount found, just description
                data.details = withoutDateBalance.trim();
            }
        }
        
        return data;
    }
    
    /**
     * Extract text from specific column positions
     */
    private String extractColumn(String line, int start, int end) {
        if (line.length() <= start) {
            return "";
        }
        int actualEnd = Math.min(end, line.length());
        return line.substring(start, actualEnd).trim();
    }
    
    /**
     * Parse amount string to BigDecimal
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return null;
        }
        
        String cleanAmount = amountStr.replace(",", "").replace("-", "");
        try {
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Parse transaction date from month and day
     */
    /**
     * Extract statement period from header line if present
     */
    private void extractStatementPeriod(String line) {
        Matcher periodMatcher = STATEMENT_PERIOD_PATTERN.matcher(line);
        if (periodMatcher.find()) {
            try {
                String startDateStr = periodMatcher.group(1);
                String endDateStr = periodMatcher.group(2);
                
                // Parse dates like "16 February 2024"
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
                statementStartDate = LocalDate.parse(startDateStr, formatter);
                statementEndDate = LocalDate.parse(endDateStr, formatter);
                
                System.out.println("📅 Extracted statement period: " + statementStartDate + " to " + statementEndDate);
            } catch (Exception e) {
                System.err.println("⚠️ Failed to parse statement period: " + e.getMessage());
            }
        }
    }

    private LocalDate parseTransactionDate(int month, int day, TransactionParsingContext context) {
        // Use extracted statement period if available
        if (statementStartDate != null && statementEndDate != null) {
            // Try to find the correct year based on the statement period
            LocalDate candidateStartYear = LocalDate.of(statementStartDate.getYear(), month, day);
            LocalDate candidateEndYear = LocalDate.of(statementEndDate.getYear(), month, day);
            
            // Check which candidate falls within the statement period
            if (!candidateStartYear.isBefore(statementStartDate) && !candidateStartYear.isAfter(statementEndDate)) {
                return candidateStartYear;
            } else if (!candidateEndYear.isBefore(statementStartDate) && !candidateEndYear.isAfter(statementEndDate)) {
                return candidateEndYear;
            }
            
            // If neither fits exactly, choose the one closer to the statement period
            if (Math.abs(candidateStartYear.toEpochDay() - statementStartDate.toEpochDay()) <= 
                Math.abs(candidateEndYear.toEpochDay() - statementStartDate.toEpochDay())) {
                return candidateStartYear;
            } else {
                return candidateEndYear;
            }
        }
        
        // Fallback to context-based parsing
        if (context == null || context.getStatementDate() == null) {
            return LocalDate.of(DEFAULT_YEAR, month, day); // Default year
        }
        
        int year = context.getStatementDate().getYear();
        return LocalDate.of(year, month, day);
    }
    
    /**
     * Extract reference number from description
     */
    private String extractReference(String description) {
        if (description == null) {
            return "";
        }
        
        Pattern refPattern = Pattern.compile("(\\d{8,})");
        Matcher matcher = refPattern.matcher(description);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
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
    
    /**
     * Internal class to hold transaction data
     */
    private static class TransactionData {
        String details;
        BigDecimal debitAmount;
        BigDecimal creditAmount;
        BigDecimal balance;
        boolean isServiceFee;
        int month;
        int day;
    }
}
