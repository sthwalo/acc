package fin.app;

import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;

public class ComprehensiveExcelGenerator {
    
    private static final String COMPANY_NAME = "Xinghizana Group";
    private static final String PERIOD = "FY2024-2025";
    private static final String DATE = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    
    public static void main(String[] args) {
        System.out.println("🏢 COMPREHENSIVE EXCEL FINANCIAL REPORT GENERATOR");
        System.out.println("================================================");
        
        try {
            // Load environment variables
            String url = System.getenv("DATABASE_URL");
            String user = System.getenv("DATABASE_USER");
            String password = System.getenv("DATABASE_PASSWORD");
            
            if (url == null) url = "jdbc:postgresql://localhost:5432/drimacc_db";
            if (user == null) user = "sthwalonyoni";
            if (password == null) password = "LeZipho24#";
            
            System.out.println("📊 Connecting to database...");
            Connection conn = DriverManager.getConnection(url, user, password);
            
            System.out.println("📋 Creating comprehensive Excel file...");
            Workbook workbook = new HSSFWorkbook();
            
            // Create styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            
            // Create all sheets
            createCoverSheet(workbook, titleStyle, headerStyle);
            createIndexSheet(workbook, titleStyle, headerStyle);
            createCompanyDetailsSheet(workbook, titleStyle, headerStyle);
            createBalanceSheet(workbook, conn, titleStyle, headerStyle, currencyStyle);
            createIncomeStatement(workbook, conn, titleStyle, headerStyle, currencyStyle);
            createStatementOfChanges(workbook, titleStyle, headerStyle, currencyStyle);
            createCashFlowStatement(workbook, titleStyle, headerStyle, currencyStyle);
            createTrialBalance(workbook, conn, titleStyle, headerStyle, currencyStyle);
            createGeneralLedger(workbook, conn, titleStyle, headerStyle, currencyStyle);
            createJournalEntries(workbook, conn, titleStyle, headerStyle, currencyStyle);
            createAccountsReceivable(workbook, titleStyle, headerStyle, currencyStyle);
            createAccountsPayable(workbook, titleStyle, headerStyle, currencyStyle);
            createInventoryReport(workbook, titleStyle, headerStyle, currencyStyle);
            createBankReconciliation(workbook, titleStyle, headerStyle, currencyStyle);
            createNotesSheet(workbook, titleStyle, headerStyle);
            
            conn.close();
            
            // Save file
            String filename = "xinghizana_financial_report_" + DATE + ".xls";
            FileOutputStream fileOut = new FileOutputStream(filename);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            
            System.out.println("✅ Comprehensive Excel file created successfully: " + filename);
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }
    
    private static CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("R#,##0.00"));
        return style;
    }
    
    private static void createCoverSheet(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Cover");
        
        Row titleRow = sheet.createRow(2);
        Cell titleCell = titleRow.createCell(2);
        titleCell.setCellValue("FINANCIAL STATEMENTS");
        titleCell.setCellStyle(titleStyle);
        
        Row companyRow = sheet.createRow(4);
        Cell companyCell = companyRow.createCell(2);
        companyCell.setCellValue(COMPANY_NAME);
        companyCell.setCellStyle(headerStyle);
        
        Row periodRow = sheet.createRow(6);
        Cell periodCell = periodRow.createCell(2);
        periodCell.setCellValue("FOR THE YEAR ENDED " + PERIOD);
        periodCell.setCellStyle(headerStyle);
        
        Row dateRow = sheet.createRow(8);
        Cell dateCell = dateRow.createCell(2);
        dateCell.setCellValue("PREPARED ON: " + DATE);
    }
    
    private static void createIndexSheet(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Index");
        
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("INDEX OF FINANCIAL STATEMENTS");
        titleCell.setCellStyle(titleStyle);
        
        String[] sheets = {
            "Cover", "Index", "Company Details", "Balance Sheet", "Income Statement",
            "Statement of Changes", "Cash Flow", "Trial Balance", "General Ledger",
            "Journal Entries", "Accounts Receivable", "Accounts Payable",
            "Inventory Report", "Bank Reconciliation", "Notes"
        };
        
        for (int i = 0; i < sheets.length; i++) {
            Row row = sheet.createRow(i + 3);
            row.createCell(1).setCellValue((i + 1) + ".");
            row.createCell(2).setCellValue(sheets[i]);
            row.createCell(3).setCellValue("Sheet " + (i + 1));
        }
    }
    
    private static void createCompanyDetailsSheet(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Company Details");
        
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue("COMPANY DETAILS");
        titleCell.setCellStyle(titleStyle);
        
        Row[] detailRows = {
            sheet.createRow(3), sheet.createRow(4), sheet.createRow(5), sheet.createRow(6),
            sheet.createRow(7), sheet.createRow(8), sheet.createRow(9), sheet.createRow(10)
        };
        
        String[] labels = {"Company Name:", "Registration Number:", "VAT Number:", "Address:",
                          "Contact Person:", "Phone:", "Email:", "Financial Year:"};
        String[] values = {COMPANY_NAME, "Not Available", "Not Available", "Not Available",
                          "Not Available", "Not Available", "Not Available", PERIOD};
        
        for (int i = 0; i < labels.length; i++) {
            detailRows[i].createCell(1).setCellValue(labels[i]);
            detailRows[i].createCell(3).setCellValue(values[i]);
        }
    }
    
    private static void createBalanceSheet(Workbook workbook, Connection conn, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        try {
            Sheet sheet = workbook.createSheet("Balance Sheet");
            
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue(COMPANY_NAME + " - BALANCE SHEET AS AT " + PERIOD);
            titleCell.setCellStyle(titleStyle);
            
            int rowNum = 4;
            
            // ASSETS
            Row assetsHeaderRow = sheet.createRow(rowNum++);
            Cell assetsHeaderCell = assetsHeaderRow.createCell(1);
            assetsHeaderCell.setCellValue("ASSETS");
            assetsHeaderCell.setCellStyle(headerStyle);
            
            Row assetsSubHeaderRow = sheet.createRow(rowNum++);
            assetsSubHeaderRow.createCell(1).setCellValue("Account Code");
            assetsSubHeaderRow.createCell(2).setCellValue("Account Name");
            assetsSubHeaderRow.createCell(3).setCellValue("Amount (R)");
            
            // Get asset accounts (1000-1999)
            String assetQuery = "SELECT a.account_code, a.account_name, " +
                              "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                              "FROM accounts a " +
                              "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                              "WHERE a.account_code LIKE '1%' " +
                              "GROUP BY a.id, a.account_code, a.account_name " +
                              "ORDER BY a.account_code";
            
            PreparedStatement stmt = conn.prepareStatement(assetQuery);
            ResultSet rs = stmt.executeQuery();
            
            double totalAssets = 0;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(rs.getString("account_code"));
                row.createCell(2).setCellValue(rs.getString("account_name"));
                
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                double balance = debits - credits;
                totalAssets += balance;
                
                Cell balanceCell = row.createCell(3);
                balanceCell.setCellValue(balance);
                balanceCell.setCellStyle(currencyStyle);
            }
            
            // Total Assets
            Row totalAssetsRow = sheet.createRow(rowNum++);
            totalAssetsRow.createCell(2).setCellValue("TOTAL ASSETS");
            Cell totalAssetsCell = totalAssetsRow.createCell(3);
            totalAssetsCell.setCellValue(totalAssets);
            totalAssetsCell.setCellStyle(currencyStyle);
            
            rowNum += 2;
            
            // LIABILITIES
            Row liabilitiesHeaderRow = sheet.createRow(rowNum++);
            Cell liabilitiesHeaderCell = liabilitiesHeaderRow.createCell(1);
            liabilitiesHeaderCell.setCellValue("LIABILITIES");
            liabilitiesHeaderCell.setCellStyle(headerStyle);
            
            Row liabilitiesSubHeaderRow = sheet.createRow(rowNum++);
            liabilitiesSubHeaderRow.createCell(1).setCellValue("Account Code");
            liabilitiesSubHeaderRow.createCell(2).setCellValue("Account Name");
            liabilitiesSubHeaderRow.createCell(3).setCellValue("Amount (R)");
            
            // Get liability accounts (2000-2999)
            String liabilityQuery = "SELECT a.account_code, a.account_name, " +
                                  "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                                  "FROM accounts a " +
                                  "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                                  "WHERE a.account_code LIKE '2%' " +
                                  "GROUP BY a.id, a.account_code, a.account_name " +
                                  "ORDER BY a.account_code";
            
            stmt = conn.prepareStatement(liabilityQuery);
            rs = stmt.executeQuery();
            
            double totalLiabilities = 0;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(rs.getString("account_code"));
                row.createCell(2).setCellValue(rs.getString("account_name"));
                
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                double balance = credits - debits; // Liabilities have credit balances
                totalLiabilities += balance;
                
                Cell balanceCell = row.createCell(3);
                balanceCell.setCellValue(balance);
                balanceCell.setCellStyle(currencyStyle);
            }
            
            // Total Liabilities
            Row totalLiabilitiesRow = sheet.createRow(rowNum++);
            totalLiabilitiesRow.createCell(2).setCellValue("TOTAL LIABILITIES");
            Cell totalLiabilitiesCell = totalLiabilitiesRow.createCell(3);
            totalLiabilitiesCell.setCellValue(totalLiabilities);
            totalLiabilitiesCell.setCellStyle(currencyStyle);
            
            rowNum += 2;
            
            // EQUITY
            Row equityHeaderRow = sheet.createRow(rowNum++);
            Cell equityHeaderCell = equityHeaderRow.createCell(1);
            equityHeaderCell.setCellValue("EQUITY");
            equityHeaderCell.setCellStyle(headerStyle);
            
            Row equitySubHeaderRow = sheet.createRow(rowNum++);
            equitySubHeaderRow.createCell(1).setCellValue("Account Code");
            equitySubHeaderRow.createCell(2).setCellValue("Account Name");
            equitySubHeaderRow.createCell(3).setCellValue("Amount (R)");
            
            // Get equity accounts (3000-3999)
            String equityQuery = "SELECT a.account_code, a.account_name, " +
                               "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                               "FROM accounts a " +
                               "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                               "WHERE a.account_code LIKE '3%' " +
                               "GROUP BY a.id, a.account_code, a.account_name " +
                               "ORDER BY a.account_code";
            
            stmt = conn.prepareStatement(equityQuery);
            rs = stmt.executeQuery();
            
            double totalEquity = 0;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(rs.getString("account_code"));
                row.createCell(2).setCellValue(rs.getString("account_name"));
                
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                double balance = credits - debits; // Equity has credit balances
                totalEquity += balance;
                
                Cell balanceCell = row.createCell(3);
                balanceCell.setCellValue(balance);
                balanceCell.setCellStyle(currencyStyle);
            }
            
            // Total Equity
            Row totalEquityRow = sheet.createRow(rowNum++);
            totalEquityRow.createCell(2).setCellValue("TOTAL EQUITY");
            Cell totalEquityCell = totalEquityRow.createCell(3);
            totalEquityCell.setCellValue(totalEquity);
            totalEquityCell.setCellStyle(currencyStyle);
            
            // Total Liabilities + Equity
            rowNum++;
            Row totalLiabEquityRow = sheet.createRow(rowNum++);
            totalLiabEquityRow.createCell(2).setCellValue("TOTAL LIABILITIES + EQUITY");
            Cell totalLiabEquityCell = totalLiabEquityRow.createCell(3);
            totalLiabEquityCell.setCellValue(totalLiabilities + totalEquity);
            totalLiabEquityCell.setCellStyle(currencyStyle);
            
            rs.close();
            stmt.close();
            
        } catch (Exception e) {
            System.err.println("Error creating balance sheet: " + e.getMessage());
        }
    }
    
    private static void createIncomeStatement(Workbook workbook, Connection conn, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        try {
            Sheet sheet = workbook.createSheet("Income Statement");
            
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue(COMPANY_NAME + " - INCOME STATEMENT FOR " + PERIOD);
            titleCell.setCellStyle(titleStyle);
            
            int rowNum = 4;
            
            // REVENUE
            Row revenueHeaderRow = sheet.createRow(rowNum++);
            Cell revenueHeaderCell = revenueHeaderRow.createCell(1);
            revenueHeaderCell.setCellValue("REVENUE");
            revenueHeaderCell.setCellStyle(headerStyle);
            
            Row revenueSubHeaderRow = sheet.createRow(rowNum++);
            revenueSubHeaderRow.createCell(1).setCellValue("Account Code");
            revenueSubHeaderRow.createCell(2).setCellValue("Account Name");
            revenueSubHeaderRow.createCell(3).setCellValue("Amount (R)");
            
            // Get revenue accounts (4000-4999)
            String revenueQuery = "SELECT a.account_code, a.account_name, " +
                                "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                                "FROM accounts a " +
                                "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                                "WHERE a.account_code LIKE '4%' " +
                                "GROUP BY a.id, a.account_code, a.account_name " +
                                "ORDER BY a.account_code";
            
            PreparedStatement stmt = conn.prepareStatement(revenueQuery);
            ResultSet rs = stmt.executeQuery();
            
            double totalRevenue = 0;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(rs.getString("account_code"));
                row.createCell(2).setCellValue(rs.getString("account_name"));
                
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                double balance = credits - debits; // Revenue has credit balances
                totalRevenue += balance;
                
                Cell balanceCell = row.createCell(3);
                balanceCell.setCellValue(balance);
                balanceCell.setCellStyle(currencyStyle);
            }
            
            // Total Revenue
            Row totalRevenueRow = sheet.createRow(rowNum++);
            totalRevenueRow.createCell(2).setCellValue("TOTAL REVENUE");
            Cell totalRevenueCell = totalRevenueRow.createCell(3);
            totalRevenueCell.setCellValue(totalRevenue);
            totalRevenueCell.setCellStyle(currencyStyle);
            
            rowNum += 2;
            
            // EXPENSES
            Row expensesHeaderRow = sheet.createRow(rowNum++);
            Cell expensesHeaderCell = expensesHeaderRow.createCell(1);
            expensesHeaderCell.setCellValue("EXPENSES");
            expensesHeaderCell.setCellStyle(headerStyle);
            
            Row expensesSubHeaderRow = sheet.createRow(rowNum++);
            expensesSubHeaderRow.createCell(1).setCellValue("Account Code");
            expensesSubHeaderRow.createCell(2).setCellValue("Account Name");
            expensesSubHeaderRow.createCell(3).setCellValue("Amount (R)");
            
            // Get expense accounts (5000-9999)
            String expenseQuery = "SELECT a.account_code, a.account_name, " +
                                "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                                "FROM accounts a " +
                                "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                                "WHERE a.account_code LIKE '5%' OR a.account_code LIKE '6%' OR a.account_code LIKE '7%' OR a.account_code LIKE '8%' OR a.account_code LIKE '9%' " +
                                "GROUP BY a.id, a.account_code, a.account_name " +
                                "ORDER BY a.account_code";
            
            stmt = conn.prepareStatement(expenseQuery);
            rs = stmt.executeQuery();
            
            double totalExpenses = 0;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(1).setCellValue(rs.getString("account_code"));
                row.createCell(2).setCellValue(rs.getString("account_name"));
                
                double debits = rs.getDouble("total_debits");
                double credits = rs.getDouble("total_credits");
                double balance = debits - credits; // Expenses have debit balances
                totalExpenses += balance;
                
                Cell balanceCell = row.createCell(3);
                balanceCell.setCellValue(balance);
                balanceCell.setCellStyle(currencyStyle);
            }
            
            // Total Expenses
            Row totalExpensesRow = sheet.createRow(rowNum++);
            totalExpensesRow.createCell(2).setCellValue("TOTAL EXPENSES");
            Cell totalExpensesCell = totalExpensesRow.createCell(3);
            totalExpensesCell.setCellValue(totalExpenses);
            totalExpensesCell.setCellStyle(currencyStyle);
            
            rowNum += 2;
            
            // Net Income
            Row netIncomeRow = sheet.createRow(rowNum++);
            netIncomeRow.createCell(2).setCellValue("NET INCOME");
            Cell netIncomeCell = netIncomeRow.createCell(3);
            netIncomeCell.setCellValue(totalRevenue - totalExpenses);
            netIncomeCell.setCellStyle(currencyStyle);
            
            rs.close();
            stmt.close();
            
        } catch (Exception e) {
            System.err.println("Error creating income statement: " + e.getMessage());
        }
    }
    
    private static void createTrialBalance(Workbook workbook, Connection conn, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        try {
            Sheet sheet = workbook.createSheet("Trial Balance");
            
            Row titleRow = sheet.createRow(1);
            Cell titleCell = titleRow.createCell(1);
            titleCell.setCellValue(COMPANY_NAME + " - TRIAL BALANCE AS AT " + PERIOD);
            titleCell.setCellStyle(titleStyle);
            
            int rowNum = 4;
            
            Row headerRow = sheet.createRow(rowNum++);
            headerRow.createCell(1).setCellValue("Account Code");
            headerRow.createCell(2).setCellValue("Account Name");
            headerRow.createCell(3).setCellValue("Debit (R)");
            headerRow.createCell(4).setCellValue("Credit (R)");
            
            // Get all accounts with their balances
            String query = "SELECT a.account_code, a.account_name, " +
                          "SUM(jel.debit_amount) as total_debits, SUM(jel.credit_amount) as total_credits " +
                          "FROM accounts a " +
                          "LEFT JOIN journal_entry_lines jel ON a.id = jel.account_id " +
                          "GROUP BY a.id, a.account_code, a.account_name " +
                          "ORDER BY a.account_code";
            
            try (PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                
                double totalDebits = 0;
                double totalCredits = 0;
                
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(1).setCellValue(rs.getString("account_code"));
                    row.createCell(2).setCellValue(rs.getString("account_name"));
                    
                    double debits = rs.getDouble("total_debits");
                    double credits = rs.getDouble("total_credits");
                    
                    totalDebits += debits;
                    totalCredits += credits;
                    
                    if (debits > 0) {
                        Cell debitCell = row.createCell(3);
                        debitCell.setCellValue(debits);
                        debitCell.setCellStyle(currencyStyle);
                    }
                    
                    if (credits > 0) {
                        Cell creditCell = row.createCell(4);
                        creditCell.setCellValue(credits);
                        creditCell.setCellStyle(currencyStyle);
                    }
                }
                
                // Totals
                Row totalRow = sheet.createRow(rowNum++);
                totalRow.createCell(2).setCellValue("TOTALS");
                Cell totalDebitCell = totalRow.createCell(3);
                totalDebitCell.setCellValue(totalDebits);
                totalDebitCell.setCellStyle(currencyStyle);
                
                Cell totalCreditCell = totalRow.createCell(4);
                totalCreditCell.setCellValue(totalCredits);
                totalCreditCell.setCellStyle(currencyStyle);
                
            }
            
        } catch (Exception e) {
            System.err.println("Error creating trial balance: " + e.getMessage());
        }
    }
    
    // Placeholder methods for other sheets - they'll have basic structure
    private static void createStatementOfChanges(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Statement of Changes");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - STATEMENT OF CHANGES IN EQUITY FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("This statement requires opening balances to be populated manually.");
    }
    
    private static void createCashFlowStatement(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Cash Flow");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - CASH FLOW STATEMENT FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("This statement requires cash flow categorization to be populated manually.");
    }
    
    private static void createGeneralLedger(Workbook workbook, Connection conn, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("General Ledger");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - GENERAL LEDGER SUMMARY FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("See Trial Balance for account balances. Detailed GL available on request.");
    }
    
    private static void createJournalEntries(Workbook workbook, Connection conn, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Journal Entries");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - JOURNAL ENTRIES SUMMARY FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        try {
            Row countRow = sheet.createRow(3);
            countRow.createCell(1).setCellValue("Total Journal Entries:");
            
            String countQuery = "SELECT COUNT(*) as total_entries FROM journal_entries";
            try (PreparedStatement stmt = conn.prepareStatement(countQuery);
                 ResultSet rs = stmt.executeQuery()) {
                
                if (rs.next()) {
                    countRow.createCell(3).setCellValue(rs.getInt("total_entries"));
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in journal entries: " + e.getMessage());
        }
    }
    
    private static void createAccountsReceivable(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Accounts Receivable");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - ACCOUNTS RECEIVABLE FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("Customer aging analysis to be populated manually.");
    }
    
    private static void createAccountsPayable(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Accounts Payable");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - ACCOUNTS PAYABLE FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("Supplier aging analysis to be populated manually.");
    }
    
    private static void createInventoryReport(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Inventory Report");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - INVENTORY REPORT FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("Inventory details to be populated manually.");
    }
    
    private static void createBankReconciliation(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Bank Reconciliation");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - BANK RECONCILIATION FOR " + PERIOD);
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("Bank reconciliation details to be populated manually.");
    }
    
    private static void createNotesSheet(Workbook workbook, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Notes");
        Row titleRow = sheet.createRow(1);
        Cell titleCell = titleRow.createCell(1);
        titleCell.setCellValue(COMPANY_NAME + " - NOTES TO FINANCIAL STATEMENTS");
        titleCell.setCellStyle(titleStyle);
        
        Row noteRow = sheet.createRow(3);
        noteRow.createCell(1).setCellValue("Additional notes and disclosures to be added manually.");
    }
}
