package fin.exception;

/**
 * Exception thrown when a transaction mapping rule operation fails.
 */
public class TransactionMappingException extends DomainException {
    public TransactionMappingException(String message) {
        super(message);
    }

    public TransactionMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}