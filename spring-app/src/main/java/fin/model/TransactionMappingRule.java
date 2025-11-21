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

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Defines rules for automatically categorizing transactions based on their description.
 * These rules help map bank transactions to the appropriate accounts in the chart of accounts.
 */
@Entity
@Table(name = "transaction_mapping_rules")
public class TransactionMappingRule {
    public enum MatchType {
        CONTAINS,       // Description contains the match value
        STARTS_WITH,    // Description starts with the match value
        ENDS_WITH,      // Description ends with the match value
        EQUALS,         // Description exactly matches the match value
        REGEX           // Description matches the regular expression
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false)
    private MatchType matchType;

    @Column(name = "match_value", nullable = false, length = 1000)
    private String matchValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public TransactionMappingRule() {
        this.isActive = true;
        this.priority = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TransactionMappingRule(Company initialCompany, String initialRuleName, MatchType initialMatchType, 
                                String initialMatchValue, Account initialAccount) {
        this();
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
        this.ruleName = initialRuleName;
        this.matchType = initialMatchType;
        this.matchValue = initialMatchValue;
        if (initialAccount == null) {
            this.account = null;
        } else {
            this.account = new Account(initialAccount.getAccountCode(), initialAccount.getAccountName(),
                                     initialAccount.getCompanyId());
            this.account.setDescription(initialAccount.getDescription());
            this.account.setCategoryId(initialAccount.getCategoryId());
            this.account.setId(initialAccount.getId());
            this.account.setActive(initialAccount.isActive());
            this.account.setCreatedAt(initialAccount.getCreatedAt());
            this.account.setUpdatedAt(initialAccount.getUpdatedAt());
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long newId) {
        this.id = newId;
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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String newRuleName) {
        this.ruleName = newRuleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType newMatchType) {
        this.matchType = newMatchType;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String newMatchValue) {
        this.matchValue = newMatchValue;
    }

    public Account getAccount() {
        if (account == null) {
            return null;
        }
        Account copy = new Account(account.getAccountCode(), account.getAccountName(),
                                 account.getCompanyId());
        copy.setDescription(account.getDescription());
        copy.setCategoryId(account.getCategoryId());
        copy.setId(account.getId());
        copy.setActive(account.isActive());
        copy.setCreatedAt(account.getCreatedAt());
        copy.setUpdatedAt(account.getUpdatedAt());
        return copy;
    }

    public void setAccount(Account newAccount) {
        if (newAccount == null) {
            this.account = null;
        } else {
            this.account = new Account(newAccount.getAccountCode(), newAccount.getAccountName(),
                                     newAccount.getCompanyId());
            this.account.setDescription(newAccount.getDescription());
            this.account.setCategoryId(newAccount.getCategoryId());
            this.account.setId(newAccount.getId());
            this.account.setActive(newAccount.isActive());
            this.account.setCreatedAt(newAccount.getCreatedAt());
            this.account.setUpdatedAt(newAccount.getUpdatedAt());
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int newPriority) {
        this.priority = newPriority;
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

    /**
     * Checks if the given transaction description matches this rule.
     * @param transactionDescription The transaction description to check
     * @return true if the description matches the rule, false otherwise
     */
    public boolean matches(String transactionDescription) {
        if (transactionDescription == null || !isActive) {
            return false;
        }

        String desc = transactionDescription.trim().toUpperCase();
        String matchVal = matchValue.trim().toUpperCase();

        switch (matchType) {
            case CONTAINS:
                return desc.contains(matchVal);
            case STARTS_WITH:
                return desc.startsWith(matchVal);
            case ENDS_WITH:
                return desc.endsWith(matchVal);
            case EQUALS:
                return desc.equals(matchVal);
            case REGEX:
                return desc.matches(matchVal);
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s -> %s", 
                ruleName, matchType, matchValue, 
                account != null ? account.toString() : "[No account]");
    }
}
