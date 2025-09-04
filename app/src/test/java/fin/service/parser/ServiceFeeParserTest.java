package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ServiceFeeParserTest {
    private ServiceFeeParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new ServiceFeeParser();
        context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
    }

    @Test
    void canParseReturnsTrueForServiceFees() {
        assertTrue(parser.canParse("FEE-ELECTRONIC ACCOUNT PAYMENT 203163753 ## 8.90-", context));
        assertTrue(parser.canParse("FEE - PAYMENT CONFIRM - EMAIL ## 0.80-", context));
        assertTrue(parser.canParse("SERVICE FEE  35.00-", context));
    }

    @Test
    void canParseReturnsFalseForNonFees() {
        assertFalse(parser.canParse("IB PAYMENT TO NTSAKO MAPHOSA", context));
        assertFalse(parser.canParse("CREDIT TRANSFER", context));
        assertFalse(parser.canParse("Fee Debits Credits Date Balance", context)); // Table header
        assertFalse(parser.canParse("Date Details Debit Credit Balance", context)); // Table header
    }

    @Test
    void parseExtractsServiceFeeAmount() {
        String line = "FEE-ELECTRONIC ACCOUNT PAYMENT 203163753 ## 8.90-";
        ParsedTransaction fee = parser.parse(line, context);
        
        assertNotNull(fee);
        assertEquals(TransactionType.SERVICE_FEE, fee.getType());
        assertEquals(new BigDecimal("8.90"), fee.getAmount());
        assertEquals("FEE-ELECTRONIC ACCOUNT PAYMENT 203163753", fee.getDescription().trim());
    }

    @Test
    void parseHandlesEmailConfirmationFee() {
        String line = "FEE: PAYMENT CONFIRM - EMAIL 0.80- ##";
        ParsedTransaction fee = parser.parse(line, context);
        
        assertNotNull(fee);
        assertEquals(TransactionType.SERVICE_FEE, fee.getType());
        assertEquals(new BigDecimal("0.80"), fee.getAmount());
        assertEquals("FEE: PAYMENT CONFIRM - EMAIL", fee.getDescription().trim());
    }
}
