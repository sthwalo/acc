package fin.service;

import fin.model.*;

/**
 * Service for managing chart of accounts initialization.
 * Follows Single Responsibility Principle - handles only chart of accounts setup.
 * 
 * @deprecated As of 2025-10-03, replaced by {@link AccountClassificationService}
 * 
 * DEPRECATION NOTICE:
 * This service uses a custom chart of accounts structure (accounts 4000-6999) that
 * conflicts with standard South African accounting practices. It has been replaced by
 * AccountClassificationService which provides:
 * - SARS-compliant account structure (accounts 1000-9999)
 * - Standard SA business accounting categories
 * - Comprehensive coverage (50+ accounts vs 25)
 * - No conflicts with mapping rules
 * 
 * MIGRATION PATH:
 * All existing code should be updated to use AccountClassificationService.
 * TransactionClassificationService now uses AccountClassificationService exclusively.
 * 
 * This class will be removed in a future release after confirming all functionality
 * works correctly with AccountClassificationService.
 * 
 * @see AccountClassificationService
 */
@Deprecated
public class ChartOfAccountsService {

    private final CategoryManagementService categoryService;
    private final AccountManagementService accountService;
    private final TransactionMappingRuleService ruleService;

    public ChartOfAccountsService(CategoryManagementService categoryService,
                                AccountManagementService accountService,
                                TransactionMappingRuleService ruleService) {
        this.categoryService = categoryService;
        this.accountService = accountService;
        this.ruleService = ruleService;
    }

    /**
     * Sets up the initial chart of accounts for a company.
     * This should be called when a new company is created.
     * 
     * @deprecated Use AccountClassificationService.initializeChartOfAccounts(Long companyId) instead
     */
    @Deprecated
    public void initializeChartOfAccounts(Company company) {
        System.err.println("⚠️  WARNING: ChartOfAccountsService is deprecated!");
        System.err.println("    Please use AccountClassificationService.initializeChartOfAccounts() instead.");
        System.err.println("    This service will be removed in a future release.");
        // Create categories
        AccountCategory currentAssets = categoryService.createCategory(
            "Current Assets", "Assets expected to be converted to cash within one year",
            AccountType.ASSET, company);
        categoryService.createCategory(
            "Fixed Assets", "Long-term tangible assets",
            AccountType.ASSET, company);
        AccountCategory currentLiabilities = categoryService.createCategory(
            "Current Liabilities", "Obligations due within one year",
            AccountType.LIABILITY, company);
        categoryService.createCategory(
            "Long-term Liabilities", "Obligations due after one year",
            AccountType.LIABILITY, company);
        AccountCategory equity = categoryService.createCategory(
            "Equity", "Owner's equity accounts",
            AccountType.EQUITY, company);
        AccountCategory revenue = categoryService.createCategory(
            "Revenue", "Income from operations",
            AccountType.REVENUE, company);
        AccountCategory costOfSales = categoryService.createCategory(
            "Cost of Sales", "Direct costs related to revenue generation",
            AccountType.EXPENSE, company);
        AccountCategory operatingExpenses = categoryService.createCategory(
            "Operating Expenses", "Costs of running the business",
            AccountType.EXPENSE, company);

        // Create accounts based on the provided chart of accounts
        createAssetAccounts(currentAssets, company);
        createLiabilityAccounts(currentLiabilities, company);
        createEquityAccounts(equity, company);
        createRevenueAccounts(revenue, company);
        createCostOfSalesAccounts(costOfSales, company);
        createOperatingExpenseAccounts(operatingExpenses, company);

        // Setup transaction mapping rules
        setupDefaultMappingRules(company);
    }

    private void createAssetAccounts(AccountCategory currentAssets, Company company) {
        // Assets (1000-1999)
        accountService.createAccount("1010", "Petty Cash", currentAssets, company,
            "Physical cash on hand for minor expenses");
        accountService.createAccount("1020", "Bank Account - Standard Bank #203163753", currentAssets, company,
            "Primary operating account. All transactions flow through here");
        accountService.createAccount("1100", "Loans to Directors / Shareholders", currentAssets, company,
            "Money the company has lent to its directors. Increases with a Credit to the bank account; Decreases with a Debit");
    }

    private void createLiabilityAccounts(AccountCategory currentLiabilities, Company company) {
        // Liabilities (2000-2999)
        accountService.createAccount("2100", "Loans from Directors / Shareholders", currentLiabilities, company,
            "Money directors have lent to the company. Increases with a Credit to the bank account; Decreases with a Debit");
        accountService.createAccount("2200", "Pension Fund Payable", currentLiabilities, company,
            "Outstanding liability for pension contributions owed to the fund");
    }

    private void createEquityAccounts(AccountCategory equity, Company company) {
        // Equity (3000-3999)
        accountService.createAccount("3000", "Owner's Capital Contribution", equity, company,
            "Original capital invested by owners");
        accountService.createAccount("3100", "Retained Earnings", equity, company,
            "Cumulative net profit/loss from previous years");
        accountService.createAccount("3200", "Current Year Profit / (Loss)", equity, company,
            "Current year's net profit/loss");
        accountService.createAccount("3300", "Director's Drawings", equity, company,
            "Personal withdrawals by directors");
    }

    private void createRevenueAccounts(AccountCategory revenue, Company company) {
        // Revenue (4000-4999)
        accountService.createAccount("4000", "Security Services Revenue", revenue, company,
            "Income from security services");
    }

    private void createCostOfSalesAccounts(AccountCategory costOfSales, Company company) {
        // Cost of Sales (5000-5999)
        accountService.createAccount("5010", "Salaries - Security Officers", costOfSales, company,
            "Gross salaries and wages for all security personnel, guards, and response team officers");
        accountService.createAccount("5020", "Salaries - Operations Management", costOfSales, company,
            "Salaries for site managers, supervisors, and operations controllers");
        accountService.createAccount("5030", "Labour Broker Fees", costOfSales, company,
            "Fees paid to external companies for providing labour");
        accountService.createAccount("5040", "Equipment Rental & Communication", costOfSales, company,
            "Cost of renting communication equipment and purchasing airtime");
        accountService.createAccount("5050", "K9 Unit Expenses", costOfSales, company,
            "All costs related to the security dog unit");
        accountService.createAccount("5060", "Site & Client Specific Expenses", costOfSales, company,
            "Direct costs for specific client or site (e.g., fuel for patrol vehicles, client-specific supplies)");
        accountService.createAccount("5070", "Uniforms & Protective Gear", costOfSales, company,
            "Cost of purchasing and maintaining security uniforms, boots, and protective equipment");
    }

    private void createOperatingExpenseAccounts(AccountCategory operatingExpenses, Company company) {
        // Operating Expenses (6000-6999)
        accountService.createAccount("6010", "Salaries - Administration & Directors", operatingExpenses, company,
            "Salaries for office staff, accountants, and the directors' remuneration for their managerial roles");
        accountService.createAccount("6020", "Accounting & Bank Fees", operatingExpenses, company,
            "All bank charges, transaction fees, and account management fees");
        accountService.createAccount("6030", "Insurance Expenses", operatingExpenses, company,
            "Premiums for business insurance, public liability insurance, vehicle insurance, etc");
        accountService.createAccount("6040", "Office Rent", operatingExpenses, company,
            "Fixed lease payments for the company's office or operational premises");
        accountService.createAccount("6050", "Telephone & Internet", operatingExpenses, company,
            "Costs for the office landline, internet data, and admin mobile phones");
        accountService.createAccount("6060", "Pension Fund Contributions", operatingExpenses, company,
            "Employer's contributions to the pension fund on behalf of employees");
        accountService.createAccount("6070", "Office Expenses", operatingExpenses, company,
            "Costs for stationery, printing, and other general office supplies");
        accountService.createAccount("6080", "Motor Vehicle Expenses (Admin)", operatingExpenses, company,
            "Fuel, maintenance, and licensing for administration vehicles");
        accountService.createAccount("6099", "Other Expenses", operatingExpenses, company,
            "A catch-all for legitimate business expenses that do not fit into any other category. Use sparingly and review manually");
    }

    /**
     * Sets up default transaction mapping rules for a company.
     */
    private void setupDefaultMappingRules(Company company) {
        // Find the accounts we need for mapping
        Account securityRevenue = accountService.findAccountByCode(company.getId(), "4000").orElseThrow();
        Account securitySalaries = accountService.findAccountByCode(company.getId(), "5010").orElseThrow();
        Account labourBrokerFees = accountService.findAccountByCode(company.getId(), "5030").orElseThrow();
        Account equipmentRental = accountService.findAccountByCode(company.getId(), "5040").orElseThrow();
        Account k9Expenses = accountService.findAccountByCode(company.getId(), "5050").orElseThrow();
        Account officeRent = accountService.findAccountByCode(company.getId(), "6040").orElseThrow();
        Account bankFees = accountService.findAccountByCode(company.getId(), "6020").orElseThrow();
        Account insurance = accountService.findAccountByCode(company.getId(), "6030").orElseThrow();
        Account telephone = accountService.findAccountByCode(company.getId(), "6050").orElseThrow();
        Account pension = accountService.findAccountByCode(company.getId(), "6060").orElseThrow();
        Account adminSalaries = accountService.findAccountByCode(company.getId(), "6010").orElseThrow();
        Account loansToDirectors = accountService.findAccountByCode(company.getId(), "1100").orElseThrow();

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
        ruleService.saveTransactionMappingRule(rule);
    }
}