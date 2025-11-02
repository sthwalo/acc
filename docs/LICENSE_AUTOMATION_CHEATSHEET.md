# License Header Automation - Quick Cheat Sheet

**Owner:** Immaculate Nyoni | **Date:** Nov 2, 2025 | **Goal:** Add license headers to 173 files

---

## 🚀 Quick Start (3 Commands)

```bash
# 1. Navigate to project
cd /Users/sthwalonyoni/FIN

# 2. Run the automation script
./scripts/add-license-headers.sh

# 3. Type 'yes' when prompted, then wait for completion
```

---

## ✅ Verification Steps

```bash
# 1. Check build
./gradlew clean build

# 2. Run tests
./gradlew test

# 3. Verify 100% licensed
./scripts/check-licenses.sh | tail -10

# 4. Review changes
git diff --stat
```

---

## 📊 Expected Results

**Before:**
```
Total: 194 | Licensed: 21 | Unlicensed: 173
```

**After:**
```
Total: 194 | Licensed: 194 | Unlicensed: 0 ✅
```

---

## 🔄 If Something Goes Wrong

```bash
# Rollback using backup
cp -r .license-backup-*/app/src/* app/src/

# OR rollback using git
git checkout app/src/
```

---

## 💾 Commit Changes

```bash
git add app/src/
git commit -m "feat: add license headers to 173 Java files"
git push origin main
```

---

## 📁 Files Created

- **Backup:** `.license-backup-YYYYMMDD_HHMMSS/` (can delete after commit)
- **Log:** `license-header-additions.log` (keep for audit)

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| Permission denied | `chmod +x scripts/add-license-headers.sh` |
| Build fails | Check for missing blank line after `*/` |
| Template not found | Ensure `LICENSE_HEADER.txt` exists |

---

## 📞 Help

**Full Guide:** `docs/LICENSE_AUTOMATION_USAGE_GUIDE.md`  
**Owner:** Immaculate Nyoni | sthwaloe@gmail.com | +27 61 514 6185

---

**Time Required:** 5-10 minutes | **Status:** ✅ Ready to Run
