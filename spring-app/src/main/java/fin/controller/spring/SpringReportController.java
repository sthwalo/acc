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

import fin.model.dto.AuditTrailResponse;
import fin.model.dto.JournalEntryDetailDTO;
import fin.service.spring.AuditTrailService;
import fin.service.spring.SpringFinancialReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Spring REST Controller for financial reporting operations.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class SpringReportController {

    private final SpringFinancialReportingService reportingService;
    private final AuditTrailService auditTrailService;

    public SpringReportController(SpringFinancialReportingService reportingService,
                                 AuditTrailService auditTrailService) {
        this.reportingService = reportingService;
        this.auditTrailService = auditTrailService;
    }

    /**
     * Generate General Ledger report
     */
    @GetMapping("/general-ledger/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateGeneralLedger(@PathVariable Long companyId,
                                                      @PathVariable Long fiscalPeriodId,
                                                      @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateGeneralLedger(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Trial Balance report
     */
    @GetMapping("/trial-balance/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateTrialBalance(@PathVariable Long companyId,
                                                     @PathVariable Long fiscalPeriodId,
                                                     @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateTrialBalance(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Income Statement report
     */
    @GetMapping("/income-statement/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateIncomeStatement(@PathVariable Long companyId,
                                                        @PathVariable Long fiscalPeriodId,
                                                        @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateIncomeStatement(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Balance Sheet report
     */
    @GetMapping("/balance-sheet/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateBalanceSheet(@PathVariable Long companyId,
                                                     @PathVariable Long fiscalPeriodId,
                                                     @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateBalanceSheet(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Cashbook report
     */
    @GetMapping("/cashbook/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateCashbook(@PathVariable Long companyId,
                                                 @PathVariable Long fiscalPeriodId,
                                                 @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateCashbook(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Audit Trail report (text-based - legacy endpoint)
     */
    @GetMapping("/audit-trail/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateAuditTrail(@PathVariable Long companyId,
                                                   @PathVariable Long fiscalPeriodId,
                                                   @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            String report = reportingService.generateAuditTrail(companyId, fiscalPeriodId, exportToFile);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ============================================================================
    // TASK_007: Structured Audit Trail Endpoints with Pagination
    // ============================================================================

    /**
     * Get structured audit trail with pagination and filtering.
     * Returns JSON response with journal entries, pagination metadata, and filter info.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param page Page number (0-indexed, default: 0)
     * @param pageSize Number of entries per page (default: 50)
     * @param startDate Optional start date filter (ISO format: yyyy-MM-dd)
     * @param endDate Optional end date filter (ISO format: yyyy-MM-dd)
     * @param searchTerm Optional search term for description/reference
     * @return Structured audit trail response with pagination
     */
    @GetMapping("/audit-trail/company/{companyId}/fiscal-period/{fiscalPeriodId}/structured")
    public ResponseEntity<AuditTrailResponse> getStructuredAuditTrail(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchTerm) {
        
        try {
            AuditTrailResponse response = auditTrailService.getAuditTrail(
                companyId, fiscalPeriodId, page, pageSize, startDate, endDate, searchTerm
            );
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            // Return 400 Bad Request for invalid company/fiscal period
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            // Return 500 Internal Server Error for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get detailed journal entry with all line items.
     * Returns JSON response with full journal entry details including debits and credits.
     * 
     * @param journalEntryId Journal entry identifier
     * @return Detailed journal entry with all lines
     */
    @GetMapping("/audit-trail/journal-entry/{journalEntryId}")
    public ResponseEntity<JournalEntryDetailDTO> getJournalEntryDetail(
            @PathVariable Long journalEntryId) {
        
        try {
            JournalEntryDetailDTO detail = auditTrailService.getJournalEntryDetail(journalEntryId);
            return ResponseEntity.ok(detail);
        } catch (SQLException e) {
            // Return 404 Not Found if journal entry doesn't exist
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            // Return 500 Internal Server Error for unexpected errors
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate comprehensive financial report package
     */
    @GetMapping("/financial/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<String> generateFinancialReportPackage(@PathVariable Long companyId,
                                                               @PathVariable Long fiscalPeriodId,
                                                               @RequestParam(defaultValue = "false") boolean exportToFile) {
        try {
            StringBuilder packageReport = new StringBuilder();
            packageReport.append("FINANCIAL REPORT PACKAGE\n");
            packageReport.append("========================\n\n");

            // Generate all reports
            packageReport.append(reportingService.generateTrialBalance(companyId, fiscalPeriodId, false));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateIncomeStatement(companyId, fiscalPeriodId, false));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateBalanceSheet(companyId, fiscalPeriodId, false));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateCashbook(companyId, fiscalPeriodId, false));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateAuditTrail(companyId, fiscalPeriodId, false));

            String reportContent = packageReport.toString();

            if (exportToFile) {
                // Export each report individually
                reportingService.generateTrialBalance(companyId, fiscalPeriodId, true);
                reportingService.generateIncomeStatement(companyId, fiscalPeriodId, true);
                reportingService.generateBalanceSheet(companyId, fiscalPeriodId, true);
                reportingService.generateCashbook(companyId, fiscalPeriodId, true);
                reportingService.generateAuditTrail(companyId, fiscalPeriodId, true);
            }

            return ResponseEntity.ok(reportContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}