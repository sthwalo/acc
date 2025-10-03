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
 * you may not use this file except in compliance with the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fin.repository;

import fin.TestConfiguration;
import fin.model.BankTransaction;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class BankTransactionRepositoryTest {

    private BankTransactionRepository repository;
    private Connection connection;

    @BeforeAll
    static void setUpClass() throws Exception {
        TestConfiguration.setupTestDatabase();
    }

    @AfterAll
    static void tearDownClass() throws Exception {
        TestConfiguration.cleanupTestDatabase();
    }

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(
            TestConfiguration.TEST_DB_URL,
            TestConfiguration.TEST_DB_USER,
            TestConfiguration.TEST_DB_PASSWORD
        );
        repository = new BankTransactionRepository(TestConfiguration.TEST_DB_URL);

        // Clean up any existing test data
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM bank_transactions");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void save_NewTransaction_SavesSuccessfully() {
        // Arrange
        BankTransaction transaction = createTestTransaction();

        // Act
        BankTransaction saved = repository.save(transaction);

        // Assert
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals(1L, saved.getCompanyId());
        assertEquals("Test transaction", saved.getDetails());
        assertEquals(new BigDecimal("100.00"), saved.getDebitAmount());
    }

    @Test
    void findById_ExistingTransaction_ReturnsTransaction() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        BankTransaction saved = repository.save(transaction);

        // Act
        Optional<BankTransaction> found = repository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Test transaction", found.get().getDetails());
    }

    @Test
    void findById_NonExistingTransaction_ReturnsEmpty() {
        // Act
        Optional<BankTransaction> found = repository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_NoTransactions_ReturnsEmptyList() {
        // Act
        List<BankTransaction> transactions = repository.findAll();

        // Assert
        assertNotNull(transactions);
        assertTrue(transactions.isEmpty());
    }

    @Test
    void findAll_WithTransactions_ReturnsAllTransactions() {
        // Arrange
        BankTransaction transaction1 = createTestTransaction();
        BankTransaction transaction2 = createTestTransaction();
        transaction2.setDetails("Second transaction");

        repository.save(transaction1);
        repository.save(transaction2);

        // Act
        List<BankTransaction> transactions = repository.findAll();

        // Assert
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().anyMatch(t -> t.getDetails().equals("Test transaction")));
        assertTrue(transactions.stream().anyMatch(t -> t.getDetails().equals("Second transaction")));
    }

    @Test
    void delete_ExistingTransaction_DeletesSuccessfully() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        BankTransaction saved = repository.save(transaction);

        // Act
        repository.delete(saved);
        Optional<BankTransaction> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingTransaction_DeletesSuccessfully() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        BankTransaction saved = repository.save(transaction);

        // Act
        repository.deleteById(saved.getId());
        Optional<BankTransaction> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void exists_ExistingTransaction_ReturnsTrue() {
        // Arrange
        BankTransaction transaction = createTestTransaction();
        BankTransaction saved = repository.save(transaction);

        // Act
        boolean exists = repository.exists(saved.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void exists_NonExistingTransaction_ReturnsFalse() {
        // Act
        boolean exists = repository.exists(999L);

        // Assert
        assertFalse(exists);
    }

    @Test
    void findByCompanyAndFiscalPeriod_ReturnsMatchingTransactions() {
        // Arrange
        BankTransaction transaction1 = createTestTransaction();
        BankTransaction transaction2 = createTestTransaction();
        transaction2.setCompanyId(1L);
        transaction2.setFiscalPeriodId(1L);

        repository.save(transaction1);
        repository.save(transaction2);

        // Act
        List<BankTransaction> transactions = repository.findByCompanyAndFiscalPeriod(1L, 1L);

        // Assert
        assertNotNull(transactions);
        assertEquals(2, transactions.size());
        assertTrue(transactions.stream().allMatch(t -> t.getCompanyId().equals(1L)));
        assertTrue(transactions.stream().allMatch(t -> t.getFiscalPeriodId().equals(1L)));
    }

    @Test
    void findByDateRange_ReturnsTransactionsInRange() {
        // Arrange
        BankTransaction transaction1 = createTestTransaction();
        transaction1.setTransactionDate(LocalDate.of(2024, 1, 15));

        BankTransaction transaction2 = createTestTransaction();
        transaction2.setTransactionDate(LocalDate.of(2024, 2, 15));

        repository.save(transaction1);
        repository.save(transaction2);

        // Act
        List<BankTransaction> transactions = repository.findByDateRange(
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 1, 31)
        );

        // Assert
        assertNotNull(transactions);
        assertEquals(1, transactions.size());
        assertEquals(LocalDate.of(2024, 1, 15), transactions.get(0).getTransactionDate());
    }

    private BankTransaction createTestTransaction() {
        BankTransaction transaction = new BankTransaction();
        transaction.setCompanyId(1L);
        transaction.setFiscalPeriodId(1L);
        transaction.setTransactionDate(LocalDate.of(2024, 1, 15));
        transaction.setDetails("Test transaction");
        transaction.setDebitAmount(new BigDecimal("100.00"));
        transaction.setCreditAmount(BigDecimal.ZERO);
        transaction.setBalance(new BigDecimal("1000.00"));
        transaction.setServiceFee(false);
        transaction.setAccountNumber("1234567890");
        transaction.setStatementPeriod("Jan 2024");
        transaction.setSourceFile("test.pdf");
        return transaction;
    }
}
