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

package fin.service.classification;

import fin.entity.*;

import fin.service.classification.chartofaccounts.ChartOfAccountsInitializationService;
import fin.service.classification.rules.TransactionMappingRuleService;
import fin.service.classification.rules.TransactionMappingRuleService.ClassificationRule;
import fin.service.classification.engine.TransactionClassificationEngine;
import fin.service.classification.engine.TransactionClassificationEngine.BatchClassificationResult;
import fin.service.classification.reporting.TransactionClassificationReportingService;
import fin.service.classification.reporting.TransactionClassificationReportingService.ClassificationStats;
import fin.service.classification.reporting.TransactionClassificationReportingService.UnclassifiedTransaction;
import fin.service.classification.reporting.TransactionClassificationReportingService.SuggestedClassification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main orchestration service for transaction classification operations.
 * Delegates to specialized services for specific responsibilities.
 *
 * SINGLE RESPONSIBILITY: Orchestration and coordination of classification operations
 */
@Service
@Transactional
public class TransactionClassificationService {

    private static final Logger LOGGER = Logger.getLogger(TransactionClassificationService.class.getName());
    private static final int CONSOLE_SEPARATOR_WIDTH = 80;

    // Specialized services for different responsibilities
    private final ChartOfAccountsInitializationService chartOfAccountsService;
    private final TransactionMappingRuleService ruleService;
    private final TransactionClassificationEngine engine;
    private final TransactionClassificationReportingService reportingService;

    public TransactionClassificationService(ChartOfAccountsInitializationService chartOfAccountsService,
                                                TransactionMappingRuleService ruleService,
                                                TransactionClassificationEngine engine,
                                                TransactionClassificationReportingService reportingService) {
        this.chartOfAccountsService = chartOfAccountsService;
        this.ruleService = ruleService;
        this.engine = engine;
        this.reportingService = reportingService;
    }

    /**
     * Initialize chart of accounts for a company
     * DELEGATES TO ChartOfAccountsInitializationService
     */
    @Transactional
    public boolean initializeChartOfAccounts(Long companyId) {
        try {
            return chartOfAccountsService.initializeChartOfAccounts(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing chart of accounts for company " + companyId, e);
            throw new RuntimeException("Failed to initialize chart of accounts", e);
        }
    }













    /**
     * Initialize transaction mapping rules for a company
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional
    public boolean initializeTransactionMappingRules(Long companyId) {
        try {
            return ruleService.initializeTransactionMappingRules(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing transaction mapping rules for company " + companyId, e);
            throw new RuntimeException("Failed to initialize transaction mapping rules", e);
        }
    }



    /**
     * Perform full initialization of a company's financial structure
     * This includes both chart of accounts AND mapping rules
     */
    @Transactional
    public boolean performFullInitialization(Long companyId) {
        try {
            LOGGER.info("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            LOGGER.info("🚀 FULL INITIALIZATION FOR COMPANY ID: " + companyId);
            LOGGER.info("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            // Step 1: Initialize chart of accounts
            LOGGER.info("\n📋 Step 1: Initializing Chart of Accounts...");
            boolean accountsOk = initializeChartOfAccounts(companyId);
            if (!accountsOk) {
                LOGGER.severe("❌ Chart of accounts initialization failed");
                return false;
            }

            // Step 2: Initialize mapping rules
            LOGGER.info("\n📋 Step 2: Initializing Transaction Mapping Rules...");
            boolean rulesOk = initializeTransactionMappingRules(companyId);
            if (!rulesOk) {
                LOGGER.severe("❌ Mapping rules initialization failed");
                return false;
            }

            LOGGER.info("\n" + "=".repeat(CONSOLE_SEPARATOR_WIDTH));
            LOGGER.info("✅ FULL INITIALIZATION COMPLETED SUCCESSFULLY");
            LOGGER.info("=".repeat(CONSOLE_SEPARATOR_WIDTH));

            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during full initialization", e);
            LOGGER.severe("❌ Error during full initialization: " + e.getMessage());
            return false;
        }
    }

    /**
     * Auto-classify all unclassified transactions using mapping rules
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public int autoClassifyTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            return engine.autoClassifyTransactions(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to auto-classify transactions", e);
        }
    }



    /**
     * Count unclassified transactions for a company and fiscal period
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public int countUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            return reportingService.countUnclassifiedTransactions(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting unclassified transactions for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to count unclassified transactions", e);
        }
    }

    /**
     * Count classified transactions for a company and fiscal period
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public int countClassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            return reportingService.countClassifiedTransactions(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error counting classified transactions for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to count classified transactions", e);
        }
    }

    /**
     * Get all transactions for a company and fiscal period
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionsByCompanyAndPeriod(Long companyId, Long fiscalPeriodId) {
        try {
            return reportingService.getTransactionsByCompanyAndPeriod(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting transactions for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to get transactions by company and period", e);
        }
    }

    /**
     * Classify a single transaction with an account code
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public boolean classifyTransactionWithAccountCode(Long transactionId, String accountCode) {
        try {
            return engine.classifyTransaction(transactionId, accountCode);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error classifying transaction " + transactionId + " with account code " + accountCode, e);
            throw new RuntimeException("Failed to classify transaction with account code", e);
        }
    }

    /**
     * Get classification statistics for a company and fiscal period
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public ClassificationStats getClassificationStats(Long companyId, Long fiscalPeriodId) {
        try {
            return reportingService.getClassificationStats(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting classification stats for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to get classification statistics", e);
        }
    }

    /**
     * Auto-classify transactions (API version that returns result)
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public BatchClassificationResult autoClassifyTransactions(Long companyId) {
        try {
            return engine.autoClassifyTransactions(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error auto-classifying transactions for company " + companyId, e);
            throw new RuntimeException("Failed to auto-classify transactions", e);
        }
    }

    /**
     * Sync journal entries for new classified transactions
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public int syncJournalEntries(Long companyId) {
        try {
            return engine.syncJournalEntries(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error syncing journal entries for company " + companyId, e);
            throw new RuntimeException("Failed to sync journal entries", e);
        }
    }

    /**
     * Regenerate all journal entries after reclassification
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public int regenerateAllJournalEntries(Long companyId) {
        try {
            return engine.regenerateAllJournalEntries(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error regenerating all journal entries for company " + companyId, e);
            throw new RuntimeException("Failed to regenerate all journal entries", e);
        }
    }

    /**
     * Get classification summary for a company
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public String getClassificationSummary(Long companyId) {
        try {
            return reportingService.getClassificationSummary(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting classification summary for company " + companyId, e);
            throw new RuntimeException("Failed to get classification summary", e);
        }
    }

    /**
     * Get uncategorized transactions for a company
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public String getUncategorizedTransactions(Long companyId) {
        try {
            return reportingService.getUncategorizedTransactions(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting uncategorized transactions for company " + companyId, e);
            throw new RuntimeException("Failed to get uncategorized transactions", e);
        }
    }



    /**
     * Get classification rules for a company
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional(readOnly = true)
    public List<ClassificationRule> getClassificationRules(Long companyId) {
        try {
            return ruleService.getClassificationRules(companyId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting classification rules for company " + companyId, e);
            throw new RuntimeException("Failed to get classification rules", e);
        }
    }

    /**
     * Add a classification rule
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional
    public ClassificationRule addClassificationRule(ClassificationRule rule) {
        try {
            return ruleService.addClassificationRule(rule);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding classification rule", e);
            throw new RuntimeException("Failed to add classification rule", e);
        }
    }

    /**
     * Update a classification rule
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional
    public ClassificationRule updateClassificationRule(Long id, ClassificationRule rule) {
        try {
            return ruleService.updateClassificationRule(id, rule);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating classification rule " + id, e);
            throw new RuntimeException("Failed to update classification rule", e);
        }
    }

    /**
     * Delete a classification rule
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional
    public void deleteClassificationRule(Long id) {
        try {
            ruleService.deleteClassificationRule(id);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deleting classification rule " + id, e);
            throw new RuntimeException("Failed to delete classification rule", e);
        }
    }

    /**
     * Classify a single transaction with an account ID
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public BankTransaction classifyTransactionWithAccountId(Long transactionId, Long accountId) {
        try {
            return engine.classifyTransaction(transactionId, accountId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error classifying transaction " + transactionId + " with account ID " + accountId, e);
            throw new RuntimeException("Failed to classify transaction with account ID", e);
        }
    }

    /**
     * Update transaction classification with manual account selection.
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public void updateTransactionClassification(Long companyId, Long transactionId,
                                              Long debitAccountId, Long creditAccountId, String username) {
        try {
            engine.updateTransactionClassification(companyId, transactionId, debitAccountId, creditAccountId, username);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating transaction classification for transaction " + transactionId + " in company " + companyId, e);
            throw new RuntimeException("Failed to update transaction classification", e);
        }
    }

    /**
     * Get unclassified transactions for a specific fiscal period as JSON data.
     * DELEGATES TO TransactionClassificationReportingService
     */
    @Transactional(readOnly = true)
    public List<UnclassifiedTransaction> getUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        try {
            return reportingService.getUnclassifiedTransactions(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting unclassified transactions for company " + companyId + " and fiscal period " + fiscalPeriodId, e);
            throw new RuntimeException("Failed to get unclassified transactions", e);
        }
    }

    /**
     * Create a new transaction mapping rule for a company.
     * DELEGATES TO TransactionMappingRuleService
     */
    @Transactional
    public void createTransactionMappingRule(Long companyId, String ruleName, String matchType,
                                           String matchValue, String accountCode, String accountName,
                                           Integer priority, String createdBy) {
        try {
            ruleService.createTransactionMappingRule(companyId, ruleName, matchType, matchValue, accountCode, accountName, priority, createdBy);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating transaction mapping rule for company " + companyId, e);
            throw new RuntimeException("Failed to create transaction mapping rule", e);
        }
    }

    /**
     * Classify a transaction with an account code.
     * DELEGATES TO TransactionClassificationEngine
     */
    @Transactional
    public void classifyTransactionByAccountCode(Long companyId, Long transactionId,
                                               String accountCode, String classifiedBy) {
        try {
            engine.classifyTransactionByAccountCode(companyId, transactionId, accountCode, classifiedBy);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error classifying transaction " + transactionId + " with account code " + accountCode + " for company " + companyId, e);
            throw new RuntimeException("Failed to classify transaction by account code", e);
        }
    }
}
