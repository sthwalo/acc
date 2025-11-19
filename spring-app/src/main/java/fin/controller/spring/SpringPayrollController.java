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

package fin.controller.spring;

import fin.model.Employee;
import fin.model.Payslip;
import fin.service.spring.SpringPayrollService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for payroll operations.
 */
@RestController
@RequestMapping("/api/v1/payroll")
public class SpringPayrollController {

    private final SpringPayrollService payrollService;

    public SpringPayrollController(SpringPayrollService payrollService) {
        this.payrollService = payrollService;
    }

    /**
     * Create a new employee
     */
    @PostMapping("/employees")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        try {
            Employee createdEmployee = payrollService.createEmployee(
                employee.getEmployeeCode(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getCompanyId(),
                employee.getBasicSalary(),
                employee.getDateOfBirth(),
                employee.getDateEngaged(),
                employee.getIdNumber(),
                employee.getEmail()
            );
            return ResponseEntity.ok(createdEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get employee by ID
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Optional<Employee> employee = payrollService.getEmployeeById(id);
        return employee.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all employees for a company
     */
    @GetMapping("/employees/company/{companyId}")
    public ResponseEntity<List<Employee>> getEmployeesByCompany(@PathVariable Long companyId) {
        try {
            List<Employee> employees = payrollService.getEmployeesByCompany(companyId);
            return ResponseEntity.ok(employees);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active employees for a company
     */
    @GetMapping("/employees/company/{companyId}/active")
    public ResponseEntity<List<Employee>> getActiveEmployeesByCompany(@PathVariable Long companyId) {
        try {
            List<Employee> employees = payrollService.getActiveEmployeesByCompany(companyId);
            return ResponseEntity.ok(employees);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update an employee
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id, @RequestBody Employee employee) {
        try {
            Employee updatedEmployee = payrollService.updateEmployee(id, employee);
            return ResponseEntity.ok(updatedEmployee);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deactivate an employee
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deactivateEmployee(@PathVariable Long id) {
        try {
            payrollService.deactivateEmployee(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Create a payroll period
     */
    @PostMapping("/periods")
    public ResponseEntity<fin.model.PayrollPeriod> createPayrollPeriod(@RequestBody fin.model.PayrollPeriod period) {
        try {
            fin.model.PayrollPeriod createdPeriod = payrollService.createPayrollPeriod(
                period.getCompanyId(),
                period.getPeriodName(),
                period.getStartDate(),
                period.getEndDate(),
                period.getPaymentDate()
            );
            return ResponseEntity.ok(createdPeriod);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payroll periods for a company
     */
    @GetMapping("/periods/company/{companyId}")
    public ResponseEntity<List<fin.model.PayrollPeriod>> getPayrollPeriodsByCompany(@PathVariable Long companyId) {
        try {
            List<fin.model.PayrollPeriod> periods = payrollService.getPayrollPeriodsByCompany(companyId);
            return ResponseEntity.ok(periods);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Process payroll for a period
     */
    @PostMapping("/periods/{payrollPeriodId}/process")
    public ResponseEntity<SpringPayrollService.PayrollProcessingResult> processPayroll(@PathVariable Long payrollPeriodId) {
        try {
            SpringPayrollService.PayrollProcessingResult result = payrollService.processPayroll(payrollPeriodId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payslips for a payroll period
     */
    @GetMapping("/payslips/period/{payrollPeriodId}")
    public ResponseEntity<List<Payslip>> getPayslipsByPeriod(@PathVariable Long payrollPeriodId) {
        try {
            List<Payslip> payslips = payrollService.getPayslipsByPeriod(payrollPeriodId);
            return ResponseEntity.ok(payslips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payslips for an employee
     */
    @GetMapping("/payslips/employee/{employeeId}")
    public ResponseEntity<List<Payslip>> getPayslipsByEmployee(@PathVariable Long employeeId) {
        try {
            List<Payslip> payslips = payrollService.getPayslipsByEmployee(employeeId);
            return ResponseEntity.ok(payslips);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get payroll summary for a period
     */
    @GetMapping("/summary/period/{payrollPeriodId}")
    public ResponseEntity<SpringPayrollService.PayrollSummary> getPayrollSummary(@PathVariable Long payrollPeriodId) {
        try {
            SpringPayrollService.PayrollSummary summary = payrollService.getPayrollSummary(payrollPeriodId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}