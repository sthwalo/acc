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