/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.service.spring;

import fin.model.Account;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.TransactionMappingRule;
import fin.model.AccountCategory;
import fin.model.AccountType;
import fin.model.JournalEntry;
import fin.model.JournalEntryLine;
import fin.repository.AccountRepository;
import fin.repository.BankTransactionRepository;
import fin.repository.CompanyRepository;
import fin.repository.AccountCategoryRepository;
import fin.repository.TransactionMappingRuleRepository;
import fin.repository.JournalEntryRepository;
import fin.repository.JournalEntryLineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Service for classifying and categorizing accounts based on transaction analysis.
 * Sets up a standard chart of accounts for South African businesses.
 */
@Service
@Transactional
public class AccountClassificationService {
    
    // Display formatting constants
    private static final int DISPLAY_WIDTH_REPORT = 60;
    
    // Array indices and limits
    
    // Rule priority constants
    private static final int PRIORITY_CRITICAL = 10;
    private static final int PRIORITY_HIGH = 9;
    private static final int PRIORITY_STANDARD = 8;
    private static final int PRIORITY_GENERIC = 5;
    private static final int PRIORITY_LOW = 6;
    private static final int PRIORITY_FALLBACK = 7;
    private static final int PRIORITY_HIGHEST = 20;
    
    private final CompanyRepository companyRepository;
    private final AccountRepository accountRepository;
    private final AccountCategoryRepository accountCategoryRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final TransactionMappingRuleRepository transactionMappingRuleRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public AccountClassificationService(
            CompanyRepository companyRepository,
            AccountRepository accountRepository,
            AccountCategoryRepository accountCategoryRepository,
            BankTransactionRepository bankTransactionRepository,
            TransactionMappingRuleRepository transactionMappingRuleRepository,
            JournalEntryRepository journalEntryRepository,
            JournalEntryLineRepository journalEntryLineRepository) {
        this.companyRepository = companyRepository;
        this.accountRepository = accountRepository;
        this.accountCategoryRepository = accountCategoryRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.transactionMappingRuleRepository = transactionMappingRuleRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
    }
    
    /**
     * Initialize the complete chart of accounts for a company
     */
    public void initializeChartOfAccounts(Long companyId) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        
        Company company = companyOpt.get();
        System.out.println("🏗️ Initializing Chart of Accounts for: " + company.getName());
        
        // 1. Create account categories
        Map<String, Long> categoryIds = createAccountCategories(companyId);
        
        // 2. Create accounts
        createStandardAccounts(companyId, categoryIds);
        
        // 3. Analyze and classify existing transactions
        analyzeTransactionPatterns(companyId);
        
        System.out.println("✅ Chart of Accounts initialization complete!");
    }

    /**
     * Initialize transaction mapping rules for a company
     * This creates the standard pattern-matching rules for auto-classification
     * 
     * @param companyId The company to initialize
     * @return Number of rules created
     */
    public int initializeTransactionMappingRules(Long companyId) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        
        Company company = companyOpt.get();
        System.out.println("🔗 Initializing Transaction Mapping Rules for: " + company.getName());
        
        // Get rules from single source of truth (this service)
        List<TransactionMappingRule> rules = getStandardMappingRules();
        
        // Persist to database using repository
        List<TransactionMappingRule> savedRules = new ArrayList<>();
        for (TransactionMappingRule rule : rules) {
            rule.setCompany(company);
            // Extract account code from description if present
            if (rule.getDescription() != null && rule.getDescription().contains("[AccountCode:")) {
                int startIndex = rule.getDescription().indexOf("[AccountCode:") + 13;
                int endIndex = rule.getDescription().indexOf("]", startIndex);
                if (endIndex > startIndex) {
                    String accountCode = rule.getDescription().substring(startIndex, endIndex);
                    // Find account by code and company
                    Optional<Account> accountOpt = accountRepository.findByCompanyIdAndAccountCode(companyId, accountCode);
                    if (accountOpt.isPresent()) {
                        rule.setAccount(accountOpt.get());
                    }
                }
            }
            savedRules.add(transactionMappingRuleRepository.save(rule));
        }
        
        System.out.println("✅ Created " + savedRules.size() + " standard mapping rules");
        return savedRules.size();
    }
    
    /**
     * Create standard account categories based on South African business practices
     */
    private Map<String, Long> createAccountCategories(Long companyId) {
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
    private Long createCategory(Long companyId, String name, String description, int accountTypeId) {
        // First check if category already exists
        Optional<AccountCategory> existingCategory = accountCategoryRepository
            .findByCompanyIdAndName(companyId, name);
        
        if (existingCategory.isPresent()) {
            Long existingId = existingCategory.get().getId();
            System.out.println("📋 Category already exists: " + name + " (ID: " + existingId + ")");
            return existingId;
        }
        
        // Category doesn't exist, create it
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }
        
        // Convert accountTypeId to AccountType enum
        AccountType accountType;
        switch (accountTypeId) {
            case 1: accountType = AccountType.ASSET; break;
            case 2: accountType = AccountType.LIABILITY; break;
            case 3: accountType = AccountType.EQUITY; break;
            case 4: accountType = AccountType.REVENUE; break;
            case 5: accountType = AccountType.EXPENSE; break;
            default: throw new IllegalArgumentException("Invalid account type ID: " + accountTypeId);
        }
        
        AccountCategory category = new AccountCategory();
        category.setCompany(companyOpt.get());
        category.setName(name);
        category.setDescription(description);
        category.setAccountType(accountType);
        
        AccountCategory savedCategory = accountCategoryRepository.save(category);
        Long id = savedCategory.getId();
        System.out.println("📋 Created new category: " + name + " (ID: " + id + ")");
        return id;
    }
    
    /**
     * Create standard accounts for a South African business
     */
    private void createStandardAccounts(Long companyId, Map<String, Long> categoryIds) {
        List<Account> accounts = getStandardAccountDefinitions(categoryIds);

        // Check which accounts already exist
        List<Account> existingAccounts = accountRepository.findByCompanyId(companyId);
        Set<String> existingAccountCodes = existingAccounts.stream()
            .map(Account::getAccountCode)
            .collect(java.util.stream.Collectors.toSet());

        // Filter out existing accounts
        List<Account> newAccounts = accounts.stream()
            .filter(account -> !existingAccountCodes.contains(account.getAccountCode()))
            .collect(java.util.stream.Collectors.toList());

        if (newAccounts.isEmpty()) {
            System.out.println("✅ All standard accounts already exist (" + accounts.size() + " accounts)");
            return;
        }

        // Get company for account creation
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Create and save new accounts
        List<Account> accountsToSave = new ArrayList<>();
        for (Account account : newAccounts) {
            Account accountToSave = new Account(account.getAccountCode(), account.getAccountName(), companyId);
            accountToSave.setDescription(account.getDescription());
            accountToSave.setCategoryId(account.getCategoryId());
            accountsToSave.add(accountToSave);
        }

        List<Account> savedAccounts = accountRepository.saveAll(accountsToSave);
        System.out.println("✅ Created " + savedAccounts.size() + " new standard accounts");
        System.out.println("ℹ️ Skipped " + existingAccountCodes.size() + " existing accounts");
    }
    
    /**
     * Get standard account definitions for South African business
     */
    private List<Account> getStandardAccountDefinitions(Map<String, Long> categoryIds) {
        List<Account> accounts = new ArrayList<>();

        // Add accounts by category
        addCurrentAssetAccounts(accounts, categoryIds);
        addNonCurrentAssetAccounts(accounts, categoryIds);
        addCurrentLiabilityAccounts(accounts, categoryIds);
        addNonCurrentLiabilityAccounts(accounts, categoryIds);
        addEquityAccounts(accounts, categoryIds);
        addOperatingRevenueAccounts(accounts, categoryIds);
        addOtherIncomeAccounts(accounts, categoryIds);
        addOperatingExpenseAccounts(accounts, categoryIds);
        addAdministrativeExpenseAccounts(accounts, categoryIds);
        addFinanceCostAccounts(accounts, categoryIds);

        return accounts;
    }
    
    /**
     * Add CURRENT ASSETS accounts (1000-1999)
     */
    private void addCurrentAssetAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long currentAssetsId = categoryIds.get("CURRENT_ASSETS");
        Account account1000 = new Account("1000", "Petty Cash", 0L);
        account1000.setDescription("Cash on hand for small expenses");
        account1000.setCategoryId(currentAssetsId);
        accounts.add(account1000);

        // Sub-accounts for loan receivables
        Account account1000001 = new Account("1000-001", "Loan Receivable - Tau", 0L);
        account1000001.setDescription("Loan receivable from Tau");
        account1000001.setCategoryId(currentAssetsId);
        accounts.add(account1000001);

        Account account1000002 = new Account("1000-002", "Loan Receivable - Maposa", 0L);
        account1000002.setDescription("Loan receivable from Maposa");
        account1000002.setCategoryId(currentAssetsId);
        accounts.add(account1000002);

        Account account1000003 = new Account("1000-003", "Loan Receivable - Other", 0L);
        account1000003.setDescription("Other loan receivables");
        account1000003.setCategoryId(currentAssetsId);
        accounts.add(account1000003);

        Account account1100 = new Account("1100", "Bank - Current Account", 0L);
        account1100.setDescription("Primary business current account");
        account1100.setCategoryId(currentAssetsId);
        accounts.add(account1100);

        // Sub-account for bank transfers
        Account account1100001 = new Account("1100-001", "Bank Transfers", 0L);
        account1100001.setDescription("Internal bank transfers");
        account1100001.setCategoryId(currentAssetsId);
        accounts.add(account1100001);

        Account account1101 = new Account("1101", "Bank - Savings Account", 0L);
        account1101.setDescription("Business savings account");
        account1101.setCategoryId(currentAssetsId);
        accounts.add(account1101);

        Account account1102 = new Account("1102", "Bank - Foreign Currency", 0L);
        account1102.setDescription("Foreign currency accounts");
        account1102.setCategoryId(currentAssetsId);
        accounts.add(account1102);

        Account account1200 = new Account("1200", "Accounts Receivable", 0L);
        account1200.setDescription("Money owed by customers");
        account1200.setCategoryId(currentAssetsId);
        accounts.add(account1200);

        Account account1300 = new Account("1300", "Inventory", 0L);
        account1300.setDescription("Stock and inventory items");
        account1300.setCategoryId(currentAssetsId);
        accounts.add(account1300);

        Account account1400 = new Account("1400", "Prepaid Expenses", 0L);
        account1400.setDescription("Expenses paid in advance");
        account1400.setCategoryId(currentAssetsId);
        accounts.add(account1400);

        Account account1500 = new Account("1500", "VAT Input", 0L);
        account1500.setDescription("VAT paid on purchases");
        account1500.setCategoryId(currentAssetsId);
        accounts.add(account1500);
    }    /**
     * Add NON-CURRENT ASSETS accounts (2000-2999)
     */
    private void addNonCurrentAssetAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long nonCurrentAssetsId = categoryIds.get("NON_CURRENT_ASSETS");
        Account account2000 = new Account("2000", "Property, Plant & Equipment", 0L);
        account2000.setDescription("Fixed assets at cost");
        account2000.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2000);

        // Sub-account for director loans
        Account account2000001 = new Account("2000-001", "Director Loan - Company Assist", 0L);
        account2000001.setDescription("Loan from director for company assistance");
        account2000001.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2000001);

        Account account2100 = new Account("2100", "Accumulated Depreciation", 0L);
        account2100.setDescription("Depreciation of fixed assets");
        account2100.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2100);

        Account account2200 = new Account("2200", "Investments", 0L);
        account2200.setDescription("Long-term investments");
        account2200.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2200);

        Account account2300 = new Account("2300", "Motor Vehicles", 0L);
        account2300.setDescription("Motor vehicles and transport equipment");
        account2300.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2300);

        Account account2400 = new Account("2400", "Furniture & Fixtures", 0L);
        account2400.setDescription("Furniture and fixtures");
        account2400.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2400);

        Account account2500 = new Account("2500", "Office Equipment", 0L);
        account2500.setDescription("Office equipment");
        account2500.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2500);

        Account account2600 = new Account("2600", "Computer Software", 0L);
        account2600.setDescription("Computer software");
        account2600.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2600);

        Account account2700 = new Account("2700", "Office Supplies", 0L);
        account2700.setDescription("Office supplies");
        account2700.setCategoryId(nonCurrentAssetsId);
        accounts.add(account2700);
    }
    
    /**
     * Add CURRENT LIABILITIES accounts (3000-3999)
     */
    private void addCurrentLiabilityAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long currentLiabilitiesId = categoryIds.get("CURRENT_LIABILITIES");
        Account account3000 = new Account("3000", "Accounts Payable", 0L);
        account3000.setDescription("Money owed to suppliers");
        account3000.setCategoryId(currentLiabilitiesId);
        accounts.add(account3000);

        Account account3100 = new Account("3100", "VAT Output", 0L);
        account3100.setDescription("VAT collected on sales");
        account3100.setCategoryId(currentLiabilitiesId);
        accounts.add(account3100);

        Account account3200 = new Account("3200", "PAYE Payable", 0L);
        account3200.setDescription("Pay-As-You-Earn tax payable");
        account3200.setCategoryId(currentLiabilitiesId);
        accounts.add(account3200);

        Account account3300 = new Account("3300", "UIF Payable", 0L);
        account3300.setDescription("Unemployment Insurance Fund payable");
        account3300.setCategoryId(currentLiabilitiesId);
        accounts.add(account3300);

        Account account3400 = new Account("3400", "SDL Payable", 0L);
        account3400.setDescription("Skills Development Levy payable");
        account3400.setCategoryId(currentLiabilitiesId);
        accounts.add(account3400);

        Account account3500 = new Account("3500", "Accrued Expenses", 0L);
        account3500.setDescription("Expenses incurred but not yet paid");
        account3500.setCategoryId(currentLiabilitiesId);
        accounts.add(account3500);
    }
    
    /**
     * Add NON-CURRENT LIABILITIES accounts (4000-4999)
     */
    private void addNonCurrentLiabilityAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long nonCurrentLiabilitiesId = categoryIds.get("NON_CURRENT_LIABILITIES");
        Account account4000 = new Account("4000", "Long-term Loans", 0L);
        account4000.setDescription("Long-term debt obligations");
        account4000.setCategoryId(nonCurrentLiabilitiesId);
        accounts.add(account4000);
    }

    /**
     * Add EQUITY accounts (5000-5999)
     */
    private void addEquityAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long equityId = categoryIds.get("EQUITY");
        Account account5000 = new Account("5000", "Share Capital", 0L);
        account5000.setDescription("Issued share capital");
        account5000.setCategoryId(equityId);
        accounts.add(account5000);

        Account account5100 = new Account("5100", "Retained Earnings", 0L);
        account5100.setDescription("Accumulated profits");
        account5100.setCategoryId(equityId);
        accounts.add(account5100);

        Account account5200 = new Account("5200", "Current Year Earnings", 0L);
        account5200.setDescription("Current year profit/loss");
        account5200.setCategoryId(equityId);
        accounts.add(account5200);

        Account account5300 = new Account("5300", "Opening Balance Equity", 0L);
        account5300.setDescription("Temporary equity account for opening balances - Cash Flow Statement only");
        account5300.setCategoryId(equityId);
        accounts.add(account5300);
    }
    
    /**
     * Add OPERATING REVENUE accounts (6000-6999)
     */
    private void addOperatingRevenueAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long operatingRevenueId = categoryIds.get("OPERATING_REVENUE");
        Account account6000 = new Account("6000", "Sales Revenue", 0L);
        account6000.setDescription("Revenue from sales");
        account6000.setCategoryId(operatingRevenueId);
        accounts.add(account6000);

        Account account6100 = new Account("6100", "Service Revenue", 0L);
        account6100.setDescription("Revenue from services");
        account6100.setCategoryId(operatingRevenueId);
        accounts.add(account6100);

        // Sub-account for specific service revenue
        Account account6100001 = new Account("6100-001", "Corobrik Service Revenue", 0L);
        account6100001.setDescription("Service revenue from Corobrik");
        account6100001.setCategoryId(operatingRevenueId);
        accounts.add(account6100001);

        Account account6200 = new Account("6200", "Other Operating Revenue", 0L);
        account6200.setDescription("Other operating income");
        account6200.setCategoryId(operatingRevenueId);
        accounts.add(account6200);
    }

    /**
     * Add OTHER INCOME accounts (7000-7999)
     */
    private void addOtherIncomeAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long otherIncomeId = categoryIds.get("OTHER_INCOME");
        Account account7000 = new Account("7000", "Interest Income", 0L);
        account7000.setDescription("Interest earned on investments");
        account7000.setCategoryId(otherIncomeId);
        accounts.add(account7000);

        Account account7100 = new Account("7100", "Dividend Income", 0L);
        account7100.setDescription("Dividends received");
        account7100.setCategoryId(otherIncomeId);
        accounts.add(account7100);

        Account account7200 = new Account("7200", "Gain on Asset Disposal", 0L);
        account7200.setDescription("Profit from asset sales");
        account7200.setCategoryId(otherIncomeId);
        accounts.add(account7200);
    }
    
    /**
     * Add OPERATING EXPENSES accounts (8000-8999)
     */
    private void addOperatingExpenseAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long operatingExpensesId = categoryIds.get("OPERATING_EXPENSES");
        Account account8000 = new Account("8000", "Cost of Goods Sold", 0L);
        account8000.setDescription("Direct costs of products sold");
        account8000.setCategoryId(operatingExpensesId);
        accounts.add(account8000);

        Account account8100 = new Account("8100", "Employee Costs", 0L);
        account8100.setDescription("Salaries, wages and benefits");
        account8100.setCategoryId(operatingExpensesId);
        accounts.add(account8100);

        Account account8100001 = new Account("8100-001", "Director Remuneration", 0L);
        account8100001.setDescription("Director remuneration");
        account8100001.setCategoryId(operatingExpensesId);
        accounts.add(account8100001);

        Account account8200 = new Account("8200", "Rent Expense", 0L);
        account8200.setDescription("Office and facility rent");
        account8200.setCategoryId(operatingExpensesId);
        accounts.add(account8200);

        Account account8300 = new Account("8300", "Utilities", 0L);
        account8300.setDescription("Electricity, water, gas");
        account8300.setCategoryId(operatingExpensesId);
        accounts.add(account8300);

        Account account8400 = new Account("8400", "Communication", 0L);
        account8400.setDescription("Telephone, internet, postage");
        account8400.setCategoryId(operatingExpensesId);
        accounts.add(account8400);

        Account account8500 = new Account("8500", "Motor Vehicle Expenses", 0L);
        account8500.setDescription("Vehicle running costs");
        account8500.setCategoryId(operatingExpensesId);
        accounts.add(account8500);

        // Sub-accounts for vehicle tracking services
        Account account8500001 = new Account("8500-001", "Cartrack Vehicle Tracking", 0L);
        account8500001.setDescription("Cartrack tracking service fees");
        account8500001.setCategoryId(operatingExpensesId);
        accounts.add(account8500001);

        Account account8500002 = new Account("8500-002", "Netstar Vehicle Tracking", 0L);
        account8500002.setDescription("Netstar tracking service fees");
        account8500002.setCategoryId(operatingExpensesId);
        accounts.add(account8500002);

        Account account8600 = new Account("8600", "Travel & Entertainment", 0L);
        account8600.setDescription("Business travel and entertainment");
        account8600.setCategoryId(operatingExpensesId);
        accounts.add(account8600);

        // Sub-accounts for fuel expenses by station
        Account account8600001 = new Account("8600-001", "Fuel Expenses - BP Stations", 0L);
        account8600001.setDescription("Fuel purchases at BP");
        account8600001.setCategoryId(operatingExpensesId);
        accounts.add(account8600001);

        Account account8600002 = new Account("8600-002", "Fuel Expenses - Shell Stations", 0L);
        account8600002.setDescription("Fuel purchases at Shell");
        account8600002.setCategoryId(operatingExpensesId);
        accounts.add(account8600002);

        Account account8600003 = new Account("8600-003", "Fuel Expenses - Sasol Stations", 0L);
        account8600003.setDescription("Fuel purchases at Sasol");
        account8600003.setCategoryId(operatingExpensesId);
        accounts.add(account8600003);

        Account account8600004 = new Account("8600-004", "Engen Fuel Expenses", 0L);
        account8600004.setDescription("Fuel purchases at Engen");
        account8600004.setCategoryId(operatingExpensesId);
        accounts.add(account8600004);

        Account account8600099 = new Account("8600-099", "Fuel Expenses - Other Stations", 0L);
        account8600099.setDescription("Fuel purchases at other stations");
        account8600099.setCategoryId(operatingExpensesId);
        accounts.add(account8600099);

        Account account8700 = new Account("8700", "Professional Services", 0L);
        account8700.setDescription("Legal, accounting, consulting");
        account8700.setCategoryId(operatingExpensesId);
        accounts.add(account8700);

        Account account8710 = new Account("8710", "Suppliers Expense", 0L);
        account8710.setDescription("Payments to suppliers and vendors");
        account8710.setCategoryId(operatingExpensesId);
        accounts.add(account8710);

        Account account8720 = new Account("8720", "HR Management Expense", 0L);
        account8720.setDescription("Human resources management and recruitment");
        account8720.setCategoryId(operatingExpensesId);
        accounts.add(account8720);

        Account account8730 = new Account("8730", "Education & Training", 0L);
        account8730.setDescription("Education fees and training costs");
        account8730.setCategoryId(operatingExpensesId);
        accounts.add(account8730);

        Account account8800 = new Account("8800", "Insurance", 0L);
        account8800.setDescription("Business insurance premiums");
        account8800.setCategoryId(operatingExpensesId);
        accounts.add(account8800);

        // Sub-accounts for insurance providers
        Account account8800001 = new Account("8800-001", "King Price Insurance Premiums", 0L);
        account8800001.setDescription("King Price insurance premiums");
        account8800001.setCategoryId(operatingExpensesId);
        accounts.add(account8800001);

        Account account8800002 = new Account("8800-002", "DOTSURE Insurance Premiums", 0L);
        account8800002.setDescription("DOTSURE insurance premiums");
        account8800002.setCategoryId(operatingExpensesId);
        accounts.add(account8800002);

        Account account8800003 = new Account("8800-003", "OUTSurance Insurance Premiums", 0L);
        account8800003.setDescription("OUTSurance insurance premiums");
        account8800003.setCategoryId(operatingExpensesId);
        accounts.add(account8800003);

        Account account8800004 = new Account("8800-004", "MIWAY Insurance Premiums", 0L);
        account8800004.setDescription("MIWAY insurance premiums");
        account8800004.setCategoryId(operatingExpensesId);
        accounts.add(account8800004);

        Account account8800005 = new Account("8800-005", "Liberty Insurance Premiums", 0L);
        account8800005.setDescription("Liberty insurance premiums");
        account8800005.setCategoryId(operatingExpensesId);
        accounts.add(account8800005);

        Account account8800006 = new Account("8800-006", "Badger Insurance Premiums", 0L);
        account8800006.setDescription("Badger insurance premiums");
        account8800006.setCategoryId(operatingExpensesId);
        accounts.add(account8800006);

        Account account8800999 = new Account("8800-999", "Other Insurance Premiums", 0L);
        account8800999.setDescription("Other insurance providers");
        account8800999.setCategoryId(operatingExpensesId);
        accounts.add(account8800999);

        Account account8900 = new Account("8900", "Repairs & Maintenance", 0L);
        account8900.setDescription("Equipment and facility maintenance");
        account8900.setCategoryId(operatingExpensesId);
        accounts.add(account8900);
    }
    
    /**
     * Add ADMINISTRATIVE EXPENSES accounts (9000-9999)
     */
    private void addAdministrativeExpenseAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long adminExpensesId = categoryIds.get("ADMINISTRATIVE_EXPENSES");
        Account account9000 = new Account("9000", "Office Supplies", 0L);
        account9000.setDescription("Stationery and office materials");
        account9000.setCategoryId(adminExpensesId);
        accounts.add(account9000);

        Account account9100 = new Account("9100", "Computer Expenses", 0L);
        account9100.setDescription("Software licenses and IT costs");
        account9100.setCategoryId(adminExpensesId);
        accounts.add(account9100);

        Account account9200 = new Account("9200", "Marketing & Advertising", 0L);
        account9200.setDescription("Promotional and marketing costs");
        account9200.setCategoryId(adminExpensesId);
        accounts.add(account9200);

        Account account9300 = new Account("9300", "Training & Development", 0L);
        account9300.setDescription("Staff training and development");
        account9300.setCategoryId(adminExpensesId);
        accounts.add(account9300);

        Account account9400 = new Account("9400", "Depreciation", 0L);
        account9400.setDescription("Depreciation of fixed assets");
        account9400.setCategoryId(adminExpensesId);
        accounts.add(account9400);
    }

    /**
     * Add FINANCE COSTS accounts (9500-9999)
     */
    private void addFinanceCostAccounts(List<Account> accounts, Map<String, Long> categoryIds) {
        Long financeCostsId = categoryIds.get("FINANCE_COSTS");
        Account account9500 = new Account("9500", "Interest Expense", 0L);
        account9500.setDescription("Interest on loans and credit");
        account9500.setCategoryId(financeCostsId);
        accounts.add(account9500);

        Account account9600 = new Account("9600", "Bank Charges", 0L);
        account9600.setDescription("Bank fees and transaction costs");
        account9600.setCategoryId(financeCostsId);
        accounts.add(account9600);

        Account account9700 = new Account("9700", "Foreign Exchange Loss", 0L);
        account9700.setDescription("Loss on currency exchange");
        account9700.setCategoryId(financeCostsId);
        accounts.add(account9700);

        Account account9800 = new Account("9800", "VAT Payments to SARS", 0L);
        account9800.setDescription("VAT payments made to South African Revenue Service");
        account9800.setCategoryId(financeCostsId);
        accounts.add(account9800);

        Account account9810 = new Account("9810", "Loan Repayments", 0L);
        account9810.setDescription("Loan repayment costs");
        account9810.setCategoryId(financeCostsId);
        accounts.add(account9810);

        Account account9820 = new Account("9820", "PAYE Expense", 0L);
        account9820.setDescription("PAYE tax payments to South African Revenue Service");
        account9820.setCategoryId(financeCostsId);
        accounts.add(account9820);

        Account account9900 = new Account("9900", "Pension Expenses", 0L);
        account9900.setDescription("Pension-related costs");
        account9900.setCategoryId(financeCostsId);
        accounts.add(account9900);
    }
    
    /**
     * Analyze existing transaction patterns and suggest account mappings
     */
    private void analyzeTransactionPatterns(Long companyId) {
        System.out.println("\n🔍 Analyzing transaction patterns for account mapping...");
        System.out.println("ℹ️ Transaction pattern analysis available in legacy implementation");
        System.out.println("ℹ️ Use SpringTransactionClassificationService for Spring-based analysis");
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
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) {
            System.err.println("Company not found: " + companyId);
            return;
        }
        
        Company company = companyOpt.get();
        printReportHeader(company);
        displayAccountCategories(companyId);
    }
    
    private void printReportHeader(Company company) {
        System.out.println("\n📈 ACCOUNT CLASSIFICATION REPORT");
        System.out.println("Company: " + company.getName());
        System.out.println("Generated: " + java.time.LocalDateTime.now());
        System.out.println("=".repeat(DISPLAY_WIDTH_REPORT));
    }
    
    private void displayAccountCategories(Long companyId) {
        List<AccountCategory> categories = accountCategoryRepository.findByCompanyId(companyId);
        
        // Group by account type
        Map<AccountType, List<AccountCategory>> categoriesByType = categories.stream()
            .collect(java.util.stream.Collectors.groupingBy(AccountCategory::getAccountType));
        
        for (AccountType accountType : AccountType.values()) {
            List<AccountCategory> typeCategories = categoriesByType.get(accountType);
            if (typeCategories != null && !typeCategories.isEmpty()) {
                System.out.println("\n" + accountType.getDescription().toUpperCase() + ":");
                for (AccountCategory category : typeCategories) {
                    List<Account> accounts = accountRepository.findByCompanyIdAndCategoryId(companyId, category.getId());
                    System.out.printf("  %-30s %3d accounts%n", category.getName(), accounts.size());
                }
            }
        }
    }
    
    /**
     * Classify all unclassified transactions for a company based on mapping rules
     *
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions classified
     */
    @Transactional
    public int classifyAllUnclassifiedTransactions(Long companyId, String username) {
        List<BankTransaction> unclassifiedTransactions = bankTransactionRepository.findUnclassifiedTransactions(companyId);
        int classifiedCount = 0;

        for (BankTransaction transaction : unclassifiedTransactions) {
            Optional<Account> matchingAccount = findMatchingAccount(transaction.getCompanyId(), transaction.getDetails());
            if (matchingAccount.isPresent()) {
                Account account = matchingAccount.get();
                transaction.setAccountCode(account.getAccountCode());
                transaction.setUpdatedAt(LocalDateTime.now());
                transaction.setUpdatedBy(username);
                bankTransactionRepository.save(transaction);
                classifiedCount++;
            }
        }

        return classifiedCount;
    }

    /**
     * Reclassify ALL transactions (including already classified ones) based on current mapping rules.
     *
     * @param companyId The company ID
     * @param username The username for audit purposes
     * @return The number of transactions reclassified
     */
    @Transactional
    public int reclassifyAllTransactions(Long companyId, String username) {
        List<BankTransaction> allTransactions = bankTransactionRepository.findByCompanyId(companyId);
        int reclassifiedCount = 0;

        for (BankTransaction transaction : allTransactions) {
            Optional<Account> matchingAccount = findMatchingAccount(transaction.getCompanyId(), transaction.getDetails());
            if (matchingAccount.isPresent()) {
                Account account = matchingAccount.get();
                transaction.setAccountCode(account.getAccountCode());
                transaction.setUpdatedAt(LocalDateTime.now());
                transaction.setUpdatedBy(username);
                bankTransactionRepository.save(transaction);
                reclassifiedCount++;
            }
        }

        return reclassifiedCount;
    }

    /**
     * Classify a single transaction with the provided account code and name
     *
     * @param transaction The transaction to classify
     * @param accountCode The account code to assign
     * @param accountName The account name to assign
     * @return true if classification was successful
     */
    @Transactional
    public boolean classifyTransaction(BankTransaction transaction, String accountCode, String accountName) {
        try {
            transaction.setAccountCode(accountCode);
            transaction.setUpdatedAt(LocalDateTime.now());
            bankTransactionRepository.save(transaction);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error classifying transaction: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find matching account for transaction details using mapping rules
     */
    private Optional<Account> findMatchingAccount(Long companyId, String details) {
        List<TransactionMappingRule> rules = transactionMappingRuleRepository
            .findByCompanyIdAndIsActiveOrderByPriorityDesc(companyId, true);

        for (TransactionMappingRule rule : rules) {
            if (matchesRule(details, rule)) {
                return accountRepository.findByCompanyIdAndAccountCode(companyId, extractAccountCodeFromRule(rule));
            }
        }

        return Optional.empty();
    }

    private boolean matchesRule(String details, TransactionMappingRule rule) {
        String matchValue = rule.getMatchValue();
        return switch (rule.getMatchType()) {
            case CONTAINS -> details.toLowerCase().contains(matchValue.toLowerCase());
            case STARTS_WITH -> details.toLowerCase().startsWith(matchValue.toLowerCase());
            case ENDS_WITH -> details.toLowerCase().endsWith(matchValue.toLowerCase());
            case REGEX -> details.matches(matchValue);
            case EQUALS -> details.equalsIgnoreCase(matchValue);
        };
    }

    private String extractAccountCodeFromRule(TransactionMappingRule rule) {
        // Extract account code from description if embedded
        String description = rule.getDescription();
        int startIndex = description.indexOf("[AccountCode:");
        if (startIndex != -1) {
            int endIndex = description.indexOf("]", startIndex);
            if (endIndex != -1) {
                return description.substring(startIndex + 13, endIndex);
            }
        }
        return rule.getDescription(); // fallback
    }
    /**
     * Get the standard set of transaction mapping rules for South African business classification.
     * These rules are designed to classify bank transactions into appropriate SARS-compliant accounts.
     *
     * @return List of TransactionMappingRule objects with embedded account codes
     */
    public List<TransactionMappingRule> getStandardMappingRules() {
        List<TransactionMappingRule> rules = new ArrayList<>();
        
        // Add rules by priority level (highest to lowest)
        addCriticalPatternsRules(rules);
        addHighConfidencePatternsRules(rules);
        addGenericPaymentPatternsRules(rules);
        addFallbackPatternsRules(rules);
        
        // Sort by priority (descending) to ensure highest priority rules are checked first
        rules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));
        
        return rules;
    }
    
    /**
     * Add PRIORITY 10: CRITICAL PATTERNS (Must match before generic patterns)
     */
    /**
     * Add PRIORITY 10: CRITICAL PATTERNS (must match before all others)
     */
    private void addCriticalPatternsRules(List<TransactionMappingRule> rules) {
        addCriticalSalaryAndLoanRules(rules);
        addCriticalRevenueRules(rules);
        addCriticalEmployeePaymentRules(rules);
        addCriticalBankChargeRules(rules);
    }

    /**
     * Add critical salary and loan repayment rules
     */
    private void addCriticalSalaryAndLoanRules(List<TransactionMappingRule> rules) {
        // HIGH PRIORITY: Salary payments to specific employees
        // NOTE: "INSURANCE CHAUKE" contains keyword "INSURANCE" but is actually a salary payment
        // This rule MUST come before generic insurance pattern to avoid misclassification
        rules.add(createRule(
            "Insurance Chauke Salaries",
            "Salary payments to Insurance Chauke - prioritize over insurance pattern",
            TransactionMappingRule.MatchType.CONTAINS,
            "INSURANCE CHAUKE",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));

        // Loan repayments from directors/employees
        rules.add(createRule(
            "Jeffrey Maphosa Loan Repayment",
            "Loan repayments from Jeffrey Maphosa to company loan assist account",
            TransactionMappingRule.MatchType.CONTAINS,
            "JEFFREY MAPHOSA LOAN",
            "4000", // Long-term Loans
            PRIORITY_CRITICAL
        ));

        // Director reimbursements (for personal credit card expenses paid by company)
        rules.add(createRule(
            "Stone Jeffrey Maphosa Reimbursement",
            "Director reimbursements for personal credit card expenses (Company Assist loan)",
            TransactionMappingRule.MatchType.REGEX,
            "STONE JEFFR.*MAPHOSA.*(REIMBURSE|REPAYMENT)",
            "4000", // Long-term Loans
            PRIORITY_CRITICAL
        ));
    }

    /**
     * Add critical revenue transaction rules
     */
    private void addCriticalRevenueRules(List<TransactionMappingRule> rules) {
        // COROBRIK customer payments (revenue)
        rules.add(createRule(
            "Corobrik Service Revenue",
            "Credit Transfer ... Corobrik",
            TransactionMappingRule.MatchType.CONTAINS,
            "COROBRIK",
            "6100-001", // Corobrik Service Revenue
            PRIORITY_CRITICAL
        ));
    }

    /**
     * Add critical employee payment rules
     */
    private void addCriticalEmployeePaymentRules(List<TransactionMappingRule> rules) {
        addCriticalDirectorPayments(rules);
        addCriticalEmployeePayments(rules);
        addCriticalSupplierPayments(rules);
    }
    
    private void addCriticalDirectorPayments(List<TransactionMappingRule> rules) {
        // IMMEDIATE PAYMENT - specific director payments (high priority)
        rules.add(createRule(
            "Immediate Payment - Jeffrey S Maphosa",
            "Director remuneration payment to Jeffrey S Maphosa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224812909 JEFFREY S MAPHOSA",
            "8100-001", // Director Remuneration
            PRIORITY_CRITICAL
        ));
    }
    
    private void addCriticalEmployeePayments(List<TransactionMappingRule> rules) {
        addCriticalEmployeePaymentsGroup1(rules);
        addCriticalEmployeePaymentsGroup2(rules);
    }

    private void addCriticalEmployeePaymentsGroup1(List<TransactionMappingRule> rules) {
        // IMMEDIATE PAYMENT - specific employee payments (high priority)
        rules.add(createRule(
            "Immediate Payment - Katleho Mogaloa",
            "Employee payment to Katleho Mogaloa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 226159243 KATLEHO MOGALOA",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));

        rules.add(createRule(
            "Immediate Payment - Jordan Moyane",
            "Employee payment to Jordan Moyane",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224812154 JORDAN MOYANE",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));

        rules.add(createRule(
            "Immediate Payment - Sibongile Dlamini",
            "Employee payment to Sibongile Dlamini",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224850901 SIBONGILE DLAMINI",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));
    }

    private void addCriticalEmployeePaymentsGroup2(List<TransactionMappingRule> rules) {
        rules.add(createRule(
            "Immediate Payment - Mabunda IP",
            "Employee payment to Mabunda IP",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 224855113 MABUNDA IP",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));

        rules.add(createRule(
            "Immediate Payment - Albert Zunga",
            "Employee payment to Albert Zunga",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 221599858 ALBERT ZUNGA",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));

        rules.add(createRule(
            "Immediate Payment - Piet Mathebula",
            "Employee payment to Piet Mathebula",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 221514559 PIET MATHEBULA",
            "8100", // Employee Costs
            PRIORITY_CRITICAL
        ));
    }
    
    private void addCriticalSupplierPayments(List<TransactionMappingRule> rules) {
        rules.add(createRule(
            "Immediate Payment - Alberake Protection",
            "Cost of goods sold - Alberake Protection",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 218989372 ALBERAKE PROTECTION",
            "8000", // Cost of Goods Sold
            PRIORITY_CRITICAL
        ));
    }

    /**
     * Add critical bank charge rules
     */
    private void addCriticalBankChargeRules(List<TransactionMappingRule> rules) {
        // FEE IMMEDIATE PAYMENT - bank charges (high priority)
        rules.add(createRule(
            "Fee Immediate Payment",
            "Bank charges for immediate payment processing",
            TransactionMappingRule.MatchType.CONTAINS,
            "FEE IMMEDIATE PAYMENT",
            "9600", // Bank Charges
            PRIORITY_CRITICAL
        ));
    }
    
    /**
     * Add PRIORITY 9: HIGH-CONFIDENCE PATTERNS
     */
    private void addHighConfidencePatternsRules(List<TransactionMappingRule> rules) {
        addReturnedDebitsAndInterestRules(rules);
        addBondAndLoanRepaymentRules(rules);
        addFuelAndTrackingRules(rules);
        addInstantMoneyAndTransferRules(rules);
        addEmployeePaymentRules(rules);
        addEmployeeBenefitRules(rules);
        addDirectorAndPensionRules(rules);
        addAllowanceAndTaxRules(rules);
        addCashAndReimbursementRules(rules);
        addCommunicationAndTrackingRules(rules);
        addLoanAndTransferRules(rules);
        addBalanceAndVatRules(rules);
        addProfessionalAndFuelRules(rules);
        addSpecificIndividualPaymentRules(rules);
    }

    private void addReturnedDebitsAndInterestRules(List<TransactionMappingRule> rules) {
        // Returned debits - offset original transactions
        // NOTE: These should go to the SAME account as the original transaction
        // For now, we'll handle specific known providers
        rules.add(createRule(
            "Returned Debit - DOTSURE",
            "Returned debit for DOTSURE insurance payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "RTD-DEBIT AGAINST PAYERS AUTH DOTSURE",
            "8800-002", // DOTSURE Insurance Premiums (offset)
            PRIORITY_HIGH
        ));

        // EXCESS INTEREST payments
        rules.add(createRule(
            "Excess Interest Expense",
            "Excess interest charges on loans/accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "EXCESS INTEREST",
            "9500", // Interest Expense
            PRIORITY_HIGH
        ));
    }

    private void addBondAndLoanRepaymentRules(List<TransactionMappingRule> rules) {
        // STD BANK BOND repayments (loans payable)
        rules.add(createRule(
            "STD Bank Bond Repayment",
            "STD Bank bond/home loan repayments",
            TransactionMappingRule.MatchType.CONTAINS,
            "STD BANK BOND",
            "4000", // Long-term Loans (Loans Payable)
            PRIORITY_HIGH
        ));
    }

    private void addFuelAndTrackingRules(List<TransactionMappingRule> rules) {
        // Fuel transfers FROM specific account
        rules.add(createRule(
            "IB Transfer From Fuel Account",
            "Internal bank transfers from fuel supplier account",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER FROM *****2689327",
            "8600-099", // Fuel Expenses - Other Stations
            PRIORITY_HIGH
        ));

        // CARTRACK vehicle tracking
        rules.add(createRule(
            "Cartrack Vehicle Tracking",
            "Cartrack vehicle tracking service fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "CARTRACK",
            "8500-001", // Cartrack Vehicle Tracking
            PRIORITY_HIGH
        ));
    }

    private void addInstantMoneyAndTransferRules(List<TransactionMappingRule> rules) {
        // IB INSTANT MONEY CASH TO - e-wallet payments to part-time employees
        rules.add(createRule(
            "IB Instant Money Cash to Employees",
            "E-wallet payments directly to part-time employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB INSTANT MONEY CASH TO",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        // AUTOBANK TRANSFER TO ACCOUNT - fuel expenses
        rules.add(createRule(
            "Autobank Transfer to Fuel Account",
            "Automated bank transfers to fuel supplier accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK TRANSFER TO ACCOUNT",
            "8600-099", // Fuel Expenses - Other Stations
            PRIORITY_HIGH
        ));

        // IMMEDIATE PAYMENT - generic employee payments (lower priority)
        rules.add(createRule(
            "Immediate Payment - Generic Employee",
            "Generic immediate payment to employees (name and surname pattern)",
            TransactionMappingRule.MatchType.REGEX,
            "IMMEDIATE PAYMENT \\d+ [A-Z]+ [A-Z]+",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));
    }

    private void addEmployeePaymentRules(List<TransactionMappingRule> rules) {
        addEmployeePaymentRulesGroup1(rules);
        addEmployeePaymentRulesGroup2(rules);
    }
    
    private void addEmployeePaymentRulesGroup1(List<TransactionMappingRule> rules) {
        // Additional employee payment patterns - Group 1
        rules.add(createRule(
            "Employee Payment - Sibongile Dlamini",
            "Employee payment to Sibongile Dlamini",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 167855420 SIBONGILE DLAMINI",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Themba Mkhatshwa",
            "Employee payment to Themba Mkhatshwa",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207008710 THEMBA MKHATSHWA",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Lawrence Phogole",
            "Employee payment to Lawrence Phogole",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207008307 LAWRENCE PHOGOLE",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Tlometsane Moraswi",
            "Employee payment to Tlometsane Moraswi",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207009081 TLOMETSANE MORASWI",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));
    }
    
    private void addEmployeePaymentRulesGroup2(List<TransactionMappingRule> rules) {
        // Additional employee payment patterns - Group 2
        rules.add(createRule(
            "Employee Payment - Mbhoni Miyambo",
            "Employee payment to Mbhoni Miyambo",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 204543849 MBHONI MIYAMBO",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Musa Nzunza",
            "Employee payment to Musa Nzunza",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 156658792 MUSA NZUNZA",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Masemola Matawaneng",
            "Employee payment to Masemola Matawaneng",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207009987 MASEMOLA MATAWANENG",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Employee Payment - Winners Chauke",
            "Employee payment to Winners Chauke",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT 207010671 WINNERS CHAUKE",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));
    }

    private void addEmployeeBenefitRules(List<TransactionMappingRule> rules) {
        // Additional high-confidence rules to reach 100 total
        rules.add(createRule(
            "Medical Aid Contributions",
            "Medical aid scheme contributions for employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "MEDICAL AID",
            "8900", // Medical Aid
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "UIF Contributions",
            "Unemployment Insurance Fund contributions",
            TransactionMappingRule.MatchType.CONTAINS,
            "UIF CONTRIBUTION",
            "9820", // UIF Expense
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Provident Fund Contributions",
            "Provident fund retirement contributions",
            TransactionMappingRule.MatchType.CONTAINS,
            "PROVIDENT FUND",
            "9900", // Provident Fund
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Cellphone Expenses",
            "Cellphone and mobile communication expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "CELLPHONE",
            "8400", // Communication
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Internet Expenses",
            "Internet service provider expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "INTERNET",
            "8400", // Communication
            PRIORITY_HIGH
        ));
    }

    private void addDirectorAndPensionRules(List<TransactionMappingRule> rules) {
        // Director payments
        rules.add(createRule(
            "Director Payment - DB Nkuna",
            "Director remuneration payment to DB Nkuna",
            TransactionMappingRule.MatchType.CONTAINS,
            "DB NKUNA",
            "8100-001", // Director Remuneration
            PRIORITY_HIGH
        ));

        // Pension contributions
        rules.add(createRule(
            "Pension Fund Contributions",
            "Pension fund contributions for employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "PENSION FUND CONTRIBUTION",
            "9900", // Pension Expenses
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Pension Fund Transfers",
            "Pension fund transfers and contributions",
            TransactionMappingRule.MatchType.CONTAINS,
            "DEBIT TRANSFER FAW",
            "9900", // Pension Expenses
            PRIORITY_HIGH
        ));

        // Training expenses
        rules.add(createRule(
            "OHS Training Expenses",
            "Occupational Health and Safety training expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "OHS TRAINING",
            "8730", // Education & Training
            PRIORITY_HIGH
        ));
    }

    private void addAllowanceAndTaxRules(List<TransactionMappingRule> rules) {
        // Petrol allowance
        rules.add(createRule(
            "Petrol Allowance",
            "Petrol allowance payments to employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "PETROL ALOWANCE",
            "8500", // Motor Vehicle Expenses
            PRIORITY_HIGH
        ));

        // PAYE payments to SARS
        rules.add(createRule(
            "PAYE Payments to SARS",
            "PAYE tax payments to South African Revenue Service",
            TransactionMappingRule.MatchType.CONTAINS,
            "PAYE-PAY-AS-",
            "9820", // PAYE Expense (tax expense)
            PRIORITY_HIGH
        ));
    }

    private void addCashAndReimbursementRules(List<TransactionMappingRule> rules) {
        addCashWithdrawalRules(rules);
        addReimbursementRules(rules);
    }

    private void addCashWithdrawalRules(List<TransactionMappingRule> rules) {
        // Cash withdrawals
        rules.add(createRule(
            "Cash Withdrawals",
            "ATM cash withdrawals",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK CASH WITHDRAWAL",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        // Stokvela payments
        rules.add(createRule(
            "Stokvela Payments",
            "Stokvela (savings club) payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "STOKVELA",
            "1000", // Petty Cash/Loans Receivable (asset accounts)
            PRIORITY_HIGH
        ));
    }

    private void addReimbursementRules(List<TransactionMappingRule> rules) {
        // Director reimbursements
        rules.add(createRule(
            "Director Reimbursements",
            "Director expense reimbursements",
            TransactionMappingRule.MatchType.CONTAINS,
            "REIMBURSE",
            "4000", // Long-term Loans (Director loans)
            PRIORITY_HIGH
        ));

        // Transport expenses
        rules.add(createRule(
            "Transport Expenses",
            "Transport and related expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "TRANSPORT",
            "8500", // Motor Vehicle Expenses
            PRIORITY_HIGH
        ));

        // Telephone expenses
        rules.add(createRule(
            "Telephone Expenses",
            "Telephone and communication expenses",
            TransactionMappingRule.MatchType.CONTAINS,
            "TELEPHONE",
            "8400", // Communication
            PRIORITY_HIGH
        ));
    }

    private void addCommunicationAndTrackingRules(List<TransactionMappingRule> rules) {
        // Vehicle tracking
        rules.add(createRule(
            "Netstar Vehicle Tracking",
            "Netstar vehicle tracking services",
            TransactionMappingRule.MatchType.CONTAINS,
            "NETSTAR",
            "8500-002", // Netstar Vehicle Tracking
            PRIORITY_HIGH
        ));

        // Loan income
        rules.add(createRule(
            "Loan Income",
            "Loans received from directors/associates",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT FROM",
            "2000-001", // Director Loan - Company Assist
            PRIORITY_HIGH
        ));
    }

    private void addLoanAndTransferRules(List<TransactionMappingRule> rules) {
        // Company transfers - specific patterns based on trailing descriptions
        rules.add(createRule(
            "Company Transfers - Company Assist",
            "Inter-company transfers for company assistance (loans)",
            TransactionMappingRule.MatchType.REGEX,
            "CREDIT TRANSFER.*COMPANY ASSIST",
            "4000", // Long-term Loans
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Company Transfers - TAU",
            "Inter-company transfers for TAU (stokvela contributions)",
            TransactionMappingRule.MatchType.REGEX,
            "CREDIT TRANSFER.*TAU",
            "1000-001", // Stokvela Contributions
            PRIORITY_HIGH
        ));

        // Returned debits (offset transactions)
        rules.add(createRule(
            "Returned Debits",
            "Returned debit orders (offset original transactions)",
            TransactionMappingRule.MatchType.REGEX,
            "RTD-.*",
            "8800", // Insurance (generic offset)
            PRIORITY_STANDARD
        ));
    }

    private void addBalanceAndVatRules(List<TransactionMappingRule> rules) {
        // BALANCE BROUGHT FORWARD - opening balance entries
        rules.add(createRule(
            "Balance Brought Forward",
            "Opening balance entries from previous periods",
            TransactionMappingRule.MatchType.CONTAINS,
            "BALANCE BROUGHT FORWARD",
            "5000", // Retained Earnings (opening balances)
            PRIORITY_HIGH
        ));

        // SARS VAT payments (CRITICAL: Must be expense, not liability adjustment)
        rules.add(createRule(
            "SARS VAT Payments",
            "VAT payments made to South African Revenue Service",
            TransactionMappingRule.MatchType.CONTAINS,
            "PAYMENT TO SARS-VAT",
            "9800", // VAT Payments to SARS (Expense)
            PRIORITY_HIGH
        ));
    }

    private void addProfessionalAndFuelRules(List<TransactionMappingRule> rules) {
        // Professional services (accounting, legal)
        rules.add(createRule(
            "Global Hope Financia Accounting",
            "Accounting services from Global Hope Financia",
            TransactionMappingRule.MatchType.CONTAINS,
            "GLOBAL HOPE FINACIA",
            "8700", // Professional Services
            PRIORITY_HIGH
        ));

        // Fuel purchases (specific account identifier)
        rules.add(createRule(
            "Fuel Purchase - Account 2689327",
            "Fuel purchases to supplier account 2689327 [AccountCode:8600-099]",
            TransactionMappingRule.MatchType.CONTAINS,
            "2689327",
            "8600-099", // Fuel Expenses - Other Stations
            PRIORITY_HIGH
        ));
    }

    private void addSpecificIndividualPaymentRules(List<TransactionMappingRule> rules) {
        addSpecificIBPaymentRules(rules);
        addSpecificImmediatePaymentRules(rules);
    }

    private void addSpecificIBPaymentRules(List<TransactionMappingRule> rules) {
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
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "IB Payment to NGWAKWANE E TAU XINGHIZANA",
            "IB payment to NGWAKWANE E TAU XINGHIZANA (director/employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT TO NGWAKWANE E TAU XINGHIZANA",
            "1000-001", // Stokvela Contributions
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "IB Payment to EUPHODIA TAU STOKFELA",
            "IB payment to EUPHODIA TAU STOKFELA (stokvela payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB PAYMENT TO EUPHODIA TAU STOKFELA",
            "1000-001", // Cash and Cash Equivalents (Stokvela)
            PRIORITY_HIGH
        ));
    }

    private void addSpecificImmediatePaymentRules(List<TransactionMappingRule> rules) {
        // IMMEDIATE PAYMENT to specific individuals
        rules.add(createRule(
            "Immediate Payment to JEFFREY MAPHOSA",
            "Immediate payment to JEFFREY MAPHOSA (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO JEFFREY MAPHOSA",
            "8100-001", // Director Remuneration
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Immediate Payment to NGWAKWANE E TAU",
            "Immediate payment to NGWAKWANE E TAU (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO NGWAKWANE E TAU",
            "1000-001", // Stokvela Contributions
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Immediate Payment to DAVID MOLEFE",
            "Immediate payment to DAVID MOLEFE (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO DAVID MOLEFE",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Immediate Payment to MUZIKAYISE ZUNGA",
            "Immediate payment to MUZIKAYISE ZUNGA (employee payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "IMMEDIATE PAYMENT TO MUZIKAYISE ZUNGA",
            "8100", // Employee Costs
            PRIORITY_HIGH
        ));
    }

    /**
     * Add PRIORITY 8: GENERIC PAYMENT PATTERNS
     */
    
    /**
     * Add PRIORITY 8: GENERIC PAYMENT PATTERNS
     */
    /**
     * Add PRIORITY 8: GENERIC PAYMENT PATTERNS
     */
    private void addGenericPaymentPatternsRules(List<TransactionMappingRule> rules) {
        addGenericPaymentRegexRules(rules);
        addCashDepositsAndTransfersRules(rules);
        addSupplierPaymentRules(rules);
        addEmployeeSalaryRules(rules);
    }

    /**
     * Add generic payment regex patterns
     */
    private void addGenericPaymentRegexRules(List<TransactionMappingRule> rules) {
        // Generic IB PAYMENT TO pattern (fallback for other individuals)
        rules.add(createRule(
            "IB Payment To - Generic",
            "Generic IB payment to individuals (fallback rule)",
            TransactionMappingRule.MatchType.REGEX,
            "IB PAYMENT TO [A-Z]+ [A-Z]+.*",
            "8100", // Employee Costs (generic fallback)
            PRIORITY_STANDARD
        ));

        // Generic IMMEDIATE PAYMENT pattern
        rules.add(createRule(
            "Immediate Payment - Generic",
            "Generic immediate payment to individuals",
            TransactionMappingRule.MatchType.REGEX,
            "IMMEDIATE PAYMENT [0-9]+ [A-Z]+ [A-Z]+.*",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));
    }

    /**
     * Add PRIORITY 7: CASH DEPOSITS AND TRANSFERS
     */
    private void addCashDepositsAndTransfersRules(List<TransactionMappingRule> rules) {
        // AUTOBANK CASH DEPOSIT (cash inflows - revenue)
        rules.add(createRule(
            "Autobank Cash Deposit - Generic",
            "Cash deposits through autobank (revenue)",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK CASH DEPOSIT",
            "1000", // Other
            PRIORITY_FALLBACK
        ));

        // Internal bank transfers (between own accounts)
        rules.add(createRule(
            "Bank Transfers - IB TRANSFER TO",
            "Internal bank transfers to other accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER TO",
            "1100-001", // Bank - Current Account
            PRIORITY_FALLBACK
        ));

        rules.add(createRule(
            "Bank Transfers - IB TRANSFER FROM",
            "Internal bank transfers from other accounts",
            TransactionMappingRule.MatchType.CONTAINS,
            "IB TRANSFER FROM",
            "1100-001", // Bank - Current Account
            PRIORITY_FALLBACK
        ));
    }

    /**
     * Add supplier and vendor payment rules
     */
    private void addSupplierPaymentRules(List<TransactionMappingRule> rules) {
        addRentAndPropertyRules(rules);
        addVehicleAndEquipmentRules(rules);
        addSupplierAndServiceRules(rules);
        addInvestmentAndEducationRules(rules);
    }

    private void addRentAndPropertyRules(List<TransactionMappingRule> rules) {
        // Rent payments
        rules.add(createRule(
            "Ellis Park Stadium Rent",
            "Rent payments to Ellis Park Stadium",
            TransactionMappingRule.MatchType.CONTAINS,
            "ELLISPARK STADIUM",
            "8200", // Rent Expense
            PRIORITY_HIGH
        ));
    }

    private void addVehicleAndEquipmentRules(List<TransactionMappingRule> rules) {
        // Vehicle purchases (capital expenditure)
        rules.add(createRule(
            "EBS Car Sales Vehicle Purchase",
            "Vehicle purchase from EBS Car Sales Mercedes",
            TransactionMappingRule.MatchType.CONTAINS,
            "EBS CAR SALES",
            "2000", // Property, Plant & Equipment
            PRIORITY_HIGH
        ));
    }

    private void addSupplierAndServiceRules(List<TransactionMappingRule> rules) {
        // Supplier payments
        rules.add(createRule(
            "Two Way Technologies Supplier",
            "Supplier payments to Two Way Technologies",
            TransactionMappingRule.MatchType.CONTAINS,
            "TWO WAY TECHNOLOGIES",
            "8710", // Suppliers Expense
            PRIORITY_HIGH
        ));

        rules.add(createRule(
            "Rent A Dog Supplier",
            "Supplier payments to Rent A Dog",
            TransactionMappingRule.MatchType.CONTAINS,
            "RENT A DOG",
            "8710", // Suppliers Expense
            PRIORITY_HIGH
        ));

        // HR Management expenses
        rules.add(createRule(
            "Neo Entle Labour Hire",
            "HR management and labour hire from Neo Entle Labour Hire",
            TransactionMappingRule.MatchType.CONTAINS,
            "NEO ENTLE LABOUR",
            "8720", // HR Management Expense
            PRIORITY_HIGH
        ));
    }

    private void addInvestmentAndEducationRules(List<TransactionMappingRule> rules) {
        // Investment transactions
        rules.add(createRule(
            "Stanlib Investment",
            "Investment transactions with Stanlib",
            TransactionMappingRule.MatchType.CONTAINS,
            "STANLIB",
            "2200", // Investments
            PRIORITY_HIGH
        ));

        // Cost of Goods Sold
        rules.add(createRule(
            "DB Projects COGS",
            "Cost of goods sold - DB Projects and Agencies",
            TransactionMappingRule.MatchType.CONTAINS,
            "DB PROJECTS",
            "8000", // Cost of Goods Sold
            PRIORITY_HIGH
        ));

        // Education/Training expenses
        rules.add(createRule(
            "Lyceum College School Fees",
            "School fees at Lyceum College",
            TransactionMappingRule.MatchType.CONTAINS,
            "LYCEUM COLLEGE",
            "8730", // Education & Training
            PRIORITY_HIGH
        ));
    }

    /**
     * Add employee salary payment rules
     */
    private void addEmployeeSalaryRules(List<TransactionMappingRule> rules) {
        // Employee salaries (specific names)
        rules.add(createRule(
            "Anthony Ndou Salary",
            "Salary payment to Anthony Ndou",
            TransactionMappingRule.MatchType.CONTAINS,
            "ANTHONY NDOU",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));

        rules.add(createRule(
            "Goodman Zunga Salary",
            "Salary payment to Goodman Zunga",
            TransactionMappingRule.MatchType.CONTAINS,
            "GOODMAN ZUNGA",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));

        // Salary payments (generic keywords)
        rules.add(createRule(
            "Salary Payments - XG SALARIES",
            "Standard salary payments with XG SALARIES keyword",
            TransactionMappingRule.MatchType.CONTAINS,
            "XG SALARIES",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));

        rules.add(createRule(
            "Salary Payments - SALARIES",
            "Standard salary payments with SALARIES keyword",
            TransactionMappingRule.MatchType.CONTAINS,
            "SALARIES",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));

        rules.add(createRule(
            "Salary Payments - WAGES",
            "Wage payments to employees",
            TransactionMappingRule.MatchType.CONTAINS,
            "WAGES",
            "8100", // Employee Costs
            PRIORITY_STANDARD
        ));
    }
    
    /**
     * Add PRIORITY 5-7: GENERIC/FALLBACK PATTERNS
     */
    private void addFallbackPatternsRules(List<TransactionMappingRule> rules) {
        addGenericFallbackPatternsRules(rules);
        addRemainingUnclassifiedPatternsRules(rules);
        addFinalRemainingPatternsRules(rules);
    }

    /**
     * Add PRIORITY 5: GENERIC/FALLBACK PATTERNS
     */
    private void addGenericFallbackPatternsRules(List<TransactionMappingRule> rules) {
        addEducationRules(rules);
        addInsuranceRules(rules);
        addBankChargeRules(rules);
        addLoanRules(rules);
    }

    private void addEducationRules(List<TransactionMappingRule> rules) {
        // Educational institutions (generic)
        rules.add(createRule(
            "Education Institutions - Generic",
            "Generic rule for college, school, and university fees",
            TransactionMappingRule.MatchType.REGEX,
            ".*(COLLEGE|SCHOOL|UNIVERSITY).*",
            "9300", // Training & Development
            PRIORITY_GENERIC
        ));
    }

    private void addInsuranceRules(List<TransactionMappingRule> rules) {
        // Insurance premiums (generic - MUST come after "Insurance Chauke" check)
        rules.add(createRule(
            "Insurance Premiums - Generic",
            "Insurance premium payments to providers",
            TransactionMappingRule.MatchType.CONTAINS,
            "INSURANCE",
            "8800", // Insurance
            PRIORITY_GENERIC
        ));

        rules.add(createRule(
            "Insurance Premiums - PREMIUM keyword",
            "Premium payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PREMIUM",
            "8800", // Insurance
            PRIORITY_GENERIC
        ));
    }

    private void addBankChargeRules(List<TransactionMappingRule> rules) {
        // Bank charges and fees - HIGHEST PRIORITY to override ALL other classifications
        rules.add(createRule(
            "Bank Charges - FEE keyword",
            "Bank fees and charges",
            TransactionMappingRule.MatchType.CONTAINS,
            "FEE",
            "9600", // Bank Charges
            PRIORITY_HIGHEST  // HIGHEST priority - overrides ALL other rules
        ));

        rules.add(createRule(
            "Bank Charges - SERVICE FEE",
            "Monthly service fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "SERVICE FEE",
            "9600", // Bank Charges
            PRIORITY_HIGHEST  // HIGHEST priority - overrides ALL other rules
        ));

        rules.add(createRule(
            "Bank Charges - CHARGE keyword",
            "Bank charges and fees",
            TransactionMappingRule.MatchType.CONTAINS,
            "CHARGE",
            "9600", // Bank Charges
            PRIORITY_HIGHEST  // HIGHEST priority - overrides ALL other rules
        ));
    }

    private void addLoanRules(List<TransactionMappingRule> rules) {
        // Loan payments (generic)
        rules.add(createRule(
            "Loan Payments - Generic",
            "Loan payments and repayments",
            TransactionMappingRule.MatchType.CONTAINS,
            "LOAN",
            "4000", // Long-term Loans
            PRIORITY_GENERIC
        ));
    }

    /**
     * Add PRIORITY 6: REMAINING UNCLASSIFIED PATTERNS
     */
    private void addRemainingUnclassifiedPatternsRules(List<TransactionMappingRule> rules) {
        addLoanCreditRules(rules);
        addPaymentFallbackRules(rules);
        addCollectionsRules(rules);
        addMobilePaymentRules(rules);
        addFinancialAdjustmentRules(rules);
    }

    private void addLoanCreditRules(List<TransactionMappingRule> rules) {
        // MAGTAPE CREDIT COMPANY ASSIST (loan/credit payments)
        rules.add(createRule(
            "MAGTAPE CREDIT COMPANY ASSIST",
            "Credit company assistance payments (loans)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT COMPANY ASSIST",
            "4000", // Long-term Loans
            PRIORITY_FALLBACK
        ));
    }

    private void addPaymentFallbackRules(List<TransactionMappingRule> rules) {
        // Generic IB PAYMENT TO (fallback for unspecified recipients)
        rules.add(createRule(
            "IB Payment To - Generic Fallback",
            "Generic IB payment to unspecified recipients (expense)",
            TransactionMappingRule.MatchType.REGEX,
            "^IB PAYMENT TO$",
            "8100", // Employee Costs (generic fallback)
            PRIORITY_FALLBACK
        ));

        // Generic IMMEDIATE PAYMENT (fallback for unspecified recipients)
        rules.add(createRule(
            "Immediate Payment - Generic Fallback",
            "Generic immediate payment to unspecified recipients",
            TransactionMappingRule.MatchType.REGEX,
            "^IMMEDIATE PAYMENT$",
            "8100", // Employee Costs (generic fallback)
            PRIORITY_FALLBACK
        ));
    }

    private void addCollectionsRules(List<TransactionMappingRule> rules) {
        // DEBIT TRANSFER (collections/payments)
        rules.add(createRule(
            "Debit Transfer - Collections",
            "DEBIT TRANSFER MIWAYCOLLE00000057007338240401",
            TransactionMappingRule.MatchType.CONTAINS,
            "MIWAY",
            "8800-004", // Miway Insurance Premiums
            PRIORITY_FALLBACK
        ));
    }

    private void addMobilePaymentRules(List<TransactionMappingRule> rules) {
        // Mobile phone payments (MTN, Vodacom prepaid)
        rules.add(createRule(
            "Mobile Phone Payments - MTN",
            "MTN prepaid mobile phone payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PRE-PAID PAYMENT TO MTN PREPAID",
            "8600", // Communications
            PRIORITY_FALLBACK
        ));

        rules.add(createRule(
            "Mobile Phone Payments - VOD",
            "Vodacom prepaid mobile phone payments",
            TransactionMappingRule.MatchType.CONTAINS,
            "PRE-PAID PAYMENT TO VOD PREPAID",
            "8600", // Communications
            PRIORITY_FALLBACK
        ));
    }

    private void addFinancialAdjustmentRules(List<TransactionMappingRule> rules) {
        // Interest adjustments/refunds (very small amounts)
        rules.add(createRule(
            "Interest Adjustment/Refund",
            "Bank interest adjustments and refunds",
            TransactionMappingRule.MatchType.CONTAINS,
            "INTEREST ADJUSTMENT/REFUND",
            "9500", // Interest Income
            PRIORITY_FALLBACK
        ));

        // Generic account payments
        rules.add(createRule(
            "Account Payment - Generic",
            "Generic account payments",
            TransactionMappingRule.MatchType.REGEX,
            "^ACCOUNT PAYMENT$",
            "8710", // Suppliers Expense (generic)
            PRIORITY_FALLBACK
        ));
    }

    /**
     * Add PRIORITY 7: FINAL REMAINING PATTERNS (100% CLASSIFICATION TARGET)
     */
    private void addFinalRemainingPatternsRules(List<TransactionMappingRule> rules) {
        addSpecificLoanPaymentRules(rules);
        addDepositRules(rules);
        addTransferRules(rules);
    }

    private void addSpecificLoanPaymentRules(List<TransactionMappingRule> rules) {
        // MAGTAPE CREDIT specific loan payments
        rules.add(createRule(
            "MAGTAPE CREDIT XINGHIZANA 13AUGLOA",
            "MAGTAPE credit payment to XINGHIZANA 13AUGLOA (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT XINGHIZANA 13AUGLOA",
            "4000", // Long-term Loans
            PRIORITY_LOW
        ));

        rules.add(createRule(
            "MAGTAPE CREDIT XG LOA MAPHOSA",
            "MAGTAPE credit payment XG LOA MAPHOSA (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT XG LOA MAPHOSA",
            "4000", // Long-term Loans
            PRIORITY_LOW
        ));

        rules.add(createRule(
            "MAGTAPE CREDIT 001 UNPAIDS/WEIERINGS CAPITEC",
            "MAGTAPE credit payment to UNPAIDS/WEIERINGS CAPITEC (loan payment)",
            TransactionMappingRule.MatchType.CONTAINS,
            "MAGTAPE CREDIT 001 UNPAIDS/WEIERINGS CAPITEC",
            "4000", // Long-term Loans
            PRIORITY_LOW
        ));
    }

    private void addDepositRules(List<TransactionMappingRule> rules) {
        // CASH DEPOSIT STOKFELA
        rules.add(createRule(
            "CASH DEPOSIT STOKFELA",
            "Cash deposit to stokvela account",
            TransactionMappingRule.MatchType.CONTAINS,
            "CASH DEPOSIT STOKFELA",
            "1000", // Cash and Cash Equivalents
            PRIORITY_LOW
        ));
    }

    private void addTransferRules(List<TransactionMappingRule> rules) {
        // AUTOBANK INSTANTMONEY CASH TO (cash withdrawal)
        rules.add(createRule(
            "AUTOBANK INSTANTMONEY CASH TO",
            "Cash withdrawal via Instant Money",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK INSTANTMONEY CASH TO",
            "8100", // Employee Costs
            PRIORITY_LOW
        ));

        // AUTOBANK TRANSFER FROM ACCOUNT
        rules.add(createRule(
            "AUTOBANK TRANSFER FROM ACCOUNT",
            "Bank transfer from another account",
            TransactionMappingRule.MatchType.CONTAINS,
            "AUTOBANK TRANSFER FROM ACCOUNT",
            "1100-001", // Bank - Current Account
            PRIORITY_LOW
        ));
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
     * Generate journal entries for classified transactions
     * TODO: Implement full journal entry generation logic
     */
    public int generateJournalEntriesForClassifiedTransactions(Long companyId, String createdBy) {
        // Find classified transactions that don't have journal entries yet
        List<BankTransaction> transactionsNeedingJournalEntries = bankTransactionRepository
            .findClassifiedTransactionsWithoutJournalEntries(companyId);

        System.out.println("🔍 DEBUG: Found " + transactionsNeedingJournalEntries.size() +
                          " classified transactions without journal entries for company " + companyId);

        if (transactionsNeedingJournalEntries.isEmpty()) {
            System.out.println("ℹ️ No classified transactions need journal entries for company ID: " + companyId);
            return 0;
        }

        System.out.println("🔄 Found " + transactionsNeedingJournalEntries.size() +
                          " classified transactions needing journal entries for company ID: " + companyId);

        int generatedCount = 0;
        
        for (BankTransaction transaction : transactionsNeedingJournalEntries) {
            try {
                // Get the classification account
                Account account = accountRepository.findByCompanyIdAndAccountCode(companyId, transaction.getAccountCode())
                    .orElse(null);
                
                if (account == null) {
                    System.err.println("⚠️ Account not found for code: " + transaction.getAccountCode());
                    continue;
                }
                
                // Determine bank account (default to "Bank Account" 1000)
                Account bankAccount = accountRepository.findByCompanyIdAndAccountCode(companyId, "1000")
                    .orElse(null);
                
                if (bankAccount == null) {
                    System.err.println("⚠️ Bank account (1000) not found");
                    continue;
                }
                
                // Create journal entry header
                JournalEntry journalEntry = new JournalEntry();
                journalEntry.setReference(transaction.getReference());
                journalEntry.setEntryDate(transaction.getTransactionDate());
                journalEntry.setDescription(transaction.getReference() + " - " + transaction.getCategory());
                journalEntry.setFiscalPeriodId(transaction.getFiscalPeriodId());
                journalEntry.setCompanyId(companyId);
                journalEntry.setCreatedBy(createdBy);
                journalEntry.setCreatedAt(LocalDateTime.now());
                journalEntry.setUpdatedAt(LocalDateTime.now());
                
                // Save journal entry header first to get ID
                journalEntry = journalEntryRepository.save(journalEntry);
                
                // Create journal entry lines (double-entry)
                // For deposits (positive amounts): Debit Bank, Credit classified account
                // For withdrawals (negative amounts): Debit classified account, Credit Bank
                
                if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    // Credit transaction (deposit): Debit Bank, Credit classified account
                    
                    // Line 1: Debit Bank Account
                    JournalEntryLine debitLine = new JournalEntryLine();
                    debitLine.setJournalEntryId(journalEntry.getId());
                    debitLine.setAccountId(bankAccount.getId());
                    debitLine.setDebitAmount(transaction.getCreditAmount());
                    debitLine.setCreditAmount(java.math.BigDecimal.ZERO);
                    debitLine.setDescription("[" + bankAccount.getAccountCode() + "] " + bankAccount.getAccountName());
                    debitLine.setSourceTransactionId(transaction.getId());
                    debitLine.setLineNumber(1);
                    journalEntryLineRepository.save(debitLine);
                    
                    // Line 2: Credit classified account
                    JournalEntryLine creditLine = new JournalEntryLine();
                    creditLine.setJournalEntryId(journalEntry.getId());
                    creditLine.setAccountId(account.getId());
                    creditLine.setDebitAmount(java.math.BigDecimal.ZERO);
                    creditLine.setCreditAmount(transaction.getCreditAmount());
                    creditLine.setDescription("[" + account.getAccountCode() + "] " + account.getAccountName());
                    creditLine.setSourceTransactionId(transaction.getId());
                    creditLine.setLineNumber(2);
                    journalEntryLineRepository.save(creditLine);
                    
                } else if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    // Debit transaction (withdrawal): Debit classified account, Credit Bank
                    
                    // Line 1: Debit classified account
                    JournalEntryLine debitLine = new JournalEntryLine();
                    debitLine.setJournalEntryId(journalEntry.getId());
                    debitLine.setAccountId(account.getId());
                    debitLine.setDebitAmount(transaction.getDebitAmount());
                    debitLine.setCreditAmount(java.math.BigDecimal.ZERO);
                    debitLine.setDescription("[" + account.getAccountCode() + "] " + account.getAccountName());
                    debitLine.setSourceTransactionId(transaction.getId());
                    debitLine.setLineNumber(1);
                    journalEntryLineRepository.save(debitLine);
                    
                    // Line 2: Credit Bank Account
                    JournalEntryLine creditLine = new JournalEntryLine();
                    creditLine.setJournalEntryId(journalEntry.getId());
                    creditLine.setAccountId(bankAccount.getId());
                    creditLine.setDebitAmount(java.math.BigDecimal.ZERO);
                    creditLine.setCreditAmount(transaction.getDebitAmount());
                    creditLine.setDescription("[" + bankAccount.getAccountCode() + "] " + bankAccount.getAccountName());
                    creditLine.setSourceTransactionId(transaction.getId());
                    creditLine.setLineNumber(2);
                    journalEntryLineRepository.save(creditLine);
                }
                
                generatedCount++;
                
            } catch (Exception e) {
                System.err.println("❌ Error creating journal entry for transaction " + transaction.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Generated " + generatedCount + " journal entries");
        return generatedCount;
    }

    /**
     * Update transaction classification with manual account selection.
     * Creates or updates journal entry with selected debit and credit accounts.
     * Based on the legacy console app's logic from DataManagementController.
     * 
     * @param companyId The company ID
     * @param transactionId The transaction to update
     * @param debitAccountId The debit account ID
     * @param creditAccountId The credit account ID
     * @param username The username of the person performing the manual classification
     * @throws IllegalArgumentException if validation fails
     */
    public void updateTransactionClassification(Long companyId, Long transactionId, 
                                               Long debitAccountId, Long creditAccountId, String username) {
        // Validate inputs
        if (companyId == null || transactionId == null || debitAccountId == null || creditAccountId == null) {
            throw new IllegalArgumentException("All parameters (companyId, transactionId, debitAccountId, creditAccountId) are required");
        }
        
        // Fetch and validate transaction
        BankTransaction transaction = bankTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found with ID: " + transactionId));
        
        if (!transaction.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Transaction does not belong to company " + companyId);
        }
        
        // Fetch and validate accounts
        Account debitAccount = accountRepository.findById(debitAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Debit account not found with ID: " + debitAccountId));
        
        Account creditAccount = accountRepository.findById(creditAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Credit account not found with ID: " + creditAccountId));
        
        if (!debitAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Debit account does not belong to company " + companyId);
        }
        
        if (!creditAccount.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Credit account does not belong to company " + companyId);
        }
        
        // Determine transaction amount
        java.math.BigDecimal amount;
        boolean isCreditTransaction = transaction.getCreditAmount() != null 
            && transaction.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0;
        
        if (isCreditTransaction) {
            amount = transaction.getCreditAmount();
        } else if (transaction.getDebitAmount() != null 
            && transaction.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            amount = transaction.getDebitAmount();
        } else {
            throw new IllegalArgumentException("Transaction has no valid amount");
        }
        
        // Check for existing journal entry lines
        List<JournalEntryLine> existingLines = journalEntryLineRepository.findBySourceTransactionId(transactionId);
        
        if (existingLines != null && !existingLines.isEmpty()) {
            // Update existing journal entry lines
            updateExistingJournalLines(existingLines, debitAccount, creditAccount, amount);
        } else {
            // Create new journal entry with lines
            createNewJournalEntry(transaction, companyId, debitAccount, creditAccount, amount, username);
        }
        
        System.out.println("✅ Updated transaction classification for transaction " + transactionId);
    }
    
    /**
     * Update existing journal entry lines with new account classifications.
     */
    private void updateExistingJournalLines(List<JournalEntryLine> lines, Account debitAccount, 
                                           Account creditAccount, java.math.BigDecimal amount) {
        JournalEntryLine debitLine = null;
        JournalEntryLine creditLine = null;
        
        // Identify debit and credit lines
        for (JournalEntryLine line : lines) {
            if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                debitLine = line;
            } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                creditLine = line;
            }
        }
        
        if (debitLine == null || creditLine == null) {
            throw new IllegalStateException("Could not identify debit and credit lines for update");
        }
        
        // Update debit line
        debitLine.setAccountId(debitAccount.getId());
        debitLine.setDescription("[" + debitAccount.getAccountCode() + "] " + debitAccount.getAccountName());
        debitLine.setDebitAmount(amount);
        journalEntryLineRepository.save(debitLine);
        
        // Update credit line
        creditLine.setAccountId(creditAccount.getId());
        creditLine.setDescription("[" + creditAccount.getAccountCode() + "] " + creditAccount.getAccountName());
        creditLine.setCreditAmount(amount);
        journalEntryLineRepository.save(creditLine);
    }
    
    /**
     * Create new journal entry with debit and credit lines.
     */
    private void createNewJournalEntry(BankTransaction transaction, Long companyId, 
                                      Account debitAccount, Account creditAccount, 
                                      java.math.BigDecimal amount, String username) {
        // Build description from account names: "DebitAccount - CreditAccount"
        String description = debitAccount.getAccountName() + " - " + creditAccount.getAccountName();
        
        // Create journal entry header
        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setCompanyId(companyId);
        journalEntry.setFiscalPeriodId(transaction.getFiscalPeriodId());
        journalEntry.setEntryDate(transaction.getTransactionDate());
        journalEntry.setDescription(description);
        journalEntry.setReference("MANUAL-" + transaction.getId());
        journalEntry.setCreatedBy(username != null ? username : "FIN");
        journalEntry.setCreatedAt(LocalDateTime.now());
        journalEntry.setUpdatedAt(LocalDateTime.now());
        journalEntry = journalEntryRepository.save(journalEntry);
        
        // Create debit line
        JournalEntryLine debitLine = new JournalEntryLine();
        debitLine.setJournalEntryId(journalEntry.getId());
        debitLine.setAccountId(debitAccount.getId());
        debitLine.setDebitAmount(amount);
        debitLine.setCreditAmount(java.math.BigDecimal.ZERO);
        debitLine.setDescription("[" + debitAccount.getAccountCode() + "] " + debitAccount.getAccountName());
        debitLine.setSourceTransactionId(transaction.getId());
        debitLine.setLineNumber(1);
        journalEntryLineRepository.save(debitLine);
        
        // Create credit line
        JournalEntryLine creditLine = new JournalEntryLine();
        creditLine.setJournalEntryId(journalEntry.getId());
        creditLine.setAccountId(creditAccount.getId());
        creditLine.setDebitAmount(java.math.BigDecimal.ZERO);
        creditLine.setCreditAmount(amount);
        creditLine.setDescription("[" + creditAccount.getAccountCode() + "] " + creditAccount.getAccountName());
        creditLine.setSourceTransactionId(transaction.getId());
        creditLine.setLineNumber(2);
        journalEntryLineRepository.save(creditLine);
        
        // Update transaction's account_code to reflect classification
        transaction.setAccountCode(debitAccount.getAccountCode());
        bankTransactionRepository.save(transaction);
    }

    /**
     * Generate journal entries for unclassified transactions
     * TODO: Implement full journal entry generation logic
     */
    public void generateJournalEntriesForUnclassifiedTransactions(Long companyId, String createdBy) {
        System.out.println("🔄 Journal entry generation for unclassified transactions - NOT YET IMPLEMENTED");
        System.out.println("ℹ️ This would create journal entries for transactions that haven't been classified yet");
    }
}
