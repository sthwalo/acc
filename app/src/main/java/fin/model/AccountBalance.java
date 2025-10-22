package fin.model;

import java.math.BigDecimal;

/**
 * Represents an account balance from General Ledger for Trial Balance posting.
 * Implements the correct accounting flow: Journal Entries → General Ledger → Trial Balance
 */
public class AccountBalance {
    private final String accountCode;
    private final String accountName;
    private final String normalBalance; // 'D' for DEBIT normal, 'C' for CREDIT normal
    private final BigDecimal openingBalance;
    private final BigDecimal periodDebits;
    private final BigDecimal periodCredits;
    private final BigDecimal closingBalance;

    public AccountBalance(String initialAccountCode, String initialAccountName, String initialNormalBalance,
                         BigDecimal initialOpeningBalance, BigDecimal initialPeriodDebits, BigDecimal initialPeriodCredits, 
                         BigDecimal initialClosingBalance) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = initialNormalBalance;
        this.openingBalance = initialOpeningBalance != null ? initialOpeningBalance : BigDecimal.ZERO;
        this.periodDebits = initialPeriodDebits != null ? initialPeriodDebits : BigDecimal.ZERO;
        this.periodCredits = initialPeriodCredits != null ? initialPeriodCredits : BigDecimal.ZERO;
        this.closingBalance = initialClosingBalance != null ? initialClosingBalance : BigDecimal.ZERO;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getPeriodDebits() {
        return periodDebits;
    }

    public BigDecimal getPeriodCredits() {
        return periodCredits;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    /**
     * Get the trial balance debit amount based on normal balance and closing balance.
     * For DEBIT normal accounts: positive closing → debit column, negative → credit column
     * For CREDIT normal accounts: positive closing → credit column, negative → debit column
     */
    public BigDecimal getTrialBalanceDebit() {
        if ("D".equals(normalBalance)) {
            return closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? closingBalance : BigDecimal.ZERO;
        } else {
            return closingBalance.compareTo(BigDecimal.ZERO) < 0 ? closingBalance.negate() : BigDecimal.ZERO;
        }
    }

    /**
     * Get the trial balance credit amount based on normal balance and closing balance.
     * For DEBIT normal accounts: negative closing → credit column, positive → debit column  
     * For CREDIT normal accounts: positive closing → credit column, negative → debit column
     */
    public BigDecimal getTrialBalanceCredit() {
        if ("D".equals(normalBalance)) {
            return closingBalance.compareTo(BigDecimal.ZERO) < 0 ? closingBalance.negate() : BigDecimal.ZERO;
        } else {
            return closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? closingBalance : BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return String.format("AccountBalance{code='%s', name='%s', normalBalance='%s', " +
                           "opening=%s, debits=%s, credits=%s, closing=%s}",
                           accountCode, accountName, normalBalance, openingBalance, 
                           periodDebits, periodCredits, closingBalance);
    }
}