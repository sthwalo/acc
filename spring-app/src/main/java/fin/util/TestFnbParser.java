package fin.util;

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.service.parser.FnbBankParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Test utility to verify the improved FNB parser handles bank charges with # prefix.
 */
public class TestFnbParser {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java TestFnbParser <text-file>");
            System.exit(1);
        }

        String textFile = args[0];
        FnbBankParser parser = new FnbBankParser();
        TransactionParsingContext context = new TransactionParsingContext.Builder()
                .statementDate(java.time.LocalDate.of(2024, 2, 1))
                .accountNumber("123456789")
                .sourceFile(textFile)
                .build();

        System.out.println("Testing FNB Parser on text: " + textFile);
        System.out.println("─".repeat(80));

        try (BufferedReader reader = new BufferedReader(new FileReader(textFile))) {
            String line;
            int lineNum = 0;
            int transactionCount = 0;

            while ((line = reader.readLine()) != null) {
                lineNum++;
                
                if (parser.canParse(line, context)) {
                    ParsedTransaction tx = parser.parse(line, context);
                    
                    if (tx != null) {
                        transactionCount++;
                        
                        // Display transaction details
                        System.out.printf("\nTransaction #%d (Line %d):\n", transactionCount, lineNum);
                        System.out.println("  Date: " + tx.getDate());
                        System.out.println("  Type: " + tx.getType());
                        System.out.println("  Description: " + tx.getDescription());
                        System.out.println("  Amount: " + tx.getAmount());
                        System.out.println("  Balance: " + tx.getBalance());
                        System.out.println("  Has Service Fee: " + tx.hasServiceFee());
                        
                        // Highlight bank charges
                        if (line.trim().startsWith("#")) {
                            System.out.println("  ** BANK CHARGE **");
                        }
                    }
                }
            }

            System.out.println("─".repeat(80));
            System.out.printf("Total transactions parsed: %d\n", transactionCount);
        }
    }
}
