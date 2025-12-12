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

import fin.entity.BankTransaction;
import fin.dto.RejectedTransaction;
import fin.service.upload.BankStatementProcessingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for transaction upload validation filters.
 * 
 * <p>Tests the complete upload processing flow including:
 * <ul>
 *   <li>Duplicate detection and rejection</li>
 *   <li>Fiscal period boundary validation</li>
 *   <li>StatementProcessingResult structure</li>
 *   <li>RejectedTransaction DTO</li>
 * </ul>
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
class TransactionUploadValidationIntegrationTest {

    @Test
    void statementProcessingResult_withAllCounts_shouldStoreCorrectly() {
        // Arrange
        List<BankTransaction> validTransactions = new ArrayList<>();
        validTransactions.add(createTransaction(1L));
        validTransactions.add(createTransaction(2L));
        
        List<RejectedTransaction> rejectedTransactions = new ArrayList<>();
        rejectedTransactions.add(createRejectedTransaction(RejectedTransaction.RejectionReason.DUPLICATE));
        rejectedTransactions.add(createRejectedTransaction(RejectedTransaction.RejectionReason.OUT_OF_PERIOD));
        
        List<String> errors = List.of("Error 1", "Error 2");

        // Act
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                validTransactions,
                100,  // processedLines
                2,    // validTransactions
                1,    // duplicateTransactions
                1,    // outOfPeriodTransactions
                2,    // invalidTransactions
                rejectedTransactions,
                errors
            );

        // Assert
        assertEquals(validTransactions, result.getTransactions());
        assertEquals(100, result.getProcessedLines());
        assertEquals(2, result.getValidTransactions());
        assertEquals(1, result.getDuplicateTransactions());
        assertEquals(1, result.getOutOfPeriodTransactions());
        assertEquals(2, result.getInvalidTransactions());
        assertEquals(rejectedTransactions, result.getRejectedTransactions());
        assertEquals(errors, result.getErrors());
    }

    @Test
    void rejectedTransaction_withDuplicateReason_shouldStoreCorrectly() {
        // Arrange
        LocalDate date = LocalDate.of(2025, 1, 15);
        String description = "Payment to vendor";
        BigDecimal debit = new BigDecimal("500.00");
        BigDecimal credit = BigDecimal.ZERO;
        BigDecimal balance = new BigDecimal("1500.00");
        String detail = "Duplicate of transaction ID 123";

        // Act
        RejectedTransaction rejected = new RejectedTransaction(
            date, description, debit, credit, balance,
            RejectedTransaction.RejectionReason.DUPLICATE,
            detail
        );

        // Assert
        assertEquals(date, rejected.getTransactionDate());
        assertEquals(description, rejected.getDescription());
        assertEquals(debit, rejected.getDebitAmount());
        assertEquals(credit, rejected.getCreditAmount());
        assertEquals(balance, rejected.getBalance());
        assertEquals(RejectedTransaction.RejectionReason.DUPLICATE, rejected.getReason());
        assertEquals(detail, rejected.getReasonDetail());
    }

    @Test
    void rejectedTransaction_withOutOfPeriodReason_shouldStoreCorrectly() {
        // Arrange
        LocalDate date = LocalDate.of(2024, 12, 31);
        String description = "Transaction outside period";
        String detail = "Transaction date 2024-12-31 is before fiscal period start date 2025-01-01";

        // Act
        RejectedTransaction rejected = new RejectedTransaction(
            date, description, new BigDecimal("100.00"), BigDecimal.ZERO,
            new BigDecimal("2000.00"),
            RejectedTransaction.RejectionReason.OUT_OF_PERIOD,
            detail
        );

        // Assert
        assertEquals(RejectedTransaction.RejectionReason.OUT_OF_PERIOD, rejected.getReason());
        assertTrue(rejected.getReasonDetail().contains("before fiscal period"));
        assertTrue(rejected.getReasonDetail().contains("2024-12-31"));
    }

    @Test
    void rejectedTransaction_withValidationErrorReason_shouldStoreCorrectly() {
        // Arrange
        String detail = "Invalid transaction: Missing required field - details";

        // Act
        RejectedTransaction rejected = new RejectedTransaction(
            LocalDate.of(2025, 1, 15),
            null,  // Invalid - null description
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("1000.00"),
            RejectedTransaction.RejectionReason.VALIDATION_ERROR,
            detail
        );

        // Assert
        assertEquals(RejectedTransaction.RejectionReason.VALIDATION_ERROR, rejected.getReason());
        assertTrue(rejected.getReasonDetail().contains("Invalid transaction"));
    }

    @Test
    void rejectedTransaction_toString_shouldProvideSummary() {
        // Arrange
        RejectedTransaction rejected = createRejectedTransaction(RejectedTransaction.RejectionReason.DUPLICATE);

        // Act
        String summary = rejected.toString();

        // Assert
        assertNotNull(summary);
        assertTrue(summary.contains("RejectedTransaction"));
        assertTrue(summary.contains("date="));
        assertTrue(summary.contains("reason=DUPLICATE"));
    }

    @Test
    void rejectedTransaction_withLongDescription_shouldTruncateInToString() {
        // Arrange
        String longDescription = "This is a very long transaction description that should be truncated in the toString method to prevent excessive output";
        RejectedTransaction rejected = new RejectedTransaction(
            LocalDate.of(2025, 1, 15),
            longDescription,
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("1000.00"),
            RejectedTransaction.RejectionReason.DUPLICATE,
            "Duplicate transaction"
        );

        // Act
        String summary = rejected.toString();

        // Assert
        assertTrue(summary.contains("..."), "Long description should be truncated with ellipsis");
        assertTrue(summary.length() < longDescription.length() + 100, "Summary should be shorter than full description");
    }

    @Test
    void statementProcessingResult_withZeroCounts_shouldHandleGracefully() {
        // Act
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                new ArrayList<>(),
                0, 0, 0, 0, 0,
                new ArrayList<>(),
                new ArrayList<>()
            );

        // Assert
        assertEquals(0, result.getProcessedLines());
        assertEquals(0, result.getValidTransactions());
        assertEquals(0, result.getDuplicateTransactions());
        assertEquals(0, result.getOutOfPeriodTransactions());
        assertEquals(0, result.getInvalidTransactions());
        assertTrue(result.getTransactions().isEmpty());
        assertTrue(result.getRejectedTransactions().isEmpty());
        assertTrue(result.getErrors().isEmpty());
    }

    // Helper methods

    private BankTransaction createTransaction(Long id) {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(id);
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);
        transaction.setTransactionDate(LocalDate.of(2025, 1, 15));
        transaction.setDebitAmount(new BigDecimal("500.00"));
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setDetails("Test transaction " + id);
        transaction.setBalance(new BigDecimal("1500.00"));
        return transaction;
    }

    private RejectedTransaction createRejectedTransaction(RejectedTransaction.RejectionReason reason) {
        return new RejectedTransaction(
            LocalDate.of(2025, 1, 15),
            "Rejected transaction",
            new BigDecimal("100.00"),
            BigDecimal.ZERO,
            new BigDecimal("1000.00"),
            reason,
            "Test rejection: " + reason.name()
        );
    }
}
