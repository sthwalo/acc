/*
 * FIN Financial Management System
 *
 * Unit tests for SpringFinancialReportingService.generateAuditTrail
 */
package fin.service.spring;

import fin.entity.*;
import fin.repository.*;
import fin.service.export.ReportExportService;
import fin.service.reporting.SpringFinancialReportingService;
import fin.repository.FinancialDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class SpringFinancialReportingServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private JournalEntryLineRepository journalEntryLineRepository;
    @Mock
    private SpringCompanyService companyService;
    @Mock
    private FiscalPeriodRepository fiscalPeriodRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ReportExportService reportExportService;
    @Mock
    private FinancialDataRepository financialDataRepository;

    private SpringFinancialReportingService reportingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reportingService = new SpringFinancialReportingService(
            companyService,
            fiscalPeriodRepository,
            accountRepository,
            journalEntryRepository,
            journalEntryLineRepository,
            reportExportService,
            financialDataRepository
        );
    }

    @Test
    @DisplayName("generateAuditTrail should not throw when journal entry reference or description is null")
    public void testGenerateAuditTrailHandlesNullReferenceAndDescription() throws Exception {
        // Arrange
        Long companyId = 1L;
        Long fiscalPeriodId = 1L;

        Company company = new Company();
        company.setId(companyId);
        company.setName("TestCo");

        FiscalPeriod period = new FiscalPeriod();
        period.setId(fiscalPeriodId);
        period.setPeriodName("FY2025-01");
        period.setStartDate(LocalDate.of(2025, 1, 1));
        period.setEndDate(LocalDate.of(2025, 1, 31));

        // Journal entry with null reference and description
        JournalEntry je = new JournalEntry();
        je.setId(100L);
        je.setCompanyId(companyId);
        je.setFiscalPeriodId(fiscalPeriodId);
        je.setEntryDate(LocalDate.of(2025, 1, 15));
        je.setCreatedBy("tester");
        je.setCreatedAt(LocalDateTime.now());
        // do not set reference or description (null)

        // Create a line with an account
        Account account = new Account();
        account.setId(500L);
        account.setAccountCode("5000");
        account.setAccountName("Sales Revenue");

        JournalEntryLine line = new JournalEntryLine();
        line.setId(1000L);
        line.setJournalEntryId(je.getId());
        line.setLineNumber(1);
        line.setAccountId(account.getId());
        line.setDebitAmount(BigDecimal.ZERO);
        line.setCreditAmount(BigDecimal.valueOf(100));
        line.setAccount(account);

        je.setJournalEntryLines(List.of(line));

        // Setup mocks
        when(companyService.getCompanyById(companyId)).thenReturn(company);
        when(fiscalPeriodRepository.findById(fiscalPeriodId)).thenReturn(Optional.of(period));
        when(journalEntryRepository.findByCompanyIdAndFiscalPeriodIdOrderByEntryDateAscIdAsc(companyId, fiscalPeriodId))
                .thenReturn(List.of(je));

        // Act
        String report = reportingService.generateAuditTrail(companyId, fiscalPeriodId);

        // Assert - report should be generated and should safely handle null reference/description
        assertNotNull(report, "Report text should not be null");
        assertTrue(report.contains("ENTRY:"), "Report should contain an entry header");
        assertTrue(report.contains("DESCRIPTION:"), "Report should contain a description line");
        // Should not throw NPE and should contain expected elements like entry header and account details
        assertTrue(report.contains("ENTRY:"), "Report should contain an 'ENTRY:' header");
        assertTrue(report.contains("DESCRIPTION:"), "Report should contain a 'DESCRIPTION:' line");
        assertTrue(report.contains("5000") || report.contains("Sales Revenue"), "Report should contain account code or account name details");
    }
}
