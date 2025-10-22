package fin.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import fin.model.BankTransaction;
import fin.model.Company;
import fin.model.FiscalPeriod;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting data to PDF format
 */
public class PdfExportService {
    
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
    private static final Font HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
    private static final Font NORMAL_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL);
    
    // Table layout constants
    private static final float[] TABLE_COLUMN_WIDTHS = {2f, 5f, 2.5f, 2.5f, 2.5f, 1.5f, 3f, 3f};
    private static final float TABLE_WIDTH_PERCENTAGE = 100f;
    private static final float SUMMARY_TABLE_WIDTH_PERCENTAGE = 50f;
    private static final float CELL_PADDING = 5f;
    
    // Page layout constants
    private static final float HEADER_MARGIN_TOP = 10f;
    private static final float FOOTER_MARGIN_BOTTOM = 10f;
    
    /**
     * Exports transaction data to PDF
     * 
     * @param transactions List of transactions to export
     * @param company Company information
     * @param fiscalPeriod Fiscal period information
     * @return Path to the generated PDF file
     */
    public String exportTransactionsToPdf(List<BankTransaction> transactions, Company company, FiscalPeriod fiscalPeriod) {
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions to export");
        }
        
        // Create reports directory if it doesn't exist
        Path reportsDir = Paths.get("reports");
        try {
            if (!Files.exists(reportsDir)) {
                Files.createDirectory(reportsDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create reports directory", e);
        }
        
        // Generate filename based on company and fiscal period
        String filename = String.format("reports/transactions_%s_%s_%s.pdf", 
                company.getName().replaceAll("\\s+", "_").toLowerCase(),
                fiscalPeriod.getPeriodName().replaceAll("\\s+", "_").toLowerCase(),
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        
        try {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
            
            // Add header and footer
            writer.setPageEvent(new HeaderFooterPageEvent(company.getName(), fiscalPeriod.getPeriodName()));
            
            document.open();
            addDocumentMetadata(document, company, fiscalPeriod);
            addTitlePage(document, company, fiscalPeriod);
            addTransactionsTable(document, transactions);
            addSummary(document, transactions);
            document.close();
            
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }
    
    private void addDocumentMetadata(Document document, Company company, FiscalPeriod fiscalPeriod) {
        document.addTitle("Transaction Report - " + company.getName());
        document.addSubject("Financial Transactions for " + fiscalPeriod.getPeriodName());
        document.addKeywords("transactions, finance, report");
        document.addAuthor("FIN Application");
        document.addCreator("FIN Application");
    }
    
    private void addTitlePage(Document document, Company company, FiscalPeriod fiscalPeriod) throws DocumentException {
        Paragraph title = new Paragraph("Transaction Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph companyInfo = new Paragraph("Company: " + company.getName(), HEADER_FONT);
        companyInfo.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph fiscalPeriodInfo = new Paragraph(
                "Fiscal Period: " + fiscalPeriod.getPeriodName() + 
                " (" + fiscalPeriod.getStartDate() + " to " + fiscalPeriod.getEndDate() + ")",
                HEADER_FONT);
        fiscalPeriodInfo.setAlignment(Element.ALIGN_CENTER);
        
        Paragraph dateInfo = new Paragraph(
                "Report Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                NORMAL_FONT);
        dateInfo.setAlignment(Element.ALIGN_CENTER);
        
        document.add(title);
        document.add(Chunk.NEWLINE);
        document.add(companyInfo);
        document.add(fiscalPeriodInfo);
        document.add(Chunk.NEWLINE);
        document.add(dateInfo);
        document.add(Chunk.NEWLINE);
        document.add(Chunk.NEWLINE);
    }
    
    private void addTransactionsTable(Document document, List<BankTransaction> transactions) throws DocumentException {
        PdfPTable table = new PdfPTable(TABLE_COLUMN_WIDTHS);
        table.setWidthPercentage(TABLE_WIDTH_PERCENTAGE);
        
        // Add table headers
        addTableHeader(table);
        
        // Add table data
        for (BankTransaction transaction : transactions) {
            addTableRow(table, transaction);
        }
        
        document.add(table);
    }
    
    private void addTableHeader(PdfPTable table) {
        String[] headers = {"Date", "Details", "Debit", "Credit", "Balance", "Fee", "Account", "Source"};
        for (String columnTitle : headers) {
            PdfPCell header = new PdfPCell();
            header.setBackgroundColor(BaseColor.LIGHT_GRAY);
            header.setBorderWidth(1);
            header.setPhrase(new Phrase(columnTitle, HEADER_FONT));
            header.setHorizontalAlignment(Element.ALIGN_CENTER);
            header.setVerticalAlignment(Element.ALIGN_MIDDLE);
            header.setPadding(CELL_PADDING);
            table.addCell(header);
        }
    }
    
    private void addTableRow(PdfPTable table, BankTransaction transaction) {
        // Date
        PdfPCell dateCell = new PdfPCell(new Phrase(
                transaction.getTransactionDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                NORMAL_FONT));
        dateCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(dateCell);
        
        // Details - Use the details field directly, no need to clean up account numbers
        String details = transaction.getDetails();
        if (details == null) {
            details = "";
        }
        PdfPCell detailsCell = new PdfPCell(new Phrase(details, NORMAL_FONT));
        table.addCell(detailsCell);
        
        // Debit
        PdfPCell debitCell = new PdfPCell(new Phrase(
                formatAmount(transaction.getDebitAmount()),
                NORMAL_FONT));
        debitCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(debitCell);
        
        // Credit
        PdfPCell creditCell = new PdfPCell(new Phrase(
                formatAmount(transaction.getCreditAmount()),
                NORMAL_FONT));
        creditCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(creditCell);
        
        // Balance
        PdfPCell balanceCell = new PdfPCell(new Phrase(
                formatAmount(transaction.getBalance()),
                NORMAL_FONT));
        balanceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(balanceCell);
        
        // Service Fee
        PdfPCell feeCell = new PdfPCell(new Phrase(
                transaction.isServiceFee() ? "Y" : "N",
                NORMAL_FONT));
        feeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(feeCell);
        
        // Account Number - Use the dedicated accountNumber field
        String accountNumber = transaction.getAccountNumber() != null ? transaction.getAccountNumber() : "";
        PdfPCell accountCell = new PdfPCell(new Phrase(accountNumber, NORMAL_FONT));
        accountCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(accountCell);
        
        // Source File
        PdfPCell sourceCell = new PdfPCell(new Phrase(
                transaction.getSourceFile() != null ? transaction.getSourceFile() : "",
                NORMAL_FONT));
        sourceCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(sourceCell);
    }
    
    private String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return String.format("%,.2f", amount);
    }
    
    private void addSummary(Document document, List<BankTransaction> transactions) throws DocumentException {
        document.add(Chunk.NEWLINE);
        
        Paragraph summaryTitle = new Paragraph("Transaction Summary", HEADER_FONT);
        document.add(summaryTitle);
        document.add(Chunk.NEWLINE);
        
        // Calculate totals
        BigDecimal totalDebits = transactions.stream()
                .filter(t -> t.getDebitAmount() != null)
                .map(BankTransaction::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalCredits = transactions.stream()
                .filter(t -> t.getCreditAmount() != null)
                .map(BankTransaction::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Create summary table
        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(SUMMARY_TABLE_WIDTH_PERCENTAGE);
        summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);
        
        // Total transactions
        PdfPCell labelCell = new PdfPCell(new Phrase("Total Transactions:", HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        summaryTable.addCell(labelCell);
        
        PdfPCell valueCell = new PdfPCell(new Phrase(String.valueOf(transactions.size()), NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(valueCell);
        
        // Total debits
        labelCell = new PdfPCell(new Phrase("Total Debits:", HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        summaryTable.addCell(labelCell);
        
        valueCell = new PdfPCell(new Phrase(formatAmount(totalDebits), NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(valueCell);
        
        // Total credits
        labelCell = new PdfPCell(new Phrase("Total Credits:", HEADER_FONT));
        labelCell.setBorder(Rectangle.NO_BORDER);
        summaryTable.addCell(labelCell);
        
        valueCell = new PdfPCell(new Phrase(formatAmount(totalCredits), NORMAL_FONT));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summaryTable.addCell(valueCell);
        
        document.add(summaryTable);
    }
    
    /**
     * Inner class to handle headers and footers on each page
     */
    private static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String companyName;
        private final String fiscalPeriodName;
        
        public HeaderFooterPageEvent(String valueCompanyName, String valueFiscalPeriodName) {
            this.companyName = valueCompanyName;
            this.fiscalPeriodName = valueFiscalPeriodName;
        }
        
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            
            // Header
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("Company: " + companyName, SMALL_FONT),
                    document.leftMargin(), document.top() + HEADER_MARGIN_TOP, 0);
            
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Fiscal Period: " + fiscalPeriodName, SMALL_FONT),
                    document.right(), document.top() + HEADER_MARGIN_TOP, 0);
            
            // Footer - page number
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase(String.format("Page %d", writer.getPageNumber()), SMALL_FONT),
                    (document.right() - document.left()) / 2 + document.leftMargin(),
                    document.bottom() - FOOTER_MARGIN_BOTTOM, 0);
        }
    }
}
