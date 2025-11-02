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

package fin.service;

import fin.context.ApplicationContext;
import fin.model.StrategicPlan;
import fin.model.StrategicPriority;
import fin.repository.StrategicPlanningRepository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Service for Strategic Planning operations
 */
public class StrategicPlanningService {
    private final StrategicPlanningRepository repository;
    private final ApplicationContext context;

    public StrategicPlanningService(String dbUrl, ApplicationContext context) {
        this.repository = new StrategicPlanningRepository(dbUrl);
        this.context = context;
    }

    /**
     * Create a new strategic plan for a company
     */
    public StrategicPlan createStrategicPlan(Long companyId, String title, String vision, String mission, String goals) throws SQLException {
        // Validate inputs
        if (companyId == null || title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Company ID and title are required");
        }

        // Check if company exists
        CompanyService companyService = context.get(CompanyService.class);
        if (companyService.getCompanyById(companyId) == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Create strategic plan
        StrategicPlan plan = new StrategicPlan(companyId, title.trim());
        plan.setVisionStatement(vision);
        plan.setMissionStatement(mission);
        plan.setGoals(goals);

        // Set default dates (3 years)
        plan.setStartDate(LocalDate.now());
        plan.setEndDate(LocalDate.now().plusYears(3));

        return repository.saveStrategicPlan(plan);
    }

    /**
     * Get all strategic plans for a company
     */
    public List<StrategicPlan> getStrategicPlansForCompany(Long companyId) throws SQLException {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return repository.getStrategicPlansByCompany(companyId);
    }

    /**
     * Get the active strategic plan for a company
     */
    public StrategicPlan getActiveStrategicPlan(Long companyId) throws SQLException {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return repository.getActiveStrategicPlan(companyId);
    }

    /**
     * Update strategic plan status
     */
    public StrategicPlan updateStrategicPlanStatus(Long planId, String status) throws SQLException {
        // This would need to be implemented in the repository
        // For now, return null to indicate not implemented
        throw new UnsupportedOperationException("Update strategic plan status not yet implemented");
    }

    /**
     * Add a strategic priority to a plan
     */
    public StrategicPriority addStrategicPriority(Long planId, String name, String description, Integer order) throws SQLException {
        if (planId == null || name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plan ID and priority name are required");
        }

        // Validate plan exists
        StrategicPlan plan = repository.getStrategicPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("Strategic plan not found: " + planId);
        }

        StrategicPriority priority = new StrategicPriority(planId, name.trim(), order != null ? order : 1);
        priority.setDescription(description);

        return repository.saveStrategicPriority(priority);
    }

    /**
     * Get strategic priorities for a plan
     */
    public List<StrategicPriority> getStrategicPriorities(Long planId) throws SQLException {
        if (planId == null) {
            throw new IllegalArgumentException("Plan ID is required");
        }
        return repository.getStrategicPriorities(planId);
    }

    /**
     * Create a sample strategic plan with standard priorities for any company
     */
    public StrategicPlan createSampleStrategicPlan(Long companyId, String title, String vision, String mission) throws SQLException {
        if (companyId == null || title == null) {
            throw new IllegalArgumentException("Company ID and title are required");
        }

        String defaultGoals = "• Achieve operational excellence\n" +
                            "• Foster stakeholder engagement\n" +
                            "• Develop sustainable practices\n" +
                            "• Build strong organizational foundation";

        StrategicPlan plan = createStrategicPlan(companyId, title, vision, mission, defaultGoals);

        // Add standard strategic priorities
        addStrategicPriority(plan.getId(), "Operational Excellence", "Streamline processes and improve efficiency", 1);
        addStrategicPriority(plan.getId(), "Stakeholder Engagement", "Build strong relationships with customers and partners", 2);
        addStrategicPriority(plan.getId(), "Innovation", "Foster creativity and technological advancement", 3);
        addStrategicPriority(plan.getId(), "Sustainability", "Develop environmentally and socially responsible practices", 4);

        return plan;
    }
}