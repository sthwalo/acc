package fin.exception;

/**
 * Exception thrown when a category-related operation fails.
 */
public class CategoryException extends DomainException {
    public CategoryException(String message) {
        super(message);
    }

    public CategoryException(String message, Throwable cause) {
        super(message, cause);
    }
}