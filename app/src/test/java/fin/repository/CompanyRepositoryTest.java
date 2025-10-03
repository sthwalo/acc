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
import fin.model.Company;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

class CompanyRepositoryTest {

    private CompanyRepository repository;
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
        repository = new CompanyRepository(TestConfiguration.TEST_DB_URL);

        // Clean up any existing test data
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM companies");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void save_NewCompany_SavesSuccessfully() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        company.setRegistrationNumber("2023/123456/07");
        company.setTaxNumber("9876543210");
        company.setAddress("123 Test St, Test City");
        company.setContactEmail("test@example.com");
        company.setContactPhone("+27-11-123-4567");

        // Act
        Company saved = repository.save(company);

        // Assert
        assertNotNull(saved);
        assertTrue(saved.getId() > 0);
        assertEquals("Test Company Ltd", saved.getName());
        assertEquals("2023/123456/07", saved.getRegistrationNumber());
    }

    @Test
    void findById_ExistingCompany_ReturnsCompany() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        company.setRegistrationNumber("2023/123456/07");
        Company saved = repository.save(company);

        // Act
        Optional<Company> found = repository.findById(saved.getId());

        // Assert
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals("Test Company Ltd", found.get().getName());
    }

    @Test
    void findById_NonExistingCompany_ReturnsEmpty() {
        // Act
        Optional<Company> found = repository.findById(999L);

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void findAll_NoCompanies_ReturnsEmptyList() {
        // Act
        List<Company> companies = repository.findAll();

        // Assert
        assertNotNull(companies);
        assertTrue(companies.isEmpty());
    }

    @Test
    void findAll_WithCompanies_ReturnsAllCompanies() {
        // Arrange
        Company company1 = new Company("Company 1");
        Company company2 = new Company("Company 2");

        repository.save(company1);
        repository.save(company2);

        // Act
        List<Company> companies = repository.findAll();

        // Assert
        assertNotNull(companies);
        assertEquals(2, companies.size());
        assertTrue(companies.stream().anyMatch(c -> c.getName().equals("Company 1")));
        assertTrue(companies.stream().anyMatch(c -> c.getName().equals("Company 2")));
    }

    @Test
    void findByName_ExistingCompany_ReturnsCompany() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        repository.save(company);

        // Act
        Optional<Company> found = repository.findByName("Test Company Ltd");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Company Ltd", found.get().getName());
    }

    @Test
    void findByName_NonExistingCompany_ReturnsEmpty() {
        // Act
        Optional<Company> found = repository.findByName("NONEXISTENT");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void delete_ExistingCompany_DeletesSuccessfully() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        Company saved = repository.save(company);

        // Act
        repository.delete(saved);
        Optional<Company> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void deleteById_ExistingCompany_DeletesSuccessfully() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        Company saved = repository.save(company);

        // Act
        repository.deleteById(saved.getId());
        Optional<Company> found = repository.findById(saved.getId());

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    void exists_ExistingCompany_ReturnsTrue() {
        // Arrange
        Company company = new Company("Test Company Ltd");
        Company saved = repository.save(company);

        // Act
        boolean exists = repository.exists(saved.getId());

        // Assert
        assertTrue(exists);
    }

    @Test
    void exists_NonExistingCompany_ReturnsFalse() {
        // Act
        boolean exists = repository.exists(999L);

        // Assert
        assertFalse(exists);
    }
}
