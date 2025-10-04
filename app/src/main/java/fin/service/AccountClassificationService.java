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
        
        // OPERATING REVENUE (6000-6999)
        Long operatingRevenueId = categoryIds.get("OPERATING_REVENUE");
        accounts.add(new AccountDefinition("6000", "Sales Revenue", "Revenue from sales", operatingRevenueId));
        accounts.add(new AccountDefinition("6100", "Service Revenue", "Revenue from services", operatingRevenueId));
        // Sub-account for specific service revenue
        accounts.add(new AccountDefinition("4000-001", "Corobrik Service Revenue", "Service revenue from Corobrik", operatingRevenueId));
        
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
        
        // Professional services (accounting, legal)
        rules.add(createRule(
            "Global Hope Financia Accounting",
            "Accounting services from Global Hope Financia",
            TransactionMappingRule.MatchType.CONTAINS,
            "GLOBAL HOPE FINACIA",
            "8700", // Professional Services
            9
        ));
        
        // Supplier payments (NOT director remuneration)
        rules.add(createRule(
            "Rent A Dog Supplier",
            "Supplier payments to Rent A Dog (not director remuneration)",
            TransactionMappingRule.MatchType.CONTAINS,
            "RENT A DOG",
            "3000", // Accounts Payable
            9
        ));
        
        // ========================================================================
        // PRIORITY 8: STANDARD BUSINESS PATTERNS
        // ========================================================================
        
        // Internal bank transfers (between own accounts)
        rules.add(createRule(
            "Bank Transfers - IB TRANSFER TO",
            "Internal bank transfers to other accounts (e.g., fuel account)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER TO",
            "1100", // Bank - Current Account
            8
        ));
        
        rules.add(createRule(
            "Bank Transfers - IB TRANSFER FROM",
            "Internal bank transfers from other accounts (e.g., fuel account)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER FROM",
            "1100", // Bank - Current Account
            8
        ));
        
        // Supplier payments
        rules.add(createRule(
            "DB Projects Supplier",
            "Supplier payments to DB Projects and Agencies",
            TransactionMappingRule.MatchType.CONTAINS,
            "DB PROJECTS",
            "3000", // Accounts Payable
            8
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
        
        // Education expenses
        rules.add(createRule(
            "Lyceum College Education",
            "School fees and education expenses at Lyceum College",
            TransactionMappingRule.MatchType.CONTAINS,
            "LYCEUM COLLEGE",
            "9300", // Training & Development
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
        
        // Bank charges and fees
        rules.add(createRule(
            "Bank Charges - FEE keyword",
            "Bank fees and charges",
            TransactionMappingRule.MatchType.CONTAINS,
            "FEE:",
            "9600", // Bank Charges
            5
        ));
        
        rules.add(createRule(
            "Bank Charges - SERVICE FEE",
            "Monthly service fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "SERVICE FEE",
            "9600", // Bank Charges
            5
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
        
        // Sort by priority (descending) to ensure highest priority rules are checked first
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        return rules;
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
