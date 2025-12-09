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

import fin.context.ApplicationContext;
import fin.entity.*;
import fin.repository.BudgetRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Service for Budget operations
 */
public class BudgetService {
    private final BudgetRepository repository;
    private final ApplicationContext context;

    public BudgetService(String dbUrl, ApplicationContext context) {
        this.repository = new BudgetRepository(dbUrl);
        this.context = context;
    }

    /**
     * Create a new budget for a company
     */
    public Budget createBudget(Long companyId, String title, Integer budgetYear, String description) throws SQLException {
        // Validate inputs
        if (companyId == null || title == null || title.trim().isEmpty() || budgetYear == null) {
            throw new IllegalArgumentException("Company ID, title, and budget year are required");
        }

        // Check if company exists
        CompanyService companyService = context.get(CompanyService.class);
        if (companyService.getCompanyById(companyId) == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if budget already exists for this year
        List<Budget> existingBudgets = repository.getBudgetsByCompany(companyId);
        boolean yearExists = existingBudgets.stream()
                .anyMatch(b -> b.getBudgetYear().equals(budgetYear));
        if (yearExists) {
            throw new IllegalArgumentException("Budget already exists for year: " + budgetYear);
        }

        // Create budget
        Budget budget = new Budget(companyId, title.trim(), budgetYear);
        budget.setDescription(description);

        return repository.saveBudget(budget);
    }

    /**
     * Get all budgets for a company
     */
    public List<Budget> getBudgetsForCompany(Long companyId) throws SQLException {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return repository.getBudgetsByCompany(companyId);
    }

    /**
     * Get budget with full details (categories and items)
     */
    public Budget getBudgetWithDetails(Long budgetId) throws SQLException {
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        return repository.getBudgetWithDetails(budgetId);
    }

    /**
     * Add a budget category
     */
    public BudgetCategory addBudgetCategory(Long budgetId, String name, String categoryType,
                                          BigDecimal allocatedPercentage, String description) throws SQLException {
        if (budgetId == null || name == null || categoryType == null) {
            throw new IllegalArgumentException("Budget ID, name, and category type are required");
        }

        if (!categoryType.equals("REVENUE") && !categoryType.equals("EXPENSE")) {
            throw new IllegalArgumentException("Category type must be REVENUE or EXPENSE");
        }

        BudgetCategory category = new BudgetCategory(budgetId, name.trim(), categoryType);
        category.setDescription(description);
        category.setAllocatedPercentage(allocatedPercentage);

        return repository.saveBudgetCategory(category);
    }

    /**
     * Add a budget item to a category
     */
    public BudgetItem addBudgetItem(Long categoryId, String description, BigDecimal amount,
                                  Long accountId, String notes) throws SQLException {
        if (categoryId == null || description == null || amount == null) {
            throw new IllegalArgumentException("Category ID, description, and amount are required");
        }

        BudgetItem item = new BudgetItem(categoryId, description.trim(), amount);
        item.setAccountId(accountId);
        item.setNotes(notes);

        return repository.saveBudgetItem(item);
    }

    /**
     * Create a sample budget with standard categories for any company
     */
    public Budget createSampleBudget(Long companyId, String title, Integer budgetYear) throws SQLException {
        if (companyId == null || title == null || budgetYear == null) {
            throw new IllegalArgumentException("Company ID, title, and budget year are required");
        }

        // Create budget
        Budget budget = createBudget(companyId, title, budgetYear, "Sample budget with standard categories");

        // Revenue Categories
        BudgetCategory tuitionFees = addBudgetCategory(budget.getId(), "Tuition Fees", "REVENUE",
                                                     new BigDecimal("50.00"), "Primary revenue from student fees");
        addBudgetCategory(budget.getId(), "Grants and Funding", "REVENUE",
                         new BigDecimal("30.00"), "Government and private grants");
        addBudgetCategory(budget.getId(), "Other Income", "REVENUE",
                         new BigDecimal("20.00"), "Miscellaneous revenue sources");

        // Expense Categories
        BudgetCategory staffSalaries = addBudgetCategory(budget.getId(), "Staff Salaries", "EXPENSE",
                                                       new BigDecimal("40.00"), "Teaching and administrative salaries");
        BudgetCategory infrastructure = addBudgetCategory(budget.getId(), "Infrastructure", "EXPENSE",
                                                        new BigDecimal("25.00"), "Facility maintenance and development");
        addBudgetCategory(budget.getId(), "Curriculum Resources", "EXPENSE",
                         new BigDecimal("20.00"), "Educational materials and equipment");
        addBudgetCategory(budget.getId(), "Administrative Expenses", "EXPENSE",
                         new BigDecimal("15.00"), "General administrative costs");

        // Add sample budget items
        addBudgetItem(tuitionFees.getId(), "Student Tuition", new BigDecimal("500000.00"), null, "Annual tuition revenue");
        addBudgetItem(staffSalaries.getId(), "Teacher Salaries", new BigDecimal("300000.00"), null, "Monthly teaching staff salaries");
        addBudgetItem(infrastructure.getId(), "Facility Maintenance", new BigDecimal("75000.00"), null, "Building and grounds maintenance");

        // Calculate totals
        updateBudgetTotals(budget.getId());

        return budget;
    }

    /**
     * Update budget totals by recalculating from categories
     */
    public void updateBudgetTotals(Long budgetId) throws SQLException {
        Budget budget = repository.getBudgetWithDetails(budgetId);
        if (budget == null) return;

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (BudgetCategory category : budget.getCategories()) {
            if ("REVENUE".equals(category.getCategoryType())) {
                totalRevenue = totalRevenue.add(category.getTotalAllocated());
            } else if ("EXPENSE".equals(category.getCategoryType())) {
                totalExpenses = totalExpenses.add(category.getTotalAllocated());
            }
        }

        // Update budget totals in database
        repository.updateBudgetTotals(budgetId, totalRevenue, totalExpenses);
    }

    /**
     * Generate budget projections for multiple years based on a base budget.
     * Creates new budgets for future years with projected amounts based on growth rate.
     * 
     * @param budgetId The base budget to project from
     * @param years Number of future years to project (1-10)
     * @param growthRate Annual growth rate as decimal (e.g., 0.05 for 5% growth)
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void generateBudgetProjections(Long budgetId, Integer years, BigDecimal growthRate) throws SQLException {
        // Validate inputs
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (years == null || years < 1 || years > 10) {
            throw new IllegalArgumentException("Years must be between 1 and 10");
        }
        if (growthRate == null) {
            throw new IllegalArgumentException("Growth rate is required");
        }

        // Get base budget with all details
        Budget baseBudget = repository.getBudgetWithDetails(budgetId);
        if (baseBudget == null) {
            throw new IllegalArgumentException("Base budget not found: " + budgetId);
        }

        // Get company to check for existing budgets
        Long companyId = baseBudget.getCompanyId();
        List<Budget> existingBudgets = repository.getBudgetsByCompany(companyId);

        // Generate projections for each year
        for (int year = 1; year <= years; year++) {
            Integer projectedYear = baseBudget.getBudgetYear() + year;
            
            // Skip if budget already exists for this year
            boolean yearExists = existingBudgets.stream()
                    .anyMatch(b -> b.getBudgetYear().equals(projectedYear));
            if (yearExists) {
                System.out.println("⚠️  Budget already exists for year " + projectedYear + ", skipping...");
                continue;
            }

            // Calculate compound growth factor: (1 + rate)^year
            BigDecimal growthFactor = BigDecimal.ONE.add(growthRate);
            for (int i = 1; i < year; i++) {
                growthFactor = growthFactor.multiply(BigDecimal.ONE.add(growthRate));
            }

            // Create projected budget
            String projectedTitle = baseBudget.getTitle() + " - Projected Year " + projectedYear;
            String projectedDescription = "Projected budget based on " + baseBudget.getBudgetYear() + 
                                        " budget with " + growthRate.multiply(new BigDecimal("100")) + 
                                        "% annual growth rate";
            
            Budget projectedBudget = createBudget(companyId, projectedTitle, projectedYear, projectedDescription);

            // Copy and project categories
            for (BudgetCategory baseCategory : baseBudget.getCategories()) {
                // Create projected category
                BudgetCategory projectedCategory = addBudgetCategory(
                    projectedBudget.getId(),
                    baseCategory.getName(),
                    baseCategory.getCategoryType(),
                    baseCategory.getAllocatedPercentage(),
                    baseCategory.getDescription()
                );

                // Copy and project budget items
                for (BudgetItem baseItem : baseCategory.getItems()) {
                    // Apply growth factor to amount
                    BigDecimal projectedAmount = baseItem.getAnnualAmount().multiply(growthFactor);
                    
                    // Round to 2 decimal places using modern RoundingMode
                    projectedAmount = projectedAmount.setScale(2, java.math.RoundingMode.HALF_UP);

                    addBudgetItem(
                        projectedCategory.getId(),
                        baseItem.getDescription(),
                        projectedAmount,
                        baseItem.getAccountId(),
                        "Projected from " + baseBudget.getBudgetYear() + " with " + 
                        growthRate.multiply(new BigDecimal("100")) + "% growth"
                    );
                }
            }

            // Update totals for projected budget
            updateBudgetTotals(projectedBudget.getId());

            System.out.println("✅ Created projected budget for year " + projectedYear);
        }

        System.out.println("🎯 Budget projections completed for " + years + " year(s)");
    }

    /**
     * Approve a budget with validation and status transition workflow.
     * Changes budget status from DRAFT to APPROVED and records approver details.
     * 
     * @param budgetId The budget to approve
     * @param approvedBy Username or identifier of the person approving
     * @return The approved budget with updated status
     * @throws SQLException if database operation fails
     * @throws IllegalArgumentException if parameters are invalid or budget not found
     * @throws IllegalStateException if budget is not in DRAFT status
     */
    public Budget approveBudget(Long budgetId, String approvedBy) throws SQLException {
        // Validate inputs
        if (budgetId == null) {
            throw new IllegalArgumentException("Budget ID is required");
        }
        if (approvedBy == null || approvedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Approver name is required");
        }

        // Get budget with current details
        Budget budget = repository.getBudgetWithDetails(budgetId);
        if (budget == null) {
            throw new IllegalArgumentException("Budget not found: " + budgetId);
        }

        // Validate budget is in DRAFT status
        String currentStatus = budget.getStatus();
        if (!"DRAFT".equals(currentStatus)) {
            throw new IllegalStateException(
                String.format("Budget cannot be approved from status '%s'. Only DRAFT budgets can be approved.", 
                    currentStatus)
            );
        }

        // Validate budget has content (categories and items)
        if (budget.getCategories() == null || budget.getCategories().isEmpty()) {
            throw new IllegalStateException(
                "Budget cannot be approved without categories. Please add at least one budget category."
            );
        }

        // Validate budget has budget items
        boolean hasItems = budget.getCategories().stream()
                .anyMatch(category -> category.getItems() != null && !category.getItems().isEmpty());
        if (!hasItems) {
            throw new IllegalStateException(
                "Budget cannot be approved without budget items. Please add items to at least one category."
            );
        }

        // Validate budget has non-zero totals
        if (budget.getTotalRevenue().compareTo(BigDecimal.ZERO) == 0 
            && budget.getTotalExpenses().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                "Budget cannot be approved with zero revenue and zero expenses. Please add budget amounts."
            );
        }

        // Update budget approval in database
        java.time.LocalDateTime approvalTime = java.time.LocalDateTime.now();
        repository.updateBudgetApproval(budgetId, "APPROVED", approvalTime, approvedBy.trim());

        // Update budget object
        budget.setStatus("APPROVED");
        budget.setApprovedAt(approvalTime);
        budget.setApprovedBy(approvedBy.trim());

        System.out.println("✅ Budget '" + budget.getTitle() + "' approved by " + approvedBy);
        System.out.println("📊 Total Revenue: " + budget.getTotalRevenue());
        System.out.println("💰 Total Expenses: " + budget.getTotalExpenses());
        System.out.println("📈 Net Position: " + budget.getTotalRevenue().subtract(budget.getTotalExpenses()));

        return budget;
    }
}