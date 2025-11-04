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
package fin.service;

import com.sun.jna.Pointer;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.util.Libharu;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service for generating professional PDF invoices from manual invoice data
 * Features: Beautiful layout, conditional VAT, banking details
 * Uses libharu (HPDF) for PDF generation
 */
public class InvoicePdfService {

    // Page dimensions
    private static final float PAGE_WIDTH_A4 = 595.28f;
    private static final float PAGE_HEIGHT_A4 = 841.89f;
    
    // Font sizes
    private static final float COMPANY_NAME_SIZE = 20f;
    private static final float HEADER_SIZE = 12f;
    private static final float NORMAL_SIZE = 10f;
    private static final float TITLE_SIZE = 18f;
    private static final float SMALL_SIZE = 9f;
    
    // Colors (RGB 0-1 range)
    private static final float[] PRIMARY_COLOR = {51/255f, 102/255f, 153/255f}; // Professional blue
    private static final float[] SECONDARY_COLOR = {240/255f, 240/255f, 240/255f}; // Light gray
    private static final float[] BORDER_COLOR = {220/255f, 220/255f, 220/255f};
    private static final float[] WHITE_COLOR = {1.0f, 1.0f, 1.0f};
    private static final float[] DARK_GRAY = {0.3f, 0.3f, 0.3f};

    // Margins
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 50f;
    private static final float MARGIN_BOTTOM = 50f;

    private final String dbUrl;

    public InvoicePdfService(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    /**
     * Generates a PDF invoice for the given invoice ID
     *
     * @param invoiceId The ID of the manual invoice
     * @param company The company information
     * @param fiscalPeriod The fiscal period information
     * @return Path to the generated PDF file
     */
    public String generateInvoicePdf(Long invoiceId, Company company, FiscalPeriod fiscalPeriod) throws SQLException, IOException {
        // Fetch invoice data from database
        InvoiceData invoiceData = fetchInvoiceData(invoiceId);

        if (invoiceData == null) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }

        // Create PDF document using libharu
        String fileName = String.format("Invoice_%s.pdf", invoiceData.invoiceNumber);
        String outputPath = "exports/" + fileName;

        // Ensure exports directory exists
        Path exportsDir = Paths.get("exports");
        if (!Files.exists(exportsDir)) {
            Files.createDirectories(exportsDir);
        }

        Libharu hpdf = Libharu.INSTANCE;
        Pointer pdf = hpdf.HPDF_New(null, null);
        
        try {
            Pointer page = hpdf.HPDF_AddPage(pdf);
            Pointer helveticaFont = hpdf.HPDF_GetFont(pdf, "Helvetica", null);
            Pointer helveticaBoldFont = hpdf.HPDF_GetFont(pdf, "Helvetica-Bold", null);
            
            // Load company logo if available
            Pointer logo = loadCompanyLogo(pdf, company);

            float currentY = PAGE_HEIGHT_A4 - MARGIN_TOP;

            // Add company header with logo
            currentY = addCompanyHeader(page, company, logo, helveticaBoldFont, helveticaFont, currentY);

            // Add invoice details
            currentY = addInvoiceDetails(page, invoiceData, fiscalPeriod, helveticaBoldFont, helveticaFont, currentY);

            // Add line items
            currentY = addInvoiceItems(page, invoiceData, helveticaBoldFont, helveticaFont, currentY);

            // Add totals (with conditional VAT based on company registration)
            currentY = addInvoiceTotals(page, invoiceData, company, helveticaBoldFont, helveticaFont, currentY);

            // Add footer (with banking details)
            addInvoiceFooter(page, company, helveticaBoldFont, helveticaFont, currentY);

            // Save the PDF
            hpdf.HPDF_SaveToFile(pdf, outputPath);

        } finally {
            hpdf.HPDF_Free(pdf);
        }

        return outputPath;
    }

    private Pointer loadCompanyLogo(Pointer pdf, Company company) {
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

    private InvoiceData fetchInvoiceData(Long invoiceId) throws SQLException {
        String sql = """
            SELECT mi.invoice_number, mi.invoice_date, mi.description, mi.amount,
                   da.account_code as debit_code, da.account_name as debit_name,
                   ca.account_code as credit_code, ca.account_name as credit_name
            FROM manual_invoices mi
            JOIN accounts da ON mi.debit_account_id = da.id
            JOIN accounts ca ON mi.credit_account_id = ca.id
            WHERE mi.id = ?
            """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, invoiceId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new InvoiceData(
                        rs.getString("invoice_number"),
                        rs.getDate("invoice_date").toLocalDate(),
                        rs.getString("description"),
                        rs.getBigDecimal("amount")
                    );
                }
            }
        }

        return null;
    }

    private float addCompanyHeader(Pointer page, Company company, Pointer logo, Pointer boldFont, Pointer normalFont, float yPos) {
        Libharu hpdf = Libharu.INSTANCE;
        
        // Company info box with gray background
        float boxX = MARGIN_LEFT + 120; // Leave space for logo
        float boxY = yPos - 100;
        float boxWidth = PAGE_WIDTH_A4 - MARGIN_LEFT - MARGIN_RIGHT - 120;
        float boxHeight = 100;
        
        // Draw gray background box
        hpdf.HPDF_Page_SetRGBFill(page, SECONDARY_COLOR[0], SECONDARY_COLOR[1], SECONDARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, boxX, boxY, boxWidth, boxHeight);
        hpdf.HPDF_Page_Fill(page);
        
        // Draw company logo if available
        if (logo != null) {
            float logoWidth = 100;
            float logoHeight = 90;
            float logoX = MARGIN_LEFT + 10;
            float logoY = boxY + 5;
            hpdf.HPDF_Page_DrawImage(page, logo, logoX, logoY, logoWidth, logoHeight);
        } else {
            // Logo placeholder text (if no logo)
            hpdf.HPDF_Page_BeginText(page);
            hpdf.HPDF_Page_SetFontAndSize(page, normalFont, SMALL_SIZE);
            hpdf.HPDF_Page_TextOut(page, MARGIN_LEFT + 30, boxY + 40, "[Logo]");
            hpdf.HPDF_Page_EndText(page);
        }
        
        // Company name (large, bold, dark gray)
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, DARK_GRAY[0], DARK_GRAY[1], DARK_GRAY[2]);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, COMPANY_NAME_SIZE);
        hpdf.HPDF_Page_TextOut(page, boxX + 10, boxY + boxHeight - 30, company.getName());
        hpdf.HPDF_Page_EndText(page);
        
        // Company details (smaller font)
        float detailY = boxY + boxHeight - 50;
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, SMALL_SIZE);
        
        if (company.getRegistrationNumber() != null && !company.getRegistrationNumber().isEmpty()) {
            hpdf.HPDF_Page_TextOut(page, boxX + 10, detailY, "Registration Number: " + company.getRegistrationNumber());
            detailY -= 12;
        }
        if (company.getTaxNumber() != null && !company.getTaxNumber().isEmpty()) {
            hpdf.HPDF_Page_TextOut(page, boxX + 10, detailY, "Tax Number: " + company.getTaxNumber());
            detailY -= 12;
        }
        if (company.getAddress() != null && !company.getAddress().isEmpty()) {
            hpdf.HPDF_Page_TextOut(page, boxX + 10, detailY, "Address: " + company.getAddress());
            detailY -= 12;
        }
        hpdf.HPDF_Page_TextOut(page, boxX + 10, detailY, "Email: " + (company.getContactEmail() != null ? company.getContactEmail() : "N/A"));
        detailY -= 12;
        hpdf.HPDF_Page_TextOut(page, boxX + 10, detailY, "Phone: " + (company.getContactPhone() != null ? company.getContactPhone() : "N/A"));
        hpdf.HPDF_Page_EndText(page);
        
        return boxY - 20; // Return new Y position with spacing
    }

    private float addInvoiceDetails(Pointer page, InvoiceData invoiceData, FiscalPeriod fiscalPeriod, Pointer boldFont, Pointer normalFont, float yPos) {
        Libharu hpdf = Libharu.INSTANCE;
        
        // "INVOICE" title with blue background
        float titleWidth = PAGE_WIDTH_A4 - MARGIN_LEFT - MARGIN_RIGHT;
        float titleHeight = 30;
        float titleY = yPos - titleHeight;
        
        // Draw blue background
        hpdf.HPDF_Page_SetRGBFill(page, PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, MARGIN_LEFT, titleY, titleWidth, titleHeight);
        hpdf.HPDF_Page_Fill(page);
        
        // Draw "INVOICE" text in white
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, WHITE_COLOR[0], WHITE_COLOR[1], WHITE_COLOR[2]);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, TITLE_SIZE);
        float textWidth = hpdf.HPDF_Page_TextWidth(page, "INVOICE");
        hpdf.HPDF_Page_TextOut(page, (PAGE_WIDTH_A4 - textWidth) / 2, titleY + 8, "INVOICE");
        hpdf.HPDF_Page_EndText(page);
        
        yPos = titleY - 20;
        
        // Invoice details table
        float labelX = MARGIN_LEFT;
        float valueX = MARGIN_LEFT + 150;
        
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        
        // Invoice Number
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos, "Invoice Number:");
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, valueX, yPos, invoiceData.invoiceNumber);
        yPos -= 20;
        
        // Invoice Date
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos, "Invoice Date:");
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, valueX, yPos, invoiceData.invoiceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        yPos -= 20;
        
        // Period
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos, "Period:");
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, valueX, yPos, fiscalPeriod.getPeriodName());
        
        hpdf.HPDF_Page_EndText(page);
        
        return yPos - 20;
    }

    private float addInvoiceItems(Pointer page, InvoiceData invoiceData, Pointer boldFont, Pointer normalFont, float yPos) {
        Libharu hpdf = Libharu.INSTANCE;
        
        // "Invoice Items" header
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
        hpdf.HPDF_Page_TextOut(page, MARGIN_LEFT, yPos, "Invoice Items");
        hpdf.HPDF_Page_EndText(page);
        
        yPos -= 20;
        
        // Table dimensions
        float tableX = MARGIN_LEFT;
        float tableWidth = PAGE_WIDTH_A4 - MARGIN_LEFT - MARGIN_RIGHT;
        float col1Width = tableWidth * 0.5f;  // Description: 50%
        float col2Width = tableWidth * 0.25f; // Quantity: 25%
        float col3Width = tableWidth * 0.25f; // Amount: 25%
        float rowHeight = 25;
        
        // Column X positions for alignment
        float col1X = tableX + 5;
        float col2X = tableX + col1Width + 5;
        float col3RightEdge = tableX + col1Width + col2Width + col3Width - 5;
        
        // Draw table header row with gray background
        hpdf.HPDF_Page_SetRGBFill(page, SECONDARY_COLOR[0], SECONDARY_COLOR[1], SECONDARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Fill(page);
        
        // Header borders
        hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        hpdf.HPDF_Page_SetLineWidth(page, 1.0f);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        // Header text
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, col1X, yPos - 17, "Description");
        hpdf.HPDF_Page_TextOut(page, col2X, yPos - 17, "Quantity");
        // Right-align "Amount" header
        String amountHeader = "Amount";
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
        float amountHeaderWidth = hpdf.HPDF_Page_TextWidth(page, amountHeader);
        hpdf.HPDF_Page_TextOut(page, col3RightEdge - amountHeaderWidth, yPos - 17, amountHeader);
        hpdf.HPDF_Page_EndText(page);
        
        yPos -= rowHeight;
        
        // Data row
        hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, col1X, yPos - 17, invoiceData.description);
        hpdf.HPDF_Page_TextOut(page, col2X, yPos - 17, "1");
        // Right-align amount value
        String amountText = "R " + invoiceData.amount.toString();
        float amountTextWidth = hpdf.HPDF_Page_TextWidth(page, amountText);
        hpdf.HPDF_Page_TextOut(page, col3RightEdge - amountTextWidth, yPos - 17, amountText);
        hpdf.HPDF_Page_EndText(page);
        
        // Draw vertical lines
        hpdf.HPDF_Page_MoveTo(page, tableX + col1Width, yPos);
        hpdf.HPDF_Page_LineTo(page, tableX + col1Width, yPos + rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        hpdf.HPDF_Page_MoveTo(page, tableX + col1Width + col2Width, yPos);
        hpdf.HPDF_Page_LineTo(page, tableX + col1Width + col2Width, yPos + rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        return yPos - rowHeight - 20;
    }

    private float addInvoiceTotals(Pointer page, InvoiceData invoiceData, Company company, Pointer boldFont, Pointer normalFont, float yPos) {
        Libharu hpdf = Libharu.INSTANCE;
        
        // Totals table on the right side
        float tableWidth = 250;
        float tableX = PAGE_WIDTH_A4 - MARGIN_RIGHT - tableWidth;
        float col1Width = 150;
        float col2Width = 100;
        float rowHeight = 25;
        
        // Column positions
        float labelX = tableX + 5;
        float valueRightEdge = tableX + col1Width + col2Width - 5;
        
        // Subtotal row with gray background
        hpdf.HPDF_Page_SetRGBFill(page, SECONDARY_COLOR[0], SECONDARY_COLOR[1], SECONDARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, col1Width, rowHeight);
        hpdf.HPDF_Page_Fill(page);
        
        hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        hpdf.HPDF_Page_SetLineWidth(page, 1.0f);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos - 17, "Subtotal:");
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        String subtotalText = "R " + String.format("%.2f", invoiceData.amount);
        float subtotalWidth = hpdf.HPDF_Page_TextWidth(page, subtotalText);
        hpdf.HPDF_Page_TextOut(page, valueRightEdge - subtotalWidth, yPos - 17, subtotalText);
        hpdf.HPDF_Page_EndText(page);
        
        yPos -= rowHeight;
        
        // VAT row (conditional based on company registration)
        BigDecimal total;
        BigDecimal vat;
        String vatText;
        
        if (company.isVatRegistered()) {
            vat = invoiceData.amount.multiply(new BigDecimal("0.15"));
            total = invoiceData.amount.add(vat);
            vatText = "VAT (15%):";
        } else {
            vat = BigDecimal.ZERO;
            total = invoiceData.amount;
            vatText = "VAT:";
        }
        
        // VAT row with gray background
        hpdf.HPDF_Page_SetRGBFill(page, SECONDARY_COLOR[0], SECONDARY_COLOR[1], SECONDARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, col1Width, rowHeight);
        hpdf.HPDF_Page_Fill(page);
        
        hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos - 17, vatText);
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
        String vatAmountText = "R " + String.format("%.2f", vat);
        float vatWidth = hpdf.HPDF_Page_TextWidth(page, vatAmountText);
        hpdf.HPDF_Page_TextOut(page, valueRightEdge - vatWidth, yPos - 17, vatAmountText);
        hpdf.HPDF_Page_EndText(page);
        
        yPos -= rowHeight;
        
        // Total row with blue background and white text
        hpdf.HPDF_Page_SetRGBFill(page, PRIMARY_COLOR[0], PRIMARY_COLOR[1], PRIMARY_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Fill(page);
        
        hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
        hpdf.HPDF_Page_Rectangle(page, tableX, yPos - rowHeight, tableWidth, rowHeight);
        hpdf.HPDF_Page_Stroke(page);
        
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, WHITE_COLOR[0], WHITE_COLOR[1], WHITE_COLOR[2]);
        hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
        hpdf.HPDF_Page_TextOut(page, labelX, yPos - 17, "Total:");
        String totalText = "R " + String.format("%.2f", total);
        float totalWidth = hpdf.HPDF_Page_TextWidth(page, totalText);
        hpdf.HPDF_Page_TextOut(page, valueRightEdge - totalWidth, yPos - 17, totalText);
        hpdf.HPDF_Page_EndText(page);
        
        return yPos - rowHeight - 30;
    }

    private void addInvoiceFooter(Pointer page, Company company, Pointer boldFont, Pointer normalFont, float yPos) {
        Libharu hpdf = Libharu.INSTANCE;
        
        // Banking details section
        if (company.getBankName() != null && !company.getBankName().isEmpty()) {
            yPos -= 20; // Spacing
            
            // "BANKING DETAILS" header with gray background
            float headerHeight = 25;
            float tableWidth = PAGE_WIDTH_A4 - MARGIN_LEFT - MARGIN_RIGHT;
            
            hpdf.HPDF_Page_SetRGBFill(page, SECONDARY_COLOR[0], SECONDARY_COLOR[1], SECONDARY_COLOR[2]);
            hpdf.HPDF_Page_Rectangle(page, MARGIN_LEFT, yPos - headerHeight, tableWidth, headerHeight);
            hpdf.HPDF_Page_Fill(page);
            
            hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
            hpdf.HPDF_Page_SetLineWidth(page, 1.0f);
            hpdf.HPDF_Page_Rectangle(page, MARGIN_LEFT, yPos - headerHeight, tableWidth, headerHeight);
            hpdf.HPDF_Page_Stroke(page);
            
            hpdf.HPDF_Page_BeginText(page);
            hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
            hpdf.HPDF_Page_SetFontAndSize(page, boldFont, HEADER_SIZE);
            float textWidth = hpdf.HPDF_Page_TextWidth(page, "BANKING DETAILS");
            hpdf.HPDF_Page_TextOut(page, (PAGE_WIDTH_A4 - textWidth) / 2, yPos - 17, "BANKING DETAILS");
            hpdf.HPDF_Page_EndText(page);
            
            yPos -= headerHeight;
            
            // Banking info details
            float labelX = MARGIN_LEFT + 10;
            float valueX = MARGIN_LEFT + 150;
            float detailY = yPos - 20;
            
            hpdf.HPDF_Page_BeginText(page);
            hpdf.HPDF_Page_SetRGBFill(page, 0, 0, 0);
            
            // Bank name
            hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
            hpdf.HPDF_Page_TextOut(page, labelX, detailY, "Bank:");
            hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
            hpdf.HPDF_Page_TextOut(page, valueX, detailY, company.getBankName());
            detailY -= 15;
            
            // Account holder
            hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
            hpdf.HPDF_Page_TextOut(page, labelX, detailY, "Account Holder:");
            hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
            hpdf.HPDF_Page_TextOut(page, valueX, detailY, company.getName());
            detailY -= 15;
            
            // Account number
            if (company.getAccountNumber() != null && !company.getAccountNumber().isEmpty()) {
                hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, labelX, detailY, "Account Number:");
                hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, valueX, detailY, company.getAccountNumber());
                detailY -= 15;
            }
            
            // Account type
            if (company.getAccountType() != null && !company.getAccountType().isEmpty()) {
                hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, labelX, detailY, "Account Type:");
                hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, valueX, detailY, company.getAccountType());
                detailY -= 15;
            }
            
            // Branch code
            if (company.getBranchCode() != null && !company.getBranchCode().isEmpty()) {
                hpdf.HPDF_Page_SetFontAndSize(page, boldFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, labelX, detailY, "Branch Code:");
                hpdf.HPDF_Page_SetFontAndSize(page, normalFont, NORMAL_SIZE);
                hpdf.HPDF_Page_TextOut(page, valueX, detailY, company.getBranchCode());
            }
            
            hpdf.HPDF_Page_EndText(page);
            
            // Draw border around banking details
            float bankingBoxHeight = yPos - detailY + 35;
            hpdf.HPDF_Page_SetRGBStroke(page, BORDER_COLOR[0], BORDER_COLOR[1], BORDER_COLOR[2]);
            hpdf.HPDF_Page_Rectangle(page, MARGIN_LEFT, detailY - 10, tableWidth, bankingBoxHeight);
            hpdf.HPDF_Page_Stroke(page);
        }
        
        // Footer text at bottom of page
        float footerY = MARGIN_BOTTOM + 20;
        hpdf.HPDF_Page_BeginText(page);
        hpdf.HPDF_Page_SetRGBFill(page, 0.5f, 0.5f, 0.5f);
        hpdf.HPDF_Page_SetFontAndSize(page, normalFont, SMALL_SIZE);
        String footerText = "Payment is due within 30 days. Thank you for your business.";
        float footerWidth = hpdf.HPDF_Page_TextWidth(page, footerText);
        hpdf.HPDF_Page_TextOut(page, (PAGE_WIDTH_A4 - footerWidth) / 2, footerY, footerText);
        hpdf.HPDF_Page_EndText(page);
    }

    /**
     * Inner class to hold invoice data
     */
    private static class InvoiceData {
        final String invoiceNumber;
        final LocalDate invoiceDate;
        final String description;
        final BigDecimal amount;

        InvoiceData(String invoiceNumber, LocalDate invoiceDate, String description, BigDecimal amount) {
            this.invoiceNumber = invoiceNumber;
            this.invoiceDate = invoiceDate;
            this.description = description;
            this.amount = amount;
        }
    }
}