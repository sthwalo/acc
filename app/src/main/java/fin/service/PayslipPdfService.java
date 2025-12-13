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

package fin.service;

import fin.entity.Company;
import fin.entity.Employee;
import fin.entity.FiscalPeriod;
import fin.entity.Payslip;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Spring service for generating PDF payslips using PDFBox.
 * Uses PDFBox for reliable PDF generation in Docker containers.
 * PDFBox provides good layout control and is compatible with aarch64 containers.
 */
@Service
public class PayslipPdfService {

    private static final Logger LOGGER = Logger.getLogger(PayslipPdfService.class.getName());

    /**
     * Generate a PDF payslip for the given data
     */
    public byte[] generatePayslipPdf(Payslip payslip, Employee employee, Company company, FiscalPeriod fiscalPeriod) {
        // Validate all required data exists (no fallbacks)
        validateRequiredData(payslip, employee, company, fiscalPeriod);

        LOGGER.info("Generating PDF payslip for employee: " + employee.getEmployeeNumber() +
                   ", payslip: " + payslip.getPayslipNumber());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Try to load a unicode TTF font (PDType0Font) for better Unicode support; fall back to Type1 Helvetica
                PDType0Font unicodeFont = null;
                org.apache.pdfbox.pdmodel.font.PDFont boldPdfFont = null;
                org.apache.pdfbox.pdmodel.font.PDFont normalPdfFont = null;
                try {
                    unicodeFont = loadSystemTTFFont(document);
                    if (unicodeFont != null) {
                        LOGGER.info("Loaded unicode TTF font for PDF generation");
                    }
                } catch (Exception ignore) {
                    // ignore; will fall back to Type1 fonts
                }

                if (unicodeFont != null) {
                    // Use unicode TTF font for both normal and bold if possible
                    normalPdfFont = unicodeFont;
                    boldPdfFont = unicodeFont;
                } else {
                    // Fall back to standard Helvetica fonts
                    boldPdfFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    normalPdfFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                }

                // Title
                contentStream.setFont(boldPdfFont, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("PAYSLIP");
                contentStream.endText();

                // Company name
                contentStream.setFont(boldPdfFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 720);
                contentStream.showText(sanitizeForFont("Company: " + company.getName(), unicodeFont, normalPdfFont));
                contentStream.endText();

                // Employee details
                contentStream.setFont(normalPdfFont, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 680);
                contentStream.showText(sanitizeForFont("Employee: " + employee.getFirstName() + " " + employee.getLastName(), unicodeFont, normalPdfFont));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText(sanitizeForFont("Employee Code: " + employee.getEmployeeNumber(), unicodeFont, normalPdfFont));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText(sanitizeForFont("Tax Number: " + (employee.getIdNumber() != null ? employee.getIdNumber() : "N/A"), unicodeFont, normalPdfFont));
                contentStream.endText();

                // Pay date (show N/A if missing)
                contentStream.setFont(normalPdfFont, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 640);
                contentStream.showText("Pay Date: " + (fiscalPeriod.getPayDate() != null ? fiscalPeriod.getPayDate().toString() : "N/A"));
                contentStream.endText();

                // Earnings and deductions
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 620);
                contentStream.showText(sanitizeForFont("Basic Salary: R " + payslip.getBasicSalary(), unicodeFont, normalPdfFont));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText(sanitizeForFont("PAYE: R " + (payslip.getPayeeTax() != null ? payslip.getPayeeTax() : "0.00"), unicodeFont, normalPdfFont));
                contentStream.newLineAtOffset(0, -15);
                contentStream.showText(sanitizeForFont("UIF: R " + (payslip.getUifEmployee() != null ? payslip.getUifEmployee() : "0.00"), unicodeFont, normalPdfFont));
                contentStream.endText();

                // Net pay
                BigDecimal netPay = payslip.getNetPay();
                if (netPay == null) {
                    BigDecimal totalEarnings = payslip.getBasicSalary();
                    BigDecimal totalDeductions = payslip.getPayeeTax() != null ? payslip.getPayeeTax() : BigDecimal.ZERO;
                    netPay = totalEarnings.subtract(totalDeductions);
                }

                contentStream.setFont(boldPdfFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 550);
                contentStream.showText(sanitizeForFont("NET PAY: R " + netPay, unicodeFont, boldPdfFont));
                contentStream.endText();
            }

            // Save to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] pdfBytes = outputStream.toByteArray();

            LOGGER.info("PDF generation completed for payslip: " + payslip.getPayslipNumber() + ", size: " + pdfBytes.length + " bytes");
            return pdfBytes;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate payslip PDF: " + e.getMessage(), e);
            throw new RuntimeException("Failed to generate payslip PDF: " + e.getMessage(), e);
        }
    }

    private PDType0Font loadSystemTTFFont(PDDocument document) {
        // First try to load an embedded font from the application resources (guaranteed fallback)
        try (java.io.InputStream is = PayslipPdfService.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
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

    /**
     * Validate that all required data exists - no fallbacks allowed
     */
    private void validateRequiredData(Payslip payslip, Employee employee, Company company, FiscalPeriod fiscalPeriod) {
        if (payslip == null) {
            throw new IllegalArgumentException("Payslip data is required and cannot be null");
        }
        if (employee == null) {
            throw new IllegalArgumentException("Employee data is required and cannot be null");
        }
        if (company == null) {
            throw new IllegalArgumentException("Company data is required and cannot be null");
        }
        if (fiscalPeriod == null) {
            throw new IllegalArgumentException("Fiscal period data is required and cannot be null");
        }

        // Validate critical payslip data exists
        if (payslip.getBasicSalary() == null) {
            throw new IllegalArgumentException("Basic salary is required for payslip generation");
        }
        if (employee.getEmployeeNumber() == null || employee.getEmployeeNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Employee number is required for payslip generation");
        }
        if (company.getName() == null || company.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required for payslip generation");
        }
        // payDate is optional for PDF generation; allow it but log if missing
        if (fiscalPeriod.getPayDate() == null) {
            LOGGER.warning("Fiscal period pay date is missing. Proceeding with PDF generation but pay date will be shown as N/A");
        }
    }
}