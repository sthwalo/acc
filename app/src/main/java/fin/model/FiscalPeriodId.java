package fin.model;

/**
 * Value object representing a fiscal period ID.
 * Follows Domain-Driven Design principles for type safety and expressiveness.
 */
public record FiscalPeriodId(Long value) {
    public FiscalPeriodId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Fiscal period ID must be a positive non-null value");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}