package fin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Budget Category model representing budget categories (Revenue/Expense) with allocations
 */
public class BudgetCategory {
    private Long id;
    private Long budgetId;
    private String name;
    private String categoryType; // REVENUE, EXPENSE
    private String description;
    private BigDecimal allocatedPercentage; // e.g., 60.00 for 60%
    private BigDecimal totalAllocated;
    private LocalDateTime createdAt;
    private List<BudgetItem> items; // For loading complete category with items

    // Constructors
    public BudgetCategory() {}

    public BudgetCategory(Long budgetId, String name, String categoryType) {
        this.budgetId = budgetId;
        this.name = name;
        this.categoryType = categoryType;
        this.totalAllocated = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBudgetId() { return budgetId; }
    public void setBudgetId(Long budgetId) { this.budgetId = budgetId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAllocatedPercentage() { return allocatedPercentage; }
    public void setAllocatedPercentage(BigDecimal allocatedPercentage) { this.allocatedPercentage = allocatedPercentage; }

    public BigDecimal getTotalAllocated() { return totalAllocated; }
    public void setTotalAllocated(BigDecimal totalAllocated) { this.totalAllocated = totalAllocated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<BudgetItem> getItems() {
        return items != null ? new ArrayList<>(items) : Collections.emptyList();
    }

    public void setItems(List<BudgetItem> items) {
        this.items = items != null ? new ArrayList<>(items) : null;
    }

    @Override
    public String toString() {
        return String.format("BudgetCategory{id=%d, name='%s', type='%s', allocated=%s}",
                           id, name, categoryType, totalAllocated);
    }
}