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

import fin.model.UserCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for UserCompany entity
 * Replaces manual JDBC UserCompanyRepository with Spring Data JPA
 */
@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

    /**
     * Find all companies a user has access to
     */
    @Query("SELECT uc FROM UserCompany uc WHERE uc.userId = :userId AND uc.active = true ORDER BY uc.createdAt")
    List<UserCompany> findCompaniesByUser(@Param("userId") Long userId);

    /**
     * Find all users with access to a company
     */
    @Query("SELECT uc FROM UserCompany uc WHERE uc.companyId = :companyId AND uc.active = true ORDER BY uc.createdAt")
    List<UserCompany> findUsersByCompany(@Param("companyId") Long companyId);

    /**
     * Check if user has access to a specific company
     */
    @Query("SELECT COUNT(uc) > 0 FROM UserCompany uc WHERE uc.userId = :userId AND uc.companyId = :companyId AND uc.active = true")
    boolean hasUserAccessToCompany(@Param("userId") Long userId, @Param("companyId") Long companyId);

    /**
     * Check if user has a specific role in a company
     */
    @Query("SELECT COUNT(uc) > 0 FROM UserCompany uc WHERE uc.userId = :userId AND uc.companyId = :companyId AND uc.role = :role AND uc.active = true")
    boolean hasUserRoleInCompany(@Param("userId") Long userId, @Param("companyId") Long companyId, @Param("role") String role);

    /**
     * Find user-company relationship by user and company
     */
    UserCompany findByUserIdAndCompanyId(Long userId, Long companyId);

    /**
     * Find all active relationships for a user
     */
    List<UserCompany> findByUserIdAndActiveTrue(Long userId);

    /**
     * Find all active relationships for a company
     */
    List<UserCompany> findByCompanyIdAndActiveTrue(Long companyId);

    /**
     * Count active companies for user
     */
    @Query("SELECT COUNT(uc) FROM UserCompany uc WHERE uc.userId = :userId AND uc.active = true")
    long countCompaniesForUser(@Param("userId") Long userId);

    /**
     * Count active users for company
     */
    @Query("SELECT COUNT(uc) FROM UserCompany uc WHERE uc.companyId = :companyId AND uc.active = true")
    long countUsersForCompany(@Param("companyId") Long companyId);

    /**
     * Find relationships by role
     */
    List<UserCompany> findByRoleAndActiveTrue(String role);

    /**
     * Find relationships by role in a specific company
     */
    List<UserCompany> findByCompanyIdAndRoleAndActiveTrue(Long companyId, String role);
}