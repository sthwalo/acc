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

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Excel Financial Report Generator
 * Creates comprehensive financial reports in Excel format following the 
 * company's standard template structure with consistent formatting
 */
public class ExcelFinancialReportService {
    private static final Logger LOGGER = Logger.getLogger(ExcelFinancialReportService.class.getName());
    private final String dbUrl;
    
    // Font size constants for Excel reports
    private static final short FONT_SIZE_HEADER_LARGE = 14;
    
    // Excel row and column position constants
    private static final int ROW_COMPANY_NAME = 10; // Row 11 (0-indexed)
    private static final int ROW_REGISTRATION_NUMBER = 12; // Row 13 (0-indexed)
    private static final int ROW_ANNUAL_STATEMENTS = 13; // Row 14 (0-indexed)
    private static final int ROW_PERIOD = 14; // Row 15 (0-indexed)
    private static final int MAX_AUTO_SIZE_COLUMNS = 13;
    
    private static final int ROW_INDEX_INTRO = 6;
    private static final int ROW_INDEX_CONTENTS_HEADER = 7;
    private static final int ROW_INDEX_FIRST_ITEM = 8;
    
    private static final int ROW_COMPANY_DETAILS_INTRO = 6;
    private static final int ROW_COMPANY_DETAILS_FIRST = 7;
    private static final int COL_COMPANY_DETAILS_VALUE = 7; // Column H
    
    private static final int ROW_BALANCE_SHEET_PERIOD = 3;
    private static final int ROW_BALANCE_SHEET_HEADERS = 7;
    private static final int ROW_BALANCE_SHEET_UNITS = 8;
    private static final int ROW_BALANCE_SHEET_FIRST_DATA = 10;
    private static final int COL_BALANCE_SHEET_NOTE = 2;
    private static final int COL_BALANCE_SHEET_CURRENT_YEAR = 3;
    private static final int COL_BALANCE_SHEET_PRIOR_YEAR = 5;
    
    private static final int ROW_INCOME_STATEMENT_PERIOD = 3;
    private static final int ROW_INCOME_STATEMENT_HEADERS = 7;
    private static final int ROW_INCOME_STATEMENT_UNITS = 8;
    private static final int ROW_INCOME_STATEMENT_FIRST_DATA = 11;
    private static final int COL_INCOME_STATEMENT_NOTE = 1;
    private static final int COL_INCOME_STATEMENT_CURRENT_YEAR = 3;
    private static final int COL_INCOME_STATEMENT_PRIOR_YEAR = 5;
    
    private static final int ROW_COMPANY_HEADER_PERIOD = 3;
    
    public ExcelFinancialReportService(String initialDbUrl) {
        this.dbUrl = initialDbUrl;
    }
    
    /**
     * Generate comprehensive financial report workbook for a company
     */
    public void generateComprehensiveFinancialReport(Long companyId, Long fiscalPeriodId, String outputPath) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            
            // Get company and period info
            CompanyInfo company = getCompanyInfo(conn, companyId);
            FiscalPeriodInfo period = getFiscalPeriodInfo(conn, fiscalPeriodId);
            
            Workbook workbook = new HSSFWorkbook();
            
            // Create all sheets following the template structure
            createCoverSheet(workbook, company, period);
            createIndexSheet(workbook, company, period);
            createCompanyDetailsSheet(workbook, company, period);
            createResponsibilityStatement(workbook, company, period);
            createAuditReport(workbook, company, period);
            createDirectorsReport(workbook, company, period);
            createBalanceSheet(workbook, company, period, conn);
            createIncomeStatement(workbook, company, period, conn);
            createStatementOfChanges(workbook, company, period, conn);
            createCashFlowStatement(workbook, company, period, conn);
            createNotes1(workbook, company, period, conn);
            createNotes2to6(workbook, company, period, conn);
            createNotes7to10(workbook, company, period, conn);
            createNotes11to12(workbook, company, period, conn);
            createTrialBalance(workbook, company, period, conn);
            
            // Save the workbook
            String filename = String.format("%s_Financial_Report_%s_%s.xls", 
                company.name.replaceAll("[^a-zA-Z0-9]", "_"),
                period.name.replaceAll("[^a-zA-Z0-9]", "_"),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            
            String fullPath = outputPath + "/" + filename;
            
            try (FileOutputStream fileOut = new FileOutputStream(fullPath)) {
                workbook.write(fileOut);
            }
            
            workbook.close();
            
            LOGGER.info("Excel Financial Report generated: " + fullPath);
            
        } catch (SQLException | IOException e) {
            LOGGER.log(Level.SEVERE, "Error generating Excel financial report", e);
            throw new RuntimeException("Failed to generate Excel financial report", e);
        }
    }
    
    private void createCoverSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Cover");
        
        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints(FONT_SIZE_HEADER_LARGE);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // Create company name (row 11 as per template)
        Row row11 = sheet.createRow(ROW_COMPANY_NAME);
        Cell companyCell = row11.createCell(0);
        companyCell.setCellValue(company.name.toUpperCase());
        companyCell.setCellStyle(headerStyle);
        
        // Registration number (row 13)
        Row row13 = sheet.createRow(ROW_REGISTRATION_NUMBER);
        Cell regCell = row13.createCell(0);
        regCell.setCellValue("(Registration Number: " + company.registrationNumber + ")");
        
        // Annual Financial Statements (row 14)
        Row row14 = sheet.createRow(ROW_ANNUAL_STATEMENTS);
        Cell titleCell = row14.createCell(0);
        titleCell.setCellValue("ANNUAL FINANCIAL STATEMENTS");
        titleCell.setCellStyle(headerStyle);
        
        // Period (row 15)
        Row row15 = sheet.createRow(ROW_PERIOD);
        Cell periodCell = row15.createCell(0);
        periodCell.setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Auto-size columns
        for (int i = 0; i < MAX_AUTO_SIZE_COLUMNS; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createIndexSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Index");
        
        // Company header
        createCompanyHeader(workbook, sheet, company, period, "Annual Financial Statements");
        
        Row row7 = sheet.createRow(ROW_INDEX_INTRO);
        row7.createCell(0).setCellValue("The reports and statements set out below comprise the annual financial statements presented to the members:");
        
        // Contents header
        Row row8 = sheet.createRow(ROW_INDEX_CONTENTS_HEADER);
        row8.createCell(0).setCellValue("Contents");
        row8.createCell(1).setCellValue("Page");
        
        // Index items
        String[][] indexItems = {
            {"General information", "2"},
            {"Statement of financial responsibility by the directors", "3"},
            {"Report of the independent auditors", "4 - 5"},
            {"Report of the directors", "6"},
            {"Statement of financial position", "7"},
            {"Statement of comprehensive income", "8"},
            {"Statement of changes in equity", "9"},
            {"Statement of cash flows", "10"},
            {"Notes to the annual financial statements", "11 - 15"}
        };
        
        for (int i = 0; i < indexItems.length; i++) {
            Row row = sheet.createRow(ROW_INDEX_FIRST_ITEM + i);
            row.createCell(0).setCellValue(indexItems[i][0]);
            row.createCell(1).setCellValue(indexItems[i][1]);
        }
    }
    
    private void createCompanyDetailsSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Co details");
        
        createCompanyHeader(workbook, sheet, company, period, "Annual Financial Statements");
        
        Row row7 = sheet.createRow(ROW_COMPANY_DETAILS_INTRO);
        row7.createCell(0).setCellValue("General information");
        
        // Company details - ALL fetched from database
        String[][] details = {
            {"Country of incorporation", company.countryOfIncorporation != null ? company.countryOfIncorporation : "South Africa"},
            {"Nature of business", company.businessNature},
            {"Director(s)", company.directors},
            {"Company secretary", company.secretary},
            {"Registered address", company.address != null ? company.address : "N/A"},
            {"Business address", company.businessAddress},
            {"Auditors", company.auditors},
            {"Bankers", company.bankers},
            {"Registration number", company.registrationNumber}
        };
        
        for (int i = 0; i < details.length; i++) {
            Row row = sheet.createRow(ROW_COMPANY_DETAILS_FIRST + i);
            row.createCell(0).setCellValue(details[i][0]);
            row.createCell(COL_COMPANY_DETAILS_VALUE).setCellValue(details[i][1]); // Column H as per template
        }
    }
    
    private void createBalanceSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Balance sheet");
        
        createCompanyHeader(workbook, sheet, company, period, "Statement of Financial Position");
        Row row4 = sheet.createRow(ROW_BALANCE_SHEET_PERIOD);
        row4.createCell(0).setCellValue("as at " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Headers
        Row row8 = sheet.createRow(ROW_BALANCE_SHEET_HEADERS);
        row8.createCell(COL_BALANCE_SHEET_NOTE).setCellValue("Note");
        row8.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(period.endDate.getYear());
        row8.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue(period.startDate.getYear());
        
        Row row9 = sheet.createRow(ROW_BALANCE_SHEET_UNITS);
        row9.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue("R");
        row9.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue("R");
        
        // Get balance sheet data
        List<BalanceSheetItem> assets = getBalanceSheetAssets(conn, company.id, period.id);
        List<BalanceSheetItem> liabilities = getBalanceSheetLiabilities(conn, company.id, period.id);
        List<BalanceSheetItem> equity = getBalanceSheetEquity(conn, company.id, period.id);
        
        int currentRow = ROW_BALANCE_SHEET_FIRST_DATA;
        
        // ASSETS
        Row assetsRow = sheet.createRow(currentRow++);
        assetsRow.createCell(0).setCellValue("ASSETS");
        currentRow++;
        
        // Non-current assets
        Row nonCurrentRow = sheet.createRow(currentRow++);
        nonCurrentRow.createCell(0).setCellValue("Non-current assets");
        currentRow++;
        
        double totalNonCurrentAssets = 0;
        for (BalanceSheetItem item : assets) {
            if (item.isNonCurrent) {
                Row row = sheet.createRow(currentRow++);
                row.createCell(0).setCellValue(item.accountName);
                row.createCell(1).setCellValue(item.noteReference);
                row.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(item.currentYearAmount);
                row.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue(item.priorYearAmount);
                totalNonCurrentAssets += item.currentYearAmount;
            }
        }
        
        // Total non-current assets
        Row totalNonCurrentRow = sheet.createRow(currentRow++);
        totalNonCurrentRow.createCell(0).setCellValue("Total non-current assets");
        totalNonCurrentRow.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(totalNonCurrentAssets);
        currentRow++;
        
        // Current assets
        Row currentAssetsRow = sheet.createRow(currentRow++);
        currentAssetsRow.createCell(0).setCellValue("Current assets");
        
        double totalCurrentAssets = 0;
        for (BalanceSheetItem item : assets) {
            if (!item.isNonCurrent) {
                Row row = sheet.createRow(currentRow++);
                row.createCell(0).setCellValue(item.accountName);
                row.createCell(1).setCellValue(item.noteReference);
                row.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(item.currentYearAmount);
                row.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue(item.priorYearAmount);
                totalCurrentAssets += item.currentYearAmount;
            }
        }
        
        // Total assets
        Row totalAssetsRow = sheet.createRow(currentRow++);
        totalAssetsRow.createCell(0).setCellValue("TOTAL ASSETS");
        totalAssetsRow.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(totalNonCurrentAssets + totalCurrentAssets);
        currentRow += 2;
        
        // EQUITY AND LIABILITIES
        Row equityLiabRow = sheet.createRow(currentRow++);
        equityLiabRow.createCell(0).setCellValue("EQUITY AND LIABILITIES");
        currentRow++;
        
        // Equity
        Row equityRow = sheet.createRow(currentRow++);
        equityRow.createCell(0).setCellValue("Equity");
        
        double totalEquity = 0;
        for (BalanceSheetItem item : equity) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(item.accountName);
            row.createCell(1).setCellValue(item.noteReference);
            row.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(item.currentYearAmount);
            row.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue(item.priorYearAmount);
            totalEquity += item.currentYearAmount;
        }
        
        // Liabilities
        currentRow++;
        Row liabilitiesRow = sheet.createRow(currentRow++);
        liabilitiesRow.createCell(0).setCellValue("Liabilities");
        
        double totalLiabilities = 0;
        for (BalanceSheetItem item : liabilities) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(item.accountName);
            row.createCell(1).setCellValue(item.noteReference);
            row.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(item.currentYearAmount);
            row.createCell(COL_BALANCE_SHEET_PRIOR_YEAR).setCellValue(item.priorYearAmount);
            totalLiabilities += item.currentYearAmount;
        }
        
        // Total equity and liabilities
        Row totalEquityLiabRow = sheet.createRow(currentRow++);
        totalEquityLiabRow.createCell(0).setCellValue("TOTAL EQUITY AND LIABILITIES");
        totalEquityLiabRow.createCell(COL_BALANCE_SHEET_CURRENT_YEAR).setCellValue(totalEquity + totalLiabilities);
    }
    
    private void createIncomeStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Income statement");
        
        createCompanyHeader(workbook, sheet, company, period, "Statement of Comprehensive Income");
        Row row4 = sheet.createRow(ROW_INCOME_STATEMENT_PERIOD);
        row4.createCell(0).setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Headers
        Row row8 = sheet.createRow(ROW_INCOME_STATEMENT_HEADERS);
        row8.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue(period.endDate.getYear());
        row8.createCell(COL_INCOME_STATEMENT_PRIOR_YEAR).setCellValue(period.startDate.getYear());
        
        Row row9 = sheet.createRow(ROW_INCOME_STATEMENT_UNITS);
        row9.createCell(COL_INCOME_STATEMENT_NOTE).setCellValue("Note");
        row9.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue("R");
        row9.createCell(COL_INCOME_STATEMENT_PRIOR_YEAR).setCellValue("R");
        
        // Get income statement data
        List<IncomeStatementItem> revenues = getIncomeStatementRevenues(conn, company.id, period.id);
        List<IncomeStatementItem> expenses = getIncomeStatementExpenses(conn, company.id, period.id);
        
        int currentRow = ROW_INCOME_STATEMENT_FIRST_DATA;
        
        // Revenue
        Row revenueRow = sheet.createRow(currentRow++);
        revenueRow.createCell(0).setCellValue("Revenue");
        revenueRow.createCell(COL_INCOME_STATEMENT_NOTE).setCellValue("2");
        
        double totalRevenue = 0;
        for (IncomeStatementItem item : revenues) {
            totalRevenue += item.amount;
        }
        revenueRow.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue(totalRevenue);
        currentRow++;
        
        // Other income
        Row otherIncomeRow = sheet.createRow(currentRow++);
        otherIncomeRow.createCell(0).setCellValue("Other income");
        otherIncomeRow.createCell(COL_INCOME_STATEMENT_NOTE).setCellValue("2.1");
        otherIncomeRow.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue(0.0); // Placeholder
        currentRow++;
        
        // Expenses
        double totalExpenses = 0;
        for (IncomeStatementItem item : expenses) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(item.accountName);
            row.createCell(COL_INCOME_STATEMENT_NOTE).setCellValue(item.noteReference);
            row.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue(-Math.abs(item.amount)); // Show as negative
            totalExpenses += Math.abs(item.amount);
        }
        
        currentRow++;
        
        // Net surplus/deficit
        Row netRow = sheet.createRow(currentRow++);
        netRow.createCell(0).setCellValue("Net surplus/(deficit) for the year");
        double netResult = totalRevenue - totalExpenses;
        netRow.createCell(COL_INCOME_STATEMENT_CURRENT_YEAR).setCellValue(netResult);
    }
    
    // Helper method to create company header
    private void createCompanyHeader(Workbook workbook, Sheet sheet, CompanyInfo company, FiscalPeriodInfo period, String reportTitle) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        
        Row row1 = sheet.createRow(0);
        Cell companyCell = row1.createCell(0);
        companyCell.setCellValue(company.name.toUpperCase());
        companyCell.setCellStyle(headerStyle);
        
        Row row2 = sheet.createRow(1);
        row2.createCell(0).setCellValue("(Registration Number: " + company.registrationNumber + ")");
        
        Row row3 = sheet.createRow(2);
        Cell titleCell = row3.createCell(0);
        titleCell.setCellValue(reportTitle);
        titleCell.setCellStyle(headerStyle);
        
        Row row4 = sheet.createRow(ROW_COMPANY_HEADER_PERIOD);
        row4.createCell(0).setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    // Add placeholder methods for other sheets
    private void createResponsibilityStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) throws SQLException {
        Sheet sheet = workbook.createSheet("State of responsibility");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Financial Responsibility by the Directors");
        
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.TOP);
        
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        
        int currentRow = ROW_INDEX_INTRO;
        
        // Fetch responsibility statement from database - NO FALLBACK
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            DirectorsResponsibilityData responsibilityData = getDirectorsResponsibilityData(conn, company.id, period.id);
            
            if (responsibilityData == null || responsibilityData.statementText == null) {
                throw new SQLException("Directors' Responsibility Statement not found in database for company " + company.id + 
                    " and fiscal period " + period.id + ". Please insert data into directors_responsibility_statements table.");
            }
            
            // Use database statement
            createResponsibilityStatementFromDatabase(sheet, company, period, responsibilityData, boldStyle, normalStyle, currentRow);
        }
        
        // Set column width for better readability
        sheet.setColumnWidth(0, 15000);
    }
    
    private void createResponsibilityStatementFromDatabase(Sheet sheet, CompanyInfo company, FiscalPeriodInfo period,
                                                          DirectorsResponsibilityData data, CellStyle boldStyle,
                                                          CellStyle normalStyle, int startRow) {
        int currentRow = startRow;
        
        // Statement header
        Row headerRow = sheet.createRow(currentRow++);
        Cell headerCell = headerRow.createCell(0);
        headerCell.setCellValue("Statement of Financial Responsibility by the Directors");
        headerCell.setCellStyle(boldStyle);
        currentRow++;
        
        // Custom statement text from database
        Row statementRow = sheet.createRow(currentRow++);
        Cell statementCell = statementRow.createCell(0);
        statementCell.setCellValue(data.statementText);
        statementCell.setCellStyle(normalStyle);
        
        // Calculate appropriate row height based on text length (roughly 20 points per 100 chars)
        int textLength = data.statementText != null ? data.statementText.length() : 0;
        float rowHeight = Math.max(40, Math.min(200, textLength / 5));
        sheet.getRow(currentRow - 1).setHeightInPoints(rowHeight);
        currentRow += 2;
        
        // Approval section
        Row approvalRow = sheet.createRow(currentRow++);
        Cell approvalCell = approvalRow.createCell(0);
        String approvalText = "The annual financial statements set out on pages 7 to 15 were approved by the board of directors";
        if (data.statementDate != null) {
            approvalText += " on " + data.statementDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        }
        approvalText += " and are signed on its behalf by:";
        approvalCell.setCellValue(approvalText);
        approvalCell.setCellStyle(normalStyle);
        sheet.getRow(currentRow - 1).setHeightInPoints(40);
        currentRow += 3;
        
        // Signature lines
        Row signatureLine1 = sheet.createRow(currentRow++);
        signatureLine1.createCell(0).setCellValue("_______________________");
        
        Row directorName = sheet.createRow(currentRow++);
        Cell directorCell = directorName.createCell(0);
        String approverText = data.approvedBy != null ? data.approvedBy : "Director";
        if (data.approvedByTitle != null) {
            approverText += " (" + data.approvedByTitle + ")";
        }
        directorCell.setCellValue(approverText);
        directorCell.setCellStyle(boldStyle);
        currentRow++;
        
        // Witness section (if provided)
        if (data.witnessName != null) {
            currentRow++;
            Row witnessLabelRow = sheet.createRow(currentRow++);
            witnessLabelRow.createCell(0).setCellValue("Witnessed by:");
            currentRow++;
            
            Row witnessSignatureLine = sheet.createRow(currentRow++);
            witnessSignatureLine.createCell(0).setCellValue("_______________________");
            
            Row witnessNameRow = sheet.createRow(currentRow++);
            Cell witnessCell = witnessNameRow.createCell(0);
            String witnessText = data.witnessName;
            if (data.witnessTitle != null) {
                witnessText += " (" + data.witnessTitle + ")";
            }
            witnessCell.setCellValue(witnessText);
            witnessCell.setCellStyle(boldStyle);
        }
        
        currentRow += 2;
        Row dateRow = sheet.createRow(currentRow++);
        String dateText = "Date: ";
        dateText += data.statementDate != null ? 
            data.statementDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) :
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        dateRow.createCell(0).setCellValue(dateText);
    }
    
    private void createAuditReport(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) throws SQLException {
        Sheet sheet = workbook.createSheet("Audit report");
        createCompanyHeader(workbook, sheet, company, period, "Report of the Independent Auditors");
        
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.TOP);
        
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        boldStyle.setWrapText(true);
        
        int currentRow = ROW_INDEX_INTRO;
        
        // Fetch audit report from database - NO FALLBACK
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            AuditReportData auditData = getAuditReportData(conn, company.id, period.id);
            
            if (auditData == null || auditData.opinionText == null) {
                throw new SQLException("Audit Report not found in database for company " + company.id + 
                    " and fiscal period " + period.id + ". Please insert data into audit_reports table.");
            }
            
            // Use database audit report
            createAuditReportFromDatabase(sheet, company, period, auditData, boldStyle, normalStyle, currentRow);
        }
        
        // Set column width for better readability
        sheet.setColumnWidth(0, 15000);
    }
    
    private void createAuditReportFromDatabase(Sheet sheet, CompanyInfo company, FiscalPeriodInfo period,
                                              AuditReportData auditData, CellStyle boldStyle, 
                                              CellStyle normalStyle, int startRow) {
        int currentRow = startRow;
        
        // To the members
        Row row1 = sheet.createRow(currentRow++);
        Cell toMembersCell = row1.createCell(0);
        toMembersCell.setCellValue("To the members of " + company.name);
        toMembersCell.setCellStyle(boldStyle);
        currentRow++;
        
        // Opinion section
        Row opinionHeaderRow = sheet.createRow(currentRow++);
        Cell opinionHeaderCell = opinionHeaderRow.createCell(0);
        opinionHeaderCell.setCellValue("Opinion");
        opinionHeaderCell.setCellStyle(boldStyle);
        
        Row opinionRow = sheet.createRow(currentRow++);
        Cell opinionCell = opinionRow.createCell(0);
        opinionCell.setCellValue(auditData.opinionText);
        opinionCell.setCellStyle(normalStyle);
        sheet.getRow(currentRow - 1).setHeightInPoints(80);
        currentRow += 2;
        
        // Basis for Opinion (if available)
        if (auditData.basisForOpinion != null) {
            Row basisHeaderRow = sheet.createRow(currentRow++);
            Cell basisHeaderCell = basisHeaderRow.createCell(0);
            basisHeaderCell.setCellValue("Basis for Opinion");
            basisHeaderCell.setCellStyle(boldStyle);
            
            Row basisRow = sheet.createRow(currentRow++);
            Cell basisCell = basisRow.createCell(0);
            basisCell.setCellValue(auditData.basisForOpinion);
            basisCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(100);
            currentRow += 2;
        }
        
        // Key Audit Matters (if available)
        if (auditData.keyAuditMatters != null) {
            Row kamHeaderRow = sheet.createRow(currentRow++);
            Cell kamHeaderCell = kamHeaderRow.createCell(0);
            kamHeaderCell.setCellValue("Key Audit Matters");
            kamHeaderCell.setCellStyle(boldStyle);
            
            Row kamRow = sheet.createRow(currentRow++);
            Cell kamCell = kamRow.createCell(0);
            kamCell.setCellValue(auditData.keyAuditMatters);
            kamCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(100);
            currentRow += 2;
        }
        
        // Management Responsibilities (if available)
        if (auditData.responsibilitiesManagement != null) {
            Row mgmtHeaderRow = sheet.createRow(currentRow++);
            Cell mgmtHeaderCell = mgmtHeaderRow.createCell(0);
            mgmtHeaderCell.setCellValue("Management's Responsibilities");
            mgmtHeaderCell.setCellStyle(boldStyle);
            
            Row mgmtRow = sheet.createRow(currentRow++);
            Cell mgmtCell = mgmtRow.createCell(0);
            mgmtCell.setCellValue(auditData.responsibilitiesManagement);
            mgmtCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(80);
            currentRow += 2;
        }
        
        // Auditor Responsibilities (if available)
        if (auditData.responsibilitiesAuditor != null) {
            Row auditorHeaderRow = sheet.createRow(currentRow++);
            Cell auditorHeaderCell = auditorHeaderRow.createCell(0);
            auditorHeaderCell.setCellValue("Auditor's Responsibilities");
            auditorHeaderCell.setCellStyle(boldStyle);
            
            Row auditorRow = sheet.createRow(currentRow++);
            Cell auditorCell = auditorRow.createCell(0);
            auditorCell.setCellValue(auditData.responsibilitiesAuditor);
            auditorCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(80);
            currentRow += 2;
        }
        
        // Signature section
        Row signatureRow = sheet.createRow(currentRow++);
        signatureRow.createCell(0).setCellValue("_______________________");
        currentRow++;
        
        Row auditorNameRow = sheet.createRow(currentRow++);
        Cell auditorNameCell = auditorNameRow.createCell(0);
        auditorNameCell.setCellValue(auditData.auditorFirmName != null ? auditData.auditorFirmName : company.auditors);
        auditorNameCell.setCellStyle(boldStyle);
        
        if (auditData.auditorPartnerName != null) {
            Row partnerRow = sheet.createRow(currentRow++);
            partnerRow.createCell(0).setCellValue(auditData.auditorPartnerName);
        }
        
        Row dateRow = sheet.createRow(currentRow++);
        String reportDate = auditData.reportDate != null ? 
            auditData.reportDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) :
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        dateRow.createCell(0).setCellValue("Date: " + reportDate);
    }
    
    private void createDirectorsReport(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) throws SQLException {
        Sheet sheet = workbook.createSheet("Directors report");
        createCompanyHeader(workbook, sheet, company, period, "Directors' Report");
        
        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setWrapText(true);
        normalStyle.setVerticalAlignment(VerticalAlignment.TOP);
        
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);
        boldStyle.setWrapText(true);
        
        int currentRow = ROW_INDEX_INTRO;
        
        // Fetch directors' report from database - NO FALLBACK
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            DirectorsReportData reportData = getDirectorsReportData(conn, company.id, period.id);
            
            if (reportData == null || reportData.natureOfBusiness == null) {
                throw new SQLException("Directors' Report not found in database for company " + company.id + 
                    " and fiscal period " + period.id + ". Please insert data into directors_reports table.");
            }
            
            // Use database report
            createDirectorsReportFromDatabase(sheet, company, period, reportData, boldStyle, normalStyle, currentRow);
        }
        
        // Set column width for better readability
        sheet.setColumnWidth(0, 15000);
    }
    
    private void createDirectorsReportFromDatabase(Sheet sheet, CompanyInfo company, FiscalPeriodInfo period,
                                                   DirectorsReportData data, CellStyle boldStyle,
                                                   CellStyle normalStyle, int startRow) {
        int currentRow = startRow;
        
        // Nature of business section
        Row natureHeaderRow = sheet.createRow(currentRow++);
        Cell natureHeaderCell = natureHeaderRow.createCell(0);
        natureHeaderCell.setCellValue("1. Nature of Business");
        natureHeaderCell.setCellStyle(boldStyle);
        
        Row natureRow = sheet.createRow(currentRow++);
        Cell natureCell = natureRow.createCell(0);
        natureCell.setCellValue(data.natureOfBusiness);
        natureCell.setCellStyle(normalStyle);
        int natureHeight = Math.max(40, Math.min(100, data.natureOfBusiness.length() / 8));
        sheet.getRow(currentRow - 1).setHeightInPoints(natureHeight);
        currentRow += 2;
        
        // Review of operations
        if (data.reviewOfOperations != null) {
            Row reviewHeaderRow = sheet.createRow(currentRow++);
            Cell reviewHeaderCell = reviewHeaderRow.createCell(0);
            reviewHeaderCell.setCellValue("2. Review of Operations");
            reviewHeaderCell.setCellStyle(boldStyle);
            
            Row reviewRow = sheet.createRow(currentRow++);
            Cell reviewCell = reviewRow.createCell(0);
            reviewCell.setCellValue(data.reviewOfOperations);
            reviewCell.setCellStyle(normalStyle);
            int reviewHeight = Math.max(40, Math.min(120, data.reviewOfOperations.length() / 8));
            sheet.getRow(currentRow - 1).setHeightInPoints(reviewHeight);
            currentRow += 2;
        }
        
        // Directors section
        if (data.directorsNames != null) {
            Row directorsHeaderRow = sheet.createRow(currentRow++);
            Cell directorsHeaderCell = directorsHeaderRow.createCell(0);
            directorsHeaderCell.setCellValue("3. Directors");
            directorsHeaderCell.setCellStyle(boldStyle);
            
            Row directorsRow = sheet.createRow(currentRow++);
            Cell directorsCell = directorsRow.createCell(0);
            directorsCell.setCellValue("The directors of the company during the year and to the date of this report are as follows:\n" + data.directorsNames);
            directorsCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(60);
            currentRow += 2;
        }
        
        // Going concern
        if (data.goingConcernAssessment != null) {
            Row concernHeaderRow = sheet.createRow(currentRow++);
            Cell concernHeaderCell = concernHeaderRow.createCell(0);
            concernHeaderCell.setCellValue("4. Going Concern");
            concernHeaderCell.setCellStyle(boldStyle);
            
            Row concernRow = sheet.createRow(currentRow++);
            Cell concernCell = concernRow.createCell(0);
            concernCell.setCellValue(data.goingConcernAssessment);
            concernCell.setCellStyle(normalStyle);
            int concernHeight = Math.max(40, Math.min(100, data.goingConcernAssessment.length() / 8));
            sheet.getRow(currentRow - 1).setHeightInPoints(concernHeight);
            currentRow += 2;
        }
        
        // Dividends
        if (data.dividendsPaid != null) {
            Row divHeaderRow = sheet.createRow(currentRow++);
            Cell divHeaderCell = divHeaderRow.createCell(0);
            divHeaderCell.setCellValue("5. Dividends");
            divHeaderCell.setCellStyle(boldStyle);
            
            Row divRow = sheet.createRow(currentRow++);
            Cell divCell = divRow.createCell(0);
            divCell.setCellValue(data.dividendsPaid);
            divCell.setCellStyle(normalStyle);
            sheet.getRow(currentRow - 1).setHeightInPoints(40);
            currentRow += 2;
        }
        
        // Share capital changes
        if (data.shareCapitalChanges != null) {
            Row shareHeaderRow = sheet.createRow(currentRow++);
            Cell shareHeaderCell = shareHeaderRow.createCell(0);
            shareHeaderCell.setCellValue("6. Share Capital");
            shareHeaderCell.setCellStyle(boldStyle);
            
            Row shareRow = sheet.createRow(currentRow++);
            Cell shareCell = shareRow.createCell(0);
            shareCell.setCellValue(data.shareCapitalChanges);
            shareCell.setCellStyle(normalStyle);
            int shareHeight = Math.max(40, Math.min(80, data.shareCapitalChanges.length() / 8));
            sheet.getRow(currentRow - 1).setHeightInPoints(shareHeight);
            currentRow += 2;
        }
        
        // Events after reporting date
        if (data.eventsAfterReportingDate != null) {
            Row eventsHeaderRow = sheet.createRow(currentRow++);
            Cell eventsHeaderCell = eventsHeaderRow.createCell(0);
            eventsHeaderCell.setCellValue("7. Events After the Reporting Date");
            eventsHeaderCell.setCellStyle(boldStyle);
            
            Row eventsRow = sheet.createRow(currentRow++);
            Cell eventsCell = eventsRow.createCell(0);
            eventsCell.setCellValue(data.eventsAfterReportingDate);
            eventsCell.setCellStyle(normalStyle);
            int eventsHeight = Math.max(40, Math.min(80, data.eventsAfterReportingDate.length() / 8));
            sheet.getRow(currentRow - 1).setHeightInPoints(eventsHeight);
            currentRow += 2;
        }
        
        // Future prospects
        if (data.futureProspects != null) {
            Row prospectHeaderRow = sheet.createRow(currentRow++);
            Cell prospectHeaderCell = prospectHeaderRow.createCell(0);
            prospectHeaderCell.setCellValue("8. Future Prospects");
            prospectHeaderCell.setCellStyle(boldStyle);
            
            Row prospectRow = sheet.createRow(currentRow++);
            Cell prospectCell = prospectRow.createCell(0);
            prospectCell.setCellValue(data.futureProspects);
            prospectCell.setCellStyle(normalStyle);
            int prospectHeight = Math.max(40, Math.min(100, data.futureProspects.length() / 8));
            sheet.getRow(currentRow - 1).setHeightInPoints(prospectHeight);
            currentRow += 2;
        }
        
        // Signature
        Row signatureRow = sheet.createRow(currentRow++);
        signatureRow.createCell(0).setCellValue("_______________________");
        currentRow++;
        
        Row directorRow = sheet.createRow(currentRow++);
        Cell directorCell = directorRow.createCell(0);
        directorCell.setCellValue("Director");
        directorCell.setCellStyle(boldStyle);
        
        Row dateRow = sheet.createRow(currentRow++);
        String dateText = "Date: " + (data.reportDate != null ? 
            data.reportDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")) :
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        dateRow.createCell(0).setCellValue(dateText);
    }
    
    private void createStatementOfChanges(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) throws SQLException {
        Sheet sheet = workbook.createSheet("State of changes");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Changes in Equity");
        Row row4 = sheet.createRow(ROW_BALANCE_SHEET_PERIOD);
        row4.createCell(0).setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        
        // Fetch equity movement data from database - NO FALLBACK
        EquityMovementData equityData = getEquityMovementData(conn, company.id, period.id);
        
        if (equityData == null) {
            throw new SQLException("Statement of Changes in Equity data not found in database for company " + company.id + 
                " and fiscal period " + period.id + ". Please insert data into equity_movements table.");
        }
        
        createStatementOfChangesFromDatabase(sheet, equityData, headerStyle, currencyStyle);
    }
    
    private void createStatementOfChangesFromDatabase(Sheet sheet, EquityMovementData data, 
                                                      CellStyle headerStyle, CellStyle currencyStyle) {
        int currentRow = 7;
        
        // Column headers
        Row headerRow = sheet.createRow(currentRow++);
        headerRow.createCell(0).setCellValue("");
        Cell col1 = headerRow.createCell(1);
        col1.setCellValue("Share Capital");
        col1.setCellStyle(headerStyle);
        Cell col2 = headerRow.createCell(2);
        col2.setCellValue("Retained Earnings");
        col2.setCellStyle(headerStyle);
        Cell col3 = headerRow.createCell(3);
        col3.setCellValue("Revaluation Reserve");
        col3.setCellStyle(headerStyle);
        Cell col4 = headerRow.createCell(4);
        col4.setCellValue("Other Reserves");
        col4.setCellStyle(headerStyle);
        Cell col5 = headerRow.createCell(5);
        col5.setCellValue("Total");
        col5.setCellStyle(headerStyle);
        
        Row unitsRow = sheet.createRow(currentRow++);
        unitsRow.createCell(0).setCellValue("");
        for (int i = 1; i <= 5; i++) {
            Cell unit = unitsRow.createCell(i);
            unit.setCellValue("R");
            unit.setCellStyle(headerStyle);
        }
        
        currentRow++;
        
        // Calculate opening total
        double openingTotal = data.openingBalanceShareCapital + data.openingBalanceRetainedEarnings + 
                             data.openingBalanceRevalutationReserve + data.openingBalanceOtherReserves;
        
        // Opening balance
        Row openingRow = sheet.createRow(currentRow++);
        openingRow.createCell(0).setCellValue("Balance at beginning of year");
        openingRow.createCell(1).setCellValue(data.openingBalanceShareCapital);
        openingRow.createCell(2).setCellValue(data.openingBalanceRetainedEarnings);
        openingRow.createCell(3).setCellValue(data.openingBalanceRevalutationReserve);
        openingRow.createCell(4).setCellValue(data.openingBalanceOtherReserves);
        openingRow.createCell(5).setCellValue(openingTotal);
        
        currentRow++;
        
        // Movements header
        Row movementsHeaderRow = sheet.createRow(currentRow++);
        Cell movementsCell = movementsHeaderRow.createCell(0);
        movementsCell.setCellValue("Changes in equity");
        movementsCell.setCellStyle(headerStyle);
        
        // Profit for the year
        if (data.profitLoss != 0) {
            Row profitRow = sheet.createRow(currentRow++);
            profitRow.createCell(0).setCellValue("Profit/(loss) for the year");
            profitRow.createCell(1).setCellValue(0.0);
            profitRow.createCell(2).setCellValue(data.profitLoss);
            profitRow.createCell(3).setCellValue(0.0);
            profitRow.createCell(4).setCellValue(0.0);
            profitRow.createCell(5).setCellValue(data.profitLoss);
        }
        
        // Dividends
        if (data.dividendsPaid != 0) {
            Row dividendRow = sheet.createRow(currentRow++);
            dividendRow.createCell(0).setCellValue("Dividends paid");
            dividendRow.createCell(1).setCellValue(0.0);
            dividendRow.createCell(2).setCellValue(-Math.abs(data.dividendsPaid));
            dividendRow.createCell(3).setCellValue(0.0);
            dividendRow.createCell(4).setCellValue(0.0);
            dividendRow.createCell(5).setCellValue(-Math.abs(data.dividendsPaid));
        }
        
        // Share issues
        if (data.shareIssues != 0) {
            Row issueRow = sheet.createRow(currentRow++);
            issueRow.createCell(0).setCellValue("Issue of shares");
            issueRow.createCell(1).setCellValue(data.shareIssues);
            issueRow.createCell(2).setCellValue(0.0);
            issueRow.createCell(3).setCellValue(0.0);
            issueRow.createCell(4).setCellValue(0.0);
            issueRow.createCell(5).setCellValue(data.shareIssues);
        }
        
        // Share buybacks
        if (data.shareBuybacks != 0) {
            Row buybackRow = sheet.createRow(currentRow++);
            buybackRow.createCell(0).setCellValue("Buyback of shares");
            buybackRow.createCell(1).setCellValue(-Math.abs(data.shareBuybacks));
            buybackRow.createCell(2).setCellValue(0.0);
            buybackRow.createCell(3).setCellValue(0.0);
            buybackRow.createCell(4).setCellValue(0.0);
            buybackRow.createCell(5).setCellValue(-Math.abs(data.shareBuybacks));
        }
        
        // Transfers to reserves
        if (data.transfersToReserves != 0) {
            Row transferToRow = sheet.createRow(currentRow++);
            transferToRow.createCell(0).setCellValue("Transfer to reserves");
            transferToRow.createCell(1).setCellValue(0.0);
            transferToRow.createCell(2).setCellValue(-Math.abs(data.transfersToReserves));
            transferToRow.createCell(3).setCellValue(0.0);
            transferToRow.createCell(4).setCellValue(Math.abs(data.transfersToReserves));
            transferToRow.createCell(5).setCellValue(0.0);
        }
        
        // Transfers from reserves
        if (data.transfersFromReserves != 0) {
            Row transferFromRow = sheet.createRow(currentRow++);
            transferFromRow.createCell(0).setCellValue("Transfer from reserves");
            transferFromRow.createCell(1).setCellValue(0.0);
            transferFromRow.createCell(2).setCellValue(Math.abs(data.transfersFromReserves));
            transferFromRow.createCell(3).setCellValue(0.0);
            transferFromRow.createCell(4).setCellValue(-Math.abs(data.transfersFromReserves));
            transferFromRow.createCell(5).setCellValue(0.0);
        }
        
        // Revaluation adjustment
        if (data.revaluationAdjustment != 0) {
            Row revalRow = sheet.createRow(currentRow++);
            revalRow.createCell(0).setCellValue("Revaluation adjustment");
            revalRow.createCell(1).setCellValue(0.0);
            revalRow.createCell(2).setCellValue(0.0);
            revalRow.createCell(3).setCellValue(data.revaluationAdjustment);
            revalRow.createCell(4).setCellValue(0.0);
            revalRow.createCell(5).setCellValue(data.revaluationAdjustment);
        }
        
        currentRow++;
        
        // Calculate closing total
        double closingTotal = data.closingBalanceShareCapital + data.closingBalanceRetainedEarnings + 
                             data.closingBalanceRevaluationReserve + data.closingBalanceOtherReserves;
        
        // Closing balance
        Row closingRow = sheet.createRow(currentRow++);
        Cell closingLabel = closingRow.createCell(0);
        closingLabel.setCellValue("Balance at end of year");
        closingLabel.setCellStyle(headerStyle);
        closingRow.createCell(1).setCellValue(data.closingBalanceShareCapital);
        closingRow.createCell(2).setCellValue(data.closingBalanceRetainedEarnings);
        closingRow.createCell(3).setCellValue(data.closingBalanceRevaluationReserve);
        closingRow.createCell(4).setCellValue(data.closingBalanceOtherReserves);
        closingRow.createCell(5).setCellValue(closingTotal);
        
        // Auto-size columns
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createCashFlowStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Cash flow");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Cash Flows");
        // Add cash flow data
    }
    
    private void createNotes1(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) throws SQLException {
        Sheet sheet = workbook.createSheet("Notes 1");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        
        int currentRow = 6;
        
        // Fetch accounting policies from database - NO FALLBACK
        List<FinancialNoteData> accountingPolicies = getFinancialNotes(conn, company.id, period.id, "accounting_policies");
        
        if (accountingPolicies.isEmpty()) {
            throw new SQLException("Accounting policies notes not found in database for company " + company.id + 
                " and fiscal period " + period.id + ". Please insert data into financial_notes table with note_category='accounting_policies'.");
        }
        
        // Create notes from database
        for (FinancialNoteData note : accountingPolicies) {
            // Note number and title (bold)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            String titleText = note.noteNumber + ". " + note.noteTitle;
            titleCell.setCellValue(titleText);
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);
            boldStyle.setFont(boldFont);
            boldStyle.setWrapText(true);
            titleCell.setCellStyle(boldStyle);
            
            // Add standard reference if available
            if (note.standardReference != null && !note.standardReference.trim().isEmpty()) {
                Row refRow = sheet.createRow(currentRow++);
                Cell refCell = refRow.createCell(0);
                refCell.setCellValue("(Reference: " + note.standardReference + ")");
                CellStyle italicStyle = workbook.createCellStyle();
                Font italicFont = workbook.createFont();
                italicFont.setItalic(true);
                italicFont.setFontHeightInPoints((short) 9);
                italicStyle.setFont(italicFont);
                italicStyle.setWrapText(true);
                refCell.setCellStyle(italicStyle);
            }
            
            // Note content (wrapped text)
            if (note.noteContent != null && !note.noteContent.trim().isEmpty()) {
                Row contentRow = sheet.createRow(currentRow++);
                Cell contentCell = contentRow.createCell(0);
                contentCell.setCellValue(note.noteContent);
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
                contentCell.setCellStyle(wrapStyle);
                
                // Calculate row height based on content length
                int textLength = note.noteContent.length();
                int estimatedLines = Math.max(2, textLength / 80); // ~80 chars per line
                int maxHeight = 400; // Reasonable maximum
                contentRow.setHeightInPoints(Math.max(40, Math.min(maxHeight, estimatedLines * 15)));
            }
            
            // Add spacing between notes
            currentRow++;
        }
        
        // Set column widths
        sheet.setColumnWidth(0, 15000);
    }
    

    
    private void createNotes2to6(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) throws SQLException {
        Sheet sheet = workbook.createSheet("Notes 2-6");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        
        int currentRow = 6;
        
        // Fetch revenue/expense notes from database - NO FALLBACK
        List<FinancialNoteData> revenueExpenseNotes = getFinancialNotes(conn, company.id, period.id, "revenue_expenses");
        
        if (revenueExpenseNotes.isEmpty()) {
            throw new SQLException("Revenue/expense notes not found in database for company " + company.id + 
                " and fiscal period " + period.id + ". Please insert data into financial_notes table with note_category='revenue_expenses'.");
        }
        
        // Create notes from database
        for (FinancialNoteData note : revenueExpenseNotes) {
            // Note number and title (bold)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            String titleText = note.noteNumber + ". " + note.noteTitle;
            titleCell.setCellValue(titleText);
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);
            boldStyle.setFont(boldFont);
            boldStyle.setWrapText(true);
            titleCell.setCellStyle(boldStyle);
            
            // Add standard reference if available
            if (note.standardReference != null && !note.standardReference.trim().isEmpty()) {
                Row refRow = sheet.createRow(currentRow++);
                Cell refCell = refRow.createCell(0);
                refCell.setCellValue("(Reference: " + note.standardReference + ")");
                CellStyle italicStyle = workbook.createCellStyle();
                Font italicFont = workbook.createFont();
                italicFont.setItalic(true);
                italicFont.setFontHeightInPoints((short) 9);
                italicStyle.setFont(italicFont);
                italicStyle.setWrapText(true);
                refCell.setCellStyle(italicStyle);
            }
            
            // Note content (wrapped text)
            if (note.noteContent != null && !note.noteContent.trim().isEmpty()) {
                Row contentRow = sheet.createRow(currentRow++);
                Cell contentCell = contentRow.createCell(0);
                contentCell.setCellValue(note.noteContent);
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
                contentCell.setCellStyle(wrapStyle);
                
                // Calculate row height
                int textLength = note.noteContent.length();
                int estimatedLines = Math.max(2, textLength / 80);
                int maxHeight = 400;
                contentRow.setHeightInPoints(Math.max(40, Math.min(maxHeight, estimatedLines * 15)));
            }
            
            // Add spacing between notes
            currentRow++;
        }
        
        // Set column widths
        sheet.setColumnWidth(0, 15000);
    }
    

    
    private void createNotes7to10(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) throws SQLException {
        Sheet sheet = workbook.createSheet("Notes 7-10");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        
        int currentRow = 6;
        
        // Fetch asset/liability notes from database - NO FALLBACK
        List<FinancialNoteData> assetNotes = getFinancialNotes(conn, company.id, period.id, "assets");
        List<FinancialNoteData> liabilityNotes = getFinancialNotes(conn, company.id, period.id, "liabilities");
        
        // Combine both lists
        List<FinancialNoteData> allNotes = new ArrayList<>();
        allNotes.addAll(assetNotes);
        allNotes.addAll(liabilityNotes);
        
        if (allNotes.isEmpty()) {
            throw new SQLException("Asset/liability notes not found in database for company " + company.id + 
                " and period " + period.id + ". Please insert financial notes with category 'assets' or 'liabilities' " +
                "into the financial_notes table.");
        }
        
        // Create notes from database
        for (FinancialNoteData note : allNotes) {
            // Note number and title (bold)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            String titleText = note.noteNumber + ". " + note.noteTitle;
            titleCell.setCellValue(titleText);
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);
            boldStyle.setFont(boldFont);
            boldStyle.setWrapText(true);
            titleCell.setCellStyle(boldStyle);
            
            // Add standard reference if available
            if (note.standardReference != null && !note.standardReference.trim().isEmpty()) {
                Row refRow = sheet.createRow(currentRow++);
                Cell refCell = refRow.createCell(0);
                refCell.setCellValue("(Reference: " + note.standardReference + ")");
                CellStyle italicStyle = workbook.createCellStyle();
                Font italicFont = workbook.createFont();
                italicFont.setItalic(true);
                italicFont.setFontHeightInPoints((short) 9);
                italicStyle.setFont(italicFont);
                italicStyle.setWrapText(true);
                refCell.setCellStyle(italicStyle);
            }
            
            // Note content (wrapped text)
            if (note.noteContent != null && !note.noteContent.trim().isEmpty()) {
                Row contentRow = sheet.createRow(currentRow++);
                Cell contentCell = contentRow.createCell(0);
                contentCell.setCellValue(note.noteContent);
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
                contentCell.setCellStyle(wrapStyle);
                
                // Calculate row height
                int textLength = note.noteContent.length();
                int estimatedLines = Math.max(2, textLength / 80);
                int maxHeight = 400;
                contentRow.setHeightInPoints(Math.max(40, Math.min(maxHeight, estimatedLines * 15)));
            }
            
            // Add spacing between notes
            currentRow++;
        }
        
        // Set column widths
        sheet.setColumnWidth(0, 15000);
    }
    

    
    private void createNotes11to12(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) throws SQLException {
        Sheet sheet = workbook.createSheet("Notes 11-12");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        
        int currentRow = 6;
        
        // Fetch cash flow notes from database - NO FALLBACK
        List<FinancialNoteData> cashFlowNotes = getFinancialNotes(conn, company.id, period.id, "cash_flow");
        
        if (cashFlowNotes.isEmpty()) {
            throw new SQLException("Cash flow notes not found in database for company " + company.id + 
                " and period " + period.id + ". Please insert financial notes with category 'cash_flow' " +
                "into the financial_notes table.");
        }
        
        // Create notes from database
        for (FinancialNoteData note : cashFlowNotes) {
            // Note number and title (bold)
            Row titleRow = sheet.createRow(currentRow++);
            Cell titleCell = titleRow.createCell(0);
            String titleText = note.noteNumber + ". " + note.noteTitle;
            titleCell.setCellValue(titleText);
            CellStyle boldStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setFontHeightInPoints((short) 11);
            boldStyle.setFont(boldFont);
            boldStyle.setWrapText(true);
            titleCell.setCellStyle(boldStyle);
            
            // Add standard reference if available
            if (note.standardReference != null && !note.standardReference.trim().isEmpty()) {
                Row refRow = sheet.createRow(currentRow++);
                Cell refCell = refRow.createCell(0);
                refCell.setCellValue("(Reference: " + note.standardReference + ")");
                CellStyle italicStyle = workbook.createCellStyle();
                Font italicFont = workbook.createFont();
                italicFont.setItalic(true);
                italicFont.setFontHeightInPoints((short) 9);
                italicStyle.setFont(italicFont);
                italicStyle.setWrapText(true);
                refCell.setCellStyle(italicStyle);
            }
            
            // Note content (wrapped text)
            if (note.noteContent != null && !note.noteContent.trim().isEmpty()) {
                Row contentRow = sheet.createRow(currentRow++);
                Cell contentCell = contentRow.createCell(0);
                contentCell.setCellValue(note.noteContent);
                CellStyle wrapStyle = workbook.createCellStyle();
                wrapStyle.setWrapText(true);
                wrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
                contentCell.setCellStyle(wrapStyle);
                
                // Calculate row height
                int textLength = note.noteContent.length();
                int estimatedLines = Math.max(2, textLength / 80);
                int maxHeight = 400;
                contentRow.setHeightInPoints(Math.max(40, Math.min(maxHeight, estimatedLines * 15)));
            }
            
            // Add spacing between notes
            currentRow++;
        }
        
        // Set column widths
        sheet.setColumnWidth(0, 15000);
    }
    

    
    private void createTrialBalance(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Trial Balance");
        createCompanyHeader(workbook, sheet, company, period, "Trial Balance");
        
        // Create header styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.MEDIUM);
        
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalStyle.setBorderTop(BorderStyle.MEDIUM);
        totalStyle.setBorderBottom(BorderStyle.DOUBLE);
        
        int currentRow = 6;
        
        // Fetch trial balance data from database
        List<TrialBalanceItem> trialBalanceItems = getTrialBalanceData(conn, company.id, period.id);
        
        // Column headers
        Row headerRow = sheet.createRow(currentRow++);
        Cell codeHeader = headerRow.createCell(0);
        codeHeader.setCellValue("Account Code");
        codeHeader.setCellStyle(headerStyle);
        
        Cell nameHeader = headerRow.createCell(1);
        nameHeader.setCellValue("Account Name");
        nameHeader.setCellStyle(headerStyle);
        
        Cell debitHeader = headerRow.createCell(2);
        debitHeader.setCellValue("Debit");
        debitHeader.setCellStyle(headerStyle);
        
        Cell creditHeader = headerRow.createCell(3);
        creditHeader.setCellValue("Credit");
        creditHeader.setCellStyle(headerStyle);
        
        currentRow++; // Add spacing
        
        // Data rows
        double totalDebits = 0;
        double totalCredits = 0;
        
        for (TrialBalanceItem item : trialBalanceItems) {
            Row dataRow = sheet.createRow(currentRow++);
            
            // Account code
            dataRow.createCell(0).setCellValue(item.accountCode);
            
            // Account name
            dataRow.createCell(1).setCellValue(item.accountName);
            
            // Debit amount
            Cell debitCell = dataRow.createCell(2);
            if (item.debitBalance > 0) {
                debitCell.setCellValue(item.debitBalance);
                debitCell.setCellStyle(currencyStyle);
                totalDebits += item.debitBalance;
            }
            
            // Credit amount
            Cell creditCell = dataRow.createCell(3);
            if (item.creditBalance > 0) {
                creditCell.setCellValue(item.creditBalance);
                creditCell.setCellStyle(currencyStyle);
                totalCredits += item.creditBalance;
            }
        }
        
        // Totals row
        currentRow++; // Add spacing before totals
        Row totalRow = sheet.createRow(currentRow++);
        
        Cell totalLabel = totalRow.createCell(1);
        totalLabel.setCellValue("TOTALS");
        totalLabel.setCellStyle(totalStyle);
        
        Cell totalDebit = totalRow.createCell(2);
        totalDebit.setCellValue(totalDebits);
        totalDebit.setCellStyle(totalStyle);
        
        Cell totalCredit = totalRow.createCell(3);
        totalCredit.setCellValue(totalCredits);
        totalCredit.setCellStyle(totalStyle);
        
        // Difference check (should be 0 for balanced books)
        if (Math.abs(totalDebits - totalCredits) > 0.01) {
            currentRow++; // Add spacing
            Row warningRow = sheet.createRow(currentRow++);
            Cell warningCell = warningRow.createCell(0);
            warningCell.setCellValue("WARNING: Trial Balance does not balance! Difference: " + 
                String.format("%.2f", Math.abs(totalDebits - totalCredits)));
            
            CellStyle warningStyle = workbook.createCellStyle();
            Font warningFont = workbook.createFont();
            warningFont.setBold(true);
            warningFont.setColor(IndexedColors.RED.getIndex());
            warningStyle.setFont(warningFont);
            warningCell.setCellStyle(warningStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    // Helper classes and methods
    private static class CompanyInfo {
        Long id;
        String name;
        String registrationNumber;
        String businessNature;
        String directors;
        String secretary;
        String address;
        String businessAddress;
        String auditors;
        String bankers;
        String countryOfIncorporation;
    }
    
    private static class FiscalPeriodInfo {
        Long id;
        String name;
        LocalDate startDate;
        LocalDate endDate;
    }
    
    private static class AuditReportData {
        String opinionText;
        String basisForOpinion;
        String keyAuditMatters;
        String responsibilitiesManagement;
        String responsibilitiesAuditor;
        String auditorFirmName;
        String auditorPartnerName;
        LocalDate reportDate;
    }
    
    private static class DirectorsResponsibilityData {
        String statementText;
        String approvedBy;
        String approvedByTitle;
        LocalDate statementDate;
        String witnessName;
        String witnessTitle;
    }
    
    private static class DirectorsReportData {
        String natureOfBusiness;
        String reviewOfOperations;
        String directorsNames;
        String goingConcernAssessment;
        String eventsAfterReportingDate;
        String dividendsPaid;
        String shareCapitalChanges;
        String futureProspects;
        LocalDate reportDate;
    }
    
    private static class EquityMovementData {
        double openingBalanceShareCapital;
        double openingBalanceRetainedEarnings;
        double openingBalanceRevalutationReserve;
        double openingBalanceOtherReserves;
        double profitLoss;
        double dividendsPaid;
        double shareIssues;
        double shareBuybacks;
        double transfersToReserves;
        double transfersFromReserves;
        double revaluationAdjustment;
        double closingBalanceShareCapital;
        double closingBalanceRetainedEarnings;
        double closingBalanceRevaluationReserve;
        double closingBalanceOtherReserves;
    }
    
    private static class FinancialNoteData {
        String noteNumber;
        String noteTitle;
        String noteContent;
        String standardReference;
    }
    
    private static class BalanceSheetItem {
        String accountName;
        String noteReference;
        double currentYearAmount;
        double priorYearAmount;
        boolean isNonCurrent;
    }
    
    private static class IncomeStatementItem {
        String accountName;
        String noteReference;
        double amount;
    }
    
    private static class TrialBalanceItem {
        String accountCode;
        String accountName;
        double debitBalance;
        double creditBalance;
    }
    
    private CompanyInfo getCompanyInfo(Connection conn, Long companyId) throws SQLException {
        // Fetch company basic info
        String sql = """
            SELECT c.name, c.registration_number, c.address, 
                   cei.nature_of_business, cei.directors_names, cei.company_secretary,
                   cei.business_address, cei.auditors_name, cei.bankers_name,
                   cei.country_of_incorporation
            FROM companies c
            LEFT JOIN company_extended_info cei ON c.id = cei.company_id
            WHERE c.id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                CompanyInfo info = new CompanyInfo();
                info.id = companyId;
                info.name = rs.getString("name");
                info.registrationNumber = rs.getString("registration_number");
                info.address = rs.getString("address");
                
                // Fetch from extended info table, fallback to reasonable defaults only if NULL
                info.countryOfIncorporation = rs.getString("country_of_incorporation");
                if (info.countryOfIncorporation == null) {
                    info.countryOfIncorporation = "South Africa"; // Default for South African companies
                }
                
                info.businessNature = rs.getString("nature_of_business");
                if (info.businessNature == null) {
                    info.businessNature = "See directors' report";
                }
                
                info.directors = rs.getString("directors_names");
                if (info.directors == null) {
                    info.directors = "See directors' report";
                }
                
                info.secretary = rs.getString("company_secretary");
                if (info.secretary == null) {
                    info.secretary = "See directors' report";
                }
                
                info.businessAddress = rs.getString("business_address");
                if (info.businessAddress == null) {
                    info.businessAddress = info.address; // Use registered address as fallback
                }
                
                info.auditors = rs.getString("auditors_name");
                if (info.auditors == null) {
                    info.auditors = "To be appointed";
                }
                
                info.bankers = rs.getString("bankers_name");
                if (info.bankers == null) {
                    info.bankers = "See company records";
                }
                
                return info;
            }
        }
        throw new RuntimeException("Company not found: " + companyId);
    }
    
    private FiscalPeriodInfo getFiscalPeriodInfo(Connection conn, Long fiscalPeriodId) throws SQLException {
        String sql = "SELECT period_name, start_date, end_date FROM fiscal_periods WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                FiscalPeriodInfo info = new FiscalPeriodInfo();
                info.id = fiscalPeriodId;
                info.name = rs.getString("period_name");
                info.startDate = rs.getDate("start_date").toLocalDate();
                info.endDate = rs.getDate("end_date").toLocalDate();
                return info;
            }
        }
        throw new RuntimeException("Fiscal period not found: " + fiscalPeriodId);
    }
    
    private AuditReportData getAuditReportData(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT opinion_text, basis_for_opinion, key_audit_matters,
                   responsibilities_management, responsibilities_auditor,
                   auditor_firm_name, auditor_partner_name, report_date
            FROM audit_reports
            WHERE company_id = ? AND fiscal_period_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    AuditReportData data = new AuditReportData();
                    data.opinionText = rs.getString("opinion_text");
                    data.basisForOpinion = rs.getString("basis_for_opinion");
                    data.keyAuditMatters = rs.getString("key_audit_matters");
                    data.responsibilitiesManagement = rs.getString("responsibilities_management");
                    data.responsibilitiesAuditor = rs.getString("responsibilities_auditor");
                    data.auditorFirmName = rs.getString("auditor_firm_name");
                    data.auditorPartnerName = rs.getString("auditor_partner_name");
                    
                    // Handle nullable report date
                    java.sql.Date reportDate = rs.getDate("report_date");
                    if (reportDate != null) {
                        data.reportDate = reportDate.toLocalDate();
                    }
                    
                    return data;
                }
            }
        }
        
        // Return null if no audit report exists - caller will use default template
        return null;
    }
    
    private DirectorsResponsibilityData getDirectorsResponsibilityData(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT statement_text, approved_by, approved_by_title, statement_date,
                   witness_name, witness_title
            FROM directors_responsibility_statements
            WHERE company_id = ? AND fiscal_period_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    DirectorsResponsibilityData data = new DirectorsResponsibilityData();
                    data.statementText = rs.getString("statement_text");
                    data.approvedBy = rs.getString("approved_by");
                    data.approvedByTitle = rs.getString("approved_by_title");
                    data.witnessName = rs.getString("witness_name");
                    data.witnessTitle = rs.getString("witness_title");
                    
                    // Handle nullable date
                    java.sql.Date statementDate = rs.getDate("statement_date");
                    if (statementDate != null) {
                        data.statementDate = statementDate.toLocalDate();
                    }
                    
                    return data;
                }
            }
        }
        
        // Return null if no statement exists - caller will use default template
        return null;
    }
    
    private DirectorsReportData getDirectorsReportData(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT nature_of_business, review_of_operations, directors_names,
                   going_concern_assessment, events_after_reporting_date,
                   dividends_paid, share_capital_changes, future_prospects, report_date
            FROM directors_reports
            WHERE company_id = ? AND fiscal_period_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    DirectorsReportData data = new DirectorsReportData();
                    data.natureOfBusiness = rs.getString("nature_of_business");
                    data.reviewOfOperations = rs.getString("review_of_operations");
                    data.directorsNames = rs.getString("directors_names");
                    data.goingConcernAssessment = rs.getString("going_concern_assessment");
                    data.eventsAfterReportingDate = rs.getString("events_after_reporting_date");
                    data.dividendsPaid = rs.getString("dividends_paid");
                    data.shareCapitalChanges = rs.getString("share_capital_changes");
                    data.futureProspects = rs.getString("future_prospects");
                    
                    // Handle nullable date
                    java.sql.Date reportDate = rs.getDate("report_date");
                    if (reportDate != null) {
                        data.reportDate = reportDate.toLocalDate();
                    }
                    
                    return data;
                }
            }
        }
        
        // Return null if no report exists - caller will use default template
        return null;
    }
    
    private EquityMovementData getEquityMovementData(Connection conn, Long companyId, Long fiscalPeriodId) throws SQLException {
        String sql = """
            SELECT opening_balance_share_capital, opening_balance_retained_earnings,
                   opening_balance_revaluation_reserve, opening_balance_other_reserves,
                   profit_loss, dividends_paid, share_issues, share_buybacks,
                   transfers_to_reserves, transfers_from_reserves, revaluation_adjustment,
                   closing_balance_share_capital, closing_balance_retained_earnings,
                   closing_balance_revaluation_reserve, closing_balance_other_reserves
            FROM equity_movements
            WHERE company_id = ? AND fiscal_period_id = ?
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    EquityMovementData data = new EquityMovementData();
                    data.openingBalanceShareCapital = rs.getDouble("opening_balance_share_capital");
                    data.openingBalanceRetainedEarnings = rs.getDouble("opening_balance_retained_earnings");
                    data.openingBalanceRevalutationReserve = rs.getDouble("opening_balance_revaluation_reserve");
                    data.openingBalanceOtherReserves = rs.getDouble("opening_balance_other_reserves");
                    data.profitLoss = rs.getDouble("profit_loss");
                    data.dividendsPaid = rs.getDouble("dividends_paid");
                    data.shareIssues = rs.getDouble("share_issues");
                    data.shareBuybacks = rs.getDouble("share_buybacks");
                    data.transfersToReserves = rs.getDouble("transfers_to_reserves");
                    data.transfersFromReserves = rs.getDouble("transfers_from_reserves");
                    data.revaluationAdjustment = rs.getDouble("revaluation_adjustment");
                    data.closingBalanceShareCapital = rs.getDouble("closing_balance_share_capital");
                    data.closingBalanceRetainedEarnings = rs.getDouble("closing_balance_retained_earnings");
                    data.closingBalanceRevaluationReserve = rs.getDouble("closing_balance_revaluation_reserve");
                    data.closingBalanceOtherReserves = rs.getDouble("closing_balance_other_reserves");
                    
                    return data;
                }
            }
        }
        
        // Return null if no equity movement data exists - caller will use default/calculated values
        return null;
    }
    
    private List<FinancialNoteData> getFinancialNotes(Connection conn, Long companyId, Long fiscalPeriodId, String noteCategory) throws SQLException {
        String sql = """
            SELECT note_number, note_title, note_content, standard_reference, display_order
            FROM financial_notes
            WHERE company_id = ? AND fiscal_period_id = ? AND note_category = ?
            ORDER BY display_order, note_number
            """;
        
        List<FinancialNoteData> notes = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setLong(2, fiscalPeriodId);
            pstmt.setString(3, noteCategory);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FinancialNoteData note = new FinancialNoteData();
                    note.noteNumber = rs.getString("note_number");
                    note.noteTitle = rs.getString("note_title");
                    note.noteContent = rs.getString("note_content");
                    note.standardReference = rs.getString("standard_reference");
                    notes.add(note);
                }
            }
        }
        
        return notes;
    }
    
    private List<BalanceSheetItem> getBalanceSheetAssets(Connection conn, Long companyId, Long fiscalPeriodId) {
        // Implementation to get asset accounts from trial balance
        return new ArrayList<>();
    }
    
    private List<BalanceSheetItem> getBalanceSheetLiabilities(Connection conn, Long companyId, Long fiscalPeriodId) {
        // Implementation to get liability accounts from trial balance
        return new ArrayList<>();
    }
    
    private List<BalanceSheetItem> getBalanceSheetEquity(Connection conn, Long companyId, Long fiscalPeriodId) {
        // Implementation to get equity accounts from trial balance
        return new ArrayList<>();
    }
    
    private List<IncomeStatementItem> getIncomeStatementRevenues(Connection conn, Long companyId, Long fiscalPeriodId) {
        List<IncomeStatementItem> revenues = new ArrayList<>();
        String sql = """
            SELECT a.account_code, a.account_name,
                   COALESCE(SUM(jel.credit_amount - jel.debit_amount), 0) as net_amount
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ? AND a.account_code LIKE '4%'
            GROUP BY a.account_code, a.account_name
            HAVING COALESCE(SUM(jel.credit_amount - jel.debit_amount), 0) != 0
            ORDER BY a.account_code
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            stmt.setLong(3, companyId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    IncomeStatementItem item = new IncomeStatementItem();
                    item.accountName = rs.getString("account_name");
                    item.noteReference = rs.getString("account_code");
                    item.amount = rs.getDouble("net_amount");
                    revenues.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting revenue data", e);
        }

        return revenues;
    }
    
    private List<IncomeStatementItem> getIncomeStatementExpenses(Connection conn, Long companyId, Long fiscalPeriodId) {
        List<IncomeStatementItem> expenses = new ArrayList<>();
        String sql = """
            SELECT a.account_code, a.account_name,
                   COALESCE(SUM(jel.debit_amount - jel.credit_amount), 0) as net_amount
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ? AND (a.account_code LIKE '5%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%')
            GROUP BY a.account_code, a.account_name
            HAVING COALESCE(SUM(jel.debit_amount - jel.credit_amount), 0) != 0
            ORDER BY a.account_code
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            stmt.setLong(3, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    IncomeStatementItem item = new IncomeStatementItem();
                    item.accountName = rs.getString("account_name");
                    item.noteReference = rs.getString("account_code");
                    item.amount = rs.getDouble("net_amount");
                    expenses.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting expense data", e);
        }

        return expenses;
    }
    
    private List<TrialBalanceItem> getTrialBalanceData(Connection conn, Long companyId, Long fiscalPeriodId) {
        List<TrialBalanceItem> items = new ArrayList<>();
        String sql = """
            SELECT a.account_code, a.account_name,
                   COALESCE(SUM(jel.debit_amount), 0) as total_debits,
                   COALESCE(SUM(jel.credit_amount), 0) as total_credits
            FROM accounts a
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
                AND je.company_id = ? AND je.fiscal_period_id = ?
            WHERE a.company_id = ? AND a.is_active = TRUE
            GROUP BY a.account_code, a.account_name
            HAVING COALESCE(SUM(jel.debit_amount), 0) != 0 OR COALESCE(SUM(jel.credit_amount), 0) != 0
            ORDER BY a.account_code
            """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, companyId);
            stmt.setLong(2, fiscalPeriodId);
            stmt.setLong(3, companyId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TrialBalanceItem item = new TrialBalanceItem();
                    item.accountCode = rs.getString("account_code");
                    item.accountName = rs.getString("account_name");
                    
                    double totalDebits = rs.getDouble("total_debits");
                    double totalCredits = rs.getDouble("total_credits");
                    double netBalance = totalDebits - totalCredits;
                    
                    // Show as debit balance if positive, credit if negative
                    if (netBalance > 0) {
                        item.debitBalance = netBalance;
                        item.creditBalance = 0;
                    } else if (netBalance < 0) {
                        item.debitBalance = 0;
                        item.creditBalance = Math.abs(netBalance);
                    } else {
                        // Zero balance - show nothing
                        item.debitBalance = 0;
                        item.creditBalance = 0;
                    }
                    
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error getting trial balance data", e);
        }

        return items;
    }
}