/*
 * Unit test for AuditTrailService description sanitization
 */
package fin.service.spring;

import fin.entity.*;
import fin.dto.JournalEntryDetailDTO;
import fin.repository.*;
import fin.service.reporting.AuditTrailService;
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
import static org.mockito.Mockito.*;

public class AuditTrailServiceDescriptionSanitizeTest {
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalEntryLineRepository journalEntryLineRepository;
    @Mock private SpringCompanyService companyService;
    @Mock private FiscalPeriodRepository fiscalPeriodRepository;
    @Mock private AccountRepository accountRepository;

    private AuditTrailService auditTrailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auditTrailService = new AuditTrailService(journalEntryRepository, journalEntryLineRepository, companyService, fiscalPeriodRepository, accountRepository);
    }

    @Test
    @DisplayName("Should sanitize literal 'null - null' header description and derive from lines")
    public void testSanitizeNullStringHeader() throws Exception {
        // Arrange
        JournalEntry je = new JournalEntry();
        je.setId(100L);
        je.setCompanyId(1L);
        je.setFiscalPeriodId(1L);
        je.setEntryDate(LocalDate.of(2024, 6, 26));
        je.setReference(null);
        je.setDescription("null - null");
        je.setCreatedBy("testuser");
        je.setCreatedAt(LocalDateTime.now());

        // line uses account with name "MIWAY Insurance Premiums"
        JournalEntryLine line = new JournalEntryLine();
        line.setId(1L);
        line.setJournalEntryId(je.getId());
        line.setLineNumber(1);
        line.setAccountId(8800L);
        line.setDebitAmount(BigDecimal.valueOf(3011.21));
        line.setCreditAmount(BigDecimal.ZERO);

        Account a1 = new Account();
        a1.setId(8800L);
        a1.setAccountName("MIWAY Insurance Premiums");

        when(journalEntryRepository.findById(je.getId())).thenReturn(Optional.of(je));
        when(journalEntryLineRepository.findByJournalEntryId(je.getId())).thenReturn(List.of(line));
        when(accountRepository.findById(8800L)).thenReturn(Optional.of(a1));
        when(companyService.getCompanyById(1L)).thenReturn(new Company());
        when(fiscalPeriodRepository.findById(1L)).thenReturn(Optional.of(new FiscalPeriod()));

        // Act
        JournalEntryDetailDTO detail = auditTrailService.getJournalEntryDetail(je.getId());

        // Assert
        assertNotNull(detail);
        assertNotEquals("null - null", detail.getDescription(), "Description should be sanitized");
        assertTrue(detail.getDescription().contains("MIWAY") || detail.getDescription().contains("Insurance"), "Derived description should reflect account name");
    }
}
