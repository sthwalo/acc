#!/bin/bash

# Test script for depreciation journal entry creation
# This script provides the necessary inputs to test depreciation calculation and journal entry creation

echo "Testing Depreciation Journal Entry Creation"
echo "=========================================="

# Create input for the application
cat << 'EOF' | ./gradlew run --console=plain
1
Limelight Academy Institutions
2
FY2025-2026
10
CE
2
4
2
20
5
EOF

echo ""
echo "Test completed. Check the output above for any errors."
echo "If successful, the depreciation schedule should be created with journal entries."