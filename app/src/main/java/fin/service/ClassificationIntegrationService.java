package fin.service;

import fin.app.ChartOfAccountsInitializer;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Integration service that connects the classification system with the main App
 * Provides methods to initialize accounts, classify transactions, and
 * synchronize classification data with the rest of the system.
 */
public class ClassificationIntegrationService {
    private static final Logger LOGGER = Logger.getLogger(ClassificationIntegrationService.class.getName());
    private final String dbUrl;
    private final InteractiveClassificationService classificationService;
    private final ChartOfAccountsInitializer accountsInitializer;
    private final TransactionMappingService mappingService;
    
    public ClassificationIntegrationService() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.classificationService = new InteractiveClassificationService();
        this.accountsInitializer = new ChartOfAccountsInitializer();
        this.mappingService = new TransactionMappingService(dbUrl);
    }
    
    /**
     * Initialize the chart of accounts for a company
     * 
     * @param companyId The ID of the company to initialize
     * @return True if successful, false otherwise
     */
    public boolean initializeChartOfAccounts(Long companyId) {
        try {
            accountsInitializer.initializeChartOfAccounts(companyId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts", e);
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize transaction mapping rules for a company
     * 
     * @param companyId The ID of the company to initialize
     * @return True if successful, false otherwise
     */
    public boolean initializeTransactionMappingRules(Long companyId) {
        try {
            accountsInitializer.initializeTransactionMappingRules(companyId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules", e);
            System.err.println("❌ Error initializing transaction mapping rules: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform full initialization of a company's financial structure
     * 
     * @param companyId The ID of the company to initialize
     * @return True if successful, false otherwise
     */
    public boolean performFullInitialization(Long companyId) {
        try {
            accountsInitializer.performFullInitialization(companyId);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during full initialization", e);
            System.err.println("❌ Error during full initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run interactive classification for a company and fiscal period
     * 
     * @param companyId The ID of the company
     * @param fiscalPeriodId The ID of the fiscal period
     */
    public void runInteractiveClassification(Long companyId, Long fiscalPeriodId) {
        try {
            // Get company information
            Company company = getCompanyById(companyId);
            if (company == null) {
                System.err.println("❌ Company not found with ID: " + companyId);
                return;
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🏢 INTERACTIVE TRANSACTION CLASSIFICATION FOR: " + company.getName());
            System.out.println("=".repeat(80));
            
            // Count unclassified transactions
            int count = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            System.out.println("Found " + count + " unclassified transactions");
            
            // If there are unclassified transactions, run the classification
            if (count > 0) {
                classificationService.runInteractiveCategorization(companyId, fiscalPeriodId);
            } else {
                System.out.println("✅ All transactions are already classified!");
                
                // Ask user if they want to review existing classifications
                Scanner scanner = new Scanner(System.in);
                System.out.print("\nDo you want to review existing classifications? (y/n): ");
                String response = scanner.nextLine().trim();
                
                if (response.toLowerCase().startsWith("y")) {
                    // Show and allow editing of existing classifications
                    showAndEditExistingClassifications(companyId, fiscalPeriodId, scanner);
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running interactive classification", e);
            System.err.println("❌ Error running interactive classification: " + e.getMessage());
        }
    }
    
    /**
     * Auto-classify all unclassified transactions for a company
     * 
     * @param companyId The ID of the company
     * @param fiscalPeriodId The ID of the fiscal period
     * @return Number of transactions classified
     */
    public int autoClassifyTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🤖 AUTO-CLASSIFICATION OF TRANSACTIONS");
            System.out.println("=".repeat(80));
            
            // Count unclassified transactions
            int count = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            System.out.println("Found " + count + " unclassified transactions");
            
            if (count == 0) {
                System.out.println("✅ All transactions are already classified!");
                return 0;
            }
            
            // Run the auto-classification
            int classifiedCount = mappingService.classifyAllUnclassifiedTransactions(companyId, "AUTO-CLASSIFY");
            System.out.println("✅ Auto-classified " + classifiedCount + " transactions");
            
            // Verify if we have any transactions left unclassified
            int remainingCount = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            if (remainingCount > 0) {
                System.out.println("⚠️ " + remainingCount + " transactions still need manual classification");
                System.out.println("   Use the Interactive Classification to classify them");
            } else {
                System.out.println("✅ All transactions are now classified!");
            }
            
            return classifiedCount;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions", e);
            System.err.println("❌ Error auto-classifying transactions: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Show and allow editing of existing classifications
     */
    private void showAndEditExistingClassifications(Long companyId, Long fiscalPeriodId, Scanner scanner) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔍 REVIEW EXISTING CLASSIFICATIONS");
            System.out.println("=".repeat(80));
            
            // Get classified transactions
            List<BankTransaction> transactions = getClassifiedTransactions(companyId, fiscalPeriodId);
            
            if (transactions.isEmpty()) {
                System.out.println("No classified transactions found.");
                return;
            }
            
            System.out.println("Found " + transactions.size() + " classified transactions");
            
            int page = 0;
            int pageSize = 10;
            boolean exit = false;
            
            while (!exit) {
                // Calculate start and end indices for current page
                int start = page * pageSize;
                int end = Math.min(start + pageSize, transactions.size());
                
                // Display current page of transactions
                System.out.println("\nShowing transactions " + (start + 1) + " to " + end + " of " + transactions.size());
                System.out.println("-".repeat(80));
                
                for (int i = start; i < end; i++) {
                    BankTransaction tx = transactions.get(i);
                    System.out.printf("%d. [%s] %s - %s\n", (i - start + 1),
                        tx.getTransactionDate(),
                        tx.getDetails().substring(0, Math.min(40, tx.getDetails().length())),
                        tx.getAccountCode() + " - " + tx.getAccountName());
                }
                
                System.out.println("\nCommands: (n)ext page, (p)revious page, (e)dit #, (q)uit");
                System.out.print("Enter command: ");
                String command = scanner.nextLine().trim();
                
                if (command.equalsIgnoreCase("n") && end < transactions.size()) {
                    page++;
                } else if (command.equalsIgnoreCase("p") && page > 0) {
                    page--;
                } else if (command.equalsIgnoreCase("q")) {
                    exit = true;
                } else if (command.toLowerCase().startsWith("e")) {
                    // Parse the transaction number
                    try {
                        int num = Integer.parseInt(command.substring(1).trim());
                        if (num >= 1 && num <= (end - start)) {
                            BankTransaction tx = transactions.get(start + num - 1);
                            editTransactionClassification(tx, scanner);
                        } else {
                            System.out.println("Invalid transaction number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid command. Use e1, e2, etc. to edit a transaction.");
                    }
                } else {
                    System.out.println("Invalid command.");
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error showing existing classifications", e);
            System.err.println("❌ Error showing existing classifications: " + e.getMessage());
        }
    }
    
    /**
     * Edit the classification of a transaction
     */
    private void editTransactionClassification(BankTransaction transaction, Scanner scanner) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🖊️ EDIT TRANSACTION CLASSIFICATION");
        System.out.println("=".repeat(80));
        
        System.out.println("Transaction Date: " + transaction.getTransactionDate());
        System.out.println("Details: " + transaction.getDetails());
        
        if (transaction.getDebitAmount() != null) {
            System.out.println("Debit Amount: " + transaction.getDebitAmount());
        }
        if (transaction.getCreditAmount() != null) {
            System.out.println("Credit Amount: " + transaction.getCreditAmount());
        }
        
        System.out.println("Current Classification: " + transaction.getAccountCode() + " - " + transaction.getAccountName());
        
        System.out.print("\nEnter new account code: ");
        String accountCode = scanner.nextLine().trim();
        
        System.out.print("Enter new account name: ");
        String accountName = scanner.nextLine().trim();
        
        if (accountCode.isEmpty() || accountName.isEmpty()) {
            System.out.println("⚠️ Classification not changed (empty input)");
            return;
        }
        
        // Update the classification
        if (mappingService.classifyTransaction(transaction, accountCode, accountName)) {
            System.out.println("✅ Transaction reclassified as " + accountCode + " - " + accountName);
            
            // Also create a rule for future similar transactions
            System.out.print("Create rule for similar transactions? (y/n): ");
            String createRule = scanner.nextLine().trim();
            
            if (createRule.toLowerCase().startsWith("y")) {
                classificationService.createMappingRule(
                    transaction.getCompanyId(), transaction.getDetails(), accountCode, accountName);
                System.out.println("✅ Classification rule created for future similar transactions");
            }
        } else {
            System.out.println("❌ Failed to reclassify transaction");
        }
    }
    
    /**
     * Count unclassified transactions for a company and fiscal period
     */
    private int countUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        String sql = """
            SELECT COUNT(*) 
            FROM bank_transactions 
            WHERE company_id = ? 
            AND fiscal_period_id = ? 
            AND account_code IS NULL
            """;
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting unclassified transactions", e);
        }
        
        return 0;
    }
    
    /**
     * Get classified transactions for a company and fiscal period
     */
    private List<BankTransaction> getClassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        String sql = """
            SELECT *
            FROM bank_transactions
            WHERE company_id = ?
            AND fiscal_period_id = ?
            AND account_code IS NOT NULL
            ORDER BY transaction_date DESC
            LIMIT 100
            """;
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BankTransaction tx = new BankTransaction();
                    tx.setId(rs.getLong("id"));
                    tx.setCompanyId(rs.getLong("company_id"));
                    tx.setFiscalPeriodId(rs.getLong("fiscal_period_id"));
                    tx.setBankAccountId(rs.getLong("bank_account_id"));
                    tx.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
                    tx.setDetails(rs.getString("details"));
                    tx.setDebitAmount(rs.getBigDecimal("debit_amount"));
                    tx.setCreditAmount(rs.getBigDecimal("credit_amount"));
                    tx.setBalance(rs.getBigDecimal("balance"));
                    tx.setAccountCode(rs.getString("account_code"));
                    tx.setAccountName(rs.getString("account_name"));
                    transactions.add(tx);
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting classified transactions", e);
        }
        
        return transactions;
    }
    
    /**
     * Get a company by ID
     */
    private Company getCompanyById(Long companyId) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Company company = new Company(rs.getString("name"));
                    company.setId(rs.getLong("id"));
                    company.setRegistrationNumber(rs.getString("registration_number"));
                    company.setTaxNumber(rs.getString("tax_number"));
                    company.setAddress(rs.getString("address"));
                    company.setContactEmail(rs.getString("contact_email"));
                    company.setContactPhone(rs.getString("contact_phone"));
                    return company;
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting company by ID", e);
        }
        
        return null;
    }
    
    /**
     * Synchronize journal entries with bank transactions
     * This ensures that all classified transactions have corresponding journal entries
     * 
     * @param companyId The ID of the company
     * @param fiscalPeriodId The ID of the fiscal period
     * @return Number of transactions synchronized
     */
    public int synchronizeJournalEntries(Long companyId, Long fiscalPeriodId) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔄 SYNCHRONIZING JOURNAL ENTRIES");
            System.out.println("=".repeat(80));
            
            // First, check if there are any classified transactions without journal entries
            String countSql = """
                SELECT COUNT(*) 
                FROM bank_transactions bt
                WHERE bt.company_id = ? 
                AND bt.fiscal_period_id = ?
                AND bt.account_code IS NOT NULL
                AND bt.id NOT IN (
                    SELECT DISTINCT source_transaction_id 
                    FROM journal_entry_lines 
                    WHERE source_transaction_id IS NOT NULL
                )
                """;
                
            int count = 0;
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(countSql)) {
                
                stmt.setLong(1, companyId);
                stmt.setLong(2, fiscalPeriodId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        count = rs.getInt(1);
                    }
                }
            }
            
            System.out.println("Found " + count + " classified transactions without journal entries");
            
            if (count == 0) {
                System.out.println("✅ All classified transactions already have journal entries");
                return 0;
            }
            
            // Generate journal entries for these transactions
            mappingService.generateJournalEntriesForUnclassifiedTransactions(companyId, "SYNC");
            
            System.out.println("✅ Successfully synchronized " + count + " transactions");
            return count;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error synchronizing journal entries", e);
            System.err.println("❌ Error synchronizing journal entries: " + e.getMessage());
            return 0;
        }
    }
}
