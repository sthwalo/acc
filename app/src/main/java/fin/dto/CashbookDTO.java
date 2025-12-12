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

package fin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Cashbook report entries.
 * Represents cash/bank account transactions with receipts and payments.
 */
public class CashbookDTO {
    private final LocalDate date;
    private final String reference;
    private final String description;
    private final BigDecimal receipts; // Debits (money coming in)
    private final BigDecimal payments; // Credits (money going out)
    private final BigDecimal balance;

    public CashbookDTO(LocalDate date, String reference, String description,
                      BigDecimal receipts, BigDecimal payments, BigDecimal balance) {
        this.date = date;
        this.reference = reference;
        this.description = description;
        this.receipts = receipts;
        this.payments = payments;
        this.balance = balance;
    }

    public LocalDate getDate() { return date; }
    public String getReference() { return reference; }
    public String getDescription() { return description; }
    public BigDecimal getReceipts() { return receipts; }
    public BigDecimal getPayments() { return payments; }
    public BigDecimal getBalance() { return balance; }
}