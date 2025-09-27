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
    private final TransactionMappingRuleService ruleService;
    private final ChartOfAccountsService chartService;

    public AccountService(String dbUrl, CompanyService companyService) {
        this.categoryService = new CategoryManagementService(dbUrl);
        this.accountService = new AccountManagementService(dbUrl);
        this.ruleService = new TransactionMappingRuleService(dbUrl);
        this.chartService = new ChartOfAccountsService(categoryService, accountService, ruleService);
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
        ruleService.saveTransactionMappingRule(rule);
    }

    public List<TransactionMappingRule> getTransactionMappingRules(Long companyId) {
        return ruleService.getTransactionMappingRules(companyId);
    }

    /**
     * Finds the best matching account for a transaction description
     * by applying the transaction mapping rules in priority order.
     */
    public Optional<Account> findMatchingAccount(Long companyId, String description) {
        return ruleService.findMatchingAccount(companyId, description);
    }
    
    // ===== Initial Setup =====

    /**
     * Sets up the initial chart of accounts for a company.
     * This should be called when a new company is created.
     */
    public void initializeChartOfAccounts(Company company) {
        chartService.initializeChartOfAccounts(company);
    }
}
