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
    
    public FiscalPeriod(Long companyId, String periodName, LocalDate startDate, LocalDate endDate) {
        this.companyId = companyId;
        this.periodName = periodName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isClosed = false;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Copy constructor for defensive copying.
     * Creates a deep copy of all FiscalPeriod fields to prevent external modification.
     */
    public FiscalPeriod(FiscalPeriod other) {
        if (other == null) return;
        
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
    public void setId(Long id) { this.id = id; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String periodName) { this.periodName = periodName; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
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
