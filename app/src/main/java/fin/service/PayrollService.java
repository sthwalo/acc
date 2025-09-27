/*
 * FIN Financial Management System - Payroll Module
 * 
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 */

package fin.service;

import fin.model.*;
import fin.config.DatabaseConfig;
import fin.repository.CompanyRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Comprehensive Payroll Service for FIN Financial Management System
 * Handles all payroll operations including tax calculations, UIF, and integration with accounting
 */
public class PayrollService {
    private static final Logger LOGGER = Logger.getLogger(PayrollService.class.getName());
    private final String dbUrl;
    private final CompanyRepository companyRepository;
    private final PayslipPdfService pdfService;
    private final EmailService emailService;
    
    public PayrollService() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.companyRepository = null;
        this.pdfService = null;
        this.emailService = null;
    }
    
    public PayrollService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.companyRepository = null;
        this.pdfService = null;
        this.emailService = null;
    }

    public PayrollService(String dbUrl, CompanyRepository companyRepository, 
                         PayslipPdfService pdfService, EmailService emailService) {
        this.dbUrl = dbUrl;
        this.companyRepository = companyRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }
    
    public CompanyRepository getCompanyRepository() {
        return companyRepository;
    }
    
    public PayslipPdfService getPayslipPdfService() {
        return pdfService;
    }
    
    // ===== EMPLOYEE MANAGEMENT =====
    
    /**
     * Create a new employee
     */
    public Employee createEmployee(Employee employee) {
        String sql = """
            INSERT INTO employees (company_id, employee_number, first_name, last_name, email, phone, 
                                 position, department, hire_date, basic_salary, employment_type, 
                                 salary_type, tax_number, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, employee.getCompanyId());
            pstmt.setString(2, employee.getEmployeeNumber());
            pstmt.setString(3, employee.getFirstName());
            pstmt.setString(4, employee.getLastName());
            pstmt.setString(5, employee.getEmail());
            pstmt.setString(6, employee.getPhone());
            pstmt.setString(7, employee.getPosition());
            pstmt.setString(8, employee.getDepartment());
            pstmt.setDate(9, java.sql.Date.valueOf(employee.getHireDate()));
            pstmt.setBigDecimal(10, employee.getBasicSalary());
            pstmt.setString(11, employee.getEmploymentType().name());
            pstmt.setString(12, employee.getSalaryType().name());
            pstmt.setString(13, employee.getTaxNumber());
            pstmt.setString(14, employee.getCreatedBy());
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                employee.setId(rs.getLong("id"));
                LOGGER.info("Created employee: " + employee.getEmployeeNumber() + " - " + employee.getFullName());
                return employee;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating employee", e);
            throw new RuntimeException("Failed to create employee: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to create employee");
    }
    
    /**
     * Update an existing employee
     */
    public Employee updateEmployee(Employee employee) {
        String sql = """
            UPDATE employees SET 
                employee_number = ?, first_name = ?, last_name = ?, email = ?, phone = ?, 
                position = ?, department = ?, hire_date = ?, basic_salary = ?, employment_type = ?, 
                salary_type = ?, tax_number = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ? AND company_id = ?
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, employee.getEmployeeNumber());
            pstmt.setString(2, employee.getFirstName());
            pstmt.setString(3, employee.getLastName());
            pstmt.setString(4, employee.getEmail());
            pstmt.setString(5, employee.getPhone());
            pstmt.setString(6, employee.getPosition());
            pstmt.setString(7, employee.getDepartment());
            pstmt.setDate(8, java.sql.Date.valueOf(employee.getHireDate()));
            pstmt.setBigDecimal(9, employee.getBasicSalary());
            pstmt.setString(10, employee.getEmploymentType().name());
            pstmt.setString(11, employee.getSalaryType().name());
            pstmt.setString(12, employee.getTaxNumber());
            pstmt.setLong(13, employee.getId());
            pstmt.setLong(14, employee.getCompanyId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Updated employee: " + employee.getEmployeeNumber() + " - " + employee.getFullName());
                return employee;
            } else {
                throw new RuntimeException("Employee not found or update failed");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating employee", e);
            throw new RuntimeException("Failed to update employee: " + e.getMessage());
        }
    }
    
    /**
     * Delete an employee (soft delete by setting is_active = false)
     */
    public void deleteEmployee(Long employeeId, Long companyId) {
        String sql = "UPDATE employees SET is_active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, employeeId);
            pstmt.setLong(2, companyId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                LOGGER.info("Deleted employee with ID: " + employeeId);
            } else {
                throw new RuntimeException("Employee not found or delete failed");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting employee", e);
            throw new RuntimeException("Failed to delete employee: " + e.getMessage());
        }
    }
    
    /**
     * Get all active employees for a company
     */
    public List<Employee> getActiveEmployees(Long companyId) {
        String sql = """
            SELECT * FROM employees 
            WHERE company_id = ? AND is_active = TRUE 
            ORDER BY employee_number
            """;
        
        List<Employee> employees = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                employees.add(mapResultSetToEmployee(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting active employees", e);
            throw new RuntimeException("Failed to get employees: " + e.getMessage());
        }
        
        return employees;
    }
    
    /**
     * Get employee by ID
     */
    public Optional<Employee> getEmployeeById(Long employeeId) {
        String sql = "SELECT * FROM employees WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, employeeId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToEmployee(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting employee by ID", e);
        }
        
        return Optional.empty();
    }
    
    // ===== PAYROLL PERIOD MANAGEMENT =====
    
    /**
     * Create a new payroll period
     */
    public PayrollPeriod createPayrollPeriod(PayrollPeriod period) {
        String sql = """
            INSERT INTO payroll_periods (company_id, fiscal_period_id, period_name, pay_date, 
                                       start_date, end_date, period_type, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, period.getCompanyId());
            pstmt.setObject(2, period.getFiscalPeriodId());
            pstmt.setString(3, period.getPeriodName());
            pstmt.setDate(4, java.sql.Date.valueOf(period.getPayDate()));
            pstmt.setDate(5, java.sql.Date.valueOf(period.getStartDate()));
            pstmt.setDate(6, java.sql.Date.valueOf(period.getEndDate()));
            pstmt.setString(7, period.getPeriodType().name());
            pstmt.setString(8, period.getCreatedBy());
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                period.setId(rs.getLong("id"));
                LOGGER.info("Created payroll period: " + period.getPeriodName());
                return period;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating payroll period", e);
            throw new RuntimeException("Failed to create payroll period: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to create payroll period");
    }
    
    /**
     * Get payroll periods for a company
     */
    public List<PayrollPeriod> getPayrollPeriods(Long companyId) {
        String sql = """
            SELECT * FROM payroll_periods 
            WHERE company_id = ? 
            ORDER BY start_date DESC
            """;
        
        List<PayrollPeriod> periods = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                periods.add(mapResultSetToPayrollPeriod(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting payroll periods", e);
            throw new RuntimeException("Failed to get payroll periods: " + e.getMessage());
        }
        
        return periods;
    }
    
    // ===== PAYROLL PROCESSING =====
    
    /**
     * Process payroll for a specific period
     */
    public void processPayroll(Long payrollPeriodId, String processedBy) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                PayrollPeriod period = getPayrollPeriodById(payrollPeriodId)
                    .orElseThrow(() -> new RuntimeException("Payroll period not found"));
                
                if (!period.canBeProcessed()) {
                    throw new RuntimeException("Payroll period cannot be processed. Status: " + period.getStatus());
                }
                
                // Get all active employees for the company
                List<Employee> employees = getActiveEmployees(period.getCompanyId());
                
                BigDecimal totalGross = BigDecimal.ZERO;
                BigDecimal totalDeductions = BigDecimal.ZERO;
                BigDecimal totalNet = BigDecimal.ZERO;
                int employeeCount = 0;
                
                // Get company info for PDF generation
                Company company = companyRepository.findById(period.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found"));
                
                List<String> generatedPdfPaths = new ArrayList<>();
                
                for (Employee employee : employees) {
                    // Calculate and create payslip for each employee
                    Payslip payslip = calculatePayslip(employee, period);
                    savePayslip(conn, payslip);
                    
                    // ✅ ADD THIS: Generate PDF for each payslip
                    if (pdfService != null) {
                        try {
                            String pdfPath = pdfService.generatePayslipPdf(payslip, employee, company, period);
                            generatedPdfPaths.add(pdfPath);
                            LOGGER.info("Generated PDF for employee: " + employee.getEmployeeNumber() + " at " + pdfPath);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to generate PDF for employee: " + employee.getEmployeeNumber(), e);
                            // Don't fail the entire payroll if PDF generation fails
                        }
                    }
                    
                    totalGross = totalGross.add(payslip.getTotalEarnings());
                    totalDeductions = totalDeductions.add(payslip.getTotalDeductions());
                    totalNet = totalNet.add(payslip.getNetPay());
                    employeeCount++;
                }
                
                // Update payroll period totals
                updatePayrollPeriodTotals(conn, payrollPeriodId, totalGross, totalDeductions, 
                                        totalNet, employeeCount, processedBy);
                
                // Generate journal entries for payroll
                generatePayrollJournalEntries(conn, period, totalGross, totalDeductions, totalNet);
                
                conn.commit();
                LOGGER.info("Payroll processed successfully for period: " + period.getPeriodName() + 
                           " (" + employeeCount + " employees)");
                
                // ✅ ADD THIS: Log PDF generation results
                if (!generatedPdfPaths.isEmpty()) {
                    LOGGER.info("Generated " + generatedPdfPaths.size() + " PDF payslips");
                }
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error processing payroll", e);
            throw new RuntimeException("Failed to process payroll: " + e.getMessage());
        }
    }
    
    /**
     * Generate PDFs for an already processed payroll period
     */
    public void generatePayslipPdfs(Long payrollPeriodId) {
        try {
            PayrollPeriod period = getPayrollPeriodById(payrollPeriodId)
                .orElseThrow(() -> new RuntimeException("Payroll period not found"));
            
            Company company = companyRepository.findById(period.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found"));
            
            // Get all payslips for this period
            String sql = "SELECT * FROM payslips WHERE payroll_period_id = ?";
            List<Payslip> payslips = new ArrayList<>();
            List<Employee> employees = new ArrayList<>();
            
            try (Connection conn = DriverManager.getConnection(dbUrl);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setLong(1, payrollPeriodId);
                ResultSet rs = pstmt.executeQuery();
                
                while (rs.next()) {
                    Payslip payslip = mapResultSetToPayslip(rs);
                    payslips.add(payslip);
                    
                    // Get employee details
                    Optional<Employee> employeeOpt = getEmployeeById(payslip.getEmployeeId());
                    employeeOpt.ifPresent(employees::add);
                }
            }
            
            // Generate PDFs
            if (pdfService != null) {
                for (int i = 0; i < payslips.size(); i++) {
                    Payslip payslip = payslips.get(i);
                    Employee employee = employees.get(i);
                    
                    try {
                        String pdfPath = pdfService.generatePayslipPdf(payslip, employee, company, period);
                        LOGGER.info("Generated PDF for " + employee.getFullName() + ": " + pdfPath);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Failed to generate PDF for " + employee.getFullName(), e);
                    }
                }
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating PDFs for payroll period", e);
            throw new RuntimeException("Failed to generate PDFs: " + e.getMessage());
        }
    }
    
    /**
     * Calculate payslip for an employee
     */
    private Payslip calculatePayslip(Employee employee, PayrollPeriod period) {
        String payslipNumber = generatePayslipNumber(employee, period);
        
        Payslip payslip = new Payslip(employee.getCompanyId(), employee.getId(), 
                                    period.getId(), payslipNumber, employee.getBasicSalary());
        
        // Calculate basic earnings
        BigDecimal grossSalary = employee.getBasicSalary();
        payslip.setGrossSalary(grossSalary);
        
        // TODO: Add overtime calculations, allowances, etc.
        
        // Calculate PAYE tax
        BigDecimal payeeTax = calculatePayeeTax(grossSalary, employee.getTaxRebateCode());
        payslip.setPayeeTax(payeeTax);
        
        // Calculate UIF
        BigDecimal uifEmployee = calculateUifEmployee(grossSalary);
        BigDecimal uifEmployer = calculateUifEmployer(grossSalary);
        payslip.setUifEmployee(uifEmployee);
        payslip.setUifEmployer(uifEmployer);
        
        // TODO: Add other deductions (medical aid, pension fund, etc.)
        
        payslip.calculateTotals();
        payslip.setCreatedBy(period.getCreatedBy());
        
        return payslip;
    }
    
    // ===== TAX CALCULATIONS =====
    
    /**
     * Calculate PAYE tax for monthly salary
     */
    private BigDecimal calculatePayeeTax(BigDecimal monthlySalary, String taxRebateCode) {
        // Simplified PAYE calculation for South African tax tables 2024
        BigDecimal annualSalary = monthlySalary.multiply(BigDecimal.valueOf(12));
        BigDecimal annualTax = BigDecimal.ZERO;
        
        // 2024 Tax Brackets
        if (annualSalary.compareTo(BigDecimal.valueOf(237100)) <= 0) {
            annualTax = annualSalary.multiply(BigDecimal.valueOf(0.18));
        } else if (annualSalary.compareTo(BigDecimal.valueOf(370500)) <= 0) {
            annualTax = BigDecimal.valueOf(42678)
                .add(annualSalary.subtract(BigDecimal.valueOf(237100))
                .multiply(BigDecimal.valueOf(0.26)));
        } else if (annualSalary.compareTo(BigDecimal.valueOf(512800)) <= 0) {
            annualTax = BigDecimal.valueOf(77362)
                .add(annualSalary.subtract(BigDecimal.valueOf(370500))
                .multiply(BigDecimal.valueOf(0.31)));
        } else if (annualSalary.compareTo(BigDecimal.valueOf(673000)) <= 0) {
            annualTax = BigDecimal.valueOf(121475)
                .add(annualSalary.subtract(BigDecimal.valueOf(512800))
                .multiply(BigDecimal.valueOf(0.36)));
        } else if (annualSalary.compareTo(BigDecimal.valueOf(857900)) <= 0) {
            annualTax = BigDecimal.valueOf(179147)
                .add(annualSalary.subtract(BigDecimal.valueOf(673000))
                .multiply(BigDecimal.valueOf(0.39)));
        } else if (annualSalary.compareTo(BigDecimal.valueOf(1817000)) <= 0) {
            annualTax = BigDecimal.valueOf(251258)
                .add(annualSalary.subtract(BigDecimal.valueOf(857900))
                .multiply(BigDecimal.valueOf(0.41)));
        } else {
            annualTax = BigDecimal.valueOf(644489)
                .add(annualSalary.subtract(BigDecimal.valueOf(1817000))
                .multiply(BigDecimal.valueOf(0.45)));
        }
        
        // Apply primary rebate (simplified - should consider age)
        BigDecimal primaryRebate = BigDecimal.valueOf(17235);
        annualTax = annualTax.subtract(primaryRebate);
        
        if (annualTax.compareTo(BigDecimal.ZERO) < 0) {
            annualTax = BigDecimal.ZERO;
        }
        
        return annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate UIF employee contribution
     */
    private BigDecimal calculateUifEmployee(BigDecimal monthlySalary) {
        BigDecimal uifMaxEarnings = BigDecimal.valueOf(17712); // 2024 monthly max
        BigDecimal uifableIncome = monthlySalary.min(uifMaxEarnings);
        return uifableIncome.multiply(BigDecimal.valueOf(0.01))
                           .setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate UIF employer contribution
     */
    private BigDecimal calculateUifEmployer(BigDecimal monthlySalary) {
        return calculateUifEmployee(monthlySalary); // Same as employee contribution
    }
    
    // ===== JOURNAL ENTRY INTEGRATION =====
    
    /**
     * Generate journal entries for payroll processing
     * Integrates with the existing accounting system
     */
    private void generatePayrollJournalEntries(Connection conn, PayrollPeriod period, 
                                             BigDecimal totalGross, BigDecimal totalDeductions, 
                                             BigDecimal totalNet) throws SQLException {
        
        // Create main journal entry
        String reference = "PAY-" + period.getId() + "-" + 
                          period.getPayDate().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String description = "Payroll for " + period.getPeriodName();
        
        String insertJournalEntry = """
            INSERT INTO journal_entries (reference, entry_date, description, fiscal_period_id, 
                                       company_id, created_by, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            RETURNING id
            """;
        
        Long journalEntryId;
        try (PreparedStatement pstmt = conn.prepareStatement(insertJournalEntry)) {
            pstmt.setString(1, reference);
            pstmt.setDate(2, java.sql.Date.valueOf(period.getPayDate()));
            pstmt.setString(3, description);
            pstmt.setObject(4, period.getFiscalPeriodId());
            pstmt.setLong(5, period.getCompanyId());
            pstmt.setString(6, period.getCreatedBy());
            
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new SQLException("Failed to create journal entry");
            }
            journalEntryId = rs.getLong("id");
        }
        
        // Get account IDs
        Long salaryExpenseAccountId = getAccountByCode(conn, "8100"); // Employee Costs
        Long payrollLiabilityAccountId = getAccountByCode(conn, "2500"); // Payroll Liabilities
        Long bankAccountId = getAccountByCode(conn, "1100"); // Bank Account
        
        // Journal entry lines
        String insertLine = """
            INSERT INTO journal_entry_lines (journal_entry_id, account_id, debit_amount, 
                                           credit_amount, description, reference, created_at)
            VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertLine)) {
            // 1. Debit: Employee Costs (Gross Salary)
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, salaryExpenseAccountId);
            pstmt.setBigDecimal(3, totalGross);
            pstmt.setBigDecimal(4, null);
            pstmt.setString(5, "Gross salaries for " + period.getPeriodName());
            pstmt.setString(6, reference + "-01");
            pstmt.executeUpdate();
            
            // 2. Credit: Payroll Liabilities (Total Deductions)
            if (totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
                pstmt.setLong(1, journalEntryId);
                pstmt.setLong(2, payrollLiabilityAccountId);
                pstmt.setBigDecimal(3, null);
                pstmt.setBigDecimal(4, totalDeductions);
                pstmt.setString(5, "Payroll deductions for " + period.getPeriodName());
                pstmt.setString(6, reference + "-02");
                pstmt.executeUpdate();
            }
            
            // 3. Credit: Bank Account (Net Pay)
            pstmt.setLong(1, journalEntryId);
            pstmt.setLong(2, bankAccountId);
            pstmt.setBigDecimal(3, null);
            pstmt.setBigDecimal(4, totalNet);
            pstmt.setString(5, "Net pay for " + period.getPeriodName());
            pstmt.setString(6, reference + "-03");
            pstmt.executeUpdate();
        }
        
        // Record the journal entry link
        String insertPayrollJournal = """
            INSERT INTO payroll_journal_entries (company_id, payroll_period_id, journal_entry_id, 
                                                entry_type, description, total_amount, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertPayrollJournal)) {
            pstmt.setLong(1, period.getCompanyId());
            pstmt.setLong(2, period.getId());
            pstmt.setLong(3, journalEntryId);
            pstmt.setString(4, "PAYROLL_SUMMARY");
            pstmt.setString(5, description);
            pstmt.setBigDecimal(6, totalNet);
            pstmt.setString(7, period.getCreatedBy());
            pstmt.executeUpdate();
        }
        
        LOGGER.info("Generated journal entry " + reference + " for payroll period " + period.getPeriodName());
    }
    
    // ===== UTILITY METHODS =====
    
    private String generatePayslipNumber(Employee employee, PayrollPeriod period) {
        return period.getPayDate().format(DateTimeFormatter.ofPattern("yyyyMM")) + 
               "-" + employee.getEmployeeNumber();
    }
    
    public Optional<PayrollPeriod> getPayrollPeriodById(Long periodId) {
        String sql = "SELECT * FROM payroll_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, periodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToPayrollPeriod(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting payroll period by ID", e);
        }
        
        return Optional.empty();
    }
    
    private Long getAccountByCode(Connection conn, String accountCode) throws SQLException {
        String sql = "SELECT id FROM accounts WHERE account_code = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountCode);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong("id");
            }
        }
        
        throw new SQLException("Account not found for code: " + accountCode);
    }
    
    private void savePayslip(Connection conn, Payslip payslip) throws SQLException {
         String sql = """
             INSERT INTO payslips (
                 company_id, employee_id, payroll_period_id, payslip_number,
                 basic_salary, gross_salary, allowances, total_earnings,
                 payee_tax, uif_employee, uif_employer, total_deductions,
                 net_pay, status, created_by, created_at
             ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,now())
             ON CONFLICT (payslip_number) DO UPDATE SET
                 company_id = EXCLUDED.company_id,
                 employee_id = EXCLUDED.employee_id,
                 payroll_period_id = EXCLUDED.payroll_period_id,
                 basic_salary = EXCLUDED.basic_salary,
                 gross_salary = EXCLUDED.gross_salary,
                 allowances = EXCLUDED.allowances,
                 total_earnings = EXCLUDED.total_earnings,
                 payee_tax = EXCLUDED.payee_tax,
                 uif_employee = EXCLUDED.uif_employee,
                 uif_employer = EXCLUDED.uif_employer,
                 total_deductions = EXCLUDED.total_deductions,
                 net_pay = EXCLUDED.net_pay,
                 status = EXCLUDED.status,
                 created_by = EXCLUDED.created_by,
                 created_at = now()
             RETURNING id;
             """;

         try (PreparedStatement ps = conn.prepareStatement(sql)) {
             int i = 1;
             ps.setLong(i++, payslip.getCompanyId());
             ps.setLong(i++, payslip.getEmployeeId());
             ps.setLong(i++, payslip.getPayrollPeriodId());
             ps.setString(i++, payslip.getPayslipNumber());

             // compute allowances (sum allowance components) instead of using undefined getAllowances()
             BigDecimal allowances = BigDecimal.ZERO;
             if (payslip.getHousingAllowance() != null) allowances = allowances.add(payslip.getHousingAllowance());
             if (payslip.getTransportAllowance() != null) allowances = allowances.add(payslip.getTransportAllowance());
             if (payslip.getMedicalAllowance() != null) allowances = allowances.add(payslip.getMedicalAllowance());
             if (payslip.getOtherAllowances() != null) allowances = allowances.add(payslip.getOtherAllowances());
             if (payslip.getCommission() != null) allowances = allowances.add(payslip.getCommission());
             if (payslip.getBonus() != null) allowances = allowances.add(payslip.getBonus());

             // BigDecimal fields - handle nulls via setObject
             ps.setObject(i++, payslip.getBasicSalary());
             ps.setObject(i++, payslip.getGrossSalary());
             ps.setObject(i++, allowances);                      // <- replaced undefined getter
             ps.setObject(i++, payslip.getTotalEarnings());
             ps.setObject(i++, payslip.getPayeeTax());
             ps.setObject(i++, payslip.getUifEmployee());
             ps.setObject(i++, payslip.getUifEmployer());
             ps.setObject(i++, payslip.getTotalDeductions());
             ps.setObject(i++, payslip.getNetPay());

             // status is an enum — store its name
             ps.setString(i++, payslip.getStatus() != null ? payslip.getStatus().name() : null);
             ps.setString(i++, payslip.getCreatedBy());

             try (ResultSet rs = ps.executeQuery()) {
                 if (rs.next()) {
                     payslip.setId(rs.getLong("id"));
                 }
             }
         }
     }
    
    private void updatePayrollPeriodTotals(Connection conn, Long periodId, BigDecimal totalGross,
                                         BigDecimal totalDeductions, BigDecimal totalNet,
                                         int employeeCount, String processedBy) throws SQLException {
        String sql = """
            UPDATE payroll_periods 
            SET total_gross_pay = ?, total_deductions = ?, total_net_pay = ?, 
                employee_count = ?, status = 'PROCESSED', processed_at = CURRENT_TIMESTAMP, 
                processed_by = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBigDecimal(1, totalGross);
            pstmt.setBigDecimal(2, totalDeductions);
            pstmt.setBigDecimal(3, totalNet);
            pstmt.setInt(4, employeeCount);
            pstmt.setString(5, processedBy);
            pstmt.setLong(6, periodId);
            pstmt.executeUpdate();
        }
    }
    
    // ===== RESULT SET MAPPERS =====
    
    private Employee mapResultSetToEmployee(ResultSet rs) throws SQLException {
        Employee employee = new Employee();
        employee.setId(rs.getLong("id"));
        employee.setCompanyId(rs.getLong("company_id"));
        employee.setEmployeeNumber(rs.getString("employee_number"));
        employee.setFirstName(rs.getString("first_name"));
        employee.setLastName(rs.getString("last_name"));
        employee.setEmail(rs.getString("email"));
        employee.setPhone(rs.getString("phone"));
        employee.setPosition(rs.getString("position"));
        employee.setDepartment(rs.getString("department"));
        employee.setHireDate(rs.getDate("hire_date").toLocalDate());
        employee.setActive(rs.getBoolean("is_active"));
        employee.setBasicSalary(rs.getBigDecimal("basic_salary"));
        employee.setTaxNumber(rs.getString("tax_number"));
        employee.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        String employmentType = rs.getString("employment_type");
        if (employmentType != null) {
            employee.setEmploymentType(Employee.EmploymentType.valueOf(employmentType));
        }
        
        String salaryType = rs.getString("salary_type");
        if (salaryType != null) {
            employee.setSalaryType(Employee.SalaryType.valueOf(salaryType));
        }
        
        return employee;
    }
    
    private PayrollPeriod mapResultSetToPayrollPeriod(ResultSet rs) throws SQLException {
        PayrollPeriod period = new PayrollPeriod();
        period.setId(rs.getLong("id"));
        period.setCompanyId(rs.getLong("company_id"));
        period.setFiscalPeriodId((Long) rs.getObject("fiscal_period_id"));
        period.setPeriodName(rs.getString("period_name"));
        period.setPayDate(rs.getDate("pay_date").toLocalDate());
        period.setStartDate(rs.getDate("start_date").toLocalDate());
        period.setEndDate(rs.getDate("end_date").toLocalDate());
        period.setStatus(PayrollPeriod.PayrollStatus.valueOf(rs.getString("status")));
        period.setTotalGrossPay(rs.getBigDecimal("total_gross_pay"));
        period.setTotalDeductions(rs.getBigDecimal("total_deductions"));
        period.setTotalNetPay(rs.getBigDecimal("total_net_pay"));
        period.setEmployeeCount(rs.getInt("employee_count"));
        period.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        String periodType = rs.getString("period_type");
        if (periodType != null) {
            period.setPeriodType(PayrollPeriod.PeriodType.valueOf(periodType));
        }
        
        return period;
    }
    
    private Payslip mapResultSetToPayslip(ResultSet rs) throws SQLException {
        Payslip payslip = new Payslip();
        payslip.setId(rs.getLong("id"));
        payslip.setCompanyId(rs.getLong("company_id"));
        payslip.setEmployeeId(rs.getLong("employee_id"));
        payslip.setPayrollPeriodId(rs.getLong("payroll_period_id"));
        payslip.setPayslipNumber(rs.getString("payslip_number"));
        payslip.setBasicSalary(rs.getBigDecimal("basic_salary"));
        
        // Debug: Print values from database
        System.out.println("Debug mapResultSetToPayslip: payslip_id=" + rs.getLong("id") + 
                          ", gross_salary=" + rs.getBigDecimal("gross_salary") + 
                          ", basic_salary=" + rs.getBigDecimal("basic_salary") +
                          ", total_earnings=" + rs.getBigDecimal("total_earnings") +
                          ", net_pay=" + rs.getBigDecimal("net_pay"));
        
        // Set all allowance fields first
        payslip.setHousingAllowance(rs.getBigDecimal("housing_allowance") != null ? rs.getBigDecimal("housing_allowance") : BigDecimal.ZERO);
        payslip.setTransportAllowance(rs.getBigDecimal("transport_allowance") != null ? rs.getBigDecimal("transport_allowance") : BigDecimal.ZERO);
        payslip.setMedicalAllowance(rs.getBigDecimal("medical_allowance") != null ? rs.getBigDecimal("medical_allowance") : BigDecimal.ZERO);
        payslip.setOtherAllowances(rs.getBigDecimal("other_allowances") != null ? rs.getBigDecimal("other_allowances") : BigDecimal.ZERO);
        payslip.setCommission(rs.getBigDecimal("commission") != null ? rs.getBigDecimal("commission") : BigDecimal.ZERO);
        payslip.setBonus(rs.getBigDecimal("bonus") != null ? rs.getBigDecimal("bonus") : BigDecimal.ZERO);
        
        // Set deductions
        payslip.setPayeeTax(rs.getBigDecimal("paye_tax") != null ? rs.getBigDecimal("paye_tax") : BigDecimal.ZERO);
        payslip.setUifEmployee(rs.getBigDecimal("uif_employee") != null ? rs.getBigDecimal("uif_employee") : BigDecimal.ZERO);
        payslip.setUifEmployer(rs.getBigDecimal("uif_employer") != null ? rs.getBigDecimal("uif_employer") : BigDecimal.ZERO);
        payslip.setMedicalAid(rs.getBigDecimal("medical_aid") != null ? rs.getBigDecimal("medical_aid") : BigDecimal.ZERO);
        payslip.setPensionFund(rs.getBigDecimal("pension_fund") != null ? rs.getBigDecimal("pension_fund") : BigDecimal.ZERO);
        payslip.setLoanDeduction(rs.getBigDecimal("loan_deduction") != null ? rs.getBigDecimal("loan_deduction") : BigDecimal.ZERO);
        payslip.setOtherDeductions(rs.getBigDecimal("other_deductions") != null ? rs.getBigDecimal("other_deductions") : BigDecimal.ZERO);
        
        // Now set gross salary (this will trigger calculateTotals, but all allowances are set)
        BigDecimal basicSalary = rs.getBigDecimal("basic_salary");
        BigDecimal grossSalary = (basicSalary != null) ? basicSalary : BigDecimal.ZERO;
        payslip.setGrossSalary(grossSalary);
        
        // Override the calculated totals with the stored values from database, but handle nulls
        BigDecimal storedTotalEarnings = rs.getBigDecimal("total_earnings");
        BigDecimal storedTotalDeductions = rs.getBigDecimal("total_deductions");
        BigDecimal storedNetPay = rs.getBigDecimal("net_pay");
        
        if (storedTotalEarnings != null) {
            payslip.setTotalEarnings(storedTotalEarnings);
        }
        if (storedTotalDeductions != null) {
            payslip.setTotalDeductions(storedTotalDeductions);
        }
        if (storedNetPay != null) {
            payslip.setNetPay(storedNetPay);
        } else {
            // If net_pay is null in database, recalculate it
            payslip.calculateTotals();
        }
        
        payslip.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return payslip;
    }
    
    /**
     * Initialize payroll database schema
     * This method should be called during system setup
     */
    public void initializePayrollSchema() {
        // TODO: Execute payroll_database_schema.sql
        LOGGER.info("Payroll schema initialization - manual execution required");
        System.out.println("⚠️  Please execute the payroll database schema manually:");
        System.out.println("   File: docs/payroll_database_schema.sql");
    }

    /**
     * Get payslips for a company
     */
    public List<Payslip> getPayslips(Long companyId) {
        String sql = """
            SELECT * FROM payslips 
            WHERE company_id = ? 
            ORDER BY created_at DESC
            """;
        
        List<Payslip> payslips = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                payslips.add(mapResultSetToPayslip(rs));
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting payslips", e);
            throw new RuntimeException("Failed to get payslips: " + e.getMessage());
        }
        
        return payslips;
    }
}
