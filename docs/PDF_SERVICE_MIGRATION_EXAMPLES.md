# PDF Service Migration Examples - Before & After

**Owner:** Immaculate Nyoni | **Company:** Sthwalo Holdings (Pty) Ltd.

---

## 📚 Complete Migration Examples

This document shows **exactly** how to update existing PDF-generating services to use the centralized `PdfBrandingService`.

---

## Example 1: PayrollReportService.java

### ❌ BEFORE (Old Inline Footer Code)

```java
package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PayrollReportService {
    
    public void generatePayslip(String employeeName, double grossSalary, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();
            
            // Add company header
            Paragraph header = new Paragraph("Payslip", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            // Add employee details
            document.add(new Paragraph("Employee: " + employeeName));
            document.add(new Paragraph("Gross Salary: R" + grossSalary));
            
            // OLD: Inline footer code - INCONSISTENT FORMATTING
            Paragraph footer = new Paragraph();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
            String timestamp = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
            footer.add(new Chunk("Generated: " + timestamp, footerFont));
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payslip", e);
        }
    }
}
```

**Problems with OLD approach:**
- ❌ Footer formatting inconsistent across different PDF services
- ❌ Timestamp format varies between services
- ❌ Hard-coded footer text ("Generated: ...")
- ❌ No page numbering
- ❌ No copyright or license information
- ❌ Duplicate code in every PDF service

---

### ✅ AFTER (Using PdfBrandingService)

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
 *
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 */

package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import fin.service.PdfBrandingService;  // ← NEW IMPORT
import java.io.FileOutputStream;

public class PayrollReportService {
    
    private final PdfBrandingService brandingService = new PdfBrandingService();  // ← NEW FIELD
    
    public void generatePayslip(String employeeName, double grossSalary, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();
            
            // Add company header
            Paragraph header = new Paragraph("Payslip", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            
            // Add employee details
            document.add(new Paragraph("Employee: " + employeeName));
            document.add(new Paragraph("Gross Salary: R" + grossSalary));
            
            // NEW: Use centralized branding service - CONSISTENT FORMATTING
            brandingService.addSystemFooter(document, 1);  // ← REPLACES ALL OLD FOOTER CODE
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payslip", e);
        }
    }
}
```

**Benefits of NEW approach:**
- ✅ Consistent footer across ALL PDF services
- ✅ Standardized timestamp format
- ✅ Includes system name: "Generated using FIN Financial Management System"
- ✅ Proper page numbering
- ✅ Option to add copyright notice
- ✅ Single line of code instead of 10+ lines
- ✅ Proper license header at top of file

---

## Example 2: BudgetReportService.java

### ❌ BEFORE

```java
package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import java.time.LocalDateTime;

public class BudgetReportService {
    
    public void generateBudgetReport(String companyName, double totalBudget, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();
            
            // Budget content
            document.add(new Paragraph("Budget Report", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            document.add(new Paragraph("Company: " + companyName));
            document.add(new Paragraph("Total Budget: R" + totalBudget));
            
            // OLD: Different footer style - INCONSISTENT
            Font smallFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, BaseColor.LIGHT_GRAY);
            Paragraph footer = new Paragraph(
                "Report generated on " + LocalDateTime.now() + " | FIN System", 
                smallFont);
            footer.setAlignment(Element.ALIGN_RIGHT);
            document.add(footer);
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate budget report", e);
        }
    }
}
```

---

### ✅ AFTER

```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * [... full license text ...]
 */

package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import fin.service.PdfBrandingService;  // ← NEW IMPORT
import java.io.FileOutputStream;

public class BudgetReportService {
    
    private final PdfBrandingService brandingService = new PdfBrandingService();  // ← NEW FIELD
    
    public void generateBudgetReport(String companyName, double totalBudget, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();
            
            // Budget content
            document.add(new Paragraph("Budget Report", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20)));
            document.add(new Paragraph("Company: " + companyName));
            document.add(new Paragraph("Total Budget: R" + totalBudget));
            
            // NEW: Centralized footer - CONSISTENT
            brandingService.addSystemFooter(document, 1);  // ← ONE LINE REPLACES ALL
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate budget report", e);
        }
    }
}
```

---

## Example 3: Multi-Page PDF with Copyright Notice

### Complete Example: InvoicePdfService.java (CURRENT IMPLEMENTATION)

```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * [... full license text ...]
 */

package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import fin.service.PdfBrandingService;
import java.io.FileOutputStream;

public class InvoicePdfService {
    
    private final PdfBrandingService brandingService = new PdfBrandingService();
    
    public void generateInvoice(String customerName, double amount, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();
            
            // INVOICE CONTENT
            document.add(new Paragraph("INVOICE", 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24)));
            document.add(new Paragraph("Customer: " + customerName));
            document.add(new Paragraph("Amount: R" + amount));
            
            // SYSTEM FOOTER - Page 1
            brandingService.addSystemFooter(document, 1);
            
            // If multi-page, add footer to each page
            document.newPage();
            document.add(new Paragraph("Terms and Conditions..."));
            brandingService.addSystemFooter(document, 2);  // Page 2
            
            // OPTIONAL: Add copyright notice at end
            brandingService.addCopyrightNotice(document);
            
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice", e);
        }
    }
}
```

**Key Points:**
- ✅ Footer on every page with correct page number
- ✅ Optional copyright notice at end
- ✅ Clean, maintainable code
- ✅ Consistent branding

---

## 🔧 Step-by-Step Migration Checklist

For each PDF-generating service:

### Step 1: Add License Header
```java
/*
 * FIN Financial Management System
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * ...
 */
```
**Copy from:** `config/standard-license-header.txt`

---

### Step 2: Add Import
```java
import fin.service.PdfBrandingService;
```

---

### Step 3: Add Private Field
```java
private final PdfBrandingService brandingService = new PdfBrandingService();
```

---

### Step 4: Replace Old Footer Code

**REMOVE THIS:**
```java
// OLD CODE - DELETE
Paragraph footer = new Paragraph();
Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.GRAY);
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
footer.add(new Chunk("Generated: " + timestamp, footerFont));
footer.setAlignment(Element.ALIGN_CENTER);
document.add(footer);
```

**ADD THIS:**
```java
// NEW CODE - ADD
brandingService.addSystemFooter(document, pageNumber);
```

---

### Step 5: Test Generated PDF

```bash
# Run your service
./gradlew run

# Open generated PDF
open exports/YOUR_FILE.pdf

# Verify footer shows:
# "Generated using FIN Financial Management System | Page X | Generated: DD/MM/YYYY HH:MM:SS"
```

---

### Step 6: Verify Build

```bash
./gradlew clean build
```

---

### Step 7: Commit Changes

```bash
git add app/src/main/java/fin/service/YourService.java
git commit -m "feat: integrate PdfBrandingService in YourService for consistent footer branding"
```

---

## 📋 Services That Need Migration

### Priority 1 (URGENT):
- [ ] `PayrollReportService.java` - Payslip PDFs
- [ ] `BudgetReportService.java` - Budget reports
- [ ] `PdfExportService.java` - Data exports

### Already Migrated ✅:
- [x] `InvoicePdfService.java` - Invoice generation

---

## 🧪 Testing Your Migration

### Test Checklist:

1. **Compilation:**
   ```bash
   ./gradlew clean build
   ```
   - Must compile without errors
   - No missing imports

2. **Functionality:**
   ```bash
   ./gradlew run
   # Generate a PDF using your updated service
   ```
   - PDF generates successfully
   - All content present
   - No missing data

3. **Footer Verification:**
   ```bash
   open exports/YOUR_FILE.pdf
   ```
   - Footer visible at bottom
   - Format: "Generated using FIN Financial Management System | Page X | Generated: timestamp"
   - Font: 8pt gray Helvetica
   - Alignment: Centered

4. **Multi-Page Check** (if applicable):
   - Footer on every page
   - Correct page numbers (1, 2, 3, ...)

5. **Copyright Notice** (if added):
   - Shows at document end
   - Format: "Copyright © 2025 Sthwalo Holdings (Pty) Ltd. | Licensed under Apache License 2.0"
   - Contact info: "Immaculate Nyoni | sthwaloe@gmail.com | +27 61 514 6185"

---

## 🚨 Common Migration Mistakes

### Mistake 1: Missing Import
```java
// ❌ WRONG
private final PdfBrandingService brandingService = new PdfBrandingService();
// ERROR: Cannot resolve symbol 'PdfBrandingService'
```
```java
// ✅ CORRECT
import fin.service.PdfBrandingService;

private final PdfBrandingService brandingService = new PdfBrandingService();
```

---

### Mistake 2: No Blank Line After License Header
```java
// ❌ WRONG
/*
 * License header...
 */
package fin.service;  // Causes compilation issues
```
```java
// ✅ CORRECT
/*
 * License header...
 */

package fin.service;  // Blank line required
```

---

### Mistake 3: Incorrect Page Number
```java
// ❌ WRONG - All pages show "Page 1"
for (int i = 0; i < 5; i++) {
    document.newPage();
    brandingService.addSystemFooter(document, 1);  // Always 1
}
```
```java
// ✅ CORRECT - Each page numbered correctly
for (int i = 0; i < 5; i++) {
    document.newPage();
    brandingService.addSystemFooter(document, i + 1);  // 1, 2, 3, 4, 5
}
```

---

### Mistake 4: Calling Footer Before Content
```java
// ❌ WRONG - Footer appears at top
document.open();
brandingService.addSystemFooter(document, 1);  // Called first
document.add(new Paragraph("Main content"));
```
```java
// ✅ CORRECT - Footer appears at bottom
document.open();
document.add(new Paragraph("Main content"));  // Content first
brandingService.addSystemFooter(document, 1);  // Footer last
```

---

## 📊 Migration Progress Tracker

Use this checklist to track your migration:

```
PDF Service Migration Status:

✅ InvoicePdfService.java          (COMPLETED - Oct 29, 2025)
⏳ PayrollReportService.java       (PENDING)
⏳ BudgetReportService.java        (PENDING)
⏳ PdfExportService.java           (PENDING)
⏳ ExcelFinancialReportService.java (PENDING - if has PDF features)

Progress: 1/5 (20%)
Target: 5/5 (100%) by Nov 5, 2025
```

---

## 💡 Pro Tips

### Tip 1: Test Early, Test Often
After migrating each service, immediately test it before moving to the next one.

### Tip 2: Keep Old Code in Comments (Temporarily)
```java
// OLD FOOTER CODE (DELETE AFTER TESTING):
// Paragraph footer = new Paragraph();
// footer.add(new Chunk("Generated: " + timestamp, footerFont));
// document.add(footer);

// NEW FOOTER CODE:
brandingService.addSystemFooter(document, pageNumber);
```
Once verified working, delete the commented code.

### Tip 3: Use Git Branches
```bash
git checkout -b feature/migrate-payroll-pdf-branding
# Make changes
git commit -m "feat: migrate PayrollReportService to PdfBrandingService"
git push origin feature/migrate-payroll-pdf-branding
```

### Tip 4: Leverage PdfBrandingService Methods
```java
// Get license info for logs
String licenseInfo = brandingService.getLicenseInfo();
System.out.println("PDF generated with: " + licenseInfo);

// Get owner info for audit trail
String ownerInfo = brandingService.getOwnerInfo();
System.out.println("Owner: " + ownerInfo);
```

---

## 📞 Need Help?

**Owner:** Immaculate Nyoni  
**Email:** sthwaloe@gmail.com  
**Phone:** +27 61 514 6185

**Documentation:**
- Full Guide: `docs/LICENSE_PROTECTION_GUIDE.md`
- Quick Reference: `docs/LICENSE_QUICK_REFERENCE.md`
- This File: `docs/PDF_SERVICE_MIGRATION_EXAMPLES.md`

---

**Last Updated:** October 29, 2025  
**Migration Status:** 1/5 services completed (20%)
