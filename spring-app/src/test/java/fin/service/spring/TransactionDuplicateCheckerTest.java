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

import fin.model.BankTransaction;
import fin.repository.BankTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionDuplicateChecker service.
 * 
 * <p>Tests duplicate detection logic using 5-field matching:
 * company_id, transaction_date, debit_amount, credit_amount, balance.
 * 
 * @author Immaculate Nyoni
 * @since 2025-12-06
 */
class TransactionDuplicateCheckerTest {

    @Mock
    private BankTransactionRepository transactionRepository;

    private TransactionDuplicateChecker duplicateChecker;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        duplicateChecker = new TransactionDuplicateChecker(transactionRepository);
    }

    @Test
    void isDuplicate_withExistingTransaction_shouldReturnTrue() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        
        when(transactionRepository.existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(new BigDecimal("500.00")),
            eq(BigDecimal.ZERO),
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        )).thenReturn(true);

        // Act
        boolean result = duplicateChecker.isDuplicate(transaction);

        // Assert
        assertTrue(result, "Should detect duplicate transaction");
        verify(transactionRepository).existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(new BigDecimal("500.00")),
            eq(BigDecimal.ZERO),
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        );
    }

    @Test
    void isDuplicate_withNonExistingTransaction_shouldReturnFalse() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        
        when(transactionRepository.existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(false);

        // Act
        boolean result = duplicateChecker.isDuplicate(transaction);

        // Assert
        assertFalse(result, "Should not detect duplicate for unique transaction");
    }

    @Test
    void isDuplicate_withNullTransaction_shouldReturnFalse() {
        // Act
        boolean result = duplicateChecker.isDuplicate(null);

        // Assert
        assertFalse(result, "Should return false for null transaction");
        verify(transactionRepository, never()).existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void isDuplicate_withNullDebitAmount_shouldConvertToZero() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        transaction.setDebitAmount(null);
        
        when(transactionRepository.existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), eq(BigDecimal.ZERO), any(), any(), any()
        )).thenReturn(false);

        // Act
        duplicateChecker.isDuplicate(transaction);

        // Assert
        verify(transactionRepository).existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(BigDecimal.ZERO),  // Null converted to ZERO
            eq(BigDecimal.ZERO),
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        );
    }

    @Test
    void isDuplicate_withNullCreditAmount_shouldConvertToZero() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        transaction.setCreditAmount(null);
        
        when(transactionRepository.existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), any(), eq(BigDecimal.ZERO), any(), any()
        )).thenReturn(false);

        // Act
        duplicateChecker.isDuplicate(transaction);

        // Assert
        verify(transactionRepository).existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(new BigDecimal("500.00")),
            eq(BigDecimal.ZERO),  // Null converted to ZERO
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        );
    }

    @Test
    void findDuplicate_withExistingTransaction_shouldReturnDuplicate() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        BankTransaction existingTransaction = createExistingTransaction();
        
        when(transactionRepository.findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(new BigDecimal("500.00")),
            eq(BigDecimal.ZERO),
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        )).thenReturn(Optional.of(existingTransaction));

        // Act
        BankTransaction result = duplicateChecker.findDuplicate(transaction);

        // Assert
        assertNotNull(result, "Should find existing duplicate transaction");
        assertEquals(100L, result.getId(), "Should return existing transaction ID");
        verify(transactionRepository).findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(new BigDecimal("500.00")),
            eq(BigDecimal.ZERO),
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        );
    }

    @Test
    void findDuplicate_withNonExistingTransaction_shouldReturnNull() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        
        when(transactionRepository.findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), any(), any(), any(), any()
        )).thenReturn(Optional.empty());

        // Act
        BankTransaction result = duplicateChecker.findDuplicate(transaction);

        // Assert
        assertNull(result, "Should return null when no duplicate found");
    }

    @Test
    void findDuplicate_withNullTransaction_shouldReturnNull() {
        // Act
        BankTransaction result = duplicateChecker.findDuplicate(null);

        // Assert
        assertNull(result, "Should return null for null transaction");
        verify(transactionRepository, never()).findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void findDuplicate_withNullAmounts_shouldConvertToZero() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        transaction.setDebitAmount(null);
        transaction.setCreditAmount(null);
        
        when(transactionRepository.findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            any(), any(), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO), any(), any()
        )).thenReturn(Optional.empty());

        // Act
        duplicateChecker.findDuplicate(transaction);

        // Assert
        verify(transactionRepository).findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
            eq(1L),
            eq(LocalDate.of(2025, 12, 1)),
            eq(BigDecimal.ZERO),  // Null debit converted to ZERO
            eq(BigDecimal.ZERO),  // Null credit converted to ZERO
            eq("Payment to vendor"),
            eq(new BigDecimal("1500.00"))
        );
    }

    // Helper methods

    private BankTransaction createTestTransaction() {
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(1L);
        transaction.setTransactionDate(LocalDate.of(2025, 12, 1));
        transaction.setDebitAmount(new BigDecimal("500.00"));
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setDetails("Payment to vendor");
        transaction.setBalance(new BigDecimal("1500.00"));
        return transaction;
    }

    private BankTransaction createExistingTransaction() {
        BankTransaction transaction = new BankTransaction();
        transaction.setId(100L);  // Existing transaction has ID
        transaction.setCompanyId(1L);
        transaction.setTransactionDate(LocalDate.of(2025, 12, 1));
        transaction.setDebitAmount(new BigDecimal("500.00"));
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setDetails("Payment to vendor");
        transaction.setBalance(new BigDecimal("1500.00"));
        return transaction;
    }
}
