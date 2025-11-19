package fin.api.controllers;

import fin.service.TransactionClassificationService;

/**
 * Controller for transaction classification endpoints.
 * Handles automatic and manual transaction classification operations.
 */
public class ClassificationController {

    private final TransactionClassificationService transactionClassificationService;

    public ClassificationController(TransactionClassificationService transactionClassificationService) {
        this.transactionClassificationService = transactionClassificationService;
    }

    // Business logic methods will be implemented in Phase 3
}