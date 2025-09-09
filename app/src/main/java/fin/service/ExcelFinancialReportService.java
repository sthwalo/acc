package fin.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Excel Financial Report Generator
 * Creates comprehensive financial reports in Excel format following the 
 * company's standard template structure with consistent formatting
 */
public class ExcelFinancialReportService {
    private static final Logger LOGGER = Logger.getLogger(ExcelFinancialReportService.class.getName());
    private final String dbUrl;
    
    public ExcelFinancialReportService(String dbUrl) {
        this.dbUrl = dbUrl;
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
            createNotes1(workbook, company, period);
            createNotes2to6(workbook, company, period, conn);
            createNotes7to10(workbook, company, period, conn);
            createNotes11to12(workbook, company, period, conn);
            createDetailedIncomeStatement(workbook, company, period, conn);
            
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
            
            System.out.println("✅ Excel Financial Report generated: " + fullPath);
            LOGGER.info("Excel Financial Report generated: " + fullPath);
            
        } catch (Exception e) {
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
        headerFont.setFontHeightInPoints((short) 14);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // Create company name (row 11 as per template)
        Row row11 = sheet.createRow(10);
        Cell companyCell = row11.createCell(0);
        companyCell.setCellValue(company.name.toUpperCase());
        companyCell.setCellStyle(headerStyle);
        
        // Registration number (row 13)
        Row row13 = sheet.createRow(12);
        Cell regCell = row13.createCell(0);
        regCell.setCellValue("(Registration Number: " + company.registrationNumber + ")");
        
        // Annual Financial Statements (row 14)
        Row row14 = sheet.createRow(13);
        Cell titleCell = row14.createCell(0);
        titleCell.setCellValue("ANNUAL FINANCIAL STATEMENTS");
        titleCell.setCellStyle(headerStyle);
        
        // Period (row 15)
        Row row15 = sheet.createRow(14);
        Cell periodCell = row15.createCell(0);
        periodCell.setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Auto-size columns
        for (int i = 0; i < 13; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private void createIndexSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Index");
        
        // Company header
        createCompanyHeader(workbook, sheet, company, period, "Annual Financial Statements");
        
        Row row7 = sheet.createRow(6);
        row7.createCell(0).setCellValue("The reports and statements set out below comprise the annual financial statements presented to the members:");
        
        // Contents header
        Row row8 = sheet.createRow(7);
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
            Row row = sheet.createRow(8 + i);
            row.createCell(0).setCellValue(indexItems[i][0]);
            row.createCell(1).setCellValue(indexItems[i][1]);
        }
    }
    
    private void createCompanyDetailsSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Co details");
        
        createCompanyHeader(workbook, sheet, company, period, "Annual Financial Statements");
        
        Row row7 = sheet.createRow(6);
        row7.createCell(0).setCellValue("General information");
        
        // Company details
        String[][] details = {
            {"Country of incorporation", "South Africa"},
            {"Nature of business", company.businessNature != null ? company.businessNature : "Private Security Services"},
            {"Director(s)", company.directors != null ? company.directors : "As per directors' report"},
            {"Company secretary", company.secretary != null ? company.secretary : "As per directors' report"},
            {"Registered address", company.address != null ? company.address : "As per company records"},
            {"Business address", company.businessAddress != null ? company.businessAddress : "As per company records"},
            {"Auditors", company.auditors != null ? company.auditors : "Independent Auditors"},
            {"Bankers", company.bankers != null ? company.bankers : "Standard Bank of South Africa"},
            {"Registration number", company.registrationNumber}
        };
        
        for (int i = 0; i < details.length; i++) {
            Row row = sheet.createRow(7 + i);
            row.createCell(0).setCellValue(details[i][0]);
            row.createCell(7).setCellValue(details[i][1]); // Column H as per template
        }
    }
    
    private void createBalanceSheet(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Balance sheet");
        
        createCompanyHeader(workbook, sheet, company, period, "Statement of Financial Position");
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("as at " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Headers
        Row row8 = sheet.createRow(7);
        row8.createCell(2).setCellValue("Note");
        row8.createCell(3).setCellValue(period.endDate.getYear());
        row8.createCell(5).setCellValue(period.startDate.getYear());
        
        Row row9 = sheet.createRow(8);
        row9.createCell(3).setCellValue("R");
        row9.createCell(5).setCellValue("R");
        
        // Get balance sheet data
        List<BalanceSheetItem> assets = getBalanceSheetAssets(conn, company.id, period.id);
        List<BalanceSheetItem> liabilities = getBalanceSheetLiabilities(conn, company.id, period.id);
        List<BalanceSheetItem> equity = getBalanceSheetEquity(conn, company.id, period.id);
        
        int currentRow = 10;
        
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
                row.createCell(3).setCellValue(item.currentYearAmount);
                row.createCell(5).setCellValue(item.priorYearAmount);
                totalNonCurrentAssets += item.currentYearAmount;
            }
        }
        
        // Total non-current assets
        Row totalNonCurrentRow = sheet.createRow(currentRow++);
        totalNonCurrentRow.createCell(0).setCellValue("Total non-current assets");
        totalNonCurrentRow.createCell(3).setCellValue(totalNonCurrentAssets);
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
                row.createCell(3).setCellValue(item.currentYearAmount);
                row.createCell(5).setCellValue(item.priorYearAmount);
                totalCurrentAssets += item.currentYearAmount;
            }
        }
        
        // Total assets
        Row totalAssetsRow = sheet.createRow(currentRow++);
        totalAssetsRow.createCell(0).setCellValue("TOTAL ASSETS");
        totalAssetsRow.createCell(3).setCellValue(totalNonCurrentAssets + totalCurrentAssets);
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
            row.createCell(3).setCellValue(item.currentYearAmount);
            row.createCell(5).setCellValue(item.priorYearAmount);
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
            row.createCell(3).setCellValue(item.currentYearAmount);
            row.createCell(5).setCellValue(item.priorYearAmount);
            totalLiabilities += item.currentYearAmount;
        }
        
        // Total equity and liabilities
        Row totalEquityLiabRow = sheet.createRow(currentRow++);
        totalEquityLiabRow.createCell(0).setCellValue("TOTAL EQUITY AND LIABILITIES");
        totalEquityLiabRow.createCell(3).setCellValue(totalEquity + totalLiabilities);
    }
    
    private void createIncomeStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Income statement");
        
        createCompanyHeader(workbook, sheet, company, period, "Statement of Comprehensive Income");
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Headers
        Row row8 = sheet.createRow(7);
        row8.createCell(3).setCellValue(period.endDate.getYear());
        row8.createCell(5).setCellValue(period.startDate.getYear());
        
        Row row9 = sheet.createRow(8);
        row9.createCell(1).setCellValue("Note");
        row9.createCell(3).setCellValue("R");
        row9.createCell(5).setCellValue("R");
        
        // Get income statement data
        List<IncomeStatementItem> revenues = getIncomeStatementRevenues(conn, company.id, period.id);
        List<IncomeStatementItem> expenses = getIncomeStatementExpenses(conn, company.id, period.id);
        
        int currentRow = 11;
        
        // Revenue
        Row revenueRow = sheet.createRow(currentRow++);
        revenueRow.createCell(0).setCellValue("Revenue");
        revenueRow.createCell(1).setCellValue("2");
        
        double totalRevenue = 0;
        for (IncomeStatementItem item : revenues) {
            totalRevenue += item.amount;
        }
        revenueRow.createCell(3).setCellValue(totalRevenue);
        currentRow++;
        
        // Other income
        Row otherIncomeRow = sheet.createRow(currentRow++);
        otherIncomeRow.createCell(0).setCellValue("Other income");
        otherIncomeRow.createCell(1).setCellValue("2.1");
        otherIncomeRow.createCell(3).setCellValue(0.0); // Placeholder
        currentRow++;
        
        // Expenses
        double totalExpenses = 0;
        for (IncomeStatementItem item : expenses) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(item.accountName);
            row.createCell(1).setCellValue(item.noteReference);
            row.createCell(3).setCellValue(-Math.abs(item.amount)); // Show as negative
            totalExpenses += Math.abs(item.amount);
        }
        
        currentRow++;
        
        // Net surplus/deficit
        Row netRow = sheet.createRow(currentRow++);
        netRow.createCell(0).setCellValue("Net surplus/(deficit) for the year");
        double netResult = totalRevenue - totalExpenses;
        netRow.createCell(3).setCellValue(netResult);
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
        
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("for the year ended " + period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    // Add placeholder methods for other sheets
    private void createResponsibilityStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("State of responsibility");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Financial Responsibility by the Directors");
        // Add standard responsibility statement content
    }
    
    private void createAuditReport(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Audit report");
        // Add audit report content
    }
    
    private void createDirectorsReport(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Directors report");
        createCompanyHeader(workbook, sheet, company, period, "Director's Report");
        // Add directors' report content
    }
    
    private void createStatementOfChanges(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("State of changes");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Changes in Equity");
        // Add equity changes
    }
    
    private void createCashFlowStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Cash flow");
        createCompanyHeader(workbook, sheet, company, period, "Statement of Cash Flows");
        // Add cash flow data
    }
    
    private void createNotes1(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period) {
        Sheet sheet = workbook.createSheet("Notes 1");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        // Add accounting policies
    }
    
    private void createNotes2to6(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Notes 2-6");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        // Add revenue notes
    }
    
    private void createNotes7to10(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Notes 7-10");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        // Add asset notes
    }
    
    private void createNotes11to12(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Notes 11-12");
        createCompanyHeader(workbook, sheet, company, period, "Notes to the Annual Financial Statements");
        // Add cash flow notes
    }
    
    private void createDetailedIncomeStatement(Workbook workbook, CompanyInfo company, FiscalPeriodInfo period, Connection conn) {
        Sheet sheet = workbook.createSheet("Detailed IS");
        createCompanyHeader(workbook, sheet, company, period, "Detailed Income Statement");
        // Add detailed income statement
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
    }
    
    private static class FiscalPeriodInfo {
        Long id;
        String name;
        LocalDate startDate;
        LocalDate endDate;
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
    
    private CompanyInfo getCompanyInfo(Connection conn, Long companyId) throws SQLException {
        String sql = "SELECT name, registration_number, tax_number, address, contact_email, contact_phone FROM companies WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                CompanyInfo info = new CompanyInfo();
                info.id = companyId;
                info.name = rs.getString("name");
                info.registrationNumber = rs.getString("registration_number");
                info.businessNature = "Private Security Services"; // Default
                info.directors = "As per directors' report"; // Default
                info.secretary = "As per directors' report"; // Default
                info.address = rs.getString("address");
                info.businessAddress = rs.getString("address"); // Use same as registered
                info.auditors = "Independent Auditors"; // Default
                info.bankers = "Standard Bank of South Africa"; // Default
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
        // Implementation to get revenue accounts
        return new ArrayList<>();
    }
    
    private List<IncomeStatementItem> getIncomeStatementExpenses(Connection conn, Long companyId, Long fiscalPeriodId) {
        // Implementation to get expense accounts
        return new ArrayList<>();
    }
}
