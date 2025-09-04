#!/bin/bash
cd /Users/sthwalonyoni/FIN

# Process bank statement with automated inputs
(
echo "1"    # Company Setup
echo "2"    # Select existing company
echo "1"    # Select Xinghizana Group
echo "4"    # Back to main menu
echo "2"    # Fiscal Period Management
echo "2"    # Select existing fiscal period
echo "3"    # Select FY2024-2025
echo "4"    # Back to main menu
echo "3"    # Import Bank Statement
echo "1"    # Import single bank statement
echo "bank/xxxxx3753 (14).pdf"
echo "y"    # Export to CSV
echo "10"   # Exit
) | java -jar app/build/libs/app.jar
