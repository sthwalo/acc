#!/bin/bash

# Fix Outdated License Headers Script
# Updates 6 files with incomplete license headers to the full standard format
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

# Check if license header file exists
if [ ! -f "$LICENSE_HEADER" ]; then
    LICENSE_HEADER="$PROJECT_ROOT/config/standard-license-header.txt"
    if [ ! -f "$LICENSE_HEADER" ]; then
        echo -e "${RED}❌ Error: License header file not found!${NC}"
        echo "   Expected location: $PROJECT_ROOT/LICENSE_HEADER.txt"
        echo "   Or: $PROJECT_ROOT/config/standard-license-header.txt"
        exit 1
    fi
fi

# Backup directory
BACKUP_DIR="$PROJECT_ROOT/.license-fix-backup-$(date +%Y%m%d_%H%M%S)"

# Log file
LOG_FILE="$PROJECT_ROOT/license-header-fixes.log"

# List of files to fix
FILES_TO_FIX=(
    "app/src/main/java/fin/repository/UserRepository.java"
    "app/src/main/java/fin/security/AuthService.java"
    "app/src/main/java/fin/model/Payslip.java"
    "app/src/main/java/fin/model/User.java"
    "app/src/main/java/fin/model/PayrollPeriod.java"
    "app/src/main/java/fin/service/PayrollService.java"
)

# Counters
PROCESSED_FILES=0
FAILED_FILES=0

# Arrays to track results
declare -a processed_files
declare -a failed_files

echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Fix Outdated License Headers                          ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}📋 Files to fix: ${#FILES_TO_FIX[@]}${NC}"
echo -e "${BLUE}📄 License template: $(basename "$LICENSE_HEADER")${NC}"
echo ""

# Function to backup a file
backup_file() {
    local file_path="$1"
    local backup_path="$BACKUP_DIR/$file_path"
    
    # Create directory structure
    mkdir -p "$(dirname "$backup_path")"
    
    # Copy original file
    cp "$file_path" "$backup_path"
}

# Function to check if file has old license header
has_old_header() {
    local file_path="$1"
    
    # Check if file starts with comment block and has copyright but missing owner
    if head -20 "$file_path" | grep -q "Copyright (c) 2024-2025 Sthwalo Holdings"; then
        if ! head -30 "$file_path" | grep -q "Owner: Immaculate Nyoni"; then
            return 0  # Has old header
        fi
    fi
    return 1  # Doesn't have old header
}

# Function to find where old header ends
find_header_end() {
    local file_path="$1"
    local line_num=1
    local in_comment=false
    
    while IFS= read -r line; do
        if [[ "$line" =~ ^/\* ]]; then
            in_comment=true
        fi
        
        if [[ "$in_comment" == true ]] && [[ "$line" =~ \*/ ]]; then
            echo $((line_num + 1))  # Return line after closing */
            return 0
        fi
        
        ((line_num++))
        
        # Safety: don't search beyond line 20
        if [ $line_num -gt 20 ]; then
            echo "1"
            return 1
        fi
    done < "$file_path"
    
    echo "1"
    return 1
}

# Function to fix license header
fix_license_header() {
    local file_path="$1"
    local temp_file="${file_path}.tmp"
    
    echo -e "${YELLOW}🔧 Processing: $file_path${NC}"
    
    # Find where old header ends
    local header_end_line=$(find_header_end "$file_path")
    
    if [ "$header_end_line" -eq 1 ]; then
        echo -e "${RED}   ❌ Could not find header end${NC}"
        return 1
    fi
    
    # Create new file with correct header
    {
        # Add new license header
        cat "$LICENSE_HEADER"
        echo ""
        
        # Add rest of file (skip old header)
        tail -n +$header_end_line "$file_path"
    } > "$temp_file"
    
    # Replace original file
    mv "$temp_file" "$file_path"
    
    return 0
}

# Function to log operation
log_operation() {
    local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[$timestamp] $1" >> "$LOG_FILE"
}

# Main processing
echo -e "${BLUE}🔍 Checking files...${NC}"
echo ""

# User confirmation
echo -e "${YELLOW}⚠️  This will replace the license headers in 6 files.${NC}"
echo -e "${YELLOW}   A backup will be created at: $(basename "$BACKUP_DIR")/${NC}"
echo ""
read -p "Continue? (yes/no): " -r
echo ""

if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo -e "${RED}❌ Operation cancelled${NC}"
    exit 0
fi

# Create backup directory
echo -e "${CYAN}📦 Creating backup...${NC}"
mkdir -p "$BACKUP_DIR"
log_operation "Backup directory created: $BACKUP_DIR"

# Process each file
for file_path in "${FILES_TO_FIX[@]}"; do
    full_path="$PROJECT_ROOT/$file_path"
    
    if [ ! -f "$full_path" ]; then
        echo -e "${RED}❌ File not found: $file_path${NC}"
        ((FAILED_FILES++))
        failed_files+=("$file_path (not found)")
        log_operation "FAILED: $file_path (file not found)"
        continue
    fi
    
    # Check if file has old header
    if ! has_old_header "$full_path"; then
        echo -e "${BLUE}⏭️  Skipped: $file_path (already updated or no old header)${NC}"
        log_operation "SKIPPED: $file_path (already updated)"
        continue
    fi
    
    # Backup file
    backup_file "$file_path"
    log_operation "BACKUP: $file_path"
    
    # Fix license header
    if fix_license_header "$full_path"; then
        echo -e "${GREEN}✅ Fixed: $file_path${NC}"
        ((PROCESSED_FILES++))
        processed_files+=("$file_path")
        log_operation "SUCCESS: $file_path"
    else
        echo -e "${RED}❌ Failed: $file_path${NC}"
        ((FAILED_FILES++))
        failed_files+=("$file_path")
        log_operation "FAILED: $file_path"
    fi
    
    echo ""
done

# Summary
echo ""
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     Processing Complete                                    ║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}📊 Summary Statistics:${NC}"
echo -e "   Total files processed: ${#FILES_TO_FIX[@]}"
echo -e "   ${GREEN}✅ Fixed successfully:  $PROCESSED_FILES${NC}"
echo -e "   ${RED}❌ Failed:              $FAILED_FILES${NC}"
echo ""

if [ $PROCESSED_FILES -gt 0 ]; then
    echo -e "${GREEN}✅ Fixed files:${NC}"
    for file in "${processed_files[@]}"; do
        echo -e "   • $file"
    done
    echo ""
fi

if [ $FAILED_FILES -gt 0 ]; then
    echo -e "${RED}❌ Failed files:${NC}"
    for file in "${failed_files[@]}"; do
        echo -e "   • $file"
    done
    echo ""
fi

echo -e "${BLUE}📦 Backup Location:${NC}"
echo -e "   Original files backed up to: $BACKUP_DIR"
echo ""

echo -e "${BLUE}📝 Log File:${NC}"
echo -e "   Detailed log saved to: $LOG_FILE"
echo ""

echo -e "${CYAN}🚀 Next Steps:${NC}"
echo -e "   1. Verify changes: ${YELLOW}git diff${NC}"
echo -e "   2. Test build:     ${YELLOW}./gradlew clean build${NC}"
echo -e "   3. Check licenses: ${YELLOW}./scripts/check-licenses.sh${NC}"
echo -e "   4. If satisfied, commit: ${YELLOW}git add . && git commit -m 'fix: update 6 outdated license headers'${NC}"
echo ""

echo -e "${MAGENTA}⚠️  Important: Review changes before committing!${NC}"
echo ""

echo -e "${BLUE}💡 To rollback changes:${NC}"
echo -e "   ${YELLOW}cp -r $BACKUP_DIR/app/src/* $PROJECT_ROOT/app/src/${NC}"
echo ""

if [ $FAILED_FILES -eq 0 ]; then
    echo -e "${GREEN}✨ All files fixed successfully!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  Some files had issues. Check the log for details.${NC}"
    exit 1
fi
