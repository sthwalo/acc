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

package fin.service.transaction;

import fin.entity.BankTransaction;
import fin.repository.BankTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Spring Service for Transaction operations
 * Provides business logic for managing bank transactions
 */
@Service
public class TransactionService {

    private final BankTransactionRepository bankTransactionRepository;

    public TransactionService(BankTransactionRepository bankTransactionRepository) {
        this.bankTransactionRepository = bankTransactionRepository;
    }

    /**
     * Get all transactions for a company and fiscal period
     */
    public List<BankTransaction> getTransactionsByCompanyAndFiscalPeriod(Long companyId, Long fiscalPeriodId) {
        return bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
    }

    /**
     * Get paginated transactions for a company and fiscal period
     */
    public Page<BankTransaction> getTransactionsByCompanyAndFiscalPeriod(Long companyId, Long fiscalPeriodId, Pageable pageable) {
        return bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId, pageable);
    }

    /**
     * Get transaction by ID
     */
    public BankTransaction getTransactionById(Long id) {
        return bankTransactionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }

    /**
     * Get all transactions for a company
     */
    public List<BankTransaction> getTransactionsByCompany(Long companyId) {
        return bankTransactionRepository.findByCompanyId(companyId);
    }

    /**
     * Count transactions for a company and fiscal period
     */
    public long countTransactionsByCompanyAndFiscalPeriod(Long companyId, Long fiscalPeriodId) {
        return bankTransactionRepository.countByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
    }

    /**
     * Get total debit amount for a company and fiscal period
     */
    public BigDecimal getTotalDebitAmount(Long companyId, Long fiscalPeriodId) {
        return bankTransactionRepository.getTotalDebitAmount(companyId, fiscalPeriodId);
    }

    /**
     * Get total credit amount for a company and fiscal period
     */
    public BigDecimal getTotalCreditAmount(Long companyId, Long fiscalPeriodId) {
        return bankTransactionRepository.getTotalCreditAmount(companyId, fiscalPeriodId);
    }

    /**
     * Get unclassified transactions for a company
     */
    public List<BankTransaction> getUnclassifiedTransactions(Long companyId) {
        return bankTransactionRepository.findUnclassifiedTransactions(companyId);
    }

    /**
     * Update transaction classification
     */
    public BankTransaction updateTransactionClassification(Long transactionId, String accountCode, String accountName, String updatedBy) {
        BankTransaction transaction = getTransactionById(transactionId);
        transaction.setAccountCode(accountCode);
        transaction.setAccountName(accountName);
        transaction.setUpdatedBy(updatedBy);
        return bankTransactionRepository.save(transaction);
    }

    /**
     * Search transactions by details
     */
    public List<BankTransaction> searchTransactionsByDetails(Long companyId, String searchTerm) {
        return bankTransactionRepository.findByCompanyIdAndDescriptionContainingIgnoreCase(companyId, searchTerm);
    }
}