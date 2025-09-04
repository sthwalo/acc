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

        // Balance validation
        BigDecimal expectedBalance = null;
        if (transaction.getDebitAmount() != null) {
            expectedBalance = transaction.getDebitAmount().negate();
        } else if (transaction.getCreditAmount() != null) {
            expectedBalance = transaction.getCreditAmount();
        }

        if (expectedBalance != null && transaction.getBalance() != null) {
            // Allow for small rounding differences
            BigDecimal difference = expectedBalance.subtract(transaction.getBalance()).abs();
            if (difference.compareTo(new BigDecimal("0.01")) > 0) {
                result.addError("balance", "Balance does not match transaction amount");
            }
        }

        return result;
    }
}
