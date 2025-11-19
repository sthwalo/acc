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

package fin.repository.jpa;

import fin.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity
 * Replaces manual JDBC UserRepository with Spring Data JPA
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by username or email
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * Find active users
     */
    List<User> findByActiveTrue();

    /**
     * Find users created after a specific date
     */
    @Query("SELECT u FROM User u WHERE u.createdAt > :createdAfter ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedAfter(@Param("createdAfter") java.time.LocalDateTime createdAfter);

    /**
     * Find users who haven't logged in since a specific date
     */
    @Query("SELECT u FROM User u WHERE u.lastLoginAt < :loginBefore OR u.lastLoginAt IS NULL")
    List<User> findInactiveUsers(@Param("loginBefore") java.time.LocalDateTime loginBefore);

    /**
     * Count active users
     */
    long countByActiveTrue();

    /**
     * Check if username exists (excluding specific user ID)
     */
    boolean existsByUsernameAndIdNot(String username, Long id);

    /**
     * Check if email exists (excluding specific user ID)
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
}