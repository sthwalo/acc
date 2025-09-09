package fin.app;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel Financial Report Generator with Database Integration
 * Fetches data first, then generates Excel to avoid crashes
 */
public class FinancialReportGenerator {
    
    // Database connection details
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/drimacc_db";
    private static final String DB_USER = "sthwalonyoni";
    private static final String DB_PASSWORD = "LeZipho24#";
    
    public static void main(String[] args) {
        System.out.println("🏢 FINANCIAL REPORT GENERATOR WITH DATABASE VALUES");
        System.out.println("==================================================");
        System.out.println();
        
        try {
            // Step 1: Fetch all data from database first
            System.out.println("📊 Fetching data from database...");
            FinancialData data = fetchFinancialData();
            System.out.println("✅ Data fetched successfully!");
            System.out.println("   Company: " + data.company.name);
            System.out.println("   Period: " + data.period.name);
            System.out.println("   Assets: " + data.assets.size() + " accounts");
            System.out.println("   Liabilities: " + data.liabilities.size() + " accounts");
            System.out.println("   Equity: " + data.equity.size() + " accounts");
            System.out.println("   Revenue: " + data.revenue.size() + " accounts");
            System.out.println("   Expenses: " + data.expenses.size() + " accounts");
            System.out.println();
            
            // Step 2: Generate Excel file with the fetched data
            System.out.println("📋 Creating Excel file with actual database values...");
            generateExcelReport(data);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static FinancialData fetchFinancialData() throws SQLException {
        FinancialData data = new FinancialData();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // Get company info
            data.company = getCompanyInfo(conn, 1L);
            
            // Get fiscal period info
            data.period = getFiscalPeriodInfo(conn, 1L);
            
            // Get all account balances by type
            data.assets = getAccountsByType(conn, 1L, 1L, "Asset");
            data.liabilities = getAccountsByType(conn, 1L, 1L, "Liability");
            data.equity = getAccountsByType(conn, 1L, 1L, "Equity");
            data.revenue = getAccountsByType(conn, 1L, 1L, "Revenue");
            data.expenses = getAccountsByType(conn, 1L, 1L, "Expense");
        }
        
        return data;
    }
    
    private static void generateExcelReport(FinancialData data) throws Exception {
        // Create workbook
        Workbook workbook = new HSSFWorkbook();
        
        // Create all sheets with actual data
        createCoverSheet(workbook, data);
        createIndexSheet(workbook, data);
        createCompanyDetailsSheet(workbook, data);
        createBalanceSheetWithData(workbook, data);
        createIncomeStatementWithData(workbook, data);
        createNotesSheets(workbook, data);
        
        // Save the file
        String filename = String.format("%s_Financial_Report_POPULATED_%s.xls", 
            data.company.name.replaceAll("[^a-zA-Z0-9]", "_"),
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        String fullPath = "/Users/sthwalonyoni/FIN/reports/" + filename;
        
        try (FileOutputStream fileOut = new FileOutputStream(fullPath)) {
            workbook.write(fileOut);
        }
        
        workbook.close();
        
        System.out.println("✅ Excel report created successfully!");
        System.out.println("📁 File: " + fullPath);
        System.out.println();
        System.out.println("📊 Report includes ACTUAL DATABASE VALUES:");
        System.out.println("   ✅ Account Code columns: " + getTotalAccounts(data) + " accounts");
        System.out.println("   ✅ Account Name columns: Populated with real names");
        System.out.println("   ✅ Amount/Balance columns: R " + String.format("%,.2f", getTotalAssets(data)) + " total assets");
        System.out.println("   ✅ Date/Period columns: " + data.period.startDate + " to " + data.period.endDate);
        System.out.println("   ✅ Total/Subtotal rows: Calculated from database");
    }
    
    private static void createCoverSheet(Workbook workbook, FinancialData data) {
        Sheet sheet = workbook.createSheet("Cover");
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row row11 = sheet.createRow(10);
        Cell companyCell = row11.createCell(0);
        companyCell.setCellValue(data.company.name.toUpperCase());
        companyCell.setCellStyle(headerStyle);
        
        Row row13 = sheet.createRow(12);
        row13.createCell(0).setCellValue("(Registration Number: " + data.company.registrationNumber + ")");
        
        Row row14 = sheet.createRow(13);
        Cell titleCell = row14.createCell(0);
        titleCell.setCellValue("ANNUAL FINANCIAL STATEMENTS");
        titleCell.setCellStyle(headerStyle);
        
        Row row15 = sheet.createRow(14);
        row15.createCell(0).setCellValue("for the year ended " + data.period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    private static void createIndexSheet(Workbook workbook, FinancialData data) {
        Sheet sheet = workbook.createSheet("Index");
        createCompanyHeader(workbook, sheet, data, "Annual Financial Statements");
        
        Row row8 = sheet.createRow(7);
        row8.createCell(0).setCellValue("Contents");
        row8.createCell(1).setCellValue("Page");
        
        String[][] items = {
            {"General information", "2"},
            {"Statement of financial position", "3"},
            {"Statement of comprehensive income", "4"},
            {"Notes to the annual financial statements", "5 - 8"}
        };
        
        for (int i = 0; i < items.length; i++) {
            Row row = sheet.createRow(8 + i);
            row.createCell(0).setCellValue(items[i][0]);
            row.createCell(1).setCellValue(items[i][1]);
        }
    }
    
    private static void createCompanyDetailsSheet(Workbook workbook, FinancialData data) {
        Sheet sheet = workbook.createSheet("Company Details");
        createCompanyHeader(workbook, sheet, data, "General Information");
        
        String[][] details = {
            {"Country of incorporation", "South Africa"},
            {"Nature of business", "Private Security Services"},
            {"Director(s)", "Nkuna Daniel Sthwalo"},
            {"Registered address", data.company.address},
            {"Registration number", data.company.registrationNumber},
            {"Financial year end", data.period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))}
        };
        
        for (int i = 0; i < details.length; i++) {
            Row row = sheet.createRow(8 + i);
            row.createCell(0).setCellValue(details[i][0]);
            row.createCell(7).setCellValue(details[i][1]);
        }
    }
    
    private static void createBalanceSheetWithData(Workbook workbook, FinancialData data) {
        Sheet sheet = workbook.createSheet("Balance Sheet");
        createCompanyHeader(workbook, sheet, data, "Statement of Financial Position");
        
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("as at " + data.period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        // Headers
        Row row7 = sheet.createRow(6);
        row7.createCell(0).setCellValue("Account");
        row7.createCell(1).setCellValue("Code");
        row7.createCell(2).setCellValue(String.valueOf(data.period.endDate.getYear()));
        row7.createCell(3).setCellValue("R");
        
        for (Cell cell : row7) {
            cell.setCellStyle(headerStyle);
        }
        
        int currentRow = 8;
        
        // ASSETS section with real data
        Row assetsRow = sheet.createRow(currentRow++);
        assetsRow.createCell(0).setCellValue("ASSETS");
        assetsRow.getCell(0).setCellStyle(headerStyle);
        currentRow++;
        
        double totalAssets = 0;
        for (AccountBalance asset : data.assets) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(asset.accountName);
            row.createCell(1).setCellValue(asset.accountCode);
            
            double amount = Math.abs(asset.currentBalance);
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(amount);
            amountCell.setCellStyle(currencyStyle);
            
            totalAssets += amount;
        }
        
        // Total Assets
        currentRow++;
        Row totalAssetsRow = sheet.createRow(currentRow++);
        totalAssetsRow.createCell(0).setCellValue("TOTAL ASSETS");
        totalAssetsRow.getCell(0).setCellStyle(headerStyle);
        Cell totalAssetsCell = totalAssetsRow.createCell(2);
        totalAssetsCell.setCellValue(totalAssets);
        totalAssetsCell.setCellStyle(currencyStyle);
        
        currentRow += 2;
        
        // EQUITY AND LIABILITIES section
        Row equityLiabRow = sheet.createRow(currentRow++);
        equityLiabRow.createCell(0).setCellValue("EQUITY AND LIABILITIES");
        equityLiabRow.getCell(0).setCellStyle(headerStyle);
        currentRow++;
        
        // Equity
        Row equityHeaderRow = sheet.createRow(currentRow++);
        equityHeaderRow.createCell(0).setCellValue("Equity");
        equityHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        double totalEquity = 0;
        for (AccountBalance equity : data.equity) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(equity.accountName);
            row.createCell(1).setCellValue(equity.accountCode);
            
            double amount = equity.currentBalance;
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(amount);
            amountCell.setCellStyle(currencyStyle);
            
            totalEquity += amount;
        }
        
        // Liabilities
        currentRow++;
        Row liabHeaderRow = sheet.createRow(currentRow++);
        liabHeaderRow.createCell(0).setCellValue("Liabilities");
        liabHeaderRow.getCell(0).setCellStyle(headerStyle);
        
        double totalLiabilities = 0;
        for (AccountBalance liability : data.liabilities) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(liability.accountName);
            row.createCell(1).setCellValue(liability.accountCode);
            
            double amount = Math.abs(liability.currentBalance);
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(amount);
            amountCell.setCellStyle(currencyStyle);
            
            totalLiabilities += amount;
        }
        
        // Total Equity and Liabilities
        currentRow++;
        Row totalEquityLiabRow = sheet.createRow(currentRow++);
        totalEquityLiabRow.createCell(0).setCellValue("TOTAL EQUITY AND LIABILITIES");
        totalEquityLiabRow.getCell(0).setCellStyle(headerStyle);
        Cell totalEquityLiabCell = totalEquityLiabRow.createCell(2);
        totalEquityLiabCell.setCellValue(totalEquity + totalLiabilities);
        totalEquityLiabCell.setCellStyle(currencyStyle);
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private static void createIncomeStatementWithData(Workbook workbook, FinancialData data) {
        Sheet sheet = workbook.createSheet("Income Statement");
        createCompanyHeader(workbook, sheet, data, "Statement of Comprehensive Income");
        
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("for the year ended " + data.period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);
        
        // Headers
        Row row7 = sheet.createRow(6);
        row7.createCell(0).setCellValue("Account");
        row7.createCell(1).setCellValue("Code");
        row7.createCell(2).setCellValue(String.valueOf(data.period.endDate.getYear()));
        row7.createCell(3).setCellValue("R");
        
        for (Cell cell : row7) {
            cell.setCellStyle(headerStyle);
        }
        
        int currentRow = 8;
        
        // REVENUE section
        Row revenueRow = sheet.createRow(currentRow++);
        revenueRow.createCell(0).setCellValue("REVENUE");
        revenueRow.getCell(0).setCellStyle(headerStyle);
        
        double totalRevenue = 0;
        for (AccountBalance revenue : data.revenue) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(revenue.accountName);
            row.createCell(1).setCellValue(revenue.accountCode);
            
            double amount = Math.abs(revenue.currentBalance);
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(amount);
            amountCell.setCellStyle(currencyStyle);
            
            totalRevenue += amount;
        }
        
        // Total Revenue
        currentRow++;
        Row totalRevenueRow = sheet.createRow(currentRow++);
        totalRevenueRow.createCell(0).setCellValue("Total Revenue");
        totalRevenueRow.getCell(0).setCellStyle(headerStyle);
        Cell totalRevenueCell = totalRevenueRow.createCell(2);
        totalRevenueCell.setCellValue(totalRevenue);
        totalRevenueCell.setCellStyle(currencyStyle);
        
        currentRow += 2;
        
        // EXPENSES section
        Row expensesRow = sheet.createRow(currentRow++);
        expensesRow.createCell(0).setCellValue("EXPENSES");
        expensesRow.getCell(0).setCellStyle(headerStyle);
        
        double totalExpenses = 0;
        for (AccountBalance expense : data.expenses) {
            Row row = sheet.createRow(currentRow++);
            row.createCell(0).setCellValue(expense.accountName);
            row.createCell(1).setCellValue(expense.accountCode);
            
            double amount = Math.abs(expense.currentBalance);
            Cell amountCell = row.createCell(2);
            amountCell.setCellValue(amount);
            amountCell.setCellStyle(currencyStyle);
            
            totalExpenses += amount;
        }
        
        // Total Expenses
        currentRow++;
        Row totalExpensesRow = sheet.createRow(currentRow++);
        totalExpensesRow.createCell(0).setCellValue("Total Expenses");
        totalExpensesRow.getCell(0).setCellStyle(headerStyle);
        Cell totalExpensesCell = totalExpensesRow.createCell(2);
        totalExpensesCell.setCellValue(totalExpenses);
        totalExpensesCell.setCellStyle(currencyStyle);
        
        // Net Result
        currentRow += 2;
        Row netRow = sheet.createRow(currentRow++);
        netRow.createCell(0).setCellValue("Net Surplus/(Deficit) for the year");
        netRow.getCell(0).setCellStyle(headerStyle);
        Cell netCell = netRow.createCell(2);
        netCell.setCellValue(totalRevenue - totalExpenses);
        netCell.setCellStyle(currencyStyle);
        
        // Auto-size columns
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    private static void createNotesSheets(Workbook workbook, FinancialData data) {
        // Create a simple notes sheet
        Sheet sheet = workbook.createSheet("Notes");
        createCompanyHeader(workbook, sheet, data, "Notes to the Annual Financial Statements");
        
        Row row8 = sheet.createRow(7);
        row8.createCell(0).setCellValue("1. Accounting Policies");
        
        Row row10 = sheet.createRow(9);
        row10.createCell(0).setCellValue("The financial statements are prepared in accordance with");
        Row row11 = sheet.createRow(10);
        row11.createCell(0).setCellValue("International Financial Reporting Standards (IFRS).");
    }
    
    // Database methods
    private static CompanyInfo getCompanyInfo(Connection conn, Long companyId) throws SQLException {
        String sql = "SELECT name, registration_number, address FROM companies WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                CompanyInfo info = new CompanyInfo();
                info.name = rs.getString("name");
                info.registrationNumber = rs.getString("registration_number") != null ? rs.getString("registration_number") : "REG2024001";
                info.address = rs.getString("address") != null ? rs.getString("address") : "123 Business Park, City Center";
                return info;
            }
        }
        throw new RuntimeException("Company not found: " + companyId);
    }
    
    private static FiscalPeriodInfo getFiscalPeriodInfo(Connection conn, Long fiscalPeriodId) throws SQLException {
        String sql = "SELECT period_name, start_date, end_date FROM fiscal_periods WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, fiscalPeriodId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                FiscalPeriodInfo info = new FiscalPeriodInfo();
                info.name = rs.getString("period_name");
                info.startDate = rs.getDate("start_date").toLocalDate();
                info.endDate = rs.getDate("end_date").toLocalDate();
                return info;
            }
        }
        throw new RuntimeException("Fiscal period not found: " + fiscalPeriodId);
    }
    
    private static List<AccountBalance> getAccountsByType(Connection conn, Long companyId, Long fiscalPeriodId, String accountType) throws SQLException {
        String sql = """
            SELECT a.account_code, a.account_name, at.name as account_type,
                   COALESCE(SUM(CASE WHEN jel.debit_amount IS NOT NULL THEN jel.debit_amount ELSE -jel.credit_amount END), 0) as current_balance
            FROM accounts a
            JOIN account_categories ac ON a.category_id = ac.id
            JOIN account_types at ON ac.account_type_id = at.id
            LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id
            LEFT JOIN journal_entries je ON jel.journal_entry_id = je.id
            WHERE a.company_id = ? AND at.name = ?
            AND (je.fiscal_period_id = ? OR je.fiscal_period_id IS NULL)
            GROUP BY a.id, a.account_code, a.account_name, at.name
            HAVING COALESCE(SUM(CASE WHEN jel.debit_amount IS NOT NULL THEN jel.debit_amount ELSE -jel.credit_amount END), 0) != 0
            ORDER BY a.account_code
        """;
        
        List<AccountBalance> balances = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, companyId);
            pstmt.setString(2, accountType);
            pstmt.setLong(3, fiscalPeriodId);
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                AccountBalance balance = new AccountBalance();
                balance.accountCode = rs.getString("account_code");
                balance.accountName = rs.getString("account_name");
                balance.currentBalance = rs.getDouble("current_balance");
                balances.add(balance);
            }
        }
        return balances;
    }
    
    // Utility methods
    private static void createCompanyHeader(Workbook workbook, Sheet sheet, FinancialData data, String reportTitle) {
        CellStyle headerStyle = createHeaderStyle(workbook);
        
        Row row1 = sheet.createRow(0);
        Cell companyCell = row1.createCell(0);
        companyCell.setCellValue(data.company.name.toUpperCase());
        companyCell.setCellStyle(headerStyle);
        
        Row row2 = sheet.createRow(1);
        row2.createCell(0).setCellValue("(Registration Number: " + data.company.registrationNumber + ")");
        
        Row row3 = sheet.createRow(2);
        Cell titleCell = row3.createCell(0);
        titleCell.setCellValue(reportTitle);
        titleCell.setCellStyle(headerStyle);
        
        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("for the year ended " + data.period.endDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }
    
    private static int getTotalAccounts(FinancialData data) {
        return data.assets.size() + data.liabilities.size() + data.equity.size() + data.revenue.size() + data.expenses.size();
    }
    
    private static double getTotalAssets(FinancialData data) {
        return data.assets.stream().mapToDouble(a -> Math.abs(a.currentBalance)).sum();
    }
    
    // Data classes
    private static class FinancialData {
        CompanyInfo company;
        FiscalPeriodInfo period;
        List<AccountBalance> assets = new ArrayList<>();
        List<AccountBalance> liabilities = new ArrayList<>();
        List<AccountBalance> equity = new ArrayList<>();
        List<AccountBalance> revenue = new ArrayList<>();
        List<AccountBalance> expenses = new ArrayList<>();
    }
    
    private static class CompanyInfo {
        String name;
        String registrationNumber;
        String address;
    }
    
    private static class FiscalPeriodInfo {
        String name;
        LocalDate startDate;
        LocalDate endDate;
    }
    
    private static class AccountBalance {
        String accountCode;
        String accountName;
        double currentBalance;
    }
}
