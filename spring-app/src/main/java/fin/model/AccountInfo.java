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
 * Simple account metadata model for General Ledger.
 * Contains essential information needed to generate ledger reports.
 */
public class AccountInfo {
    private String accountCode;
    private String accountName;
    private String normalBalance;  // 'D' for Debit, 'C' for Credit
    private String accountType;    // Asset, Liability, Equity, Revenue, Expense

    public AccountInfo() {
    }

    public AccountInfo(String initialAccountCode, String initialAccountName, String initialNormalBalance, String initialAccountType) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = initialNormalBalance;
        this.accountType = initialAccountType;
    }

    // Getters and Setters
    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String newAccountCode) {
        this.accountCode = newAccountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String newAccountName) {
        this.accountName = newAccountName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }

    public void setNormalBalance(String newNormalBalance) {
        this.normalBalance = newNormalBalance;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String newAccountType) {
        this.accountType = newAccountType;
    }

    @Override
    public String toString() {
        return String.format("AccountInfo{code='%s', name='%s', normalBalance='%s', type='%s'}",
                accountCode, accountName, normalBalance, accountType);
    }
}