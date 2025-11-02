#!/bin/bash

# License Header Automation Script for FIN Financial Management System
# 
# This script automatically adds the standard license header to all Java files
# that are currently missing proper license headers.
# 
# Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
# Owner: Immaculate Nyoni

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color

# Find the project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# License header file
LICENSE_HEADER="$PROJECT_ROOT/LICENSE_HEADER.txt"

# Backup directory
BACKUP_DIR="$PROJECT_ROOT/.license-backup-$(date +%Y%m%d_%H%M%S)"

# Log file
LOG_FILE="$PROJECT_ROOT/license-header-additions.log"

# Counters
TOTAL_FILES=0
PROCESSED_FILES=0
SKIPPED_FILES=0
FAILED_FILES=0

# Arrays to store file paths
declare -a processed_files
declare -a skipped_files
declare -a failed_files

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     FIN License Header Automation Tool                     ║${NC}"
echo -e "${BLUE}║     Adds license headers to unlicensed Java files          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${CYAN}📅 Date: $(date '+%Y-%m-%d %H:%M:%S')${NC}"
echo -e "${CYAN}👤 Owner: Immaculate Nyoni${NC}"
echo -e "${CYAN}🏢 Company: Sthwalo Holdings (Pty) Ltd.${NC}"
echo ""

# Check if license header file exists
if [ ! -f "$LICENSE_HEADER" ]; then
    echo -e "${RED}❌ Error: License header file not found: $LICENSE_HEADER${NC}"
    echo ""
    echo -e "${YELLOW}Looking for alternative locations...${NC}"
    
    # Try alternative location
    if [ -f "$PROJECT_ROOT/config/standard-license-header.txt" ]; then
        LICENSE_HEADER="$PROJECT_ROOT/config/standard-license-header.txt"
        echo -e "${GREEN}✅ Found license header at: $LICENSE_HEADER${NC}"
    else
        echo -e "${RED}❌ No license header file found. Please create one at:${NC}"
        echo -e "${RED}   $PROJECT_ROOT/LICENSE_HEADER.txt${NC}"
        echo -e "${RED}   OR${NC}"
        echo -e "${RED}   $PROJECT_ROOT/config/standard-license-header.txt${NC}"
        exit 1
    fi
fi

echo -e "${GREEN}✅ License header template found: $LICENSE_HEADER${NC}"
echo ""

# Function to check if file already has a license header
has_license_header() {
    local file="$1"
    
    # Check for copyright notice in first 30 lines
    if head -n 30 "$file" | grep -q "Copyright (c) 2024-2025 Sthwalo Holdings"; then
        return 0  # Has license
    fi
    
    return 1  # No license
}

# Function to backup file
backup_file() {
    local file="$1"
    local relative_path="${file#$PROJECT_ROOT/}"
    local backup_path="$BACKUP_DIR/$relative_path"
    
    # Create backup directory structure
    mkdir -p "$(dirname "$backup_path")"
    
    # Copy original file to backup
    cp "$file" "$backup_path"
}

# Function to add license header to file
add_license_header() {
    local file="$1"
    local temp_file="${file}.tmp"
    
    # Create temporary file with license header + blank line + original content
    cat "$LICENSE_HEADER" > "$temp_file"
    echo "" >> "$temp_file"
    cat "$file" >> "$temp_file"
    
    # Replace original file with new content
    mv "$temp_file" "$file"
}

# Function to process a single file
process_file() {
    local file="$1"
    
    ((TOTAL_FILES++))
    
    # Check if file already has license
    if has_license_header "$file"; then
        echo -e "${YELLOW}⏭️  Skipping (already licensed): ${file#$PROJECT_ROOT/}${NC}"
        ((SKIPPED_FILES++))
        skipped_files+=("$file")
        return 0
    fi
    
    echo -e "${CYAN}🔧 Processing: ${file#$PROJECT_ROOT/}${NC}"
    
    # Backup original file
    backup_file "$file"
    
    # Add license header
    if add_license_header "$file"; then
        echo -e "${GREEN}✅ Added license header: ${file#$PROJECT_ROOT/}${NC}"
        ((PROCESSED_FILES++))
        processed_files+=("$file")
        
        # Log the change
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] Added license to: $file" >> "$LOG_FILE"
    else
        echo -e "${RED}❌ Failed to add license: ${file#$PROJECT_ROOT/}${NC}"
        ((FAILED_FILES++))
        failed_files+=("$file")
        
        # Log the failure
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] FAILED to add license to: $file" >> "$LOG_FILE"
    fi
    
    echo ""
}

# Main processing function
main() {
    echo -e "${BLUE}🔍 Scanning for Java files without license headers...${NC}"
    echo ""
    
    # Create backup directory
    mkdir -p "$BACKUP_DIR"
    echo -e "${GREEN}📦 Backup directory created: $BACKUP_DIR${NC}"
    echo ""
    
    # Initialize log file
    echo "License Header Addition Log - $(date '+%Y-%m-%d %H:%M:%S')" > "$LOG_FILE"
    echo "Owner: Immaculate Nyoni" >> "$LOG_FILE"
    echo "Company: Sthwalo Holdings (Pty) Ltd." >> "$LOG_FILE"
    echo "----------------------------------------" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    
    # Find all Java files in app/src directory
    echo -e "${BLUE}📂 Processing files in: $PROJECT_ROOT/app/src${NC}"
    echo ""
    
    # Process each Java file
    while IFS= read -r -d '' file; do
        process_file "$file"
    done < <(find "$PROJECT_ROOT/app/src" -type f -name "*.java" -print0)
    
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║     Processing Complete                                    ║${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo -e "${CYAN}📊 Summary Statistics:${NC}"
    echo -e "${CYAN}   Total files scanned:       ${TOTAL_FILES}${NC}"
    echo -e "${GREEN}   ✅ License headers added:  ${PROCESSED_FILES}${NC}"
    echo -e "${YELLOW}   ⏭️  Already licensed:       ${SKIPPED_FILES}${NC}"
    echo -e "${RED}   ❌ Failed:                  ${FAILED_FILES}${NC}"
    echo ""
    
    # Show backup location
    echo -e "${MAGENTA}📦 Backup Location:${NC}"
    echo -e "${MAGENTA}   Original files backed up to: $BACKUP_DIR${NC}"
    echo ""
    
    # Show log file location
    echo -e "${MAGENTA}📝 Log File:${NC}"
    echo -e "${MAGENTA}   Detailed log saved to: $LOG_FILE${NC}"
    echo ""
    
    # Show next steps
    if [ $PROCESSED_FILES -gt 0 ]; then
        echo -e "${BLUE}🚀 Next Steps:${NC}"
        echo -e "${BLUE}   1. Verify changes: ${YELLOW}git diff${NC}"
        echo -e "${BLUE}   2. Test build:     ${YELLOW}./gradlew clean build${NC}"
        echo -e "${BLUE}   3. Run tests:      ${YELLOW}./gradlew test${NC}"
        echo -e "${BLUE}   4. Check licenses: ${YELLOW}./scripts/check-licenses.sh${NC}"
        echo -e "${BLUE}   5. If satisfied, commit: ${YELLOW}git add . && git commit -m 'feat: add license headers to ${PROCESSED_FILES} files'${NC}"
        echo ""
        echo -e "${YELLOW}⚠️  Important: Review changes before committing!${NC}"
        echo ""
        
        # Option to rollback
        echo -e "${MAGENTA}💡 To rollback changes:${NC}"
        echo -e "${MAGENTA}   cp -r $BACKUP_DIR/app/src/* $PROJECT_ROOT/app/src/${NC}"
        echo ""
    fi
    
    # Show failed files if any
    if [ $FAILED_FILES -gt 0 ]; then
        echo -e "${RED}❌ Failed Files (${FAILED_FILES}):${NC}"
        for file in "${failed_files[@]}"; do
            echo -e "${RED}   - ${file#$PROJECT_ROOT/}${NC}"
        done
        echo ""
        echo -e "${YELLOW}⚠️  Please review these files manually${NC}"
        echo ""
    fi
    
    # Exit with appropriate code
    if [ $FAILED_FILES -gt 0 ]; then
        exit 1
    else
        exit 0
    fi
}

# Confirmation prompt
echo -e "${YELLOW}⚠️  WARNING: This script will modify ${TOTAL_FILES} Java files${NC}"
echo -e "${YELLOW}   Backups will be created in: $BACKUP_DIR${NC}"
echo ""
echo -e "${CYAN}Do you want to proceed? (yes/no): ${NC}"
read -r response

if [ "$response" != "yes" ] && [ "$response" != "y" ] && [ "$response" != "YES" ] && [ "$response" != "Y" ]; then
    echo -e "${RED}❌ Operation cancelled by user${NC}"
    exit 0
fi

echo ""
echo -e "${GREEN}✅ Starting license header addition process...${NC}"
echo ""

# Run main processing
main
