package fin.util;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    static final float[] COLOR_BLACK = {0, 0, 0};
    static final float[] COLOR_BLUE = {0.2f, 0.4f, 0.8f};
    static final float[] COLOR_LIGHT_BLUE = {0.9f, 0.95f, 1.0f};
    static final float[] COLOR_LIGHT_GRAY = {0.9f, 0.9f, 0.9f};
    static final float[] COLOR_DARK_GRAY = {0.3f, 0.3f, 0.3f};

    /**
     * Draw a professional header section with title and company information
     */
    public static float drawHeaderSection(PDPageContentStream contentStream, String title, String companyName,
                                        String subtitle, float startY) throws IOException {
        return drawHeaderSection(contentStream, title, companyName, subtitle, startY, PAGE_WIDTH);
    }

    /**
     * Draw a professional header section with title and company information (with custom page width)
     */
    public static float drawHeaderSection(PDPageContentStream contentStream, String title, String companyName,
                                        String subtitle, float startY, float pageWidth) throws IOException {
        float currentY = startY;
        float contentWidth = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;

        // Draw decorative header border
        contentStream.setLineWidth(2.0f);
        contentStream.setStrokingColor(COLOR_BLUE[0], COLOR_BLUE[1], COLOR_BLUE[2]);
        contentStream.addRect(MARGIN_LEFT, currentY - 140, contentWidth, 140);
        contentStream.stroke();

        // Reset stroke color to black
        contentStream.setStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        contentStream.setLineWidth(1.0f);

        // Title with background
        contentStream.setNonStrokingColor(COLOR_LIGHT_BLUE[0], COLOR_LIGHT_BLUE[1], COLOR_LIGHT_BLUE[2]);
        contentStream.addRect(MARGIN_LEFT + 5, currentY - 35, contentWidth - 10, 30);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_TITLE);
        contentStream.beginText();
        float titleWidth = title.length() * 8; // Approximate width calculation
        float titleX = (pageWidth - titleWidth) / 2;
        contentStream.newLineAtOffset(titleX, currentY - 25);
        contentStream.showText(title);
        contentStream.endText();

        currentY -= 50;

        // Company name with underline
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_HEADER);
        contentStream.beginText();
        float companyNameWidth = companyName.length() * 6; // Approximate width
        float companyX = (pageWidth - companyNameWidth) / 2;
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
            float subtitleX = (pageWidth - subtitleWidth) / 2;
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
        return drawSectionHeader(contentStream, sectionTitle, startY, sectionHeight, CONTENT_WIDTH);
    }

    /**
     * Draw a section with background, border, and header (with custom content width)
     */
    public static float drawSectionHeader(PDPageContentStream contentStream, String sectionTitle,
                                        float startY, float sectionHeight, float contentWidth) throws IOException {
        // Section background
        contentStream.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        contentStream.addRect(MARGIN_LEFT, startY - sectionHeight, contentWidth, sectionHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Section border
        contentStream.setLineWidth(1.0f);
        contentStream.addRect(MARGIN_LEFT, startY - sectionHeight, contentWidth, sectionHeight);
        contentStream.stroke();

        // Section header with background
        contentStream.setNonStrokingColor(COLOR_DARK_GRAY[0], COLOR_DARK_GRAY[1], COLOR_DARK_GRAY[2]);
        contentStream.addRect(MARGIN_LEFT + 2, startY - 20, contentWidth - 4, 18);
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
        return drawTable(contentStream, headers, data, startY, rowHeight, CONTENT_WIDTH, MARGIN_LEFT);
    }

    /**
     * Draw a professional table with headers and data (with custom page dimensions)
     */
    public static float drawTable(PDPageContentStream contentStream, String[] headers, String[][] data,
                                float startY, float rowHeight, float pageContentWidth, float pageMarginLeft) throws IOException {
        float currentY = startY;
        float tableWidth = pageContentWidth;
        int numColumns = headers.length;

        // Calculate column widths based on content (smarter than equal distribution)
        float[] columnWidths = calculateColumnWidths(headers, data, tableWidth);

        // Table border
        float tableHeight = (data.length + 1) * rowHeight; // +1 for header
        contentStream.setLineWidth(1.0f);
        contentStream.addRect(pageMarginLeft, currentY - tableHeight, tableWidth, tableHeight);
        contentStream.stroke();

        // Header row with background
        contentStream.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        contentStream.addRect(pageMarginLeft + 1, currentY - rowHeight + 1, tableWidth - 2, rowHeight - 2);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Draw headers
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_SMALL);
        float xPos = pageMarginLeft + 5;
        for (int i = 0; i < headers.length; i++) {
            contentStream.beginText();
            contentStream.newLineAtOffset(xPos, currentY - rowHeight + 5);
            contentStream.showText(headers[i]);
            contentStream.endText();
            xPos += columnWidths[i];
        }

        // Header underline
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(pageMarginLeft, currentY - rowHeight);
        contentStream.lineTo(pageMarginLeft + tableWidth, currentY - rowHeight);
        contentStream.stroke();
        contentStream.setLineWidth(1.0f);

        currentY -= rowHeight;

        // Draw data rows
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        for (String[] row : data) {
            xPos = pageMarginLeft + 5;
            for (int i = 0; i < row.length && i < headers.length; i++) {
                String cellText = row[i] != null ? row[i] : "";
                drawTextInCell(contentStream, cellText, xPos, currentY - rowHeight, columnWidths[i] - 10, rowHeight - 6);
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
        return drawSummaryBox(contentStream, metrics, startY, boxHeight, CONTENT_WIDTH);
    }

    /**
     * Draw a summary box with key metrics (with custom content width)
     */
    public static float drawSummaryBox(PDPageContentStream contentStream, String[][] metrics,
                                     float startY, float boxHeight, float contentWidth) throws IOException {
        float currentY = startY;

        // Summary box background and border
        contentStream.setNonStrokingColor(0.95f, 1.0f, 0.95f); // Light green background
        contentStream.addRect(MARGIN_LEFT, currentY - boxHeight, contentWidth, boxHeight);
        contentStream.fill();
        contentStream.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        contentStream.setLineWidth(1.5f);
        contentStream.addRect(MARGIN_LEFT, currentY - boxHeight, contentWidth, boxHeight);
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
                contentStream.newLineAtOffset(MARGIN_LEFT + contentWidth - 120, metricY);
                contentStream.showText(metric[1]);
                contentStream.endText();

                metricY -= LINE_SPACING_NORMAL;
            }
        }

        return currentY - boxHeight - 20; // Space after summary box
    }

    /**
     * Draw a professional footer with page numbering
     */
    public static void drawFooter(PDPageContentStream contentStream, String footerText, String additionalInfo) throws IOException {
        drawFooter(contentStream, footerText, additionalInfo, 1, 1);
    }

    /**
     * Draw a professional footer with page numbering
     */
    public static void drawFooter(PDPageContentStream contentStream, String footerText, String additionalInfo,
                                int currentPage, int totalPages) throws IOException {
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
        contentStream.showText("Page " + currentPage + " of " + totalPages);
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

    /**
     * Draw a wrapped text block that utilizes the full content width with proper text wrapping
     */
    public static float drawWrappedTextBlock(PDPageContentStream contentStream, String title, String content,
                                           float startY, float maxWidth) throws IOException {
        float currentY = startY;

        // Draw title
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), FONT_SIZE_NORMAL);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_LEFT, currentY);
        contentStream.showText(title + ":");
        contentStream.endText();

        currentY -= LINE_SPACING_NORMAL;

        // Draw wrapped content using full width
        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), FONT_SIZE_SMALL);
        currentY = drawWrappedText(contentStream, content, MARGIN_LEFT, currentY, maxWidth, LINE_SPACING_SMALL);

        return currentY - LINE_SPACING_SMALL; // Space after block
    }

    /**
     * Draw wrapped text across multiple lines within specified width bounds
     */
    private static float drawWrappedText(PDPageContentStream contentStream, String text, float x, float startY,
                                       float maxWidth, float lineHeight) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return startY;
        }

        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = FONT_SIZE_SMALL;
        float currentY = startY;

        // Split text into words
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            float textWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            // If this word would make the line too long, draw current line and start new one
            if (textWidth > maxWidth && currentLine.length() > 0) {
                // Draw current line
                contentStream.beginText();
                contentStream.newLineAtOffset(x, currentY);
                contentStream.showText(currentLine.toString());
                contentStream.endText();

                // Move to next line
                currentY -= lineHeight;
                currentLine = new StringBuilder(word);
            } else {
                // Add word to current line
                currentLine = new StringBuilder(testLine);
            }
        }

        // Draw the final line
        if (currentLine.length() > 0) {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, currentY);
            contentStream.showText(currentLine.toString());
            contentStream.endText();
            currentY -= lineHeight;
        }

        return currentY;
    }

    /**
     * Draw text within a table cell with automatic wrapping for long content and markdown formatting support
     */
    private static void drawTextInCell(PDPageContentStream contentStream, String text, float x, float y,
                                     float cellWidth, float cellHeight) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font italicFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
        float fontSize = FONT_SIZE_SMALL;
        float lineHeight = LINE_SPACING_SMALL;

        // Parse markdown formatting and split into segments
        List<TextSegment> segments = parseMarkdownText(text);

        float currentY = y + cellHeight - 5; // Start near top of cell
        float currentX = x;

        for (TextSegment segment : segments) {
            PDType1Font font = getFontForStyle(segment.style, normalFont, boldFont, italicFont);
            String segmentText = segment.text;

            // Check if segment fits on current line
            float segmentWidth = font.getStringWidth(segmentText) / 1000 * fontSize;

            if (currentX + segmentWidth > x + cellWidth && currentX > x) {
                // Move to next line
                currentY -= lineHeight;
                currentX = x;

                // Check if we've run out of vertical space
                if (currentY < y + 3) { // Leave some bottom margin
                    // Add ellipsis to indicate truncated text
                    contentStream.setFont(normalFont, fontSize);
                    contentStream.beginText();
                    contentStream.newLineAtOffset(x, currentY);
                    contentStream.showText("...");
                    contentStream.endText();
                    return;
                }
            }

            // Draw the segment
            contentStream.setFont(font, fontSize);
            contentStream.beginText();
            contentStream.newLineAtOffset(currentX, currentY);
            contentStream.showText(segmentText);
            contentStream.endText();

            currentX += segmentWidth;
        }
    }

    /**
     * Parse markdown-style formatting (**bold**, *italic*) into text segments
     */
    private static List<TextSegment> parseMarkdownText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        int i = 0;

        while (i < text.length()) {
            if (i + 1 < text.length() && text.charAt(i) == '*' && text.charAt(i + 1) == '*') {
                // Bold text: **text**
                int endIndex = text.indexOf("**", i + 2);
                if (endIndex != -1) {
                    String boldText = text.substring(i + 2, endIndex);
                    segments.add(new TextSegment(boldText, TextStyle.BOLD));
                    i = endIndex + 2;
                } else {
                    // No closing **, treat as normal text
                    segments.add(new TextSegment(text.substring(i), TextStyle.NORMAL));
                    break;
                }
            } else if (text.charAt(i) == '*') {
                // Italic text: *text*
                int endIndex = text.indexOf("*", i + 1);
                if (endIndex != -1) {
                    String italicText = text.substring(i + 1, endIndex);
                    segments.add(new TextSegment(italicText, TextStyle.ITALIC));
                    i = endIndex + 1;
                } else {
                    // No closing *, treat as normal text
                    segments.add(new TextSegment(text.substring(i), TextStyle.NORMAL));
                    break;
                }
            } else {
                // Normal text - find next formatting marker
                int nextBold = text.indexOf("**", i);
                int nextItalic = text.indexOf("*", i);

                int nextMarker = -1;
                if (nextBold != -1 && (nextItalic == -1 || nextBold < nextItalic)) {
                    nextMarker = nextBold;
                } else if (nextItalic != -1) {
                    nextMarker = nextItalic;
                }

                String normalText;
                if (nextMarker != -1) {
                    normalText = text.substring(i, nextMarker);
                    i = nextMarker;
                } else {
                    normalText = text.substring(i);
                    i = text.length();
                }

                if (!normalText.isEmpty()) {
                    segments.add(new TextSegment(normalText, TextStyle.NORMAL));
                }
            }
        }

        return segments;
    }

    /**
     * Get the appropriate font for a text style
     */
    private static PDType1Font getFontForStyle(TextStyle style, PDType1Font normalFont,
                                             PDType1Font boldFont, PDType1Font italicFont) {
        switch (style) {
            case BOLD:
                return boldFont;
            case ITALIC:
                return italicFont;
            case NORMAL:
            default:
                return normalFont;
        }
    }

    /**
     * Text segment with formatting style
     */
    private static class TextSegment {
        final String text;
        final TextStyle style;

        TextSegment(String text, TextStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    /**
     * Text formatting styles
     */
    private enum TextStyle {
        NORMAL, BOLD, ITALIC
    }

    /**
     * Calculate column widths based on content length for better table layout
     */
    private static float[] calculateColumnWidths(String[] headers, String[][] data, float tableWidth) throws IOException {
        int numColumns = headers.length;
        float[] columnWidths = new float[numColumns];

        // Minimum and maximum column widths (in points)
        final float MIN_COLUMN_WIDTH = 60f;  // Minimum width for any column
        final float MAX_COLUMN_WIDTH_RATIO = 0.6f; // No column can be more than 60% of table width

        // Calculate the maximum content width for each column
        float[] maxContentWidths = new float[numColumns];

        // Use a temporary font to measure text width
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        float fontSize = FONT_SIZE_SMALL;

        // Measure header widths
        for (int i = 0; i < headers.length; i++) {
            float headerWidth = font.getStringWidth(headers[i]) / 1000 * fontSize;
            maxContentWidths[i] = Math.max(maxContentWidths[i], headerWidth);
        }

        // Measure data widths
        for (String[] row : data) {
            for (int i = 0; i < row.length && i < numColumns; i++) {
                if (row[i] != null) {
                    float dataWidth = font.getStringWidth(row[i]) / 1000 * fontSize;
                    maxContentWidths[i] = Math.max(maxContentWidths[i], dataWidth);
                }
            }
        }

        // Add padding for each column (left/right margins)
        final float COLUMN_PADDING = 10f;
        for (int i = 0; i < numColumns; i++) {
            maxContentWidths[i] += COLUMN_PADDING * 2; // Left and right padding
        }

        // Calculate total content width needed
        float totalContentWidth = 0;
        for (float width : maxContentWidths) {
            totalContentWidth += width;
        }

        // If total content fits within table width, distribute proportionally
        if (totalContentWidth <= tableWidth) {
            // Calculate proportional widths but ensure minimums
            float remainingWidth = tableWidth;
            int flexibleColumns = 0;

            // First pass: assign minimum widths and count flexible columns
            for (int i = 0; i < numColumns; i++) {
                if (maxContentWidths[i] < MIN_COLUMN_WIDTH) {
                    columnWidths[i] = MIN_COLUMN_WIDTH;
                    remainingWidth -= MIN_COLUMN_WIDTH;
                } else {
                    flexibleColumns++;
                }
            }

            // Second pass: distribute remaining width proportionally
            if (flexibleColumns > 0) {
                float widthPerFlexibleColumn = remainingWidth / flexibleColumns;
                for (int i = 0; i < numColumns; i++) {
                    if (columnWidths[i] == 0) { // Not yet assigned
                        columnWidths[i] = Math.max(maxContentWidths[i], widthPerFlexibleColumn);
                    }
                }
            }
        } else {
            // Content is too wide, need to compress
            // Use proportional allocation but respect maximum ratios
            float[] proportions = new float[numColumns];
            for (int i = 0; i < numColumns; i++) {
                proportions[i] = maxContentWidths[i] / totalContentWidth;
            }

            for (int i = 0; i < numColumns; i++) {
                columnWidths[i] = Math.max(MIN_COLUMN_WIDTH,
                    Math.min(tableWidth * MAX_COLUMN_WIDTH_RATIO, tableWidth * proportions[i]));
            }

            // Redistribute any unused space from max ratio limits
            float totalAssigned = 0;
            for (float width : columnWidths) {
                totalAssigned += width;
            }
            float unusedSpace = tableWidth - totalAssigned;
            if (unusedSpace > 0) {
                // Distribute unused space proportionally
                for (int i = 0; i < numColumns; i++) {
                    columnWidths[i] += unusedSpace * (columnWidths[i] / totalAssigned);
                }
            }
        }

        return columnWidths;
    }
}