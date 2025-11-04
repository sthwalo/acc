#!/bin/bash

# =============================================================================
# FIN Database Backup Setup Script
# Configures automated daily backups with cron on macOS
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_SCRIPT="$SCRIPT_DIR/backup_database.sh"
BACKUP_DIR="$PROJECT_ROOT/backups"
LOG_FILE="$BACKUP_DIR/backup.log"

echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}FIN Database Backup - Automated Setup${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Step 1: Verify PostgreSQL installation
echo -e "${YELLOW}[1/6]${NC} Verifying PostgreSQL installation..."
PG_DUMP=$(which pg_dump 2>/dev/null || find /opt/homebrew -name pg_dump -type f 2>/dev/null | head -1 || echo "")
if [ -z "$PG_DUMP" ]; then
    echo -e "${RED}✗ PostgreSQL pg_dump not found!${NC}"
    echo "Please install PostgreSQL 17 or update the path in backup_database.sh"
    exit 1
fi
echo -e "${GREEN}✓ PostgreSQL found: $PG_DUMP${NC}"

# Step 2: Verify backup script exists and is executable
echo -e "${YELLOW}[2/6]${NC} Checking backup script..."
if [ ! -f "$BACKUP_SCRIPT" ]; then
    echo -e "${RED}✗ Backup script not found at: $BACKUP_SCRIPT${NC}"
    exit 1
fi
chmod +x "$BACKUP_SCRIPT"
echo -e "${GREEN}✓ Backup script is executable${NC}"

# Step 3: Create backup directory
echo -e "${YELLOW}[3/6]${NC} Setting up backup directory..."
mkdir -p "$BACKUP_DIR"
echo -e "${GREEN}✓ Backup directory ready: $BACKUP_DIR${NC}"

# Step 4: Test backup script
echo -e "${YELLOW}[4/6]${NC} Testing backup script..."
echo "Running a test backup (this may take a moment)..."
if "$BACKUP_SCRIPT" >> "$LOG_FILE" 2>&1; then
    echo -e "${GREEN}✓ Test backup successful!${NC}"
    # Show the latest backup file
    LATEST_BACKUP=$(ls -t "$BACKUP_DIR"/*.dump.gz 2>/dev/null | head -1)
    if [ -n "$LATEST_BACKUP" ]; then
        BACKUP_SIZE=$(du -h "$LATEST_BACKUP" | cut -f1)
        echo "  Latest backup: $(basename "$LATEST_BACKUP") ($BACKUP_SIZE)"
    fi
else
    echo -e "${RED}✗ Test backup failed!${NC}"
    echo "Check the log for errors: $LOG_FILE"
    tail -20 "$LOG_FILE"
    exit 1
fi

# Step 5: Configure cron job
echo -e "${YELLOW}[5/6]${NC} Configuring automated daily backups..."

# Backup current crontab
CRON_BACKUP="$BACKUP_DIR/crontab_backup_$(date +%Y%m%d_%H%M%S).txt"
crontab -l > "$CRON_BACKUP" 2>/dev/null || echo "" > "$CRON_BACKUP"

# Check if cron job already exists
if crontab -l 2>/dev/null | grep -q "$BACKUP_SCRIPT"; then
    echo -e "${YELLOW}⚠ Cron job already exists. Updating...${NC}"
    # Remove old entry
    crontab -l 2>/dev/null | grep -v "$BACKUP_SCRIPT" | crontab -
fi

# Add new cron job (daily at 2:00 AM)
(crontab -l 2>/dev/null; echo "0 2 * * * $BACKUP_SCRIPT >> $LOG_FILE 2>&1") | crontab -

echo -e "${GREEN}✓ Cron job configured (daily at 2:00 AM)${NC}"
echo "  Crontab backup saved: $CRON_BACKUP"

# Step 6: Summary and next steps
echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Automated Backup Setup Complete!${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo "Configuration Summary:"
echo "  • Backup Schedule: Daily at 2:00 AM"
echo "  • Backup Location: $BACKUP_DIR"
echo "  • Retention Policy: 30 days"
echo "  • Log File: $LOG_FILE"
echo ""
echo "Management Commands:"
echo "  • View current schedule:   crontab -l"
echo "  • Check backup log:        tail -f $LOG_FILE"
echo "  • Manual backup:           $BACKUP_SCRIPT"
echo "  • List backups:            ls -lh $BACKUP_DIR/*.dump.gz"
echo "  • Remove cron job:         crontab -e (then delete the line)"
echo ""
echo "Next Steps:"
echo "  1. Configure offsite backup in $BACKUP_SCRIPT (optional)"
echo "  2. Test restoration: pg_restore -d testdb backup_file.dump"
echo "  3. Monitor logs regularly to ensure backups succeed"
echo ""
echo -e "${YELLOW}Note:${NC} On macOS, ensure Terminal/cron has Full Disk Access in:"
echo "  System Preferences → Security & Privacy → Privacy → Full Disk Access"
echo ""
