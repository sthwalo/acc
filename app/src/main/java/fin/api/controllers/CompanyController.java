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

package fin.api.controllers;

import fin.api.dto.requests.CompanyRequest;
import fin.api.dto.responses.ApiResponse;
import fin.api.dto.responses.CompanyResponse;
import fin.model.Company;
import fin.service.CompanyService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Company Controller
 * Handles company management operations
 */
public class CompanyController {
    private final CompanyService companyService;

    /**
     * Constructor with dependency injection
     * @param companyService the company service
     */
    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * Get all companies
     * @return API response with list of companies
     */
    public ApiResponse<List<CompanyResponse>> getAllCompanies() {
        try {
            List<Company> companies = companyService.getAllCompanies();
            List<CompanyResponse> companyResponses = companies.stream()
                    .map(CompanyResponse::new)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, companyResponses, "Companies retrieved successfully");

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to retrieve companies: " + e.getMessage());
        }
    }

    /**
     * Get companies for a specific user
     * @param userId the user ID
     * @return API response with list of companies
     */
    public ApiResponse<List<CompanyResponse>> getCompaniesForUser(Long userId) {
        try {
            List<Company> companies = companyService.getCompaniesForUser(userId);
            List<CompanyResponse> companyResponses = companies.stream()
                    .map(CompanyResponse::new)
                    .collect(Collectors.toList());

            return new ApiResponse<>(true, companyResponses, "User companies retrieved successfully");

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to retrieve user companies: " + e.getMessage());
        }
    }

    /**
     * Get company by ID
     * @param id the company ID
     * @return API response with company data
     */
    public ApiResponse<CompanyResponse> getCompanyById(Long id) {
        try {
            Company company = companyService.getCompanyById(id);
            if (company == null) {
                return new ApiResponse<>(false, null, "Company not found with ID: " + id);
            }

            CompanyResponse companyResponse = new CompanyResponse(company);
            return new ApiResponse<>(true, companyResponse, "Company retrieved successfully");

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to retrieve company: " + e.getMessage());
        }
    }

    /**
     * Create a new company
     * @param request the company creation request
     * @return API response with created company
     */
    public ApiResponse<CompanyResponse> createCompany(CompanyRequest request) {
        try {
            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return new ApiResponse<>(false, null, "Company name is required");
            }

            // Create company model from request
            Company company = new Company();
            company.setName(request.getName());
            company.setRegistrationNumber(request.getRegistrationNumber());
            company.setTaxNumber(request.getTaxNumber());
            company.setAddress(request.getAddress());
            company.setContactEmail(request.getContactEmail());
            company.setContactPhone(request.getContactPhone());
            company.setLogoPath(request.getLogoPath());
            company.setBankName(request.getBankName());
            company.setAccountNumber(request.getAccountNumber());
            company.setAccountType(request.getAccountType());
            company.setBranchCode(request.getBranchCode());
            company.setVatRegistered(request.isVatRegistered());

            // Save company
            Company createdCompany = companyService.createCompany(company);
            CompanyResponse companyResponse = new CompanyResponse(createdCompany);

            return new ApiResponse<>(true, companyResponse, "Company created successfully");

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to create company: " + e.getMessage());
        }
    }

    /**
     * Update an existing company
     * @param id the company ID
     * @param request the company update request
     * @return API response with updated company
     */
    public ApiResponse<CompanyResponse> updateCompany(Long id, CompanyRequest request) {
        try {
            // Check if company exists
            Company existingCompany = companyService.getCompanyById(id);
            if (existingCompany == null) {
                return new ApiResponse<>(false, null, "Company not found with ID: " + id);
            }

            // Validate request
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                return new ApiResponse<>(false, null, "Company name is required");
            }

            // Update company model
            existingCompany.setName(request.getName());
            existingCompany.setRegistrationNumber(request.getRegistrationNumber());
            existingCompany.setTaxNumber(request.getTaxNumber());
            existingCompany.setAddress(request.getAddress());
            existingCompany.setContactEmail(request.getContactEmail());
            existingCompany.setContactPhone(request.getContactPhone());
            existingCompany.setLogoPath(request.getLogoPath());
            existingCompany.setBankName(request.getBankName());
            existingCompany.setAccountNumber(request.getAccountNumber());
            existingCompany.setAccountType(request.getAccountType());
            existingCompany.setBranchCode(request.getBranchCode());
            existingCompany.setVatRegistered(request.isVatRegistered());

            // Update company
            Company updatedCompany = companyService.updateCompany(existingCompany);
            CompanyResponse companyResponse = new CompanyResponse(updatedCompany);

            return new ApiResponse<>(true, companyResponse, "Company updated successfully");

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to update company: " + e.getMessage());
        }
    }

    /**
     * Delete a company
     * @param id the company ID
     * @return API response indicating success/failure
     */
    public ApiResponse<Void> deleteCompany(Long id) {
        try {
            // Check if company exists
            Company existingCompany = companyService.getCompanyById(id);
            if (existingCompany == null) {
                return new ApiResponse<>(false, null, "Company not found with ID: " + id);
            }

            // Delete company
            boolean deleted = companyService.deleteCompany(id);
            if (deleted) {
                return new ApiResponse<>(true, null, "Company deleted successfully");
            } else {
                return new ApiResponse<>(false, null, "Failed to delete company");
            }

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to delete company: " + e.getMessage());
        }
    }
}