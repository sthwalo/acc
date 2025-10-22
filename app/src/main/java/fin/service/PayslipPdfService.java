package fin.service;

import com.sun.jna.Pointer;
import fin.model.*;
import fin.repository.CompanyRepository;

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
    private static final float FONT_SIZE_NET_PAY = 14f;
    private static final float FONT_SIZE_BOLD_NORMAL = 11f;

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
    private static final float EARNINGS_DEDUCTIONS_TABLE_HEADER_OFFSET = 15f;
    private static final float EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET = 70f;
    private static final float EARNINGS_DEDUCTIONS_ROW_START_OFFSET = 50f;
    private static final float EARNINGS_DEDUCTIONS_ROW_SPACING = 30f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_SPACING = 35f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_HEIGHT = 20f;
    private static final float EARNINGS_DEDUCTIONS_BOTTOM_SPACING = 70f;

    // Net pay section constants
    private static final float NET_PAY_SECTION_HEIGHT = 40f;
    private static final float NET_PAY_TEXT_OFFSET = 20f;
    private static final float NET_PAY_LABEL_OFFSET = 20f;
    private static final float NET_PAY_AMOUNT_OFFSET = 120f;
    private static final float NET_PAY_BOTTOM_SPACING = 20f;

    // Footer constants
    private static final float FOOTER_Y_POSITION = 100f;
    private static final float FOOTER_LINE_SPACING = 15f;

    // Additional constants for earnings/deductions section
    private static final float EARNINGS_DEDUCTIONS_TOTAL_ROW_SPACING = 35f;
    private static final float EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT = 20f;
    private static final float EARNINGS_DEDUCTIONS_SECTION_BOTTOM_SPACING = 70f;

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
    private static final float[] COLOR_LIGHT_GREEN_BACKGROUND = {0.95f, 1.0f, 0.95f};
    private static final float[] COLOR_VERY_LIGHT_GRAY = {0.95f, 0.95f, 0.95f};

    // Line width constants
    private static final float LINE_WIDTH_THICK = 2.0f;
    private static final float LINE_WIDTH_NORMAL = 1.0f;
    private static final float LINE_WIDTH_MEDIUM = 1.5f;

    // Text truncation constants
    private static final int MAX_NAME_LENGTH = 25;
    private static final int NAME_TRUNCATE_LENGTH = 22;

    private final CompanyRepository companyRepository;

    public PayslipPdfService(CompanyRepository initialCompanyRepository) {
        this.companyRepository = initialCompanyRepository;
    }

    /**
     * Generates a PDF payslip for an employee using libharu
     */
    public String generatePayslipPdf(Payslip payslip, Employee employee, Company company, PayrollPeriod payrollPeriod) {
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
                payrollPeriod.getPayDate().format(DateTimeFormatter.ofPattern("yyyyMM")),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))));

        String filename = filePath.toString();

        try {
            generatePayslipPDF(payslip, employee, company, payrollPeriod, filename);
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate payslip PDF: " + e.getMessage(), e);
        }
    }

    private static void generatePayslipPDF(Payslip payslip, Employee employee, Company company, PayrollPeriod payrollPeriod, String filename) {
        // Create PDF document
        Pointer pdf = Libharu.INSTANCE.HPDF_New(null, null);
        if (pdf == null) {
            throw new RuntimeException("Failed to create PDF document");
        }

        try {
            // Add a page
            Pointer page = Libharu.INSTANCE.HPDF_AddPage(pdf);
            if (page == null) {
                throw new RuntimeException("Failed to add page");
            }

            // Set page size to A4
            Libharu.INSTANCE.HPDF_Page_SetSize(page, Libharu.HPDF_PAGE_SIZE_A4, Libharu.HPDF_PAGE_PORTRAIT);

            // Get page dimensions
            float pageWidth = PAGE_WIDTH_A4; // A4 width in points
            float pageHeight = PAGE_HEIGHT_A4; // A4 height in points
            float marginLeft = MARGIN_LEFT;
            float marginRight = MARGIN_RIGHT;
            float marginTop = MARGIN_TOP;
            float contentWidth = pageWidth - marginLeft - marginRight;

            // Load fonts
            Pointer font = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica", null);
            Pointer boldFont = Libharu.INSTANCE.HPDF_GetFont(pdf, "Helvetica-Bold", null);

            // Try to load company logo
            Pointer logo = null;
            if (company.getLogoPath() != null && !company.getLogoPath().isEmpty()) {
                try {
                    logo = Libharu.INSTANCE.HPDF_LoadPngImageFromFile(pdf, company.getLogoPath());
                    if (logo != null) {
                        System.out.println("✅ Company logo loaded successfully");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to load company logo: " + e.getMessage());
                }
            }

            // Start drawing content
            float yPosition = pageHeight - marginTop;

            // Draw header section with enhanced styling
            yPosition = drawHeaderSection(page, company, logo, yPosition, pageWidth, marginLeft, contentWidth, boldFont);

            // Draw employee details section with borders
            yPosition = drawEmployeeDetailsSection(page, employee, payslip, payrollPeriod, yPosition, marginLeft, contentWidth, font, boldFont);

            // Draw earnings and deductions section with tables
            yPosition = drawEarningsDeductionsSection(page, payslip, yPosition, marginLeft, contentWidth, font, boldFont);

            // Draw net pay section with enhanced styling
            yPosition = drawNetPaySection(page, payslip, yPosition, marginLeft, contentWidth, boldFont);

            // Draw footer with enhanced styling
            drawFooterSection(page, yPosition, marginLeft, pageWidth, marginRight, font);

            // Save the PDF
            int result = Libharu.INSTANCE.HPDF_SaveToFile(pdf, filename);
            if (result != 0) {
                throw new RuntimeException("Failed to save PDF, error code: " + result);
            }

        } finally {
            // Free resources
            Libharu.INSTANCE.HPDF_Free(pdf);
        }
    }

    private static float drawHeaderSection(Pointer page, Company company, Pointer logo, float yPosition,
                                         float pageWidth, float marginLeft, float contentWidth, Pointer boldFont) {
        // Draw decorative header border
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, COLOR_BLUE_BORDER[0], COLOR_BLUE_BORDER[1], COLOR_BLUE_BORDER[2]); // Blue border
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - HEADER_BORDER_HEIGHT, contentWidth, HEADER_BORDER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Reset stroke color to black
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_NORMAL);

        // Draw company logo if available - with proper scaling to avoid stretching
        if (logo != null) {
            // Use a reasonable size that maintains aspect ratio
            // Logo will be scaled to fit within 60x60 box while maintaining aspect ratio
            float maxLogoSize = LOGO_MAX_SIZE;
            float logoWidth = maxLogoSize;
            float logoHeight = maxLogoSize * LOGO_ASPECT_RATIO; // Assume 4:3 aspect ratio, adjust if needed
            float logoX = (pageWidth - logoWidth) / 2; // Center the logo
            float logoY = yPosition - logoHeight - LOGO_BOTTOM_SPACING;

            Libharu.INSTANCE.HPDF_Page_DrawImage(page, logo, logoX, logoY, logoWidth, logoHeight);
            yPosition -= (logoHeight + LOGO_BOTTOM_MARGIN); // More space after logo
        } else {
            yPosition -= LOGO_PLACEHOLDER_SPACING; // Space for where logo would be
        }

        // Draw PAYSLIP title with background - ensure no overlap with logo
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_BLUE_BACKGROUND[0], COLOR_LIGHT_BLUE_BACKGROUND[1], COLOR_LIGHT_BLUE_BACKGROUND[2]); // Light blue background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft + TITLE_BACKGROUND_PADDING, yPosition - TITLE_BACKGROUND_HEIGHT, contentWidth - TITLE_BACKGROUND_PADDING * 2, TITLE_BACKGROUND_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]); // Reset to black

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_TITLE);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String title = "PAYSLIP";
        float titleWidth = TITLE_APPROXIMATE_WIDTH; // Approximate width for centering
        float titleX = (pageWidth - titleWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(page, titleX, yPosition - TITLE_VERTICAL_OFFSET, title);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= COMPANY_NAME_VERTICAL_OFFSET; // More space after title

        // Draw company name with underline - ensure proper spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_COMPANY_NAME);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String companyName = company.getName();
        float companyNameWidth = companyName.length() * COMPANY_NAME_CHAR_WIDTH; // Approximate width
        float companyX = (pageWidth - companyNameWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(page, companyX, yPosition, companyName);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Draw underline under company name
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, companyX, yPosition - UNDERLINE_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, companyX + companyNameWidth, yPosition - UNDERLINE_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        yPosition -= HEADER_BOTTOM_SPACING; // More space after header

        return yPosition;
    }

    private static float drawEmployeeDetailsSection(Pointer page, Employee employee, Payslip payslip, PayrollPeriod payrollPeriod,
                                                  float yPosition, float marginLeft, float contentWidth,
                                                  Pointer font, Pointer boldFont) {
        // Draw section background and border
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GRAY_BACKGROUND[0], COLOR_LIGHT_GRAY_BACKGROUND[1], COLOR_LIGHT_GRAY_BACKGROUND[2]); // Light gray background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - EMPLOYEE_SECTION_HEIGHT, contentWidth, EMPLOYEE_SECTION_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]); // Reset to black

        // Draw border
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_NORMAL);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - EMPLOYEE_SECTION_HEIGHT, contentWidth, EMPLOYEE_SECTION_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Section header with background
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_BLUE_HEADER[0], COLOR_LIGHT_BLUE_HEADER[1], COLOR_LIGHT_BLUE_HEADER[2]); // Light blue header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft + EMPLOYEE_RECTANGLE_INSET, yPosition - EMPLOYEE_SECTION_HEADER_HEIGHT, contentWidth - EMPLOYEE_RECTANGLE_WIDTH_REDUCTION, EMPLOYEE_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SECTION_HEADER);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + EMPLOYEE_HEADER_TEXT_OFFSET, yPosition - EMPLOYEE_SECTION_HEADER_OFFSET, "EMPLOYEE DETAILS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= EMPLOYEE_CONTENT_START_OFFSET;

        // Two-column layout with better alignment and spacing
        float col1X = marginLeft + EMPLOYEE_COLUMN_SPACING;
        float col2X = marginLeft + contentWidth / 2 + EMPLOYEE_COLUMN_SPACING; // Better spacing
        float labelWidth = EMPLOYEE_LABEL_WIDTH; // Fixed width for labels
        float rowHeight = EMPLOYEE_ROW_HEIGHT; // Increased row height to prevent overlap

        // Row 1: Employee Code and Pay Date
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Employee Code:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, employee.getEmployeeNumber());
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Pay Date:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String payDate = payrollPeriod.getPayDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, payDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= rowHeight;

        // Row 2: Employee Name and Pay Period
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Employee Name:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String fullName = employee.getFullName();
        // Truncate long names if necessary to prevent overlap
        if (fullName.length() > MAX_NAME_LENGTH) {
            fullName = fullName.substring(0, NAME_TRUNCATE_LENGTH) + "...";
        }
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, fullName);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Pay Period:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, payrollPeriod.getPeriodName());
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= rowHeight;

        // Row 3: Tax Number and Employment Date
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Tax Number:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String taxNumber = employee.getTaxNumber() != null ? employee.getTaxNumber() : "Not Provided";
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, taxNumber);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Employment Date:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String hireDate = employee.getHireDate() != null ?
            employee.getHireDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Not Provided";
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, hireDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= EMPLOYEE_SECTION_BOTTOM_SPACING; // Increased space after employee details for better separation

        return yPosition;
    }

    private static float drawEarningsDeductionsSection(Pointer page, Payslip payslip, float yPosition,
                                                     float marginLeft, float contentWidth, Pointer font, Pointer boldFont) {
        float columnWidth = (contentWidth - EARNINGS_DEDUCTIONS_TABLE_SPACING) / 2;
        float leftColumnX = marginLeft;
        float rightColumnX = marginLeft + columnWidth + EARNINGS_DEDUCTIONS_TABLE_SPACING;
        float tableTopY = yPosition - EARNINGS_DEDUCTIONS_TABLE_TOP_OFFSET; // Top Y for both tables
        float tableHeight = EARNINGS_DEDUCTIONS_TABLE_HEIGHT; // Increased height for better padding

        // Outer border for both tables
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX, tableTopY - tableHeight, columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING, tableHeight);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Remove colorful section backgrounds - keep it clean and white
        // Section backgrounds removed for less color

        // Declare rowY for table rows with more spacing
        float rowY = tableTopY - EARNINGS_DEDUCTIONS_ROW_START_Y_OFFSET; // Increased spacing between headers and content
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);

        // Section headers with subtle gray backgrounds instead of colors
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GRAY_HEADER[0], COLOR_LIGHT_GRAY_HEADER[1], COLOR_LIGHT_GRAY_HEADER[2]); // Light gray header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET, tableTopY + EARNINGS_DEDUCTIONS_SECTION_HEADER_OFFSET, columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION, EARNINGS_DEDUCTIONS_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_LIGHT_GRAY_HEADER[0], COLOR_LIGHT_GRAY_HEADER[1], COLOR_LIGHT_GRAY_HEADER[2]); // Light gray header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, rightColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET, tableTopY + EARNINGS_DEDUCTIONS_SECTION_HEADER_OFFSET, columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION, EARNINGS_DEDUCTIONS_SECTION_HEADER_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Section header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SECTION_HEADER);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, tableTopY + EARNINGS_DEDUCTIONS_HEADER_Y_OFFSET, "EARNINGS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, tableTopY + EARNINGS_DEDUCTIONS_HEADER_Y_OFFSET, "DEDUCTIONS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Table header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_NORMAL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Amount");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_Y_OFFSET, "Amount");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Table header border (thicker)
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, leftColumnX, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, leftColumnX + columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING, tableTopY - EARNINGS_DEDUCTIONS_TABLE_HEADER_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);

        // Table rows with increased spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "Basic Salary");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY, String.format("R %.2f", payslip.getBasicSalary()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Deductions rows with increased spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "PAYE");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY, String.format("R %.2f", payslip.getPayeeTax()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        rowY -= EMPLOYEE_ROW_HEIGHT; // Increased spacing between rows
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "UIF");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY, String.format("R %.2f", payslip.getUifEmployee()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        rowY -= EMPLOYEE_ROW_HEIGHT; // Increased spacing between rows
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_HEADER_TEXT_OFFSET, rowY, "Medical Aid");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_SMALL);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - EARNINGS_DEDUCTIONS_AMOUNT_COLUMN_OFFSET, rowY, String.format("R %.2f", payslip.getMedicalAid()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Total rows with subtle highlight instead of strong colors
        rowY -= EARNINGS_DEDUCTIONS_TOTAL_ROW_SPACING; // More space before totals
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_VERY_LIGHT_GRAY[0], COLOR_VERY_LIGHT_GRAY[1], COLOR_VERY_LIGHT_GRAY[2]); // Very light gray for total earnings
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET, rowY - EARNINGS_DEDUCTIONS_TOTAL_BACKGROUND_Y_OFFSET, columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION, EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_VERY_LIGHT_GRAY[0], COLOR_VERY_LIGHT_GRAY[1], COLOR_VERY_LIGHT_GRAY[2]); // Very light gray for total deductions
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, rightColumnX + EARNINGS_DEDUCTIONS_RECTANGLE_INSET, rowY - EARNINGS_DEDUCTIONS_TOTAL_BACKGROUND_Y_OFFSET, columnWidth - EARNINGS_DEDUCTIONS_RECTANGLE_WIDTH_REDUCTION, EARNINGS_DEDUCTIONS_TOTAL_ROW_HEIGHT);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, FONT_SIZE_MEDIUM);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + EARNINGS_DEDUCTIONS_TOTAL_TEXT_OFFSET, rowY, "Total Earnings");
        // For total earnings
        BigDecimal totalEarnings = payslip.getTotalEarnings();
        if (totalEarnings == null) {
            // Calculate from components if null
            totalEarnings = payslip.getBasicSalary() != null ? payslip.getBasicSalary() : BigDecimal.ZERO;
            // Add allowances if they exist
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
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - EARNINGS_DEDUCTIONS_TOTAL_AMOUNT_OFFSET, rowY, String.format("R %.2f", totalEarnings));
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + EARNINGS_DEDUCTIONS_TOTAL_TEXT_OFFSET, rowY, "Total Deductions");
        // For total deductions  
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
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - EARNINGS_DEDUCTIONS_TOTAL_AMOUNT_OFFSET, rowY, String.format("R %.2f", totalDeductions));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Thick border under total rows
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_THICK);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, leftColumnX, rowY - EARNINGS_DEDUCTIONS_TOTAL_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, leftColumnX + columnWidth * 2 + EARNINGS_DEDUCTIONS_TABLE_OUTER_BORDER_SPACING, rowY - EARNINGS_DEDUCTIONS_TOTAL_BORDER_Y_OFFSET);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, LINE_WIDTH_MEDIUM);

        // Use the lower Y position
        yPosition = rowY - EARNINGS_DEDUCTIONS_BOTTOM_SPACING; // More space after totals
        yPosition -= EARNINGS_DEDUCTIONS_SECTION_BOTTOM_SPACING; // More space after earnings/deductions

        return yPosition;
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
        if (payslip == null) {
            throw new IllegalArgumentException("Payslip cannot be null");
        }

        // Ensure grossSalary is never null - do this FIRST before any calculations
        if (payslip.getGrossSalary() == null) {
            System.err.println("WARNING: Gross salary is null for payslip " + payslip.getPayslipNumber() + ", setting to basic salary");
            if (payslip.getBasicSalary() != null) {
                payslip.setGrossSalary(payslip.getBasicSalary());
            } else {
                payslip.setGrossSalary(BigDecimal.ZERO);
            }
        }

        // Ensure totalEarnings is calculated
        if (payslip.getTotalEarnings() == null) {
            BigDecimal total = payslip.getGrossSalary()  // Now safe to use since we checked above
                .add(payslip.getHousingAllowance() != null ? payslip.getHousingAllowance() : BigDecimal.ZERO)
                .add(payslip.getTransportAllowance() != null ? payslip.getTransportAllowance() : BigDecimal.ZERO)
                .add(payslip.getMedicalAllowance() != null ? payslip.getMedicalAllowance() : BigDecimal.ZERO)
                .add(payslip.getOtherAllowances() != null ? payslip.getOtherAllowances() : BigDecimal.ZERO)
                .add(payslip.getCommission() != null ? payslip.getCommission() : BigDecimal.ZERO)
                .add(payslip.getBonus() != null ? payslip.getBonus() : BigDecimal.ZERO);
            payslip.setTotalEarnings(total);
        }

        // Ensure totalDeductions is calculated
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

        // Ensure netPay is calculated
        if (payslip.getNetPay() == null && payslip.getTotalEarnings() != null && payslip.getTotalDeductions() != null) {
            payslip.setNetPay(payslip.getTotalEarnings().subtract(payslip.getTotalDeductions()));
        }
    }
}