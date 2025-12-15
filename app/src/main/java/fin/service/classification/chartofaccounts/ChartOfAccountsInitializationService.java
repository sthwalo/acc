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

package fin.service.classification.chartofaccounts;

import fin.entity.*;
import fin.repository.AccountCategoryRepository;
import fin.repository.AccountRepository;
import fin.repository.ChartOfAccountsTemplateRepository;
import fin.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for initializing and managing chart of accounts for companies.
 * Handles the creation of accounts from templates and category management.
 *
 * SINGLE RESPONSIBILITY: Chart of accounts initialization and management
 */
@Service
@Transactional
public class ChartOfAccountsInitializationService {

    private static final Logger LOGGER = Logger.getLogger(ChartOfAccountsInitializationService.class.getName());

    private final CompanyService companyService;
    private final AccountCategoryRepository accountCategoryRepository;
    private final AccountRepository accountRepository;
    private final ChartOfAccountsTemplateRepository chartOfAccountsTemplateRepository;

    public ChartOfAccountsInitializationService(CompanyService companyService,
                                             AccountCategoryRepository accountCategoryRepository,
                                             AccountRepository accountRepository,
                                             ChartOfAccountsTemplateRepository chartOfAccountsTemplateRepository) {
        this.companyService = companyService;
        this.accountCategoryRepository = accountCategoryRepository;
        this.accountRepository = accountRepository;
        this.chartOfAccountsTemplateRepository = chartOfAccountsTemplateRepository;
    }

    /**
     * Initialize chart of accounts for a company using industry templates
     */
    @Transactional
    public boolean initializeChartOfAccounts(Long companyId) {
        try {
            LOGGER.info("Initializing chart of accounts for company: " + companyId);

            // Validate company exists
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }

            if (company.getIndustryId() == null) {
                throw new IllegalArgumentException("Company must have an industry selected before initializing chart of accounts");
            }

            // Get chart of accounts templates for the company's industry
            List<ChartOfAccountsTemplate> templates = getChartOfAccountsTemplates(company.getIndustryId());

            if (templates.isEmpty()) {
                throw new IllegalStateException("No chart of accounts templates found for industry " + company.getIndustryId() +
                    ". Please ensure industry templates are populated in the database.");
            }

            // Create accounts from templates
            int accountsCreated = createAccountsFromTemplates(companyId, templates);

            LOGGER.info("Chart of accounts initialization completed successfully - created " + accountsCreated + " accounts");
            return true; // Success if process completed without errors, regardless of how many accounts were created

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts", e);
            System.err.println("❌ Error initializing chart of accounts: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get chart of accounts templates for a specific industry
     */
    private List<ChartOfAccountsTemplate> getChartOfAccountsTemplates(Long industryId) {
        return chartOfAccountsTemplateRepository.findByIndustryIdOrderByLevelAscAccountCodeAsc(industryId);
    }

    /**
     * Create accounts from templates for a company
     */
    private int createAccountsFromTemplates(Long companyId, List<ChartOfAccountsTemplate> templates) {
        int count = 0;
        for (ChartOfAccountsTemplate template : templates) {
            try {
                // Check if account already exists
                Optional<Account> existingAccountOpt = accountRepository.findByCompanyIdAndAccountCode(companyId, template.getAccountCode());
                if (existingAccountOpt.isPresent()) {
                    LOGGER.info("Account already exists: " + template.getAccountCode() + " for company " + companyId);
                    continue;
                }

                // Create new account from template
                Account newAccount = new Account();
                newAccount.setCompanyId(companyId);
                newAccount.setAccountCode(template.getAccountCode());
                newAccount.setAccountName(template.getAccountName());
                newAccount.setDescription(template.getDescription());
                newAccount.setActive(true);
                newAccount.setCreatedAt(java.time.LocalDateTime.now());
                newAccount.setUpdatedAt(java.time.LocalDateTime.now());

                accountRepository.save(newAccount);
                count++;

                LOGGER.info("Created account: " + template.getAccountCode() + " - " + template.getAccountName());

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to create account from template: " + template.getAccountCode(), e);
            }
        }
        return count;
    }

    /**
     * Determine account type from account code using IFRS conventions
     */
    private AccountType determineAccountTypeFromCode(String accountCode) {
        if (accountCode == null || accountCode.length() < 1) {
            return AccountType.ASSET;
        }

        char firstChar = accountCode.charAt(0);
        switch (firstChar) {
            case '1':
                return AccountType.ASSET;
            case '2':
                return AccountType.LIABILITY;
            case '3':
                return AccountType.EQUITY;
            case '4':
                return AccountType.REVENUE;
            default:
                return AccountType.EXPENSE;
        }
    }



    /**
     * Perform full initialization of a company's financial structure
     * This includes both chart of accounts AND mapping rules
     */
    @Transactional
    public boolean performFullInitialization(Long companyId) {
        try {
            LOGGER.info("Performing full financial structure initialization for company: " + companyId);

            // Initialize chart of accounts
            boolean chartSuccess = initializeChartOfAccounts(companyId);
            if (!chartSuccess) {
                LOGGER.warning("Chart of accounts initialization failed for company: " + companyId);
                return false;
            }

            // Note: Transaction mapping rules initialization is now handled by TransactionMappingRuleService

            LOGGER.info("Full financial structure initialization completed for company: " + companyId);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to perform full initialization for company: " + companyId, e);
            return false;
        }
    }
}