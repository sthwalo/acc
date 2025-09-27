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

    private final CompanyRepository companyRepository;

    public PayslipPdfService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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
            float pageWidth = 595.28f; // A4 width in points
            float pageHeight = 841.89f; // A4 height in points
            float marginLeft = 50;
            float marginRight = 50;
            float marginTop = 50;
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
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 2.0f);
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, 0.2f, 0.4f, 0.8f); // Blue border
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - 140, contentWidth, 140);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Reset stroke color to black
        Libharu.INSTANCE.HPDF_Page_SetRGBStroke(page, 0, 0, 0);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.0f);

        // Draw company logo if available - with proper scaling to avoid stretching
        if (logo != null) {
            // Use a reasonable size that maintains aspect ratio
            // Logo will be scaled to fit within 60x60 box while maintaining aspect ratio
            float maxLogoSize = 60;
            float logoWidth = maxLogoSize;
            float logoHeight = maxLogoSize * 0.75f; // Assume 4:3 aspect ratio, adjust if needed
            float logoX = (pageWidth - logoWidth) / 2; // Center the logo
            float logoY = yPosition - logoHeight - 20;

            Libharu.INSTANCE.HPDF_Page_DrawImage(page, logo, logoX, logoY, logoWidth, logoHeight);
            yPosition -= (logoHeight + 40); // More space after logo
        } else {
            yPosition -= 20; // Space for where logo would be
        }

        // Draw PAYSLIP title with background - ensure no overlap with logo
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.9f, 0.95f, 1.0f); // Light blue background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft + 5, yPosition - 35, contentWidth - 10, 30);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0); // Reset to black

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 24);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String title = "PAYSLIP";
        float titleWidth = 100; // Approximate width for centering
        float titleX = (pageWidth - titleWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(page, titleX, yPosition - 25, title);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= 50; // More space after title

        // Draw company name with underline - ensure proper spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 16);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String companyName = company.getName();
        float companyNameWidth = companyName.length() * 8; // Approximate width
        float companyX = (pageWidth - companyNameWidth) / 2;
        Libharu.INSTANCE.HPDF_Page_TextOut(page, companyX, yPosition, companyName);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Draw underline under company name
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.5f);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, companyX, yPosition - 3);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, companyX + companyNameWidth, yPosition - 3);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        yPosition -= 80; // More space after header

        return yPosition;
    }

    private static float drawEmployeeDetailsSection(Pointer page, Employee employee, Payslip payslip, PayrollPeriod payrollPeriod,
                                                  float yPosition, float marginLeft, float contentWidth,
                                                  Pointer font, Pointer boldFont) {
        // Draw section background and border
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.95f, 0.95f, 0.95f); // Light gray background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - 120, contentWidth, 120);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0); // Reset to black

        // Draw border
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.0f);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, yPosition - 120, contentWidth, 120);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Section header with background
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.8f, 0.8f, 0.9f); // Light blue header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft + 2, yPosition - 20, contentWidth - 4, 18);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 12);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 10, yPosition - 15, "EMPLOYEE DETAILS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= 30;

        // Two-column layout with better alignment and spacing
        float col1X = marginLeft + 15;
        float col2X = marginLeft + contentWidth / 2 + 15; // Better spacing
        float labelWidth = 85; // Fixed width for labels
        float rowHeight = 30; // Increased row height to prevent overlap

        // Row 1: Employee Code and Pay Date
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Employee Code:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, employee.getEmployeeNumber());
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Pay Date:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String payDate = payrollPeriod.getPayDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, payDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= rowHeight;

        // Row 2: Employee Name and Pay Period
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Employee Name:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String fullName = employee.getFullName();
        // Truncate long names if necessary to prevent overlap
        if (fullName.length() > 25) {
            fullName = fullName.substring(0, 22) + "...";
        }
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, fullName);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Pay Period:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, payrollPeriod.getPeriodName());
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= rowHeight;

        // Row 3: Tax Number and Employment Date
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X, yPosition, "Tax Number:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String taxNumber = employee.getTaxNumber() != null ? employee.getTaxNumber() : "Not Provided";
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col1X + labelWidth, yPosition, taxNumber);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X, yPosition, "Employment Date:");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String hireDate = employee.getHireDate() != null ?
            employee.getHireDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "Not Provided";
        Libharu.INSTANCE.HPDF_Page_TextOut(page, col2X + labelWidth, yPosition, hireDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        yPosition -= 80; // Increased space after employee details for better separation

        return yPosition;
    }

    private static float drawEarningsDeductionsSection(Pointer page, Payslip payslip, float yPosition,
                                                     float marginLeft, float contentWidth, Pointer font, Pointer boldFont) {
        float columnWidth = (contentWidth - 20) / 2;
        float leftColumnX = marginLeft;
        float rightColumnX = marginLeft + columnWidth + 20;
        float tableTopY = yPosition - 15; // Top Y for both tables
        float tableHeight = 150; // Increased height for better padding

        // Outer border for both tables
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX, tableTopY - tableHeight, columnWidth * 2 + 20, tableHeight);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);

        // Remove colorful section backgrounds - keep it clean and white
        // Section backgrounds removed for less color

        // Declare rowY for table rows with more spacing
        float rowY = tableTopY - 50; // Increased spacing between headers and content
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);

        // Section headers with subtle gray backgrounds instead of colors
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.9f, 0.9f, 0.9f); // Light gray header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX + 2, tableTopY + 12, columnWidth - 4, 15);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.9f, 0.9f, 0.9f); // Light gray header
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, rightColumnX + 2, tableTopY + 12, columnWidth - 4, 15);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);

        // Section header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 12);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + 10, tableTopY + 15, "EARNINGS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, tableTopY + 15, "DEDUCTIONS");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Table header text
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + 10, tableTopY - 15, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - 70, tableTopY - 15, "Amount");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, tableTopY - 15, "Description");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - 70, tableTopY - 15, "Amount");
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Table header border (thicker)
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 2.0f);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, leftColumnX, tableTopY - 2);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, leftColumnX + columnWidth * 2 + 20, tableTopY - 2);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.0f);

        // Table rows with increased spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + 10, rowY, "Basic Salary");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - 70, rowY, String.format("R %.2f", payslip.getBasicSalary()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Deductions rows with increased spacing
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, rowY, "PAYE");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - 70, rowY, String.format("R %.2f", payslip.getPayeeTax()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        rowY -= 30; // Increased spacing between rows
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, rowY, "UIF");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - 70, rowY, String.format("R %.2f", payslip.getUifEmployee()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        rowY -= 30; // Increased spacing between rows
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, rowY, "Medical Aid");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - 70, rowY, String.format("R %.2f", payslip.getMedicalAid()));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Total rows with subtle highlight instead of strong colors
        rowY -= 35; // More space before totals
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.95f, 0.95f, 0.95f); // Very light gray for total earnings
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, leftColumnX + 2, rowY - 5, columnWidth - 4, 20);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.95f, 0.95f, 0.95f); // Very light gray for total deductions
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, rightColumnX + 2, rowY - 5, columnWidth - 4, 20);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);

        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 11);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + 10, rowY, "Total Earnings");
        // For total earnings
        BigDecimal totalEarnings = payslip.getTotalEarnings();
        if (totalEarnings == null) {
            // Calculate from components if null
            totalEarnings = payslip.getBasicSalary() != null ? payslip.getBasicSalary() : BigDecimal.ZERO;
            // Add allowances if they exist
            if (payslip.getHousingAllowance() != null) totalEarnings = totalEarnings.add(payslip.getHousingAllowance());
            if (payslip.getTransportAllowance() != null) totalEarnings = totalEarnings.add(payslip.getTransportAllowance());
            if (payslip.getMedicalAllowance() != null) totalEarnings = totalEarnings.add(payslip.getMedicalAllowance());
            if (payslip.getOtherAllowances() != null) totalEarnings = totalEarnings.add(payslip.getOtherAllowances());
            if (payslip.getCommission() != null) totalEarnings = totalEarnings.add(payslip.getCommission());
            if (payslip.getBonus() != null) totalEarnings = totalEarnings.add(payslip.getBonus());
        }
        Libharu.INSTANCE.HPDF_Page_TextOut(page, leftColumnX + columnWidth - 70, rowY, String.format("R %.2f", totalEarnings));
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + 10, rowY, "Total Deductions");
        // For total deductions  
        BigDecimal totalDeductions = payslip.getTotalDeductions();
        if (totalDeductions == null) {
            totalDeductions = BigDecimal.ZERO;
            if (payslip.getPayeeTax() != null) totalDeductions = totalDeductions.add(payslip.getPayeeTax());
            if (payslip.getUifEmployee() != null) totalDeductions = totalDeductions.add(payslip.getUifEmployee());
            if (payslip.getMedicalAid() != null) totalDeductions = totalDeductions.add(payslip.getMedicalAid());
            if (payslip.getPensionFund() != null) totalDeductions = totalDeductions.add(payslip.getPensionFund());
            if (payslip.getLoanDeduction() != null) totalDeductions = totalDeductions.add(payslip.getLoanDeduction());
            if (payslip.getOtherDeductions() != null) totalDeductions = totalDeductions.add(payslip.getOtherDeductions());
        }
        Libharu.INSTANCE.HPDF_Page_TextOut(page, rightColumnX + columnWidth - 70, rowY, String.format("R %.2f", totalDeductions));
        Libharu.INSTANCE.HPDF_Page_EndText(page);

        // Thick border under total rows
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 2.0f);
        Libharu.INSTANCE.HPDF_Page_MoveTo(page, leftColumnX, rowY - 5);
        Libharu.INSTANCE.HPDF_Page_LineTo(page, leftColumnX + columnWidth * 2 + 20, rowY - 5);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.0f);

        // Use the lower Y position
        yPosition = rowY - 15; // More space after totals
        yPosition -= 70; // More space after earnings/deductions

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
        float sectionHeight = 40;
        float sectionY = yPosition - sectionHeight;
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0.95f, 1.0f, 0.95f); // Light green background
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, sectionY, contentWidth, sectionHeight);
        Libharu.INSTANCE.HPDF_Page_Fill(page);
        Libharu.INSTANCE.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        Libharu.INSTANCE.HPDF_Page_SetLineWidth(page, 1.5f);
        Libharu.INSTANCE.HPDF_Page_Rectangle(page, marginLeft, sectionY, contentWidth, sectionHeight);
        Libharu.INSTANCE.HPDF_Page_Stroke(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, boldFont, 14);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 20, sectionY + sectionHeight / 2, "NET PAY:");
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + contentWidth - 120, sectionY + sectionHeight / 2, String.format("R %.2f", netPay));
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        return sectionY - 20;
    }

    private static void drawFooterSection(Pointer page, float yPosition, float marginLeft, float pageWidth, float marginRight, Pointer font) {
        float footerY = 100; // Fixed position at bottom of page
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 10);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 15, footerY + 5, "This is an official payslip. Please retain for your records.");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_SetFontAndSize(page, font, 9);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 15, footerY - 10, "Page 1 of 1");
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        String generatedDate = "Generated on: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 15, footerY - 25, generatedDate);
        Libharu.INSTANCE.HPDF_Page_EndText(page);
        Libharu.INSTANCE.HPDF_Page_BeginText(page);
        Libharu.INSTANCE.HPDF_Page_TextOut(page, marginLeft + 15, footerY - 40, "Confidential - For employee use only.");
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
            if (payslip.getPayeeTax() != null) deductions = deductions.add(payslip.getPayeeTax());
            if (payslip.getUifEmployee() != null) deductions = deductions.add(payslip.getUifEmployee());
            if (payslip.getMedicalAid() != null) deductions = deductions.add(payslip.getMedicalAid());
            if (payslip.getPensionFund() != null) deductions = deductions.add(payslip.getPensionFund());
            if (payslip.getLoanDeduction() != null) deductions = deductions.add(payslip.getLoanDeduction());
            if (payslip.getOtherDeductions() != null) deductions = deductions.add(payslip.getOtherDeductions());
            payslip.setTotalDeductions(deductions);
        }

        // Ensure netPay is calculated
        if (payslip.getNetPay() == null && payslip.getTotalEarnings() != null && payslip.getTotalDeductions() != null) {
            payslip.setNetPay(payslip.getTotalEarnings().subtract(payslip.getTotalDeductions()));
        }
    }
}