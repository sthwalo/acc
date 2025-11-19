package fin.api.controllers;

import fin.service.BankStatementProcessingService;
import fin.repository.BankTransactionRepository;
import fin.model.BankTransaction;
import fin.api.dto.responses.ApiResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.math.BigDecimal;

/**
 * Controller for transaction management endpoints.
 * Handles transaction retrieval and processing operations.
 */
public class TransactionController {

    private final BankStatementProcessingService bankStatementProcessingService;
    private final BankTransactionRepository transactionRepository;

    public TransactionController(BankStatementProcessingService bankStatementProcessingService, String dbUrl) {
        this.bankStatementProcessingService = bankStatementProcessingService;
        this.transactionRepository = new BankTransactionRepository(dbUrl);
    }

    /**
     * Get all transactions for a company and fiscal period
     */
    public ApiResponse<Map<String, Object>> getTransactions(Long companyId, Long fiscalPeriodId, int page, int size) {
        try {
            List<BankTransaction> transactions = transactionRepository.findByCompanyAndFiscalPeriod(companyId, fiscalPeriodId);

            // Apply pagination
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, transactions.size());
            List<BankTransaction> paginatedTransactions = transactions.subList(startIndex, endIndex);

            List<Map<String, Object>> transactionData = paginatedTransactions.stream()
                .map(this::convertToTransactionMap)
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("data", transactionData);
            result.put("count", transactions.size());
            result.put("company_id", companyId);
            result.put("timestamp", System.currentTimeMillis());
            result.put("note", "Transactions retrieved successfully");

            return new ApiResponse<>(true, result, "Transactions retrieved successfully");
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to retrieve transactions: " + e.getMessage());
        }
    }

    /**
     * Get transaction by ID
     */
    public ApiResponse<Map<String, Object>> getTransaction(Long companyId, Long fiscalPeriodId, Long id) {
        try {
            BankTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // Verify the transaction belongs to the correct company and fiscal period
            if (!transaction.getCompanyId().equals(companyId) ||
                !transaction.getFiscalPeriodId().equals(fiscalPeriodId)) {
                return new ApiResponse<>(false, null, "Transaction not found for this company and fiscal period");
            }

            Map<String, Object> transactionData = convertToTransactionMap(transaction);

            return new ApiResponse<>(true, transactionData, "Transaction retrieved successfully");
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to retrieve transaction: " + e.getMessage());
        }
    }

    /**
     * Search transactions by details
     */
    public ApiResponse<Map<String, Object>> searchTransactions(Long companyId, Long fiscalPeriodId, String query) {
        try {
            List<BankTransaction> allTransactions = transactionRepository.findByCompanyAndFiscalPeriod(companyId, fiscalPeriodId);

            // Filter by search query
            List<BankTransaction> filteredTransactions = allTransactions.stream()
                .filter(t -> t.getDetails() != null &&
                           t.getDetails().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());

            List<Map<String, Object>> transactionData = filteredTransactions.stream()
                .map(this::convertToTransactionMap)
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("data", transactionData);
            result.put("count", transactionData.size());
            result.put("company_id", companyId);
            result.put("timestamp", System.currentTimeMillis());
            result.put("note", "Search completed successfully");

            return new ApiResponse<>(true, result, "Transactions searched successfully");
        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to search transactions: " + e.getMessage());
        }
    }

    /**
     * Convert BankTransaction to the expected Transaction format
     */
    private Map<String, Object> convertToTransactionMap(BankTransaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transaction.getId());
        map.put("company_id", transaction.getCompanyId());
        map.put("fiscal_period_id", transaction.getFiscalPeriodId());
        map.put("date", transaction.getTransactionDate().toString());
        map.put("description", transaction.getDetails() != null ? transaction.getDetails() : "");
        map.put("reference", transaction.getAccountNumber() != null ? transaction.getAccountNumber() : "");

        // Determine type and amount based on debit/credit
        if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
            map.put("type", "debit");
            map.put("amount", transaction.getDebitAmount().doubleValue());
        } else if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
            map.put("type", "credit");
            map.put("amount", transaction.getCreditAmount().doubleValue());
        } else {
            map.put("type", "unknown");
            map.put("amount", 0.0);
        }

        map.put("category", "Unclassified"); // TODO: Add classification logic
        map.put("created_at", null); // TODO: Add created_at field to model

        return map;
    }
}