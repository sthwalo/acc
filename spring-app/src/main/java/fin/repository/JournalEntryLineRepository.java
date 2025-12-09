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

import fin.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for journal entry line operations
 */
@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, Long> {

    List<JournalEntryLine> findByJournalEntryIdOrderByLineNumber(Long journalEntryId);

    List<JournalEntryLine> findByAccountIdOrderByJournalEntry_EntryDateDesc(Long accountId);

    @Query("SELECT jel FROM JournalEntryLine jel WHERE jel.journalEntry.companyId = :companyId ORDER BY jel.journalEntry.entryDate DESC, jel.lineNumber ASC")
    List<JournalEntryLine> findByCompanyIdOrderByEntryDateDesc(@Param("companyId") Long companyId);

    @Query("SELECT jel FROM JournalEntryLine jel WHERE jel.journalEntry.companyId = :companyId AND jel.accountId = :accountId ORDER BY jel.journalEntry.entryDate DESC")
    List<JournalEntryLine> findByCompanyIdAndAccountIdOrderByEntryDateDesc(@Param("companyId") Long companyId, @Param("accountId") Long accountId);

    @Query("SELECT SUM(jel.debitAmount) FROM JournalEntryLine jel WHERE jel.accountId = :accountId AND jel.journalEntry.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumDebitByAccountIdAndFiscalPeriodId(@Param("accountId") Long accountId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    @Query("SELECT SUM(jel.creditAmount) FROM JournalEntryLine jel WHERE jel.accountId = :accountId AND jel.journalEntry.fiscalPeriodId = :fiscalPeriodId")
    BigDecimal sumCreditByAccountIdAndFiscalPeriodId(@Param("accountId") Long accountId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    @Query("SELECT jel FROM JournalEntryLine jel WHERE jel.accountId = :accountId AND jel.journalEntry.fiscalPeriodId = :fiscalPeriodId")
    List<JournalEntryLine> findByAccountIdAndJournalEntry_FiscalPeriodId(@Param("accountId") Long accountId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    @Query("SELECT jel FROM JournalEntryLine jel WHERE jel.accountId = :accountId AND jel.journalEntry.fiscalPeriodId = :fiscalPeriodId ORDER BY jel.journalEntry.entryDate")
    List<JournalEntryLine> findByAccountIdAndJournalEntry_FiscalPeriodIdOrderByJournalEntry_EntryDate(@Param("accountId") Long accountId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    @Query("SELECT COUNT(jel) FROM JournalEntryLine jel WHERE jel.journalEntryId = :journalEntryId")
    long countByJournalEntryId(@Param("journalEntryId") Long journalEntryId);

    /**
     * Find journal entry lines by journal entry ID
     */
    List<JournalEntryLine> findByJournalEntryId(Long journalEntryId);

    /**
     * Delete journal entry lines by journal entry ID
     */
    void deleteByJournalEntryId(Long journalEntryId);

    /**
     * Find journal entry lines by source transaction ID
     */
    List<JournalEntryLine> findBySourceTransactionId(Long sourceTransactionId);
}