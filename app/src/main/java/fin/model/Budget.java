package fin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Budget model representing an annual budget for an organization
 */
public class Budget {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String title;
    private String description;
    private Integer budgetYear;
    private String status; // DRAFT, APPROVED, ACTIVE, ARCHIVED
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private List<BudgetCategory> categories; // For loading complete budget with categories

    // Constructors
    public Budget() {}

    public Budget(Long companyId, String title, Integer budgetYear) {
        this.companyId = companyId;
        this.title = title;
        this.budgetYear = budgetYear;
        this.status = "DRAFT";
        this.totalRevenue = BigDecimal.ZERO;
        this.totalExpenses = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getBudgetYear() { return budgetYear; }
    public void setBudgetYear(Integer budgetYear) { this.budgetYear = budgetYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public List<BudgetCategory> getCategories() { return categories; }
    public void setCategories(List<BudgetCategory> categories) { this.categories = categories; }

    // Helper methods
    public BigDecimal getNetBudget() {
        return totalRevenue.subtract(totalExpenses);
    }

    @Override
    public String toString() {
        return String.format("Budget{id=%d, title='%s', year=%d, status='%s', net=%s}",
                           id, title, budgetYear, status, getNetBudget());
    }
}