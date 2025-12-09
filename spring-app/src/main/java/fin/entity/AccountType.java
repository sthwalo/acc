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

package fin.entity;

/**
 * Represents the type of an account in the chart of accounts.
 * Each type has a normal balance (debit or credit) that determines
 * whether increases to the account are recorded as debits or credits.
 */
public enum AccountType {
    ASSET("A", "Asset", 'D'),
    LIABILITY("L", "Liability", 'C'),
    EQUITY("E", "Equity", 'C'),
    REVENUE("R", "Revenue", 'C'),
    EXPENSE("X", "Expense", 'D');

    private final String code;
    private final String description;
    private final char normalBalance;

    AccountType(String initialCode, String initialDescription, char initialNormalBalance) {
        this.code = initialCode;
        this.description = initialDescription;
        this.normalBalance = initialNormalBalance;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public char getNormalBalance() {
        return normalBalance;
    }

    public static AccountType fromCode(String code) {
        for (AccountType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown account type code: " + code);
    }
}
