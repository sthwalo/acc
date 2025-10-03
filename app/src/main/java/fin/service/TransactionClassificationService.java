package fin.service;

import fin.model.Company;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified Transaction Classification Service
 * 
 * This service consolidates all transaction classification operations into a single,
 * clear entry point. It replaces the confusing mix of:
 * - TransactionClassifier (thin wrapper)
 * - ClassificationIntegrationService (orchestrator)
 * - TransactionMappingService (implementation)
 * 
 * Architecture:
 * - Orchestrates classification workflows
 * - Delegates to specialized services (ChartOfAccounts, MappingRules)
 * - Provides clear API for controllers
 * 
 * @author Sthwalo Nyoni
 * @version 1.0
 * @since 2025-10-03
 */
public class TransactionClassificationService {
    private static final Logger LOGGER = Logger.getLogger(TransactionClassificationService.class.getName());
    
    private final String dbUrl;
    private final AccountClassificationService accountClassificationService;
    private final TransactionMappingRuleService ruleService;
    private final TransactionMappingService mappingService;
    private final InteractiveClassificationService interactiveService;
    
    /**
     * Constructor with full dependency injection
     * 
     * NOTE: ChartOfAccountsService has been DEPRECATED in favor of AccountClassificationService
     * which provides standard SARS-compliant South African chart of accounts structure.
     */
    public TransactionClassificationService(String dbUrl,
                                           TransactionMappingRuleService ruleService,
                                           TransactionMappingService mappingService,
                                           InteractiveClassificationService interactiveService) {
        this.dbUrl = dbUrl;
        this.accountClassificationService = new AccountClassificationService(dbUrl);
        this.ruleService = ruleService;
        this.mappingService = mappingService;
        this.interactiveService = interactiveService;
        
        LOGGER.info("TransactionClassificationService initialized with AccountClassificationService");
    }
    
    /**
     * Simplified constructor for backward compatibility
     * Creates its own service dependencies
     */
    public TransactionClassificationService(String dbUrl) {
        this.dbUrl = dbUrl;
        
        // Use AccountClassificationService as single source of truth for chart of accounts
        // Standard SARS-compliant South African accounting structure (accounts 1000-9999)
        this.accountClassificationService = new AccountClassificationService(dbUrl);
        this.ruleService = new TransactionMappingRuleService(dbUrl);
        this.mappingService = new TransactionMappingService(dbUrl);
        this.interactiveService = new InteractiveClassificationService();
        
        LOGGER.info("TransactionClassificationService initialized (simplified) with AccountClassificationService");
    }
    
    // ============================================================================
    // PUBLIC API - Main Entry Points
    // ============================================================================
    
    /**
     * Initialize chart of accounts for a company
     * This creates the standard account structure and categories
     * 
     * @param companyId The company to initialize
     * @return true if successful, false otherwise
     */
    public boolean initializeChartOfAccounts(Long companyId) {
        try {
            LOGGER.info("Initializing chart of accounts for company: " + companyId);
            
            // Use AccountClassificationService to create full chart of accounts
            // This includes: account categories, standard accounts, and pattern analysis
            accountClassificationService.initializeChartOfAccounts(companyId);
            
            LOGGER.info("Chart of accounts initialization completed successfully");
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts", e);
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Initialize transaction mapping rules for a company
     * This creates the standard pattern-matching rules for auto-classification
     * 
     * @param companyId The company to initialize
     * @return true if successful, false otherwise
     */
    public boolean initializeTransactionMappingRules(Long companyId) {
        try {
            LOGGER.info("Initializing transaction mapping rules for company: " + companyId);
            
            // Use the TransactionMappingService to create standard rules
            int rulesCreated = mappingService.createStandardMappingRules(companyId);
            
            System.out.println("✅ Created " + rulesCreated + " standard mapping rules");
            LOGGER.info("Created " + rulesCreated + " mapping rules for company: " + companyId);
            
            return rulesCreated > 0;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules", e);
            System.err.println("❌ Error initializing mapping rules: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Perform full initialization of a company's financial structure
     * This includes both chart of accounts AND mapping rules
     * 
     * @param companyId The company to initialize
     * @return true if successful, false otherwise
     */
    public boolean performFullInitialization(Long companyId) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🚀 FULL INITIALIZATION FOR COMPANY ID: " + companyId);
            System.out.println("=".repeat(80));
            
            // Step 1: Initialize chart of accounts
            System.out.println("\n📋 Step 1: Initializing Chart of Accounts...");
            boolean accountsOk = initializeChartOfAccounts(companyId);
            if (!accountsOk) {
                System.err.println("❌ Chart of accounts initialization failed");
                return false;
            }
            
            // Step 2: Initialize mapping rules
            System.out.println("\n📋 Step 2: Initializing Transaction Mapping Rules...");
            boolean rulesOk = initializeTransactionMappingRules(companyId);
            if (!rulesOk) {
                System.err.println("❌ Mapping rules initialization failed");
                return false;
            }
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ FULL INITIALIZATION COMPLETED SUCCESSFULLY");
            System.out.println("=".repeat(80));
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during full initialization", e);
            System.err.println("❌ Error during full initialization: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Run interactive classification for unclassified transactions
     * User is prompted to classify each transaction manually
     * 
     * @param companyId The company to classify transactions for
     * @param fiscalPeriodId The fiscal period to classify
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
            System.out.println("🏢 INTERACTIVE CLASSIFICATION FOR: " + company.getName());
            System.out.println("=".repeat(80));
            
            // Count unclassified transactions
            int count = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            System.out.println("Found " + count + " unclassified transactions");
            
            if (count > 0) {
                // Delegate to interactive service
                interactiveService.runInteractiveCategorization(companyId, fiscalPeriodId);
            } else {
                System.out.println("✅ All transactions are already classified!");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running interactive classification", e);
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Auto-classify all unclassified transactions using mapping rules
     * 
     * @param companyId The company to classify transactions for
     * @param fiscalPeriodId The fiscal period to classify
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
            
            // Run auto-classification
            int classifiedCount = mappingService.classifyAllUnclassifiedTransactions(
                companyId, "AUTO-CLASSIFY");
            
            System.out.println("✅ Auto-classified " + classifiedCount + " transactions");
            
            // Check if any remain unclassified
            int remainingCount = countUnclassifiedTransactions(companyId, fiscalPeriodId);
            if (remainingCount > 0) {
                System.out.println("⚠️  " + remainingCount + " transactions still need manual classification");
                System.out.println("   Use Interactive Classification to classify them");
            } else {
                System.out.println("✅ All transactions are now classified!");
            }
            
            return classifiedCount;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Synchronize journal entries with classified transactions
     * Ensures all classified transactions have corresponding journal entries
     * 
     * @param companyId The company to synchronize
     * @param fiscalPeriodId The fiscal period to synchronize
     * @return Number of transactions synchronized
     */
    public int synchronizeJournalEntries(Long companyId, Long fiscalPeriodId) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔄 SYNCHRONIZING JOURNAL ENTRIES");
            System.out.println("=".repeat(80));
            
            // Count classified transactions without journal entries
            int count = countClassifiedWithoutJournalEntries(companyId, fiscalPeriodId);
            System.out.println("Found " + count + " classified transactions without journal entries");
            
            if (count == 0) {
                System.out.println("✅ All classified transactions already have journal entries");
                return 0;
            }
            
            // Generate journal entries
            mappingService.generateJournalEntriesForUnclassifiedTransactions(companyId, "SYNC");
            
            System.out.println("✅ Successfully synchronized " + count + " transactions");
            return count;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error synchronizing journal entries", e);
            System.err.println("❌ Error: " + e.getMessage());
            return 0;
        }
    }
    
    // ============================================================================
    // HELPER METHODS - Internal Operations
    // ============================================================================
    
    /**
     * Get a company by ID from database
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
     * Count classified transactions without journal entries
     */
    private int countClassifiedWithoutJournalEntries(Long companyId, Long fiscalPeriodId) {
        String sql = """
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
            LOGGER.log(Level.SEVERE, "Error counting classified without journal entries", e);
        }
        
        return 0;
    }
    
    /**
     * Get the interactive classification service
     * For backward compatibility with existing code
     */
    public InteractiveClassificationService getInteractiveClassificationService() {
        return interactiveService;
    }
}
