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

package fin.controller.spring;

import fin.entity.BankTransaction;
import fin.service.upload.SpringCsvImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Spring REST Controller for CSV import operations.
 */
@RestController
@RequestMapping("/api/v1/csv")
public class SpringCsvImportController {

    private final SpringCsvImportService csvImportService;

    public SpringCsvImportController(SpringCsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    /**
     * Import transactions from CSV file
     */
    @PostMapping("/import/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<SpringCsvImportService.CsvImportResult> importCsv(
            @RequestParam("file") MultipartFile file,
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            SpringCsvImportService.CsvImportResult result =
                csvImportService.importCsvTransactions(file, companyId, fiscalPeriodId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Validate CSV file format before import
     */
    @PostMapping("/validate")
    public ResponseEntity<List<String>> validateCsv(@RequestParam("file") MultipartFile file) {
        try {
            List<String> validationErrors = csvImportService.validateCsvFormat(file);
            return ResponseEntity.ok(validationErrors);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get imported transactions for a company and fiscal period
     */
    @GetMapping("/transactions/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<BankTransaction>> getImportedTransactions(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<BankTransaction> transactions = csvImportService.getImportedTransactions(companyId, fiscalPeriodId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}