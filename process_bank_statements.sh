#!/bin/bash

# Process Bank Statements Script
echo "🏦 Processing Bank Statement PDFs"
echo "=================================="

# Check if API server is running
if ! curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
    echo "🚀 Starting API server..."
    nohup ./gradlew run --args="api" -Dfin.license.autoconfirm=true > api_server.log 2>&1 &
    sleep 20
    echo "⏳ Waiting for server startup..."
fi

# Check server health
echo "🔍 Checking server status..."
HEALTH=$(curl -s http://localhost:8080/api/v1/health)
if [ $? -eq 0 ]; then
    echo "✅ Server is running"
    echo "$HEALTH" | jq '.' 2>/dev/null || echo "$HEALTH"
else
    echo "❌ Server not accessible"
    exit 1
fi

# Get company ID (use the first company)
COMPANY_ID=$(curl -s http://localhost:8080/api/v1/companies | jq -r '.data[0].id' 2>/dev/null)
if [ "$COMPANY_ID" = "null" ] || [ -z "$COMPANY_ID" ]; then
    echo "❌ No companies found. Please create a company first."
    exit 1
fi

echo "🏢 Using Company ID: $COMPANY_ID"

# Process each PDF file
PDF_COUNT=0
SUCCESS_COUNT=0
FAILED_COUNT=0

echo ""
echo "📄 Processing PDF files..."
echo "=========================="

for pdf_file in input/*.pdf; do
    if [ -f "$pdf_file" ]; then
        PDF_COUNT=$((PDF_COUNT + 1))
        filename=$(basename "$pdf_file")
        echo ""
        echo "[$PDF_COUNT] Processing: $filename"
        echo "-----------------------------------"
        
        # Upload and process the PDF
        RESPONSE=$(curl -s -X POST \
            -F "file=@$pdf_file" \
            "http://localhost:8080/api/v1/companies/$COMPANY_ID/upload")
        
        # Check if successful
        if echo "$RESPONSE" | grep -q '"success":true' 2>/dev/null; then
            SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
            echo "✅ Successfully processed"
            
            # Extract transaction count from response
            TRANS_COUNT=$(echo "$RESPONSE" | jq -r '.transactionsProcessed // .data.transactionsProcessed // "N/A"' 2>/dev/null)
            echo "📊 Transactions extracted: $TRANS_COUNT"
        else
            FAILED_COUNT=$((FAILED_COUNT + 1))
            echo "❌ Failed to process"
            echo "Response: $RESPONSE"
        fi
    fi
done

echo ""
echo "🎯 PROCESSING SUMMARY"
echo "===================="
echo "📄 Total PDFs found: $PDF_COUNT"
echo "✅ Successfully processed: $SUCCESS_COUNT"
echo "❌ Failed: $FAILED_COUNT"

# Check final database state
echo ""
echo "🗄️  DATABASE SUMMARY"
echo "==================="

# Query the database directly
echo "📊 Transaction counts by table:"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    'bank_transactions' as table_name,
    COUNT(*) as count 
FROM bank_transactions
UNION ALL
SELECT 
    'companies' as table_name,
    COUNT(*) as count 
FROM companies;
" 2>/dev/null || echo "❌ Could not connect to database"

# Show recent transactions
echo ""
echo "📋 Recent transactions (latest 10):"
psql -U sthwalonyoni -d drimacc_db -c "
SELECT 
    transaction_date,
    details,
    debit_amount,
    credit_amount,
    balance,
    source_file
FROM bank_transactions 
ORDER BY created_at DESC 
LIMIT 10;
" 2>/dev/null || echo "❌ Could not query transactions"

echo ""
echo "🎉 Processing complete!"
echo "💡 You can now view the data in the database or through the API"
