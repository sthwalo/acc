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

import fin.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Company entity
 * Replaces manual JDBC CompanyRepository with Spring Data JPA
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * Find companies by name containing (case insensitive)
     */
    List<Company> findByNameContainingIgnoreCase(String name);

    /**
     * Find companies by registration number
     */
    Company findByRegistrationNumber(String registrationNumber);

    /**
     * Find companies by tax number
     */
    Company findByTaxNumber(String taxNumber);

    /**
     * Find companies by VAT registered status
     */
    List<Company> findByVatRegistered(Boolean vatRegistered);

    /**
     * Find active companies (custom query)
     */
    @Query("SELECT c FROM Company c WHERE c.active = true ORDER BY c.name")
    List<Company> findActiveCompanies();

    /**
     * Find companies created after a specific date
     */
    @Query("SELECT c FROM Company c WHERE c.createdAt > :createdAfter ORDER BY c.createdAt DESC")
    List<Company> findCompaniesCreatedAfter(@Param("createdAfter") java.time.LocalDateTime createdAfter);

    /**
     * Count companies by VAT registration status
     */
    long countByVatRegistered(Boolean vatRegistered);

    /**
     * Check if company exists by registration number (excluding specific ID)
     */
    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    /**
     * Check if company exists by tax number (excluding specific ID)
     */
    boolean existsByTaxNumberAndIdNot(String taxNumber, Long id);
}