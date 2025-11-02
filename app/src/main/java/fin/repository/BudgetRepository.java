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

import fin.model.Budget;
import fin.model.BudgetCategory;
import fin.model.BudgetItem;
import fin.model.BudgetMonthlyAllocation;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Budget operations
 */
public class BudgetRepository {
    private final String dbUrl;

    public BudgetRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Save a budget
     */
    public Budget saveBudget(Budget budget) throws SQLException {
        String sql = "INSERT INTO budgets (company_id, fiscal_period_id, title, description, budget_year, status, total_revenue, total_expenses, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budget.getCompanyId());
            stmt.setObject(2, budget.getFiscalPeriodId());
            stmt.setString(3, budget.getTitle());
            stmt.setString(4, budget.getDescription());
            stmt.setInt(5, budget.getBudgetYear());
            stmt.setString(6, budget.getStatus());
            stmt.setBigDecimal(7, budget.getTotalRevenue());
            stmt.setBigDecimal(8, budget.getTotalExpenses());
            stmt.setTimestamp(9, Timestamp.valueOf(budget.getCreatedAt()));
            stmt.setTimestamp(10, Timestamp.valueOf(budget.getUpdatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                budget.setId(rs.getLong(1));
            }
            return budget;
        }
    }

    /**
     * Get budgets for a company
     */
    public List<Budget> getBudgetsByCompany(Long companyId) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE company_id = ? ORDER BY budget_year DESC, created_at DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            List<Budget> budgets = new ArrayList<>();
            while (rs.next()) {
                budgets.add(mapResultSetToBudget(rs));
            }
            return budgets;
        }
    }

    /**
     * Get budget by ID with categories and items
     */
    public Budget getBudgetWithDetails(Long budgetId) throws SQLException {
        Budget budget = getBudgetById(budgetId);
        if (budget != null) {
            budget.setCategories(getBudgetCategories(budgetId));
            for (BudgetCategory category : budget.getCategories()) {
                category.setItems(getBudgetItems(category.getId()));
            }
        }
        return budget;
    }

    /**
     * Get budget by ID
     */
    public Budget getBudgetById(Long budgetId) throws SQLException {
        String sql = "SELECT * FROM budgets WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToBudget(rs);
            }
            return null;
        }
    }

    /**
     * Save budget category
     */
    public BudgetCategory saveBudgetCategory(BudgetCategory category) throws SQLException {
        String sql = "INSERT INTO budget_categories (budget_id, name, category_type, description, allocated_percentage, total_allocated, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, category.getBudgetId());
            stmt.setString(2, category.getName());
            stmt.setString(3, category.getCategoryType());
            stmt.setString(4, category.getDescription());
            stmt.setBigDecimal(5, category.getAllocatedPercentage());
            stmt.setBigDecimal(6, category.getTotalAllocated());
            stmt.setTimestamp(7, Timestamp.valueOf(category.getCreatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                category.setId(rs.getLong(1));
            }
            return category;
        }
    }

    /**
     * Get budget categories for a budget
     */
    public List<BudgetCategory> getBudgetCategories(Long budgetId) throws SQLException {
        String sql = "SELECT * FROM budget_categories WHERE budget_id = ? ORDER BY category_type, name";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetId);
            ResultSet rs = stmt.executeQuery();

            List<BudgetCategory> categories = new ArrayList<>();
            while (rs.next()) {
                BudgetCategory category = new BudgetCategory();
                category.setId(rs.getLong("id"));
                category.setBudgetId(rs.getLong("budget_id"));
                category.setName(rs.getString("name"));
                category.setCategoryType(rs.getString("category_type"));
                category.setDescription(rs.getString("description"));
                category.setAllocatedPercentage(rs.getBigDecimal("allocated_percentage"));
                category.setTotalAllocated(rs.getBigDecimal("total_allocated"));
                category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                categories.add(category);
            }
            return categories;
        }
    }

    /**
     * Save budget item
     */
    public BudgetItem saveBudgetItem(BudgetItem item) throws SQLException {
        String sql = "INSERT INTO budget_items (budget_category_id, account_id, description, annual_amount, notes, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, item.getBudgetCategoryId());
            stmt.setObject(2, item.getAccountId());
            stmt.setString(3, item.getDescription());
            stmt.setBigDecimal(4, item.getAnnualAmount());
            stmt.setString(5, item.getNotes());
            stmt.setTimestamp(6, Timestamp.valueOf(item.getCreatedAt()));
            stmt.setTimestamp(7, Timestamp.valueOf(item.getUpdatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                item.setId(rs.getLong(1));
            }
            return item;
        }
    }

    /**
     * Get budget items for a category
     */
    public List<BudgetItem> getBudgetItems(Long budgetCategoryId) throws SQLException {
        String sql = "SELECT * FROM budget_items WHERE budget_category_id = ? ORDER BY description";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetCategoryId);
            ResultSet rs = stmt.executeQuery();

            List<BudgetItem> items = new ArrayList<>();
            while (rs.next()) {
                BudgetItem item = new BudgetItem();
                item.setId(rs.getLong("id"));
                item.setBudgetCategoryId(rs.getLong("budget_category_id"));
                item.setAccountId(rs.getObject("account_id") != null ? rs.getLong("account_id") : null);
                item.setDescription(rs.getString("description"));
                item.setAnnualAmount(rs.getBigDecimal("annual_amount"));
                item.setNotes(rs.getString("notes"));
                item.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                item.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                items.add(item);
            }
            return items;
        }
    }

    private Budget mapResultSetToBudget(ResultSet rs) throws SQLException {
        Budget budget = new Budget();
        budget.setId(rs.getLong("id"));
        budget.setCompanyId(rs.getLong("company_id"));
        budget.setFiscalPeriodId(rs.getObject("fiscal_period_id") != null ? rs.getLong("fiscal_period_id") : null);
        budget.setTitle(rs.getString("title"));
        budget.setDescription(rs.getString("description"));
        budget.setBudgetYear(rs.getInt("budget_year"));
        budget.setStatus(rs.getString("status"));
        budget.setTotalRevenue(rs.getBigDecimal("total_revenue"));
        budget.setTotalExpenses(rs.getBigDecimal("total_expenses"));
        budget.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        budget.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());

        Timestamp approvedAt = rs.getTimestamp("approved_at");
        if (approvedAt != null) {
            budget.setApprovedAt(approvedAt.toLocalDateTime());
        }
        budget.setApprovedBy(rs.getString("approved_by"));

        return budget;
    }

    /**
     * Save monthly budget allocation
     */
    public BudgetMonthlyAllocation saveMonthlyAllocation(BudgetMonthlyAllocation allocation) throws SQLException {
        String sql = "INSERT INTO budget_monthly_allocations (budget_item_id, month_number, budgeted_amount, actual_amount, variance_amount, notes, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, allocation.getBudgetItemId());
            stmt.setInt(2, allocation.getMonthNumber());
            stmt.setBigDecimal(3, allocation.getAllocatedAmount());
            stmt.setBigDecimal(4, allocation.getActualAmount());
            stmt.setBigDecimal(5, allocation.getVarianceAmount());
            stmt.setString(6, allocation.getNotes());
            stmt.setTimestamp(7, Timestamp.valueOf(allocation.getCreatedAt()));
            stmt.setTimestamp(8, Timestamp.valueOf(allocation.getUpdatedAt()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                allocation.setId(rs.getLong(1));
            }
            return allocation;
        }
    }

    /**
     * Get monthly allocations for a budget item
     */
    public List<BudgetMonthlyAllocation> getMonthlyAllocations(Long budgetItemId) throws SQLException {
        String sql = "SELECT * FROM budget_monthly_allocations WHERE budget_item_id = ? ORDER BY month_number";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetItemId);
            ResultSet rs = stmt.executeQuery();

            List<BudgetMonthlyAllocation> allocations = new ArrayList<>();
            while (rs.next()) {
                BudgetMonthlyAllocation allocation = new BudgetMonthlyAllocation();
                allocation.setId(rs.getLong("id"));
                allocation.setBudgetItemId(rs.getLong("budget_item_id"));
                allocation.setMonthNumber(rs.getInt("month_number"));
                allocation.setAllocatedAmount(rs.getBigDecimal("budgeted_amount"));
                allocation.setActualAmount(rs.getBigDecimal("actual_amount"));
                allocation.setVarianceAmount(rs.getBigDecimal("variance_amount"));
                allocation.setNotes(rs.getString("notes"));
                allocation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                allocation.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                allocations.add(allocation);
            }
            return allocations;
        }
    }

    /**
     * Get monthly allocation for specific budget item and month
     */
    public BudgetMonthlyAllocation getMonthlyAllocation(Long budgetItemId, int monthNumber) throws SQLException {
        String sql = "SELECT * FROM budget_monthly_allocations WHERE budget_item_id = ? AND month_number = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetItemId);
            stmt.setInt(2, monthNumber);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                BudgetMonthlyAllocation allocation = new BudgetMonthlyAllocation();
                allocation.setId(rs.getLong("id"));
                allocation.setBudgetItemId(rs.getLong("budget_item_id"));
                allocation.setMonthNumber(rs.getInt("month_number"));
                allocation.setAllocatedAmount(rs.getBigDecimal("budgeted_amount"));
                allocation.setActualAmount(rs.getBigDecimal("actual_amount"));
                allocation.setVarianceAmount(rs.getBigDecimal("variance_amount"));
                allocation.setNotes(rs.getString("notes"));
                allocation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                allocation.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                return allocation;
            }
            return null;
        }
    }

    /**
     * Update actual amount for monthly allocation
     */
    public void updateActualAmount(Long allocationId, BigDecimal actualAmount) throws SQLException {
        String sql = "UPDATE budget_monthly_allocations SET actual_amount = ?, variance_amount = budgeted_amount - ?, updated_at = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, actualAmount);
            stmt.setBigDecimal(2, actualAmount);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(4, allocationId);

            stmt.executeUpdate();
        }
    }

    /**
     * Get all monthly allocations for a budget (across all items)
     */
    public List<BudgetMonthlyAllocation> getAllMonthlyAllocationsForBudget(Long budgetId) throws SQLException {
        String sql = "SELECT bma.* FROM budget_monthly_allocations bma " +
                    "JOIN budget_items bi ON bma.budget_item_id = bi.id " +
                    "JOIN budget_categories bc ON bi.budget_category_id = bc.id " +
                    "WHERE bc.budget_id = ? ORDER BY bi.description, bma.month_number";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, budgetId);
            ResultSet rs = stmt.executeQuery();

            List<BudgetMonthlyAllocation> allocations = new ArrayList<>();
            while (rs.next()) {
                BudgetMonthlyAllocation allocation = new BudgetMonthlyAllocation();
                allocation.setId(rs.getLong("id"));
                allocation.setBudgetItemId(rs.getLong("budget_item_id"));
                allocation.setMonthNumber(rs.getInt("month_number"));
                allocation.setAllocatedAmount(rs.getBigDecimal("budgeted_amount"));
                allocation.setActualAmount(rs.getBigDecimal("actual_amount"));
                allocation.setVarianceAmount(rs.getBigDecimal("variance_amount"));
                allocation.setNotes(rs.getString("notes"));
                allocation.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                allocation.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                allocations.add(allocation);
            }
            return allocations;
        }
    }
}