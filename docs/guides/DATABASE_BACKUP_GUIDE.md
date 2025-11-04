# Database Backup Management Guide

## Overview
The FIN application includes automated daily database backups using PostgreSQL's `pg_dump` utility and cron scheduling.

## Current Configuration

### Backup Schedule
- **Frequency**: Daily at 2:00 AM (South African Standard Time)
- **Retention**: 30 days (older backups automatically deleted)
- **Format**: Custom PostgreSQL format (`.dump.gz` - compressed)
- **Location**: `/Users/sthwalonyoni/FIN/backups/`

### What Gets Backed Up
- Full database: `drimacc_db` (PostgreSQL 17)
- All tables, indexes, sequences, and constraints
- Complete transaction history (3,813+ transactions)
- Company data, payroll records, budgets, and financial reports

## Quick Start

### Setup Automated Backups
```bash
cd /Users/sthwalonyoni/FIN/scripts
chmod +x setup_automated_backups.sh
./setup_automated_backups.sh
```

This script will:
1. Verify PostgreSQL installation
2. Test the backup script
3. Configure daily cron job
4. Run a test backup

### Manual Backup
```bash
cd /Users/sthwalonyoni/FIN/scripts
./backup_database.sh
```

### View Backup Status
```bash
# Check recent backups
ls -lh /Users/sthwalonyoni/FIN/backups/*.dump.gz

# View backup log
tail -50 /Users/sthwalonyoni/FIN/backups/backup.log

# Monitor live backup process
tail -f /Users/sthwalonyoni/FIN/backups/backup.log
```

## Management Commands

### Check Cron Schedule
```bash
# View all cron jobs
crontab -l

# Should show:
# 0 2 * * * /Users/sthwalonyoni/FIN/scripts/backup_database.sh >> /Users/sthwalonyoni/FIN/backups/backup.log 2>&1
```

### Modify Backup Schedule
```bash
# Edit crontab
crontab -e

# Cron format: minute hour day month weekday command
# Examples:
# 0 2 * * *    - Daily at 2:00 AM
# 0 */6 * * *  - Every 6 hours
# 0 2 * * 0    - Weekly on Sunday at 2:00 AM
# 0 2 1 * *    - Monthly on the 1st at 2:00 AM
```

### Disable Automated Backups
```bash
# Remove the cron job
crontab -l | grep -v "backup_database.sh" | crontab -

# Or edit and delete the line manually
crontab -e
```

### Change Retention Policy
Edit `/Users/sthwalonyoni/FIN/scripts/backup_database.sh`:
```bash
# Change this line (default 30 days):
RETENTION_DAYS=30

# Options:
# 7  - Keep 1 week
# 14 - Keep 2 weeks
# 30 - Keep 1 month
# 90 - Keep 3 months
```

## Restore from Backup

### List Available Backups
```bash
ls -lh /Users/sthwalonyoni/FIN/backups/*.dump.gz
```

### Restore to New Database (Safe)
```bash
# 1. Decompress backup
gunzip -c /Users/sthwalonyoni/FIN/backups/drimacc_db_backup_YYYYMMDD_HHMMSS.dump.gz > backup.dump

# 2. Create new database
createdb -U sthwalonyoni drimacc_db_restored

# 3. Restore backup
pg_restore -U sthwalonyoni -d drimacc_db_restored backup.dump

# 4. Verify restoration
psql -d drimacc_db_restored -c "SELECT COUNT(*) FROM bank_transactions;"
```

### Restore to Production (Destructive)
```bash
# ⚠️ WARNING: This will replace all data in drimacc_db!

# 1. Stop the application
pkill -f "java.*fin"

# 2. Drop existing database
dropdb -U sthwalonyoni drimacc_db

# 3. Create fresh database
createdb -U sthwalonyoni drimacc_db

# 4. Decompress and restore
gunzip -c /Users/sthwalonyoni/FIN/backups/drimacc_db_backup_YYYYMMDD_HHMMSS.dump.gz | \
    pg_restore -U sthwalonyoni -d drimacc_db

# 5. Verify restoration
psql -d drimacc_db -c "\dt"
```

## Offsite Backup (Recommended)

### Option 1: External Drive
Edit `backup_database.sh` and uncomment:
```bash
rsync -av "$COMPRESSED_FILE" /Volumes/ExternalDrive/FIN_backups/
```

### Option 2: Cloud Storage (Google Drive, Dropbox, etc.)
```bash
# Install rclone
brew install rclone

# Configure remote (follow prompts)
rclone config

# Edit backup_database.sh and uncomment:
rclone copy "$COMPRESSED_FILE" remote:FIN_backups/
```

### Option 3: Remote Server
```bash
# Edit backup_database.sh and uncomment:
scp "$COMPRESSED_FILE" user@remote-server:/path/to/backups/
```

### Option 4: Time Machine (macOS)
Ensure the `/Users/sthwalonyoni/FIN/backups/` folder is included in Time Machine backups:
- System Preferences → Time Machine → Options
- Remove `/Users/sthwalonyoni/FIN/backups` from exclusion list if present

## Monitoring and Alerts

### Check Backup Success
```bash
# View recent backup results
tail -100 /Users/sthwalonyoni/FIN/backups/backup.log | grep -E "completed|ERROR"

# Check if backup ran today
ls -l /Users/sthwalonyoni/FIN/backups/*.dump.gz | grep "$(date +%Y-%m-%d)"
```

### Email Notifications (Optional)
Add to `backup_database.sh` after successful backup:
```bash
# Install mailutils or use macOS mail command
if [ -f "$COMPRESSED_FILE" ]; then
    echo "Backup successful: $COMPRESSED_FILE ($BACKUP_SIZE)" | \
        mail -s "FIN Database Backup Success" your-email@example.com
else
    echo "Backup failed! Check logs." | \
        mail -s "FIN Database Backup FAILED" your-email@example.com
fi
```

### Slack/Discord Notifications (Optional)
```bash
# After successful backup
curl -X POST -H 'Content-type: application/json' \
    --data "{\"text\":\"✅ FIN backup completed: $COMPRESSED_FILE ($BACKUP_SIZE)\"}" \
    YOUR_WEBHOOK_URL
```

## Troubleshooting

### Issue: "pg_dump: command not found"
**Solution**: The script now uses full path to pg_dump. If you see this error, update the path in `backup_database.sh`:
```bash
PG_DUMP="/opt/homebrew/opt/postgresql@17/bin/pg_dump"
```

### Issue: Cron job not running
**Checks**:
1. Verify cron job exists: `crontab -l`
2. Check system time: `date`
3. Ensure cron has Full Disk Access (macOS):
   - System Preferences → Security & Privacy → Privacy → Full Disk Access
   - Add Terminal or `/usr/sbin/cron`

### Issue: Backup file not created
**Checks**:
1. Database credentials: Edit `.env` file
2. PostgreSQL running: `psql -d drimacc_db -c "SELECT 1;"`
3. Disk space: `df -h /Users/sthwalonyoni/FIN/backups`
4. Permissions: `ls -la /Users/sthwalonyoni/FIN/backups`

### Issue: Large backup files
**Solutions**:
- Backups are compressed with gzip (reduces size 80-90%)
- Clean up old test data: `DELETE FROM table WHERE is_test = true;`
- Increase retention policy to fewer days
- Consider incremental backups for very large databases

## Backup File Information

### File Naming Convention
```
drimacc_db_backup_YYYY-MM-DD_HHMMSS.dump.gz
Example: drimacc_db_backup_2025-11-04_020001.dump.gz
```

### Expected File Sizes
- Small dataset (< 1,000 transactions): ~100 KB
- Medium dataset (1,000-10,000 transactions): ~500 KB - 2 MB
- Large dataset (> 10,000 transactions): > 2 MB

### Backup Contents
- Schema: All table structures, indexes, constraints
- Data: All transaction records, companies, employees, payroll
- Sequences: Current values for auto-increment fields
- Permissions: Database roles and grants

## Security Best Practices

1. **Encrypt Backups**: Use encrypted external drives or cloud storage
2. **Secure Credentials**: Store database password in `.env` (already in `.gitignore`)
3. **Test Restoration**: Monthly test restore to verify backup integrity
4. **Multiple Locations**: Keep at least 3 copies (local, external, cloud)
5. **Access Control**: Restrict backup directory permissions:
   ```bash
   chmod 700 /Users/sthwalonyoni/FIN/backups
   ```

## Backup Verification

### Test Backup Integrity
```bash
# Decompress and verify structure
gunzip -c backup_file.dump.gz | pg_restore --list | head -20

# Should show table definitions and data
```

### Verify Data Completeness
```bash
# After restoration, compare counts
psql -d drimacc_db_restored -c "
SELECT 
    'bank_transactions' as table_name, COUNT(*) as count 
FROM bank_transactions
UNION ALL
SELECT 'companies', COUNT(*) FROM companies
UNION ALL
SELECT 'payslips', COUNT(*) FROM payslips;"
```

## Maintenance

### Monthly Tasks
- [ ] Review backup log for errors
- [ ] Test restore one backup file
- [ ] Verify offsite backup sync
- [ ] Check disk space in backup directory

### Quarterly Tasks
- [ ] Full restore test to temporary database
- [ ] Review and adjust retention policy
- [ ] Update backup script if PostgreSQL upgraded
- [ ] Audit backup access logs

## Support

For issues or questions:
1. Check logs: `/Users/sthwalonyoni/FIN/backups/backup.log`
2. Review this guide: `/Users/sthwalonyoni/FIN/docs/guides/DATABASE_BACKUP_GUIDE.md`
3. PostgreSQL documentation: https://www.postgresql.org/docs/17/backup.html

---

**Last Updated**: November 4, 2025  
**PostgreSQL Version**: 17  
**Backup Format**: Custom (pg_dump -Fc)
