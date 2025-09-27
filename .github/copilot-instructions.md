# AI Coding Agent Instructions for FIN Financial Management System

## 🏢 System Architecture Overview

FIN is a production-ready Java 17+ financial management system with PostgreSQL database, featuring a modular service architecture and three execution modes.

### Core Components
- **Database**: PostgreSQL with complex schema (companies, fiscal_periods, accounts, journal_entries, bank_transactions)
- **Services**: 5 core services (TransactionClassifier, AccountManager, JournalEntryGenerator, RuleMappingService, TransactionBatchProcessor)
- **Processing Pipeline**: Bank PDFs → Text Extraction → Transaction Parsing → Classification → Journal Entries → Reports
- **Architecture Pattern**: Repository pattern with dependency injection via ApplicationContext

### Application Modes
1. **API Server** (Recommended): `./gradlew run --args="api"` - REST API with CORS at http://localhost:8080/api/v1/
2. **Console Application**: `./gradlew run` - Interactive terminal interface
3. **Batch Processing**: `./gradlew run --args="--batch [command]"` - Automated processing

## 🔧 Critical Developer Workflows

### Environment Setup
```bash
# Required: PostgreSQL 12+ running with credentials in .env
source .env
./gradlew build
./gradlew test
```

### Running the Application
```bash
# API Server (primary mode)
./gradlew run --args="api"

# Console Application
./gradlew run

# Batch Processing
./gradlew run --args="--batch process-transactions"
```

### Database Configuration
- Environment variables in `.env` file: `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`
- Default: `jdbc:postgresql://localhost:5432/drimacc_db`
- Connection tested via `DatabaseConfig.testConnection()`

### Build & Test
```bash
./gradlew build          # Full build with tests
./gradlew test           # Run 118+ JUnit tests
./gradlew fatJar         # Create executable JAR with all dependencies
```

## 📋 Project-Specific Conventions

### Code Organization
- **Services**: `fin.service.*` - Business logic with dependency injection
- **Repositories**: `fin.repository.*` - Data access layer (JDBC-based)
- **Models**: `fin.model.*` - Immutable data classes (e.g., `Account`, `JournalEntry`, `BankTransaction`)
- **Controllers**: `fin.controller.*` - Application flow coordination
- **Config**: `fin.config.*` - Configuration management

### Service Instantiation Pattern
```java
// Always use ApplicationContext for dependency injection
ApplicationContext context = new ApplicationContext();
CompanyService companyService = context.getCompanyService();

// NEVER manually instantiate with database URLs
// CompanyService companyService = new CompanyService(dbUrl);
```

### Database Operations
```java
// Repository pattern - always inject database URL
public CompanyRepository(String dbUrl) {
    this.dbUrl = dbUrl;
}

// Use prepared statements for all queries
try (Connection conn = DriverManager.getConnection(dbUrl);
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    // Execute operations
}
```

### Testing Patterns
```java
// TDD approach with Mockito for service dependencies
@Mock private RuleMappingService mockRuleService;
@Mock private JournalEntryGenerator mockJournalGenerator;

@BeforeEach void setUp() {
    MockitoAnnotations.openMocks(this);
    processor = new TransactionBatchProcessor(mockRuleService, mockJournalGenerator);
}
```

### Exception Handling
```java
// Comprehensive exception management
try {
    // Business logic
} catch (SQLException e) {
    throw new RuntimeException("Database operation failed: " + e.getMessage(), e);
} catch (ValidationException e) {
    // Handle validation errors
}
```

## 🔗 Integration Points & Dependencies

### External Libraries
- **Database**: PostgreSQL driver, HikariCP connection pooling
- **PDF Processing**: Apache PDFBox 3.0.0 for text extraction
- **Excel Generation**: Apache POI 5.2.4 for financial reports
- **Email**: JavaMail API for payslip distribution
- **REST API**: Spark Java framework with Gson for JSON
- **Testing**: JUnit Jupiter, Mockito for mocking

### File Processing Pipeline
```java
// PDF → Text → Transactions → Classification → Journal Entries
DocumentTextExtractor extractor = new DocumentTextExtractor();
String rawText = extractor.extractText(pdfFile);

BankStatementProcessingService parser = new BankStatementProcessingService(dbUrl);
List<BankTransaction> transactions = parser.parseBankStatement(rawText, companyId);

TransactionBatchProcessor processor = new TransactionBatchProcessor(ruleService, journalGenerator);
processor.processBatch(transactions, companyId);
```

### API Endpoints Structure
```java
// Spark Java routes with CORS
get("/api/v1/health", (req, res) -> healthCheck());
get("/api/v1/companies", (req, res) -> getCompanies());
post("/api/v1/companies/:id/upload", (req, res) -> uploadBankStatement(req, res));
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
- **SpotBugs**: Configured in `config/spotbugs/exclude.xml`
- **Java 17 features**: Use modern Java constructs (records, text blocks, etc.)

### Naming Conventions
- **Services**: `*Service` suffix (e.g., `CompanyService`, `ReportService`)
- **Repositories**: `*Repository` suffix (e.g., `CompanyRepository`)
- **Models**: PascalCase (e.g., `JournalEntry`, `BankTransaction`)
- **Test Classes**: `*Test` suffix with descriptive method names

Remember: This system processes real financial data with 7,156+ transactions. Always validate operations and maintain data integrity through proper transaction handling and balance verification.</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/.github/copilot-instructions.md