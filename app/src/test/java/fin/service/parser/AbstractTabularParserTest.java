package fin.service.parser;

import fin.service.transaction.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for AbstractTabularParser base class.
 * Validates common parsing utilities used across all bank statement parsers.
 */
class AbstractTabularParserTest {

    private TestableTabularParser parser;

    // Concrete implementation for testing
    private static class TestableTabularParser extends AbstractTabularParser {
        @Override
        public boolean canParse(String line, TransactionParsingContext context) {
            return true;
        }

        @Override
        public ParsedTransaction parse(String line, TransactionParsingContext context) {
            return null;
        }

        // Expose protected methods for testing
        public BigDecimal testParseAmount(String amountStr) {
            return parseAmount(amountStr);
        }

        public LocalDate testParseDate(String dateStr) {
            return parseDate(dateStr);
        }

        public String testCleanDescription(String description) {
            return cleanDescription(description);
        }

        public ParsedTransaction testBuildTransaction(LocalDate date, String description,
                                                         BigDecimal debitAmount, BigDecimal creditAmount,
                                                         BigDecimal balance, BigDecimal serviceFee,
                                                         String reference) {
            return buildTransaction(date, description, debitAmount, creditAmount, balance, serviceFee, reference);
        }
    }

    @BeforeEach
    void setUp() {
        parser = new TestableTabularParser();
    }

    @Test
    void testParseAmount_StandardFormat() {
        assertEquals(new BigDecimal("1234.56"), parser.testParseAmount("1,234.56"));
    }

    @Test
    void testParseAmount_NegativeSuffix() {
        assertEquals(new BigDecimal("-123.45"), parser.testParseAmount("123.45-"));
    }

    @Test
    void testParseAmount_CurrencySymbol() {
        assertEquals(new BigDecimal("500.00"), parser.testParseAmount("R 500.00"));
    }

    @Test
    void testParseAmount_NullOrEmpty() {
        assertNull(parser.testParseAmount(null));
        assertNull(parser.testParseAmount(""));
        assertNull(parser.testParseAmount("   "));
    }

    @Test
    void testParseDate_DDMMYYYY() {
        LocalDate expected = LocalDate.of(2023, 12, 15);
        assertEquals(expected, parser.testParseDate("15/12/2023"));
    }

    @Test
    void testParseDate_DDMMYY() {
        LocalDate expected = LocalDate.of(2023, 12, 15);
        assertEquals(expected, parser.testParseDate("15/12/23"));
    }

    @Test
    void testParseDate_NullOrEmpty() {
        assertNull(parser.testParseDate(null));
        assertNull(parser.testParseDate(""));
        assertNull(parser.testParseDate("   "));
    }

    @Test
    void testParseDate_InvalidFormat() {
        assertNull(parser.testParseDate("invalid-date"));
    }

    @Test
    void testCleanDescription_RemovesExtraSpaces() {
        assertEquals("Description with spaces", 
                    parser.testCleanDescription("Description   with    spaces"));
    }

    @Test
    void testCleanDescription_RemovesLineBreaks() {
        assertEquals("Line one Line two", 
                    parser.testCleanDescription("Line one\nLine two"));
    }

    @Test
    void testCleanDescription_NullInput() {
        assertNull(parser.testCleanDescription(null));
    }

    @Test
    void testBuildTransaction_ValidInputs() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        BigDecimal debit = new BigDecimal("100.00");
        BigDecimal credit = new BigDecimal("0.00");
        BigDecimal balance = new BigDecimal("500.00");

        ParsedTransaction transaction = parser.testBuildTransaction(
            date, "Test Transaction", debit, credit, balance, BigDecimal.ZERO, "REF123"
        );

        assertNotNull(transaction);
        assertEquals(date, transaction.getDate());
        assertEquals("Test Transaction", transaction.getDescription());
        assertEquals(debit, transaction.getAmount());
        assertEquals(balance, transaction.getBalance());
    }

    @Test
    void testBuildTransaction_NullDate() {
        assertNull(parser.testBuildTransaction(
            null, "Test", BigDecimal.TEN, BigDecimal.ZERO, 
            BigDecimal.TEN, BigDecimal.ZERO, null
        ));
    }

    @Test
    void testBuildTransaction_NullDescription() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertNull(parser.testBuildTransaction(
            date, null, BigDecimal.TEN, BigDecimal.ZERO,
            BigDecimal.TEN, BigDecimal.ZERO, null
        ));
    }

    @Test
    void testBuildTransaction_EmptyDescription() {
        LocalDate date = LocalDate.of(2023, 12, 15);
        assertNull(parser.testBuildTransaction(
            date, "", BigDecimal.TEN, BigDecimal.ZERO,
            BigDecimal.TEN, BigDecimal.ZERO, null
        ));
    }
}