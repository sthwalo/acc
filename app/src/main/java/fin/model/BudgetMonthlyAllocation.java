package fin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget Monthly Allocation model representing monthly budget allocations and actual spending
 */
public class BudgetMonthlyAllocation {
    private Long id;
    private Long budgetItemId;
    private Integer monthNumber; // 1=Jan, 2=Feb, etc.
    private BigDecimal allocatedAmount;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount; // Calculated: actual - allocated
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BudgetMonthlyAllocation() {}

    public BudgetMonthlyAllocation(Long budgetItemId, Integer monthNumber, BigDecimal allocatedAmount) {
        this.budgetItemId = budgetItemId;
        this.monthNumber = monthNumber;
        this.allocatedAmount = allocatedAmount;
        this.actualAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBudgetItemId() { return budgetItemId; }
    public void setBudgetItemId(Long budgetItemId) { this.budgetItemId = budgetItemId; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public void setVarianceAmount(BigDecimal varianceAmount) { this.varianceAmount = varianceAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getMonthName() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return monthNumber != null && monthNumber >= 1 && monthNumber <= 12 ?
               months[monthNumber - 1] : "Unknown";
    }

    public boolean isOverBudget() {
        return actualAmount != null && allocatedAmount != null &&
               actualAmount.compareTo(allocatedAmount) > 0;
    }

    public boolean isUnderBudget() {
        return actualAmount != null && allocatedAmount != null &&
               actualAmount.compareTo(allocatedAmount) < 0;
    }

    @Override
    public String toString() {
        return String.format("BudgetMonthlyAllocation{id=%d, month=%s, allocated=%s, actual=%s, variance=%s}",
                           id, getMonthName(), allocatedAmount, actualAmount, varianceAmount);
    }
}