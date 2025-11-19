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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for User entity
 * Replaces manual JDBC UserRepository with Spring Data JPA
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     */
    User findByEmail(String email);

    /**
     * Find users by role
     */
    List<User> findByRole(String role);

    /**
     * Find active users only
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY u.createdAt DESC")
    List<User> findActiveUsers();

    /**
     * Find users by plan ID
     */
    List<User> findByPlanId(Long planId);

    /**
     * Find users created after a specific date
     */
    @Query("SELECT u FROM User u WHERE u.createdAt > :createdAfter ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedAfter(@Param("createdAfter") java.time.LocalDateTime createdAfter);

    /**
     * Count users by role
     */
    long countByRole(String role);

    /**
     * Check if user exists by email (excluding specific ID)
     */
    boolean existsByEmailAndIdNot(String email, Long id);
}