package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreditTransactionParserTest {
    private CreditTransactionParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new CreditTransactionParser();
        context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
    }

    @Test
    void canParseReturnsTrueForCreditTransactions() {
        assertTrue(parser.canParse("CREDIT TRANSFER FROM JOHN DOE 1,500.00", context));
        assertTrue(parser.canParse("DEPOSIT REF 123456 500.00", context));
        assertTrue(parser.canParse("PAYMENT FROM CUSTOMER 750.50", context));
    }

    @Test
    void canParseReturnsFalseForNonCreditTransactions() {
        assertFalse(parser.canParse("IB PAYMENT TO VENDOR", context));
        assertFalse(parser.canParse("FEE-ELECTRONIC ACCOUNT PAYMENT 8.90-", context));
        assertFalse(parser.canParse("DEBIT ORDER PAYMENT", context));
    }

    @Test
    void parseExtractsTransactionAmount() {
        String line = "CREDIT TRANSFER FROM JOHN DOE 1,500.00";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());
        assertEquals("CREDIT TRANSFER FROM JOHN DOE", result.getDescription().trim());
        assertEquals(context.getStatementDate(), result.getDate());
    }

    @Test
    void parseHandlesSimpleDeposit() {
        String line = "DEPOSIT REF 123456 500.00";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals("DEPOSIT REF 123456", result.getDescription().trim());
    }

    @Test
    void parseHandlesPaymentFrom() {
        String line = "PAYMENT FROM CUSTOMER 750.50";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("750.50"), result.getAmount());
        assertEquals("PAYMENT FROM CUSTOMER", result.getDescription().trim());
    }
}
