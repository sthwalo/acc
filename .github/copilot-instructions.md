# AI Coding Agent Instructions for FIN Financial Management System

## 🏢 System Architecture Overview

FIN is a production-ready Java 17+ financial management system with PostgreSQL database, featuring a modular service architecture with dependency injection and three execution modes.

### Core Architecture Pattern
- **Dependency Injection**: All services managed via `ApplicationContext` (see `fin.context.ApplicationContext`)
- **Repository Pattern**: Data access layer (`fin.repository.*`) with JDBC-based implementation
- **Service Layer**: Business logic (`fin.service.*`) with 15+ services for different domains
- **Controller Layer**: Application flow coordination (`fin.controller.*`) for different feature areas
- **Model Layer**: Immutable data classes (`fin.model.*`) representing domain entities

### Database Schema (PostgreSQL)
- **companies**: Multi-tenant company management with fiscal period tracking
- **fiscal_periods**: Date ranges for financial reporting (start_date, end_date)
- **accounts**: Chart of accounts with codes (e.g., 8100=Employee Costs, 9600=Bank Charges)
- **bank_transactions**: Raw bank data with classification fields (account_code, account_name)
- **journal_entries** + **journal_entry_lines**: Double-entry accounting entries
- **transaction_mapping_rules**: Pattern-based classification rules (e.g., "XG SALARIES" → 8100)
- **employees**, **payroll_periods**: Payroll management with PAYE/UIF calculations

### Processing Pipeline
```
Bank PDFs → DocumentTextExtractor → BankStatementProcessingService → 
  → TransactionClassifier (pattern matching) → 
  → JournalEntryGenerator (double-entry) → 
  → PostgreSQL → Financial Reports (Console/Excel/PDF)
```

### Application Modes
1. **API Server** (Production): `./gradlew run --args="api"` - REST API at http://localhost:8080/api/v1/ (CORS enabled for localhost:3000)
2. **Console Application**: `./gradlew run` or `./run.sh` - Interactive terminal with menu system
3. **Batch Processing**: `./gradlew run --args="--batch [command]"` - Automated background processing

## 🔧 Critical Developer Workflows

### Environment Setup
```bash
# 1. Ensure PostgreSQL 12+ is running
# 2. Create .env file with database credentials:
#    DATABASE_URL=jdbc:postgresql://localhost:5432/drimacc_db
#    DATABASE_USER=your_username
#    DATABASE_PASSWORD=your_password

source .env              # Load environment variables
./gradlew build          # Initial build with tests (118+ JUnit tests)
./gradlew test           # Run all tests
```

### Running the Application
```bash
# API Server (production mode - recommended for frontend integration)
./gradlew run --args="api"
# Accessible at http://localhost:8080/api/v1/

# Console Application (interactive menu)
./gradlew run
# OR use the convenience script:
./run.sh

# Batch Processing (automated tasks)
./gradlew run --args="--batch process-transactions"
```

### Database Operations
```bash
# Connect to database
psql -U sthwalonyoni -d drimacc_db -h localhost
# OR use script:
./scripts/connect_db.sh

# Check transaction count
SELECT COUNT(*) FROM bank_transactions;

# View recent transactions
SELECT transaction_date, details, debit_amount, credit_amount 
FROM bank_transactions 
ORDER BY transaction_date DESC LIMIT 10;
```

### Build & Test Workflow
```bash
# Standard build
./gradlew build                    # Full build with all tests

# Fast build (skip tests)
./gradlew clean build -x test      # Compile and package only

# Create deployable artifact
./gradlew fatJar                   # Creates app/build/libs/app-fat.jar with all dependencies

# Run specific test class
./gradlew test --tests "fin.service.AccountManagerTest"
```

### ⚠️ CRITICAL ENFORCEMENT: Code Changes
**MANDATORY REQUIREMENT**: After ANY changes to non-test code files (.java, .kt, build.gradle.kts, etc.), you MUST run:
```bash
./gradlew clean build -x test
```
This ensures:
- All code compiles correctly
- Dependencies are resolved (PostgreSQL driver, PDFBox 3.0.0, Apache POI 5.2.4, Spark Java, etc.)
- No regressions in existing functionality
- Build artifacts (fat JAR) are up-to-date

**FAILURE TO COMPLY** will result in broken builds, runtime errors, and deployment issues. This is STRICTLY enforced for all code changes.

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
// Use Mockito for unit tests (118+ tests across codebase)
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

// 3. Classify transactions using pattern matching rules
TransactionClassifier classifier = new TransactionClassifier(dbUrl);
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

### Double-Entry Accounting
```java
// Debit = Credit validation for journal entries
BigDecimal totalDebit = entries.stream().map(JournalEntryLine::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
BigDecimal totalCredit = entries.stream().map(JournalEntryLine::getCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
if (totalDebit.compareTo(totalCredit) != 0) {
    throw new ValidationException("Debit and credit amounts must balance");
}
```

### Transaction Classification
```java
// Pattern-based classification with fallback to interactive mode
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
- **118+ tests** across all services
- **Test-Driven Development** (TDD) approach
- **Mockito** for service dependencies
- **PostgreSQL test database** for repository tests

### Integration Testing
```java
// Test service integration with real database
@Test
void testTransactionBatchProcessorIntegration() {
    // Setup real services with test database
    TransactionBatchProcessor processor = new TransactionBatchProcessor(realRuleService, realJournalGenerator);
    // Execute integration test
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
   - Run `./gradlew clean build -x test` after code changes
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
- When working with classification: Use `ClassificationIntegrationService` as entry point (until refactored)
- When creating mapping rules: Use `TransactionMappingRuleService` (newer, better structure)
- When initializing accounts: Use `ChartOfAccountsService.initializeChartOfAccounts()`
- **DO NOT** create new classification services or mapping rules in new code
- See `/docs/DATA_MANAGEMENT_FLOW_ANALYSIS.md` for complete analysis and refactoring plan

**Status:** Under review for consolidation into single `TransactionClassificationService`

---

## 🩹 Patches & Updates

### Patch 2025-09-28: Employee Management & Document Handling
- **Employee Field Updates**: Enhanced employee management interface to allow updating all payslip-relevant fields including tax numbers, UIF numbers, medical aid numbers, pension fund numbers, and complete banking information (bank name, account number, branch code, account holder name)
- **Document Deletion**: Added document management functionality to payroll system allowing users to list and delete payslip PDF documents from both `exports/` and `payslips/` directories with confirmation prompts
- **Database Field Verification**: Confirmed that employee data fields exist but many are currently empty, requiring population for complete payslip generation
- **UI Improvements**: Updated payroll menu to include "Document Management" option and enhanced employee update form to display current values and allow comprehensive field editing</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/.github/copilot-instructions.md