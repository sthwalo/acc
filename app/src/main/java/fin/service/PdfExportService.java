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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.awt.Color;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting data to PDF format using Apache PDFBox (open source).
 */
public class PdfExportService {
    
    private static final float TITLE_FONT_SIZE = 16f;
    private static final float HEADER_FONT_SIZE = 12f;
    private static final float NORMAL_FONT_SIZE = 10f;
    private static final float SMALL_FONT_SIZE = 8f;
    private static final float LINE_HEIGHT = 15f;
    private static final float MARGIN = 50f;
    
    public String exportTransactionsToPdf(List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) {
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions to export");
        }
        
        Path reportsDir = Paths.get("reports");
        try {
            if (!Files.exists(reportsDir)) {
                Files.createDirectory(reportsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create reports directory", e);
        }
        
        String filename = String.format("reports/transactions_%s_%s_%s.pdf", 
                company.getName().replaceAll("\\s+", "_").toLowerCase(),
                fiscalPeriod.getPeriodName().replaceAll("\\s+", "_").toLowerCase(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        try (PDDocument document = new PDDocument()) {
            addMetadata(document, company, fiscalPeriod);
            // Landscape mode: use custom PDRectangle with swapped dimensions
            PDPage page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            document.addPage(page);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float yPosition = addTitleSection(contentStream, company, fiscalPeriod);
                yPosition -= 30;
                addTransactionsTable(contentStream, transactions, yPosition);
            }
            
            document.save(filename);
            return filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
    
    private void addMetadata(PDDocument document, Company company, FiscalPeriod fiscalPeriod) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle("Transaction Report - " + company.getName());
        info.setSubject("Financial Transactions for " + fiscalPeriod.getPeriodName());
        info.setKeywords("transactions, finance, report");
        info.setAuthor("FIN Application");
        info.setCreator("FIN Application");
    }
    
    private float addTitleSection(PDPageContentStream contentStream, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        // Landscape dimensions (A4 rotated)
        float yPosition = PDRectangle.A4.getWidth() - MARGIN;
        float centerX = PDRectangle.A4.getHeight() / 2;
        
        PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        String title = "Transaction Report";
        float titleWidth = titleFont.getStringWidth(title) / 1000 * TITLE_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(titleFont, TITLE_FONT_SIZE);
        contentStream.newLineAtOffset(centerX - titleWidth / 2, yPosition);
        contentStream.showText(title);
        contentStream.endText();
        
        yPosition -= 25;
        String companyText = "Company: " + company.getName();
        float companyWidth = titleFont.getStringWidth(companyText) / 1000 * HEADER_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(titleFont, HEADER_FONT_SIZE);
        contentStream.newLineAtOffset(centerX - companyWidth / 2, yPosition);
        contentStream.showText(companyText);
        contentStream.endText();
        
        yPosition -= 20;
        String periodText = String.format("Fiscal Period: %s (%s to %s)",
            fiscalPeriod.getPeriodName(),
            fiscalPeriod.getStartDate(),
            fiscalPeriod.getEndDate());
        float periodWidth = normalFont.getStringWidth(periodText) / 1000 * NORMAL_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(normalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(centerX - periodWidth / 2, yPosition);
        contentStream.showText(periodText);
        contentStream.endText();
        
        yPosition -= 20;
        String dateText = "Report Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        float dateWidth = normalFont.getStringWidth(dateText) / 1000 * NORMAL_FONT_SIZE;
        contentStream.beginText();
        contentStream.setFont(normalFont, NORMAL_FONT_SIZE);
        contentStream.newLineAtOffset(centerX - dateWidth / 2, yPosition);
        contentStream.showText(dateText);
        contentStream.endText();
        
        return yPosition;
    }
    
    private void addTransactionsTable(PDPageContentStream contentStream, List<BankTransaction> transactions, float startY) throws IOException {
        PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        float yPosition = startY;
        float xStart = MARGIN;
        float colWidth = 100f;
        
        String[] headers = {"Date", "Details", "Debit", "Credit", "Balance"};
        contentStream.setFont(headerFont, SMALL_FONT_SIZE);
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xStart + i * colWidth, yPosition);
            contentStream.showText(headers[i]);
            contentStream.endText();
        }
        
        yPosition -= LINE_HEIGHT;
        contentStream.setStrokingColor(Color.LIGHT_GRAY);
        contentStream.moveTo(xStart, yPosition);
        contentStream.lineTo(xStart + colWidth * headers.length, yPosition);
        contentStream.stroke();
        
        yPosition -= LINE_HEIGHT;
        contentStream.setFont(normalFont, SMALL_FONT_SIZE);
        
        for (BankTransaction transaction : transactions) {
            if (yPosition < MARGIN + 50) break;
            
            String[] values = {
                transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                truncate(transaction.getDetails(), 30),
                formatAmount(transaction.getDebitAmount()),
                formatAmount(transaction.getCreditAmount()),
                formatAmount(transaction.getBalance())
            };
            
            for (int i = 0; i < values.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(xStart + i * colWidth, yPosition);
                contentStream.showText(values[i]);
                contentStream.endText();
            }
            
            yPosition -= LINE_HEIGHT;
        }
        
        yPosition -= 20;
        BigDecimal totalDebits = transactions.stream()
            .filter(t -> t.getDebitAmount() != null)
            .map(BankTransaction::getDebitAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = transactions.stream()
            .filter(t -> t.getCreditAmount() != null)
            .map(BankTransaction::getCreditAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        contentStream.setFont(headerFont, NORMAL_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(xStart, yPosition);
        contentStream.showText(String.format("Total Transactions: %d | Total Debits: %s | Total Credits: %s",
            transactions.size(), formatAmount(totalDebits), formatAmount(totalCredits)));
        contentStream.endText();
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "";
        return String.format("%,.2f", amount);
    }
    
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
