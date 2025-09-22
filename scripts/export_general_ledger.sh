#!/bin/bash
# Export General Ledger from Journal Entries
# Usage: ./export_general_ledger.sh

echo "📊 Exporting General Ledger from Journal Entries..."
echo "=================================================="

# Create exports directory
mkdir -p exports

# Get current timestamp for filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "🔗 Connecting to database..."

# Export General Ledger summary by account
echo "📈 Exporting General Ledger summary..."
psql -U sthwalonyoni -d drimacc_db -c "
COPY (
    SELECT
        a.account_code,
        a.account_name,
        COUNT(jel.id) as transaction_count,
        COALESCE(SUM(jel.debit_amount), 0) as total_debits,
        COALESCE(SUM(jel.credit_amount), 0) as total_credits,
        (COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0)) as balance,
        CASE
            WHEN a.account_code LIKE '1%' THEN 'ASSET'
            WHEN a.account_code LIKE '2%' THEN 'LIABILITY'
            WHEN a.account_code LIKE '3%' THEN 'EQUITY'
            WHEN a.account_code LIKE '4%' OR a.account_code LIKE '5%' OR a.account_code LIKE '6%' THEN 'INCOME'
            WHEN a.account_code LIKE '7%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%' THEN 'EXPENSE'
            ELSE 'UNKNOWN'
        END as account_type
    FROM accounts a
    LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
    GROUP BY a.id, a.account_code, a.account_name
    HAVING COUNT(jel.id) > 0
    ORDER BY a.account_code
) TO STDOUT WITH CSV HEADER;" > "exports/general_ledger_summary_${TIMESTAMP}.csv"

# Export detailed General Ledger with all transactions
echo "📄 Exporting detailed General Ledger..."
psql -U sthwalonyoni -d drimacc_db -c "
COPY (
    SELECT
        a.account_code,
        a.account_name,
        je.entry_date,
        je.reference,
        jel.debit_amount,
        jel.credit_amount,
        jel.description,
        je.description as journal_description,
        bt.transaction_date as source_date,
        bt.details as source_details,
        bt.debit_amount as source_debit,
        bt.credit_amount as source_credit
    FROM accounts a
    JOIN journal_entry_lines jel ON a.id = jel.account_id
    JOIN journal_entries je ON jel.journal_entry_id = je.id
    LEFT JOIN bank_transactions bt ON jel.source_transaction_id = bt.id
    ORDER BY a.account_code, je.entry_date, je.id, jel.id
) TO STDOUT WITH CSV HEADER;" > "exports/general_ledger_detailed_${TIMESTAMP}.csv"

# Export readable General Ledger text format
echo "📝 Exporting human-readable General Ledger..."
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    '================================================================================' || E'\n' ||
    'GENERAL LEDGER - ' || a.account_code || ' - ' || a.account_name || E'\n' ||
    '================================================================================' || E'\n' ||
    'Account Type: ' ||
    CASE
        WHEN a.account_code LIKE '1%' THEN 'ASSET'
        WHEN a.account_code LIKE '2%' THEN 'LIABILITY'
        WHEN a.account_code LIKE '3%' THEN 'EQUITY'
        WHEN a.account_code LIKE '4%' OR a.account_code LIKE '5%' OR a.account_code LIKE '6%' THEN 'INCOME'
        WHEN a.account_code LIKE '7%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%' THEN 'EXPENSE'
        ELSE 'UNKNOWN'
    END || E'\n' ||
    'Transactions: ' || COUNT(jel.id) || E'\n' ||
    'Total Debits: R' || TO_CHAR(SUM(jel.debit_amount), 'FM999,999,999.00') || E'\n' ||
    'Total Credits: R' || TO_CHAR(SUM(jel.credit_amount), 'FM999,999,999.00') || E'\n' ||
    'Balance: R' || TO_CHAR((SUM(jel.debit_amount) - SUM(jel.credit_amount)), 'FM999,999,999.00') || E'\n' ||
    '================================================================================' || E'\n' ||
    E'\nTRANSACTION DETAILS:\n' ||
    COALESCE(
        string_agg(
            TO_CHAR(je.entry_date, 'YYYY-MM-DD') || ' | ' ||
            COALESCE(je.reference, 'N/A') || ' | ' ||
            CASE
                WHEN jel.debit_amount > 0 THEN 'DR R' || jel.debit_amount
                WHEN jel.credit_amount > 0 THEN 'CR R' || jel.credit_amount
                ELSE 'ZERO'
            END || ' | ' ||
            COALESCE(jel.description, 'N/A') ||
            CASE
                WHEN bt.id IS NOT NULL THEN E'\n    [Source: ' || bt.transaction_date || ' - ' || LEFT(bt.details, 50) || ']'
                ELSE ''
            END,
            E'\n'
            ORDER BY je.entry_date, je.id
        ),
        'No transactions found'
    ) ||
    E'\n\n================================================================================\n'
FROM accounts a
LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
LEFT JOIN bank_transactions bt ON jel.source_transaction_id = bt.id
GROUP BY a.id, a.account_code, a.account_name
HAVING COUNT(jel.id) > 0
ORDER BY a.account_code;" > "exports/general_ledger_readable_${TIMESTAMP}.txt"

# Export GL summary by account type
echo "📊 Exporting GL summary by account type..."
echo "=== GENERAL LEDGER SUMMARY BY ACCOUNT TYPE ===" > "exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo "ASSETS:" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    a.account_code || ' - ' || a.account_name || ': R' ||
    TO_CHAR((COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0)), 'FM999,999,999.00') ||
    ' (' || COUNT(jel.id) || ' txns)'
FROM accounts a
JOIN journal_entry_lines jel ON a.id = jel.account_id
WHERE a.account_code LIKE '1%'
GROUP BY a.id, a.account_code, a.account_name
ORDER BY a.account_code;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo "" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "LIABILITIES:" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    a.account_code || ' - ' || a.account_name || ': R' ||
    TO_CHAR((COALESCE(SUM(jel.credit_amount), 0) - COALESCE(SUM(jel.debit_amount), 0)), 'FM999,999,999.00') ||
    ' (' || COUNT(jel.id) || ' txns)'
FROM accounts a
JOIN journal_entry_lines jel ON a.id = jel.account_id
WHERE a.account_code LIKE '2%'
GROUP BY a.id, a.account_code, a.account_name
ORDER BY a.account_code;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo "" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "INCOME:" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    a.account_code || ' - ' || a.account_name || ': R' ||
    TO_CHAR((COALESCE(SUM(jel.credit_amount), 0) - COALESCE(SUM(jel.debit_amount), 0)), 'FM999,999,999.00') ||
    ' (' || COUNT(jel.id) || ' txns)'
FROM accounts a
JOIN journal_entry_lines jel ON a.id = jel.account_id
WHERE a.account_code LIKE '4%' OR a.account_code LIKE '5%' OR a.account_code LIKE '6%'
GROUP BY a.id, a.account_code, a.account_name
ORDER BY a.account_code;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo "" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "EXPENSES:" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    a.account_code || ' - ' || a.account_name || ': R' ||
    TO_CHAR((COALESCE(SUM(jel.debit_amount), 0) - COALESCE(SUM(jel.credit_amount), 0)), 'FM999,999,999.00') ||
    ' (' || COUNT(jel.id) || ' txns)'
FROM accounts a
JOIN journal_entry_lines jel ON a.id = jel.account_id
WHERE a.account_code LIKE '7%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%'
GROUP BY a.id, a.account_code, a.account_name
ORDER BY a.account_code;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo "" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "TOTALS:" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 'Total Assets: R' || COALESCE(TO_CHAR(SUM(asset_balance), 'FM999,999,999.00'), '0.00')
FROM (
    SELECT SUM(jel.debit_amount) - SUM(jel.credit_amount) as asset_balance
    FROM accounts a
    JOIN journal_entry_lines jel ON a.id = jel.account_id
    WHERE a.account_code LIKE '1%'
    GROUP BY a.id
) as asset_totals;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

psql -U sthwalonyoni -d drimacc_db -c "
SELECT 'Total Liabilities: R' || COALESCE(TO_CHAR(SUM(liability_balance), 'FM999,999,999.00'), '0.00')
FROM (
    SELECT SUM(jel.credit_amount) - SUM(jel.debit_amount) as liability_balance
    FROM accounts a
    JOIN journal_entry_lines jel ON a.id = jel.account_id
    WHERE a.account_code LIKE '2%'
    GROUP BY a.id
) as liability_totals;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

psql -U sthwalonyoni -d drimacc_db -c "
SELECT 'Total Income: R' || COALESCE(TO_CHAR(SUM(income_balance), 'FM999,999,999.00'), '0.00')
FROM (
    SELECT SUM(jel.credit_amount) - SUM(jel.debit_amount) as income_balance
    FROM accounts a
    JOIN journal_entry_lines jel ON a.id = jel.account_id
    WHERE a.account_code LIKE '4%' OR a.account_code LIKE '5%' OR a.account_code LIKE '6%'
    GROUP BY a.id
) as income_totals;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

psql -U sthwalonyoni -d drimacc_db -c "
SELECT 'Total Expenses: R' || COALESCE(TO_CHAR(SUM(expense_balance), 'FM999,999,999.00'), '0.00')
FROM (
    SELECT SUM(jel.debit_amount) - SUM(jel.credit_amount) as expense_balance
    FROM accounts a
    JOIN journal_entry_lines jel ON a.id = jel.account_id
    WHERE a.account_code LIKE '7%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%'
    GROUP BY a.id
) as expense_totals;" >> "exports/general_ledger_by_type_${TIMESTAMP}.txt"

echo ""
echo "✅ General Ledger export completed!"
echo "📁 Files created in 'exports/' directory:"
echo "   - general_ledger_summary_${TIMESTAMP}.csv (Account balances)"
echo "   - general_ledger_detailed_${TIMESTAMP}.csv (All transactions)"
echo "   - general_ledger_readable_${TIMESTAMP}.txt (Human readable)"
echo "   - general_ledger_by_type_${TIMESTAMP}.txt (By account type)"
echo ""
echo "📊 GL Statistics:"
echo "Accounts with transactions: $(psql -U sthwalonyoni -d drimacc_db -t -c "SELECT COUNT(DISTINCT a.id) FROM accounts a JOIN journal_entry_lines jel ON a.id = jel.account_id;")"
echo "Total transactions: $(psql -U sthwalonyoni -d drimacc_db -t -c "SELECT COUNT(*) FROM journal_entry_lines;")"
echo ""
echo "🔍 To analyze the GL:"
echo "   head exports/general_ledger_readable_${TIMESTAMP}.txt"
echo "   grep 'INCOME:' exports/general_ledger_by_type_${TIMESTAMP}.txt"
echo "   grep 'EXPENSES:' exports/general_ledger_by_type_${TIMESTAMP}.txt"