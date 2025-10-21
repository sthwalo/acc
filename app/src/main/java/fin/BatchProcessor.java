package fin;

import fin.context.ApplicationContext;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.service.BankStatementProcessingService;
import fin.service.CompanyService;
import fin.state.ApplicationState;
import fin.ui.OutputFormatter;

import java.io.FileWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Batch processor for automated console operations
 * Supports command-line batch processing without interactive input
 */
public class BatchProcessor {
    private final CompanyService companyService;
    private final BankStatementProcessingService bankStatementService;
    private final ApplicationState applicationState;
    private final OutputFormatter outputFormatter;
    
    // Command line argument indices and minimum requirements
    private static final int COMMAND_ARG_INDEX = 1;
    private static final int MIN_ARGS_PROCESS_STATEMENT = 3;
    private static final int MIN_ARGS_CREATE_COMPANY = 3;
    private static final int MIN_ARGS_CREATE_FISCAL_PERIOD = 5;
    private static final int FISCAL_PERIOD_NAME_ARG_INDEX = 2;
    private static final int FISCAL_PERIOD_START_ARG_INDEX = 3;
    private static final int FISCAL_PERIOD_END_ARG_INDEX = 4;
    
    public BatchProcessor(ApplicationContext context) {
        this.companyService = context.get(fin.service.CompanyService.class);
        this.bankStatementService = context.get(fin.service.BankStatementProcessingService.class);
        this.applicationState = context.getApplicationState();
        this.outputFormatter = new OutputFormatter(); // Create a basic output formatter
    }
    
    /**
     * Process batch command from command line arguments
     */
    public void processBatchCommand(String[] args) {
        if (args.length < 2) {
            printUsage();
            return;
        }
        
        String command = args[COMMAND_ARG_INDEX];
        
        switch (command.toLowerCase()) {
            case "process-statement":
                if (args.length < MIN_ARGS_PROCESS_STATEMENT) {
                    System.err.println("❌ Missing file path for process-statement command");
                    printUsage();
                    return;
                }
                processBankStatement(args[2]);
                break;
                
            case "create-company":
                if (args.length < MIN_ARGS_CREATE_COMPANY) {
                    System.err.println("❌ Missing company name for create-company command");
                    printUsage();
                    return;
                }
                createCompany(args[2]);
                break;
                
            case "create-fiscal-period":
                if (args.length < MIN_ARGS_CREATE_FISCAL_PERIOD) {
                    System.err.println("❌ Missing parameters for create-fiscal-period command");
                    printUsage();
                    return;
                }
                createFiscalPeriod(args[FISCAL_PERIOD_NAME_ARG_INDEX], args[FISCAL_PERIOD_START_ARG_INDEX], args[FISCAL_PERIOD_END_ARG_INDEX]);
                break;
                
            default:
                System.err.println("❌ Unknown batch command: " + command);
                printUsage();
        }
    }
    
    /**
     * Process a bank statement in batch mode
     */
    private void processBankStatement(String filePath) {
        try {
            outputFormatter.printHeader("Batch Processing: Bank Statement");
            outputFormatter.printInfo("Processing file: " + filePath);
            
            // Ensure we have company and fiscal period context
            ensureContext();
            
            // Process the bank statement
            outputFormatter.printProcessing("Processing bank statement...");
            List<BankTransaction> transactions = bankStatementService.processStatement(
                filePath, applicationState.getCurrentCompany());
            
            outputFormatter.printSuccess("Successfully processed " + transactions.size() + " transactions");
            
            // Export to CSV automatically
            exportTransactionsToCSV(transactions, "batch_processed");
            
            outputFormatter.printSuccess("Batch processing completed");
            
        } catch (Exception e) {
            outputFormatter.printError("Error in batch processing: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create a company in batch mode
     */
    private void createCompany(String companyName) {
        try {
            outputFormatter.printHeader("Batch Processing: Create Company");
            outputFormatter.printInfo("Creating company: " + companyName);
            
            Company company = new Company(companyName);
            company = companyService.createCompany(company);
            applicationState.setCurrentCompany(company);
            
            outputFormatter.printSuccess("Company created: " + company.getName() + " (ID: " + company.getId() + ")");
            
        } catch (Exception e) {
            outputFormatter.printError("Error creating company: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Create a fiscal period in batch mode
     */
    private void createFiscalPeriod(String periodName, String startDateStr, String endDateStr) {
        try {
            outputFormatter.printHeader("Batch Processing: Create Fiscal Period");
            outputFormatter.printInfo("Creating fiscal period: " + periodName);
            
            // Ensure we have a company selected
            if (!applicationState.hasCurrentCompany()) {
                throw new IllegalStateException("No company selected. Use create-company first.");
            }
            
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);
            
            FiscalPeriod fiscalPeriod = new FiscalPeriod(
                applicationState.getCurrentCompany().getId(), 
                periodName, 
                startDate, 
                endDate);
            
            fiscalPeriod = companyService.createFiscalPeriod(fiscalPeriod);
            applicationState.setCurrentFiscalPeriod(fiscalPeriod);
            
            outputFormatter.printSuccess("Fiscal period created: " + fiscalPeriod.getPeriodName());
            
        } catch (Exception e) {
            outputFormatter.printError("Error creating fiscal period: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Ensure we have the required company and fiscal period context
     */
    private void ensureContext() {
        // Try to select default company if none selected
        if (!applicationState.hasCurrentCompany()) {
            List<Company> companies = companyService.getAllCompanies();
            if (!companies.isEmpty()) {
                applicationState.setCurrentCompany(companies.get(0));
                outputFormatter.printInfo("Selected default company: " + companies.get(0).getName());
            } else {
                throw new IllegalStateException("No companies available. Create a company first.");
            }
        }
        
        // Try to select default fiscal period if none selected
        if (!applicationState.hasCurrentFiscalPeriod()) {
            List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(
                applicationState.getCurrentCompany().getId());
            if (!periods.isEmpty()) {
                applicationState.setCurrentFiscalPeriod(periods.get(0));
                outputFormatter.printInfo("Selected default fiscal period: " + periods.get(0).getPeriodName());
            } else {
                throw new IllegalStateException("No fiscal periods available. Create a fiscal period first.");
            }
        }
    }
    
    /**
     * Export transactions to CSV file
     */
    private void exportTransactionsToCSV(List<BankTransaction> transactions, String prefix) {
        try {
            String timestamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = String.format("%s_%s.csv", prefix, timestamp);
            
            try (FileWriter writer = new FileWriter(filename, java.nio.charset.StandardCharsets.UTF_8)) {
                // Write header
                writer.write(String.format("Date,Details,Debit Amount,Credit Amount,Balance,Account Number%n"));
                
                // Write transactions
                for (BankTransaction tx : transactions) {
                    String formattedLine = String.format("%s,\"%s\",%s,%s,%s,%s%n",
                        tx.getTransactionDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        tx.getDetails() != null ? tx.getDetails().replace("\"", "\"\"") : "",
                        tx.getDebitAmount() != null ? tx.getDebitAmount() : "",
                        tx.getCreditAmount() != null ? tx.getCreditAmount() : "",
                        tx.getBalance() != null ? tx.getBalance() : "",
                        tx.getAccountNumber() != null ? tx.getAccountNumber() : ""
                    );
                    writer.write(formattedLine);
                }
            }
            
            outputFormatter.printSuccess("Transactions exported to: " + filename);
            
        } catch (Exception e) {
            outputFormatter.printError("Error exporting transactions: " + e.getMessage());
        }
    }
    
    /**
     * Print usage information
     */
    private void printUsage() {
        System.out.println("FIN Batch Processing Usage:");
        System.out.println("java -jar app.jar --batch <command> [args...]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  create-company <name>                    Create a new company");
        System.out.println("  create-fiscal-period <name> <start> <end> Create a fiscal period (dates in YYYY-MM-DD)");
        System.out.println("  process-statement <file-path>            Process a bank statement PDF");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar app.jar --batch create-company \"My Company\"");
        System.out.println("  java -jar app.jar --batch create-fiscal-period FY2024-2025 2024-04-01 2025-03-31");
        System.out.println("  java -jar app.jar --batch process-statement input/statement.pdf");
    }
}