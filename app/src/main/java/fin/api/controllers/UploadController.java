package fin.api.controllers;

import fin.service.BankStatementProcessingService;

/**
 * Controller for file upload endpoints.
 * Handles bank statement and document upload processing.
 */
public class UploadController {

    private final BankStatementProcessingService bankStatementProcessingService;

    public UploadController(BankStatementProcessingService bankStatementProcessingService) {
        this.bankStatementProcessingService = bankStatementProcessingService;
    }

    // Business logic methods will be implemented in Phase 3
}