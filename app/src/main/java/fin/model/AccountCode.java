package fin.model;

/**
 * Value object representing an account code.
 * Follows Domain-Driven Design principles for type safety and expressiveness.
 */
public record AccountCode(String value) {
    public AccountCode {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account code cannot be null or empty");
        }
        if (!value.matches("^[0-9]{4}(-[0-9]{1,3})?$")) {
            throw new IllegalArgumentException("Account code must be in format XXXX or XXXX-YYY (e.g., 1000 or 1000-001)");
        }
    }

    /**
     * Gets the main account code (first 4 digits).
     */
    public String getMainCode() {
        return value.split("-")[0];
    }

    /**
     * Gets the subaccount code if present.
     */
    public String getSubCode() {
        String[] parts = value.split("-");
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Checks if this is a subaccount.
     */
    public boolean isSubAccount() {
        return value.contains("-");
    }

    @Override
    public String toString() {
        return value;
    }
}