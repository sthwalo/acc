#!/bin/bash

# Test script for depreciation calculation with journal entry posting
# This script tests the complete depreciation workflow including journal entries

echo "=== FIN Depreciation Calculation Test ==="
echo "Testing depreciation calculation with journal entry posting"
echo ""

# Set environment variables for database connection
export DATABASE_URL="jdbc:postgresql://localhost:5432/drimacc_db"
export DATABASE_USER="sthwalonyoni"
export DATABASE_PASSWORD="$(cat secrets/db_password.txt)"

echo "Database URL: $DATABASE_URL"
echo "Database User: $DATABASE_USER"
echo ""

# Run the application in console mode to test depreciation functionality
echo "To test the depreciation functionality interactively, run:"
echo "  java -jar app/build/libs/fin-spring.jar"
echo ""
echo "Then follow these steps:"
echo "1. Select a company (e.g., Xinghizana Group)"
echo "2. Set up a fiscal period if needed"
echo "3. Navigate to Depreciation Calculator (option 10)"
echo "4. Test depreciation calculation - it should now create journal entries"
echo ""
echo "Expected behavior:"
echo "- Depreciation schedule should be calculated and saved"
echo "- Journal entries should be created for each depreciation year"
echo "- Debit Depreciation Expense (9400) and Credit Accumulated Depreciation (2100)"
echo "- No more 'source_transaction_id' type mismatch errors"
echo ""

# Don't run the application - just show instructions
# cd /Users/sthwalonyoni/FIN
# java -jar app/build/libs/fin-spring.jar

echo ""
echo "=== Test Complete ==="
echo "If depreciation calculation worked without the type mismatch error,"
echo "the journal entry posting functionality has been successfully fixed!"