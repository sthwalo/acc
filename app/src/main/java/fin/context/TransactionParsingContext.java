package fin.context;

import java.time.LocalDate;

/**
 * Context object providing additional information needed for parsing transactions.
 */
public class TransactionParsingContext {
    private final LocalDate statementDate;
    private final String accountNumber;
    private final String statementPeriod;
    private final String sourceFile;

    private TransactionParsingContext(Builder builder) {
        this.statementDate = builder.statementDate;
        this.accountNumber = builder.accountNumber;
        this.statementPeriod = builder.statementPeriod;
        this.sourceFile = builder.sourceFile;
    }

    public LocalDate getStatementDate() {
        return statementDate;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getStatementPeriod() {
        return statementPeriod;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public static class Builder {
        private LocalDate statementDate;
        private String accountNumber;
        private String statementPeriod;
        private String sourceFile;

        public Builder statementDate(LocalDate date) {
            this.statementDate = date;
            return this;
        }

        public Builder accountNumber(String number) {
            this.accountNumber = number;
            return this;
        }

        public Builder statementPeriod(String period) {
            this.statementPeriod = period;
            return this;
        }

        public Builder sourceFile(String file) {
            this.sourceFile = file;
            return this;
        }

        public TransactionParsingContext build() {
            if (statementDate == null) {
                throw new IllegalStateException("Statement date is required");
            }
            return new TransactionParsingContext(this);
        }
    }
}
