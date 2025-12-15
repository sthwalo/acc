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

import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.entity.ManualInvoice;
import fin.repository.ManualInvoiceRepository;
import fin.util.Debugger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Spring service for generating professional PDF invoices from manual invoice data
 * Features: Beautiful layout, conditional VAT, banking details
 * Uses Apache PDFBox for PDF generation
 */
@Service
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

    private final ManualInvoiceRepository manualInvoiceRepository;
    private final Debugger debugger;
    private final fin.repository.InvoicePdfRepository invoicePdfRepository;

    public InvoicePdfService(ManualInvoiceRepository manualInvoiceRepository,
                                 Debugger debugger,
                                 fin.repository.InvoicePdfRepository invoicePdfRepository) {
        this.manualInvoiceRepository = manualInvoiceRepository;
        this.debugger = debugger;
        this.invoicePdfRepository = invoicePdfRepository;
    }

    /**
     * Generates a PDF invoice as byte array for the given invoice ID
     *
     * @param invoiceId The ID of the manual invoice
     * @param company The company information
     * @param fiscalPeriod The fiscal period information
     * @return PDF content as byte array
     */
    public byte[] generateInvoicePdfBytes(Long invoiceId, Company company, FiscalPeriod fiscalPeriod) throws SQLException, IOException {
        debugger.logMethodEntry("InvoicePdfService", "generateInvoicePdfBytes", invoiceId, company.getId(), fiscalPeriod.getId());

        // Fetch invoice data from database
        InvoiceData invoiceData = fetchInvoiceData(invoiceId);

        if (invoiceData == null) {
            debugger.logValidationError("InvoicePdfService", "generateInvoicePdfBytes", "invoiceId", invoiceId.toString(), "Invoice not found");
            throw new IllegalArgumentException("Invoice not found: " + invoiceId);
        }

        // Generate PDF bytes using PDFBox
        byte[] pdfBytes = generatePdfBytesWithPdfBox(invoiceData, company, fiscalPeriod);

        debugger.logMethodExit("InvoicePdfService", "generateInvoicePdfBytes", "PDF generated successfully");
        return pdfBytes;
    }

    private InvoiceData fetchInvoiceData(Long invoiceId) throws SQLException {
        try {
            fin.dto.InvoicePdfData dto = invoicePdfRepository.findInvoicePdfDataByInvoiceId(invoiceId);
            if (dto == null) return null;
            return new InvoiceData(dto.getInvoiceNumber(), dto.getInvoiceDate(), dto.getDescription(), dto.getAmount());
        } catch (Exception e) {
            debugger.logException("Fetch Invoice Data", "InvoicePdfService", "fetchInvoiceData", new SQLException(e), invoiceId);
            throw new SQLException(e);
        }
    }

    private byte[] generatePdfBytesWithPdfBox(InvoiceData invoiceData, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        // Create a simple PDF using PDFBox 3.0.0
        try (org.apache.pdfbox.pdmodel.PDDocument document = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            document.addPage(page);

            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                // Load standard fonts
                org.apache.pdfbox.pdmodel.font.PDFont boldFont = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA_BOLD);
                org.apache.pdfbox.pdmodel.font.PDFont normalFont = new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);

                // Add basic invoice content
                contentStream.beginText();
                contentStream.setFont(boldFont, TITLE_SIZE);
                contentStream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT_A4 - MARGIN_TOP);
                contentStream.showText("INVOICE");
                contentStream.endText();

                contentStream.beginText();
                contentStream.setFont(normalFont, NORMAL_SIZE);
                contentStream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT_A4 - MARGIN_TOP - 50);
                contentStream.showText("Invoice Number: " + invoiceData.invoiceNumber);
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT_A4 - MARGIN_TOP - 70);
                contentStream.showText("Date: " + invoiceData.invoiceDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT_A4 - MARGIN_TOP - 90);
                contentStream.showText("Amount: R" + invoiceData.amount.toString());
                contentStream.endText();

                if (invoiceData.description != null) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(MARGIN_LEFT, PAGE_HEIGHT_A4 - MARGIN_TOP - 110);
                    contentStream.showText("Description: " + invoiceData.description);
                    contentStream.endText();
                }
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
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