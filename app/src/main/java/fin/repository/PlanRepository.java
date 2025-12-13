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

import fin.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Plan entities
 */
@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {

    /**
     * Find all active plans ordered by price
     */
    List<Plan> findByIsActiveTrueOrderByPriceAsc();

    /**
     * Find plan by name (case-sensitive)
     */
    Optional<Plan> findByNameAndIsActiveTrue(String name);

    /**
     * Find trial plan (price = 0)
     */
    @Query("SELECT p FROM Plan p WHERE p.price = 0 AND p.isActive = true")
    Optional<Plan> findTrialPlan();

    /**
     * Find plans by billing cycle
     */
    List<Plan> findByBillingCycleAndIsActiveTrue(String billingCycle);

    /**
     * Find plans with API access
     */
    List<Plan> findByHasApiAccessTrueAndIsActiveTrue();

    /**
     * Find plans that allow multiple companies
     */
    List<Plan> findByCanAccessMultipleCompaniesTrueAndIsActiveTrue();

    /**
     * Find plans within price range
     */
    @Query("SELECT p FROM Plan p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.isActive = true ORDER BY p.price ASC")
    List<Plan> findByPriceRange(@Param("minPrice") java.math.BigDecimal minPrice,
                               @Param("maxPrice") java.math.BigDecimal maxPrice);

    /**
     * Count active plans
     */
    long countByIsActiveTrue();
}