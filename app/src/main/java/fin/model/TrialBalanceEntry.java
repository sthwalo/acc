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
    public TrialBalanceEntry(String initialAccountCode, String initialAccountName, 
                           BigDecimal initialDebitBalance, BigDecimal initialCreditBalance) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = "D"; // Default to debit normal balance
        this.debitBalance = initialDebitBalance;
        this.creditBalance = initialCreditBalance;
        this.openingBalance = BigDecimal.ZERO;
        this.periodDebits = initialDebitBalance;
        this.periodCredits = initialCreditBalance;
        this.closingBalance = initialDebitBalance.subtract(initialCreditBalance);
    }

    // Constructor for comprehensive trial balance (assumes Debit normal balance)
    public TrialBalanceEntry(String initialAccountCode, String initialAccountName, 
                           BigDecimal initialOpeningBalance, BigDecimal initialPeriodDebits, 
                           BigDecimal initialPeriodCredits, BigDecimal initialClosingBalance) {
        this(initialAccountCode, initialAccountName, "D", initialOpeningBalance, initialPeriodDebits, initialPeriodCredits, initialClosingBalance);
    }

    // Full constructor with normal balance
    public TrialBalanceEntry(String initialAccountCode, String initialAccountName, String initialNormalBalance,
                           BigDecimal initialOpeningBalance, BigDecimal initialPeriodDebits, 
                           BigDecimal initialPeriodCredits, BigDecimal initialClosingBalance) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = initialNormalBalance;
        this.openingBalance = initialOpeningBalance;
        this.periodDebits = initialPeriodDebits;
        this.periodCredits = initialPeriodCredits;
        this.closingBalance = initialClosingBalance;
        
        // Set legacy fields for backward compatibility
        this.debitBalance = initialPeriodDebits;
        this.creditBalance = initialPeriodCredits;
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