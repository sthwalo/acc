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

package fin.service;

import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.entity.FiscalPeriodSummary;
import fin.entity.UserCompany;
import fin.repository.CompanyRepository;
import fin.repository.FiscalPeriodRepository;
import fin.repository.UserCompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Spring Service for Company operations
 * Replaces manual JDBC CompanyService with Spring-managed service using JPA
 */
@Service
@Transactional
public class CompanyService {

    private static final Logger LOGGER = Logger.getLogger(CompanyService.class.getName());

    private final CompanyRepository companyRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public CompanyService(CompanyRepository companyRepository,
                               UserCompanyRepository userCompanyRepository,
                               FiscalPeriodRepository fiscalPeriodRepository) {
        LOGGER.info("🔧 DEBUG: CompanyService constructor called - service is being instantiated");
        this.companyRepository = companyRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        LOGGER.info("🔧 DEBUG: CompanyService constructor completed successfully");
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
        return companyRepository.findAll(); // JDBC version returns all companies
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
    public List<Company> getCompaniesForUser(Long userId) throws SQLException {
        // Get user-company relationships for this user
        List<UserCompany> userCompanies = userCompanyRepository.findCompaniesByUser(userId);

        // Extract company IDs that the user has access to
        List<Long> companyIds = userCompanies.stream()
                .map(UserCompany::getCompanyId)
                .toList();

        if (companyIds.isEmpty()) {
            return new ArrayList<>();
        }

        // Find companies by IDs - JDBC version doesn't have findAllById, so we need to fetch individually
        List<Company> companies = new ArrayList<>();
        for (Long companyId : companyIds) {
            Optional<Company> company = companyRepository.findById(companyId);
            company.ifPresent(companies::add);
        }
        return companies;
    }

    /**
     * Create a new company
     */
    public Company createCompany(Company company) {
        // For JDBC version, we can't easily check uniqueness constraints without custom queries
        // This would need to be implemented with custom SQL queries in the repository
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

        // For JDBC version, uniqueness validation would need custom implementation
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
        // JDBC version doesn't have findByNameContainingIgnoreCase
        // For now, return all companies and filter in memory
        return companyRepository.findAll().stream()
                .filter(company -> company.getName() != null &&
                        company.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }

    /**
     * Get companies by VAT registration status
     */
    @Transactional(readOnly = true)
    public List<Company> getCompaniesByVatStatus(Boolean vatRegistered) {
        // JDBC version doesn't have findByVatRegistered
        // This would need custom SQL implementation
        return companyRepository.findAll(); // Return all for now
    }

    /**
     * Count companies by VAT status
     */
    @Transactional(readOnly = true)
    public long countCompaniesByVatStatus(Boolean vatRegistered) {
        // JDBC version doesn't have countByVatRegistered
        // This would need custom SQL implementation
        return companyRepository.findAll().size(); // Return total count for now
    }

    /**
     * Check if company exists by registration number
     */
    @Transactional(readOnly = true)
    public boolean existsByRegistrationNumber(String registrationNumber) {
        // JDBC version doesn't have findByRegistrationNumber
        // This would need custom SQL implementation
        return false; // Placeholder
    }

    /**
     * Get total count of companies
     */
    @Transactional(readOnly = true)
    public long getCompanyCount() {
        return companyRepository.findAll().size();
    }

    // ==================== FISCAL PERIOD METHODS ====================

    /**
     * Get fiscal periods for a company
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriod> getFiscalPeriodsByCompany(Long companyId) {
        return fiscalPeriodRepository.findByCompanyId(companyId);
    }

    /**
     * Get fiscal periods for a company within date range
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriod> getFiscalPeriodsByCompanyAndDateRange(Long companyId, 
                                                                   java.time.LocalDate startDate, 
                                                                   java.time.LocalDate endDate) {
        return fiscalPeriodRepository.findByCompanyIdAndDateRangeBetween(companyId, startDate, endDate);
    }

    /**
     * Get fiscal period by ID
     */
    @Transactional(readOnly = true)
    public FiscalPeriod getFiscalPeriodById(Long id) {
        return fiscalPeriodRepository.findById(id).orElse(null);
    }

    /**
     * Create a new fiscal period
     */
    @Transactional
    public FiscalPeriod createFiscalPeriod(FiscalPeriod fiscalPeriod) {
        return fiscalPeriodRepository.save(fiscalPeriod);
    }

    /**
     * Update an existing fiscal period
     */
    @Transactional
    public FiscalPeriod updateFiscalPeriod(Long id, FiscalPeriod updatedPeriod) {
        FiscalPeriod existingPeriod = getFiscalPeriodById(id);
        if (existingPeriod == null) {
            return null;
        }

        // Update only the fields that are provided, preserving existing values
        if (updatedPeriod.getPeriodName() != null) {
            existingPeriod.setPeriodName(updatedPeriod.getPeriodName());
        }
        if (updatedPeriod.getStartDate() != null) {
            existingPeriod.setStartDate(updatedPeriod.getStartDate());
        }
        if (updatedPeriod.getEndDate() != null) {
            existingPeriod.setEndDate(updatedPeriod.getEndDate());
        }
        // Note: isClosed is a boolean, not a status enum
        existingPeriod.setClosed(updatedPeriod.isClosed());

        // Preserve company_id and other system fields
        existingPeriod.setUpdatedAt(java.time.LocalDateTime.now());

        return fiscalPeriodRepository.save(existingPeriod);
    }

    /**
     * Delete a fiscal period
     */
    @Transactional
    public boolean deleteFiscalPeriod(Long id) {
        if (!fiscalPeriodRepository.existsById(id)) {
            return false;
        }
        fiscalPeriodRepository.deleteById(id);
        return true;
    }

    /**
     * Close a fiscal period
     */
    @Transactional
    public FiscalPeriod closeFiscalPeriod(Long id) {
        FiscalPeriod period = getFiscalPeriodById(id);
        if (period != null) {
            period.setClosed(true);
            return fiscalPeriodRepository.save(period);
        }
        return null;
    }

    /**
     * Get all fiscal periods with company information for frontend display
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriodSummary> getAllFiscalPeriodsWithCompanyInfo() {
        return fiscalPeriodRepository.findAllFiscalPeriodsWithCompanyInfo();
    }
}