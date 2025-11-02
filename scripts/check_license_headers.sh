#!/bin/bash

# License Header Checker for FIN Financial Management System
# 
# This script checks all Java source files for proper license headers
# and reports files that are missing or have incorrect headers.

# Expected copyright holder
EXPECTED_HOLDER="Immaculate Nyoni"
EXPECTED_COMPANY="Sthwalo Holdings (Pty) Ltd"
EXPECTED_CONTACT="sthwaloe@gmail.com"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     FIN License Header Checker                             ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo ""

# Find the project root (parent of scripts directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo -e "${BLUE}📂 Project root: ${PROJECT_ROOT}${NC}"
echo ""

# Counters
TOTAL_FILES=0
MISSING_COPYRIGHT=0
WRONG_HOLDER=0
MISSING_CONTACT=0
CORRECT_FILES=0

# Arrays to store problematic files
declare -a missing_copyright_files
declare -a wrong_holder_files
declare -a missing_contact_files

# Check Java files in app/src directory
echo -e "${BLUE}🔍 Scanning Java source files...${NC}"
echo ""

while IFS= read -r -d '' file; do
    ((TOTAL_FILES++))
    
    # Read first 30 lines (should contain header)
    header=$(head -n 30 "$file")
    
    has_issues=false
    
    # Check for copyright notice
    if ! echo "$header" | grep -q "Copyright"; then
        missing_copyright_files+=("$file")
        ((MISSING_COPYRIGHT++))
        has_issues=true
    else
        # Check for correct copyright holder
        if ! echo "$header" | grep -q "$EXPECTED_HOLDER"; then
            wrong_holder_files+=("$file")
            ((WRONG_HOLDER++))
            has_issues=true
        fi
    fi
    
    # Check for contact information
    if ! echo "$header" | grep -q "$EXPECTED_CONTACT"; then
        missing_contact_files+=("$file")
        ((MISSING_CONTACT++))
        has_issues=true
    fi
    
    if [ "$has_issues" = false ]; then
        ((CORRECT_FILES++))
    fi
    
done < <(find "$PROJECT_ROOT/app/src" -name "*.java" -print0)

# Print results
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}📊 RESULTS SUMMARY${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "Total Java files scanned: ${BLUE}$TOTAL_FILES${NC}"
echo -e "Files with correct headers: ${GREEN}$CORRECT_FILES${NC}"
echo -e "Files missing copyright: ${RED}$MISSING_COPYRIGHT${NC}"
echo -e "Files with wrong copyright holder: ${YELLOW}$WRONG_HOLDER${NC}"
echo -e "Files missing contact info: ${YELLOW}$MISSING_CONTACT${NC}"
echo ""

# Report missing copyright files
if [ $MISSING_COPYRIGHT -gt 0 ]; then
    echo -e "${RED}❌ FILES MISSING COPYRIGHT NOTICE:${NC}"
    for file in "${missing_copyright_files[@]}"; do
        echo -e "   ${RED}•${NC} ${file#$PROJECT_ROOT/}"
    done
    echo ""
fi

# Report wrong holder files
if [ $WRONG_HOLDER -gt 0 ]; then
    echo -e "${YELLOW}⚠️  FILES WITH INCORRECT COPYRIGHT HOLDER:${NC}"
    echo -e "${YELLOW}   Expected: $EXPECTED_HOLDER${NC}"
    for file in "${wrong_holder_files[@]}"; do
        echo -e "   ${YELLOW}•${NC} ${file#$PROJECT_ROOT/}"
    done
    echo ""
fi

# Report missing contact files
if [ $MISSING_CONTACT -gt 0 ]; then
    echo -e "${YELLOW}⚠️  FILES MISSING CONTACT INFORMATION:${NC}"
    echo -e "${YELLOW}   Expected: $EXPECTED_CONTACT${NC}"
    for file in "${missing_contact_files[@]}"; do
        echo -e "   ${YELLOW}•${NC} ${file#$PROJECT_ROOT/}"
    done
    echo ""
fi

# Final status
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
if [ $CORRECT_FILES -eq $TOTAL_FILES ]; then
    echo -e "${GREEN}✅ ALL FILES HAVE CORRECT LICENSE HEADERS!${NC}"
    exit 0
else
    compliance_percentage=$((CORRECT_FILES * 100 / TOTAL_FILES))
    echo -e "${YELLOW}⚠️  LICENSE HEADER COMPLIANCE: ${compliance_percentage}%${NC}"
    echo -e "${YELLOW}   Please update the flagged files with proper headers.${NC}"
    exit 1
fi
