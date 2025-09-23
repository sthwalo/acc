package fin.model;

/**
 * Result of transaction classification containing account code and name
 */
public class ClassificationResult {
    private String accountCode;
    private String accountName;
    private String classificationReason;

    public ClassificationResult() {}

    public ClassificationResult(String accountCode, String accountName, String classificationReason) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.classificationReason = classificationReason;
    }

    // Getters and setters
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

    public String getClassificationReason() { return classificationReason; }
    public void setClassificationReason(String classificationReason) { this.classificationReason = classificationReason; }
}