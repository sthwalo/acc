package fin.model;

/**
 * Rule mapping for transaction classification
 */
public class RuleMapping {
    private String accountCode;
    private String accountName;

    public RuleMapping() {}

    public RuleMapping(String accountCode, String accountName) {
        this.accountCode = accountCode;
        this.accountName = accountName;
    }

    // Getters and setters
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
}