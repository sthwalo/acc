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

package fin.service.classification.reporting;

import fin.entity.*;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import fin.service.journal.AccountService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/**
 * Service responsible for transaction classification reporting and statistics.
 * Handles data retrieval, statistics calculation, and report generation.
 *
 * SINGLE RESPONSIBILITY: Classification reporting and analytics
 */
@Service
@Transactional(readOnly = true)
public class TransactionClassificationReportingService {

    private static final Logger LOGGER = Logger.getLogger(TransactionClassificationReportingService.class.getName());
    private final NumberFormat currencyFormat;

    // Console output formatting constants
    private static final int CONSOLE_SEPARATOR_WIDTH = 80;

    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountService accountService;

    public TransactionClassificationReportingService(BankTransactionRepository bankTransactionRepository,
                                                  FiscalPeriodRepository fiscalPeriodRepository,
                                                  AccountService accountService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountService = accountService;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }

    /**
     * Count unclassified transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public int countUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        return (int) transactions.stream()
                .filter(t -> t.getAccountCode() == null)
                .count();
    }

    /**
     * Count classified transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public int countClassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        return (int) transactions.stream()
                .filter(t -> t.getAccountCode() != null)
                .count();
    }

    /**
     * Get all transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> getTransactionsByCompanyAndPeriod(Long companyId, Long fiscalPeriodId) {
        if (companyId == null || fiscalPeriodId == null) {
            throw new IllegalArgumentException("Company ID and fiscal period ID are required");
        }
        return bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
    }

    /**
     * Get classification statistics for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public ClassificationStats getClassificationStats(Long companyId, Long fiscalPeriodId) {
        int total = (int) bankTransactionRepository.countByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
        int classified = countClassifiedTransactions(companyId, fiscalPeriodId);
        int unclassified = total - classified;

        return new ClassificationStats(total, classified, unclassified);
    }

    /**
     * Get classification summary for a company
     */
    @Transactional(readOnly = true)
    public String getClassificationSummary(Long companyId) {
        StringBuilder summary = new StringBuilder();
        summary.append("CLASSIFICATION SUMMARY\n");
        summary.append("=====================\n\n");

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);
        int totalTransactions = 0;
        int totalClassified = 0;

        for (FiscalPeriod period : periods) {
            int periodTotal = (int) bankTransactionRepository.countByCompanyIdAndFiscalPeriodId(companyId, period.getId());
            int periodClassified = countClassifiedTransactions(companyId, period.getId());
            int periodUnclassified = periodTotal - periodClassified;

            summary.append(String.format("PERIOD: %s\n", period.getPeriodName()));
            summary.append(String.format("  Total: %d, Classified: %d, Unclassified: %d\n",
                periodTotal, periodClassified, periodUnclassified));

            if (periodTotal > 0) {
                double rate = (double) periodClassified / periodTotal * 100;
                summary.append(String.format("  Rate: %.1f%%\n", rate));
            }
            summary.append("\n");

            totalTransactions += periodTotal;
            totalClassified += periodClassified;
        }

        double overallRate = totalTransactions > 0 ? (double) totalClassified / totalTransactions * 100 : 0;
        summary.append(String.format("OVERALL: %d/%d transactions classified (%.1f%%)\n",
            totalClassified, totalTransactions, overallRate));

        return summary.toString();
    }

    /**
     * Get uncategorized transactions for a company
     */
    @Transactional(readOnly = true)
    public String getUncategorizedTransactions(Long companyId) {
        StringBuilder report = new StringBuilder();
        report.append("UNCATEGORIZED TRANSACTIONS\n");
        report.append("==========================\n\n");

        List<FiscalPeriod> periods = fiscalPeriodRepository.findByCompanyId(companyId);

        for (FiscalPeriod period : periods) {
            List<BankTransaction> unclassified = bankTransactionRepository
                .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, period.getId());

            if (!unclassified.isEmpty()) {
                report.append(String.format("PERIOD: %s (%d transactions)\n", period.getPeriodName(), unclassified.size()));
                report.append("-".repeat(50)).append("\n");

                for (BankTransaction transaction : unclassified) {
                    report.append(String.format("ID: %d | %s | %s | %s\n",
                        transaction.getId(),
                        transaction.getTransactionDate(),
                        truncateString(transaction.getDescription(), 50),
                        formatCurrency(transaction.getDebitAmount() != null ? transaction.getDebitAmount() : transaction.getCreditAmount())));
                }
                report.append("\n");
            }
        }

        return report.toString();
    }

    /**
     * Get unclassified transactions for a specific fiscal period as JSON data.
     * Used by the frontend TransactionClassificationReview component.
     */
    @Transactional(readOnly = true)
    public List<UnclassifiedTransaction> getUnclassifiedTransactions(Long companyId, Long fiscalPeriodId) {
        List<BankTransaction> unclassified = bankTransactionRepository
            .findByCompanyIdAndFiscalPeriodIdAndAccountCodeIsNull(companyId, fiscalPeriodId);

        return unclassified.stream()
            .map(this::convertToUnclassifiedTransaction)
            .toList();
    }

    /**
     * Convert BankTransaction to UnclassifiedTransaction with suggestions
     */
    private UnclassifiedTransaction convertToUnclassifiedTransaction(BankTransaction transaction) {
        UnclassifiedTransaction result = new UnclassifiedTransaction();
        result.setId(transaction.getId());
        result.setDate(transaction.getTransactionDate().toString());
        result.setDescription(transaction.getDescription() != null ? transaction.getDescription() : "");

        // Calculate net amount (debit - credit)
        BigDecimal debit = transaction.getDebitAmount() != null ? transaction.getDebitAmount() : BigDecimal.ZERO;
        BigDecimal credit = transaction.getCreditAmount() != null ? transaction.getCreditAmount() : BigDecimal.ZERO;
        result.setAmount(debit.subtract(credit).doubleValue());

        result.setType(debit.compareTo(credit) > 0 ? "debit" : "credit");
        result.setReference(transaction.getReference() != null ? transaction.getReference() : "");

        // For now, don't include suggestions to avoid complexity
        // TODO: Add suggestion logic later if needed
        result.setSuggestedClassification(null);

        return result;
    }



    // Helper methods

    private String truncateString(String str, int maxLength) {
        if (str == null) return "";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "R 0.00";
        return currencyFormat.format(amount);
    }

    /**
     * Inner class for classification statistics
     */
    public static class ClassificationStats {
        private final int total;
        private final int classified;
        private final int unclassified;

        public ClassificationStats(int total, int classified, int unclassified) {
            this.total = total;
            this.classified = classified;
            this.unclassified = unclassified;
        }

        public int getTotal() { return total; }
        public int getClassified() { return classified; }
        public int getUnclassified() { return unclassified; }
        public double getClassificationRate() {
            return total > 0 ? (double) classified / total * 100 : 0;
        }
    }

    /**
     * DTO for unclassified transactions
     */
    public static class UnclassifiedTransaction {
        private Long id;
        private String date;
        private String description;
        private Double amount;
        private String type;
        private String reference;
        private SuggestedClassification suggestedClassification;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public SuggestedClassification getSuggestedClassification() { return suggestedClassification; }
        public void setSuggestedClassification(SuggestedClassification suggestedClassification) {
            this.suggestedClassification = suggestedClassification;
        }
    }

    /**
     * DTO for classification suggestions
     */
    public static class SuggestedClassification {
        private String accountCode;
        private String accountName;
        private Double confidence;

        public SuggestedClassification(String accountCode, String accountName, Double confidence) {
            this.accountCode = accountCode;
            this.accountName = accountName;
            this.confidence = confidence;
        }

        public String getAccountCode() { return accountCode; }
        public String getAccountName() { return accountName; }
        public Double getConfidence() { return confidence; }
    }
}