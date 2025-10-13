package fin.service;

import fin.model.*;

import java.sql.*;
import java.util.*;
import java.math.BigDecimal;

/**
 * Service for classifying and categorizing accounts based on transaction analysis.
 * Sets up a standard chart of accounts for South African busines        // FINANCE COSTS (9000-9999)
        Long financeCostsId = categoryIds.get("FINANCE_COSTS");
        
        accounts.add(new AccountDefinition("9500", "Interest Expense", "Interest on loans and credit", financeCostsId));
        // Sub-account for excess interest charges
        accounts.add(new AccountDefinition("9500-001", "Excess Interest Expense", "Excess interest charges on overdrafts", financeCostsId));
        
        accounts.add(new AccountDefinition("9600", "Bank Charges", "Bank fees and transaction costs", financeCostsId));
        // Sub-accounts for bank-specific fees
        accounts.add(new AccountDefinition("9600-001", "Standard Bank Fees", "Standard Bank service fees", financeCostsId));
        accounts.add(new AccountDefinition("9600-002", "Capitec Bank Fees", "Capitec Bank service fees", financeCostsId));
        accounts.add(new AccountDefinition("9600-003", "ATM Withdrawal Fees", "ATM withdrawal charges", financeCostsId));
        accounts.add(new AccountDefinition("9600-004", "EFT Transaction Fees", "Electronic funds transfer charges", financeCostsId));
        accounts.add(new AccountDefinition("9600-005", "Debit Order Fees", "Debit order processing fees", financeCostsId));
        accounts.add(new AccountDefinition("9600-999", "Other Bank Fees", "Other banking charges", financeCostsId));
        
        accounts.add(new AccountDefinition("9700", "Foreign Exchange Loss", "Loss on currency conversion", financeCostsId));ities.
 */
public class AccountClassificationService {
    
    private final String dbUrl;
    private final CompanyService companyService;
    
    public AccountClassificationService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.companyService = new CompanyService(dbUrl);
    }
    
    /**
     * Initialize the complete chart of accounts for a company
     */
    public void initializeChartOfAccounts(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }
            
            System.out.println("🏗️ Initializing Chart of Accounts for: " + company.getName());
            
            // 1. Create account categories
            Map<String, Long> categoryIds = createAccountCategories(companyId);
            
            // 2. Create accounts
            createStandardAccounts(companyId, categoryIds);
            
            // 3. Analyze and classify existing transactions
            analyzeTransactionPatterns(companyId);
            
            System.out.println("✅ Chart of Accounts initialization complete!");
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Initialize transaction mapping rules for a company
     * This creates the standard pattern-matching rules for auto-classification
     * 
     * @param companyId The company to initialize
     * @return Number of rules created
     */
    public int initializeTransactionMappingRules(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }
            
            System.out.println("🔗 Initializing Transaction Mapping Rules for: " + company.getName());
            
            // Get rules from single source of truth (this service)
            List<TransactionMappingRule> rules = getStandardMappingRules();
            
            // Persist to database using TransactionMappingRuleService
            TransactionMappingRuleService ruleService = new TransactionMappingRuleService(dbUrl);
            ruleService.persistStandardRules(companyId, rules);
            
            System.out.println("✅ Created " + rules.size() + " standard mapping rules");
            return rules.size();
            
        } catch (Exception e) {
            System.err.println("❌ Error initializing mapping rules: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Create standard account categories based on South African business practices
     */
    private Map<String, Long> createAccountCategories(Long companyId) throws SQLException {
        Map<String, Long> categoryIds = new HashMap<>();
        
        // Asset categories
        categoryIds.put("CURRENT_ASSETS", createCategory(companyId, "Current Assets", 
            "Cash, bank accounts, and assets convertible to cash within one year", 1)); // Asset type
        categoryIds.put("NON_CURRENT_ASSETS", createCategory(companyId, "Non-Current Assets", 
            "Property, plant, equipment and long-term investments", 1));
        
        // Liability categories
        categoryIds.put("CURRENT_LIABILITIES", createCategory(companyId, "Current Liabilities", 
            "Debts and obligations due within one year", 2)); // Liability type
        categoryIds.put("NON_CURRENT_LIABILITIES", createCategory(companyId, "Non-Current Liabilities", 
            "Long-term debts and obligations", 2));
        
        // Equity categories
        categoryIds.put("EQUITY", createCategory(companyId, "Owner's Equity", 
            "Owner's claim on business assets", 3)); // Equity type
        
        // Revenue categories
        categoryIds.put("OPERATING_REVENUE", createCategory(companyId, "Operating Revenue", 
            "Income from primary business activities", 4)); // Revenue type
        categoryIds.put("OTHER_INCOME", createCategory(companyId, "Other Income", 
            "Non-operating income and gains", 4));
        
        // Expense categories
        categoryIds.put("OPERATING_EXPENSES", createCategory(companyId, "Operating Expenses", 
            "Costs of running the business", 5)); // Expense type
        categoryIds.put("ADMINISTRATIVE_EXPENSES", createCategory(companyId, "Administrative Expenses", 
            "General administration and overhead costs", 5));
        categoryIds.put("FINANCE_COSTS", createCategory(companyId, "Finance Costs", 
            "Interest, bank charges and financial expenses", 5));
        
        System.out.println("✅ Created " + categoryIds.size() + " account categories");
        return categoryIds;
    }
    
    /**
     * Create a single account category
     */
    private Long createCategory(Long companyId, String name, String description, int accountTypeId) throws SQLException {
        // First check if category already exists
        String selectSql = "SELECT id FROM account_categories WHERE company_id = ? AND name = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
            
            selectStmt.setLong(1, companyId);
            selectStmt.setString(2, name);
            
            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    Long existingId = rs.getLong("id");
                    System.out.println("📋 Category already exists: " + name + " (ID: " + existingId + ")");
                    return existingId;
                }
            }
            
            // Category doesn't exist, create it
            String insertSql = "INSERT INTO account_categories (name, description, account_type_id, company_id) " +
                        "VALUES (?, ?, ?, ?) RETURNING id";
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, name);
                insertStmt.setString(2, description);
                insertStmt.setInt(3, accountTypeId);
                insertStmt.setLong(4, companyId);
                
                try (ResultSet rs = insertStmt.executeQuery()) {
                    if (rs.next()) {
                        Long id = rs.getLong("id");
                        System.out.println("📋 Created new category: " + name + " (ID: " + id + ")");
                        return id;
                    }
                }
            }
            
            throw new SQLException("Failed to create category: " + name);
        }
    }
    
    /**
     * Create standard accounts for a South African business
     */
    private void createStandardAccounts(Long companyId, Map<String, Long> categoryIds) throws SQLException {
        List<AccountDefinition> accounts = getStandardAccountDefinitions(categoryIds);
        
        // Check which accounts already exist
        String checkSql = "SELECT account_code FROM accounts WHERE company_id = ?";
        Set<String> existingAccounts = new HashSet<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            
            checkStmt.setLong(1, companyId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                while (rs.next()) {
                    existingAccounts.add(rs.getString("account_code"));
                }
            }
        }
        
        // Filter out existing accounts
        List<AccountDefinition> newAccounts = accounts.stream()
            .filter(account -> !existingAccounts.contains(account.code))
            .collect(java.util.stream.Collectors.toList());
        
        if (newAccounts.isEmpty()) {
            System.out.println("✅ All standard accounts already exist (" + accounts.size() + " accounts)");
            return;
        }
        
        String sql = "INSERT INTO accounts (account_code, account_name, description, category_id, company_id) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (AccountDefinition account : newAccounts) {
                stmt.setString(1, account.code);
                stmt.setString(2, account.name);
                stmt.setString(3, account.description);
                stmt.setLong(4, account.categoryId);
                stmt.setLong(5, companyId);
                stmt.addBatch();
            }
            
            int[] results = stmt.executeBatch();
            System.out.println("✅ Created " + results.length + " new standard accounts");
            System.out.println("ℹ️ Skipped " + existingAccounts.size() + " existing accounts");
        }
    }
    
    /**
     * Get standard account definitions for South African business
     */
    private List<AccountDefinition> getStandardAccountDefinitions(Map<String, Long> categoryIds) {
        List<AccountDefinition> accounts = new ArrayList<>();
        
        // CURRENT ASSETS (1000-1999)
        Long currentAssetsId = categoryIds.get("CURRENT_ASSETS");
        accounts.add(new AccountDefinition("1000", "Petty Cash", "Cash on hand for small expenses", currentAssetsId));
        // Sub-accounts for loan receivables
        accounts.add(new AccountDefinition("1000-001", "Loan Receivable - Tau", "Loan receivable from Tau", currentAssetsId));
        accounts.add(new AccountDefinition("1000-002", "Loan Receivable - Maposa", "Loan receivable from Maposa", currentAssetsId));
        accounts.add(new AccountDefinition("1000-003", "Loan Receivable - Other", "Other loan receivables", currentAssetsId));
        
        accounts.add(new AccountDefinition("1100", "Bank - Current Account", "Primary business current account", currentAssetsId));
        // Sub-account for bank transfers
        accounts.add(new AccountDefinition("1100-001", "Bank Transfers", "Internal bank transfers", currentAssetsId));
        
        accounts.add(new AccountDefinition("1101", "Bank - Savings Account", "Business savings account", currentAssetsId));
        accounts.add(new AccountDefinition("1102", "Bank - Foreign Currency", "Foreign currency accounts", currentAssetsId));
        accounts.add(new AccountDefinition("1200", "Accounts Receivable", "Money owed by customers", currentAssetsId));
        accounts.add(new AccountDefinition("1300", "Inventory", "Stock and inventory items", currentAssetsId));
        accounts.add(new AccountDefinition("1400", "Prepaid Expenses", "Expenses paid in advance", currentAssetsId));
        accounts.add(new AccountDefinition("1500", "VAT Input", "VAT paid on purchases", currentAssetsId));
        
        // NON-CURRENT ASSETS (2000-2999)
        Long nonCurrentAssetsId = categoryIds.get("NON_CURRENT_ASSETS");
        accounts.add(new AccountDefinition("2000", "Property, Plant & Equipment", "Fixed assets at cost", nonCurrentAssetsId));
        // Sub-account for director loans
        accounts.add(new AccountDefinition("2000-001", "Director Loan - Company Assist", "Loan from director for company assistance", nonCurrentAssetsId));
        
        accounts.add(new AccountDefinition("2100", "Accumulated Depreciation", "Depreciation of fixed assets", nonCurrentAssetsId));
        accounts.add(new AccountDefinition("2200", "Investments", "Long-term investments", nonCurrentAssetsId));
        
        // CURRENT LIABILITIES (3000-3999)
        Long currentLiabilitiesId = categoryIds.get("CURRENT_LIABILITIES");
        accounts.add(new AccountDefinition("3000", "Accounts Payable", "Money owed to suppliers", currentLiabilitiesId));
        accounts.add(new AccountDefinition("3100", "VAT Output", "VAT collected on sales", currentLiabilitiesId));
        accounts.add(new AccountDefinition("3200", "PAYE Payable", "Pay-As-You-Earn tax payable", currentLiabilitiesId));
        accounts.add(new AccountDefinition("3300", "UIF Payable", "Unemployment Insurance Fund payable", currentLiabilitiesId));
        accounts.add(new AccountDefinition("3400", "SDL Payable", "Skills Development Levy payable", currentLiabilitiesId));
        accounts.add(new AccountDefinition("3500", "Accrued Expenses", "Expenses incurred but not yet paid", currentLiabilitiesId));
        
        // NON-CURRENT LIABILITIES (4000-4999)
        Long nonCurrentLiabilitiesId = categoryIds.get("NON_CURRENT_LIABILITIES");
        accounts.add(new AccountDefinition("4000", "Long-term Loans", "Long-term debt obligations", nonCurrentLiabilitiesId));
        
        // EQUITY (5000-5999)
        Long equityId = categoryIds.get("EQUITY");
        accounts.add(new AccountDefinition("5000", "Share Capital", "Issued share capital", equityId));
        accounts.add(new AccountDefinition("5100", "Retained Earnings", "Accumulated profits", equityId));
        accounts.add(new AccountDefinition("5200", "Current Year Earnings", "Current year profit/loss", equityId));
        accounts.add(new AccountDefinition("5300", "Opening Balance Equity", "Temporary equity account for opening balances - Cash Flow Statement only", equityId));
        
        // OPERATING REVENUE (6000-6999)
        Long operatingRevenueId = categoryIds.get("OPERATING_REVENUE");
        accounts.add(new AccountDefinition("6000", "Sales Revenue", "Revenue from sales", operatingRevenueId));
        accounts.add(new AccountDefinition("6100", "Service Revenue", "Revenue from services", operatingRevenueId));
        // Sub-account for specific service revenue
        accounts.add(new AccountDefinition("6100-001", "Corobrik Service Revenue", "Service revenue from Corobrik", operatingRevenueId));
        
        accounts.add(new AccountDefinition("6200", "Other Operating Revenue", "Other operating income", operatingRevenueId));
        
        // OTHER INCOME (7000-7999)
        Long otherIncomeId = categoryIds.get("OTHER_INCOME");
        accounts.add(new AccountDefinition("7000", "Interest Income", "Interest earned on investments", otherIncomeId));
        accounts.add(new AccountDefinition("7100", "Dividend Income", "Dividends received", otherIncomeId));
        accounts.add(new AccountDefinition("7200", "Gain on Asset Disposal", "Profit from asset sales", otherIncomeId));
        
        // OPERATING EXPENSES (8000-8999)
        Long operatingExpensesId = categoryIds.get("OPERATING_EXPENSES");
        accounts.add(new AccountDefinition("8000", "Cost of Goods Sold", "Direct costs of products sold", operatingExpensesId));
        accounts.add(new AccountDefinition("8100", "Employee Costs", "Salaries, wages and benefits", operatingExpensesId));
        accounts.add(new AccountDefinition("8100-001", "Director Remuneration", "Director remuneration", operatingExpensesId));
        accounts.add(new AccountDefinition("8200", "Rent Expense", "Office and facility rent", operatingExpensesId));
        accounts.add(new AccountDefinition("8300", "Utilities", "Electricity, water, gas", operatingExpensesId));
        accounts.add(new AccountDefinition("8400", "Communication", "Telephone, internet, postage", operatingExpensesId));
        
        accounts.add(new AccountDefinition("8500", "Motor Vehicle Expenses", "Vehicle running costs", operatingExpensesId));
        // Sub-accounts for vehicle tracking services
        accounts.add(new AccountDefinition("8500-001", "Cartrack Vehicle Tracking", "Cartrack tracking service fees", operatingExpensesId));
        accounts.add(new AccountDefinition("8500-002", "Netstar Vehicle Tracking", "Netstar tracking service fees", operatingExpensesId));
        
        accounts.add(new AccountDefinition("8600", "Travel & Entertainment", "Business travel and entertainment", operatingExpensesId));
        // Sub-accounts for fuel expenses by station
        accounts.add(new AccountDefinition("8600-001", "Fuel Expenses - BP Stations", "Fuel purchases at BP", operatingExpensesId));
        accounts.add(new AccountDefinition("8600-002", "Fuel Expenses - Shell Stations", "Fuel purchases at Shell", operatingExpensesId));
        accounts.add(new AccountDefinition("8600-003", "Fuel Expenses - Sasol Stations", "Fuel purchases at Sasol", operatingExpensesId));
        accounts.add(new AccountDefinition("8600-004", "Engen Fuel Expenses", "Fuel purchases at Engen", operatingExpensesId));
        accounts.add(new AccountDefinition("8600-099", "Fuel Expenses - Other Stations", "Fuel purchases at other stations", operatingExpensesId));
        
        accounts.add(new AccountDefinition("8700", "Professional Services", "Legal, accounting, consulting", operatingExpensesId));
        accounts.add(new AccountDefinition("8710", "Suppliers Expense", "Payments to suppliers and vendors", operatingExpensesId));
        accounts.add(new AccountDefinition("8720", "HR Management Expense", "Human resources management and recruitment", operatingExpensesId));
        accounts.add(new AccountDefinition("8730", "Education & Training", "Education fees and training costs", operatingExpensesId));
        
        accounts.add(new AccountDefinition("8800", "Insurance", "Business insurance premiums", operatingExpensesId));
        // Sub-accounts for insurance providers
        accounts.add(new AccountDefinition("8800-001", "King Price Insurance Premiums", "King Price insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-002", "DOTSURE Insurance Premiums", "DOTSURE insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-003", "OUTSurance Insurance Premiums", "OUTSurance insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-004", "MIWAY Insurance Premiums", "MIWAY insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-005", "Liberty Insurance Premiums", "Liberty insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-006", "Badger Insurance Premiums", "Badger insurance premiums", operatingExpensesId));
        accounts.add(new AccountDefinition("8800-999", "Other Insurance Premiums", "Other insurance providers", operatingExpensesId));
        
        accounts.add(new AccountDefinition("8900", "Repairs & Maintenance", "Equipment and facility maintenance", operatingExpensesId));
        
        // ADMINISTRATIVE EXPENSES (9000-9999)
        Long adminExpensesId = categoryIds.get("ADMINISTRATIVE_EXPENSES");
        accounts.add(new AccountDefinition("9000", "Office Supplies", "Stationery and office materials", adminExpensesId));
        accounts.add(new AccountDefinition("9100", "Computer Expenses", "Software licenses and IT costs", adminExpensesId));
        accounts.add(new AccountDefinition("9200", "Marketing & Advertising", "Promotional and marketing costs", adminExpensesId));
        accounts.add(new AccountDefinition("9300", "Training & Development", "Staff training and development", adminExpensesId));
        accounts.add(new AccountDefinition("9400", "Depreciation", "Depreciation of fixed assets", adminExpensesId));
        
        // FINANCE COSTS (9500-9999)
        Long financeCostsId = categoryIds.get("FINANCE_COSTS");
        accounts.add(new AccountDefinition("9500", "Interest Expense", "Interest on loans and credit", financeCostsId));
        accounts.add(new AccountDefinition("9600", "Bank Charges", "Bank fees and transaction costs", financeCostsId));
        accounts.add(new AccountDefinition("9700", "Foreign Exchange Loss", "Loss on currency exchange", financeCostsId));
        accounts.add(new AccountDefinition("9800", "VAT Payments to SARS", "VAT payments made to South African Revenue Service", financeCostsId));
        accounts.add(new AccountDefinition("9810", "Loan Repayments", "Loan repayment costs", financeCostsId));
        accounts.add(new AccountDefinition("9820", "PAYE Expense", "PAYE tax payments to South African Revenue Service", financeCostsId));
        accounts.add(new AccountDefinition("9900", "Pension Expenses", "Pension-related costs", financeCostsId));
        
        return accounts;
    }
    
    /**
     * Analyze existing transaction patterns and suggest account mappings
     */
    private void analyzeTransactionPatterns(Long companyId) throws SQLException {
        System.out.println("\n🔍 Analyzing transaction patterns for account mapping...");
        
        // Query to analyze patterns without using PostgreSQL-specific functions
        String sql = """
            SELECT 
                CASE 
                    WHEN details LIKE '%FEE%' OR details LIKE '%CHARGE%' THEN 'Bank Charges'
                    WHEN details LIKE '%TRANSFER FROM%' OR details LIKE '%DEPOSIT%' THEN 'Deposits/Transfers In'
                    WHEN details LIKE '%TRANSFER TO%' OR details LIKE '%PAYMENT TO%' THEN 'Payments/Transfers Out'
                    WHEN details LIKE '%SALARY%' OR details LIKE '%WAGE%' THEN 'Employee Costs'
                    WHEN details LIKE '%INTEREST%' THEN 'Interest'
                    WHEN details LIKE '%RENT%' THEN 'Rent'
                    WHEN details LIKE '%INSURANCE%' THEN 'Insurance'
                    WHEN details LIKE '%FUEL%' OR details LIKE '%PETROL%' THEN 'Vehicle Expenses'
                    WHEN details LIKE '%ELECTRIC%' OR details LIKE '%WATER%' OR details LIKE '%MUNICIPAL%' THEN 'Utilities'
                    WHEN details LIKE '%TELEPHONE%' OR details LIKE '%MOBILE%' OR details LIKE '%INTERNET%' THEN 'Communication'
                    ELSE 'Unclassified'
                END as suggested_category,
                COUNT(*) as transaction_count,
                SUM(COALESCE(credit_amount, 0)) as total_credits,
                SUM(COALESCE(debit_amount, 0)) as total_debits
            FROM bank_transactions 
            WHERE details IS NOT NULL
            GROUP BY 1
            ORDER BY transaction_count DESC
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            System.out.println("\n📊 Transaction Pattern Analysis:");
            System.out.println("=" .repeat(80));
            System.out.printf("%-25s %10s %15s %15s%n", "Category", "Count", "Credits", "Debits");
            System.out.println("-".repeat(80));
            
            while (rs.next()) {
                String category = rs.getString("suggested_category");
                int count = rs.getInt("transaction_count");
                BigDecimal credits = rs.getBigDecimal("total_credits");
                BigDecimal debits = rs.getBigDecimal("total_debits");
                
                System.out.printf("%-25s %10d %15s %15s%n", 
                    category, count, 
                    formatAmount(credits), 
                    formatAmount(debits));
            }
            System.out.println("=" .repeat(80));
            
            // Also get sample transactions for each category
            System.out.println("\n📝 Sample Transactions:");
            System.out.println("-".repeat(80));
            
            String sampleSql = """
                SELECT 
                    CASE 
                        WHEN details LIKE '%FEE%' OR details LIKE '%CHARGE%' THEN 'Bank Charges'
                        WHEN details LIKE '%TRANSFER FROM%' OR details LIKE '%DEPOSIT%' THEN 'Deposits/Transfers In'
                        WHEN details LIKE '%TRANSFER TO%' OR details LIKE '%PAYMENT TO%' THEN 'Payments/Transfers Out'
                        WHEN details LIKE '%SALARY%' OR details LIKE '%WAGE%' THEN 'Employee Costs'
                        WHEN details LIKE '%INTEREST%' THEN 'Interest'
                        WHEN details LIKE '%RENT%' THEN 'Rent'
                        WHEN details LIKE '%INSURANCE%' THEN 'Insurance'
                        WHEN details LIKE '%FUEL%' OR details LIKE '%PETROL%' THEN 'Vehicle Expenses'
                        WHEN details LIKE '%ELECTRIC%' OR details LIKE '%WATER%' OR details LIKE '%MUNICIPAL%' THEN 'Utilities'
                        WHEN details LIKE '%TELEPHONE%' OR details LIKE '%MOBILE%' OR details LIKE '%INTERNET%' THEN 'Communication'
                        ELSE 'Unclassified'
                    END as category,
                    details
                FROM bank_transactions
                WHERE details IS NOT NULL
                ORDER BY category, details
                LIMIT 10
                """;
            
            try (PreparedStatement sampleStmt = conn.prepareStatement(sampleSql);
                 ResultSet sampleRs = sampleStmt.executeQuery()) {
                
                String currentCategory = "";
                int sampleCount = 0;
                
                while (sampleRs.next()) {
                    String category = sampleRs.getString("category");
                    String details = sampleRs.getString("details");
                    
                    if (!category.equals(currentCategory)) {
                        if (!currentCategory.isEmpty()) {
                            System.out.println();
                        }
                        currentCategory = category;
                        System.out.println(category + ":");
                        sampleCount = 0;
                    }
                    
                    if (sampleCount < 3) { // Limit to 3 samples per category
                        System.out.println("  - " + details);
                        sampleCount++;
                    }
                }
            }
        }
    }
    
    /**
     * Get account classification suggestions based on transaction analysis
     */
    public Map<String, String> getAccountMappingSuggestions(Long companyId) {
        Map<String, String> suggestions = new HashMap<>();
        
        // Based on our transaction analysis, suggest account mappings
        suggestions.put("Bank Charges", "9600 - Bank Charges");
        suggestions.put("Employee Costs", "8100 - Employee Costs");
        suggestions.put("Deposits/Transfers In", "1100 - Bank - Current Account");
        suggestions.put("Payments/Transfers Out", "Multiple (depends on purpose)");
        suggestions.put("Interest", "7000 - Interest Income / 9500 - Interest Expense");
        suggestions.put("Rent", "8200 - Rent Expense");
        suggestions.put("Insurance", "8800 - Insurance");
        suggestions.put("Vehicle Expenses", "8500 - Motor Vehicle Expenses");
        suggestions.put("Utilities", "8300 - Utilities");
        suggestions.put("Communication", "8400 - Communication");
        
        return suggestions;
    }
    
    /**
     * Generate account classification report
     */
    public void generateClassificationReport(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            System.out.println("\n📈 ACCOUNT CLASSIFICATION REPORT");
            System.out.println("Company: " + company.getName());
            System.out.println("Generated: " + java.time.LocalDateTime.now());
            System.out.println("=" .repeat(60));
            
            // Show account categories and counts
            String sql = """
                SELECT 
                    ac.name as category_name,
                    at.name as account_type,
                    COUNT(a.id) as account_count
                FROM account_categories ac
                JOIN account_types at ON ac.account_type_id = at.id
                LEFT JOIN accounts a ON ac.id = a.category_id
                WHERE ac.company_id = ?
                GROUP BY ac.name, at.name, at.id
                ORDER BY at.id, ac.name
                """;
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                ResultSet rs = stmt.executeQuery();
                
                String currentType = "";
                while (rs.next()) {
                    String accountType = rs.getString("account_type");
                    String categoryName = rs.getString("category_name");
                    int accountCount = rs.getInt("account_count");
                    
                    if (!accountType.equals(currentType)) {
                        currentType = accountType;
                        System.out.println("\n" + accountType.toUpperCase() + ":");
                    }
                    
                    System.out.printf("  %-30s %3d accounts%n", categoryName, accountCount);
                }
            }
            
            // Show mapping suggestions
            System.out.println("\n🎯 RECOMMENDED ACCOUNT MAPPINGS:");
            System.out.println("-".repeat(60));
            Map<String, String> suggestions = getAccountMappingSuggestions(companyId);
            suggestions.forEach((pattern, account) -> 
                System.out.printf("%-25s → %s%n", pattern, account));
            
        } catch (Exception e) {
            System.err.println("Error generating classification report: " + e.getMessage());
        }
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null || amount.equals(BigDecimal.ZERO)) {
            return "R0.00";
        }
        return String.format("R%,.2f", amount);
    }
    
    /**
     * Helper method to create a transaction mapping rule template.
     * These are lightweight rule definitions without company/account object references.
     * The account code is embedded in the description for later resolution when persisting.
     * 
     * @param ruleName Name of the rule
     * @param description Human-readable description
     * @param matchType Type of pattern matching
     * @param matchValue Pattern to match against transaction details
     * @param accountCode SARS-compliant account code (e.g., "8100", "1100")
     * @param priority Rule priority (higher = checked first)
     * @return TransactionMappingRule template ready for persistence
     */
    private TransactionMappingRule createRule(String ruleName, String description, 
                                             TransactionMappingRule.MatchType matchType,
                                             String matchValue, String accountCode, int priority) {
        TransactionMappingRule rule = new TransactionMappingRule();
        rule.setRuleName(ruleName);
        // Store account code in description for now (will be extracted when persisting)
        rule.setDescription(description + " [AccountCode:" + accountCode + "]");
        rule.setMatchType(matchType);
        rule.setMatchValue(matchValue);
        rule.setPriority(priority);
        rule.setActive(true);
        return rule;
    }
    
    /**
     * Get standard transaction mapping rules for South African business.
     * 
     * This is the SINGLE SOURCE OF TRUTH for all classification rules.
     * ALL other services should read from this method, not define their own rules.
     * 
     * Rules are ordered by priority (highest first):
     * - Priority 10: Critical/specific patterns (must match first)
     * - Priority 9: High-confidence patterns
     * - Priority 8: Standard business patterns
     * - Priority 5: Generic/fallback patterns
     * 
     * When adding new rules:
     * 1. Add rule definition here FIRST
     * 2. Run "Initialize Mapping Rules" to persist to database
     * 3. Test with "Auto-Classify Transactions"
     * 
     * NOTE: Returns TransactionMappingRule objects WITHOUT company/account references.
     * These are lightweight rule templates that will be enriched when persisted.
     * 
     * @return List of standard mapping rules ordered by priority (descending)
     */
    public List<TransactionMappingRule> getStandardMappingRules() {
        List<TransactionMappingRule> rules = new ArrayList<>();
        
        // ========================================================================
        // PRIORITY 10: CRITICAL PATTERNS (Must match before generic patterns)
        // ========================================================================
        
        // HIGH PRIORITY: Salary payments to specific employees
        // NOTE: "INSURANCE CHAUKE" contains keyword "INSURANCE" but is actually a salary payment
        // This rule MUST come before generic insurance pattern to avoid misclassification
        rules.add(createRule(
            "Insurance Chauke Salaries",
            "Salary payments to Insurance Chauke - prioritize over insurance pattern",
            TransactionMappingRule.MatchType.CONTAINS,
            "INSURANCE CHAUKE",
            "8100", // Employee Costs
            10
        ));
        
        // Loan repayments from directors/employees
        rules.add(createRule(
            "Jeffrey Maphosa Loan Repayment",
            "Loan repayments from Jeffrey Maphosa to company loan assist account",
            TransactionMappingRule.MatchType.CONTAINS,
            "JEFFREY MAPHOSA LOAN",
            "4000", // Long-term Loans
            10
        ));
        
        // Director reimbursements (for personal credit card expenses paid by company)
        rules.add(createRule(
            "Stone Jeffrey Maphosa Reimbursement",
            "Director reimbursements for personal credit card expenses (Company Assist loan)",
            TransactionMappingRule.MatchType.REGEX,
            "STONE JEFFR.*MAPHOSA.*(REIMBURSE|REPAYMENT)",
            "4000", // Long-term Loans
            10
        ));
        
        // ========================================================================
        // PRIORITY 9: HIGH-CONFIDENCE PATTERNS
        // ========================================================================
        
        // COROBRIK customer payments (revenue)
        rules.add(createRule(
            "Corobrik Service Revenue",
            "Credit Transfer ... Corobrik",
            TransactionMappingRule.MatchType.CONTAINS,
            "COROBRIK",
            "6100-001", // Corobrik Service Revenue
            10
        ));
        
        // Returned debits - offset original transactions
        // NOTE: These should go to the SAME account as the original transaction
        // For now, we'll handle specific known providers
        rules.add(createRule(
            "Returned Debit - DOTSURE",
            "Returned debit for DOTSURE insurance payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "RTD-DEBIT AGAINST PAYERS AUTH DOTSURE",
            "8800-002", // DOTSURE Insurance Premiums (offset)
            9
        ));
        
        // EXCESS INTEREST payments
        rules.add(createRule(
            "Excess Interest Expense",
            "Excess interest charges on loans/accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "EXCESS INTEREST",
            "9500", // Interest Expense
            9
        ));
        
        // STD BANK BOND repayments (loans payable)
        rules.add(createRule(
            "STD Bank Bond Repayment",
            "STD Bank bond/home loan repayments",
            TransactionMappingRule.MatchType.CONTAINS,
            "STD BANK BOND",
            "4000", // Long-term Loans (Loans Payable)
            9
        ));
        
        // Fuel transfers FROM specific account
        rules.add(createRule(
            "IB Transfer From Fuel Account",
            "Internal bank transfers from fuel supplier account",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER FROM *****2689327",
            "8600-099", // Fuel Expenses - Other Stations
            9
        ));
        
        // CARTRACK vehicle tracking
        rules.add(createRule(
            "Cartrack Vehicle Tracking",
            "Cartrack vehicle tracking service fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "CARTRACK",
            "8500-001", // Cartrack Vehicle Tracking
            9
        ));
        
        // IB INSTANT MONEY CASH TO - e-wallet payments to part-time employees
        rules.add(createRule(
            "IB Instant Money Cash to Employees",
            "E-wallet payments directly to part-time employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB INSTANT MONEY CASH TO",
            "8100", // Employee Costs
            9
        ));
        
        // AUTOBANK TRANSFER TO ACCOUNT - fuel expenses
        rules.add(createRule(
            "Autobank Transfer to Fuel Account",
            "Automated bank transfers to fuel supplier accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK TRANSFER TO ACCOUNT",
            "8600-099", // Fuel Expenses - Other Stations
            9
        ));
        
        // IMMEDIATE PAYMENT - specific employee payments (high priority)
        rules.add(createRule(
            "Immediate Payment - Jeffrey S Maphosa",
            "Director remuneration payment to Jeffrey S Maphosa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224812909 JEFFREY S MAPHOSA",
            "8100-001", // Director Remuneration
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Katleho Mogaloa",
            "Employee payment to Katleho Mogaloa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 226159243 KATLEHO MOGALOA",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Jordan Moyane",
            "Employee payment to Jordan Moyane",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224812154 JORDAN MOYANE",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Sibongile Dlamini",
            "Employee payment to Sibongile Dlamini",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224850901 SIBONGILE DLAMINI",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Mabunda IP",
            "Employee payment to Mabunda IP",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224855113 MABUNDA IP",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Albert Zunga",
            "Employee payment to Albert Zunga",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 221599858 ALBERT ZUNGA",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Piet Mathebula",
            "Employee payment to Piet Mathebula",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 221514559 PIET MATHEBULA",
            "8100", // Employee Costs
            10
        ));
        
        rules.add(createRule(
            "Immediate Payment - Alberake Protection",
            "Cost of goods sold - Alberake Protection",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 218989372 ALBERAKE PROTECTION",
            "8000", // Cost of Goods Sold
            10
        ));
        
        // FEE IMMEDIATE PAYMENT - bank charges (high priority)
        rules.add(createRule(
            "Fee Immediate Payment",
            "Bank charges for immediate payment processing",
            TransactionMappingRule.MatchType.CONTAINS,
            "FEE IMMEDIATE PAYMENT",
            "9600", // Bank Charges
            10
        ));
        
        // IMMEDIATE PAYMENT - generic employee payments (lower priority)
        rules.add(createRule(
            "Immediate Payment - Generic Employee",
            "Generic immediate payment to employees (name and surname pattern)",
            TransactionMappingRule.MatchType.REGEX,
            "IMMEDIATE PAYMENT \\d+ [A-Z]+ [A-Z]+",
            "8100", // Employee Costs
            8
        ));
        
        // Additional employee payment patterns
        rules.add(createRule(
            "Employee Payment - Sibongile Dlamini",
            "Employee payment to Sibongile Dlamini",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 167855420 SIBONGILE DLAMINI",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Themba Mkhatshwa",
            "Employee payment to Themba Mkhatshwa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207008710 THEMBA MKHATSHWA",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Lawrence Phogole",
            "Employee payment to Lawrence Phogole",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207008307 LAWRENCE PHOGOLE",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Tlometsane Moraswi",
            "Employee payment to Tlometsane Moraswi",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207009081 TLOMETSANE MORASWI",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Mbhoni Miyambo",
            "Employee payment to Mbhoni Miyambo",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 204543849 MBHONI MIYAMBO",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Musa Nzunza",
            "Employee payment to Musa Nzunza",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 156658792 MUSA NZUNZA",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Masemola Matawaneng",
            "Employee payment to Masemola Matawaneng",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207009987 MASEMOLA MATAWANENG",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Employee Payment - Winners Chauke",
            "Employee payment to Winners Chauke",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207010671 WINNERS CHAUKE",
            "8100", // Employee Costs
            9
        ));
        
        // Director payments
        rules.add(createRule(
            "Director Payment - DB Nkuna",
            "Director remuneration payment to DB Nkuna",
            TransactionMappingRule.MatchType.CONTAINS,
            "DB NKUNA",
            "8100-001", // Director Remuneration
            9
        ));
        
        // Pension contributions
        rules.add(createRule(
            "Pension Fund Contributions",
            "Pension fund contributions for employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "PENSION FUND CONTRIBUTION",
            "9900", // Pension Expenses
            9
        ));
        
        rules.add(createRule(
            "Pension Fund Transfers",
            "Pension fund transfers and contributions",
            TransactionMappingRule.MatchType.CONTAINS,
            "DEBIT TRANSFER FAW",
            "9900", // Pension Expenses
            9
        ));
        
        // Training expenses
        rules.add(createRule(
            "OHS Training Expenses",
            "Occupational Health and Safety training expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "OHS TRAINING",
            "8730", // Education & Training
            9
        ));
        
        // Petrol allowance
        rules.add(createRule(
            "Petrol Allowance",
            "Petrol allowance payments to employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "PETROL ALOWANCE",
            "8500", // Motor Vehicle Expenses
            9
        ));
        
        // PAYE payments to SARS
        rules.add(createRule(
            "PAYE Payments to SARS",
            "PAYE tax payments to South African Revenue Service",
            TransactionMappingRule.MatchType.CONTAINS,
            "PAYE-PAY-AS-",
            "9820", // PAYE Expense (tax expense)
            9
        ));
        
        // Cash withdrawals
        rules.add(createRule(
            "Cash Withdrawals",
            "ATM cash withdrawals",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK CASH WITHDRAWAL",
            "8100", // Employee Costs
            9
        ));
        
        // Director reimbursements
        rules.add(createRule(
            "Director Reimbursements",
            "Director expense reimbursements",
            TransactionMappingRule.MatchType.CONTAINS,
            "REIMBURSE",
            "4000", // Long-term Loans (Director loans)
            9
        ));
        
        // Stokvela payments
        rules.add(createRule(
            "Stokvela Payments",
            "Stokvela (savings club) payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "STOKVELA",
            "1000", // Petty Cash/Loans Receivable (asset accounts)
            8
        ));
        
        // Transport expenses
        rules.add(createRule(
            "Transport Expenses",
            "Transport and related expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "TRANSPORT",
            "8500", // Motor Vehicle Expenses
            9
        ));
        
        // Telephone expenses
        rules.add(createRule(
            "Telephone Expenses",
            "Telephone and communication expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "TELEPHONE",
            "8400", // Communication
            9
        ));
        
        // Vehicle tracking
        rules.add(createRule(
            "Netstar Vehicle Tracking",
            "Netstar vehicle tracking services",
            TransactionMappingRule.MatchType.CONTAINS,
            "NETSTAR",
            "8500-002", // Netstar Vehicle Tracking
            9
        ));
        
        // Loan income
        rules.add(createRule(
            "Loan Income",
            "Loans received from directors/associates",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT FROM",
            "2000-001", // Director Loan - Company Assist
            9
        ));
        
        // Company transfers - specific patterns based on trailing descriptions
        rules.add(createRule(
            "Company Transfers - Company Assist",
            "Inter-company transfers for company assistance (loans)",
            TransactionMappingRule.MatchType.REGEX,
            "CREDIT TRANSFER.*COMPANY ASSIST",
            "4000", // Long-term Loans
            9
        ));
        
        rules.add(createRule(
            "Company Transfers - TAU",
            "Inter-company transfers for TAU (stokvela contributions)",
            TransactionMappingRule.MatchType.REGEX,
            "CREDIT TRANSFER.*TAU",
            "1000-001", // Stokvela Contributions
            9
        ));
        
        // Returned debits (offset transactions)
        rules.add(createRule(
            "Returned Debits",
            "Returned debit orders (offset original transactions)",
            TransactionMappingRule.MatchType.REGEX,
            "RTD-.*",
            "8800", // Insurance (generic offset)
            8
        ));
        
        // BALANCE BROUGHT FORWARD - opening balance entries
        rules.add(createRule(
            "Balance Brought Forward",
            "Opening balance entries from previous periods",
            TransactionMappingRule.MatchType.CONTAINS,
            "BALANCE BROUGHT FORWARD",
            "5000", // Retained Earnings (opening balances)
            9
        ));
        
        // COROBRIK customer payments (revenue)
        rules.add(createRule(
            "Corobrik Revenue",
            "Customer payments from Corobrik",
            TransactionMappingRule.MatchType.CONTAINS,
            "CREDIT TRANSFER.*COROBRIK",
            "6100-001", // Corobrik Service Revenue
            10
        ));
        
        // IB TRANSFER FROM fuel account (offsetting fuel expenses)
        rules.add(createRule(
            "IB Transfer From Fuel Account",
            "Internal bank transfers from fuel supplier account",
            TransactionMappingRule.MatchType.REGEX,
            "IB TRANSFER FROM \\*\\*\\*\\*\\*2689327.*",
            "8600-099", // Fuel Expenses - Other Stations
            8
        ));
        
        // Returned debits - extract provider and map to same account (offset logic)
        rules.add(createRule(
            "Returned Debit - DOTSURE Offset",
            "Returned debit orders for DOTSURE insurance (offset in same account)",
            TransactionMappingRule.MatchType.REGEX,
            "RTD-DEBIT AGAINST PAYERS AUTH DOTSURE.*",
            "8800-002", // DOTSURE Insurance Premiums (offset)
            8
        ));
        
        rules.add(createRule(
            "Returned Debit - Generic Provider Offset",
            "Returned debit orders for other providers (extract provider and offset)",
            TransactionMappingRule.MatchType.REGEX,
            "RTD-DEBIT AGAINST PAYERS AUTH ([A-Z]+).*",
            "8800", // Insurance (generic - will be refined by provider extraction)
            7
        ));
        
        rules.add(createRule(
            "Returned Debit - RTD NOT PROVIDED Offset",
            "Returned debit orders with RTD-NOT PROVIDED pattern",
            TransactionMappingRule.MatchType.REGEX,
            "RTD-NOT PROVIDED FOR ([A-Z]+).*",
            "8800", // Insurance (generic - will be refined by provider extraction)
            7
        ));
        
        // SARS VAT payments (CRITICAL: Must be expense, not liability adjustment)
        rules.add(createRule(
            "SARS VAT Payments",
            "VAT payments made to South African Revenue Service",
            TransactionMappingRule.MatchType.CONTAINS,
            "PAYMENT TO SARS-VAT",
            "9800", // VAT Payments to SARS (Expense)
            9
        ));
        
        // Professional services (accounting, legal)
        rules.add(createRule(
            "Global Hope Financia Accounting",
            "Accounting services from Global Hope Financia",
            TransactionMappingRule.MatchType.CONTAINS,
            "GLOBAL HOPE FINACIA",
            "8700", // Professional Services
            9
        ));
        
        // Fuel purchases (specific account identifier)
        rules.add(createRule(
            "Fuel Purchase - Account 2689327",
            "Fuel purchases to supplier account 2689327 [AccountCode:8600-099]",
            TransactionMappingRule.MatchType.CONTAINS,
            "2689327",
            "8600-099", // Fuel Expenses - Other Stations
            9
        ));
        
        // ========================================================================
        // PRIORITY 9: SPECIFIC INDIVIDUAL PAYMENTS (HIGH PRIORITY)
        // ========================================================================
        
        // IB PAYMENT TO specific individuals (likely director/employee payments)
        rules.add(createRule(
            "IB Payment to EUPHODIA N TAU XINGHIZANA",
            "IB payment to EUPHODIA N TAU XINGHIZANA (director/employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT TO EUPHODIA N TAU XINGHIZANA",
            "1000-001", // stokvela Contributions
            9
        ));
        
        rules.add(createRule(
            "IB Payment to NGWAKWANE E TAU XINGHIZANA",
            "IB payment to NGWAKWANE E TAU XINGHIZANA (director/employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT TO NGWAKWANE E TAU XINGHIZANA",
            "1000-001", // Stokvela Contributions
            9
        ));
        
        rules.add(createRule(
            "IB Payment to EUPHODIA TAU STOKFELA",
            "IB payment to EUPHODIA TAU STOKFELA (stokvela payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT TO EUPHODIA TAU STOKFELA",
            "1000-001", // Cash and Cash Equivalents (Stokvela)
            9
        ));
        
        // IMMEDIATE PAYMENT to specific individuals
        rules.add(createRule(
            "Immediate Payment to JEFFREY MAPHOSA",
            "Immediate payment to JEFFREY MAPHOSA (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO JEFFREY MAPHOSA",
            "8100-001", // Director Remuneration
            9
        ));
        
        rules.add(createRule(
            "Immediate Payment to NGWAKWANE E TAU",
            "Immediate payment to NGWAKWANE E TAU (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO NGWAKWANE E TAU",
            "1000-001", // Stokvela Contributions
            9
        ));
        
        rules.add(createRule(
            "Immediate Payment to DAVID MOLEFE",
            "Immediate payment to DAVID MOLEFE (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO DAVID MOLEFE",
            "8100", // Employee Costs
            9
        ));
        
        rules.add(createRule(
            "Immediate Payment to MUZIKAYISE ZUNGA",
            "Immediate payment to MUZIKAYISE ZUNGA (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO MUZIKAYISE ZUNGA",
            "8100", // Employee Costs
            9
        ));
        
        // ========================================================================
        // PRIORITY 8: GENERIC PAYMENT PATTERNS
        // ========================================================================
        
        // Generic IB PAYMENT TO pattern (fallback for other individuals)
        rules.add(createRule(
            "IB Payment To - Generic",
            "Generic IB payment to individuals (fallback rule)",
            TransactionMappingRule.MatchType.REGEX,
            "IB PAYMENT TO [A-Z]+ [A-Z]+.*",
            "8100", // Employee Costs (generic fallback)
            8
        ));
        
        // Generic IMMEDIATE PAYMENT pattern
        rules.add(createRule(
            "Immediate Payment - Generic",
            "Generic immediate payment to individuals",
            TransactionMappingRule.MatchType.REGEX,
            "IMMEDIATE PAYMENT [0-9]+ [A-Z]+ [A-Z]+.*",
            "8100", // Employee Costs
            8
        ));
        
        // ========================================================================
        // PRIORITY 7: CASH DEPOSITS AND TRANSFERS
        // ========================================================================
        
        // AUTOBANK CASH DEPOSIT (cash inflows - revenue)
        rules.add(createRule(
            "Autobank Cash Deposit - Generic",
            "Cash deposits through autobank (revenue)",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK CASH DEPOSIT",
            "1000", // Other 
            7
        ));
        
        // Internal bank transfers (between own accounts)
        rules.add(createRule(
            "Bank Transfers - IB TRANSFER TO",
            "Internal bank transfers to other accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER TO",
            "1100-001", // Bank - Current Account
            7
        ));
        
        rules.add(createRule(
            "Bank Transfers - IB TRANSFER FROM",
            "Internal bank transfers from other accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER FROM",
            "1100-001", // Bank - Current Account
            7
        ));
        
        // Rent payments
        rules.add(createRule(
            "Ellis Park Stadium Rent",
            "Rent payments to Ellis Park Stadium",
            TransactionMappingRule.MatchType.CONTAINS,
            "ELLISPARK STADIUM",
            "8200", // Rent Expense
            9
        ));
        
        // Vehicle purchases (capital expenditure)
        rules.add(createRule(
            "EBS Car Sales Vehicle Purchase",
            "Vehicle purchase from EBS Car Sales Mercedes",
            TransactionMappingRule.MatchType.CONTAINS,
            "EBS CAR SALES",
            "2000", // Property, Plant & Equipment
            9
        ));
        
        // Supplier payments
        rules.add(createRule(
            "Two Way Technologies Supplier",
            "Supplier payments to Two Way Technologies",
            TransactionMappingRule.MatchType.CONTAINS,
            "TWO WAY TECHNOLOGIES",
            "8710", // Suppliers Expense
            9
        ));
        
        rules.add(createRule(
            "Rent A Dog Supplier",
            "Supplier payments to Rent A Dog",
            TransactionMappingRule.MatchType.CONTAINS,
            "RENT A DOG",
            "8710", // Suppliers Expense
            9
        ));
        
        // HR Management expenses
        rules.add(createRule(
            "Neo Entle Labour Hire",
            "HR management and labour hire from Neo Entle Labour Hire",
            TransactionMappingRule.MatchType.CONTAINS,
            "NEO ENTLE LABOUR",
            "8720", // HR Management Expense
            9
        ));
        
        // Investment transactions
        rules.add(createRule(
            "Stanlib Investment",
            "Investment transactions with Stanlib",
            TransactionMappingRule.MatchType.CONTAINS,
            "STANLIB",
            "2200", // Investments
            9
        ));
        
        // Cost of Goods Sold
        rules.add(createRule(
            "DB Projects COGS",
            "Cost of goods sold - DB Projects and Agencies",
            TransactionMappingRule.MatchType.CONTAINS,
            "DB PROJECTS",
            "8000", // Cost of Goods Sold
            9
        ));
        
        // Education/Training expenses
        rules.add(createRule(
            "Lyceum College School Fees",
            "School fees at Lyceum College",
            TransactionMappingRule.MatchType.CONTAINS,
            "LYCEUM COLLEGE",
            "8730", // Education & Training
            9
        ));
        
        // Employee salaries (specific names)
        rules.add(createRule(
            "Anthony Ndou Salary",
            "Salary payment to Anthony Ndou",
            TransactionMappingRule.MatchType.CONTAINS,
            "ANTHONY NDOU",
            "8100", // Employee Costs
            8
        ));
        
        rules.add(createRule(
            "Goodman Zunga Salary",
            "Salary payment to Goodman Zunga",
            TransactionMappingRule.MatchType.CONTAINS,
            "GOODMAN ZUNGA",
            "8100", // Employee Costs
            8
        ));
        
        // Salary payments (generic keywords)
        rules.add(createRule(
            "Salary Payments - XG SALARIES",
            "Standard salary payments with XG SALARIES keyword",
            TransactionMappingRule.MatchType.CONTAINS,
            "XG SALARIES",
            "8100", // Employee Costs
            8
        ));
        
        rules.add(createRule(
            "Salary Payments - SALARIES",
            "Standard salary payments with SALARIES keyword",
            TransactionMappingRule.MatchType.CONTAINS,
            "SALARIES",
            "8100", // Employee Costs
            8
        ));
        
        rules.add(createRule(
            "Salary Payments - WAGES",
            "Wage payments to employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "WAGES",
            "8100", // Employee Costs
            8
        ));
        
        // ========================================================================
        // PRIORITY 5: GENERIC/FALLBACK PATTERNS
        // ========================================================================
        
        // Educational institutions (generic)
        rules.add(createRule(
            "Education Institutions - Generic",
            "Generic rule for college, school, and university fees",
            TransactionMappingRule.MatchType.REGEX,
            ".*(COLLEGE|SCHOOL|UNIVERSITY).*",
            "9300", // Training & Development
            5
        ));
        
        // Insurance premiums (generic - MUST come after "Insurance Chauke" check)
        rules.add(createRule(
            "Insurance Premiums - Generic",
            "Insurance premium payments to providers",
            TransactionMappingRule.MatchType.CONTAINS,
            "INSURANCE",
            "8800", // Insurance
            5
        ));
        
        rules.add(createRule(
            "Insurance Premiums - PREMIUM keyword",
            "Premium payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PREMIUM",
            "8800", // Insurance
            5
        ));
        
        // Bank charges and fees - HIGHEST PRIORITY to override ALL other classifications
        rules.add(createRule(
            "Bank Charges - FEE keyword",
            "Bank fees and charges",
            TransactionMappingRule.MatchType.CONTAINS,
            "FEE",
            "9600", // Bank Charges
            20  // HIGHEST priority - overrides ALL other rules
        ));
        
        rules.add(createRule(
            "Bank Charges - SERVICE FEE",
            "Monthly service fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "SERVICE FEE",
            "9600", // Bank Charges
            20  // HIGHEST priority - overrides ALL other rules
        ));
        
        rules.add(createRule(
            "Bank Charges - CHARGE keyword",
            "Bank charges and fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "CHARGE",
            "9600", // Bank Charges
            20  // HIGHEST priority - overrides ALL other rules
        ));
        
        // Loan payments (generic)
        rules.add(createRule(
            "Loan Payments - Generic",
            "Loan payments and repayments",
            TransactionMappingRule.MatchType.CONTAINS,
            "LOAN",
            "4000", // Long-term Loans
            5
        ));
        
        // ========================================================================
        // PRIORITY 6: REMAINING UNCLASSIFIED PATTERNS
        // ========================================================================
        
        // MAGTAPE CREDIT COMPANY ASSIST (loan/credit payments)
        rules.add(createRule(
            "MAGTAPE CREDIT COMPANY ASSIST",
            "Credit company assistance payments (loans)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT COMPANY ASSIST",
            "4000", // Long-term Loans
            6
        ));
        
        // Generic IB PAYMENT TO (fallback for unspecified recipients)
        rules.add(createRule(
            "IB Payment To - Generic Fallback",
            "Generic IB payment to unspecified recipients (expense)",
            TransactionMappingRule.MatchType.REGEX,
            "^IB PAYMENT TO$",
            "8100", // Employee Costs (generic fallback)
            6
        ));
        
        // Generic IMMEDIATE PAYMENT (fallback for unspecified recipients)
        rules.add(createRule(
            "Immediate Payment - Generic Fallback",
            "Generic immediate payment to unspecified recipients",
            TransactionMappingRule.MatchType.REGEX,
            "^IMMEDIATE PAYMENT$",
            "8100", // Employee Costs (generic fallback)
            6
        ));
        
        // DEBIT TRANSFER (collections/payments)
        rules.add(createRule(
            "Debit Transfer - Collections",
            "DEBIT TRANSFER MIWAYCOLLE00000057007338240401",
            TransactionMappingRule.MatchType.CONTAINS,
            "MIWAY",
            "8800-004", // Miway Insurance Premiums
            6
        ));
        
        // Mobile phone payments (MTN, Vodacom prepaid)
        rules.add(createRule(
            "Mobile Phone Payments - MTN",
            "MTN prepaid mobile phone payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PRE-PAID PAYMENT TO MTN PREPAID",
            "8600", // Communications
            6
        ));
        
        rules.add(createRule(
            "Mobile Phone Payments - VOD",
            "Vodacom prepaid mobile phone payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PRE-PAID PAYMENT TO VOD PREPAID",
            "8600", // Communications
            6
        ));
        
        // Interest adjustments/refunds (very small amounts)
        rules.add(createRule(
            "Interest Adjustment/Refund",
            "Bank interest adjustments and refunds",
            TransactionMappingRule.MatchType.CONTAINS,
            "INTEREST ADJUSTMENT/REFUND",
            "9500", // Interest Income
            6
        ));
        
        // Generic account payments
        rules.add(createRule(
            "Account Payment - Generic",
            "Generic account payments",
            TransactionMappingRule.MatchType.REGEX,
            "^ACCOUNT PAYMENT$",
            "8710", // Suppliers Expense (generic)
            6
        ));
        
        // ========================================================================
        // PRIORITY 7: FINAL REMAINING PATTERNS (100% CLASSIFICATION TARGET)
        // ========================================================================
        
        // MAGTAPE CREDIT specific loan payments
        rules.add(createRule(
            "MAGTAPE CREDIT XINGHIZANA 13AUGLOA",
            "MAGTAPE credit payment to XINGHIZANA 13AUGLOA (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT XINGHIZANA 13AUGLOA",
            "4000", // Long-term Loans
            7
        ));
        
        rules.add(createRule(
            "MAGTAPE CREDIT XG LOA MAPHOSA",
            "MAGTAPE credit payment XG LOA MAPHOSA (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT XG LOA MAPHOSA",
            "4000", // Long-term Loans
            7
        ));
        
        rules.add(createRule(
            "MAGTAPE CREDIT 001 UNPAIDS/WEIERINGS CAPITEC",
            "MAGTAPE credit payment to UNPAIDS/WEIERINGS CAPITEC (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT 001 UNPAIDS/WEIERINGS CAPITEC",
            "4000", // Long-term Loans
            7
        ));
        
        // CASH DEPOSIT STOKFELA
        rules.add(createRule(
            "CASH DEPOSIT STOKFELA",
            "Cash deposit to stokvela account",
            TransactionMappingRule.MatchType.CONTAINS,
            "CASH DEPOSIT STOKFELA",
            "1000", // Cash and Cash Equivalents
            7
        ));
        
        // AUTOBANK INSTANTMONEY CASH TO (cash withdrawal)
        rules.add(createRule(
            "AUTOBANK INSTANTMONEY CASH TO",
            "Cash withdrawal via Instant Money",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK INSTANTMONEY CASH TO",
            "8100", // Employee Costs
            7
        ));
        
        // AUTOBANK TRANSFER FROM ACCOUNT
        rules.add(createRule(
            "AUTOBANK TRANSFER FROM ACCOUNT",
            "Bank transfer from another account",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK TRANSFER FROM ACCOUNT",
            "1100-001", // Bank - Current Account
            7
        ));
        
        // Sort by priority (descending) to ensure highest priority rules are checked first
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        return rules;
    }
    
    /**
     * Classify all unclassified transactions for a company based on mapping rules
     * 
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions classified
     */
    public int classifyAllUnclassifiedTransactions(Long companyId, String username) {
        try {
            TransactionMappingRuleService ruleService = new TransactionMappingRuleService(dbUrl);
            
            String sql = """
                SELECT *
                FROM bank_transactions
                WHERE company_id = ? AND account_code IS NULL
                ORDER BY transaction_date
                """;
            
            int classifiedCount = 0;
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    BankTransaction transaction = mapResultSetToBankTransaction(rs);
                    
                    // Try to find matching account using rules
                    java.util.Optional<Account> matchingAccount = 
                        ruleService.findMatchingAccount(companyId, transaction.getDetails());
                    
                    if (matchingAccount.isPresent()) {
                        Account account = matchingAccount.get();
                        
                        // Update transaction with classification
                        String updateSql = """
                            UPDATE bank_transactions 
                            SET account_code = ?, account_name = ?, last_modified = NOW() 
                            WHERE id = ?
                            """;
                        
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, account.getAccountCode());
                            updateStmt.setString(2, account.getAccountName());
                            updateStmt.setLong(3, transaction.getId());
                            updateStmt.executeUpdate();
                            classifiedCount++;
                        }
                    }
                }
            }
            
            return classifiedCount;
            
        } catch (Exception e) {
            System.err.println("❌ Error classifying transactions: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Reclassify ALL transactions (including already classified ones) based on current mapping rules.
     * 
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions reclassified
     */
    public int reclassifyAllTransactions(Long companyId, String username) {
        try {
            TransactionMappingRuleService ruleService = new TransactionMappingRuleService(dbUrl);
            
            String sql = """
                SELECT *
                FROM bank_transactions
                WHERE company_id = ?
                ORDER BY transaction_date
                """;
            
            int reclassifiedCount = 0;
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setLong(1, companyId);
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    BankTransaction transaction = mapResultSetToBankTransaction(rs);
                    
                    // Try to find matching account using current rules
                    java.util.Optional<Account> matchingAccount = 
                        ruleService.findMatchingAccount(companyId, transaction.getDetails());
                    
                    if (matchingAccount.isPresent()) {
                        Account account = matchingAccount.get();
                        
                        // Update transaction with new classification
                        String updateSql = """
                            UPDATE bank_transactions 
                            SET account_code = ?, account_name = ?, classification_date = CURRENT_TIMESTAMP, classified_by = ?
                            WHERE id = ?
                            """;
                        
                        try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                            updateStmt.setString(1, account.getAccountCode());
                            updateStmt.setString(2, account.getAccountName());
                            updateStmt.setString(3, username);
                            updateStmt.setLong(4, transaction.getId());
                            updateStmt.executeUpdate();
                            reclassifiedCount++;
                        }
                    }
                }
            }
            
            return reclassifiedCount;
            
        } catch (Exception e) {
            System.err.println("❌ Error reclassifying transactions: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Generate journal entries for classified transactions
     * 
     * @param companyId The company ID
     * @param createdBy The username for audit purposes
     * @return Number of journal entries created
     */
    public int generateJournalEntriesForClassifiedTransactions(Long companyId, String createdBy) {
        // Delegate to TransactionProcessingService for journal entry generation
        TransactionProcessingService processingService = new TransactionProcessingService(dbUrl);
        return processingService.generateJournalEntriesForClassifiedTransactions(companyId, createdBy);
    }
    
    /**
     * Generate journal entries for unclassified transactions
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        // Delegate to TransactionProcessingService for journal entry generation
        TransactionProcessingService processingService = new TransactionProcessingService(dbUrl);
        processingService.generateJournalEntriesForUnclassifiedTransactions(companyId, createdBy);
    }
    
    /**
     * Classify a single transaction with the provided account code and name
     * 
     * @param transaction The transaction to classify
     * @param accountCode The account code to assign
     * @param accountName The account name to assign
     * @return true if classification was successful
     */
    public boolean classifyTransaction(BankTransaction transaction, String accountCode, String accountName) {
        try {
            String sql = """
                UPDATE bank_transactions 
                SET account_code = ?, account_name = ?, last_modified = NOW() 
                WHERE id = ?
                """;
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, accountCode);
                stmt.setString(2, accountName);
                stmt.setLong(3, transaction.getId());
                
                int rowsUpdated = stmt.executeUpdate();
                return rowsUpdated > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Error classifying transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Helper method to map ResultSet to BankTransaction
     */
    private BankTransaction mapResultSetToBankTransaction(ResultSet rs) throws SQLException {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(rs.getLong("id"));
        transaction.setCompanyId(rs.getLong("company_id"));
        transaction.setTransactionDate(rs.getDate("transaction_date").toLocalDate());
        transaction.setDetails(rs.getString("details"));
        transaction.setDebitAmount(rs.getBigDecimal("debit_amount"));
        transaction.setCreditAmount(rs.getBigDecimal("credit_amount"));
        transaction.setAccountCode(rs.getString("account_code"));
        transaction.setAccountName(rs.getString("account_name"));
        return transaction;
    }
    
    /**
     * Helper class for account definitions
     */
    private static class AccountDefinition {
        final String code;
        final String name;
        final String description;
        final Long categoryId;
        
        AccountDefinition(String code, String name, String description, Long categoryId) {
            this.code = code;
            this.name = name;
            this.description = description;
            this.categoryId = categoryId;
        }
    }
}
