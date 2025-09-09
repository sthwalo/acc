package fin.app;

import fin.service.ReportService;
import fin.service.CsvImportService;
import fin.service.CompanyService;
import fin.model.Company;
import fin.model.FiscalPeriod;
import fin.config.DatabaseConfig;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Enhanced PDF Export Service that generates comprehensive PDF reports
 * matching the TXT report format with proper PDF formatting
 */
public class EnhancedPdfExporter {
    
    private ReportService reportService;
    private CompanyService companyService;
    private CsvImportService csvImportService;
    private String dbUrl;
    
    // PDF formatting constants
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
    private static final Font SUBHEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    
    public EnhancedPdfExporter() {
        if (!DatabaseConfig.testConnection()) {
            throw new RuntimeException("Failed to connect to database");
        }
        
        this.dbUrl = DatabaseConfig.getDatabaseUrl();
        this.companyService = new CompanyService(dbUrl);
        this.csvImportService = new CsvImportService(dbUrl, companyService);
        this.reportService = new ReportService(dbUrl, csvImportService);
    }
    
    public void generateComprehensivePdfReport(String filename) throws DocumentException, IOException {
        System.out.println("📄 ENHANCED PDF FINANCIAL REPORT GENERATOR");
        System.out.println("==========================================");
        
        // Get company and fiscal period data
        List<Company> companies = companyService.getAllCompanies();
        if (companies.isEmpty()) {
            throw new RuntimeException("No companies found in database");
        }
        
        Company company = companies.get(0);
        List<FiscalPeriod> periods = companyService.getFiscalPeriodsByCompany(company.getId());
        if (periods.isEmpty()) {
            throw new RuntimeException("No fiscal periods found for company");
        }
        
        FiscalPeriod period = periods.get(0);
        
        System.out.println("🏢 Company: " + company.getName());
        System.out.println("📅 Period: " + period.getPeriodName());
        System.out.println("📄 Creating enhanced PDF file...");
        
        // Create PDF document
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
        
        // Add header and footer
        HeaderFooterPageEvent headerFooter = new HeaderFooterPageEvent(company.getName(), period.getPeriodName());
        writer.setPageEvent(headerFooter);
        
        document.open();
        
        // Add title page
        addTitlePage(document, company, period);
        document.newPage();
        
        // Add table of contents
        addTableOfContents(document);
        document.newPage();
        
        // Add each report section
        addCashbookReport(document, period);
        document.newPage();
        
        addGeneralLedgerReport(document, period);
        document.newPage();
        
        addTrialBalanceReport(document, period);
        document.newPage();
        
        addIncomeStatementReport(document, period);
        document.newPage();
        
        addBalanceSheetReport(document, period);
        document.newPage();
        
        addTransactionSummary(document, period);
        
        document.close();
        
        System.out.println("✅ Enhanced PDF file created successfully: " + filename);
    }
    
    private void addTitlePage(Document document, Company company, FiscalPeriod period) throws DocumentException {
        // Company name
        Paragraph companyName = new Paragraph(company.getName(), TITLE_FONT);
        companyName.setAlignment(Element.ALIGN_CENTER);
        companyName.setSpacingAfter(20);
        document.add(companyName);
        
        // Report title
        Paragraph title = new Paragraph("COMPREHENSIVE FINANCIAL REPORT", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);
        
        // Period information
        Paragraph periodInfo = new Paragraph();
        periodInfo.add(new Chunk("Fiscal Period: ", SUBHEADER_FONT));
        periodInfo.add(new Chunk(period.getPeriodName(), NORMAL_FONT));
        periodInfo.add(Chunk.NEWLINE);
        periodInfo.add(new Chunk("Date Range: ", SUBHEADER_FONT));
        periodInfo.add(new Chunk(period.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                                " - " + period.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        periodInfo.add(Chunk.NEWLINE);
        periodInfo.add(Chunk.NEWLINE);
        periodInfo.add(new Chunk("Generated on: ", SUBHEADER_FONT));
        periodInfo.add(new Chunk(java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), NORMAL_FONT));
        
        periodInfo.setAlignment(Element.ALIGN_CENTER);
        periodInfo.setSpacingAfter(50);
        document.add(periodInfo);
        
        // Disclaimer
        Paragraph disclaimer = new Paragraph();
        disclaimer.add(new Chunk("CONFIDENTIAL FINANCIAL INFORMATION", SUBHEADER_FONT));
        disclaimer.add(Chunk.NEWLINE);
        disclaimer.add(new Chunk("This report contains confidential financial information and is intended for authorized recipients only.", SMALL_FONT));
        disclaimer.setAlignment(Element.ALIGN_CENTER);
        document.add(disclaimer);
    }
    
    private void addTableOfContents(Document document) throws DocumentException {
        Paragraph title = new Paragraph("TABLE OF CONTENTS", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(30);
        document.add(title);
        
        String[] sections = {
            "1. Cashbook Report",
            "2. General Ledger Report", 
            "3. Trial Balance Report",
            "4. Income Statement",
            "5. Balance Sheet",
            "6. Transaction Summary"
        };
        
        for (String section : sections) {
            Paragraph sectionItem = new Paragraph(section, NORMAL_FONT);
            sectionItem.setSpacingAfter(10);
            document.add(sectionItem);
        }
    }
    
    private void addCashbookReport(Document document, FiscalPeriod period) throws DocumentException {
        // Section title
        Paragraph title = new Paragraph("CASHBOOK REPORT", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Get report data from ReportService
        String cashbookReport = reportService.generateCashbookReport(period.getId());
        
        // Convert text report to formatted PDF content
        addFormattedTextReport(document, cashbookReport);
    }
    
    private void addGeneralLedgerReport(Document document, FiscalPeriod period) throws DocumentException {
        Paragraph title = new Paragraph("GENERAL LEDGER REPORT", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        String generalLedgerReport = reportService.generateGeneralLedgerReport(period.getId());
        addFormattedTextReport(document, generalLedgerReport);
    }
    
    private void addTrialBalanceReport(Document document, FiscalPeriod period) throws DocumentException {
        Paragraph title = new Paragraph("TRIAL BALANCE REPORT", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        String trialBalanceReport = reportService.generateTrialBalanceReport(period.getId());
        addFormattedTextReport(document, trialBalanceReport);
    }
    
    private void addIncomeStatementReport(Document document, FiscalPeriod period) throws DocumentException {
        Paragraph title = new Paragraph("INCOME STATEMENT", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        String incomeStatement = reportService.generateIncomeStatementReport(period.getId());
        addFormattedTextReport(document, incomeStatement);
    }
    
    private void addBalanceSheetReport(Document document, FiscalPeriod period) throws DocumentException {
        Paragraph title = new Paragraph("BALANCE SHEET", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        String balanceSheet = reportService.generateBalanceSheetReport(period.getId());
        addFormattedTextReport(document, balanceSheet);
    }
    
    private void addTransactionSummary(Document document, FiscalPeriod period) throws DocumentException {
        Paragraph title = new Paragraph("TRANSACTION SUMMARY", HEADER_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);
        
        // Get transaction data
        List<fin.model.BankTransaction> transactions = csvImportService.getTransactionsByFiscalPeriod(period.getId());
        
        Paragraph summary = new Paragraph();
        summary.add(new Chunk("Total Transactions: ", SUBHEADER_FONT));
        summary.add(new Chunk(String.valueOf(transactions.size()), NORMAL_FONT));
        summary.add(Chunk.NEWLINE);
        
        java.math.BigDecimal totalDebits = transactions.stream()
            .filter(t -> t.getDebitAmount() != null)
            .map(fin.model.BankTransaction::getDebitAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            
        java.math.BigDecimal totalCredits = transactions.stream()
            .filter(t -> t.getCreditAmount() != null)
            .map(fin.model.BankTransaction::getCreditAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        summary.add(new Chunk("Total Debits: ", SUBHEADER_FONT));
        summary.add(new Chunk(String.format("R %,.2f", totalDebits.doubleValue()), NORMAL_FONT));
        summary.add(Chunk.NEWLINE);
        
        summary.add(new Chunk("Total Credits: ", SUBHEADER_FONT));
        summary.add(new Chunk(String.format("R %,.2f", totalCredits.doubleValue()), NORMAL_FONT));
        summary.add(Chunk.NEWLINE);
        
        summary.add(new Chunk("Net Position: ", SUBHEADER_FONT));
        summary.add(new Chunk(String.format("R %,.2f", totalCredits.subtract(totalDebits).doubleValue()), NORMAL_FONT));
        
        document.add(summary);
    }
    
    private void addFormattedTextReport(Document document, String textReport) throws DocumentException {
        // Split report into lines and format appropriately
        String[] lines = textReport.split("\n");
        
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                document.add(new Paragraph(" ", SMALL_FONT)); // Empty line
                continue;
            }
            
            Paragraph paragraph;
            
            // Check if line is a header (starts with capital letters or contains "===")
            if (line.contains("===") || line.matches("^[A-Z ]+$")) {
                paragraph = new Paragraph(line, SUBHEADER_FONT);
            } else if (line.startsWith("Account:") || line.startsWith("Company:") || line.startsWith("Period:")) {
                paragraph = new Paragraph(line, SUBHEADER_FONT);
            } else if (line.contains("TOTALS") || line.contains("Balance")) {
                paragraph = new Paragraph(line, SUBHEADER_FONT);
            } else {
                // Use monospace font for tabular data to preserve alignment
                Font monoFont = new Font(Font.FontFamily.COURIER, 9, Font.NORMAL);
                paragraph = new Paragraph(line, monoFont);
            }
            
            paragraph.setSpacingAfter(2);
            document.add(paragraph);
        }
    }
    
    // Inner class for header/footer
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private String companyName;
        private String periodName;
        
        public HeaderFooterPageEvent(String companyName, String periodName) {
            this.companyName = companyName;
            this.periodName = periodName;
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            // Header
            Phrase header = new Phrase(companyName + " - Financial Reports", SMALL_FONT);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, header, 
                (document.right() - document.left()) / 2 + document.leftMargin(), 
                document.top() + 10, 0);
            
            // Footer
            Phrase footer = new Phrase("Page " + document.getPageNumber() + " - " + periodName, SMALL_FONT);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                (document.right() - document.left()) / 2 + document.leftMargin(),
                document.bottom() - 10, 0);
        }
    }
    
    public static void main(String[] args) {
        try {
            EnhancedPdfExporter exporter = new EnhancedPdfExporter();
            String filename = "../reports/generated/enhanced_financial_report_" + 
                java.time.LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            exporter.generateComprehensivePdfReport(filename);
        } catch (Exception e) {
            System.err.println("❌ Error generating PDF report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
