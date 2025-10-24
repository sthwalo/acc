package fin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget Item model representing individual line items within budget categories
 */
public class BudgetItem {
    private Long id;
    private Long budgetCategoryId;
    private Long accountId; // Link to chart of accounts
    private String description;
    private BigDecimal annualAmount;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BudgetItem() {}

    public BudgetItem(Long budgetCategoryId, String description, BigDecimal annualAmount) {
        this.budgetCategoryId = budgetCategoryId;
        this.description = description;
        this.annualAmount = annualAmount;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBudgetCategoryId() { return budgetCategoryId; }
    public void setBudgetCategoryId(Long budgetCategoryId) { this.budgetCategoryId = budgetCategoryId; }

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getAnnualAmount() { return annualAmount; }
    public void setAnnualAmount(BigDecimal annualAmount) { this.annualAmount = annualAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("BudgetItem{id=%d, description='%s', annualAmount=%s}", id, description, annualAmount);
    }
}