# Phase 2: Account Code Migration Plan

**Date**: 3 October 2025  
**Status**: IN PROGRESS  
**Objective**: Complete migration from custom codes to SARS-compliant codes

---

## 🎯 What Phase 1 Accomplished

✅ Service consolidation (3 → 1)  
✅ Fixed `getCategoryIdForAccountCode()` to use correct category IDs (7-16)  
✅ Build compiles successfully  
✅ System no longer references non-existent category_id=18

---

## 🔧 What Phase 2 Must Do

### Account Code Migrations Needed in `TransactionMappingService.java`

| Old Code | Old Name | New Code | New Name | Lines |
|----------|----------|----------|----------|-------|
| `5000-001` through `5000-007` | Interest Income variations | `7000` | Interest Income | 643-716 |
| `6000-001` through `6000-008` | Reversals & Adjustments | `7200` | Adjustments/Refunds | 668-706 |
| `8310-001` | Telephone & Internet | `8400` | Communication Costs | ~950 |
| `8100-XXX` | Salary - Employee Name | `8100` | Employee Costs (main) | 795-805 |
| `8100-999` | Salary - Unspecified | `8100` | Employee Costs (main) | 805, 812 |
| `8110-001` | Pension FAW Fund | `8100` | Employee Costs | 833 |
| `8110-999` | Pension Other | `8100` | Employee Costs | 835 |
| `3100-001` | PAYE Payments | `2100` | Current Liabilities - Tax | 841 |
| `3100-002` | VAT Payments | `2100` | Current Liabilities - Tax | 843 |
| `3100-999` | Other Tax | `2100` | Current Liabilities - Tax | 845 |
| `8200-XXX` | Directors' Remuneration | `8200` | Directors' Costs | 970-980 |
| `8400-XXX` | Communication - Vendor | `8400` | Communication Costs | 950-960 |
| `8900-XXX` | Other Expenses - Vendor | `8900` | Other Operating Expenses | 890-920 |
| `3000-XXX` | Trade Payables - Vendor | `2000` | Current Liabilities - Trade Payables | 995-1015 |

### Strategy: Use Standard Accounts Instead of Dynamic Creation

**OLD APPROACH** (causing problems):
```java
String employeeName = extractEmployeeName(details);
String accountCode = "8100-" + generateEmployeeCode(employeeName);
String accountName = "Salary - " + employeeName;
return getOrCreateDetailedAccount(accountCode, accountName, "8100", "Employee Costs");
```

**NEW APPROACH** (SARS-compliant):
```java
// Use standard account 8100 for ALL employee costs
// Store employee name in transaction details/notes, not in account code
return getStandardAccountId("8100"); // Returns ID for "Employee Costs"
```

### Benefits of New Approach

1. ✅ **SARS Compliant**: Uses standard account codes only
2. ✅ **No Dynamic Accounts**: Eliminates foreign key violations
3. ✅ **Simpler**: One account per expense type, not hundreds
4. ✅ **Reporting**: Employee details available in transaction descriptions
5. ✅ **Maintainable**: No custom account management needed

---

## 📋 Implementation Steps

### Step 1: Create `getStandardAccountId()` Helper Method ✅ DONE

Added method to look up standard accounts by code without creating new ones.

### Step 2: Update Interest Income References (Lines 640-720) ⏳ NEXT

Change all `5000-XXX` variations to use single `7000` (Interest Income).

### Step 3: Update Reversals/Adjustments (Lines 665-710) ⏳ PENDING

Change all `6000-XXX` variations to use single `7200` (Other Income - Adjustments).

### Step 4: Update Employee Costs (Lines 795-850) ⏳ PENDING

Remove dynamic employee-specific accounts, use standard `8100`.

### Step 5: Update Tax Liabilities (Lines 838-848) ⏳ PENDING

Change `3100-XXX` to use `2100` (Current Liabilities).

### Step 6: Update Communication Costs (Lines 945-965) ⏳ PENDING

Change `8310-001` and `8400-XXX` to standard `8400`.

### Step 7: Update Trade Payables (Lines 995-1020) ⏳ PENDING

Change `3000-XXX` vendor-specific to standard `2000`.

### Step 8: Update Other Expenses (Lines 885-925) ⏳ PENDING

Change `8900-XXX` to standard `8900`.

### Step 9: Test Auto-Classification ⏳ PENDING

Run full integration test to verify all 37 unclassified transactions can be processed.

### Step 10: Update Documentation ⏳ PENDING

Update Phase 1 docs to include Phase 2 completion.

---

## 🎯 Success Criteria

- [ ] All 37 unclassified transactions can be auto-classified
- [ ] Zero foreign key constraint violations
- [ ] System uses only standard SARS account codes (1000-9999, no suffixes)
- [ ] Build compiles successfully
- [ ] Integration tests pass
- [ ] Documentation updated

---

## ⏱️ Estimated Time

- Step 1: ✅ 15 minutes (DONE)
- Steps 2-8: ~2 hours (systematic code updates)
- Step 9: ~30 minutes (integration testing)
- Step 10: ~30 minutes (documentation)

**Total**: ~3 hours to complete Phase 2

---

## 📞 Next Actions

1. Get approval to proceed with Phase 2
2. Execute Steps 2-10 systematically
3. Test after each major change
4. Document results

