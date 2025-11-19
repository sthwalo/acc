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

import fin.model.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA repository for PayrollPeriod entity
 */
@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    /**
     * Find all payroll periods for a company
     */
    List<PayrollPeriod> findByCompanyId(Integer companyId);

    /**
     * Find payroll periods by company ordered by start date descending
     */
    List<PayrollPeriod> findByCompanyIdOrderByStartDateDesc(Integer companyId);

    /**
     * Find payroll periods that contain a specific date
     */
    @Query("SELECT p FROM PayrollPeriod p WHERE p.companyId = :companyId AND p.startDate <= :date AND p.endDate >= :date")
    List<PayrollPeriod> findByCompanyIdAndDateRange(@Param("companyId") Integer companyId, @Param("date") LocalDate date);

    /**
     * Find open payroll periods for a company
     */
    List<PayrollPeriod> findByCompanyIdAndStatus(Integer companyId, PayrollPeriod.PayrollStatus status);

    /**
     * Count payroll periods by company and status
     */
    long countByCompanyIdAndStatus(Integer companyId, PayrollPeriod.PayrollStatus status);

    /**
     * Find payroll periods by fiscal period
     */
    List<PayrollPeriod> findByCompanyIdAndFiscalPeriodId(Integer companyId, Integer fiscalPeriodId);

    /**
     * Check if payroll period exists with overlapping dates
     */
    @Query("SELECT COUNT(p) > 0 FROM PayrollPeriod p WHERE p.companyId = :companyId AND " +
           "((p.startDate <= :endDate AND p.endDate >= :startDate)) AND p.id != :excludeId")
    boolean existsOverlappingPeriod(@Param("companyId") Integer companyId,
                                   @Param("startDate") LocalDate startDate,
                                   @Param("endDate") LocalDate endDate,
                                   @Param("excludeId") Long excludeId);

    /**
     * Find payroll periods that can be processed (OPEN status)
     */
    @Query("SELECT p FROM PayrollPeriod p WHERE p.companyId = :companyId AND p.status = 'OPEN' ORDER BY p.startDate DESC")
    List<PayrollPeriod> findProcessablePeriods(@Param("companyId") Integer companyId);
}