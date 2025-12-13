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

package fin.service;

import fin.entity.*;
import fin.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Service for Budget operations
 * Provides budget management functionality using JPA repositories
 */
@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CompanyService companyService;

    public BudgetService(BudgetRepository budgetRepository, CompanyService companyService) {
        this.budgetRepository = budgetRepository;
        this.companyService = companyService;
    }

    /**
     * Create a new budget for a company
     */
    @Transactional
    public Budget createBudget(Long companyId, String title, Integer budgetYear, String description) {
        // Validate inputs
        if (companyId == null || title == null || title.trim().isEmpty() || budgetYear == null) {
            throw new IllegalArgumentException("Company ID, title, and budget year are required");
        }

        // Check if company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if budget already exists for this year
        Optional<Budget> existingBudget = budgetRepository.findByCompanyIdAndBudgetYear(companyId, budgetYear);
        if (existingBudget.isPresent()) {
            throw new IllegalArgumentException("Budget already exists for year: " + budgetYear);
        }

        // Create budget
        Budget budget = new Budget();
        budget.setCompanyId(companyId);
        budget.setTitle(title.trim());
        budget.setBudgetYear(budgetYear);
        budget.setDescription(description);
        budget.setStatus("DRAFT");

        return budgetRepository.save(budget);
    }

    /**
     * Get all budgets for a company
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsForCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return budgetRepository.findByCompanyId(companyId);
    }

    /**
     * Get budgets for a company and year
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByCompanyAndYear(Long companyId, Integer budgetYear) {
        if (companyId == null || budgetYear == null) {
            throw new IllegalArgumentException("Company ID and budget year are required");
        }
        Optional<Budget> budget = budgetRepository.findByCompanyIdAndBudgetYear(companyId, budgetYear);
        return budget.map(List::of).orElse(List.of());
    }

    /**
     * Get approved budgets for a company
     */
    @Transactional(readOnly = true)
    public List<Budget> getApprovedBudgetsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return budgetRepository.findApprovedBudgetsByCompanyId(companyId);
    }

    /**
     * Get budget by ID
     */
    @Transactional(readOnly = true)
    public Optional<Budget> getBudgetById(Long id) {
        return budgetRepository.findById(id);
    }

    /**
     * Update a budget
     */
    @Transactional
    public Budget updateBudget(Long id, Budget updatedBudget) {
        if (id == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (updatedBudget == null) {
            throw new IllegalArgumentException("Updated budget data is required");
        }

        Optional<Budget> existingBudgetOpt = budgetRepository.findById(id);
        if (existingBudgetOpt.isEmpty()) {
            throw new IllegalArgumentException("Budget not found: " + id);
        }

        Budget existingBudget = existingBudgetOpt.get();

        // Update fields
        existingBudget.setTitle(updatedBudget.getTitle());
        existingBudget.setDescription(updatedBudget.getDescription());
        existingBudget.setStatus(updatedBudget.getStatus());
        existingBudget.setApprovedBy(updatedBudget.getApprovedBy());
        existingBudget.setApprovedAt(updatedBudget.getApprovedAt());

        return budgetRepository.save(existingBudget);
    }

    /**
     * Delete a budget
     */
    @Transactional
    public boolean deleteBudget(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }

        if (!budgetRepository.existsById(id)) {
            return false;
        }

        budgetRepository.deleteById(id);
        return true;
    }

    /**
     * Approve a budget
     */
    @Transactional
    public Budget approveBudget(Long budgetId, String approvedBy) {
        if (budgetId == null || approvedBy == null) {
            throw new IllegalArgumentException("Budget ID and approver are required");
        }

        Optional<Budget> budgetOpt = budgetRepository.findById(budgetId);
        if (budgetOpt.isEmpty()) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        Budget budget = budgetOpt.get();
        budget.setStatus("APPROVED");
        budget.setApprovedBy(approvedBy);
        budget.setApprovedAt(java.time.LocalDateTime.now());

        return budgetRepository.save(budget);
    }

    /**
     * Reject a budget
     */
    @Transactional
    public Budget rejectBudget(Long budgetId) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }

        Optional<Budget> budgetOpt = budgetRepository.findById(budgetId);
        if (budgetOpt.isEmpty()) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        Budget budget = budgetOpt.get();
        budget.setStatus("REJECTED");

        return budgetRepository.save(budget);
    }

    /**
     * Close a budget
     */
    @Transactional
    public Budget closeBudget(Long budgetId) {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }

        Optional<Budget> budgetOpt = budgetRepository.findById(budgetId);
        if (budgetOpt.isEmpty()) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        Budget budget = budgetOpt.get();
        budget.setStatus("CLOSED");

        return budgetRepository.save(budget);
    }

    /**
     * Add budget line item
     */
    @Transactional
    public Budget addBudgetLineItem(Long budgetId, Long accountId, BigDecimal amount, String description) {
        // TODO: Implement budget line items - current model uses simple revenue/expense totals
        throw new UnsupportedOperationException("Budget line items not yet implemented");
    }

    /**
     * Update budget line item
     */
    @Transactional
    public Budget updateBudgetLineItem(Long budgetId, Long lineItemId, BigDecimal amount, String description) {
        // TODO: Implement budget line items - current model uses simple revenue/expense totals
        throw new UnsupportedOperationException("Budget line items not yet implemented");
    }

    /**
     * Remove budget line item
     */
    @Transactional
    public Budget removeBudgetLineItem(Long budgetId, Long lineItemId) {
        // TODO: Implement budget line items - current model uses simple revenue/expense totals
        throw new UnsupportedOperationException("Budget line items not yet implemented");
    }

    /**
     * Get budget variance analysis
     */
    @Transactional(readOnly = true)
    public BudgetVariance getBudgetVariance(Long budgetId) {
        // TODO: Implement budget variance analysis - requires actual transaction data comparison
        throw new UnsupportedOperationException("Budget variance analysis not yet implemented");
    }

    /**
     * Get budget summary for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public BudgetSummary getBudgetSummary(Long companyId, Long fiscalPeriodId) {
        // TODO: Implement budget summary - requires actual transaction data
        throw new UnsupportedOperationException("Budget summary not yet implemented");
    }

    /**
     * Create a sample budget with default categories
     */
    @Transactional
    public Budget createSampleBudget(Long companyId, String title, Integer budgetYear) {
        Budget budget = createBudget(companyId, title, budgetYear, "Sample budget with default categories");

        // Note: In a real implementation, you would create budget categories and items here
        // For now, we just return the basic budget

        return budget;
    }

    /**
     * Get budgets by fiscal period
     */
    @Transactional(readOnly = true)
    public List<Budget> getBudgetsByFiscalPeriod(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }
        return budgetRepository.findByFiscalPeriodId(fiscalPeriodId);
    }

    /**
     * Get active budgets for a company
     */
    @Transactional(readOnly = true)
    public List<Budget> getActiveBudgetsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return budgetRepository.findByCompanyIdAndStatusIn(companyId, List.of("APPROVED", "ACTIVE"));
    }

    /**
     * Count budgets for a company
     */
    @Transactional(readOnly = true)
    public long countBudgetsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return budgetRepository.findByCompanyId(companyId).size();
    }

    /**
     * Budget variance analysis result
     */
    public static class BudgetVariance {
        private final Long budgetId;
        private final String category;
        private final double budgetedAmount;
        private final double actualAmount;
        private final double variance;
        private final double variancePercentage;

        public BudgetVariance(Long budgetId, String category, double budgetedAmount,
                            double actualAmount, double variance, double variancePercentage) {
            this.budgetId = budgetId;
            this.category = category;
            this.budgetedAmount = budgetedAmount;
            this.actualAmount = actualAmount;
            this.variance = variance;
            this.variancePercentage = variancePercentage;
        }

        public Long getBudgetId() { return budgetId; }
        public String getCategory() { return category; }
        public double getBudgetedAmount() { return budgetedAmount; }
        public double getActualAmount() { return actualAmount; }
        public double getVariance() { return variance; }
        public double getVariancePercentage() { return variancePercentage; }
    }

    /**
     * Budget summary information
     */
    public static class BudgetSummary {
        private final Long budgetId;
        private final String title;
        private final Integer budgetYear;
        private final String status;
        private final double totalBudgeted;
        private final double totalActual;
        private final double totalVariance;
        private final double variancePercentage;

        public BudgetSummary(Long budgetId, String title, Integer budgetYear, String status,
                           double totalBudgeted, double totalActual, double totalVariance, double variancePercentage) {
            this.budgetId = budgetId;
            this.title = title;
            this.budgetYear = budgetYear;
            this.status = status;
            this.totalBudgeted = totalBudgeted;
            this.totalActual = totalActual;
            this.totalVariance = totalVariance;
            this.variancePercentage = variancePercentage;
        }

        public Long getBudgetId() { return budgetId; }
        public String getTitle() { return title; }
        public Integer getBudgetYear() { return budgetYear; }
        public String getStatus() { return status; }
        public double getTotalBudgeted() { return totalBudgeted; }
        public double getTotalActual() { return totalActual; }
        public double getTotalVariance() { return totalVariance; }
        public double getVariancePercentage() { return variancePercentage; }
    }
}