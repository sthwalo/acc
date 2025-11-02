#!/bin/bash
# FIN License Protection and Branding Implementation
# Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
# Owner: Immaculate Nyoni

echo "🔒 FIN License and Copyright Protection System"
echo "=============================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Counters
TOTAL_FILES=0
LICENSED_FILES=0
UNLICENSED_FILES=0

# Standard license header to check for
LICENSE_PATTERN="Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd."
OWNER_PATTERN="Owner: Immaculate Nyoni"

echo "📋 Scanning Java files for license headers..."
echo ""

# Find all Java files
for file in $(find app/src -name "*.java"); do
    TOTAL_FILES=$((TOTAL_FILES + 1))
    
    # Check if file has license header
    if head -30 "$file" | grep -q "$LICENSE_PATTERN"; then
        # Verify owner name
        if head -30 "$file" | grep -q "$OWNER_PATTERN"; then
            LICENSED_FILES=$((LICENSED_FILES + 1))
            echo -e "${GREEN}✅${NC} $file"
        else
            UNLICENSED_FILES=$((UNLICENSED_FILES + 1))
            echo -e "${YELLOW}⚠️${NC}  $file (outdated owner)"
        fi
    else
        UNLICENSED_FILES=$((UNLICENSED_FILES + 1))
        echo -e "${RED}❌${NC} $file (missing license)"
    fi
done

echo ""
echo "=============================================="
echo "📊 Summary:"
echo "   Total files: $TOTAL_FILES"
echo -e "   ${GREEN}Licensed: $LICENSED_FILES${NC}"
echo -e "   ${RED}Unlicensed: $UNLICENSED_FILES${NC}"
echo ""

if [ $UNLICENSED_FILES -eq 0 ]; then
    echo -e "${GREEN}🎉 All files are properly licensed!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some files need license headers${NC}"
    echo ""
    echo "To add the standard license header to a file, add the contents of:"
    echo "  config/standard-license-header.txt"
    echo ""
    echo "at the top of each file."
    exit 1
fi
