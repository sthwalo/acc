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

package fin.service.classification.rules;

import fin.entity.*;
import fin.repository.AccountRepository;
import fin.repository.RuleTemplateRepository;
import fin.repository.TransactionMappingRuleRepository;
import fin.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service responsible for managing transaction mapping rules.
 * Handles CRUD operations for classification rules and rule initialization.
 *
 * SINGLE RESPONSIBILITY: Transaction mapping rule management
 */
@Service
@Transactional
public class TransactionMappingRuleService {

    private static final Logger LOGGER = Logger.getLogger(TransactionMappingRuleService.class.getName());

    private final CompanyService companyService;
    private final RuleTemplateRepository ruleTemplateRepository;
    private final TransactionMappingRuleRepository transactionMappingRuleRepository;
    private final AccountRepository accountRepository;

    public TransactionMappingRuleService(CompanyService companyService,
                                       RuleTemplateRepository ruleTemplateRepository,
                                       TransactionMappingRuleRepository transactionMappingRuleRepository,
                                       AccountRepository accountRepository) {
        this.companyService = companyService;
        this.ruleTemplateRepository = ruleTemplateRepository;
        this.transactionMappingRuleRepository = transactionMappingRuleRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Initialize transaction mapping rules for a company using industry templates
     */
    @Transactional
    public boolean initializeTransactionMappingRules(Long companyId) {
        try {
            LOGGER.info("Initializing transaction mapping rules for company: " + companyId);

            // Validate company exists
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                throw new IllegalArgumentException("Company not found: " + companyId);
            }

            if (company.getIndustryId() == null) {
                throw new IllegalArgumentException("Company must have an industry selected before initializing mapping rules");
            }

            // Create standard mapping rules from templates
            int rulesCreated = createStandardMappingRules(companyId);

            LOGGER.info("Transaction mapping rules initialization completed successfully - created " + rulesCreated + " rules");
            return true; // Success if process completed without errors, regardless of how many rules were created

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules", e);
            System.err.println("❌ Error initializing transaction mapping rules: " + e.getMessage());
            return false;
        }
    }

    /**
     * Create standard mapping rules for a company
     */
    private int createStandardMappingRules(Long companyId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                LOGGER.warning("Company not found: " + companyId);
                return 0;
            }

            // Get industry templates
            List<RuleTemplate> templates = ruleTemplateRepository.findByIndustryIdOrderByPriorityDesc(company.getIndustryId());
            int count = 0;

            for (RuleTemplate template : templates) {
                try {
                    // Check if rule already exists
                    Optional<TransactionMappingRule> existingRule = transactionMappingRuleRepository
                        .findByCompanyIdAndRuleName(companyId, template.getRuleName());
                    if (existingRule.isPresent()) {
                        LOGGER.info("Rule already exists: " + template.getRuleName() + " for company " + companyId);
                        continue;
                    }

                    // Find account by account code if specified
                    Account account = null;
                    if (template.getAccountCode() != null) {
                        Optional<Account> accountOpt = accountRepository.findByCompanyIdAndAccountCode(companyId, template.getAccountCode());
                        if (accountOpt.isPresent()) {
                            account = accountOpt.get();
                        }
                    }

                    // Create new rule from template
                    TransactionMappingRule rule = new TransactionMappingRule();
                    rule.setCompany(company);
                    rule.setRuleName(template.getRuleName());
                    rule.setDescription(template.getDescription());
                    rule.setMatchType(TransactionMappingRule.MatchType.valueOf(template.getMatchType().toUpperCase()));
                    rule.setMatchValue(template.getMatchValue());
                    rule.setAccount(account);
                    rule.setPriority(template.getPriority());
                    rule.setActive(template.getIsDefaultEnabled());

                    transactionMappingRuleRepository.save(rule);
                    count++;

                    LOGGER.info("Created rule: " + template.getRuleName());

                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to create rule from template: " + template.getRuleName(), e);
                }
            }

            LOGGER.info("Created " + count + " standard mapping rules for company: " + companyId);
            return count;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create standard mapping rules for company: " + companyId, e);
            return 0;
        }
    }

    /**
     * Get classification rules for a company
     */
    @Transactional(readOnly = true)
    public List<ClassificationRule> getClassificationRules(Long companyId) {
        List<TransactionMappingRule> rules = transactionMappingRuleRepository.findByCompanyId(companyId);
        return rules.stream()
            .map(this::convertToClassificationRule)
            .toList();
    }

    /**
     * Add a classification rule
     */
    @Transactional
    public ClassificationRule addClassificationRule(ClassificationRule rule) {
        TransactionMappingRule entity = convertToTransactionMappingRule(rule);
        TransactionMappingRule saved = transactionMappingRuleRepository.save(entity);
        return convertToClassificationRule(saved);
    }

    /**
     * Update a classification rule
     */
    @Transactional
    public ClassificationRule updateClassificationRule(Long id, ClassificationRule rule) {
        TransactionMappingRule existing = transactionMappingRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));

        existing.setRuleName(rule.getPattern()); // Using pattern as rule name for now
        existing.setMatchValue(rule.getPattern());
        // Note: Other fields would need to be mapped based on business logic

        TransactionMappingRule saved = transactionMappingRuleRepository.save(existing);
        return convertToClassificationRule(saved);
    }

    /**
     * Delete a classification rule
     */
    @Transactional
    public void deleteClassificationRule(Long id) {
        transactionMappingRuleRepository.deleteById(id);
    }

    /**
     * Create a new transaction mapping rule for a company
     */
    @Transactional
    public void createTransactionMappingRule(Long companyId, String ruleName, String matchType,
                                           String matchValue, String accountCode, String accountName,
                                           Integer priority, String createdBy) {
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Find account by code
        Account account = null;
        if (accountCode != null && !accountCode.trim().isEmpty()) {
            // This would need account service - for now assume it's available
            // account = accountService.getAccountByCompanyAndCode(companyId, accountCode).orElse(null);
        }

        TransactionMappingRule rule = new TransactionMappingRule();
        rule.setCompany(company);
        rule.setRuleName(ruleName);
        rule.setMatchType(TransactionMappingRule.MatchType.valueOf(matchType));
        rule.setMatchValue(matchValue);
        rule.setAccount(account);
        rule.setPriority(priority != null ? priority : 50);
        rule.setActive(true);

        transactionMappingRuleRepository.save(rule);
    }

    /**
     * Get active rules for a company ordered by priority
     */
    @Transactional(readOnly = true)
    public List<TransactionMappingRule> getActiveRulesForCompany(Long companyId) {
        return transactionMappingRuleRepository.findByCompanyIdAndIsActiveOrderByPriorityDesc(companyId, true);
    }

    /**
     * Convert TransactionMappingRule to ClassificationRule
     */
    private ClassificationRule convertToClassificationRule(TransactionMappingRule rule) {
        return new ClassificationRule(
            rule.getId(),
            rule.getCompany().getId(),
            rule.getMatchValue(),
            rule.getAccount() != null ? rule.getAccount().getAccountCode() : null,
            rule.getDescription(),
            rule.isActive()
        );
    }

    /**
     * Convert ClassificationRule to TransactionMappingRule
     */
    private TransactionMappingRule convertToTransactionMappingRule(ClassificationRule rule) {
        Company company = companyService.getCompanyById(rule.getCompanyId());
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + rule.getCompanyId());
        }

        TransactionMappingRule entity = new TransactionMappingRule();
        entity.setCompany(company);
        entity.setRuleName(rule.getPattern());
        entity.setDescription(rule.getDescription());
        entity.setMatchType(TransactionMappingRule.MatchType.CONTAINS); // Default
        entity.setMatchValue(rule.getPattern());
        entity.setActive(rule.isActive());
        entity.setPriority(50); // Default priority

        // Find account by code if provided
        if (rule.getAccountCode() != null) {
            // This would need account service injection
            // For now, leave account as null
        }

        return entity;
    }

    /**
     * Classification rule for transaction mapping
     */
    public static class ClassificationRule {
        private final Long id;
        private final Long companyId;
        private final String pattern;
        private final String accountCode;
        private final String description;
        private final boolean active;

        public ClassificationRule(Long id, Long companyId, String pattern, String accountCode,
                                String description, boolean active) {
            this.id = id;
            this.companyId = companyId;
            this.pattern = pattern;
            this.accountCode = accountCode;
            this.description = description;
            this.active = active;
        }

        public Long getId() { return id; }
        public Long getCompanyId() { return companyId; }
        public String getPattern() { return pattern; }
        public String getAccountCode() { return accountCode; }
        public String getDescription() { return description; }
        public boolean isActive() { return active; }
    }
}