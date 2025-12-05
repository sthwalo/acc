package fin.service.parser;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for StandardBankTabularParser.
 * Tests multiline description handling and "-" suffix parsing.
 */
class StandardBankTabularParserTest {

    private StandardBankTabularParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new StandardBankTabularParser();
        context = new TransactionParsingContext.Builder()
                .statementDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Test
    void shouldSkipHeaderFooterLines() {
        assertFalse(parser.canParse("Details Service Fee Debits Credits Date Balance", context));
        assertFalse(parser.canParse("BRAAMFONTEIN", context));
        assertFalse(parser.canParse("MARSHALLTOWN", context));
        assertFalse(parser.canParse("Page 1", context));
        assertFalse(parser.canParse("BANK STATEMENT", context));
    }

    @Test
    void shouldParseDebitTransaction() {
        String line = "IMMEDIATE PAYMENT 1,310.00- 03 16 24,106.81";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals("IMMEDIATE PAYMENT", tx.getDescription().trim());
        assertEquals(TransactionType.DEBIT, tx.getType());
        assertEquals(new BigDecimal("1310.00"), tx.getAmount());
        assertEquals(new BigDecimal("24106.81"), tx.getBalance());
        assertFalse(tx.hasServiceFee());
    }

    @Test
    void shouldParseCreditTransaction() {
        String line = "IIB TRANSFER TO 8,000.00 03 19 12,341.21";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals("IIB TRANSFER TO", tx.getDescription().trim());
        assertEquals(TransactionType.CREDIT, tx.getType());
        assertEquals(new BigDecimal("8000.00"), tx.getAmount());
        assertEquals(new BigDecimal("12341.21"), tx.getBalance());
        assertFalse(tx.hasServiceFee());
    }

    @Test
    void shouldParseServiceFeeWithHashMarker() {
        String line = "CASH WITHDRAWAL FEE ## 52.60- 03 20 7,136.41";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals("CASH WITHDRAWAL FEE", tx.getDescription().trim());
        assertEquals(TransactionType.SERVICE_FEE, tx.getType());
        assertEquals(new BigDecimal("52.60"), tx.getAmount());
        assertEquals(new BigDecimal("7136.41"), tx.getBalance());
        assertTrue(tx.hasServiceFee());
    }

    @Test
    void shouldHandleNegativeDebitAmounts() {
        String line = "ATM WITHDRAWAL 200.00- 03 21 6,936.41";
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(TransactionType.DEBIT, tx.getType());
        assertEquals(new BigDecimal("200.00"), tx.getAmount());
        assertEquals(new BigDecimal("6936.41"), tx.getBalance());
    }

    @Test
    void shouldParseVariousDateFormats() {
        String[] dateLines = {
            "PAYMENT RECEIVED 500.00 01 15 24,606.81",  // January 15
            "SALARY DEPOSIT 3000.00 02 28 27,606.81",    // February 28
            "INTEREST PAYMENT 50.00 12 31 27,656.81"     // December 31
        };

        LocalDate[] expectedDates = {
            LocalDate.of(2024, 1, 15),
            LocalDate.of(2024, 2, 28),
            LocalDate.of(2024, 12, 31)
        };

        for (int i = 0; i < dateLines.length; i++) {
            ParsedTransaction tx = parser.parse(dateLines[i], context);
            assertNotNull(tx);
            assertEquals(expectedDates[i], tx.getDate(), "Line: " + dateLines[i]);
        }
    }

    @Test
    void shouldClassifyDebitTransactions() {
        String[] debitLines = {
            "IMMEDIATE PAYMENT 1,310.00- 03 16 24,106.81",
            "ATM WITHDRAWAL 200.00- 03 21 6,936.41",
            "DEBIT ORDER 150.00- 03 22 6,786.41"
        };

        for (String line : debitLines) {
            ParsedTransaction tx = parser.parse(line, context);
            assertNotNull(tx);
            assertEquals(TransactionType.DEBIT, tx.getType(), "Line: " + line);
        }
    }

    @Test
    void shouldClassifyCreditTransactions() {
        String[] creditLines = {
            "IIB TRANSFER TO 8,000.00 03 19 12,341.21",
            "SALARY DEPOSIT 3000.00 03 20 15,341.21",
            "INTEREST PAYMENT 50.00 03 21 15,391.21"
        };

        for (String line : creditLines) {
            ParsedTransaction tx = parser.parse(line, context);
            assertNotNull(tx);
            assertEquals(TransactionType.CREDIT, tx.getType(), "Line: " + line);
        }
    }

    @Test
    void shouldClassifyServiceFees() {
        String[] feeLines = {
            "CASH WITHDRAWAL FEE ## 52.60- 03 20 7,136.41",
            "MONTHLY SERVICE FEE ## 150.00- 03 21 6,986.41",
            "ACCOUNT FEE ## 25.00- 03 22 6,961.41"
        };

        for (String line : feeLines) {
            ParsedTransaction tx = parser.parse(line, context);
            assertNotNull(tx);
            assertEquals(TransactionType.SERVICE_FEE, tx.getType(), "Line: " + line);
            assertTrue(tx.hasServiceFee(), "Line: " + line);
        }
    }

    @Test
    void shouldHandleInvalidLines() {
        String[] invalidLines = {
            "",
            "   ",
            "Invalid line without date",
            "Not a transaction line",
            "Description only no date balance"
        };

        for (String line : invalidLines) {
            assertFalse(parser.canParse(line, context));
            assertNull(parser.parse(line, context));
        }
    }

    @Test
    void shouldHandleCommaInAmounts() {
        String line = "LARGE DEPOSIT 10,000.00 03 15 34,606.81";
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(new BigDecimal("10000.00"), tx.getAmount());
        assertEquals(new BigDecimal("34606.81"), tx.getBalance());
    }

    @Test
    void shouldResetParserState() {
        // Parse some lines to set state
        parser.parse("TEST PAYMENT 100.00- 03 16 24,106.81", context);

        // Reset
        parser.reset();

        // Should be able to parse new lines
        ParsedTransaction tx = parser.parse("NEW PAYMENT 200.00 03 17 24,306.81", context);
        assertNotNull(tx);
    }

    /**
     * INTEGRATION TEST: Test with real Standard Bank statement patterns
     */
    @Test
    void shouldParseRealStandardBankPatterns_INTEGRATION() {
        String[] realLines = {
            "IMMEDIATE PAYMENT 1,310.00- 03 16 24,106.81",
            "CASH WITHDRAWAL FEE ## 52.60- 03 20 7,136.41",
            "IIB TRANSFER TO 8,000.00 03 19 12,341.21"
        };

        for (String line : realLines) {
            assertTrue(parser.canParse(line, context), "Should parse: " + line);
            ParsedTransaction tx = parser.parse(line, context);
            assertNotNull(tx, "Should return transaction for: " + line);
            assertNotNull(tx.getDate(), "Should have date for: " + line);
            assertNotNull(tx.getDescription(), "Should have description for: " + line);
            assertNotNull(tx.getAmount(), "Should have amount for: " + line);
            assertNotNull(tx.getBalance(), "Should have balance for: " + line);
        }
    }

    /**
     * Test edge cases and boundary conditions
     */
    @Test
    void shouldHandleEdgeCases() {
        // Very long description
        String longDesc = "VERY LONG TRANSACTION DESCRIPTION THAT MIGHT CAUSE ISSUES WITH PARSING 500.00- 03 16 24,106.81";
        ParsedTransaction tx = parser.parse(longDesc, context);
        assertNotNull(tx);
        assertTrue(tx.getDescription().length() > 50);

        // Minimum valid transaction
        String minimal = "PAYMENT 1.00- 01 01 100.00";
        tx = parser.parse(minimal, context);
        assertNotNull(tx);
        assertEquals(new BigDecimal("1.00"), tx.getAmount());
        assertEquals(new BigDecimal("100.00"), tx.getBalance());
    }

    @Test
    void shouldFinalizePendingTransactions() {
        ParsedTransaction tx = parser.finalizeParsing();
        assertNull(tx); // No pending transactions initially

        // Parse a transaction
        parser.parse("TEST TX 100.00- 03 16 24,106.81", context);

        // Should have a completed transaction
        tx = parser.finalizeParsing();
        assertNotNull(tx);
        assertEquals("TEST TX", tx.getDescription().trim());
    }
}