#!/bin/bash
# Schema Comparison Verification Script
# Usage: ./verify_schema_sync.sh

set -e

echo "=== FIN Database Schema Synchronization Verification ==="
echo "Generated: $(date)"
echo

# File paths
PROD_SCHEMA="/Users/sthwalonyoni/FIN/backups/backupschema.sql"
TEST_SCHEMA="/Users/sthwalonyoni/FIN/app/src/test/resources/test_schema.sql"

# Check if files exist
if [[ ! -f "$PROD_SCHEMA" ]]; then
    echo "❌ Production schema file not found: $PROD_SCHEMA"
    exit 1
fi

if [[ ! -f "$TEST_SCHEMA" ]]; then
    echo "❌ Test schema file not found: $TEST_SCHEMA"
    exit 1
fi

echo "📊 SCHEMA STATISTICS"
echo "==================="

# Count tables
PROD_TABLES=$(grep -c "^CREATE TABLE" "$PROD_SCHEMA")
TEST_TABLES=$(grep -c "^CREATE TABLE" "$TEST_SCHEMA")
echo "Production Tables: $PROD_TABLES"
echo "Test Tables:       $TEST_TABLES"
echo "Difference:        $((PROD_TABLES - TEST_TABLES))"
echo

# Count functions
PROD_FUNCTIONS=$(grep -c "^CREATE.*FUNCTION" "$PROD_SCHEMA")
TEST_FUNCTIONS=$(grep -c "^CREATE.*FUNCTION" "$TEST_SCHEMA")
echo "Production Functions: $PROD_FUNCTIONS"
echo "Test Functions:       $TEST_FUNCTIONS"
echo "Difference:           $((PROD_FUNCTIONS - TEST_FUNCTIONS))"
echo

# Count indexes
PROD_INDEXES=$(grep -c "^CREATE.*INDEX" "$PROD_SCHEMA")
TEST_INDEXES=$(grep -c "^CREATE.*INDEX" "$TEST_SCHEMA")
echo "Production Indexes: $PROD_INDEXES"
echo "Test Indexes:       $TEST_INDEXES"
echo "Difference:         $((PROD_INDEXES - TEST_INDEXES))"
echo

echo "🔍 MISSING TABLES IN TEST SCHEMA"
echo "================================="

# Extract table names from both schemas
PROD_TABLE_NAMES=$(grep "^CREATE TABLE" "$PROD_SCHEMA" | sed 's/CREATE TABLE public\.\([a-zA-Z_][a-zA-Z0-9_]*\).*/\1/' | sort)
TEST_TABLE_NAMES=$(grep "^CREATE TABLE" "$TEST_SCHEMA" | sed 's/CREATE TABLE public\.\([a-zA-Z_][a-zA-Z0-9_]*\).*/\1/' | sort)

# Find tables in production but not in test
MISSING_TABLES=$(comm -23 <(echo "$PROD_TABLE_NAMES") <(echo "$TEST_TABLE_NAMES"))

if [[ -z "$MISSING_TABLES" ]]; then
    echo "✅ All production tables present in test schema"
else
    echo "❌ Missing tables:"
    while IFS= read -r table; do
        [[ -n "$table" ]] && echo "  - $table"
    done <<< "$MISSING_TABLES"
fi

echo
echo "🔍 MISSING FUNCTIONS IN TEST SCHEMA"
echo "==================================="

# Extract function names (only actual function definitions, not triggers)
PROD_FUNCTION_NAMES=$(grep "^CREATE FUNCTION" "$PROD_SCHEMA" | sed 's/CREATE FUNCTION public\.\([a-zA-Z_][a-zA-Z0-9_]*\).*/\1/' | sort)
TEST_FUNCTION_NAMES=$(grep "^CREATE FUNCTION" "$TEST_SCHEMA" | sed 's/CREATE FUNCTION public\.\([a-zA-Z_][a-zA-Z0-9_]*\).*/\1/' | sort)

# Find functions in production but not in test
MISSING_FUNCTIONS=$(comm -23 <(echo "$PROD_FUNCTION_NAMES") <(echo "$TEST_FUNCTION_NAMES"))

if [[ -z "$MISSING_FUNCTIONS" ]]; then
    echo "✅ All production functions present in test schema"
else
    echo "❌ Missing functions:"
    while IFS= read -r func; do
        [[ -n "$func" ]] && echo "  - $func"
    done <<< "$MISSING_FUNCTIONS"
fi

echo
echo "📋 SUMMARY"
echo "=========="

# Calculate missing counts more simply
TABLE_COUNT=$(echo "$MISSING_TABLES" | wc -l)
FUNCTION_COUNT=$(echo "$MISSING_FUNCTIONS" | wc -l)
# Adjust for empty strings (wc -l returns 1 for empty input)
[[ -z "$MISSING_TABLES" ]] && TABLE_COUNT=0
[[ -z "$MISSING_FUNCTIONS" ]] && FUNCTION_COUNT=0
TOTAL_MISSING=$(( TABLE_COUNT + FUNCTION_COUNT ))

if [[ $TOTAL_MISSING -eq 0 ]]; then
    echo "✅ SCHEMAS ARE SYNCHRONIZED"
    echo "All production tables and functions are present in test schema."
    exit 0
else
    echo "❌ SCHEMAS ARE OUT OF SYNC"
    echo "Total missing objects: $TOTAL_MISSING"
    echo
    echo "⚠️  ACTION REQUIRED:"
    echo "   1. Update test schema with missing tables and functions"
    echo "   2. Update test data to include new table data"
    echo "   3. Add test cases for new functionality"
    echo "   4. Update database migration scripts"
    exit 1
fi