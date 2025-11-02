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

import java.math.BigDecimal;

/**
 * Represents an account balance from General Ledger for Trial Balance posting.
 * Implements the correct accounting flow: Journal Entries → General Ledger → Trial Balance
 */
public class AccountBalance {
    private final String accountCode;
    private final String accountName;
    private final String normalBalance; // 'D' for DEBIT normal, 'C' for CREDIT normal
    private final BigDecimal openingBalance;
    private final BigDecimal periodDebits;
    private final BigDecimal periodCredits;
    private final BigDecimal closingBalance;

    public AccountBalance(String initialAccountCode, String initialAccountName, String initialNormalBalance,
                         BigDecimal initialOpeningBalance, BigDecimal initialPeriodDebits, BigDecimal initialPeriodCredits, 
                         BigDecimal initialClosingBalance) {
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        this.normalBalance = initialNormalBalance;
        this.openingBalance = initialOpeningBalance != null ? initialOpeningBalance : BigDecimal.ZERO;
        this.periodDebits = initialPeriodDebits != null ? initialPeriodDebits : BigDecimal.ZERO;
        this.periodCredits = initialPeriodCredits != null ? initialPeriodCredits : BigDecimal.ZERO;
        this.closingBalance = initialClosingBalance != null ? initialClosingBalance : BigDecimal.ZERO;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getNormalBalance() {
        return normalBalance;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public BigDecimal getPeriodDebits() {
        return periodDebits;
    }

    public BigDecimal getPeriodCredits() {
        return periodCredits;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    /**
     * Get the trial balance debit amount based on normal balance and closing balance.
     * For DEBIT normal accounts: positive closing → debit column, negative → credit column
     * For CREDIT normal accounts: positive closing → credit column, negative → debit column
     */
    public BigDecimal getTrialBalanceDebit() {
        if ("D".equals(normalBalance)) {
            return closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? closingBalance : BigDecimal.ZERO;
        } else {
            return closingBalance.compareTo(BigDecimal.ZERO) < 0 ? closingBalance.negate() : BigDecimal.ZERO;
        }
    }

    /**
     * Get the trial balance credit amount based on normal balance and closing balance.
     * For DEBIT normal accounts: negative closing → credit column, positive → debit column  
     * For CREDIT normal accounts: positive closing → credit column, negative → debit column
     */
    public BigDecimal getTrialBalanceCredit() {
        if ("D".equals(normalBalance)) {
            return closingBalance.compareTo(BigDecimal.ZERO) < 0 ? closingBalance.negate() : BigDecimal.ZERO;
        } else {
            return closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? closingBalance : BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return String.format("AccountBalance{code='%s', name='%s', normalBalance='%s', " +
                           "opening=%s, debits=%s, credits=%s, closing=%s}",
                           accountCode, accountName, normalBalance, openingBalance, 
                           periodDebits, periodCredits, closingBalance);
    }
}