package fin.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FiscalPeriod {
    private Long id;
    private Long companyId;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isClosed;
    private LocalDateTime createdAt;
    
    // Constructors, getters, and setters
    public FiscalPeriod() {}
    
    public FiscalPeriod(Long initialCompanyId, String initialPeriodName, LocalDate initialStartDate, LocalDate initialEndDate) {
        this.companyId = initialCompanyId;
        this.periodName = initialPeriodName;
        this.startDate = initialStartDate;
        this.endDate = initialEndDate;
        this.isClosed = false;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Copy constructor for defensive copying.
     * Creates a deep copy of all FiscalPeriod fields to prevent external modification.
     */
    public FiscalPeriod(FiscalPeriod other) {
        if (other == null) {
            return;
        }
        
        this.id = other.id;
        this.companyId = other.companyId;
        this.periodName = other.periodName;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.isClosed = other.isClosed;
        this.createdAt = other.createdAt;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String newPeriodName) { this.periodName = newPeriodName; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate newStartDate) { this.startDate = newStartDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate newEndDate) { this.endDate = newEndDate; }
    
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    @Override
    public String toString() {
        return "FiscalPeriod{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", periodName='" + periodName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isClosed=" + isClosed +
                '}';
    }
}
