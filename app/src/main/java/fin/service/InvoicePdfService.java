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

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.io.FileOutputStream;
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
 * Now uses centralized PdfBrandingService for consistent footer and copyright
 */
public class InvoicePdfService {

    private static final Font COMPANY_NAME_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.WHITE);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL);
    
    private static final BaseColor PRIMARY_COLOR = new BaseColor(51, 102, 153); // Professional blue
    private static final BaseColor SECONDARY_COLOR = new BaseColor(240, 240, 240); // Light gray
    private static final BaseColor BORDER_COLOR = new BaseColor(220, 220, 220);

    private final String dbUrl;
    private final PdfBrandingService brandingService;

    public InvoicePdfService(String dbUrl) {
        this.dbUrl = dbUrl;
        this.brandingService = new PdfBrandingService();
    }

    /**
     * Generates a PDF invoice for the given invoice ID
     *
     * @param invoiceId The ID of the manual invoice
     * @param company The company information
     * @param fiscalPeriod The fiscal period information
     * @return Path to the generated PDF file
     */
    public String generateInvoicePdf(Long invoiceId, Company company, FiscalPeriod fiscalPeriod) throws SQLException, IOException, DocumentException {
        // Fetch invoice data from database
        InvoiceData invoiceData = fetchInvoiceData(invoiceId);

        if (invoiceData == null) {
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }

        // Create PDF document
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        String fileName = String.format("Invoice_%s.pdf", invoiceData.invoiceNumber);
        String outputPath = "exports/" + fileName;

        // Ensure exports directory exists
        Path exportsDir = Paths.get("exports");
        if (!Files.exists(exportsDir)) {
            Files.createDirectories(exportsDir);
        }

        PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();

        try {
            // Add company header
            addCompanyHeader(document, company);

            // Add invoice details
            addInvoiceDetails(document, invoiceData, fiscalPeriod);

            // Add line items
            addInvoiceItems(document, invoiceData);

            // Add totals (with conditional VAT based on company registration)
            addInvoiceTotals(document, invoiceData, company);

            // Add footer (with banking details)
            addInvoiceFooter(document, company);

        } finally {
            document.close();
        }

        return outputPath;
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

    private void addCompanyHeader(Document document, Company company) throws DocumentException, IOException {
        // Create header table with 2 columns (logo | company info)
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{1, 3}); // Logo column smaller than info column
        headerTable.setSpacingAfter(20);
        
        // Left cell: Company logo (if available)
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(10);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        if (company.getLogoPath() != null && !company.getLogoPath().isEmpty()) {
            try {
                Path logoPath = Paths.get(company.getLogoPath());
                if (Files.exists(logoPath)) {
                    Image logo = Image.getInstance(company.getLogoPath());
                    // Scale logo to fit nicely (max 80x80 pixels)
                    logo.scaleToFit(80, 80);
                    logo.setAlignment(Element.ALIGN_CENTER);
                    logoCell.addElement(logo);
                } else {
                    // Logo file not found, add placeholder
                    Paragraph placeholder = new Paragraph("[Logo]", SMALL_FONT);
                    placeholder.setAlignment(Element.ALIGN_CENTER);
                    logoCell.addElement(placeholder);
                }
            } catch (Exception e) {
                // Error loading logo, add placeholder
                Paragraph placeholder = new Paragraph("[Logo]", SMALL_FONT);
                placeholder.setAlignment(Element.ALIGN_CENTER);
                logoCell.addElement(placeholder);
            }
        } else {
            // No logo configured
            logoCell.addElement(new Paragraph("", SMALL_FONT));
        }
        
        headerTable.addCell(logoCell);
        
        // Right cell: Company name and details
        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(Rectangle.NO_BORDER);
        infoCell.setPadding(10);
        infoCell.setBackgroundColor(SECONDARY_COLOR);
        
        // Company name with professional styling
        Paragraph title = new Paragraph(company.getName(), COMPANY_NAME_FONT);
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(5);
        infoCell.addElement(title);
        
        // Company details
        Paragraph companyInfo = new Paragraph();
        companyInfo.setFont(SMALL_FONT);
        companyInfo.setAlignment(Element.ALIGN_LEFT);
        
        if (company.getRegistrationNumber() != null && !company.getRegistrationNumber().isEmpty()) {
            companyInfo.add("Registration Number: " + company.getRegistrationNumber() + "\n");
        }
        if (company.getTaxNumber() != null && !company.getTaxNumber().isEmpty()) {
            companyInfo.add("Tax Number: " + company.getTaxNumber() + "\n");
        }
        if (company.getAddress() != null && !company.getAddress().isEmpty()) {
            companyInfo.add("Address: " + company.getAddress() + "\n");
        }
        companyInfo.add("Email: " + (company.getContactEmail() != null ? company.getContactEmail() : "N/A") + "\n");
        companyInfo.add("Phone: " + (company.getContactPhone() != null ? company.getContactPhone() : "N/A"));
        
        infoCell.addElement(companyInfo);
        headerTable.addCell(infoCell);
        
        document.add(headerTable);
    }

    private void addInvoiceDetails(Document document, InvoiceData invoiceData, FiscalPeriod fiscalPeriod) throws DocumentException {
        // Invoice header with colored background
        PdfPTable invoiceHeaderTable = new PdfPTable(1);
        invoiceHeaderTable.setWidthPercentage(100);
        invoiceHeaderTable.setSpacingAfter(20);
        
        PdfPCell invoiceHeaderCell = new PdfPCell(new Phrase("INVOICE", TITLE_FONT));
        invoiceHeaderCell.setBackgroundColor(PRIMARY_COLOR);
        invoiceHeaderCell.setPadding(10);
        invoiceHeaderCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        invoiceHeaderCell.setBorder(Rectangle.NO_BORDER);
        invoiceHeaderTable.addCell(invoiceHeaderCell);
        
        document.add(invoiceHeaderTable);

        // Invoice details table with borders
        PdfPTable detailsTable = new PdfPTable(new float[]{3, 7});
        detailsTable.setWidthPercentage(100);
        detailsTable.setSpacingAfter(20);

        // Invoice Number
        detailsTable.addCell(createStyledCell("Invoice Number:", HEADER_FONT, true));
        detailsTable.addCell(createStyledCell(invoiceData.invoiceNumber, NORMAL_FONT, false));

        // Invoice Date
        detailsTable.addCell(createStyledCell("Invoice Date:", HEADER_FONT, true));
        detailsTable.addCell(createStyledCell(invoiceData.invoiceDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT, false));

        // Fiscal Period
        detailsTable.addCell(createStyledCell("Period:", HEADER_FONT, true));
        detailsTable.addCell(createStyledCell(fiscalPeriod.getPeriodName(), NORMAL_FONT, false));

        document.add(detailsTable);
    }

    private void addInvoiceItems(Document document, InvoiceData invoiceData) throws DocumentException {
        Paragraph itemsHeader = new Paragraph("Invoice Items", HEADER_FONT);
        itemsHeader.setSpacingAfter(10);
        document.add(itemsHeader);

        // Items table
        PdfPTable itemsTable = new PdfPTable(new float[]{4, 2, 2});
        itemsTable.setWidthPercentage(100);
        itemsTable.setSpacingAfter(20);

        // Headers
        itemsTable.addCell(createCell("Description", HEADER_FONT));
        itemsTable.addCell(createCell("Quantity", HEADER_FONT));
        itemsTable.addCell(createCell("Amount", HEADER_FONT));

        // Item row
        itemsTable.addCell(createCell(invoiceData.description, NORMAL_FONT));
        itemsTable.addCell(createCell("1", NORMAL_FONT));
        itemsTable.addCell(createCell("R " + invoiceData.amount.toString(), NORMAL_FONT));

        document.add(itemsTable);
    }

    private void addInvoiceTotals(Document document, InvoiceData invoiceData, Company company) throws DocumentException {
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(50);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalsTable.setSpacingAfter(30);

        // Subtotal
        PdfPCell subtotalLabel = createCell("Subtotal:", HEADER_FONT);
        subtotalLabel.setBackgroundColor(SECONDARY_COLOR);
        totalsTable.addCell(subtotalLabel);
        totalsTable.addCell(createCell("R " + String.format("%.2f", invoiceData.amount), NORMAL_FONT));

        // VAT (conditional based on company registration)
        BigDecimal total;
        
        if (company.isVatRegistered()) {
            BigDecimal vat = invoiceData.amount.multiply(new BigDecimal("0.15"));
            PdfPCell vatLabel = createCell("VAT (15%):", HEADER_FONT);
            vatLabel.setBackgroundColor(SECONDARY_COLOR);
            totalsTable.addCell(vatLabel);
            totalsTable.addCell(createCell("R " + String.format("%.2f", vat), NORMAL_FONT));
            total = invoiceData.amount.add(vat);
        } else {
            PdfPCell vatLabel = createCell("VAT:", HEADER_FONT);
            vatLabel.setBackgroundColor(SECONDARY_COLOR);
            totalsTable.addCell(vatLabel);
            totalsTable.addCell(createCell("R 0.00", NORMAL_FONT));
            total = invoiceData.amount;
        }

        // Total with colored background
        PdfPCell totalLabel = createCell("Total:", HEADER_FONT);
        totalLabel.setBackgroundColor(PRIMARY_COLOR);
        Font whiteFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        totalLabel.setPhrase(new Phrase("Total:", whiteFont));
        totalsTable.addCell(totalLabel);
        
        PdfPCell totalValue = createCell("R " + String.format("%.2f", total), HEADER_FONT);
        totalValue.setBackgroundColor(PRIMARY_COLOR);
        totalValue.setPhrase(new Phrase("R " + String.format("%.2f", total), whiteFont));
        totalsTable.addCell(totalValue);

        document.add(totalsTable);
    }

    private void addInvoiceFooter(Document document, Company company) throws DocumentException {
        // Add spacing before footer
        document.add(new Paragraph("\n"));
        
        // Banking details section
        if (company.getBankName() != null && !company.getBankName().isEmpty()) {
            PdfPTable bankingTable = new PdfPTable(1);
            bankingTable.setWidthPercentage(100);
            bankingTable.setSpacingBefore(20);
            bankingTable.setSpacingAfter(10);
            
            PdfPCell bankingHeader = new PdfPCell(new Phrase("BANKING DETAILS", HEADER_FONT));
            bankingHeader.setBackgroundColor(SECONDARY_COLOR);
            bankingHeader.setPadding(8);
            bankingHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            bankingHeader.setBorder(Rectangle.BOX);
            bankingHeader.setBorderColor(BORDER_COLOR);
            bankingTable.addCell(bankingHeader);
            
            // Banking info table
            PdfPTable bankInfo = new PdfPTable(new float[]{3, 7});
            bankInfo.setWidthPercentage(100);
            
            // Bank name
            bankInfo.addCell(createStyledCell("Bank:", HEADER_FONT, true));
            bankInfo.addCell(createStyledCell(company.getBankName(), NORMAL_FONT, false));
            
            // Account holder (company name)
            bankInfo.addCell(createStyledCell("Account Holder:", HEADER_FONT, true));
            bankInfo.addCell(createStyledCell(company.getName(), NORMAL_FONT, false));
            
            // Account number
            if (company.getAccountNumber() != null && !company.getAccountNumber().isEmpty()) {
                bankInfo.addCell(createStyledCell("Account Number:", HEADER_FONT, true));
                bankInfo.addCell(createStyledCell(company.getAccountNumber(), NORMAL_FONT, false));
            }
            
            // Account type
            if (company.getAccountType() != null && !company.getAccountType().isEmpty()) {
                bankInfo.addCell(createStyledCell("Account Type:", HEADER_FONT, true));
                bankInfo.addCell(createStyledCell(company.getAccountType(), NORMAL_FONT, false));
            }
            
            // Branch code
            if (company.getBranchCode() != null && !company.getBranchCode().isEmpty()) {
                bankInfo.addCell(createStyledCell("Branch Code:", HEADER_FONT, true));
                bankInfo.addCell(createStyledCell(company.getBranchCode(), NORMAL_FONT, false));
            }
            
            PdfPCell bankInfoCell = new PdfPCell(bankInfo);
            bankInfoCell.setBorder(Rectangle.BOX);
            bankInfoCell.setBorderColor(BORDER_COLOR);
            bankInfoCell.setPadding(5);
            bankingTable.addCell(bankInfoCell);
            
            document.add(bankingTable);
        }
        
        // Use centralized branding service for footer and copyright
        brandingService.addFullBranding(document, 1);
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        return cell;
    }
    
    /**
     * Creates a styled cell with optional background color
     *
     * @param text The text content
     * @param font The font to use
     * @param isLabel Whether this is a label (gets background color)
     * @return Styled PdfPCell
     */
    private PdfPCell createStyledCell(String text, Font font, boolean isLabel) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER_COLOR);
        
        if (isLabel) {
            cell.setBackgroundColor(SECONDARY_COLOR);
        }
        
        return cell;
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