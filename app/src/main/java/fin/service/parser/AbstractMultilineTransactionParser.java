package fin.service.parser;

import fin.model.parser.StandardizedTransaction;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for transaction parsers that support multiline descriptions.
 * Provides centralized multiline handling logic that can be reused by all parsers.
 * 
 * Parsers extending this class should:
 * 1. Implement isContinuationLine() to detect description continuation lines
 * 2. Implement parseTransactionLine() to parse a complete transaction line
 * 3. Call handleMultilineParsing() in their parse() method
 */
public abstract class AbstractMultilineTransactionParser implements TransactionParser {
    
    // State tracking for multiline descriptions
    protected StandardizedTransaction pendingTransaction = null;
    protected final List<String> currentDescriptionLines = new ArrayList<>();
    protected boolean lastLineWasTransaction = false;

    /**
     * Check if the line is a continuation of the previous transaction's description.
     * Continuation lines typically:
     * - Start with spaces (indentation)
     * - Don't have a date at the start
     * - Don't have amount/balance at the end
     * 
     * @param line The line to check
     * @return true if this is a continuation line
     */
    protected abstract boolean isContinuationLine(String line);

    /**
     * Parse a transaction line (not a continuation line).
     * Should extract date, description, amounts, and balance.
     * 
     * @param line The transaction line to parse
     * @return StandardizedTransaction with parsed data
     */
    protected abstract StandardizedTransaction parseTransactionLine(String line);

    /**
     * Centralized multiline parsing logic.
     * Call this method from your parse() implementation.
     * 
     * @param line The line to parse
     * @param canParse Whether the line can be parsed by this parser
     * @return StandardizedTransaction if a complete transaction is ready, null otherwise
     */
    protected StandardizedTransaction handleMultilineParsing(String line, boolean canParse) {
        if (!canParse) {
            return null;
        }

        try {
            // Check if this is a continuation line
            if (isContinuationLine(line)) {
                if (pendingTransaction != null) {
                    currentDescriptionLines.add(line.trim());
                }
                return null; // Don't return transaction yet - accumulating description
            }

            // This is a new transaction line - complete previous if exists
            StandardizedTransaction completedTransaction = null;
            if (pendingTransaction != null) {
                completedTransaction = buildCompletedTransaction();
            }

            // Parse new transaction
            StandardizedTransaction newTransaction = parseTransactionLine(line);
            if (newTransaction != null) {
                pendingTransaction = newTransaction;
                currentDescriptionLines.clear();
                currentDescriptionLines.add(newTransaction.getDescription());
                lastLineWasTransaction = true;
            }

            return completedTransaction; // Return previous transaction (if any)
        } catch (Exception e) {
            // Let subclasses log with their own logger
            return null;
        }
    }

    /**
     * Build a completed transaction with accumulated multiline description.
     * 
     * @return StandardizedTransaction with complete description
     */
    protected StandardizedTransaction buildCompletedTransaction() {
        if (pendingTransaction == null || currentDescriptionLines.isEmpty()) {
            return null;
        }

        String fullDescription = String.join(" ", currentDescriptionLines);
        
        // Rebuild transaction with complete description
        return new StandardizedTransaction.Builder()
                .date(pendingTransaction.getDate())
                .description(fullDescription)
                .debitAmount(pendingTransaction.getDebitAmount())
                .creditAmount(pendingTransaction.getCreditAmount())
                .balance(pendingTransaction.getBalance())
                .serviceFee(pendingTransaction.getServiceFee())
                .build();
    }

    /**
     * Get the final pending transaction when end of statement is reached.
     * Call this after processing all lines to get the last transaction.
     * 
     * @return The final completed transaction, or null if none pending
     */
    public StandardizedTransaction getCompletedTransaction() {
        if (pendingTransaction != null && !currentDescriptionLines.isEmpty()) {
            StandardizedTransaction completed = buildCompletedTransaction();
            pendingTransaction = null;
            currentDescriptionLines.clear();
            lastLineWasTransaction = false;
            return completed;
        }
        return null;
    }

    /**
     * Reset parser state. Call this when starting a new statement.
     */
    public void reset() {
        pendingTransaction = null;
        currentDescriptionLines.clear();
        lastLineWasTransaction = false;
    }
}
