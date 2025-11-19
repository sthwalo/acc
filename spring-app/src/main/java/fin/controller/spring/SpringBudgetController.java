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

package fin.controller.spring;

import fin.model.Budget;
import fin.service.spring.SpringBudgetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for budget management operations.
 */
@RestController
@RequestMapping("/api/v1/budgets")
public class SpringBudgetController {

    private final SpringBudgetService budgetService;

    public SpringBudgetController(SpringBudgetService budgetService) {
        this.budgetService = budgetService;
    }

    /**
     * Create a new budget
     */
    @PostMapping
    public ResponseEntity<Budget> createBudget(@RequestBody Budget budget) {
        try {
            Budget createdBudget = budgetService.createBudget(
                budget.getCompanyId(),
                budget.getTitle(),
                budget.getBudgetYear(),
                budget.getDescription()
            );
            return ResponseEntity.ok(createdBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get budget by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudgetById(@PathVariable Long id) {
        Optional<Budget> budget = budgetService.getBudgetById(id);
        return budget.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all budgets for a company
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<Budget>> getBudgetsByCompany(@PathVariable Long companyId) {
        try {
            List<Budget> budgets = budgetService.getBudgetsForCompany(companyId);
            return ResponseEntity.ok(budgets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get budgets by fiscal period
     */
    @GetMapping("/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<Budget>> getBudgetsByFiscalPeriod(@PathVariable Long fiscalPeriodId) {
        try {
            List<Budget> budgets = budgetService.getBudgetsByFiscalPeriod(fiscalPeriodId);
            return ResponseEntity.ok(budgets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active budgets for a company
     */
    @GetMapping("/company/{companyId}/active")
    public ResponseEntity<List<Budget>> getActiveBudgetsByCompany(@PathVariable Long companyId) {
        try {
            List<Budget> budgets = budgetService.getActiveBudgetsByCompany(companyId);
            return ResponseEntity.ok(budgets);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a budget
     */
    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(@PathVariable Long id, @RequestBody Budget budget) {
        try {
            Budget updatedBudget = budgetService.updateBudget(id, budget);
            return ResponseEntity.ok(updatedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Approve a budget
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Budget> approveBudget(@PathVariable Long id) {
        try {
            Budget approvedBudget = budgetService.approveBudget(id, "system");
            return ResponseEntity.ok(approvedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Reject a budget
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Budget> rejectBudget(@PathVariable Long id) {
        try {
            Budget rejectedBudget = budgetService.rejectBudget(id);
            return ResponseEntity.ok(rejectedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Close a budget
     */
    @PostMapping("/{id}/close")
    public ResponseEntity<Budget> closeBudget(@PathVariable Long id) {
        try {
            Budget closedBudget = budgetService.closeBudget(id);
            return ResponseEntity.ok(closedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add budget line item
     */
    @PostMapping("/{budgetId}/line-items")
    public ResponseEntity<Budget> addBudgetLineItem(@PathVariable Long budgetId,
                                                 @RequestParam Long accountId,
                                                 @RequestParam BigDecimal amount,
                                                 @RequestParam String description) {
        try {
            Budget updatedBudget = budgetService.addBudgetLineItem(budgetId, accountId, amount, description);
            return ResponseEntity.ok(updatedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update budget line item
     */
    @PutMapping("/{budgetId}/line-items/{lineItemId}")
    public ResponseEntity<Budget> updateBudgetLineItem(@PathVariable Long budgetId,
                                                    @PathVariable Long lineItemId,
                                                    @RequestParam BigDecimal amount,
                                                    @RequestParam String description) {
        try {
            Budget updatedBudget = budgetService.updateBudgetLineItem(budgetId, lineItemId, amount, description);
            return ResponseEntity.ok(updatedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Remove budget line item
     */
    @DeleteMapping("/{budgetId}/line-items/{lineItemId}")
    public ResponseEntity<Budget> removeBudgetLineItem(@PathVariable Long budgetId,
                                                    @PathVariable Long lineItemId) {
        try {
            Budget updatedBudget = budgetService.removeBudgetLineItem(budgetId, lineItemId);
            return ResponseEntity.ok(updatedBudget);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get budget variance analysis
     */
    @GetMapping("/{id}/variance")
    public ResponseEntity<SpringBudgetService.BudgetVariance> getBudgetVariance(@PathVariable Long id) {
        try {
            SpringBudgetService.BudgetVariance variance = budgetService.getBudgetVariance(id);
            return ResponseEntity.ok(variance);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get budget summary for a company and fiscal period
     */
    @GetMapping("/company/{companyId}/fiscal-period/{fiscalPeriodId}/summary")
    public ResponseEntity<SpringBudgetService.BudgetSummary> getBudgetSummary(@PathVariable Long companyId,
                                                                            @PathVariable Long fiscalPeriodId) {
        try {
            SpringBudgetService.BudgetSummary summary = budgetService.getBudgetSummary(companyId, fiscalPeriodId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}