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

package fin.service.reporting;

import fin.entity.Account;
import fin.entity.BankTransaction;
import fin.entity.JournalEntryLine;
import fin.repository.AccountRepository;
import fin.repository.BankTransactionRepository;
import fin.repository.JournalEntryLineRepository;
import fin.util.Debugger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Spring service for exporting transactions to CSV format
 */
@Service
public class CsvExportService {

    private final BankTransactionRepository bankTransactionRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;
    private final Debugger debugger;

    public CsvExportService(
            BankTransactionRepository bankTransactionRepository,
            JournalEntryLineRepository journalEntryLineRepository,
            AccountRepository accountRepository,
            Debugger debugger) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.accountRepository = accountRepository;
        this.debugger = debugger;
    }

    /**
     * Export transactions to CSV bytes for a company and fiscal period
     *
     * @param companyId The company ID
     * @param fiscalPeriodId The fiscal period ID
     * @return CSV content as byte array
     */
    public byte[] exportTransactionsToCsvBytes(Long companyId, Long fiscalPeriodId) throws IOException {
        debugger.logMethodEntry("CsvExportService", "exportTransactionsToCsvBytes", companyId, fiscalPeriodId);

        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);

        if (transactions.isEmpty()) {
            debugger.logValidationError("CsvExportService", "exportTransactionsToCsvBytes", "transactions", "empty", "No transactions found to export");
            throw new IllegalArgumentException("No transactions found to export for company " + companyId + " and fiscal period " + fiscalPeriodId);
        }

        // Enrich transactions with classification data from journal entries
        enrichTransactionsWithClassification(transactions, companyId);

        StringBuilder csvContent = new StringBuilder();

        // Write header
        csvContent.append("ID,Date,Details,Debit,Credit,Balance,Classification,Created At\n");

        // Format for currency values (no thousands separator, 2 decimal places)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");

        // Write transactions
        for (BankTransaction transaction : transactions) {
            StringBuilder line = new StringBuilder();

            // ID
            line.append(transaction.getId()).append(",");

            // Date (DD/MM format)
            line.append(transaction.getTransactionDate().format(dateFormatter)).append(",");

            // Details (escape quotes and commas)
            line.append(escapeField(transaction.getDetails())).append(",");

            // Debit
            line.append(formatAmount(transaction.getDebitAmount())).append(",");

            // Credit
            line.append(formatAmount(transaction.getCreditAmount())).append(",");

            // Balance
            line.append(formatAmount(transaction.getBalance())).append(",");

            // Classification - Main Account in [code] name format
            String classification = getMainAccountClassification(transaction);
            line.append(escapeField(classification)).append(",");

            // Created At
            line.append(transaction.getCreatedAt() != null ? transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");

            csvContent.append(line).append("\n");
        }

        debugger.logMethodExit("CsvExportService", "exportTransactionsToCsvBytes",
            String.format("Generated CSV with %d transactions", transactions.size()));

        return csvContent.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeField(String field) {
        if (field == null) return "";
        // Replace quotes with double quotes and wrap in quotes if contains comma or newline
        String escaped = field.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "";
        // CRITICAL: Use US locale to ensure period as decimal separator (not comma)
        // CSV format requires comma as delimiter, so decimal separator MUST be period
        return String.format(Locale.US, "%.2f", amount);
    }

    /**
     * Enrich transactions with classification data from journal entries.
     * Populates the transient debit/credit account fields by querying journal_entry_lines.
     * This matches the pattern used in CompanyController.getTransactions().
     */
    private void enrichTransactionsWithClassification(List<BankTransaction> transactions, Long companyId) {
        for (BankTransaction transaction : transactions) {
            List<JournalEntryLine> journalLines = journalEntryLineRepository.findBySourceTransactionId(transaction.getId());
            
            if (!journalLines.isEmpty()) {
                // For each transaction, we have 2 journal lines (debit and credit)
                // Find the debit line (debit_amount > 0) and credit line (credit_amount > 0)
                for (JournalEntryLine line : journalLines) {
                    Optional<Account> accountOpt = accountRepository.findById(line.getAccountId());
                    
                    if (accountOpt.isPresent()) {
                        Account account = accountOpt.get();
                        
                        if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            // This is a debit line
                            transaction.setDebitAccountId(account.getId());
                            transaction.setDebitAccountCode(account.getAccountCode());
                            transaction.setDebitAccountName(account.getAccountName());
                        } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            // This is a credit line
                            transaction.setCreditAccountId(account.getId());
                            transaction.setCreditAccountCode(account.getAccountCode());
                            transaction.setCreditAccountName(account.getAccountName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the main account for classification display.
     * Logic: Show the non-cash/non-bank account
     * - Credit transaction (money IN) → show credit account (revenue/income)
     * - Debit transaction (money OUT) → show debit account (expense)
     */
    private String getMainAccountClassification(BankTransaction transaction) {
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            // Credit transaction → main account is the credit account (revenue/income)
            if (transaction.getCreditAccountCode() != null && transaction.getCreditAccountName() != null) {
                return "[" + transaction.getCreditAccountCode() + "] " + transaction.getCreditAccountName();
            }
        } else if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            // Debit transaction → main account is the debit account (expense)
            if (transaction.getDebitAccountCode() != null && transaction.getDebitAccountName() != null) {
                return "[" + transaction.getDebitAccountCode() + "] " + transaction.getDebitAccountName();
            }
        }
        // Fallback to original category if not classified
        return transaction.getCategory() != null ? transaction.getCategory() : "Not classified";
    }
}