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
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.*;

/**
 * Comprehensive Payroll Service for FIN Financial Management System
 * Handles all payroll operations including tax calculations, UIF, and integration with accounting
 */
public class PayrollService {
    private static final Logger LOGGER = Logger.getLogger(PayrollService.class.getName());
    private final String dbUrl;
    private final SARSTaxCalculator sarsTaxCalculator;
    private final CompanyRepository companyRepository;
    private final PayslipPdfService pdfService;
    private final EmailService emailService;
    
    // Journal Entry Line Numbers
    private static final int JOURNAL_LINE_GROSS_SALARY = 1;
    private static final int JOURNAL_LINE_PAYROLL_DEDUCTIONS = 2;
    private static final int JOURNAL_LINE_NET_PAY = 3;
    
    // Employee Import Field Indices
    private static final int FIELD_EMPLOYEE_CODE = 0;
    private static final int FIELD_SURNAME = 1;
    private static final int FIELD_DATE_OF_BIRTH = 2;
    private static final int FIELD_DATE_ENGAGED = 3;
    private static final int FIELD_TITLE = 4;
    private static final int FIELD_FIRST_NAME = 6;
    private static final int FIELD_SECOND_NAME = 7;
    private static final int FIELD_ID_NUMBER = 9;
    private static final int FIELD_UNIT_NUMBER = 15;
    private static final int FIELD_COMPLEX_NAME = 16;
    private static final int FIELD_STREET_NUMBER = 17;
    private static final int FIELD_STREET_NAME = 18;
    private static final int FIELD_SUBURB = 19;
    private static final int FIELD_CITY = 20;
    private static final int FIELD_POSTAL_CODE = 21;
    private static final int FIELD_CELL_NUMBER = 22;
    private static final int FIELD_EMAIL = 24;
    private static final int FIELD_JOB_TITLE = 28;
    private static final int FIELD_FIXED_SALARY = 39;
    
    // Other Constants
    private static final int MIN_EMPLOYEE_FIELDS = 40;
    private static final int MAX_ERROR_MESSAGE_LENGTH = 100;
    
    // PreparedStatement Parameter Indices
    private static final int PARAM_EMPLOYEE_NUMBER = 2;
    private static final int PARAM_FIRST_NAME = 3;
    private static final int PARAM_LAST_NAME = 4;
    private static final int PARAM_EMAIL = 5;
    private static final int PARAM_PHONE = 6;
    private static final int PARAM_POSITION = 7;
    private static final int PARAM_DEPARTMENT = 8;
    private static final int PARAM_HIRE_DATE = 9;
    private static final int PARAM_BASIC_SALARY = 10;
    private static final int PARAM_EMPLOYMENT_TYPE = 11;
    private static final int PARAM_SALARY_TYPE = 12;
    private static final int PARAM_TAX_NUMBER = 13;
    private static final int PARAM_CREATED_BY = 14;
    private static final int PARAM_ID = 13;
    private static final int PARAM_COMPANY_ID = 14;
    
    // Payroll Period Parameter Indices
    private static final int PARAM_PERIOD_NAME = 3;
    private static final int PARAM_PAY_DATE = 4;
    private static final int PARAM_START_DATE = 5;
    private static final int PARAM_END_DATE = 6;
    private static final int PARAM_PERIOD_TYPE = 7;
    
    // Query Parameter Indices
    private static final int PARAM_YEAR = 2;
    private static final int PARAM_MONTH = 3;
    
    // Journal Entry Parameter Indices
    private static final int PARAM_REFERENCE = 1;
    private static final int PARAM_ENTRY_DATE = 2;
    private static final int PARAM_DESCRIPTION = 3;
    private static final int PARAM_FISCAL_PERIOD_ID = 4;
    private static final int PARAM_JOURNAL_COMPANY_ID = 5;
    private static final int PARAM_JOURNAL_CREATED_BY = 6;
    private static final int PARAM_JOURNAL_ENTRY_ID = 1;
    private static final int PARAM_ACCOUNT_ID = 2;
    private static final int PARAM_DEBIT_AMOUNT = 3;
    private static final int PARAM_CREDIT_AMOUNT = 4;
    private static final int PARAM_JOURNAL_LINE_DESCRIPTION = 5;
    private static final int PARAM_JOURNAL_REFERENCE = 6;
    
    // Payroll Journal Parameter Indices
    private static final int PARAM_PAYROLL_COMPANY_ID = 1;
    private static final int PARAM_PAYROLL_PERIOD_ID = 2;
    private static final int PARAM_PAYROLL_JOURNAL_ENTRY_ID = 3;
    private static final int PARAM_ENTRY_TYPE = 4;
    private static final int PARAM_PAYROLL_DESCRIPTION = 5;
    private static final int PARAM_TOTAL_AMOUNT = 6;
    private static final int PARAM_PAYROLL_CREATED_BY = 7;
    
    // Payslip Parameter Indices
    private static final int PARAM_PAYSLIP_COMPANY_ID = 1;
    private static final int PARAM_PAYSLIP_EMPLOYEE_ID = 2;
    private static final int PARAM_PAYSLIP_PERIOD_ID = 3;
    private static final int PARAM_PAYSLIP_NUMBER = 4;
    private static final int PARAM_PAYSLIP_BASIC_SALARY = 5;
    private static final int PARAM_PAYSLIP_GROSS_SALARY = 6;
    private static final int PARAM_PAYSLIP_TOTAL_EARNINGS = 7;
    private static final int PARAM_PAYSLIP_PAYEE_TAX = 8;
    private static final int PARAM_PAYSLIP_UIF_EMPLOYEE = 9;
    private static final int PARAM_PAYSLIP_UIF_EMPLOYER = 10;
    private static final int PARAM_PAYSLIP_SDL_LEVY = 11;
    private static final int PARAM_PAYSLIP_TOTAL_DEDUCTIONS = 12;
    private static final int PARAM_PAYSLIP_NET_PAY = 13;
    private static final int PARAM_PAYSLIP_STATUS = 14;
    private static final int PARAM_PAYSLIP_CREATED_BY = 15;
    
    // Update Payroll Period Parameter Indices
    private static final int PARAM_TOTAL_GROSS_PAY = 1;
    private static final int PARAM_TOTAL_DEDUCTIONS = 2;
    private static final int PARAM_TOTAL_NET_PAY = 3;
    private static final int PARAM_EMPLOYEE_COUNT = 4;
    private static final int PARAM_PROCESSED_BY = 5;
    private static final int PARAM_PERIOD_ID = 6;
    
    // Update Employee Parameter Indices
    private static final int PARAM_UPDATE_EMPLOYEE_NUMBER = 1;
    private static final int PARAM_UPDATE_FIRST_NAME = 2;
    private static final int PARAM_UPDATE_LAST_NAME = 3;
    private static final int PARAM_UPDATE_EMAIL = 4;
    private static final int PARAM_UPDATE_PHONE = 5;
    private static final int PARAM_UPDATE_POSITION = 6;
    private static final int PARAM_UPDATE_DEPARTMENT = 7;
    private static final int PARAM_UPDATE_HIRE_DATE = 8;
    private static final int PARAM_UPDATE_BASIC_SALARY = 9;
    private static final int PARAM_UPDATE_EMPLOYMENT_TYPE = 10;
    private static final int PARAM_UPDATE_SALARY_TYPE = 11;
    private static final int PARAM_UPDATE_TAX_NUMBER = 12;
    private static final int PARAM_UPDATE_ID = 13;
    private static final int PARAM_UPDATE_COMPANY_ID = 14;
    
    // Create Payroll Period Parameter Indices
    private static final int PARAM_CREATE_PERIOD_NAME = 3;
    private static final int PARAM_CREATE_PAY_DATE = 4;
    private static final int PARAM_CREATE_START_DATE = 5;
    private static final int PARAM_CREATE_END_DATE = 6;
    private static final int PARAM_CREATE_PERIOD_TYPE = 7;
    private static final int PARAM_CREATE_CREATED_BY = 8;
    
    public PayrollService() {
        String databaseUrl = DatabaseConfig.getDatabaseUrl();
        SARSTaxCalculator taxCalc = new SARSTaxCalculator();
        initializeTaxCalculator(taxCalc);
        this.dbUrl = databaseUrl;
        this.sarsTaxCalculator = taxCalc;
        this.companyRepository = null;
        this.pdfService = null;
        this.emailService = null;
    }
    
    public PayrollService(String dbUrl) {
        SARSTaxCalculator taxCalc = new SARSTaxCalculator();
        initializeTaxCalculator(taxCalc);
        CompanyRepository companyRepo = new CompanyRepository(dbUrl);
        PayslipPdfService pdfSvc = new PayslipPdfService(companyRepo);
        EmailService emailSvc = new EmailService();
        this.dbUrl = dbUrl;
        this.sarsTaxCalculator = taxCalc;
        this.companyRepository = companyRepo;
        this.pdfService = pdfSvc;
        this.emailService = emailSvc;
    }

    public PayrollService(String dbUrl, CompanyRepository companyRepository, 
                         PayslipPdfService pdfService, EmailService emailService) {
        SARSTaxCalculator taxCalc = new SARSTaxCalculator();
        initializeTaxCalculator(taxCalc);
        this.dbUrl = dbUrl;
        this.sarsTaxCalculator = taxCalc;
        this.companyRepository = companyRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }
    
    private void initializeTaxCalculator(SARSTaxCalculator taxCalc) {
        // Skip tax calculator initialization in test environment
        if (isTestEnvironment()) {
            System.out.println("🧪 Skipping SARS tax calculator initialization in test environment");
            return;
        }
        
        try {
            // Load tax tables from the PDF text file for accurate SARS 2026 calculations
            String pdfTextPath = "input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt";
            taxCalc.loadTaxTablesFromPDFText(pdfTextPath);
            System.out.println("✅ SARS Tax Calculator initialized with official 2026 tables");
        } catch (IOException e) {
            System.out.println("❌ Failed to load SARS tax tables: " + e.getMessage() + " - using default calculations");
            // Don't throw exception - allow constructor to complete
        }
    }
    
    /**
     * Check if we're running in a test environment
     */
    private boolean isTestEnvironment() {
        // Check for test system property or environment variable
        return System.getProperty("test.mode", "false").equals("true") ||
               System.getenv("TEST_MODE") != null ||
               System.getProperty("java.class.path", "").contains("test");
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
            pstmt.setString(PARAM_EMPLOYEE_NUMBER, employee.getEmployeeNumber());
            pstmt.setString(PARAM_FIRST_NAME, employee.getFirstName());
            pstmt.setString(PARAM_LAST_NAME, employee.getLastName());
            pstmt.setString(PARAM_EMAIL, employee.getEmail());
            pstmt.setString(PARAM_PHONE, employee.getPhone());
            pstmt.setString(PARAM_POSITION, employee.getPosition());
            pstmt.setString(PARAM_DEPARTMENT, employee.getDepartment());
            pstmt.setDate(PARAM_HIRE_DATE, java.sql.Date.valueOf(employee.getHireDate()));
            pstmt.setBigDecimal(PARAM_BASIC_SALARY, employee.getBasicSalary());
            pstmt.setString(PARAM_EMPLOYMENT_TYPE, employee.getEmploymentType().name());
            pstmt.setString(PARAM_SALARY_TYPE, employee.getSalaryType().name());
            pstmt.setString(PARAM_TAX_NUMBER, employee.getTaxNumber());
            pstmt.setString(PARAM_CREATED_BY, employee.getCreatedBy());
            
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
            
            pstmt.setString(PARAM_UPDATE_EMPLOYEE_NUMBER, employee.getEmployeeNumber());
            pstmt.setString(PARAM_UPDATE_FIRST_NAME, employee.getFirstName());
            pstmt.setString(PARAM_UPDATE_LAST_NAME, employee.getLastName());
            pstmt.setString(PARAM_UPDATE_EMAIL, employee.getEmail());
            pstmt.setString(PARAM_UPDATE_PHONE, employee.getPhone());
            pstmt.setString(PARAM_UPDATE_POSITION, employee.getPosition());
            pstmt.setString(PARAM_UPDATE_DEPARTMENT, employee.getDepartment());
            pstmt.setDate(PARAM_UPDATE_HIRE_DATE, java.sql.Date.valueOf(employee.getHireDate()));
            pstmt.setBigDecimal(PARAM_UPDATE_BASIC_SALARY, employee.getBasicSalary());
            pstmt.setString(PARAM_UPDATE_EMPLOYMENT_TYPE, employee.getEmploymentType().name());
            pstmt.setString(PARAM_UPDATE_SALARY_TYPE, employee.getSalaryType().name());
            pstmt.setString(PARAM_UPDATE_TAX_NUMBER, employee.getTaxNumber());
            pstmt.setLong(PARAM_UPDATE_ID, employee.getId());
            pstmt.setLong(PARAM_UPDATE_COMPANY_ID, employee.getCompanyId());
            
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
        // Auto-determine fiscal period if not set
        if (period.getFiscalPeriodId() == null) {
            Long fiscalPeriodId = findFiscalPeriodForDate(period.getCompanyId(), period.getStartDate());
            if (fiscalPeriodId != null) {
                period.setFiscalPeriodId(fiscalPeriodId);
                LOGGER.info("Auto-assigned fiscal period ID: " + fiscalPeriodId + " for payroll period: " + period.getPeriodName());
            } else {
                LOGGER.warning("No fiscal period found for dates " + period.getStartDate() + " to " + period.getEndDate());
            }
        }
        
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
            pstmt.setString(PARAM_CREATE_PERIOD_NAME, period.getPeriodName());
            pstmt.setDate(PARAM_CREATE_PAY_DATE, java.sql.Date.valueOf(period.getPayDate()));
            pstmt.setDate(PARAM_CREATE_START_DATE, java.sql.Date.valueOf(period.getStartDate()));
            pstmt.setDate(PARAM_CREATE_END_DATE, java.sql.Date.valueOf(period.getEndDate()));
            pstmt.setString(PARAM_CREATE_PERIOD_TYPE, period.getPeriodType().name());
            pstmt.setString(PARAM_CREATE_CREATED_BY, period.getCreatedBy());
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                period.setId(rs.getLong("id"));
                LOGGER.info("Created payroll period: " + period.getPeriodName());
                // Retrieve the complete period data including fiscal_period_id
                return getPayrollPeriodById(period.getId()).orElse(period);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating payroll period", e);
            throw new RuntimeException("Failed to create payroll period: " + e.getMessage());
        }
        
        throw new RuntimeException("Failed to create payroll period");
    }
    
    /**
     * Find the fiscal period that contains a given date
     */
    private Long findFiscalPeriodForDate(Long companyId, LocalDate date) {
        String sql = """
            SELECT id FROM fiscal_periods 
            WHERE company_id = ? 
            AND ? BETWEEN start_date AND end_date
            LIMIT 1
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setDate(2, java.sql.Date.valueOf(date));
            
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error finding fiscal period for date: " + date, e);
        }
        
        return null;
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
    
    /**
     * Delete a payroll period (only if it's in OPEN status)
     */
    public void deletePayrollPeriod(Long periodId, Long companyId) {
        // First check if the period exists and can be deleted
        Optional<PayrollPeriod> periodOpt = getPayrollPeriodById(periodId);
        if (periodOpt.isEmpty()) {
            throw new RuntimeException("Payroll period not found");
        }
        
        PayrollPeriod period = periodOpt.get();
        
        // Verify the period belongs to the company
        if (!period.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Payroll period does not belong to this company");
        }
        
        // Only allow deletion of OPEN periods (not processed)
        if (period.getStatus() != PayrollPeriod.PayrollStatus.OPEN) {
            throw new RuntimeException("Cannot delete payroll period with status: " + period.getStatus() + 
                                     ". Only OPEN periods can be deleted.");
        }
        
        String sql = "DELETE FROM payroll_periods WHERE id = ? AND company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, periodId);
            pstmt.setLong(2, companyId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("Payroll period not found or could not be deleted");
            }
            
            LOGGER.info("Deleted payroll period: " + period.getPeriodName());
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting payroll period", e);
            throw new RuntimeException("Failed to delete payroll period: " + e.getMessage());
        }
    }
    
    /**
     * Force delete a payroll period regardless of status (DANGER: This will delete processed periods)
     */
    public void forceDeletePayrollPeriod(Long periodId, Long companyId) {
        // First check if the period exists
        Optional<PayrollPeriod> periodOpt = getPayrollPeriodById(periodId);
        if (periodOpt.isEmpty()) {
            throw new RuntimeException("Payroll period not found");
        }
        
        PayrollPeriod period = periodOpt.get();
        
        // Verify the period belongs to the company
        if (!period.getCompanyId().equals(companyId)) {
            throw new RuntimeException("Payroll period does not belong to this company");
        }
        
        // WARNING: This will delete the period regardless of status
        // Also delete associated payslips and journal entries
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try {
                // Delete associated payslips first
                String deletePayslipsSql = "DELETE FROM payslips WHERE payroll_period_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deletePayslipsSql)) {
                    pstmt.setLong(1, periodId);
                    int payslipsDeleted = pstmt.executeUpdate();
                    LOGGER.info("Deleted " + payslipsDeleted + " payslips for period: " + period.getPeriodName());
                }
                
                // Delete associated payroll journal entries
                String deleteJournalSql = "DELETE FROM payroll_journal_entries WHERE payroll_period_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteJournalSql)) {
                    pstmt.setLong(1, periodId);
                    int journalEntriesDeleted = pstmt.executeUpdate();
                    LOGGER.info("Deleted " + journalEntriesDeleted + " journal entries for period: " + period.getPeriodName());
                }
                
                // Delete the payroll period
                String deletePeriodSql = "DELETE FROM payroll_periods WHERE id = ? AND company_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deletePeriodSql)) {
                    pstmt.setLong(1, periodId);
                    pstmt.setLong(2, companyId);
                    
                    int rowsAffected = pstmt.executeUpdate();
                    if (rowsAffected == 0) {
                        throw new RuntimeException("Payroll period not found or could not be deleted");
                    }
                }
                
                conn.commit();
                LOGGER.warning("FORCE DELETED payroll period: " + period.getPeriodName() + " (Status: " + period.getStatus() + ")");
                
            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error force deleting payroll period", e);
            throw new RuntimeException("Failed to force delete payroll period: " + e.getMessage());
        }
    }
    
    /**
     * Force delete all payroll periods for a specific month and year
     */
    public void forceDeleteAllPayrollPeriodsForMonth(Long companyId, int year, int month) {
        String sql = "SELECT id, period_name, status FROM payroll_periods WHERE company_id = ? AND EXTRACT(YEAR FROM pay_date) = ? AND EXTRACT(MONTH FROM pay_date) = ?";
        
        List<PayrollPeriod> periodsToDelete = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setInt(2, year);
            pstmt.setInt(PARAM_MONTH, month);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PayrollPeriod period = new PayrollPeriod();
                period.setId(rs.getLong("id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStatus(PayrollPeriod.PayrollStatus.valueOf(rs.getString("status")));
                periodsToDelete.add(period);
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding payroll periods for month", e);
            throw new RuntimeException("Failed to find payroll periods: " + e.getMessage());
        }
        
        if (periodsToDelete.isEmpty()) {
            LOGGER.info("No payroll periods found for " + year + "-" + String.format("%02d", month));
            return;
        }
        
        LOGGER.warning("FORCE DELETING " + periodsToDelete.size() + " payroll periods for " + year + "-" + String.format("%02d", month));
        
        for (PayrollPeriod period : periodsToDelete) {
            try {
                forceDeletePayrollPeriod(period.getId(), companyId);
                LOGGER.warning("Deleted period: " + period.getPeriodName() + " (Status: " + period.getStatus() + ")");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to delete period " + period.getPeriodName(), e);
                // Continue with other periods
            }
        }
        
        LOGGER.warning("Completed force deletion of " + periodsToDelete.size() + " payroll periods for " + year + "-" + String.format("%02d", month));
    }
    
    // ===== PAYROLL PROCESSING =====
    
    /**
     * Process payroll for a specific period
     */
    public void processPayroll(Long payrollPeriodId, String processedBy) {
        // Get company info BEFORE starting transaction to avoid connection issues
        PayrollPeriod period = getPayrollPeriodById(payrollPeriodId)
            .orElseThrow(() -> new RuntimeException("Payroll period not found"));

        Company company = companyRepository.findById(period.getCompanyId())
            .orElseThrow(() -> new RuntimeException("Company not found"));

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);

            try {
                // Clear existing payslips if reprocessing
                boolean isReprocessing = false;
                if (period.getStatus() == PayrollPeriod.PayrollStatus.PROCESSED) {
                    LOGGER.info("Clearing existing payslips for reprocessing of period: " + period.getPeriodName());
                    clearExistingPayslips(conn, payrollPeriodId);
                    
                    // Reset payroll period status to allow reprocessing
                    resetPayrollPeriodStatus(conn, payrollPeriodId);
                    LOGGER.info("Payroll period reset to OPEN status for reprocessing");
                    isReprocessing = true;
                    
                    // Update in-memory period status to reflect database change
                    period.setStatus(PayrollPeriod.PayrollStatus.OPEN);
                }

                if (!period.canBeProcessed() && !isReprocessing) {
                    throw new RuntimeException("Payroll period cannot be processed. Status: " + period.getStatus());
                }

                // Get all active employees for the company
                List<Employee> employees = getActiveEmployees(period.getCompanyId());

                // Calculate total company payroll for SDL determination
                BigDecimal totalCompanyGross = BigDecimal.ZERO;
                for (Employee emp : employees) {
                    totalCompanyGross = totalCompanyGross.add(emp.getBasicSalary() != null ? emp.getBasicSalary() : BigDecimal.ZERO);
                }

                BigDecimal totalGross = BigDecimal.ZERO;
                BigDecimal totalDeductions = BigDecimal.ZERO;
                BigDecimal totalNet = BigDecimal.ZERO;
                int employeeCount = 0;

                List<String> generatedPdfPaths = new ArrayList<>();

                for (Employee employee : employees) {
                    // Calculate and create payslip for each employee (pass total company payroll for SDL calculation)
                    Payslip payslip = calculatePayslip(employee, period, totalCompanyGross);
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

            } catch (RuntimeException e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error processing payroll", e);
            throw new RuntimeException("Failed to process payroll: " + e.getMessage());
        }
    }
    
    /**
     * Clear existing payslips for a payroll period to allow reprocessing
     * This deletes all payslip records associated with the given payroll period
     */
    private void clearExistingPayslips(Connection conn, Long payrollPeriodId) throws SQLException {
        String deleteSql = "DELETE FROM payslips WHERE payroll_period_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setLong(1, payrollPeriodId);
            int deletedCount = pstmt.executeUpdate();
            LOGGER.info("Deleted " + deletedCount + " existing payslips for reprocessing");
        }
    }
    
    /**
     * Reset payroll period status to OPEN to allow reprocessing
     */
    private void resetPayrollPeriodStatus(Connection conn, Long payrollPeriodId) throws SQLException {
        String updateSql = """
            UPDATE payroll_periods 
            SET status = 'OPEN', 
                total_gross_pay = 0, 
                total_deductions = 0, 
                total_net_pay = 0,
                employee_count = 0,
                processed_at = NULL,
                processed_by = NULL
            WHERE id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setLong(1, payrollPeriodId);
            pstmt.executeUpdate();
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
     * Calculate payslip for an employee using SARSTaxCalculator
     */
    private Payslip calculatePayslip(Employee employee, PayrollPeriod period, BigDecimal totalCompanyGross) {
        String payslipNumber = generatePayslipNumber(employee, period);
        
        Payslip payslip = new Payslip(employee.getCompanyId(), employee.getId(), 
                                    period.getId(), payslipNumber, employee.getBasicSalary());
        
        // Calculate basic earnings
        BigDecimal grossSalary = employee.getBasicSalary();
        payslip.setGrossSalary(grossSalary);
        
        // TODO: Add overtime calculations, allowances, etc.
        
        // Calculate PAYE tax using SARSTaxCalculator
        double grossDouble = grossSalary.doubleValue();
        double payeDouble = sarsTaxCalculator.findPAYE(grossDouble);
        BigDecimal payeeTax = BigDecimal.valueOf(payeDouble);
        payslip.setPayeeTax(payeeTax);
        
        // Calculate UIF using SARSTaxCalculator (same for employee and employer)
        double uifDouble = sarsTaxCalculator.calculateUIF(grossDouble);
        BigDecimal uifEmployee = BigDecimal.valueOf(uifDouble);
        BigDecimal uifEmployer = BigDecimal.valueOf(uifDouble);
        payslip.setUifEmployee(uifEmployee);
        payslip.setUifEmployer(uifEmployer);
        
        // Calculate SDL (Skills Development Levy) - 1% of gross for companies with payroll > R500k/year
        double sdlDouble = sarsTaxCalculator.calculateSDL(grossDouble, totalCompanyGross.doubleValue());
        BigDecimal sdlLevy = BigDecimal.valueOf(sdlDouble);
        payslip.setSdlLevy(sdlLevy);
        
        // TODO: Add other deductions (medical aid, pension fund, etc.)
        
        payslip.calculateTotals();
        payslip.setCreatedBy(period.getCreatedBy());
        
        return payslip;
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
            pstmt.setString(PARAM_REFERENCE, reference);
            pstmt.setDate(PARAM_ENTRY_DATE, java.sql.Date.valueOf(period.getPayDate()));
            pstmt.setString(PARAM_DESCRIPTION, description);
            pstmt.setObject(PARAM_FISCAL_PERIOD_ID, period.getFiscalPeriodId());
            pstmt.setLong(PARAM_JOURNAL_COMPANY_ID, period.getCompanyId());
            pstmt.setString(PARAM_JOURNAL_CREATED_BY, period.getCreatedBy());
            
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
            pstmt.setLong(PARAM_JOURNAL_ENTRY_ID, journalEntryId);
            pstmt.setLong(PARAM_ACCOUNT_ID, salaryExpenseAccountId);
            pstmt.setBigDecimal(PARAM_DEBIT_AMOUNT, totalGross);
            pstmt.setBigDecimal(PARAM_CREDIT_AMOUNT, null);
            pstmt.setString(PARAM_JOURNAL_LINE_DESCRIPTION, "Gross salaries for " + period.getPeriodName());
            pstmt.setString(PARAM_JOURNAL_REFERENCE, reference + "-0" + JOURNAL_LINE_GROSS_SALARY);
            pstmt.executeUpdate();
            
            // 2. Credit: Payroll Liabilities (Total Deductions)
            if (totalDeductions.compareTo(BigDecimal.ZERO) > 0) {
                pstmt.setLong(PARAM_JOURNAL_ENTRY_ID, journalEntryId);
                pstmt.setLong(PARAM_ACCOUNT_ID, payrollLiabilityAccountId);
                pstmt.setBigDecimal(PARAM_DEBIT_AMOUNT, null);
                pstmt.setBigDecimal(PARAM_CREDIT_AMOUNT, totalDeductions);
                pstmt.setString(PARAM_JOURNAL_LINE_DESCRIPTION, "Payroll deductions for " + period.getPeriodName());
                pstmt.setString(PARAM_JOURNAL_REFERENCE, reference + "-0" + JOURNAL_LINE_PAYROLL_DEDUCTIONS);
                pstmt.executeUpdate();
            }
            
            // 3. Credit: Bank Account (Net Pay)
            pstmt.setLong(PARAM_JOURNAL_ENTRY_ID, journalEntryId);
            pstmt.setLong(PARAM_ACCOUNT_ID, bankAccountId);
            pstmt.setBigDecimal(PARAM_DEBIT_AMOUNT, null);
            pstmt.setBigDecimal(PARAM_CREDIT_AMOUNT, totalNet);
            pstmt.setString(PARAM_JOURNAL_LINE_DESCRIPTION, "Net pay for " + period.getPeriodName());
            pstmt.setString(PARAM_JOURNAL_REFERENCE, reference + "-0" + JOURNAL_LINE_NET_PAY);
            pstmt.executeUpdate();
        }
        
        // Record the journal entry link
        String insertPayrollJournal = """
            INSERT INTO payroll_journal_entries (company_id, payroll_period_id, journal_entry_id, 
                                                entry_type, description, total_amount, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertPayrollJournal)) {
            pstmt.setLong(PARAM_PAYROLL_COMPANY_ID, period.getCompanyId());
            pstmt.setLong(PARAM_PAYROLL_PERIOD_ID, period.getId());
            pstmt.setLong(PARAM_PAYROLL_JOURNAL_ENTRY_ID, journalEntryId);
            pstmt.setString(PARAM_ENTRY_TYPE, "PAYROLL_SUMMARY");
            pstmt.setString(PARAM_PAYROLL_DESCRIPTION, description);
            pstmt.setBigDecimal(PARAM_TOTAL_AMOUNT, totalNet);
            pstmt.setString(PARAM_PAYROLL_CREATED_BY, period.getCreatedBy());
            pstmt.executeUpdate();
        }
        
        LOGGER.info("Generated journal entry " + reference + " for payroll period " + period.getPeriodName());
    }
    
    // ===== UTILITY METHODS =====
    
    private String generatePayslipNumber(Employee employee, PayrollPeriod period) {
        return period.getPayDate().format(DateTimeFormatter.ofPattern("yyyyMM")) +
               "-" + period.getId() + "-" + employee.getEmployeeNumber();
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
        // First check if payslip already exists
        String checkSql = "SELECT id FROM payslips WHERE payslip_number = ? AND payroll_period_id = ?";
        Long existingId = null;

        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, payslip.getPayslipNumber());
            checkStmt.setLong(2, payslip.getPayrollPeriodId());
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                existingId = rs.getLong("id");
                LOGGER.warning("Payslip already exists: " + payslip.getPayslipNumber() + ", updating instead");
            }
        }

        if (existingId != null) {
            // UPDATE existing payslip
            String updateSql = """
                UPDATE payslips SET
                    basic_salary = ?, gross_salary = ?, total_earnings = ?,
                    payee_tax = ?, uif_employee = ?, uif_employer = ?, sdl_levy = ?, total_deductions = ?,
                    net_pay = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                int i = 1;
                pstmt.setObject(i++, payslip.getBasicSalary());
                pstmt.setObject(i++, payslip.getGrossSalary());
                pstmt.setObject(i++, payslip.getTotalEarnings());
                pstmt.setObject(i++, payslip.getPayeeTax());
                pstmt.setObject(i++, payslip.getUifEmployee());
                pstmt.setObject(i++, payslip.getUifEmployer());
                pstmt.setObject(i++, payslip.getSdlLevy());
                pstmt.setObject(i++, payslip.getTotalDeductions());
                pstmt.setObject(i++, payslip.getNetPay());
                pstmt.setString(i++, payslip.getStatus() != null ? payslip.getStatus().name() : "GENERATED");
                pstmt.setLong(i, existingId);

                pstmt.executeUpdate();
                payslip.setId(existingId);
            }
        } else {
            // INSERT new payslip
            String insertSql = """
                INSERT INTO payslips (
                    company_id, employee_id, payroll_period_id, payslip_number,
                    basic_salary, gross_salary, total_earnings,
                    paye_tax, uif_employee, uif_employer, sdl_levy, total_deductions,
                    net_pay, status, created_by, created_at
                ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)
                RETURNING id
                """;

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                int i = 1;
                pstmt.setLong(i++, payslip.getCompanyId());
                pstmt.setLong(i++, payslip.getEmployeeId());
                pstmt.setLong(i++, payslip.getPayrollPeriodId());
                pstmt.setString(i++, payslip.getPayslipNumber());
                pstmt.setObject(i++, payslip.getBasicSalary());
                pstmt.setObject(i++, payslip.getGrossSalary());
                pstmt.setObject(i++, payslip.getTotalEarnings());
                pstmt.setObject(i++, payslip.getPayeeTax());
                pstmt.setObject(i++, payslip.getUifEmployee());
                pstmt.setObject(i++, payslip.getUifEmployer());
                pstmt.setObject(i++, payslip.getSdlLevy());
                pstmt.setObject(i++, payslip.getTotalDeductions());
                pstmt.setObject(i++, payslip.getNetPay());
                pstmt.setString(i++, payslip.getStatus() != null ? payslip.getStatus().name() : "GENERATED");
                pstmt.setString(i++, payslip.getCreatedBy());

                ResultSet rs = pstmt.executeQuery();
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
        
        // Debug: Print values from database
        System.out.println("Debug mapResultSetToPayslip: payslip_id=" + rs.getLong("id") + 
                          ", gross_salary=" + rs.getBigDecimal("gross_salary") + 
                          ", basic_salary=" + rs.getBigDecimal("basic_salary") +
                          ", total_earnings=" + rs.getBigDecimal("total_earnings") +
                          ", net_pay=" + rs.getBigDecimal("net_pay"));
        
        // Set basic salary first
        BigDecimal basicSalary = rs.getBigDecimal("basic_salary");
        payslip.setBasicSalary(basicSalary != null ? basicSalary : BigDecimal.ZERO);
        
        // Set gross salary directly from database (don't calculate)
        BigDecimal grossSalary = rs.getBigDecimal("gross_salary");
        payslip.setGrossSalary(grossSalary != null ? grossSalary : BigDecimal.ZERO);
        
        // Set allowances individually (handle nulls)
        payslip.setHousingAllowance(getBigDecimalOrZero(rs, "housing_allowance"));
        payslip.setTransportAllowance(getBigDecimalOrZero(rs, "transport_allowance"));
        payslip.setMedicalAllowance(getBigDecimalOrZero(rs, "medical_allowance"));
        payslip.setOtherAllowances(getBigDecimalOrZero(rs, "other_allowances"));
        payslip.setCommission(getBigDecimalOrZero(rs, "commission"));
        payslip.setBonus(getBigDecimalOrZero(rs, "bonus"));
        
        // Set deductions
        payslip.setPayeeTax(getBigDecimalOrZero(rs, "paye_tax"));
        payslip.setUifEmployee(getBigDecimalOrZero(rs, "uif_employee"));
        payslip.setUifEmployer(getBigDecimalOrZero(rs, "uif_employer"));
        payslip.setSdlLevy(getBigDecimalOrZero(rs, "sdl_levy"));
        payslip.setMedicalAid(getBigDecimalOrZero(rs, "medical_aid"));
        payslip.setPensionFund(getBigDecimalOrZero(rs, "pension_fund"));
        payslip.setLoanDeduction(getBigDecimalOrZero(rs, "loan_deduction"));
        payslip.setOtherDeductions(getBigDecimalOrZero(rs, "other_deductions"));
        
        // Set totals directly from database (don't recalculate)
        BigDecimal totalEarnings = rs.getBigDecimal("total_earnings");
        BigDecimal totalDeductions = rs.getBigDecimal("total_deductions");
        BigDecimal netPay = rs.getBigDecimal("net_pay");
        
        payslip.setTotalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        payslip.setTotalDeductions(totalDeductions != null ? totalDeductions : BigDecimal.ZERO);
        payslip.setNetPay(netPay != null ? netPay : BigDecimal.ZERO);
        
        payslip.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return payslip;
    }

    // Helper method to handle null BigDecimal values
    private BigDecimal getBigDecimalOrZero(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return value != null ? value : BigDecimal.ZERO;
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
     * Import employees from a tab-separated TXT file
     * @param filePath Path to the employee data file
     * @param companyId Company ID to assign employees to
     * @return Number of employees imported
     */
    public int importEmployeesFromFile(String filePath, Long companyId) {
        int importedCount = 0;
        int skippedCount = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                // Skip header line
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                
                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                try {
                    Employee employee = parseEmployeeFromLine(line, companyId);
                    if (employee != null) {
                        // Check if employee already exists
                        if (employeeExists(employee.getEmployeeNumber(), companyId)) {
                            LOGGER.info("Employee already exists, skipping: " + employee.getEmployeeNumber() + " - " + employee.getFullName());
                            skippedCount++;
                        } else {
                            createEmployee(employee);
                            importedCount++;
                            LOGGER.info("Imported employee: " + employee.getEmployeeNumber() + " - " + employee.getFullName());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warning("Failed to import employee from line: " + line.substring(0, Math.min(MAX_ERROR_MESSAGE_LENGTH, line.length())));
                    LOGGER.warning("Error: " + e.getMessage());
                }
            }
            
            LOGGER.info("Successfully imported " + importedCount + " employees from " + filePath + " (skipped " + skippedCount + " existing)");
            
        } catch (IOException e) {
            LOGGER.severe("Error reading employee file: " + e.getMessage());
            throw new RuntimeException("Failed to import employees from file", e);
        }
        
        return importedCount;
    }
    
    /**
     * Check if an employee already exists by employee number and company ID
     */
    private boolean employeeExists(String employeeNumber, Long companyId) {
        String sql = "SELECT COUNT(*) FROM employees WHERE employee_number = ? AND company_id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, employeeNumber);
            pstmt.setLong(2, companyId);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking if employee exists", e);
        }
        
        return false;
    }
    
    /**
     * Parse a single line from the employee data file into an Employee object
     */
    private Employee parseEmployeeFromLine(String line, Long companyId) {
        String[] fields = line.split("\t");
        if (fields.length < MIN_EMPLOYEE_FIELDS) {
            throw new IllegalArgumentException("Line does not have enough fields: " + fields.length);
        }
        
        Employee employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setCreatedBy("system");
        
        // Basic employee information
        employee.setEmployeeNumber(fields[FIELD_EMPLOYEE_CODE].trim()); // Employee Code
        employee.setLastName(fields[FIELD_SURNAME].trim()); // Surname
        
        // Date of Birth
        if (!fields[FIELD_DATE_OF_BIRTH].trim().isEmpty()) {
            employee.setHireDate(LocalDate.parse(fields[FIELD_DATE_OF_BIRTH].trim())); // We'll use hire date for DOB for now
        }
        
        // Date Engaged (Hire Date)
        if (!fields[FIELD_DATE_ENGAGED].trim().isEmpty()) {
            employee.setHireDate(LocalDate.parse(fields[FIELD_DATE_ENGAGED].trim()));
        }
        
        employee.setTitle(fields[FIELD_TITLE].trim()); // Title
        employee.setFirstName(fields[FIELD_FIRST_NAME].trim()); // First Name
        employee.setSecondName(fields[FIELD_SECOND_NAME].trim()); // Second Name
        
        // Email
        if (fields[FIELD_EMAIL].trim().length() > 0) {
            employee.setEmail(fields[FIELD_EMAIL].trim());
        }
        
        // Phone (use cell number)
        if (fields[FIELD_CELL_NUMBER].trim().length() > 0) {
            employee.setPhone(fields[FIELD_CELL_NUMBER].trim());
        }
        
        // Position/Job Title
        if (fields[FIELD_JOB_TITLE].trim().length() > 0) {
            employee.setPosition(fields[FIELD_JOB_TITLE].trim());
        } else {
            employee.setPosition("Educator"); // Default position
        }
        
        employee.setDepartment("Education"); // Default department
        
        // Tax Number (ID Number)
        if (fields[FIELD_ID_NUMBER].trim().length() > 0) {
            employee.setTaxNumber(fields[FIELD_ID_NUMBER].trim());
        }
        
        // Basic Salary (Fixed Salary) - convert from string with comma decimal separator
        if (fields[FIELD_FIXED_SALARY].trim().length() > 0) {
            String salaryStr = fields[FIELD_FIXED_SALARY].trim().replace(",", ".");
            try {
                BigDecimal salary = new BigDecimal(salaryStr);
                employee.setBasicSalary(salary);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid salary format: " + salaryStr + " for employee " + employee.getEmployeeNumber());
                employee.setBasicSalary(BigDecimal.ZERO);
            }
        } else {
            employee.setBasicSalary(BigDecimal.ZERO);
        }
        
        // Address Information (use residential address)
        StringBuilder address = new StringBuilder();
        if (fields[FIELD_UNIT_NUMBER].trim().length() > 0) address.append(fields[FIELD_UNIT_NUMBER].trim()).append(" "); // Unit Number
        if (fields[FIELD_COMPLEX_NAME].trim().length() > 0) address.append(fields[FIELD_COMPLEX_NAME].trim()).append(" "); // Complex Name
        if (fields[FIELD_STREET_NUMBER].trim().length() > 0) address.append(fields[FIELD_STREET_NUMBER].trim()).append(" "); // Street Number
        if (fields[FIELD_STREET_NAME].trim().length() > 0) address.append(fields[FIELD_STREET_NAME].trim()).append(" "); // Street/Farm Name
        if (fields[FIELD_SUBURB].trim().length() > 0) address.append(fields[FIELD_SUBURB].trim()).append(" "); // Suburb/District
        employee.setAddressLine1(address.toString().trim());
        employee.setCity(fields[FIELD_CITY].trim()); // City/Town
        employee.setPostalCode(fields[FIELD_POSTAL_CODE].trim()); // Postal Code
        employee.setCountry("ZA"); // Default to South Africa
        
        return employee;
    }
    
    /**
     * Clean up all processed payslips and reset payroll periods for testing
     * WARNING: This will delete all payslip data and reset payroll processing status
     */
    public void cleanupAllPayslipsForTesting() {
        String deletePayslips = "DELETE FROM payslips";
        String deletePayrollJournalEntries = "DELETE FROM payroll_journal_entries";
        String resetPayrollPeriods = "UPDATE payroll_periods SET status = 'CREATED', processed_at = NULL, processed_by = NULL, total_gross_pay = NULL, total_deductions = NULL, total_net_pay = NULL, employee_count = NULL";
        
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(false);
            
            try (PreparedStatement stmt1 = conn.prepareStatement(deletePayslips);
                 PreparedStatement stmt2 = conn.prepareStatement(deletePayrollJournalEntries);
                 PreparedStatement stmt3 = conn.prepareStatement(resetPayrollPeriods)) {
                
                int payslipsDeleted = stmt1.executeUpdate();
                int journalEntriesDeleted = stmt2.executeUpdate();
                int periodsReset = stmt3.executeUpdate();
                
                conn.commit();
                
                System.out.println("✅ Cleanup completed:");
                System.out.println("   - Deleted " + payslipsDeleted + " payslips");
                System.out.println("   - Deleted " + journalEntriesDeleted + " payroll journal entries");
                System.out.println("   - Reset " + periodsReset + " payroll periods to CREATED status");
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during payslip cleanup", e);
            throw new RuntimeException("Failed to cleanup payslips: " + e.getMessage());
        }
    }
}