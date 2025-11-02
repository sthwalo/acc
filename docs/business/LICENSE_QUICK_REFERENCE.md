# FIN License - Quick Reference
**Owner:** Immaculate Nyoni | **Company:** Sthwalo Holdings (Pty) Ltd.  
**Last Updated:** November 2, 2025

---

## 🚀 Quick Commands

```bash
# Check license compliance (run from FIN root)
./scripts/check-licenses.sh

# Check specific directory
./scripts/check-licenses.sh app/src/main/java/fin/service/

# Save audit report
./scripts/check-licenses.sh > license_audit.txt 2>&1

# View standard license template
cat config/standard-license-header.txt

# Add licenses to all files (automation)
./scripts/add-license-headers.sh

# Build after adding licenses
./gradlew clean build

# Run tests
./gradlew test
```

---

## 📋 Adding License to New File (3 Steps)

### Step 1: Copy Template
```bash
cat config/standard-license-header.txt | pbcopy  # macOS
```

### Step 2: Paste at Top
Place **before** `package` declaration, add blank line after `*/`

### Step 3: Verify
```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * [... rest of license ...]
 */

package fin.service;  // ← blank line above

public class MyNewService {
    // Your code
}
```

---

## 🎨 PDF Branding (3 Lines)

### Quick Integration

```java
import fin.service.PdfBrandingService;

public class MyPdfService {
    private final PdfBrandingService branding = new PdfBrandingService();
    
    public void generatePdf() {
        Document doc = new Document();
        // ... your content ...
        branding.addSystemFooter(doc, 1);      // Page footer
        branding.addCopyrightNotice(doc);       // Copyright
        doc.close();
    }
}
```

---

## 💼 Commercial License Tiers

| Tier | Price | Companies | Transactions/mo | Support |
|------|-------|-----------|-----------------|---------|
| **Starter** | $29/mo | 3 | 1,000 | Email 48h |
| **Professional** | $99/mo | 10 | 10,000 | Email 24h |
| **Enterprise** | $299/mo | Unlimited | Unlimited | Phone 4h |
| **Custom** | Contact | Custom | Custom | Dedicated |

**Get Licensed:** sthwaloe@gmail.com | +27 61 514 6185

---

## ✅ Automation Quick Start (3 Commands)

```bash
# 1. Navigate to project
cd /Users/sthwalonyoni/FIN

# 2. Run automation
./scripts/add-license-headers.sh

# 3. Verify (should show 100%)
./scripts/check-licenses.sh | tail -5
```

**Expected:** `Total: 194 | Licensed: 194 | Unlicensed: 0 ✅`

---

## 🔄 Rollback

```bash
# Using backup
cp -r .license-backup-*/app/src/* app/src/

# Using git
git checkout app/src/
```

---

## 📊 Current Status (Nov 2, 2025)

- ✅ **194/194** Java files licensed (100%)
- ✅ **All PDF services** using PdfBrandingService
- ✅ **Automated checking** enabled
- ✅ **Documentation** complete

---

## 🛠️ Common Issues

| Issue | Solution |
|-------|----------|
| Permission denied | `chmod +x scripts/*.sh` |
| Build fails | Check blank line after `*/` |
| Template not found | Ensure `config/standard-license-header.txt` exists |
| PDF footer missing | Inject `PdfBrandingService` |

---

## 📞 Contact

**Owner:** Immaculate Nyoni  
**Email:** sthwaloe@gmail.com  
**Phone:** +27 61 514 6185  
**Company:** Sthwalo Holdings (Pty) Ltd.

---

## 📚 Full Documentation

**Comprehensive Guide:** `/docs/business/LICENSE_GUIDE.md`  
**Commercial Terms:** `/docs/business/COMMERCIAL_LICENSE.md`  
**IP Protection:** `/docs/business/IP_PROTECTION_STRATEGY.md`

---

**Quick Reference v2.0** | **Time to Add License:** 2 minutes
