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

import fin.entity.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Budget entities
 */
@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /**
     * Find budgets by company ID
     */
    List<Budget> findByCompanyId(Long companyId);

    /**
     * Find budgets by company ID and status
     */
    List<Budget> findByCompanyIdAndStatus(Long companyId, String status);

    /**
     * Find budget by company ID and budget year
     */
    Optional<Budget> findByCompanyIdAndBudgetYear(Long companyId, Integer budgetYear);

    /**
     * Find budgets by company ID ordered by budget year descending
     */
    List<Budget> findByCompanyIdOrderByBudgetYearDesc(Long companyId);

    /**
     * Find active budgets by company ID
     */
    List<Budget> findByCompanyIdAndStatusIn(Long companyId, List<String> statuses);

    /**
     * Find budgets by fiscal period ID
     */
    List<Budget> findByFiscalPeriodId(Long fiscalPeriodId);

    /**
     * Find approved budgets by company ID
     */
    @Query("SELECT b FROM Budget b WHERE b.companyId = :companyId AND b.status IN ('APPROVED', 'ACTIVE')")
    List<Budget> findApprovedBudgetsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find draft budgets by company ID
     */
    List<Budget> findByCompanyIdAndStatusOrderByCreatedAtDesc(Long companyId, String status);

    /**
     * Count budgets by company ID and status
     */
    long countByCompanyIdAndStatus(Long companyId, String status);

    /**
     * Find budgets by title containing text (case-insensitive)
     */
    List<Budget> findByCompanyIdAndTitleContainingIgnoreCase(Long companyId, String title);

    /**
     * Find budgets within a year range
     */
    @Query("SELECT b FROM Budget b WHERE b.companyId = :companyId AND b.budgetYear BETWEEN :startYear AND :endYear ORDER BY b.budgetYear")
    List<Budget> findByCompanyIdAndBudgetYearBetween(@Param("companyId") Long companyId,
                                                   @Param("startYear") Integer startYear,
                                                   @Param("endYear") Integer endYear);
}