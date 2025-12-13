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
import fin.entity.FiscalPeriod;
import fin.repository.FiscalPeriodRepository;
import fin.service.FiscalPeriodBoundaryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FiscalPeriodBoundaryValidator service.
 * 
 * <p>Tests fiscal period boundary validation to ensure transactions
 * fall within their assigned fiscal period date ranges.
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
class FiscalPeriodBoundaryValidatorTest {

    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;

    private FiscalPeriodBoundaryValidator validator;

    private FiscalPeriod testFiscalPeriod;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new FiscalPeriodBoundaryValidator(fiscalPeriodRepository);
        
        // Create test fiscal period: Jan 1 to Jan 31, 2025
        testFiscalPeriod = new FiscalPeriod();
        testFiscalPeriod.setId(1L);
        testFiscalPeriod.setPeriodName("January 2025");
        testFiscalPeriod.setStartDate(LocalDate.of(2025, 1, 1));
        testFiscalPeriod.setEndDate(LocalDate.of(2025, 1, 31));
    }

    @Test
    void isWithinFiscalPeriod_withValidTransactionDate_shouldReturnTrue() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertTrue(result, "Transaction date within period should be valid");
        verify(fiscalPeriodRepository).findById(eq(1L));
    }

    @Test
    void isWithinFiscalPeriod_onStartDate_shouldReturnTrue() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 1));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertTrue(result, "Transaction on period start date should be valid");
    }

    @Test
    void isWithinFiscalPeriod_onEndDate_shouldReturnTrue() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 31));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertTrue(result, "Transaction on period end date should be valid");
    }

    @Test
    void isWithinFiscalPeriod_beforeStartDate_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2024, 12, 31));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertFalse(result, "Transaction before period start should be invalid");
    }

    @Test
    void isWithinFiscalPeriod_afterEndDate_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 2, 1));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertFalse(result, "Transaction after period end should be invalid");
    }

    @Test
    void isWithinFiscalPeriod_withNullTransaction_shouldReturnFalse() {
        // Act
        boolean result = validator.isWithinFiscalPeriod(null);

        // Assert
        assertFalse(result, "Null transaction should be invalid");
        verify(fiscalPeriodRepository, never()).findById(any());
    }

    @Test
    void isWithinFiscalPeriod_withNullFiscalPeriodId_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        transaction.setFiscalPeriodId(null);

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertFalse(result, "Transaction with null fiscal period ID should be invalid");
        verify(fiscalPeriodRepository, never()).findById(any());
    }

    @Test
    void isWithinFiscalPeriod_withNullTransactionDate_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTransaction(null);

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertFalse(result, "Transaction with null date should be invalid");
        verify(fiscalPeriodRepository, never()).findById(any());
    }

    @Test
    void isWithinFiscalPeriod_withNonExistentFiscalPeriod_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // Act
        boolean result = validator.isWithinFiscalPeriod(transaction);

        // Assert
        assertFalse(result, "Transaction with non-existent fiscal period should be invalid");
    }

    @Test
    void getValidationErrorMessage_forValidTransaction_shouldReturnNull() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        String errorMessage = validator.getValidationErrorMessage(transaction);

        // Assert
        assertNull(errorMessage, "Valid transaction should have no error message");
    }

    @Test
    void getValidationErrorMessage_forDateBeforeStart_shouldReturnSpecificMessage() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2024, 12, 31));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        String errorMessage = validator.getValidationErrorMessage(transaction);

        // Assert
        assertNotNull(errorMessage, "Transaction before period should have error message");
        assertTrue(errorMessage.contains("before fiscal period start date"), 
            "Error message should mention 'before start date'");
        assertTrue(errorMessage.contains("2024-12-31"), "Error message should include transaction date");
        assertTrue(errorMessage.contains("2025-01-01"), "Error message should include period start date");
    }

    @Test
    void getValidationErrorMessage_forDateAfterEnd_shouldReturnSpecificMessage() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 2, 1));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        String errorMessage = validator.getValidationErrorMessage(transaction);

        // Assert
        assertNotNull(errorMessage, "Transaction after period should have error message");
        assertTrue(errorMessage.contains("after fiscal period end date"), 
            "Error message should mention 'after end date'");
        assertTrue(errorMessage.contains("2025-02-01"), "Error message should include transaction date");
        assertTrue(errorMessage.contains("2025-01-31"), "Error message should include period end date");
    }

    @Test
    void getValidationErrorMessage_withNullTransaction_shouldReturnMessage() {
        // Act
        String errorMessage = validator.getValidationErrorMessage(null);

        // Assert
        assertNotNull(errorMessage, "Null transaction should have error message");
        assertTrue(errorMessage.contains("null"), "Error message should mention 'null'");
    }

    @Test
    void getValidationErrorMessage_withNullFiscalPeriodId_shouldReturnMessage() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        transaction.setFiscalPeriodId(null);

        // Act
        String errorMessage = validator.getValidationErrorMessage(transaction);

        // Assert
        assertNotNull(errorMessage, "Transaction with null fiscal period ID should have error message");
        assertTrue(errorMessage.contains("no fiscal period"), "Error message should mention missing fiscal period");
    }

    @Test
    void getValidationErrorMessage_withNonExistentFiscalPeriod_shouldReturnMessage() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.empty());

        // Act
        String errorMessage = validator.getValidationErrorMessage(transaction);

        // Assert
        assertNotNull(errorMessage, "Non-existent fiscal period should have error message");
        assertTrue(errorMessage.contains("not found"), "Error message should mention 'not found'");
    }

    @Test
    void validateTransaction_withValidDate_shouldReturnValidResult() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2025, 1, 15));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        FiscalPeriodBoundaryValidator.ValidationResult result = validator.validateTransaction(transaction);

        // Assert
        assertTrue(result.isValid(), "Validation result should be valid");
        assertNull(result.getErrorMessage(), "Valid result should have no error message");
    }

    @Test
    void validateTransaction_withInvalidDate_shouldReturnInvalidResult() {
        // Arrange
        BankTransaction transaction = createTransaction(LocalDate.of(2024, 12, 31));
        when(fiscalPeriodRepository.findById(eq(1L))).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        FiscalPeriodBoundaryValidator.ValidationResult result = validator.validateTransaction(transaction);

        // Assert
        assertFalse(result.isValid(), "Validation result should be invalid");
        assertNotNull(result.getErrorMessage(), "Invalid result should have error message");
        assertTrue(result.getErrorMessage().contains("before fiscal period"), 
            "Error message should explain the validation failure");
    }

    // Helper methods

    private BankTransaction createTransaction(LocalDate transactionDate) {
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);
        transaction.setTransactionDate(transactionDate);
        transaction.setDebitAmount(new BigDecimal("500.00"));
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setDetails("Test transaction");
        transaction.setBalance(new BigDecimal("1500.00"));
        return transaction;
    }
}
