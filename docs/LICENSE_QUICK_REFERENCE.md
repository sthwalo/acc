# FIN License Protection - Quick Reference Guide

**Owner:** Immaculate Nyoni | **Company:** Sthwalo Holdings (Pty) Ltd.

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

# Copy license template to clipboard (macOS)
cat config/standard-license-header.txt | pbcopy

# Build after adding licenses
./gradlew clean build

# Run tests
./gradlew test
```

---

## 📋 Adding License to New File

**Step 1:** Copy template
```bash
cat config/standard-license-header.txt
```

**Step 2:** Paste at top of `.java` file (before `package` declaration)

**Step 3:** Add blank line after license block

**Step 4:** Continue with code

**Example:**
```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * [... rest of license text ...]
 */

package fin.service;

import java.util.*;

public class MyNewService {
    // Your code here
}
```

---

## 🎨 Using PdfBrandingService in PDF Code

**Step 1:** Add import
```java
import fin.service.PdfBrandingService;
```

**Step 2:** Create instance
```java
private final PdfBrandingService brandingService = new PdfBrandingService();
```

**Step 3:** Use in your PDF generation
```java
public void generatePdf() {
    Document document = new Document();
    
    // ... add your content ...
    
    // Add system footer (page 1)
    brandingService.addSystemFooter(document, 1);
    
    // Optional: Add copyright notice
    brandingService.addCopyrightNotice(document);
    
    document.close();
}
```

---

## 📊 Current Status (2025-10-29)

```
Total Files:     194
Licensed:        21 (10.8%)
Unlicensed:      173 (89.2%)
Outdated Owner:  3 files
```

**Properly Licensed Examples:**
- ✅ `PdfBrandingService.java`
- ✅ `InvoicePdfService.java`
- ✅ `CompanyRepository.java`
- ✅ `LicenseManager.java`

**Files Needing Urgent Update:**
- ⚠️ `UserRepository.java` (outdated owner)
- ⚠️ `AuthService.java` (outdated owner)
- ⚠️ `Payslip.java` (outdated owner)

---

## 🔧 Fixing Outdated Owner

**Find files:**
```bash
grep -r "Sthwalo Nyoni" app/src/main/java/ --include="*.java"
```

**Replace in file:**
```
OLD: Owner: Sthwalo Nyoni
NEW: Owner: Immaculate Nyoni
```

**Verify:**
```bash
./scripts/check-licenses.sh | grep "⚠️"
```

---

## ✅ Daily Workflow Checklist

**Before Committing Code:**
- [ ] Run license checker: `./scripts/check-licenses.sh`
- [ ] All new files have license headers
- [ ] Build passes: `./gradlew clean build`
- [ ] Tests pass: `./gradlew test`
- [ ] PDF footers use `PdfBrandingService` (no inline code)

**When Creating New PDF Service:**
- [ ] Add license header at top of file
- [ ] Import `PdfBrandingService`
- [ ] Use `brandingService.addSystemFooter(document, pageNumber)`
- [ ] Test generated PDF has correct footer
- [ ] No hard-coded copyright information

**Weekly Review:**
- [ ] Run full audit: `./scripts/check-licenses.sh`
- [ ] Track progress toward 100% licensed
- [ ] Update copyright year if needed (current: `2024-2025`)

---

## 🚨 Common Mistakes to Avoid

❌ **Don't:**
- Create PDF footers manually (use `PdfBrandingService`)
- Use "Sthwalo Nyoni" as owner (correct: "Immaculate Nyoni")
- Commit files without license headers
- Modify standard license template without approval
- Hard-code copyright info (use `LicenseManager`)

✅ **Do:**
- Add license header to ALL new `.java` files
- Use `PdfBrandingService` for ALL PDF footers
- Run license checker before commits
- Keep copyright year range updated
- Follow standardized template exactly

---

## 📞 Quick Contact

**Owner:** Immaculate Nyoni  
**Email:** sthwaloe@gmail.com  
**Phone:** +27 61 514 6185

**Full Documentation:** `docs/LICENSE_PROTECTION_GUIDE.md`

---

**Last Updated:** 2025-10-29
