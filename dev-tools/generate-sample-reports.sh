#!/bin/bash

# Generate sample reports to analyze format for Excel/PDF enhancement

cd /Users/sthwalonyoni/FIN

echo "🔍 GENERATING SAMPLE REPORTS FOR ANALYSIS"
echo "========================================"

# Use expect to automate the console app interaction
expect << 'EOF'
spawn ./gradlew run
expect "Enter your choice"
send "1\r"
expect "Enter your choice"
send "1\r"
expect "Enter your choice"
send "6\r"
expect "Enter your choice"
send "1\r"
expect "Enter your choice"
send "2\r"
expect "Enter your choice"
send "3\r"
expect "Enter your choice"
send "7\r"
expect "Enter your choice"
send "10\r"
expect eof
EOF
