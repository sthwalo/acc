-- Database Migration: Drop match_value Column from transaction_mapping_rules
-- Date: October 12, 2025
-- Purpose: Complete schema cleanup after consolidating to pattern_text column
-- Prerequisite: All services updated to use pattern_text column only

-- =====================================================================
-- STEP 1: BACKUP VERIFICATION
-- Verify both columns contain identical data before dropping match_value
-- =====================================================================

-- Check data synchronization (should return only 'IDENTICAL')
SELECT 
    CASE 
        WHEN match_value = pattern_text THEN 'IDENTICAL'
        WHEN match_value IS NULL AND pattern_text IS NULL THEN 'BOTH_NULL'
        WHEN match_value IS NULL OR pattern_text IS NULL THEN 'NULL_MISMATCH'
        ELSE 'DIFFERENT'
    END as comparison_status,
    COUNT(*) as count
FROM transaction_mapping_rules 
GROUP BY 1;

-- Expected result: Only 'IDENTICAL' with count = 150
-- If you see any other status, DO NOT PROCEED with migration

-- =====================================================================
-- STEP 2: VERIFY APPLICATION COMPATIBILITY  
-- =====================================================================

-- Before running this migration, ensure:
-- 1. ✅ TransactionMappingRuleService updated to use pattern_text
-- 2. ✅ TransactionProcessingService fallback removed
-- 3. ✅ Application compiles successfully
-- 4. ✅ No services reference match_value column

-- =====================================================================
-- STEP 3: PERFORM MIGRATION
-- =====================================================================

-- Begin transaction for safety
BEGIN;

-- Step 3.1: Double-check no data loss will occur
DO $$
DECLARE
    mismatch_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO mismatch_count
    FROM transaction_mapping_rules 
    WHERE match_value != pattern_text OR 
          (match_value IS NULL AND pattern_text IS NOT NULL) OR
          (match_value IS NOT NULL AND pattern_text IS NULL);
          
    IF mismatch_count > 0 THEN
        RAISE EXCEPTION 'Data mismatch detected! Found % rows with different values. Aborting migration.', mismatch_count;
    END IF;
    
    RAISE NOTICE 'Data verification passed. All % rows have identical match_value and pattern_text.', 
        (SELECT COUNT(*) FROM transaction_mapping_rules);
END $$;

-- Step 3.2: Drop the match_value column
ALTER TABLE transaction_mapping_rules DROP COLUMN match_value;

-- Step 3.3: Verify the drop was successful
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'transaction_mapping_rules' 
  AND column_name IN ('match_value', 'pattern_text')
ORDER BY column_name;

-- Expected result: Only pattern_text should remain

-- Step 3.4: Verify data integrity after migration
SELECT COUNT(*) as total_rules, 
       COUNT(pattern_text) as non_null_patterns,
       COUNT(pattern_text) - COUNT(*) as null_pattern_count
FROM transaction_mapping_rules;

-- Expected: total_rules = 150, non_null_patterns = 150, null_pattern_count = 0

-- Step 3.5: Test query that services will use
SELECT pattern_text, account_code, account_name 
FROM transaction_mapping_rules 
WHERE company_id = 2 AND active = true
ORDER BY priority DESC, id
LIMIT 5;

-- Should return valid data without errors

-- Commit the migration
COMMIT;

-- =====================================================================
-- STEP 4: POST-MIGRATION VERIFICATION
-- =====================================================================

-- Verify column is completely removed
SELECT column_name 
FROM information_schema.columns 
WHERE table_name = 'transaction_mapping_rules' 
  AND column_name = 'match_value';

-- Expected result: 0 rows (column no longer exists)

-- Verify application functionality
-- After running this migration:
-- 1. Test transaction classification via application
-- 2. Verify mapping rules load correctly
-- 3. Confirm no errors in application logs

-- =====================================================================
-- ROLLBACK PLAN (if needed)
-- =====================================================================

/*
-- If rollback is needed, re-add the column and copy data:

BEGIN;

-- Add match_value column back
ALTER TABLE transaction_mapping_rules 
ADD COLUMN match_value VARCHAR(500);

-- Copy data from pattern_text to match_value
UPDATE transaction_mapping_rules 
SET match_value = pattern_text;

-- Make it NOT NULL if original schema required it
ALTER TABLE transaction_mapping_rules 
ALTER COLUMN match_value SET NOT NULL;

COMMIT;

-- Note: This rollback will work only if pattern_text data is still intact
*/

-- =====================================================================
-- MIGRATION LOG
-- =====================================================================

-- Record this migration in your deployment log:
-- Date: October 12, 2025
-- Action: Dropped match_value column from transaction_mapping_rules
-- Reason: Schema cleanup after service consolidation
-- Services Updated: TransactionMappingRuleService, TransactionProcessingService
-- Data Loss: None (both columns contained identical data)
-- Rollback: Available via ADD COLUMN + UPDATE (see above)

VACUUM ANALYZE transaction_mapping_rules;

SELECT 'Migration completed successfully! match_value column dropped.' as result;