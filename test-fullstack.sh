#!/bin/bash

# API Testing Script for FIN Full-Stack Environment
# Tests all backend endpoints and verifies frontend connectivity
# Copyright 2025 Immaculate Nyoni

echo "🔍 FIN API Testing Suite"
echo "========================"
echo ""

BACKEND_URL="http://localhost:8080/api/v1"
FRONTEND_URL="http://localhost:3000"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    
    echo -n "🔧 Testing $method $endpoint - $description... "
    
    if [ "$method" = "GET" ]; then
        response=$(curl -s -w "%{http_code}" -o /tmp/api_response "$BACKEND_URL$endpoint")
    elif [ "$method" = "POST" ]; then
        response=$(curl -s -w "%{http_code}" -o /tmp/api_response -X POST \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BACKEND_URL$endpoint")
    fi
    
    http_code="${response: -3}"
    
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}✅ PASS${NC} (HTTP $http_code)"
        if [ "$1" = "-v" ]; then
            echo "   Response: $(cat /tmp/api_response | jq . 2>/dev/null || cat /tmp/api_response)"
        fi
    else
        echo -e "${RED}❌ FAIL${NC} (HTTP $http_code)"
        echo "   Response: $(cat /tmp/api_response)"
    fi
    
    echo ""
}

# Test backend connectivity
echo "📊 BACKEND API TESTS"
echo "===================="
echo ""

# Health check
test_endpoint "GET" "/health" "Health Check"

# API documentation
test_endpoint "GET" "/docs" "API Documentation"

# Companies list (should be empty initially)
test_endpoint "GET" "/companies" "List Companies"

# Create a test company
echo "📝 Creating test company..."
test_company='{
    "name": "Test Company Ltd",
    "registrationNumber": "TEST123",
    "taxNumber": "TAX456",
    "address": "123 Test Street, Test City",
    "contactEmail": "test@company.com",
    "contactPhone": "+1234567890"
}'

test_endpoint "POST" "/companies" "Create Test Company" "$test_company"

# List companies again (should now have our test company)
test_endpoint "GET" "/companies" "List Companies (after creation)"

echo ""
echo "🎨 FRONTEND CONNECTIVITY TESTS"
echo "==============================="
echo ""

# Test if frontend is running
echo -n "🌐 Testing frontend connectivity... "
if curl -s "$FRONTEND_URL" > /dev/null; then
    echo -e "${GREEN}✅ Frontend is running${NC}"
    echo "   URL: $FRONTEND_URL"
else
    echo -e "${YELLOW}⚠️  Frontend not accessible${NC}"
    echo "   Make sure frontend is running on port 3000"
fi

echo ""

# Test CORS
echo -n "🔗 Testing CORS configuration... "
cors_response=$(curl -s -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: GET" \
    -H "Access-Control-Request-Headers: Content-Type" \
    -X OPTIONS \
    "$BACKEND_URL/health" \
    -w "%{http_code}")

http_code="${cors_response: -3}"

if [ "$http_code" = "200" ]; then
    echo -e "${GREEN}✅ CORS properly configured${NC}"
else
    echo -e "${RED}❌ CORS issue detected${NC} (HTTP $http_code)"
fi

echo ""
echo "🧪 INTEGRATION TESTS"
echo "===================="
echo ""

# Test if frontend can fetch from backend
echo -n "🔄 Testing frontend-to-backend communication... "
if command -v node &> /dev/null; then
    node -e "
        fetch('$BACKEND_URL/health')
            .then(response => response.json())
            .then(data => {
                console.log('✅ Frontend can communicate with backend');
                console.log('   Status:', data.status);
            })
            .catch(error => {
                console.log('❌ Frontend-to-backend communication failed');
                console.log('   Error:', error.message);
            });
    " 2>/dev/null
else
    echo -e "${YELLOW}⚠️  Node.js not available for fetch test${NC}"
fi

echo ""
echo "📊 TEST SUMMARY"
echo "==============="
echo ""
echo "🎯 Quick Manual Tests:"
echo "   1. Visit: $FRONTEND_URL"
echo "   2. Open browser dev tools (F12)"
echo "   3. Check console for errors"
echo "   4. Try creating a company in the UI"
echo ""
echo "🔗 Useful URLs:"
echo "   📊 Health Check:    $BACKEND_URL/health"
echo "   🏢 Companies API:   $BACKEND_URL/companies"
echo "   📖 API Docs:        $BACKEND_URL/docs"
echo "   🎨 Frontend UI:     $FRONTEND_URL"
echo ""
echo "🛠️  Development Commands:"
echo "   📋 View backend logs:  tail -f backend.log"
echo "   📋 View frontend logs: tail -f frontend.log"
echo "   🔄 Restart backend:    pkill -f 'app.jar api' && ./start-fullstack.sh"
echo "   🔄 Restart frontend:   cd drimacc && npm run dev"
echo ""

# Cleanup
rm -f /tmp/api_response

echo "✅ API testing complete!"
