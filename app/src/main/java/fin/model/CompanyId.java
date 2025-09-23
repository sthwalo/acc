package fin.model;

/**
 * Value object representing a company ID.
 * Follows Domain-Driven Design principles for type safety and expressiveness.
 */
public record CompanyId(Long value) {
    public CompanyId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Company ID must be a positive non-null value");
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}