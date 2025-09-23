package fin.exception;

/**
 * Base exception for all domain-related errors in the financial management system.
 * Follows clean code principles by providing specific exception types instead of generic RuntimeExceptions.
 */
public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}