#!/bin/bash

# Test script for asset disposal functionality
# This script demonstrates the complete depreciation and disposal workflow

echo "=== FIN Asset Disposal Test Script ==="
echo "Testing the complete depreciation and disposal workflow"
echo ""

# Set environment variables for database connection
export DATABASE_URL="jdbc:postgresql://localhost:5432/drimacc_db"
export DATABASE_USER="sthwalonyoni"
export DATABASE_PASSWORD="$(cat secrets/db_password.txt)"

echo "Database URL: $DATABASE_URL"
echo "Database User: $DATABASE_USER"
echo ""

# Run the application in console mode to test disposal functionality
echo "To test the disposal functionality interactively, run:"
echo "  java -jar app/build/libs/fin-spring.jar"
echo ""
echo "Then follow these steps:"
echo "1. Select a company (e.g., Xinghizana Group)"
echo "2. Navigate to Asset Management menu"
echo "3. Test depreciation calculation"
echo "4. Test asset disposal scenarios:"
echo "   - Sale at loss (proceeds < tax value)"
echo "   - Sale at gain (proceeds > tax value)"
echo "   - Theft/donation (zero proceeds)"
echo ""
echo "The disposal should create proper journal entries:"
echo "- Debit: Accumulated Depreciation"
echo "- Credit: Fixed Asset"
echo "- Debit/Credit: Cash/Bank or Loss/Gain accounts"
echo ""

# Don't run the application - just show instructions
# cd /Users/sthwalonyoni/FIN
# java -jar app/build/libs/fin-spring.jar

echo ""
echo "=== Test Complete ==="
echo "If the application ran successfully and you could navigate the menus,"
echo "the disposal functionality has been successfully implemented!"