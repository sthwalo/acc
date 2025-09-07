package fin.validation;

import fin.model.BankTransaction;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Validator for BankTransaction entities
 */
public class BankTransactionValidator implements ModelValidator<BankTransaction> {
    @Override
    public ValidationResult validate(BankTransaction transaction) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (transaction.getCompanyId() == null) {
            result.addError("companyId", "Company ID is required");
        }

        if (transaction.getFiscalPeriodId() == null) {
            result.addError("fiscalPeriodId", "Fiscal Period ID is required");
        }

        if (transaction.getTransactionDate() == null) {
            result.addError("transactionDate", "Transaction date is required");
        } else if (transaction.getTransactionDate().isAfter(LocalDate.now())) {
            result.addError("transactionDate", "Transaction date cannot be in the future");
        }

        if (transaction.getDetails() == null || transaction.getDetails().trim().isEmpty()) {
            result.addError("details", "Transaction details are required");
        }

        // Amount validation
        if (transaction.getDebitAmount() != null && transaction.getCreditAmount() != null) {
            if (transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 
                && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                result.addError("amount", "Transaction cannot have both debit and credit amounts");
            }
        }

        if (transaction.getDebitAmount() == null && transaction.getCreditAmount() == null) {
            result.addError("amount", "Either debit or credit amount must be specified");
        }

        // Amount sign validation
        if (transaction.getDebitAmount() != null 
            && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("debitAmount", "Debit amount cannot be negative");
        }

        if (transaction.getCreditAmount() != null 
            && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) < 0) {
            result.addError("creditAmount", "Credit amount cannot be negative");
        }

        // Balance validation - balance should be positive (running balance after transaction)
        if (transaction.getBalance() != null 
            && transaction.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            // Allow negative balances but warn
            // Some accounts can have overdrafts
        }

        return result;
    }
}
