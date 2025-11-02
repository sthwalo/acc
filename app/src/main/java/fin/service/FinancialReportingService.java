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

package fin.service;

import fin.model.FiscalPeriod;
import fin.model.Company;
import fin.repository.FinancialDataRepository;
import fin.repository.JdbcFinancialDataRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

/**
 * Enhanced Financial Reporting Service that generates comprehensive
 * double-entry accounting reports with export functionality.
 * Now uses modular services for better maintainability and data integrity.
 */
public class FinancialReportingService {
    private static final Logger LOGGER = Logger.getLogger(FinancialReportingService.class.getName());
    private final String dbUrl;
    private final NumberFormat currencyFormat;
    private final DataSource dataSource;
    private final FinancialDataRepository repository;

    // Database connection pool constants
    private static final int MAX_CONNECTION_POOL_SIZE = 10;
    private static final int MIN_IDLE_CONNECTIONS = 2;

    // Report formatting constants
    private static final int REPORT_SEPARATOR_WIDTH = 120;
    private static final int ACCOUNT_NAME_TRUNCATE_CHECK = 23;
    private static final int ACCOUNT_NAME_TRUNCATE_LENGTH = 20;
    private static final int DESCRIPTION_TRUNCATE_CHECK = 28;
    private static final int DESCRIPTION_TRUNCATE_LENGTH = 25;

    // Modular services
    private final CashbookService cashbookService;
    private final GeneralLedgerService generalLedgerService;
    private final TrialBalanceService trialBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final BalanceSheetService balanceSheetService;
    private final CashFlowService cashFlowService;

    public FinancialReportingService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));

        // Create DataSource from dbUrl
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(initialDbUrl);
        config.setMaximumPoolSize(MAX_CONNECTION_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE_CONNECTIONS);
        this.dataSource = new HikariDataSource(config);

        // Create repository
        this.repository = new JdbcFinancialDataRepository(dataSource);

        // Initialize modular services with repository
        this.cashbookService = new CashbookService(repository);
        this.generalLedgerService = new GeneralLedgerService(repository);
        this.trialBalanceService = new TrialBalanceService(repository, generalLedgerService);
        this.incomeStatementService = new IncomeStatementService(repository, generalLedgerService);
        this.balanceSheetService = new BalanceSheetService(repository, generalLedgerService);
        this.cashFlowService = new CashFlowService(repository, incomeStatementService);
    }
    
    /**
     * Generates a comprehensive General Ledger report
     */
    public String generateGeneralLedger(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular GeneralLedgerService
            String reportContent = generalLedgerService.generateGeneralLedger(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
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
    public String generateTrialBalance(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular TrialBalanceService
            String reportContent = trialBalanceService.generateTrialBalance(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
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
    public String generateIncomeStatement(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular IncomeStatementService
            String reportContent = incomeStatementService.generateIncomeStatement(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
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
    public String generateBalanceSheet(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular BalanceSheetService
            String reportContent = balanceSheetService.generateBalanceSheet(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
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
    public String generateCashbook(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular CashbookService
            String reportContent = cashbookService.generateCashbook(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
                exportReport("cashbook", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating cashbook", e);
            return "Error generating report: " + e.getMessage();
        }
    }
    
    /**
     * Generates a Cash Flow Statement
     */
    public String generateCashFlowStatement(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        try {
            // Use the modular CashFlowService
            String reportContent = cashFlowService.generateCashFlow(companyId.intValue(), fiscalPeriodId.intValue());

            if (exportToFile) {
                FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
                Company company = getCompany(companyId);
                exportReport("cash_flow_statement", company, period, reportContent);
            }

            return reportContent;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating cash flow statement", e);
            return "Error generating report: " + e.getMessage();
        }
    }
    public String generateAuditTrail(Long companyId, Long fiscalPeriodId, boolean exportToFile) {
        StringBuilder report = new StringBuilder();
        FiscalPeriod period = getFiscalPeriod(fiscalPeriodId);
        Company company = getCompany(companyId);
        
        // Report header
        report.append(generateReportHeader("AUDIT TRAIL", company, period));
        
        String sql = """
            SELECT 
                je.reference,
                je.entry_date,
                je.description as journal_description,
                je.created_by,
                je.created_at,
                a.account_code,
                a.account_name,
                jel.debit_amount,
                jel.credit_amount,
                jel.description as line_description,
                jel.reference as line_reference
            FROM journal_entries je
            JOIN journal_entry_lines jel ON je.id = jel.journal_entry_id
            JOIN accounts a ON jel.account_id = a.id
            WHERE je.company_id = ? AND je.fiscal_period_id = ?
            ORDER BY je.entry_date, je.id, jel.id
            """;
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            String currentEntry = "";
            BigDecimal grandTotalDebits = BigDecimal.ZERO;
            BigDecimal grandTotalCredits = BigDecimal.ZERO;
            
            while (rs.next()) {
                String reference = rs.getString("reference");
                java.sql.Date entryDate = rs.getDate("entry_date");
                String journalDescription = rs.getString("journal_description");
                String createdBy = rs.getString("created_by");
                Timestamp createdAt = rs.getTimestamp("created_at");
                
                // New journal entry header
                if (!reference.equals(currentEntry)) {
                    currentEntry = reference;
                    
                    report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                    report.append(String.format("ENTRY: %-20s DATE: %-12s CREATED BY: %-20s%n",
                            reference,
                            entryDate.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                            createdBy != null ? createdBy : "SYSTEM"));
                    report.append(String.format("DESCRIPTION: %s%n", journalDescription));
                    report.append(String.format("TIMESTAMP: %s%n", 
                            createdAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
                    report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                    report.append(String.format("%-10s %-25s %-30s %15s %15s%n",
                            "Code", "Account", "Description", "Debit", "Credit"));
                    report.append("-".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
                }
                
                // Journal entry line
                String accountCode = rs.getString("account_code");
                String accountName = rs.getString("account_name");
                String lineDescription = rs.getString("line_description");
                BigDecimal debitAmount = rs.getBigDecimal("debit_amount");
                BigDecimal creditAmount = rs.getBigDecimal("credit_amount");
                
                if (debitAmount != null) {
                    grandTotalDebits = grandTotalDebits.add(debitAmount);
                }
                if (creditAmount != null) {
                    grandTotalCredits = grandTotalCredits.add(creditAmount);
                }
                
                report.append(String.format("%-10s %-25s %-30s %15s %15s%n",
                        accountCode,
                        accountName.length() > ACCOUNT_NAME_TRUNCATE_CHECK ? accountName.substring(0, ACCOUNT_NAME_TRUNCATE_LENGTH) + "..." : accountName,
                        lineDescription != null && lineDescription.length() > DESCRIPTION_TRUNCATE_CHECK ? 
                            lineDescription.substring(0, DESCRIPTION_TRUNCATE_LENGTH) + "..." : lineDescription,
                        debitAmount != null ? formatCurrency(debitAmount) : "",
                        creditAmount != null ? formatCurrency(creditAmount) : ""));
            }
            
            // Grand totals
            report.append("\n").append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            report.append(String.format("%-71s %15s %15s%n", 
                    "GRAND TOTALS:", 
                    formatCurrency(grandTotalDebits),
                    formatCurrency(grandTotalCredits)));
            report.append("=".repeat(REPORT_SEPARATOR_WIDTH)).append("\n");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error generating audit trail", e);
            report.append("Error generating report: ").append(e.getMessage());
        }
        
        String reportContent = report.toString();
        if (exportToFile) {
            exportReport("audit_trail", company, period, reportContent);
        }
        
        return reportContent;
    }
    
    // Helper methods
    
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

            try (FileWriter writer = new FileWriter(filePath.toFile(), java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(content);
            }

            LOGGER.info("Report exported to: " + filePath.toAbsolutePath());

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
        String sql = "SELECT * FROM fiscal_periods WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                FiscalPeriod period = new FiscalPeriod();
                period.setId(rs.getLong("id"));
                period.setCompanyId(rs.getLong("company_id"));
                period.setPeriodName(rs.getString("period_name"));
                period.setStartDate(rs.getDate("start_date").toLocalDate());
                period.setEndDate(rs.getDate("end_date").toLocalDate());
                period.setClosed(rs.getBoolean("is_closed"));
                return period;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting fiscal period", e);
        }
        
        return null;
    }
    
    private Company getCompany(Long companyId) {
        String sql = "SELECT * FROM companies WHERE id = ?";
        
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Company company = new Company();
                company.setId(rs.getLong("id"));
                company.setName(rs.getString("name"));
                return company;
            }
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting company", e);
        }
        
        // Return default company if not found
        Company defaultCompany = new Company();
        defaultCompany.setId(companyId);
        defaultCompany.setName("Xinghizana Group");
        return defaultCompany;
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
}
