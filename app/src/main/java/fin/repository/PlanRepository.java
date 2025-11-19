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

import fin.model.Plan;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing pricing plans
 */
public class PlanRepository {
    private final String dbUrl;

    public PlanRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Save a plan
     */
    public Plan save(Plan plan) {
        String sql = "INSERT INTO plans (name, description, price, currency, billing_cycle, is_active, " +
                    "can_access_dashboard, can_manage_companies, can_process_bank_statements, can_generate_reports, " +
                    "can_manage_payroll, can_manage_budgets, can_access_multiple_companies, max_companies, max_users, " +
                    "max_transactions_per_month, has_api_access, has_priority_support, " +
                    "created_at, updated_at, created_by, updated_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (name) DO UPDATE SET " +
                    "description = EXCLUDED.description, " +
                    "price = EXCLUDED.price, " +
                    "currency = EXCLUDED.currency, " +
                    "billing_cycle = EXCLUDED.billing_cycle, " +
                    "is_active = EXCLUDED.is_active, " +
                    "can_access_dashboard = EXCLUDED.can_access_dashboard, " +
                    "can_manage_companies = EXCLUDED.can_manage_companies, " +
                    "can_process_bank_statements = EXCLUDED.can_process_bank_statements, " +
                    "can_generate_reports = EXCLUDED.can_generate_reports, " +
                    "can_manage_payroll = EXCLUDED.can_manage_payroll, " +
                    "can_manage_budgets = EXCLUDED.can_manage_budgets, " +
                    "can_access_multiple_companies = EXCLUDED.can_access_multiple_companies, " +
                    "max_companies = EXCLUDED.max_companies, " +
                    "max_users = EXCLUDED.max_users, " +
                    "max_transactions_per_month = EXCLUDED.max_transactions_per_month, " +
                    "has_api_access = EXCLUDED.has_api_access, " +
                    "has_priority_support = EXCLUDED.has_priority_support, " +
                    "updated_at = EXCLUDED.updated_at, " +
                    "updated_by = EXCLUDED.updated_by " +
                    "RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, plan.getName());
            stmt.setString(2, plan.getDescription());
            stmt.setBigDecimal(3, plan.getPrice());
            stmt.setString(4, plan.getCurrency());
            stmt.setString(5, plan.getBillingCycle());
            stmt.setBoolean(6, plan.isActive());
            stmt.setBoolean(7, plan.isCanAccessDashboard());
            stmt.setBoolean(8, plan.isCanManageCompanies());
            stmt.setBoolean(9, plan.isCanProcessBankStatements());
            stmt.setBoolean(10, plan.isCanGenerateReports());
            stmt.setBoolean(11, plan.isCanManagePayroll());
            stmt.setBoolean(12, plan.isCanManageBudgets());
            stmt.setBoolean(13, plan.isCanAccessMultipleCompanies());
            stmt.setInt(14, plan.getMaxCompanies());
            stmt.setInt(15, plan.getMaxUsers());
            stmt.setInt(16, plan.getMaxTransactionsPerMonth());
            stmt.setBoolean(17, plan.isHasApiAccess());
            stmt.setBoolean(18, plan.isHasPrioritySupport());
            stmt.setTimestamp(19, Timestamp.valueOf(plan.getCreatedAt()));
            stmt.setTimestamp(20, Timestamp.valueOf(plan.getUpdatedAt()));
            stmt.setString(21, plan.getCreatedBy());
            stmt.setString(22, plan.getUpdatedBy());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                plan.setId(rs.getLong("id"));
            }

            return plan;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save plan", e);
        }
    }

    /**
     * Find plan by ID
     */
    public Optional<Plan> findById(Long id) {
        String sql = "SELECT * FROM plans WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToPlan(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find plan by ID", e);
        }
    }

    /**
     * Find plan by name
     */
    public Optional<Plan> findByName(String name) {
        String sql = "SELECT * FROM plans WHERE name = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToPlan(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find plan by name", e);
        }
    }

    /**
     * Get all active plans
     */
    public List<Plan> findAllActive() {
        String sql = "SELECT * FROM plans WHERE is_active = true ORDER BY price ASC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            List<Plan> plans = new ArrayList<>();
            while (rs.next()) {
                plans.add(mapResultSetToPlan(rs));
            }

            return plans;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all active plans", e);
        }
    }

    /**
     * Get trial plan (price = 0)
     */
    public Optional<Plan> findTrialPlan() {
        String sql = "SELECT * FROM plans WHERE price = 0 AND is_active = true LIMIT 1";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToPlan(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find trial plan", e);
        }
    }

    /**
     * Check if plan allows a specific feature
     */
    public boolean canPlanAccessFeature(Long planId, String feature) {
        Optional<Plan> planOpt = findById(planId);
        if (planOpt.isEmpty()) {
            return false;
        }

        Plan plan = planOpt.get();
        return plan.canUserAccessFeature(feature);
    }

    /**
     * Check if plan allows multiple companies
     */
    public boolean canPlanAccessMultipleCompanies(Long planId) {
        return canPlanAccessFeature(planId, "multiple_companies");
    }

    /**
     * Get maximum companies allowed for plan
     */
    public int getMaxCompaniesForPlan(Long planId) {
        Optional<Plan> planOpt = findById(planId);
        return planOpt.map(Plan::getMaxCompanies).orElse(1);
    }

    /**
     * Get maximum users allowed for plan
     */
    public int getMaxUsersForPlan(Long planId) {
        Optional<Plan> planOpt = findById(planId);
        return planOpt.map(Plan::getMaxUsers).orElse(1);
    }

    /**
     * Get maximum transactions per month for plan
     */
    public int getMaxTransactionsPerMonthForPlan(Long planId) {
        Optional<Plan> planOpt = findById(planId);
        return planOpt.map(Plan::getMaxTransactionsPerMonth).orElse(100);
    }

    /**
     * Update plan
     */
    public Plan update(Plan plan) {
        String sql = "UPDATE plans SET name = ?, description = ?, price = ?, currency = ?, billing_cycle = ?, " +
                    "is_active = ?, can_access_dashboard = ?, can_manage_companies = ?, " +
                    "can_process_bank_statements = ?, can_generate_reports = ?, can_manage_payroll = ?, " +
                    "can_manage_budgets = ?, can_access_multiple_companies = ?, max_companies = ?, " +
                    "max_users = ?, max_transactions_per_month = ?, has_api_access = ?, has_priority_support = ?, " +
                    "updated_at = ?, updated_by = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, plan.getName());
            stmt.setString(2, plan.getDescription());
            stmt.setBigDecimal(3, plan.getPrice());
            stmt.setString(4, plan.getCurrency());
            stmt.setString(5, plan.getBillingCycle());
            stmt.setBoolean(6, plan.isActive());
            stmt.setBoolean(7, plan.isCanAccessDashboard());
            stmt.setBoolean(8, plan.isCanManageCompanies());
            stmt.setBoolean(9, plan.isCanProcessBankStatements());
            stmt.setBoolean(10, plan.isCanGenerateReports());
            stmt.setBoolean(11, plan.isCanManagePayroll());
            stmt.setBoolean(12, plan.isCanManageBudgets());
            stmt.setBoolean(13, plan.isCanAccessMultipleCompanies());
            stmt.setInt(14, plan.getMaxCompanies());
            stmt.setInt(15, plan.getMaxUsers());
            stmt.setInt(16, plan.getMaxTransactionsPerMonth());
            stmt.setBoolean(17, plan.isHasApiAccess());
            stmt.setBoolean(18, plan.isHasPrioritySupport());
            stmt.setTimestamp(19, Timestamp.valueOf(plan.getUpdatedAt()));
            stmt.setString(20, plan.getUpdatedBy());
            stmt.setLong(21, plan.getId());

            stmt.executeUpdate();
            return plan;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update plan", e);
        }
    }

    /**
     * Deactivate plan
     */
    public void deactivatePlan(Long planId) {
        String sql = "UPDATE plans SET is_active = false, updated_at = ?, updated_by = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, "FIN");
            stmt.setLong(3, planId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to deactivate plan", e);
        }
    }

    private Plan mapResultSetToPlan(ResultSet rs) throws SQLException {
        Plan plan = new Plan();
        plan.setId(rs.getLong("id"));
        plan.setName(rs.getString("name"));
        plan.setDescription(rs.getString("description"));
        plan.setPrice(rs.getBigDecimal("price"));
        plan.setCurrency(rs.getString("currency"));
        plan.setBillingCycle(rs.getString("billing_cycle"));
        plan.setActive(rs.getBoolean("is_active"));
        plan.setCanAccessDashboard(rs.getBoolean("can_access_dashboard"));
        plan.setCanManageCompanies(rs.getBoolean("can_manage_companies"));
        plan.setCanProcessBankStatements(rs.getBoolean("can_process_bank_statements"));
        plan.setCanGenerateReports(rs.getBoolean("can_generate_reports"));
        plan.setCanManagePayroll(rs.getBoolean("can_manage_payroll"));
        plan.setCanManageBudgets(rs.getBoolean("can_manage_budgets"));
        plan.setCanAccessMultipleCompanies(rs.getBoolean("can_access_multiple_companies"));
        plan.setMaxCompanies(rs.getInt("max_companies"));
        plan.setMaxUsers(rs.getInt("max_users"));
        plan.setMaxTransactionsPerMonth(rs.getInt("max_transactions_per_month"));
        plan.setHasApiAccess(rs.getBoolean("has_api_access"));
        plan.setHasPrioritySupport(rs.getBoolean("has_priority_support"));
        plan.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        plan.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        plan.setCreatedBy(rs.getString("created_by"));
        plan.setUpdatedBy(rs.getString("updated_by"));
        return plan;
    }
}