import fin.service.parser.ServiceFeeParser;
import fin.service.parser.TransactionParsingContext;
import java.time.LocalDate;

public class TestParserFix {
    public static void main(String[] args) {
        ServiceFeeParser parser = new ServiceFeeParser();
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
        
        // Test the problematic line that was causing the error
        String problematicLine = "Fee Debits Credits Date Balance";
        
        System.out.println("Testing line: '" + problematicLine + "'");
        System.out.println("Can parse: " + parser.canParse(problematicLine, context));
        
        // Test a valid service fee line
        String validLine = "SERVICE FEE  35.00-";
        System.out.println("Testing line: '" + validLine + "'");
        System.out.println("Can parse: " + parser.canParse(validLine, context));
        
        // Test another table header
        String headerLine = "Date Details Debit Credit Balance";
        System.out.println("Testing line: '" + headerLine + "'");
        System.out.println("Can parse: " + parser.canParse(headerLine, context));
    }
}
