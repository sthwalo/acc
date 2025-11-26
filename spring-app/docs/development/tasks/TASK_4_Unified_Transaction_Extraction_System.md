# TASK 4: Unified Transaction Extraction System
**Status:** 📋 PLANNED - Design & Analysis Phase
**Created:** 2025-11-26
**Priority:** HIGH - Core Business Feature
**Risk Level:** HIGH - Transaction Processing Accuracy
**Estimated Effort:** 10-14 days (80-112 hours)

## 🎯 Task Overview

Implement a comprehensive unified transaction extraction system that can automatically detect file types, extract raw data from any document format (PDF, TXT, CSV, etc.), and parse transactions from multiple South African bank statement formats including Standard Bank, FNB, ABSA, Capitec, and Nedbank.

## 📋 Current Limitations

### Existing System Constraints
- **Single Bank Support**: Only Standard Bank statements are currently supported
- **Single Format**: Limited to PDF text extraction only
- **Manual Format Detection**: No automatic file type or bank detection
- **Rigid Parsing**: Hardcoded parsing logic for Standard Bank tabular format only

### File Types Currently Unsupported
- **FNB Statements**: GOLD BUSINESS ACCOUNT PDFs (11+ sample files)
- **ABSA Statements**: Account statement PDFs
- **Capitec Statements**: Not yet available in sample data
- **Nedbank Statements**: Not yet available in sample data
- **Text Files**: PAYE tax tables, employee data files
- **CSV Files**: Potential future import formats

## 🎯 Requirements

### Core Functionality
- [ ] **Universal File Extraction**: Extract raw data from any file type (PDF, TXT, CSV, etc.)
- [ ] **Automatic Bank Detection**: Identify bank type from document content/metadata
- [ ] **Multi-Format Parsing**: Parse transactions from all major South African banks
- [ ] **Data Structure Analysis**: Dynamically analyze and adapt to different statement layouts
- [ ] **Transaction Validation**: Ensure extracted data integrity and completeness

### Supported File Types
- [ ] **PDF Documents**: Bank statements, financial reports, payslips
- [ ] **Text Files**: Raw data exports, tax tables, employee data
- [ ] **CSV Files**: Transaction exports, bulk data imports
- [ ] **Future Formats**: Excel files, JSON exports, XML documents

### Supported Banks
- [ ] **Standard Bank**: Existing implementation (tabular format)
- [ ] **FNB**: GOLD BUSINESS ACCOUNT statements
- [ ] **ABSA**: Account statements
- [ ] **Capitec**: Business account statements
- [ ] **Nedbank**: Business account statements

### Technical Requirements
- [ ] **Modular Architecture**: Extensible parser framework
- [ ] **Error Handling**: Clear error messages for unsupported formats
- [ ] **Performance**: Efficient processing of large documents
- [ ] **Memory Management**: Proper resource cleanup for large files
- [ ] **Thread Safety**: Concurrent processing capability

## 🏗️ Implementation Architecture

### Core Components

#### 1. Universal Document Extractor
```java
public interface UniversalDocumentExtractor {
    /**
     * Extract raw data from any file type
     * @param filePath Path to the document file
     * @return RawDocument containing extracted data and metadata
     */
    RawDocument extract(String filePath) throws ExtractionException;

    /**
     * Detect file type and capabilities
     * @param filePath Path to analyze
     * @return FileTypeInfo with format details
     */
    FileTypeInfo detectFileType(String filePath) throws DetectionException;
}
```

#### 2. Bank Detection Service
```java
public interface BankDetectionService {
    /**
     * Detect bank type from document content
     * @param rawDocument Extracted document data
     * @return BankType enum value
     */
    BankType detectBank(RawDocument rawDocument) throws BankDetectionException;

    /**
     * Get bank-specific parsing capabilities
     * @param bankType Detected bank type
     * @return BankCapabilities with supported features
     */
    BankCapabilities getBankCapabilities(BankType bankType);
}
```

#### 3. Transaction Parser Framework
```java
public interface TransactionParser {
    /**
     * Parse transactions from raw document data
     * @param rawDocument Extracted document data
     * @param bankType Detected bank type
     * @return List of parsed transactions
     */
    List<ParsedTransaction> parseTransactions(RawDocument rawDocument, BankType bankType)
        throws ParseException;

    /**
     * Check if parser can handle this document
     * @param rawDocument Document to check
     * @param bankType Bank type
     * @return true if parser can handle this document
     */
    boolean canParse(RawDocument rawDocument, BankType bankType);
}
```

### File Type Handlers

#### PDF Handler
```java
@Service
public class PdfDocumentExtractor implements UniversalDocumentExtractor {
    private final PdfTextExtractionService pdfService;

    @Override
    public RawDocument extract(String filePath) throws ExtractionException {
        try {
            String rawText = pdfService.extractTextFromPdf(filePath);
            List<String> lines = Arrays.asList(rawText.split("\\n"));

            return RawDocument.builder()
                .filePath(filePath)
                .fileType(FileType.PDF)
                .rawText(rawText)
                .lines(lines)
                .metadata(extractPdfMetadata(filePath))
                .build();
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract PDF content", e);
        }
    }
}
```

#### Text Handler
```java
@Service
public class TextDocumentExtractor implements UniversalDocumentExtractor {
    @Override
    public RawDocument extract(String filePath) throws ExtractionException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            String rawText = String.join("\n", lines);

            return RawDocument.builder()
                .filePath(filePath)
                .fileType(FileType.TEXT)
                .rawText(rawText)
                .lines(lines)
                .metadata(extractTextMetadata(filePath))
                .build();
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract text content", e);
        }
    }
}
```

#### CSV Handler
```java
@Service
public class CsvDocumentExtractor implements UniversalDocumentExtractor {
    @Override
    public RawDocument extract(String filePath) throws ExtractionException {
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            List<CsvRecord> records = parseCsvLines(lines);

            return RawDocument.builder()
                .filePath(filePath)
                .fileType(FileType.CSV)
                .rawText(String.join("\n", lines))
                .lines(lines)
                .csvRecords(records)
                .metadata(extractCsvMetadata(filePath, records))
                .build();
        } catch (IOException e) {
            throw new ExtractionException("Failed to extract CSV content", e);
        }
    }
}
```

### Bank-Specific Parsers

#### FNB Parser
```java
@Service
@ParserForBank(BankType.FNB)
public class FnbTransactionParser implements TransactionParser {
    @Override
    public boolean canParse(RawDocument document, BankType bankType) {
        if (bankType != BankType.FNB) return false;

        // Check for FNB-specific patterns
        String content = document.getRawText().toUpperCase();
        return content.contains("FIRST NATIONAL BANK") ||
               content.contains("FNB") ||
               content.contains("GOLD BUSINESS ACCOUNT");
    }

    @Override
    public List<ParsedTransaction> parseTransactions(RawDocument document, BankType bankType)
            throws ParseException {
        // FNB-specific parsing logic
        List<ParsedTransaction> transactions = new ArrayList<>();

        // Analyze FNB statement structure and extract transactions
        // Implementation based on FNB GOLD BUSINESS ACCOUNT format

        return transactions;
    }
}
```

#### ABSA Parser
```java
@Service
@ParserForBank(BankType.ABSA)
public class AbsaTransactionParser implements TransactionParser {
    @Override
    public boolean canParse(RawDocument document, BankType bankType) {
        if (bankType != BankType.ABSA) return false;

        String content = document.getRawText().toUpperCase();
        return content.contains("ABSA") ||
               content.contains("ABSABANK") ||
               content.contains("BANK SETA");
    }

    @Override
    public List<ParsedTransaction> parseTransactions(RawDocument document, BankType bankType)
            throws ParseException {
        // ABSA-specific parsing logic
        List<ParsedTransaction> transactions = new ArrayList<>();

        // Analyze ABSA statement structure and extract transactions

        return transactions;
    }
}
```

### Data Models

#### RawDocument
```java
@Data
@Builder
public class RawDocument {
    private String filePath;
    private FileType fileType;
    private String rawText;
    private List<String> lines;
    private List<CsvRecord> csvRecords; // For CSV files
    private Map<String, Object> metadata;
    private LocalDateTime extractedAt;
}
```

#### ParsedTransaction
```java
@Data
@Builder
public class ParsedTransaction {
    private String transactionId;
    private LocalDate transactionDate;
    private String description;
    private BigDecimal amount;
    private TransactionType type; // DEBIT, CREDIT
    private String reference;
    private BigDecimal balance;
    private String bankReference;
    private BankType bankType;
    private Map<String, Object> metadata;
}
```

#### BankType Enum
```java
public enum BankType {
    STANDARD_BANK("Standard Bank"),
    FNB("First National Bank"),
    ABSA("ABSA Bank"),
    CAPITEC("Capitec Bank"),
    NEDBANK("Nedbank"),
    UNKNOWN("Unknown Bank");

    private final String displayName;

    BankType(String displayName) {
        this.displayName = displayName;
    }
}
```

## 🔄 Implementation Workflow

### Phase 1: Universal Extraction Framework (Days 1-3)
1. **Create UniversalDocumentExtractor interface** and implementations
2. **Implement file type detection** (PDF, TXT, CSV)
3. **Add RawDocument data model** for unified data representation
4. **Create extraction service registry** for different file types

### Phase 2: Bank Detection System (Days 4-5)
1. **Implement BankDetectionService** with pattern matching
2. **Add bank-specific detection logic** for all supported banks
3. **Create BankCapabilities** system for feature detection
4. **Add confidence scoring** for bank detection accuracy

### Phase 3: Parser Framework Enhancement (Days 6-7)
1. **Extend TransactionParser interface** for multi-bank support
2. **Update existing StandardBankTabularParser** to use new framework
3. **Create parser registry** with @ParserForBank annotations
4. **Add parser capability detection** and selection logic

### Phase 4: FNB Parser Implementation (Days 8-9)
1. **Analyze FNB statement samples** (11+ GOLD BUSINESS ACCOUNT files)
2. **Implement FnbTransactionParser** with FNB-specific logic
3. **Add FNB format detection** and validation
4. **Test FNB parsing** against sample data

### Phase 5: ABSA Parser Implementation (Days 10-11)
1. **Analyze ABSA statement samples** (account statement PDFs)
2. **Implement AbsaTransactionParser** with ABSA-specific logic
3. **Add ABSA format detection** and validation
4. **Test ABSA parsing** against sample data

### Phase 6: Integration & Testing (Days 12-14)
1. **Update BankStatementProcessingService** to use unified system
2. **Add comprehensive testing** for all supported banks
3. **Implement error handling** and fallback mechanisms
4. **Performance optimization** and memory management
5. **Documentation** and user guide creation

## 🧪 Testing Strategy

### Unit Testing
- [ ] **File Type Detection**: Test detection accuracy for all supported formats
- [ ] **Bank Detection**: Test bank identification from various document types
- [ ] **Parser Selection**: Test correct parser selection for each bank
- [ ] **Data Extraction**: Test raw data extraction from different file types

### Integration Testing
- [ ] **End-to-End Processing**: Test complete extraction → parsing → database flow
- [ ] **Multi-Bank Support**: Test all supported banks with sample data
- [ ] **Error Handling**: Test graceful failure for unsupported formats
- [ ] **Performance Testing**: Test processing speed for large documents

### Sample Data Testing
- [ ] **Standard Bank**: 13+ PDF files in XG:STD directory
- [ ] **FNB**: 11+ PDF files in GHC:FNB directory
- [ ] **ABSA**: 1+ PDF file in Rock Absa directory
- [ ] **Text Files**: PAYE tax tables, employee data files
- [ ] **Edge Cases**: Corrupted files, empty files, large files

## 📊 Success Criteria

### Functional Requirements
- [ ] **Universal Extraction**: Successfully extract data from PDF, TXT, and CSV files
- [ ] **Bank Detection**: 95%+ accuracy in identifying bank types from documents
- [ ] **Transaction Parsing**: Parse transactions from all 5 supported banks
- [ ] **Data Integrity**: Maintain transaction data accuracy and completeness
- [ ] **Error Handling**: Clear error messages for unsupported formats

### Performance Requirements
- [ ] **Processing Speed**: Extract and parse documents within 30 seconds
- [ ] **Memory Usage**: Efficient memory usage for large documents (< 500MB)
- [ ] **Concurrent Processing**: Support multiple simultaneous extractions
- [ ] **Scalability**: Handle increasing document volumes without degradation

### Quality Requirements
- [ ] **Code Coverage**: 90%+ unit test coverage for extraction logic
- [ ] **Error Rate**: < 1% parsing errors on valid documents
- [ ] **Maintainability**: Modular, extensible architecture for future banks
- [ ] **Documentation**: Complete technical documentation and user guides

## 🔍 Risk Assessment

### High Risk Items
- **Bank Format Changes**: Banks may change statement formats without notice
- **New Bank Requirements**: Adding support for additional banks increases complexity
- **Data Accuracy**: Incorrect parsing could lead to financial errors
- **Performance Issues**: Large documents may cause memory or processing issues

### Mitigation Strategies
- **Modular Design**: Easy to update parsers when formats change
- **Comprehensive Testing**: Extensive testing with real sample data
- **Validation Logic**: Multiple validation layers to ensure data accuracy
- **Monitoring**: Performance monitoring and alerting for issues

## 📋 Dependencies

### Internal Dependencies
- **PdfTextExtractionService**: For PDF text extraction (existing)
- **BankStatementProcessingService**: For transaction processing orchestration
- **TransactionParser Interface**: For parser framework (existing)
- **Database Schema**: bank_transactions table for storing parsed data

### External Dependencies
- **Apache PDFBox**: For PDF text extraction (existing)
- **OpenCSV**: For CSV file parsing (to be added)
- **Jackson**: For JSON processing if needed (existing)

## 🚀 Future Enhancements

### Phase 2 Features (Post-Implementation)
- [ ] **Excel Support**: Add .xlsx and .xls file processing
- [ ] **JSON/XML Support**: Handle structured data formats
- [ ] **Image OCR**: Extract text from scanned documents
- [ ] **Machine Learning**: Auto-detect statement layouts
- [ ] **Multi-Language**: Support for Afrikaans statements

### Advanced Features
- [ ] **Real-time Processing**: Stream processing for large files
- [ ] **Batch Processing**: Queue system for bulk document processing
- [ ] **API Integration**: REST endpoints for document upload and processing
- [ ] **Audit Logging**: Complete audit trail for all extraction operations
- [ ] **Security**: File type validation and malware scanning

## 📚 References

- **[Current Parser Framework](../../technical/PARSER_FRAMEWORK.md)** - Existing parser architecture
- **[Bank Statement Formats](../../technical/BANK_STATEMENT_FORMATS.md)** - Bank-specific format documentation
- **[File Processing Standards](../../technical/FILE_PROCESSING_STANDARDS.md)** - File handling best practices
- **[Database Schema](../../schemas/bank_transactions_schema.md)** - Transaction storage schema

## ✅ Implementation Checklist

### Pre-Implementation
- [ ] Analyze all sample documents in /input/ directories
- [ ] Document current Standard Bank parser implementation
- [ ] Create file type detection test cases
- [ ] Design RawDocument and ParsedTransaction data models

### Implementation Phases
- [ ] Phase 1: Universal extraction framework
- [ ] Phase 2: Bank detection system
- [ ] Phase 3: Parser framework enhancement
- [ ] Phase 4: FNB parser implementation
- [ ] Phase 5: ABSA parser implementation
- [ ] Phase 6: Integration and testing

### Post-Implementation
- [ ] Performance testing and optimization
- [ ] Documentation completion
- [ ] User acceptance testing
- [ ] Production deployment preparation</content>
<parameter name="filePath">/Users/sthwalonyoni/FIN/spring-app/docs/development/tasks/TASK_4_Unified_Transaction_Extraction_System.md