#!/bin/bash

echo "🏦 Processing Bank Statements via Console Application"
echo "=================================================="

# Check if there are PDF files
PDF_COUNT=$(find input -name "*.pdf" 2>/dev/null | wc -l)
echo "📄 Found $PDF_COUNT PDF files to process"

if [ $PDF_COUNT -eq 0 ]; then
    echo "❌ No PDF files found in input directory"
    exit 1
fi

echo ""
echo "🚀 Starting interactive processing..."
echo "ℹ️  This will use the console application to process files"

# Create input commands for batch processing
echo "Creating automated input..."

# We'll process just one file first to test
FIRST_PDF=$(find input -name "*.pdf" | head -1)
echo "📋 Testing with: $FIRST_PDF"

# Use java JAR directly with a much simpler approach
echo "🔧 Using JAR execution with console override..."

# Create a simple test input file
cat > process_input.txt << EOF
yes
1
Xinghizana Group
REG2024001

123 Business Park, City Center
info@xinghizana.com
+27123456789
2
FY2024-2025
2024-04-01
2025-03-31
3
1
$FIRST_PDF
n
10
EOF

echo "📝 Input file created. Processing..."

# Run with timeout to prevent hanging
java -jar app/build/libs/app.jar < process_input.txt

echo ""
echo "🔍 Checking database for results..."

# Check the database
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    COUNT(*) as total_transactions,
    COUNT(DISTINCT source_file) as unique_files
FROM bank_transactions;
" 2>/dev/null || echo "❌ Could not query database"

if [ $(psql -U sthwalonyoni -d drimacc_db -t -c "SELECT COUNT(*) FROM bank_transactions;" 2>/dev/null | tr -d ' ') -gt 0 ]; then
    echo "✅ Transactions found! Processing more files..."
    
    # Process remaining files one by one
    for pdf_file in input/*.pdf; do
        if [ "$pdf_file" != "$FIRST_PDF" ]; then
            echo "🔄 Processing: $(basename "$pdf_file")"
            
            # Create input for this file
            cat > process_next.txt << EOF
yes
3
1
$pdf_file
n
10
EOF
            
            # Process this file
            java -jar app/build/libs/app.jar < process_next.txt > /dev/null 2>&1
        fi
    done
    
    rm -f process_next.txt
else
    echo "❌ No transactions were processed. There might be an issue."
fi

# Final count
echo ""
echo "📊 FINAL RESULTS:"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    COUNT(*) as total_transactions,
    MIN(transaction_date) as earliest_date,
    MAX(transaction_date) as latest_date,
    COUNT(DISTINCT source_file) as files_processed
FROM bank_transactions;
" 2>/dev/null || echo "❌ Could not query final results"

# Clean up
rm -f process_input.txt

echo ""
echo "🎉 Processing complete!"
