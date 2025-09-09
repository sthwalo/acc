# Test Directory Organization

## 📁 Test Structure

### **Unit Tests (`app/src/test/java/fin/`)**
- `AppTest.java` - Main application tests

### **Service Tests (`app/src/test/java/fin/service/`)**
- `BankStatementProcessingServiceTest.java` - Bank statement processing tests
- `CompanyServiceTest.java` - Company service tests  
- `DocumentTextExtractorTest.java` - PDF text extraction tests

### **Parser Tests (`app/src/test/java/fin/service/parser/`)**
- `CreditTransactionParserTest.java` - Credit transaction parsing tests
- `MultiTransactionParserTest.java` - Multi-transaction parsing tests
- `ServiceFeeParserTest.java` - Service fee parsing tests

### **Integration Tests (`app/src/test/java/fin/integration/`)**
- `Test2025Files.java` - Test processing 2025 bank statement files
- `TestPdfDates.java` - Test date extraction from multiple PDFs
- `TestSinglePdf.java` - Test single PDF processing with detailed debugging
- `TestSinglePdfDates.java` - Test date extraction from single PDF

### **Utility Tests (`app/src/test/java/fin/utility/`)**
- `TestStandardParser.java` - Test Standard Bank parser utility functions

## 🎯 Test Categories

### **Unit Tests**
Test individual components in isolation:
- Service classes
- Parser classes  
- Model classes
- Utility functions

### **Integration Tests**
Test complete workflows:
- PDF processing pipeline
- Database integration
- End-to-end transaction processing

### **Utility Tests**
Test specific utility functions:
- Parser algorithms
- Helper methods
- Data transformation utilities

## 🚀 Running Tests

### **All Tests**
```bash
./gradlew test
```

### **Specific Test Categories**
```bash
# Unit tests only
./gradlew test --tests "*Test"

# Service tests
./gradlew test --tests "fin.service.*"

# Parser tests
./gradlew test --tests "fin.service.parser.*"

# Integration tests
./gradlew test --tests "fin.integration.*"

# Utility tests
./gradlew test --tests "fin.utility.*"
```

### **Specific Test Files**
```bash
# Bank statement processing
./gradlew test --tests "*BankStatementProcessingServiceTest*"

# PDF date extraction
./gradlew test --tests "*TestPdfDates*"

# Standard parser
./gradlew test --tests "*TestStandardParser*"
```

## 📊 Test Coverage

### **Current Test Coverage**
- ✅ Bank statement processing
- ✅ PDF text extraction
- ✅ Transaction parsing
- ✅ Company service operations
- ✅ Date extraction from PDFs
- ✅ Standard Bank parser algorithms

### **Areas Needing More Tests**
- Excel generation services
- API endpoints
- Database migration scripts
- Error handling scenarios
- Edge cases in PDF parsing

## 🛠️ Development Guidelines

### **Adding New Tests**

1. **Unit Tests**: Place in appropriate service/model directory
2. **Integration Tests**: Place in `integration/` directory
3. **Utility Tests**: Place in `utility/` directory

### **Test Naming Conventions**
- Unit tests: `ClassNameTest.java`
- Integration tests: `TestFeatureName.java`
- Utility tests: `TestUtilityName.java`

### **Package Structure**
- All test files must have correct package declarations
- Use `fin.integration.*`, `fin.utility.*`, etc.
- Import statements must be complete

## 🔧 Debugging Tests

### **Common Issues**
1. **Missing Package Declaration**: All moved files need correct package
2. **Import Statements**: Use full import paths
3. **Database Connection**: Integration tests need proper DB setup
4. **File Paths**: Ensure test files exist in expected locations

### **Test Data**
- Bank statements: `input/` directory (gitignored)
- Test databases: Use PostgreSQL test instance
- Sample data: Create in `test/resources/` if needed

## 📝 Notes

- All tests now use PostgreSQL instead of SQLite
- Integration tests require actual PDF files (not committed to repo)
- Test files are organized by purpose rather than alphabetically
- Package declarations updated to match directory structure
