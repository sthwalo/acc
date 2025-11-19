package fin.api.controllers;

import fin.service.CompanyService;

/**
 * Controller for fiscal period management endpoints.
 * Handles CRUD operations for fiscal periods within companies.
 */
public class FiscalPeriodController {

    private final CompanyService companyService;

    public FiscalPeriodController(CompanyService companyService) {
        this.companyService = companyService;
    }

    // Business logic methods will be implemented in Phase 3
}