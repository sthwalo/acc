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

package fin.controller;

import fin.service.PayrollService;
import fin.service.PayrollReportService;
import fin.model.Employee;
import fin.model.PayrollPeriod;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PayrollControllerTest {
    
    @Mock(lenient = true) private PayrollService payrollService;
    @Mock(lenient = true) private PayrollReportService payrollReportService;
    @Mock(lenient = true) private InputHandler inputHandler;
    @Mock(lenient = true) private OutputFormatter outputFormatter;
    
    private PayrollController payrollController;
    
    @BeforeAll
    static void setUpClass() {
        // Skip database setup for unit tests
        System.setProperty("test.skip.db", "true");
        System.setProperty("test.mode", "true");
    }
    
    @BeforeEach
    void setUp() {
        payrollController = new PayrollController(payrollService, payrollReportService, inputHandler, outputFormatter);
        
        // Setup default mocks to prevent real operations
        when(payrollService.getActiveEmployees(anyLong())).thenReturn(Collections.emptyList());
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(Collections.emptyList());
        doNothing().when(payrollService).processPayroll(anyLong(), anyString());
        doNothing().when(inputHandler).waitForEnter();
        doNothing().when(outputFormatter).printHeader(anyString());
        doNothing().when(outputFormatter).printPlain(anyString());
        doNothing().when(outputFormatter).printSuccess(anyString());
        doNothing().when(outputFormatter).printError(anyString());
    }
    
    @Test
    @Timeout(10) // 10 second timeout to prevent hanging
    void handlePayrollManagement_ListEmployees_CompletesSuccessfully() {
        // Mock menu navigation: Use anyInt() to match all range calls
        when(inputHandler.getInteger(anyString(), anyInt(), anyInt()))
            .thenReturn(1) // Select Employee Management
            .thenReturn(1) // Select List Employees
            .thenReturn(5) // Back to Payroll Management
            .thenReturn(7); // Back to Main Menu
        
        Employee mockEmployee = mock(Employee.class);
        when(mockEmployee.getEmployeeNumber()).thenReturn("EMP001");
        when(mockEmployee.getFirstName()).thenReturn("John");
        when(mockEmployee.getLastName()).thenReturn("Doe");
        when(payrollService.getActiveEmployees(anyLong())).thenReturn(List.of(mockEmployee));
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        verify(payrollService, atLeastOnce()).getActiveEmployees(1L);
        verify(outputFormatter, atLeastOnce()).printPlain(contains("Active Employees"));
    }
    
    @Test
    @Timeout(10)
    void handlePayrollManagement_ListPayrollPeriods_CompletesSuccessfully() {
        // Mock menu navigation: Use anyInt() to match all range calls
        when(inputHandler.getInteger(anyString(), anyInt(), anyInt()))
            .thenReturn(2) // Select Payroll Period Management
            .thenReturn(1) // Select List Payroll Periods
            .thenReturn(5) // Back to Payroll Management
            .thenReturn(7); // Back to Main Menu
        
        PayrollPeriod mockPeriod = mock(PayrollPeriod.class);
        when(mockPeriod.getPeriodName()).thenReturn("September 2025");
        when(mockPeriod.getStartDate()).thenReturn(LocalDate.of(2025, 9, 1));
        when(mockPeriod.getEndDate()).thenReturn(LocalDate.of(2025, 9, 30));
        when(mockPeriod.getStatus()).thenReturn(PayrollPeriod.PayrollStatus.OPEN);
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(List.of(mockPeriod));
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        verify(payrollService, atLeastOnce()).getPayrollPeriods(1L);
        verify(outputFormatter, atLeastOnce()).printPlain(contains("Payroll Periods"));
    }
    
    @Test
    @Timeout(15) // Longer timeout for payroll processing
    void handlePayrollManagement_ProcessPayroll_CompletesSuccessfully() {
        PayrollPeriod mockPeriod = mock(PayrollPeriod.class);
        when(mockPeriod.getId()).thenReturn(1L);
        when(mockPeriod.getPeriodName()).thenReturn("Test Period");
        when(mockPeriod.getStartDate()).thenReturn(LocalDate.now());
        when(mockPeriod.getEndDate()).thenReturn(LocalDate.now());
        when(mockPeriod.canBeProcessed()).thenReturn(true);
        
        // Mock complete menu flow: 3 (Process Payroll) -> 1 (select period) -> 7 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), anyInt(), anyInt()))
            .thenReturn(3) // Select Process Payroll
            .thenReturn(1) // Select first period
            .thenReturn(7); // Back to Main Menu
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(List.of(mockPeriod));
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        verify(payrollService, atLeastOnce()).processPayroll(1L, "system");
    }
    
    @Test
    @Timeout(10)
    void handlePayrollManagement_BackToMainMenu_ExitsCorrectly() {
        // Mock immediate exit: 7 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), anyInt(), anyInt())).thenReturn(7);
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        // Verify no service calls were made
        verify(payrollService, never()).getActiveEmployees(anyLong());
        verify(payrollService, never()).getPayrollPeriods(anyLong());
        verify(payrollService, never()).processPayroll(anyLong(), anyString());
    }
    
    @Test
    @Timeout(10)
    void handlePayrollManagement_InvalidOption_HandledGracefully() {
        // Mock invalid option then exit: 999 (invalid) -> 7 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), anyInt(), anyInt()))
            .thenReturn(999) // Invalid option
            .thenReturn(7);  // Exit
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        // Should handle invalid input gracefully
        verify(outputFormatter, atLeastOnce()).printError(anyString());
    }
}