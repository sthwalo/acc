# AI Coding Agent Instructions for FIN Financial Management System

**Last Updated:** October 12, 2025
**System Status:** ✅ Production Ready | **Database:** PostgreSQL 12+ | **Java:** 17+ | **Test Coverage:** 118+ tests

## 🏢 System Architecture Overview

FIN is a production-ready Java 17+ financial management system processing **7,156+ real transactions** with PostgreSQL database, featuring modular service architecture with dependency injection and three execution modes.

### Core Architecture Pattern
- **Dependency Injection**: All services managed via `ApplicationContext` (see `fin.context.ApplicationContext`)
  - Services registered in `HashMap<Class<?>, Object>` during initialization
  - Services retrieved via `context.get(ServiceClass.class)` - NEVER manual instantiation
  - Automatic database connection testing and credential injection from `.env`
- **Repository Pattern**: Data access layer (`fin.repository.*`) with JDBC-based implementation
- **Service Layer**: Business logic (`fin.service.*`) with 15+ services for different domains
- **Controller Layer**: Application flow coordination (`fin.controller.*`) for different feature areas
- **Model Layer**: Mutable POJOs (`fin.model.*`) with getters/setters representing domain entities

### Database Schema (PostgreSQL)
- **companies**: Multi-tenant company management with fiscal period tracking
- **fiscal_periods**: Date ranges for financial reporting (start_date, end_date)
- **accounts**: Chart of accounts with codes (e.g., 8100=Employee Costs, 9600=Bank Charges)
- **bank_transactions**: Raw bank data with classification fields (account_code, account_name)
- **journal_entries** + **journal_entry_lines**: Double-entry accounting entries
- **transaction_mapping_rules**: Pattern-based classification rules (e.g., "XG SALARIES" → 8100)
- **employees**, **payroll_periods**, **payslips**: Payroll with PAYE/UIF/SDL calculations
  - **payslips.sdl_levy**: Skills Development Levy (1% of gross, added Oct 2025)

### Processing Pipeline
```
Bank PDFs → DocumentTextExtractor (PDFBox 3.0.0) → BankStatementProcessingService → 
  → TransactionClassificationService (pattern matching + interactive fallback) → 
  → JournalEntryGenerator (double-entry validation) → 
  → PostgreSQL → Financial Reports (Console/Excel/PDF)
```

### Application Modes
1. **API Server** (Production): `./gradlew run --args="api"` - REST API at http://localhost:8080/api/v1/ (CORS enabled for localhost:3000)
2. **Console Application**: `./gradlew run` or `./run.sh` - Interactive terminal with menu system  
3. **Batch Processing**: `./gradlew run --args="--batch [command]"` - Automated background processing

**Entry Points:**
- `fin.ApiApplication.main()` → REST API mode
- `fin.ConsoleApplication.main()` → Interactive console mode
- `fin.AppTransition.main()` → Legacy/migration entry point

## 🔧 Critical Developer Workflows

### Environment Setup
```bash
# 1. Ensure PostgreSQL 12+ is running
# 2. Create .env file in project root with database credentials:
#    DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_db
#    DATABASE_USER=your_username
#    DATABASE_PASSWORD=your_password
# 3. DatabaseConfig.java automatically loads .env and falls back to system env vars

source .env              # Load environment variables (optional, DatabaseConfig handles this)
./gradlew clean spotbugsMain # Run SpotBugs analysis
./gradlew clean build          # Compile and package application
./gradlew test           # Run all tests (maxHeapSize=2G configured)
./gradlew clean build          # Compile and package application
```


### Running the Application
```bash
# API Server (production mode - recommended for frontend integration)
./gradlew run --args="api"
# OR: java -jar app/build/libs/app-fat.jar api
# Accessible at http://localhost:8080/api/v1/
# CORS enabled for http://localhost:3000

# Console Application (interactive menu)
./gradlew run
# OR use the convenience script:
./run.sh

# Batch Processing (automated tasks)
./gradlew run --args="--batch process-transactions"

# Specialized test runners (see build.gradle.kts)
./gradlew runClassificationTest      # Test transaction classification
./gradlew runTestDatabaseSetup       # Initialize test database
./gradlew runTestConfiguration       # Validate configuration
```

### Database Operations
```bash
# Connect to database
psql -U sthwalonyoni -d drimacc_db -h localhost

# Check transaction count (should be 7,156+)
SELECT COUNT(*) FROM bank_transactions;

# View recent classified transactions
SELECT transaction_date, details, debit_amount, credit_amount, account_code, account_name
FROM bank_transactions 
ORDER BY transaction_date DESC LIMIT 10;

# Check payroll SDL implementation (Oct 2025)
SELECT payslip_number, gross_salary, sdl_levy 
FROM payslips 
WHERE payroll_period_id = 10;  -- September 2025

# Verify chart of accounts
SELECT code, name, category FROM accounts ORDER BY code;
```

### Build & Test Workflow
```bash
# Clean and analyze code
./gradlew clean spotbugsMain # Run SpotBugs analysis

# Standard build with tests (ALWAYS use ./gradlew clean build for builds)
./gradlew clean build          # Compile and package application
# Fast build (REQUIRED after code changes - skip tests for speed)
./run.sh                    # Compile and package only (uses run.sh)

# Create deployable artifact (fat JAR with all dependencies)
./gradlew fatJar                 # Creates app/build/libs/app-fat.jar

# Run specific test class
./gradlew test --tests "fin.service.AccountManagerTest"

# Run specific test method
./gradlew test --tests "fin.service.PayrollServiceTest.testSDLCalculation"
```

### Useful Scripts (in `/scripts/`)
```bash
./scripts/backup_database.sh           # Backup PostgreSQL to backups/
./scripts/cleanup-reports.sh           # Delete old reports from exports/
./scripts/show-status.sh               # Display system status
./scripts/demo-api.sh                  # Test API endpoints
./scripts/verify_sdl_implementation.sh # Verify SDL calculations
```

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

## 📋 Project-Specific Conventions

### Package Structure
```
fin/
├── api/                    # REST API endpoints (ApiServer.java)
├── config/                 # Database configuration (DatabaseConfig.java)
├── context/                # Dependency injection (ApplicationContext.java)
├── controller/             # Flow coordination (CompanyController, ReportController, etc.)
├── exception/              # Custom exceptions
├── license/                # License compliance (LicenseManager.java)
├── model/                  # Domain entities (BankTransaction, JournalEntry, Employee, etc.)
├── repository/             # Data access (CompanyRepository, etc.)
├── service/                # Business logic (15+ services)
├── ui/                     # Console UI (ConsoleMenu, InputHandler, OutputFormatter)
├── util/                   # Utilities
├── validation/             # Input validation
├── ApiApplication.java     # API server entry point
├── ConsoleApplication.java # Console app entry point
└── BatchProcessor.java     # Batch processing entry point
```

### Service Instantiation Pattern
```java
// ✅ CORRECT: Always use ApplicationContext for dependency injection
ApplicationContext context = new ApplicationContext();
CompanyService companyService = context.get(CompanyService.class);
PayrollService payrollService = context.get(PayrollService.class);

// ❌ WRONG: Never manually instantiate services with database URLs
// CompanyService companyService = new CompanyService(dbUrl);  // DON'T DO THIS!

// The ApplicationContext automatically:
// 1. Tests database connection via DatabaseConfig.testConnection()
// 2. Initializes all 15+ services in correct dependency order
// 3. Registers services in internal HashMap for lookup
// 4. Injects database URL from .env file
```

### Database Operations
```java
// Repository pattern - constructor injection
public class CompanyRepository {
    private final String dbUrl;
    
    public CompanyRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }
}

// Always use try-with-resources and prepared statements
try (Connection conn = DriverManager.getConnection(dbUrl);
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setLong(1, companyId);
    stmt.setString(2, accountCode);
    ResultSet rs = stmt.executeQuery();
    // Process results
} catch (SQLException e) {
    throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
}
```

### Model Design Pattern
```java
// Models are mutable POJOs with getters/setters
public class BankTransaction {
    private Long id;
    private LocalDate transactionDate;
    private String details;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private String accountCode;      // Classification field
    private String accountName;      // Classification field
    
    // Helper methods
    @Override
    public String toString() { /* formatted output */ }
}

// Journal entries have balance validation
JournalEntry entry = new JournalEntry();
entry.addLine(new JournalEntryLine(accountId, debit, credit));
if (!entry.isBalanced()) {
    throw new ValidationException("Debit and credit amounts must balance");
}
```

### Testing Patterns
```java
// Use Mockito for unit tests (32 test classes covering all services)
@Mock private RuleMappingService mockRuleService;
@Mock private JournalEntryGenerator mockJournalGenerator;
@Mock private Connection mockConnection;
@Mock private PreparedStatement mockPreparedStatement;

@BeforeEach void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    processor = new TransactionBatchProcessor(mockRuleService, mockJournalGenerator);
}

@Test
void testTransactionClassification() {
    // Arrange
    BankTransaction transaction = new BankTransaction();
    transaction.setDetails("XG SALARIES");
    
    // Act
    ClassificationResult result = classifier.classify(transaction);
    
    // Assert
    assertEquals("8100", result.getAccountCode());
    assertEquals("Employee Costs", result.getAccountName());
}

@Test
void testSDLCalculation() {
    // SDL added October 2025 - 1% of gross when payroll > R41,667/month
    BigDecimal grossSalary = new BigDecimal("170100.00");
    BigDecimal expectedSDL = new BigDecimal("1701.00");
    
    BigDecimal actualSDL = SARSTaxCalculator.calculateSDL(grossSalary);
    
    assertEquals(expectedSDL, actualSDL);
}
```

### Console Output Pattern
```java
// Use emoji and formatting for console feedback
System.out.println("🔗 Connecting to database: " + dbUrl);
System.out.println("✅ Operation completed successfully");
System.err.println("❌ Error: " + e.getMessage());
System.out.println("📊 Processing 7,156+ transactions...");
```

## 🔗 Integration Points & Dependencies

### External Libraries (from build.gradle.kts)
```gradle
// Database
implementation("org.postgresql:postgresql:42.7.3")
implementation("com.zaxxer:HikariCP:5.0.1")

// PDF Processing
implementation("org.apache.pdfbox:pdfbox:3.0.0")
implementation("com.itextpdf:itextpdf:5.5.13.3")

// Excel Generation
implementation("org.apache.poi:poi:5.2.4")
implementation("org.apache.poi:poi-ooxml:5.2.4")

// Email (Payroll)
implementation("com.sun.mail:javax.mail:1.6.2")

// REST API
implementation("com.sparkjava:spark-core:2.9.4")
implementation("com.google.code.gson:gson:2.10.1")

// Testing
testImplementation("org.junit.jupiter:junit-jupiter")
testImplementation("org.mockito:mockito-core:5.5.0")
```

### File Processing Pipeline
```java
// Complete flow from PDF to database
// 1. Extract text from PDF
DocumentTextExtractor extractor = new DocumentTextExtractor();
String rawText = extractor.extractText(pdfFile);  // Uses Apache PDFBox 3.0.0

// 2. Parse bank statement into transactions
BankStatementProcessingService parser = new BankStatementProcessingService(dbUrl);
List<BankTransaction> transactions = parser.parseBankStatement(rawText, companyId);

// 3. Classify transactions using TransactionClassificationService (unified entry point)
TransactionClassificationService classifier = new TransactionClassificationService(dbUrl);
for (BankTransaction tx : transactions) {
    ClassificationResult result = classifier.classify(tx);
    tx.setAccountCode(result.getAccountCode());
    tx.setAccountName(result.getAccountName());
}

// 4. Generate journal entries (double-entry bookkeeping)
JournalEntryGenerator generator = new JournalEntryGenerator(dbUrl);
JournalEntry entry = generator.createJournalEntryForTransaction(tx);

// 5. Persist to PostgreSQL
repository.saveBankTransactions(transactions);
repository.saveJournalEntry(entry);
```

### API Endpoints (Spark Java with CORS)
```java
// ApiServer.java - port 8080
public class ApiServer {
    public void start() {
        port(8080);
        setupCors();  // Enable CORS for localhost:3000
        
        // Health check
        get("/api/v1/health", (req, res) -> {
            return gson.toJson(Map.of(
                "status", "healthy",
                "database", DatabaseConfig.testConnection() ? "connected" : "disconnected"
            ));
        });
        
        // Company management
        get("/api/v1/companies", (req, res) -> gson.toJson(companyService.getAllCompanies()));
        post("/api/v1/companies", (req, res) -> /* create company */);
        
        // Bank statement upload
        post("/api/v1/companies/:id/upload", (req, res) -> {
            // Multipart file upload handling
            // Process PDF → extract → classify → store
        });
    }
}
```

## ⚖️ Business Rules & Validation

### Accounting Hierarchy (CRITICAL - Rewritten Oct 2025)
```java
// CORRECT FLOW: Journal Entries → General Ledger → Trial Balance → Financial Statements

// 1. Journal Entries (Source of Truth)
// All transactions MUST be recorded in journal_entries and journal_entry_lines tables
// Enforced at database level with foreign key constraints

// 2. General Ledger (Posted from Journals)
// ✅ CORRECT: GeneralLedgerService reads from journal_entries
List<AccountInfo> accounts = repository.getActiveAccountsFromJournals(companyId, fiscalPeriodId);
// ❌ WRONG: Never read from Trial Balance or bank_transactions for GL

// 3. Trial Balance (Summarized from GL)
// Should verify against GL totals (Phase 2 - pending)
// Must validate: Total Debits = Total Credits

// 4. Financial Statements (Compiled from TB)
// Income Statement: Revenue (6000-7999) and Expense (8000-9999) accounts
// Balance Sheet: Assets (1000-2999), Liabilities (3000-4999), Equity (5000-5999)
// Verify: Assets = Liabilities + Equity
```

### Double-Entry Accounting
```java
// Debit = Credit validation for journal entries
BigDecimal totalDebit = entries.stream().map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
BigDecimal totalCredit = entries.stream().map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
if (totalDebit.compareTo(totalCredit) != 0) {
    throw new ValidationException("Debit and credit amounts must balance");
}
```

### Normal Balance Types
```java
// Account types and their normal balances (for GL calculations)
// Assets (1000-2999): DEBIT normal balance
//   Closing = Opening + Period Debits - Period Credits
// Liabilities (3000-4999): CREDIT normal balance
//   Closing = Opening + Period Credits - Period Debits
// Equity (5000-5999): CREDIT normal balance
//   Closing = Opening + Period Credits - Period Debits
// Revenue (6000-7999): CREDIT normal balance
//   Closing = Period Credits - Period Debits (no opening balance)
// Expenses (8000-9999): DEBIT normal balance
//   Closing = Period Debits - Period Credits (no opening balance)
```

### Transaction Classification
```java
// Unified classification through TransactionClassificationService
TransactionClassificationService classifier = context.get(TransactionClassificationService.class);
ClassificationResult result = classifier.classify(transaction);
if (result.getAccountCode() == null) {
    // Fallback to interactive classification
    result = interactiveClassifier.classifyInteractively(transaction);
}
```

### License Compliance
```java
// Always check license before core operations
if (!LicenseManager.checkLicenseCompliance()) {
    throw new RuntimeException("License compliance check failed");
}
```

## 🧪 Testing Strategy

### Unit Test Coverage
- **118+ test classes** covering all critical services
- **Mockito 5.5.0** for mocking dependencies (Connection, PreparedStatement, etc.)
- **JUnit Jupiter** platform with `maxHeapSize=2G` for memory-intensive tests
- **Test database** isolation via `TEST_DATABASE_URL` env var
- **Test working directory** set to project root for `.env` loading

### Integration Testing
```java
// Test service integration with real database
@Test
void testTransactionBatchProcessorIntegration() {
    // Setup real services with test database
    TransactionBatchProcessor processor = new TransactionBatchProcessor(realRuleService, realJournalGenerator);
    // Execute integration test with actual database queries
}

// Test payroll reprocessing (Oct 2025 feature)
@Test
void testPayrollReprocessing() throws SQLException {
    // Test that reprocessing clears old payslips and recalculates
    Long payrollPeriodId = 10L; // September 2025
    payrollService.processPayroll(payrollPeriodId);
    
    // Verify SDL calculated correctly
    List<Payslip> payslips = payrollService.getPayslips(payrollPeriodId);
    BigDecimal totalSDL = payslips.stream()
        .map(Payslip::getSdlLevy)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    assertTrue(totalSDL.compareTo(BigDecimal.ZERO) > 0);
}
```

## 📊 Financial Reporting

### Report Types
- **Console Reports**: Cashbook, General Ledger, Trial Balance, Income Statement, Balance Sheet
- **Excel Reports**: Professional financial statements with real data
- **PDF Reports**: Payslips with employee details and tax calculations

### Data Flow
```
Database Tables → ReportService → Formatting → Output (Console/Excel/PDF)
```

## 🚀 Deployment & Production

### Production Requirements
- **Java 17+** runtime
- **PostgreSQL 12+** database
- **Environment variables** configured
- **License compliance** verified

### Build Artifacts
```bash
./gradlew fatJar  # Creates app-fat.jar with all dependencies
java -jar app/build/libs/app-fat.jar api  # Run API server
```

## 🔍 Debugging & Troubleshooting

### Common Issues
- **Database Connection**: Check `.env` file and PostgreSQL service
- **License Errors**: Verify license compliance for commercial use
- **Memory Issues**: Use `-Xmx1g` for large transaction processing
- **PDF Parsing**: Ensure Apache PDFBox dependencies are correct versions

### Logging Pattern
```java
// Use System.out.println for console output
System.out.println("🔗 Connecting to database: " + dbUrl);
System.out.println("✅ Operation completed successfully");
System.err.println("❌ Error: " + e.getMessage());
```

## 📝 Code Quality Standards

### Checkstyle & SpotBugs
- **Checkstyle**: Enforced via `config/checkstyle/checkstyle.xml`
- **SpotBugs**: Configured in `config/spotbugs/exclude.xml` (currently ignoreFailures=true for EI_EXPOSE_REP)
- **Java 17 features**: Use modern Java constructs (records, text blocks, etc.)

### Naming Conventions
- **Services**: `*Service` suffix (e.g., `CompanyService`, `ReportService`)
- **Repositories**: `*Repository` suffix (e.g., `CompanyRepository`)
- **Models**: PascalCase (e.g., `JournalEntry`, `BankTransaction`)
- **Test Classes**: `*Test` suffix with descriptive method names

### ⚠️ CRITICAL: Magic Number Cleanup Protocol (Established October 2025)

**MANDATORY PATTERN**: All magic number cleanup must follow this systematic approach:

#### 1. **Comprehensive Inventory First** (MANDATORY)
**ALWAYS run this command BEFORE starting any magic number work:**
```bash
./gradlew clean checkstyleMain --no-daemon 2>&1 | grep "MagicNumber" | sort | uniq
```
This provides the complete list of ALL magic number violations across the entire codebase. **Never start working on individual files without this comprehensive inventory.**

#### 2. **Systematic File-by-File Completion** (MANDATORY)
- Work on **ONE file at a time only**
- **Complete ALL magic numbers in the current file** before moving to the next
- Replace magic numbers with **named constants** (not inline literals)
- **Verify each file is 100% clean** using checkstyle before proceeding
- **Only after fully completing one file and running `./gradlew clean build` to make sure everything is working**, then move to the next file in the inventory

#### 3. **Documentation Updates** (MANDATORY)
- Update task documentation **immediately after each file completion**
- Mark files as "✅ COMPLETED" with specific magic numbers fixed
- Update progress metrics and remaining work
- **Never move to next file without documentation update**

#### 4. **No Partial Fixes** (STRICTLY ENFORCED)
- ❌ **DO NOT** fix some magic numbers in multiple files simultaneously
- ❌ **DO NOT** leave files partially completed
- ❌ **DO NOT** skip documentation updates
- ❌ **DO NOT** work on files without comprehensive inventory

#### Consequences of Violation:
- Incomplete fixes across multiple files
- Inconsistent code quality
- Difficulty tracking progress
- Potential regressions in partially modified files

**This protocol ensures systematic, complete cleanup rather than scattered partial fixes.**

### ⚠️ CRITICAL: Missing Braces Cleanup Protocol (Established October 2025)

**MANDATORY PATTERN**: All missing braces cleanup must follow this systematic approach:

#### 1. **Comprehensive Inventory First** (MANDATORY)
**ALWAYS run this command BEFORE starting any missing braces work:**
```bash
./gradlew clean checkstyleMain --no-daemon 2>&1 | grep "NeedBraces" | sort | uniq
```
This provides the complete list of ALL missing braces violations across the entire codebase. **Never start working on individual files without this comprehensive inventory.**

#### 2. **Systematic File-by-File Completion** (MANDATORY)
- Work on **ONE file at a time only**
- **Complete ALL missing braces in the current file** before moving to the next
- Add curly braces `{}` around single-line `if`, `else`, `for`, `while`, and `do-while` statements
- **Verify each file is 100% clean** using checkstyle before proceeding
- **Only after fully completing one file and running `./gradlew clean build` to make sure everything is working**, then move to the next file in the inventory

#### 3. **Documentation Updates** (MANDATORY)
- Update task documentation **immediately after each file completion**
- Mark files as "✅ COMPLETED" with specific missing braces fixed
- Update progress metrics and remaining work
- **Never move to next file without documentation update**

#### 4. **No Partial Fixes** (STRICTLY ENFORCED)
- ❌ **DO NOT** fix some missing braces in multiple files simultaneously
- ❌ **DO NOT** leave files partially completed
- ❌ **DO NOT** skip documentation updates
- ❌ **DO NOT** work on files without comprehensive inventory

#### Consequences of Violation:
- Incomplete fixes across multiple files
- Inconsistent code quality
- Difficulty tracking progress
- Potential regressions in partially modified files

**This protocol ensures systematic, complete cleanup rather than scattered partial fixes.**

Remember: This system processes real financial data with 7,156+ transactions. Always validate operations and maintain data integrity through proper transaction handling and balance verification.

## � Financial Reporting

### Report Types
- **Console Reports**: Cashbook, General Ledger, Trial Balance, Income Statement, Balance Sheet
- **Excel Reports**: Professional financial statements with real data
- **PDF Reports**: Payslips with employee details and tax calculations

### Data Flow
```
Database Tables → ReportService → Formatting → Output (Console/Excel/PDF)
```

## 🚀 Deployment & Production

### Production Requirements
- **Java 17+** runtime
- **PostgreSQL 12+** database
- **Environment variables** configured
- **License compliance** verified

### Build Artifacts
```bash
./gradlew fatJar  # Creates app-fat.jar with all dependencies
java -jar app/build/libs/app-fat.jar api  # Run API server
```

## 🔍 Debugging & Troubleshooting

### Common Issues
- **Database Connection**: Check `.env` file and PostgreSQL service
- **License Errors**: Verify license compliance for commercial use
- **Memory Issues**: Use `-Xmx1g` for large transaction processing
- **PDF Parsing**: Ensure Apache PDFBox dependencies are correct versions

### Logging Pattern
```java
// Use System.out.println for console output
System.out.println("🔗 Connecting to database: " + dbUrl);
System.out.println("✅ Operation completed successfully");
System.err.println("❌ Error: " + e.getMessage());
```

## �📝 Code Quality Standards

### Checkstyle & SpotBugs
- **Checkstyle**: Enforced via `config/checkstyle/checkstyle.xml`
- **SpotBugs**: Configured in `config/spotbugs/exclude.xml` (currently ignoreFailures=true for EI_EXPOSE_REP)
- **Java 17 features**: Use modern Java constructs (records, text blocks, etc.)

### Naming Conventions
- **Services**: `*Service` suffix (e.g., `CompanyService`, `ReportService`)
- **Repositories**: `*Repository` suffix (e.g., `CompanyRepository`)
- **Models**: PascalCase (e.g., `JournalEntry`, `BankTransaction`)
- **Test Classes**: `*Test` suffix with descriptive method names

Remember: This system processes real financial data with 7,156+ transactions. Always validate operations and maintain data integrity through proper transaction handling and balance verification.

## 🤝 Coding Partnership Principles

**Established:** October 3, 2025  
**Partners:** AI Agent & Sthwalo Nyoni

### Mandatory Workflow Rules

1. **Incremental Changes with Review**
   - Complete ONE task at a time
   - Show ALL changes made (files created, modified, deleted)
   - Explain WHY each change was made
   - Wait for partner approval before proceeding

2. **Verification & Agreement**
   - Both partners must review and approve changes
   - Test/verify changes work as expected
   - Document any issues or concerns
   - No assumptions - when in doubt, ask

3. **Change Communication Format**
   ```
   ## Task: [Brief description]
   
   ### Changes Made:
   - Created: [file paths]
   - Modified: [file paths with summary]
   - Deleted: [file paths with reason]
   
   ### Why These Changes:
   [Detailed explanation]
   
   ### Testing/Verification:
   [How to verify changes work]
   
   ### Ready to Proceed?
   [Wait for partner approval]
   ```

4. **Build & Test After Each Change**
   - Run `./gradlew clean build` after code changes
   - Fix any compilation errors before proceeding
   - Document build results

---

## 🚨 Known Issues & Technical Debt

### CRITICAL: Transaction Classification Redundancies (Identified 2025-10-03)

**Problem:** Multiple overlapping services handling transaction classification with conflicting implementations.

**Affected Components:**
- `fin.app.TransactionClassifier` (thin wrapper - deprecated)
- `fin.service.ClassificationIntegrationService` (orchestrator)
- `fin.service.TransactionMappingService` (actual implementation with 2000+ lines of hardcoded logic)
- `fin.service.RuleMappingService` (old mapping rules - schema conflicts)
- `fin.service.TransactionMappingRuleService` (new mapping rules - repository pattern)
- `fin.service.ChartOfAccountsService` (account initialization)

**Key Issues:**
1. **THREE classification services** doing similar operations
2. **TWO mapping rule services** with incompatible database schemas (`match_value` vs `pattern_text`)
3. **Chart of Accounts initialization scattered** across 4 different locations
4. **Hardcoded classification logic** (2000+ lines in `TransactionMappingService.mapTransactionToAccount()`)
5. **Database schema conflicts** requiring migration logic to handle both column names

**Recommended Approach:**
- When working with classification: Use `TransactionClassificationService` as entry point (unified API)
- When creating mapping rules: Use `TransactionMappingRuleService` (newer, better structure)
- When initializing accounts: Use `ChartOfAccountsService.initializeChartOfAccounts()`
- **DO NOT** create new classification services or mapping rules in new code
- See `/docs/development/DATA_MANAGEMENT_FLOW_ANALYSIS.md` for complete analysis and refactoring plan

**Status:** Under review for consolidation into single `TransactionClassificationService`

---

### 🚨 CRITICAL: General Ledger Calculation Errors (FIXED 2025-10-11)

**Problem:** General Ledger showed incorrect balances and wrong signs (CREDIT instead of DEBIT for asset accounts).

**Impact:**
- Bank account showed R32,283.77 **CREDIT** (impossible - assets should be DEBIT)
- Expected closing: R24,109.81 **DEBIT** (per bank statement)
- Discrepancy: R8,173.96 + wrong sign
- **Root causes identified:**
  1. **33 missing journal entries** (R493,178.42 total, including R275,000 deposit)
  2. **GL calculation didn't account for normal balance types** (DR vs CR accounts)
  3. **Missing deposit journal entries** causing underreported debits

**RESOLUTION COMPLETED (2025-10-11):**
```
Expected:  R24,109.81 DR (bank statement)
Actual:    R24,537.81 (incorrect GL)
```
**FIXES APPLIED:**
1. **Fixed GL data source**: Now reads from `journal_entries` table (not Trial Balance)
2. **Added normal balance logic**: Assets=DR, Liabilities/Equity=CR, Revenue=CR, Expenses=DR
3. **Generated missing journal entries**: 33 transactions via Data Management menu
4. **Added R275,000 deposit**: DEBIT bank, CREDIT loan account
5. **Fixed balance calculations**: `Closing = Opening + Debits - Credits` (for DR accounts)

**Code Changes:**
- `GeneralLedgerService.java`: Added normal balance type handling
- `JdbcFinancialDataRepository.java`: Added `getAccountNormalBalance()` method
- `AccountInfo.java`: Added `normalBalance` field

**Status:** ✅ RESOLVED - GL now shows correct balances and signs

---

## 🩹 Recent Updates & Features

### October 2025: SDL & Payroll Reprocessing
**SDL (Skills Development Levy) - COMPLETED ✅**
- Automatic 1% SDL calculation on payroll > R500k/year (R41,667/month threshold)
- Added `payslips.sdl_levy` database column with migration
- Integrated into `SARSTaxCalculator.calculateSDL()` method
- SARS-compliant reporting in EMP 201 tax submission forms
- Tested with September 2025 payroll: R1,701.00 SDL on R170,100 gross

**Payroll Reprocessing Enhancement - COMPLETED ✅**
- Can now reprocess payroll periods without "already processed" errors
- Automatic deletion of existing payslips with transaction safety (ACID)
- Period status reset from CLOSED → OPEN → recalculate → CLOSED
- Use case: Correcting employee data, applying new features (SDL), testing scenarios
- Methods: `clearExistingPayslips()`, `resetPayrollPeriodStatus()` in `PayrollService`
- In-memory state synchronization prevents stale `ApplicationState` data

**Documentation:**
- `/docs/SDL_IMPLEMENTATION_2025-10-06.md` - Complete SDL implementation details
- `/docs/PAYROLL_REPROCESSING_2025-10-06.md` - Reprocessing workflow and transaction safety
- `/docs/QUICK_REFERENCE_2025-10-06.md` - Quick testing guide for SDL verification
- `/docs/CHANGES_SUMMARY_2025-10-06.md` - Combined implementation report

### September 2025: Employee Management & Document Handling
- **Employee Field Updates**: Enhanced employee management for payslip-relevant fields (tax numbers, UIF, medical aid, pension, complete banking details)
- **Document Management**: List and delete payslip PDFs from `exports/` and `payslips/` with confirmation prompts
- **UI Improvements**: Added "Document Management" menu option, enhanced employee update forms with current values</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/.github/copilot-instructions.md