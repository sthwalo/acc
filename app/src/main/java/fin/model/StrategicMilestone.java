package fin.model;

/**
 * Strategic Milestone Model
 * Represents measurable outcomes and KPIs for strategic initiatives
 */
public class StrategicMilestone {
    private Long id;
    private Long strategicInitiativeId;
    private String title;
    private String description;
    private String targetDate;
    private String completionDate;
    private String status; // PENDING, ACHIEVED, DELAYED, CANCELLED

    public StrategicMilestone() {}

    public StrategicMilestone(Long strategicInitiativeId, String title, String description,
                             String targetDate, String status) {
        this.strategicInitiativeId = strategicInitiativeId;
        this.title = title;
        this.description = description;
        this.targetDate = targetDate;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStrategicInitiativeId() {
        return strategicInitiativeId;
    }

    public void setStrategicInitiativeId(Long strategicInitiativeId) {
        this.strategicInitiativeId = strategicInitiativeId;
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

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "StrategicMilestone{" +
                "id=" + id +
                ", strategicInitiativeId=" + strategicInitiativeId +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", targetDate='" + targetDate + '\'' +
                ", completionDate='" + completionDate + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}