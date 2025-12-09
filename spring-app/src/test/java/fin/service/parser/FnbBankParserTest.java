package fin.service.parser;

import fin.service.transaction.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for FnbBankParser.
 * Tests "Cr" suffix detection and multiline description handling.
 */
class FnbBankParserTest {

    private FnbBankParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new FnbBankParser();
        context = new TransactionParsingContext.Builder()
                .statementDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Test
    void shouldDetectFnbStatementHeader() {
        assertFalse(parser.canParse("FNB", context));
        assertFalse(parser.canParse("First National Bank", context));
        assertFalse(parser.canParse("Account Number: 1234567890", context));
        assertFalse(parser.canParse("Business Account", context));
    }

    @Test
    void shouldSkipHeaderFooterLines() {
        assertFalse(parser.canParse("Date Transaction Description Amount Balance", context));
        assertFalse(parser.canParse("First National Bank", context));
        assertFalse(parser.canParse("Page 1 of 5", context));
        assertFalse(parser.canParse("Account Summary", context));
    }

    @Test
    void shouldParseCreditWithCrSuffix() {
        String line = "02 Apr Magtape Credit Xinghlzana Group 7,500.00Cr 5,969.38Cr";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 4, 2), tx.getDate());
        assertEquals("Magtape Credit Xinghlzana Group", tx.getDescription().trim());
        assertEquals(TransactionType.CREDIT, tx.getType());
        assertEquals(new BigDecimal("7500.00"), tx.getAmount());
        assertEquals(new BigDecimal("5969.38"), tx.getBalance());
    }

    @Test
    void shouldParseDebitWithoutSuffix() {
        String line = "03 Apr Internal Pmt To Rent Ndluhidwe 2,600.00 3,351.38Cr";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 4, 3), tx.getDate());
        assertEquals("Internal Pmt To Rent Ndluhidwe", tx.getDescription().trim());
        assertEquals(TransactionType.DEBIT, tx.getType());
        assertEquals(new BigDecimal("2600.00"), tx.getAmount());
        assertEquals(new BigDecimal("3351.38"), tx.getBalance());
    }

    @Test
    void shouldParseTransactionWithReference() {
        String line = "15/03/2024 EFT Payment Ref: 123456 1,500.00 2,734.56";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 3, 15), tx.getDate());
        assertEquals("EFT Payment (Ref: 123456)", tx.getDescription().trim());
        assertEquals(TransactionType.DEBIT, tx.getType());
        assertEquals(new BigDecimal("1500.00"), tx.getAmount());
        assertEquals(new BigDecimal("2734.56"), tx.getBalance());
    }

    @Test
    void shouldParseServiceFee() {
        String line = "15/03/2024 Monthly Service Fee 150.00 2,584.56";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 3, 15), tx.getDate());
        assertEquals("Monthly Service Fee", tx.getDescription().trim());
        assertEquals(TransactionType.SERVICE_FEE, tx.getType());
        assertEquals(new BigDecimal("150.00"), tx.getAmount());
        assertEquals(new BigDecimal("2584.56"), tx.getBalance());
        assertTrue(tx.hasServiceFee());
    }

    @Test
    void shouldParseBalanceBroughtForward() {
        String line = "01/03/2024 Balance Brought Forward 2,734.56";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 3, 1), tx.getDate());
        assertEquals("Balance Brought Forward", tx.getDescription().trim());
        assertEquals(TransactionType.CREDIT, tx.getType());
        assertEquals(BigDecimal.ZERO, tx.getAmount());
        assertEquals(new BigDecimal("2734.56"), tx.getBalance());
    }

    @Test
    void shouldParseOpeningBalance() {
        String line = "01/03/2024 Opening Balance 2,734.56";
        assertTrue(parser.canParse(line, context));
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(LocalDate.of(2024, 3, 1), tx.getDate());
        assertEquals("Opening Balance", tx.getDescription().trim());
        assertEquals(TransactionType.CREDIT, tx.getType());
        assertEquals(BigDecimal.ZERO, tx.getAmount());
        assertEquals(new BigDecimal("2734.56"), tx.getBalance());
    }

    @Test
    void shouldHandleNegativeAmounts() {
        String line = "15/03/2024 Refund Received 500.00- 2,734.56";
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(TransactionType.CREDIT, tx.getType()); // Negative debit = credit
        assertEquals(new BigDecimal("500.00"), tx.getAmount());
        assertEquals(new BigDecimal("2734.56"), tx.getBalance());
    }

    @Test
    void shouldClassifyDebitTransactions() {
        String[] debitLines = {
            "15/03/2024 ATM Withdrawal - Sandton 500.00 1,234.56",
            "16/03/2024 Debit Order Payment 250.00 984.56",
            "17/03/2024 EFT Out Payment 300.00 684.56"
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
            "15/03/2024 Salary Deposit 5000.00Cr 6,234.56Cr",
            "16/03/2024 Interest Payment 150.00Cr 6,384.56Cr",
            "17/03/2024 EFT In Transfer 1000.00Cr 7,384.56Cr"
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
            "15/03/2024 Monthly Service Fee 150.00 2,584.56",
            "16/03/2024 ATM Fee 5.00 2,579.56",
            "17/03/2024 Account Fee 50.00 2,529.56"
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
            "99/99/9999 Invalid date format"
        };

        for (String line : invalidLines) {
            assertFalse(parser.canParse(line, context));
            assertNull(parser.parse(line, context));
        }
    }

    @Test
    void shouldParseVariousDateFormats() {
        String[] dateLines = {
            "15/03/2024 ATM Withdrawal 500.00 1,234.56",
            "16/04/2024 Deposit 1000.00Cr 2,234.56Cr",
            "01/01/2024 Opening Balance 500.00"
        };

        LocalDate[] expectedDates = {
            LocalDate.of(2024, 3, 15),
            LocalDate.of(2024, 4, 16),
            LocalDate.of(2024, 1, 1)
        };

        for (int i = 0; i < dateLines.length; i++) {
            ParsedTransaction tx = parser.parse(dateLines[i], context);
            assertNotNull(tx);
            assertEquals(expectedDates[i], tx.getDate(), "Line: " + dateLines[i]);
        }
    }

    @Test
    void shouldHandleCommaInAmounts() {
        String line = "15/03/2024 Large Deposit 10,000.00Cr 12,234.56Cr";
        ParsedTransaction tx = parser.parse(line, context);

        assertNotNull(tx);
        assertEquals(new BigDecimal("10000.00"), tx.getAmount());
        assertEquals(new BigDecimal("12234.56"), tx.getBalance());
    }

    @Test
    void shouldParseRealFnbStatementPatterns_INTEGRATION() {
        String[] realLines = {
            "02 Apr Magtape Credit Xinghlzana Group 7,500.00Cr 5,969.38Cr",
            "03 Apr Internal Pmt To Rent Ndluhidwe 2,600.00 3,351.38Cr",
            "09 Apr #Excess Item Fee 1 Items On 24/04/03 155.00 1,157.47 155.00"
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
}