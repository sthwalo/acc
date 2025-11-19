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

import fin.model.*;
import fin.repository.CompanyRepository;
import fin.repository.EmployeeImportDefaultsRepository;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PayrollServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PayslipPdfService pdfService;

    @Mock
    private EmailService emailService;

    @Mock
    private EmployeeImportDefaultsRepository employeeImportDefaultsRepository;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private PayrollService payrollService;
    private MockedStatic<DriverManager> driverManagerMock;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(() -> DriverManager.getConnection(anyString()))
                        .thenReturn(connection);

        payrollService = new PayrollService("jdbc:test:db", companyRepository, pdfService, emailService, employeeImportDefaultsRepository);
    }

    @AfterEach
    void tearDown() {
        driverManagerMock.close();
    }

    @Test
    void getEmployeeById_WithValidId_ReturnsEmployee() throws SQLException {
        // Arrange
        Long employeeId = 1L;
        String sql = "SELECT * FROM employees WHERE id = ?";

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("id")).thenReturn(employeeId);
        when(resultSet.getLong("company_id")).thenReturn(1L);
        when(resultSet.getString("employee_number")).thenReturn("EMP001");
        when(resultSet.getString("first_name")).thenReturn("John");
        when(resultSet.getString("last_name")).thenReturn("Doe");
        when(resultSet.getBigDecimal("basic_salary")).thenReturn(new BigDecimal("15000.00"));
        when(resultSet.getBoolean("is_active")).thenReturn(true);
        when(resultSet.getDate("hire_date")).thenReturn(java.sql.Date.valueOf(LocalDate.now()));
        when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(java.time.LocalDateTime.now()));

        // Act
        Optional<Employee> result = payrollService.getEmployeeById(employeeId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(employeeId, result.get().getId());
        assertEquals("John Doe", result.get().getFullName());
        verify(connection).prepareStatement(sql);
        verify(preparedStatement).setLong(1, employeeId);
    }

    @Test
    void createEmployee_WithValidData_CreatesAndReturnsEmployee() throws SQLException {
        // Arrange
        Employee employee = createTestEmployee(null, "John Doe");
        String sql = """
            INSERT INTO employees (company_id, employee_number, first_name, last_name, email, phone, 
                                 position, department, hire_date, basic_salary, employment_type, 
                                 salary_type, tax_number, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong("id")).thenReturn(1L);

        // Act
        Employee result = payrollService.createEmployee(employee);

        // Assert
        assertNotNull(result.getId());
        assertEquals(1L, result.getId());
        assertEquals("John Doe", result.getFullName());
        verify(connection).prepareStatement(sql);
    }

    @Test
    void updateEmployee_WithValidData_UpdatesAndReturnsEmployee() throws SQLException {
        // Arrange
        Employee employee = createTestEmployee(1L, "John Doe");
        employee.setFirstName("Johnny");
        String sql = """
            UPDATE employees SET 
                employee_number = ?, first_name = ?, last_name = ?, email = ?, phone = ?, 
                position = ?, department = ?, hire_date = ?, basic_salary = ?, employment_type = ?, 
                salary_type = ?, tax_number = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND company_id = ?
            """;

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act
        Employee result = payrollService.updateEmployee(employee);

        // Assert
        assertEquals("Johnny Doe", result.getFullName());
        verify(connection).prepareStatement(sql);
    }

    @Test
    void deleteEmployee_WithValidId_DeletesEmployee() throws SQLException {
        // Arrange
        Long employeeId = 1L;
        Long companyId = 1L;
        String sql = "UPDATE employees SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND company_id = ?";

        when(connection.prepareStatement(sql)).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);

        // Act & Assert
        assertDoesNotThrow(() -> payrollService.deleteEmployee(employeeId, companyId));
        verify(connection).prepareStatement(sql);
        verify(preparedStatement).setLong(1, employeeId);
        verify(preparedStatement).setLong(2, companyId);
    }

    private Employee createTestEmployee(Long id, String fullName) {
        Employee employee = new Employee();
        employee.setId(id);
        String[] names = fullName.split(" ");
        employee.setFirstName(names[0]);
        employee.setLastName(names[1]);
        employee.setEmployeeNumber("EMP" + (id != null ? id.toString() : "NEW"));
        employee.setCompanyId(1L);
        employee.setBasicSalary(new BigDecimal("15000.00"));
        employee.setActive(true);
        employee.setHireDate(LocalDate.now());
        employee.setEmploymentType(Employee.EmploymentType.PERMANENT);
        employee.setSalaryType(Employee.SalaryType.MONTHLY);
        employee.setCreatedBy("test-user");
        employee.setEmail("john.doe@example.com");
        employee.setPhone("123-456-7890");
        employee.setPosition("Developer");
        employee.setDepartment("IT");
        employee.setTaxNumber("1234567890");
        return employee;
    }
}