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

import fin.model.TransactionMappingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for TransactionMappingRule entities
 */
@Repository
public interface TransactionMappingRuleRepository extends JpaRepository<TransactionMappingRule, Long> {

    /**
     * Find active mapping rules by company ID, ordered by priority (descending)
     */
    List<TransactionMappingRule> findByCompanyIdAndIsActiveOrderByPriorityDesc(Long companyId, boolean isActive);

    /**
     * Find mapping rules by company ID
     */
    List<TransactionMappingRule> findByCompanyId(Long companyId);

    /**
     * Find mapping rule by company ID and rule name
     */
    Optional<TransactionMappingRule> findByCompanyIdAndRuleName(Long companyId, String ruleName);

    /**
     * Count active rules by company ID
     */
    long countByCompanyIdAndIsActive(Long companyId, boolean isActive);
}