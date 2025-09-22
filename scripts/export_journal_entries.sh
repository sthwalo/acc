#!/bin/bash
# Export Journal Entries to Multiple Formats
# Usage: ./export_journal_entries.sh

echo "📊 Exporting Journal Entries Data..."
echo "====================================="

# Create exports directory
mkdir -p exports

# Get current timestamp for filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

echo "🔗 Connecting to database..."

# Export journal entries with lines to CSV
echo "📄 Exporting journal entries to CSV..."
psql -U sthwalonyoni -d drimacc_db -c "
COPY (
    SELECT
        je.id as journal_entry_id,
        je.reference,
        je.entry_date,
        je.description as journal_description,
        jel.id as line_id,
        a.account_code,
        a.account_name,
        jel.debit_amount,
        jel.credit_amount,
        jel.description as line_description,
        jel.reference as line_reference,
        bt.transaction_date as source_transaction_date,
        bt.details as source_transaction_details,
        bt.debit_amount as source_debit,
        bt.credit_amount as source_credit,
        je.created_at,
        je.created_by
    FROM journal_entries je
    LEFT JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
    LEFT JOIN accounts a ON jel.account_id = a.id
    LEFT JOIN bank_transactions bt ON jel.source_transaction_id = bt.id
    ORDER BY je.entry_date, je.id, jel.id
) TO STDOUT WITH CSV HEADER;" > "exports/journal_entries_${TIMESTAMP}.csv"

# Export summary by date to CSV
echo "📊 Exporting journal entries summary to CSV..."
psql -U sthwalonyoni -d drimacc_db -c "
COPY (
    SELECT
        entry_date,
        COUNT(DISTINCT je.id) as journal_entries_count,
        COUNT(jel.id) as total_lines,
        SUM(jel.debit_amount) as total_debits,
        SUM(jel.credit_amount) as total_credits,
        COUNT(DISTINCT jel.account_id) as accounts_used
    FROM journal_entries je
    LEFT JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
    GROUP BY entry_date
    ORDER BY entry_date
) TO STDOUT WITH CSV HEADER;" > "exports/journal_entries_summary_${TIMESTAMP}.csv"

# Export detailed readable TXT format
echo "📝 Exporting human-readable TXT format..."
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    '=== JOURNAL ENTRY #' || je.id || ' ===' || E'\n' ||
    'Date: ' || je.entry_date || E'\n' ||
    'Reference: ' || COALESCE(je.reference, 'N/A') || E'\n' ||
    'Description: ' || COALESCE(je.description, 'N/A') || E'\n' ||
    'Created: ' || je.created_at || ' by ' || COALESCE(je.created_by, 'System') || E'\n' ||
    E'\nLINES:\n' ||
    string_agg(
        '  ' || a.account_code || ' - ' || a.account_name || ': ' ||
        CASE
            WHEN jel.debit_amount > 0 THEN 'DEBIT R' || jel.debit_amount
            WHEN jel.credit_amount > 0 THEN 'CREDIT R' || jel.credit_amount
            ELSE 'ZERO'
        END ||
        CASE
            WHEN jel.description IS NOT NULL AND jel.description != '' THEN ' (' || jel.description || ')'
            ELSE ''
        END,
        E'\n'
    ) ||
    E'\nSOURCE TRANSACTION: ' ||
    CASE
        WHEN bt.id IS NOT NULL THEN
            bt.transaction_date || ' - ' || LEFT(bt.details, 60) || ' (R' ||
            COALESCE(bt.debit_amount::text, '0.00') || '/R' || COALESCE(bt.credit_amount::text, '0.00') || ')'
        ELSE 'N/A'
    END ||
    E'\n' || E'================================================================================\n'
FROM journal_entries je
LEFT JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
LEFT JOIN accounts a ON jel.account_id = a.id
LEFT JOIN bank_transactions bt ON jel.source_transaction_id = bt.id
GROUP BY je.id, je.entry_date, je.reference, je.description, je.created_at, je.created_by, bt.id, bt.transaction_date, bt.details, bt.debit_amount, bt.credit_amount
ORDER BY je.entry_date, je.id;" > "exports/journal_entries_detailed_${TIMESTAMP}.txt"

# Export summary statistics to TXT
echo "📈 Exporting summary statistics to TXT..."
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    '=== JOURNAL ENTRIES SUMMARY STATISTICS ===' || E'\n' ||
    'Total Journal Entries: ' || COUNT(DISTINCT je.id) || E'\n' ||
    'Total Journal Lines: ' || COUNT(jel.id) || E'\n' ||
    'Date Range: ' || MIN(je.entry_date) || ' to ' || MAX(je.entry_date) || E'\n' ||
    'Total Debit Amount: R' || TO_CHAR(SUM(jel.debit_amount), 'FM999,999,999.00') || E'\n' ||
    'Total Credit Amount: R' || TO_CHAR(SUM(jel.credit_amount), 'FM999,999,999.00') || E'\n' ||
    'Accounts Used: ' || COUNT(DISTINCT jel.account_id) || E'\n' ||
    'Source Transactions Linked: ' || COUNT(DISTINCT jel.source_transaction_id) || E'\n' ||
    E'\n=== TOP 10 ACCOUNTS BY USAGE ===\n'
FROM journal_entries je
LEFT JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id;" > "exports/journal_entries_stats_${TIMESTAMP}.txt"

# Add top accounts separately
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    a.account_code || ' - ' || a.account_name || ': ' || COUNT(*) || ' entries'
FROM journal_entry_lines jel
JOIN accounts a ON jel.account_id = a.id
GROUP BY a.id, a.account_code, a.account_name
ORDER BY COUNT(*) DESC
LIMIT 10;" >> "exports/journal_entries_stats_${TIMESTAMP}.txt"

# Add entries by date
echo -e "\n=== ENTRIES BY DATE ===" >> "exports/journal_entries_stats_${TIMESTAMP}.txt"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT
    entry_date || ': ' || COUNT(DISTINCT id) || ' entries'
FROM journal_entries
GROUP BY entry_date
ORDER BY entry_date DESC
LIMIT 20;" >> "exports/journal_entries_stats_${TIMESTAMP}.txt"

# Create XLSX using Python (if available)
echo "📊 Attempting to create XLSX format..."
if command -v python3 &> /dev/null; then
    python3 << 'EOF'
import pandas as pd
import sys
import os

# Read the CSV file
csv_file = f"exports/journal_entries_${os.environ['TIMESTAMP']}.csv"
xlsx_file = f"exports/journal_entries_${os.environ['TIMESTAMP']}.xlsx"

try:
    df = pd.read_csv(csv_file)
    df.to_excel(xlsx_file, index=False, engine='openpyxl')
    print(f"✅ XLSX file created: {xlsx_file}")
except Exception as e:
    print(f"❌ Failed to create XLSX: {e}")
    print("Note: Install pandas and openpyxl with: pip install pandas openpyxl")
EOF
else
    echo "⚠️  Python3 not found, skipping XLSX creation"
    echo "   To create XLSX files, install Python and run: pip install pandas openpyxl"
fi

# Create PDF using pandoc (if available)
echo "📄 Attempting to create PDF format..."
if command -v pandoc &> /dev/null; then
    pandoc "exports/journal_entries_stats_${TIMESTAMP}.txt" -o "exports/journal_entries_report_${TIMESTAMP}.pdf" --pdf-engine=pdflatex
    if [ $? -eq 0 ]; then
        echo "✅ PDF report created: exports/journal_entries_report_${TIMESTAMP}.pdf"
    else
        echo "❌ Failed to create PDF (missing LaTeX?)"
        echo "   On macOS, install MacTeX: brew install --cask mactex"
    fi
else
    echo "⚠️  Pandoc not found, skipping PDF creation"
    echo "   To create PDF files, install pandoc: brew install pandoc"
fi

echo ""
echo "✅ Export completed!"
echo "📁 Files created in 'exports/' directory:"
echo "   - journal_entries_${TIMESTAMP}.csv (Complete data)"
echo "   - journal_entries_summary_${TIMESTAMP}.csv (Daily summary)"
echo "   - journal_entries_detailed_${TIMESTAMP}.txt (Human readable)"
echo "   - journal_entries_stats_${TIMESTAMP}.txt (Statistics)"
if [ -f "exports/journal_entries_${TIMESTAMP}.xlsx" ]; then
    echo "   - journal_entries_${TIMESTAMP}.xlsx (Excel format)"
fi
if [ -f "exports/journal_entries_report_${TIMESTAMP}.pdf" ]; then
    echo "   - journal_entries_report_${TIMESTAMP}.pdf (PDF report)"
fi
echo ""
echo "📊 Quick stats:"
echo "Total Journal Entries: $(psql -U sthwalonyoni -d drimacc_db -t -c 'SELECT COUNT(*) FROM journal_entries;' | tr -d ' ')"
echo "Total Journal Lines: $(psql -U sthwalonyoni -d drimacc_db -t -c 'SELECT COUNT(*) FROM journal_entry_lines;' | tr -d ' ')"
echo "Date Range: $(psql -U sthwalonyoni -d drimacc_db -t -c "SELECT MIN(entry_date) || ' to ' || MAX(entry_date) FROM journal_entries;" | tr -d ' ')"
echo ""
echo "🔍 To analyze the data:"
echo "   head exports/journal_entries_detailed_${TIMESTAMP}.txt"
echo "   wc -l exports/journal_entries_${TIMESTAMP}.csv"
echo "   open exports/journal_entries_${TIMESTAMP}.xlsx  # if created"