package fin.model;

/**
 * Value object representing a transaction ID.
 * Follows Domain-Driven Design principles for type safety and expressiveness.
 */
public record TransactionId(Long value) {
    public TransactionId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Transaction ID must be a positive non-null value");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}