-- Migration: Add source_transaction_id to journal_entry_lines
-- Date: 2025-10-04
-- Purpose: Link journal entries back to source bank transactions for audit trail
-- 
-- This column is CRITICAL for:
-- 1. Audit Trail generation (shows transaction → journal entry flow)
-- 2. Preventing duplicate journal entries
-- 3. Transaction history tracking
-- 4. Compliance reporting

-- Step 1: Add the column (nullable first, in case existing data)
ALTER TABLE journal_entry_lines 
ADD COLUMN IF NOT EXISTS source_transaction_id INTEGER;

-- Step 2: Add index for performance
CREATE INDEX IF NOT EXISTS idx_journal_entry_lines_source_transaction 
ON journal_entry_lines(source_transaction_id);

-- Step 3: Add foreign key constraint
ALTER TABLE journal_entry_lines
ADD CONSTRAINT journal_entry_lines_source_transaction_id_fkey 
FOREIGN KEY (source_transaction_id) REFERENCES bank_transactions(id)
ON DELETE SET NULL;  -- If transaction deleted, keep journal entry but clear link

-- Step 4: Verify the change
\d journal_entry_lines

-- Step 5: Show statistics
SELECT 
    COUNT(*) as total_journal_lines,
    COUNT(source_transaction_id) as lines_with_source,
    COUNT(*) - COUNT(source_transaction_id) as lines_without_source
FROM journal_entry_lines;

-- Expected output:
-- - Column added successfully
-- - Index created
-- - Foreign key constraint added
-- - Statistics show current state

COMMENT ON COLUMN journal_entry_lines.source_transaction_id IS 
'Links journal entry line to the bank transaction that generated it. Used for audit trail and preventing duplicates.';
