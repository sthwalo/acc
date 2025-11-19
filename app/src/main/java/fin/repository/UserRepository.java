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

import fin.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for User entities
 */
public class UserRepository implements BaseRepository<User, Long> {
    private final String dbUrl;

    // PreparedStatement parameter indices for INSERT operation
    private static final int PARAM_EMAIL = 1;
    private static final int PARAM_PASSWORD_HASH = 2;
    private static final int PARAM_SALT = 3;
    private static final int PARAM_FIRST_NAME = 4;
    private static final int PARAM_LAST_NAME = 5;
    private static final int PARAM_ROLE = 6;
    private static final int PARAM_PLAN_ID = 7;
    private static final int PARAM_IS_ACTIVE = 8;
    private static final int PARAM_CREATED_BY = 9;
    private static final int PARAM_CREATED_AT = 10;
    private static final int PARAM_UPDATED_AT = 11;

    public UserRepository(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }

    @Override
    public User save(User user) {
        String sql = user.getId() == null ?
            "INSERT INTO users (email, password_hash, salt, first_name, last_name, role, plan_id, is_active, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)" :
            "UPDATE users SET email = ?, password_hash = ?, salt = ?, first_name = ?, last_name = ?, role = ?, plan_id = ?, is_active = ?, updated_by = ?, updated_at = ?, last_login_at = ? WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(PARAM_EMAIL, user.getEmail());
            stmt.setString(PARAM_PASSWORD_HASH, user.getPasswordHash());
            stmt.setString(PARAM_SALT, user.getSalt());
            stmt.setString(PARAM_FIRST_NAME, user.getFirstName());
            stmt.setString(PARAM_LAST_NAME, user.getLastName());
            stmt.setString(PARAM_ROLE, user.getRole());
            stmt.setLong(PARAM_PLAN_ID, user.getPlanId());
            stmt.setBoolean(PARAM_IS_ACTIVE, user.isActive());

            if (user.getId() == null) {
                // INSERT operation
                stmt.setString(PARAM_CREATED_BY, user.getCreatedBy());
                stmt.setTimestamp(PARAM_CREATED_AT, Timestamp.valueOf(user.getCreatedAt()));
                stmt.setTimestamp(PARAM_UPDATED_AT, Timestamp.valueOf(user.getUpdatedAt()));
            } else {
                // UPDATE operation
                stmt.setString(9, user.getUpdatedBy()); // updated_by
                stmt.setTimestamp(10, Timestamp.valueOf(user.getUpdatedAt())); // updated_at
                if (user.getLastLoginAt() != null) {
                    stmt.setTimestamp(11, Timestamp.valueOf(user.getLastLoginAt())); // last_login_at
                } else {
                    stmt.setNull(11, Types.TIMESTAMP);
                }
                stmt.setLong(12, user.getId()); // WHERE id
            }

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            if (user.getId() == null) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            return user;

        } catch (SQLException e) {
            throw new RuntimeException("Error saving user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by ID: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;

        } catch (SQLException e) {
            throw new RuntimeException("Error finding all users: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Error deleting user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(Long id) {
        String sql = "SELECT 1 FROM users WHERE id = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            throw new RuntimeException("Error checking user existence: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(User user) {
        deleteById(user.getId());
    }

    /**
     * Find user by email
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToUser(rs);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error finding user by email: " + e.getMessage(), e);
        }

        return null;
    }

    /**
     * Find users by company ID
     */
    public List<User> findByCompanyId(Long companyId) {
        String sql = "SELECT * FROM users WHERE company_id = ? ORDER BY created_at DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, companyId);
            ResultSet rs = stmt.executeQuery();

            List<User> users = new ArrayList<>();
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            return users;

        } catch (SQLException e) {
            throw new RuntimeException("Error finding users by company: " + e.getMessage(), e);
        }
    }

    /**
     * Update user
     */
    public void update(User user) {
        save(user); // Since save handles both insert and update
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setRole(rs.getString("role"));
        user.setPlanId(rs.getLong("plan_id"));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        user.setCreatedBy(rs.getString("created_by"));
        user.setUpdatedBy(rs.getString("updated_by"));

        Timestamp lastLoginTs = rs.getTimestamp("last_login_at");
        if (lastLoginTs != null) {
            user.setLastLoginAt(lastLoginTs.toLocalDateTime());
        }

        return user;
    }
}