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

package fin.controller.spring;

import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.service.spring.BankStatementProcessingService;
import fin.service.spring.SpringTransactionClassificationService;
import fin.repository.FiscalPeriodRepository;
import fin.service.spring.SpringCompanyService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spring REST Controller for import operations (bank statements, transaction processing).
 */
@RestController
@RequestMapping("/api/v1/import")
public class SpringImportController {

    private final BankStatementProcessingService bankStatementService;
    private final SpringTransactionClassificationService classificationService;
    private final SpringCompanyService companyService;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public SpringImportController(BankStatementProcessingService bankStatementService,
                                SpringTransactionClassificationService classificationService,
                                SpringCompanyService companyService,
                                FiscalPeriodRepository fiscalPeriodRepository) {
        this.bankStatementService = bankStatementService;
        this.classificationService = classificationService;
        this.companyService = companyService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Upload and process a bank statement PDF
     */
    @PostMapping("/bank-statement")
    public ResponseEntity<BankStatementProcessingService.StatementProcessingResult> processBankStatement(
            @RequestParam("file") MultipartFile file,
            @RequestParam("companyId") Long companyId,
            @RequestParam("fiscalPeriodId") Long fiscalPeriodId) {
        try {
            BankStatementProcessingService.StatementProcessingResult result =
                bankStatementService.processStatement(file, companyId, fiscalPeriodId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get transactions for a company and fiscal period
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions")
    public ResponseEntity<List<BankTransaction>> getTransactionsByCompanyAndPeriod(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period - handle null fiscalPeriodId values
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get unclassified transactions for a company
     */
    @GetMapping("/companies/{companyId}/transactions/unclassified")
    public ResponseEntity<List<BankTransaction>> getUnclassifiedTransactions(@PathVariable Long companyId) {
        try {
            List<BankTransaction> transactions = bankStatementService.getUnclassifiedTransactions(companyId);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Classify a single transaction
     */
    @PostMapping("/transactions/{transactionId}/classify")
    public ResponseEntity<BankTransaction> classifyTransaction(@PathVariable Long transactionId,
                                                             @RequestParam Long accountId) {
        try {
            BankTransaction classifiedTransaction = classificationService.classifyTransaction(transactionId, accountId);
            return ResponseEntity.ok(classifiedTransaction);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Auto-classify transactions for a company
     */
    @PostMapping("/companies/{companyId}/transactions/auto-classify")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationResult> autoClassifyTransactions(
            @PathVariable Long companyId) {
        try {
            SpringTransactionClassificationService.ClassificationResult result =
                classificationService.autoClassifyTransactions(companyId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Initialize chart of accounts for a company
     */
    @PostMapping("/chart-of-accounts/company/{companyId}/initialize")
    public ResponseEntity<String> initializeChartOfAccounts(@PathVariable Long companyId) {
        try {
            classificationService.initializeChartOfAccounts(companyId);
            return ResponseEntity.ok("Chart of accounts initialized successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get classification rules for a company
     */
    @GetMapping("/classification-rules/company/{companyId}")
    public ResponseEntity<List<SpringTransactionClassificationService.ClassificationRule>> getClassificationRules(
            @PathVariable Long companyId) {
        try {
            List<SpringTransactionClassificationService.ClassificationRule> rules =
                classificationService.getClassificationRules(companyId);
            return ResponseEntity.ok(rules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Add a classification rule
     */
    @PostMapping("/classification-rules")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationRule> addClassificationRule(
            @RequestBody SpringTransactionClassificationService.ClassificationRule rule) {
        try {
            SpringTransactionClassificationService.ClassificationRule createdRule =
                classificationService.addClassificationRule(rule);
            return ResponseEntity.ok(createdRule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a classification rule
     */
    @PutMapping("/classification-rules/{id}")
    public ResponseEntity<SpringTransactionClassificationService.ClassificationRule> updateClassificationRule(
            @PathVariable Long id,
            @RequestBody SpringTransactionClassificationService.ClassificationRule rule) {
        try {
            SpringTransactionClassificationService.ClassificationRule updatedRule =
                classificationService.updateClassificationRule(id, rule);
            return ResponseEntity.ok(updatedRule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a classification rule
     */
    @DeleteMapping("/classification-rules/{id}")
    public ResponseEntity<Void> deleteClassificationRule(@PathVariable Long id) {
        try {
            classificationService.deleteClassificationRule(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get transaction processing statistics
     */
    @GetMapping("/statistics/company/{companyId}")
    public ResponseEntity<BankStatementProcessingService.ProcessingStatistics> getProcessingStatistics(
            @PathVariable Long companyId) {
        try {
            BankStatementProcessingService.ProcessingStatistics stats =
                bankStatementService.getProcessingStatistics(companyId);
            return ResponseEntity.ok(stats);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Export transactions to PDF
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions/export/pdf")
    public ResponseEntity<ByteArrayResource> exportTransactionsToPdf(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            // Get company and fiscal period information
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                return ResponseEntity.badRequest().build();
            }
            
            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.badRequest().build();
            }

            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();

            byte[] pdfBytes = generateTransactionPdf(transactions, company, fiscalPeriod);

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String filename = String.format("transactions_%s_%s.pdf", 
                company.getName().replaceAll("[^a-zA-Z0-9]", "_"), 
                fiscalPeriod.getPeriodName().replaceAll("[^a-zA-Z0-9]", "_"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export transactions to CSV
     */
    @GetMapping("/companies/{companyId}/fiscal-periods/{fiscalPeriodId}/transactions/export/csv")
    public ResponseEntity<ByteArrayResource> exportTransactionsToCsv(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            // Get company and fiscal period information
            Company company = companyService.getCompanyById(companyId);
            if (company == null) {
                return ResponseEntity.badRequest().build();
            }

            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(fiscalPeriodId).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.badRequest().build();
            }

            List<BankTransaction> transactions = bankStatementService.getTransactionsByCompany(companyId);
            // Filter by fiscal period
            transactions = transactions.stream()
                    .filter(t -> t.getFiscalPeriodId() != null && t.getFiscalPeriodId().equals(fiscalPeriodId))
                    .toList();

            byte[] csvBytes = generateTransactionCsv(transactions, company, fiscalPeriod);

            ByteArrayResource resource = new ByteArrayResource(csvBytes);

            String filename = String.format("transactions_%s_%s.csv",
                company.getName().replaceAll("[^a-zA-Z0-9]", "_"),
                fiscalPeriod.getPeriodName().replaceAll("[^a-zA-Z0-9]", "_"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(csvBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Generate CSV for transactions
     */
    private byte[] generateTransactionCsv(List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) throws IOException {
        StringBuilder csv = new StringBuilder();

        // Add header information
        csv.append("# FIN Financial Management System - Transaction Export\n");
        csv.append("# Company: ").append(company != null ? company.getName() : "Unknown").append("\n");
        csv.append("# Period: ").append(fiscalPeriod != null ? fiscalPeriod.getPeriodName() : "Unknown").append("\n");
        csv.append("# Generated: ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))).append("\n");
        csv.append("# Total Transactions: ").append(transactions.size()).append("\n\n");

        // CSV headers
        csv.append("Date,Details,Debit,Credit,Balance,Reference,Classification\n");

        // Add transaction data
        for (BankTransaction transaction : transactions) {
            String dateStr = transaction.getTransactionDate() != null ?
                transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
            String details = transaction.getDetails() != null ? transaction.getDetails().replace("\"", "\"\"") : "";
            String debitStr = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 ?
                transaction.getDebitAmount().toString() : "";
            String creditStr = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0 ?
                transaction.getCreditAmount().toString() : "";
            String balanceStr = transaction.getBalance() != null ? transaction.getBalance().toString() : "";
            String reference = transaction.getReference() != null ? transaction.getReference().replace("\"", "\"\"") : "";
            String classification = transaction.getCategory() != null ? transaction.getCategory().replace("\"", "\"\"") : "";

            // Escape fields containing commas or quotes
            if (details.contains(",") || details.contains("\"")) {
                details = "\"" + details + "\"";
            }
            if (reference.contains(",") || reference.contains("\"")) {
                reference = "\"" + reference + "\"";
            }
            if (classification.contains(",") || classification.contains("\"")) {
                classification = "\"" + classification + "\"";
            }

            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                dateStr, details, debitStr, creditStr, balanceStr, reference, classification));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Generate PDF for transactions using Apache PDFBox
     */
    private byte[] generateTransactionPdf(List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) throws IOException {
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
            return outputStream.toByteArray();
            
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
        final float[] COLUMN_WIDTHS = {60f, 120f, 80f, 80f, 80f, 60f}; // Date, Details, Debit, Credit, Balance, Ref
        final String[] HEADERS = {"Date", "Details", "Debit", "Credit", "Balance", "Reference"};
        
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
                addTransactionRow(contentStream, transaction, COLUMN_WIDTHS, MARGIN_LEFT, yPosition, FONT_SIZE);
                yPosition -= LINE_HEIGHT;
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
     * Add transaction data row
     */
    private void addTransactionRow(PDPageContentStream contentStream, BankTransaction transaction, 
                                  float[] columnWidths, float startX, float yPosition, float fontSize) throws IOException {
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), fontSize);
        
        float xPosition = startX;
        
        // Date
        String dateStr = transaction.getTransactionDate() != null ? 
            transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(dateStr, 10));
        contentStream.endText();
        xPosition += columnWidths[0];
        
        // Details
        String details = transaction.getDetails() != null ? transaction.getDetails() : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(details, 18));
        contentStream.endText();
        xPosition += columnWidths[1];
        
        // Debit
        String debitStr = transaction.getDebitAmount() != null && transaction.getDebitAmount().compareTo(BigDecimal.ZERO) > 0 ? 
            String.format("%.2f", transaction.getDebitAmount()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(debitStr);
        contentStream.endText();
        xPosition += columnWidths[2];
        
        // Credit
        String creditStr = transaction.getCreditAmount() != null && transaction.getCreditAmount().compareTo(BigDecimal.ZERO) > 0 ? 
            String.format("%.2f", transaction.getCreditAmount()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(creditStr);
        contentStream.endText();
        xPosition += columnWidths[3];
        
        // Balance
        String balanceStr = transaction.getBalance() != null ? 
            String.format("%.2f", transaction.getBalance()) : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(balanceStr);
        contentStream.endText();
        xPosition += columnWidths[4];
        
        // Reference
        String reference = transaction.getReference() != null ? transaction.getReference() : "";
        contentStream.beginText();
        contentStream.newLineAtOffset(xPosition, yPosition);
        contentStream.showText(truncateText(reference, 9));
        contentStream.endText();
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
}