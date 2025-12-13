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
 */

package fin.service.reporting;

import fin.entity.Payslip;
import fin.service.PayrollService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
//import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring service for generating PDF payroll reports using PDFBox.
 * Handles payroll summary, employee reports, and EMP 201 tax submissions.
 */
@Service
public class PayrollReportService {

    private static final Logger LOGGER = Logger.getLogger(PayrollReportService.class.getName());

    /**
     * Generate PDF payroll summary report
     */
    public byte[] generatePayrollSummaryPdf(PayrollService.PayrollSummary summary) {
        if (summary == null) {
            throw new IllegalArgumentException("Payroll summary data is required and cannot be null");
        }

        LOGGER.info("Generating PDF payroll summary report");

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Load fonts
                PDType0Font unicodeFont = loadSystemTTFFont(document);
                org.apache.pdfbox.pdmodel.font.PDFont boldFont = unicodeFont != null ?
                    unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                org.apache.pdfbox.pdmodel.font.PDFont normalFont = unicodeFont != null ?
                    unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                // Title
                contentStream.setFont(boldFont, 18);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("PAYROLL SUMMARY REPORT");
                contentStream.endText();

                // Summary data
                contentStream.setFont(normalFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText(sanitizeForFont("Total Gross Pay: R " + summary.getTotalGross(), unicodeFont, normalFont));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(sanitizeForFont("Total PAYE: R " + summary.getTotalPAYE(), unicodeFont, normalFont));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(sanitizeForFont("Total UIF: R " + summary.getTotalUIF(), unicodeFont, normalFont));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(sanitizeForFont("Total SDL: R " + summary.getTotalSDL(), unicodeFont, normalFont));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(sanitizeForFont("Total Net Pay: R " + summary.getTotalNet(), unicodeFont, normalFont));
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText(sanitizeForFont("Employee Count: " + summary.getEmployeeCount(), unicodeFont, normalFont));
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();

            LOGGER.info("Payroll summary PDF generated, size: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate payroll summary PDF: " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate payroll summary PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF employee payroll report
     */
    public byte[] generateEmployeePayrollPdf(List<Payslip> payslips) {
        if (payslips == null || payslips.isEmpty()) {
            throw new IllegalArgumentException("Payslip data is required and cannot be empty");
        }

        LOGGER.info("Generating PDF employee payroll report for " + payslips.size() + " payslips");

        try (PDDocument document = new PDDocument()) {
            // Load fonts once
            PDType0Font unicodeFont = loadSystemTTFFont(document);
            org.apache.pdfbox.pdmodel.font.PDFont boldFont = unicodeFont != null ?
                unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            org.apache.pdfbox.pdmodel.font.PDFont normalFont = unicodeFont != null ?
                unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage currentPage = new PDPage();
            document.addPage(currentPage);
            int currentY = 750;

            // Title on first page
            try (PDPageContentStream titleStream = new PDPageContentStream(document, currentPage)) {
                titleStream.setFont(boldFont, 18);
                titleStream.beginText();
                titleStream.newLineAtOffset(50, currentY);
                titleStream.showText("EMPLOYEE PAYROLL REPORT");
                titleStream.endText();
                currentY = 700;
            }

            // Process each payslip
            for (int i = 0; i < payslips.size(); i++) {
                Payslip payslip = payslips.get(i);

                // Check if we need a new page
                if (currentY < 100) {
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    currentY = 750;
                }

                // Write payslip data
                try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.setFont(normalFont, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, currentY);
                    contentStream.showText(sanitizeForFont("Payslip: " + payslip.getPayslipNumber() +
                        " | Basic: R" + payslip.getBasicSalary() +
                        " | PAYE: R" + (payslip.getPayeeTax() != null ? payslip.getPayeeTax() : "0.00") +
                        " | Net: R" + payslip.getNetPay(), unicodeFont, normalFont));
                    contentStream.endText();
                }

                currentY -= 15;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();

            LOGGER.info("Employee payroll PDF generated, size: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate employee payroll PDF: " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate employee payroll PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PDF EMP 201 SARS tax submission report
     */
    public byte[] generateEmp201Pdf(String emp201Text) {
        if (emp201Text == null || emp201Text.trim().isEmpty()) {
            throw new IllegalArgumentException("EMP 201 report text is required and cannot be empty");
        }

        LOGGER.info("Generating PDF EMP 201 SARS tax submission report");

        try (PDDocument document = new PDDocument()) {
            // Load fonts once
            PDType0Font unicodeFont = loadSystemTTFFont(document);
            org.apache.pdfbox.pdmodel.font.PDFont boldFont = unicodeFont != null ?
                unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            org.apache.pdfbox.pdmodel.font.PDFont normalFont = unicodeFont != null ?
                unicodeFont : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage currentPage = new PDPage();
            document.addPage(currentPage);
            int currentY = 750;

            // Title on first page
            try (PDPageContentStream titleStream = new PDPageContentStream(document, currentPage)) {
                titleStream.setFont(boldFont, 18);
                titleStream.beginText();
                titleStream.newLineAtOffset(50, currentY);
                titleStream.showText("EMP 201 SARS TAX SUBMISSION REPORT");
                titleStream.endText();
                currentY = 700;
            }

            // Process each line
            String[] lines = emp201Text.split("\n");
            for (String line : lines) {
                // Check if we need a new page
                if (currentY < 50) {
                    currentPage = new PDPage();
                    document.addPage(currentPage);
                    currentY = 750;
                }

                // Write line
                try (PDPageContentStream contentStream = new PDPageContentStream(document, currentPage, PDPageContentStream.AppendMode.APPEND, true, true)) {
                    contentStream.setFont(normalFont, 10);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(50, currentY);
                    contentStream.showText(sanitizeForFont(line, unicodeFont, normalFont));
                    contentStream.endText();
                }

                currentY -= 12;
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();

            LOGGER.info("EMP 201 PDF generated, size: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate EMP 201 PDF: " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate EMP 201 PDF: " + e.getMessage(), e);
        }
    }

    private PDType0Font loadSystemTTFFont(PDDocument document) {
        // First try to load an embedded font from the application resources (guaranteed fallback)
        try (java.io.InputStream is = PayrollReportService.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (is != null) {
                return PDType0Font.load(document, is);
            }
        } catch (Exception ignored) {
            // continue to try system fonts
        }

        String[] candidatePaths = new String[] {
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/usr/share/fonts/truetype/freefont/FreeSans.ttf",
            "/usr/share/fonts/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/ubuntu/Ubuntu-R.ttf"
        };

        for (String path : candidatePaths) {
            try {
                java.io.File f = new java.io.File(path);
                if (f.exists()) {
                    return PDType0Font.load(document, f);
                }
            } catch (Exception ignored) {
                // continue to next path
            }
        }

        // No system TTF found - return null to indicate fallback
        return null;
    }

    private String sanitizeForFont(String text, PDType0Font unicodeFont, org.apache.pdfbox.pdmodel.font.PDFont fallbackFont) {
        if (text == null) return "";
        if (unicodeFont != null) {
            return text; // unicode font should support text
        }

        // Fallback - replace non-encodable chars with '?'
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            try {
                fallbackFont.encode(ch);
                sb.append(ch);
            } catch (IllegalArgumentException | java.io.IOException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}