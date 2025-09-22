package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;

/**
 * Interface for transaction parsers.
 * Each implementation handles a specific type of transaction pattern.
 */
public interface TransactionParser {
    /**
     * Checks if this parser can handle the given line.
     * @param line The line to check
     * @param context Additional context that might be needed for parsing decision
     * @return true if this parser can handle the line
     */
    boolean canParse(String line, TransactionParsingContext context);

    /**
     * Parse a line into a transaction.
     * @param line The line to parse
     * @param context The parsing context (statement metadata, etc.)
     * @return The parsed transaction or null if parsing fails
     * @throws IllegalArgumentException if the line cannot be parsed
     */
    ParsedTransaction parse(String line, TransactionParsingContext context);
}
