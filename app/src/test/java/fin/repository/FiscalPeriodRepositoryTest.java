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
import fin.model.FiscalPeriod;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

class FiscalPeriodRepositoryTest {

    private FiscalPeriodRepository repository;
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
        repository = new FiscalPeriodRepository(TestConfiguration.TEST_DB_URL);

        // Clean up any existing test data
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM fiscal_periods");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void save_NewFiscalPeriod_SavesSuccessfully() {
        // Arrange
        FiscalPeriod period = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));

        // Act
        FiscalPeriod saved = repository.save(period);

        // Assert
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals(1L, saved.getCompanyId());
        assertEquals("2024", saved.getPeriodName());
        assertFalse(saved.isClosed());
    }

    @Test
    void findById_ExistingFiscalPeriod_ReturnsFiscalPeriod() {
        // Arrange
        FiscalPeriod period = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod saved = repository.save(period);

        // Act
        Optional<FiscalPeriod> found = repository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("2024", found.get().getPeriodName());
    }

    @Test
    void findById_NonExistingFiscalPeriod_ReturnsEmpty() {
        // Act
        Optional<FiscalPeriod> found = repository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_NoFiscalPeriods_ReturnsEmptyList() {
        // Act
        List<FiscalPeriod> periods = repository.findAll();

        // Assert
        assertNotNull(periods);
        assertTrue(periods.isEmpty());
    }

    @Test
    void findAll_WithFiscalPeriods_ReturnsAllFiscalPeriods() {
        // Arrange
        FiscalPeriod period1 = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod period2 = new FiscalPeriod(1L, "2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        repository.save(period1);
        repository.save(period2);

        // Act
        List<FiscalPeriod> periods = repository.findAll();

        // Assert
        assertNotNull(periods);
        assertEquals(2, periods.size());
        assertTrue(periods.stream().anyMatch(p -> p.getPeriodName().equals("2024")));
        assertTrue(periods.stream().anyMatch(p -> p.getPeriodName().equals("2025")));
    }

    @Test
    void findByCompanyId_ReturnsMatchingFiscalPeriods() {
        // Arrange
        FiscalPeriod period1 = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod period2 = new FiscalPeriod(1L, "2025", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));

        repository.save(period1);
        repository.save(period2);

        // Act
        List<FiscalPeriod> periods = repository.findByCompanyId(1L);

        // Assert
        assertNotNull(periods);
        assertEquals(2, periods.size());
        assertTrue(periods.stream().anyMatch(p -> p.getPeriodName().equals("2024")));
        assertTrue(periods.stream().anyMatch(p -> p.getPeriodName().equals("2025")));
    }

    @Test
    void findActiveByCompanyId_ReturnsOnlyActiveFiscalPeriods() {
        // Arrange
        FiscalPeriod activePeriod = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod closedPeriod = new FiscalPeriod(1L, "2023", LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31));
        closedPeriod.setClosed(true);

        repository.save(activePeriod);
        repository.save(closedPeriod);

        // Act
        List<FiscalPeriod> activePeriods = repository.findActiveByCompanyId(1L);

        // Assert
        assertNotNull(activePeriods);
        assertEquals(1, activePeriods.size());
        assertEquals("2024", activePeriods.get(0).getPeriodName());
        assertFalse(activePeriods.get(0).isClosed());
    }

    @Test
    void delete_ExistingFiscalPeriod_DeletesSuccessfully() {
        // Arrange
        FiscalPeriod period = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod saved = repository.save(period);

        // Act
        repository.delete(saved);
        Optional<FiscalPeriod> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingFiscalPeriod_DeletesSuccessfully() {
        // Arrange
        FiscalPeriod period = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod saved = repository.save(period);

        // Act
        repository.deleteById(saved.getId());
        Optional<FiscalPeriod> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void exists_ExistingFiscalPeriod_ReturnsTrue() {
        // Arrange
        FiscalPeriod period = new FiscalPeriod(1L, "2024", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31));
        FiscalPeriod saved = repository.save(period);

        // Act
        boolean exists = repository.exists(saved.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void exists_NonExistingFiscalPeriod_ReturnsFalse() {
        // Act
        boolean exists = repository.exists(999L);

        // Assert
        assertFalse(exists);
    }
}
