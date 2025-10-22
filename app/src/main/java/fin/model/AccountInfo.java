package fin.model;

/**
 * Simple account metadata model for General Ledger.
 * Contains essential information needed to generate ledger reports.
 */
public class AccountInfo {
    private String accountCode;
    private String accountName;
    private String normalBalance;  // 'D' for Debit, 'C' for Credit
    private String accountType;    // Asset, Liability, Equity, Revenue, Expense

    public AccountInfo() {
    }

    public AccountInfo(String initialAccountCode, String initialAccountName, String initialNormalBalance, String initialAccountType) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = initialNormalBalance;
        this.accountType = initialAccountType;
    }

    // Getters and Setters
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String newAccountCode) {
        this.accountCode = newAccountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String newAccountName) {
        this.accountName = newAccountName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(String newNormalBalance) {
        this.normalBalance = newNormalBalance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String newAccountType) {
        this.accountType = newAccountType;
    }

    @Override
    public String toString() {
        return String.format("AccountInfo{code='%s', name='%s', normalBalance='%s', type='%s'}",
                accountCode, accountName, normalBalance, accountType);
    }
}
