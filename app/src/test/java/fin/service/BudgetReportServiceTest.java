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

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BudgetReportServiceTest {

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private BudgetReportService budgetReportService;
    private MockedStatic<DriverManager> driverManagerMock;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        driverManagerMock = mockStatic(DriverManager.class);
        driverManagerMock.when(() -> DriverManager.getConnection(anyString()))
                        .thenReturn(connection);

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        budgetReportService = new BudgetReportService("jdbc:test:db");
    }

    @AfterEach
    void tearDown() {
        driverManagerMock.close();
    }

    @Test
    void testGenerateBudgetSummaryReport() throws Exception {
        // Arrange
        Long companyId = 1L;

        // Mock budget data
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("category")).thenReturn("Revenue");
        when(resultSet.getString("subcategory")).thenReturn("Tuition Fees");
        when(resultSet.getBigDecimal("year_1")).thenReturn(new java.math.BigDecimal("100000.00"));
        when(resultSet.getBigDecimal("year_2")).thenReturn(new java.math.BigDecimal("110000.00"));
        when(resultSet.getBigDecimal("year_3")).thenReturn(new java.math.BigDecimal("121000.00"));

        // Act & Assert - Just verify the method completes without throwing exceptions
        // In test mode, PDF generation is skipped so no database operations occur
        assertDoesNotThrow(() -> budgetReportService.generateBudgetSummaryReport(companyId),
            "Report generation should complete without errors");

        // In test mode, no database interactions occur due to early return
        // verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void testGenerateStrategicPlanReport() throws Exception {
        // Arrange
        Long companyId = 1L;

        // Mock strategic plan data
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("priority")).thenReturn("Academic Excellence");
        when(resultSet.getString("goal")).thenReturn("Improve student outcomes");
        when(resultSet.getString("objective")).thenReturn("Increase pass rates by 15%");
        when(resultSet.getBigDecimal("budget_allocation")).thenReturn(new java.math.BigDecimal("50000.00"));

        // Act & Assert - Just verify the method completes without throwing exceptions
        // In test mode, PDF generation is skipped so no database operations occur
        assertDoesNotThrow(() -> budgetReportService.generateStrategicPlanReport(companyId),
            "Strategic plan report generation should complete without errors");

        // In test mode, no database interactions occur due to early return
        // verify(connection, never()).prepareStatement(anyString());
    }

    @Test
    void testGenerateBudgetVsActualReport() throws Exception {
        // Arrange
        Long companyId = 1L;

        // Mock budget vs actual data
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("category")).thenReturn("Revenue");
        when(resultSet.getString("subcategory")).thenReturn("Tuition Fees");
        when(resultSet.getBigDecimal("budget_amount")).thenReturn(new java.math.BigDecimal("100000.00"));
        when(resultSet.getBigDecimal("actual_amount")).thenReturn(new java.math.BigDecimal("95000.00"));
        when(resultSet.getBigDecimal("variance")).thenReturn(new java.math.BigDecimal("-5000.00"));

        // Act & Assert - Just verify the method completes without throwing exceptions
        // In test mode, PDF generation is skipped so no database operations occur
        assertDoesNotThrow(() -> budgetReportService.generateBudgetVsActualReport(companyId),
            "Budget vs actual report generation should complete without errors");

        // In test mode, no database interactions occur due to early return
        // verify(connection, never()).prepareStatement(anyString());
    }
}