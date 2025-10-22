package fin.model;

/**
 * Result of transaction classification containing account code and name
 */
public class ClassificationResult {
    private String accountCode;
    private String accountName;
    private String classificationReason;

    public ClassificationResult() {}

    public ClassificationResult(String initialAccountCode, String initialAccountName, String initialClassificationReason) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.classificationReason = initialClassificationReason;
    }

    // Getters and setters
    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String newAccountCode) { this.accountCode = newAccountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String newAccountName) { this.accountName = newAccountName; }

    public String getClassificationReason() { return classificationReason; }
    public void setClassificationReason(String newClassificationReason) { this.classificationReason = newClassificationReason; }
}