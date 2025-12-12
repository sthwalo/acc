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

package fin.repository;

import fin.entity.JournalEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for journal entry operations
 */
@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long> {

    List<JournalEntry> findByCompanyIdOrderByEntryDateDesc(Long companyId);

    List<JournalEntry> findByCompanyIdAndFiscalPeriodIdOrderByEntryDateDesc(Long companyId, Long fiscalPeriodId);

    List<JournalEntry> findByCompanyIdAndEntryDateBetweenOrderByEntryDateDesc(
        Long companyId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT je FROM JournalEntry je WHERE je.companyId = :companyId AND je.reference = :reference")
    JournalEntry findByCompanyIdAndReference(@Param("companyId") Long companyId, @Param("reference") String reference);

    @Query("SELECT COUNT(je) FROM JournalEntry je WHERE je.companyId = :companyId AND je.fiscalPeriodId = :fiscalPeriodId")
    long countByCompanyIdAndFiscalPeriodId(@Param("companyId") Long companyId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    List<JournalEntry> findByCompanyIdAndFiscalPeriodIdOrderByEntryDateAscIdAsc(Long companyId, Long fiscalPeriodId);

    /**
     * Find journal entries by company ID
     */
    List<JournalEntry> findByCompanyId(Long companyId);

    /**
     * Find journal entries by fiscal period ID
     */
    List<JournalEntry> findByFiscalPeriodId(Long fiscalPeriodId);

    /**
     * Delete journal entries by company ID
     */
    void deleteByCompanyId(Long companyId);

    // ============================================================================
    // TASK_007: Pagination support for Audit Trail
    // ============================================================================

    /**
     * Find journal entries by company and fiscal period with pagination.
     * Results are sorted by entry date descending, then by ID descending.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param pageable Pagination information (page number, page size, sort)
     * @return Page of journal entries
     */
    @Query("SELECT je FROM JournalEntry je WHERE je.companyId = :companyId " +
           "AND je.fiscalPeriodId = :fiscalPeriodId " +
           "ORDER BY je.entryDate DESC, je.id DESC")
    Page<JournalEntry> findByCompanyIdAndFiscalPeriodIdPaginated(
        @Param("companyId") Long companyId,
        @Param("fiscalPeriodId") Long fiscalPeriodId,
        Pageable pageable);

    /**
     * Find journal entries by company, fiscal period, and date range with pagination.
     * Results are sorted by entry date descending, then by ID descending.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param pageable Pagination information (page number, page size, sort)
     * @return Page of journal entries within date range
     */
    @Query("SELECT je FROM JournalEntry je WHERE je.companyId = :companyId " +
           "AND je.fiscalPeriodId = :fiscalPeriodId " +
           "AND je.entryDate BETWEEN :startDate AND :endDate " +
           "ORDER BY je.entryDate DESC, je.id DESC")
    Page<JournalEntry> findByCompanyIdAndFiscalPeriodIdAndDateRange(
        @Param("companyId") Long companyId,
        @Param("fiscalPeriodId") Long fiscalPeriodId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        Pageable pageable);

    /**
     * Find journal entries by company, fiscal period, and search term with pagination.
     * Searches in description and reference fields (case-insensitive).
     * Results are sorted by entry date descending, then by ID descending.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param searchTerm Search term to match in description or reference
     * @param pageable Pagination information (page number, page size, sort)
     * @return Page of journal entries matching search term
     */
    @Query("SELECT je FROM JournalEntry je WHERE je.companyId = :companyId " +
           "AND je.fiscalPeriodId = :fiscalPeriodId " +
           "AND (LOWER(je.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(je.reference) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY je.entryDate DESC, je.id DESC")
    Page<JournalEntry> findByCompanyIdAndFiscalPeriodIdAndSearchTerm(
        @Param("companyId") Long companyId,
        @Param("fiscalPeriodId") Long fiscalPeriodId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);

    /**
     * Find journal entries by company, fiscal period, date range, and search term with pagination.
     * Combines all filters: date range and search term.
     * Results are sorted by entry date descending, then by ID descending.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @param searchTerm Search term to match in description or reference
     * @param pageable Pagination information (page number, page size, sort)
     * @return Page of journal entries matching all filters
     */
    @Query("SELECT je FROM JournalEntry je WHERE je.companyId = :companyId " +
           "AND je.fiscalPeriodId = :fiscalPeriodId " +
           "AND je.entryDate BETWEEN :startDate AND :endDate " +
           "AND (LOWER(je.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "     OR LOWER(je.reference) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY je.entryDate DESC, je.id DESC")
    Page<JournalEntry> findByCompanyIdAndFiscalPeriodIdAndDateRangeAndSearchTerm(
        @Param("companyId") Long companyId,
        @Param("fiscalPeriodId") Long fiscalPeriodId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("searchTerm") String searchTerm,
        Pageable pageable);
}