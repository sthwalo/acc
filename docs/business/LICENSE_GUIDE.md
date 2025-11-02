# FIN License & IP Protection Guide
**Copyright 2024-2025 Sthwalo Holdings (Pty) Ltd. All Rights Reserved.**  
**Owner:** Immaculate Nyoni  
**Contact:** sthwaloe@gmail.com | +27 61 514 6185  
**Last Updated:** November 2, 2025

---

## 📋 Table of Contents
1. [License Overview](#license-overview)
2. [Commercial License Terms](#commercial-license-terms)
3. [IP Protection Strategy](#ip-protection-strategy)
4. [Implementation Guide](#implementation-guide)
5. [PDF Branding System](#pdf-branding-system)
6. [License Compliance](#license-compliance)

---

## 🎯 License Overview

### License Types Summary

| Use Case | License Required | Terms |
|----------|------------------|-------|
| **Personal Use** | Apache 2.0 (Free) | Individual users, personal finance management |
| **Educational Use** | Apache 2.0 (Free) | Students, researchers, educational institutions |
| **Open Source Development** | Apache 2.0 (Free) | Contributing to FIN project, non-commercial development |
| **Commercial Use** | Commercial License (Paid) | Business use, revenue generation, commercial deployment |

### 🚫 Important: Commercial Use Restrictions

**The Apache 2.0 license does NOT grant commercial use rights for the APPLICATION.**

Commercial use includes but is not limited to:
- Using FIN to manage business finances
- Providing financial services using FIN
- Hosting FIN for paying customers
- Integrating FIN into commercial products
- Using FIN in any revenue-generating activity

---

## 💼 Commercial License Terms

### 🥉 Starter License - $29/month
**Perfect for small businesses and startups**

**Limitations:**
- Up to 3 companies/entities
- Up to 1,000 transactions per month
- Up to 2 fiscal periods active
- Email support only
- Single user access

**Includes:**
- ✅ Full FIN application access
- ✅ PDF statement processing
- ✅ Basic financial reports
- ✅ CSV import/export
- ✅ Email support (48h response)

---

### 🥈 Professional License - $99/month
**For growing businesses with complex needs**

**Limitations:**
- Up to 10 companies/entities
- Up to 10,000 transactions per month
- Unlimited fiscal periods
- Priority email support
- Up to 5 user accounts

**Includes:**
- ✅ Everything in Starter
- ✅ Advanced reporting suite
- ✅ Multi-company consolidation
- ✅ API access for integrations
- ✅ Priority support (24h response)
- ✅ Quarterly business reviews

---

### 🥇 Enterprise License - $299/month
**For large organizations and financial firms**

**No Limitations:**
- Unlimited companies/entities
- Unlimited transactions
- Unlimited fiscal periods
- Unlimited user accounts
- Phone and email support

**Includes:**
- ✅ Everything in Professional
- ✅ Custom report builder
- ✅ Advanced audit trails
- ✅ Compliance reporting
- ✅ Priority phone support (4h response)
- ✅ Monthly business reviews
- ✅ Custom feature requests
- ✅ White-label options

---

### 🏢 Custom License - Contact for Pricing
**For specialized industries and unique requirements**

- Regulatory compliance (SOX, GAAP, IFRS)
- Custom integrations
- On-premises deployment
- Source code escrow
- Dedicated support team
- Training and certification

---

## 📞 Getting Licensed

### Start Your Commercial License:
1. **Visit:** https://fin-licensing.com/get-started
2. **Email:** sthwaloe@gmail.com
3. **Phone:** +27 61 514 6185
4. **Schedule:** Free consultation call

### License Activation Process:
1. Choose your license tier
2. Complete business verification
3. Sign license agreement
4. Receive activation key
5. Download commercial version

### Payment Terms:
- Monthly or annual billing
- 30-day free trial for all tiers
- No setup fees
- Cancel anytime (with 30-day notice)

---

## 🛡️ IP Protection Strategy

### Patent Protection
This software and its unique approaches to financial data processing are protected by pending patents and trade secrets. Any attempt to reverse engineer, copy, or create derivative works for commercial purposes without proper licensing is strictly prohibited.

### Copyright Protection
- **All source code:** Copyright 2024-2025 Sthwalo Holdings (Pty) Ltd.
- **All algorithms and business logic:** Proprietary to Sthwalo Holdings
- **All documentation and user interfaces:** Proprietary to Sthwalo Holdings
- **All data processing methods:** Trade secrets of Sthwalo Holdings

### Trademark Protection
"FIN Financial Management System" and related logos are trademarks of Sthwalo Holdings (Pty) Ltd.

---

## ⚖️ Legal Enforcement

### Violation Consequences
Unauthorized commercial use will result in:
1. **Immediate cease and desist notice**
2. **Retroactive licensing fees** (3x standard rate)
3. **Legal action** for damages and injunctive relief
4. **Public disclosure** of violation

### Monitoring and Detection
We actively monitor for unauthorized commercial use through:
- Automated scanning of business listings
- Financial technology industry monitoring
- User reports and community surveillance
- Legal technology tracking services

---

## 🏗️ Implementation Guide

### 1. Standard License Header Template

**Location:** `config/standard-license-header.txt`

All Java source files must include this header:

```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
```

### 2. Adding License to New Files

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
 * [... rest of license text ...]
 */

package fin.service;

import java.util.*;

public class MyNewService {
    // Your code here
}
```

---

## 🎨 PDF Branding System

### PdfBrandingService (Centralized Branding)

**Location:** `app/src/main/java/fin/service/PdfBrandingService.java`

**Purpose:** Single service that provides consistent PDF footers, copyright notices, and license information across all PDF-generating services.

### Key Methods

```java
// Add system footer with page number and timestamp
public void addSystemFooter(Document document, int pageNumber)

// Add copyright notice at bottom of document
public void addCopyrightNotice(Document document)

// Get formatted license information
public String getLicenseInfo()

// Get formatted owner information
public String getOwnerInfo()
```

### Footer Format
```
Generated using FIN Financial Management System | Page X | Generated: DD/MM/YYYY HH:MM:SS
```

### Copyright Format
```
Copyright © 2025 Sthwalo Holdings (Pty) Ltd. | Licensed under Apache License 2.0
Immaculate Nyoni | sthwaloe@gmail.com | +27 61 514 6185
```

### Integration Example

```java
// In any PDF-generating service
import fin.service.PdfBrandingService;

public class MyPdfService {
    private final PdfBrandingService brandingService = new PdfBrandingService();
    
    public void generatePdf() {
        Document document = new Document();
        // ... add content ...
        
        // Add system footer (page 1)
        brandingService.addSystemFooter(document, 1);
        
        // Optionally add copyright notice
        brandingService.addCopyrightNotice(document);
        
        document.close();
    }
}
```

### Services Using PdfBrandingService
- `PayslipPdfService` - Employee payslip generation
- `ReportService` - Financial report PDFs
- `BankStatementPdfService` - Bank statement exports
- `InvoicePdfService` - Invoice generation
- All other PDF-generating services

---

## ✅ License Compliance

### Automated Compliance Checking

**Check Script:** `./scripts/check-licenses.sh`

```bash
# Check all files
./scripts/check-licenses.sh

# Check specific directory
./scripts/check-licenses.sh app/src/main/java/fin/service/

# Save audit report
./scripts/check-licenses.sh > license_audit.txt 2>&1
```

### Automation Script

**Add Headers Script:** `./scripts/add-license-headers.sh`

```bash
# Navigate to project
cd /Users/sthwalonyoni/FIN

# Run automation
./scripts/add-license-headers.sh

# Type 'yes' when prompted
```

### Expected Results

**Before:**
```
Total: 194 | Licensed: 21 | Unlicensed: 173
```

**After:**
```
Total: 194 | Licensed: 194 | Unlicensed: 0 ✅
```

### Verification Steps

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

## 🔄 Rollback & Recovery

### If Something Goes Wrong

```bash
# Rollback using backup
cp -r .license-backup-*/app/src/* app/src/

# OR rollback using git
git checkout app/src/
```

### Commit Changes

```bash
git add app/src/
git commit -m "feat: add license headers to Java files"
git push origin main
```

---

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| Permission denied | `chmod +x scripts/add-license-headers.sh` |
| Build fails | Check for missing blank line after `*/` |
| Template not found | Ensure `config/standard-license-header.txt` exists |
| PDF footer missing | Verify `PdfBrandingService` is injected |
| Copyright notice wrong | Update `LicenseManager` configuration |

---

## 📊 Current Compliance Status

**As of November 2, 2025:**
- ✅ **194/194** Java files with license headers (100%)
- ✅ **All PDF services** using `PdfBrandingService`
- ✅ **Automated compliance** checking enabled
- ✅ **Standard template** maintained in config/
- ✅ **Documentation** complete and up-to-date

---

## 🎯 Why License FIN Commercially?

### For Your Business:
- ✅ **Legal Protection** - Fully compliant commercial use
- ✅ **Professional Support** - Get help when you need it
- ✅ **Regular Updates** - Latest features and security patches
- ✅ **Peace of Mind** - No risk of license violations
- ✅ **Commercial Rights** - Full commercial use permissions
- ✅ **Priority Support** - Fast response times
- ✅ **Feature Requests** - Influence product roadmap

### For Sthwalo Holdings:
- ✅ **Sustainable Development** - Fund ongoing development
- ✅ **Support Operations** - Maintain infrastructure
- ✅ **Innovation** - Invest in new features
- ✅ **Quality Assurance** - Professional testing and QA
- ✅ **Documentation** - Comprehensive user guides
- ✅ **Community** - Support open-source contributors

---

## 📞 Support & Contact

**Owner:** Immaculate Nyoni  
**Company:** Sthwalo Holdings (Pty) Ltd.  
**Email:** sthwaloe@gmail.com  
**Phone:** +27 61 514 6185  
**Website:** https://fin-licensing.com

**For Commercial Licensing:**  
- Schedule a free consultation call
- Get custom pricing for enterprise needs
- Discuss compliance requirements
- Request feature demonstrations

**For Technical Support:**  
- Email support for license issues
- GitHub Issues for technical questions
- Integration assistance
- PDF branding customization
- Compliance audit support

---

## 📚 Additional Resources

- **Quick Reference:** See `/docs/business/LICENSE_QUICK_REFERENCE.md`
- **Apache License:** https://www.apache.org/licenses/LICENSE-2.0
- **Privacy Policy:** https://fin-licensing.com/privacy
- **Terms of Service:** https://fin-licensing.com/terms
- **Security Policy:** https://fin-licensing.com/security

---

**Document Version:** 2.0  
**Last Review:** November 2, 2025  
**Next Review:** February 2, 2026
