package fin.model;

import java.time.LocalDateTime;

/**
 * Strategic Priority model representing key priority areas within a strategic plan
 * (e.g., Academic Excellence, Student Well-being, Community Engagement, Infrastructure Development)
 */
public class StrategicPriority {
    private Long id;
    private Long strategicPlanId;
    private String name;
    private String description;
    private Integer priorityOrder;
    private LocalDateTime createdAt;

    // Constructors
    public StrategicPriority() {}

    public StrategicPriority(Long strategicPlanId, String name, Integer priorityOrder) {
        this.strategicPlanId = strategicPlanId;
        this.name = name;
        this.priorityOrder = priorityOrder;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStrategicPlanId() { return strategicPlanId; }
    public void setStrategicPlanId(Long strategicPlanId) { this.strategicPlanId = strategicPlanId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriorityOrder() { return priorityOrder; }
    public void setPriorityOrder(Integer priorityOrder) { this.priorityOrder = priorityOrder; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("StrategicPriority{id=%d, name='%s', order=%d}", id, name, priorityOrder);
    }
}