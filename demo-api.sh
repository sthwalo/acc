#!/bin/bash

# FIN API Demo Script
echo "🚀 FIN Financial Management System - API Demo"
echo "============================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if server is running
echo -e "${BLUE}1. Checking server health...${NC}"
HEALTH_RESPONSE=$(curl -s http://localhost:8080/api/v1/health)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Server is running!${NC}"
    echo "Response: $HEALTH_RESPONSE"
else
    echo -e "${RED}❌ Server not running. Please start with:${NC}"
    echo "./gradlew run --args=\"api\" -Dfin.license.autoconfirm=true"
    exit 1
fi
echo ""

# Test Companies API
echo -e "${BLUE}2. Testing Companies API...${NC}"
echo -e "${YELLOW}Getting all companies:${NC}"
curl -s http://localhost:8080/api/v1/companies | jq '.'
echo ""

echo -e "${YELLOW}Creating a test company:${NC}"
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/companies \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Demo Company",
    "registrationNumber": "DEMO001",
    "address": "123 Demo Street",
    "contactEmail": "demo@company.com",
    "contactPhone": "+1234567890"
  }')
echo "Response: $CREATE_RESPONSE"
echo ""

# Extract company ID for further tests
COMPANY_ID=$(echo $CREATE_RESPONSE | jq -r '.id // 1')

echo -e "${YELLOW}Getting company details (ID: $COMPANY_ID):${NC}"
curl -s http://localhost:8080/api/v1/companies/$COMPANY_ID | jq '.'
echo ""

# Test Bank Statement Processing (if PDF files exist)
echo -e "${BLUE}3. Testing Bank Statement Processing...${NC}"
if [ -f "input/xxxxx3753 (02).pdf" ]; then
    echo -e "${YELLOW}Processing Standard Bank statement:${NC}"
    STATEMENT_RESPONSE=$(curl -s -X POST \
      http://localhost:8080/api/v1/companies/$COMPANY_ID/statements \
      -H "Content-Type: multipart/form-data" \
      -F "file=@input/xxxxx3753 (02).pdf" \
      -F "processingDate=2024-02-17")
    echo "Response: $STATEMENT_RESPONSE"
    echo ""
else
    echo -e "${YELLOW}⚠️  No PDF files found in input/ directory${NC}"
fi

# Test Transactions API
echo -e "${BLUE}4. Getting transactions for company...${NC}"
curl -s "http://localhost:8080/api/v1/companies/$COMPANY_ID/transactions?limit=5" | jq '.'
echo ""

echo -e "${GREEN}🎉 API Demo Complete!${NC}"
echo ""
echo -e "${BLUE}Available endpoints:${NC}"
echo "• Health: http://localhost:8080/api/v1/health"
echo "• Companies: http://localhost:8080/api/v1/companies"
echo "• Transactions: http://localhost:8080/api/v1/companies/{id}/transactions"
echo "• Process Statement: POST http://localhost:8080/api/v1/companies/{id}/statements"
echo "• API Docs: http://localhost:8080/api/v1/docs"
