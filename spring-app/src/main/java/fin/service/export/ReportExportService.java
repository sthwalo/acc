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

package fin.service.export;

import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.model.report.ColumnDefinition;
import fin.service.spring.SpringCompanyService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralized service for exporting reports to various formats (PDF, Excel, CSV).
 * Eliminates code duplication and provides consistent formatting across all report types.
 * 
 * Phase 1: PDF export using Apache PDFBox 3.0.0
 * Future: Excel export (Apache POI), CSV export
 */
@Service
public class ReportExportService {
    
    // PDF Configuration (from application.properties)
    @Value("${fin.reports.pdf.font-size:10}")
    private int pdfFontSize;
    
    @Value("${fin.reports.pdf.title-font-size:16}")
    private int pdfTitleFontSize;
    
    @Value("${fin.reports.pdf.header-font-size:12}")
    private int pdfHeaderFontSize;
    
    @Value("${fin.reports.company-logo-path:logos/}")
    private String companyLogoPath;
    
    // Excel Configuration (from application.properties)
    @Value("${fin.reports.excel.auto-size-columns:true}")
    private boolean excelAutoSizeColumns;
    
    @Value("${fin.reports.excel.freeze-header-row:true}")
    private boolean excelFreezeHeaderRow;
    
    @Value("${fin.reports.excel.default-column-width:15}")
    private int excelDefaultColumnWidth;
    
    // CSV Configuration (from application.properties)
    @Value("${fin.reports.csv.delimiter:,}")
    private String csvDelimiter;
    
    @Value("${fin.reports.csv.quote-character:\"}")
    private String csvQuoteCharacter;
    
    @Value("${fin.reports.csv.include-header:true}")
    private boolean csvIncludeHeader;
    
    private final SpringCompanyService companyService;
    private final NumberFormat currencyFormat;
    
    public ReportExportService(SpringCompanyService companyService) {
        this.companyService = companyService;
        this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA"));
    }
    
    /**
     * Export report data to PDF format
     *
     * @param data List of data rows (each row is a Map of field name to value)
     * @param columns Column definitions (header names, widths, alignment)
     * @param reportTitle Report title for header (e.g., "TRIAL BALANCE")
     * @param companyId Company ID for fetching company details
     * @param fiscalPeriod Fiscal period for report header
     * @return PDF content as byte array
     * @throws IOException if PDF generation fails
     */
    public byte[] exportToPDF(
            List<Map<String, Object>> data,
            List<ColumnDefinition> columns,
            String reportTitle,
            Long companyId,
            FiscalPeriod fiscalPeriod) throws IOException {
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data provided for PDF export");
        }
        
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("No column definitions provided for PDF export");
        }
        
        // Fetch company details
        Company company = companyService.getCompanyById(companyId);
        
        PDDocument document = new PDDocument();
        
        try {
            // Add metadata
            addMetadata(document, company, fiscalPeriod, reportTitle);
            
            // Create title page
            PDPage titlePage = new PDPage(PDRectangle.A4);
            document.addPage(titlePage);
            addTitlePage(document, titlePage, company, fiscalPeriod, reportTitle, data.size());
            
            // Add report data pages
            addReportPages(document, data, columns, company, fiscalPeriod, reportTitle);
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
            
        } finally {
            document.close();
        }
    }
    
    /**
     * Export report data to Excel format (.xlsx)
     *
     * @param data List of data rows (each row is a Map of field name to value)
     * @param columns Column definitions (header names, widths, alignment)
     * @param reportTitle Report title for sheet name
     * @param companyId Company ID for fetching company details
     * @param fiscalPeriod Fiscal period for report header
     * @return Excel workbook as byte array
     * @throws IOException if Excel generation fails
     */
    public byte[] exportToExcel(
            List<Map<String, Object>> data,
            List<ColumnDefinition> columns,
            String reportTitle,
            Long companyId,
            FiscalPeriod fiscalPeriod) throws IOException {
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data provided for Excel export");
        }
        
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("No column definitions provided for Excel export");
        }
        
        // Fetch company details
        Company company = companyService.getCompanyById(companyId);
        
        Workbook workbook = new XSSFWorkbook();
        
        try {
            // Create title sheet
            Sheet titleSheet = workbook.createSheet("Title");
            addExcelTitlePage(titleSheet, reportTitle, company, fiscalPeriod, data.size());
            
            // Create data sheet
            Sheet dataSheet = workbook.createSheet(reportTitle.replaceAll("[^\\w]", "_"));
            
            // Freeze header row if configured
            if (excelFreezeHeaderRow) {
                dataSheet.createFreezePane(0, 1);
            }
            
            int rowNum = 0;
            
            // Add column headers
            Row headerRow = dataSheet.createRow(rowNum++);
            CellStyle headerStyle = createExcelHeaderStyle(workbook);
            
            for (int i = 0; i < columns.size(); i++) {
                ColumnDefinition column = columns.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.getHeaderName());
                cell.setCellStyle(headerStyle);
                
                // Set column width
                int width = column.getWidth() > 0 ? column.getWidth() : excelDefaultColumnWidth;
                dataSheet.setColumnWidth(i, width * 256); // POI uses 1/256th of a character width
            }
            
            // Add data rows
            CellStyle dataStyle = createExcelDataStyle(workbook);
            
            for (Map<String, Object> rowData : data) {
                Row dataRow = dataSheet.createRow(rowNum++);
                
                for (int i = 0; i < columns.size(); i++) {
                    ColumnDefinition column = columns.get(i);
                    Cell cell = dataRow.createCell(i);
                    
                    Object value = rowData.get(column.getFieldName());
                    setExcelCellValue(cell, value, column.getFormat());
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // Auto-size columns if configured
            if (excelAutoSizeColumns) {
                for (int i = 0; i < columns.size(); i++) {
                    dataSheet.autoSizeColumn(i);
                }
            }
            
            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } finally {
            workbook.close();
        }
    }
    
    /**
     * Add title page to Excel workbook
     */
    private void addExcelTitlePage(Sheet sheet, String reportTitle, Company company, FiscalPeriod fiscalPeriod, int dataCount) {
        int rowNum = 0;
        
        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportTitle.toUpperCase());
        CellStyle titleStyle = sheet.getWorkbook().createCellStyle();
        Font titleFont = sheet.getWorkbook().createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
        
        // Empty row
        sheet.createRow(rowNum++);
        
        // Company name
        Row companyRow = sheet.createRow(rowNum++);
        Cell companyCell = companyRow.createCell(0);
        companyCell.setCellValue("Company: " + (company != null ? company.getName() : "N/A"));
        
        // Registration number if available
        if (company != null && company.getRegistrationNumber() != null) {
            Row regRow = sheet.createRow(rowNum++);
            Cell regCell = regRow.createCell(0);
            regCell.setCellValue("Registration Number: " + company.getRegistrationNumber());
        }
        
        // Empty row
        sheet.createRow(rowNum++);
        
        // Fiscal period
        if (fiscalPeriod != null) {
            Row periodRow = sheet.createRow(rowNum++);
            Cell periodCell = periodRow.createCell(0);
            String periodText = String.format("Period: %s (%s to %s)",
                fiscalPeriod.getPeriodName(),
                fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                fiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
            periodCell.setCellValue(periodText);
        }
        
        // Empty row
        sheet.createRow(rowNum++);
        
        // Record count
        Row countRow = sheet.createRow(rowNum++);
        Cell countCell = countRow.createCell(0);
        countCell.setCellValue("Total Records: " + dataCount);
        
        // Empty row
        sheet.createRow(rowNum++);
        
        // Generation date
        Row genRow = sheet.createRow(rowNum++);
        Cell genCell = genRow.createCell(0);
        genCell.setCellValue("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        
        // Empty row
        sheet.createRow(rowNum++);
        
        // Instructions
        Row instrRow = sheet.createRow(rowNum++);
        Cell instrCell = instrRow.createCell(0);
        instrCell.setCellValue("Data: See '" + reportTitle.replaceAll("[^\\w]", "_") + "' sheet");
        
        // Auto-size column
        sheet.autoSizeColumn(0);
    }
    
    /**
     * Create Excel header cell style
     */
    private CellStyle createExcelHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }
    
    /**
     * Create Excel data cell style
     */
    private CellStyle createExcelDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }
    
    /**
     * Set Excel cell value based on data type and format
     */
    private void setExcelCellValue(Cell cell, Object value, String format) {
        if (value == null) {
            cell.setCellValue("");
            return;
        }
        
        switch (format) {
            case "currency":
                if (value instanceof BigDecimal) {
                    cell.setCellValue(((BigDecimal) value).doubleValue());
                } else if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else {
                    cell.setCellValue(value.toString());
                }
                // Set currency format
                CellStyle currencyStyle = cell.getSheet().getWorkbook().createCellStyle();
                currencyStyle.cloneStyleFrom(cell.getCellStyle());
                DataFormat dataFormat = cell.getSheet().getWorkbook().createDataFormat();
                currencyStyle.setDataFormat(dataFormat.getFormat("R #,##0.00"));
                cell.setCellStyle(currencyStyle);
                break;
                
            case "date":
                if (value instanceof LocalDate) {
                    cell.setCellValue(java.sql.Date.valueOf((LocalDate) value));
                } else if (value instanceof LocalDateTime) {
                    cell.setCellValue(java.sql.Timestamp.valueOf((LocalDateTime) value));
                } else {
                    cell.setCellValue(value.toString());
                }
                break;
                
            case "number":
                if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                } else {
                    cell.setCellValue(value.toString());
                }
                break;
                
            case "text":
            default:
                cell.setCellValue(value.toString());
                break;
        }
    }
    
    /**
     * Export report data to CSV format
     *
     * @param data List of data rows (each row is a Map of field name to value)
     * @param columns Column definitions (header names only needed for CSV)
     * @return CSV content as string
     */
    public String exportToCSV(
            List<Map<String, Object>> data,
            List<ColumnDefinition> columns) {
        
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("No data provided for CSV export");
        }
        
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("No column definitions provided for CSV export");
        }
        
        StringBuilder csv = new StringBuilder();
        
        // Add header row if configured
        if (csvIncludeHeader) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) csv.append(csvDelimiter);
                csv.append(escapeCSV(columns.get(i).getHeaderName()));
            }
            csv.append("\n");
        }
        
        // Add data rows
        for (Map<String, Object> rowData : data) {
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) csv.append(csvDelimiter);
                ColumnDefinition column = columns.get(i);
                Object value = rowData.get(column.getFieldName());
                String formattedValue = formatValue(value, column.getFormat());
                csv.append(escapeCSV(formattedValue));
            }
            csv.append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Escape CSV value according to RFC 4180
     */
    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        
        // If value contains delimiter, quote, or newline, wrap in quotes
        if (value.contains(csvDelimiter) || value.contains(csvQuoteCharacter) || value.contains("\n") || value.contains("\r")) {
            // Escape quotes by doubling them
            String escaped = value.replace(csvQuoteCharacter, csvQuoteCharacter + csvQuoteCharacter);
            return csvQuoteCharacter + escaped + csvQuoteCharacter;
        }
        
        return value;
    }
    
    /**
     * Format value based on column format specification
     */
    private String formatValue(Object value, String format) {
        if (value == null) {
            return "";
        }
        
        switch (format) {
            case "currency":
                if (value instanceof BigDecimal) {
                    return currencyFormat.format((BigDecimal) value);
                } else if (value instanceof Number) {
                    return currencyFormat.format(((Number) value).doubleValue());
                }
                return value.toString();
                
            case "date":
                if (value instanceof LocalDate) {
                    return ((LocalDate) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else if (value instanceof LocalDateTime) {
                    return ((LocalDateTime) value).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
                return value.toString();
                
            case "number":
                if (value instanceof Number) {
                    return String.format("%,.2f", ((Number) value).doubleValue());
                }
                return value.toString();
                
            case "text":
            default:
                return value.toString();
        }
    }

    /**
     * Add document metadata
     */
    private void addMetadata(PDDocument document, Company company, FiscalPeriod fiscalPeriod, String reportTitle) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(reportTitle + " - " + (company != null ? company.getName() : "FIN Report"));
        info.setSubject("Financial Report for " + (fiscalPeriod != null ? fiscalPeriod.getPeriodName() : ""));
        info.setKeywords("finance, report, " + reportTitle.toLowerCase().replace(" ", ", "));
        info.setAuthor("FIN Financial Management System");
        info.setCreator("FIN Application v1.0");
    }

    /**
     * Add title page with company and period information
     */
    private void addTitlePage(PDDocument document, PDPage page, Company company, FiscalPeriod fiscalPeriod, String reportTitle, int dataCount) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        try {
            float yPosition = PDRectangle.A4.getHeight() - 100;
            float centerX = PDRectangle.A4.getWidth() / 2;

            // Title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
            String title = reportTitle.toUpperCase();
            float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                .getStringWidth(title) / 1000 * 20;
            contentStream.newLineAtOffset(centerX - titleWidth / 2, yPosition);
            contentStream.showText(title);
            contentStream.endText();

            yPosition -= 50;

            // Company information
            if (company != null) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                String companyName = company.getName();
                float companyWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(companyName) / 1000 * 14;
                contentStream.newLineAtOffset(centerX - companyWidth / 2, yPosition);
                contentStream.showText(companyName);
                contentStream.endText();

                yPosition -= 25;

                if (company.getRegistrationNumber() != null) {
                    String regText = "Registration Number: " + company.getRegistrationNumber();
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    float regWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(regText) / 1000 * 12;
                    contentStream.newLineAtOffset(centerX - regWidth / 2, yPosition);
                    contentStream.showText(regText);
                    contentStream.endText();

                    yPosition -= 20;
                }
            }

            // Fiscal period information
            if (fiscalPeriod != null) {
                String periodText = String.format("Period: %s (%s to %s)",
                    fiscalPeriod.getPeriodName(),
                    fiscalPeriod.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    fiscalPeriod.getEndDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                );

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                float periodWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(periodText) / 1000 * 12;
                contentStream.newLineAtOffset(centerX - periodWidth / 2, yPosition - 15);
                contentStream.showText(periodText);
                contentStream.endText();

                yPosition -= 40;
            }

            // Data count
            String countText = String.format("Total Records: %d", dataCount);
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            float countWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(countText) / 1000 * 12;
            contentStream.newLineAtOffset(centerX - countWidth / 2, yPosition);
            contentStream.showText(countText);
            contentStream.endText();

            yPosition -= 30;

            // Generation date
            String genDateText = "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float genWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(genDateText) / 1000 * 10;
            contentStream.newLineAtOffset(centerX - genWidth / 2, yPosition);
            contentStream.showText(genDateText);
            contentStream.endText();

        } finally {
            contentStream.close();
        }
    }

    /**
     * Add pages with report data in table format
     */
    private void addReportPages(PDDocument document, List<Map<String, Object>> data, List<ColumnDefinition> columns, Company company, FiscalPeriod fiscalPeriod, String reportTitle) throws IOException {
        final float MARGIN_LEFT = 40f;
        final float MARGIN_TOP = 80f;
        final float MARGIN_BOTTOM = 50f;
        final float LINE_HEIGHT = 15f;
        final float FONT_SIZE = 8f;

        // Calculate column widths based on ColumnDefinition
        float[] columnWidths = new float[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            columnWidths[i] = columns.get(i).getWidth();
        }

        String[] headers = columns.stream().map(ColumnDefinition::getHeaderName).toArray(String[]::new);

        PDPage currentPage = new PDPage(PDRectangle.A4);
        document.addPage(currentPage);
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

        float yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
        int pageNumber = 2; // Title page is page 1

        try {
            // Add table header
            addTableHeader(contentStream, headers, columnWidths, MARGIN_LEFT, yPosition);
            yPosition -= LINE_HEIGHT * 1.5f;

            // Add data rows
            for (int i = 0; i < data.size(); i++) {
                Map<String, Object> rowData = data.get(i);

                // Check if we need a new page
                if (yPosition < MARGIN_BOTTOM + LINE_HEIGHT) {
                    // Add footer to current page
                    addPageFooter(contentStream, pageNumber, company, fiscalPeriod, reportTitle);
                    contentStream.close();

                    // Create new page
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
                    pageNumber++;

                    // Add table header to new page
                    addTableHeader(contentStream, headers, columnWidths, MARGIN_LEFT, yPosition);
                    yPosition -= LINE_HEIGHT * 1.5f;
                }

                // Add data row
                float usedHeight = addDataRow(contentStream, rowData, columns, columnWidths, MARGIN_LEFT, yPosition, FONT_SIZE);
                yPosition -= usedHeight;
            }

            // Add footer to last page
            addPageFooter(contentStream, pageNumber, company, fiscalPeriod, reportTitle);

        } finally {
            contentStream.close();
        }
    }

    /**
     * Add table header row
     */
    private void addTableHeader(PDPageContentStream contentStream, String[] headers, float[] columnWidths,
                               float startX, float yPosition) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);

        float xPosition = startX;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition, yPosition);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }

        // Draw header line
        contentStream.moveTo(startX, yPosition - 5);
        contentStream.lineTo(xPosition - columnWidths[columnWidths.length - 1], yPosition - 5);
        contentStream.stroke();
    }

    /**
     * Add data row with multiline support for text columns
     * Returns the height used by this row (for pagination)
     */
    private float addDataRow(PDPageContentStream contentStream, Map<String, Object> rowData,
                            List<ColumnDefinition> columns, float[] columnWidths, float startX, float yPosition, float fontSize) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), fontSize);

        float xPosition = startX;
        float maxHeightUsed = fontSize + 2; // Minimum row height

        for (int i = 0; i < columns.size(); i++) {
            ColumnDefinition column = columns.get(i);
            Object value = rowData.get(column.getFieldName());
            String displayValue = formatValue(value, column.getFormat());

            // Check if this column might need multiline (e.g., if it's a text field with long content)
            if (column.getFormat().equals("text") && displayValue.length() > 50) {
                // Use multiline for long text
                float textHeight = addMultilineText(contentStream, displayValue, xPosition, yPosition, columnWidths[i], fontSize);
                maxHeightUsed = Math.max(maxHeightUsed, textHeight);
            } else {
                // Simple text
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, yPosition);
                contentStream.showText(truncateText(displayValue, (int)(columnWidths[i] / 5))); // Rough char limit
                contentStream.endText();
            }

            xPosition += columnWidths[i];
        }

        return maxHeightUsed;
    }

    /**
     * Add multiline text within a column width
     * Returns the total height used by the multiline text
     */
    private float addMultilineText(PDPageContentStream contentStream, String text, float xPosition,
                                 float startY, float maxWidth, float fontSize) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return fontSize + 2; // Return minimum height
        }

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);
        contentStream.setFont(font, fontSize);

        // Split text into lines based on existing line breaks or wrap long lines
        String[] lines = text.split("\n");
        float currentY = startY;
        float lineHeight = fontSize + 2;
        int totalLines = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                // Handle empty lines
                totalLines++;
                currentY -= lineHeight;
                continue;
            }

            // If line is too long, wrap it
            if (font.getStringWidth(line) / 1000 * fontSize > maxWidth) {
                String[] wrappedLines = wrapText(line, maxWidth, fontSize, font);
                for (String wrappedLine : wrappedLines) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, currentY);
                    contentStream.showText(wrappedLine);
                    contentStream.endText();
                    currentY -= lineHeight;
                    totalLines++;
                }
            } else {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, currentY);
                contentStream.showText(line);
                contentStream.endText();
                currentY -= lineHeight;
                totalLines++;
            }
        }

        return totalLines * lineHeight;
    }

    /**
     * Wrap text to fit within max width
     */
    private String[] wrapText(String text, float maxWidth, float fontSize, PDType1Font font) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;

            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Add page footer
     */
    private void addPageFooter(PDPageContentStream contentStream, int pageNumber, Company company, FiscalPeriod fiscalPeriod, String reportTitle) throws IOException {
        float centerX = PDRectangle.A4.getWidth() / 2;
        float footerY = 30f;

        String footerText = String.format("Page %d | Generated: %s | FIN Financial Management System",
            pageNumber,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        float footerWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
            .getStringWidth(footerText) / 1000 * 8;
        contentStream.newLineAtOffset(centerX - footerWidth / 2, footerY);
        contentStream.showText(footerText);
        contentStream.endText();
    }

    /**
     * Truncate text to fit in column
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 1) + "." : text;
    }
}
