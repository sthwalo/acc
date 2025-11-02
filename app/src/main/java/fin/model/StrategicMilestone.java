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