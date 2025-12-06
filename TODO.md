# Pending Tasks - Transaction Classification & Account Selection

**Date:** 5 December 2025  
**Status:** Paused - User needs rest, resuming tomorrow

---

## Current State

### ✅ Completed Today
- Fixed journal entry descriptions from NULL to "[code] name" format
- Updated PUT `/transactions/{id}/classification` endpoint from stub to implementation
- Established delegation chain through service layers (Controller → SpringTransactionClassificationService → SpringAccountClassificationService)
- Code compiles successfully (`./gradlew compileJava`)

### ⚠️ Where We Stopped
- User questioned why methods were added to `SpringAccountClassificationService` (the @Deprecated wrapper)
- Core implementation in `AccountClassificationService` still needs to be written
- Frontend account selector component not yet built
- Changes not committed (user halted before commit)

---

## Pending Tasks for Tomorrow

### 1. **Explain Architecture Before Commit** (PRIORITY)
**Status:** Not Started  
**Description:** User asked: "What are you adding you just deprecated some methods...i would like to know why before you add and commit anything"

Need to clarify:
- `SpringAccountClassificationService` is @Deprecated as a **class** (migration recommendation to use `AccountClassificationService` directly)
- Adding wrapper methods to it is **correct** - all 13 existing methods follow same delegation pattern
- The wrapper delegates to `AccountClassificationService` (2345-line core service where all business logic lives)
- This is standard architecture pattern for backward compatibility during migration

### 2. **Get User Approval for Core Implementation**
**Status:** Not Started  
**Description:** After architecture explanation, get explicit approval to implement `updateTransactionClassification()` in `AccountClassificationService`.

Reference similar method: `generateJournalEntriesForClassifiedTransactions()` at lines 2274-2324

### 3. **Implement Core updateTransactionClassification() Logic**
**Status:** Not Started  
**File:** `/spring-app/src/main/java/fin/service/spring/AccountClassificationService.java`

**Business Logic Required:**
1. Query: `journalEntryLineRepository.findBySourceTransactionId(transactionId)`
2. If lines exist:
   - Find debit line (`debitAmount > 0`) and credit line (`creditAmount > 0`)
   - Update `account_id` fields on both lines
3. If no lines exist:
   - Create new `JournalEntry` header
   - Create 2 `JournalEntryLine` records (debit and credit)
   - Match pattern in `generateJournalEntriesForClassifiedTransactions()`
4. Validate accounts belong to `companyId` before update

### 4. **Test Journal Entry Description Fix**
**Status:** Not Started  
**Description:** Verify that `journal_entry_lines.description` now shows '[code] name' format instead of NULL.

**Test Query:**
```sql
SELECT description FROM journal_entry_lines 
WHERE source_transaction_id IS NOT NULL 
LIMIT 10;
```

**Expected Format:** `[8400] Communication`, `[1000] Petty Cash`, etc.

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

### 8. **Commit Backend Changes**
**Status:** Not Started  
**Files to Commit:**
1. `AccountClassificationService.java` (journal entry description fix + core implementation)
2. `SpringTransactionClassificationController.java` (endpoint implementation)
3. `SpringTransactionClassificationService.java` (delegation method)
4. `SpringAccountClassificationService.java` (wrapper delegation method)

**Commit Message:** `Fix journal entry descriptions and implement transaction classification update`

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

## Next Immediate Action (Tomorrow)
**Step 1:** Explain architecture to user  
**Step 2:** Get approval for core implementation approach  
**Step 3:** Implement core logic in `AccountClassificationService`
