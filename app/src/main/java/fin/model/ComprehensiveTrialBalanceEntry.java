package fin.model;

import java.math.BigDecimal;

/**
 * Represents a comprehensive trial balance entry with opening balances, period movements, and closing balances
 */
public class ComprehensiveTrialBalanceEntry {
    private final String accountCode;
    private final String accountName;
    private final BigDecimal openingBalance;
    private final BigDecimal periodDebits;
    private final BigDecimal periodCredits;
    private final BigDecimal closingBalance;

    public ComprehensiveTrialBalanceEntry(String accountCode, String accountName, 
                                        BigDecimal openingBalance, BigDecimal periodDebits, 
                                        BigDecimal periodCredits, BigDecimal closingBalance) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.openingBalance = openingBalance;
        this.periodDebits = periodDebits;
        this.periodCredits = periodCredits;
        this.closingBalance = closingBalance;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
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

    @Override
    public String toString() {
        return String.format("ComprehensiveTrialBalanceEntry{accountCode='%s', accountName='%s', opening=%s, debits=%s, credits=%s, closing=%s}",
                accountCode, accountName, openingBalance, periodDebits, periodCredits, closingBalance);
    }
}