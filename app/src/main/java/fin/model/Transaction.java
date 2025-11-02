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

package fin.model;

/**
 * Simple transaction model for the modularized services
 */
public class Transaction {
    private Long id;
    private String description;
    private java.math.BigDecimal amount;
    private String debitCredit; // "D" for debit, "C" for credit

    public Transaction() {}

    public Transaction(Long initialId, String initialDescription, java.math.BigDecimal initialAmount, String initialDebitCredit) {
        this.id = initialId;
        this.description = initialDescription;
        this.amount = initialAmount;
        this.debitCredit = initialDebitCredit;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }

    public String getDescription() { return description; }
    public void setDescription(String newDescription) { this.description = newDescription; }

    public java.math.BigDecimal getAmount() { return amount; }
    public void setAmount(java.math.BigDecimal newAmount) { this.amount = newAmount; }

    public String getDebitCredit() { return debitCredit; }
    public void setDebitCredit(String newDebitCredit) { this.debitCredit = newDebitCredit; }
}