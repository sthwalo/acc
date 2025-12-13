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

package fin.controller;

import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.repository.FiscalPeriodRepository;
import fin.service.CompanyService;
import fin.service.reporting.CsvExportService;
import fin.service.reporting.PdfExportService;
import fin.service.upload.BankStatementProcessingService;
import fin.entity.BankTransaction;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spring REST Controller for centralized export operations (CSV and PDF).
 * This controller centralizes all export functionality to avoid code duplication.
 */
@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;
    private final CompanyService companyService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final BankStatementProcessingService bankStatementService;

    public ExportController(CsvExportService csvExportService,
                                PdfExportService pdfExportService,
                                CompanyService companyService,
                                FiscalPeriodRepository fiscalPeriodRepository,
                                BankStatementProcessingService bankStatementService) {
        this.csvExportService = csvExportService;
        this.pdfExportService = pdfExportService;
        this.companyService = companyService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.bankStatementService = bankStatementService;
    }

    /**
     * Export transactions to CSV for a company and fiscal period
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions/csv")
    public ResponseEntity<ByteArrayResource> exportTransactionsToCsv(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            // Get company and fiscal period information
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                return ResponseEntity.badRequest().build();
            }

            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.badRequest().build();
            }

            // Use the centralized CSV export service
            byte[] csvBytes = csvExportService.exportTransactionsToCsvBytes(companyId, fiscalPeriodId);

            ByteArrayResource resource = new ByteArrayResource(csvBytes);

            String filename = String.format("transactions_%s_%s.csv",
                company.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                fiscalPeriod.getPeriodName().replaceAll("[^a-zA-Z0-9]", "_"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(csvBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export transactions to PDF for a company and fiscal period
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions/pdf")
    public ResponseEntity<ByteArrayResource> exportTransactionsToPdf(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            // Get company and fiscal period information
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                return ResponseEntity.badRequest().build();
            }

            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.badRequest().build();
            }

            // Get transactions for the fiscal period
            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();

            // Use the centralized PDF export service
            byte[] pdfBytes = pdfExportService.exportTransactionsToPdfBytes(transactions, company, fiscalPeriod);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String filename = String.format("transactions_%s_%s.pdf",
                company.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                fiscalPeriod.getPeriodName().replaceAll("[^a-zA-Z0-9]", "_"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}