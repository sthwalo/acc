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

import fin.model.*;
import fin.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring Service for Account operations
 * Provides account management functionality using JPA repositories
 */
@Service
@Transactional
public class SpringAccountService {

    private final AccountRepository accountRepository;
    private final SpringCompanyService companyService;

    public SpringAccountService(AccountRepository accountRepository, SpringCompanyService companyService) {
        this.accountRepository = accountRepository;
        this.companyService = companyService;
    }

    /**
     * Create a new account
     */
    @Transactional
    public Account createAccount(String accountCode, String accountName, Long companyId,
                               Long categoryId, String description, Long parentAccountId) {
        // Validate inputs
        if (accountCode == null || accountCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Account code is required");
        }
        if (accountName == null || accountName.trim().isEmpty()) {
            throw new IllegalArgumentException("Account name is required");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID is required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if account code already exists for this company
        if (accountRepository.existsByCompanyIdAndAccountCode(companyId, accountCode.trim())) {
            throw new IllegalArgumentException("Account code already exists for this company: " + accountCode);
        }

        // Create account
        Account account = new Account();
        account.setAccountCode(accountCode.trim());
        account.setAccountName(accountName.trim());
        account.setDescription(description);
        account.setCategoryId(categoryId);
        account.setParentAccountId(parentAccountId);
        account.setCompanyId(companyId);
        account.setActive(true); // New accounts are active by default

        return accountRepository.save(account);
    }

    /**
     * Get all accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findByCompanyIdOrderByAccountCodeAsc(companyId);
    }

    /**
     * Get active accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getActiveAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    /**
     * Get account by ID
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccountById(Long id) {
        return accountRepository.findById(id);
    }

    /**
     * Get account by company and account code
     */
    @Transactional(readOnly = true)
    public Optional<Account> getAccountByCompanyAndCode(Long companyId, String accountCode) {
        if (companyId == null || accountCode == null) {
            throw new IllegalArgumentException("Company ID and account code are required");
        }
        return accountRepository.findByCompanyIdAndAccountCode(companyId, accountCode);
    }

    /**
     * Update an existing account
     */
    @Transactional
    public Account updateAccount(Long id, Account updatedAccount) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID is required");
        }
        if (updatedAccount == null) {
            throw new IllegalArgumentException("Updated account data is required");
        }

        Optional<Account> existingAccountOpt = accountRepository.findById(id);
        if (existingAccountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + id);
        }

        Account existingAccount = existingAccountOpt.get();

        // Validate account code uniqueness if changed
        if (!existingAccount.getAccountCode().equals(updatedAccount.getAccountCode())) {
            if (accountRepository.existsByCompanyIdAndAccountCode(existingAccount.getCompanyId(), updatedAccount.getAccountCode())) {
                throw new IllegalArgumentException("Account code already exists for this company: " + updatedAccount.getAccountCode());
            }
        }

        // Update fields
        existingAccount.setAccountCode(updatedAccount.getAccountCode());
        existingAccount.setAccountName(updatedAccount.getAccountName());
        existingAccount.setDescription(updatedAccount.getDescription());
        existingAccount.setCategoryId(updatedAccount.getCategoryId());
        existingAccount.setParentAccountId(updatedAccount.getParentAccountId());
        existingAccount.setActive(updatedAccount.isActive());

        return accountRepository.save(existingAccount);
    }

    /**
     * Delete an account
     */
    @Transactional
    public boolean deleteAccount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        if (!accountRepository.existsById(id)) {
            return false;
        }

        accountRepository.deleteById(id);
        return true;
    }

    /**
     * Get accounts by type
     */
    @Transactional(readOnly = true)
    public List<Account> getAccountsByType(Long companyId, String accountType) {
        if (companyId == null || accountType == null) {
            throw new IllegalArgumentException("Company ID and account type are required");
        }
        return accountRepository.findByCompanyIdAndAccountType(companyId, accountType);
    }

    /**
     * Get asset accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getAssetAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findAssetAccountsByCompanyId(companyId);
    }

    /**
     * Get liability accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getLiabilityAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findLiabilityAccountsByCompanyId(companyId);
    }

    /**
     * Get equity accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getEquityAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findEquityAccountsByCompanyId(companyId);
    }

    /**
     * Get income accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getIncomeAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findIncomeAccountsByCompanyId(companyId);
    }

    /**
     * Get expense accounts for a company
     */
    @Transactional(readOnly = true)
    public List<Account> getExpenseAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.findExpenseAccountsByCompanyId(companyId);
    }

    /**
     * Count active accounts for a company
     */
    @Transactional(readOnly = true)
    public long countActiveAccountsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return accountRepository.countByCompanyIdAndIsActiveTrue(companyId);
    }

    /**
     * Deactivate an account
     */
    @Transactional
    public Account deactivateAccount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + id);
        }

        Account account = accountOpt.get();
        account.setActive(false);
        return accountRepository.save(account);
    }

    /**
     * Activate an account
     */
    @Transactional
    public Account activateAccount(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Account ID is required");
        }

        Optional<Account> accountOpt = accountRepository.findById(id);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found: " + id);
        }

        Account account = accountOpt.get();
        account.setActive(true);
        return accountRepository.save(account);
    }
}