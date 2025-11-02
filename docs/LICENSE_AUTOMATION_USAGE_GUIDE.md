# License Header Automation - Usage Guide

**Created:** November 2, 2025  
**Owner:** Immaculate Nyoni  
**Company:** Sthwalo Holdings (Pty) Ltd.  
**Status:** ✅ Ready for Use

---

## 🎯 Overview

This guide explains how to use the automated license header addition tool to add license headers to all 173 unlicensed Java files in the FIN codebase.

**Current Status (from audit):**
```
Total files:     194
Licensed:        21 (10.8%)
Unlicensed:      173 (89.2%)
```

**Goal:** Add license headers to all 173 unlicensed files automatically.

---

## 📋 What the Script Does

The `add-license-headers.sh` script:

1. ✅ **Scans** all Java files in `app/src/` directory
2. ✅ **Identifies** files missing license headers
3. ✅ **Backs up** original files before modification
4. ✅ **Adds** standard license header to each unlicensed file
5. ✅ **Logs** all changes for audit trail
6. ✅ **Reports** summary statistics

**Safety Features:**
- 🔒 Creates backups before any modifications
- 🔒 Skips files that already have license headers
- 🔒 Requires user confirmation before proceeding
- 🔒 Provides rollback instructions if needed
- 🔒 Generates detailed log file

---

## 🚀 Quick Start

### Prerequisites:

1. **Ensure license header template exists:**
   ```bash
   # Check if template file exists
   ls -la LICENSE_HEADER.txt
   # OR
   ls -la config/standard-license-header.txt
   ```

2. **Verify you're in the FIN project root:**
   ```bash
   pwd
   # Should show: /Users/sthwalonyoni/FIN
   ```

3. **Check current license status:**
   ```bash
   ./scripts/check-licenses.sh | tail -10
   # Should show: 173 unlicensed files
   ```

### Running the Script:

```bash
# Step 1: Navigate to FIN project root
cd /Users/sthwalonyoni/FIN

# Step 2: Run the license header automation script
./scripts/add-license-headers.sh

# Step 3: Confirm when prompted
# Type 'yes' and press Enter

# Step 4: Wait for completion (may take 1-2 minutes)

# Step 5: Review the summary report
```

---

## 📖 Detailed Usage Instructions

### Step 1: Pre-Flight Checks

Before running the script, verify everything is ready:

```bash
# Check license header template exists
cat LICENSE_HEADER.txt | head -5
# Should show:
# /*
#  * FIN Financial Management System
#  * 
#  * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
#  * Owner: Immaculate Nyoni

# Check script is executable
ls -la scripts/add-license-headers.sh
# Should show: -rwxr-xr-x (executable permissions)

# Run audit to see current status
./scripts/check-licenses.sh 2>&1 | tail -10
```

---

### Step 2: Run the Script

```bash
./scripts/add-license-headers.sh
```

**Expected Output:**

```
╔════════════════════════════════════════════════════════════╗
║     FIN License Header Automation Tool                     ║
║     Adds license headers to unlicensed Java files          ║
╚════════════════════════════════════════════════════════════╝

📅 Date: 2025-11-02 14:30:45
👤 Owner: Immaculate Nyoni
🏢 Company: Sthwalo Holdings (Pty) Ltd.

✅ License header template found: /Users/sthwalonyoni/FIN/LICENSE_HEADER.txt

⚠️  WARNING: This script will modify 194 Java files
   Backups will be created in: /Users/sthwalonyoni/FIN/.license-backup-20251102_143045

Do you want to proceed? (yes/no):
```

**Type:** `yes` and press Enter

---

### Step 3: Monitor Progress

The script will show real-time progress:

```
✅ Starting license header addition process...

🔍 Scanning for Java files without license headers...

📦 Backup directory created: .license-backup-20251102_143045

📂 Processing files in: /Users/sthwalonyoni/FIN/app/src

🔧 Processing: app/src/main/java/fin/service/PayrollReportService.java
✅ Added license header: app/src/main/java/fin/service/PayrollReportService.java

⏭️  Skipping (already licensed): app/src/main/java/fin/service/InvoicePdfService.java

🔧 Processing: app/src/main/java/fin/service/BudgetReportService.java
✅ Added license header: app/src/main/java/fin/service/BudgetReportService.java

... [continues for all files] ...
```

---

### Step 4: Review Summary

After completion, you'll see a detailed summary:

```
╔════════════════════════════════════════════════════════════╗
║     Processing Complete                                    ║
╚════════════════════════════════════════════════════════════╝

📊 Summary Statistics:
   Total files scanned:       194
   ✅ License headers added:  173
   ⏭️  Already licensed:       21
   ❌ Failed:                  0

📦 Backup Location:
   Original files backed up to: .license-backup-20251102_143045

📝 Log File:
   Detailed log saved to: license-header-additions.log

🚀 Next Steps:
   1. Verify changes: git diff
   2. Test build:     ./gradlew clean build
   3. Run tests:      ./gradlew test
   4. Check licenses: ./scripts/check-licenses.sh
   5. If satisfied, commit: git add . && git commit -m 'feat: add license headers to 173 files'

⚠️  Important: Review changes before committing!

💡 To rollback changes:
   cp -r .license-backup-20251102_143045/app/src/* app/src/
```

---

## ✅ Post-Execution Verification

### 1. Check Git Diff

Review what changed:

```bash
# View summary of changed files
git status

# View detailed changes (sample)
git diff app/src/main/java/fin/service/PayrollReportService.java | head -50

# Count modified files
git status --short | grep "^ M" | wc -l
# Should show: 173
```

---

### 2. Run Build

Ensure everything still compiles:

```bash
./gradlew clean build

# Expected output:
# BUILD SUCCESSFUL in Xs
# 251 actionable tasks: 251 executed
```

**If build fails:**
- Check error messages
- Verify no syntax errors introduced
- Review problematic files
- Use rollback if needed (see below)

---

### 3. Run Tests

Verify functionality is intact:

```bash
./gradlew test

# Expected output:
# BUILD SUCCESSFUL
# Tests: 118 passed, 0 failed, 0 skipped
```

---

### 4. Re-run License Audit

Verify all files now have licenses:

```bash
./scripts/check-licenses.sh 2>&1 | tail -15

# Expected output:
# ╔════════════════════════════════════════════════════════════╗
# ║     All files properly licensed! ✅                        ║
# ╚════════════════════════════════════════════════════════════╝
# 
# 📊 Summary:
#    Total files: 194
#    Licensed: 194
#    Unlicensed: 0
```

---

### 5. Review Log File

Check detailed log of all changes:

```bash
cat license-header-additions.log | head -20

# Shows:
# License Header Addition Log - 2025-11-02 14:30:45
# Owner: Immaculate Nyoni
# Company: Sthwalo Holdings (Pty) Ltd.
# ----------------------------------------
# 
# [2025-11-02 14:30:45] Added license to: app/src/main/java/fin/service/PayrollReportService.java
# [2025-11-02 14:30:45] Added license to: app/src/main/java/fin/service/BudgetReportService.java
# ...
```

---

## 🔄 Rollback Procedure

If something goes wrong, you can rollback all changes:

### Option 1: Using Backup Directory

```bash
# Find your backup directory name
ls -la | grep ".license-backup"

# Restore from backup
cp -r .license-backup-YYYYMMDD_HHMMSS/app/src/* app/src/

# Verify restoration
git status
# Should show no changes
```

---

### Option 2: Using Git

```bash
# Discard all changes
git checkout app/src/

# Or reset to last commit
git reset --hard HEAD

# Verify clean state
git status
# Should show: nothing to commit, working tree clean
```

---

## 🎯 Commit Your Changes

After successful verification:

```bash
# Stage all modified files
git add app/src/

# Create commit
git commit -m "feat: add Apache 2.0 license headers to 173 Java files

- Added standard license header to all previously unlicensed files
- License includes copyright, owner attribution, and Apache 2.0 notice
- Owner: Immaculate Nyoni
- Company: Sthwalo Holdings (Pty) Ltd.
- All files now properly licensed (194/194)

Automated using: scripts/add-license-headers.sh
Backup created: .license-backup-YYYYMMDD_HHMMSS
Log file: license-header-additions.log"

# Push to repository
git push origin main
```

---

## 📊 Expected Results

### Before Script Execution:

```
Total files:     194
Licensed:        21 (10.8%)
Unlicensed:      173 (89.2%)
Status:          🔴 Non-compliant
```

---

### After Script Execution:

```
Total files:     194
Licensed:        194 (100%)
Unlicensed:      0 (0%)
Status:          🟢 Fully Compliant
```

---

## 🛠️ Troubleshooting

### Issue 1: Permission Denied

**Error:**
```
bash: ./scripts/add-license-headers.sh: Permission denied
```

**Solution:**
```bash
chmod +x scripts/add-license-headers.sh
./scripts/add-license-headers.sh
```

---

### Issue 2: License Header File Not Found

**Error:**
```
❌ Error: License header file not found: LICENSE_HEADER.txt
```

**Solution:**
```bash
# Check if file exists
ls -la LICENSE_HEADER.txt
ls -la config/standard-license-header.txt

# If missing, create it from template
cat > LICENSE_HEADER.txt << 'EOF'
/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * [... rest of license text ...]
 */
EOF
```

---

### Issue 3: Build Fails After Adding Headers

**Error:**
```
Compilation failed; see the compiler error output for details.
```

**Solution:**
```bash
# Check which files have compilation errors
./gradlew clean build 2>&1 | grep "error:"

# Common issue: Missing blank line after license header
# Fix: Add blank line between license block and package declaration

# Example fix:
# WRONG:
# */
# package fin.service;

# CORRECT:
# */
#
# package fin.service;
```

**Quick Fix Script:**
```bash
# Add missing blank lines (if needed)
find app/src -name "*.java" -exec sed -i '' 's|^package fin|\\npackage fin|' {} \;
```

---

### Issue 4: Some Files Still Unlicensed

**Error:**
```
Unlicensed: 3 files
```

**Solution:**
```bash
# Find which files are still unlicensed
./scripts/check-licenses.sh 2>&1 | grep "❌"

# Manually add license to those files
# 1. Copy LICENSE_HEADER.txt contents
# 2. Open problematic file
# 3. Paste at top
# 4. Add blank line
# 5. Save
```

---

## 📚 Script Details

### Script Location:
```
/Users/sthwalonyoni/FIN/scripts/add-license-headers.sh
```

### What It Modifies:
- All `.java` files in `app/src/main/java/`
- All `.java` files in `app/src/test/java/`

### What It Doesn't Modify:
- Files that already have license headers
- Non-Java files (.txt, .md, .xml, .gradle, etc.)
- Build artifacts in `app/build/`
- Configuration files

### Backup Location:
```
.license-backup-YYYYMMDD_HHMMSS/
```
(Timestamped directory in project root)

### Log File:
```
license-header-additions.log
```
(Created in project root)

---

## 🔐 Security & Safety

### Backup Strategy:
- ✅ Full backup created before any modifications
- ✅ Timestamped backup directories prevent overwrites
- ✅ Easy rollback with single command
- ✅ Original files preserved until manual cleanup

### Validation:
- ✅ Checks for existing license headers (no duplicates)
- ✅ Verifies license template file exists
- ✅ Requires explicit user confirmation
- ✅ Logs all operations for audit trail

### Error Handling:
- ✅ Exits on error (set -e)
- ✅ Reports failed files separately
- ✅ Non-zero exit code if failures occur
- ✅ Clear error messages with suggested fixes

---

## 📞 Support

**Owner:** Immaculate Nyoni  
**Email:** sthwaloe@gmail.com  
**Phone:** +27 61 514 6185  
**Company:** Sthwalo Holdings (Pty) Ltd.

**Documentation:**
- This guide: `docs/LICENSE_AUTOMATION_USAGE_GUIDE.md`
- Full guide: `docs/LICENSE_PROTECTION_GUIDE.md`
- Quick reference: `docs/LICENSE_QUICK_REFERENCE.md`

---

## 🎉 Success Metrics

After running this script successfully, you will have:

1. ✅ **100% License Compliance** - All 194 files licensed
2. ✅ **Proper Attribution** - Owner: Immaculate Nyoni in all files
3. ✅ **Legal Protection** - Apache 2.0 license in all source files
4. ✅ **Audit Trail** - Complete log of all changes
5. ✅ **Backup Safety** - Ability to rollback if needed
6. ✅ **Build Success** - All files compile correctly
7. ✅ **Test Success** - All tests pass
8. ✅ **Git Ready** - Changes staged and ready to commit

**Expected Time:** 5-10 minutes total (including verification)

---

**Document Version:** 1.0  
**Last Updated:** November 2, 2025  
**Script Version:** 1.0  
**Status:** ✅ Production Ready
