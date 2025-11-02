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

package fin.model;

import java.time.LocalDateTime;

/**
 * Represents an account in the chart of accounts.
 * Each account is part of a category and has a unique account code.
 */
public class Account {
    private Long id;
    private String accountCode;
    private String accountName;
    private AccountCategory category;
    private Account parentAccount;
    private Company company;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Account(String initialAccountCode, String initialAccountName, AccountCategory initialCategory, 
                  Company initialCompany, String initialDescription) {
        this();
        this.accountCode = initialAccountCode;
        this.accountName = initialAccountName;
        if (initialCategory == null) {
            this.category = null;
        } else {
            this.category = new AccountCategory(initialCategory.getName(), initialCategory.getDescription(), 
                                              initialCategory.getAccountType(), initialCategory.getCompany());
            this.category.setId(initialCategory.getId());
            this.category.setActive(initialCategory.isActive());
            this.category.setCreatedAt(initialCategory.getCreatedAt());
            this.category.setUpdatedAt(initialCategory.getUpdatedAt());
        }
        if (initialCompany == null) {
            this.company = null;
        } else {
            this.company = new Company(initialCompany.getName());
            this.company.setId(initialCompany.getId());
            this.company.setRegistrationNumber(initialCompany.getRegistrationNumber());
            this.company.setTaxNumber(initialCompany.getTaxNumber());
            this.company.setAddress(initialCompany.getAddress());
            this.company.setContactEmail(initialCompany.getContactEmail());
            this.company.setContactPhone(initialCompany.getContactPhone());
            this.company.setLogoPath(initialCompany.getLogoPath());
            this.company.setCreatedAt(initialCompany.getCreatedAt());
        }
        this.description = initialDescription;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long newId) {
        this.id = newId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String newAccountCode) {
        this.accountCode = newAccountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String newAccountName) {
        this.accountName = newAccountName;
    }

    public AccountCategory getCategory() {
        if (category == null) {
            return null;
        }
        return new AccountCategory(category.getName(), category.getDescription(), 
                                 category.getAccountType(), category.getCompany());
    }

    public void setCategory(AccountCategory newCategory) {
        if (newCategory == null) {
            this.category = null;
        } else {
            this.category = new AccountCategory(newCategory.getName(), newCategory.getDescription(), 
                                              newCategory.getAccountType(), newCategory.getCompany());
            this.category.setId(newCategory.getId());
            this.category.setActive(newCategory.isActive());
            this.category.setCreatedAt(newCategory.getCreatedAt());
            this.category.setUpdatedAt(newCategory.getUpdatedAt());
        }
    }

    public Account getParentAccount() {
        if (parentAccount == null) {
            return null;
        }
        Account copy = new Account(parentAccount.getAccountCode(), parentAccount.getAccountName(),
                                 parentAccount.getCategory(), parentAccount.getCompany(),
                                 parentAccount.getDescription());
        copy.setId(parentAccount.getId());
        copy.setActive(parentAccount.isActive());
        copy.setCreatedAt(parentAccount.getCreatedAt());
        copy.setUpdatedAt(parentAccount.getUpdatedAt());
        return copy;
    }

    public void setParentAccount(Account newParentAccount) {
        if (newParentAccount == null) {
            this.parentAccount = null;
        } else {
            this.parentAccount = new Account(newParentAccount.getAccountCode(), newParentAccount.getAccountName(),
                                           newParentAccount.getCategory(), newParentAccount.getCompany(),
                                           newParentAccount.getDescription());
            this.parentAccount.setId(newParentAccount.getId());
            this.parentAccount.setActive(newParentAccount.isActive());
            this.parentAccount.setCreatedAt(newParentAccount.getCreatedAt());
            this.parentAccount.setUpdatedAt(newParentAccount.getUpdatedAt());
        }
    }

    public Company getCompany() {
        if (company == null) {
            return null;
        }
        Company copy = new Company(company.getName());
        copy.setId(company.getId());
        copy.setRegistrationNumber(company.getRegistrationNumber());
        copy.setTaxNumber(company.getTaxNumber());
        copy.setAddress(company.getAddress());
        copy.setContactEmail(company.getContactEmail());
        copy.setContactPhone(company.getContactPhone());
        copy.setLogoPath(company.getLogoPath());
        copy.setCreatedAt(company.getCreatedAt());
        return copy;
    }

    public void setCompany(Company newCompany) {
        if (newCompany == null) {
            this.company = null;
        } else {
            this.company = new Company(newCompany.getName());
            this.company.setId(newCompany.getId());
            this.company.setRegistrationNumber(newCompany.getRegistrationNumber());
            this.company.setTaxNumber(newCompany.getTaxNumber());
            this.company.setAddress(newCompany.getAddress());
            this.company.setContactEmail(newCompany.getContactEmail());
            this.company.setContactPhone(newCompany.getContactPhone());
            this.company.setLogoPath(newCompany.getLogoPath());
            this.company.setCreatedAt(newCompany.getCreatedAt());
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
    }

    public boolean isDebitAccount() {
        return category != null && category.getAccountType() != null && 
               category.getAccountType().getNormalBalance() == 'D';
    }

    public boolean isCreditAccount() {
        return !isDebitAccount();
    }

    @Override
    public String toString() {
        return String.format("%s - %s", accountCode, accountName);
    }
}
