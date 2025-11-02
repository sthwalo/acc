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

package fin.service;

import fin.model.BankTransaction;
import fin.model.ClassificationResult;
import fin.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test-Driven Development for JournalEntryGenerator module
 * This module handles the creation of journal entries for classified transactions
 *
 * STATUS: Service implemented, tests enabled
 * TODO: Verify integration with AccountRepository and classification services
 */
public class JournalEntryGeneratorTest {

    @Mock
    private Connection mockConnection;

    @Mock
    private PreparedStatement mockPreparedStatement;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private AccountRepository mockAccountRepository;

    private JournalEntryGenerator journalGenerator;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockConnection.prepareStatement(anyString(), anyInt())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockPreparedStatement.getGeneratedKeys()).thenReturn(mockResultSet);

        journalGenerator = new TestableJournalEntryGenerator("jdbc:test:db", mockAccountRepository, mockConnection);
    }

    @Test
    void testCreateJournalEntryForDebitTransaction() throws SQLException {
        // Given
        BankTransaction transaction = createDebitTransaction("INSURANCE PREMIUM PAYMENT", 1500.00);
        transaction.setId(1001L);
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);

        ClassificationResult classificationResult = new ClassificationResult("8800", "Insurance", "INSURANCE PREMIUM");

        when(mockAccountRepository.getOrCreateDetailedAccount("8800", "Insurance", "8800", "Expenses")).thenReturn(2001L);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("id")).thenReturn(3001L); // Bank account ID

        // When
        boolean success = journalGenerator.createJournalEntryForTransaction(transaction, classificationResult);

        // Then
        assertTrue(success);
        verify(mockAccountRepository).getOrCreateDetailedAccount("8800", "Insurance", "8800", "Expenses");
        verify(mockPreparedStatement, times(3)).executeUpdate(); // 1 header + 2 lines
    }

    @Test
    void testCreateJournalEntryForCreditTransaction() throws SQLException {
        // Given
        BankTransaction transaction = createCreditTransaction("DIRECTOR LOAN PAYMENT", 5000.00);
        transaction.setId(1002L);
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);

        ClassificationResult classificationResult = new ClassificationResult("2000", "Director Loans", "DIRECTOR LOAN");

        when(mockAccountRepository.getOrCreateDetailedAccount("2000", "Director Loans", "2000", "Liabilities")).thenReturn(2002L);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("id")).thenReturn(3001L); // Bank account ID

        // When
        boolean success = journalGenerator.createJournalEntryForTransaction(transaction, classificationResult);

        // Then
        assertTrue(success);
        verify(mockAccountRepository).getOrCreateDetailedAccount("2000", "Director Loans", "2000", "Liabilities");
        verify(mockPreparedStatement, times(3)).executeUpdate(); // 1 header + 2 lines
    }

    @Test
    void testCreateJournalEntryForTransaction_Failure() throws SQLException {
        // Given
        BankTransaction transaction = createDebitTransaction("TEST TRANSACTION", 100.00);
        transaction.setId(1003L);

        ClassificationResult classificationResult = new ClassificationResult("1000", "Test Account", "TEST");

        when(mockAccountRepository.getOrCreateDetailedAccount(anyString(), anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // When
        boolean success = journalGenerator.createJournalEntryForTransaction(transaction, classificationResult);

        // Then
        assertFalse(success);
    }

    @Test
    void testGenerateJournalEntriesForUnclassifiedTransactions() throws SQLException {
        // Given - placeholder test for batch processing
        Long companyId = 1L;

        // When
        int entriesCreated = journalGenerator.generateJournalEntriesForUnclassifiedTransactions(companyId);

        // Then
        assertEquals(0, entriesCreated); // Currently returns 0 as batch processing is not implemented
    }

    // Helper methods
    private BankTransaction createDebitTransaction(String details, double amount) {
        BankTransaction transaction = new BankTransaction();
        transaction.setDetails(details);
        transaction.setDebitAmount(new BigDecimal(String.valueOf(amount)));
        transaction.setTransactionDate(LocalDate.now());
        return transaction;
    }

    private BankTransaction createCreditTransaction(String details, double amount) {
        BankTransaction transaction = new BankTransaction();
        transaction.setDetails(details);
        transaction.setCreditAmount(new BigDecimal(String.valueOf(amount)));
        transaction.setTransactionDate(LocalDate.now());
        return transaction;
    }
}

/**
 * Testable version of JournalEntryGenerator that uses a mock connection
 */
class TestableJournalEntryGenerator extends JournalEntryGenerator {
    private final Connection mockConnection;

    public TestableJournalEntryGenerator(String dbUrl, AccountRepository accountRepository, Connection mockConnection) {
        super(dbUrl, accountRepository);
        this.mockConnection = mockConnection;
    }

    @Override
    protected Connection getConnection() throws SQLException {
        return mockConnection;
    }
}