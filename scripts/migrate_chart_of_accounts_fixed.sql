-- =========================================
-- Chart of Accounts Migration Script (FIXED)
-- Date: 2025-10-03
-- Purpose: Update bank_transactions with correct account codes
-- 
-- Changes:
--   8310-001 → 8400 (Telephone/Mobile to Communication)
--   5000     → 7000 (Share Capital to Interest Income - where details indicate interest)
--   6000     → 7200 (Sales Revenue to Gain on Asset Disposal - where details indicate reversals)
--
-- NOTE: transaction_mapping_rules table uses account_id (FK), not account_code
--       No migration needed for that table - it references accounts by ID
-- =========================================

BEGIN;

\echo '========================================='
\echo 'STEP 1: Current State Analysis'
\echo '========================================='

\echo 'Transactions with conflicting account codes:'
SELECT 
    account_code,
    account_name,
    COUNT(*) as transaction_count
FROM bank_transactions
WHERE account_code IN ('8310-001', '5000', '6000')
GROUP BY account_code, account_name
ORDER BY account_code;

\echo ''
\echo '========================================='
\echo 'STEP 2: Creating Backup Table'
\echo '========================================='

-- Create backup table for bank_transactions
CREATE TABLE IF NOT EXISTS bank_transactions_backup_20251003 AS
SELECT * FROM bank_transactions
WHERE account_code IN ('8310-001', '5000', '6000');

\echo 'Backup created:'
SELECT COUNT(*) as transactions_backed_up FROM bank_transactions_backup_20251003;

-- Step 3: Update bank_transactions table
\echo ''
\echo '========================================='
\echo 'STEP 3: Migrating bank_transactions'
\echo '========================================='

-- Fix 8310-001 → 8400 (Communication)
UPDATE bank_transactions
SET 
    account_code = '8400',
    account_name = 'Communication'
WHERE account_code = '8310-001';

\echo 'Updated telephone/mobile transactions (8310-001 → 8400):'
SELECT COUNT(*) FROM bank_transactions WHERE account_code = '8400';

-- Fix 5000 → 7000 (Interest Income)
-- Only update where details clearly indicate interest
UPDATE bank_transactions
SET 
    account_code = '7000',
    account_name = 'Interest Income'
WHERE account_code = '5000'
AND (
    LOWER(details) LIKE '%interest%'
    OR LOWER(details) LIKE '%excess interest%'
);

\echo 'Updated interest income transactions (5000 → 7000):'
SELECT COUNT(*) FROM bank_transactions WHERE account_code = '7000';

-- Fix 6000 → 7200 (Gain on Asset Disposal - temporary for reversals)
-- Only update where details clearly indicate reversals
UPDATE bank_transactions
SET 
    account_code = '7200',
    account_name = 'Gain on Asset Disposal'
WHERE account_code = '6000'
AND (
    LOWER(details) LIKE '%rtd-%'
    OR LOWER(details) LIKE '%reversal%'
    OR LOWER(details) LIKE '%not provided for%'
    OR LOWER(details) LIKE '%debit against payers%'
);

\echo 'Updated reversal/adjustment transactions (6000 → 7200):'
SELECT COUNT(*) FROM bank_transactions WHERE account_code = '7200';

\echo ''
\echo '========================================='
\echo 'STEP 4: Verification'
\echo '========================================='

\echo 'Remaining transactions with old codes (should be 0):'
SELECT 
    account_code,
    COUNT(*) as remaining_count
FROM bank_transactions
WHERE account_code IN ('8310-001', '5000', '6000')
GROUP BY account_code;

\echo ''
\echo 'Summary of migrated transactions by new code:'
SELECT 
    account_code,
    account_name,
    COUNT(*) as transaction_count
FROM bank_transactions
WHERE account_code IN ('8400', '7000', '7200')
GROUP BY account_code, account_name
ORDER BY account_code;

\echo ''
\echo '========================================='
\echo 'STEP 5: Rollback Instructions'
\echo '========================================='
\echo 'To rollback this migration, run:'
\echo 'BEGIN;'
\echo 'DELETE FROM bank_transactions WHERE id IN (SELECT id FROM bank_transactions_backup_20251003);'
\echo 'INSERT INTO bank_transactions SELECT * FROM bank_transactions_backup_20251003;'
\echo 'COMMIT;'

COMMIT;

\echo ''
\echo '========================================='
\echo 'MIGRATION COMPLETED SUCCESSFULLY!'
\echo '========================================='
\echo 'Backup table created:'
\echo '  - bank_transactions_backup_20251003'
\echo ''
\echo 'Next steps:'
\echo '1. Run ./run.sh and test chart of accounts initialization'
\echo '2. Test transaction classification workflows'
\echo '3. Verify reports generate correctly'
\echo '4. If all works, drop backup table after 30 days:'
\echo '   DROP TABLE bank_transactions_backup_20251003;'
\echo '========================================='
