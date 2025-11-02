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
