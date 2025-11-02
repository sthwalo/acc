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

package fin.repository;

import fin.model.StrategicPlan;
import fin.model.StrategicPriority;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Strategic Planning operations
 */
public class StrategicPlanningRepository {
    private final String dbUrl;

    public StrategicPlanningRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Save a strategic plan
     */
    public StrategicPlan saveStrategicPlan(StrategicPlan plan) throws SQLException {
        String sql = "INSERT INTO strategic_plans (company_id, title, vision_statement, mission_statement, goals, status, start_date, end_date, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, plan.getCompanyId());
            stmt.setString(2, plan.getTitle());
            stmt.setString(3, plan.getVisionStatement());
            stmt.setString(4, plan.getMissionStatement());
            stmt.setString(5, plan.getGoals());
            stmt.setString(6, plan.getStatus());
            stmt.setDate(7, plan.getStartDate() != null ? Date.valueOf(plan.getStartDate()) : null);
            stmt.setDate(8, plan.getEndDate() != null ? Date.valueOf(plan.getEndDate()) : null);
            stmt.setTimestamp(9, Timestamp.valueOf(plan.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(plan.getUpdatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                plan.setId(rs.getLong(1));
            }
            return plan;
        }
    }

    /**
     * Get strategic plans for a company
     */
    public List<StrategicPlan> getStrategicPlansByCompany(Long companyId) throws SQLException {
        String sql = "SELECT * FROM strategic_plans WHERE company_id = ? ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            List<StrategicPlan> plans = new ArrayList<>();
            while (rs.next()) {
                plans.add(mapResultSetToStrategicPlan(rs));
            }
            return plans;
        }
    }

    /**
     * Get active strategic plan for a company
     */
    public StrategicPlan getActiveStrategicPlan(Long companyId) throws SQLException {
        String sql = "SELECT * FROM strategic_plans WHERE company_id = ? AND status = 'ACTIVE' LIMIT 1";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToStrategicPlan(rs);
            }
            return null;
        }
    }

    /**
     * Get strategic plan by ID
     */
    public StrategicPlan getStrategicPlanById(Long planId) throws SQLException {
        String sql = "SELECT * FROM strategic_plans WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, planId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToStrategicPlan(rs);
            }
            return null;
        }
    }

    /**
     * Save a strategic priority
     */
    public StrategicPriority saveStrategicPriority(StrategicPriority priority) throws SQLException {
        String sql = "INSERT INTO strategic_priorities (strategic_plan_id, name, description, priority_order, created_at) " +
                    "VALUES (?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, priority.getStrategicPlanId());
            stmt.setString(2, priority.getName());
            stmt.setString(3, priority.getDescription());
            stmt.setInt(4, priority.getPriorityOrder());
            stmt.setTimestamp(5, Timestamp.valueOf(priority.getCreatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                priority.setId(rs.getLong(1));
            }
            return priority;
        }
    }

    /**
     * Get strategic priorities for a plan
     */
    public List<StrategicPriority> getStrategicPriorities(Long strategicPlanId) throws SQLException {
        String sql = "SELECT * FROM strategic_priorities WHERE strategic_plan_id = ? ORDER BY priority_order";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, strategicPlanId);
            ResultSet rs = stmt.executeQuery();

            List<StrategicPriority> priorities = new ArrayList<>();
            while (rs.next()) {
                StrategicPriority priority = new StrategicPriority();
                priority.setId(rs.getLong("id"));
                priority.setStrategicPlanId(rs.getLong("strategic_plan_id"));
                priority.setName(rs.getString("name"));
                priority.setDescription(rs.getString("description"));
                priority.setPriorityOrder(rs.getInt("priority_order"));
                priority.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                priorities.add(priority);
            }
            return priorities;
        }
    }

    private StrategicPlan mapResultSetToStrategicPlan(ResultSet rs) throws SQLException {
        StrategicPlan plan = new StrategicPlan();
        plan.setId(rs.getLong("id"));
        plan.setCompanyId(rs.getLong("company_id"));
        plan.setTitle(rs.getString("title"));
        plan.setVisionStatement(rs.getString("vision_statement"));
        plan.setMissionStatement(rs.getString("mission_statement"));
        plan.setGoals(rs.getString("goals"));
        plan.setStatus(rs.getString("status"));

        Date startDate = rs.getDate("start_date");
        if (startDate != null) {
            plan.setStartDate(startDate.toLocalDate());
        }

        Date endDate = rs.getDate("end_date");
        if (endDate != null) {
            plan.setEndDate(endDate.toLocalDate());
        }

        plan.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        plan.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        return plan;
    }
}