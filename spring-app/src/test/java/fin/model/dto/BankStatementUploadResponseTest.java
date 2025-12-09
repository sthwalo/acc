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

package fin.model.dto;

import fin.dto.BankStatementUploadResponse;
import fin.dto.RejectedTransaction;
import fin.entity.BankTransaction;
import fin.service.upload.BankStatementProcessingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BankStatementUploadResponse DTO.
 * 
 * <p>Tests the API response structure including success status,
 * summary messages, and processing counts.
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
class BankStatementUploadResponseTest {

    @Test
    void constructor_withAllValidTransactions_shouldIndicateSuccess() {
        // Arrange
        List<BankTransaction> validTransactions = createTransactionList(5);
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                validTransactions,
                100,  // processedLines
                5,    // validTransactions
                0,    // duplicateTransactions
                0,    // outOfPeriodTransactions
                0,    // invalidTransactions
                new ArrayList<>(),
                new ArrayList<>()
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertTrue(response.isSuccess(), "Should indicate success when all transactions valid");
        assertTrue(response.getMessage().contains("All 5 transactions saved"),
            "Message should indicate all transactions saved");
        assertEquals(5, response.getSummary().getValidTransactions());
        assertEquals(0, response.getSummary().getDuplicateTransactions());
        assertEquals(0, response.getSummary().getOutOfPeriodTransactions());
        assertEquals(0, response.getSummary().getValidationErrors());
        assertEquals(0, response.getSummary().getTotalRejected());
    }

    @Test
    void constructor_withSomeRejections_shouldIndicatePartialSuccess() {
        // Arrange
        List<BankTransaction> validTransactions = createTransactionList(3);
        List<RejectedTransaction> rejectedTransactions = createRejectedList(2, 1, 1);
        
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                validTransactions,
                100,  // processedLines
                3,    // validTransactions
                2,    // duplicateTransactions
                1,    // outOfPeriodTransactions
                1,    // invalidTransactions
                rejectedTransactions,
                List.of("Validation error 1")
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertTrue(response.isSuccess(), "Should indicate success when some transactions saved");
        assertTrue(response.getMessage().contains("3 of 7 transactions saved"),
            "Message should show partial success");
        assertTrue(response.getMessage().contains("2 duplicates"),
            "Message should mention duplicates");
        assertTrue(response.getMessage().contains("1 out-of-period"),
            "Message should mention out-of-period");
        assertTrue(response.getMessage().contains("1 validation errors"),
            "Message should mention validation errors");
        
        assertEquals(3, response.getSummary().getValidTransactions());
        assertEquals(2, response.getSummary().getDuplicateTransactions());
        assertEquals(1, response.getSummary().getOutOfPeriodTransactions());
        assertEquals(1, response.getSummary().getValidationErrors());
        assertEquals(4, response.getSummary().getTotalRejected());
    }

    @Test
    void constructor_withNoValidTransactions_shouldIndicateFailure() {
        // Arrange
        List<RejectedTransaction> rejectedTransactions = createRejectedList(2, 1, 1);
        
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                new ArrayList<>(),
                100,  // processedLines
                0,    // validTransactions
                2,    // duplicateTransactions
                1,    // outOfPeriodTransactions
                1,    // invalidTransactions
                rejectedTransactions,
                List.of("Error 1", "Error 2")
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertFalse(response.isSuccess(), "Should indicate failure when no valid transactions");
        assertTrue(response.getMessage().contains("Upload failed"),
            "Message should indicate upload failed");
        assertTrue(response.getMessage().contains("No valid transactions found"),
            "Message should explain no valid transactions");
        
        assertEquals(0, response.getSummary().getValidTransactions());
        assertEquals(4, response.getSummary().getTotalRejected());
    }

    @Test
    void getSavedTransactions_shouldReturnValidTransactionsList() {
        // Arrange
        List<BankTransaction> validTransactions = createTransactionList(3);
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                validTransactions,
                50, 3, 0, 0, 0,
                new ArrayList<>(),
                new ArrayList<>()
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertEquals(validTransactions, response.getSavedTransactions());
        assertEquals(3, response.getSavedTransactions().size());
    }

    @Test
    void getRejectedTransactions_shouldReturnRejectionsList() {
        // Arrange
        List<RejectedTransaction> rejectedTransactions = createRejectedList(2, 1, 0);
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                createTransactionList(1),
                50, 1, 2, 1, 0,
                rejectedTransactions,
                new ArrayList<>()
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertEquals(rejectedTransactions, response.getRejectedTransactions());
        assertEquals(3, response.getRejectedTransactions().size());
    }

    @Test
    void getErrors_shouldReturnErrorsList() {
        // Arrange
        List<String> errors = List.of("Error 1", "Error 2", "Error 3");
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                new ArrayList<>(),
                50, 0, 0, 0, 3,
                new ArrayList<>(),
                errors
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertEquals(errors, response.getErrors());
        assertEquals(3, response.getErrors().size());
    }

    @Test
    void processingSummary_getTotalRejected_shouldSumAllRejections() {
        // Arrange
        BankStatementUploadResponse.ProcessingSummary summary = 
            new BankStatementUploadResponse.ProcessingSummary(
                100,  // totalLinesProcessed
                5,    // validTransactions
                3,    // duplicateTransactions
                2,    // outOfPeriodTransactions
                4     // validationErrors
            );

        // Act
        int totalRejected = summary.getTotalRejected();

        // Assert
        assertEquals(9, totalRejected, "Total rejected should be sum of all rejection types");
        assertEquals(3 + 2 + 4, totalRejected);
    }

    @Test
    void processingSummary_withZeroCounts_shouldHandleGracefully() {
        // Arrange
        BankStatementUploadResponse.ProcessingSummary summary = 
            new BankStatementUploadResponse.ProcessingSummary(0, 0, 0, 0, 0);

        // Assert
        assertEquals(0, summary.getTotalLinesProcessed());
        assertEquals(0, summary.getValidTransactions());
        assertEquals(0, summary.getDuplicateTransactions());
        assertEquals(0, summary.getOutOfPeriodTransactions());
        assertEquals(0, summary.getValidationErrors());
        assertEquals(0, summary.getTotalRejected());
    }

    @Test
    void message_withOnlyDuplicates_shouldMentionDuplicatesOnly() {
        // Arrange
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                createTransactionList(2),
                50, 2, 3, 0, 0,
                createRejectedList(3, 0, 0),
                new ArrayList<>()
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertTrue(response.getMessage().contains("3 duplicates"));
        assertTrue(response.getMessage().contains("0 out-of-period"));
        assertTrue(response.getMessage().contains("0 validation errors"));
    }

    @Test
    void message_withOnlyOutOfPeriod_shouldMentionOutOfPeriodOnly() {
        // Arrange
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                createTransactionList(2),
                50, 2, 0, 3, 0,
                createRejectedList(0, 3, 0),
                new ArrayList<>()
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertTrue(response.getMessage().contains("0 duplicates"));
        assertTrue(response.getMessage().contains("3 out-of-period"));
        assertTrue(response.getMessage().contains("0 validation errors"));
    }

    @Test
    void message_withOnlyValidationErrors_shouldMentionValidationErrorsOnly() {
        // Arrange
        BankStatementProcessingService.StatementProcessingResult result = 
            new BankStatementProcessingService.StatementProcessingResult(
                createTransactionList(2),
                50, 2, 0, 0, 3,
                createRejectedList(0, 0, 3),
                List.of("Error 1", "Error 2", "Error 3")
            );

        // Act
        BankStatementUploadResponse response = new BankStatementUploadResponse(result);

        // Assert
        assertTrue(response.getMessage().contains("0 duplicates"));
        assertTrue(response.getMessage().contains("0 out-of-period"));
        assertTrue(response.getMessage().contains("3 validation errors"));
    }

    // Helper methods

    private List<BankTransaction> createTransactionList(int count) {
        List<BankTransaction> transactions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            BankTransaction transaction = new BankTransaction();
            transaction.setId((long) (i + 1));
            transaction.setCompanyId(1L);
            transaction.setFiscalPeriodId(1L);
            transaction.setTransactionDate(LocalDate.of(2025, 1, 15));
            transaction.setDebitAmount(new BigDecimal("100.00"));
            transaction.setCreditAmount(BigDecimal.ZERO);
            transaction.setDetails("Transaction " + (i + 1));
            transaction.setBalance(new BigDecimal("1000.00"));
            transactions.add(transaction);
        }
        return transactions;
    }

    private List<RejectedTransaction> createRejectedList(int duplicates, int outOfPeriod, int validationErrors) {
        List<RejectedTransaction> rejected = new ArrayList<>();
        
        for (int i = 0; i < duplicates; i++) {
            rejected.add(new RejectedTransaction(
                LocalDate.of(2025, 1, 15),
                "Duplicate " + (i + 1),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                RejectedTransaction.RejectionReason.DUPLICATE,
                "Duplicate of transaction ID " + (i + 1)
            ));
        }
        
        for (int i = 0; i < outOfPeriod; i++) {
            rejected.add(new RejectedTransaction(
                LocalDate.of(2024, 12, 31),
                "Out of period " + (i + 1),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                RejectedTransaction.RejectionReason.OUT_OF_PERIOD,
                "Date before fiscal period start"
            ));
        }
        
        for (int i = 0; i < validationErrors; i++) {
            rejected.add(new RejectedTransaction(
                LocalDate.of(2025, 1, 15),
                null,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000.00"),
                RejectedTransaction.RejectionReason.VALIDATION_ERROR,
                "Validation error " + (i + 1)
            ));
        }
        
        return rejected;
    }
}
