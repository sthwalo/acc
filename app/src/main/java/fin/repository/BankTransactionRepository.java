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

import fin.entity.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for BankTransaction entity.
 * Provides database operations for bank transactions with Spring Data JPA.
 */
@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    /**
     * Find all transactions for a specific company and fiscal period
     */
    List<BankTransaction> findByCompanyIdAndFiscalPeriodId(Long companyId, Long fiscalPeriodId);

    /**
     * Find all transactions for a specific company and fiscal period with pagination
     */
    org.springframework.data.domain.Page<BankTransaction> findByCompanyIdAndFiscalPeriodId(Long companyId, Long fiscalPeriodId, org.springframework.data.domain.Pageable pageable);

    /**
     * Find all transactions for a specific company
     */
    List<BankTransaction> findByCompanyId(Long companyId);

    /**
     * Find transactions within a date range
     */
    List<BankTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions for a company within a date range
     */
    List<BankTransaction> findByCompanyIdAndTransactionDateBetween(Long companyId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by account number
     */
    List<BankTransaction> findByAccountNumber(String accountNumber);

    /**
     * Find transactions by source file
     */
    List<BankTransaction> findBySourceFile(String sourceFile);

    /**
     * Find service fee transactions for a company
     */
    List<BankTransaction> findByCompanyIdAndServiceFee(Long companyId, boolean serviceFee);

    /**
     * Count transactions for a company
     */
    long countByCompanyId(Long companyId);

    /**
     * Count transactions for a company and fiscal period
     */
    long countByCompanyIdAndFiscalPeriodId(Long companyId, Long fiscalPeriodId);

    /**
     * Find transactions by account code (for classification)
     */
    List<BankTransaction> findByAccountCode(String accountCode);

    /**
     * Find unclassified transactions (no account code assigned)
     */
    @Query("SELECT t FROM BankTransaction t WHERE t.companyId = :companyId AND t.accountCode IS NULL")
    List<BankTransaction> findUnclassifiedTransactions(@Param("companyId") Long companyId);

    /**
     * Find transactions with debit amounts greater than specified value
     */
    @Query("SELECT t FROM BankTransaction t WHERE t.debitAmount > :amount ORDER BY t.debitAmount DESC")
    List<BankTransaction> findHighValueDebitTransactions(@Param("amount") java.math.BigDecimal amount);

    /**
     * Find transactions with credit amounts greater than specified value
     */
    @Query("SELECT t FROM BankTransaction t WHERE t.creditAmount > :amount ORDER BY t.creditAmount DESC")
    List<BankTransaction> findHighValueCreditTransactions(@Param("amount") java.math.BigDecimal amount);

    /**
     * Find transactions by company and description containing text
     */
    List<BankTransaction> findByCompanyIdAndDescriptionContainingIgnoreCase(Long companyId, String description);

    /**
     * Get total debit amount for a company and fiscal period
     */
    @Query("SELECT COALESCE(SUM(t.debitAmount), 0) FROM BankTransaction t WHERE t.companyId = :companyId AND t.fiscalPeriodId = :fiscalPeriodId")
    java.math.BigDecimal getTotalDebitAmount(@Param("companyId") Long companyId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Get total credit amount for a company and fiscal period
     */
    @Query("SELECT COALESCE(SUM(t.creditAmount), 0) FROM BankTransaction t WHERE t.companyId = :companyId AND t.fiscalPeriodId = :fiscalPeriodId")
    java.math.BigDecimal getTotalCreditAmount(@Param("companyId") Long companyId, @Param("fiscalPeriodId") Long fiscalPeriodId);

    /**
     * Find transactions by company ID and bank account ID is null
     */
    List<BankTransaction> findByCompanyIdAndBankAccountIdIsNull(Long companyId);

    /**
     * Find unclassified transactions for a company and fiscal period (account code is null)
     */
    List<BankTransaction> findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(Long companyId, Long fiscalPeriodId);

    /**
     * Find classified transactions that don't have journal entries yet
     */
    @Query("SELECT t FROM BankTransaction t WHERE t.companyId = :companyId AND t.accountCode IS NOT NULL AND t.id NOT IN (SELECT DISTINCT jel.sourceTransactionId FROM JournalEntryLine jel WHERE jel.sourceTransactionId IS NOT NULL)")
    List<BankTransaction> findClassifiedTransactionsWithoutJournalEntries(@Param("companyId") Long companyId);

    /**
     * Check if transaction exists (duplicate detection).
     * Uses 5-field matching: company_id, transaction_date, debit_amount, 
     * credit_amount, description (case-insensitive), balance.
     * 
     * @param companyId Company ID
     * @param transactionDate Transaction date
     * @param debitAmount Debit amount
     * @param creditAmount Credit amount
     * @param description Transaction description (case-insensitive match)
     * @param balance Running balance after transaction
     * @return true if transaction exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM BankTransaction t " +
           "WHERE t.companyId = :companyId " +
           "AND t.transactionDate = :transactionDate " +
           "AND t.debitAmount = :debitAmount " +
           "AND t.creditAmount = :creditAmount " +
           "AND LOWER(t.description) = LOWER(:description) " +
           "AND t.balance = :balance")
    boolean existsByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
        @Param("companyId") Long companyId,
        @Param("transactionDate") LocalDate transactionDate,
        @Param("debitAmount") java.math.BigDecimal debitAmount,
        @Param("creditAmount") java.math.BigDecimal creditAmount,
        @Param("description") String description,
        @Param("balance") java.math.BigDecimal balance
    );

    /**
     * Find duplicate transaction (for reporting).
     * Uses same 5-field matching as existsByCompanyIdAnd... method.
     * 
     * @param companyId Company ID
     * @param transactionDate Transaction date
     * @param debitAmount Debit amount
     * @param creditAmount Credit amount
     * @param description Transaction description (case-insensitive match)
     * @param balance Running balance after transaction
     * @return Existing transaction if found, empty Optional otherwise
     */
    @Query("SELECT t FROM BankTransaction t " +
           "WHERE t.companyId = :companyId " +
           "AND t.transactionDate = :transactionDate " +
           "AND t.debitAmount = :debitAmount " +
           "AND t.creditAmount = :creditAmount " +
           "AND LOWER(t.description) = LOWER(:description) " +
           "AND t.balance = :balance")
    java.util.Optional<BankTransaction> findByCompanyIdAndTransactionDateAndAmountsAndDescriptionAndBalance(
        @Param("companyId") Long companyId,
        @Param("transactionDate") LocalDate transactionDate,
        @Param("debitAmount") java.math.BigDecimal debitAmount,
        @Param("creditAmount") java.math.BigDecimal creditAmount,
        @Param("description") String description,
        @Param("balance") java.math.BigDecimal balance
    );

    void deleteByCompanyId(Long companyId);
}