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

package fin.controller.spring;

import fin.controller.spring.ApiResponse;
import fin.controller.spring.BusinessException;
import fin.controller.spring.ErrorCode;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.model.FiscalPeriodSummary;
import fin.model.User;
import fin.service.spring.BankStatementProcessingService;
import fin.service.spring.SpringCompanyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring REST Controller for company management operations.
 */
@RestController
@RequestMapping("/api/v1/companies")
public class SpringCompanyController {

    private final SpringCompanyService companyService;
    private final BankStatementProcessingService bankStatementService;

    public SpringCompanyController(SpringCompanyService companyService,
                                 BankStatementProcessingService bankStatementService) {
        this.companyService = companyService;
        this.bankStatementService = bankStatementService;
    }

    /**
     * Get all companies
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Company>>> getAllCompanies() {
        try {
            List<Company> companies = companyService.getAllCompanies();
            return ResponseEntity.ok(ApiResponse.success("Companies retrieved successfully", companies, companies.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve companies: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Get active companies
     */
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveCompanies() {
        try {
            List<Company> companies = companyService.getActiveCompanies();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", companies);
            response.put("count", companies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve active companies: " + e.getMessage()
            ));
        }
    }

    /**
     * Get companies for authenticated user
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getCompaniesForUser(@RequestAttribute("user") User user) {
        try {
            List<Company> companies = companyService.getCompaniesForUser(user.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", companies);
            response.put("count", companies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve user companies: " + e.getMessage()
            ));
        }
    }

    /**
     * Get company by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCompanyById(@PathVariable Long id) {
        try {
            Company company = companyService.getCompanyById(id);
            if (company == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", company);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve company: " + e.getMessage()
            ));
        }
    }

    /**
     * Search companies by name
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCompanies(@RequestParam String name) {
        try {
            List<Company> companies = companyService.searchCompaniesByName(name);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", companies);
            response.put("count", companies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to search companies: " + e.getMessage()
            ));
        }
    }

    /**
     * Get companies by VAT status
     */
    @GetMapping("/vat-status")
    public ResponseEntity<Map<String, Object>> getCompaniesByVatStatus(@RequestParam Boolean vatRegistered) {
        try {
            List<Company> companies = companyService.getCompaniesByVatStatus(vatRegistered);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", companies);
            response.put("count", companies.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve companies by VAT status: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new company
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createCompany(@RequestBody Company company) {
        try {
            Company createdCompany = companyService.createCompany(company);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdCompany);
            response.put("message", "Company created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to create company: " + e.getMessage()
            ));
        }
    }

    /**
     * Update an existing company
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateCompany(@PathVariable Long id, @RequestBody Company company) {
        try {
            company.setId(id);
            Company updatedCompany = companyService.updateCompany(company);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedCompany);
            response.put("message", "Company updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update company: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a company
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCompany(@PathVariable Long id) {
        try {
            boolean deleted = companyService.deleteCompany(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Company deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to delete company: " + e.getMessage()
            ));
        }
    }

    /**
     * Get fiscal periods for a company
     */
    @GetMapping("/{companyId}/fiscal-periods")
    public ResponseEntity<Map<String, Object>> getFiscalPeriods(@PathVariable Long companyId) {
        try {
            List<FiscalPeriod> fiscalPeriods = companyService.getFiscalPeriodsByCompany(companyId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", fiscalPeriods);
            response.put("count", fiscalPeriods.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve fiscal periods: " + e.getMessage()
            ));
        }
    }

    /**
     * Get fiscal periods for a company within date range
     */
    @GetMapping("/{companyId}/fiscal-periods/range")
    public ResponseEntity<Map<String, Object>> getFiscalPeriodsByDateRange(
            @PathVariable Long companyId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        try {
            List<FiscalPeriod> fiscalPeriods = companyService.getFiscalPeriodsByCompanyAndDateRange(companyId, startDate, endDate);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", fiscalPeriods);
            response.put("count", fiscalPeriods.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve fiscal periods by date range: " + e.getMessage()
            ));
        }
    }

    /**
     * Get fiscal period by ID
     */
    @GetMapping("/fiscal-periods/{id}")
    public ResponseEntity<Map<String, Object>> getFiscalPeriodById(@PathVariable Long id) {
        try {
            FiscalPeriod fiscalPeriod = companyService.getFiscalPeriodById(id);
            if (fiscalPeriod == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", fiscalPeriod);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve fiscal period: " + e.getMessage()
            ));
        }
    }

    /**
     * Create a new fiscal period
     */
    @PostMapping("/{companyId}/fiscal-periods")
    public ResponseEntity<Map<String, Object>> createFiscalPeriod(@PathVariable Long companyId, @RequestBody FiscalPeriod fiscalPeriod) {
        try {
            fiscalPeriod.setCompanyId(companyId);
            FiscalPeriod createdPeriod = companyService.createFiscalPeriod(fiscalPeriod);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", createdPeriod);
            response.put("message", "Fiscal period created successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to create fiscal period: " + e.getMessage()
            ));
        }
    }

    /**
     * Update a fiscal period
     */
    @PutMapping("/fiscal-periods/{id}")
    public ResponseEntity<Map<String, Object>> updateFiscalPeriod(@PathVariable Long id, @RequestBody FiscalPeriod fiscalPeriod) {
        try {
            FiscalPeriod updatedPeriod = companyService.updateFiscalPeriod(id, fiscalPeriod);
            if (updatedPeriod == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updatedPeriod);
            response.put("message", "Fiscal period updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to update fiscal period: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete a fiscal period
     */
    @DeleteMapping("/fiscal-periods/{id}")
    public ResponseEntity<Map<String, Object>> deleteFiscalPeriod(@PathVariable Long id) {
        try {
            boolean deleted = companyService.deleteFiscalPeriod(id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Fiscal period deleted successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to delete fiscal period: " + e.getMessage()
            ));
        }
    }

    /**
     * Close a fiscal period
     */
    @PostMapping("/fiscal-periods/{id}/close")
    public ResponseEntity<Map<String, Object>> closeFiscalPeriod(@PathVariable Long id) {
        try {
            FiscalPeriod closedPeriod = companyService.closeFiscalPeriod(id);
            if (closedPeriod == null) {
                return ResponseEntity.notFound().build();
            }
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", closedPeriod);
            response.put("message", "Fiscal period closed successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to close fiscal period: " + e.getMessage()
            ));
        }
    }

    /**
     * Get company statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCompanyStats() {
        try {
            long totalCompanies = companyService.getCompanyCount();
            long vatRegistered = companyService.countCompaniesByVatStatus(true);
            long nonVatRegistered = companyService.countCompaniesByVatStatus(false);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCompanies", totalCompanies);
            stats.put("vatRegistered", vatRegistered);
            stats.put("nonVatRegistered", nonVatRegistered);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve company statistics: " + e.getMessage()
            ));
        }
    }

    /**
     * Get all fiscal periods across all companies with company information
     */
    @GetMapping("/fiscal-periods/all")
    public ResponseEntity<Map<String, Object>> getAllFiscalPeriodsWithCompanyInfo() {
        try {
            List<FiscalPeriodSummary> fiscalPeriods = companyService.getAllFiscalPeriodsWithCompanyInfo();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", fiscalPeriods);
            response.put("count", fiscalPeriods.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Failed to retrieve fiscal periods: " + e.getMessage()
            ));
        }
    }

    /**
     * Get transactions for a company and fiscal period
     */
    @GetMapping("/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions")
    public ResponseEntity<ApiResponse<List<BankTransaction>>> getTransactions(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period - handle null fiscal_period_id values safely
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();

            if (transactions.isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("companyId", companyId);
                metadata.put("fiscalPeriodId", fiscalPeriodId);
                metadata.put("suggestion", "No transactions found for the selected fiscal period. Please check if transactions have been uploaded and classified for this period.");

                return ResponseEntity.ok(ApiResponse.empty(
                    "No transactions found for fiscal period " + fiscalPeriodId,
                    metadata
                ));
            }

            return ResponseEntity.ok(ApiResponse.success(
                "Transactions retrieved successfully",
                transactions,
                transactions.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve transactions: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }
}
