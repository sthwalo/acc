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

import fin.entity.*;
import fin.repository.JdbcBaseRepository;
import fin.exception.CategoryException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing account categories.
 * Follows Single Responsibility Principle - handles only category CRUD operations.
 */
public class CategoryManagementService extends JdbcBaseRepository {
    private static final Logger LOGGER = Logger.getLogger(CategoryManagementService.class.getName());

    // Cache for better performance
    private final Map<Long, List<AccountCategory>> categoriesByCompany = new HashMap<>();

    public CategoryManagementService(String dbUrl) {
        super(dbUrl);
    }

    /**
     * Creates a new account category.
     */
    public AccountCategory createCategory(String name, String description,
                                        AccountType type, Company company) {
        AccountCategory category = new AccountCategory(name, description, type, company);
        saveCategory(category);
        return category;
    }

    /**
     * Saves a category to the database.
     */
    public void saveCategory(AccountCategory category) {
        String sql =
            """
            INSERT INTO account_categories (name, description, account_type_id, company_id, is_active)
            VALUES (?, ?,
                (SELECT id FROM account_types WHERE code = ?),
                ?, ?)
            ON CONFLICT(company_id, name) DO UPDATE SET
                description = excluded.description,
                account_type_id = excluded.account_type_id,
                is_active = excluded.is_active,
                updated_at = CURRENT_TIMESTAMP
            RETURNING id, created_at, updated_at
            """;

        try {
            executeQuery(sql, rs -> {
                if (rs.next()) {
                    category.setId(rs.getLong("id"));
                    category.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    category.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
                return category;
            }, category.getName(), category.getDescription(), category.getAccountType().getCode(),
               category.getCompany().getId(), category.isActive());

            // Clear cache
            categoriesByCompany.remove(category.getCompany().getId());

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error saving account category", e);
            throw new CategoryException("Failed to save account category", e);
        }
    }

    /**
     * Retrieves all categories for a company.
     */
    public List<AccountCategory> getCategoriesByCompany(Long companyId) {
        // Check cache first
        if (categoriesByCompany.containsKey(companyId)) {
            return categoriesByCompany.get(companyId);
        }

        String sql =
                "SELECT c.*, t.code as type_code, t.name as type_name, t.normal_balance " +
                "FROM account_categories c " +
                "JOIN account_types t ON c.account_type_id = t.id " +
                "WHERE c.company_id = ? AND c.is_active = true " +
                "ORDER BY t.code, c.name";

        try {
            List<AccountCategory> categories = executeQuery(sql, rs -> buildCategoryFromResultSet(rs, companyId), companyId);

            // Update cache
            categoriesByCompany.put(companyId, categories);

            return categories;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching account categories", e);
            throw new CategoryException("Failed to fetch account categories", e);
        }
    }

    /**
     * Builds an AccountCategory object from a ResultSet.
     */
    private AccountCategory buildCategoryFromResultSet(ResultSet rs, Long companyId) throws SQLException {
        AccountCategory category = new AccountCategory();
        category.setId(rs.getLong("id"));
        category.setName(rs.getString("name"));
        category.setDescription(rs.getString("description"));
        category.setActive(rs.getBoolean("is_active"));

        // Set account type
        String typeCode = rs.getString("type_code");
        category.setAccountType(AccountType.fromCode(typeCode));

        // Set company (lightweight, just ID)
        Company company = new Company();
        company.setId(companyId);
        category.setCompany(company);

        return category;
    }

    /**
     * Clears the cache for a specific company.
     */
    public void clearCache(Long companyId) {
        categoriesByCompany.remove(companyId);
    }
}