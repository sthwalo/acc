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

package fin.service.reporting;

import fin.entity.Account;
import fin.entity.BankTransaction;
import fin.entity.Company;
import fin.entity.FiscalPeriod;
import fin.entity.JournalEntryLine;
import fin.repository.AccountRepository;
import fin.repository.JournalEntryLineRepository;
import fin.util.SpringDebugger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Spring service for exporting transactions to PDF format with full multiline support
 */
@Service
public class SpringPdfExportService {

    private final JournalEntryLineRepository journalEntryLineRepository;
    private final AccountRepository accountRepository;
    private final SpringDebugger debugger;

    public SpringPdfExportService(
            JournalEntryLineRepository journalEntryLineRepository,
            AccountRepository accountRepository,
            SpringDebugger debugger) {
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.accountRepository = accountRepository;
        this.debugger = debugger;
    }

    /**
     * Export transactions to PDF bytes for a company and fiscal period
     *
     * @param transactions List of transactions to export
     * @param company The company information
     * @param fiscalPeriod The fiscal period information
     * @return PDF content as byte array
     */
    public byte[] exportTransactionsToPdfBytes(List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        debugger.logMethodEntry("SpringPdfExportService", "exportTransactionsToPdfBytes",
            transactions.size(), company != null ? company.getName() : "null", fiscalPeriod != null ? fiscalPeriod.getPeriodName() : "null");

        // Enrich transactions with classification data from journal entries
        if (company != null) {
            enrichTransactionsWithClassification(transactions, company.getId());
        }

        PDDocument document = new PDDocument();

        try {
            // Add metadata
            addMetadata(document, company, fiscalPeriod);

            // Create title page
            PDPage titlePage = new PDPage(PDRectangle.A4);
            document.addPage(titlePage);
            addTitlePage(document, titlePage, company, fiscalPeriod, transactions.size());

            // Add transaction pages
            addTransactionPages(document, transactions, company, fiscalPeriod);

            // Convert to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            byte[] result = outputStream.toByteArray();

            debugger.logMethodExit("SpringPdfExportService", "exportTransactionsToPdfBytes",
                String.format("Generated PDF with %d transactions, %d bytes", transactions.size(), result.length));

            return result;

        } finally {
            document.close();
        }
    }

    /**
     * Add document metadata
     */
    private void addMetadata(PDDocument document, Company company, FiscalPeriod fiscalPeriod) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle("Transaction Report - " + (company != null ? company.getName() : "FIN Report"));
        info.setSubject("Financial Transactions for " + (fiscalPeriod != null ? fiscalPeriod.getPeriodName() : ""));
        info.setKeywords("finance, transactions, report");
        info.setAuthor("FIN Financial Management System");
        info.setCreator("FIN Application v1.0");
    }

    /**
     * Add title page with company and period information
     */
    private void addTitlePage(PDDocument document, PDPage page, Company company, FiscalPeriod fiscalPeriod, int transactionCount) throws IOException {
        PDPageContentStream contentStream = new PDPageContentStream(document, page);

        try {
            float yPosition = PDRectangle.A4.getHeight() - 100;
            float centerX = PDRectangle.A4.getWidth() / 2;

            // Title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
            String title = "FINANCIAL TRANSACTIONS REPORT";
            float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                .getStringWidth(title) / 1000 * 20;
            contentStream.newLineAtOffset(centerX - titleWidth / 2, yPosition);
            contentStream.showText(title);
            contentStream.endText();

            yPosition -= 50;

            // Company information
            if (company != null) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                String companyName = company.getName();
                float companyWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(companyName) / 1000 * 14;
                contentStream.newLineAtOffset(centerX - companyWidth / 2, yPosition);
                contentStream.showText(companyName);
                contentStream.endText();

                yPosition -= 25;

                if (company.getRegistrationNumber() != null) {
                    String regText = "Registration Number: " + company.getRegistrationNumber();
                    contentStream.beginText();
                    contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    float regWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                        .getStringWidth(regText) / 1000 * 12;
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
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                float periodWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    .getStringWidth(periodText) / 1000 * 12;
                contentStream.newLineAtOffset(centerX - periodWidth / 2, yPosition - 15);
                contentStream.showText(periodText);
                contentStream.endText();

                yPosition -= 40;
            }

            // Transaction count
            String countText = String.format("Total Transactions: %d", transactionCount);
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            float countWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(countText) / 1000 * 12;
            contentStream.newLineAtOffset(centerX - countWidth / 2, yPosition);
            contentStream.showText(countText);
            contentStream.endText();

            yPosition -= 30;

            // Generation date
            String genDateText = "Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float genWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
                .getStringWidth(genDateText) / 1000 * 10;
            contentStream.newLineAtOffset(centerX - genWidth / 2, yPosition);
            contentStream.showText(genDateText);
            contentStream.endText();

        } finally {
            contentStream.close();
        }
    }

    /**
     * Add pages with transaction data in table format
     */
    private void addTransactionPages(PDDocument document, List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        final float MARGIN_LEFT = 40f;
        final float MARGIN_TOP = 80f;
        final float MARGIN_BOTTOM = 50f;
        final float LINE_HEIGHT = 15f;
        final float FONT_SIZE = 8f;

        // Column widths (total should be PDRectangle.A4.getWidth() - MARGIN_LEFT - MARGIN_RIGHT)
        final float[] COLUMN_WIDTHS = {30f, 50f, 120f, 60f, 60f, 60f, 70f, 80f}; // ID, Date, Details, Debit, Credit, Balance, Classification, Created At
        final String[] HEADERS = {"ID", "Date", "Details", "Debit", "Credit", "Balance", "Classification", "Created At"};

        PDPage currentPage = new PDPage(PDRectangle.A4);
        document.addPage(currentPage);
        PDPageContentStream contentStream = new PDPageContentStream(document, currentPage);

        float yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
        int pageNumber = 2; // Title page is page 1

        try {
            // Add table header
            addTableHeader(contentStream, HEADERS, COLUMN_WIDTHS, MARGIN_LEFT, yPosition);
            yPosition -= LINE_HEIGHT * 1.5f;

            // Add transactions
            for (int i = 0; i < transactions.size(); i++) {
                BankTransaction transaction = transactions.get(i);

                // Check if we need a new page
                if (yPosition < MARGIN_BOTTOM + LINE_HEIGHT) {
                    // Add footer to current page
                    addPageFooter(contentStream, pageNumber, company, fiscalPeriod);
                    contentStream.close();

                    // Create new page
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yPosition = PDRectangle.A4.getHeight() - MARGIN_TOP;
                    pageNumber++;

                    // Add header to new page
                    addTableHeader(contentStream, HEADERS, COLUMN_WIDTHS, MARGIN_LEFT, yPosition);
                    yPosition -= LINE_HEIGHT * 1.5f;
                }

                // Add transaction row
                float usedHeight = addTransactionRow(contentStream, transaction, COLUMN_WIDTHS, MARGIN_LEFT, yPosition, FONT_SIZE);
                yPosition -= usedHeight;
            }

            // Add footer to last page
            addPageFooter(contentStream, pageNumber, company, fiscalPeriod);

        } finally {
            contentStream.close();
        }
    }

    /**
     * Add table header row
     */
    private void addTableHeader(PDPageContentStream contentStream, String[] headers, float[] columnWidths,
                               float startX, float yPosition) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 9);

        float xPosition = startX;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPosition, yPosition);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPosition += columnWidths[i];
        }

        // Draw header line
        contentStream.moveTo(startX, yPosition - 5);
        contentStream.lineTo(xPosition - columnWidths[columnWidths.length - 1], yPosition - 5);
        contentStream.stroke();
    }

    /**
     * Add transaction data row with multiline details support
     * Returns the height used by this row (for pagination)
     */
    private float addTransactionRow(PDPageContentStream contentStream, BankTransaction transaction,
                                  float[] columnWidths, float startX, float yPosition, float fontSize) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), fontSize);

        float xPosition = startX;
        float maxHeightUsed = fontSize + 2; // Minimum row height

        // ID
        String idStr = String.valueOf(transaction.getId());
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(idStr, 8));
        contentStream.endText();
        xPosition += columnWidths[0];

        // Date
        String dateStr = transaction.getTransactionDate() != null ?
            transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(dateStr, 10));
        contentStream.endText();
        xPosition += columnWidths[1];

        // Details - Full text with multiline support (this can span multiple lines)
        float detailsHeight = addMultilineText(contentStream, transaction.getDetails(), xPosition, yPosition, columnWidths[2], fontSize);
        maxHeightUsed = Math.max(maxHeightUsed, detailsHeight);
        xPosition += columnWidths[2];

        // Debit
        String debitStr = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0 ?
            String.format("%.2f", transaction.getDebitAmount()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(debitStr);
        contentStream.endText();
        xPosition += columnWidths[3];

        // Credit
        String creditStr = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0 ?
            String.format("%.2f", transaction.getCreditAmount()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(creditStr);
        contentStream.endText();
        xPosition += columnWidths[4];

        // Balance
        String balanceStr = transaction.getBalance() != null ?
            String.format("%.2f", transaction.getBalance()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(balanceStr);
        contentStream.endText();
        xPosition += columnWidths[5];

        // Classification - Main Account in [code] name format
        String classification = getMainAccountClassification(transaction);
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(classification, 18));
        contentStream.endText();
        xPosition += columnWidths[6];

        // Created At
        String createdAtStr = transaction.getCreatedAt() != null ?
            transaction.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(createdAtStr, 16));
        contentStream.endText();

        return maxHeightUsed;
    }

    /**
     * Add multiline text within a column width
     * Returns the total height used by the multiline text
     */
    private float addMultilineText(PDPageContentStream contentStream, String text, float xPosition,
                                 float startY, float maxWidth, float fontSize) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return fontSize + 2; // Return minimum height
        }

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.COURIER);
        contentStream.setFont(font, fontSize);

        // Split text into lines based on existing line breaks or wrap long lines
        String[] lines = text.split("\n");
        float currentY = startY;
        float lineHeight = fontSize + 2;
        int totalLines = 0;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                // Handle empty lines
                totalLines++;
                currentY -= lineHeight;
                continue;
            }

            // If line is too long, wrap it
            if (font.getStringWidth(line) / 1000 * fontSize > maxWidth) {
                String[] wrappedLines = wrapText(line, maxWidth, fontSize, font);
                for (String wrappedLine : wrappedLines) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xPosition, currentY);
                    contentStream.showText(wrappedLine);
                    contentStream.endText();
                    currentY -= lineHeight;
                    totalLines++;
                }
            } else {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPosition, currentY);
                contentStream.showText(line);
                contentStream.endText();
                currentY -= lineHeight;
                totalLines++;
            }
        }

        return totalLines * lineHeight;
    }

    /**
     * Wrap text to fit within max width
     */
    private String[] wrapText(String text, float maxWidth, float fontSize, PDType1Font font) throws IOException {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            float width = font.getStringWidth(testLine) / 1000 * fontSize;

            if (width > maxWidth && currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Add page footer
     */
    private void addPageFooter(PDPageContentStream contentStream, int pageNumber, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        float centerX = PDRectangle.A4.getWidth() / 2;
        float footerY = 30f;

        String footerText = String.format("Page %d | Generated: %s | FIN Financial Management System",
            pageNumber,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        contentStream.beginText();
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8);
        float footerWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA)
            .getStringWidth(footerText) / 1000 * 8;
        contentStream.newLineAtOffset(centerX - footerWidth / 2, footerY);
        contentStream.showText(footerText);
        contentStream.endText();
    }

    /**
     * Truncate text to fit in column
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength - 1) + "." : text;
    }

    /**
     * Enrich transactions with classification data from journal entries.
     * Populates the transient debit/credit account fields by querying journal_entry_lines.
     * This matches the pattern used in SpringCompanyController.getTransactions().
     */
    private void enrichTransactionsWithClassification(List<BankTransaction> transactions, Long companyId) {
        for (BankTransaction transaction : transactions) {
            List<JournalEntryLine> journalLines = journalEntryLineRepository.findBySourceTransactionId(transaction.getId());
            
            if (!journalLines.isEmpty()) {
                // For each transaction, we have 2 journal lines (debit and credit)
                // Find the debit line (debit_amount > 0) and credit line (credit_amount > 0)
                for (JournalEntryLine line : journalLines) {
                    Optional<Account> accountOpt = accountRepository.findById(line.getAccountId());
                    
                    if (accountOpt.isPresent()) {
                        Account account = accountOpt.get();
                        
                        if (line.getDebitAmount() != null && line.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            // This is a debit line
                            transaction.setDebitAccountId(account.getId());
                            transaction.setDebitAccountCode(account.getAccountCode());
                            transaction.setDebitAccountName(account.getAccountName());
                        } else if (line.getCreditAmount() != null && line.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                            // This is a credit line
                            transaction.setCreditAccountId(account.getId());
                            transaction.setCreditAccountCode(account.getAccountCode());
                            transaction.setCreditAccountName(account.getAccountName());
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the main account for classification display.
     * Logic: Show the non-cash/non-bank account
     * - Credit transaction (money IN) → show credit account (revenue/income)
     * - Debit transaction (money OUT) → show debit account (expense)
     */
    private String getMainAccountClassification(BankTransaction transaction) {
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            // Credit transaction → main account is the credit account (revenue/income)
            if (transaction.getCreditAccountCode() != null && transaction.getCreditAccountName() != null) {
                return "[" + transaction.getCreditAccountCode() + "] " + transaction.getCreditAccountName();
            }
        } else if (transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            // Debit transaction → main account is the debit account (expense)
            if (transaction.getDebitAccountCode() != null && transaction.getDebitAccountName() != null) {
                return "[" + transaction.getDebitAccountCode() + "] " + transaction.getDebitAccountName();
            }
        }
        // Fallback to original category if not classified
        return transaction.getCategory() != null ? transaction.getCategory() : "Not classified";
    }
}