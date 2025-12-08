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

/**
 * DTO for Income Statement report entries.
 * Represents categorized revenue and expense accounts.
 */
public class IncomeStatementDTO {
    private final String category;
    private final String accountCode;
    private final String accountName;
    private final BigDecimal amount;
    private final String type; // "REVENUE" or "EXPENSE"

    public IncomeStatementDTO(String category, String accountCode, String accountName,
                             BigDecimal amount, String type) {
        this.category = category;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.amount = amount;
        this.type = type;
    }

    public String getCategory() { return category; }
    public String getAccountCode() { return accountCode; }
    public String getAccountName() { return accountName; }
    public BigDecimal getAmount() { return amount; }
    public String getType() { return type; }
}