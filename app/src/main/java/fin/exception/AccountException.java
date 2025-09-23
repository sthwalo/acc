package fin.exception;

/**
 * Exception thrown when an account-related operation fails.
 */
public class AccountException extends DomainException {
    public AccountException(String message) {
        super(message);
    }

    public AccountException(String message, Throwable cause) {
        super(message, cause);
    }
}