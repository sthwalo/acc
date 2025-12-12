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
 * DTO for Trial Balance report entries.
 * Represents account balances with debit/credit columns.
 */
public class TrialBalanceDTO {
    private final String accountCode;
    private final String accountName;
    private final BigDecimal debit;
    private final BigDecimal credit;

    public TrialBalanceDTO(String accountCode, String accountName, BigDecimal debit, BigDecimal credit) {
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.debit = debit;
        this.credit = credit;
    }

    public String getAccountCode() { return accountCode; }
    public String getAccountName() { return accountName; }
    public BigDecimal getDebit() { return debit; }
    public BigDecimal getCredit() { return credit; }
}