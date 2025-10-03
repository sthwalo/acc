-- =====================================================================
-- Migration Script: Standardize Chart of Accounts
-- =====================================================================
-- Purpose: Migrate from ChartOfAccountsService structure to 
--          AccountClassificationService (SARS-compliant) structure
-- Date: 2025-10-03
-- Author: AI Assistant + Sthwalo Nyoni
-- =====================================================================

BEGIN;

-- Step 1: Display current state
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

\echo 'Mapping rules with conflicting account codes:'
SELECT 
    id,
    pattern_text,
    account_code,
    account_name
FROM transaction_mapping_rules
WHERE account_code IN ('8310-001', '5000', '6000')
ORDER BY account_code;

-- Step 2: Create backup table
\echo ''
\echo '========================================='
\echo 'STEP 2: Creating Backup Tables'
\echo '========================================='

CREATE TABLE IF NOT EXISTS bank_transactions_backup_20251003 AS
SELECT * FROM bank_transactions
WHERE account_code IN ('8310-001', '5000', '6000');

CREATE TABLE IF NOT EXISTS transaction_mapping_rules_backup_20251003 AS
SELECT * FROM transaction_mapping_rules
WHERE account_code IN ('8310-001', '5000', '6000');

SELECT 
    (SELECT COUNT(*) FROM bank_transactions_backup_20251003) as transactions_backed_up,
    (SELECT COUNT(*) FROM transaction_mapping_rules_backup_20251003) as rules_backed_up;

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
-- Only update if account_name indicates it's interest/income
UPDATE bank_transactions
SET 
    account_code = '7000',
    account_name = 'Interest Income'
WHERE account_code = '5000'
  AND (account_name ILIKE '%interest%' OR account_name ILIKE '%income%');

\echo 'Updated interest income transactions (5000 → 7000):'
SELECT COUNT(*) FROM bank_transactions WHERE account_code = '7000';

-- Fix 6000 → 7200 (Gain on Asset Disposal / Adjustments)
-- Only update if account_name indicates reversals/adjustments
UPDATE bank_transactions
SET 
    account_code = '7200',
    account_name = 'Gain on Asset Disposal'
WHERE account_code = '6000'
  AND (account_name ILIKE '%reversal%' OR account_name ILIKE '%adjustment%');

\echo 'Updated reversal/adjustment transactions (6000 → 7200):'
SELECT COUNT(*) FROM bank_transactions WHERE account_code = '7200';

-- Step 4: Update transaction_mapping_rules table
\echo ''
\echo '========================================='
\echo 'STEP 4: Migrating transaction_mapping_rules'
\echo '========================================='

-- Fix 8310-001 → 8400 (Communication)
UPDATE transaction_mapping_rules
SET 
    account_code = '8400',
    account_name = 'Communication'
WHERE account_code = '8310-001';

\echo 'Updated telephone/mobile mapping rules (8310-001 → 8400):'
SELECT COUNT(*) FROM transaction_mapping_rules WHERE account_code = '8400';

-- Fix 5000 → 7000 (Interest Income)
UPDATE transaction_mapping_rules
SET 
    account_code = '7000',
    account_name = 'Interest Income'
WHERE account_code = '5000';

\echo 'Updated interest mapping rules (5000 → 7000):'
SELECT COUNT(*) FROM transaction_mapping_rules WHERE account_code = '7000';

-- Fix 6000 → 7200 (Gain on Asset Disposal)
UPDATE transaction_mapping_rules
SET 
    account_code = '7200',
    account_name = 'Gain on Asset Disposal'
WHERE account_code = '6000';

\echo 'Updated reversal mapping rules (6000 → 7200):'
SELECT COUNT(*) FROM transaction_mapping_rules WHERE account_code = '7200';

-- Step 5: Verify migration
\echo ''
\echo '========================================='
\echo 'STEP 5: Verification'
\echo '========================================='

\echo 'Remaining transactions with old codes (should be 0):'
SELECT 
    account_code,
    COUNT(*) as count
FROM bank_transactions
WHERE account_code IN ('8310-001', '5000', '6000')
GROUP BY account_code;

\echo 'Remaining mapping rules with old codes (should be 0):'
SELECT 
    account_code,
    COUNT(*) as count
FROM transaction_mapping_rules
WHERE account_code IN ('8310-001', '5000', '6000')
GROUP BY account_code;

\echo 'Summary of migrated transactions by new code:'
SELECT 
    account_code,
    account_name,
    COUNT(*) as transaction_count,
    SUM(COALESCE(debit_amount, 0)) as total_debits,
    SUM(COALESCE(credit_amount, 0)) as total_credits
FROM bank_transactions
WHERE account_code IN ('8400', '7000', '7200')
GROUP BY account_code, account_name
ORDER BY account_code;

-- Step 6: Create rollback script
\echo ''
\echo '========================================='
\echo 'STEP 6: Rollback Script Created'
\echo '========================================='

\echo 'To rollback this migration, run:'
\echo 'BEGIN;'
\echo 'DELETE FROM bank_transactions WHERE account_code IN (SELECT account_code FROM bank_transactions_backup_20251003);'
\echo 'INSERT INTO bank_transactions SELECT * FROM bank_transactions_backup_20251003;'
\echo 'DELETE FROM transaction_mapping_rules WHERE account_code IN (SELECT account_code FROM transaction_mapping_rules_backup_20251003);'
\echo 'INSERT INTO transaction_mapping_rules SELECT * FROM transaction_mapping_rules_backup_20251003;'
\echo 'COMMIT;'

-- Commit all changes
COMMIT;

\echo ''
\echo '========================================='
\echo 'MIGRATION COMPLETED SUCCESSFULLY!'
\echo '========================================='
\echo 'Backup tables created:'
\echo '  - bank_transactions_backup_20251003'
\echo '  - transaction_mapping_rules_backup_20251003'
\echo ''
\echo 'Next steps:'
\echo '1. Run ./run.sh and test chart of accounts initialization'
\echo '2. Test transaction classification workflows'
\echo '3. Verify reports generate correctly'
\echo '4. If all works, drop backup tables after 30 days'
\echo '========================================='
