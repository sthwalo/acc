package fin.service;

import fin.model.*;
import fin.config.DatabaseConfig;
import java.util.*;

/**
 * Facade service for account-related operations.
 * Delegates to specialized services following Single Responsibility Principle.
 * Provides a unified interface while maintaining separation of concerns.
 */
public class AccountService {
    private final CategoryManagementService categoryService;
    private final AccountManagementService accountService;
    private final ClassificationRuleManager ruleManager;
    private final AccountClassificationService accountClassificationService;

    public AccountService(String dbUrl, CompanyService companyService) {
        this.categoryService = new CategoryManagementService(dbUrl);
        this.accountService = new AccountManagementService(dbUrl);
        this.ruleManager = new ClassificationRuleManager();
        this.accountClassificationService = new AccountClassificationService(dbUrl);
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        // Skip table creation if using PostgreSQL - tables are created via migration script
        if (DatabaseConfig.isUsingPostgreSQL()) {
            System.out.println("📊 Using PostgreSQL - account tables already exist");
            return;
        }
        
        // Database initialization is handled by Flyway migrations for PostgreSQL
        // This method is kept for any additional initialization
    }
    
    // ===== Account Category Methods =====

    public AccountCategory createCategory(String name, String description,
                                        AccountType type, Company company) {
        return categoryService.createCategory(name, description, type, company);
    }

    public void saveCategory(AccountCategory category) {
        categoryService.saveCategory(category);
    }

    public List<AccountCategory> getCategoriesByCompany(Long companyId) {
        return categoryService.getCategoriesByCompany(companyId);
    }
    
    // ===== Account Methods =====

    public Account createAccount(String code, String name, AccountCategory category,
                               Company company, String description) {
        return accountService.createAccount(code, name, category, company, description);
    }

    public void saveAccount(Account account) {
        accountService.saveAccount(account);
    }

    public List<Account> getAccountsByCompany(Long companyId) {
        return accountService.getAccountsByCompany(companyId);
    }

    public Optional<Account> findAccountByCode(Long companyId, String accountCode) {
        return accountService.findAccountByCode(companyId, accountCode);
    }
    
    // ===== Transaction Mapping Rules =====

    public void saveTransactionMappingRule(TransactionMappingRule rule) {
        // Convert TransactionMappingRule to ClassificationRuleManager format
        Long companyId = rule.getCompany() != null ? rule.getCompany().getId() : null;
        String accountCode = rule.getAccount() != null ? rule.getAccount().getAccountCode() : null;
        String accountName = rule.getAccount() != null ? rule.getAccount().getAccountName() : null;
        
        if (companyId != null && accountCode != null && rule.getMatchValue() != null) {
            ruleManager.createRule(companyId, rule.getMatchValue(), accountCode, accountName);
        }
    }

    public List<TransactionMappingRule> getTransactionMappingRules(Long companyId) {
        // Convert ClassificationRuleManager rules to TransactionMappingRule format
        List<ClassificationRuleManager.ClassificationRule> rules = ruleManager.getRulesByCompany(companyId);
        return rules.stream()
                .map(this::convertToTransactionMappingRule)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Finds the best matching account for a transaction description
     * by applying the transaction mapping rules in priority order.
     */
    public Optional<Account> findMatchingAccount(Long companyId, String description) {
        List<ClassificationRuleManager.ClassificationRule> rules = ruleManager.getRulesByCompany(companyId);
        for (ClassificationRuleManager.ClassificationRule rule : rules) {
            if (description.toLowerCase().contains(rule.getPattern().toLowerCase())) {
                Account account = new Account();
                account.setAccountCode(rule.getAccountCode());
                account.setAccountName(rule.getAccountName());
                return Optional.of(account);
            }
        }
        return Optional.empty();
    }
    
    private TransactionMappingRule convertToTransactionMappingRule(ClassificationRuleManager.ClassificationRule rule) {
        TransactionMappingRule mappingRule = new TransactionMappingRule();
        mappingRule.setId(rule.getId());
        
        // Create company object
        Company company = new Company();
        company.setId(rule.getCompanyId());
        mappingRule.setCompany(company);
        
        mappingRule.setRuleName("Rule_" + rule.getId());
        mappingRule.setMatchValue(rule.getPattern());
        mappingRule.setMatchType(TransactionMappingRule.MatchType.CONTAINS);
        
        // Create account object
        Account account = new Account();
        account.setAccountCode(rule.getAccountCode());
        account.setAccountName(rule.getAccountName());
        mappingRule.setAccount(account);
        
        return mappingRule;
    }
    
    // ===== Initial Setup =====

    /**
     * Sets up the initial chart of accounts for a company.
     * This should be called when a new company is created.
     * Uses AccountClassificationService for SARS-compliant accounts.
     */
    public void initializeChartOfAccounts(Company company) {
        accountClassificationService.initializeChartOfAccounts(company.getId());
    }
}
