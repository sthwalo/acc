package fin.model;

import java.time.LocalDateTime;

/**
 * Operational Activity model representing monthly operational activities and milestones
 * aligned with the strategic plan
 */
public class OperationalActivity {
    private Long id;
    private Long strategicPlanId;
    private Integer monthNumber; // 1=Jan, 2=Feb, etc.
    private String title;
    private String activities; // Key activities and milestones for the month
    private String responsibleParties; // Who is responsible (comma-separated)
    private String status; // PLANNED, IN_PROGRESS, COMPLETED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public OperationalActivity() {}

    public OperationalActivity(Long strategicPlanId, Integer monthNumber, String title, String activities, String responsibleParties) {
        this.strategicPlanId = strategicPlanId;
        this.monthNumber = monthNumber;
        this.title = title;
        this.activities = activities;
        this.responsibleParties = responsibleParties;
        this.status = "PLANNED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStrategicPlanId() { return strategicPlanId; }
    public void setStrategicPlanId(Long strategicPlanId) { this.strategicPlanId = strategicPlanId; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getActivities() { return activities; }
    public void setActivities(String activities) { this.activities = activities; }

    public String getResponsibleParties() { return responsibleParties; }
    public void setResponsibleParties(String responsibleParties) { this.responsibleParties = responsibleParties; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper method to get month name
    public String getMonthName() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return monthNumber != null && monthNumber >= 1 && monthNumber <= 12 ? months[monthNumber - 1] : "Unknown";
    }

    @Override
    public String toString() {
        return String.format("OperationalActivity{id=%d, month=%s, title='%s', status='%s'}",
                           id, getMonthName(), title, status);
    }
}