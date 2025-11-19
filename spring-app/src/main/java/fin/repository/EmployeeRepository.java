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

import fin.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Employee entity
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Find all active employees for a company
     */
    List<Employee> findByCompanyIdAndIsActiveTrue(Long companyId);

    /**
     * Find all employees for a company (active and inactive)
     */
    List<Employee> findByCompanyId(Long companyId);

    /**
     * Find employee by employee number and company
     */
    Employee findByEmployeeNumberAndCompanyId(String employeeNumber, Long companyId);

    /**
     * Find employees by department
     */
    List<Employee> findByCompanyIdAndDepartment(Long companyId, String department);

    /**
     * Find employees by position
     */
    List<Employee> findByCompanyIdAndPosition(Long companyId, String position);

    /**
     * Count active employees for a company
     */
    long countByCompanyIdAndIsActiveTrue(Long companyId);

    /**
     * Check if employee number exists for company
     */
    boolean existsByEmployeeNumberAndCompanyId(String employeeNumber, Long companyId);

    /**
     * Find employees with tax numbers
     */
    @Query("SELECT e FROM Employee e WHERE e.companyId = :companyId AND e.isActive = true AND e.taxNumber IS NOT NULL")
    List<Employee> findActiveEmployeesWithTaxNumbers(@Param("companyId") Long companyId);

    /**
     * Find employees with UIF numbers
     */
    @Query("SELECT e FROM Employee e WHERE e.companyId = :companyId AND e.isActive = true AND e.uifNumber IS NOT NULL")
    List<Employee> findActiveEmployeesWithUifNumbers(@Param("companyId") Long companyId);

    /**
     * Check if employee number exists for company
     */
    boolean existsByCompanyIdAndEmployeeNumber(Long companyId, String employeeNumber);
}