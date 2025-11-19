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

import fin.model.*;
import fin.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Spring Service for financial reporting using JPA repositories.
 * Generates comprehensive double-entry accounting reports with export functionality.
 */
@Service
@Transactional(readOnly = true)
public class SpringFinancialReportingService {

    private static final Logger LOGGER = Logger.getLogger(SpringFinancialReportingService.class.getName());
    private final NumberFormat currencyFormat;

    // Report formatting constants
    private static final int REPORT_SEPARATOR_WIDTH = 120;
    private static final int ACCOUNT_NAME_TRUNCATE_CHECK = 23;
    private static final int ACCOUNT_NAME_TRUNCATE_LENGTH = 20;
    private static final int DESCRIPTION_TRUNCATE_CHECK = 28;
    private static final int DESCRIPTION_TRUNCATE_LENGTH = 25;

    // Dependencies
    private final SpringCompanyService companyService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryLineRepository journalEntryLineRepository;

    public SpringFinancialReportingService(SpringCompanyService companyService,
                                         FiscalPeriodRepository fiscalPeriodRepository,
                                         AccountRepository accountRepository,
                                         JournalEntryRepository journalEntryRepository,
                                         JournalEntryLineRepository journalEntryLineRepository) {
        this.companyService = companyService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.accountRepository = accountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }

    /**
     * Generates a comprehensive General Ledger report
     */
    @Transactional(readOnly = true)
    public String generateGeneralLedger(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
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

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("general_ledger", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating general ledger", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Trial Balance report
     */
    @Transactional(readOnly = true)
    public String generateTrialBalance(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("TRIAL BALANCE", company, period));

            report.append(String.format("%-10s %-30s %15s %15s%n",
                    "Code", "Account Name", "Debit", "Credit"));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

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
                    report.append(String.format("%-10s %-30s %15s %15s%n",
                            account.getAccountCode(),
                            truncateText(account.getAccountName(), ACCOUNT_NAME_TRUNCATE_CHECK, ACCOUNT_NAME_TRUNCATE_LENGTH),
                            netBalance.compareTo(BigDecimal.ZERO) > 0 ? formatCurrency(netBalance) : "",
                            netBalance.compareTo(BigDecimal.ZERO) < 0 ? formatCurrency(netBalance.abs()) : ""));

                    if (netBalance.compareTo(BigDecimal.ZERO) > 0) {
                        totalDebits = totalDebits.add(netBalance);
                    } else {
                        totalCredits = totalCredits.add(netBalance.abs());
                    }
                }
            }

            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-40s %15s %15s%n",
                    "TOTALS:",
                    formatCurrency(totalDebits),
                    formatCurrency(totalCredits)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("trial_balance", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating trial balance", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates an Income Statement (Profit & Loss)
     */
    @Transactional(readOnly = true)
    public String generateIncomeStatement(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
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

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("income_statement", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating income statement", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Balance Sheet
     */
    @Transactional(readOnly = true)
    public String generateBalanceSheet(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
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
            report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES & EQUITY:", formatCurrency(totalLiabilitiesAndEquity)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("balance_sheet", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating balance sheet", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates a Cashbook report
     */
    @Transactional(readOnly = true)
    public String generateCashbook(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
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

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("cashbook", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating cashbook", e);
            return "Error generating report: " + e.getMessage();
        }
    }

    /**
     * Generates an Audit Trail report
     */
    @Transactional(readOnly = true)
    public String generateAuditTrail(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            Company company = companyService.getCompanyById(companyId);
            FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);

            StringBuilder report = new StringBuilder();
            report.append(generateReportHeader("AUDIT TRAIL", company, period));

            List<JournalEntry> journalEntries = journalEntryRepository
                .findByCompanyIdAndFiscalPeriodIdOrderByEntryDateAscIdAsc(companyId, fiscalPeriodId);

            String currentEntry = "";
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;

            for (JournalEntry je : journalEntries) {
                // New journal entry header
                if (!je.getReference().equals(currentEntry)) {
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

            String reportContent = report.toString();
            if (exportToFile) {
                exportReport("audit_trail", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating audit trail", e);
            return "Error generating report: " + e.getMessage();
        }
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

    private String generateReportHeader(String reportTitle, Company company, FiscalPeriod period) {
        StringBuilder header = new StringBuilder();
        header.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
        header.append(String.format("%s%n", centerText(reportTitle, REPORT_SEPARATOR_WIDTH)));
        header.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");

        if (company != null) {
            header.append(String.format("Company: %s%n", company.getName()));
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
        return fiscalPeriodRepository.findById(fiscalPeriodId).orElse(null);
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