package fin.util;

import fin.service.transaction.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.service.parser.AbsaBankParser;
import fin.service.parser.AbstractMultilineTransactionParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Test utility to verify the improved Absa parser handles OCR-extracted text correctly.
 * Processes OCR text line by line and displays parsed transactions.
 */
public class TestAbsaParser {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: java TestAbsaParser <ocr-text-file>");
            System.exit(1);
        }

        String textFile = args[0];
        AbsaBankParser parser = new AbsaBankParser();
        TransactionParsingContext context = new TransactionParsingContext.Builder()
                .statementDate(java.time.LocalDate.of(2023, 2, 20))
                .accountNumber("4068820115")
                .sourceFile(textFile)
                .build();

        System.out.println("Testing Absa Parser on OCR-extracted text: " + textFile);
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
                        
                        // Show first 5 transactions in detail, then just count
                        if (transactionCount == 5) {
                            System.out.println("\n... (showing first 5, continuing parsing) ...\n");
                        }
                    }
                }
            }

            // Finalize any pending multiline transaction
            if (parser instanceof AbstractMultilineTransactionParser) {
                ParsedTransaction lastTx = parser.parse("", context);
                if (lastTx != null) {
                    transactionCount++;
                }
            }

            System.out.println("─".repeat(80));
            System.out.printf("Total transactions parsed: %d\n", transactionCount);
        }
    }
}
