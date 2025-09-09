package fin.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to read and analyze Excel templates for financial reporting
 */
public class ExcelTemplateReader {
    
    public static void main(String[] args) {
        String filePath;
        if (args.length == 0) {
            // Default to the generated template file
            filePath = "/Users/sthwalonyoni/FIN/reports/Xinghizana_Group_Financial_Report_Template_20250909.xls";
            System.out.println("No file specified, analyzing generated template: " + filePath);
        } else {
            filePath = args[0];
        }
        
        ExcelTemplateReader reader = new ExcelTemplateReader();
        reader.analyzeTemplate(filePath);
    }
    
    public void analyzeTemplate(String filePath) {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            
            Workbook workbook;
            if (filePath.endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(fis);
            } else {
                workbook = new HSSFWorkbook(fis);
            }
            
            System.out.println("🏢 FINANCIAL REPORT TEMPLATE ANALYSIS");
            System.out.println("=".repeat(80));
            System.out.println("File: " + filePath);
            System.out.println("Number of sheets: " + workbook.getNumberOfSheets());
            System.out.println();
            
            // Analyze each sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                analyzeSheet(sheet, i + 1);
            }
            
            workbook.close();
            
        } catch (IOException e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void analyzeSheet(Sheet sheet, int sheetNumber) {
        System.out.println(String.format("📋 SHEET %d: %s", sheetNumber, sheet.getSheetName()));
        System.out.println("-".repeat(60));
        
        int rowCount = 0;
        int maxCols = 0;
        List<String> headers = new ArrayList<>();
        List<List<String>> sampleData = new ArrayList<>();
        
        for (Row row : sheet) {
            if (row == null) continue;
            
            rowCount++;
            List<String> rowData = new ArrayList<>();
            
            int lastCol = row.getLastCellNum();
            if (lastCol > maxCols) maxCols = lastCol;
            
            for (int colNum = 0; colNum < lastCol; colNum++) {
                Cell cell = row.getCell(colNum);
                String cellValue = getCellValueAsString(cell);
                rowData.add(cellValue);
                
                // Capture headers (assuming first non-empty row contains headers)
                if (rowCount == 1 && !cellValue.trim().isEmpty()) {
                    headers.add(cellValue);
                }
            }
            
            // Store first 15 rows as sample data
            if (rowCount <= 15) {
                sampleData.add(rowData);
            }
        }
        
        System.out.println("Dimensions: " + rowCount + " rows × " + maxCols + " columns");
        
        if (!headers.isEmpty()) {
            System.out.println("\nHeaders detected:");
            for (int i = 0; i < headers.size(); i++) {
                System.out.println(String.format("  Col %d: %s", i + 1, headers.get(i)));
            }
        }
        
        System.out.println("\nSample data (first 15 rows):");
        System.out.println("-".repeat(120));
        
        for (int i = 0; i < Math.min(15, sampleData.size()); i++) {
            List<String> row = sampleData.get(i);
            System.out.printf("Row %2d: ", i + 1);
            
            for (int col = 0; col < Math.min(8, row.size()); col++) {
                String value = row.get(col);
                if (value.length() > 12) {
                    value = value.substring(0, 9) + "...";
                }
                System.out.printf("%-15s", value);
            }
            
            if (row.size() > 8) {
                System.out.print("...");
            }
            System.out.println();
        }
        
        // Look for specific financial report patterns
        analyzeFinancialPatterns(sampleData);
        
        System.out.println();
    }
    
    private void analyzeFinancialPatterns(List<List<String>> data) {
        System.out.println("\n📊 Financial Report Pattern Analysis:");
        
        boolean hasAccountCode = false;
        boolean hasAccountName = false;
        boolean hasAmountColumns = false;
        boolean hasDateColumns = false;
        boolean hasTotals = false;
        
        for (List<String> row : data) {
            for (String cell : row) {
                String cellLower = cell.toLowerCase().trim();
                
                if (cellLower.contains("account") && cellLower.contains("code")) {
                    hasAccountCode = true;
                }
                if (cellLower.contains("account") && cellLower.contains("name")) {
                    hasAccountName = true;
                }
                if (cellLower.contains("amount") || cellLower.contains("balance") || 
                    cellLower.contains("debit") || cellLower.contains("credit")) {
                    hasAmountColumns = true;
                }
                if (cellLower.contains("date") || cellLower.contains("period")) {
                    hasDateColumns = true;
                }
                if (cellLower.contains("total") || cellLower.contains("subtotal")) {
                    hasTotals = true;
                }
            }
        }
        
        System.out.println("  • Account Code column: " + (hasAccountCode ? "✅" : "❌"));
        System.out.println("  • Account Name column: " + (hasAccountName ? "✅" : "❌"));
        System.out.println("  • Amount/Balance columns: " + (hasAmountColumns ? "✅" : "❌"));
        System.out.println("  • Date/Period columns: " + (hasDateColumns ? "✅" : "❌"));
        System.out.println("  • Total/Subtotal rows: " + (hasTotals ? "✅" : "❌"));
        
        // Suggest report type
        if (hasAccountCode && hasAccountName && hasAmountColumns) {
            System.out.println("  🎯 Detected format: Chart of Accounts / Trial Balance style");
        } else if (hasDateColumns && hasAmountColumns) {
            System.out.println("  🎯 Detected format: Transaction ledger style");
        } else {
            System.out.println("  🎯 Detected format: Custom financial report");
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
