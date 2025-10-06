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
    
    public PayrollService() {
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.sarsTaxCalculator = new SARSTaxCalculator();
        this.companyRepository = null;
        this.pdfService = null;
        this.emailService = null;
        initializeTaxCalculator();
    }
    
    public PayrollService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.sarsTaxCalculator = new SARSTaxCalculator();
        this.companyRepository = null;
        this.pdfService = null;
        this.emailService = null;
        initializeTaxCalculator();
    }

    public PayrollService(String dbUrl, CompanyRepository companyRepository, 
                         PayslipPdfService pdfService, EmailService emailService) {
        this.dbUrl = dbUrl;
        this.sarsTaxCalculator = new SARSTaxCalculator();
        this.companyRepository = companyRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
        initializeTaxCalculator();
    }
    
    private void initializeTaxCalculator() {
        // Skip tax calculator initialization in test environment
        if (isTestEnvironment()) {
            System.out.println("🧪 Skipping SARS tax calculator initialization in test environment");
            return;
        }
        
        try {
            // Load tax tables from the PDF text file for accurate SARS 2026 calculations
            String pdfTextPath = "input/PAYE-GEN-01-G01-A03-2026-Monthly-Tax-Deduction-Tables-External-Annexure.txt";
            sarsTaxCalculator.loadTaxTablesFromPDFText(pdfTextPath);
            System.out.println("✅ SARS Tax Calculator initialized with official 2026 tables");
        } catch (IOException e) {
            System.err.println("❌ Failed to load SARS tax tables: " + e.getMessage());
            throw new RuntimeException("Tax calculator initialization failed", e);
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
                
            } catch (Exception e) {
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
            pstmt.setInt(3, month);
            
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
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
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
                    LOGGER.warning("Failed to import employee from line: " + line.substring(0, Math.min(100, line.length())));
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
        if (fields.length < 40) {
            throw new IllegalArgumentException("Line does not have enough fields: " + fields.length);
        }
        
        Employee employee = new Employee();
        employee.setCompanyId(companyId);
        employee.setCreatedBy("system");
        
        // Basic employee information
        employee.setEmployeeNumber(fields[0].trim()); // Employee Code
        employee.setLastName(fields[1].trim()); // Surname
        
        // Date of Birth
        if (!fields[2].trim().isEmpty()) {
            employee.setHireDate(LocalDate.parse(fields[2].trim())); // We'll use hire date for DOB for now
        }
        
        // Date Engaged (Hire Date)
        if (!fields[3].trim().isEmpty()) {
            employee.setHireDate(LocalDate.parse(fields[3].trim()));
        }
        
        employee.setTitle(fields[4].trim()); // Title
        employee.setFirstName(fields[6].trim()); // First Name
        employee.setSecondName(fields[7].trim()); // Second Name
        
        // Email
        if (fields[24].trim().length() > 0) {
            employee.setEmail(fields[24].trim());
        }
        
        // Phone (use cell number)
        if (fields[22].trim().length() > 0) {
            employee.setPhone(fields[22].trim());
        }
        
        // Position/Job Title
        if (fields[28].trim().length() > 0) {
            employee.setPosition(fields[28].trim());
        } else {
            employee.setPosition("Educator"); // Default position
        }
        
        employee.setDepartment("Education"); // Default department
        
        // Tax Number (ID Number)
        if (fields[9].trim().length() > 0) {
            employee.setTaxNumber(fields[9].trim());
        }
        
        // Basic Salary (Fixed Salary) - convert from string with comma decimal separator
        if (fields[39].trim().length() > 0) {
            String salaryStr = fields[39].trim().replace(",", ".");
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
        if (fields[15].trim().length() > 0) address.append(fields[15].trim()).append(" "); // Unit Number
        if (fields[16].trim().length() > 0) address.append(fields[16].trim()).append(" "); // Complex Name
        if (fields[17].trim().length() > 0) address.append(fields[17].trim()).append(" "); // Street Number
        if (fields[18].trim().length() > 0) address.append(fields[18].trim()).append(" "); // Street/Farm Name
        if (fields[19].trim().length() > 0) address.append(fields[19].trim()).append(" "); // Suburb/District
        employee.setAddressLine1(address.toString().trim());
        employee.setCity(fields[20].trim()); // City/Town
        employee.setPostalCode(fields[21].trim()); // Postal Code
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