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

import fin.dto.TrialBalanceDTO;
import fin.dto.IncomeStatementDTO;
import fin.dto.BalanceSheetDTO;
import fin.dto.CashbookDTO;
import fin.dto.GeneralLedgerDTO;
import fin.dto.AuditTrailDTO;
import fin.model.dto.AuditTrailResponse;
import fin.model.dto.JournalEntryDetailDTO;
import fin.service.spring.AuditTrailService;
import fin.service.spring.SpringFinancialReportingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    public ResponseEntity<List<GeneralLedgerDTO>> generateGeneralLedger(@PathVariable Long companyId,
                                                                       @PathVariable Long fiscalPeriodId) {
        try {
            List<GeneralLedgerDTO> report = reportingService.generateGeneralLedgerDTOs(companyId, fiscalPeriodId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Trial Balance report
     */
    @GetMapping("/trial-balance/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<TrialBalanceDTO>> generateTrialBalance(@PathVariable Long companyId,
                                                     @PathVariable Long fiscalPeriodId) {
        try {
            List<TrialBalanceDTO> report = reportingService.generateTrialBalance(companyId, fiscalPeriodId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export Trial Balance report to specified format (PDF, EXCEL, CSV)
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param format Export format (PDF, EXCEL, CSV) - defaults to PDF
     * @return File as byte array or string with appropriate headers
     */
    @GetMapping("/trial-balance/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportTrialBalance(@PathVariable Long companyId,
                                                @PathVariable Long fiscalPeriodId,
                                                @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportTrialBalanceToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("TrialBalance_Company%d_Period%d_%s.pdf", 
                                                     companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                    
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportTrialBalanceToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("TrialBalance_Company%d_Period%d_%s.xlsx", 
                                                       companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                    
                case "CSV":
                    String csvContent = reportingService.exportTrialBalanceToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("TrialBalance_Company%d_Period%d_%s.csv", 
                                                     companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                    
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
                    
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export General Ledger report to specified format (PDF, EXCEL, CSV)
     */
    @GetMapping("/general-ledger/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportGeneralLedger(@PathVariable Long companyId,
                                                  @PathVariable Long fiscalPeriodId,
                                                  @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportGeneralLedgerToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("GeneralLedger_Company%d_Period%d_%s.pdf",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportGeneralLedgerToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("GeneralLedger_Company%d_Period%d_%s.xlsx",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                case "CSV":
                    String csvContent = reportingService.exportGeneralLedgerToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("GeneralLedger_Company%d_Period%d_%s.csv",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Income Statement report to specified format (PDF, EXCEL, CSV)
     */
    @GetMapping("/income-statement/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportIncomeStatement(@PathVariable Long companyId,
                                                   @PathVariable Long fiscalPeriodId,
                                                   @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportIncomeStatementToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("IncomeStatement_Company%d_Period%d_%s.pdf",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportIncomeStatementToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("IncomeStatement_Company%d_Period%d_%s.xlsx",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                case "CSV":
                    String csvContent = reportingService.exportIncomeStatementToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("IncomeStatement_Company%d_Period%d_%s.csv",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Balance Sheet report to specified format (PDF, EXCEL, CSV)
     */
    @GetMapping("/balance-sheet/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportBalanceSheet(@PathVariable Long companyId,
                                                @PathVariable Long fiscalPeriodId,
                                                @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportBalanceSheetToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("BalanceSheet_Company%d_Period%d_%s.pdf",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportBalanceSheetToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("BalanceSheet_Company%d_Period%d_%s.xlsx",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                case "CSV":
                    String csvContent = reportingService.exportBalanceSheetToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("BalanceSheet_Company%d_Period%d_%s.csv",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Generate Income Statement report
     */
    @GetMapping("/income-statement/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<IncomeStatementDTO>> generateIncomeStatement(@PathVariable Long companyId,
                                                                           @PathVariable Long fiscalPeriodId) {
        try {
            List<IncomeStatementDTO> report = reportingService.generateIncomeStatementDTOs(companyId, fiscalPeriodId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Balance Sheet report
     */
    @GetMapping("/balance-sheet/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<BalanceSheetDTO>> generateBalanceSheet(@PathVariable Long companyId,
                                                                    @PathVariable Long fiscalPeriodId) {
        try {
            List<BalanceSheetDTO> report = reportingService.generateBalanceSheetDTOs(companyId, fiscalPeriodId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Generate Cashbook report
     */
    @GetMapping("/cashbook/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<CashbookDTO>> generateCashbook(@PathVariable Long companyId,
                                                            @PathVariable Long fiscalPeriodId) {
        try {
            List<CashbookDTO> report = reportingService.generateCashbookDTOs(companyId, fiscalPeriodId);
            return ResponseEntity.ok(report);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export Cashbook report to specified format (PDF, EXCEL, CSV)
     */
    @GetMapping("/cashbook/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportCashbook(@PathVariable Long companyId,
                                            @PathVariable Long fiscalPeriodId,
                                            @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportCashbookToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("Cashbook_Company%d_Period%d_%s.pdf",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportCashbookToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("Cashbook_Company%d_Period%d_%s.xlsx",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                case "CSV":
                    String csvContent = reportingService.exportCashbookToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("Cashbook_Company%d_Period%d_%s.csv",
                            companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Export Audit Trail report in specified format
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param format Export format (PDF, EXCEL, CSV) - defaults to PDF
     * @return File as byte array or string with appropriate headers
     */
    @GetMapping("/audit-trail/company/{companyId}/fiscal-period/{fiscalPeriodId}/export")
    public ResponseEntity<?> exportAuditTrail(@PathVariable Long companyId,
                                              @PathVariable Long fiscalPeriodId,
                                              @RequestParam(defaultValue = "PDF") String format) {
        try {
            String upperFormat = format.toUpperCase();
            HttpHeaders headers = new HttpHeaders();
            
            // Generate filename with timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            switch (upperFormat) {
                case "PDF":
                    byte[] pdfBytes = reportingService.exportAuditTrailToPDF(companyId, fiscalPeriodId);
                    String pdfFilename = String.format("AuditTrail_Company%d_Period%d_%s.pdf", 
                                                     companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.APPLICATION_PDF);
                    headers.setContentDispositionFormData("attachment", pdfFilename);
                    headers.setContentLength(pdfBytes.length);
                    return ResponseEntity.ok().headers(headers).body(pdfBytes);
                    
                case "EXCEL":
                    byte[] excelBytes = reportingService.exportAuditTrailToExcel(companyId, fiscalPeriodId);
                    String excelFilename = String.format("AuditTrail_Company%d_Period%d_%s.xlsx", 
                                                       companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                    headers.setContentDispositionFormData("attachment", excelFilename);
                    headers.setContentLength(excelBytes.length);
                    return ResponseEntity.ok().headers(headers).body(excelBytes);
                    
                case "CSV":
                    String csvContent = reportingService.exportAuditTrailToCSV(companyId, fiscalPeriodId);
                    String csvFilename = String.format("AuditTrail_Company%d_Period%d_%s.csv", 
                                                     companyId, fiscalPeriodId, timestamp);
                    headers.setContentType(MediaType.TEXT_PLAIN);
                    headers.setContentDispositionFormData("attachment", csvFilename);
                    return ResponseEntity.ok().headers(headers).body(csvContent);
                    
                default:
                    return ResponseEntity.badRequest().body("Unsupported format: " + format + ". Supported formats: PDF, EXCEL, CSV");
            }
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Export failed: " + e.getMessage());
        }
    }

    /**
     * Generate Audit Trail report (text-based - legacy endpoint)
     */
    @GetMapping("/audit-trail/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<List<AuditTrailDTO>> generateAuditTrail(@PathVariable Long companyId,
                                                                @PathVariable Long fiscalPeriodId) {
        try {
            List<AuditTrailDTO> report = reportingService.generateAuditTrailDTOs(companyId, fiscalPeriodId);
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
                                                               @PathVariable Long fiscalPeriodId) {
        try {
            StringBuilder packageReport = new StringBuilder();
            packageReport.append("FINANCIAL REPORT PACKAGE\n");
            packageReport.append("========================\n\n");

            // Generate all reports
            packageReport.append(reportingService.generateTrialBalance(companyId, fiscalPeriodId));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateIncomeStatement(companyId, fiscalPeriodId));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateBalanceSheet(companyId, fiscalPeriodId));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateCashbook(companyId, fiscalPeriodId));
            packageReport.append("\n\n");

            packageReport.append(reportingService.generateAuditTrail(companyId, fiscalPeriodId));

            String reportContent = packageReport.toString();

            return ResponseEntity.ok(reportContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}