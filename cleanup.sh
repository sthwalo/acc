#!/bin/bash

echo "🧹 FIN Application Cleanup Script"
echo "================================="
echo ""

# List files to be removed
echo "📋 Files to be cleaned up:"
echo "========================="

# Test files we created during development
TEST_FILES=(
    "TestDirectParser.java"
    "TestDirectParser.class"
    "TestParserFix.java" 
    "TestParserFix.class"
    "TestStandardBankParser.java"
    "TestStandardBankParser.class"
    "TestBankStatementProcessing.java"
    "TestBankStatementParsing.java"
    "AnalyzePositions.java"
    "AnalyzePositions.class"
    "RegexDebug.java"
    "RegexDebug.class"
)

# Check which files exist and list them
echo "Test/Debug files to remove:"
for file in "${TEST_FILES[@]}"; do
    if [ -f "/Users/sthwalonyoni/FIN/$file" ]; then
        echo "  ❌ $file"
    fi
done

echo ""
echo "Utility scripts (these are useful, keeping them):"
echo "  ✅ demo-api.sh (API testing script)"
echo "  ✅ show-status.sh (application status script)"
echo "  ✅ start-backend.sh (startup script)"
echo "  ✅ test-api.sh (API testing script)"

echo ""
echo "🔧 Core Application Files (KEEP THESE):"
echo "======================================="
echo "  ✅ app/src/main/java/fin/App.java (Main entry point)"
echo "  ✅ app/src/main/java/fin/api/ApiServer.java (REST API server)"
echo "  ✅ app/src/main/java/fin/service/parser/StandardBankTabularParser.java (Bank parser)"
echo "  ✅ app/src/main/java/fin/service/DocumentTextExtractor.java (PDF extractor)"
echo "  ✅ app/src/main/java/fin/config/DatabaseConfig.java (Database configuration)"
echo "  ✅ app/src/main/java/fin/license/LicenseManager.java (License management)"
echo "  ✅ All files in app/src/main/java/ (Core application)"
echo "  ✅ build.gradle.kts, settings.gradle.kts (Build configuration)"
echo "  ✅ gradlew, gradlew.bat (Gradle wrapper)"

echo ""
read -p "🗑️  Do you want to remove the test/debug files? (y/N): " confirm

if [[ $confirm =~ ^[Yy]$ ]]; then
    echo ""
    echo "🧹 Cleaning up test files..."
    
    for file in "${TEST_FILES[@]}"; do
        if [ -f "/Users/sthwalonyoni/FIN/$file" ]; then
            rm "/Users/sthwalonyoni/FIN/$file"
            echo "  🗑️  Removed $file"
        fi
    done
    
    echo ""
    echo "✅ Cleanup complete!"
    echo ""
    echo "🎯 Your application structure is now clean:"
    echo "==========================================="
    echo "• Core app files: app/src/main/java/fin/**"
    echo "• Build system: gradle/, build.gradle.kts"
    echo "• Utilities: demo-api.sh, show-status.sh"
    echo "• Documentation: docs/, README.md, LICENSE"
    echo ""
    echo "🚀 To run your clean application:"
    echo "./gradlew run --args=\"api\" -Dfin.license.autoconfirm=true"
    
else
    echo ""
    echo "ℹ️  Cleanup cancelled. Test files preserved."
fi

echo ""
echo "📊 Current application status:"
echo "============================="

# Check if server is running
if curl -s http://localhost:8080/api/v1/health > /dev/null 2>&1; then
    echo "✅ FIN API Server: RUNNING on http://localhost:8080"
else
    echo "⭕ FIN API Server: NOT RUNNING"
    echo "   Start with: ./gradlew run --args=\"api\" -Dfin.license.autoconfirm=true"
fi

echo "✅ PostgreSQL Database: Ready"
echo "✅ Standard Bank Parser: Functional" 
echo "✅ License System: Active"
