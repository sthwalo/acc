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

package fin.service.upload;

import fin.entity.BankTransaction;
import fin.repository.BankTransactionRepository;
import fin.repository.FiscalPeriodRepository;
import fin.service.spring.SpringCompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Service for CSV import operations
 * Handles CSV file uploads and transaction parsing
 */
@Service
@Transactional
public class SpringCsvImportService {

    private final BankTransactionRepository bankTransactionRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final SpringCompanyService companyService;

    public SpringCsvImportService(BankTransactionRepository bankTransactionRepository,
                                FiscalPeriodRepository fiscalPeriodRepository,
                                SpringCompanyService companyService) {
        this.bankTransactionRepository = bankTransactionRepository;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.companyService = companyService;
    }

    /**
     * Import CSV result
     */
    public static class CsvImportResult {
        private final int totalRows;
        private final int successfulImports;
        private final int failedImports;
        private final List<String> errors;

        public CsvImportResult(int totalRows, int successfulImports, int failedImports, List<String> errors) {
            this.totalRows = totalRows;
            this.successfulImports = successfulImports;
            this.failedImports = failedImports;
            this.errors = errors;
        }

        public int getTotalRows() { return totalRows; }
        public int getSuccessfulImports() { return successfulImports; }
        public int getFailedImports() { return failedImports; }
        public List<String> getErrors() { return errors; }
    }

    /**
     * Import transactions from CSV file
     */
    @Transactional
    public CsvImportResult importCsvTransactions(MultipartFile file, Long companyId, Long fiscalPeriodId) {
        List<String> errors = new ArrayList<>();
        int successfulImports = 0;
        int totalRows = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                totalRows++;

                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    BankTransaction transaction = parseCsvLine(line, companyId, fiscalPeriodId);
                    if (transaction != null) {
                        bankTransactionRepository.save(transaction);
                        successfulImports++;
                    }
                } catch (Exception e) {
                    errors.add("Row " + totalRows + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            errors.add("File processing error: " + e.getMessage());
        }

        return new CsvImportResult(totalRows, successfulImports, totalRows - successfulImports - 1, errors); // -1 for header
    }

    /**
     * Parse a single CSV line into a BankTransaction
     */
    private BankTransaction parseCsvLine(String line, Long companyId, Long fiscalPeriodId) throws Exception {
        String[] parts = line.split(",");

        // Expected format: date,description,amount,type,reference
        if (parts.length < 4) {
            throw new Exception("Insufficient columns. Expected: date,description,amount,type[,reference]");
        }

        try {
            // Parse date
            LocalDate transactionDate = LocalDate.parse(parts[0].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            // Parse description
            String description = parts[1].trim();

            // Parse amount
            BigDecimal amount = new BigDecimal(parts[2].trim().replaceAll("[^0-9.-]", ""));

            // Parse type (DEBIT/CREDIT)
            String type = parts[3].trim().toUpperCase();
            if (!type.equals("DEBIT") && !type.equals("CREDIT")) {
                throw new Exception("Invalid transaction type: " + type + ". Must be DEBIT or CREDIT");
            }

            // Optional reference
            String reference = parts.length > 4 ? parts[4].trim() : null;

            // Validate fiscal period
            var fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId);
            if (fiscalPeriod.isEmpty()) {
                throw new Exception("Fiscal period not found: " + fiscalPeriodId);
            }

            if (!fiscalPeriod.get().containsDate(transactionDate)) {
                throw new Exception("Transaction date " + transactionDate + " is outside fiscal period " +
                                  fiscalPeriod.get().getStartDate() + " to " + fiscalPeriod.get().getEndDate());
            }

            // Create transaction
            BankTransaction transaction = new BankTransaction();
            transaction.setCompanyId(companyId);
            transaction.setFiscalPeriodId(fiscalPeriodId);
            transaction.setTransactionDate(transactionDate);
            transaction.setDetails(description);
            transaction.setReference(reference);

            // Set debit or credit amount based on type
            if ("DEBIT".equals(type)) {
                transaction.setDebitAmount(amount);
                transaction.setCreditAmount(BigDecimal.ZERO);
            } else {
                transaction.setDebitAmount(BigDecimal.ZERO);
                transaction.setCreditAmount(amount);
            }

            return transaction;

        } catch (DateTimeParseException e) {
            throw new Exception("Invalid date format. Expected yyyy-MM-dd, got: " + parts[0]);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid amount format: " + parts[2]);
        }
    }

    /**
     * Get imported transactions for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public List<BankTransaction> getImportedTransactions(Long companyId, Long fiscalPeriodId) {
        return bankTransactionRepository.findByCompanyIdAndFiscalPeriodId(companyId, fiscalPeriodId);
    }

    /**
     * Validate CSV format
     */
    public List<String> validateCsvFormat(MultipartFile file) {
        List<String> validationErrors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip header row
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 4) {
                    validationErrors.add("Line " + lineNumber + ": Insufficient columns. Expected at least 4, got " + parts.length);
                } else {
                    // Validate date format
                    try {
                        LocalDate.parse(parts[0].trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    } catch (DateTimeParseException e) {
                        validationErrors.add("Line " + lineNumber + ": Invalid date format. Expected yyyy-MM-dd, got: " + parts[0]);
                    }

                    // Validate amount
                    try {
                        new BigDecimal(parts[2].trim().replaceAll("[^0-9.-]", ""));
                    } catch (NumberFormatException e) {
                        validationErrors.add("Line " + lineNumber + ": Invalid amount format: " + parts[2]);
                    }

                    // Validate type
                    String type = parts[3].trim().toUpperCase();
                    if (!type.equals("DEBIT") && !type.equals("CREDIT")) {
                        validationErrors.add("Line " + lineNumber + ": Invalid transaction type: " + type + ". Must be DEBIT or CREDIT");
                    }
                }
            }

        } catch (Exception e) {
            validationErrors.add("File reading error: " + e.getMessage());
        }

        return validationErrors;
    }
}