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

    public AccountInfo(String accountCode, String accountName, String normalBalance, String accountType) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.normalBalance = normalBalance;
        this.accountType = accountType;
    }

    // Getters and Setters
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(String normalBalance) {
        this.normalBalance = normalBalance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    @Override
    public String toString() {
        return String.format("AccountInfo{code='%s', name='%s', normalBalance='%s', type='%s'}",
                accountCode, accountName, normalBalance, accountType);
    }
}
