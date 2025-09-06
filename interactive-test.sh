#!/bin/bash

# FIN Application Interactive Testing Suite
# Generates detailed output files for manual analysis

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Create output directory with timestamp
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUTPUT_DIR="test_results_$TIMESTAMP"
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}🧪 FIN Application Interactive Testing Suite${NC}"
echo "=============================================="
echo "📁 Output directory: $OUTPUT_DIR"
echo ""

# Function to log both to console and file
log_output() {
    echo -e "$1" | tee -a "$OUTPUT_DIR/test_log.txt"
}

# Function to run API test and save output
run_api_test() {
    local endpoint="$1"
    local description="$2"
    local filename="$3"
    local method="${4:-GET}"
    local data="$5"
    
    log_output "${YELLOW}Testing: $description${NC}"
    log_output "Endpoint: $method $endpoint"
    log_output "Time: $(date)"
    log_output "----------------------------------------"
    
    if [ "$method" = "GET" ]; then
        curl -s -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" \
             "$endpoint" | tee "$OUTPUT_DIR/$filename.json" | jq . 2>/dev/null || cat "$OUTPUT_DIR/$filename.json"
    else
        echo "$data" | curl -s -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" \
             -X "$method" -H "Content-Type: application/json" -d @- \
             "$endpoint" | tee "$OUTPUT_DIR/$filename.json" | jq . 2>/dev/null || cat "$OUTPUT_DIR/$filename.json"
    fi
    
    echo "" | tee -a "$OUTPUT_DIR/test_log.txt"
}

# Check server health
log_output "${BLUE}1. Server Health Check${NC}"
log_output "======================"
if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
    log_output "${GREEN}✅ Server is running!${NC}"
    run_api_test "http://localhost:8080/api/v1/health" "Health Check" "01_health_check"
else
    log_output "${RED}❌ Server not running. Please start the server first.${NC}"
    exit 1
fi

# Test Companies API
log_output "${BLUE}2. Companies API Testing${NC}"
log_output "========================"

# Get all companies
run_api_test "http://localhost:8080/api/v1/companies" "Get All Companies" "02_companies_list"

# Create a test company
TEST_COMPANY_DATA='{
    "name": "Interactive Test Company",
    "registrationNumber": "TEST001",
    "address": "123 Test Street, Test City",
    "contactEmail": "test@company.com",
    "contactPhone": "+1234567890"
}'

run_api_test "http://localhost:8080/api/v1/companies" "Create Test Company" "03_company_create" "POST" "$TEST_COMPANY_DATA"

# Extract company ID from response
COMPANY_ID=$(cat "$OUTPUT_DIR/03_company_create.json" | jq -r '.id // 1' 2>/dev/null || echo "1")
log_output "Using Company ID: $COMPANY_ID"

# Get specific company
run_api_test "http://localhost:8080/api/v1/companies/$COMPANY_ID" "Get Company Details" "04_company_details"

# Test Bank Statement Processing
log_output "${BLUE}3. Bank Statement Processing${NC}"
log_output "============================="

# Check available PDF files
log_output "Available PDF files for testing:"
if ls input/*.pdf 1> /dev/null 2>&1; then
    ls -la input/*.pdf | tee "$OUTPUT_DIR/05_available_pdfs.txt"
    
    # Process each PDF file
    for pdf_file in input/*.pdf; do
        if [ -f "$pdf_file" ]; then
            filename=$(basename "$pdf_file")
            log_output "${YELLOW}Processing: $filename${NC}"
            
            # Create multipart form data request
            curl -s -w "\nHTTP Status: %{http_code}\nResponse Time: %{time_total}s\n" \
                 -X POST \
                 -H "Content-Type: multipart/form-data" \
                 -F "file=@$pdf_file" \
                 -F "processingDate=2024-02-17" \
                 "http://localhost:8080/api/v1/companies/$COMPANY_ID/statements" \
                 | tee "$OUTPUT_DIR/06_statement_processing_${filename%.*}.json" \
                 | jq . 2>/dev/null || cat "$OUTPUT_DIR/06_statement_processing_${filename%.*}.json"
            
            echo "" | tee -a "$OUTPUT_DIR/test_log.txt"
        fi
    done
else
    log_output "${YELLOW}⚠️  No PDF files found in input/ directory${NC}"
    echo "No PDF files available" > "$OUTPUT_DIR/05_available_pdfs.txt"
fi

# Test Transactions API
log_output "${BLUE}4. Transactions Retrieval${NC}"
log_output "========================="

# Get transactions for the company
run_api_test "http://localhost:8080/api/v1/companies/$COMPANY_ID/transactions?limit=20" "Get Company Transactions (Limited)" "07_transactions_limited"

# Get all transactions
run_api_test "http://localhost:8080/api/v1/companies/$COMPANY_ID/transactions" "Get All Company Transactions" "08_transactions_all"

# Database Direct Queries for Manual Analysis
log_output "${BLUE}5. Database Analysis${NC}"
log_output "==================="

# Export database data to CSV files
log_output "Exporting database data to CSV files..."

# Companies table
psql -h localhost -p 5432 -U sthwalonyoni -d drimacc_db -c "\copy (SELECT * FROM companies) TO '$PWD/$OUTPUT_DIR/09_companies_table.csv' WITH CSV HEADER;" 2>/dev/null || log_output "Could not export companies table"

# Bank transactions table
psql -h localhost -p 5432 -U sthwalonyoni -d drimacc_db -c "\copy (SELECT * FROM bank_transactions) TO '$PWD/$OUTPUT_DIR/10_bank_transactions.csv' WITH CSV HEADER;" 2>/dev/null || log_output "Could not export transactions table"

# Fiscal periods table
psql -h localhost -p 5432 -U sthwalonyoni -d drimacc_db -c "\copy (SELECT * FROM fiscal_periods) TO '$PWD/$OUTPUT_DIR/11_fiscal_periods.csv' WITH CSV HEADER;" 2>/dev/null || log_output "Could not export fiscal periods table"

# Transaction summary analysis
psql -h localhost -p 5432 -U sthwalonyoni -d drimacc_db -c "
SELECT 
    transaction_type,
    COUNT(*) as count,
    SUM(amount) as total_amount,
    AVG(amount) as avg_amount,
    MIN(amount) as min_amount,
    MAX(amount) as max_amount
FROM bank_transactions 
GROUP BY transaction_type 
ORDER BY transaction_type;
" -o "$OUTPUT_DIR/12_transaction_summary.txt" 2>/dev/null || log_output "Could not generate transaction summary"

# Recent transactions analysis
psql -h localhost -p 5432 -U sthwalonyoni -d drimacc_db -c "
SELECT 
    company_id,
    transaction_date,
    transaction_type,
    amount,
    description,
    created_at
FROM bank_transactions 
ORDER BY created_at DESC 
LIMIT 50;
" -o "$OUTPUT_DIR/13_recent_transactions.txt" 2>/dev/null || log_output "Could not export recent transactions"

# Generate parser test output
log_output "${BLUE}6. Parser Testing${NC}"
log_output "================="

# Test the StandardBankTabularParser directly
if [ -f "app/build/libs/app.jar" ]; then
    log_output "Testing Standard Bank parser with sample data..."
    
    cat > "$OUTPUT_DIR/14_parser_test_input.txt" << 'EOF'
REAL TIME TRANSFER FROM                                                                         5,000.00       02 17                       5,101.28
AUTOBANK CASH WITHDRAWAL AT                                                   4,700.00-                        02 17                         401.28
IMMEDIATE PAYMENT                                                               400.00-                        02 21                           11.28
FEE IMMEDIATE PAYMENT                                     ##                       37.00-                      02 21                         25.72-
IB TRANSFER FROM                                                                                    10.00      02 21                         411.28
REAL TIME TRANSFER FROM                                                                         1,450.00       02 22                       1,424.28
IMMEDIATE PAYMENT                                                             1,210.00-                        02 22                         214.28
EOF

    # Create a simple parser test (if TestDirectParser was available)
    log_output "Parser test input saved to 14_parser_test_input.txt"
fi

# Generate comprehensive summary
log_output "${BLUE}7. Test Summary Generation${NC}"
log_output "========================="

cat > "$OUTPUT_DIR/00_TEST_SUMMARY.md" << EOF
# FIN Application Test Results
## Test Execution: $(date)

### Files Generated:
- \`test_log.txt\` - Complete test execution log
- \`01_health_check.json\` - Server health status
- \`02_companies_list.json\` - All companies in system
- \`03_company_create.json\` - Test company creation result
- \`04_company_details.json\` - Company details
- \`05_available_pdfs.txt\` - List of PDF files for processing
- \`06_statement_processing_*.json\` - Bank statement processing results
- \`07_transactions_limited.json\` - Limited transaction results
- \`08_transactions_all.json\` - All transactions
- \`09_companies_table.csv\` - Companies database export
- \`10_bank_transactions.csv\` - Bank transactions database export
- \`11_fiscal_periods.csv\` - Fiscal periods database export
- \`12_transaction_summary.txt\` - Transaction analysis summary
- \`13_recent_transactions.txt\` - Recent transactions list
- \`14_parser_test_input.txt\` - Parser test input data

### Manual Analysis Checklist:
- [ ] Verify API responses in JSON files
- [ ] Check transaction parsing accuracy in CSV files
- [ ] Validate data consistency between API and database exports
- [ ] Review transaction types and amounts
- [ ] Confirm date parsing correctness
- [ ] Verify Standard Bank format handling

### Key Metrics to Analyze:
1. **Transaction Count**: Check if all transactions were parsed
2. **Amount Accuracy**: Verify credit/debit amounts match PDF
3. **Date Parsing**: Confirm dates are correctly interpreted
4. **Transaction Types**: CREDIT, DEBIT, SERVICE_FEE classification
5. **Description Extraction**: Complete transaction descriptions
6. **Database Integrity**: Consistent data across all tables

EOF

log_output "${GREEN}🎉 Interactive Testing Complete!${NC}"
log_output "=================================="
log_output "📁 All results saved to: $OUTPUT_DIR/"
log_output "📊 Summary report: $OUTPUT_DIR/00_TEST_SUMMARY.md"
log_output ""
log_output "${BLUE}Next Steps for Manual Analysis:${NC}"
log_output "1. Review JSON files for API responses"
log_output "2. Open CSV files in Excel/LibreOffice for data analysis"
log_output "3. Compare parsed transactions with original PDF files"
log_output "4. Check transaction summary for accuracy"
log_output ""
log_output "${YELLOW}Files ready for analysis:${NC}"
ls -la "$OUTPUT_DIR/" | tail -n +2
