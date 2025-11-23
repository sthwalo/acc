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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    private final SpringCompanyService companyService;

    // Tax rates (simplified - in production these would come from SARS tables)
    private static final BigDecimal PAYE_RATE = new BigDecimal("0.18"); // 18% PAYE
    private static final BigDecimal UIF_RATE = new BigDecimal("0.01"); // 1% UIF
    private static final BigDecimal SDL_RATE = new BigDecimal("0.01"); // 1% SDL

    public SpringPayrollService(EmployeeRepository employeeRepository,
                              FiscalPeriodRepository fiscalPeriodRepository,
                              PayslipRepository payslipRepository,
                              SpringCompanyService companyService) {
        System.out.println("=== DEBUG: SpringPayrollService constructor called ===");
        this.employeeRepository = employeeRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.payslipRepository = payslipRepository;
        this.companyService = companyService;
        System.out.println("DEBUG: SpringPayrollService initialized successfully");
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
        if (employeeRepository.existsByEmployeeNumberAndCompanyId(employeeCode.trim(), companyId)) {
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
     */
    @Transactional
    public PayrollProcessingResult processPayroll(Long fiscalPeriodId) {
        if (fiscalPeriodId == null) {
            throw new IllegalArgumentException("Fiscal period ID is required");
        }

        LOGGER.info("Processing payroll for fiscal period: " + fiscalPeriodId);

        // Get fiscal period
        Optional<FiscalPeriod> periodOpt = fiscalPeriodRepository.findById(fiscalPeriodId);
        if (periodOpt.isEmpty()) {
            LOGGER.severe("Fiscal period not found: " + fiscalPeriodId);
            throw new IllegalArgumentException("Fiscal period not found: " + fiscalPeriodId);
        }

        FiscalPeriod period = periodOpt.get();
        LOGGER.info("Found fiscal period: " + period.getPeriodName() + ", status: " + period.getPayrollStatus() + ", closed: " + period.isClosed());

        if (!period.canBeProcessed()) {
            LOGGER.severe("Fiscal period cannot be processed. Status: " + period.getPayrollStatus() + ", closed: " + period.isClosed());
            throw new IllegalArgumentException("Fiscal period cannot be processed. Status: " + period.getPayrollStatus());
        }

        // Get active employees for the company
        List<Employee> employees = getActiveEmployeesByCompany(period.getCompanyId());
        LOGGER.info("Found " + employees.size() + " active employees for company " + period.getCompanyId());

        if (employees.isEmpty()) {
            LOGGER.warning("No active employees found for company " + period.getCompanyId());
            return new PayrollProcessingResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        int processedCount = 0;

        // Process each employee
        for (Employee employee : employees) {
            LOGGER.info("Processing employee: " + employee.getEmployeeCode() + " - " + employee.getFirstName() + " " + employee.getLastName());
            Payslip payslip = calculatePayslip(employee, period);
            payslipRepository.save(payslip);

            totalGross = totalGross.add(payslip.getGrossSalary());
            totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
            totalNet = totalNet.add(payslip.getNetSalary());
            processedCount++;
        }

        LOGGER.info("Processed " + processedCount + " employees. Total gross: " + totalGross + ", total deductions: " + totalDeductions + ", total net: " + totalNet);

        // Update period totals and mark as processed
        period.setTotalGrossPay(totalGross);
        period.setTotalDeductions(totalDeductions);
        period.setTotalNetPay(totalNet);
        period.setEmployeeCount(processedCount);
        period.setPayrollStatus(FiscalPeriod.PayrollStatus.PROCESSED);
        period.setProcessedAt(java.time.LocalDateTime.now());
        fiscalPeriodRepository.save(period);

        LOGGER.info("Payroll processing completed successfully for period: " + fiscalPeriodId);

        return new PayrollProcessingResult(totalGross, totalDeductions, totalNet, processedCount);
    }

    /**
     * Calculate payslip for an employee
     */
    private Payslip calculatePayslip(Employee employee, FiscalPeriod period) {
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
    public void generatePayslips(Long fiscalPeriodId, List<Long> employeeIds) {
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

        // TODO: Implement PDF generation using SpringPayslipPdfService
        // For now, just log
        LOGGER.info("Generated " + payslips.size() + " payslip PDFs for period: " + fiscalPeriodId);
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