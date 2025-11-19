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

import fin.model.ManualInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for manual invoice operations
 */
@Repository
public interface ManualInvoiceRepository extends JpaRepository<ManualInvoice, Long> {

    List<ManualInvoice> findByCompanyIdOrderByInvoiceDateDesc(Long companyId);

    List<ManualInvoice> findByCompanyIdAndInvoiceDateBetweenOrderByInvoiceDateDesc(
        Long companyId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT mi FROM ManualInvoice mi WHERE mi.companyId = :companyId AND mi.invoiceNumber = :invoiceNumber")
    ManualInvoice findByCompanyIdAndInvoiceNumber(@Param("companyId") Long companyId, @Param("invoiceNumber") String invoiceNumber);

    @Query("SELECT COUNT(mi) FROM ManualInvoice mi WHERE mi.companyId = :companyId AND mi.fiscalPeriodId = :fiscalPeriodId")
    long countByCompanyIdAndFiscalPeriodId(@Param("companyId") Long companyId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    boolean existsByCompanyIdAndInvoiceNumber(Long companyId, String invoiceNumber);

    @Query("SELECT mi FROM ManualInvoice mi WHERE mi.companyId = :companyId AND mi.id NOT IN (SELECT jel.manualInvoiceId FROM JournalEntryLine jel WHERE jel.manualInvoiceId IS NOT NULL)")
    List<ManualInvoice> findInvoicesWithoutJournalEntries(@Param("companyId") Long companyId);

    /**
     * Find manual invoices by company ID
     */
    List<ManualInvoice> findByCompanyId(Long companyId);

    /**
     * Delete manual invoices by company ID
     */
    void deleteByCompanyId(Long companyId);
}