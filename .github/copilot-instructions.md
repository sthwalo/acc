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
- *
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

## Architecture Overview
- **Backend**: Java 17 + Spring Boot 3.3 + PostgreSQL
- **Frontend**: React 19 + TypeScript + Vite
- **Modular Services**: 5 core services (TransactionClassifier, AccountManager, JournalEntryGenerator, RuleMappingService, TransactionBatchProcessor)
- **Data Flow**: PDF Bank Statements → OCR/Text Extraction → Transaction Parsing → Classification → Journal Entries → PostgreSQL → Financial Reports

## Key Directories & Structure
- `app/src/main/java/fin/` - Main Java source code
  - `service/` - Business logic services (classification/, journal/, parser/, reporting/, transaction/, upload/)
  - `controller/spring/` - REST API controllers (all prefixed with "Spring")
  - `entity/` - JPA entities (Company, BankTransaction, JournalEntry, etc.)
  - `repository/` - Spring Data JPA repositories
  - `dto/` - Data transfer objects

- `frontend/src/components/` - React components (CompaniesView, TransactionsView, PayrollManagementView, etc.)
- `docs/system_architecture/` - Technical specifications and implementation details

## Development Workflows

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

### Testing
```bash
# Backend tests
./gradlew test

# Frontend tests
cd frontend && npm test
```

## Code Patterns & Conventions

### Service Naming
- All Spring services prefixed with "Spring" (e.g., `SpringTransactionClassificationService`)
- Business logic services in dedicated subdirectories (classification/, journal/, etc.)
- Single source of truth: `AccountClassificationService` for all classification logic

### API Patterns
- Base path: `/api/v1/`
- Controllers return `ApiResponse<T>` wrapper
- CORS enabled for `http://localhost:3000`
- File uploads: `POST /companies/{id}/upload` for PDF processing

### Database Patterns
- PostgreSQL with Flyway migrations
- Full ACID compliance with foreign key constraints
- Multi-company data isolation
- Real financial data with 7,000+ transactions

### Error Handling
- Custom `BusinessException` with `ErrorCode` enum
- Comprehensive exception management across all services
- Transaction-safe operations with `@Transactional`

### Build Configuration
- Gradle with Spring Boot plugin
- Java 17 toolchain requirement
- Code quality: Checkstyle + SpotBugs
- Custom tasks for PDF analysis and OCR extraction

## External Dependencies & Integration

### Document Processing
- **PDF Text Extraction**: Apache PDFBox 3.0.0
- **OCR**: Tesseract via Tess4J 5.9.0 (for image-based PDFs)
- **Excel Generation**: Apache POI 5.2.4 (professional financial reports)
- **Email**: Spring Mail + Angus Mail (payslip distribution)

### South African Tax Compliance
- SARS-compliant tax calculations (PAYE, UIF, SDL)
- EMP 201 report generation
- Automatic SDL calculation (1% of payroll > R500k)
- Tax number validation and form compliance

### Security
- Spring Security with JWT tokens
- Role-based access control
- Multi-factor authentication support
- Audit logging for all financial operations

## Common Development Tasks

### Adding New Transaction Classification Rules
1. Update `AccountClassificationService.classifyTransaction()`
2. Add pattern matching in `SpringTransactionClassificationService`
3. Test with real bank statement data
4. Update unit tests in `src/test/`

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

## Testing & Validation

### Backend Testing
- JUnit 5 with Mockito for unit tests
- Spring Boot Test for integration tests
- H2 in-memory database for testing
- 118+ existing tests covering all services

### Frontend Testing
- Vitest for unit testing
- React Testing Library patterns
- TypeScript strict mode enabled

### Data Validation
- Real financial data with 45+ accounts
- Transaction balance verification
- Double-entry accounting validation
- SARS compliance checking

## Deployment & Production

### Containerized Deployment
- Multi-stage Docker builds
- PostgreSQL + Spring Boot + React containers
- Health checks and service discovery
- Environment-specific configurations

### Licensing
- Dual license: Apache 2.0 for personal use, Commercial for business
- Copyright headers required on all source files
- License checking scripts in `scripts/`

## Key Files for Understanding
- [System Architecture](../docs/system_architecture/SYSTEM_ARCHITECTURE.md)
- [Technical Specifications](../docs/system_architecture/TECHNICAL_SPECIFICATIONS.md)
- [API Documentation](../app/src/main/java/fin/controller/) - Controller classes
- [Database Schema](../docs/technical/POSTGRESQL_MIGRATION_GUIDE.md)
- [Build Configuration](../app/build.gradle.kts)

## 🔒 CRITICAL SECURITY POLICY - ZERO TOLERANCE

### ⚠️ MANDATORY SECURITY REQUIREMENTS - NEVER COMMIT CREDENTIALS

**SECURITY PRINCIPLE**: `.env` file is the SINGLE SOURCE OF TRUTH for all credentials. NO credentials, passwords, database names, usernames, API keys, or secrets should EVER be hardcoded in committed files.

#### 🚫 ABSOLUTELY FORBIDDEN (Will Be Rejected in Code Review)

**Pattern 1: Hardcoded Credentials in Code**
```java
// ❌ FORBIDDEN - Never hardcode credentials
String dbUrl = "jdbc:postgresql://localhost:5432/your_database";
String dbUser = "your_username";
String dbPassword = "your_password";
Connection conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
```

**Pattern 2: Default/Fallback Credentials in Properties**
```properties
# ❌ FORBIDDEN - Never use real credentials as defaults
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/your_database}
spring.datasource.username=${DATABASE_USER:your_username}
spring.datasource.password=${DATABASE_PASSWORD:your_password}
```

**Pattern 3: Hardcoded Paths with Usernames**
```java
// ❌ FORBIDDEN - Never hardcode user-specific paths
String basePath = "/Users/your_username/your_project";
```

**Pattern 4: Weak Default Secrets**
```properties
# ❌ FORBIDDEN - Never use weak defaults for production secrets
fin.jwt.secret=${JWT_SECRET:fin-secret-key-change-in-production}
```

**Pattern 5: Database Names in Documentation**
```bash
# ❌ AVOID - Use environment variables in docs
psql -U your_username -d your_database
```

#### ✅ REQUIRED SECURITY PATTERNS (Mandatory Across Entire Codebase)

**Correct Pattern 1: Environment Variables Only**
```java
// ✅ CORRECT - Always fetch from environment
String dbUrl = System.getenv("DATABASE_URL");
String dbUser = System.getenv("DATABASE_USER");
String dbPassword = System.getenv("DATABASE_PASSWORD");

if (dbUrl == null || dbUser == null || dbPassword == null) {
    throw new IllegalStateException(
        "Database credentials missing. Set DATABASE_URL, DATABASE_USER, and DATABASE_PASSWORD in .env file"
    );
}
```

**Correct Pattern 2: Properties with NO Defaults**
```properties
# ✅ CORRECT - Force environment variables, no defaults
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USER}
spring.datasource.password=${DATABASE_PASSWORD}
fin.jwt.secret=${JWT_SECRET}
```

**Correct Pattern 3: Dynamic Path Detection**
```java
// ✅ CORRECT - Detect paths dynamically
String basePath = System.getenv("FIN_BASE_PATH");
if (basePath == null) {
    basePath = System.getProperty("user.dir");
}
```

**Correct Pattern 4: Strong Secrets Required**
```properties
# ✅ CORRECT - Require strong secrets from environment
fin.jwt.secret=${JWT_SECRET}  # Must be set in .env, minimum 32 characters
```

**Correct Pattern 5: Generic Documentation Examples**
```bash
# ✅ CORRECT - Use placeholders in documentation
source .env  # Load credentials from .env
psql -U $DATABASE_USER -d $(basename $DATABASE_URL)
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

#### 🚨 IMMEDIATE ACTION REQUIRED IF CREDENTIALS EXPOSED

If credentials are accidentally committed:
1. **Immediately rotate ALL exposed credentials** (change passwords, regenerate keys)
2. **Remove from Git history**: `git filter-branch` or `BFG Repo-Cleaner`
3. **Force push cleaned history**: `git push --force`
4. **Update `.env` with new credentials**
5. **Audit all services using exposed credentials**
6. **Document incident in `docs/security/INCIDENT_*.md`**

#### 📋 SECURITY SCANNING COMMANDS

**Scan for exposed credentials before commit:**
```bash
# Scan for potential credential exposure
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "(password|PASSWORD|secret|SECRET|api.?key|API.?KEY).*=.*['\"].*['\"]" .

# Scan for database names
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "jdbc:postgresql://.*5432/[a-zA-Z_]+" .

# Scan for usernames in connection strings
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "user.*=.*['\"][a-zA-Z0-9_]+['\"]" .

# Scan for hardcoded paths
grep -rn --exclude-dir={.git,node_modules,build,dist} \
  -E "/Users/[a-zA-Z0-9]+/" .
```

## Getting Started Tips
1. Always run `./start.sh` for full development environment
2. Check `docker compose logs -f` for troubleshooting
3. Use real bank statement PDFs from `docs/` for testing
4. Review existing services before adding new ones
5. Maintain the modular service architecture pattern</content>
<parameter name="filePath">.github/copilot-instructions.md