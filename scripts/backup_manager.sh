#!/bin/bash
# =============================================================================
# FIN Database Backup - Quick Reference Commands
# =============================================================================

show_help() {
    cat << EOF
FIN Database Backup - Quick Reference

BACKUP MANAGEMENT:
  backup now        Run immediate backup
  backup status     Show backup status and recent files
  backup log        View backup log (last 50 lines)
  backup monitor    Monitor live backup process
  backup verify     Test backup integrity
  backup cleanup    Manually clean old backups (30+ days)

CRON MANAGEMENT:
  backup schedule   Show current backup schedule
  backup enable     Enable automated daily backups
  backup disable    Disable automated backups
  backup test       Run setup and test

RESTORE OPERATIONS:
  backup list       List all available backups
  backup restore    Interactive restore wizard

OFFSITE BACKUP:
  backup offsite    Configure offsite backup location

EXAMPLES:
  ./backup_manager.sh backup now
  ./backup_manager.sh backup status
  ./backup_manager.sh backup restore

EOF
}

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKUP_SCRIPT="$SCRIPT_DIR/backup_database.sh"
BACKUP_DIR="$PROJECT_ROOT/backups"
LOG_FILE="$BACKUP_DIR/backup.log"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

case "${1:-}" in
    backup)
        case "${2:-}" in
            now)
                echo -e "${BLUE}Running manual backup...${NC}"
                "$BACKUP_SCRIPT"
                ;;
            status)
                echo -e "${BLUE}═══ Backup Status ═══${NC}"
                echo ""
                echo "Schedule:"
                crontab -l 2>/dev/null | grep backup_database || echo "  No cron job configured"
                echo ""
                echo "Recent backups:"
                ls -lh "$BACKUP_DIR"/*.dump.gz 2>/dev/null | tail -5 | awk '{print "  " $9, "(" $5 ")"}'
                echo ""
                echo "Disk usage:"
                du -sh "$BACKUP_DIR"
                echo ""
                echo "Last backup result:"
                tail -5 "$LOG_FILE" | grep -E "completed|ERROR" | tail -1
                ;;
            log)
                tail -50 "$LOG_FILE"
                ;;
            monitor)
                echo -e "${YELLOW}Monitoring backup log (Ctrl+C to exit)...${NC}"
                tail -f "$LOG_FILE"
                ;;
            verify)
                echo -e "${BLUE}Verifying latest backup...${NC}"
                LATEST=$(ls -t "$BACKUP_DIR"/*.dump.gz 2>/dev/null | head -1)
                if [ -n "$LATEST" ]; then
                    echo "Latest backup: $LATEST"
                    echo "Size: $(du -h "$LATEST" | cut -f1)"
                    echo "Listing contents (first 20 items):"
                    gunzip -c "$LATEST" | pg_restore --list | head -20
                else
                    echo -e "${RED}No backup files found${NC}"
                fi
                ;;
            cleanup)
                echo -e "${YELLOW}Cleaning up backups older than 30 days...${NC}"
                find "$BACKUP_DIR" -name "drimacc_db_backup_*.dump.gz" -mtime +30 -exec ls -lh {} \;
                find "$BACKUP_DIR" -name "drimacc_db_backup_*.dump.gz" -mtime +30 -delete
                echo -e "${GREEN}Cleanup complete${NC}"
                ;;
            schedule)
                echo -e "${BLUE}Current backup schedule:${NC}"
                crontab -l 2>/dev/null | grep -v "^#" | grep backup_database || echo "No schedule configured"
                ;;
            enable)
                echo -e "${BLUE}Enabling automated backups...${NC}"
                if crontab -l 2>/dev/null | grep -q "$BACKUP_SCRIPT"; then
                    echo -e "${YELLOW}Already enabled${NC}"
                else
                    (crontab -l 2>/dev/null; echo "0 2 * * * $BACKUP_SCRIPT >> $LOG_FILE 2>&1") | crontab -
                    echo -e "${GREEN}Enabled: Daily at 2:00 AM${NC}"
                fi
                ;;
            disable)
                echo -e "${YELLOW}Disabling automated backups...${NC}"
                crontab -l 2>/dev/null | grep -v "$BACKUP_SCRIPT" | crontab -
                echo -e "${GREEN}Disabled${NC}"
                ;;
            test)
                echo -e "${BLUE}Running backup setup and test...${NC}"
                "$SCRIPT_DIR/setup_automated_backups.sh"
                ;;
            list)
                echo -e "${BLUE}Available backups:${NC}"
                ls -lh "$BACKUP_DIR"/*.dump.gz 2>/dev/null | awk '{print $9, "(" $5, $6, $7 ")"}'
                ;;
            restore)
                echo -e "${BLUE}═══ Backup Restore Wizard ═══${NC}"
                echo ""
                BACKUPS=($(ls -t "$BACKUP_DIR"/*.dump.gz 2>/dev/null))
                if [ ${#BACKUPS[@]} -eq 0 ]; then
                    echo -e "${RED}No backup files found${NC}"
                    exit 1
                fi
                
                echo "Available backups:"
                for i in "${!BACKUPS[@]}"; do
                    SIZE=$(du -h "${BACKUPS[$i]}" | cut -f1)
                    echo "  [$i] $(basename "${BACKUPS[$i]}") ($SIZE)"
                done
                echo ""
                read -p "Select backup number: " BACKUP_NUM
                
                if [ -z "${BACKUPS[$BACKUP_NUM]}" ]; then
                    echo -e "${RED}Invalid selection${NC}"
                    exit 1
                fi
                
                BACKUP_FILE="${BACKUPS[$BACKUP_NUM]}"
                echo ""
                echo -e "${YELLOW}WARNING: This will replace all data in drimacc_db!${NC}"
                read -p "Type 'RESTORE' to continue: " CONFIRM
                
                if [ "$CONFIRM" != "RESTORE" ]; then
                    echo "Cancelled"
                    exit 0
                fi
                
                echo ""
                echo "1. Decompressing backup..."
                TEMP_FILE="/tmp/restore_$(date +%s).dump"
                gunzip -c "$BACKUP_FILE" > "$TEMP_FILE"
                
                echo "2. Dropping existing database..."
                dropdb -U sthwalonyoni drimacc_db
                
                echo "3. Creating fresh database..."
                createdb -U sthwalonyoni drimacc_db
                
                echo "4. Restoring backup..."
                pg_restore -U sthwalonyoni -d drimacc_db "$TEMP_FILE"
                
                echo "5. Cleaning up..."
                rm "$TEMP_FILE"
                
                echo ""
                echo -e "${GREEN}✓ Restore complete!${NC}"
                echo "Verifying data..."
                psql -d drimacc_db -c "SELECT COUNT(*) as transaction_count FROM bank_transactions;" -t
                ;;
            offsite)
                echo -e "${BLUE}Offsite Backup Configuration${NC}"
                echo ""
                echo "Edit the backup script to enable offsite backup:"
                echo "  $BACKUP_SCRIPT"
                echo ""
                echo "Options:"
                echo "  1. External Drive: Uncomment rsync line"
                echo "  2. Cloud Storage: Install rclone and uncomment rclone line"
                echo "  3. Remote Server: Uncomment scp line"
                echo ""
                echo "See docs/guides/DATABASE_BACKUP_GUIDE.md for details"
                ;;
            *)
                show_help
                ;;
        esac
        ;;
    *)
        show_help
        ;;
esac
