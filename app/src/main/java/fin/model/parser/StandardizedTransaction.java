package fin.model.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Standardized tabular transaction format that all bank parsers must conform to.
 * Based on Standard Bank's tabular structure: Details, Service Fee, Debits, Credits, Date, Balance
 *
 * This ensures consistent handling across all banks (FNB, Standard Bank, Absa) before
 * final conversion to BankTransaction entities.
 */
public class StandardizedTransaction {
    private final LocalDate date;
    private final String description;
    private final BigDecimal debitAmount;      // Debits column (negative amounts or amounts with "-" suffix)
    private final BigDecimal creditAmount;     // Credits column (positive amounts)
    private final BigDecimal balance;          // Running balance
    private final BigDecimal serviceFee;       // Service fee amount (if applicable)
    private final String reference;            // Transaction reference (optional)
    private final TransactionType type;        // Final determined type

    private StandardizedTransaction(Builder builder) {
        this.date = builder.date;
        this.description = builder.description;
        this.debitAmount = builder.debitAmount != null ? builder.debitAmount : BigDecimal.ZERO;
        this.creditAmount = builder.creditAmount != null ? builder.creditAmount : BigDecimal.ZERO;
        this.balance = builder.balance;
        this.serviceFee = builder.serviceFee != null ? builder.serviceFee : BigDecimal.ZERO;
        this.reference = builder.reference;
        this.type = determineTransactionType();
    }

    /**
     * Determine transaction type based on amounts and description
     */
    private TransactionType determineTransactionType() {
        // Service fees take precedence
        if (serviceFee.compareTo(BigDecimal.ZERO) > 0 ||
            description.toLowerCase().contains("fee") ||
            description.toLowerCase().contains("charge")) {
            return TransactionType.SERVICE_FEE;
        }

        // If we have explicit debit/credit amounts, use them
        if (debitAmount.compareTo(BigDecimal.ZERO) > 0 && creditAmount.compareTo(BigDecimal.ZERO) == 0) {
            return TransactionType.DEBIT;
        }
        if (creditAmount.compareTo(BigDecimal.ZERO) > 0 && debitAmount.compareTo(BigDecimal.ZERO) == 0) {
            return TransactionType.CREDIT;
        }

        // Fallback: analyze description for keywords
        String desc = description.toLowerCase();
        if (desc.contains("withdrawal") || desc.contains("debit") || desc.contains("payment") ||
            desc.contains("transfer to") || desc.contains("atm") || desc.contains("eft out")) {
            return TransactionType.DEBIT;
        }
        if (desc.contains("deposit") || desc.contains("credit") || desc.contains("salary") ||
            desc.contains("transfer from") || desc.contains("interest") || desc.contains("dividend") ||
            desc.contains("eft in") || desc.contains("refund")) {
            return TransactionType.CREDIT;
        }

        // Default to credit for positive net amounts, debit for negative
        BigDecimal netAmount = creditAmount.subtract(debitAmount);
        return netAmount.compareTo(BigDecimal.ZERO) >= 0 ? TransactionType.CREDIT : TransactionType.DEBIT;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public String getDescription() { return description; }
    public BigDecimal getDebitAmount() { return debitAmount; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getServiceFee() { return serviceFee; }
    public String getReference() { return reference; }
    public TransactionType getType() { return type; }

    /**
     * Get the transaction amount (debit amount if debit, credit amount if credit, service fee if service fee)
     */
    public BigDecimal getAmount() {
        switch (type) {
            case DEBIT:
                return debitAmount;
            case CREDIT:
                return creditAmount;
            case SERVICE_FEE:
                return serviceFee;
            default:
                return BigDecimal.ZERO;
        }
    }

    /**
     * Check if this transaction has a service fee
     */
    public boolean hasServiceFee() {
        return serviceFee.compareTo(BigDecimal.ZERO) > 0 || type == TransactionType.SERVICE_FEE;
    }

    public static class Builder {
        private LocalDate date;
        private String description;
        private BigDecimal debitAmount;
        private BigDecimal creditAmount;
        private BigDecimal balance;
        private BigDecimal serviceFee;
        private String reference;

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder debitAmount(BigDecimal debitAmount) {
            this.debitAmount = debitAmount;
            return this;
        }

        public Builder creditAmount(BigDecimal creditAmount) {
            this.creditAmount = creditAmount;
            return this;
        }

        public Builder balance(BigDecimal balance) {
            this.balance = balance;
            return this;
        }

        public Builder serviceFee(BigDecimal serviceFee) {
            this.serviceFee = serviceFee;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        /**
         * Set amount with automatic debit/credit determination based on sign
         */
        public Builder amount(BigDecimal amount) {
            if (amount == null) {
                return this;
            }
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                // Negative amount = debit
                this.debitAmount = amount.abs();
                this.creditAmount = BigDecimal.ZERO;
            } else {
                // Positive amount = credit
                this.creditAmount = amount;
                this.debitAmount = BigDecimal.ZERO;
            }
            return this;
        }

        /**
         * Set amount with explicit type specification
         */
        public Builder amount(BigDecimal amount, TransactionType type) {
            if (amount == null || type == null) {
                return this;
            }

            BigDecimal absAmount = amount.abs();
            switch (type) {
                case DEBIT:
                    this.debitAmount = absAmount;
                    this.creditAmount = BigDecimal.ZERO;
                    break;
                case CREDIT:
                    this.creditAmount = absAmount;
                    this.debitAmount = BigDecimal.ZERO;
                    break;
                case SERVICE_FEE:
                    this.serviceFee = absAmount;
                    this.debitAmount = BigDecimal.ZERO;
                    this.creditAmount = BigDecimal.ZERO;
                    break;
            }
            return this;
        }

        public StandardizedTransaction build() {
            if (date == null) {
                throw new IllegalStateException("Date is required");
            }
            if (description == null || description.trim().isEmpty()) {
                throw new IllegalStateException("Description is required");
            }
            return new StandardizedTransaction(this);
        }
    }
}