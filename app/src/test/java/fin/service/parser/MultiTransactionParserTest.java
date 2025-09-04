package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MultiTransactionParserTest {
    private MultiTransactionParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new MultiTransactionParser();
        context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
    }

    @Test
    void canParseReturnsTrueForMultipartTransactions() {
        assertTrue(parser.canParse("TRANSFER TO JOHN DOE 1,500.00- FEE: 8.90-", context));
        assertTrue(parser.canParse("PAYMENT TO VENDOR 750.50- FEE-ELECTRONIC PAYMENT 8.90-", context));
        assertTrue(parser.canParse("PURCHASE FROM STORE 250.00- FEE 5.50-", context));
    }

    @Test
    void canParseReturnsFalseForSingleTransactions() {
        assertFalse(parser.canParse("TRANSFER TO JOHN DOE", context));
        assertFalse(parser.canParse("FEE-ELECTRONIC PAYMENT 8.90-", context));
        assertFalse(parser.canParse("PURCHASE FROM STORE", context));
    }

    @Test
    void parseExtractsTransactionAndFee() {
        String line = "TRANSFER TO JOHN DOE 1,500.00- FEE: 8.90-";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());
        assertEquals("TRANSFER TO JOHN DOE", result.getDescription().trim());
        assertEquals(context.getStatementDate(), result.getDate());
    }

    @Test
    void parseHandlesPaymentWithFee() {
        String line = "PAYMENT TO VENDOR 750.50- FEE-ELECTRONIC PAYMENT 8.90-";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(new BigDecimal("750.50"), result.getAmount());
        assertEquals("PAYMENT TO VENDOR", result.getDescription().trim());
    }

    @Test
    void parseHandlesPurchaseWithFee() {
        String line = "PURCHASE FROM STORE 250.00- FEE 5.50-";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.DEBIT, result.getType());
        assertEquals(new BigDecimal("250.00"), result.getAmount());
        assertEquals("PURCHASE FROM STORE", result.getDescription().trim());
    }
}
