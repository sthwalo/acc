# TASK 2.1 - Fix Depreciation Posting Logic

## Problem Statement
The current depreciation posting system creates redundant correction entries and incorrect journal postings. The depreciation schedule already contains all calculated values, but the posting logic attempts to "catch up" with bulk corrections instead of posting current year depreciation only.

## Current Issues
1. **Redundant Correction Entries**: `DEPR-CORR-YRS1-4` posts cumulative depreciation when schedule already has calculations
2. **Incorrect Asset Acquisition**: Shows gross cost instead of net book value
3. **Bulk Corrections**: System tries to catch up missing years instead of posting current year only
4. **Future Year Posting**: Posts Year 5 depreciation before Year 4 is complete

## Required Solution

### Core Logic Changes
1. **Depreciation Schedule as Source of Truth**: All calculations already exist in the schedule
2. **Current Year Only Posting**: Post only the annual depreciation for current fiscal period
3. **Net Book Value in Acquisition**: Asset acquisition shows cost minus accumulated depreciation
4. **No Correction Entries**: Remove all bulk correction logic

### Target Journal Entries

**Asset Acquisition (ASSET-001):**
```
Debit:  Office Equipment          R23,000.00
Credit: Accumulated Depreciation  R2,916.44  (cumulative up to current year)
Credit: Book Value               R20,083.56  (net book value)
```

**Current Year Depreciation (DEPR-XX-4):**
```
Debit:  Depreciation Expense      R692.47     (Year 4 annual depreciation only)
Credit: Accumulated Depreciation R692.47
```

**Remove:** `DEPR-CORR-YRS1-4` correction entries entirely

## Implementation Plan

### Phase 1: Remove Correction Logic
**File: `DepreciationService.java`**
- Remove `syncDepreciationJournalEntries()` bulk correction functionality
- Remove `cleanupIncorrectDepreciationEntries()` correction cleanup
- Remove all correction-related journal entry creation

### Phase 2: Fix Asset Acquisition Posting
**File: `DepreciationService.java`**
- Modify asset acquisition journal entry creation
- Include accumulated depreciation credit line
- Include net book value credit line
- Use cumulative depreciation from schedule up to current year

### Phase 3: Simplify Depreciation Posting
**File: `DepreciationService.java`**
- Modify `postDepreciationToJournal()` to post only current year's annual depreciation
- Remove fiscal period overlap logic
- Use depreciation year directly from schedule
- No bulk posting of multiple years

### Phase 4: Update Posting Detection
**File: `DepreciationService.java`**
- Update `isDepreciationYearPosted()` to check for current year only
- Remove cumulative posting logic
- Ensure only one year's depreciation is posted per fiscal period

### Phase 5: Clean Existing Data
**Database Cleanup:**
- Remove existing correction journal entries (`DEPR-CORR-*`)
- Remove future year depreciation entries
- Reset depreciation schedule posting status
- Clean journal entry references

## Files to Modify

### Primary Files
1. **`DepreciationService.java`** - Major refactoring of posting logic
   - Remove correction methods
   - Simplify posting to current year only
   - Fix asset acquisition entries

### Secondary Files
2. **`DepreciationRepository.java`** - Minor updates
   - Remove correction-related database operations
   - Update posting status tracking

3. **Database Schema** - No changes required
   - Existing tables sufficient
   - Journal entries will be cleaner

## Implementation Steps

### Step 1: Backup Current State
```bash
# Create backup of current depreciation data
pg_dump -h localhost -U sthwalonyoni drimacc_db --table=depreciation_schedules --table=depreciation_entries --table=journal_entries --table=journal_entry_lines > depreciation_backup_$(date +%Y%m%d_%H%M%S).sql
```

### Step 2: Remove Correction Logic
- Delete `syncDepreciationJournalEntries()` method
- Delete `cleanupIncorrectDepreciationEntries()` method
- Remove correction-related constants and variables

### Step 3: Refactor Posting Logic
```java
// Old logic: Complex fiscal period overlap detection
// New logic: Simple current year posting
private void postDepreciationToJournal(DepreciationSchedule schedule, Long companyId) {
    // Find current year's depreciation from schedule
    DepreciationYear currentYear = findCurrentFiscalYearDepreciation(schedule);
    if (currentYear == null || isDepreciationYearPosted(schedule.getId(), currentYear.getYear())) {
        return;
    }
    // Post only this year's annual depreciation
    createJournalEntryForDepreciationYear(schedule, currentYear, ...);
}
```

### Step 4: Fix Asset Acquisition
```java
// New asset acquisition entry with net book value
private void createAssetAcquisitionEntry(Asset asset, DepreciationSchedule schedule) {
    BigDecimal cumulativeDepreciation = getCumulativeDepreciationUpToCurrentYear(schedule);
    BigDecimal netBookValue = asset.getCost().subtract(cumulativeDepreciation);
    
    // Debit: Asset cost
    // Credit: Accumulated depreciation (cumulative)
    // Credit: Net book value
}
```

### Step 5: Update Posting Detection
- Change `isDepreciationYearPosted()` to check specific year only
- Remove bulk posting checks
- Ensure clean single-year posting

### Step 6: Database Cleanup
```sql
-- Remove correction entries
DELETE FROM journal_entry_lines WHERE journal_entry_id IN (
    SELECT id FROM journal_entries WHERE reference LIKE 'DEPR-CORR-%'
);
DELETE FROM journal_entries WHERE reference LIKE 'DEPR-CORR-%';

-- Reset posting status
UPDATE depreciation_schedules SET status = 'CALCULATED' WHERE status = 'POSTED';
UPDATE depreciation_entries SET journal_entry_line_id = NULL, status = NULL;
```

## Testing Plan

### Unit Tests
- Update `DepreciationServiceTest.java` for new posting logic
- Test single-year posting only
- Test net book value calculation
- Verify no correction entries created

### Integration Tests
- Test full depreciation workflow
- Verify journal entries are correct
- Check audit trail cleanliness
- Validate balance sheet impact

### Manual Testing
1. Create new asset
2. Calculate depreciation schedule
3. Verify asset acquisition shows net book value
4. Post depreciation for current year only
5. Check no correction entries appear
6. Verify audit trail shows clean entries

## Success Criteria

### Functional
- ✅ Asset acquisition shows net book value
- ✅ Only current year depreciation posted
- ✅ No correction entries in audit trail
- ✅ Depreciation schedule remains source of truth
- ✅ Journal entries are clean and minimal

### Technical
- ✅ All existing tests pass
- ✅ No redundant database operations
- ✅ Posting logic is simplified
- ✅ Code is maintainable and clear

### Business
- ✅ IAS 16 compliance maintained
- ✅ Audit trail is clean and understandable
- ✅ Financial statements show correct values
- ✅ No double-counting of depreciation

## Risk Assessment

### Low Risk
- Removing correction logic (simplifies system)
- Using existing schedule calculations (already tested)

### Medium Risk
- Asset acquisition entry changes (affects balance sheet)
- Posting logic changes (affects P&L)

### Mitigation
- Comprehensive testing before deployment
- Database backup before changes
- Gradual rollout with rollback plan

## Timeline
- **Phase 1**: Remove correction logic (2 hours)
- **Phase 2**: Fix asset acquisition (2 hours)
- **Phase 3**: Simplify depreciation posting (3 hours)
- **Phase 4**: Update detection logic (1 hour)
- **Phase 5**: Database cleanup (1 hour)
- **Testing**: (4 hours)
- **Total**: 13 hours

## Dependencies
- Requires TASK 2.0 completion (fiscal year calculations)
- Database backup before implementation
- Test environment validation

## Rollback Plan
1. Restore database from backup
2. Revert code changes
3. Re-run depreciation calculations
4. Verify system returns to working state</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/docs/development/tasks/TASK_2.1_Depreciation_Posting_Fix.md