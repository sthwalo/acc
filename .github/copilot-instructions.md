# FIN Financial Management System - Copilot Instructions

## Project Overview
FIN is a production-ready financial management system built in Java 17 with PostgreSQL 17+, handling 7,156+ real transactions. It provides comprehensive financial document processing including bank statement processing, financial reporting, payroll management, budget planning, and REST API capabilities for South African businesses with SARS compliance.

## Architecture & Entry Points

**Core Architecture**: Single Gradle module with dependency injection via `ApplicationContext`. Three runtime modes:
- **Console**: `./gradlew run` → `ConsoleApplication.main()` → interactive menu system
- **API Server**: `./gradlew run --args="api"` → `ApiApplication.main()` → REST API on port 8080
- **Batch**: `./gradlew run --args="--batch [command]"` → automated processing

**Key Components**:
- `ApplicationContext`: Central DI container - register ALL new services here using secure constructor pattern
- `ApplicationController`: Orchestrates console flow via domain-specific controllers
- `ApiServer`: SparkJava-based REST endpoints with `setup*` methods for route organization
- `DatabaseConfig`: PostgreSQL connection management with environment-based configuration

## Critical Development Patterns

### ⚠️ CRITICAL RULE: NO FALLBACK DATA ALLOWED - ZERO TOLERANCE POLICY

**ARCHITECTURAL PRINCIPLE**: Database is the SINGLE SOURCE OF TRUTH. ALL business data, templates, compliance text, and configuration MUST come from database. If database is empty, the system MUST throw a clear exception - NEVER silently fall back to hardcoded data.

**This rule applies to ALL services across the ENTIRE codebase:**
- ✅ Financial reporting services (ExcelFinancialReportService, FinancialReportingService, TextReportToPdfService)
- ✅ Document generation (reports, invoices, statements, letters)
- ✅ Compliance text (IFRS, IAS, SARS regulations, tax disclaimers)
- ✅ Template data (email templates, report templates, document templates)
- ✅ Business rules (validation messages, error messages, user prompts)
- ✅ Configuration data (system settings, user preferences, display formats)

#### 🚫 FORBIDDEN PATTERNS (Will Be Rejected in Code Review)

**Pattern 1: Silent Fallback on Null**
```java
// ❌ WRONG - Silent fallback
Data data = fetchFromDatabase(conn, id);
if (data == null) {
    data = createDefaultData(); // FORBIDDEN!
}
return data;
```

**Pattern 2: Try-Catch with Fallback**
```java
// ❌ WRONG - Fallback in catch block
try {
    return fetchFromDatabase(conn, id);
} catch (SQLException e) {
    LOGGER.warning("Database error, using defaults"); // FORBIDDEN!
    return createDefaultData(); // FORBIDDEN!
}
```

**Pattern 3: Default/Template Methods**
```java
// ❌ WRONG - Hardcoded template method
private String createDefaultAuditReport() { // FORBIDDEN method name!
    return "We have audited the financial statements in accordance with ISA..."; // FORBIDDEN hardcoded text!
}

private List<String> getDefaultAccountingPolicies() { // FORBIDDEN!
    return Arrays.asList(
        "Revenue is recognized when control transfers, in accordance with IFRS 15...", // FORBIDDEN!
        "Property, plant and equipment are stated at cost, in accordance with IAS 16..." // FORBIDDEN!
    );
}
```

**Pattern 4: Hardcoded Business Text**
```java
// ❌ WRONG - Hardcoded compliance/business text
String disclaimer = "The financial statements have been prepared in accordance " + 
                   "with International Financial Reporting Standards (IFRS) " +
                   "and the Companies Act, 2008..."; // FORBIDDEN!

String taxNotice = "For SARS tax purposes, this document serves as proof of..."; // FORBIDDEN!
```

**Pattern 5: Inline Default Values**
```java
// ❌ WRONG - Inline defaults for business data
String auditorName = company.getAuditor() != null ? 
    company.getAuditor() : "Independent Auditors (Pty) Ltd"; // FORBIDDEN default!

String complianceText = rs.getString("compliance_text");
if (complianceText == null) {
    complianceText = "Prepared in accordance with IFRS and IAS standards"; // FORBIDDEN!
}
```

#### ✅ REQUIRED PATTERN (Mandatory Across Entire Codebase)

**Correct Pattern: Fail-Fast with Clear Error Messages**
```java
// CORRECT: Throw exception if database empty
List<Data> data = fetchFromDatabase(conn, companyId, periodId);
if (data == null || data.isEmpty()) {
    throw new SQLException(
        "Data not found in table 'table_name' for company " + companyId + 
        " and period " + periodId + ". Please insert data into database. " +
        "SQL: INSERT INTO table_name (company_id, period_id, field1, field2) VALUES (?, ?, ?, ?)"
    );
}
// Proceed with real data - NO FALLBACK
processData(data);
```

**Benefits of Fail-Fast Pattern:**
1. ✅ **Clear Error Messages**: User knows exactly what's missing
2. ✅ **Database First**: Forces proper data management
3. ✅ **Maintainability**: All business text in one place (database)
4. ✅ **Compliance**: Regulatory text updated via SQL, not code deployments
5. ✅ **Testing**: Easy to test error conditions
6. ✅ **Production Safety**: No silent failures with wrong data

#### 📋 REFACTORING METHODOLOGY (Apply to Any Service)

When you encounter fallback/hardcoded data patterns in ANY file:

**Step 1: AUDIT - Identify All Violations**
```bash
# Search for forbidden patterns
grep -rn "createDefault" app/src/main/java/
grep -rn "useFallback" app/src/main/java/
grep -rn "getTemplate" app/src/main/java/
grep -rn "catch.*SQLException.*{" app/src/main/java/ | grep -A 5 "return"
```

**Step 2: INVENTORY - Document What You Find**
Create a list:
- Method names with "default", "fallback", "template"
- Try-catch blocks that create data on exception
- Hardcoded strings > 50 characters (likely business text)
- `if (data == null) { ... }` blocks that create data

**Step 3: DELETE - Remove All Fallback Methods**
- Delete ENTIRE methods named `createDefault*()`, `useFallback*()`, `getTemplate*()`
- Do NOT refactor them - DELETE them completely
- Remove 100% of hardcoded business text from these methods

**Step 4: UPDATE - Fix Callers to Throw Exceptions**
```java
// Before (BAD):
try {
    data = fetchData();
    if (data.isEmpty()) {
        createDefaultData(); // Delete this!
    }
} catch (SQLException e) {
    createDefaultData(); // Delete this!
}

// After (GOOD):
List<Data> data = fetchData(conn, id);
if (data.isEmpty()) {
    throw new SQLException("Data not found in table_name for id " + id + 
        ". Please insert: INSERT INTO table_name (...) VALUES (...)");
}
// Use real data
```

**Step 5: VERIFY - Test and Document**
```bash
# Compile to catch errors
./gradlew compileJava --no-daemon

# Verify no forbidden patterns remain
grep -c "createDefault" YourService.java  # Should be 0
grep -c "useFallback" YourService.java    # Should be 0

# Document line reduction
wc -l YourService.java  # Should be significantly smaller
```

**Step 6: SEED - Create Database Migration (If Needed)**
If you removed hardcoded templates, create SQL seed data:
```sql
-- scripts/migrations/seed_your_templates.sql
INSERT INTO your_table (company_id, template_text, created_at) VALUES
(1, 'The actual business text that was hardcoded', NOW()),
(2, 'Another template that was in code', NOW());
```

#### 🎯 SPECIFIC FORBIDDEN PATTERNS BY SERVICE TYPE

**Report Generation Services (ExcelFinancialReportService, PDF services, etc.):**
- ❌ NO hardcoded IFRS/IAS/ISA compliance text
- ❌ NO default audit opinions, director statements, or disclaimers
- ❌ NO template financial notes or accounting policies
- ✅ ALL regulatory text MUST come from: `audit_reports`, `directors_reports`, `financial_notes`, `compliance_statements` tables

**Document Generation Services (Invoice, Receipt, Letter generators):**
- ❌ NO hardcoded terms & conditions
- ❌ NO default payment instructions or disclaimers
- ❌ NO template business addresses or contact info
- ✅ ALL document text MUST come from: `document_templates`, `company_settings`, `terms_conditions` tables

**Email/Notification Services:**
- ❌ NO hardcoded email subject lines or body text
- ❌ NO default greeting/closing templates
- ❌ NO hardcoded notification messages
- ✅ ALL email content MUST come from: `email_templates`, `notification_templates` tables

**Validation/Business Rule Services:**
- ❌ NO hardcoded error messages (except system errors)
- ❌ NO hardcoded validation messages
- ❌ NO default warning texts
- ✅ ALL business messages MUST come from: `validation_rules`, `business_messages` tables

#### 🔍 CODE REVIEW CHECKLIST

Before committing ANY code, verify:
- [ ] No methods named `createDefault*`, `useFallback*`, `getTemplate*`
- [ ] No try-catch blocks that create data on exception
- [ ] No hardcoded strings > 50 chars (except SQL queries, log messages)
- [ ] No `if (data == null) { create data }` patterns
- [ ] All database fetch methods throw SQLException when empty
- [ ] Exception messages specify exact table and SQL INSERT example
- [ ] File size reduced if removing fallback code
- [ ] `./gradlew compileJava` succeeds
- [ ] No compiler warnings about unused fields/methods

#### 📊 SUCCESS METRICS

A successful refactoring should show:
- **Code Reduction**: 10-20% fewer lines (fallback methods deleted)
- **Zero Fallbacks**: 0 occurrences of forbidden patterns
- **Clear Errors**: All SQLException messages specify table + INSERT statement
- **Clean Build**: No compilation errors or warnings
- **Maintainability**: All business text in database, not code

**PDF Library Standards - CRITICAL**: 
- ✅ **MUST USE**: Apache PDFBox 3.0.0 (open source) for PDF reading/text extraction/document generation
- ✅ **MUST USE**: libharu via JNA (open source) for complex PDF generation (payslips, invoices, reports with precise layouts)
- ❌ **NEVER USE**: iText (commercial licensing restrictions - removed from project)
- 📚 **Reference Implementations**:
  - `PayslipPdfService.java` → libharu pattern (JNA with Pointer, precise layout control)
  - `TextReportToPdfService.java` → PDFBox pattern (PDDocument, PDPage, PDPageContentStream)
  - `PdfTextExtractionService.java` → PDFBox for reading PDFs
- **When to Use Which**:
  - **PDFBox**: Simple reports, text-heavy documents, PDF reading/parsing
  - **libharu**: Complex layouts, precise positioning, tables, branded documents (payslips, invoices)

**Service Registration**: Always register new services in `ApplicationContext.initialize*Services()` methods:
```java
// In ApplicationContext
SomeService someService = new SomeService(dbUrl, dependency);
register(SomeService.class, someService);
```

**Database Access**: Use repositories over direct JDBC. Connection pooling via HikariCP is pre-configured:
```java
// Prefer this pattern
public class SomeRepository {
    public SomeRepository(String dbUrl) { /* use DatabaseConfig.getConnection() */ }
}
```

**Transaction Processing Pipeline**: 
1. `DocumentTextExtractor` → PDF text extraction
2. `BankStatementProcessingService` → orchestrates parsing via registered `TransactionParser` implementations
3. `TransactionClassificationService` → applies business rules (2k+ lines - don't duplicate logic)
4. Database persistence via repositories

**UI Patterns**: 
- Console: Inject `OutputFormatter`/`InputHandler` - never use `System.out.println` directly
- API: Use `ApiServer.setup*` methods, return Gson-wrapped `{success, data}` responses

## Essential Commands & Workflows

**Build & Test**:
```bash
./gradlew clean build                    # Full build with quality checks
./gradlew test                          # Run test suite
./gradlew checkstyleMain                # Code quality scan
```

**Database Setup**:
```bash
source .env                             # Load database credentials
# Requires: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
# Tests use: TEST_DATABASE_URL, TEST_DATABASE_USER, TEST_DATABASE_PASSWORD
```

**Development Modes**:
```bash
./gradlew run                           # Console application
./gradlew run --args="api"              # REST API server
./start-backend.sh                      # API with environment setup
./test-api.sh                          # API integration tests
```

## Code Quality & Documentation

**Mandatory Pre-Work Check**: Before ANY file modification:
```bash
# Check existing task documentation
ls -la docs/development/tasks/
grep -r "TASK.*" docs/development/tasks/ | grep -i "[your-topic]"
cat docs/development/tasks/README.md
```

**Quality Protocol**:
1. **Inventory violations**: `./gradlew clean checkstyleMain --no-daemon 2>&1 | grep -E "(MethodLength|MagicNumber|HiddenField)" > violations_inventory.txt`
2. **Fix ONE file at a time** - address ALL violations before moving on
3. **Document ALL work** in `docs/development/tasks/TASK_[NUMBER]_[Name].md`
4. **Build verification**: `./gradlew clean build` after every change

**File Processing**: PDFs go in `input/`, reports generate to `output/`. Scripts in `scripts/` expect this structure.

### ⚠️ CRITICAL ENFORCEMENT: Code Changes & Testing Protocol

**MANDATORY REQUIREMENT**: After ANY changes to non-test code files (.java, .kt, build.gradle.kts, etc.):

#### 1. **BUILD VERIFICATION** (Required After Every Code Change)
```bash
./gradlew clean build
```
This ensures:
- All code compiles correctly
- Dependencies are resolved (PostgreSQL driver, PDFBox 3.0.0, Apache POI 5.2.4, Spark Java, etc.)
- No regressions in existing functionality
- Build artifacts (fat JAR) are up-to-date
- Checkstyle issues are bypassed for faster development

#### 2. **USER VERIFICATION** (CRITICAL - DO NOT SKIP)
**🚨 STOP - DO NOT COMMIT OR PUSH UNTIL USER CONFIRMS FIX WORKS 🚨**

After making code changes:
- ✅ Explain **WHAT** you changed (which files, which methods)
- ✅ Explain **WHY** you changed it (what problem does it solve)
- ✅ Explain **HOW** to test the fix (specific steps user should follow)
- ✅ Wait for user to **RUN THE APPLICATION** and verify the fix works
- ✅ Wait for user to **EXPLICITLY CONFIRM** "the fix works" or "ready to commit"
- ❌ **DO NOT** Make code changes without user review
- ❌ **DO NOT** commit changes until user confirms
- ❌ **DO NOT** assume the fix works just because it compiles
- ❌ **DO NOT** rush to commit and push

#### 3. **TESTING WORKFLOW** (Required Before Any Commit)
```bash
# User must verify the fix by:
1. Running the application: ./run.sh
2. Testing the changed functionality interactively
3. Reviewing generated reports/output
4. Confirming the fix resolves the original issue
5. Giving explicit approval: "Yes, this fix works, commit it"

# Only after user confirmation:
git add <changed-files>
git commit -m "descriptive message"
git push origin main
```

#### 4. **CONSEQUENCES OF VIOLATION**
- Committing unverified code → production bugs
- Rushing commits → broken functionality
- Skipping user confirmation → wasted time on incorrect fixes
- Not testing thoroughly → financial data corruption

**FAILURE TO COMPLY** will result in broken builds, runtime errors, deployment issues, and financial reporting errors. This protocol is **STRICTLY ENFORCED** for all code changes, especially those affecting financial calculations, database queries, or report generation.

## Service Dependencies & Data Flow

**Core Services** (registered in `ApplicationContext`):
- `CompanyService` → company/fiscal period management
- `BankStatementProcessingService` → PDF processing orchestration  
- `TransactionClassificationService` → business rule application (2k+ lines)
- `PayrollService` → SARS-compliant payroll calculations with PAYE, UIF, SDL
- `BudgetService` → strategic planning and budget management
- `FinancialReportingService` → comprehensive report generation (uses repository pattern)
- `DataManagementService` → manual data entry, corrections, and audit trails
- `TransactionVerificationService` → data integrity validation and reconciliation

**Classification System**: Use existing pattern-matching in `TransactionClassificationService`. Don't recreate regex/keyword logic - extend `ClassificationRuleManager` instead.

**System Architecture Components**:
1. **Document Processing**: PDF/CSV upload → text extraction → structured data review
2. **Financial Data Storage**: PostgreSQL 17+ with comprehensive schema and audit trails
3. **Automated Accounting**: Categorization, reconciliation, general ledger, tax calculations
4. **Payroll Processing**: Employee management, SARS tax calculations, payslip generation
5. **Budget Management**: Strategic planning, variance analysis, cash flow forecasting
6. **Output Generation**: Financial reports, tax returns, payroll documents, budget analysis
7. **Security & Compliance**: POPIA compliance, role-based access, audit logging

**Reporting**: All reports go through `FinancialDataRepository` for caching/pooling. Excel generation uses Apache POI, console reports use `OutputFormatter`. The system generates comprehensive financial reports including:
- Standard financial statements (Trial Balance, Income Statement, Balance Sheet, Cash Flow)
- SARS-compliant tax returns and compliance reports
- Payroll documents (payslips, tax certificates, IRP5 certificates)
- Budget reports and variance analysis (forecasts, strategic plans)
- Custom reports with multiple export formats (PDF, Excel, CSV)

## Database Schema Notes

**Companies Table Schema** (as of November 2025 - PostgreSQL 17):
- Core fields: `id`, `name`, `registration_number`, `tax_number`, `address`, `contact_email`, `contact_phone`, `created_at`, `updated_at`, `logo_path`
- Banking fields: `bank_name`, `account_number`, `account_type`, `branch_code` (added Nov 4, 2025)
- Tax compliance: `vat_registered` (boolean, added Nov 4, 2025)
- **IMPORTANT**: When restoring from backups, verify banking columns exist: `bank_name`, `account_number`, `account_type`, `branch_code`, `vat_registered`
- **Migration Command** if columns missing:
  ```sql
  ALTER TABLE companies 
  ADD COLUMN IF NOT EXISTS bank_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS account_number VARCHAR(50),
  ADD COLUMN IF NOT EXISTS account_type VARCHAR(50),
  ADD COLUMN IF NOT EXISTS branch_code VARCHAR(20),
  ADD COLUMN IF NOT EXISTS vat_registered BOOLEAN DEFAULT FALSE;
  ```

**Current Database Status** (November 4, 2025):
- PostgreSQL 17 running on localhost:5432
- Database: `drimacc_db` owned by user `sthwalonyoni`
- Restored from backup: `drimacc_db_20251028_132936.dump`
- Active companies: 3 (Xinghizana Group, Rock Of Ages Fellowship Church, Limelight Academy Institutions)
- Total transactions: 3,813 bank transactions
- Total payslips: 26

## Testing & Environment

**Test Structure**: JUnit 5 + Mockito in `app/src/test/java`. Integration tests use real PostgreSQL 17+.
**Environment Variables**: Production uses `DATABASE_*`, tests use `TEST_DATABASE_*`
**License Check**: `LicenseManager.checkLicenseCompliance()` required on all entry points

**API Testing**: CORS enabled for `localhost:3000`. Key endpoints:
- `GET /api/v1/health` - system status
- `POST /api/v1/companies/{id}/upload` - file processing
- `GET /api/v1/companies` - company management
- `POST /api/v1/payroll/process` - payroll processing
- `GET /api/v1/budgets/{id}/variance` - budget analysis
- `GET /api/v1/reports/financial` - financial report generation

## Current Development Focus

**Active Refactoring** (see `docs/development/tasks/TASK_6.*`):
- `AccountClassificationService` (2,230 lines) → extract services
- `InteractiveClassificationService` (2,066 lines) → dependency injection
- Output formatting standardization across services

**Quality Remediation**: SpotBugs warnings addressed with defensive copying patterns. EI_EXPOSE_REP fixes documented in task files.

When modifying financial processing logic, maintain the existing transaction flow and update relevant task documentation. The system processes real financial data - ensure accuracy over speed.
