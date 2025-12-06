# Pending Tasks - Transaction Classification & Account Selection

**Date:** 6 December 2025  
**Status:** Backend Complete - Frontend Implementation Next

---

## Current State

### ✅ Completed (Backend)
- Fixed journal entry descriptions from NULL to "[code] name" format
- Implemented full `updateTransactionClassification()` in `AccountClassificationService` (157 lines)
  - Validates transaction and accounts belong to company
  - Updates existing journal entries or creates new ones
  - Maintains double-entry accounting with debit/credit lines
- Established delegation chain through all service layers
- Updated PUT `/api/v1/companies/{companyId}/classification/transactions/{transactionId}` endpoint
- Code compiles successfully (`./gradlew compileJava`)
- API testing completed via curl - verified create and update scenarios
- Database verification confirmed descriptions show "[code] name" format
- All changes committed (commit hash: 6c5945a)
- Enhanced collaboration protocol documented in copilot-instructions.md

### ⚠️ Current Focus
- Frontend account selector component (not started)
- Frontend integration in DataManagementView (not started)

---

## Pending Tasks

### 1. ~~Explain Architecture Before Commit~~ ✅
**Status:** Completed  
**Description:** Clarified SpringAccountClassificationService wrapper pattern to user. User confirmed understanding after explanation.

### 2. ~~Get User Approval for Core Implementation~~ ✅
**Status:** Completed  
**Description:** User approved phased implementation approach (Core Logic → Compilation → Testing).

### 3. ~~Implement Core updateTransactionClassification() Logic~~ ✅
**Status:** Completed  
**File:** `/spring-app/src/main/java/fin/service/spring/AccountClassificationService.java`

**Implementation Details:**
- Lines 2336-2485: Main method `updateTransactionClassification()` (150 lines)
- Lines 2409-2438: Helper method `updateExistingJournalLines()`
- Lines 2443-2485: Helper method `createNewJournalEntry()`
- Validates transaction and accounts belong to company
- Updates existing journal entries or creates new ones
- Maintains double-entry accounting (debit + credit lines)
- Updates transaction.account_code on successful classification

### 4. ~~Test Journal Entry Description Fix~~ ✅
**Status:** Completed  
**Test Results:**
- Transaction 17570 tested (create and update scenarios)
- Descriptions show correct format: "[1100] Bank - Current Account", "[8400] Communication", "[6100-001] Corobrik Service Revenue"
- Database queries confirmed account_id updates work correctly
- Same journal_entry_line IDs (136852, 136853) prove update vs create

**API Endpoint Tested:**
```bash
curl -X PUT "http://localhost:8080/api/v1/companies/1/classification/transactions/17570" \
  -H "Content-Type: application/json" \
  -d '{"debitAccountId": 1340, "creditAccountId": 1330}'
```

### 5. **Build Frontend AccountSelector Component**
**Status:** Not Started  
**File:** `/frontend/src/components/AccountSelector.tsx`

**Requirements:**
- Match legacy console app pattern from `DataManagementController.selectAccount()` (lines 1034-1073)
- Display numbered list: "1. [1000] Petty Cash", "2. [8400] Communication", etc.
- Features: searchable/filterable by code or name
- Returns selected account ID
- Props interface:
  ```typescript
  {
    companyId: number;
    onSelect: (accountId: number) => void;
    label?: string;
  }
  ```

### 6. **Integrate AccountSelector in DataManagementView**
**Status:** Not Started  
**Description:** Replace text input placeholders in frontend DataManagementView edit mode.

**Changes:**
- Replace "Select debit account..." text input with `AccountSelector` dropdown
- Replace "Select credit account..." text input with `AccountSelector` dropdown
- Wire up `onSelect` callbacks to update transaction classification state
- Trigger PUT `/transactions/{id}/classification` API call when both accounts selected

### 7. **End-to-End Testing**
**Status:** Not Started  
**Test Flow:**
1. Manual transaction edit in DataManagementView
2. Select debit account from dropdown
3. Select credit account from dropdown
4. Verify PUT `/transactions/{id}/classification` succeeds
5. Query `journal_entry_lines` to confirm `account_id` fields updated correctly
6. Verify descriptions show '[code] name' format

### 8. ~~Commit Backend Changes~~ ✅
**Status:** Completed (commit hash: 6c5945a)  
**Files Committed:**
1. `AccountClassificationService.java` (journal entry description fix + core implementation)
2. `SpringTransactionClassificationController.java` (endpoint implementation)
3. `SpringTransactionClassificationService.java` (delegation method)
4. `SpringAccountClassificationService.java` (wrapper delegation method)
5. `.github/copilot-instructions.md` (collaboration protocol enhancement)
6. `TODO.md` (this file - task tracking)

**Commit Stats:** 6 files changed, 396 insertions(+), 10 deletions(-)

### 9. **Resolve StandardBankTabularParserTest Failures** (Optional/Future)
**Status:** Not Started  
**Description:** 19 test failures in `StandardBankTabularParserTest` (unrelated to classification changes).

Tests failing: `shouldParseDebitTransaction`, `shouldParseCreditTransaction`, `shouldClassifyServiceFees`, etc.

Build succeeds but tests fail. Should be investigated separately.

---

## Technical Context

### Service Architecture (Three-Layer Delegation)
```
SpringTransactionClassificationController (REST endpoint)
    ↓
SpringTransactionClassificationService (orchestration, 648 lines)
    ↓
SpringAccountClassificationService (@Deprecated wrapper, 143 lines)
    ↓
AccountClassificationService (core business logic, 2345 lines)
```

### Legacy Console App Pattern
**File:** `/app/src/main/java/fin/controller/DataManagementController.java`  
**Method:** `selectAccount()` (lines 1034-1073)

**Pattern:**
```java
System.out.printf("%d. [%s] %s%n", i+1, accountCode, accountName);
int selection = inputHandler.getInteger("Select account number", 1, accounts.size());
return accounts.get(selection - 1).getId();
```

### Journal Entry Description Format
**Changed from:** `transaction.getReference() + " - " + transaction.getCategory()`  
**Changed to:** `"[" + account.getAccountCode() + "] " + account.getAccountName()`

### Method Signature
```java
void updateTransactionClassification(
    Long companyId, 
    Long transactionId, 
    Long debitAccountId, 
    Long creditAccountId
)
```

---

## Build Status
- ✅ `./gradlew compileJava` - SUCCESS
- ❌ `./gradlew build` - 19 test failures (unrelated to current work)

---

## Next Immediate Actions
**Priority 1:** Build `AccountSelector.tsx` component with numbered list display  
**Priority 2:** Integrate `AccountSelector` in `DataManagementView` to replace text input placeholders  
**Priority 3:** Wire up API calls and test end-to-end flow  

**Backend Status:** ✅ Complete and committed  
**Frontend Status:** 🚧 Not started - awaiting user approval to proceed
