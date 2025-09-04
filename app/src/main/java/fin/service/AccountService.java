package fin.service;

import fin.model.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Service for managing accounts, categories, and transaction mapping rules.
 */
public class AccountService {
    private static final Logger LOGGER = Logger.getLogger(AccountService.class.getName());
    private final String dbUrl;
    private final CompanyService companyService;
    
    // Cache for better performance
    private Map<Long, List<Account>> accountsByCompany = new HashMap<>();
    private Map<Long, List<AccountCategory>> categoriesByCompany = new HashMap<>();
    private Map<Long, List<TransactionMappingRule>> mappingRulesByCompany = new HashMap<>();
    
    public AccountService(String dbUrl, CompanyService companyService) {
        this.dbUrl = dbUrl;
        this.companyService = companyService;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        // Database initialization is handled by Flyway migrations
        // This method is kept for any additional initialization
    }
    
    // ===== Account Category Methods =====
    
    public AccountCategory createCategory(String name, String description, 
                                        AccountType type, Company company) {
        AccountCategory category = new AccountCategory(name, description, type, company);
        saveCategory(category);
        return category;
    }
    
    public void saveCategory(AccountCategory category) {
        String sql = 
            """
            INSERT INTO account_categories (name, description, account_type_id, company_id, is_active)
            VALUES (?, ?, 
                (SELECT id FROM account_types WHERE code = ?), 
                ?, ?)
            ON CONFLICT(company_id, name) DO UPDATE SET
                description = excluded.description,
                account_type_id = excluded.account_type_id,
                is_active = excluded.is_active,
                updated_at = CURRENT_TIMESTAMP
            RETURNING id, created_at, updated_at
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, category.getName());
            pstmt.setString(2, category.getDescription());
            pstmt.setString(3, category.getAccountType().getCode());
            pstmt.setLong(4, category.getCompany().getId());
            pstmt.setBoolean(5, category.isActive());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    category.setId(rs.getLong("id"));
                    category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    category.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            
            // Clear cache
            categoriesByCompany.remove(category.getCompany().getId());
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving account category", e);
            throw new RuntimeException("Failed to save account category", e);
        }
    }
    
    public List<AccountCategory> getCategoriesByCompany(Long companyId) {
        // Check cache first
        if (categoriesByCompany.containsKey(companyId)) {
            return categoriesByCompany.get(companyId);
        }
        
        List<AccountCategory> categories = new ArrayList<>();
        String sql = 
                "SELECT c.*, t.code as type_code, t.name as type_name, t.normal_balance " +
                "FROM account_categories c " +
                "JOIN account_types t ON c.account_type_id = t.id " +
                "WHERE c.company_id = ? AND c.is_active = 1 " +
                "ORDER BY t.code, c.name";
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AccountCategory category = new AccountCategory();
                    category.setId(rs.getLong("id"));
                    category.setName(rs.getString("name"));
                    category.setDescription(rs.getString("description"));
                    category.setActive(rs.getBoolean("is_active"));
                    
                    // Set account type
                    String typeCode = rs.getString("type_code");
                    category.setAccountType(AccountType.fromCode(typeCode));
                    
                    // Set company (lightweight, just ID)
                    Company company = new Company();
                    company.setId(companyId);
                    category.setCompany(company);
                    
                    categories.add(category);
                }
            }
            
            // Update cache
            categoriesByCompany.put(companyId, categories);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching account categories", e);
            throw new RuntimeException("Failed to fetch account categories", e);
        }
        
        return categories;
    }
    
    // ===== Account Methods =====
    
    public Account createAccount(String code, String name, AccountCategory category, 
                               Company company, String description) {
        Account account = new Account(code, name, category, company, description);
        saveAccount(account);
        return account;
    }
    
    public void saveAccount(Account account) {
        String sql = 
            "INSERT INTO accounts (account_code, account_name, description, " +
            "category_id, parent_account_id, company_id, is_active) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(company_id, account_code) DO UPDATE SET " +
            "account_name = excluded.account_name, " +
            "description = excluded.description, " +
            "category_id = excluded.category_id, " +
            "parent_account_id = excluded.parent_account_id, " +
            "is_active = excluded.is_active, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "RETURNING id, created_at, updated_at";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, account.getAccountCode());
            pstmt.setString(2, account.getAccountName());
            pstmt.setString(3, account.getDescription());
            pstmt.setLong(4, account.getCategory().getId());
            
            if (account.getParentAccount() != null) {
                pstmt.setLong(5, account.getParentAccount().getId());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }
            
            pstmt.setLong(6, account.getCompany().getId());
            pstmt.setBoolean(7, account.isActive());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    account.setId(rs.getLong("id"));
                    account.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    account.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            
            // Clear cache
            accountsByCompany.remove(account.getCompany().getId());
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving account", e);
            throw new RuntimeException("Failed to save account", e);
        }
    }
    
    public List<Account> getAccountsByCompany(Long companyId) {
        // Check cache first
        if (accountsByCompany.containsKey(companyId)) {
            return accountsByCompany.get(companyId);
        }
        
        List<Account> accounts = new ArrayList<>();
        String sql = 
            "SELECT a.*, c.id as category_id, c.name as category_name, " +
            "t.code as type_code, t.name as type_name, t.normal_balance " +
            "FROM accounts a " +
            "JOIN account_categories c ON a.category_id = c.id " +
            "JOIN account_types t ON c.account_type_id = t.id " +
            "WHERE a.company_id = ? AND a.is_active = 1 " +
            "ORDER BY a.account_code";
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Account account = new Account();
                    account.setId(rs.getLong("id"));
                    account.setAccountCode(rs.getString("account_code"));
                    account.setAccountName(rs.getString("account_name"));
                    account.setDescription(rs.getString("description"));
                    account.setActive(rs.getBoolean("is_active"));
                    
                    // Set category
                    AccountCategory category = new AccountCategory();
                    category.setId(rs.getLong("category_id"));
                    category.setName(rs.getString("category_name"));
                    category.setAccountType(AccountType.fromCode(rs.getString("type_code")));
                    account.setCategory(category);
                    
                    // Set company (lightweight, just ID)
                    Company company = new Company();
                    company.setId(companyId);
                    account.setCompany(company);
                    
                    accounts.add(account);
                }
            }
            
            // Update cache
            accountsByCompany.put(companyId, accounts);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching accounts", e);
            throw new RuntimeException("Failed to fetch accounts", e);
        }
        
        return accounts;
    }
    
    public Optional<Account> findAccountByCode(Long companyId, String accountCode) {
        return getAccountsByCompany(companyId).stream()
            .filter(a -> a.getAccountCode().equals(accountCode))
            .findFirst();
    }
    
    // ===== Transaction Mapping Rules =====
    
    public void saveTransactionMappingRule(TransactionMappingRule rule) {
        String sql = 
            "INSERT INTO transaction_mapping_rules " +
            "(company_id, rule_name, description, match_type, match_value, " +
            "account_id, is_active, priority) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT(id) DO UPDATE SET " +
            "rule_name = excluded.rule_name, " +
            "description = excluded.description, " +
            "match_type = excluded.match_type, " +
            "match_value = excluded.match_value, " +
            "account_id = excluded.account_id, " +
            "is_active = excluded.is_active, " +
            "priority = excluded.priority, " +
            "updated_at = CURRENT_TIMESTAMP " +
            "RETURNING id, created_at, updated_at";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, rule.getCompany().getId());
            pstmt.setString(2, rule.getRuleName());
            pstmt.setString(3, rule.getDescription());
            pstmt.setString(4, rule.getMatchType().name());
            pstmt.setString(5, rule.getMatchValue());
            pstmt.setLong(6, rule.getAccount().getId());
            pstmt.setBoolean(7, rule.isActive());
            pstmt.setInt(8, rule.getPriority());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    rule.setId(rs.getLong("id"));
                    rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    rule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
            }
            
            // Clear cache
            mappingRulesByCompany.remove(rule.getCompany().getId());
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error saving transaction mapping rule", e);
            throw new RuntimeException("Failed to save transaction mapping rule", e);
        }
    }
    
    public List<TransactionMappingRule> getTransactionMappingRules(Long companyId) {
        // Check cache first
        if (mappingRulesByCompany.containsKey(companyId)) {
            return mappingRulesByCompany.get(companyId);
        }
        
        List<TransactionMappingRule> rules = new ArrayList<>();
        String sql = 
            "SELECT r.*, a.account_code, a.account_name, " +
            "c.name as category_name, t.code as type_code " +
            "FROM transaction_mapping_rules r " +
            "JOIN accounts a ON r.account_id = a.id " +
            "JOIN account_categories c ON a.category_id = c.id " +
            "JOIN account_types t ON c.account_type_id = t.id " +
            "WHERE r.company_id = ? AND r.is_active = 1 " +
            "ORDER BY r.priority DESC, r.rule_name";
            
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TransactionMappingRule rule = new TransactionMappingRule();
                    rule.setId(rs.getLong("id"));
                    rule.setRuleName(rs.getString("rule_name"));
                    rule.setDescription(rs.getString("description"));
                    rule.setMatchType(TransactionMappingRule.MatchType.valueOf(
                        rs.getString("match_type")));
                    rule.setMatchValue(rs.getString("match_value"));
                    rule.setActive(rs.getBoolean("is_active"));
                    rule.setPriority(rs.getInt("priority"));
                    rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    rule.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                    
                    // Set account (lightweight)
                    Account account = new Account();
                    account.setId(rs.getLong("account_id"));
                    account.setAccountCode(rs.getString("account_code"));
                    account.setAccountName(rs.getString("account_name"));
                    
                    // Set category (lightweight)
                    AccountCategory category = new AccountCategory();
                    category.setName(rs.getString("category_name"));
                    category.setAccountType(AccountType.fromCode(rs.getString("type_code")));
                    account.setCategory(category);
                    
                    rule.setAccount(account);
                    
                    // Set company (lightweight, just ID)
                    Company company = new Company();
                    company.setId(companyId);
                    rule.setCompany(company);
                    
                    rules.add(rule);
                }
            }
            
            // Update cache
            mappingRulesByCompany.put(companyId, rules);
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching transaction mapping rules", e);
            throw new RuntimeException("Failed to fetch transaction mapping rules", e);
        }
        
        return rules;
    }
    
    /**
     * Finds the best matching account for a transaction description
     * by applying the transaction mapping rules in priority order.
     */
    public Optional<Account> findMatchingAccount(Long companyId, String description) {
        if (description == null || description.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return getTransactionMappingRules(companyId).stream()
            .filter(rule -> rule.matches(description))
            .findFirst()
            .map(TransactionMappingRule::getAccount);
    }
    
    // ===== Initial Setup =====
    
    /**
     * Sets up the initial chart of accounts for a company.
     * This should be called when a new company is created.
     */
    public void initializeChartOfAccounts(Company company) {
        // Create categories
        AccountCategory currentAssets = createCategory("Current Assets", "Assets expected to be converted to cash within one year", 
                                                     AccountType.ASSET, company);
        AccountCategory fixedAssets = createCategory("Fixed Assets", "Long-term tangible assets", 
                                                   AccountType.ASSET, company);
        AccountCategory currentLiabilities = createCategory("Current Liabilities", "Obligations due within one year", 
                                                         AccountType.LIABILITY, company);
        AccountCategory longTermLiabilities = createCategory("Long-term Liabilities", "Obligations due after one year", 
                                                          AccountType.LIABILITY, company);
        AccountCategory equity = createCategory("Equity", "Owner's equity accounts", 
                                             AccountType.EQUITY, company);
        AccountCategory revenue = createCategory("Revenue", "Income from operations", 
                                              AccountType.REVENUE, company);
        AccountCategory costOfSales = createCategory("Cost of Sales", "Direct costs related to revenue generation", 
                                                  AccountType.EXPENSE, company);
        AccountCategory operatingExpenses = createCategory("Operating Expenses", "Costs of running the business", 
                                                        AccountType.EXPENSE, company);
        
        // Create accounts based on the provided chart of accounts
        // Assets (1000-1999)
        createAccount("1010", "Petty Cash", currentAssets, company, "Physical cash on hand for minor expenses");
        createAccount("1020", "Bank Account - Standard Bank #203163753", currentAssets, company, "Primary operating account. All transactions flow through here");
        createAccount("1100", "Loans to Directors / Shareholders", currentAssets, company, "Money the company has lent to its directors. Increases with a Credit to the bank account; Decreases with a Debit");
        
        // Liabilities (2000-2999)
        createAccount("2100", "Loans from Directors / Shareholders", currentLiabilities, company, "Money directors have lent to the company. Increases with a Credit to the bank account; Decreases with a Debit");
        createAccount("2200", "Pension Fund Payable", currentLiabilities, company, "Outstanding liability for pension contributions owed to the fund");
        
        // Equity (3000-3999)
        createAccount("3000", "Owner's Capital Contribution", equity, company, "Original capital invested by owners");
        createAccount("3100", "Retained Earnings", equity, company, "Cumulative net profit/loss from previous years");
        createAccount("3200", "Current Year Profit / (Loss)", equity, company, "Current year's net profit/loss");
        createAccount("3300", "Director's Drawings", equity, company, "Personal withdrawals by directors");
        
        // Revenue (4000-4999)
        createAccount("4000", "Security Services Revenue", revenue, company, "Income from security services");
        
        // Cost of Sales (5000-5999)
        createAccount("5010", "Salaries - Security Officers", costOfSales, company, "Gross salaries and wages for all security personnel, guards, and response team officers");
        createAccount("5020", "Salaries - Operations Management", costOfSales, company, "Salaries for site managers, supervisors, and operations controllers");
        createAccount("5030", "Labour Broker Fees", costOfSales, company, "Fees paid to external companies for providing labour");
        createAccount("5040", "Equipment Rental & Communication", costOfSales, company, "Cost of renting communication equipment and purchasing airtime");
        createAccount("5050", "K9 Unit Expenses", costOfSales, company, "All costs related to the security dog unit");
        createAccount("5060", "Site & Client Specific Expenses", costOfSales, company, "Direct costs for specific client or site (e.g., fuel for patrol vehicles, client-specific supplies)");
        createAccount("5070", "Uniforms & Protective Gear", costOfSales, company, "Cost of purchasing and maintaining security uniforms, boots, and protective equipment");
        
        // Operating Expenses (6000-6999)
        createAccount("6010", "Salaries - Administration & Directors", operatingExpenses, company, "Salaries for office staff, accountants, and the directors' remuneration for their managerial roles");
        createAccount("6020", "Accounting & Bank Fees", operatingExpenses, company, "All bank charges, transaction fees, and account management fees");
        createAccount("6030", "Insurance Expenses", operatingExpenses, company, "Premiums for business insurance, public liability insurance, vehicle insurance, etc");
        createAccount("6040", "Office Rent", operatingExpenses, company, "Fixed lease payments for the company's office or operational premises");
        createAccount("6050", "Telephone & Internet", operatingExpenses, company, "Costs for the office landline, internet data, and admin mobile phones");
        createAccount("6060", "Pension Fund Contributions", operatingExpenses, company, "Employer's contributions to the pension fund on behalf of employees");
        createAccount("6070", "Office Expenses", operatingExpenses, company, "Costs for stationery, printing, and other general office supplies");
        createAccount("6080", "Motor Vehicle Expenses (Admin)", operatingExpenses, company, "Fuel, maintenance, and licensing for administration vehicles");
        createAccount("6099", "Other Expenses", operatingExpenses, company, "A catch-all for legitimate business expenses that do not fit into any other category. Use sparingly and review manually");
        
        // Setup transaction mapping rules
        setupDefaultMappingRules(company);
    }
    
    /**
     * Sets up default transaction mapping rules for a company.
     */
    private void setupDefaultMappingRules(Company company) {
        // Find the accounts we need for mapping
        Account securityRevenue = findAccountByCode(company.getId(), "4000").orElseThrow();
        Account securitySalaries = findAccountByCode(company.getId(), "5010").orElseThrow();
        Account labourBrokerFees = findAccountByCode(company.getId(), "5030").orElseThrow();
        Account equipmentRental = findAccountByCode(company.getId(), "5040").orElseThrow();
        Account k9Expenses = findAccountByCode(company.getId(), "5050").orElseThrow();
        Account officeRent = findAccountByCode(company.getId(), "6040").orElseThrow();
        Account bankFees = findAccountByCode(company.getId(), "6020").orElseThrow();
        Account insurance = findAccountByCode(company.getId(), "6030").orElseThrow();
        Account telephone = findAccountByCode(company.getId(), "6050").orElseThrow();
        Account pension = findAccountByCode(company.getId(), "6060").orElseThrow();
        Account adminSalaries = findAccountByCode(company.getId(), "6010").orElseThrow();
        Account loansToDirectors = findAccountByCode(company.getId(), "1100").orElseThrow();
        
        // Create mapping rules (in order of priority)
        // Revenue Rules
        createMappingRule(company, "Corobrik Revenue", "Map all Corobrik deposits to Security Revenue", 
                         TransactionMappingRule.MatchType.CONTAINS, "COROBRIK", securityRevenue, 100);
        
        // Cost of Sales Rules
        createMappingRule(company, "Security Salaries", "Map XG SALARIES (including Jordan Moyane) to Security Salaries", 
                         TransactionMappingRule.MatchType.CONTAINS, "XG SALARIES", securitySalaries, 90);
        
        createMappingRule(company, "Labour Broker Fees", "Map NEO ENTLE LABOUR HUMAN RESOUR to Labour Broker Fees", 
                         TransactionMappingRule.MatchType.CONTAINS, "NEO ENTLE LABOUR", labourBrokerFees, 90);
        
        createMappingRule(company, "Equipment Rental", "Map TWO WAY TECHNOLOGIES to Equipment Rental", 
                         TransactionMappingRule.MatchType.CONTAINS, "TWO WAY TECHNOLOGIES", equipmentRental, 90);
        
        createMappingRule(company, "K9 Expenses", "Map RENT A DOG to K9 Unit Expenses", 
                         TransactionMappingRule.MatchType.CONTAINS, "RENT A DOG", k9Expenses, 90);
        
        // Operating Expenses Rules
        createMappingRule(company, "Office Rent", "Map ELLISPARK STADIUM XINGHIZANA to Office Rent", 
                         TransactionMappingRule.MatchType.CONTAINS, "ELLISPARK STADIUM", officeRent, 90);
        
        createMappingRule(company, "Bank Fees", "Map all FEE transactions to Accounting & Bank Fees", 
                         TransactionMappingRule.MatchType.CONTAINS, "FEE", bankFees, 80);
        
        createMappingRule(company, "Insurance", "Map INSURANCE PREMIUM to Insurance Expenses", 
                         TransactionMappingRule.MatchType.CONTAINS, "INSURANCE PREMIUM", insurance, 80);
        
        createMappingRule(company, "Telephone", "Map TELEPHONE ACCOUNT to Telephone & Internet", 
                         TransactionMappingRule.MatchType.CONTAINS, "TELEPHONE ACCOUNT", telephone, 80);
        
        createMappingRule(company, "Pension", "Map PENSION FUND CONTRIBUTION to Pension Fund Contributions", 
                         TransactionMappingRule.MatchType.CONTAINS, "PENSION FUND CONTRIBUTION", pension, 80);
        
        createMappingRule(company, "Admin Salaries", "Map JEFFREY MAPHOSA debits to Admin Salaries", 
                         TransactionMappingRule.MatchType.CONTAINS, "JEFFREY MAPHOSA", adminSalaries, 70);
        
        // Special case: Jeffrey Maphosa repayments (credits) should go to Loans to Directors
        createMappingRule(company, "Director Loan Repayment", "Map JEFFREY MAPHOSA credits to Loans to Directors", 
                         TransactionMappingRule.MatchType.CONTAINS, "JEFFREY MAPHOSA", loansToDirectors, 60);
    }
    
    private void createMappingRule(Company company, String name, String description, 
                                 TransactionMappingRule.MatchType matchType, String matchValue, 
                                 Account account, int priority) {
        TransactionMappingRule rule = new TransactionMappingRule(
            company, name, matchType, matchValue, account);
        rule.setDescription(description);
        rule.setPriority(priority);
        saveTransactionMappingRule(rule);
    }
}
