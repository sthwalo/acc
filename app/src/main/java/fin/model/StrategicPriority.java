/*
 * FIN Financial Management System
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 * 
 * This source code is licensed under the Apache License 2.0.
 * Commercial use of the APPLICATION requires separate licensing.
 * 
 * Contains proprietary algorithms and business logic.
 * Unauthorized commercial use is strictly prohibited.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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