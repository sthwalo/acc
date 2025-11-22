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

package fin.service.spring;

import fin.model.BankTransaction;
import fin.model.FiscalPeriod;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import fin.util.SpringDebugger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Spring service for exporting transactions to CSV format
 */
@Service
public class SpringCsvExportService {

    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final SpringDebugger debugger;

    public SpringCsvExportService(BankTransactionRepository bankTransactionRepository,
                                FiscalPeriodRepository fiscalPeriodRepository,
                                SpringDebugger debugger) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
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
        debugger.logMethodEntry("SpringCsvExportService", "exportTransactionsToCsvBytes", companyId, fiscalPeriodId);

        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
            .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + fiscalPeriodId));

        List<BankTransaction> transactions = bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);

        if (transactions.isEmpty()) {
            debugger.logValidationError("SpringCsvExportService", "exportTransactionsToCsvBytes", "transactions", "empty", "No transactions found to export");
            throw new IllegalArgumentException("No transactions found to export for company " + companyId + " and fiscal period " + fiscalPeriodId);
        }

        StringBuilder csvContent = new StringBuilder();

        // Write header
        csvContent.append("Date,Details,Debits,Credits,Balance,ServiceFee,AccountNumber,StatementPeriod,source_file,FiscalPeriod\n");

        // Format for currency values (no thousands separator, 2 decimal places)
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM");

        // Write transactions
        for (BankTransaction transaction : transactions) {
            StringBuilder line = new StringBuilder();

            // Date (DD/MM format)
            line.append(transaction.getTransactionDate().format(dateFormatter)).append(",");

            // Details (escape quotes and commas)
            line.append(escapeField(transaction.getDetails())).append(",");

            // Debits
            line.append(formatAmount(transaction.getDebitAmount())).append(",");

            // Credits
            line.append(formatAmount(transaction.getCreditAmount())).append(",");

            // Balance
            line.append(formatAmount(transaction.getBalance())).append(",");

            // Service Fee
            line.append(transaction.isServiceFee() ? "Y" : "").append(",");

            // Account Number
            line.append(escapeField(transaction.getAccountNumber())).append(",");

            // Statement Period
            line.append(escapeField(transaction.getStatementPeriod())).append(",");

            // Source File
            line.append(escapeField(transaction.getSourceFile())).append(",");

            // Fiscal Period
            line.append(fiscalPeriod.getPeriodName());

            csvContent.append(line).append("\n");
        }

        debugger.logMethodExit("SpringCsvExportService", "exportTransactionsToCsvBytes",
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
        return String.format(Locale.US, "%.2f", amount);
    }
}