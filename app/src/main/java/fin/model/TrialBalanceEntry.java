package fin.model;

import java.math.BigDecimal;

/**
 * Represents a trial balance entry with account information and balances
 */
public class TrialBalanceEntry {
    private final String accountCode;
    private final String accountName;
    private final BigDecimal debitBalance;
    private final BigDecimal creditBalance;

    public TrialBalanceEntry(String accountCode, String accountName, BigDecimal debitBalance, BigDecimal creditBalance) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.debitBalance = debitBalance;
        this.creditBalance = creditBalance;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public BigDecimal getDebitBalance() {
        return debitBalance;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public BigDecimal getNetBalance() {
        return debitBalance.subtract(creditBalance);
    }

    @Override
    public String toString() {
        return String.format("TrialBalanceEntry{accountCode='%s', accountName='%s', debit=%s, credit=%s}",
                accountCode, accountName, debitBalance, creditBalance);
    }
}