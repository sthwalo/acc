# Mapping Rules Analysis - Account Code Alignment

**Generated:** 2025-10-03  
**Purpose:** Compare mapping rules between `ChartOfAccountsService.java` (security business-specific) and `TransactionMappingService.java` (generic) with `AccountClassificationService.java` (standard South African accounts)

---

## 📊 Executive Summary

### 🚨 **CRITICAL MISALIGNMENT IDENTIFIED**

There are **THREE different account structures** in the system:

1. **ChartOfAccountsService.java** - Security business specific (4000-6999 range)
2. **AccountClassificationService.java** - Standard South African business (1000-9999 range)  
3. **TransactionMappingService.java** - Mixed/conflicting codes (references non-existent accounts)

### Key Issues:
- ❌ **Account code conflicts**: Same codes mean different things
- ❌ **Missing accounts**: Mapping rules reference accounts that don't exist in chart
- ❌ **Wrong account codes**: Account 8310-001 doesn't exist in AccountClassificationService
- ⚠️ **Business model mismatch**: Security business structure vs. generic business structure

---

## 🏗️ Account Structure Comparison

### 1. ChartOfAccountsService (Security Business - Xinghizana Group)

**Account Range:** 1000-6999

#### Assets (1000-1999)
- `1010` - Petty Cash
- `1020` - Bank Account - Standard Bank #203163753
- `1100` - Loans to Directors / Shareholders

#### Liabilities (2000-2999)
- `2100` - Loans from Directors / Shareholders
- `2200` - Pension Fund Payable

#### Equity (3000-3999)
- `3000` - Owner's Capital Contribution
- `3100` - Retained Earnings
- `3200` - Current Year Profit / (Loss)
- `3300` - Director's Drawings

#### Revenue (4000-4999)
- `4000` - Security Services Revenue

#### Cost of Sales (5000-5999)
- `5010` - Salaries - Security Officers
- `5020` - Salaries - Operations Management
- `5030` - Labour Broker Fees
- `5040` - Equipment Rental & Communication
- `5050` - K9 Unit Expenses
- `5060` - Site & Client Specific Expenses
- `5070` - Uniforms & Protective Gear

#### Operating Expenses (6000-6999)
- `6010` - Salaries - Administration & Directors
- `6020` - Accounting & Bank Fees
- `6030` - Insurance Expenses
- `6040` - Office Rent
- `6050` - Telephone & Internet
- `6060` - Pension Fund Contributions
- `6070` - Office Expenses
- `6080` - Motor Vehicle Expenses (Admin)
- `6099` - Other Expenses

**Total Accounts:** ~25 accounts (security business specific)

---

### 2. AccountClassificationService (Standard South African Business)

**Account Range:** 1000-9999 (comprehensive)

#### Current Assets (1000-1999)
- `1000` - Petty Cash
- `1100` - Bank - Current Account
- `1101` - Bank - Savings Account
- `1102` - Bank - Foreign Currency
- `1200` - Accounts Receivable
- `1300` - Inventory
- `1400` - Prepaid Expenses
- `1500` - VAT Input

#### Non-Current Assets (2000-2999)
- `2000` - Property, Plant & Equipment
- `2100` - Accumulated Depreciation
- `2200` - Investments

#### Current Liabilities (3000-3999)
- `3000` - Accounts Payable
- `3100` - VAT Output
- `3200` - PAYE Payable
- `3300` - UIF Payable
- `3400` - SDL Payable
- `3500` - Accrued Expenses

#### Non-Current Liabilities (4000-4999)
- `4000` - Long-term Loans

#### Equity (5000-5999)
- `5000` - Share Capital
- `5100` - Retained Earnings
- `5200` - Current Year Earnings

#### Operating Revenue (6000-6999)
- `6000` - Sales Revenue
- `6100` - Service Revenue
- `6200` - Other Operating Revenue

#### Other Income (7000-7999)
- `7000` - Interest Income
- `7100` - Dividend Income
- `7200` - Gain on Asset Disposal

#### Operating Expenses (8000-8999)
- `8000` - Cost of Goods Sold
- `8100` - Employee Costs
- `8200` - Rent Expense
- `8300` - Utilities
- `8400` - Communication
- `8500` - Motor Vehicle Expenses
- `8600` - Travel & Entertainment
- `8700` - Professional Services
- `8800` - Insurance
- `8900` - Repairs & Maintenance

#### Administrative Expenses (9000-9499)
- `9000` - Office Supplies
- `9100` - Computer Expenses
- `9200` - Marketing & Advertising
- `9300` - Training & Development
- `9400` - Depreciation

#### Finance Costs (9500-9999)
- `9500` - Interest Expense
- `9600` - Bank Charges
- `9700` - Foreign Exchange Loss

**Total Accounts:** ~50+ accounts (comprehensive coverage)

---

### 3. TransactionMappingService Mapping Rules

**CRITICAL:** This service creates mapping rules that reference accounts from BOTH structures above, causing conflicts:

#### Rules Referencing AccountClassificationService Codes (8000-9999):
✅ `8100` - Employee Costs (SALARY, WAGE, XG SALARIES)
✅ `8200` - Rent Expense (RENT)
✅ `8300` - Utilities (ELECTRICITY, WATER)
❌ `8310-001` - Mobile Phone Payments (TELEPHONE, CELL, MOBILE, MTN, VOD) **DOES NOT EXIST**
✅ `8400` - Communication (INTERNET, TELKOM, STATIONERY, PRINTING, OFFICE)
✅ `8500` - Motor Vehicle Expenses (CARTRACK, NETSTAR)
✅ `8600` - Travel & Entertainment (FUEL, PETROL, DIESEL, BP, SHELL, SASOL, ENGEN)
✅ `8800` - Insurance (INSURANCE, DOTSURE, MIWAY, KINGPRICE)
✅ `9500` - Interest Expense (BALANCE BROUGHT FORWARD)
✅ `9600` - Bank Charges (FEE, CHARGE, ADMIN FEE)

#### Rules Referencing ChartOfAccountsService Codes (4000-6999):
❌ `5000` - Other Income (INTEREST, EXCESS INTEREST) **CONFLICTS - In ChartOfAccountsService, 5000s are Cost of Sales!**
❌ `6000` - Reversals & Adjustments (RTD-NOT PROVIDED FOR, RTD-DEBIT AGAINST PAYERS AUTH) **CONFLICTS - In ChartOfAccountsService, 6000s are Operating Expenses!**

#### Rules Referencing Mixed Codes:
⚠️ `1100` - Current Assets (IB TRANSFER TO, IB TRANSFER FROM, IB INSTANT MONEY CASH TO)
  - AccountClassificationService: "Bank - Current Account"
  - ChartOfAccountsService: "Loans to Directors / Shareholders"

---

## 🔍 Detailed Analysis

### Issue 1: Account Code 8310-001 Does Not Exist

**Problem:**
```java
// TransactionMappingService.java - Line 334-345
createMappingRule(companyId, "TELEPHONE", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "CELL", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "MOBILE", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "MTN", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "VOD", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8310-001", "Mobile Phone Payments");
```

**AccountClassificationService defines:**
- `8300` - Utilities (parent)
- `8400` - Communication (correct parent for telephone/mobile)

**No sub-account 8310-001 exists!**

**Fix Required:**
```java
// Should be:
createMappingRule(companyId, "TELEPHONE", "8400", "Communication");
createMappingRule(companyId, "CELL", "8400", "Communication");
createMappingRule(companyId, "MOBILE", "8400", "Communication");
createMappingRule(companyId, "MTN", "8400", "Communication");
createMappingRule(companyId, "VOD", "8400", "Communication");
createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8400", "Communication");
```

---

### Issue 2: Account Code Conflicts (5000 and 6000 ranges)

#### Problem A: Code 5000
**TransactionMappingService says:**
```java
createMappingRule(companyId, "INTEREST", "5000", "Other Income");
```

**But in AccountClassificationService:**
- `5000` = Share Capital (Equity category)

**And in ChartOfAccountsService:**
- `5010` = Salaries - Security Officers (Cost of Sales)
- `5020` = Salaries - Operations Management
- `5030` = Labour Broker Fees
- etc.

**Fix Required:**
```java
// Should use AccountClassificationService code:
createMappingRule(companyId, "INTEREST", "7000", "Interest Income");
createMappingRule(companyId, "EXCESS INTEREST", "7000", "Interest Income");
```

#### Problem B: Code 6000
**TransactionMappingService says:**
```java
createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "6000", "Reversals & Adjustments");
```

**But in AccountClassificationService:**
- `6000` = Sales Revenue (Operating Revenue category)

**And in ChartOfAccountsService:**
- `6010` = Salaries - Administration & Directors
- `6020` = Accounting & Bank Fees
- `6030` = Insurance Expenses
- etc.

**Fix Required:**
Need to create a proper "Reversals & Adjustments" account or use an existing one like:
```java
// Option 1: Create new account in 7000 range (Other Income)
// 7300 - Reversals & Adjustments

// Option 2: Use existing account
createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "7200", "Gain on Asset Disposal");
```

---

### Issue 3: ChartOfAccountsService Mapping Rules Use Different Structure

**ChartOfAccountsService.setupDefaultMappingRules() uses:**

```java
// Revenue (4000 range - correct for ChartOfAccountsService)
"4000" - Security Services Revenue

// Cost of Sales (5000 range - conflicts with AccountClassificationService)
"5010" - Security Salaries
"5030" - Labour Broker Fees  
"5040" - Equipment Rental
"5050" - K9 Expenses

// Operating Expenses (6000 range - conflicts with AccountClassificationService)
"6010" - Admin Salaries
"6020" - Bank Fees
"6030" - Insurance
"6040" - Office Rent
"6050" - Telephone
"6060" - Pension
```

**This is CORRECT for the security business model** but **CONFLICTS with AccountClassificationService standard codes**.

---

## 🎯 Recommendations

### Option 1: Use AccountClassificationService as Single Source of Truth (RECOMMENDED)

**Rationale:**
- Standard South African accounting structure
- Comprehensive coverage (50+ accounts)
- Aligns with accounting best practices
- Already implements SARS-compliant structure

**Action Required:**
1. **Deprecate ChartOfAccountsService** or convert it to use 8000-9000 range codes
2. **Fix TransactionMappingService** to use only AccountClassificationService codes
3. **Update all mapping rules:**
   - `8310-001` → `8400` (Communication)
   - `5000` (Other Income) → `7000` (Interest Income)
   - `6000` (Reversals) → Create new account or use `7200`
   - Keep `8100`, `8200`, `8300`, `8400`, `8500`, `8600`, `8800`, `9500`, `9600` (already correct)

**Migration Path:**
```sql
-- Update existing transactions with old codes
UPDATE bank_transactions SET account_code = '8400' WHERE account_code = '8310-001';
UPDATE bank_transactions SET account_code = '7000' WHERE account_code = '5000' AND account_name = 'Other Income';
UPDATE bank_transactions SET account_code = '7200' WHERE account_code = '6000' AND account_name LIKE '%Reversal%';
```

---

### Option 2: Extend AccountClassificationService for Security Business (ALTERNATIVE)

**Rationale:**
- Keep security-specific accounts as sub-accounts
- Maintain standard structure as foundation
- Add industry-specific accounts in appropriate ranges

**Action Required:**
1. Keep AccountClassificationService as base
2. Add security-specific accounts in 8000-9000 range:
   ```java
   // Add to AccountClassificationService.getStandardAccountDefinitions()
   accounts.add(new AccountDefinition("8110", "Security Officers Salaries", "Salaries for security personnel", operatingExpensesId));
   accounts.add(new AccountDefinition("8120", "Operations Management Salaries", "Salaries for operations staff", operatingExpensesId));
   accounts.add(new AccountDefinition("8130", "Labour Broker Fees", "Fees for contract labour", operatingExpensesId));
   accounts.add(new AccountDefinition("8410", "K9 Unit Expenses", "Security dog unit costs", operatingExpensesId));
   accounts.add(new AccountDefinition("8420", "Equipment Rental", "Security equipment rental", operatingExpensesId));
   ```
3. Update ChartOfAccountsService to use extended codes
4. Fix TransactionMappingService to reference correct codes

---

### Option 3: Keep Separate Structures per Company Type (NOT RECOMMENDED)

**Rationale:**
- Allows different chart of accounts per business type
- More complex to maintain
- Harder to consolidate reporting

**Issues:**
- Need company type field in database
- Duplicate code/logic
- Mapping rules must be company-type aware
- Reports must handle multiple account structures

---

## ✅ Immediate Action Items

### Priority 1: Fix Critical Errors (URGENT)
1. **Fix account 8310-001 reference**
   - File: `TransactionMappingService.java` lines 334-345
   - Change: `8310-001` → `8400`
   - Impact: 6 mapping rules

2. **Fix account 5000 (Interest) conflict**
   - File: `TransactionMappingService.java` lines 316-319
   - Change: `5000` → `7000`
   - Impact: 2 mapping rules

3. **Fix account 6000 (Reversals) conflict**
   - File: `TransactionMappingService.java` lines 310-313
   - Change: `6000` → Create new account or use `7200`
   - Impact: 2 mapping rules

### Priority 2: Test Chart of Accounts Initialization
1. Run `./run.sh`
2. Initialize Chart of Accounts using AccountClassificationService
3. Verify all accounts created correctly
4. Check for conflicts with existing data

### Priority 3: Align Mapping Rules
1. Decide on Option 1 (recommended) or Option 2
2. Update TransactionMappingService.createStandardMappingRules()
3. Update ChartOfAccountsService.setupDefaultMappingRules()
4. Run build verification: `./gradlew clean build -x test`

### Priority 4: Data Migration
1. Query existing transactions with conflicting codes
2. Create migration script to update account codes
3. Test migration on copy of database
4. Execute migration with transaction rollback safety

---

## 📝 Code Change Summary

### File 1: TransactionMappingService.java

**Lines 334-345:** Change telephone/mobile mapping
```java
// OLD (WRONG - account doesn't exist):
createMappingRule(companyId, "TELEPHONE", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "CELL", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "MOBILE", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "MTN", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "VOD", "8310-001", "Mobile Phone Payments");
createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8310-001", "Mobile Phone Payments");

// NEW (CORRECT - uses existing Communication account):
createMappingRule(companyId, "TELEPHONE", "8400", "Communication");
createMappingRule(companyId, "CELL", "8400", "Communication");
createMappingRule(companyId, "MOBILE", "8400", "Communication");
createMappingRule(companyId, "MTN", "8400", "Communication");
createMappingRule(companyId, "VOD", "8400", "Communication");
createMappingRule(companyId, "PRE-PAID PAYMENT TO", "8400", "Communication");
```

**Lines 316-319:** Change interest mapping
```java
// OLD (WRONG - conflicts with Share Capital):
createMappingRule(companyId, "INTEREST", "5000", "Other Income");
createMappingRule(companyId, "EXCESS INTEREST", "5000", "Other Income");

// NEW (CORRECT - uses Interest Income account):
createMappingRule(companyId, "INTEREST", "7000", "Interest Income");
createMappingRule(companyId, "EXCESS INTEREST", "7000", "Interest Income");
```

**Lines 310-313:** Change reversals mapping
```java
// OLD (WRONG - conflicts with Sales Revenue):
createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "6000", "Reversals & Adjustments");
createMappingRule(companyId, "RTD-DEBIT AGAINST PAYERS AUTH", "6000", "Reversals & Adjustments");

// NEW (TEMPORARY - use asset disposal until proper account created):
createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "7200", "Gain on Asset Disposal");
createMappingRule(companyId, "RTD-DEBIT AGAINST PAYERS AUTH", "7200", "Gain on Asset Disposal");

// BETTER: Add new account in AccountClassificationService first:
// accounts.add(new AccountDefinition("7300", "Reversals & Adjustments", 
//     "Transaction reversals and adjustments", otherIncomeId));
// Then use:
// createMappingRule(companyId, "RTD-NOT PROVIDED FOR", "7300", "Reversals & Adjustments");
```

---

## 🔗 Related Documentation

- [DATA_MANAGEMENT_FLOW_ANALYSIS.md](./DATA_MANAGEMENT_FLOW_ANALYSIS.md) - Complete analysis of classification services
- [TRANSACTION_CLASSIFICATION_GUIDE.md](./TRANSACTION_CLASSIFICATION_GUIDE.md) - Classification workflow documentation
- [DATABASE_REFERENCE.md](./DATABASE_REFERENCE.md) - Database schema reference

---

## 📅 Next Steps

1. ✅ **Review this analysis** with Sthwalo
2. ⏳ **Decide on approach** (Option 1 recommended)
3. ⏳ **Fix critical mapping rule errors** (8310-001, 5000, 6000)
4. ⏳ **Test chart of accounts initialization**
5. ⏳ **Migrate existing transaction data**
6. ⏳ **Update documentation**
7. ⏳ **Run full integration test**

---

**Analysis Completed:** 2025-10-03  
**Status:** AWAITING DECISION & IMPLEMENTATION
