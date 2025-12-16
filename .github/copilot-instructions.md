# AI Coding Agent Instructions for FIN Financial Management System

## Project Overview
FIN is a production-ready financial management system for South African small businesses, handling bank statement processing, accounting, payroll, tax compliance (SARS), and financial reporting. The system processes 7,000+ real financial transactions with full double-entry accounting.

## Collaboration Protocol

### ⚠️ CRITICAL COLLABORATION REQUIREMENTS

**MANDATORY WORKFLOW**: We work together as a team. Do NOT jump into actions without explicit agreement.

#### 1. Phased Task Execution and Agreement
- **One Task at a Time**: Proceed through the project one task at a time, based on the established project plan
- **Mutual Agreement Required**: Mutual agreement on a task's scope and expected outcome is required before implementation begins
- **Pause and Explain**: If either of us needs clarification or a pause during execution, we commit to stopping and explaining the context thoroughly
- **Shared Ownership**: Both parties must understand and agree on what will be done before proceeding

#### 2. Code Implementation and Verification
- **Adherence to Architecture**: All code changes and configuration updates must adhere to the suggestions and architectural decisions we mutually agree upon
- **Rigorous Testing**: Use rigorous testing practices and verify fixes/implementations together before marking a task as complete
- **Shared Quality**: Verify together before proceeding to the next stage - this ensures shared ownership of the solution's integrity
- **No Premature Commits**: Do not commit or push code until user explicitly confirms the fix works

#### 3. Thorough Analysis and Documentation
- **Analysis First**: Before implementing any solution, commit to analyzing all related files, dependencies, and potential downstream impacts
- **Shared Understanding**: This analysis phase is crucial for ensuring we both have a shared understanding of:
  - **What** needs to be changed
  - **Where** those changes occur
  - **Why** they are necessary
  - **How** they impact the rest of the system
- **Document Key Decisions**: Document architectural decisions and significant changes in appropriate locations

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
- **Transparency**: If uncertain about any aspect, ask for clarification rather than making assumptions

#### **USER VERIFICATION** (CRITICAL - DO NOT SKIP)
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

### ⚠️ CRITICAL RULE: NO FALLBACK DATA ALLOWED - ZERO TOLERANCE POLICY

**ARCHITECTURAL PRINCIPLE**: Database is the SINGLE SOURCE OF TRUTH. ALL business data, templates, compliance text, and configuration MUST come from database. If database is empty, the system MUST throw a clear exception - NEVER silently fall back to hardcoded data.

#### Why? This is Better for SOC/DRY/Small Classes:
- **SOC**: Entity = persistence, DTO = API transfer, Service = business logic.
- **DRY**: Avoids repeating relationship-handling logic across methods; centralizes in service.
- **Small Methods**: Service method becomes a simple mapper + saver; no bloated entity manipulation.
- **Maintainability**: Changes to API fields only affect the DTO; entities stay lean.

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
- [ ] No fallback methods (`createDefault*`, `useFallback*()`, `getTemplate*()`)
- [ ] No hardcoded strings > 50 chars (except SQL/log messages)
- [ ] All database fetch methods throw SQLException when empty
- [ ] Exception messages specify exact table and SQL INSERT example
- [ ] `./gradlew compileJava` succeeds

#### 📊 SUCCESS METRICS

A successful refactoring should show:
- **Code Reduction**: 10-20% fewer lines (fallback methods deleted)
- **Zero Fallbacks**: 0 occurrences of forbidden patterns
- **Clear Errors**: All SQLException messages specify table + INSERT statement

## Architecture Overview
- **Backend**: Java 17 + Spring Boot 3.3 + PostgreSQL + JPA
- **Frontend**: React 19 + TypeScript + Vite
- **Modular Services**: 5 core services (TransactionClassifier, AccountManager, JournalEntryGenerator, RuleMappingService, TransactionBatchProcessor)
- **Data Flow**: PDF Bank Statements → OCR/Text Extraction → Transaction Parsing → Classification → Journal Entries → PostgreSQL → Financial Reports

## Critical Developer Workflows

### Full-Stack Development (Recommended)
```bash
# Build both backend/frontend and start containers with auto browser launch
./start.sh

# Alternative: Start containers from frontend directory
cd frontend && npm run start:containers
```

### Backend-Only Development
```bash
# Build JAR
./gradlew build

# Run API server
java -jar app/build/libs/app.jar api

# Run console application
java -jar app/build/libs/app.jar
```

### Frontend-Only Development
```bash
cd frontend
npm install
npm run dev  # Runs on port 3000
```

## Key Architectural Patterns

### Service Layer Organization
- **Spring Services**: All prefixed with "Spring" (e.g., `SpringCompanyService`, `SpringTransactionClassificationService`)
- **Business Logic Services**: Organized in subdirectories (`classification/`, `journal/`, `parser/`, `reporting/`, `transaction/`, `upload/`)
- **Repository Pattern**: Spring Data JPA repositories in `repository/` package
- **Transactional**: All service methods use `@Transactional`

### API Patterns
- **Base Path**: `/api/v1/`
- **Response Wrapper**: All controllers return `ApiResponse<T>`
- **CORS**: Enabled for `http://localhost:3000`
- **Controllers**: Located in `controller/spring/` package, all prefixed with "Spring"

### Database Patterns
- **PostgreSQL**: Production database with Flyway migrations
- **JPA Entities**: Located in `entity/` package
- **Connection**: Environment variables only (`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`)
- **ACID Compliance**: Full transaction safety for financial operations

### Frontend Patterns
- **API Communication**: Axios-based `ApiService.ts` with TypeScript interfaces
- **Proxy Configuration**: Vite proxies `/api` requests to backend (port 8080)
- **Container Mode**: `CONTAINER_MODE=true` environment variable for containerized development
- **Component Structure**: Feature-based components in `components/` directory

## Security Requirements (ZERO TOLERANCE)

### 🚫 ABSOLUTELY FORBIDDEN
- **No Hardcoded Credentials**: Never commit database URLs, usernames, passwords, or API keys
- **No Fallback Data**: Database is single source of truth - throw `SQLException` if data missing
- **No Weak Defaults**: Environment variables required, no fallback values

### ✅ REQUIRED PATTERNS
```java
// Correct: Environment variables only
String dbUrl = System.getenv("DATABASE_URL");
if (dbUrl == null) {
    throw new IllegalStateException("DATABASE_URL environment variable required");
}

// Correct: Fail-fast for missing database data
List<Data> data = repository.findByCompanyId(companyId);
if (data.isEmpty()) {
    throw new SQLException("No data found for company " + companyId + ". Insert data first.");
}
```

#### 🔍 SECURITY CODE REVIEW CHECKLIST

Before committing ANY code, verify:
- [ ] No hardcoded database names (use environment variables)
- [ ] No hardcoded usernames (use environment variables)
- [ ] No hardcoded passwords (any string that looks like a password)
- [ ] No hardcoded file paths with usernames (`/Users/username/`)
- [ ] No weak default secrets in properties files
- [ ] All credentials fetched from `System.getenv()` or `${ENV_VAR}`
- [ ] `.gitignore` includes `.env` and all credential files
- [ ] No credentials in log statements or error messages
- [ ] No credentials in documentation (use `$DATABASE_USER` instead)
- [ ] Application fails fast if environment variables missing

#### 📊 FILES THAT MUST NEVER CONTAIN CREDENTIALS

**Configuration Files (Committed to Git)**:
- ❌ `application.properties` - Use `${ENV_VAR}` only
- ❌ `application.properties.example` - Use placeholder values only
- ❌ `docker-compose.yml` - Use `${ENV_VAR}` only
- ❌ `Dockerfile` - Never hardcode secrets
- ❌ `.md` documentation files - Use generic examples

**Code Files (Committed to Git)**:
- ❌ `*.java` - Fetch from `System.getenv()`
- ❌ `*.ts` - Fetch from `import.meta.env.VITE_*`
- ❌ `*.sh` - Source from `.env` file
- ❌ `*.sql` - Use parameterized queries

**Files That CAN Contain Credentials (MUST be in .gitignore)**:
- ✅ `.env` - Real credentials here (gitignored)
- ✅ `secrets/*` - Encrypted secrets (gitignored)
- ✅ `*.key`, `*.pem` - Private keys (gitignored)

## Code Quality Standards

### File Headers
All source files must include comprehensive copyright headers:
```java
/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */
```

### Build & Test
```bash
# Backend tests
./gradlew test

# Frontend tests
cd frontend && npm test

# Code quality checks
./gradlew checkstyleMain checkstyleTest
./gradlew spotbugsMain
```

## Integration Points

### Document Processing
- **PDF Text Extraction**: Apache PDFBox 3.0.0
- **OCR**: Tesseract via Tess4J 5.9.0
- **Excel Generation**: Apache POI 5.2.4
- **Email**: Spring Mail + Angus Mail

### South African Tax Compliance
- **SARS Integration**: EMP 201 reports, PAYE/UIF/SDL calculations
- **Tax Number Validation**: Built-in South African tax number validation
- **Compliance Reports**: Automatic tax form generation

## Development Environment Setup

### Containerized Development
```yaml
# docker-compose.yml key services
fin-app: # Spring Boot backend (port 8080)
fin-frontend: # React frontend (port 3000)
postgres: # PostgreSQL database
```

### Environment Variables Required
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/fin_database
DATABASE_USER=fin_user
DATABASE_PASSWORD=secure_password_here
```

## Common Development Tasks

### Adding New Transaction Classification Rules
1. Update `AccountClassificationService.classifyTransaction()`
2. Add pattern matching in `SpringTransactionClassificationService`
3. Test with real bank statement data
4. Update unit tests

### Implementing New Financial Reports
1. Add method to `reporting/` services
2. Create Excel template in `SpringExportController`
3. Use Apache POI for professional formatting
4. Add to `GenerateReportsView.tsx` frontend component

### Payroll Processing
1. Use `SpringPayrollController` for employee management
2. Tax calculations in dedicated service methods
3. PDF payslip generation with JNA/Libharu
4. Email distribution via SMTP

## Key Files for Understanding
- **Architecture**: `docs/system_architecture/SYSTEM_ARCHITECTURE.md`
- **Technical Specs**: `docs/system_architecture/TECHNICAL_SPECIFICATIONS.md`
- **Database Schema**: `docs/technical/POSTGRESQL_MIGRATION_GUIDE.md`
- **Main Application**: `app/src/main/java/fin/FinApplication.java`
- **API Controllers**: `app/src/main/java/fin/controller/spring/`
- **Business Services**: `app/src/main/java/fin/service/`
- **Frontend App**: `frontend/src/App.tsx`
- **API Service**: `frontend/src/services/ApiService.ts`