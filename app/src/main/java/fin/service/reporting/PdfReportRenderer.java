package fin.service.reporting;

import fin.dto.AuditTrailDTO;
import fin.dto.AuditTrailLineDTO;
import fin.entity.Company;
import fin.entity.FiscalPeriod;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper for rendering structured reports as styled PDFs (tables, headers, footers) using PDFBox.
 */
public final class PdfReportRenderer {

    private static final float MARGIN = 50f;
    private static final float TITLE_FONT_SIZE = 14f;
    private static final float HEADER_FONT_SIZE = 10f;
    private static final float BODY_FONT_SIZE = 9f;
    private static final float LINE_SPACING = 1.2f;

    private PdfReportRenderer() {}

    public static byte[] renderAuditTrail(List<AuditTrailDTO> entries, Company company, FiscalPeriod period) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // Add document metadata and cover page consistent with other exporters
            addMetadata(document, company, period);

            PDPage titlePage = new PDPage(PDRectangle.A4);
            addTitlePage(document, titlePage, company, period, "AUDIT TRAIL");

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont bodyFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float pageWidth = page.getMediaBox().getWidth();
            float usableWidth = pageWidth - 2 * MARGIN;

            // Column widths (adjustable)
            float codeW = 60f;
            float accountW = 120f;
            float debitW = 80f;
            float creditW = 80f;
            float spacing = 8f;
            float descW = usableWidth - (codeW + accountW + debitW + creditW + spacing * 3);

            PDPageContentStream cs = new PDPageContentStream(document, page);

            float yStart = page.getMediaBox().getHeight() - MARGIN;
            float yPosition = yStart;

            // Title and company header on content page (smaller than cover)
            cs.setFont(titleFont, TITLE_FONT_SIZE);
            String title = "AUDIT TRAIL";
            centerText(cs, title, titleFont, TITLE_FONT_SIZE, page.getMediaBox().getWidth());
            yPosition -= TITLE_FONT_SIZE * 1.5f;

            cs.setFont(bodyFont, HEADER_FONT_SIZE);
            String companyLine = company.getName() + " - " + period.getPeriodName();
            cs.beginText();
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(companyLine);
            cs.endText();
            yPosition -= HEADER_FONT_SIZE * 1.5f;

            // Generated timestamp
            String generated = "Generated: " + DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").format(java.time.LocalDateTime.now());
            cs.beginText();
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(generated);
            cs.endText();
            yPosition -= HEADER_FONT_SIZE * 1.8f;

            // Table header
            cs.setFont(headerFont, HEADER_FONT_SIZE);
            if (yPosition - (HEADER_FONT_SIZE * LINE_SPACING * 2) < MARGIN) {
                cs.close();
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                cs = new PDPageContentStream(document, page);
                cs.setFont(headerFont, HEADER_FONT_SIZE);
                yPosition = yStart;
            }

            cs.beginText();
            cs.newLineAtOffset(MARGIN, yPosition);
            cs.showText(padRight("Code", (int) codeW / 6) + padRight("Account", (int) accountW / 6) + padRight("Description", (int) descW / 6) + padLeft("Debit", (int) debitW / 6) + padLeft("Credit", (int) creditW / 6));
            cs.endText();

            yPosition -= HEADER_FONT_SIZE * LINE_SPACING * 1.8f;

            cs.setFont(bodyFont, BODY_FONT_SIZE);
            float bodyLineHeight = BODY_FONT_SIZE * LINE_SPACING;

            for (AuditTrailDTO entry : entries) {
                // Entry header
                if (yPosition - (BODY_FONT_SIZE * 4.5f) < MARGIN) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    cs = new PDPageContentStream(document, page);
                    cs.setFont(bodyFont, BODY_FONT_SIZE);
                    yPosition = yStart;
                }
                cs.beginText();
                cs.newLineAtOffset(MARGIN, yPosition);
                cs.showText("ENTRY: " + entry.getReference());
                cs.endText();
                yPosition -= BODY_FONT_SIZE * LINE_SPACING;

                cs.beginText();
                cs.newLineAtOffset(MARGIN, yPosition);
                cs.showText("DATE: " + entry.getEntryDate().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "  CREATED BY: " + entry.getCreatedBy());
                cs.endText();
                yPosition -= BODY_FONT_SIZE * LINE_SPACING;

                cs.beginText();
                cs.newLineAtOffset(MARGIN, yPosition);
                cs.showText("DESCRIPTION: " + (entry.getDescription() == null ? "" : entry.getDescription()));
                cs.endText();
                yPosition -= BODY_FONT_SIZE * LINE_SPACING;

                // Table of lines
                cs.setFont(bodyFont, BODY_FONT_SIZE);

                for (AuditTrailLineDTO line : entry.getLines()) {
                    // Wrap description
                    List<String> wrapped = wrapText(bodyFont, line.getDescription() == null ? "" : line.getDescription(), BODY_FONT_SIZE, descW);
                    int lineCount = Math.max(wrapped.size(), 1);
                    if (yPosition - (bodyLineHeight * lineCount) < MARGIN) {
                        cs.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        cs = new PDPageContentStream(document, page);
                        cs.setFont(bodyFont, BODY_FONT_SIZE);
                        yPosition = yStart;
                    }

                    // First line with amounts
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, yPosition);
                    cs.showText(padRight(line.getAccountCode(), (int) codeW / 6));
                    cs.showText(padRight(truncate(line.getAccountName(), 24), (int) accountW / 6));
                    cs.showText(padRight(wrapped.get(0), (int) descW / 6));
                    cs.showText(padLeft(line.getDebit() != null ? formatCurrency(line.getDebit()) : "", (int) debitW / 6));
                    cs.showText(padLeft(line.getCredit() != null ? formatCurrency(line.getCredit()) : "", (int) creditW / 6));
                    cs.endText();
                    yPosition -= bodyLineHeight;

                    // Subsequent wrapped lines
                    for (int i = 1; i < wrapped.size(); i++) {
                        if (yPosition - bodyLineHeight < MARGIN) {
                            cs.close();
                            page = new PDPage(PDRectangle.A4);
                            document.addPage(page);
                            cs = new PDPageContentStream(document, page);
                            cs.setFont(bodyFont, BODY_FONT_SIZE);
                            yPosition = yStart;
                        }
                        cs.beginText();
                        cs.newLineAtOffset(MARGIN + codeW + accountW + spacing, yPosition);
                        cs.showText(wrapped.get(i));
                        cs.endText();
                        yPosition -= bodyLineHeight;
                    }
                }

                // Spacer after entry
                yPosition -= BODY_FONT_SIZE * LINE_SPACING * 0.9f;
            }

            // Footer with page number etc. — add footer to all pages
            addFooterToAllPages(document, company, period);

            cs.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void centerText(PDPageContentStream cs, String text, PDFont font, float fontSize, float pageWidth) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float startX = (pageWidth - textWidth) / 2f;
        float y = PDRectangle.A4.getHeight() - MARGIN - fontSize;
        cs.beginText();
        cs.newLineAtOffset(startX, y);
        cs.showText(text);
        cs.endText();
    }

    private static List<String> wrapText(PDFont font, String text, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String w : words) {
            String test = current.length() == 0 ? w : current + " " + w;
            float width = font.getStringWidth(test) / 1000 * fontSize;
            if (width <= maxWidth) {
                if (current.length() > 0) current.append(" ");
                current.append(w);
            } else {
                if (current.length() > 0) lines.add(current.toString());
                current = new StringBuilder(w);
            }
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }


    private static void addFooterToAllPages(PDDocument document, Company company, FiscalPeriod period) throws IOException {
        int pageCount = document.getNumberOfPages();
        for (int i = 0; i < pageCount; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream footerStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true)) {
                footerStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
                String generated = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String footerText = String.format("Page %d | Generated: %s | FIN Financial Management System", i + 1, generated);
                float footerWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(footerText) / 1000 * 8;
                float centerX = PDRectangle.A4.getWidth() / 2;
                footerStream.beginText();
                footerStream.newLineAtOffset(centerX - footerWidth / 2, 20);
                footerStream.showText(footerText);
                footerStream.endText();
            }
        }
    }

    private static String padRight(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return s + " ".repeat(width - s.length());
    }

    private static void addMetadata(PDDocument document, Company company, FiscalPeriod period) {
        org.apache.pdfbox.pdmodel.PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle("Audit Trail - " + (company != null ? company.getName() : "FIN Report"));
        info.setSubject("Audit Trail for " + (period != null ? period.getPeriodName() : ""));
        info.setKeywords("audit, audit-trail, report");
        info.setAuthor("FIN Financial Management System");
        info.setCreator("FIN Application v1.0");
    }

    private static void addTitlePage(PDDocument document, PDPage page, Company company, FiscalPeriod period, String title) throws IOException {
        document.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
            float yPosition = PDRectangle.A4.getHeight() - 100;
            float centerX = PDRectangle.A4.getWidth() / 2;

            // Title
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
            float titleWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(title) / 1000 * 20;
            cs.newLineAtOffset(centerX - titleWidth / 2, yPosition);
            cs.showText(title);
            cs.endText();

            yPosition -= 50;

            // Company name
            if (company != null) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                String companyName = company.getName();
                float companyWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(companyName) / 1000 * 14;
                cs.newLineAtOffset(centerX - companyWidth / 2, yPosition);
                cs.showText(companyName);
                cs.endText();

                yPosition -= 25;

                if (company.getRegistrationNumber() != null) {
                    String regText = "Registration Number: " + company.getRegistrationNumber();
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    float regWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(regText) / 1000 * 12;
                    cs.newLineAtOffset(centerX - regWidth / 2, yPosition);
                    cs.showText(regText);
                    cs.endText();

                    yPosition -= 20;
                }
            }

            // Fiscal period info
            if (period != null) {
                String periodText = String.format("Period: %s (%s to %s)",
                    period.getPeriodName(),
                    period.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    period.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"))
                );

                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                float periodWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD).getStringWidth(periodText) / 1000 * 12;
                cs.newLineAtOffset(centerX - periodWidth / 2, yPosition - 15);
                cs.showText(periodText);
                cs.endText();

                yPosition -= 40;
            }

            // Generation date
            String genDateText = "Generated: " + java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
            float genWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(genDateText) / 1000 * 10;
            cs.newLineAtOffset(centerX - genWidth / 2, yPosition);
            cs.showText(genDateText);
            cs.endText();
        }
    }

    private static String padLeft(String s, int width) {
        if (s == null) s = "";
        if (s.length() >= width) return s.substring(0, width);
        return " ".repeat(width - s.length()) + s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max - 3) + "...";
    }

    private static String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null) return "";
        java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("en-ZA"));
        return nf.format(amount);
    }
}