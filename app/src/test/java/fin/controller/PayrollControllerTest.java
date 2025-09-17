package fin.controller;

import fin.controller.PayrollController;
import fin.service.PayrollService;
import fin.model.Employee;
import fin.model.PayrollPeriod;
import fin.ui.InputHandler;
import fin.ui.OutputFormatter;
import org.junit.jupiter.api.*;
import org.mockito.*;

import java.util.Collections;

import static org.mockito.Mockito.*;

class PayrollControllerTest {
    @Mock PayrollService payrollService;
    @Mock InputHandler inputHandler;
    @Mock OutputFormatter outputFormatter;
    @Mock Employee employee;

    PayrollController payrollController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        payrollController = new PayrollController(payrollService, inputHandler, outputFormatter);
    }

    @Test
    void handlePayrollManagement_ListEmployees() {
        when(inputHandler.getInteger(anyString(), eq(1), eq(4))).thenReturn(1, 4);
        when(payrollService.getActiveEmployees(anyLong())).thenReturn(Collections.emptyList());
        payrollController.handlePayrollManagement(1L);
        verify(payrollService).getActiveEmployees(1L);
        verify(outputFormatter).printPlain("Active Employees:");
    }

    @Test
    void handlePayrollManagement_ListPayrollPeriods() {
        when(inputHandler.getInteger(anyString(), eq(1), eq(4))).thenReturn(2, 4);
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(Collections.emptyList());
        payrollController.handlePayrollManagement(1L);
        verify(payrollService).getPayrollPeriods(1L);
        verify(outputFormatter).printPlain("Payroll Periods:");
    }

    @Test
    void handlePayrollManagement_ProcessPayroll() {
        PayrollPeriod period = mock(PayrollPeriod.class);
        when(period.getId()).thenReturn(1L);
        when(period.getPeriodName()).thenReturn("Test Period");
        when(period.getStartDate()).thenReturn(java.time.LocalDate.now());
        when(period.getEndDate()).thenReturn(java.time.LocalDate.now());
        when(inputHandler.getInteger(anyString(), eq(1), eq(4))).thenReturn(3, 4);
        when(payrollService.getPayrollPeriods(anyLong())).thenReturn(Collections.singletonList(period));
        when(inputHandler.getInteger(anyString(), eq(1), eq(1))).thenReturn(1);
        payrollController.handlePayrollManagement(1L);
        verify(payrollService).processPayroll(1L, "system");
        verify(outputFormatter).printSuccess("Payroll processed for period: Test Period");
    }
}