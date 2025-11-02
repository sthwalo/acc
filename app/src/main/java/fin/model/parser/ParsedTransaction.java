/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    private final BigDecimal balance;
    private final boolean hasServiceFee;

    private ParsedTransaction(Builder builder) {
        this.type = builder.type;
        this.description = builder.description;
        this.amount = builder.amount;
        this.date = builder.date;
        this.reference = builder.reference;
        this.balance = builder.balance;
        this.hasServiceFee = builder.hasServiceFee;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean hasServiceFee() {
        return hasServiceFee;
    }

    public static class Builder {
        private TransactionType type;
        private String description;
        private BigDecimal amount;
        private LocalDate date;
        private String reference;
        private BigDecimal balance;
        private boolean hasServiceFee;

        public Builder type(TransactionType newType) {
            this.type = newType;
            return this;
        }

        public Builder description(String newDescription) {
            this.description = newDescription;
            return this;
        }

        public Builder amount(BigDecimal newAmount) {
            this.amount = newAmount;
            return this;
        }

        public Builder date(LocalDate newDate) {
            this.date = newDate;
            return this;
        }

        public Builder reference(String newReference) {
            this.reference = newReference;
            return this;
        }

        public Builder balance(BigDecimal newBalance) {
            this.balance = newBalance;
            return this;
        }

        public Builder hasServiceFee(boolean newHasServiceFee) {
            this.hasServiceFee = newHasServiceFee;
            return this;
        }

        public ParsedTransaction build() {
            return new ParsedTransaction(this);
        }
    }
}
