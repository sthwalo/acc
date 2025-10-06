package fin.model;

import java.math.BigDecimal;

/**
 * Represents a trial balance entry with account information, opening balances, 
 * period movements, and closing balances.
 * Supports both simple (debit/credit totals) and comprehensive (full movements) formats.
 */
public class TrialBalanceEntry {
    private final String accountCode;
    private final String accountName;
    private final String normalBalance; // 'D' for Debit, 'C' for Credit
    private final BigDecimal openingBalance;
    private final BigDecimal periodDebits;
    private final BigDecimal periodCredits;
    private final BigDecimal closingBalance;
    
    // Legacy fields for backward compatibility
    private final BigDecimal debitBalance;
    private final BigDecimal creditBalance;

    // Constructor for simple trial balance (backward compatibility)
    public TrialBalanceEntry(String accountCode, String accountName, 
                           BigDecimal debitBalance, BigDecimal creditBalance) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.normalBalance = "D"; // Default to debit normal balance
        this.debitBalance = debitBalance;
        this.creditBalance = creditBalance;
        this.openingBalance = BigDecimal.ZERO;
        this.periodDebits = debitBalance;
        this.periodCredits = creditBalance;
        this.closingBalance = debitBalance.subtract(creditBalance);
    }

    // Constructor for comprehensive trial balance (assumes Debit normal balance)
    public TrialBalanceEntry(String accountCode, String accountName, 
                           BigDecimal openingBalance, BigDecimal periodDebits, 
                           BigDecimal periodCredits, BigDecimal closingBalance) {
        this(accountCode, accountName, "D", openingBalance, periodDebits, periodCredits, closingBalance);
    }

    // Full constructor with normal balance
    public TrialBalanceEntry(String accountCode, String accountName, String normalBalance,
                           BigDecimal openingBalance, BigDecimal periodDebits, 
                           BigDecimal periodCredits, BigDecimal closingBalance) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.normalBalance = normalBalance;
        this.openingBalance = openingBalance;
        this.periodDebits = periodDebits;
        this.periodCredits = periodCredits;
        this.closingBalance = closingBalance;
        
        // Set legacy fields for backward compatibility
        this.debitBalance = periodDebits;
        this.creditBalance = periodCredits;
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

    // Legacy methods for backward compatibility
    public BigDecimal getDebitBalance() {
        return debitBalance;
    }

    public BigDecimal getCreditBalance() {
        return creditBalance;
    }

    public BigDecimal getNetBalance() {
        return closingBalance;
    }

    @Override
    public String toString() {
        return String.format("TrialBalanceEntry{accountCode='%s', accountName='%s', normalBalance='%s', opening=%s, debits=%s, credits=%s, closing=%s}",
                accountCode, accountName, normalBalance, openingBalance, periodDebits, periodCredits, closingBalance);
    }
}