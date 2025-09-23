package fin.exception;

/**
 * Exception thrown when a database operation fails.
 * Provides more specific error information than generic RuntimeException.
 */
public class DatabaseException extends DomainException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}