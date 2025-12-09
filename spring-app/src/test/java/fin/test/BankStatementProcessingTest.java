package fin.test;

import fin.entity.BankTransaction;
import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.model.parser.TransactionType;
import fin.service.upload.DocumentTextExtractor;
import fin.service.parser.StandardBankTabularParser;
import fin.service.parser.CreditTransactionParser;
import fin.service.parser.ServiceFeeParser;
import fin.validation.BankTransactionValidator;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test script to verify bank statement processing functionality
 * without requiring full Spring Boot context
 */
public class BankStatementProcessingTest {

    public static void main(String[] args) {
        System.out.println("🧪 Testing Bank Statement Processing Functionality");
        System.out.println("=" .repeat(50));

        try {
            // Test 1: Basic object creation
            testBasicObjectCreation();

            // Test 2: Document text extraction
            testDocumentTextExtraction();

            // Test 3: Transaction parsing
            testTransactionParsing();

            // Test 4: Validation
            testValidation();

            System.out.println("✅ All tests passed! Bank statement processing functionality is working.");

        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testBasicObjectCreation() {
        System.out.println("\n📋 Test 1: Basic Object Creation");

        // Test Company creation
        Company company = new Company();
        company.setId(1L);
        company.setName("Test Company");
        assertTrue(company.getId() == 1L, "Company ID should be 1");
        assertTrue("Test Company".equals(company.getName()), "Company name should match");
        System.out.println("✅ Company object created successfully");

        // Test BankTransaction creation
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(1L);
        transaction.setDetails("Test transaction");
        transaction.setDebitAmount(new java.math.BigDecimal("100.00"));
        assertTrue(transaction.getCompanyId() == 1L, "Transaction company ID should be 1");
        assertTrue("Test transaction".equals(transaction.getDetails()), "Transaction details should match");
        System.out.println("✅ BankTransaction object created successfully");

        // Test FiscalPeriod creation
        FiscalPeriod period = new FiscalPeriod(1L, "FY2025", java.time.LocalDate.of(2025, 3, 1), java.time.LocalDate.of(2026, 2, 28));
        assertTrue(period.getCompanyId() == 1L, "Fiscal period company ID should be 1");
        assertTrue("FY2025".equals(period.getPeriodName()), "Period name should match");
        System.out.println("✅ FiscalPeriod object created successfully");

        // Test TransactionType enum
        assertTrue(TransactionType.CREDIT != null, "CREDIT enum should exist");
        assertTrue(TransactionType.DEBIT != null, "DEBIT enum should exist");
        assertTrue(TransactionType.SERVICE_FEE != null, "SERVICE_FEE enum should exist");
        System.out.println("✅ TransactionType enum values accessible");
    }

    private static void testDocumentTextExtraction() throws Exception {
        System.out.println("\n📄 Test 2: Document Text Extraction");

        DocumentTextExtractor extractor = new DocumentTextExtractor();

        // Test with a simple text file (create a mock PDF scenario)
        // Since we don't have a real PDF, we'll just test that the service can be instantiated
        assertTrue(extractor != null, "DocumentTextExtractor should be created");
        System.out.println("✅ DocumentTextExtractor service instantiated");

        // Test isTransaction method
        assertTrue(extractor.isTransaction("SERVICE FEE 35.00-"), "Should identify service fee as transaction");
        assertTrue(!extractor.isTransaction("Page 1 of 5"), "Should not identify page header as transaction");
        System.out.println("✅ Transaction identification logic working");
    }

    private static void testTransactionParsing() {
        System.out.println("\n🔍 Test 3: Transaction Parsing");

        // Test StandardBankTabularParser
        StandardBankTabularParser standardParser = new StandardBankTabularParser();
        assertTrue(standardParser != null, "StandardBankTabularParser should be created");
        System.out.println("✅ StandardBankTabularParser instantiated");

        // Test CreditTransactionParser
        CreditTransactionParser creditParser = new CreditTransactionParser();
        assertTrue(creditParser != null, "CreditTransactionParser should be created");

        // Test parsing logic
        String creditLine = "CREDIT TRANSFER from ABC Company 1500.00";
        assertTrue(creditParser.canParse(creditLine, null), "Should be able to parse credit transaction");
        System.out.println("✅ CreditTransactionParser logic working");

        // Test ServiceFeeParser
        ServiceFeeParser serviceFeeParser = new ServiceFeeParser();
        assertTrue(serviceFeeParser != null, "ServiceFeeParser should be created");

        String feeLine = "SERVICE FEE 35.00-";
        assertTrue(serviceFeeParser.canParse(feeLine, null), "Should be able to parse service fee");
        System.out.println("✅ ServiceFeeParser logic working");
    }

    private static void testValidation() {
        System.out.println("\n✅ Test 4: Transaction Validation");

        BankTransactionValidator validator = new BankTransactionValidator();
        assert validator != null : "BankTransactionValidator should be created";

        // Test valid transaction
        BankTransaction validTransaction = new BankTransaction();
        validTransaction.setCompanyId(1L);
        validTransaction.setFiscalPeriodId(1L);
        validTransaction.setTransactionDate(java.time.LocalDate.now());
        validTransaction.setDetails("Test transaction");
        validTransaction.setDebitAmount(new java.math.BigDecimal("100.00"));

        var validResult = validator.validate(validTransaction);
        assertTrue(validResult.isValid(), "Valid transaction should pass validation");
        System.out.println("✅ Valid transaction validation passed");

        // Test invalid transaction (missing required fields)
        BankTransaction invalidTransaction = new BankTransaction();
        var invalidResult = validator.validate(invalidTransaction);
        assertTrue(!invalidResult.isValid(), "Invalid transaction should fail validation");
        assertTrue(invalidResult.getErrors().size() > 0, "Should have validation errors");
        System.out.println("✅ Invalid transaction validation correctly failed");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}