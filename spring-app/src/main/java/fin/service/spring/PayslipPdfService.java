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

import com.sun.jna.Pointer;
import fin.model.Company;
import fin.model.Employee;
import fin.model.FiscalPeriod;
import fin.model.Payslip;
import fin.util.Libharu;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating PDF payslips using libharu
 */
public class PayslipPdfService {

    // Helper classes for parameter reduction
    private static class PdfDimensions {
        private final float pageWidth;
        private final float pageHeight;
        private final float marginLeft;
        private final float marginRight;
        private final float marginTop;
        private final float contentWidth;

        PdfDimensions(float width, float height, float leftMargin, float rightMargin, float topMargin,
                     float widthContent) {
            this.pageWidth = width;
            this.pageHeight = height;
            this.marginLeft = leftMargin;
            this.marginRight = rightMargin;
            this.marginTop = topMargin;
            this.contentWidth = widthContent;
        }
    }

    private static class FontPair {
        private final Pointer regularFont;
        private final Pointer boldFont;

        FontPair(Pointer font, Pointer fontBold) {
            this.regularFont = font;
            this.boldFont = fontBold;
        }
    }

    // Page layout constants
    private static final float PAGE_WIDTH_A4 = 595.28f;  // A4 width in points
    private static final float PAGE_HEIGHT_A4 = 841.89f; // A4 height in points
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 50f;

    // Font size constants
    private static final float FONT_SIZE_TITLE = 24f;
    private static final float FONT_SIZE_COMPANY_NAME = 16f;
    private static final float FONT_SIZE_SECTION_HEADER = 12f;
    private static final float FONT_SIZE_NORMAL = 10f;
    private static final float FONT_SIZE_SMALL = 9f;

    // Spacing and positioning constants
    private static final float HEADER_BORDER_HEIGHT = 140f;
    private static final float LOGO_MAX_SIZE = 60f;
    private static final float LOGO_ASPECT_RATIO = 0.75f; // 4:3 aspect ratio
    private static final float LOGO_BOTTOM_SPACING = 20f;
    private static final float LOGO_BOTTOM_MARGIN = 40f;
    private static final float LOGO_PLACEHOLDER_SPACING = 20f;
    private static final float TITLE_BACKGROUND_HEIGHT = 30f;
    private static final float TITLE_BACKGROUND_PADDING = 5f;
    private static final float TITLE_APPROXIMATE_WIDTH = 100f;
    private static final float TITLE_VERTICAL_OFFSET = 25f;
    private static final float COMPANY_NAME_VERTICAL_OFFSET = 50f;
    private static final float COMPANY_NAME_CHAR_WIDTH = 8f;
    private static final float UNDERLINE_OFFSET = 3f;
    private static final float HEADER_BOTTOM_SPACING = 80f;

    // Employee details section constants
    private static final float EMPLOYEE_SECTION_HEIGHT = 120f;
    private static final float EMPLOYEE_SECTION_HEADER_HEIGHT = 18f;
    private static final float EMPLOYEE_SECTION_HEADER_OFFSET = 15f;
    private static final float EMPLOYEE_HEADER_TEXT_OFFSET = 10f;
    private static final float EMPLOYEE_RECTANGLE_INSET = 2f;
    private static final float EMPLOYEE_RECTANGLE_WIDTH_REDUCTION = 4f;
    private static final float EMPLOYEE_CONTENT_START_OFFSET = 30f;
    private static final float EMPLOYEE_COLUMN_SPACING = 15f;
    private static final float EMPLOYEE_LABEL_WIDTH = 85f;
    private static final float EMPLOYEE_ROW_HEIGHT = 30f;
    private static final float EMPLOYEE_SECTION_BOTTOM_SPACING = 80f;

    // Earnings/Deductions section constants
    private static final float EARNINGS_DEDUCTIONS_TABLE_SPACING = 20f;
    private static final float EARNINGS_DEDUCTIONS_TABLE_TOP_OFFSET = 15f;
    private static final float EARNINGS_DEDUCTIONS_TABLE_HEIGHT = 150f;
    private static final float EARNINGS_DEDUCTIONS_SECTION_HEADER_HEIGHT = 15f;
    private static final float EARNINGS_DEDUCTIONS_SECTION_HEADER_OFFSET = 12f;
    private static final float EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET = 70f;
    private static final float EARNINGS_DEDUCTIONS_BOTTOM_SPACING = 40f;

    // Net pay section constants
    private static final float NET_PAY_SECTION_HEIGHT = 40f;
    private static final float NET_PAY_LABEL_OFFSET = 20f;
    private static final float NET_PAY_AMOUNT_OFFSET = 120f;
    private static final float NET_PAY_BOTTOM_SPACING = 20f;

    // Footer constants
    private static final float FOOTER_LINE_SPACING = 15f;

    // Additional constants for earnings/deductions section
    private static final float EARNINGS_DEDUCTIONS_TOTAL_ROW_SPACING = 95f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT = 20f;
    private static final float EARNINGS_DEDUCTIONS_SECTION_BOTTOM_SPACING = 30f;

    // Table layout constants for earnings/deductions section
    private static final float EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING = 20f;
    private static final float EARNINGS_DEDUCTIONS_ROW_START_Y_OFFSET = 50f;
    private static final float EARNINGS_DEDUCTIONS_RECTANGLE_INSET = 2f;
    private static final float EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION = 4f;
    private static final float EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET = 10f;
    private static final float EARNINGS_DEDUCTIONS_HEADER_Y_OFFSET = 15f;
    private static final float EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET = 15f;
    private static final float EARNINGS_DEDUCTIONS_TABLE_HEADER_BORDER_Y_OFFSET = 2f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_BACKGROUND_Y_OFFSET = 5f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_BORDER_Y_OFFSET = 5f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_TEXT_OFFSET = 10f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_AMOUNT_OFFSET = 70f;

    // Additional font size constants
    private static final float FONT_SIZE_MEDIUM = 11f;
    private static final float FONT_SIZE_LARGE = 14f;
    private static final float FONT_SIZE_TINY = 9f;

    // Additional color constants
    private static final float[] COLOR_LIGHT_GREEN = {0.95f, 1.0f, 0.95f};

    // Additional footer constants
    private static final float FOOTER_FIXED_POSITION = 100f;
    private static final float FOOTER_TEXT_OFFSET = 15f;
    private static final float FOOTER_DATE_OFFSET = 25f;
    private static final float FOOTER_CONFIDENTIAL_OFFSET = 40f;

    // Color constants (RGB values)
    private static final float[] COLOR_BLUE_BORDER = {0.2f, 0.4f, 0.8f};
    private static final float[] COLOR_BLACK = {0f, 0f, 0f};
    private static final float[] COLOR_LIGHT_BLUE_BACKGROUND = {0.9f, 0.95f, 1.0f};
    private static final float[] COLOR_LIGHT_GRAY_BACKGROUND = {0.95f, 0.95f, 0.95f};
    private static final float[] COLOR_LIGHT_BLUE_HEADER = {0.8f, 0.8f, 0.9f};
    private static final float[] COLOR_LIGHT_GRAY_HEADER = {0.9f, 0.9f, 0.9f};
    private static final float[] COLOR_VERY_LIGHT_GRAY = {0.95f, 0.95f, 0.95f};

    // Line width constants
    private static final float LINE_WIDTH_THICK = 2.0f;
    private static final float LINE_WIDTH_NORMAL = 1.0f;
    private static final float LINE_WIDTH_MEDIUM = 1.5f;

    // Text truncation constants
    private static final int MAX_NAME_LENGTH = 25;
    private static final int NAME_TRUNCATE_LENGTH = 22;

    private static class DrawingContext {
        private final Pointer page;
        private final float marginLeft;
        private final float contentWidth;
        private final Pointer font;
        private final Pointer boldFont;

        DrawingContext(Pointer pagePtr, float leftMargin, float widthContent, Pointer fontPtr, Pointer fontBold) {
            this.page = pagePtr;
            this.marginLeft = leftMargin;
            this.contentWidth = widthContent;
            this.font = fontPtr;
            this.boldFont = fontBold;
        }
    }

    private static class HeaderDrawingContext {
        private final Pointer page;
        private final Company company;
        private final Pointer logo;
        private final float pageWidth;
        private final float marginLeft;
        private final float contentWidth;
        private final Pointer boldFont;

        HeaderDrawingContext(Pointer pagePtr, Company companyData, Pointer logoPtr, float width,
                           float leftMargin, float widthContent, Pointer fontBold) {
            this.page = pagePtr;
            this.company = companyData;
            this.logo = logoPtr;
            this.pageWidth = width;
            this.marginLeft = leftMargin;
            this.contentWidth = widthContent;
            this.boldFont = fontBold;
        }
    }

    /**
     * Generates a PDF payslip for an employee using libharu
     */
    public String generatePayslipPdf(Payslip payslip, Employee employee, Company company, FiscalPeriod fiscalPeriod) {
        // Validate and complete payslip data
        validatePayslipData(payslip);

        // Create payslips directory if it doesn't exist
        Path payslipsDir = Paths.get("payslips");
        try {
            if (!Files.exists(payslipsDir)) {
                Files.createDirectories(payslipsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create payslips directory", e);
        }

        // Generate filename with absolute path
        Path filePath = payslipsDir.resolve(String.format("payslip_%s_%s_%s.pdf",
                employee.getEmployeeNumber(),
                fiscalPeriod.getPayDate().format(DateTimeFormatter.ofPattern("yyyyMM")),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

        String filename = filePath.toString();

        try {
            generatePayslipPDF(payslip, employee, company, fiscalPeriod, filename);
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payslip PDF: " + e.getMessage(), e);
        }
    }

    private static void generatePayslipPDF(Payslip payslip, Employee employee, Company company, FiscalPeriod fiscalPeriod, String filename) {
        Pointer pdf = initializePdfDocument();
        Pointer page = setupPdfPage(pdf);
        PdfDimensions dimensions = calculatePageDimensions();
        FontPair fonts = loadFonts(pdf);
        Pointer logo = loadCompanyLogo(pdf, company);

        PayslipContentData contentData = new PayslipContentData(company, logo, employee, payslip, fiscalPeriod);
        drawPayslipContent(page, contentData, dimensions, fonts.regularFont, fonts.boldFont);

        saveAndCleanupPdf(pdf, filename);
    }

    private static Pointer initializePdfDocument() {
        Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
        if (pdf == null) {
            throw new RuntimeException("Failed to create PDF document");
        }
        return pdf;
    }

    private static Pointer setupPdfPage(Pointer pdf) {
        Pointer page = Libharu.INSTANCE.HPDF_AddPage(pdf);
        if (page == null) {
            throw new RuntimeException("Failed to add page");
        }
        Libharu.INSTANCE.HPDF_Page_SetSize(page, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);
        return page;
    }

    private static PdfDimensions calculatePageDimensions() {
        return new PdfDimensions(PAGE_WIDTH_A4, PAGE_HEIGHT_A4, MARGIN_LEFT, MARGIN_RIGHT, MARGIN_TOP,
                               PAGE_WIDTH_A4 - MARGIN_LEFT - MARGIN_RIGHT);
    }

    private static FontPair loadFonts(Pointer pdf) {
        Pointer font = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica", null);
        Pointer boldFont = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica-Bold", null);
        return new FontPair(font, boldFont);
    }

    private static Pointer loadCompanyLogo(Pointer pdf, Company company) {
        if (company.getLogoPath() != null && !company.getLogoPath().isEmpty()) {
            try {
                Pointer logo = Libharu.INSTANCE.HPDF_LoadPngImageFromFile(pdf, company.getLogoPath());
                if (logo != null) {
                    System.out.println("✅ Company logo loaded successfully");
                    return logo;
                }
            } catch (Exception e) {
                System.err.println("⚠️ Failed to load company logo: " + e.getMessage());
            }
        }
        return null;
    }

    private static void drawPayslipContent(Pointer page, PayslipContentData contentData, PdfDimensions dimensions,
                                         Pointer font, Pointer boldFont) {
        float yPosition = dimensions.pageHeight - dimensions.marginTop;

        // Draw header section with enhanced styling
        HeaderDrawingContext headerContext = new HeaderDrawingContext(page, contentData.company, contentData.logo, dimensions.pageWidth,
                                                                     dimensions.marginLeft, dimensions.contentWidth, boldFont);
        yPosition = drawHeaderSection(headerContext, yPosition);

        // Draw employee details section with borders
        DrawingContext context = new DrawingContext(page, dimensions.marginLeft, dimensions.contentWidth, font, boldFont);
        yPosition = drawEmployeeDetailsSection(context, contentData.employee, contentData.payslip, contentData.fiscalPeriod, yPosition);

        // Draw earnings and deductions section with tables
        yPosition = drawEarningsDeductionsSection(page, contentData.payslip, yPosition, dimensions.marginLeft,
                                                dimensions.contentWidth, font, boldFont);

        // Draw net pay section with enhanced styling
        yPosition = drawNetPaySection(page, contentData.payslip, yPosition, dimensions.marginLeft, dimensions.contentWidth, boldFont);

        // Draw footer with enhanced styling
        drawFooterSection(page, yPosition, dimensions.marginLeft, dimensions.pageWidth, dimensions.marginRight, font);
    }

    private static void saveAndCleanupPdf(Pointer pdf, String filename) {
        int result = Libharu.INSTANCE.HPDF_SaveToFile(pdf, filename);
        if (result != 0) {
            throw new RuntimeException("Failed to save PDF, error code: " + result);
        }
        Libharu.INSTANCE.HPDF_Free(pdf);
    }

    private static float drawHeaderSection(HeaderDrawingContext context, float yPosition) {
        // Draw header border
        drawHeaderBorder(context.page, context.marginLeft, yPosition, context.contentWidth);

        // Draw company logo
        yPosition = drawCompanyLogo(context, yPosition);

        // Draw payslip title
        yPosition = drawPayslipTitle(context, yPosition);

        // Draw company name
        yPosition = drawCompanyName(context, yPosition);

        return yPosition - HEADER_BOTTOM_SPACING;
    }

    private static void drawHeaderBorder(Pointer page, float marginLeft, float yPosition, float contentWidth) {
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, COLOR_BLUE_BORDER[0], COLOR_BLUE_BORDER[1], COLOR_BLUE_BORDER[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - HEADER_BORDER_HEIGHT, contentWidth, HEADER_BORDER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_NORMAL);
    }

    private static float drawCompanyLogo(HeaderDrawingContext context, float yPosition) {
        if (context.logo != null) {
            float logoWidth = LOGO_MAX_SIZE;
            float logoHeight = LOGO_MAX_SIZE * LOGO_ASPECT_RATIO;
            float logoX = (context.pageWidth - logoWidth) / 2;
            float logoY = yPosition - logoHeight - LOGO_BOTTOM_SPACING;

            Libharu.INSTANCE.HPDF_Page_DrawImage(context.page, context.logo, logoX, logoY, logoWidth, logoHeight);
            return yPosition - (logoHeight + LOGO_BOTTOM_MARGIN);
        }
        return yPosition - LOGO_PLACEHOLDER_SPACING;
    }

    private static float drawPayslipTitle(HeaderDrawingContext context, float yPosition) {
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(context.page, COLOR_LIGHT_BLUE_BACKGROUND[0], COLOR_LIGHT_BLUE_BACKGROUND[1], COLOR_LIGHT_BLUE_BACKGROUND[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(context.page, context.marginLeft + TITLE_BACKGROUND_PADDING,
                                           yPosition - TITLE_BACKGROUND_HEIGHT,
                                           context.contentWidth - TITLE_BACKGROUND_PADDING * 2, TITLE_BACKGROUND_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(context.page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(context.page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.boldFont, FONT_SIZE_TITLE);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        String title = "PAYSLIP";
        float titleWidth = TITLE_APPROXIMATE_WIDTH;
        float titleX = (context.pageWidth - titleWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, titleX, yPosition - TITLE_VERTICAL_OFFSET, title);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);

        return yPosition - COMPANY_NAME_VERTICAL_OFFSET;
    }

    private static float drawCompanyName(HeaderDrawingContext context, float yPosition) {
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.boldFont, FONT_SIZE_COMPANY_NAME);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        String companyName = context.company.getName();
        float companyNameWidth = companyName.length() * COMPANY_NAME_CHAR_WIDTH;
        float companyX = (context.pageWidth - companyNameWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, companyX, yPosition, companyName);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);

        Libharu.INSTANCE.HPDF_Page_SetLineWidth(context.page, LINE_WIDTH_MEDIUM);
        Libharu.INSTANCE.HPDF_Page_MoveTo(context.page, companyX, yPosition - UNDERLINE_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(context.page, companyX + companyNameWidth, yPosition - UNDERLINE_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(context.page);

        return yPosition;
    }

    private static float drawEmployeeDetailsSection(DrawingContext context, Employee employee, Payslip payslip, FiscalPeriod fiscalPeriod, float yPosition) {
        // Draw employee section background
        drawEmployeeSectionBackground(context.page, context.marginLeft, yPosition, context.contentWidth);

        // Draw employee section header
        drawEmployeeSectionHeader(context, yPosition);

        // Draw employee details rows
        yPosition = drawEmployeeDetailsRows(context, employee, payslip, fiscalPeriod, yPosition);

        return yPosition - EMPLOYEE_SECTION_BOTTOM_SPACING;
    }

    private static void drawEmployeeSectionBackground(Pointer page, float marginLeft, float yPosition, float contentWidth) {
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GRAY_BACKGROUND[0], COLOR_LIGHT_GRAY_BACKGROUND[1], COLOR_LIGHT_GRAY_BACKGROUND[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - EMPLOYEE_SECTION_HEIGHT, contentWidth, EMPLOYEE_SECTION_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_NORMAL);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - EMPLOYEE_SECTION_HEIGHT, contentWidth, EMPLOYEE_SECTION_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
    }

    private static void drawEmployeeSectionHeader(DrawingContext context, float yPosition) {
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(context.page, COLOR_LIGHT_BLUE_HEADER[0], COLOR_LIGHT_BLUE_HEADER[1], COLOR_LIGHT_BLUE_HEADER[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(context.page, context.marginLeft + EMPLOYEE_RECTANGLE_INSET,
                                           yPosition - EMPLOYEE_SECTION_HEADER_HEIGHT,
                                           context.contentWidth - EMPLOYEE_RECTANGLE_WIDTH_REDUCTION, EMPLOYEE_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(context.page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(context.page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.boldFont, FONT_SIZE_SECTION_HEADER);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, context.marginLeft + EMPLOYEE_HEADER_TEXT_OFFSET,
                                         yPosition - EMPLOYEE_SECTION_HEADER_OFFSET, "EMPLOYEE DETAILS");
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);
    }

    private static float drawEmployeeDetailsRows(DrawingContext context, Employee employee, Payslip payslip, FiscalPeriod fiscalPeriod, float yPosition) {
        yPosition -= EMPLOYEE_CONTENT_START_OFFSET;

        float col1X = context.marginLeft + EMPLOYEE_COLUMN_SPACING;
        float col2X = context.marginLeft + context.contentWidth / 2 + EMPLOYEE_COLUMN_SPACING;
        float labelWidth = EMPLOYEE_LABEL_WIDTH;
        float rowHeight = EMPLOYEE_ROW_HEIGHT;

        // Row 1: Employee Code and Pay Date
        EmployeeDetailRowData row1Data = new EmployeeDetailRowData("Employee Code:", employee.getEmployeeNumber(),
                                                                 "Pay Date:", fiscalPeriod.getPayDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        drawEmployeeDetailRow(context, col1X, col2X, labelWidth, yPosition, row1Data);
        yPosition -= rowHeight;

        // Row 2: Employee Name and Pay Period
        String fullName = employee.getFullName();
        if (fullName.length() > MAX_NAME_LENGTH) {
            fullName = fullName.substring(0, NAME_TRUNCATE_LENGTH) + "...";
        }
        EmployeeDetailRowData row2Data = new EmployeeDetailRowData("Employee Name:", fullName,
                                                                 "Pay Period:", fiscalPeriod.getPeriodName());
        drawEmployeeDetailRow(context, col1X, col2X, labelWidth, yPosition, row2Data);
        yPosition -= rowHeight;

        // Row 3: Tax Number and Employment Date
        String taxNumber = employee.getTaxNumber() != null ? employee.getTaxNumber() : "Not Provided";
        String hireDate = employee.getHireDate() != null
            ? employee.getHireDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            : "Not Provided";
        EmployeeDetailRowData row3Data = new EmployeeDetailRowData("Tax Number:", taxNumber,
                                                                 "Employment Date:", hireDate);
        drawEmployeeDetailRow(context, col1X, col2X, labelWidth, yPosition, row3Data);

        return yPosition;
    }

    private static void drawEmployeeDetailRow(DrawingContext context, float col1X, float col2X, float labelWidth,
                                            float yPosition, EmployeeDetailRowData rowData) {
        // Left column
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, col1X, yPosition, rowData.label1);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, col1X + labelWidth, yPosition, rowData.value1);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);

        // Right column
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, col2X, yPosition, rowData.label2);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(context.page, context.boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(context.page);
        Libharu.INSTANCE.HPDF_Page_TextOut(context.page, col2X + labelWidth, yPosition, rowData.value2);
        Libharu.INSTANCE.HPDF_Page_EndText(context.page);
    }

    private static float drawEarningsDeductionsSection(Pointer page, Payslip payslip, float yPosition,
                                                     float marginLeft, float contentWidth, Pointer font, Pointer boldFont) {
        EarningsDeductionsLayout layout = calculateEarningsDeductionsLayout(marginLeft, contentWidth, yPosition);

        // Draw table structure and borders
        drawEarningsDeductionsTableStructure(page, layout);

        // Draw section headers
        drawEarningsDeductionsHeaders(page, layout, boldFont);

        // Draw table column headers
        drawEarningsDeductionsTableHeaders(page, layout, boldFont);

        // Draw earnings table content
        float rowY = drawEarningsTable(page, payslip, layout, font, boldFont);

        // Draw deductions table content
        drawDeductionsTable(page, payslip, layout, font, boldFont);

        // Draw totals section
        rowY = drawEarningsDeductionsTotals(page, payslip, layout, rowY, boldFont);

        // Calculate final position
        return calculateEarningsDeductionsFinalPosition(rowY);
    }

    private static class EarningsDeductionsLayout {
        private final float columnWidth;
        private final float leftColumnX;
        private final float rightColumnX;
        private final float tableTopY;
        private final float tableHeight;

        EarningsDeductionsLayout(float width, float leftX, float rightX, float topY, float height) {
            this.columnWidth = width;
            this.leftColumnX = leftX;
            this.rightColumnX = rightX;
            this.tableTopY = topY;
            this.tableHeight = height;
        }
    }

    private static class PayslipContentData {
        private final Company company;
        private final Pointer logo;
        private final Employee employee;
        private final Payslip payslip;
        private final FiscalPeriod fiscalPeriod;

        PayslipContentData(Company companyData, Pointer logoData, Employee employeeData, Payslip payslipData, FiscalPeriod periodData) {
            this.company = companyData;
            this.logo = logoData;
            this.employee = employeeData;
            this.payslip = payslipData;
            this.fiscalPeriod = periodData;
        }
    }

    private static class EmployeeDetailRowData {
        private final String label1;
        private final String value1;
        private final String label2;
        private final String value2;

        EmployeeDetailRowData(String lbl1, String val1, String lbl2, String val2) {
            this.label1 = lbl1;
            this.value1 = val1;
            this.label2 = lbl2;
            this.value2 = val2;
        }
    }

    private static EarningsDeductionsLayout calculateEarningsDeductionsLayout(float marginLeft, float contentWidth, float yPosition) {
        float columnWidth = (contentWidth - EARNINGS_DEDUCTIONS_TABLE_SPACING) / 2;
        float leftColumnX = marginLeft;
        float rightColumnX = marginLeft + columnWidth + EARNINGS_DEDUCTIONS_TABLE_SPACING;
        float tableTopY = yPosition - EARNINGS_DEDUCTIONS_TABLE_TOP_OFFSET;
        float tableHeight = EARNINGS_DEDUCTIONS_TABLE_HEIGHT;

        return new EarningsDeductionsLayout(columnWidth, leftColumnX, rightColumnX, tableTopY, tableHeight);
    }

    private static void drawEarningsDeductionsTableStructure(Pointer page, EarningsDeductionsLayout layout) {
        // Outer border for both tables
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, layout.leftColumnX, layout.tableTopY - layout.tableHeight,
                                           layout.columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING, layout.tableHeight);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
    }

    private static void drawEarningsDeductionsHeaders(Pointer page, EarningsDeductionsLayout layout, Pointer boldFont) {
        // Section headers with subtle gray backgrounds
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GRAY_HEADER[0], COLOR_LIGHT_GRAY_HEADER[1], COLOR_LIGHT_GRAY_HEADER[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET,
                                           layout.tableTopY + EARNINGS_DEDUCTIONS_SECTION_HEADER_OFFSET,
                                           layout.columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION,
                                           EARNINGS_DEDUCTIONS_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);

        Libharu.INSTANCE.HPDF_Page_Rectangle(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET,
                                           layout.tableTopY + EARNINGS_DEDUCTIONS_SECTION_HEADER_OFFSET,
                                           layout.columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION,
                                           EARNINGS_DEDUCTIONS_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Section header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SECTION_HEADER);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET,
                                         layout.tableTopY + EARNINGS_DEDUCTIONS_HEADER_Y_OFFSET, "EARNINGS");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET,
                                         layout.tableTopY + EARNINGS_DEDUCTIONS_HEADER_Y_OFFSET, "DEDUCTIONS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
    }

    private static void drawEarningsDeductionsTableHeaders(Pointer page, EarningsDeductionsLayout layout, Pointer boldFont) {
        // Table header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET,
                                         layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET,
                                         layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Amount");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET,
                                         layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET,
                                         layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Amount");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Table header border (thicker)
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, layout.leftColumnX, layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, layout.leftColumnX + layout.columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING,
                                        layout.tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);
    }

    private static float drawEarningsTable(Pointer page, Payslip payslip, EarningsDeductionsLayout layout,
                                         Pointer font, Pointer boldFont) {
        float rowY = layout.tableTopY - EARNINGS_DEDUCTIONS_ROW_START_Y_OFFSET;

        // Basic Salary row
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "Basic Salary");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY,
                                         String.format("R %.2f", payslip.getBasicSalary()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        return rowY;
    }

    private static void drawDeductionsTable(Pointer page, Payslip payslip, EarningsDeductionsLayout layout,
                                          Pointer font, Pointer boldFont) {
        float rowY = layout.tableTopY - EARNINGS_DEDUCTIONS_ROW_START_Y_OFFSET;

        // PAYE row
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "PAYE");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY,
                                         String.format("R %.2f", payslip.getPayeeTax()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // UIF row
        rowY -= EMPLOYEE_ROW_HEIGHT;
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "UIF");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY,
                                         String.format("R %.2f", payslip.getUifEmployee()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Medical Aid row
        rowY -= EMPLOYEE_ROW_HEIGHT;
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "Medical Aid");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY,
                                         String.format("R %.2f", payslip.getMedicalAid()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);
    }

    private static float drawEarningsDeductionsTotals(Pointer page, Payslip payslip, EarningsDeductionsLayout layout,
                                                     float rowY, Pointer boldFont) {
        // Total rows with subtle highlight
        rowY -= EARNINGS_DEDUCTIONS_TOTAL_ROW_SPACING;
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_VERY_LIGHT_GRAY[0], COLOR_VERY_LIGHT_GRAY[1], COLOR_VERY_LIGHT_GRAY[2]);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET,
                                           rowY - EARNINGS_DEDUCTIONS_TOTAL_BACKGROUND_Y_OFFSET,
                                           layout.columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION,
                                           EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);

        Libharu.INSTANCE.HPDF_Page_Rectangle(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET,
                                           rowY - EARNINGS_DEDUCTIONS_TOTAL_BACKGROUND_Y_OFFSET,
                                           layout.columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION,
                                           EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Total text and amounts
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_MEDIUM);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        BigDecimal totalEarnings = calculateTotalEarnings(payslip);
        BigDecimal totalDeductions = calculateTotalDeductions(payslip);

        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + EARNINGS_DEDUCTIONS_TOTAL_TEXT_OFFSET, rowY, "Total Earnings");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.leftColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_TOTAL_AMOUNT_OFFSET, rowY,
                                         String.format("R %.2f", totalEarnings));
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + EARNINGS_DEDUCTIONS_TOTAL_TEXT_OFFSET, rowY, "Total Deductions");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, layout.rightColumnX + layout.columnWidth - EARNINGS_DEDUCTIONS_TOTAL_AMOUNT_OFFSET, rowY,
                                         String.format("R %.2f", totalDeductions));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Thick border under total rows
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, layout.leftColumnX, rowY - EARNINGS_DEDUCTIONS_TOTAL_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, layout.leftColumnX + layout.columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING,
                                        rowY - EARNINGS_DEDUCTIONS_TOTAL_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);

        return rowY;
    }

    private static float calculateEarningsDeductionsFinalPosition(float rowY) {
        float yPosition = rowY - EARNINGS_DEDUCTIONS_BOTTOM_SPACING;
        yPosition -= EARNINGS_DEDUCTIONS_SECTION_BOTTOM_SPACING;
        return yPosition;
    }

    private static BigDecimal calculateTotalEarnings(Payslip payslip) {
        BigDecimal totalEarnings = payslip.getTotalEarnings();
        if (totalEarnings == null) {
            totalEarnings = payslip.getBasicSalary() != null ? payslip.getBasicSalary() : BigDecimal.ZERO;
            if (payslip.getHousingAllowance() != null) {
                totalEarnings = totalEarnings.add(payslip.getHousingAllowance());
            }
            if (payslip.getTransportAllowance() != null) {
                totalEarnings = totalEarnings.add(payslip.getTransportAllowance());
            }
            if (payslip.getMedicalAllowance() != null) {
                totalEarnings = totalEarnings.add(payslip.getMedicalAllowance());
            }
            if (payslip.getOtherAllowances() != null) {
                totalEarnings = totalEarnings.add(payslip.getOtherAllowances());
            }
            if (payslip.getCommission() != null) {
                totalEarnings = totalEarnings.add(payslip.getCommission());
            }
            if (payslip.getBonus() != null) {
                totalEarnings = totalEarnings.add(payslip.getBonus());
            }
        }
        return totalEarnings;
    }

    private static BigDecimal calculateTotalDeductions(Payslip payslip) {
        BigDecimal totalDeductions = payslip.getTotalDeductions();
        if (totalDeductions == null) {
            totalDeductions = BigDecimal.ZERO;
            if (payslip.getPayeeTax() != null) {
                totalDeductions = totalDeductions.add(payslip.getPayeeTax());
            }
            if (payslip.getUifEmployee() != null) {
                totalDeductions = totalDeductions.add(payslip.getUifEmployee());
            }
            if (payslip.getMedicalAid() != null) {
                totalDeductions = totalDeductions.add(payslip.getMedicalAid());
            }
            if (payslip.getPensionFund() != null) {
                totalDeductions = totalDeductions.add(payslip.getPensionFund());
            }
            if (payslip.getLoanDeduction() != null) {
                totalDeductions = totalDeductions.add(payslip.getLoanDeduction());
            }
            if (payslip.getOtherDeductions() != null) {
                totalDeductions = totalDeductions.add(payslip.getOtherDeductions());
            }
        }
        return totalDeductions;
    }

    private static float drawNetPaySection(Pointer page, Payslip payslip, float yPosition, float marginLeft, float contentWidth, Pointer boldFont) {
        // Use stored values directly from database instead of triggering calculations
        BigDecimal netPayValue = payslip.getNetPay();
        
        // If stored net pay is null, calculate from stored totals to avoid triggering calculateTotals()
        if (netPayValue == null) {
            BigDecimal totalEarnings = payslip.getTotalEarnings();
            BigDecimal totalDeductions = payslip.getTotalDeductions();
            if (totalEarnings != null && totalDeductions != null) {
                netPayValue = totalEarnings.subtract(totalDeductions);
            } else {
                netPayValue = BigDecimal.ZERO; // Fallback to zero
            }
        }
        
        float netPay = netPayValue.floatValue();
        float sectionHeight = NET_PAY_SECTION_HEIGHT;
        float sectionY = yPosition - sectionHeight;
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GREEN[0], COLOR_LIGHT_GREEN[1], COLOR_LIGHT_GREEN[2]); // Light green background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, sectionY, contentWidth, sectionHeight);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, sectionY, contentWidth, sectionHeight);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_LARGE);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + NET_PAY_LABEL_OFFSET, sectionY + sectionHeight / 2, "NET PAY:");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + contentWidth - NET_PAY_AMOUNT_OFFSET, sectionY + sectionHeight / 2, String.format("R %.2f", netPay));
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        return sectionY - NET_PAY_BOTTOM_SPACING;
    }

    private static void drawFooterSection(Pointer page, float yPosition, float marginLeft, float pageWidth, float marginRight, Pointer font) {
        float footerY = FOOTER_FIXED_POSITION; // Fixed position at bottom of page
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + FOOTER_TEXT_OFFSET, footerY + FOOTER_LINE_SPACING, "This is an official payslip. Please retain for your records.");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_TINY);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + FOOTER_TEXT_OFFSET, footerY - FOOTER_LINE_SPACING, "Page 1 of 1");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String generatedDate = "Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + FOOTER_TEXT_OFFSET, footerY - FOOTER_DATE_OFFSET, generatedDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + FOOTER_TEXT_OFFSET, footerY - FOOTER_CONFIDENTIAL_OFFSET, "Confidential - For employee use only.");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
    }

    private static void validatePayslipData(Payslip payslip) {
        validatePayslipNotNull(payslip);
        ensureGrossSalaryIsSet(payslip);
        calculateTotalEarningsIfNeeded(payslip);
        calculateTotalDeductionsIfNeeded(payslip);
        calculateNetPayIfNeeded(payslip);
    }

    private static void validatePayslipNotNull(Payslip payslip) {
        if (payslip == null) {
            throw new IllegalArgumentException("Payslip cannot be null");
        }
    }

    private static void ensureGrossSalaryIsSet(Payslip payslip) {
        if (payslip.getGrossSalary() == null) {
            System.err.println("WARNING: Gross salary is null for payslip " + payslip.getPayslipNumber() + ", setting to basic salary");
            if (payslip.getBasicSalary() != null) {
                payslip.setGrossSalary(payslip.getBasicSalary());
            } else {
                payslip.setGrossSalary(BigDecimal.ZERO);
            }
        }
    }

    private static void calculateTotalEarningsIfNeeded(Payslip payslip) {
        if (payslip.getTotalEarnings() == null) {
            BigDecimal total = payslip.getGrossSalary()
                .add(payslip.getHousingAllowance() != null ? payslip.getHousingAllowance() : BigDecimal.ZERO)
                .add(payslip.getTransportAllowance() != null ? payslip.getTransportAllowance() : BigDecimal.ZERO)
                .add(payslip.getMedicalAllowance() != null ? payslip.getMedicalAllowance() : BigDecimal.ZERO)
                .add(payslip.getOtherAllowances() != null ? payslip.getOtherAllowances() : BigDecimal.ZERO)
                .add(payslip.getCommission() != null ? payslip.getCommission() : BigDecimal.ZERO)
                .add(payslip.getBonus() != null ? payslip.getBonus() : BigDecimal.ZERO);
            payslip.setTotalEarnings(total);
        }
    }

    private static void calculateTotalDeductionsIfNeeded(Payslip payslip) {
        if (payslip.getTotalDeductions() == null) {
            BigDecimal deductions = BigDecimal.ZERO;
            if (payslip.getPayeeTax() != null) {
                deductions = deductions.add(payslip.getPayeeTax());
            }
            if (payslip.getUifEmployee() != null) {
                deductions = deductions.add(payslip.getUifEmployee());
            }
            if (payslip.getMedicalAid() != null) {
                deductions = deductions.add(payslip.getMedicalAid());
            }
            if (payslip.getPensionFund() != null) {
                deductions = deductions.add(payslip.getPensionFund());
            }
            if (payslip.getLoanDeduction() != null) {
                deductions = deductions.add(payslip.getLoanDeduction());
            }
            if (payslip.getOtherDeductions() != null) {
                deductions = deductions.add(payslip.getOtherDeductions());
            }
            payslip.setTotalDeductions(deductions);
        }
    }

    private static void calculateNetPayIfNeeded(Payslip payslip) {
        if (payslip.getNetPay() == null && payslip.getTotalEarnings() != null && payslip.getTotalDeductions() != null) {
            payslip.setNetPay(payslip.getTotalEarnings().subtract(payslip.getTotalDeductions()));
        }
    }
}
