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

import fin.dto.*;
import fin.entity.*;
import fin.model.report.ColumnDefinition;
import fin.repository.*;
import fin.service.export.ReportExportService;
import fin.service.CompanyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

// PDFBox imports
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

// Apache POI imports
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Java IO imports
import java.io.ByteArrayOutputStream;

/**
 * Spring Service for financial reporting using JPA repositories.
 * Generates comprehensive double-entry accounting reports with export functionality.
 */
@Service
@Transactional(readOnly = true)
public class FinancialReportingService {

    private static final Logger LOGGER = Logger.getLogger(FinancialReportingService.class.getName());
    private final NumberFormat currencyFormat;

    // Report formatting constants
    private static final int REPORT_SEPARATOR_WIDTH = 120;
    private static final int ACCOUNT_NAME_TRUNCATE_CHECK = 23;
    private static final int ACCOUNT_NAME_TRUNCATE_LENGTH = 20;
    private static final int DESCRIPTION_TRUNCATE_CHECK = 28;
    private static final int DESCRIPTION_TRUNCATE_LENGTH = 25;

    // Dependencies
    private final CompanyService companyService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;
    private final ReportExportService reportExportService;
    private final FinancialDataRepository financialDataRepository;

    public FinancialReportingService(CompanyService companyService,
                                         FiscalPeriodRepository fiscalPeriodRepository,
                                         AccountRepository accountRepository,
                                         JournalEntryRepository journalEntryRepository,
                                         JournalEntryLineRepository journalEntryLineRepository,
                                         ReportExportService reportExportService,
                                         FinancialDataRepository financialDataRepository) {
        this.companyService = companyService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.reportExportService = reportExportService;
        this.financialDataRepository = financialDataRepository;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }

    /**
     * Generates a comprehensive General Ledger report
     */
    @Transactional(readOnly = true)
    public List<GeneralLedgerDTO> generateGeneralLedgerDTOs(Long companyId, Long fiscalPeriodId) {
        try {
            List<GeneralLedgerDTO> entries = new ArrayList<>();

            // Get all accounts for the company
            List<Account> accounts = accountRepository.findByCompanyId(companyId);

            for (Account account : accounts) {
                List<JournalEntryLine> journalEntries = journalEntryLineRepository
                    .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

                if (journalEntries.isEmpty()) {
                    continue;
                }

                BigDecimal runningBalance = BigDecimal.ZERO;

                for (JournalEntryLine entry : journalEntries) {
                    JournalEntry journalEntry = entry.getJournalEntry();
                    BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
                    BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;

                    // Update running balance
                    runningBalance = runningBalance.add(debit).subtract(credit);

                    entries.add(new GeneralLedgerDTO(
                        journalEntry.getEntryDate(),
                        journalEntry.getReference(),
                        entry.getDescription(),
                        debit.compareTo(BigDecimal.ZERO) > 0 ? debit : BigDecimal.ZERO,
                        credit.compareTo(BigDecimal.ZERO) > 0 ? credit : BigDecimal.ZERO,
                        runningBalance
                    ));
                }
            }

            return entries;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating general ledger", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Generates a comprehensive General Ledger report
     */
    @Transactional(readOnly = true)
    public String generateGeneralLedger(Long companyId, Long fiscalPeriodId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("GENERAL LEDGER", company, period));

            // Get all accounts for the company
            List<Account> accounts = accountRepository.findByCompanyId(companyId);

            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;

            for (Account account : accounts) {
                List<JournalEntryLine> entries = journalEntryLineRepository
                    .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

                if (entries.isEmpty()) {
                    continue;
                }

                // Account header
                report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("ACCOUNT: %s - %s%n", account.getAccountCode(), account.getAccountName()));
                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("%-12s %-15s %-30s %15s %15s %15s%n",
                        "Date", "Reference", "Description", "Debit", "Credit", "Balance"));
                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

                BigDecimal runningBalance = BigDecimal.ZERO;

                for (JournalEntryLine entry : entries) {
                    JournalEntry journalEntry = entry.getJournalEntry();
                    BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
                    BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;

                    // Update running balance
                    runningBalance = runningBalance.add(debit).subtract(credit);

                    report.append(String.format("%-12s %-15s %-30s %15s %15s %15s%n",
                            journalEntry.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            journalEntry.getReference(),
                            truncateText(entry.getDescription(), DESCRIPTION_TRUNCATE_CHECK, DESCRIPTION_TRUNCATE_LENGTH),
                            debit.compareTo(BigDecimal.ZERO) > 0 ? formatCurrency(debit) : "",
                            credit.compareTo(BigDecimal.ZERO) > 0 ? formatCurrency(credit) : "",
                            formatCurrency(runningBalance)));
                }

                // Account totals
                BigDecimal accountDebits = entries.stream()
                    .map(e -> e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal accountCredits = entries.stream()
                    .map(e -> e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                grandTotalDebits = grandTotalDebits.add(accountDebits);
                grandTotalCredits = grandTotalCredits.add(accountCredits);

                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("%-58s %15s %15s%n",
                        "ACCOUNT TOTALS:",
                        formatCurrency(accountDebits),
                        formatCurrency(accountCredits)));
            }

            // Grand totals
            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-58s %15s %15s%n",
                    "GRAND TOTALS:",
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            return report.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating general ledger", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Trial Balance report data
     */
    @Transactional(readOnly = true)
    public List<TrialBalanceDTO> generateTrialBalance(Long companyId, Long fiscalPeriodId) {
        try {
            // Use the new DTO-based repository method
            return financialDataRepository.getTrialBalanceDTOs(companyId, fiscalPeriodId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating trial balance", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Builds Trial Balance column definitions for export
     */
    private List<ColumnDefinition> buildTrialBalanceColumns() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("Account Code", "accountCode", 90, "text", "left"));
        columns.add(new ColumnDefinition("Account Name", "accountName", 230, "text", "left"));
        columns.add(new ColumnDefinition("Debit", "debit", 90, "currency", "right"));
        columns.add(new ColumnDefinition("Credit", "credit", 90, "currency", "right"));
        return columns;
    }

    private List<ColumnDefinition> buildIncomeStatementColumns() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("Account Code", "accountCode", 90, "text", "left"));
        columns.add(new ColumnDefinition("Account Name", "accountName", 230, "text", "left"));
        columns.add(new ColumnDefinition("Amount", "amount", 110, "currency", "right"));
        return columns;
    }

    private List<ColumnDefinition> buildBalanceSheetColumns() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("Account Code", "accountCode", 90, "text", "left"));
        columns.add(new ColumnDefinition("Account Name", "accountName", 230, "text", "left"));
        columns.add(new ColumnDefinition("Amount", "amount", 110, "currency", "right"));
        return columns;
    }

    /**
     * Builds Trial Balance data as a list of maps for export
     */
    private List<Map<String, Object>> buildTrialBalanceData(Long companyId, Long fiscalPeriodId) throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        List<Account> accounts = accountRepository.findByCompanyId(companyId);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (Account account : accounts) {
            List<JournalEntryLine> entries = journalEntryLineRepository
                .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            for (JournalEntryLine entry : entries) {
                if (entry.getDebitAmount() != null) {
                    debitBalance = debitBalance.add(entry.getDebitAmount());
                }
                if (entry.getCreditAmount() != null) {
                    creditBalance = creditBalance.add(entry.getCreditAmount());
                }
            }

            BigDecimal netBalance = debitBalance.subtract(creditBalance);

            if (netBalance.compareTo(BigDecimal.ZERO) != 0) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", account.getAccountCode());
                row.put("accountName", account.getAccountName());
                row.put("debit", netBalance.compareTo(BigDecimal.ZERO) > 0 ? netBalance : BigDecimal.ZERO);
                row.put("credit", netBalance.compareTo(BigDecimal.ZERO) < 0 ? netBalance.abs() : BigDecimal.ZERO);
                data.add(row);

                if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                    totalDebits = totalDebits.add(netBalance);
                } else {
                    totalCredits = totalCredits.add(netBalance.abs());
                }
            }
        }

        // Add totals row
        Map<String, Object> totalsRow = new HashMap<>();
        totalsRow.put("accountCode", "");
        totalsRow.put("accountName", "TOTALS:");
        totalsRow.put("debit", totalDebits);
        totalsRow.put("credit", totalCredits);
        data.add(totalsRow);

        return data;
    }

    /**
     * Exports Trial Balance to PDF format
     */
    @Transactional(readOnly = true)
    public byte[] exportTrialBalanceToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<TrialBalanceDTO> entries = financialDataRepository.getTrialBalanceDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildTrialBalanceColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (TrialBalanceDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("debit", e.getDebit());
                row.put("credit", e.getCredit());
                data.add(row);
            }

            return reportExportService.exportToPDF(data, columns, "TRIAL BALANCE", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting trial balance to PDF", e);
            throw new SQLException("Failed to export trial balance to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Trial Balance to Excel format
     */
    @Transactional(readOnly = true)
    public byte[] exportTrialBalanceToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<TrialBalanceDTO> entries = financialDataRepository.getTrialBalanceDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildTrialBalanceColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (TrialBalanceDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("debit", e.getDebit());
                row.put("credit", e.getCredit());
                data.add(row);
            }

            return reportExportService.exportToExcel(data, columns, "TRIAL BALANCE", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting trial balance to Excel", e);
            throw new SQLException("Failed to export trial balance to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Trial Balance to CSV format
     */
    @Transactional(readOnly = true)
    public String exportTrialBalanceToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            List<TrialBalanceDTO> entries = financialDataRepository.getTrialBalanceDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildTrialBalanceColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (TrialBalanceDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("debit", e.getDebit());
                row.put("credit", e.getCredit());
                data.add(row);
            }

            return reportExportService.exportToCSV(data, columns);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting trial balance to CSV", e);
            throw new SQLException("Failed to export trial balance to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Income Statement to PDF format
     */
    @Transactional(readOnly = true)
    public byte[] exportIncomeStatementToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<FinancialReportDTO> entries = financialDataRepository.getIncomeStatementDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildIncomeStatementColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToPDF(data, columns, "INCOME STATEMENT", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting income statement to PDF", e);
            throw new SQLException("Failed to export income statement to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Income Statement to Excel format
     */
    @Transactional(readOnly = true)
    public byte[] exportIncomeStatementToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<FinancialReportDTO> entries = financialDataRepository.getIncomeStatementDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildIncomeStatementColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToExcel(data, columns, "INCOME STATEMENT", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting income statement to Excel", e);
            throw new SQLException("Failed to export income statement to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Income Statement to CSV format
     */
    @Transactional(readOnly = true)
    public String exportIncomeStatementToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            List<FinancialReportDTO> entries = financialDataRepository.getIncomeStatementDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildIncomeStatementColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToCSV(data, columns);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting income statement to CSV", e);
            throw new SQLException("Failed to export income statement to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Balance Sheet to PDF format
     */
    @Transactional(readOnly = true)
    public byte[] exportBalanceSheetToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<FinancialReportDTO> entries = financialDataRepository.getBalanceSheetDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildBalanceSheetColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToPDF(data, columns, "BALANCE SHEET", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting balance sheet to PDF", e);
            throw new SQLException("Failed to export balance sheet to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Balance Sheet to Excel format
     */
    @Transactional(readOnly = true)
    public byte[] exportBalanceSheetToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<FinancialReportDTO> entries = financialDataRepository.getBalanceSheetDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildBalanceSheetColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToExcel(data, columns, "BALANCE SHEET", companyId, period);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting balance sheet to Excel", e);
            throw new SQLException("Failed to export balance sheet to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Exports Balance Sheet to CSV format
     */
    @Transactional(readOnly = true)
    public String exportBalanceSheetToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            List<FinancialReportDTO> entries = financialDataRepository.getBalanceSheetDTOs(companyId, fiscalPeriodId);
            List<ColumnDefinition> columns = buildBalanceSheetColumns();

            List<Map<String, Object>> data = new ArrayList<>();
            for (FinancialReportDTO e : entries) {
                Map<String, Object> row = new HashMap<>();
                row.put("accountCode", e.getAccountCode());
                row.put("accountName", e.getAccountName());
                row.put("amount", e.getAmount());
                data.add(row);
            }

            return reportExportService.exportToCSV(data, columns);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting balance sheet to CSV", e);
            throw new SQLException("Failed to export balance sheet to CSV: " + e.getMessage(), e);
        }
    }

    /**
     * Generates an Income Statement (Profit & Loss)
     */
    @Transactional(readOnly = true)
    /**
     * Generates an Income Statement report data
     */
    public List<IncomeStatementDTO> generateIncomeStatementDTOs(Long companyId, Long fiscalPeriodId) {
        try {
            List<IncomeStatementDTO> entries = new ArrayList<>();

            // Revenue accounts (typically 4000-4999)
            List<IncomeStatementDTO> revenueEntries = getAccountEntries(companyId, fiscalPeriodId, "4%", "Revenue");
            entries.addAll(revenueEntries);

            // Expense accounts (typically 5000-5999)
            List<IncomeStatementDTO> expenseEntries = getAccountEntries(companyId, fiscalPeriodId, "5%", "Expenses");
            entries.addAll(expenseEntries);

            return entries;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating income statement", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Generates an Income Statement report as formatted text
     */
    @Transactional(readOnly = true)
    public String generateIncomeStatement(Long companyId, Long fiscalPeriodId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("INCOME STATEMENT", company, period));

            // Revenue accounts (typically 4000-4999)
            BigDecimal totalRevenue = generateAccountSection(report, companyId, fiscalPeriodId,
                    "REVENUE", "4%", "Revenue");

            // Expense accounts (typically 5000-5999)
            BigDecimal totalExpenses = generateAccountSection(report, companyId, fiscalPeriodId,
                    "EXPENSES", "5%", "Expenses");

            // Calculate profit/loss
            BigDecimal profitLoss = totalRevenue.subtract(totalExpenses);

            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-60s %15s%n", "NET PROFIT/(LOSS):", formatCurrency(profitLoss)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            return report.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating income statement", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Balance Sheet
     */
    @Transactional(readOnly = true)
    /**
     * Generates a Balance Sheet report data
     */
    public List<BalanceSheetDTO> generateBalanceSheetDTOs(Long companyId, Long fiscalPeriodId) {
        try {
            List<BalanceSheetDTO> entries = new ArrayList<>();

            // Assets (typically 1000-1999)
            List<BalanceSheetDTO> assetEntries = getBalanceSheetEntries(companyId, fiscalPeriodId, "1%", "Assets");
            entries.addAll(assetEntries);

            // Liabilities (typically 2000-2999)
            List<BalanceSheetDTO> liabilityEntries = getBalanceSheetEntries(companyId, fiscalPeriodId, "2%", "Liabilities");
            entries.addAll(liabilityEntries);

            // Equity (typically 3000-3999)
            List<BalanceSheetDTO> equityEntries = getBalanceSheetEntries(companyId, fiscalPeriodId, "3%", "Equity");
            entries.addAll(equityEntries);

            return entries;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating balance sheet", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Generates a Balance Sheet report as formatted text
     */
    @Transactional(readOnly = true)
    public String generateBalanceSheet(Long companyId, Long fiscalPeriodId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("BALANCE SHEET", company, period));

            // Assets (typically 1000-1999)
            BigDecimal totalAssets = generateAccountSection(report, companyId, fiscalPeriodId,
                    "ASSETS", "1%", "Assets");

            // Liabilities (typically 2000-2999)
            BigDecimal totalLiabilities = generateAccountSection(report, companyId, fiscalPeriodId,
                    "LIABILITIES", "2%", "Liabilities");

            // Equity (typically 3000-3999)
            BigDecimal totalEquity = generateAccountSection(report, companyId, fiscalPeriodId,
                    "EQUITY", "3%", "Equity");

            BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-60s %15s%n", "TOTAL ASSETS:", formatCurrency(totalAssets)));
            report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES & EQUITY:", formatCurrency(totalLiabilitiesAndEquity)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            return report.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating balance sheet", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Cashbook report data
     */
    public List<CashbookDTO> generateCashbookDTOs(Long companyId, Long fiscalPeriodId) {
        try {
            List<CashbookDTO> entries = new ArrayList<>();

            // Get cash/bank accounts (typically starting with 1)
            List<Account> cashAccounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, "1");

            for (Account account : cashAccounts) {
                List<JournalEntryLine> journalEntries = journalEntryLineRepository
                    .findByAccountIdAndJournalEntry_FiscalPeriodIdOrderByJournalEntry_EntryDate(account.getId(), fiscalPeriodId);

                if (journalEntries.isEmpty()) {
                    continue;
                }

                BigDecimal runningBalance = BigDecimal.ZERO;

                for (JournalEntryLine entry : journalEntries) {
                    JournalEntry journalEntry = entry.getJournalEntry();
                    BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
                    BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;

                    // For cashbook, debits are receipts, credits are payments
                    runningBalance = runningBalance.add(debit).subtract(credit);

                    entries.add(new CashbookDTO(
                        journalEntry.getEntryDate(),
                        journalEntry.getReference(),
                        entry.getDescription(),
                        debit.compareTo(BigDecimal.ZERO) > 0 ? debit : BigDecimal.ZERO,
                        credit.compareTo(BigDecimal.ZERO) > 0 ? credit : BigDecimal.ZERO,
                        runningBalance
                    ));
                }
            }

            return entries;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating cashbook", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Generates a Cashbook report
     */
    @Transactional(readOnly = true)
    public String generateCashbook(Long companyId, Long fiscalPeriodId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("CASHBOOK", company, period));

            // Get cash/bank accounts (typically starting with 1)
            List<Account> cashAccounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, "1");

            BigDecimal totalDebits = BigDecimal.ZERO;
            BigDecimal totalCredits = BigDecimal.ZERO;

            for (Account account : cashAccounts) {
                List<JournalEntryLine> entries = journalEntryLineRepository
                    .findByAccountIdAndJournalEntry_FiscalPeriodIdOrderByJournalEntry_EntryDate(account.getId(), fiscalPeriodId);

                if (entries.isEmpty()) {
                    continue;
                }

                // Account header
                report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("ACCOUNT: %s - %s%n", account.getAccountCode(), account.getAccountName()));
                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("%-12s %-15s %-30s %15s %15s %15s%n",
                        "Date", "Reference", "Description", "Receipts", "Payments", "Balance"));
                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

                BigDecimal runningBalance = BigDecimal.ZERO;

                for (JournalEntryLine entry : entries) {
                    JournalEntry journalEntry = entry.getJournalEntry();
                    BigDecimal debit = entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO;
                    BigDecimal credit = entry.getCreditAmount() != null ? entry.getCreditAmount() : BigDecimal.ZERO;

                    // For cashbook, debits are receipts, credits are payments
                    runningBalance = runningBalance.add(debit).subtract(credit);

                    report.append(String.format("%-12s %-15s %-30s %15s %15s %15s%n",
                            journalEntry.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            journalEntry.getReference(),
                            truncateText(entry.getDescription(), DESCRIPTION_TRUNCATE_CHECK, DESCRIPTION_TRUNCATE_LENGTH),
                            debit.compareTo(BigDecimal.ZERO) > 0 ? formatCurrency(debit) : "",
                            credit.compareTo(BigDecimal.ZERO) > 0 ? formatCurrency(credit) : "",
                            formatCurrency(runningBalance)));
                }

                // Account totals
                BigDecimal accountDebits = entries.stream()
                    .map(e -> e.getDebitAmount() != null ? e.getDebitAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal accountCredits = entries.stream()
                    .map(e -> e.getCreditAmount() != null ? e.getCreditAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalDebits = totalDebits.add(accountDebits);
                totalCredits = totalCredits.add(accountCredits);

                report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                report.append(String.format("%-58s %15s %15s%n",
                        "ACCOUNT TOTALS:",
                        formatCurrency(accountDebits),
                        formatCurrency(accountCredits)));
            }

            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-58s %15s %15s%n",
                    "GRAND TOTALS:",
                    formatCurrency(totalDebits),
                    formatCurrency(totalCredits)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            return report.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating cashbook", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Exports General Ledger to PDF format (combined across all active accounts)
     */
    @Transactional(readOnly = true)
    public byte[] exportGeneralLedgerToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            int cId = companyId.intValue();
            int fId = fiscalPeriodId.intValue();
            List<AccountInfo> accounts = financialDataRepository.getActiveAccountsFromJournals(cId, fId);
            List<ColumnDefinition> columns = buildGeneralLedgerColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (AccountInfo account : accounts) {
                List<GeneralLedgerDTO> entries = financialDataRepository.getGeneralLedgerDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (GeneralLedgerDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("debit", e.getDebit());
                    row.put("credit", e.getCredit());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToPDF(data, columns, "GENERAL LEDGER", companyId, period);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting general ledger to PDF", e);
            throw new SQLException("Failed to export general ledger to PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Exports General Ledger to Excel format
     */
    @Transactional(readOnly = true)
    public byte[] exportGeneralLedgerToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            int cId = companyId.intValue();
            int fId = fiscalPeriodId.intValue();
            List<AccountInfo> accounts = financialDataRepository.getActiveAccountsFromJournals(cId, fId);
            List<ColumnDefinition> columns = buildGeneralLedgerColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (AccountInfo account : accounts) {
                List<GeneralLedgerDTO> entries = financialDataRepository.getGeneralLedgerDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (GeneralLedgerDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("debit", e.getDebit());
                    row.put("credit", e.getCredit());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToExcel(data, columns, "GENERAL LEDGER", companyId, period);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting general ledger to Excel", e);
            throw new SQLException("Failed to export general ledger to Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Exports General Ledger to CSV format
     */
    @Transactional(readOnly = true)
    public String exportGeneralLedgerToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            int cId = companyId.intValue();
            int fId = fiscalPeriodId.intValue();
            List<AccountInfo> accounts = financialDataRepository.getActiveAccountsFromJournals(cId, fId);
            List<ColumnDefinition> columns = buildGeneralLedgerColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (AccountInfo account : accounts) {
                List<GeneralLedgerDTO> entries = financialDataRepository.getGeneralLedgerDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (GeneralLedgerDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("debit", e.getDebit());
                    row.put("credit", e.getCredit());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToCSV(data, columns);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting general ledger to CSV", e);
            throw new SQLException("Failed to export general ledger to CSV: " + e.getMessage(), e);
        }
    }

    private List<ColumnDefinition> buildGeneralLedgerColumns() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("Account Code", "accountCode", 90, "text", "left"));
        columns.add(new ColumnDefinition("Account Name", "accountName", 200, "text", "left"));
        columns.add(new ColumnDefinition("Date", "date", 90, "date", "left"));
        columns.add(new ColumnDefinition("Reference", "reference", 120, "text", "left"));
        columns.add(new ColumnDefinition("Description", "description", 300, "text", "left"));
        columns.add(new ColumnDefinition("Debit", "debit", 90, "currency", "right"));
        columns.add(new ColumnDefinition("Credit", "credit", 90, "currency", "right"));
        columns.add(new ColumnDefinition("Balance", "balance", 90, "currency", "right"));
        return columns;
    }

    /**
     * Exports Cashbook to PDF format (combined across cash accounts)
     */
    @Transactional(readOnly = true)
    public byte[] exportCashbookToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<Account> cashAccounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, "1");
            List<ColumnDefinition> columns = buildCashbookColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (Account account : cashAccounts) {
                List<CashbookDTO> entries = financialDataRepository.getCashbookDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (CashbookDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("receipts", e.getReceipts());
                    row.put("payments", e.getPayments());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToPDF(data, columns, "CASHBOOK", companyId, period);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting cashbook to PDF", e);
            throw new SQLException("Failed to export cashbook to PDF: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public byte[] exportCashbookToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
            List<Account> cashAccounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, "1");
            List<ColumnDefinition> columns = buildCashbookColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (Account account : cashAccounts) {
                List<CashbookDTO> entries = financialDataRepository.getCashbookDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (CashbookDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("receipts", e.getReceipts());
                    row.put("payments", e.getPayments());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToExcel(data, columns, "CASHBOOK", companyId, period);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting cashbook to Excel", e);
            throw new SQLException("Failed to export cashbook to Excel: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public String exportCashbookToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            List<Account> cashAccounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, "1");
            List<ColumnDefinition> columns = buildCashbookColumns();
            List<Map<String, Object>> data = new ArrayList<>();

            for (Account account : cashAccounts) {
                List<CashbookDTO> entries = financialDataRepository.getCashbookDTOs(companyId, fiscalPeriodId, account.getAccountCode());
                for (CashbookDTO e : entries) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("date", e.getDate());
                    row.put("reference", e.getReference());
                    row.put("description", e.getDescription());
                    row.put("receipts", e.getReceipts());
                    row.put("payments", e.getPayments());
                    row.put("balance", e.getBalance());
                    row.put("accountCode", account.getAccountCode());
                    row.put("accountName", account.getAccountName());
                    data.add(row);
                }
            }

            return reportExportService.exportToCSV(data, columns);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error exporting cashbook to CSV", e);
            throw new SQLException("Failed to export cashbook to CSV: " + e.getMessage(), e);
        }
    }

    private List<ColumnDefinition> buildCashbookColumns() {
        List<ColumnDefinition> columns = new ArrayList<>();
        columns.add(new ColumnDefinition("Account Code", "accountCode", 90, "text", "left"));
        columns.add(new ColumnDefinition("Account Name", "accountName", 200, "text", "left"));
        columns.add(new ColumnDefinition("Date", "date", 90, "date", "left"));
        columns.add(new ColumnDefinition("Reference", "reference", 120, "text", "left"));
        columns.add(new ColumnDefinition("Description", "description", 300, "text", "left"));
        columns.add(new ColumnDefinition("Receipts", "receipts", 90, "currency", "right"));
        columns.add(new ColumnDefinition("Payments", "payments", 90, "currency", "right"));
        columns.add(new ColumnDefinition("Balance", "balance", 90, "currency", "right"));
        return columns;
    }

    /**
     * Generates Audit Trail DTOs for UI display
     */
    @Transactional(readOnly = true)
    public List<AuditTrailDTO> generateAuditTrailDTOs(Long companyId, Long fiscalPeriodId) {
        try {
            List<AuditTrailDTO> entries = new ArrayList<>();

            List<JournalEntry> journalEntries = journalEntryRepository
                .findByCompanyIdAndFiscalPeriodIdOrderByEntryDateAscIdAsc(companyId, fiscalPeriodId);

            String currentEntry = null;

            for (JournalEntry je : journalEntries) {
                // New journal entry header — always print header for the first entry even if reference is null
                if (currentEntry == null || !Objects.equals(je.getReference(), currentEntry)) {
                    currentEntry = je.getReference();

                    // Create line DTOs for this entry
                    List<AuditTrailLineDTO> lines = new ArrayList<>();
                    for (JournalEntryLine jel : je.getJournalEntryLines()) {
                        Account account = jel.getAccount();
                        lines.add(new AuditTrailLineDTO(
                            account.getAccountCode(),
                            account.getAccountName(),
                            jel.getDescription(),
                            jel.getDebitAmount(),
                            jel.getCreditAmount()
                        ));
                    }

                    entries.add(new AuditTrailDTO(
                        je.getReference(),
                        je.getEntryDate().atStartOfDay(), // Convert LocalDate to LocalDateTime
                        je.getDescription(),
                        je.getCreatedBy() != null ? je.getCreatedBy() : "FIN",
                        je.getCreatedAt(),
                        lines
                    ));
                }
            }

            return entries;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating audit trail DTOs", e);
            throw new RuntimeException("Error generating report: " + e.getMessage());
        }
    }

    /**
     * Generates an Audit Trail report
     */
    @Transactional(readOnly = true)
    public String generateAuditTrail(Long companyId, Long fiscalPeriodId) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("AUDIT TRAIL", company, period));

            List<JournalEntry> journalEntries = journalEntryRepository
                .findByCompanyIdAndFiscalPeriodIdOrderByEntryDateAscIdAsc(companyId, fiscalPeriodId);

            String currentEntry = null;
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;

            for (JournalEntry je : journalEntries) {
                // New journal entry header — always print header for the first entry even if reference is null
                if (currentEntry == null || !Objects.equals(je.getReference(), currentEntry)) {
                    currentEntry = je.getReference();

                    report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                    report.append(String.format("ENTRY: %-20s DATE: %-12s CREATED BY: %-20s%n",
                            je.getReference(),
                            je.getEntryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            je.getCreatedBy() != null ? je.getCreatedBy() : "FIN"));
                    report.append(String.format("DESCRIPTION: %s%n", je.getDescription()));
                    report.append(String.format("TIMESTAMP: %s%n",
                            je.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
                    report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                    report.append(String.format("%-10s %-25s %-30s %15s %15s%n",
                            "Code", "Account", "Description", "Debit", "Credit"));
                    report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                }

                // Journal entry lines
                for (JournalEntryLine jel : je.getJournalEntryLines()) {
                    Account account = jel.getAccount();

                    if (jel.getDebitAmount() != null) {
                        grandTotalDebits = grandTotalDebits.add(jel.getDebitAmount());
                    }
                    if (jel.getCreditAmount() != null) {
                        grandTotalCredits = grandTotalCredits.add(jel.getCreditAmount());
                    }

                    report.append(String.format("%-10s %-25s %-30s %15s %15s%n",
                            account.getAccountCode(),
                            truncateText(account.getAccountName(), ACCOUNT_NAME_TRUNCATE_CHECK, ACCOUNT_NAME_TRUNCATE_LENGTH),
                            truncateText(jel.getDescription(), DESCRIPTION_TRUNCATE_CHECK, DESCRIPTION_TRUNCATE_LENGTH),
                            jel.getDebitAmount() != null ? formatCurrency(jel.getDebitAmount()) : "",
                            jel.getCreditAmount() != null ? formatCurrency(jel.getCreditAmount()) : ""));
                }
            }

            // Grand totals
            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-71s %15s %15s%n",
                    "GRAND TOTALS:",
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            return report.toString();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating audit trail", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Export Audit Trail to PDF format
     */
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToPDF(Long companyId, Long fiscalPeriodId) throws SQLException {
        String reportContent = generateAuditTrail(companyId, fiscalPeriodId);
        return exportTextReportToPDF("AUDIT TRAIL", reportContent, companyId, fiscalPeriodId);
    }

    /**
     * Export Audit Trail to Excel format
     */
    @Transactional(readOnly = true)
    public byte[] exportAuditTrailToExcel(Long companyId, Long fiscalPeriodId) throws SQLException {
        String reportContent = generateAuditTrail(companyId, fiscalPeriodId);
        return exportTextReportToExcel("AUDIT TRAIL", reportContent, companyId, fiscalPeriodId);
    }

    /**
     * Export Audit Trail to CSV format
     */
    @Transactional(readOnly = true)
    public String exportAuditTrailToCSV(Long companyId, Long fiscalPeriodId) throws SQLException {
        String reportContent = generateAuditTrail(companyId, fiscalPeriodId);
        return exportTextReportToCSV("AUDIT TRAIL", reportContent, companyId, fiscalPeriodId);
    }

    /**
     * Export text-based report to PDF format
     */
    private byte[] exportTextReportToPDF(String reportTitle, String reportContent, Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            PDDocument document = new PDDocument();
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);

            float margin = 50;
            float yStart = page.getMediaBox().getHeight() - margin;
            float yPosition = yStart;

            // Add header
            String header = generateReportHeader(reportTitle, company, period);
            String[] headerLines = header.split("\n");
            for (String line : headerLines) {
                if (yPosition < margin) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);
                    yPosition = yStart;
                }
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= 12;
            }

            // Add report content
            String[] contentLines = reportContent.split("\n");
            for (String line : contentLines) {
                if (yPosition < margin) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 10);
                    yPosition = yStart;
                }
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(line);
                contentStream.endText();
                yPosition -= 12;
            }

            contentStream.close();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            document.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SQLException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Export text-based report to Excel format
     */
    private byte[] exportTextReportToExcel(String reportTitle, String reportContent, Long companyId, Long fiscalPeriodId) throws SQLException {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Report");

            int rowNum = 0;

            // Add header
            String header = generateReportHeader(reportTitle, company, period);
            String[] headerLines = header.split("\n");
            for (String line : headerLines) {
                Row row = sheet.createRow(rowNum++);
                Cell cell = row.createCell(0);
                cell.setCellValue(line);
            }

            // Add empty row
            rowNum++;

            // Add report content
            String[] contentLines = reportContent.split("\n");
            for (String line : contentLines) {
                Row row = sheet.createRow(rowNum++);
                Cell cell = row.createCell(0);
                cell.setCellValue(line);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SQLException("Failed to generate Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Export text-based report to CSV format
     */
    private String exportTextReportToCSV(String reportTitle, String reportContent, Long companyId, Long fiscalPeriodId) throws SQLException {
        Company company = companyService.getCompanyById(companyId);
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

        StringBuilder csv = new StringBuilder();

        // Add header
        String header = generateReportHeader(reportTitle, company, period);
        String[] headerLines = header.split("\n");
        for (String line : headerLines) {
            csv.append(line).append("\n");
        }

        // Add report content
        csv.append(reportContent);

        return csv.toString();
    }

    // Helper methods

    private BigDecimal generateAccountSection(StringBuilder report, Long companyId, Long fiscalPeriodId,
                                            String sectionTitle, String accountCodePrefix, String accountType) {
        report.append("\n").append(sectionTitle).append("\n");
        report.append("-".repeat(50)).append("\n");

        List<Account> accounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, accountCodePrefix.substring(0, 1));

        BigDecimal sectionTotal = BigDecimal.ZERO;

        for (Account account : accounts) {
            List<JournalEntryLine> entries = journalEntryLineRepository
                .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            for (JournalEntryLine entry : entries) {
                if (entry.getDebitAmount() != null) {
                    debitBalance = debitBalance.add(entry.getDebitAmount());
                }
                if (entry.getCreditAmount() != null) {
                    creditBalance = creditBalance.add(entry.getCreditAmount());
                }
            }

            BigDecimal netBalance = debitBalance.subtract(creditBalance);

            if (netBalance.compareTo(BigDecimal.ZERO) != 0) {
                report.append(String.format("%-40s %15s%n",
                        truncateText(account.getAccountName(), 35, 32),
                        formatCurrency(netBalance)));
                sectionTotal = sectionTotal.add(netBalance);
            }
        }

        report.append("-".repeat(50)).append("\n");
        report.append(String.format("%-40s %15s%n", "TOTAL " + sectionTitle + ":", formatCurrency(sectionTotal)));
        report.append("=".repeat(50)).append("\n");

        return sectionTotal;
    }

    private List<IncomeStatementDTO> getAccountEntries(Long companyId, Long fiscalPeriodId,
                                                     String accountCodePrefix, String category) {
        List<IncomeStatementDTO> entries = new ArrayList<>();

        List<Account> accounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, accountCodePrefix.substring(0, 1));

        for (Account account : accounts) {
            List<JournalEntryLine> journalEntries = journalEntryLineRepository
                .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            for (JournalEntryLine entry : journalEntries) {
                if (entry.getDebitAmount() != null) {
                    debitBalance = debitBalance.add(entry.getDebitAmount());
                }
                if (entry.getCreditAmount() != null) {
                    creditBalance = creditBalance.add(entry.getCreditAmount());
                }
            }

            BigDecimal netBalance = debitBalance.subtract(creditBalance);

            if (netBalance.compareTo(BigDecimal.ZERO) != 0) {
                entries.add(new IncomeStatementDTO(category, account.getAccountCode(),
                        account.getAccountName(), netBalance, category.toUpperCase()));
            }
        }

        return entries;
    }

    private List<BalanceSheetDTO> getBalanceSheetEntries(Long companyId, Long fiscalPeriodId,
                                                             String accountCodePrefix, String category) {
        List<BalanceSheetDTO> entries = new ArrayList<>();

        List<Account> accounts = accountRepository.findByCompanyIdAndAccountCodeStartingWith(companyId, accountCodePrefix.substring(0, 1));

        for (Account account : accounts) {
            List<JournalEntryLine> journalEntries = journalEntryLineRepository
                .findByAccountIdAndJournalEntry_FiscalPeriodId(account.getId(), fiscalPeriodId);

            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            for (JournalEntryLine entry : journalEntries) {
                if (entry.getDebitAmount() != null) {
                    debitBalance = debitBalance.add(entry.getDebitAmount());
                }
                if (entry.getCreditAmount() != null) {
                    creditBalance = creditBalance.add(entry.getCreditAmount());
                }
            }

            BigDecimal netBalance = debitBalance.subtract(creditBalance);

            if (netBalance.compareTo(BigDecimal.ZERO) != 0) {
                entries.add(new BalanceSheetDTO(category, account.getAccountCode(),
                        account.getAccountName(), netBalance, category.toLowerCase()));
            }
        }

        return entries;
    }

    private String generateReportHeader(String reportTitle, Company company, FiscalPeriod period) {
        StringBuilder header = new StringBuilder();
        header.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
        header.append(String.format("%s%n", centerText(reportTitle, REPORT_SEPARATOR_WIDTH)));
        header.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

        if (company != null) {
            header.append(String.format("Company: %s%n", company.getName()));
            if (company.getLogoPath() != null && !company.getLogoPath().trim().isEmpty()) {
                header.append(String.format("Company Logo: Available (%s)%n", company.getLogoPath()));
            }
        }
        if (period != null) {
            header.append(String.format("Fiscal Period: %s (%s to %s)%n",
                    period.getPeriodName(),
                    period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
        header.append(String.format("Generated: %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
        header.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n\n");

        return header.toString();
    }

    private void exportReport(String reportType, Company company, FiscalPeriod period, String content) {
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }

            // Generate filename in the format: ReportType(period).txt
            String reportName = getReportDisplayName(reportType);
            String periodName = period != null ? period.getPeriodName() : "Unknown";
            String filename = String.format("%s(%s).txt", reportName, periodName);

            Path filePath = reportsDir.resolve(filename);

            // Export as TXT file
            try (FileWriter writer = new FileWriter(filePath.toFile(), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            LOGGER.info("Report exported to TXT: " + filePath.toAbsolutePath());

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error exporting report", e);
        }
    }

    private String getReportDisplayName(String reportType) {
        return switch (reportType) {
            case "cashbook" -> "CashBook";
            case "general_ledger" -> "GeneralLedger";
            case "trial_balance" -> "TrialBalance";
            case "income_statement" -> "IncomeStatement";
            case "balance_sheet" -> "BalanceSheet";
            case "cash_flow_statement" -> "CashFlowStatement";
            case "audit_trail" -> "AuditTrail";
            default -> "Report";
        };
    }

    private FiscalPeriod getFiscalPeriod(Long fiscalPeriodId) {
        return fiscalPeriodRepository.findById(fiscalPeriodId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fiscal period not found with ID: " + fiscalPeriodId + 
                        ". Please ensure the fiscal period exists in the database."));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return currencyFormat.format(amount);
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }

    private String truncateText(String text, int maxLength, int truncateLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, truncateLength) + "...";
    }
}