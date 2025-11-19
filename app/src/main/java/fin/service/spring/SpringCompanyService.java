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

package fin.service.spring;

import fin.entity.Company;
import fin.repository.jpa.CompanyRepository;
import fin.repository.jpa.UserCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring Service for Company operations
 * Replaces manual JDBC CompanyService with Spring-managed service using JPA
 */
@Service
@Transactional
public class SpringCompanyService {

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;

    @Autowired
    public SpringCompanyService(CompanyRepository companyRepository,
                               UserCompanyRepository userCompanyRepository) {
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
    }

    /**
     * Get all companies
     */
    @Transactional(readOnly = true)
    public List<Company> getAllCompanies() {
        return companyRepository.findAll();
    }

    /**
     * Get active companies only
     */
    @Transactional(readOnly = true)
    public List<Company> getActiveCompanies() {
        return companyRepository.findActiveCompanies();
    }

    /**
     * Get company by ID
     */
    @Transactional(readOnly = true)
    public Company getCompanyById(Long id) {
        Optional<Company> company = companyRepository.findById(id);
        return company.orElse(null);
    }

    /**
     * Get companies for a specific user
     */
    @Transactional(readOnly = true)
    public List<Company> getCompaniesForUser(Long userId) {
        // Get user-company relationships for this user
        List<fin.model.UserCompany> userCompanies = userCompanyRepository.findCompaniesByUser(userId);

        // Extract company IDs that the user has access to
        List<Long> companyIds = userCompanies.stream()
                .map(fin.model.UserCompany::getCompanyId)
                .toList();

        if (companyIds.isEmpty()) {
            return List.of();
        }

        // Find companies by IDs
        return companyRepository.findAllById(companyIds);
    }

    /**
     * Create a new company
     */
    public Company createCompany(Company company) {
        // Validate uniqueness constraints
        if (company.getRegistrationNumber() != null &&
            companyRepository.existsByRegistrationNumberAndIdNot(company.getRegistrationNumber(), null)) {
            throw new IllegalArgumentException("Registration number already exists: " + company.getRegistrationNumber());
        }

        if (company.getTaxNumber() != null &&
            companyRepository.existsByTaxNumberAndIdNot(company.getTaxNumber(), null)) {
            throw new IllegalArgumentException("Tax number already exists: " + company.getTaxNumber());
        }

        return companyRepository.save(company);
    }

    /**
     * Update an existing company
     */
    public Company updateCompany(Company company) {
        if (company.getId() == null) {
            throw new IllegalArgumentException("Company ID cannot be null for update operation");
        }

        // Check if company exists
        if (!companyRepository.existsById(company.getId())) {
            throw new IllegalArgumentException("Company not found with ID: " + company.getId());
        }

        // Validate uniqueness constraints (excluding current company)
        if (company.getRegistrationNumber() != null &&
            companyRepository.existsByRegistrationNumberAndIdNot(company.getRegistrationNumber(), company.getId())) {
            throw new IllegalArgumentException("Registration number already exists: " + company.getRegistrationNumber());
        }

        if (company.getTaxNumber() != null &&
            companyRepository.existsByTaxNumberAndIdNot(company.getTaxNumber(), company.getId())) {
            throw new IllegalArgumentException("Tax number already exists: " + company.getTaxNumber());
        }

        return companyRepository.save(company);
    }

    /**
     * Delete a company
     */
    public boolean deleteCompany(Long id) {
        if (!companyRepository.existsById(id)) {
            return false;
        }

        companyRepository.deleteById(id);
        return true;
    }

    /**
     * Search companies by name
     */
    @Transactional(readOnly = true)
    public List<Company> searchCompaniesByName(String name) {
        return companyRepository.findByNameContainingIgnoreCase(name);
    }

    /**
     * Get companies by VAT registration status
     */
    @Transactional(readOnly = true)
    public List<Company> getCompaniesByVatStatus(Boolean vatRegistered) {
        return companyRepository.findByVatRegistered(vatRegistered);
    }

    /**
     * Count companies by VAT status
     */
    @Transactional(readOnly = true)
    public long countCompaniesByVatStatus(Boolean vatRegistered) {
        return companyRepository.countByVatRegistered(vatRegistered);
    }

    /**
     * Check if company exists by registration number
     */
    @Transactional(readOnly = true)
    public boolean existsByRegistrationNumber(String registrationNumber) {
        return companyRepository.findByRegistrationNumber(registrationNumber) != null;
    }

    /**
     * Check if company exists by tax number
     */
    @Transactional(readOnly = true)
    public boolean existsByTaxNumber(String taxNumber) {
        return companyRepository.findByTaxNumber(taxNumber) != null;
    }
}