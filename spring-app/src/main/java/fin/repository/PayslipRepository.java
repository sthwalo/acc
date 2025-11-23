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
     * Find payslips for a specific fiscal period
     */
    List<Payslip> findByFiscalPeriodId(Long fiscalPeriodId);

    /**
     * Find payslips for a specific employee
     */
    List<Payslip> findByEmployeeId(Long employeeId);

    /**
     * Find payslips by fiscal period and list of employee IDs
     */
    List<Payslip> findByFiscalPeriodIdAndEmployeeIdIn(Long fiscalPeriodId, List<Long> employeeIds);

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
     * Find payslips by fiscal period and status
     */
    List<Payslip> findByFiscalPeriodIdAndStatus(Long fiscalPeriodId, Payslip.PayslipStatus status);

    /**
     * Count payslips by fiscal period
     */
    long countByFiscalPeriodId(Long fiscalPeriodId);

    /**
     * Count payslips by fiscal period and status
     */
    long countByFiscalPeriodIdAndStatus(Long fiscalPeriodId, Payslip.PayslipStatus status);

    /**
     * Sum total net pay for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.netPay), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumNetPayByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum total gross pay for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.totalEarnings), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumGrossPayByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum total deductions for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.totalDeductions), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumDeductionsByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum PAYE tax for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.payeTax), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumPayeeTaxByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum UIF employee contributions for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.uifEmployee), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumUifEmployeeByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum UIF employer contributions for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.uifEmployer), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumUifEmployerByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Sum SDL levy for a fiscal period
     */
    @Query("SELECT COALESCE(SUM(p.sdlLevy), 0) FROM Payslip p WHERE p.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumSdlLevyByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Find payslips that need to be exported (APPROVED status)
     */
    @Query("SELECT p FROM Payslip p WHERE p.companyId = :companyId AND p.status = 'APPROVED' ORDER BY p.fiscalPeriodId, p.employeeId")
    List<Payslip> findApprovedPayslipsForExport(@Param("companyId") Integer companyId);

    /**
     * Check if payslip exists for employee and fiscal period
     */
    boolean existsByEmployeeIdAndFiscalPeriodId(Long employeeId, Long fiscalPeriodId);
}