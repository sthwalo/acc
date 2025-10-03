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
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
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
        // Mock menu navigation: 1 (List Employees) -> 6 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), eq(1), eq(6)))
            .thenReturn(1) // Select Employee Management
            .thenReturn(1) // Select List Employees
            .thenReturn(5) // Back to Payroll Management
            .thenReturn(6); // Back to Main Menu
        
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
        // Mock menu navigation: 2 (Payroll Period Management) -> 1 (List Periods) -> 5 (Back) -> 6 (Back to Main)
        when(inputHandler.getInteger(anyString(), eq(1), eq(6)))
            .thenReturn(2) // Select Payroll Period Management
            .thenReturn(1) // Select List Payroll Periods
            .thenReturn(5) // Back to Payroll Management
            .thenReturn(6); // Back to Main Menu
        
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
        
        // Mock complete menu flow: 3 (Process Payroll) -> 1 (select period) -> 6 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), eq(1), eq(6)))
            .thenReturn(3) // Select Process Payroll
            .thenReturn(6); // Back to Main Menu
        when(inputHandler.getInteger(anyString(), eq(1), eq(1))).thenReturn(1); // Select first period
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(List.of(mockPeriod));
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        verify(payrollService, atLeastOnce()).processPayroll(1L, "system");
    }
    
    @Test
    @Timeout(10)
    void handlePayrollManagement_BackToMainMenu_ExitsCorrectly() {
        // Mock immediate exit: 6 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), eq(1), eq(6))).thenReturn(6);
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        // Verify no service calls were made
        verify(payrollService, never()).getActiveEmployees(anyLong());
        verify(payrollService, never()).getPayrollPeriods(anyLong());
        verify(payrollService, never()).processPayroll(anyLong(), anyString());
    }
    
    @Test
    @Timeout(10)
    void handlePayrollManagement_InvalidOption_HandledGracefully() {
        // Mock invalid option then exit: 999 (invalid) -> 6 (Back to Main Menu)
        when(inputHandler.getInteger(anyString(), eq(1), eq(6)))
            .thenReturn(999) // Invalid option
            .thenReturn(6);  // Exit
        
        assertDoesNotThrow(() -> payrollController.handlePayrollManagement(1L));
        
        // Should handle invalid input gracefully
        verify(outputFormatter, atLeastOnce()).printError(anyString());
    }
}