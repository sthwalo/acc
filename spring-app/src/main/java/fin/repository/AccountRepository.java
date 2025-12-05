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

import fin.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Account entities
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * Find accounts by company ID
     */
    List<Account> findByCompanyId(Long companyId);

    /**
     * Find accounts by company ID ordered by account code
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId ORDER BY a.accountCode ASC")
    List<Account> findByCompanyIdOrderByAccountCodeAsc(@Param("companyId") Long companyId);

    /**
     * Find account by company ID and account code
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND a.accountCode = :accountCode")
    Optional<Account> findByCompanyIdAndAccountCode(@Param("companyId") Long companyId, @Param("accountCode") String accountCode);

    /**
     * Find accounts by company ID and category ID
     */
    List<Account> findByCompanyIdAndCategoryId(Long companyId, Long categoryId);

    /**
     * Find active accounts by company ID
     */
    List<Account> findByCompanyIdAndIsActiveTrue(Long companyId);

    /**
     * Find accounts by company ID and account type
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND " +
           "((:accountType = 'ASSET' AND a.accountCode LIKE '1%') OR " +
           "(:accountType = 'ASSET' AND a.accountCode LIKE '2%') OR " +
           "(:accountType = 'LIABILITY' AND a.accountCode LIKE '3%') OR " +
           "(:accountType = 'LIABILITY' AND a.accountCode LIKE '4%') OR " +
           "(:accountType = 'EQUITY' AND a.accountCode LIKE '5%') OR " +
           "(:accountType = 'INCOME' AND a.accountCode LIKE '6%') OR " +
           "(:accountType = 'INCOME' AND a.accountCode LIKE '7%') OR " +
           "(:accountType = 'EXPENSE' AND a.accountCode LIKE '8%') OR " +
           "(:accountType = 'EXPENSE' AND a.accountCode LIKE '9%'))")
    List<Account> findByCompanyIdAndAccountType(@Param("companyId") Long companyId, @Param("accountType") String accountType);

    /**
     * Find asset accounts by company ID
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND (a.accountCode LIKE '1%' OR a.accountCode LIKE '2%') AND a.isActive = true")
    List<Account> findAssetAccountsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find liability accounts by company ID
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND (a.accountCode LIKE '3%' OR a.accountCode LIKE '4%') AND a.isActive = true")
    List<Account> findLiabilityAccountsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find equity accounts by company ID
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND a.accountCode LIKE '5%' AND a.isActive = true")
    List<Account> findEquityAccountsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find income accounts by company ID
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND (a.accountCode LIKE '6%' OR a.accountCode LIKE '7%') AND a.isActive = true")
    List<Account> findIncomeAccountsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Find expense accounts by company ID
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND (a.accountCode LIKE '8%' OR a.accountCode LIKE '9%') AND a.isActive = true")
    List<Account> findExpenseAccountsByCompanyId(@Param("companyId") Long companyId);

    /**
     * Count active accounts by company ID
     */
    long countByCompanyIdAndIsActiveTrue(Long companyId);

    /**
     * Check if account code exists for company
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a WHERE a.companyId = :companyId AND a.accountCode = :accountCode")
    boolean existsByCompanyIdAndAccountCode(@Param("companyId") Long companyId, @Param("accountCode") String accountCode);

    /**
     * Find accounts by parent account ID
     */
    List<Account> findByParentAccountId(Long parentAccountId);

    /**
     * Find root accounts (no parent) by company ID
     */
    List<Account> findByCompanyIdAndParentAccountIdIsNull(Long companyId);

    /**
     * Find accounts by company ID and account code starting with prefix
     */
    @Query("SELECT a FROM Account a WHERE a.companyId = :companyId AND a.accountCode LIKE CONCAT(:accountCodePrefix, '%')")
    List<Account> findByCompanyIdAndAccountCodeStartingWith(@Param("companyId") Long companyId, @Param("accountCodePrefix") String accountCodePrefix);

    /**
     * Delete accounts by company ID
     */
    void deleteByCompanyId(Long companyId);
}