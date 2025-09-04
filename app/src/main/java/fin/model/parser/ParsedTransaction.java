package fin.model.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable value object representing a parsed banking transaction.
 */
public class ParsedTransaction {
    private final TransactionType type;
    private final String description;
    private final BigDecimal amount;
    private final LocalDate date;
    private final String reference;

    private ParsedTransaction(Builder builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.amount = builder.amount;
        this.date = builder.date;
        this.reference = builder.reference;
    }

    public TransactionType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getReference() {
        return reference;
    }

    public static class Builder {
        private TransactionType type;
        private String description;
        private BigDecimal amount;
        private LocalDate date;
        private String reference;

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public ParsedTransaction build() {
            return new ParsedTransaction(this);
        }
    }
}
