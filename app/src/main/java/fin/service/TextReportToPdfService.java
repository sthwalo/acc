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
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Service for converting text-based financial reports to PDF format using Apache PDFBox.
 * Handles proper formatting, page breaks, headers, footers, and styling.
 * 
 * Uses Apache PDFBox (open source) instead of iText (commercial licensing).
 */
public class TextReportToPdfService {
    
    private static final Logger LOGGER = Logger.getLogger(TextReportToPdfService.class.getName());
    
    // Font sizes
    private static final float TITLE_FONT_SIZE = 18f;
    private static final float SUBTITLE_FONT_SIZE = 12f;
    private static final float HEADER_FONT_SIZE = 10f;
    private static final float NORMAL_FONT_SIZE = 8f;
    private static final float FOOTER_FONT_SIZE = 8f;
    
    // Page settings
    private static final float MARGIN_TOP = 80f;
    private static final float MARGIN_BOTTOM = 50f;
    private static final float MARGIN_LEFT = 40f;
    private static final float LINE_HEIGHT = 12f;
    
    public TextReportToPdfService() {
        // Constructor - no dependencies needed
    }
    
    /**
     * Converts a text report to PDF format with proper formatting
     * 
     * @param textContent The text content of the report
     * @param reportType Type of report (e.g., "general_ledger", "trial_balance")
     * @param company Company information
     * @param fiscalPeriod Fiscal period information
     * @return Path to the generated PDF file
     */
    public String convertTextReportToPdf(String textContent, String reportType, 
                                        Company company, FiscalPeriod fiscalPeriod) {
        try {
            // Create reports directory if it doesn't exist
            Path reportsDir = Paths.get("reports");
            if (!Files.exists(reportsDir)) {
                Files.createDirectories(reportsDir);
            }
            
            // Generate filename
            String reportName = getReportDisplayName(reportType);
            String periodName = fiscalPeriod != null ? fiscalPeriod.getPeriodName() : "Unknown";
            String filename = String.format("reports/%s(%s).pdf", reportName, periodName);
            
            // Create PDF document
            PDDocument document = new PDDocument();
            
            try {
                // Add metadata
                addMetadata(document, reportName, company, fiscalPeriod);
                
                // Create first page with title
                PDPage titlePage = new PDPage(PDRectangle.A4);
                document.addPage(titlePage);
                addTitleSection(document, titlePage, reportName, company, fiscalPeriod);
                
                // Add content pages
                addReportContent(document, textContent, reportName, company, fiscalPeriod);
                
                // Save document
                document.save(filename);
                
                LOGGER.info("PDF report generated: " + filename);
                return filename;
                
            } finally {
                document.close();
            }
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error converting text report to PDF", e);
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }
    
    /**
     * Adds document metadata
     */
    private void addMetadata(PDDocument document, String reportName, Company company, FiscalPeriod fiscalPeriod) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(reportName + " - " + (company != null ? company.getName() : "FIN Report"));
        info.setSubject("Financial Report for " + (fiscalPeriod != null ? fiscalPeriod.getPeriodName() : ""));
        info.setKeywords("finance, report, " + reportName.toLowerCase());
        info.setAuthor("FIN Financial Management System");
        info.setCreator("FIN Application v1.0");
    }
    
    /**
     * Adds title section with company info
     */
    private void addTitleSection(PDDocument document, PDPage page, String reportName, 
                                 Company company, FiscalPeriod fiscalPeriod) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);
        
        try {
            float yPosition = PDRectangle.A4.getHeight() - 100;
            float centerX = PDRectangle.A4.getWidth() / 2;
            
            // Report title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), TITLE_FONT_SIZE);
            float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                .getStringWidth(reportName) / 1000 * TITLE_FONT_SIZE;
            contentStream.newLineAtOffset(centerX - titleWidth / 2, yPosition);
            contentStream.showText(reportName);
            contentStream.endText();
            
            yPosition -= 40;
            
            // Company information
            if (company != null) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), SUBTITLE_FONT_SIZE);
                float companyWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(company.getName()) / 1000 * SUBTITLE_FONT_SIZE;
                contentStream.newLineAtOffset(centerX - companyWidth / 2, yPosition);
                contentStream.showText(company.getName());
                contentStream.endText();
                
                yPosition -= 20;
                
                if (company.getRegistrationNumber() != null) {
                    String regText = "Reg No: " + company.getRegistrationNumber();
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), NORMAL_FONT_SIZE);
                    float regWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(regText) / 1000 * NORMAL_FONT_SIZE;
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
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), SUBTITLE_FONT_SIZE);
                float periodWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(periodText) / 1000 * SUBTITLE_FONT_SIZE;
                contentStream.newLineAtOffset(centerX - periodWidth / 2, yPosition - 15);
                contentStream.showText(periodText);
                contentStream.endText();
                
                yPosition -= 35;
            }
            
            // Generation date
            String genDateText = "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), NORMAL_FONT_SIZE);
            float genWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(genDateText) / 1000 * NORMAL_FONT_SIZE;
            contentStream.newLineAtOffset(centerX - genWidth / 2, yPosition);
            contentStream.showText(genDateText);
            contentStream.endText();
            
        } finally {
            contentStream.close();
        }
    }
    
    /**
     * Adds the main report content from text
     */
    private void addReportContent(PDDocument document, String textContent, String reportName,
                                  Company company, FiscalPeriod fiscalPeriod) throws IOException {
        // Split content into lines
        String[] lines = textContent.split("\n");
        
        PDPage currentPage = new PDPage(PDRectangle.A4);
        document.addPage(currentPage);
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);
        
        float yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
        String companyName = company != null ? company.getName() : "FIN Report";
        String periodName = fiscalPeriod != null ? fiscalPeriod.getPeriodName() : "Unknown";
        
        try {
            for (String line : lines) {
                // Check if we need a new page
                if (yPosition < MARGIN_BOTTOM + LINE_HEIGHT) {
                    contentStream.close();
                    
                    // Add page number footer
                    addFooter(document, currentPage, document.getNumberOfPages(), companyName, reportName, periodName);
                    
                    // Create new page
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
                }
                
                // Determine line type and format accordingly
                PDType1Font font;
                float fontSize;
                
                if (isHeaderLine(line)) {
                    font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontSize = HEADER_FONT_SIZE;
                } else if (isTotalLine(line)) {
                    font = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                    fontSize = HEADER_FONT_SIZE;
                } else {
                    font = new PDType1Font(Standard14Fonts.FontName.COURIER);
                    fontSize = NORMAL_FONT_SIZE;
                }
                
                // Add line to PDF
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.newLineAtOffset(MARGIN_LEFT, yPosition);
                
                // Truncate line if too long
                String displayLine = line.length() > 100 ? line.substring(0, 100) : line;
                contentStream.showText(displayLine);
                contentStream.endText();
                
                yPosition -= LINE_HEIGHT;
            }
            
        } finally {
            contentStream.close();
            
            // Add footer to last page
            addFooter(document, currentPage, document.getNumberOfPages(), companyName, reportName, periodName);
        }
    }
    
    /**
     * Adds footer to a page
     */
    private void addFooter(PDDocument document, PDPage page, int pageNumber, 
                          String companyName, String reportName, String periodName) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page, 
            PDPageContentStream.AppendMode.APPEND, true, true);
        
        try {
            String footerText = String.format("Page %d | Generated: %s | FIN Financial Management System",
                pageNumber,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            float centerX = PDRectangle.A4.getWidth() / 2;
            float footerWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(footerText) / 1000 * FOOTER_FONT_SIZE;
            
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FOOTER_FONT_SIZE);
            contentStream.newLineAtOffset(centerX - footerWidth / 2, MARGIN_BOTTOM - 20);
            contentStream.showText(footerText);
            contentStream.endText();
            
        } finally {
            contentStream.close();
        }
    }
    
    /**
     * Determines if a line is a header line (typically contains uppercase or key terms)
     */
    private boolean isHeaderLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = line.trim();
        
        // Check for common header patterns
        return trimmed.matches("^[A-Z][A-Z\\s]+:.*") || // All caps with colon
               trimmed.matches("^[A-Z][a-zA-Z\\s]+Report.*") || 
               trimmed.matches("^Period:.*") ||
               trimmed.matches("^Company:.*") ||
               trimmed.matches("^Date.*:.*") ||
               trimmed.matches("^Account.*") ||
               (trimmed.startsWith("===") && trimmed.endsWith("==="));
    }
    
    /**
     * Determines if a line contains totals (typically has "Total" or "Balance" keywords)
     */
    private boolean isTotalLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return false;
        }
        
        String lower = line.toLowerCase();
        return lower.contains("total") || 
               lower.contains("balance") ||
               lower.contains("subtotal") ||
               lower.contains("grand total") ||
               lower.matches(".*net (income|loss|profit).*");
    }
    
    /**
     * Gets display name for report type
     */
    private String getReportDisplayName(String reportType) {
        return switch (reportType) {
            case "cashbook" -> "Cash Book";
            case "general_ledger" -> "General Ledger";
            case "trial_balance" -> "Trial Balance";
            case "income_statement" -> "Income Statement";
            case "balance_sheet" -> "Balance Sheet";
            case "cash_flow_statement" -> "Cash Flow Statement";
            case "audit_trail" -> "Audit Trail";
            default -> "Financial Report";
        };
    }
}
