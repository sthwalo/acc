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
 */

package fin.repository;

import fin.model.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Data JPA repository for Payslip entity
 */
@Repository
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    /**
     * Find all payslips for a company
     */
    List<Payslip> findByCompanyId(Integer companyId);

    /**
     * Find payslips for a specific payroll period
     */
    List<Payslip> findByPayrollPeriodId(Long payrollPeriodId);

    /**
     * Find payslips for a specific employee
     */
    List<Payslip> findByEmployeeId(Long employeeId);

    /**
     * Find payslips for a company and employee
     */
    List<Payslip> findByCompanyIdAndEmployeeId(Integer companyId, Long employeeId);

    /**
     * Find payslips by status
     */
    List<Payslip> findByStatus(Payslip.PayslipStatus status);

    /**
     * Find payslips by company and status
     */
    List<Payslip> findByCompanyIdAndStatus(Integer companyId, Payslip.PayslipStatus status);

    /**
     * Find payslips by payroll period and status
     */
    List<Payslip> findByPayrollPeriodIdAndStatus(Long payrollPeriodId, Payslip.PayslipStatus status);

    /**
     * Count payslips by payroll period
     */
    long countByPayrollPeriodId(Long payrollPeriodId);

    /**
     * Count payslips by payroll period and status
     */
    long countByPayrollPeriodIdAndStatus(Long payrollPeriodId, Payslip.PayslipStatus status);

    /**
     * Sum total net pay for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.netPay), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumNetPayByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum total gross pay for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.totalEarnings), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumGrossPayByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum total deductions for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.totalDeductions), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumDeductionsByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum PAYE tax for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.payeeTax), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumPayeeTaxByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum UIF employee contributions for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.uifEmployee), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumUifEmployeeByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum UIF employer contributions for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.uifEmployer), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumUifEmployerByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Sum SDL levy for a payroll period
     */
    @Query("SELECT COALESCE(SUM(p.sdlLevy), 0) FROM Payslip p WHERE p.payrollPeriodId = :payrollPeriodId")
    BigDecimal sumSdlLevyByPayrollPeriodId(@Param("payrollPeriodId") Long payrollPeriodId);

    /**
     * Find payslips that need to be exported (APPROVED status)
     */
    @Query("SELECT p FROM Payslip p WHERE p.companyId = :companyId AND p.status = 'APPROVED' ORDER BY p.payrollPeriodId, p.employeeId")
    List<Payslip> findApprovedPayslipsForExport(@Param("companyId") Integer companyId);

    /**
     * Check if payslip exists for employee and payroll period
     */
    boolean existsByEmployeeIdAndPayrollPeriodId(Long employeeId, Long payrollPeriodId);
}