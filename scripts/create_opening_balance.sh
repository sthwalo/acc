#!/bin/bash

# Opening Balance Creation Script
# Date: October 6, 2025
# Purpose: Create opening balance journal entry for company 2, fiscal period 7

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║        Opening Balance Creation - October 6, 2025             ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Check if database is accessible
echo "🔍 Checking database connection..."
psql -U sthwalonyoni -d drimacc_db -h localhost -c "\q" 2>/dev/null
if [ $? -eq 0 ]; then
    echo "✅ Database connection successful"
else
    echo "❌ Database connection failed"
    exit 1
fi
echo ""

# Display fiscal period information
echo "📅 Fiscal Period Information:"
psql -U sthwalonyoni -d drimacc_db -h localhost -t -c "
SELECT '   Period: ' || period_name || '
   Start Date: ' || start_date || '
   End Date: ' || end_date || '
   Status: ' || CASE WHEN is_closed THEN 'CLOSED' ELSE 'OPEN' END
FROM fiscal_periods
WHERE id = 7;
"
echo ""

# Calculate opening balance
echo "💰 Calculating Opening Balance:"
psql -U sthwalonyoni -d drimacc_db -h localhost -t -c "
WITH first_transaction AS (
    SELECT 
        transaction_date,
        details,
        balance,
        debit_amount,
        credit_amount,
        (balance + COALESCE(debit_amount, 0) - COALESCE(credit_amount, 0)) as opening_balance
    FROM bank_transactions
    WHERE company_id = 2 AND fiscal_period_id = 7
    ORDER BY transaction_date, id
    LIMIT 1
),
last_transaction AS (
    SELECT 
        transaction_date,
        balance as closing_balance
    FROM bank_transactions
    WHERE company_id = 2 AND fiscal_period_id = 7
    ORDER BY transaction_date DESC, id DESC
    LIMIT 1
)
SELECT 
    '   First Transaction: ' || ft.transaction_date || ' - ' || ft.details || '
   Balance After First Transaction: R ' || TO_CHAR(ft.balance, 'FM999,999,999.99') || '
   First Transaction Amount: R ' || TO_CHAR(COALESCE(ft.debit_amount, 0), 'FM999,999,999.99') || ' (debit)
   ----------------------------------------------------------
   CALCULATED OPENING BALANCE: R ' || TO_CHAR(ft.opening_balance, 'FM999,999,999.99') || '
   
   Last Transaction: ' || lt.transaction_date || '
   CLOSING BALANCE: R ' || TO_CHAR(lt.closing_balance, 'FM999,999,999.99')
FROM first_transaction ft, last_transaction lt;
"
echo ""

# Check if opening balance entry already exists
echo "🔍 Checking for existing opening balance entry..."
EXISTING_COUNT=$(psql -U sthwalonyoni -d drimacc_db -h localhost -t -c "
SELECT COUNT(*) 
FROM journal_entries 
WHERE company_id = 2 AND fiscal_period_id = 7 AND reference LIKE 'OB-%';
" | xargs)

if [ "$EXISTING_COUNT" -gt "0" ]; then
    echo "⚠️  Found $EXISTING_COUNT existing opening balance entry(ies)"
    echo ""
    echo "Existing Entry Details:"
    psql -U sthwalonyoni -d drimacc_db -h localhost -c "
    SELECT 
        je.reference,
        je.entry_date,
        je.description,
        a.account_code,
        a.account_name,
        jel.debit_amount,
        jel.credit_amount
    FROM journal_entries je
    JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
    JOIN accounts a ON jel.account_id = a.id
    WHERE je.company_id = 2 AND je.fiscal_period_id = 7 AND je.reference LIKE 'OB-%'
    ORDER BY je.reference, jel.debit_amount DESC NULLS LAST;
    "
    echo ""
    read -p "Delete existing entry and recreate? (yes/no): " CONFIRM
    if [ "$CONFIRM" != "yes" ]; then
        echo "❌ Operation cancelled"
        exit 0
    fi
fi
echo ""

# Run the Java service to create opening balance
echo "🚀 Creating opening balance journal entry..."
echo ""

# Use gradle to run a custom task (we'll create this next)
# For now, show the SQL that would be executed
echo "📋 SQL to be executed:"
echo ""
echo "-- Delete existing opening balance entry if exists"
echo "DELETE FROM journal_entry_lines"
echo "WHERE journal_entry_id IN ("
echo "    SELECT id FROM journal_entries"
echo "    WHERE company_id = 2 AND fiscal_period_id = 7 AND reference LIKE 'OB-%'"
echo ");"
echo ""
echo "DELETE FROM journal_entries"
echo "WHERE company_id = 2 AND fiscal_period_id = 7 AND reference LIKE 'OB-%';"
echo ""
echo "-- Create new opening balance entry"
echo "INSERT INTO journal_entries (reference, entry_date, description, fiscal_period_id, company_id, created_by, created_at, updated_at)"
echo "VALUES ('OB-7', '2024-03-01', 'Opening Balance - FY2024-2025', 7, 2, 'system', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
echo "RETURNING id;  -- Assume id = 1000"
echo ""
echo "-- DEBIT Bank Account (1100)"
echo "INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, reference, created_at)"
echo "VALUES (1000, (SELECT id FROM accounts WHERE company_id = 2 AND account_code = '1100'), 479507.94, NULL, 'Bank - Current Account', 'OB-7-L1', CURRENT_TIMESTAMP);"
echo ""
echo "-- CREDIT Retained Earnings (5100)"
echo "INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, credit_amount, description, reference, created_at)"
echo "VALUES (1000, (SELECT id FROM accounts WHERE company_id = 2 AND account_code = '5100'), NULL, 479507.94, 'Opening Balance Equity', 'OB-7-L2', CURRENT_TIMESTAMP);"
echo ""
echo "╔════════════════════════════════════════════════════════════════╗"
echo "║                    MANUAL EXECUTION REQUIRED                    ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""
echo "To create the opening balance entry, run:"
echo ""
echo "  java -jar app/build/libs/fin-spring.jar"
echo ""
echo "Then navigate to:"
echo "  Data Management → Opening Balance Management → Create Opening Balance"},{
echo ""
echo "Or execute the SQL above manually in psql."
echo ""
