package fin.service.parser;

import fin.service.transaction.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class AbsaBankParserDebugTest {
    @Test
    public void debugParsingLines() {
        AbsaBankParser parser = new AbsaBankParser();
        TransactionParsingContext context = new TransactionParsingContext.Builder()
                .statementDate(LocalDate.of(2024, 1, 1))
                .build();

        String[] lines = {
                "23/02/2023 Atm Payment Fr Killarney 600.00 54 882.66",
                "24/02/2023 Debit Order Payment 250.00 54 632.66",
                "25/02/2023 Cash Withdrawal 200.00 54 432.66",
                "23/02/2023 Digital Payment Dt Settlement 10.00 1 300.00 53 582.66",
                "23/02/2023 Monthly Service Fee 150.00 54 732.66",
                "24/02/2023 Atm Fee 5.00 54 727.66",
                "25/02/2023 Account Maintenance Fee 50.00 54 677.66",
                "23/02/2023 Digital Payment Dt Settlement 1 300.00 53 582.66"
        };

        for (String line : lines) {
            parser.reset();
            ParsedTransaction tx = parser.parse(line, context);
            System.out.println("Line: " + line);
            System.out.println("  -> type=" + (tx == null ? "null" : tx.getType()) + ", amount=" + (tx == null ? "null" : tx.getAmount()) + ", balance=" + (tx == null ? "null" : tx.getBalance()) + ", serviceFee=" + (tx == null ? "null" : tx.hasServiceFee()));
        }
    }
}
