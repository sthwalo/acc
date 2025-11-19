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

import fin.service.spring.SpringFinancialReportingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Spring REST Controller for financial reporting operations.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class SpringReportController {

    private final SpringFinancialReportingService reportingService;

    public SpringReportController(SpringFinancialReportingService reportingService) {
        this.reportingService = reportingService;
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
     * Generate Audit Trail report
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