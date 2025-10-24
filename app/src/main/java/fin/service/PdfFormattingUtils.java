package fin.service;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;

/**
 * PDF Formatting Utilities for FIN Financial Management System
 * Provides professional formatting methods inspired by payslip generation techniques
 * Features table drawing, typography hierarchy, and visual enhancements
 */
public class PdfFormattingUtils {

    // PDF Layout Constants (A4 page)
    public static final float PAGE_WIDTH = 595.28f;
    public static final float PAGE_HEIGHT = 841.89f;
    public static final float MARGIN_LEFT = 50f;
    public static final float MARGIN_RIGHT = 50f;
    public static final float MARGIN_TOP = 50f;
    public static final float MARGIN_BOTTOM = 50f;
    public static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    // Font Sizes (Typography Hierarchy)
    public static final float FONT_SIZE_TITLE = 18f;
    public static final float FONT_SIZE_HEADER = 14f;
    public static final float FONT_SIZE_SUBHEADER = 12f;
    public static final float FONT_SIZE_NORMAL = 11f;
    public static final float FONT_SIZE_SMALL = 10f;
    public static final float FONT_SIZE_TINY = 9f;

    // Vertical Spacing
    public static final float LINE_SPACING_LARGE = 25f;
    public static final float LINE_SPACING_NORMAL = 18f;
    public static final float LINE_SPACING_SMALL = 15f;
    public static final float LINE_SPACING_TINY = 12f;

    // Colors (RGB values 0.0-1.0)
    public static final float[] COLOR_BLACK = {0, 0, 0};
    public static final float[] COLOR_BLUE = {0.2f, 0.4f, 0.8f};
    public static final float[] COLOR_LIGHT_BLUE = {0.9f, 0.95f, 1.0f};
    public static final float[] COLOR_LIGHT_GRAY = {0.9f, 0.9f, 0.9f};
    public static final float[] COLOR_DARK_GRAY = {0.3f, 0.3f, 0.3f};

    /**
     * Draw a professional header section with title and company information
     */
    public static float drawHeaderSection(PDPageContentStream contentStream, String title, String companyName,
                                        String subtitle, float startY) throws IOException {
        float currentY = startY;

        // Draw decorative header border
        contentStream.setLineWidth(2.0f);
        contentStream.setStrokingColor(COLOR_BLUE[0], COLOR_BLUE[1], COLOR_BLUE[2]);
        contentStream.addRect(MARGIN_LEFT, currentY - 140, CONTENT_WIDTH, 140);
        contentStream.stroke();

        // Reset stroke color to black
        contentStream.setStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        contentStream.setLineWidth(1.0f);

        // Title with background
        contentStream.setNonStrokingColor(COLOR_LIGHT_BLUE[0], COLOR_LIGHT_BLUE[1], COLOR_LIGHT_BLUE[2]);
        contentStream.addRect(MARGIN_LEFT + 5, currentY - 35, CONTENT_WIDTH - 10, 30);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
        contentStream.beginText();
        float titleWidth = title.length() * 8; // Approximate width calculation
        float titleX = (PAGE_WIDTH - titleWidth) / 2;
        contentStream.newLineAtOffset(titleX, currentY - 25);
        contentStream.showText(title);
        contentStream.endText();

        currentY -= 50;

        // Company name with underline
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADER);
        contentStream.beginText();
        float companyNameWidth = companyName.length() * 6; // Approximate width
        float companyX = (PAGE_WIDTH - companyNameWidth) / 2;
        contentStream.newLineAtOffset(companyX, currentY);
        contentStream.showText(companyName);
        contentStream.endText();

        // Draw underline
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(companyX, currentY - 3);
        contentStream.lineTo(companyX + companyNameWidth, currentY - 3);
        contentStream.stroke();
        contentStream.setLineWidth(1.0f);

        currentY -= 30;

        // Subtitle if provided
        if (subtitle != null && !subtitle.isEmpty()) {
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_NORMAL);
            contentStream.beginText();
            float subtitleWidth = subtitle.length() * 5; // Approximate width
            float subtitleX = (PAGE_WIDTH - subtitleWidth) / 2;
            contentStream.newLineAtOffset(subtitleX, currentY);
            contentStream.showText(subtitle);
            contentStream.endText();
            currentY -= LINE_SPACING_NORMAL;
        }

        return currentY - 20; // Extra space after header
    }

    /**
     * Draw a section with background, border, and header
     */
    public static float drawSectionHeader(PDPageContentStream contentStream, String sectionTitle,
                                        float startY, float sectionHeight) throws IOException {
        // Section background
        contentStream.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        contentStream.addRect(MARGIN_LEFT, startY - sectionHeight, CONTENT_WIDTH, sectionHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Section border
        contentStream.setLineWidth(1.0f);
        contentStream.addRect(MARGIN_LEFT, startY - sectionHeight, CONTENT_WIDTH, sectionHeight);
        contentStream.stroke();

        // Section header with background
        contentStream.setNonStrokingColor(COLOR_DARK_GRAY[0], COLOR_DARK_GRAY[1], COLOR_DARK_GRAY[2]);
        contentStream.addRect(MARGIN_LEFT + 2, startY - 20, CONTENT_WIDTH - 4, 18);
        contentStream.fill();
        contentStream.setNonStrokingColor(1, 1, 1); // White text on dark background

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADER);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_LEFT + 10, startY - 15);
        contentStream.showText(sectionTitle);
        contentStream.endText();

        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        return startY - 30; // Return Y position for content
    }

    /**
     * Draw a professional table with headers and data
     */
    public static float drawTable(PDPageContentStream contentStream, String[] headers, String[][] data,
                                float startY, float rowHeight) throws IOException {
        float currentY = startY;
        float tableWidth = CONTENT_WIDTH;
        int numColumns = headers.length;

        // Calculate column widths (equal distribution)
        float[] columnWidths = new float[numColumns];
        float columnWidth = tableWidth / numColumns;
        for (int i = 0; i < numColumns; i++) {
            columnWidths[i] = columnWidth;
        }

        // Table border
        float tableHeight = (data.length + 1) * rowHeight; // +1 for header
        contentStream.setLineWidth(1.0f);
        contentStream.addRect(MARGIN_LEFT, currentY - tableHeight, tableWidth, tableHeight);
        contentStream.stroke();

        // Header row with background
        contentStream.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        contentStream.addRect(MARGIN_LEFT + 1, currentY - rowHeight + 1, tableWidth - 2, rowHeight - 2);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Draw headers
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SMALL);
        float xPos = MARGIN_LEFT + 5;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPos, currentY - rowHeight + 5);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPos += columnWidths[i];
        }

        // Header underline
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(MARGIN_LEFT, currentY - rowHeight);
        contentStream.lineTo(MARGIN_LEFT + tableWidth, currentY - rowHeight);
        contentStream.stroke();
        contentStream.setLineWidth(1.0f);

        currentY -= rowHeight;

        // Draw data rows
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        for (String[] row : data) {
            xPos = MARGIN_LEFT + 5;
            for (int i = 0; i < row.length && i < headers.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(xPos, currentY - rowHeight + 5);
                contentStream.showText(row[i] != null ? row[i] : "");
                contentStream.endText();
                xPos += columnWidths[i];
            }
            currentY -= rowHeight;
        }

        return currentY - 10; // Space after table
    }

    /**
     * Draw a summary box with key metrics
     */
    public static float drawSummaryBox(PDPageContentStream contentStream, String[][] metrics,
                                     float startY, float boxHeight) throws IOException {
        float currentY = startY;

        // Summary box background and border
        contentStream.setNonStrokingColor(0.95f, 1.0f, 0.95f); // Light green background
        contentStream.addRect(MARGIN_LEFT, currentY - boxHeight, CONTENT_WIDTH, boxHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        contentStream.setLineWidth(1.5f);
        contentStream.addRect(MARGIN_LEFT, currentY - boxHeight, CONTENT_WIDTH, boxHeight);
        contentStream.stroke();
        contentStream.setLineWidth(1.0f);

        // Draw metrics
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        float metricY = currentY - 20;
        for (String[] metric : metrics) {
            if (metric.length >= 2) {
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT + 20, metricY);
                contentStream.showText(metric[0] + ":");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT + CONTENT_WIDTH - 120, metricY);
                contentStream.showText(metric[1]);
                contentStream.endText();

                metricY -= LINE_SPACING_NORMAL;
            }
        }

        return currentY - boxHeight - 20; // Space after summary box
    }

    /**
     * Draw a professional footer
     */
    public static void drawFooter(PDPageContentStream contentStream, String footerText, String additionalInfo) throws IOException {
        float footerY = MARGIN_BOTTOM + 20;

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE), FONT_SIZE_TINY);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_LEFT + 15, footerY + 5);
        contentStream.showText(footerText);
        contentStream.endText();

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN_LEFT + 15, footerY - 10);
            contentStream.showText(additionalInfo);
            contentStream.endText();
        }

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_LEFT + 15, footerY - 25);
        contentStream.showText("Page 1 of 1");
        contentStream.endText();
    }

    /**
     * Draw a two-column layout section
     */
    public static float drawTwoColumnSection(PDPageContentStream contentStream, String leftTitle, String[] leftItems,
                                           String rightTitle, String[] rightItems, float startY) throws IOException {
        float currentY = startY;
        float columnWidth = (CONTENT_WIDTH - 20) / 2;
        float leftColumnX = MARGIN_LEFT;
        float rightColumnX = MARGIN_LEFT + columnWidth + 20;

        // Section headers
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SUBHEADER);
        contentStream.beginText();
        contentStream.newLineAtOffset(leftColumnX, currentY);
        contentStream.showText(leftTitle);
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(rightColumnX, currentY);
        contentStream.showText(rightTitle);
        contentStream.endText();

        currentY -= LINE_SPACING_NORMAL;

        // Draw items
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        int maxItems = Math.max(leftItems.length, rightItems.length);

        for (int i = 0; i < maxItems; i++) {
            if (i < leftItems.length) {
                contentStream.beginText();
                contentStream.newLineAtOffset(leftColumnX, currentY);
                contentStream.showText(leftItems[i]);
                contentStream.endText();
            }

            if (i < rightItems.length) {
                contentStream.beginText();
                contentStream.newLineAtOffset(rightColumnX, currentY);
                contentStream.showText(rightItems[i]);
                contentStream.endText();
            }

            currentY -= LINE_SPACING_SMALL;
        }

        return currentY - 10; // Space after section
    }

    /**
     * Draw a key-value pair list with proper alignment
     */
    public static float drawKeyValueList(PDPageContentStream contentStream, String[][] keyValuePairs,
                                       float startY, float labelWidth) throws IOException {
        float currentY = startY;

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);

        for (String[] pair : keyValuePairs) {
            if (pair.length >= 2) {
                // Label
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT, currentY);
                contentStream.showText(pair[0] + ":");
                contentStream.endText();

                // Value
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SMALL);
                contentStream.beginText();
                contentStream.newLineAtOffset(MARGIN_LEFT + labelWidth, currentY);
                contentStream.showText(pair[1]);
                contentStream.endText();

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
                currentY -= LINE_SPACING_SMALL;
            }
        }

        return currentY - 5; // Small space after list
    }
}