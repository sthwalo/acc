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

package fin.service.spring;

import fin.model.*;
import fin.repository.EmployeeRepository;
import fin.repository.PayrollPeriodRepository;
import fin.repository.PayslipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Spring Service for payroll operations using JPA repositories.
 * Handles employee management, payroll calculations, and payslip generation.
 */
@Service
@Transactional
public class SpringPayrollService {

    private static final Logger LOGGER = Logger.getLogger(SpringPayrollService.class.getName());

    private final EmployeeRepository employeeRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayslipRepository payslipRepository;
    private final SpringCompanyService companyService;

    // Tax rates (simplified - in production these would come from SARS tables)
    private static final BigDecimal PAYE_RATE = new BigDecimal("0.18"); // 18% PAYE
    private static final BigDecimal UIF_RATE = new BigDecimal("0.01"); // 1% UIF
    private static final BigDecimal SDL_RATE = new BigDecimal("0.01"); // 1% SDL

    public SpringPayrollService(EmployeeRepository employeeRepository,
                              PayrollPeriodRepository payrollPeriodRepository,
                              PayslipRepository payslipRepository,
                              SpringCompanyService companyService) {
        this.employeeRepository = employeeRepository;
        this.payrollPeriodRepository = payrollPeriodRepository;
        this.payslipRepository = payslipRepository;
        this.companyService = companyService;
    }

    /**
     * Create a new employee
     */
    @Transactional
    public Employee createEmployee(String employeeCode, String firstName, String lastName,
                                 Long companyId, BigDecimal basicSalary, LocalDate dateOfBirth,
                                 LocalDate dateEngaged, String idNumber, String email) {
        // Validate inputs
        if (employeeCode == null || employeeCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Employee code is required");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        if (basicSalary == null || basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valid basic salary is required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if employee code already exists for this company
        if (employeeRepository.existsByCompanyIdAndEmployeeNumber(companyId, employeeCode.trim())) {
            throw new IllegalArgumentException("Employee code already exists for this company: " + employeeCode);
        }

        // Create employee
        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode.trim());
        employee.setFirstName(firstName.trim());
        employee.setLastName(lastName.trim());
        employee.setCompanyId(companyId);
        employee.setBasicSalary(basicSalary);
        employee.setDateOfBirth(dateOfBirth);
        employee.setDateEngaged(dateEngaged);
        employee.setIdNumber(idNumber);
        employee.setEmail(email);
        employee.setActive(true);

        return employeeRepository.save(employee);
    }

    /**
     * Get all employees for a company
     */
    @Transactional(readOnly = true)
    public List<Employee> getEmployeesByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return employeeRepository.findByCompanyId(companyId);
    }

    /**
     * Get active employees for a company
     */
    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployeesByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return employeeRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    /**
     * Get employee by ID
     */
    @Transactional(readOnly = true)
    public Optional<Employee> getEmployeeById(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        return employeeRepository.findById(employeeId);
    }

    /**
     * Update an employee
     */
    @Transactional
    public Employee updateEmployee(Long id, Employee updatedEmployee) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        if (updatedEmployee == null) {
            throw new IllegalArgumentException("Updated employee data is required");
        }

        Optional<Employee> existingEmployeeOpt = employeeRepository.findById(id);
        if (existingEmployeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee existingEmployee = existingEmployeeOpt.get();

        // Update fields
        existingEmployee.setFirstName(updatedEmployee.getFirstName());
        existingEmployee.setLastName(updatedEmployee.getLastName());
        existingEmployee.setBasicSalary(updatedEmployee.getBasicSalary());
        existingEmployee.setEmail(updatedEmployee.getEmail());
        existingEmployee.setActive(updatedEmployee.isActive());

        return employeeRepository.save(existingEmployee);
    }

    /**
     * Deactivate an employee
     */
    @Transactional
    public Employee deactivateEmployee(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee employee = employeeOpt.get();
        employee.setActive(false);
        return employeeRepository.save(employee);
    }

    /**
     * Create a payroll period
     */
    @Transactional
    public PayrollPeriod createPayrollPeriod(Long companyId, String periodName,
                                           LocalDate startDate, LocalDate endDate,
                                           LocalDate paymentDate) {
        if (companyId == null || periodName == null || startDate == null ||
            endDate == null || paymentDate == null) {
            throw new IllegalArgumentException("All payroll period fields are required");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        PayrollPeriod period = new PayrollPeriod();
        period.setCompanyId(companyId);
        period.setPeriodName(periodName);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setPaymentDate(paymentDate);
        period.setProcessed(false);

        return payrollPeriodRepository.save(period);
    }

    /**
     * Process payroll for a period
     */
    @Transactional
    public PayrollProcessingResult processPayroll(Long payrollPeriodId) {
        if (payrollPeriodId == null) {
            throw new IllegalArgumentException("Payroll period ID is required");
        }

        // Get payroll period
        Optional<PayrollPeriod> periodOpt = payrollPeriodRepository.findById(payrollPeriodId);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Payroll period not found: " + payrollPeriodId);
        }

        PayrollPeriod period = periodOpt.get();
        if (period.isProcessed()) {
            throw new IllegalArgumentException("Payroll period has already been processed");
        }

        // Get active employees for the company
        List<Employee> employees = getActiveEmployeesByCompany(period.getCompanyId());

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        int processedCount = 0;

        // Process each employee
        for (Employee employee : employees) {
            Payslip payslip = calculatePayslip(employee, period);
            payslipRepository.save(payslip);

            totalGross = totalGross.add(payslip.getGrossSalary());
            totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
            totalNet = totalNet.add(payslip.getNetSalary());
            processedCount++;
        }

        // Mark period as processed
        period.setProcessed(true);
        payrollPeriodRepository.save(period);

        return new PayrollProcessingResult(totalGross, totalDeductions, totalNet, processedCount);
    }

    /**
     * Calculate payslip for an employee
     */
    private Payslip calculatePayslip(Employee employee, PayrollPeriod period) {
        // Calculate gross salary (assuming monthly salary)
        BigDecimal grossSalary = employee.getBasicSalary();

        // Calculate PAYE (simplified)
        BigDecimal paye = grossSalary.multiply(PAYE_RATE).setScale(2, RoundingMode.HALF_UP);

        // Calculate UIF
        BigDecimal uif = grossSalary.multiply(UIF_RATE).setScale(2, RoundingMode.HALF_UP);

        // Calculate SDL (employer's contribution)
        BigDecimal sdl = grossSalary.multiply(SDL_RATE).setScale(2, RoundingMode.HALF_UP);

        // Calculate total deductions
        BigDecimal totalDeductions = paye.add(uif);

        // Calculate net salary
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        // Create payslip
        Payslip payslip = new Payslip();
        payslip.setEmployeeId(employee.getId());
        payslip.setPayrollPeriodId(period.getId());
        payslip.setGrossSalary(grossSalary);
        payslip.setPaye(paye);
        payslip.setUif(uif);
        payslip.setSdl(sdl);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(netSalary);
        payslip.setPaymentDate(period.getPaymentDate());

        return payslip;
    }

    /**
     * Get payslips for a payroll period
     */
    @Transactional(readOnly = true)
    public List<Payslip> getPayslipsByPeriod(Long payrollPeriodId) {
        if (payrollPeriodId == null) {
            throw new IllegalArgumentException("Payroll period ID is required");
        }
        return payslipRepository.findByPayrollPeriodId(payrollPeriodId);
    }

    /**
     * Get payslips for an employee
     */
    @Transactional(readOnly = true)
    public List<Payslip> getPayslipsByEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }
        return payslipRepository.findByEmployeeId(employeeId);
    }

    /**
     * Get payroll periods for a company
     */
    @Transactional(readOnly = true)
    public List<PayrollPeriod> getPayrollPeriodsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return payrollPeriodRepository.findByCompanyId(companyId.intValue());
    }

    /**
     * Process payroll for a period with optional email sending
     */
    @Transactional
    public void processPayroll(Long payrollPeriodId, String processedBy, boolean sendEmails) {
        // For now, just call the basic processPayroll method
        // Email functionality can be added later
        processPayroll(payrollPeriodId);
    }

    /**
     * Process payroll for a period (without email)
     */
    @Transactional
    public void processPayroll(Long payrollPeriodId, String processedBy) {
        processPayroll(payrollPeriodId, processedBy, false);
    }

    /**
     * Get payroll periods for a company
     */
    @Transactional(readOnly = true)
    public List<PayrollPeriod> getPayrollPeriods(Long companyId) {
        return getPayrollPeriodsByCompany(companyId);
    }

    /**
     * Get active employees for a company
     */
    @Transactional(readOnly = true)
    public List<Employee> getActiveEmployees(Long companyId) {
        return getActiveEmployeesByCompany(companyId);
    }

    /**
     * Create a payroll period from PayrollPeriod object
     */
    @Transactional
    public PayrollPeriod createPayrollPeriod(PayrollPeriod period) {
        if (period == null) {
            throw new IllegalArgumentException("Payroll period is required");
        }
        return createPayrollPeriod(period.getCompanyId(), period.getPeriodName(),
                                 period.getStartDate(), period.getEndDate(),
                                 period.getPaymentDate());
    }

    /**
     * Delete a payroll period
     */
    @Transactional
    public void deletePayrollPeriod(Long periodId, Long companyId) {
        if (periodId == null || companyId == null) {
            throw new IllegalArgumentException("Period ID and Company ID are required");
        }

        Optional<PayrollPeriod> periodOpt = payrollPeriodRepository.findById(periodId);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Payroll period not found: " + periodId);
        }

        PayrollPeriod period = periodOpt.get();
        if (!period.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Payroll period does not belong to company: " + companyId);
        }

        if (period.isProcessed()) {
            throw new IllegalArgumentException("Cannot delete processed payroll period");
        }

        payrollPeriodRepository.deleteById(periodId);
    }

    /**
     * Force delete a payroll period (dangerous - deletes processed periods)
     */
    @Transactional
    public void forceDeletePayrollPeriod(Long periodId, Long companyId) {
        if (periodId == null || companyId == null) {
            throw new IllegalArgumentException("Period ID and Company ID are required");
        }

        Optional<PayrollPeriod> periodOpt = payrollPeriodRepository.findById(periodId);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Payroll period not found: " + periodId);
        }

        PayrollPeriod period = periodOpt.get();
        if (!period.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Payroll period does not belong to company: " + companyId);
        }

        // Delete associated payslips first
        List<Payslip> payslips = payslipRepository.findByPayrollPeriodId(periodId);
        for (Payslip payslip : payslips) {
            payslipRepository.delete(payslip);
        }

        // Delete the period
        payrollPeriodRepository.deleteById(periodId);
        LOGGER.warning("Force deleted payroll period: " + period.getPeriodName());
    }

    /**
     * Force delete all payroll periods for a specific month and year
     */
    @Transactional
    public void forceDeleteAllPayrollPeriodsForMonth(Long companyId, int year, int month) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        List<PayrollPeriod> periods = payrollPeriodRepository.findByCompanyId(companyId.intValue());
        int deletedCount = 0;

        for (PayrollPeriod period : periods) {
            LocalDate paymentDate = period.getPaymentDate();
            if (paymentDate.getYear() == year && paymentDate.getMonthValue() == month) {
                forceDeletePayrollPeriod(period.getId(), companyId);
                deletedCount++;
            }
        }

        LOGGER.info("Force deleted " + deletedCount + " payroll periods for " + year + "-" + month);
    }

    /**
     * Generate payslip PDFs for a payroll period
     */
    @Transactional(readOnly = true)
    public void generatePayslipPdfs(Long payrollPeriodId) {
        // PDF generation not implemented yet - placeholder
        LOGGER.info("PDF generation not yet implemented for payroll period: " + payrollPeriodId);
    }

    /**
     * Email payslips to employees
     */
    @Transactional(readOnly = true)
    public Object emailPayslipsToEmployees(Long payrollPeriodId) {
        // Email functionality not implemented yet - placeholder
        LOGGER.info("Email functionality not yet implemented for payroll period: " + payrollPeriodId);
        return null; // Return type to be determined
    }

    /**
     * Import employees from file
     */
    @Transactional
    public int importEmployeesFromFile(String filePath, Long companyId) {
        // File import not implemented yet - placeholder
        LOGGER.info("Employee import from file not yet implemented: " + filePath);
        return 0;
    }

    /**
     * Clean up all payslips for testing
     */
    @Transactional
    public void cleanupAllPayslipsForTesting() {
        List<Payslip> allPayslips = payslipRepository.findAll();
        for (Payslip payslip : allPayslips) {
            payslipRepository.delete(payslip);
        }
        LOGGER.warning("Cleaned up all payslips for testing");
    }

    /**
     * Calculate total payroll cost for a period
     */
    @Transactional(readOnly = true)
    public PayrollSummary getPayrollSummary(Long payrollPeriodId) {
        List<Payslip> payslips = getPayslipsByPeriod(payrollPeriodId);

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalPAYE = BigDecimal.ZERO;
        BigDecimal totalUIF = BigDecimal.ZERO;
        BigDecimal totalSDL = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        for (Payslip payslip : payslips) {
            totalGross = totalGross.add(payslip.getGrossSalary());
            totalPAYE = totalPAYE.add(payslip.getPaye());
            totalUIF = totalUIF.add(payslip.getUif());
            totalSDL = totalSDL.add(payslip.getSdl());
            totalNet = totalNet.add(payslip.getNetSalary());
        }

        return new PayrollSummary(totalGross, totalPAYE, totalUIF, totalSDL, totalNet, payslips.size());
    }
    public static class PayrollProcessingResult {
        private final BigDecimal totalGross;
        private final BigDecimal totalDeductions;
        private final BigDecimal totalNet;
        private final int employeeCount;

        public PayrollProcessingResult(BigDecimal totalGross, BigDecimal totalDeductions,
                                    BigDecimal totalNet, int employeeCount) {
            this.totalGross = totalGross;
            this.totalDeductions = totalDeductions;
            this.totalNet = totalNet;
            this.employeeCount = employeeCount;
        }

        public BigDecimal getTotalGross() { return totalGross; }
        public BigDecimal getTotalDeductions() { return totalDeductions; }
        public BigDecimal getTotalNet() { return totalNet; }
        public int getEmployeeCount() { return employeeCount; }
    }

    /**
     * Inner class for payroll summary
     */
    public static class PayrollSummary {
        private final BigDecimal totalGross;
        private final BigDecimal totalPAYE;
        private final BigDecimal totalUIF;
        private final BigDecimal totalSDL;
        private final BigDecimal totalNet;
        private final int employeeCount;

        public PayrollSummary(BigDecimal totalGross, BigDecimal totalPAYE, BigDecimal totalUIF,
                           BigDecimal totalSDL, BigDecimal totalNet, int employeeCount) {
            this.totalGross = totalGross;
            this.totalPAYE = totalPAYE;
            this.totalUIF = totalUIF;
            this.totalSDL = totalSDL;
            this.totalNet = totalNet;
            this.employeeCount = employeeCount;
        }

        public BigDecimal getTotalGross() { return totalGross; }
        public BigDecimal getTotalPAYE() { return totalPAYE; }
        public BigDecimal getTotalUIF() { return totalUIF; }
        public BigDecimal getTotalSDL() { return totalSDL; }
        public BigDecimal getTotalNet() { return totalNet; }
        public int getEmployeeCount() { return employeeCount; }
    }
}