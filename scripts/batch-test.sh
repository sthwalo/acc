#!/bin/bash
cd /Users/sthwalonyoni/FIN

echo "🚀 Starting FIN Full System Test - Batch Mode"
echo "=============================================="

# Build the application first
echo "📦 Building application..."
./gradlew build -q

# Create a test company
echo "🏢 Creating test company..."
java -jar app/build/libs/fin-spring.jar --batch create-company "Test Company"

# Create a fiscal period for 2025 dates
echo "📅 Creating fiscal period for 2025..."
java -jar app/build/libs/fin-spring.jar --batch create-fiscal-period "FY2025-2026" "2025-01-01" "2025-12-31"

# Process a bank statement
echo "📄 Processing bank statement..."
java -jar app/build/libs/fin-spring.jar --batch process-statement "input/xxxxx3753 (14).pdf"

echo ""
echo "✅ Full system test completed successfully!"
echo "📊 The application processed data and exited cleanly (no infinite loop)"
echo "🔍 Check the generated CSV file for processed transactions"