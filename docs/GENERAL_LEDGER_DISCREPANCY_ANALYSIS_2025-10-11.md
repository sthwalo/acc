# General Ledger Discrepancy Analysis & Resolution

**Date:** October 11, 2025  
**Company:** Company ID 2  
**Fiscal Period:** FY2024-2025 (ID: 7)  
**Status:** 🚨 CRITICAL - Multiple Issues Found

---

## 🎯 Executive Summary

The General Ledger report is showing **INCORRECT** balances due to:
1. ❌ **Missing journal entries** (33 transactions, R493,178.42 total)
2. ❌ **Wrong balance signs** (showing CREDIT instead of DEBIT for Bank account)
3. ❌ **Incorrect GL calculation logic** (not properly accounting for normal balances)

---

## 📊 Database-Level Findings

### Bank Account (1100) - Actual vs Reported

| Source | Opening | Debits | Credits | Closing | Sign |
|--------|---------|--------|---------|---------|------|
| **Bank Statement** | R479,507.94 | - | - | **R24,109.81** | **DR** ✅ |
| **Bank Transactions Table** | R479,507.94 | R729,970.13 | R275,000.00 | R24,537.81* | DR |
| **Journal Entries** | R479,507.94 | R479,507.94 | R511,791.71 | -R32,283.77 | **CR** ❌ |
| **GL Report** | R479,507.94 | - | R511,791.71 | **R32,283.77** | **CR** ❌❌❌ |

*Calculated: 479,507.94 - 729,970.13 + 275,000.00 = R24,537.81 (R428 discrepancy from actual)

---

## 🔍 Root Cause Analysis

### Issue 1: Missing Journal Entries (33 transactions)

**Database Query:**
```sql
SELECT COUNT(*) as missing_count, 
       SUM(bt.debit_amount) as missing_debits, 
       SUM(bt.credit_amount) as missing_credits
FROM bank_transactions bt
LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id
WHERE bt.company_id = 2 AND bt.fiscal_period_id = 7 AND jel.id IS NULL;
```

**Results:**
- **33 transactions** without journal entries
- **Missing debits (payments):** R218,178.42
- **Missing credits (deposits):** R275,000.00
- **Total missing amount:** R493,178.42

**Sample Missing Transactions:**
```
2024-03-01 | PENSION FUND CONTRIBUTION     | R28,813.93 | R0 | 9900
2024-03-01 | NGWAKWANE E TAU (Loan)        | R26,000.00 | R0 | 4000
2024-03-02 | SARS-VAT-VALUE AD             | R46,095.65 | R0 | 3100
2024-03-12 | MAGTAPE CREDIT COMPANY ASSIST | R0         | R275,000 | (unclassified)
```

**Impact on GL:**
- Bank account journal entries show ONLY R479,507.94 in debits (opening balance)
- Should show: R479,507.94 (opening) + R275,000 (deposit) = R754,507.94 debits
- Credits show R511,791.71 but should be R729,970.13 (all payments)

### Issue 2: Bank Transaction Terminology vs Accounting Terminology

**Bank Statement Perspective:**
- **DEBIT** = Money OUT (payment) → Balance DECREASES
- **CREDIT** = Money IN (deposit) → Balance INCREASES

**Accounting Perspective (Company's Books):**
- Bank is an **ASSET** account
- **DEBIT** = Money IN (deposit) → Asset INCREASES
- **CREDIT** = Money OUT (payment) → Asset DECREASES

**Current Journal Entry Logic (WRONG):**
```
Payment: R1,200 (bank statement shows as DEBIT)
Journal Entry Created:
  DEBIT: Expense Account (8100) R1,200  ✅ Correct
  CREDIT: Bank Account (1100) R1,200    ✅ Correct

Deposit: R275,000 (bank statement shows as CREDIT)
Journal Entry Created:
  ??? Missing - No journal entry generated!  ❌ WRONG
```

**The deposit should create:**
```
  DEBIT: Bank Account (1100) R275,000     (increases asset)
  CREDIT: Source Account (loan/revenue) R275,000
```

### Issue 3: GL Balance Calculation Logic (WRONG)

**Current Code Logic (from GeneralLedgerService.java):**
```java
// For Bank account (1100 - Asset):
Opening: R479,507.94 DR
Credits: R511,791.71
Balance shown: Opening + Credits = R479,507.94 + R511,791.71 = R991,299.65 ???

// Report shows R32,283.77 CR because calculation is:
// Net = Debits - Credits = 479,507.94 - 511,791.71 = -32,283.77 (CREDIT)
```

**CORRECT Logic Should Be:**
```java
// Asset accounts have DEBIT normal balance
// Formula: Closing = Opening + Period Debits - Period Credits

Opening: R479,507.94 DR
Period Debits: R275,000.00 (deposit - should be in journal entries!)
Period Credits: R730,398.13 (all payments including missing R218,178.42)
Closing = 479,507.94 + 275,000.00 - 730,398.13 = R24,109.81 DR ✅
```

---

## 🔧 Required Fixes

### Fix 1: Generate Missing Journal Entries (DATABASE)

**Step 1: Generate journal entries for the 33 missing transactions**

This needs to be done through the application's "Generate Journal Entries" function, which will:
1. Read each bank transaction without a journal entry
2. Create journal entry header
3. Create two lines:
   - Line 1: CREDIT Bank (1100) with transaction's debit_amount (payment out)
   - Line 2: DEBIT Expense/Asset account based on account_code
   - OR for deposits:
   - Line 1: DEBIT Bank (1100) with transaction's credit_amount (deposit in)
   - Line 2: CREDIT Source account (loan/revenue)

**Critical Transaction to Fix:**
```sql
-- The R275,000 deposit (ID = find it)
SELECT id, transaction_date, details, credit_amount 
FROM bank_transactions 
WHERE company_id = 2 
  AND fiscal_period_id = 7 
  AND credit_amount = 275000.00;
```

This deposit needs:
```
Journal Entry: AUTO-XXXXX
  DEBIT: 1100 Bank R275,000.00
  CREDIT: 4000 Long-term Loans R275,000.00 (based on description "MAGTAPE CREDIT COMPANY ASSIST")
```

### Fix 2: GL Service Calculation Logic (CODE)

**File:** `app/src/main/java/fin/service/GeneralLedgerService.java`

**Current Problem:**
The GL report is showing balances but not properly handling normal balance types.

**Required Changes:**

1. **Add normal balance type handling:**
```java
// Get account category to determine normal balance
String normalBalance = accountInfo.getNormalBalance(); // 'D' or 'C'

// Calculate closing balance based on normal balance type
BigDecimal closingBalance;
if ("D".equals(normalBalance)) {
    // DEBIT normal balance (Assets, Expenses)
    // Closing = Opening + Debits - Credits
    closingBalance = openingBalance.add(totalDebits).subtract(totalCredits);
} else {
    // CREDIT normal balance (Liabilities, Equity, Revenue)
    // Closing = Opening + Credits - Debits
    closingBalance = openingBalance.add(totalCredits).subtract(totalDebits);
}

// Determine balance side
String balanceSide = closingBalance.compareTo(BigDecimal.ZERO) >= 0 ? normalBalance : (normalBalance.equals("D") ? "C" : "D");
```

2. **Fix balance display:**
```java
// If closing balance is negative for a DEBIT account, show as CREDIT
// If closing balance is negative for a CREDIT account, show as DEBIT
if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
    closingBalance = closingBalance.abs(); // Make positive
    balanceSide = balanceSide.equals("DR") ? "CR" : "DR"; // Flip side
}
```

### Fix 3: Repository Methods (CODE)

**File:** `app/src/main/java/fin/repository/JdbcFinancialDataRepository.java`

**Add method to get account normal balance:**
```java
public String getAccountNormalBalance(Long accountId) throws SQLException {
    String sql = """
        SELECT ac.normal_balance 
        FROM accounts a
        JOIN account_categories ac ON a.category_id = ac.id
        WHERE a.id = ?
    """;
    
    try (Connection conn = DriverManager.getConnection(dbUrl);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setLong(1, accountId);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getString("normal_balance");
        }
    }
    return "D"; // Default to DEBIT if not found
}
```

---

## ✅ Verification Queries

### After Fixing, Run These Queries:

**1. Verify all transactions have journal entries:**
```sql
SELECT COUNT(*) as missing_count
FROM bank_transactions bt
LEFT JOIN journal_entry_lines jel ON bt.id = jel.source_transaction_id
WHERE bt.company_id = 2 AND bt.fiscal_period_id = 7 AND jel.id IS NULL;
-- Expected: 0
```

**2. Verify Bank account journal entry totals:**
```sql
SELECT 
    SUM(jel.debit_amount) as total_debits,
    SUM(jel.credit_amount) as total_credits,
    SUM(COALESCE(jel.debit_amount, 0)) - SUM(COALESCE(jel.credit_amount, 0)) as net_balance
FROM journal_entry_lines jel
JOIN accounts a ON jel.account_id = a.id
JOIN journal_entries je ON jel.journal_entry_id = je.id
WHERE je.company_id = 2 AND je.fiscal_period_id = 7 AND a.account_code = '1100';
-- Expected: 
-- Debits: R754,507.94 (R479,507.94 opening + R275,000 deposit)
-- Credits: R730,398.13 (all payments)
-- Net: R24,109.81 DEBIT
```

**3. Verify closing balance matches bank statement:**
```sql
SELECT balance as bank_statement_closing
FROM bank_transactions
WHERE company_id = 2 AND fiscal_period_id = 7
ORDER BY transaction_date DESC, id DESC LIMIT 1;
-- Expected: R24,109.81
```

---

## 📋 Action Plan

### Immediate Actions (In Order):

1. **Generate Missing Journal Entries** (via application)
   - Use Data Management → Generate Journal Entries
   - This will create entries for all 33 missing transactions
   
2. **Manually Classify the R275,000 Deposit** (if not already classified)
   ```sql
   UPDATE bank_transactions
   SET account_code = '4000',
       account_name = 'Long-term Loans'
   WHERE company_id = 2 
     AND fiscal_period_id = 7 
     AND credit_amount = 275000.00;
   ```

3. **Fix GeneralLedgerService.java Code**
   - Add normal balance type handling
   - Fix closing balance calculation
   - Fix balance side determination

4. **Test GL Report Generation**
   - Run GL report for fiscal period 7
   - Verify Bank (1100) shows R24,109.81 DR
   - Verify all accounts show correct signs (DR/CR)

5. **Verify Trial Balance**
   - Ensure Total Debits = Total Credits
   - Verify Bank account shows as DEBIT (not CREDIT)

---

## 🎯 Expected Results After Fixes

### Bank Account (1100) - After Fixes:

| Metric | Value | Sign |
|--------|-------|------|
| Opening Balance | R479,507.94 | DR |
| Period Debits | R275,000.00 | (deposit) |
| Period Credits | R730,398.13 | (payments) |
| **Closing Balance** | **R24,109.81** | **DR** ✅ |

### General Ledger Report - After Fixes:

```
ACCOUNT: 1100 - Bank - Current Account
==================================================================
Opening Balance (March 1, 2024):              R 479,507.94 DR

Date       Reference    Description           Debit       Credit      Balance
==================================================================
2024-03-01 AUTO-00001   IB PAYMENT TO...      -           R 1,200.00  R 478,307.94 DR
...
2024-03-12 AUTO-00XXX   MAGTAPE CREDIT...     R 275,000   -           R 284,507.69 DR
...
2024-03-16 AUTO-00214   CASH WITHDRAWAL...    -           R 110.40    R 24,109.81 DR
==================================================================
Period Totals:                                 R 275,000   R 730,398.13
Closing Balance (February 28, 2025):          R 24,109.81 DR ✅
```

---

## 📝 Code Files Requiring Changes

1. **GeneralLedgerService.java**
   - Fix closing balance calculation logic
   - Add normal balance type handling
   - Fix balance side determination

2. **JdbcFinancialDataRepository.java**
   - Add `getAccountNormalBalance(Long accountId)` method
   - Ensure AccountInfo includes normal_balance field

3. **AccountInfo.java** (model)
   - Add `normalBalance` field with getter/setter

---

**Document Status:** Analysis Complete, Awaiting Implementation  
**Priority:** 🔴 CRITICAL - Blocks accurate financial reporting  
**Next Action:** Generate missing journal entries + Fix GL calculation code
