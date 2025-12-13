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

package fin.service.reporting;

import fin.entity.*;
import fin.dto.*;
import fin.repository.*;
import fin.service.CompanyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing Audit Trail operations.
 * Provides structured JSON responses for journal entries with pagination and filtering.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
@Service
public class AuditTrailService {

    private static final Logger LOGGER = Logger.getLogger(AuditTrailService.class.getName());

    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final CompanyService companyService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;

    public AuditTrailService(JournalEntryRepository journalEntryRepository,
                           JournalEntryLineRepository journalEntryLineRepository,
                           CompanyService companyService,
                           FiscalPeriodRepository fiscalPeriodRepository,
                           AccountRepository accountRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.companyService = companyService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Get paginated audit trail with optional filters.
     * 
     * @param companyId Company identifier
     * @param fiscalPeriodId Fiscal period identifier
     * @param page Page number (0-indexed)
     * @param pageSize Number of entries per page
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param searchTerm Optional search term for description/reference
     * @return Structured audit trail response with pagination
     * @throws SQLException If company or fiscal period not found, or no entries exist
     */
    @Transactional(readOnly = true)
    public AuditTrailResponse getAuditTrail(Long companyId, Long fiscalPeriodId, int page, int pageSize,
                                           LocalDate startDate, LocalDate endDate, String searchTerm) throws SQLException {
        
        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new SQLException(
                "Company not found in table 'companies' with id " + companyId + ". " +
                "Please ensure the company exists. " +
                "SQL: SELECT * FROM companies WHERE id = " + companyId
            );
        }

        // Validate fiscal period exists
        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId)
            .orElseThrow(() -> new SQLException(
                "Fiscal period not found in table 'fiscal_periods' with id " + fiscalPeriodId + ". " +
                "Please ensure the fiscal period exists. " +
                "SQL: SELECT * FROM fiscal_periods WHERE id = " + fiscalPeriodId
            ));

        // Create pageable with sorting
        Pageable pageable = PageRequest.of(page, pageSize);

        // Query journal entries based on active filters
        Page<JournalEntry> journalEntriesPage = queryJournalEntries(
            companyId, fiscalPeriodId, startDate, endDate, searchTerm, pageable
        );

        // Check if any entries exist
        if (journalEntriesPage.isEmpty() && page == 0) {
            LOGGER.warning("No journal entries found for company " + companyId + 
                         " and fiscal period " + fiscalPeriodId);
            // Return empty response instead of throwing exception
            PaginationMetadata pagination = new PaginationMetadata(page, pageSize, 0, 0);
            FilterMetadata filters = new FilterMetadata(startDate, endDate, null, searchTerm);
            return new AuditTrailResponse(new ArrayList<>(), pagination, filters);
        }

        // Convert entities to DTOs
        List<JournalEntryDTO> entryDTOs = journalEntriesPage.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());

        // Build pagination metadata
        PaginationMetadata pagination = new PaginationMetadata(
            journalEntriesPage.getNumber(),
            journalEntriesPage.getSize(),
            journalEntriesPage.getTotalElements(),
            journalEntriesPage.getTotalPages()
        );

        // Build filter metadata
        FilterMetadata filters = new FilterMetadata(startDate, endDate, null, searchTerm);

        return new AuditTrailResponse(entryDTOs, pagination, filters);
    }

    /**
     * Get detailed journal entry with all line items.
     * 
     * @param journalEntryId Journal entry identifier
     * @return Detailed journal entry with all lines
     * @throws SQLException If journal entry not found
     */
    @Transactional(readOnly = true)
    public JournalEntryDetailDTO getJournalEntryDetail(Long journalEntryId) throws SQLException {
        // Fetch journal entry
        JournalEntry journalEntry = journalEntryRepository.findById(journalEntryId)
            .orElseThrow(() -> new SQLException(
                "Journal entry not found in table 'journal_entries' with id " + journalEntryId + ". " +
                "Please ensure the journal entry exists. " +
                "SQL: SELECT * FROM journal_entries WHERE id = " + journalEntryId
            ));

        // Convert to detail DTO
        return convertToDetailDTO(journalEntry);
    }

    /**
     * Query journal entries based on active filters.
     */
    private Page<JournalEntry> queryJournalEntries(Long companyId, Long fiscalPeriodId,
                                                   LocalDate startDate, LocalDate endDate,
                                                   String searchTerm, Pageable pageable) {
        boolean hasDateFilter = startDate != null && endDate != null;
        boolean hasSearchFilter = searchTerm != null && !searchTerm.trim().isEmpty();

        if (hasDateFilter && hasSearchFilter) {
            // Both filters active
            return journalEntryRepository.findByCompanyIdAndFiscalPeriodIdAndDateRangeAndSearchTerm(
                companyId, fiscalPeriodId, startDate, endDate, searchTerm.trim(), pageable
            );
        } else if (hasDateFilter) {
            // Only date filter active
            return journalEntryRepository.findByCompanyIdAndFiscalPeriodIdAndDateRange(
                companyId, fiscalPeriodId, startDate, endDate, pageable
            );
        } else if (hasSearchFilter) {
            // Only search filter active
            return journalEntryRepository.findByCompanyIdAndFiscalPeriodIdAndSearchTerm(
                companyId, fiscalPeriodId, searchTerm.trim(), pageable
            );
        } else {
            // No filters - get all entries
            return journalEntryRepository.findByCompanyIdAndFiscalPeriodIdPaginated(
                companyId, fiscalPeriodId, pageable
            );
        }
    }

    /**
     * Convert JournalEntry entity to summary DTO.
     */
    private JournalEntryDTO convertToDTO(JournalEntry entry) {
        // Load lines explicitly to avoid lazy loading issues
        List<JournalEntryLine> lines = journalEntryLineRepository.findByJournalEntryId(entry.getId());
        
        // Calculate totals from journal entry lines
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        int lineCount = lines.size();

        for (JournalEntryLine line : lines) {
            if (line.getDebitAmount() != null) {
                totalDebit = totalDebit.add(line.getDebitAmount());
            }
            if (line.getCreditAmount() != null) {
                totalCredit = totalCredit.add(line.getCreditAmount());
            }
        }

        // Build description from journal entry lines (showing both accounts)
        String description = buildDescriptionFromLines(entry, lines);

        return new JournalEntryDTO(
            entry.getId(),
            entry.getReference(),
            entry.getEntryDate(),
            description,
            "Bank Transaction", // Default transaction type - could be enhanced with lookup
            entry.getCreatedBy() != null ? entry.getCreatedBy() : "FIN",
            entry.getCreatedAt(),
            totalDebit,
            totalCredit,
            lineCount
        );
    }

    /**
     * Build journal entry description from line items showing both accounts.
     * Format: "DebitAccount - CreditAccount" (e.g., "Cash - Revenue" or "Expenses - Cash")
     * This follows the legacy system's pattern of showing the double-entry structure.
     */
    private String buildDescriptionFromLines(JournalEntry entry, List<JournalEntryLine> lines) {
        // If header has a valid description, use it. Treat literal 'null' or 'null - null' as invalid.
        if (entry.getDescription() != null) {
            String headerDesc = entry.getDescription().trim();
            // Normalize 'null' strings produced by legacy concatenation
            if (!headerDesc.equalsIgnoreCase("null") && !headerDesc.matches("(?i)\\s*null\\s*(-\\s*null\\s*)?")) {
                if (!headerDesc.isEmpty()) {
                    return headerDesc;
                }
            }
        }

        // Otherwise, build from lines (show both accounts in double-entry format)
        if (lines == null || lines.isEmpty()) {
            return "No description";
        }
        
        // Find debit and credit accounts
        String debitAccount = null;
        String creditAccount = null;

        for (JournalEntryLine line : lines) {
            Account account = accountRepository.findById(line.getAccountId()).orElse(null);
            if (account != null) {
                if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(BigDecimal.ZERO) > 0) {
                    debitAccount = account.getAccountName();
                } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(BigDecimal.ZERO) > 0) {
                    creditAccount = account.getAccountName();
                }
            }
        }

        // Build description in format: "Debit Account - Credit Account"
        if (debitAccount != null && creditAccount != null) {
            return debitAccount + " - " + creditAccount;
        } else if (debitAccount != null) {
            return debitAccount;
        } else if (creditAccount != null) {
            return creditAccount;
        }

        return "Journal Entry";
    }

    /**
     * Convert JournalEntry entity to detail DTO with all lines.
     */
    private JournalEntryDetailDTO convertToDetailDTO(JournalEntry entry) {
        // Fetch company and fiscal period details
        Company company = companyService.getCompanyById(entry.getCompanyId());
        String companyName = company != null ? company.getName() : "Unknown Company";

        FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(entry.getFiscalPeriodId()).orElse(null);
        String fiscalPeriodName = fiscalPeriod != null ? fiscalPeriod.getPeriodName() : "Unknown Period";

        // Load and convert journal entry lines to DTOs
        List<JournalEntryLine> lines = journalEntryLineRepository.findByJournalEntryId(entry.getId());
        List<JournalEntryLineDTO> lineDTOs = new ArrayList<>();
        
        // Calculate totals from lines
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        
        if (lines != null) {
            for (JournalEntryLine line : lines) {
                lineDTOs.add(convertLineToDTO(line));
                
                // Add to totals
                if (line.getDebitAmount() != null) {
                    totalDebit = totalDebit.add(line.getDebitAmount());
                }
                if (line.getCreditAmount() != null) {
                    totalCredit = totalCredit.add(line.getCreditAmount());
                }
            }
        }

        String description = entry.getDescription();
        if (description == null || description.trim().isEmpty() || description.matches("(?i)\\s*null\\s*(-\\s*null\\s*)?")) {
            description = buildDescriptionFromLines(entry, lines);
        }

        return new JournalEntryDetailDTO(
            entry.getId(),
            entry.getReference(),
            entry.getEntryDate(),
            description,
            "Bank Transaction", // Default transaction type
            entry.getFiscalPeriodId(),
            fiscalPeriodName,
            entry.getCompanyId(),
            companyName,
            entry.getCreatedBy() != null ? entry.getCreatedBy() : "FIN",
            entry.getCreatedAt(),
            entry.getUpdatedAt(),
            entry.getCreatedBy(), // lastModifiedBy - could be enhanced to track separately
            entry.getUpdatedAt(), // lastModifiedAt
            totalDebit,
            totalCredit,
            lineDTOs.size(),
            lineDTOs
        );
    }

    /**
     * Convert JournalEntryLine entity to DTO.
     */
    private JournalEntryLineDTO convertLineToDTO(JournalEntryLine line) {
        // Fetch account details - use repository to avoid lazy loading issues
        Account account = accountRepository.findById(line.getAccountId()).orElse(null);
        String accountCode = account != null ? account.getAccountCode() : "N/A";
        String accountName = account != null ? account.getAccountName() : "Unknown Account";

        return new JournalEntryLineDTO(
            line.getId(),
            line.getLineNumber(),
            line.getAccountId(),
            accountCode,
            accountName,
            line.getDescription(),
            line.getDebitAmount() != null ? line.getDebitAmount() : BigDecimal.ZERO,
            line.getCreditAmount() != null ? line.getCreditAmount() : BigDecimal.ZERO
        );
    }
}
