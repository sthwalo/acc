#!/bin/bash

echo "🏦 Direct Bank Statement Processing"
echo "=================================="

# Process files using the application's interactive mode
echo "📄 Processing all PDF files directly into database..."

# Create a temp file with commands
cat > /tmp/process_commands.txt << 'EOF'
1
1
3
input/xxxxx3753 (02).pdf
3
input/xxxxx3753 (03).pdf
3
input/xxxxx3753 (04).pdf
3
input/xxxxx3753 (05).pdf
3
input/xxxxx3753 (06).pdf
3
input/xxxxx3753 (07).pdf
3
input/xxxxx3753 (08).pdf
3
input/xxxxx3753 (09).pdf
3
input/xxxxx3753 (10).pdf
3
input/xxxxx3753 (11).pdf
3
input/xxxxx3753 (12).pdf
3
input/xxxxx3753 (13).pdf
3
input/xxxxx3753 (14).pdf
10
EOF

echo "🚀 Starting batch processing..."

# Run the application with the commands
timeout 60 ./gradlew run -Dfin.license.autoconfirm=true < /tmp/process_commands.txt || true

echo ""
echo "🗄️  Checking database for results..."

# Check the database
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    COUNT(*) as total_transactions,
    MIN(transaction_date) as earliest_date,
    MAX(transaction_date) as latest_date,
    SUM(CASE WHEN credit_amount > 0 THEN credit_amount ELSE 0 END) as total_credits,
    SUM(CASE WHEN debit_amount > 0 THEN debit_amount ELSE 0 END) as total_debits
FROM bank_transactions;
" 2>/dev/null || echo "❌ Could not query database"

echo ""
echo "📋 Sample transactions:"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    transaction_date,
    SUBSTRING(details, 1, 50) as details_preview,
    debit_amount,
    credit_amount,
    balance,
    SUBSTRING(source_file, 1, 30) as file_source
FROM bank_transactions 
ORDER BY transaction_date DESC 
LIMIT 5;
" 2>/dev/null || echo "❌ Could not query transactions"

# Clean up
rm -f /tmp/process_commands.txt

echo ""
echo "🎉 Processing complete!"
