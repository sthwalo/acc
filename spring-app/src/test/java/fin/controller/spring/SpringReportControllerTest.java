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

import fin.model.dto.*;
import fin.service.spring.AuditTrailService;
import fin.service.spring.SpringFinancialReportingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SpringReportController audit trail endpoints.
 * Tests HTTP responses, error handling, and parameter validation.
 */
public class SpringReportControllerTest {

    @Mock
    private SpringFinancialReportingService reportingService;

    @Mock
    private AuditTrailService auditTrailService;

    private SpringReportController controller;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new SpringReportController(reportingService, auditTrailService);
    }

    private AuditTrailResponse createMockAuditTrailResponse() {
        List<JournalEntryDTO> entries = new ArrayList<>();
        
        JournalEntryDTO entry = new JournalEntryDTO();
        entry.setId(1L);
        entry.setReference("JE-2024-0001");
        entry.setEntryDate(LocalDate.of(2024, 4, 1));
        entry.setDescription("Test entry");
        entry.setCreatedBy("testuser");
        entry.setTotalDebit(BigDecimal.valueOf(1000.00));
        entry.setTotalCredit(BigDecimal.valueOf(1000.00));
        
        entries.add(entry);

        PaginationMetadata pagination = new PaginationMetadata();
        pagination.setCurrentPage(0);
        pagination.setPageSize(20);
        pagination.setTotalPages(1);
        pagination.setTotalEntries(1L);

        FilterMetadata filters = new FilterMetadata();

        return new AuditTrailResponse(entries, pagination, filters);
    }

    private JournalEntryDetailDTO createMockJournalEntryDetail() {
        JournalEntryDetailDTO detail = new JournalEntryDetailDTO();
        detail.setId(1L);
        detail.setReference("JE-2024-0001");
        detail.setEntryDate(LocalDate.of(2024, 4, 1));
        detail.setDescription("Test entry");
        detail.setCompanyName("Test Company");
        detail.setFiscalPeriodName("FY2024-2025");
        detail.setCreatedBy("testuser");

        List<JournalEntryLineDTO> lines = new ArrayList<>();
        
        JournalEntryLineDTO debitLine = new JournalEntryLineDTO();
        debitLine.setId(1L);
        debitLine.setLineNumber(1);
        debitLine.setAccountCode("10001");
        debitLine.setAccountName("Asset Account");
        debitLine.setDescription("Debit line");
        debitLine.setDebitAmount(BigDecimal.valueOf(1000.00));
        debitLine.setCreditAmount(BigDecimal.ZERO);
        lines.add(debitLine);

        JournalEntryLineDTO creditLine = new JournalEntryLineDTO();
        creditLine.setId(2L);
        creditLine.setLineNumber(2);
        creditLine.setAccountCode("20001");
        creditLine.setAccountName("Liability Account");
        creditLine.setDescription("Credit line");
        creditLine.setDebitAmount(BigDecimal.ZERO);
        creditLine.setCreditAmount(BigDecimal.valueOf(1000.00));
        lines.add(creditLine);

        detail.setLines(lines);

        return detail;
    }

    @Test
    @DisplayName("GET /api/v1/reports/audit-trail should return 200 with data")
    public void testGetAuditTrail_success() throws SQLException {
        // Arrange
        Long companyId = 1L;
        Long fiscalPeriodId = 1L;
        int page = 0;
        int size = 20;
        
        AuditTrailResponse mockResponse = createMockAuditTrailResponse();
        
        when(auditTrailService.getAuditTrail(companyId, fiscalPeriodId, page, size, null, null, null))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<?> response = controller.getStructuredAuditTrail(
                companyId, fiscalPeriodId, page, size, null, null, null
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuditTrailResponse);
        
        AuditTrailResponse body = (AuditTrailResponse) response.getBody();
        assertEquals(1, body.getEntries().size());
        assertEquals(0, body.getPagination().getCurrentPage());
        assertFalse(body.getFilters().hasActiveFilters());

        verify(auditTrailService, times(1))
                .getAuditTrail(companyId, fiscalPeriodId, page, size, null, null, null);
    }

    @Test
    @DisplayName("GET /api/v1/reports/audit-trail with filters should return filtered data")
    public void testGetAuditTrail_withFilters() throws SQLException {
        // Arrange
        Long companyId = 1L;
        Long fiscalPeriodId = 1L;
        int page = 0;
        int size = 20;
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2024, 4, 30);
        String searchTerm = "Test";
        
        AuditTrailResponse mockResponse = createMockAuditTrailResponse();
        mockResponse.getFilters().setStartDate(startDate);
        mockResponse.getFilters().setEndDate(endDate);
        mockResponse.getFilters().setSearchTerm(searchTerm);
        
        when(auditTrailService.getAuditTrail(
                companyId, fiscalPeriodId, page, size, startDate, endDate, searchTerm))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<?> response = controller.getStructuredAuditTrail(
                companyId, fiscalPeriodId, page, size, startDate, endDate, searchTerm
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuditTrailResponse body = (AuditTrailResponse) response.getBody();
        assertTrue(body.getFilters().hasActiveFilters());
        assertEquals(startDate, body.getFilters().getStartDate());
        assertEquals(endDate, body.getFilters().getEndDate());
        assertEquals(searchTerm, body.getFilters().getSearchTerm());
    }

    @Test
    @DisplayName("GET /api/v1/reports/journal-entry/{id} should return 200 with detail")
    public void testGetJournalEntryDetail_success() throws SQLException {
        // Arrange
        Long entryId = 1L;
        JournalEntryDetailDTO mockDetail = createMockJournalEntryDetail();
        
        when(auditTrailService.getJournalEntryDetail(entryId))
                .thenReturn(mockDetail);

        // Act
        ResponseEntity<?> response = controller.getJournalEntryDetail(entryId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof JournalEntryDetailDTO);
        
        JournalEntryDetailDTO body = (JournalEntryDetailDTO) response.getBody();
        assertEquals(entryId, body.getId());
        assertEquals("JE-2024-0001", body.getReference());
        assertEquals(2, body.getLines().size());

        verify(auditTrailService, times(1)).getJournalEntryDetail(entryId);
    }

    @Test
    @DisplayName("GET /api/v1/reports/journal-entry/{id} should return 404 when not found")
    public void testGetJournalEntryDetail_notFound() throws SQLException {
        // Arrange
        Long entryId = 999L;
        
        when(auditTrailService.getJournalEntryDetail(entryId))
                .thenThrow(new SQLException("Journal entry not found with ID: " + entryId));

        // Act
        ResponseEntity<?> response = controller.getJournalEntryDetail(entryId);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/reports/audit-trail should return 400 on SQLException")
    public void testGetAuditTrail_sqlException() throws SQLException {
        // Arrange
        Long companyId = 1L;
        Long fiscalPeriodId = 999L;
        
        when(auditTrailService.getAuditTrail(anyLong(), anyLong(), anyInt(), anyInt(), any(), any(), any()))
                .thenThrow(new SQLException("Fiscal period not found"));

        // Act
        ResponseEntity<?> response = controller.getStructuredAuditTrail(
                companyId, fiscalPeriodId, 0, 20, null, null, null
        );

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/reports/audit-trail should return 500 on unexpected error")
    public void testGetAuditTrail_unexpectedError() throws SQLException {
        // Arrange
        when(auditTrailService.getAuditTrail(anyLong(), anyLong(), anyInt(), anyInt(), any(), any(), any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<?> response = controller.getStructuredAuditTrail(
                1L, 1L, 0, 20, null, null, null
        );

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

    @Test
    @DisplayName("GET /api/v1/reports/journal-entry/{id} should return 500 on unexpected error")
    public void testGetJournalEntryDetail_unexpectedError() throws SQLException {
        // Arrange
        when(auditTrailService.getJournalEntryDetail(anyLong()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act
        ResponseEntity<?> response = controller.getJournalEntryDetail(1L);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

        @Test
        @DisplayName("GET /api/v1/reports/trial-balance should return 200 with text")
        public void testGetTrialBalance_success() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = false;
                String expectedReport = "TRIAL BALANCE REPORT TEXT";

                when(reportingService.generateTrialBalance(companyId, fiscalPeriodId, exportToFile))
                                .thenReturn(expectedReport);

                // Act
                ResponseEntity<String> response = controller.generateTrialBalance(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(expectedReport, response.getBody());
                verify(reportingService, times(1)).generateTrialBalance(companyId, fiscalPeriodId, exportToFile);
        }

        @Test
        @DisplayName("GET /api/v1/reports/trial-balance/export?format=PDF should return PDF bytes and headers")
        public void testExportTrialBalance_pdf_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "PDF";
                byte[] pdfBytes = new byte[] {1, 2, 3, 4};

                when(reportingService.exportTrialBalanceToPDF(companyId, fiscalPeriodId)).thenReturn(pdfBytes);

                // Act
                ResponseEntity<?> response = controller.exportTrialBalance(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.APPLICATION_PDF));
                assertEquals(pdfBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(pdfBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportTrialBalanceToPDF(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/trial-balance/export?format=EXCEL should return Excel bytes and headers")
        public void testExportTrialBalance_excel_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "EXCEL";
                byte[] excelBytes = new byte[] {10, 20, 30};

                when(reportingService.exportTrialBalanceToExcel(companyId, fiscalPeriodId)).thenReturn(excelBytes);

                // Act
                ResponseEntity<?> response = controller.exportTrialBalance(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
                assertEquals(excelBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(excelBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportTrialBalanceToExcel(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/trial-balance/export?format=CSV should return CSV string and headers")
        public void testExportTrialBalance_csv_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "CSV";
                String csv = "account,credit,debit\n1000,100.00,0.00";

                when(reportingService.exportTrialBalanceToCSV(companyId, fiscalPeriodId)).thenReturn(csv);

                // Act
                ResponseEntity<?> response = controller.exportTrialBalance(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(org.springframework.http.MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
                assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("TrialBalance"));
                assertEquals(csv, response.getBody());
                verify(reportingService, times(1)).exportTrialBalanceToCSV(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/income-statement/export?format=PDF should return PDF bytes and headers")
        public void testExportIncomeStatement_pdf_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "PDF";
                byte[] pdfBytes = new byte[] {9, 8, 7};

                when(reportingService.exportIncomeStatementToPDF(companyId, fiscalPeriodId)).thenReturn(pdfBytes);

                // Act
                ResponseEntity<?> response = controller.exportIncomeStatement(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.APPLICATION_PDF));
                assertEquals(pdfBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(pdfBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportIncomeStatementToPDF(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/balance-sheet/export?format=EXCEL should return Excel bytes and headers")
        public void testExportBalanceSheet_excel_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "EXCEL";
                byte[] excelBytes = new byte[] {2, 4, 6};

                when(reportingService.exportBalanceSheetToExcel(companyId, fiscalPeriodId)).thenReturn(excelBytes);

                // Act
                ResponseEntity<?> response = controller.exportBalanceSheet(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
                assertEquals(excelBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(excelBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportBalanceSheetToExcel(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/general-ledger/export?format=PDF should return PDF bytes and headers")
        public void testExportGeneralLedger_pdf_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "PDF";
                byte[] pdfBytes = new byte[] {11, 12, 13};

                when(reportingService.exportGeneralLedgerToPDF(companyId, fiscalPeriodId)).thenReturn(pdfBytes);

                // Act
                ResponseEntity<?> response = controller.exportGeneralLedger(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.APPLICATION_PDF));
                assertEquals(pdfBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(pdfBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportGeneralLedgerToPDF(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/general-ledger/export?format=EXCEL should return Excel bytes and headers")
        public void testExportGeneralLedger_excel_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "EXCEL";
                byte[] excelBytes = new byte[] {21, 22, 23};

                when(reportingService.exportGeneralLedgerToExcel(companyId, fiscalPeriodId)).thenReturn(excelBytes);

                // Act
                ResponseEntity<?> response = controller.exportGeneralLedger(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
                assertEquals(excelBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(excelBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportGeneralLedgerToExcel(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/general-ledger/export?format=CSV should return CSV string and headers")
        public void testExportGeneralLedger_csv_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "CSV";
                String csv = "date,desc,debit,credit\n2025-01-01,Sample,100.00,0.00";

                when(reportingService.exportGeneralLedgerToCSV(companyId, fiscalPeriodId)).thenReturn(csv);

                // Act
                ResponseEntity<?> response = controller.exportGeneralLedger(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(org.springframework.http.MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
                assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("GeneralLedger"));
                assertEquals(csv, response.getBody());
                verify(reportingService, times(1)).exportGeneralLedgerToCSV(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/cashbook/export?format=PDF should return PDF bytes and headers")
        public void testExportCashbook_pdf_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "PDF";
                byte[] pdfBytes = new byte[] {31, 32, 33};

                when(reportingService.exportCashbookToPDF(companyId, fiscalPeriodId)).thenReturn(pdfBytes);

                // Act
                ResponseEntity<?> response = controller.exportCashbook(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.APPLICATION_PDF));
                assertEquals(pdfBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(pdfBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportCashbookToPDF(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/cashbook/export?format=EXCEL should return Excel bytes and headers")
        public void testExportCashbook_excel_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "EXCEL";
                byte[] excelBytes = new byte[] {41, 42, 43};

                when(reportingService.exportCashbookToExcel(companyId, fiscalPeriodId)).thenReturn(excelBytes);

                // Act
                ResponseEntity<?> response = controller.exportCashbook(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertTrue(response.getHeaders().getContentType().includes(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")));
                assertEquals(excelBytes.length, response.getHeaders().getContentLength());
                assertArrayEquals(excelBytes, (byte[]) response.getBody());
                verify(reportingService, times(1)).exportCashbookToExcel(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/cashbook/export?format=CSV should return CSV string and headers")
        public void testExportCashbook_csv_success() throws SQLException {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "CSV";
                String csv = "date,ref,desc,receipts,payments,balance\n2025-01-01,REF001,Sample,100.00,0.00,100.00";

                when(reportingService.exportCashbookToCSV(companyId, fiscalPeriodId)).thenReturn(csv);

                // Act
                ResponseEntity<?> response = controller.exportCashbook(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(org.springframework.http.MediaType.TEXT_PLAIN, response.getHeaders().getContentType());
                assertTrue(response.getHeaders().getContentDisposition().getFilename().contains("Cashbook"));
                assertEquals(csv, response.getBody());
                verify(reportingService, times(1)).exportCashbookToCSV(companyId, fiscalPeriodId);
        }

        @Test
        @DisplayName("GET /api/v1/reports/cashbook should return 200 with text")
        public void testGetCashbook_success() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = false;
                String expectedReport = "CASHBOOK REPORT TEXT";

                when(reportingService.generateCashbook(companyId, fiscalPeriodId, exportToFile))
                                .thenReturn(expectedReport);

                // Act
                ResponseEntity<String> response = controller.generateCashbook(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(expectedReport, response.getBody());
                verify(reportingService, times(1)).generateCashbook(companyId, fiscalPeriodId, exportToFile);
        }

        @Test
        @DisplayName("GET /api/v1/reports/general-ledger should return 200 with text")
        public void testGetGeneralLedger_success() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = false;
                String expectedReport = "GENERAL LEDGER REPORT TEXT";

                when(reportingService.generateGeneralLedger(companyId, fiscalPeriodId, exportToFile))
                                .thenReturn(expectedReport);

                // Act
                ResponseEntity<String> response = controller.generateGeneralLedger(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(expectedReport, response.getBody());
                verify(reportingService, times(1)).generateGeneralLedger(companyId, fiscalPeriodId, exportToFile);
        }

        @Test
        @DisplayName("GET /api/v1/reports/financial should return combined package text and export invoked when requested")
        public void testGenerateFinancialReportPackage_success_exportFiles() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = true;

                when(reportingService.generateTrialBalance(companyId, fiscalPeriodId, false)).thenReturn("TB");
                when(reportingService.generateIncomeStatement(companyId, fiscalPeriodId, false)).thenReturn("IS");
                when(reportingService.generateBalanceSheet(companyId, fiscalPeriodId, false)).thenReturn("BS");
                when(reportingService.generateCashbook(companyId, fiscalPeriodId, false)).thenReturn("CB");
                when(reportingService.generateAuditTrail(companyId, fiscalPeriodId, false)).thenReturn("AT");

                // Act
                ResponseEntity<String> response = controller.generateFinancialReportPackage(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertNotNull(response.getBody());
                String content = response.getBody();
                assertTrue(content.contains("TB"));
                assertTrue(content.contains("IS"));
                assertTrue(content.contains("BS"));
                assertTrue(content.contains("CB"));
                assertTrue(content.contains("AT"));

                // Verify that export variant was invoked for each report
                verify(reportingService, times(1)).generateTrialBalance(companyId, fiscalPeriodId, true);
                verify(reportingService, times(1)).generateIncomeStatement(companyId, fiscalPeriodId, true);
                verify(reportingService, times(1)).generateBalanceSheet(companyId, fiscalPeriodId, true);
                verify(reportingService, times(1)).generateCashbook(companyId, fiscalPeriodId, true);
                verify(reportingService, times(1)).generateAuditTrail(companyId, fiscalPeriodId, true);
        }

        @Test
        @DisplayName("GET /api/v1/reports/income-statement should return 200 with text")
        public void testGetIncomeStatement_success() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = false;
                String expectedReport = "INCOME STATEMENT TEXT";

                when(reportingService.generateIncomeStatement(companyId, fiscalPeriodId, exportToFile))
                                .thenReturn(expectedReport);

                // Act
                ResponseEntity<String> response = controller.generateIncomeStatement(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(expectedReport, response.getBody());
                verify(reportingService, times(1)).generateIncomeStatement(companyId, fiscalPeriodId, exportToFile);
        }

        @Test
        @DisplayName("GET /api/v1/reports/balance-sheet should return 200 with text")
        public void testGetBalanceSheet_success() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                boolean exportToFile = false;
                String expectedReport = "BALANCE SHEET TEXT";

                when(reportingService.generateBalanceSheet(companyId, fiscalPeriodId, exportToFile))
                                .thenReturn(expectedReport);

                // Act
                ResponseEntity<String> response = controller.generateBalanceSheet(companyId, fiscalPeriodId, exportToFile);

                // Assert
                assertEquals(HttpStatus.OK, response.getStatusCode());
                assertEquals(expectedReport, response.getBody());
                verify(reportingService, times(1)).generateBalanceSheet(companyId, fiscalPeriodId, exportToFile);
        }

        @Test
        @DisplayName("GET /api/v1/reports/trial-balance/export?format=unknown should return 400")
        public void testExportTrialBalance_invalidFormat_returnsBadRequest() {
                // Arrange
                Long companyId = 1L;
                Long fiscalPeriodId = 1L;
                String format = "TXT"; // unsupported

                // Act
                ResponseEntity<?> response = controller.exportTrialBalance(companyId, fiscalPeriodId, format);

                // Assert
                assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
}
