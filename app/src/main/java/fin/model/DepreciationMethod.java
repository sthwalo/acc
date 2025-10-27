package fin.model;

/**
 * Enumeration of supported depreciation methods
 */
public enum DepreciationMethod {
    STRAIGHT_LINE("SL", "Straight-Line Depreciation"),
    DECLINING_BALANCE("DB", "Declining Balance Depreciation"),
    FIN("FIN", "FIN Depreciation (Half-Year Convention)");

    private final String code;
    private final String description;

    DepreciationMethod(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}