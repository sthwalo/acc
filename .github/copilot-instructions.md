# FIN Financial Management System - Copilot Instructions

## 🐳 DOCKER-FIRST DEVELOPMENT - CRITICAL REQUIREMENT

**🚨 EVERYTHING RUNS THROUGH DOCKER 🚨**

**MANDATORY POLICY**: All development, testing, and production deployment MUST use Docker containers. No direct JAR execution allowed. This ensures:
- ✅ **Zero deployment surprises**: Dev environment = Production environment
- ✅ **Environment consistency**: Same JVM, dependencies, and configuration
- ✅ **Frontend integration**: Test against real containerized APIs
- ✅ **Production confidence**: Container = production deployment

## Project Overview
FIN is a production-ready financial management system built in Java 17 with PostgreSQL 17+, handling 7,156+ real transactions. It provides comprehensive financial document processing including bank statement processing, financial reporting, payroll management, budget planning, and REST API capabilities for South African businesses with SARS compliance.

**Architecture Overview**:
- **Spring Boot `spring-app/`**: Pure Spring Boot implementation with Spring MVC and JPA
- **Frontend**: React/TypeScript application with container-first development workflow
- **Database**: PostgreSQL 17+ running in Docker containers
- **All Services**: Containerized and orchestrated via Docker Compose

## Architecture & Entry Points

The system is built with Spring Boot and runs in production Docker containers:

#### Spring Boot Implementation (`spring-app/` folder)
- **Status**: Production-ready with full containerization
- **Runtime**: `docker run -p 8080:8080 fin-app`
- **Features**: Spring MVC, JPA, Security, JWT authentication

### 🐳 Docker Production Options (MANDATORY)

**Build Metadata Requirements**:
- ✅ **Include Author**: All Docker images MUST include `LABEL author="Immaculate Nyoni <sthwaloe@gmail.com>"`
- ✅ **Copyright**: Copyright information is in the codebase (LICENSE, NOTICE files) - ensure it's included in builds

**Spring Boot Production**:
```dockerfile
FROM openjdk:17-jre-slim
LABEL author="Immaculate Nyoni <sthwaloe@gmail.com>"
COPY spring-app/build/libs/fin-spring.jar /app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app.jar"]
```

**Frontend Production**:
```dockerfile
FROM nginx:alpine
LABEL author="Immaculate Nyoni <sthwaloe@gmail.com>"
COPY dist/ /usr/share/nginx/html/
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
```

## Implementation Strategy

**Current Status**: Spring Boot is the primary and only implementation for new development.

**Development Recommendations**:
- **🐳 New Features**: Implement in `spring-app/` - **TEST IN DOCKER**
- **🐳 Bug Fixes**: Apply fixes in `spring-app/` - **VERIFY IN DOCKER**
- **🐳 Testing**: Test implementation against frontend - **DOCKER CONTAINERS ONLY**
- **🐳 Production**: Use `spring-app/` for all deployments - **DOCKER DEPLOYMENT**

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
Data data = fetchFromDatabase(conn, id);
if (data == null) {
    data = createDefaultData(); // FORBIDDEN!
}
```

**Pattern 2: Try-Catch with Fallback**
```java
try {
    return fetchFromDatabase(conn, id);
} catch (SQLException e) {
    return createDefaultData(); // FORBIDDEN!
}
```

**Pattern 3: Hardcoded Business Data**
- Avoid methods named `createDefault*()`, `useFallback*()`, `getTemplate*()`
- No hardcoded strings > 50 characters (except SQL/log messages)
- No inline default values for business data

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

#### 🔍 CODE REVIEW CHECKLIST

Before committing ANY code, verify:
- [ ] No fallback methods (`createDefault*`, `useFallback*`, `getTemplate*`)
- [ ] No hardcoded strings > 50 chars (except SQL/log messages)
- [ ] All database fetch methods throw SQLException when empty
- [ ] Exception messages specify exact table and SQL INSERT example
- [ ] `./gradlew compileJava` succeeds

#### 📊 SUCCESS METRICS

A successful refactoring should show:
- **Code Reduction**: 10-20% fewer lines (fallback methods deleted)
- **Zero Fallbacks**: 0 occurrences of forbidden patterns
- **Clear Errors**: All SQLException messages specify table + INSERT statement
- **Clean Build**: No compilation errors or warnings

**PDF Library Standards - CRITICAL**: 
- ✅ **MUST USE**: Apache PDFBox 3.0.0 for PDF reading/text extraction
- ✅ **MUST USE**: libharu via JNA for complex PDF generation (payslips, invoices)
- ❌ **NEVER USE**: iText (commercial licensing restrictions)
- **When to Use Which**: PDFBox for simple reports/PDF reading; libharu for complex layouts

**Service Registration**:
- **Spring Boot**: Use `@Service`, `@Repository`, `@Controller` annotations - Spring handles registration automatically

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
cd /Users/sthwalonyoni/FIN && ./start.sh.    
cd /Users/sthwalonyoni/FIN/spring-app && ./gradlew clean build  # Full build with quality checks
./gradlew test                          # Run test suite
./gradlew checkstyleMain                # Code quality scan
```

**Database Setup**:
```bash
source .env                             # Load database credentials
# Requires: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD
# Tests use: TEST_DATABASE_URL, TEST_DATABASE_USER, TEST_DATABASE_PASSWORD
```

**Local Development**:
```bash
# Start both backend and frontend locally for development (hot reload enabled)
./dev.sh

# Alternative: Start services individually
# Backend only: cd spring-app && ./gradlew bootRun
# Frontend only: cd frontend && npm run dev
```

**Container Management**:
```bash
# Start both backend (fin-app) and frontend (fin-frontend-prod) containers
docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# Start backend container only (fin-app)
docker compose -f docker-compose.yml up -d fin-app

# Restart backend container
docker compose -f docker-compose.yml restart fin-app

# Check container status
docker compose -f docker-compose.yml -f docker-compose.frontend.yml ps

# Stop all containers
docker compose -f docker-compose.yml -f docker-compose.frontend.yml down

# View container logs
docker compose -f docker-compose.yml -f docker-compose.frontend.yml logs -f
```

**Production Deployment**:
```bash
./gradlew build
docker build -t fin-backend .
docker run -d -p 8080:8080 --env-file .env fin-backend
```

## Code Quality & Documentation

**Mandatory Pre-Work Check**: Before ANY file modification:
```bash
# Check existing task documentation
ls -la spring-app/docs/development/tasks/
grep -r "TASK.*" spring-app/docs/development/tasks/ | grep -i "[your-topic]"
cat spring-app/docs/development/tasks/README.md
```

**Quality Protocol**:
1. **Inventory violations**: `./gradlew clean checkstyleMain --no-daemon 2>&1 | grep -E "(ALL)" > violations_inventory.txt`
2. **Fix ONE file at a time** - address ALL violations before moving on
3. **Document ALL work** in `spring-app/docs/development/tasks/TASK_[NUMBER]_[Name].md`
4. **Build verification**: `./gradlew clean build` after every change

### ⚠️ CRITICAL ENFORCEMENT: Code Changes & Testing Protocol

**MANDATORY REQUIREMENT**: After ANY changes to non-test code files (.java, .kt, build.gradle.kts, etc.):

#### 1. **BUILD VERIFICATION** (Required After Every Code Change)
```bash
cd /Users/sthwalonyoni/FIN/spring-app && ./gradlew clean build --no-daemon
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

#### 3. **TESTING WORKFLOW** (Local Development + Container Validation)
```bash
# Option A: Local Development Testing (during active development)
./dev.sh                                 # Start both services locally
# Test endpoints in browser or with curl
curl http://localhost:8080/api/v1/health
curl http://localhost:3000

# Option B: Container Testing (production-same environment)
# Build JAR for testing (if needed)
./gradlew build

# Start services in containers (production-same)
docker compose -f docker-compose.yml -f docker-compose.frontend.yml up -d

# Test backend health (fin-app container)
curl http://localhost:8080/api/v1/health

# Test frontend health (fin-frontend-prod container)  
curl http://localhost:3000

# Test API endpoints
curl http://localhost:8080/api/v1/companies
curl http://localhost:8080/api/v1/companies/1/fiscal-periods

# Stop services
docker compose -f docker-compose.yml -f docker-compose.frontend.yml down
```

#### 4. **CONSEQUENCES OF VIOLATION**
- Committing untested code → production container failures
- Skipping local development testing → development workflow issues
- Skipping Docker compose testing → deployment surprises
- Not testing all endpoints → broken frontend integration
- Direct JAR execution → unreliable API testing

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

## Database Schema Notes

**Companies Table Schema** (PostgreSQL 17):
- Core fields: `id`, `name`, `registration_number`, `tax_number`, `address`, `contact_email`, `contact_phone`, `created_at`, `updated_at`, `logo_path`
- Banking fields: `bank_name`, `account_number`, `account_type`, `branch_code` (recently added)
- Tax compliance: `vat_registered` (boolean, recently added)
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

## Testing & Environment

**Test Structure**:
- **Spring Boot**: JUnit 5 + Spring Boot Test with `@SpringBootTest`

**Environment Variables**:
- **Spring Boot**: Standard `application.properties` with profiles

**License Check**: `LicenseManager.checkLicenseCompliance()` required on all entry points

## Current Development Focus

**Primary Framework**: Spring Boot is the standard framework for all new development and maintenance.

## Architecture Decision Records

**ADR-001: Spring Boot as Primary Framework** - Standardize on Spring Boot for enterprise features, security, and testing capabilities.

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
