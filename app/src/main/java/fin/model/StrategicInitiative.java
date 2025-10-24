package fin.model;

/**
 * Strategic Initiative Model
 * Represents specific initiatives within strategic priorities with timelines and budget allocations
 */
public class StrategicInitiative {
    private Long id;
    private Long strategicPriorityId;
    private String title;
    private String description;
    private String startDate;
    private String endDate;
    private java.math.BigDecimal budgetAllocated;
    private String status; // PLANNED, IN_PROGRESS, COMPLETED, CANCELLED

    public StrategicInitiative() {}

    public StrategicInitiative(Long strategicPriorityId, String title, String description,
                              String startDate, String endDate, java.math.BigDecimal budgetAllocated, String status) {
        this.strategicPriorityId = strategicPriorityId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.budgetAllocated = budgetAllocated;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStrategicPriorityId() {
        return strategicPriorityId;
    }

    public void setStrategicPriorityId(Long strategicPriorityId) {
        this.strategicPriorityId = strategicPriorityId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public java.math.BigDecimal getBudgetAllocated() {
        return budgetAllocated;
    }

    public void setBudgetAllocated(java.math.BigDecimal budgetAllocated) {
        this.budgetAllocated = budgetAllocated;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StrategicInitiative{" +
                "id=" + id +
                ", strategicPriorityId=" + strategicPriorityId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", budgetAllocated=" + budgetAllocated +
                ", status='" + status + '\'' +
                '}';
    }
}