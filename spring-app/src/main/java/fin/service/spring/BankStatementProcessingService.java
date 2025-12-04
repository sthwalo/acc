package fin.service.spring;

import fin.context.TransactionParsingContext;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.model.parser.ParsedTransaction;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import fin.service.parser.*;
import fin.validation.BankTransactionValidator;
import fin.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service that orchestrates bank statement processing using the parser framework.
 * This replaces the old BankStatementService and BankStatementParsingService.
 */
@Service
public class BankStatementProcessingService {
    private final List<TransactionParser> parsers;
    private final DocumentTextExtractor textExtractor;
    private final BankTransactionRepository transactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final BankTransactionValidator validator;
    private final SpringCompanyService companyService;

    // Date constants for fiscal year handling
    private static final int DEFAULT_FISCAL_YEAR = 2025;
    private static final int MID_YEAR_MONTH = 6; // June
    private static final int MID_YEAR_DAY = 30; // 30th

    @Autowired
    public BankStatementProcessingService(
            DocumentTextExtractor textExtractor,
            BankTransactionRepository transactionRepository,
            FiscalPeriodRepository fiscalPeriodRepository,
            BankTransactionValidator validator,
            SpringCompanyService companyService,
            StandardBankTabularParser standardBankParser,
            AbsaBankParser absaBankParser,
            FnbBankParser fnbBankParser,
            CreditTransactionParser creditParser,
            ServiceFeeParser serviceFeeParser) {
        this.textExtractor = textExtractor;
        this.transactionRepository = transactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.validator = validator;
        this.companyService = companyService;
        this.parsers = Arrays.asList(standardBankParser, absaBankParser, fnbBankParser, creditParser, serviceFeeParser);
    }

    /**
     * Process a bank statement PDF and return structured transactions.
     */
    public List<BankTransaction> processStatement(String pdfPath, Company company) {
        try {
            List<String> lines = textExtractor.parseDocument(new File(pdfPath));
            List<BankTransaction> transactions = processLines(lines, pdfPath, company);

            // Update each transaction with metadata from the document
            // String accountNumber = textExtractor.getAccountNumber();

            // Note: accountNumber is extracted but not stored in current schema
            // for (BankTransaction transaction : transactions) {
            //     transaction.setAccountNumber(accountNumber);
            // }

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
     * Process a bank statement PDF and return structured transactions.
     */
    public List<BankTransaction> processStatement(String pdfPath, Long companyId) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        return processStatement(pdfPath, company);
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
        // Extract statement date from the PDF content
        LocalDate statementDate = extractStatementDateFromPdf();
        if (statementDate == null) {
            // Fallback to a date in the correct fiscal year (2025)
            statementDate = LocalDate.of(DEFAULT_FISCAL_YEAR, MID_YEAR_MONTH, MID_YEAR_DAY); // Mid fiscal year
        }

        return new TransactionParsingContext.Builder()
            .statementDate(statementDate)
            .sourceFile(pdfPath)
            .build();
    }

    /**
     * Extract statement date from the PDF content.
     * Parses the statement period to get the appropriate year.
     */
    private LocalDate extractStatementDateFromPdf() {
        String statementPeriod = textExtractor.getStatementPeriod();
        if (statementPeriod == null || statementPeriod.isEmpty()) {
            return null;
        }

        try {
            // Parse statement period like "16 January 2025 to 15 February 2025" or "01 Jan 2024 to 31 Jan 2024"
            // We want to extract the year from the end date
            Pattern datePattern = Pattern.compile("to\\s*(\\d{1,2}\\s+\\w+\\s+(\\d{4}))");
            Matcher matcher = datePattern.matcher(statementPeriod);
            if (matcher.find()) {
                String yearStr = matcher.group(2);
                int year = Integer.parseInt(yearStr);

                // Return mid-year date for that year
                return LocalDate.of(year, MID_YEAR_MONTH, MID_YEAR_DAY);
            }

            // Alternative pattern: look for any 4-digit year
            Pattern yearPattern = Pattern.compile("(\\d{4})");
            Matcher yearMatcher = yearPattern.matcher(statementPeriod);
            if (yearMatcher.find()) {
                int year = Integer.parseInt(yearMatcher.group(1));
                return LocalDate.of(year, MID_YEAR_MONTH, MID_YEAR_DAY);
            }

        } catch (Exception e) {
            System.err.println("Failed to parse statement period: " + statementPeriod);
        }

        return null;
    }

    private List<ParsedTransaction> parseTransactions(List<String> lines, TransactionParsingContext context) {
        List<ParsedTransaction> results = new ArrayList<>();

        // Find the StandardBankTabularParser
        StandardBankTabularParser standardParser = null;
        for (TransactionParser parser : parsers) {
            if (parser instanceof StandardBankTabularParser) {
                standardParser = (StandardBankTabularParser) parser;
                break;
            }
        }

        for (String line : lines) {
            // Skip empty lines
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            // Try StandardBankTabularParser first for Standard Bank format
            if (standardParser != null && standardParser.canParse(line, context)) {
                try {
                    ParsedTransaction parsed = standardParser.parse(line, context);
                    if (parsed != null) {
                        results.add(parsed);
                    }
                    // Continue processing even if null (description line)
                    continue;
                } catch (Exception e) {
                    System.err.println("StandardBank parser failed for line: " + line + " - " + e.getMessage());
                }
            }

            // For other formats, use remaining parsers
            for (TransactionParser parser : parsers) {
                if (!(parser instanceof StandardBankTabularParser) && parser.canParse(line, context)) {
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

        // Finalize any pending transactions from StandardBank parser
        if (standardParser != null) {
            try {
                ParsedTransaction finalTransaction = ((StandardBankTabularParser) standardParser).finalizeParsing();
                if (finalTransaction != null) {
                    results.add(finalTransaction);
                }
            } catch (Exception e) {
                System.err.println("Failed to finalize pending transaction - " + e.getMessage());
            }
            ((StandardBankTabularParser) standardParser).reset();
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

            // Set balance if available
            if (parsed.getBalance() != null) {
                transaction.setBalance(parsed.getBalance());
            }

            // Set service fee flag
            transaction.setServiceFee(parsed.hasServiceFee());

            // Determine fiscal period based on transaction date (only if not already set)
            if (transaction.getFiscalPeriodId() == null) {
                FiscalPeriod fiscalPeriod = findFiscalPeriodForDate(company.getId(), parsed.getDate());
                if (fiscalPeriod != null) {
                    transaction.setFiscalPeriodId(fiscalPeriod.getId());
                }
            }

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

    /**
     * Find the fiscal period that contains the given transaction date
     */
    private FiscalPeriod findFiscalPeriodForDate(Long companyId, LocalDate transactionDate) {
        List<FiscalPeriod> fiscalPeriods = fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(companyId);

        for (FiscalPeriod period : fiscalPeriods) {
            if (!transactionDate.isBefore(period.getStartDate()) &&
                !transactionDate.isAfter(period.getEndDate())) {
                return period;
            }
        }

        // No matching fiscal period found
        System.err.println("Warning: No fiscal period found for transaction date: " + transactionDate);
        return null;
    }

    /**
     * Get all transactions for a company
     */
    public List<BankTransaction> getTransactionsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return transactionRepository.findByCompanyId(companyId);
    }

    /**
     * Get transactions for a company within a date range
     */
    public List<BankTransaction> getTransactionsByCompanyAndDateRange(Long companyId, LocalDate startDate, LocalDate endDate) {
        if (companyId == null || startDate == null || endDate == null) {
            throw new IllegalArgumentException("Company ID, start date, and end date are required");
        }
        return transactionRepository.findByCompanyIdAndTransactionDateBetween(companyId, startDate, endDate);
    }

    /**
     * Count transactions for a company
     */
    public long countTransactionsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return transactionRepository.countByCompanyId(companyId);
    }

    /**
     * Process bank statement with MultipartFile (for REST API)
     */
    public StatementProcessingResult processStatement(org.springframework.web.multipart.MultipartFile file, Long companyId, Long fiscalPeriodId) {
        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        try {
            // Create temporary file
            File tempFile = File.createTempFile("bank_statement_", ".pdf");
            file.transferTo(tempFile);

            try {
                List<String> lines = textExtractor.parseDocument(tempFile);
                List<BankTransaction> transactions = processLines(lines, file.getOriginalFilename(), company);

                // Validate and save each transaction
                List<BankTransaction> validTransactions = new ArrayList<>();
                List<String> errors = new ArrayList<>();
                for (BankTransaction transaction : transactions) {
                    // Set fiscal period if provided BEFORE validation
                    if (fiscalPeriodId != null) {
                        transaction.setFiscalPeriodId(fiscalPeriodId);
                    }

                    ValidationResult validationResult = validator.validate(transaction);
                    if (validationResult.isValid()) {
                        validTransactions.add(transactionRepository.save(transaction));
                    } else {
                        // Collect validation errors
                        StringBuilder errorMsg = new StringBuilder("Invalid transaction: " + transaction.getDetails());
                        validationResult.getErrors().forEach(error ->
                            errorMsg.append(" - ").append(error.getField()).append(": ").append(error.getMessage()));
                        errors.add(errorMsg.toString());
                    }
                }

                return new StatementProcessingResult(validTransactions, lines.size(), validTransactions.size(), errors.size(), errors);
            } finally {
                // Clean up temporary file
                tempFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to process uploaded file: " + file.getOriginalFilename(), e);
        }
    }

    /**
     * Get unclassified transactions for a company
     */
    public List<BankTransaction> getUnclassifiedTransactions(Long companyId) {
        List<BankTransaction> transactions = transactionRepository.findByCompanyId(companyId);
        return transactions.stream()
                .filter(t -> t.getAccountCode() == null || t.getAccountCode().trim().isEmpty())
                .toList();
    }

    /**
     * Get processing statistics for a company
     */
    public ProcessingStatistics getProcessingStatistics(Long companyId) {
        long totalTransactions = transactionRepository.countByCompanyId(companyId);
        long classifiedTransactions = transactionRepository.findByCompanyId(companyId).stream()
                .filter(t -> t.getAccountCode() != null && !t.getAccountCode().trim().isEmpty())
                .count();
        // Placeholder values for other stats
        return new ProcessingStatistics(1, totalTransactions, totalTransactions - classifiedTransactions, 1000);
    }

    /**
     * Reprocess transactions for a fiscal period
     */
    public int reprocessTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = transactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        // Placeholder - in real implementation, re-run classification
        return transactions.size();
    }

    /**
     * Result of statement processing operation
     */
    public static class StatementProcessingResult {
        private final List<BankTransaction> transactions;
        private final int processedLines;
        private final int validTransactions;
        private final int invalidTransactions;
        private final List<String> errors;

        public StatementProcessingResult(List<BankTransaction> transactions, int processedLines,
                                       int validTransactions, int invalidTransactions, List<String> errors) {
            this.transactions = transactions;
            this.processedLines = processedLines;
            this.validTransactions = validTransactions;
            this.invalidTransactions = invalidTransactions;
            this.errors = errors;
        }

        public List<BankTransaction> getTransactions() { return transactions; }
        public int getProcessedLines() { return processedLines; }
        public int getValidTransactions() { return validTransactions; }
        public int getInvalidTransactions() { return invalidTransactions; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * Processing statistics for monitoring
     */
    public static class ProcessingStatistics {
        private final long totalFilesProcessed;
        private final long totalTransactionsProcessed;
        private final long totalErrors;
        private final long averageProcessingTimeMs;

        public ProcessingStatistics(long totalFilesProcessed, long totalTransactionsProcessed,
                                  long totalErrors, long averageProcessingTimeMs) {
            this.totalFilesProcessed = totalFilesProcessed;
            this.totalTransactionsProcessed = totalTransactionsProcessed;
            this.totalErrors = totalErrors;
            this.averageProcessingTimeMs = averageProcessingTimeMs;
        }

        public long getTotalFilesProcessed() { return totalFilesProcessed; }
        public long getTotalTransactionsProcessed() { return totalTransactionsProcessed; }
        public long getTotalErrors() { return totalErrors; }
        public long getAverageProcessingTimeMs() { return averageProcessingTimeMs; }
    }
}