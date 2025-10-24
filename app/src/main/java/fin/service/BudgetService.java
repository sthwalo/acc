package fin.service;

import fin.context.ApplicationContext;
import fin.model.*;
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
        // TODO: Implement updateBudgetTotals in repository
    }

    /**
     * Generate budget projections for multiple years
     */
    public void generateBudgetProjections(Long budgetId, Integer years, BigDecimal growthRate) throws SQLException {
        // TODO: Implement multi-year projections
        throw new UnsupportedOperationException("Budget projections not yet implemented");
    }

    /**
     * Approve a budget
     */
    public Budget approveBudget(Long budgetId, String approvedBy) throws SQLException {
        // TODO: Implement budget approval workflow
        throw new UnsupportedOperationException("Budget approval not yet implemented");
    }
}