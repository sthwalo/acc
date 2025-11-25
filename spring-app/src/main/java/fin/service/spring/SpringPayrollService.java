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
import fin.repository.FiscalPeriodRepository;
import fin.repository.PayslipRepository;
import fin.repository.CompanyRepository;
import fin.validation.EmployeeValidator;
import fin.validation.ValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring Service for payroll operations using unified FiscalPeriod model.
 * FiscalPeriod now handles both financial reporting AND payroll processing.
 */
@Service
public class SpringPayrollService {

    private static final Logger LOGGER = Logger.getLogger(SpringPayrollService.class.getName());

    private final EmployeeRepository employeeRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final PayslipRepository payslipRepository;
    private final CompanyRepository companyRepository;
    private final SpringPayslipPdfService payslipPdfService;
    private final SpringCompanyService companyService;
    private final SARSTaxCalculator sarsTaxCalculator;
    private final EmployeeValidator employeeValidator;

    public SpringPayrollService(EmployeeRepository employeeRepository,
                              FiscalPeriodRepository fiscalPeriodRepository,
                              PayslipRepository payslipRepository,
                              CompanyRepository companyRepository,
                              SpringPayslipPdfService payslipPdfService,
                              SpringCompanyService companyService,
                              SARSTaxCalculator sarsTaxCalculator,
                              EmployeeValidator employeeValidator) {
        this.employeeRepository = employeeRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.payslipRepository = payslipRepository;
        this.companyRepository = companyRepository;
        this.payslipPdfService = payslipPdfService;
        this.companyService = companyService;
        this.sarsTaxCalculator = sarsTaxCalculator;
        this.employeeValidator = employeeValidator;
    }

    /**
     * Create a new employee
     */
    @Transactional
    public Employee createEmployee(String employeeCode, String firstName, String lastName,
                                 String email, String phone, String bankName, String accountNumber,
                                 String branchCode, String taxNumber, LocalDate hireDate, BigDecimal salary,
                                 Long companyId) {
        // Create employee object from parameters
        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        employee.setFirstName(firstName);
        employee.setLastName(lastName);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setBankName(bankName);
        employee.setAccountNumber(accountNumber);
        employee.setBranchCode(branchCode);
        employee.setTaxNumber(taxNumber);
        employee.setHireDate(hireDate);
        employee.setBasicSalary(salary);
        employee.setCompanyId(companyId);
        employee.setActive(true);

        // Validate the employee using the validator
        ValidationResult validationResult = employeeValidator.validate(employee);
        if (!validationResult.isValid()) {
            // Collect all validation error messages
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ValidationResult.ValidationError error : validationResult.getErrors()) {
                errorMessage.append(error.getField()).append(": ").append(error.getMessage()).append("; ");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if employee code already exists for this company
        if (employeeRepository.existsByEmployeeNumberAndCompanyId(employeeCode.trim(), companyId)) {
            throw new IllegalArgumentException("Employee code already exists for this company: " + employeeCode);
        }

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
     * Get employees for a company with pagination and search
     */
    @Transactional(readOnly = true)
    public Page<Employee> getEmployeesByCompany(Long companyId, Pageable pageable, String search) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        if (search != null && !search.trim().isEmpty()) {
            return employeeRepository.findByCompanyIdAndSearch(companyId, search.trim(), pageable);
        } else {
            return employeeRepository.findByCompanyId(companyId, pageable);
        }
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
     * Get a payslip by ID
     */
    @Transactional(readOnly = true)
    public Optional<Payslip> getPayslipById(Long payslipId) {
        if (payslipId == null) {
            throw new IllegalArgumentException("Payslip ID is required");
        }
        return payslipRepository.findById(payslipId);
    }

    /**
     * Get a company by ID
     */
    @Transactional(readOnly = true)
    public Optional<Company> getCompanyById(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return companyRepository.findById(companyId);
    }

    /**
     * Get a fiscal period by ID
     */
    @Transactional(readOnly = true)
    public Optional<FiscalPeriod> getFiscalPeriodById(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }
        return fiscalPeriodRepository.findById(fiscalPeriodId);
    }

    /**
     * Update an employee with comprehensive validation and audit logging
     * Supports partial updates - only non-null parameters will be updated
     */
    @Transactional
    public Employee updateEmployee(Long id, String title, String firstName, String secondName, String lastName,
                                 String email, String phone, String position, String department, LocalDate hireDate,
                                 LocalDate terminationDate, Boolean isActive, String addressLine1, String addressLine2,
                                 String city, String province, String postalCode, String country, String bankName,
                                 String accountHolderName, String accountNumber, String branchCode, String accountType,
                                 String employmentType, String salaryType, BigDecimal basicSalary, BigDecimal overtimeRate,
                                 String taxNumber, String taxRebateCode, String uifNumber, String medicalAidNumber,
                                 String pensionFundNumber, String updatedBy) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        // Retrieve existing employee
        Optional<Employee> existingEmployeeOpt = employeeRepository.findById(id);
        if (existingEmployeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee existingEmployee = existingEmployeeOpt.get();

        // Apply partial updates (only non-null values)
        if (title != null) existingEmployee.setTitle(title);
        if (firstName != null) existingEmployee.setFirstName(firstName);
        if (secondName != null) existingEmployee.setSecondName(secondName);
        if (lastName != null) existingEmployee.setLastName(lastName);
        if (email != null) existingEmployee.setEmail(email);
        if (phone != null) existingEmployee.setPhone(phone);
        if (position != null) existingEmployee.setPosition(position);
        if (department != null) existingEmployee.setDepartment(department);
        if (hireDate != null) existingEmployee.setHireDate(hireDate);
        if (terminationDate != null) existingEmployee.setTerminationDate(terminationDate);
        if (isActive != null) existingEmployee.setActive(isActive);
        if (addressLine1 != null) existingEmployee.setAddressLine1(addressLine1);
        if (addressLine2 != null) existingEmployee.setAddressLine2(addressLine2);
        if (city != null) existingEmployee.setCity(city);
        if (province != null) existingEmployee.setProvince(province);
        if (postalCode != null) existingEmployee.setPostalCode(postalCode);
        if (country != null) existingEmployee.setCountry(country);
        if (bankName != null) existingEmployee.setBankName(bankName);
        if (accountHolderName != null) existingEmployee.setAccountHolderName(accountHolderName);
        if (accountNumber != null) existingEmployee.setAccountNumber(accountNumber);
        if (branchCode != null) existingEmployee.setBranchCode(branchCode);
        if (accountType != null) existingEmployee.setAccountType(accountType);
        if (employmentType != null) existingEmployee.setEmploymentType(Employee.EmploymentType.valueOf(employmentType));
        if (salaryType != null) existingEmployee.setSalaryType(Employee.SalaryType.valueOf(salaryType));
        if (basicSalary != null) existingEmployee.setBasicSalary(basicSalary);
        if (overtimeRate != null) existingEmployee.setOvertimeRate(overtimeRate);
        if (taxNumber != null) existingEmployee.setTaxNumber(taxNumber);
        if (taxRebateCode != null) existingEmployee.setTaxRebateCode(taxRebateCode);
        if (uifNumber != null) existingEmployee.setUifNumber(uifNumber);
        if (medicalAidNumber != null) existingEmployee.setMedicalAidNumber(medicalAidNumber);
        if (pensionFundNumber != null) existingEmployee.setPensionFundNumber(pensionFundNumber);

        // Validate the updated employee data
        ValidationResult validationResult = employeeValidator.validateForUpdate(existingEmployee);
        if (!validationResult.isValid()) {
            // Collect all validation error messages
            StringBuilder errorMessage = new StringBuilder("Validation failed: ");
            for (ValidationResult.ValidationError error : validationResult.getErrors()) {
                errorMessage.append(error.getField()).append(": ").append(error.getMessage()).append("; ");
            }
            throw new IllegalArgumentException(errorMessage.toString());
        }

        // Check data integrity constraints
        validateEmployeeUpdateIntegrity(existingEmployee);

        // Update audit fields
        existingEmployee.setUpdatedAt(java.time.LocalDateTime.now());
        if (updatedBy != null && !updatedBy.trim().isEmpty()) {
            existingEmployee.setUpdatedBy(updatedBy);
        }

        // Save and return updated employee
        Employee savedEmployee = employeeRepository.save(existingEmployee);
        LOGGER.info("Employee updated successfully: " + savedEmployee.getEmployeeNumber() +
                   " (" + savedEmployee.getFirstName() + " " + savedEmployee.getLastName() + ")");

        return savedEmployee;
    }

    /**
     * Validate data integrity constraints for employee updates
     */
    private void validateEmployeeUpdateIntegrity(Employee employee) {
        // Validate termination date logic
        if (employee.getTerminationDate() != null) {
            if (employee.getHireDate() != null && employee.getTerminationDate().isBefore(employee.getHireDate())) {
                throw new IllegalArgumentException("Termination date cannot be before hire date");
            }

            if (employee.getTerminationDate().isAfter(java.time.LocalDate.now())) {
                throw new IllegalArgumentException("Termination date cannot be in the future");
            }
        }

        // Validate salary
        if (employee.getBasicSalary() != null &&
            employee.getBasicSalary().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Basic salary must be greater than zero");
        }

        // Validate overtime rate
        if (employee.getOvertimeRate() != null) {
            if (employee.getOvertimeRate().compareTo(BigDecimal.ONE) < 0) {
                throw new IllegalArgumentException("Overtime rate cannot be less than 1.0");
            }

            if (employee.getOvertimeRate().compareTo(new BigDecimal("3.0")) > 0) {
                throw new IllegalArgumentException("Overtime rate seems unusually high. Please verify");
            }
        }
    }

    /**
     * Deactivate an employee
     * @param id the employee ID
     * @return the deactivated employee
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
     * Activate an employee
     * @param id the employee ID
     * @return the activated employee
     */
    @Transactional
    public Employee activateEmployee(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee employee = employeeOpt.get();
        employee.setActive(true);
        return employeeRepository.save(employee);
    }

    /**
     * Change employee active status
     * @param id the employee ID
     * @param active the new active status
     * @return the updated employee
     */
    @Transactional
    public Employee changeEmployeeStatus(Long id, boolean active) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee employee = employeeOpt.get();
        employee.setActive(active);
        return employeeRepository.save(employee);
    }

    /**
     * Hard delete an employee (permanently remove from database)
     * @param id the employee ID
     * @throws IllegalArgumentException if employee has payroll history or doesn't exist
     */
    @Transactional
    public void hardDeleteEmployee(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Employee ID is required");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(id);
        if (employeeOpt.isEmpty()) {
            throw new IllegalArgumentException("Employee not found: " + id);
        }

        Employee employee = employeeOpt.get();

        // Check if employee has any payslips (payroll history)
        List<Payslip> payslips = payslipRepository.findByEmployeeId(id);
        if (!payslips.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot hard delete employee " + employee.getEmployeeCode() + " " +
                employee.getFirstName() + " " + employee.getLastName() +
                ". Employee has " + payslips.size() + " payslip(s) in the system. " +
                "Use deactivate instead to preserve payroll history."
            );
        }

        // Check if employee has any payroll documents
        // Note: Document management is not fully implemented yet, but we should check anyway
        try {
            List<PayrollDocument> documents = listPayrollDocuments(id, null, null, 0, Integer.MAX_VALUE);
            if (!documents.isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot hard delete employee " + employee.getEmployeeCode() + " " +
                    employee.getFirstName() + " " + employee.getLastName() +
                    ". Employee has " + documents.size() + " document(s) in the system."
                );
            }
        } catch (Exception e) {
            // Document listing might not be implemented yet, continue with deletion
            LOGGER.warning("Could not check for employee documents during hard delete: " + e.getMessage());
        }

        // Safe to delete - no payroll history
        employeeRepository.deleteById(id);
        LOGGER.info("Hard deleted employee: " + employee.getEmployeeCode() + " " +
                   employee.getFirstName() + " " + employee.getLastName());
    }

    /**
     * Create a payroll period (now using unified FiscalPeriod)
     */
    @Transactional
    public FiscalPeriod createPayrollPeriod(Long companyId, String periodName,
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

        // Check if period name already exists for this company
        if (fiscalPeriodRepository.existsByCompanyIdAndPeriodName(companyId, periodName)) {
            throw new IllegalArgumentException("Period name already exists for this company: " + periodName);
        }

        FiscalPeriod period = new FiscalPeriod();
        period.setCompanyId(companyId);
        period.setPeriodName(periodName);
        period.setStartDate(startDate);
        period.setEndDate(endDate);
        period.setPayDate(paymentDate);
        period.setPeriodType(FiscalPeriod.PeriodType.MONTHLY);
        period.setPayrollStatus(FiscalPeriod.PayrollStatus.OPEN);
        period.setClosed(false);

        return fiscalPeriodRepository.save(period);
    }

    /**
     * Process payroll for a period
     * @param fiscalPeriodId the ID of the fiscal period to process payroll for
     * @return the result of the payroll processing
     */
    @Transactional
    public PayrollProcessingResult processPayroll(Long fiscalPeriodId) {
        return processPayroll(fiscalPeriodId, false);
    }

    /**
     * Reprocess payroll for a period (delete existing payslips and recalculate)
     * @param fiscalPeriodId the ID of the fiscal period to reprocess payroll for
     * @return the result of the payroll reprocessing
     */
    @Transactional
    public PayrollProcessingResult reprocessPayroll(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }

        LOGGER.info("Reprocessing payroll for fiscal period: " + fiscalPeriodId);
        return processPayroll(fiscalPeriodId, true);
    }

    /**
     * Process payroll for a period with optional reprocessing
     * @param fiscalPeriodId the ID of the fiscal period to process payroll for
     * @param reprocess if true, delete existing payslips and reprocess
     * @return the result of the payroll processing
     */
    @Transactional
    public PayrollProcessingResult processPayroll(Long fiscalPeriodId, boolean reprocess) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }

        LOGGER.info("Processing payroll for fiscal period: " + fiscalPeriodId + (reprocess ? " (reprocessing)" : ""));

        // Get and validate fiscal period
        FiscalPeriod period = getAndValidateFiscalPeriod(fiscalPeriodId, reprocess);

        // If reprocessing, clean up existing payslips and reset totals
        if (reprocess) {
            cleanupExistingPayslips(fiscalPeriodId);
            resetPeriodTotals(period);
        }

        // Get active employees for the company
        List<Employee> employees = getActiveEmployeesByCompany(period.getCompanyId());
        LOGGER.info("Found " + employees.size() + " active employees for company " + period.getCompanyId());

        if (employees.isEmpty()) {
            LOGGER.warning("No active employees found for company " + period.getCompanyId());
            return new PayrollProcessingResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        // Process all employees and calculate totals
        PayrollProcessingResult result = processEmployeesAndCalculateTotals(employees, period);

        // Update period with results and mark as processed
        updatePeriodWithPayrollResults(period, result);

        LOGGER.info("Payroll processing completed successfully for period: " + fiscalPeriodId);

        return result;
    }

    /**
     * Get and validate fiscal period for payroll processing
     */
    private FiscalPeriod getAndValidateFiscalPeriod(Long fiscalPeriodId) {
        return getAndValidateFiscalPeriod(fiscalPeriodId, false);
    }

    /**
     * Get and validate fiscal period for payroll processing with reprocessing support
     */
    private FiscalPeriod getAndValidateFiscalPeriod(Long fiscalPeriodId, boolean reprocess) {
        Optional<FiscalPeriod> periodOpt = fiscalPeriodRepository.findById(fiscalPeriodId);
        if (periodOpt.isEmpty()) {
            LOGGER.severe("Fiscal period not found: " + fiscalPeriodId);
            throw new IllegalArgumentException("Fiscal period not found: " + fiscalPeriodId);
        }

        FiscalPeriod period = periodOpt.get();
        LOGGER.info("Found fiscal period: " + period.getPeriodName() + ", status: " + period.getPayrollStatus() + ", closed: " + period.isClosed());

        // For reprocessing, check if period can be reprocessed
        if (reprocess && !period.canBeReprocessed()) {
            LOGGER.severe("Fiscal period cannot be reprocessed. Status: " + period.getPayrollStatus() + ", closed: " + period.isClosed());
            throw new IllegalArgumentException("Fiscal period cannot be reprocessed. Status: " + period.getPayrollStatus());
        }

        // For initial processing, check if period can be processed
        if (!reprocess && !period.canBeProcessed()) {
            LOGGER.severe("Fiscal period cannot be processed. Status: " + period.getPayrollStatus() + ", closed: " + period.isClosed());
            throw new IllegalArgumentException("Fiscal period cannot be processed. Status: " + period.getPayrollStatus());
        }

        return period;
    }

    /**
     * Clean up existing payslips for reprocessing
     */
    private void cleanupExistingPayslips(Long fiscalPeriodId) {
        List<Payslip> existingPayslips = payslipRepository.findByFiscalPeriodId(fiscalPeriodId);
        if (!existingPayslips.isEmpty()) {
            LOGGER.info("Deleting " + existingPayslips.size() + " existing payslips for reprocessing");
            for (Payslip payslip : existingPayslips) {
                payslipRepository.delete(payslip);
            }
        }
    }

    /**
     * Reset period totals for reprocessing
     */
    private void resetPeriodTotals(FiscalPeriod period) {
        LOGGER.info("Resetting period totals for reprocessing: " + period.getPeriodName());
        period.setTotalGrossPay(BigDecimal.ZERO);
        period.setTotalDeductions(BigDecimal.ZERO);
        period.setTotalNetPay(BigDecimal.ZERO);
        period.setEmployeeCount(0);
        period.setProcessedAt(null);
        fiscalPeriodRepository.save(period);
    }

    /**
     * Process all employees and calculate payroll totals
     */
    private PayrollProcessingResult processEmployeesAndCalculateTotals(List<Employee> employees, FiscalPeriod period) {
        // First pass: calculate total company payroll for SDL calculation
        BigDecimal totalCompanyPayroll = BigDecimal.ZERO;
        for (Employee employee : employees) {
            totalCompanyPayroll = totalCompanyPayroll.add(employee.getBasicSalary());
        }

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        int processedCount = 0;

        // Second pass: process each employee with total payroll for accurate SDL calculation
        for (Employee employee : employees) {
            LOGGER.info("Processing employee: " + employee.getEmployeeCode() + " - " + employee.getFirstName() + " " + employee.getLastName());
            Payslip payslip = calculatePayslip(employee, period, totalCompanyPayroll);
            payslipRepository.save(payslip);

            totalGross = totalGross.add(payslip.getGrossSalary());
            totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
            totalNet = totalNet.add(payslip.getNetSalary());
            processedCount++;
        }

        LOGGER.info("Processed " + processedCount + " employees. Total gross: " + totalGross + ", total deductions: " + totalDeductions + ", total net: " + totalNet);

        return new PayrollProcessingResult(totalGross, totalDeductions, totalNet, processedCount);
    }

    /**
     * Update fiscal period with payroll processing results
     */
    private void updatePeriodWithPayrollResults(FiscalPeriod period, PayrollProcessingResult result) {
        period.setTotalGrossPay(result.getTotalGross());
        period.setTotalDeductions(result.getTotalDeductions());
        period.setTotalNetPay(result.getTotalNet());
        period.setEmployeeCount(result.getEmployeeCount());
        period.setPayrollStatus(FiscalPeriod.PayrollStatus.PROCESSED);
        period.setProcessedAt(java.time.LocalDateTime.now());
        fiscalPeriodRepository.save(period);
    }

    /**
     * Calculate payslip for an employee
     * @param employee the employee to calculate payslip for
     * @param period the fiscal period
     * @param totalCompanyPayroll the total payroll for the company (for SDL calculation)
     * @return the calculated payslip
     */
    private Payslip calculatePayslip(Employee employee, FiscalPeriod period, BigDecimal totalCompanyPayroll) {
        // Calculate gross salary (assuming monthly salary)
        BigDecimal grossSalary = employee.getBasicSalary();
        double grossDouble = grossSalary.doubleValue();
        double totalPayrollDouble = totalCompanyPayroll.doubleValue();

        // Calculate PAYE using SARS tax calculator
        double payeDouble = sarsTaxCalculator.findPAYE(grossDouble);
        BigDecimal paye = BigDecimal.valueOf(payeDouble).setScale(2, RoundingMode.HALF_UP);

        // Calculate UIF using SARS tax calculator
        double uifDouble = sarsTaxCalculator.calculateUIF(grossDouble);
        BigDecimal uif = BigDecimal.valueOf(uifDouble).setScale(2, RoundingMode.HALF_UP);

        // Calculate SDL (employer's contribution) using SARS tax calculator
        double sdlDouble = sarsTaxCalculator.calculateSDL(grossDouble, totalPayrollDouble);
        BigDecimal sdl = BigDecimal.valueOf(sdlDouble).setScale(2, RoundingMode.HALF_UP);

        // Calculate total deductions
        BigDecimal totalDeductions = paye.add(uif);

        // Calculate net salary
        BigDecimal netSalary = grossSalary.subtract(totalDeductions);

        // Create payslip
        Payslip payslip = new Payslip();
        payslip.setCompanyId(employee.getCompanyId());
        payslip.setEmployeeId(employee.getId());
        payslip.setFiscalPeriodId(period.getId()); // FiscalPeriod ID is now the payroll period ID
        payslip.setPayslipNumber("PSL-" + period.getId() + "-" + employee.getId());
        payslip.setBasicSalary(employee.getBasicSalary());
        payslip.setGrossSalary(grossSalary);
        payslip.setTotalEarnings(grossSalary); // For now, total earnings = gross salary
        payslip.setPaye(paye);
        payslip.setUif(uif);
        payslip.setSdl(sdl);
        payslip.setTotalDeductions(totalDeductions);
        payslip.setNetSalary(netSalary);
        payslip.setPaymentDate(period.getPayDate());

        return payslip;
    }

    /**
     * Get payslips for a fiscal period
     */
    @Transactional(readOnly = true)
    public List<Payslip> getPayslipsByPeriod(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }
        return payslipRepository.findByFiscalPeriodId(fiscalPeriodId);
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
     * Get payroll periods for a company (now returns FiscalPeriod list)
     * @param companyId the company ID
     * @return list of fiscal periods
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriod> getPayrollPeriodsByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return fiscalPeriodRepository.findByCompanyId(companyId);
    }

    /**
     * Process payroll for a period with optional email sending
     */
    @Transactional
    public void processPayroll(Long fiscalPeriodId, String processedBy, boolean sendEmails) {
        // For now, just call the basic processPayroll method
        // Email functionality can be added later
        processPayroll(fiscalPeriodId);
    }

    /**
     * Process payroll for a period (without email)
     */
    @Transactional
    public void processPayroll(Long fiscalPeriodId, String processedBy) {
        processPayroll(fiscalPeriodId, processedBy, false);
    }

    /**
     * Get payroll periods for a company
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriod> getPayrollPeriods(Long companyId) {
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
     * Create a payroll period from FiscalPeriod object
     */
    @Transactional
    public FiscalPeriod createPayrollPeriod(FiscalPeriod period) {
        if (period == null) {
            throw new IllegalArgumentException("Fiscal period is required");
        }
        return createPayrollPeriod(period.getCompanyId(), period.getPeriodName(),
                                 period.getStartDate(), period.getEndDate(),
                                 period.getPayDate());
    }

    /**
     * Delete a payroll period
     */
    @Transactional
    public void deletePayrollPeriod(Long periodId, Long companyId) {
        if (periodId == null || companyId == null) {
            throw new IllegalArgumentException("Period ID and Company ID are required");
        }

        Optional<FiscalPeriod> periodOpt = fiscalPeriodRepository.findById(periodId);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Fiscal period not found: " + periodId);
        }

        FiscalPeriod period = periodOpt.get();
        if (!period.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Fiscal period does not belong to company: " + companyId);
        }

        if (period.isPayrollProcessed()) {
            throw new IllegalArgumentException("Cannot delete processed payroll period");
        }

        fiscalPeriodRepository.deleteById(periodId);
    }

    /**
     * Force delete a payroll period (dangerous - deletes processed periods)
     */
    @Transactional
    public void forceDeletePayrollPeriod(Long periodId, Long companyId) {
        if (periodId == null || companyId == null) {
            throw new IllegalArgumentException("Period ID and Company ID are required");
        }

        Optional<FiscalPeriod> periodOpt = fiscalPeriodRepository.findById(periodId);
        if (periodOpt.isEmpty()) {
            throw new IllegalArgumentException("Fiscal period not found: " + periodId);
        }

        FiscalPeriod period = periodOpt.get();
        if (!period.getCompanyId().equals(companyId)) {
            throw new IllegalArgumentException("Fiscal period does not belong to company: " + companyId);
        }

        // Delete associated payslips first
        List<Payslip> payslips = payslipRepository.findByFiscalPeriodId(periodId);
        for (Payslip payslip : payslips) {
            payslipRepository.delete(payslip);
        }

        // Delete the period
        fiscalPeriodRepository.deleteById(periodId);
        LOGGER.warning("Force deleted fiscal period: " + period.getPeriodName());
    }

    /**
     * Force delete all payroll periods for a company (dangerous operation)
     */
    @Transactional
    public void forceDeleteAllPayrollPeriods(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);
        for (FiscalPeriod period : periods) {
            forceDeletePayrollPeriod(period.getId(), companyId);
        }
        LOGGER.warning("Force deleted all fiscal periods for company: " + companyId);
    }

    /**
     * Generate payslip PDFs for a fiscal period (optionally for specific employees)
     */
    @Transactional(readOnly = true)
    public List<byte[]> generatePayslips(Long fiscalPeriodId, List<Long> employeeIds) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }

        List<Payslip> payslips;
        if (employeeIds != null && !employeeIds.isEmpty()) {
            // Generate for specific employees
            payslips = payslipRepository.findByFiscalPeriodIdAndEmployeeIdIn(fiscalPeriodId, employeeIds);
        } else {
            // Generate for all employees in the period
            payslips = getPayslipsByPeriod(fiscalPeriodId);
        }

        List<byte[]> pdfs = new ArrayList<>();
        for (Payslip payslip : payslips) {
            try {
                // Get related data from database
                Employee employee = employeeRepository.findById(payslip.getEmployeeId())
                    .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + payslip.getEmployeeId()));

                Company company = companyRepository.findById(employee.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found: " + employee.getCompanyId()));

                FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
                    .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + fiscalPeriodId));

                // Generate PDF using SpringPayslipPdfService
                byte[] pdfBytes = payslipPdfService.generatePayslipPdf(payslip, employee, company, fiscalPeriod);
                pdfs.add(pdfBytes);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to generate PDF for payslip: " + payslip.getId(), e);
                // Continue with other payslips
            }
        }

        LOGGER.info("Generated " + pdfs.size() + " payslip PDFs for period: " + fiscalPeriodId);
        return pdfs;
    }

    /**
     * Generate EMP 201 SARS tax submission report
     */
    @Transactional(readOnly = true)
    public String generateEmp201Report(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }

        List<Payslip> payslips = getPayslipsByPeriod(fiscalPeriodId);
        if (payslips.isEmpty()) {
            throw new IllegalArgumentException("No payslips found for fiscal period: " + fiscalPeriodId);
        }

        // TODO: Implement proper EMP201 format
        // For now, return a simple text report
        StringBuilder report = new StringBuilder();
        report.append("EMP201 SARS Tax Submission Report\n");
        report.append("==================================\n\n");

        BigDecimal totalPAYE = BigDecimal.ZERO;
        BigDecimal totalUIF = BigDecimal.ZERO;
        BigDecimal totalSDL = BigDecimal.ZERO;

        for (Payslip payslip : payslips) {
            totalPAYE = totalPAYE.add(payslip.getPaye());
            totalUIF = totalUIF.add(payslip.getUif());
            totalSDL = totalSDL.add(payslip.getSdl());
        }

        report.append("Total PAYE: ").append(totalPAYE).append("\n");
        report.append("Total UIF: ").append(totalUIF).append("\n");
        report.append("Total SDL: ").append(totalSDL).append("\n");

        return report.toString();
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
    public PayrollSummary getPayrollSummary(Long fiscalPeriodId) {
        List<Payslip> payslips = getPayslipsByPeriod(fiscalPeriodId);

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
     * Upload payroll document
     */
    @Transactional
    public void uploadPayrollDocument(Long employeeId, Long periodId, String fileName, byte[] fileData, String documentType) {
        // TODO: Implement document storage (database or file system)
        LOGGER.info("Document upload not yet implemented: " + fileName + " for employee " + employeeId);
    }

    /**
     * List payroll documents
     */
    @Transactional(readOnly = true)
    public List<PayrollDocument> listPayrollDocuments(Long employeeId, Long periodId, String type, int page, int size) {
        // TODO: Implement document listing
        LOGGER.info("Document listing not yet implemented");
        return new java.util.ArrayList<>();
    }

    /**
     * Get payroll document
     */
    @Transactional(readOnly = true)
    public PayrollDocument getPayrollDocument(Long id) {
        // TODO: Implement document retrieval
        throw new IllegalArgumentException("Document retrieval not yet implemented");
    }

    /**
     * Delete payroll document
     */
    @Transactional
    public void deletePayrollDocument(Long id) {
        // TODO: Implement document deletion
        LOGGER.info("Document deletion not yet implemented for ID: " + id);
    }

    // ===== INNER CLASSES =====

    /**
     * Simple payroll document class
     */
    public static class PayrollDocument {
        private Long id;
        private Long employeeId;
        private Long periodId;
        private String fileName;
        private byte[] fileData;
        private String documentType;
        private LocalDate uploadDate;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

        public Long getPeriodId() { return periodId; }
        public void setPeriodId(Long periodId) { this.periodId = periodId; }

        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }

        public byte[] getFileData() { return fileData; }
        public void setFileData(byte[] fileData) { this.fileData = fileData; }

        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }

        public LocalDate getUploadDate() { return uploadDate; }
        public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }
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