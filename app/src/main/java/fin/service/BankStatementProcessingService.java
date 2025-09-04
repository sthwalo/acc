package fin.service;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.parser.ParsedTransaction;
import fin.service.parser.*;
import fin.repository.BankTransactionRepository;
import fin.validation.BankTransactionValidator;
import fin.validation.ValidationResult;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * New service that orchestrates bank statement processing using the parser framework.
 * This replaces the old BankStatementService and BankStatementParsingService.
 */
public class BankStatementProcessingService {
    private final List<TransactionParser> parsers;
    private final DocumentTextExtractor textExtractor;
    private final BankTransactionRepository transactionRepository;
    private final BankTransactionValidator validator;

    public BankStatementProcessingService(String dbUrl) {
        this.textExtractor = new DocumentTextExtractor();
        this.transactionRepository = new BankTransactionRepository(dbUrl);
        this.validator = new BankTransactionValidator();
        this.parsers = new ArrayList<>();
        
        // Register parsers in order of specificity
        parsers.add(new ServiceFeeParser());
        parsers.add(new CreditTransactionParser());
        parsers.add(new MultiTransactionParser());
    }

    /**
     * Process a bank statement PDF and return structured transactions.
     */
    public List<BankTransaction> processStatement(String pdfPath, Company company) {
        try {
            List<String> lines = textExtractor.parseDocument(new File(pdfPath));
            List<BankTransaction> transactions = processLines(lines, pdfPath, company);
            
            // Update each transaction with metadata from the document
            String accountNumber = textExtractor.getAccountNumber();
            String statementPeriod = textExtractor.getStatementPeriod();
            
            for (BankTransaction transaction : transactions) {
                transaction.setAccountNumber(accountNumber);
                // TODO: Parse and set statement period once FiscalPeriod support is added
            }
            
            // Validate and save each transaction
            List<BankTransaction> validTransactions = new ArrayList<>();
            for (BankTransaction transaction : transactions) {
                ValidationResult validationResult = validator.validate(transaction);
                if (validationResult.isValid()) {
                    validTransactions.add(transactionRepository.save(transaction));
                } else {
                    // Log validation errors
                    System.err.println("Invalid transaction: " + transaction.getDetails());
                    validationResult.getErrors().forEach(error -> 
                        System.err.println(" - " + error.getField() + ": " + error.getMessage()));
                }
            }
            
            return validTransactions;
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF: " + pdfPath, e);
        }
    }
    
    /**
     * Process a list of bank statement lines and return structured transactions.
     * This method is primarily used for testing.
     */
    public List<BankTransaction> processLines(List<String> lines, String sourceName, Company company) {
        // Create parsing context
        TransactionParsingContext context = createParsingContext(sourceName);
        
        // Parse transactions using registered parsers
        List<ParsedTransaction> parsedTransactions = parseTransactions(lines, context);
        
        // Convert to BankTransaction entities and validate
        List<BankTransaction> transactions = convertToEntities(parsedTransactions, company);
        
        // When used for testing, don't save to database
        return transactions;
    }
    
    /**
     * Process and save a list of transactions.
     * Used for batch processing and importing.
     */
    public List<BankTransaction> processAndSaveTransactions(List<BankTransaction> transactions) {
        List<BankTransaction> savedTransactions = new ArrayList<>();
        
        for (BankTransaction transaction : transactions) {
            ValidationResult validationResult = validator.validate(transaction);
            if (validationResult.isValid()) {
                savedTransactions.add(transactionRepository.save(transaction));
            } else {
                // Log validation errors
                System.err.println("Invalid transaction: " + transaction.getDetails());
                validationResult.getErrors().forEach(error -> 
                    System.err.println(" - " + error.getField() + ": " + error.getMessage()));
            }
        }
        
        return savedTransactions;
    }

    private TransactionParsingContext createParsingContext(String pdfPath) {
        // TODO: Extract metadata from PDF for context
        return new TransactionParsingContext.Builder()
            .statementDate(LocalDate.now())  // Temporary
            .sourceFile(pdfPath)
            .build();
    }

    private List<ParsedTransaction> parseTransactions(List<String> lines, TransactionParsingContext context) {
        List<ParsedTransaction> results = new ArrayList<>();
        
        for (String line : lines) {
            // Skip empty lines and header/footer lines
            if (line == null || line.trim().isEmpty() || !textExtractor.isTransaction(line)) {
                continue;
            }
            
            // Find the first parser that can handle this line
            for (TransactionParser parser : parsers) {
                if (parser.canParse(line, context)) {
                    try {
                        ParsedTransaction parsed = parser.parse(line, context);
                        if (parsed != null) {
                            results.add(parsed);
                            break; // Stop after first successful parse
                        }
                    } catch (Exception e) {
                        // Log parsing error but continue with other lines
                        System.err.println("Failed to parse line: " + line + " - " + e.getMessage());
                        break; // Skip this line and continue with next
                    }
                }
            }
        }
        
        return results;
    }

    private List<BankTransaction> convertToEntities(List<ParsedTransaction> parsedTransactions, Company company) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        for (ParsedTransaction parsed : parsedTransactions) {
            BankTransaction transaction = new BankTransaction();
            transaction.setCompanyId(company.getId());
            transaction.setTransactionDate(parsed.getDate());
            transaction.setDetails(parsed.getDescription());
            
            // Set amounts based on transaction type
            switch (parsed.getType()) {
                case CREDIT:
                    transaction.setCreditAmount(parsed.getAmount());
                    transaction.setDebitAmount(java.math.BigDecimal.ZERO);
                    break;
                case DEBIT:
                case SERVICE_FEE:
                    transaction.setDebitAmount(parsed.getAmount());
                    transaction.setCreditAmount(java.math.BigDecimal.ZERO);
                    break;
            }
            
            transactions.add(transaction);
        }
        
        return transactions;
    }
}
