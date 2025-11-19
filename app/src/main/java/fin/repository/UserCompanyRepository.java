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

import fin.model.UserCompany;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing user-company relationships
 */
public class UserCompanyRepository {
    private final String dbUrl;

    public UserCompanyRepository(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Save a user-company relationship
     */
    public UserCompany save(UserCompany userCompany) {
        String sql = "INSERT INTO user_companies (user_id, company_id, role, is_active, created_at, updated_at, created_by, updated_by) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON CONFLICT (user_id, company_id) DO UPDATE SET " +
                    "role = EXCLUDED.role, " +
                    "is_active = EXCLUDED.is_active, " +
                    "updated_at = EXCLUDED.updated_at, " +
                    "updated_by = EXCLUDED.updated_by " +
                    "RETURNING id";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userCompany.getUserId());
            stmt.setLong(2, userCompany.getCompanyId());
            stmt.setString(3, userCompany.getRole());
            stmt.setBoolean(4, userCompany.isActive());
            stmt.setTimestamp(5, Timestamp.valueOf(userCompany.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(userCompany.getUpdatedAt()));
            stmt.setString(7, userCompany.getCreatedBy());
            stmt.setString(8, userCompany.getUpdatedBy());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                userCompany.setId(rs.getLong("id"));
            }

            return userCompany;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user-company relationship", e);
        }
    }

    /**
     * Find user-company relationship by ID
     */
    public Optional<UserCompany> findById(Long id) {
        String sql = "SELECT * FROM user_companies WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUserCompany(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user-company relationship by ID", e);
        }
    }

    /**
     * Find user-company relationship by user and company IDs
     */
    public Optional<UserCompany> findByUserAndCompany(Long userId, Long companyId) {
        String sql = "SELECT * FROM user_companies WHERE user_id = ? AND company_id = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, companyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUserCompany(rs));
            }

            return Optional.empty();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user-company relationship", e);
        }
    }

    /**
     * Get all companies a user has access to
     */
    public List<UserCompany> findCompaniesByUser(Long userId) {
        String sql = "SELECT * FROM user_companies WHERE user_id = ? AND is_active = true ORDER BY created_at";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            List<UserCompany> userCompanies = new ArrayList<>();
            while (rs.next()) {
                userCompanies.add(mapResultSetToUserCompany(rs));
            }

            return userCompanies;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find companies for user", e);
        }
    }

    /**
     * Get all users with access to a company
     */
    public List<UserCompany> findUsersByCompany(Long companyId) {
        String sql = "SELECT * FROM user_companies WHERE company_id = ? AND is_active = true ORDER BY created_at";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            List<UserCompany> userCompanies = new ArrayList<>();
            while (rs.next()) {
                userCompanies.add(mapResultSetToUserCompany(rs));
            }

            return userCompanies;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find users for company", e);
        }
    }

    /**
     * Check if user has access to a specific company
     */
    public boolean hasUserAccessToCompany(Long userId, Long companyId) {
        String sql = "SELECT COUNT(*) FROM user_companies WHERE user_id = ? AND company_id = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, companyId);
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user access to company", e);
        }
    }

    /**
     * Check if user has a specific role in a company
     */
    public boolean hasUserRoleInCompany(Long userId, Long companyId, String role) {
        String sql = "SELECT COUNT(*) FROM user_companies WHERE user_id = ? AND company_id = ? AND role = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            stmt.setLong(2, companyId);
            stmt.setString(3, role);
            ResultSet rs = stmt.executeQuery();

            return rs.next() && rs.getInt(1) > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user role in company", e);
        }
    }

    /**
     * Update user-company relationship
     */
    public UserCompany update(UserCompany userCompany) {
        String sql = "UPDATE user_companies SET role = ?, is_active = ?, updated_at = ?, updated_by = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userCompany.getRole());
            stmt.setBoolean(2, userCompany.isActive());
            stmt.setTimestamp(3, Timestamp.valueOf(userCompany.getUpdatedAt()));
            stmt.setString(4, userCompany.getUpdatedBy());
            stmt.setLong(5, userCompany.getId());

            stmt.executeUpdate();
            return userCompany;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user-company relationship", e);
        }
    }

    /**
     * Remove user access from company
     */
    public void removeUserFromCompany(Long userId, Long companyId) {
        String sql = "UPDATE user_companies SET is_active = false, updated_at = ?, updated_by = ? WHERE user_id = ? AND company_id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(2, "FIN");
            stmt.setLong(3, userId);
            stmt.setLong(4, companyId);

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove user from company", e);
        }
    }

    /**
     * Grant user access to company
     */
    public UserCompany grantUserAccessToCompany(Long userId, Long companyId, String role, String grantedBy) {
        UserCompany userCompany = new UserCompany(userId, companyId, role);
        userCompany.setCreatedBy(grantedBy);
        userCompany.setUpdatedBy(grantedBy);
        return save(userCompany);
    }

    /**
     * Get count of active companies for user
     */
    public int getCompanyCountForUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM user_companies WHERE user_id = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count companies for user", e);
        }
    }

    /**
     * Get count of active users for company
     */
    public int getUserCountForCompany(Long companyId) {
        String sql = "SELECT COUNT(*) FROM user_companies WHERE company_id = ? AND is_active = true";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            return rs.next() ? rs.getInt(1) : 0;

        } catch (SQLException e) {
            throw new RuntimeException("Failed to count users for company", e);
        }
    }

    private UserCompany mapResultSetToUserCompany(ResultSet rs) throws SQLException {
        UserCompany userCompany = new UserCompany();
        userCompany.setId(rs.getLong("id"));
        userCompany.setUserId(rs.getLong("user_id"));
        userCompany.setCompanyId(rs.getLong("company_id"));
        userCompany.setRole(rs.getString("role"));
        userCompany.setActive(rs.getBoolean("is_active"));
        userCompany.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        userCompany.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        userCompany.setCreatedBy(rs.getString("created_by"));
        userCompany.setUpdatedBy(rs.getString("updated_by"));
        return userCompany;
    }
}