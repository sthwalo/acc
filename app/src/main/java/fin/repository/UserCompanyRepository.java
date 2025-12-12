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

import fin.entity.UserCompany;
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
     * Find user-company relationships for a specific user
     */
    List<UserCompany> findByUserId(Long userId);

    /**
     * Find user-company relationships for a specific company
     */
    List<UserCompany> findByCompanyId(Long companyId);

    /**
     * Find active user-company relationships for a user
     */
    List<UserCompany> findByUserIdAndIsActive(Long userId, boolean isActive);

    /**
     * Find companies accessible by a user (custom query for service compatibility)
     */
    @Query("SELECT uc FROM UserCompany uc WHERE uc.userId = :userId AND uc.isActive = true")
    List<UserCompany> findCompaniesByUser(@Param("userId") Long userId);

    /**
     * Check if user has access to company
     */
    boolean existsByUserIdAndCompanyIdAndIsActive(Long userId, Long companyId, boolean isActive);

    /**
     * Remove user access to company
     */
    void deleteByUserIdAndCompanyId(Long userId, Long companyId);
}
