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

import fin.model.*;
import fin.repository.FinancialDataRepository;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Service for generating balance sheet reports.
 * Shows assets, liabilities, and equity positions.
 * 
 * FOLLOWS PROPER ACCOUNTING HIERARCHY:
 * Journal Entries > General Ledger > Trial Balance > Balance Sheet
 */
public class BalanceSheetService {

    // Display width constants
    private static final int DISPLAY_WIDTH_STANDARD = 80;
    private static final int DISPLAY_WIDTH_ACCOUNT_NAME = 43;
    private static final int DISPLAY_WIDTH_HEADER = 60;

    // Account range constants
    private static final int ACCOUNT_RANGE_ASSETS_MIN = 1000;
    private static final int ACCOUNT_RANGE_ASSETS_MAX = 2999;
    private static final int ACCOUNT_RANGE_LIABILITIES_MIN = 3000;
    private static final int ACCOUNT_RANGE_LIABILITIES_MAX = 4999;
    private static final int ACCOUNT_RANGE_EQUITY_MIN = 5000;
    private static final int ACCOUNT_RANGE_EQUITY_MAX = 5999;
    private static final int ACCOUNT_RANGE_REVENUE_MIN = 6000;
    private static final int ACCOUNT_RANGE_REVENUE_MAX = 7999;
    private static final int ACCOUNT_RANGE_EXPENSES_MIN = 8000;
    private static final int ACCOUNT_RANGE_EXPENSES_MAX = 9999;

    // Formatting constants
    private static final int ACCOUNT_CODE_MIN_LENGTH = 4;
    private static final int TRUNCATE_SUFFIX_LENGTH = 3;
    private static final String TRUNCATE_SUFFIX = "...";

    private final FinancialDataRepository repository;
    private final GeneralLedgerService generalLedgerService;

    /**
     * Constructor with dependency injection.
     *
     * NOTE: EI_EXPOSE_REP warning is intentionally suppressed for this constructor.
     * This is an architectural design decision for Dependency Injection pattern:
     * - Services are injected as constructor parameters to enable loose coupling
     * - Allows for better testability through mock injection
     * - Enables service composition for financial reporting workflows
     * - Maintains separation between data access and business logic
     * - Suppressions are configured in config/spotbugs/exclude.xml for all service constructors
     *
     * @param initialRepository the financial data repository for database operations
     * @param initialGeneralLedgerService the general ledger service for account balance calculations
     */
    public BalanceSheetService(FinancialDataRepository initialRepository, GeneralLedgerService initialGeneralLedgerService) {
        this.repository = initialRepository;
        this.generalLedgerService = initialGeneralLedgerService;
    }

    /**
     * Generate balance sheet report content
    /**
     * CORRECT ACCOUNTING FLOW:
     * This method reads closing balances FROM General Ledger (same as Trial Balance).
     * Implements: Journal Entries > General Ledger > Trial Balance > Balance Sheet
     */
    public String generateBalanceSheet(int companyId, int fiscalPeriodId) throws SQLException {
        Company company = repository.getCompany(companyId);
        FiscalPeriod fiscalPeriod = repository.getFiscalPeriod(fiscalPeriodId);
        
        // [OK] CORRECT FLOW: Get closing balances FROM General Ledger (same source as Trial Balance)
        Map<String, AccountBalance> accountBalances = generalLedgerService.getAccountClosingBalances(companyId, fiscalPeriodId);

        StringBuilder report = new StringBuilder();
        report.append(generateReportHeader("BALANCE SHEET", company, fiscalPeriod));
        report.append("\n");
        report.append("Source: General Ledger Account Balances (Double-Entry Bookkeeping)\n");
        report.append("Hierarchy: Journal Entries > General Ledger > Trial Balance > Balance Sheet\n");
        report.append("\n");

        // Categorize accounts by Balance Sheet type
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        // Calculate net profit/loss for retained earnings
        BigDecimal netProfit = calculateNetProfit(accountBalances);

        // Assets section (1000-2999)
        report.append("ASSETS\n");
        report.append("=".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isAssetAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), DISPLAY_WIDTH_ACCOUNT_NAME),
                            formatCurrency(amount)));
                    totalAssets = totalAssets.add(amount);
                }
            }
        }
        report.append("-".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL ASSETS", formatCurrency(totalAssets)));
        report.append("\n");

        // Liabilities section (3000-4999)
        report.append("LIABILITIES\n");
        report.append("=".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isLiabilityAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), DISPLAY_WIDTH_ACCOUNT_NAME),
                            formatCurrency(amount)));
                    totalLiabilities = totalLiabilities.add(amount);
                }
            }
        }
        report.append("-".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES", formatCurrency(totalLiabilities)));
        report.append("\n");

        // Equity section (5000-5999) + Retained Earnings
        report.append("EQUITY\n");
        report.append("=".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        
        // Show existing equity accounts first (opening balances)
        BigDecimal openingEquity = BigDecimal.ZERO;
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            if (isEquityAccount(accountCode)) {
                BigDecimal amount = balance.getClosingBalance();
                if (amount.compareTo(BigDecimal.ZERO) != 0) {
                    report.append(String.format("%-15s %-45s %15s%n",
                            accountCode,
                            truncateString(balance.getAccountName(), DISPLAY_WIDTH_ACCOUNT_NAME),
                            formatCurrency(amount)));
                    openingEquity = openingEquity.add(amount);
                }
            }
        }
        
        // Add Retained Earnings calculation: Opening Equity + Net Profit/Loss
        BigDecimal retainedEarnings = openingEquity.add(netProfit);
        report.append(String.format("%-15s %-45s %15s%n",
                "RETAINED",
                "Retained Earnings (Opening + Net Profit)",
                formatCurrency(retainedEarnings)));
        totalEquity = retainedEarnings;
        report.append("\n");

        // Total Liabilities and Equity
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);
        report.append("TOTAL LIABILITIES AND EQUITY\n");
        report.append("=".repeat(DISPLAY_WIDTH_STANDARD)).append("\n");
        report.append(String.format("%-60s %15s%n", "TOTAL LIABILITIES & EQUITY", formatCurrency(totalLiabilitiesAndEquity)));
        report.append("\n");

        // Balance Sheet equation check: Assets = Liabilities + Equity
        BigDecimal difference = totalAssets.subtract(totalLiabilitiesAndEquity);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            report.append("*** BALANCE SHEET DOES NOT BALANCE ***\n");
            report.append(String.format("Difference: %s%n", formatCurrency(difference.abs())));
            if (totalAssets.compareTo(totalLiabilitiesAndEquity) > 0) {
                report.append("Assets exceed Liabilities + Equity\n");
            } else {
                report.append("Liabilities + Equity exceed Assets\n");
            }
        } else {
            report.append("*** BALANCE SHEET BALANCES ***\n");
            report.append("Assets = Liabilities + Equity [OK]\n");
        }

        return report.toString();
    }

    /**
     * Calculate net profit/loss for the period
     * Net Profit = Total Revenue - Total Expenses
     * Revenue accounts: 6000-7999
     * Expense accounts: 8000-9999
     */
    private BigDecimal calculateNetProfit(Map<String, AccountBalance> accountBalances) {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;
        
        for (AccountBalance balance : accountBalances.values()) {
            String accountCode = balance.getAccountCode();
            BigDecimal amount = balance.getClosingBalance();
            
            // Revenue accounts (6000-7999) - CREDIT normal balance
            if (isRevenueAccount(accountCode)) {
                totalRevenue = totalRevenue.add(amount);
            }
            // Expense accounts (8000-9999) - DEBIT normal balance  
            else if (isExpenseAccount(accountCode)) {
                totalExpenses = totalExpenses.add(amount);
            }
        }
        
        // Net Profit = Revenue - Expenses
        return totalRevenue.subtract(totalExpenses);
    }

    /**
     * Check if account code represents an Asset (1000-2999)
     */
    private boolean isAssetAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < ACCOUNT_CODE_MIN_LENGTH) {
            return false;
        }
        try {
            int code = Integer.parseInt(accountCode);
            return code >= ACCOUNT_RANGE_ASSETS_MIN && code <= ACCOUNT_RANGE_ASSETS_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents a Liability (3000-4999)
     */
    private boolean isLiabilityAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < ACCOUNT_CODE_MIN_LENGTH) {
            return false;
        }
        try {
            int code = Integer.parseInt(accountCode);
            return code >= ACCOUNT_RANGE_LIABILITIES_MIN && code <= ACCOUNT_RANGE_LIABILITIES_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents Equity (5000-5999)
     */
    private boolean isEquityAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < ACCOUNT_CODE_MIN_LENGTH) {
            return false;
        }
        try {
            int code = Integer.parseInt(accountCode);
            return code >= ACCOUNT_RANGE_EQUITY_MIN && code <= ACCOUNT_RANGE_EQUITY_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if account code represents Revenue (6000-7999)
     */
    private boolean isRevenueAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < ACCOUNT_CODE_MIN_LENGTH) {
            return false;
        }
        // Handle account codes with dashes (e.g., "6100-001")
        String cleanCode = accountCode.split("-")[0];
        try {
            int code = Integer.parseInt(cleanCode);
            return code >= ACCOUNT_RANGE_REVENUE_MIN && code <= ACCOUNT_RANGE_REVENUE_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if account code represents Expenses (8000-9999)
     */
    private boolean isExpenseAccount(String accountCode) {
        if (accountCode == null || accountCode.length() < ACCOUNT_CODE_MIN_LENGTH) {
            return false;
        }
        // Handle account codes with dashes (e.g., "8100-001")
        String cleanCode = accountCode.split("-")[0];
        try {
            int code = Integer.parseInt(cleanCode);
            return code >= ACCOUNT_RANGE_EXPENSES_MIN && code <= ACCOUNT_RANGE_EXPENSES_MAX;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String generateReportHeader(String title, Company company, FiscalPeriod fiscalPeriod) {
        StringBuilder header = new StringBuilder();
        header.append(centerText(title, DISPLAY_WIDTH_HEADER)).append("\n");
        header.append(centerText("Company: " + company.getName(), DISPLAY_WIDTH_HEADER)).append("\n");
        header.append(centerText("Registration: " + company.getRegistrationNumber(), DISPLAY_WIDTH_HEADER)).append("\n");
        header.append(centerText("As at: " + fiscalPeriod.getEndDate(), DISPLAY_WIDTH_HEADER)).append("\n");
        header.append(centerText("Generated on: " + java.time.LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), DISPLAY_WIDTH_HEADER)).append("\n");
        return header.toString();
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text;
    }

    private String truncateString(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - TRUNCATE_SUFFIX_LENGTH) + TRUNCATE_SUFFIX;
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return "R      0.00";
        }
        return String.format("R %10.2f", amount);
    }
}