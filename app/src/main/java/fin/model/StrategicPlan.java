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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Strategic Plan model representing an organization's strategic planning document
 */
public class StrategicPlan {
    private Long id;
    private Long companyId;
    private String title;
    private String visionStatement;
    private String missionStatement;
    private String goals;
    private String strategicPriorities;
    private String implementationTimeline;
    private String financialProjections;
    private String budgetAllocation;
    private String status; // DRAFT, ACTIVE, ARCHIVED
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public StrategicPlan() {}

    public StrategicPlan(Long companyId, String title) {
        this.companyId = companyId;
        this.title = title;
        this.status = "DRAFT";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getVisionStatement() { return visionStatement; }
    public void setVisionStatement(String visionStatement) { this.visionStatement = visionStatement; }

    public String getMissionStatement() { return missionStatement; }
    public void setMissionStatement(String missionStatement) { this.missionStatement = missionStatement; }

    public String getGoals() { return goals; }
    public void setGoals(String goals) { this.goals = goals; }

    public String getStrategicPriorities() { return strategicPriorities; }
    public void setStrategicPriorities(String strategicPriorities) { this.strategicPriorities = strategicPriorities; }

    public String getImplementationTimeline() { return implementationTimeline; }
    public void setImplementationTimeline(String implementationTimeline) { this.implementationTimeline = implementationTimeline; }

    public String getFinancialProjections() { return financialProjections; }
    public void setFinancialProjections(String financialProjections) { this.financialProjections = financialProjections; }

    public String getBudgetAllocation() { return budgetAllocation; }
    public void setBudgetAllocation(String budgetAllocation) { this.budgetAllocation = budgetAllocation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("StrategicPlan{id=%d, title='%s', status='%s'}", id, title, status);
    }
}