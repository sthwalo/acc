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

import fin.dto.AuditTrailResponse;
import fin.dto.JournalEntryDetailDTO;
import fin.entity.JournalEntry;
import fin.entity.JournalEntryLine;
import fin.entity.Account;
import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.repository.JournalEntryRepository;
import fin.repository.JournalEntryLineRepository;
import fin.repository.FiscalPeriodRepository;
import fin.repository.AccountRepository;
import fin.service.reporting.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditTrailService.
 * Tests pagination, filtering, and error handling.
 */
public class AuditTrailServiceTest {

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

    private AuditTrailService auditTrailService;

    private Company testCompany;
    private FiscalPeriod testFiscalPeriod;
    private List<JournalEntry> testJournalEntries;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        auditTrailService = new AuditTrailService(
                journalEntryRepository,
                journalEntryLineRepository,
                companyService,
                fiscalPeriodRepository,
                accountRepository
        );

        // Setup test data
        testCompany = new Company();
        testCompany.setId(1L);
        testCompany.setName("Test Company");

        testFiscalPeriod = new FiscalPeriod();
        testFiscalPeriod.setId(1L);
        testFiscalPeriod.setPeriodName("FY2024-2025");
        testFiscalPeriod.setStartDate(LocalDate.of(2024, 4, 1));
        testFiscalPeriod.setEndDate(LocalDate.of(2025, 3, 31));

        testJournalEntries = createTestJournalEntries();
    }

    private List<JournalEntry> createTestJournalEntries() {
        List<JournalEntry> entries = new ArrayList<>();

        for (int i = 1; i <= 3; i++) {
            JournalEntry entry = new JournalEntry();
            entry.setId((long) i);
            entry.setCompanyId(testCompany.getId());
            entry.setFiscalPeriodId(testFiscalPeriod.getId());
            entry.setReference("JE-2024-" + String.format("%04d", i));
            entry.setEntryDate(LocalDate.of(2024, 4, i));
            entry.setDescription("Test journal entry " + i);
            entry.setCreatedBy("testuser");

            // Add lines to entry
            List<JournalEntryLine> lines = new ArrayList<>();
            
            // Debit line
            JournalEntryLine debitLine = new JournalEntryLine();
            debitLine.setId((long) (i * 2 - 1));
            debitLine.setJournalEntryId(entry.getId());
            debitLine.setLineNumber(1);
            debitLine.setAccountId((long) (100 + i));
            debitLine.setDescription("Debit entry " + i);
            debitLine.setDebitAmount(BigDecimal.valueOf(1000.00 * i));
            debitLine.setCreditAmount(BigDecimal.ZERO);
            
            Account debitAccount = new Account();
            debitAccount.setId((long) (100 + i));
            debitAccount.setAccountCode("1000" + i);
            debitAccount.setAccountName("Asset Account " + i);
            debitLine.setAccount(debitAccount);
            
            lines.add(debitLine);

            // Credit line
            JournalEntryLine creditLine = new JournalEntryLine();
            creditLine.setId((long) (i * 2));
            creditLine.setJournalEntryId(entry.getId());
            creditLine.setLineNumber(2);
            creditLine.setAccountId((long) (200 + i));
            creditLine.setDescription("Credit entry " + i);
            creditLine.setDebitAmount(BigDecimal.ZERO);
            creditLine.setCreditAmount(BigDecimal.valueOf(1000.00 * i));
            
            Account creditAccount = new Account();
            creditAccount.setId((long) (200 + i));
            creditAccount.setAccountCode("2000" + i);
            creditAccount.setAccountName("Liability Account " + i);
            creditLine.setAccount(creditAccount);
            
            lines.add(creditLine);

            entry.setLines(lines);
            
            entries.add(entry);
        }

        return entries;
    }

    @Test
    @DisplayName("Should return paginated audit trail without filters")
    public void testGetAuditTrail_withoutFilters() throws SQLException {
        // Arrange
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);
        Page<JournalEntry> mockPage = new PageImpl<>(testJournalEntries, pageable, testJournalEntries.size());

        when(companyService.getCompanyById(testCompany.getId())).thenReturn(testCompany);
        when(fiscalPeriodRepository.findById(testFiscalPeriod.getId())).thenReturn(Optional.of(testFiscalPeriod));
        when(journalEntryRepository.findByCompanyIdAndFiscalPeriodIdPaginated(
                eq(testCompany.getId()),
                eq(testFiscalPeriod.getId()),
                any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        AuditTrailResponse response = auditTrailService.getAuditTrail(
                testCompany.getId(),
                testFiscalPeriod.getId(),
                page,
                size,
                null,
                null,
                null
        );

        // Assert
        assertNotNull(response);
        assertEquals(3, response.getEntries().size());
        assertEquals(page, response.getPagination().getCurrentPage());
        assertEquals(size, response.getPagination().getPageSize());
        assertEquals(1, response.getPagination().getTotalPages());
        assertFalse(response.getFilters().hasActiveFilters());

        verify(journalEntryRepository, times(1))
                .findByCompanyIdAndFiscalPeriodIdPaginated(
                        eq(testCompany.getId()),
                        eq(testFiscalPeriod.getId()),
                        any(Pageable.class));
    }

    @Test
    @DisplayName("Should return filtered audit trail with date range")
    public void testGetAuditTrail_withDateRange() throws SQLException {
        // Arrange
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2024, 4, 2);
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);
        
        List<JournalEntry> filteredEntries = testJournalEntries.subList(0, 2);
        Page<JournalEntry> mockPage = new PageImpl<>(filteredEntries, pageable, filteredEntries.size());

        when(companyService.getCompanyById(testCompany.getId())).thenReturn(testCompany);
        when(fiscalPeriodRepository.findById(testFiscalPeriod.getId())).thenReturn(Optional.of(testFiscalPeriod));
        when(journalEntryRepository.findByCompanyIdAndFiscalPeriodIdAndDateRange(
                eq(testCompany.getId()),
                eq(testFiscalPeriod.getId()),
                eq(startDate),
                eq(endDate),
                any(Pageable.class)))
                .thenReturn(mockPage);

        // Act
        AuditTrailResponse response = auditTrailService.getAuditTrail(
                testCompany.getId(),
                testFiscalPeriod.getId(),
                page,
                size,
                startDate,
                endDate,
                null
        );

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getEntries().size());
        assertTrue(response.getFilters().hasActiveFilters());
        assertEquals(startDate, response.getFilters().getStartDate());
        assertEquals(endDate, response.getFilters().getEndDate());
        assertNull(response.getFilters().getSearchTerm());

        verify(journalEntryRepository, times(1))
                .findByCompanyIdAndFiscalPeriodIdAndDateRange(
                        eq(testCompany.getId()),
                        eq(testFiscalPeriod.getId()),
                        eq(startDate),
                        eq(endDate),
                        any(Pageable.class));
    }

    @Test
    @DisplayName("Should return journal entry detail with all line items")
    public void testGetJournalEntryDetail() throws SQLException {
        // Arrange
        Long entryId = 1L;
        JournalEntry entry = testJournalEntries.get(0);

        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(companyService.getCompanyById(entry.getCompanyId())).thenReturn(testCompany);
        when(fiscalPeriodRepository.findById(entry.getFiscalPeriodId())).thenReturn(Optional.of(testFiscalPeriod));

        // Act
        JournalEntryDetailDTO detail = auditTrailService.getJournalEntryDetail(entryId);

        // Assert
        assertNotNull(detail);
        assertEquals(entry.getId(), detail.getId());
        assertEquals(entry.getReference(), detail.getReference());
        assertEquals(entry.getDescription(), detail.getDescription());
        assertEquals(testCompany.getName(), detail.getCompanyName());
        assertEquals(testFiscalPeriod.getPeriodName(), detail.getFiscalPeriodName());
        assertEquals(2, detail.getLines().size());

        verify(journalEntryRepository, times(1)).findById(entryId);
    }

    @Test
    @DisplayName("Should throw SQLException when journal entry not found")
    public void testGetJournalEntryDetail_notFound() {
        // Arrange
        Long entryId = 999L;

        when(journalEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                SQLException.class,
                () -> auditTrailService.getJournalEntryDetail(entryId)
        );

        verify(journalEntryRepository, times(1)).findById(entryId);
    }

    @Test
    @DisplayName("Should return empty list when no entries found")
    public void testGetAuditTrail_noEntries() throws SQLException {
        // Arrange
        int page = 0;
        int size = 20;
        Pageable pageable = PageRequest.of(page, size);
        Page<JournalEntry> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(companyService.getCompanyById(testCompany.getId())).thenReturn(testCompany);
        when(fiscalPeriodRepository.findById(testFiscalPeriod.getId())).thenReturn(Optional.of(testFiscalPeriod));
        when(journalEntryRepository.findByCompanyIdAndFiscalPeriodIdPaginated(
                anyLong(), anyLong(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        AuditTrailResponse response = auditTrailService.getAuditTrail(
                testCompany.getId(),
                testFiscalPeriod.getId(),
                page,
                size,
                null,
                null,
                null
        );

        // Assert
        assertNotNull(response);
        assertTrue(response.getEntries().isEmpty());
        assertEquals(0, response.getPagination().getTotalPages());
    }

    @Test
    @DisplayName("Should throw SQLException when company not found")
    public void testGetAuditTrail_companyNotFound() {
        // Arrange
        when(companyService.getCompanyById(999L)).thenReturn(null);

        // Act & Assert
        assertThrows(
                SQLException.class,
                () -> auditTrailService.getAuditTrail(999L, 1L, 0, 20, null, null, null)
        );
    }

    @Test
    @DisplayName("Should throw SQLException when fiscal period not found")
    public void testGetAuditTrail_fiscalPeriodNotFound() {
        // Arrange
        when(companyService.getCompanyById(1L)).thenReturn(testCompany);
        when(fiscalPeriodRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                SQLException.class,
                () -> auditTrailService.getAuditTrail(1L, 999L, 0, 20, null, null, null)
        );
    }
}
