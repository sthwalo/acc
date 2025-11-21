# FIN Financial Management System - Copilot Instructions

## 🐳 DOCKER-FIRST DEVELOPMENT - CRITICAL REQUIREMENT

**🚨 EVERYTHING RUNS THROUGH DOCKER 🚨**

**MANDATORY POLICY**: All development, testing, and production deployment MUST use Docker containers. No direct JAR execution allowed. This ensures:
- ✅ **Zero deployment surprises**: Dev environment = Production environment
- ✅ **Environment consistency**: Same JVM, dependencies, and configuration
- ✅ **Frontend integration**: Test against real containerized APIs
- ✅ **Production confidence**: Container = production deployment

**VIOLATION CONSEQUENCES**: Code that doesn't work in Docker containers will fail in production. All changes must be tested in Docker before commit.

## Project Overview
FIN is a production-ready financial management system built in Java 17 with PostgreSQL 17+, handling 7,156+ real transactions. It provides comprehensive financial document processing including bank statement processing, financial reporting, payroll management, budget planning, and REST API capabilities for South African businesses with SARS compliance.

**🐳 Docker-First Architecture**:
- **Legacy `app/`**: SparkJava-based with custom dependency injection (currently migrating to Spring Boot)
- **Modern `spring-app/`**: Pure Spring Boot implementation with Spring MVC and JPA
- **Frontend**: React/TypeScript application with container-first development workflow
- **Database**: PostgreSQL 17+ running in Docker containers
- **All Services**: Containerized and orchestrated via Docker Compose

## Architecture & Entry Points

### 🐳 Docker-First Implementation Strategy

**ALL DEVELOPMENT happens in Docker containers**. The system provides dual implementations, both designed to run in production Docker containers:

#### Option A: Legacy SparkJava Implementation (`app/` folder)
**Status**: ACTIVE - Currently migrating to Spring Boot
**Runtime**: Docker container with JAR execution

**Core Architecture**: Custom dependency injection via `ApplicationContext`. Three runtime modes (all Docker-ready):
- **API Server**: `docker run fin-backend java -jar app.jar api` → REST API on port 8080
- **Console**: `docker run -it fin-backend java -jar app.jar` → interactive menu system
- **Batch**: `docker run fin-backend java -jar app.jar --batch [command]` → automated processing

#### Option B: Modern Spring Boot Implementation (`spring-app/` folder) - **🐳 RECOMMENDED**
**Status**: PRODUCTION-READY - Full Docker containerization
**Runtime**: `docker run -p 8080:8080 fin-spring-backend`

**Core Architecture**: Spring Boot 3.2.0 with Spring MVC and Spring Data JPA:
- **API Server**: `docker run fin-spring-backend` → REST API on port 8080
- **Database**: Spring Data JPA repositories with Hibernate (Docker PostgreSQL)
- **Security**: Spring Security with JWT authentication
- **Configuration**: Spring Boot `application.properties` with Docker environment variables

### Choosing Between Implementations

| Criteria | Use `app/` (SparkJava) | Use `spring-app/` (Spring Boot) |
|----------|----------------------|-------------------------------|
| **🐳 Docker Compatibility** | ✅ Container-ready | ✅ **FULL CONTAINERIZATION** |
| **New Development** | ❌ Avoid | ✅ **RECOMMENDED** |
| **Enterprise Features** | ❌ Limited | ✅ Security, Testing, Monitoring |
| **Learning/Prototyping** | ✅ Lightweight | ❌ More complex |
| **Production Deployment** | ⚠️ Legacy containers | ✅ **DOCKER PRODUCTION** |
| **Migration Status** | 🔄 In Progress | ✅ Complete |

## 🚀 JAR-First Development Workflow (🐳 Docker Production Ready)

**MANDATORY**: All development and testing uses JAR files directly - same as production Docker containers.

### Spring Boot Implementation (🐳 RECOMMENDED - Full Docker)
```bash
# Build JAR (only when code changes)
cd spring-app && ./gradlew build

# Run API server (production-same)
java -jar spring-app/build/libs/spring-app.jar &

# Test endpoints systematically
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/companies
```

### Legacy SparkJava Implementation (🐳 Docker Compatible)
```bash
# Build JAR (only when code changes)
cd app && ./gradlew build

# Run API server (production-same)
java -jar app/build/libs/app.jar api &

# Run console mode
java -jar app/build/libs/app.jar

# Run batch processing
java -jar app/build/libs/app.jar --batch [command]
```

### 🐳 Docker Production Options (MANDATORY)

**Spring Boot Production**:
```dockerfile
FROM openjdk:17-jre-slim
COPY spring-app/build/libs/spring-app.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
```

**Legacy Production**:
```dockerfile
FROM openjdk:17-jre-slim
COPY app/build/libs/app.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar", "api"]
```

**Why JAR-First?**
- ✅ **Dev = Prod**: Eliminates deployment surprises
- ✅ **🐳 Docker-ready**: Containerized from day one
- ✅ **No Gradle daemon issues**: Reliable API testing
- ✅ **Systematic endpoint testing**: Build frontend incrementally
- ✅ **🐳 Production confidence**: Test against real deployment artifact

## Implementation Migration Strategy

### Current Status (November 2025)
- **Legacy `app/`**: Contains both SparkJava and Spring Boot dependencies during migration
- **Modern `spring-app/`**: Clean Spring Boot implementation, fully functional
- **Migration Path**: Gradually move features from `app/` to `spring-app/`

### Migration Checklist
- [ ] Core services migrated (`CompanyService`, `TransactionClassificationService`, etc.)
- [ ] API endpoints parity achieved
- [ ] Database schema compatibility verified
- [ ] Frontend integration tested with both backends
- [ ] Performance benchmarks completed
- [ ] Documentation updated
- [ ] Production deployment validated

### Development Recommendations
1. **🐳 New Features**: Implement in `spring-app/` first - **TEST IN DOCKER**
2. **🐳 Bug Fixes**: Apply to both during migration - **VERIFY IN DOCKER**
3. **🐳 Testing**: Test both implementations against frontend - **DOCKER CONTAINERS ONLY**
4. **🐳 Production**: Use `spring-app/` for new deployments - **DOCKER DEPLOYMENT**

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

**Service Registration**:
- **Spring Boot**: Use `@Service`, `@Repository`, `@Controller` annotations - Spring handles registration automatically
- **Legacy**: Always register new services in `ApplicationContext.initialize*Services()` methods:
```java
// In ApplicationContext (legacy only)
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

**Development Modes** (JAR-First Approach):
```bash
# Build JAR (only when code changes)
./gradlew build

# API Server (Docker-same as production)
java -jar app/build/libs/app.jar api &

# Console Application
java -jar app/build/libs/app.jar

# Batch Processing
java -jar app/build/libs/app.jar --batch [command]

# Docker Development (optional)
docker build -t fin-backend .
docker run -p 8080:8080 fin-backend
```

**Production Deployment**:
```bash
# Build and deploy JAR to Docker
./gradlew build
docker build -t fin-backend .
docker run -d -p 8080:8080 --env-file .env fin-backend
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

#### 3. **TESTING WORKFLOW** (JAR-First - Docker Ready)
```bash
# Build JAR for testing
./gradlew build

# Start API server (same as production Docker)
java -jar app/build/libs/app.jar api &
API_PID=$!

# Test ALL endpoints systematically before frontend
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/companies
curl http://localhost:8080/api/v1/companies/1/fiscal-periods
# ... test all endpoints ...

# Frontend connects to containerized backend
# Start frontend: npm run dev (connects to localhost:8080)

# Stop API server
kill $API_PID
```

**🐳 Docker Container Testing** (MANDATORY - Production-Ready):
```bash
# Build and test in Docker (same as production)
docker build -t fin-backend .
docker run -d -p 8080:8080 --name fin-api fin-backend

# Test endpoints against container
curl http://localhost:8080/api/v1/health

# Frontend development against container
# docker run -p 3000:3000 your-frontend-image

# Stop container
docker stop fin-api
```

#### 4. **CONSEQUENCES OF VIOLATION** (🐳 Docker-First Policy)
- Committing untested JAR → production container failures
- Skipping Docker testing → deployment surprises
- Not testing all endpoints → broken frontend integration
- Gradle daemon usage → unreliable API testing
- **FAILURE TO COMPLY** will result in broken containers, runtime errors, deployment issues, and failed frontend integration. This **🐳 DOCKER-FIRST** protocol is **STRICTLY ENFORCED** for all API development and containerization.

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

**Test Structure**:
- **Spring Boot**: JUnit 5 + Spring Boot Test with `@SpringBootTest`
- **Legacy**: JUnit 5 + Mockito with custom test setup

**Environment Variables**:
- **Spring Boot**: Standard `application.properties` with profiles
- **Legacy**: Production uses `DATABASE_*`, tests use `TEST_DATABASE_*`

**License Check**: `LicenseManager.checkLicenseCompliance()` required on all entry points

**API Testing**: CORS enabled for `localhost:3000`. Key endpoints work with both implementations:
- `GET /api/v1/health` - system status
- `POST /api/v1/companies/{id}/upload` - file processing
- `GET /api/v1/companies` - company management
- `POST /api/v1/payroll/process` - payroll processing
- `GET /api/v1/budgets/{id}/variance` - budget analysis
- `GET /api/v1/reports/financial` - financial report generation

## 🐳 Docker Containerization Strategy (MANDATORY)

**🚨 CRITICAL REQUIREMENT 🚨**: Frontend development MUST use Docker containerized backend to ensure production compatibility.

**Container Development Workflow**:
```bash
# Build and run backend in Docker
docker build -t fin-backend .
docker run -d -p 8080:8080 --name fin-api --env-file .env fin-backend

# Verify container is running
curl http://localhost:8080/api/v1/health

# Frontend connects to containerized backend
# npm run dev (configure to use http://localhost:8080)

# Stop container
docker stop fin-api
docker rm fin-api
```

**Why 🐳 Container-First?**
- ✅ **Production accuracy**: Test against same runtime as production
- ✅ **Environment consistency**: Same JVM, dependencies, config
- ✅ **Deployment confidence**: No "works on my machine" issues
- ✅ **Frontend integration**: Test real API calls in container
- ✅ **Zero deployment surprises**: Container = production

## Current Development Focus

**Migration Priority** (November 2025):
- Complete migration from `app/` SparkJava to `spring-app/` Spring Boot
- Validate feature parity between implementations
- Update documentation and workflows
- Frontend integration testing with both backends

**Active Refactoring** (see `docs/development/tasks/TASK_6.*`):
- `AccountClassificationService` (2,230 lines) → extract services
- `InteractiveClassificationService` (2,066 lines) → dependency injection
- Output formatting standardization across services

**Implementation Choice for Tasks**:
- **🐳 New Services**: Implement in `spring-app/` only - **DOCKER TESTING REQUIRED**
- **🐳 Bug Fixes**: Apply to both during migration - **DOCKER VERIFICATION MANDATORY**
- **🐳 API Changes**: Design in Spring Boot first, then port to SparkJava if needed - **CONTAINER COMPATIBILITY FIRST**
- **🐳 Database Changes**: Ensure compatibility with both implementations - **DOCKER ENVIRONMENT TESTING**

**Quality Remediation**: SpotBugs warnings addressed with defensive copying patterns. EI_EXPOSE_REP fixes documented in task files.

## Architecture Decision Records

### ADR-001: Dual Implementation Strategy
**Context**: Need to modernize from SparkJava to Spring Boot while maintaining functionality
**Decision**: Maintain parallel implementations during migration period
**Status**: ACTIVE - Migration in progress
**Consequences**:
- ✅ Zero downtime during migration
- ✅ Feature parity validation
- ✅ Gradual adoption possible
- ❌ Code duplication during transition

### ADR-002: Spring Boot as Primary Framework
**Context**: SparkJava limitations in enterprise features (security, testing, monitoring)
**Decision**: Standardize on Spring Boot for all new development
**Status**: ACCEPTED
**Consequences**:
- ✅ Enterprise-grade features available
- ✅ Better ecosystem and community support
- ✅ Improved maintainability and testing
- ❌ Increased complexity for simple use cases

## Collaboration Protocol

### ⚠️ CRITICAL COLLABORATION REQUIREMENTS

**MANDATORY WORKFLOW**: We work together as a team. Do NOT jump into actions without explicit agreement.

#### Question-Answering Protocol
1. **When Asked a Question**: First analyze the question, provide observations and analysis of the provided information
2. **Outline Approach**: Present your proposed solution or investigation plan clearly
3. **Wait for Agreement**: Only proceed with implementation or further actions after explicit user confirmation
4. **Collaborative Decision Making**: Consider user suggestions and feedback before taking any action

#### Investigation vs Implementation
- **Investigations Allowed**: Database queries, code analysis, file searches, and exploratory queries can be performed proactively to gather information
- **Implementation Requires Agreement**: Code changes, database modifications, or system alterations must wait for explicit user approval
- **No Direct Database CRUD**: Do not perform direct database operations (INSERT, UPDATE, DELETE). Instead, write application code that handles these tasks through proper service/repository patterns

#### Communication Standards
- **Clear Explanations**: Always explain WHAT you plan to do, WHY, and HOW before proceeding
- **User Confirmation Required**: Wait for explicit statements like "yes, proceed" or "that approach looks good" before implementing
- **Joint Problem Solving**: Work together to identify root causes and develop solutions

When modifying financial processing logic, maintain the existing transaction flow and update relevant task documentation. The system processes real financial data - ensure accuracy over speed.

**🐳 FINAL REMINDER: EVERYTHING RUNS IN DOCKER** 🚨
- **NO CODE CHANGES WITHOUT DOCKER TESTING**
- **ALL ENDPOINTS MUST WORK IN CONTAINERS**
- **FRONTEND INTEGRATION REQUIRES CONTAINERIZED BACKEND**
- **PRODUCTION = DOCKER CONTAINER - TEST ACCORDINGLY**
