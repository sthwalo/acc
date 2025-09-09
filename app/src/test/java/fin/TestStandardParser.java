package fin;

import fin.service.parser.StandardBankTabularParser;
import fin.service.parser.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TestStandardParser {
    public static void main(String[] args) {
        StandardBankTabularParser parser = new StandardBankTabularParser();
        
        // Create context with proper builder
        TransactionParsingContext context = new TransactionParsingContext.Builder()
            .statementDate(LocalDate.of(2024, 11, 16))
            .accountNumber("20316375")
            .statementPeriod("2024-11-16 to 2024-12-14")
            .sourceFile("test.pdf")
            .build();
        
        // Test lines from the sample
        String[] testLines = {
            "IMMEDIATE PAYMENT                                                             3,500.00-                        11 18                     22,053.09",
            "207725668 SIBONGILE DLAMINI",
            "FEE IMMEDIATE PAYMENT                                     ##                     37.00-                        11 18                     22,016.09",
            "IB TRANSFER FROM                                                                                   100.00      11 22                         171.50"
        };
        
        List<ParsedTransaction> transactions = new ArrayList<>();
        
        for (String line : testLines) {
            System.out.println("Testing line: " + line);
            System.out.println("Length: " + line.length());
            
            if (parser.canParse(line, context)) {
                System.out.println("  ✅ Can parse this line");
                try {
                    ParsedTransaction transaction = parser.parse(line, context);
                    if (transaction != null) {
                        transactions.add(transaction);
                        System.out.println("  ✅ Parsed transaction:");
                        System.out.println("    Type: " + transaction.getType());
                        System.out.println("    Amount: " + transaction.getAmount());
                        System.out.println("    Description: " + transaction.getDescription());
                        System.out.println("    Date: " + transaction.getDate());
                        System.out.println("    Balance: " + transaction.getBalance());
                        System.out.println("    Service Fee: " + transaction.hasServiceFee());
                    } else {
                        System.out.println("  🔄 Parsed as description line (null returned)");
                    }
                } catch (Exception e) {
                    System.out.println("  ❌ Error parsing: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("  ❌ Cannot parse this line");
            }
            System.out.println();
        }
        
        System.out.println("Total transactions parsed: " + transactions.size());
    }
}
