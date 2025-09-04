package fin.model;

/**
 * Represents the type of an account in the chart of accounts.
 * Each type has a normal balance (debit or credit) that determines
 * whether increases to the account are recorded as debits or credits.
 */
public enum AccountType {
    ASSET("A", "Asset", 'D'),
    LIABILITY("L", "Liability", 'C'),
    EQUITY("E", "Equity", 'C'),
    REVENUE("R", "Revenue", 'C'),
    EXPENSE("X", "Expense", 'D');

    private final String code;
    private final String description;
    private final char normalBalance;

    AccountType(String code, String description, char normalBalance) {
        this.code = code;
        this.description = description;
        this.normalBalance = normalBalance;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public char getNormalBalance() {
        return normalBalance;
    }

    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type code: " + code);
    }
}
