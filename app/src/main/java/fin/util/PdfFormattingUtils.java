package fin.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;

/**
 * PDF Formatting Utilities for FIN Financial Management System
 * Rewritten to use font metrics, accurate wrapping, and alignment helpers.
 */
public class PdfFormattingUtils {

    // PDF Layout Constants (A4)
    public static final float PAGE_WIDTH = 595.28f;
    public static final float PAGE_HEIGHT = 841.89f;
    public static final float MARGIN_LEFT = 50f;
    public static final float MARGIN_RIGHT = 50f;
    public static final float MARGIN_TOP = 50f;
    public static final float MARGIN_BOTTOM = 50f;
    public static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    // Font sizes
    public static final float FONT_SIZE_TITLE = 18f;
    public static final float FONT_SIZE_HEADER = 14f;
    public static final float FONT_SIZE_SUBHEADER = 12f;
    public static final float FONT_SIZE_NORMAL = 11f;
    public static final float FONT_SIZE_SMALL = 10f;
    public static final float FONT_SIZE_TINY = 9f;

    // Leading / spacing
    public static final float LINE_HEIGHT_FACTOR = 1.15f;

    // Colors (RGB 0..1)
    static final float[] COLOR_BLACK = {0f, 0f, 0f};
    static final float[] COLOR_BLUE = {0.2f, 0.4f, 0.8f};
    static final float[] COLOR_LIGHT_BLUE = {0.9f, 0.95f, 1.0f};
    static final float[] COLOR_LIGHT_GRAY = {0.95f, 0.95f, 0.95f};
    static final float[] COLOR_DARK_GRAY = {0.25f, 0.25f, 0.25f};

    // Fonts (Standard 14 fonts)
    private static final PDFont FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont FONT_OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    // ------------------------
    //  Helper: text width in user units
    // ------------------------
    private static float textWidth(PDFont font, float fontSize, String text) throws IOException {
        if (text == null || text.isEmpty()) return 0f;
        return font.getStringWidth(text) / 1000f * fontSize;
    }

    // ------------------------
    //  Center text on page horizontally at given Y
    // ------------------------
    public static void showCenteredText(PDPageContentStream cs, PDFont font, float fontSize,
                                        String text, float pageWidth, float y) throws IOException {
        if (text == null) text = "";
        float tw = textWidth(font, fontSize, text);
        float x = (pageWidth - tw) / 2f;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    // ------------------------
    //  Right align text to a column right edge
    // ------------------------
    public static void showRightAlignedText(PDPageContentStream cs, PDFont font, float fontSize,
                                            String text, float rightEdgeX, float y) throws IOException {
        if (text == null) text = "";
        float tw = textWidth(font, fontSize, text);
        float x = rightEdgeX - tw;
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    // ------------------------
    //  Word-wrap text into lines that fit maxWidth
    // ------------------------
    public static List<String> wrapText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            lines.add(""); // Return empty string instead of space - empty cells should be empty
            return lines;
        }

        // Check if text is purely numeric (numbers, currency symbols, commas, dots, dashes, percentages)
        // These should NEVER be wrapped with hyphens
        boolean isNumeric = text.matches("^[R$€£¥₹\\d\\s,\\.\\-]+%?$");
        
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            String candidate = line.length() == 0 ? w : line + " " + w;
            float wWidth = textWidth(font, fontSize, candidate);
            if (wWidth <= maxWidth) {
                line = new StringBuilder(candidate);
            } else {
                if (line.length() > 0) lines.add(line.toString());
                // if single word longer than maxWidth, break the word (simple hyphenation)
                if (textWidth(font, fontSize, w) > maxWidth) {
                    // CRITICAL: Never hyphenate numeric values (currency, numbers, percentages)
                    // Check if this specific word is numeric (including percentages like "100.0%" or "4.0%")
                    boolean isWordNumeric = w.matches("^[R$€£¥₹\\d\\s,\\.\\-]+%?$") || w.endsWith("%");
                    
                    if (isWordNumeric || isNumeric) {
                        // Don't break numeric values - just add as-is even if too wide
                        // This prevents "30000.00" from becoming "3000-0.00"
                        lines.add(w);
                        line = new StringBuilder();
                    } else {
                        // Only break non-numeric text
                        String remaining = w;
                        while (textWidth(font, fontSize, remaining) > maxWidth && remaining.length() > 1) {
                            int approxChars = Math.max(1, (int) (remaining.length() * maxWidth / textWidth(font, fontSize, remaining)));
                            String part = remaining.substring(0, approxChars);
                            // shrink until fits
                            while (textWidth(font, fontSize, part) > maxWidth && part.length() > 1) {
                                part = part.substring(0, part.length() - 1);
                            }
                            // Only add hyphen if we're actually breaking the word
                            if (remaining.length() > part.length()) {
                                lines.add(part + "-");
                                remaining = remaining.substring(part.length());
                            } else {
                                lines.add(part);
                                remaining = "";
                            }
                        }
                        if (!remaining.isEmpty()) line = new StringBuilder(remaining);
                        else line = new StringBuilder();
                    }
                } else {
                    line = new StringBuilder(w);
                }
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        if (lines.isEmpty()) lines.add(""); // Ensure at least one line even if empty
        return lines;
    }

    // ------------------------
    //  Draw header section
    // ------------------------
    public static float drawHeaderSection(PDPageContentStream cs, String title, String companyName, String subtitle,
                                          float startY, float pageWidth) throws IOException {
        float currentY = startY;
        float contentW = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;

        // Decorative border rectangle (light)
        cs.setLineWidth(1.5f);
        cs.setStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        cs.addRect(MARGIN_LEFT, currentY - 120, contentW, 120);
        cs.stroke();

        // Title (centered)
        float titleY = currentY - 22;
        showCenteredText(cs, FONT_BOLD, FONT_SIZE_TITLE, title, pageWidth, titleY);

        currentY -= 48;

        // Company name (center & underline)
        float compY = currentY;
        showCenteredText(cs, FONT_BOLD, FONT_SIZE_HEADER, companyName, pageWidth, compY);

        // underline using measured width
        float cWidth = textWidth(FONT_BOLD, FONT_SIZE_HEADER, companyName);
        float compX = (pageWidth - cWidth) / 2f;
        cs.setLineWidth(1.2f);
        cs.moveTo(compX, compY - 3);
        cs.lineTo(compX + cWidth, compY - 3);
        cs.stroke();

        currentY -= 28;

        // Subtitle (if present)
        if (subtitle != null && !subtitle.isEmpty()) {
            showCenteredText(cs, FONT_REGULAR, FONT_SIZE_NORMAL, subtitle, pageWidth, currentY);
            currentY -= FONT_SIZE_NORMAL * LINE_HEIGHT_FACTOR;
        }

        // Reset color to black
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        cs.setStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        return currentY - 8;
    }

    // ------------------------
    // Centralised logo drawing helpers
    // ------------------------
    public static void drawLogo(PDPageContentStream cs, PDDocument doc, String logoPath,
                                float currentY, float pageWidth) throws IOException {
        if (logoPath == null || logoPath.isEmpty() || doc == null) return;
        PDImageXObject img = PDImageXObject.createFromFile(logoPath, doc);
        drawImageWithMax(cs, img, currentY, pageWidth);
    }

    public static void drawLogo(PDPageContentStream cs, PDDocument doc, byte[] logoBytes,
                                float currentY, float pageWidth) throws IOException {
        if (logoBytes == null || logoBytes.length == 0 || doc == null) return;
        PDImageXObject img = PDImageXObject.createFromByteArray(doc, logoBytes, "logo");
        drawImageWithMax(cs, img, currentY, pageWidth);
    }

    private static void drawImageWithMax(PDPageContentStream cs, PDImageXObject img,
                                         float currentY, float pageWidth) throws IOException {
        final float MAX_LOGO = 64f;
        float iw = img.getWidth();
        float ih = img.getHeight();
        float scale = Math.min(MAX_LOGO / iw, MAX_LOGO / ih);
        if (scale > 1f) scale = 1f; 
        float drawW = iw * scale;
        float drawH = ih * scale;
        float logoX = MARGIN_LEFT + 10f;
        float logoY = currentY - 36 - drawH / 2f;
        cs.drawImage(img, logoX, logoY, drawW, drawH);
    }

    /**
     * Draw header section with optional logo. If logoPath is provided and loadable, the logo will be
     * drawn inside the header decoration on the left.
     */
    public static float drawHeaderSection(PDPageContentStream cs, PDDocument doc, String logoPath,
                                          String title, String companyName, String subtitle,
                                          float startY, float pageWidth) throws IOException {
        float currentY = startY;
        float contentW = pageWidth - MARGIN_LEFT - MARGIN_RIGHT;

        // Decorative border rectangle (light)
        cs.setLineWidth(1.5f);
        cs.setStrokingColor(COLOR_BLUE[0], COLOR_BLUE[1], COLOR_BLUE[2]);
        cs.addRect(MARGIN_LEFT, currentY - 120, contentW, 120);
        cs.stroke();

        // Logo
        try {
            drawLogo(cs, doc, logoPath, currentY, pageWidth);
        } catch (IOException ex) {
        }

        // Title (centered)
        float titleY = currentY - 22;
        showCenteredText(cs, FONT_BOLD, FONT_SIZE_TITLE, title, pageWidth, titleY);

        currentY -= 48;

        // Company name (center & underline)
        float compY = currentY;
        showCenteredText(cs, FONT_BOLD, FONT_SIZE_HEADER, companyName, pageWidth, compY);

        // underline using measured width
        float cWidth = textWidth(FONT_BOLD, FONT_SIZE_HEADER, companyName);
        float compX = (pageWidth - cWidth) / 2f;
        cs.setLineWidth(1.2f);
        cs.moveTo(compX, compY - 3);
        cs.lineTo(compX + cWidth, compY - 3);
        cs.stroke();

        currentY -= 28;

        // Subtitle (if present)
        if (subtitle != null && !subtitle.isEmpty()) {
            showCenteredText(cs, FONT_REGULAR, FONT_SIZE_NORMAL, subtitle, pageWidth, currentY);
            currentY -= FONT_SIZE_NORMAL * LINE_HEIGHT_FACTOR;
        }

        // Reset color to black
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        cs.setStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        return currentY - 8;
    }

    // ------------------------
    //  Draw a simple styled section header
    // ------------------------
    public static float drawSectionHeader(PDPageContentStream cs, String sectionTitle, float startY,
                                          float contentWidth) throws IOException {
        // Add consistent spacing BEFORE header (only if not at top of page)
        // This prevents excessive blank lines between sections
        float headerY = startY - 0f; // Reduced from 15f to 0f for tighter spacing (20pt total with bottom margin)
        
        // Dark header bar
        cs.setNonStrokingColor(COLOR_DARK_GRAY[0], COLOR_DARK_GRAY[1], COLOR_DARK_GRAY[2]);
        cs.addRect(MARGIN_LEFT, headerY - 20, contentWidth, 18);
        cs.fill();

        // Title (white)
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(FONT_BOLD, FONT_SIZE_SUBHEADER);
        cs.newLineAtOffset(MARGIN_LEFT + 8, headerY - 16);
        cs.showText(sectionTitle);
        cs.endText();

        // reset color
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        // Return position with moderate spacing AFTER header (12 points)
        return headerY - 32;
    }

    // ------------------------
    //  Draw a subsection header (same style as main section header for consistency)
    // ------------------------
    public static float drawSubsectionHeader(PDPageContentStream cs, String subsectionTitle, float startY,
                                             float contentWidth) throws IOException {
        float headerY = startY - 5f; // Small spacing before subsection
        
        // Dark gray header bar (same as main section for consistency)
        cs.setNonStrokingColor(COLOR_DARK_GRAY[0], COLOR_DARK_GRAY[1], COLOR_DARK_GRAY[2]);
        cs.addRect(MARGIN_LEFT, headerY - 16, contentWidth, 14);
        cs.fill();

        // Title (white)
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.beginText();
        cs.setFont(FONT_BOLD, FONT_SIZE_NORMAL);
        cs.newLineAtOffset(MARGIN_LEFT + 6, headerY - 12);
        cs.showText(subsectionTitle);
        cs.endText();

        // reset color
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);
        // Return position with spacing after header
        return headerY - 26;
    }

    // ------------------------
    //  Draw table with measured columns, wrapping inside cells, and right align numbers
    //  (this function keeps the same signature but uses helpers)
    // ------------------------
    public static float drawTable(PDPageContentStream cs, PDDocument doc, String[] headers, String[][] data,
                                  float startY, float rowHeight, float pageContentWidth, float pageMarginLeft) throws IOException {
        float currentY = startY;
        float tableWidth = pageContentWidth;
        int numColumns = headers.length;

        float[] columnWidths = calculateColumnWidths(headers, data, tableWidth);

        // Calculate table height to draw border (we don't paginate here — keep simple)
        float tableHeight = (data.length + 1) * rowHeight;
        cs.setLineWidth(1f);
        cs.addRect(pageMarginLeft, currentY - tableHeight, tableWidth, tableHeight);
        cs.stroke();

        // Header background
        cs.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        cs.addRect(pageMarginLeft + 1, currentY - rowHeight + 1, tableWidth - 2, rowHeight - 2);
        cs.fill();
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Draw header text with padding
        float headerX = pageMarginLeft;
        float headerTextY = currentY - rowHeight + (rowHeight - FONT_SIZE_SMALL) / 2f + 2;
        cs.setFont(FONT_BOLD, FONT_SIZE_SMALL);
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(headerX + 6, headerTextY);  // 6pt left padding
            cs.showText(headers[i]);
            cs.endText();
            headerX += columnWidths[i];
        }

        // Header underline
        cs.setLineWidth(1.2f);
        cs.moveTo(pageMarginLeft, currentY - rowHeight);
        cs.lineTo(pageMarginLeft + tableWidth, currentY - rowHeight);
        cs.stroke();
        cs.setLineWidth(1f);

        currentY -= rowHeight;

        // Draw rows
        cs.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
        for (String[] row : data) {
            float cellTop = currentY;
            for (int col = 0; col < numColumns; col++) {
                String cell = (col < row.length) && row[col] != null ? row[col] : "";
                
                // Calculate column start position explicitly to prevent accumulation errors
                float colStart = pageMarginLeft;
                for (int i = 0; i < col; i++) {
                    colStart += columnWidths[i];
                }
                
                // Calculate the actual left and right edges with padding
                float textLeft = colStart + 6;  // 6pt left padding
                float textRight = colStart + columnWidths[col] - 6;  // 6pt right padding

                // If looks like a numeric value (currency/number) — right align
                if (isNumericLike(cell)) {
                    showRightAlignedText(cs, FONT_REGULAR, FONT_SIZE_SMALL, cell, textRight, cellTop - (FONT_SIZE_SMALL));
                } else {
                    // wrap lines inside cell
                    float textWidth = textRight - textLeft;  // Available width for text
                    List<String> lines = wrapText(cell, FONT_REGULAR, FONT_SIZE_SMALL, textWidth);
                    float lineY = cellTop - 4 - FONT_SIZE_SMALL; // small top padding
                    int lineCount = 0;
                    int maxLines = (int) Math.floor((rowHeight - 8) / (FONT_SIZE_SMALL * LINE_HEIGHT_FACTOR));
                    
                    for (String ln : lines) {
                        // Skip empty lines to avoid unnecessary "..." display
                        if (ln.trim().isEmpty()) {
                            continue; // Don't count or render empty lines
                        }
                        
                        if (lineCount >= maxLines) {
                            // Show ellipsis only if there are more non-empty lines remaining
                            boolean hasMoreContent = false;
                            for (int i = lines.indexOf(ln); i < lines.size(); i++) {
                                if (!lines.get(i).trim().isEmpty()) {
                                    hasMoreContent = true;
                                    break;
                                }
                            }
                            if (hasMoreContent) {
                                cs.beginText();
                                cs.newLineAtOffset(textLeft, lineY);
                                cs.showText("...");
                                cs.endText();
                            }
                            break;
                        }
                        
                        cs.beginText();
                        cs.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
                        cs.newLineAtOffset(textLeft, lineY);
                        cs.showText(ln);
                        cs.endText();
                        lineY -= FONT_SIZE_SMALL * LINE_HEIGHT_FACTOR;
                        lineCount++;
                    }
                }
                // No need to accumulate cellLeft - we calculate colStart explicitly each iteration
            }
            currentY -= rowHeight;
        }

        return currentY - 8;
    }

    // ------------------------
    //  Draw wrapped paragraph block
    // ------------------------
    public static float drawWrappedTextBlock(PDPageContentStream cs, String title, String content, float startY, float maxWidth) throws IOException {
        float y = startY;
        if (title != null && !title.isEmpty()) {
            cs.setFont(FONT_BOLD, FONT_SIZE_NORMAL);
            cs.beginText();
            cs.newLineAtOffset(MARGIN_LEFT, y);
            cs.showText(title + ":");
            cs.endText();
            y -= FONT_SIZE_NORMAL * LINE_HEIGHT_FACTOR;
        }

        if (content != null && !content.trim().isEmpty()) {
            // Split by newlines first to respect line breaks
            String[] contentLines = content.split("\n");
            cs.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
            
            for (String line : contentLines) {
                if (line.trim().isEmpty()) {
                    // Empty line - add small spacing
                    y -= FONT_SIZE_SMALL * LINE_HEIGHT_FACTOR * 0.5f;
                    continue;
                }
                
                // Detect if this is a numbered item (e.g., "1. Academic Excellence")
                // or a bullet item (starts with •)
                boolean isNumberedItem = line.matches("^\\d+\\.\\s+.*");
                boolean isBulletItem = line.trim().startsWith("•");
                boolean isYearHeader = line.trim().matches("^Year\\s+\\d+.*");
                
                float indent = MARGIN_LEFT;
                String displayLine = line.trim();
                
                if (isBulletItem) {
                    // Bullet item - indent by 20 points
                    indent = MARGIN_LEFT + 20f;
                    displayLine = line.trim(); // Keep the bullet
                } else if (isNumberedItem || isYearHeader) {
                    // Numbered item or Year header - no extra indent (bold font)
                    cs.setFont(FONT_BOLD, FONT_SIZE_SMALL);
                }
                
                // Wrap the line if it's too long
                List<String> wrappedLines = wrapText(displayLine, 
                    (isNumberedItem || isYearHeader) ? FONT_BOLD : FONT_REGULAR, 
                    FONT_SIZE_SMALL, 
                    maxWidth - (indent - MARGIN_LEFT));
                
                for (int i = 0; i < wrappedLines.size(); i++) {
                    String wrappedLine = wrappedLines.get(i);
                    cs.beginText();
                    cs.newLineAtOffset(indent, y);
                    cs.showText(wrappedLine);
                    cs.endText();
                    y -= FONT_SIZE_SMALL * LINE_HEIGHT_FACTOR;
                    
                    // For continuation lines of the same logical line, add extra indent
                    if (i == 0 && wrappedLines.size() > 1) {
                        indent += 10f; // Additional indent for wrapped continuations
                    }
                }
                
                // Reset font to regular for next line
                if (isNumberedItem || isYearHeader) {
                    cs.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
                }
            }
        }
        return y - 6;
    }

    // ------------------------
    //  Simple summary box drawing (kept compatible)
    // ------------------------
    public static float drawSummaryBox(PDPageContentStream cs, String[][] metrics, float startY, float boxHeight, float contentWidth) throws IOException {
        float currentY = startY;

        // background
        cs.setNonStrokingColor(0.95f, 1.0f, 0.95f);
        cs.addRect(MARGIN_LEFT, currentY - boxHeight, contentWidth, boxHeight);
        cs.fill();
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // border
        cs.setLineWidth(1.2f);
        cs.addRect(MARGIN_LEFT, currentY - boxHeight, contentWidth, boxHeight);
        cs.stroke();

        // draw metrics
        float metricY = currentY - 18;
        for (String[] m : metrics) {
            if (m.length >= 2) {
                cs.beginText();
                cs.setFont(FONT_BOLD, FONT_SIZE_NORMAL);
                cs.newLineAtOffset(MARGIN_LEFT + 12, metricY);
                cs.showText(m[0] + ":");
                cs.endText();

                showRightAlignedText(cs, FONT_BOLD, FONT_SIZE_NORMAL, m[1], MARGIN_LEFT + contentWidth - 12, metricY);
                metricY -= FONT_SIZE_NORMAL * LINE_HEIGHT_FACTOR;
            }
        }
        return currentY - boxHeight - 8;
    }

    // ------------------------
    //  Footer helper (original - backward compatibility)
    // ------------------------
    public static void drawFooter(PDPageContentStream cs, String footerText, String additionalInfo, int currentPage, int totalPages) throws IOException {
        drawFooter(cs, footerText, additionalInfo, null, currentPage, totalPages);
    }

    // ------------------------
    //  Footer helper (with custom right text)
    // ------------------------
    public static void drawFooter(PDPageContentStream cs, String footerText, String additionalInfo, String rightText, int currentPage, int totalPages) throws IOException {
        float footerY = MARGIN_BOTTOM + 18;
        cs.setFont(FONT_OBLIQUE, FONT_SIZE_TINY);

        // left: footerText
        cs.beginText();
        cs.newLineAtOffset(MARGIN_LEFT, footerY + 6);
        cs.showText(footerText == null ? "" : footerText);
        cs.endText();

        // center: page numbering (always centered)
        String pageText = "Page " + currentPage + " of " + totalPages;
        float pageTW = textWidth(FONT_REGULAR, FONT_SIZE_TINY, pageText);
        float centerX = (PAGE_WIDTH - pageTW) / 2f;
        cs.beginText();
        cs.newLineAtOffset(centerX, footerY + 6);
        cs.showText(pageText);
        cs.endText();

        // right: custom text (if provided)
        if (rightText != null && !rightText.isEmpty()) {
            float rightTW = textWidth(FONT_REGULAR, FONT_SIZE_TINY, rightText);
            cs.beginText();
            cs.newLineAtOffset(PAGE_WIDTH - MARGIN_RIGHT - rightTW, footerY + 6);
            cs.showText(rightText);
            cs.endText();
        }
    }

    // ------------------------
    //  Column width calculator (refined with better wrapping support)
    // ------------------------
    private static float[] calculateColumnWidths(String[] headers, String[][] data, float tableWidth) throws IOException {
        int cols = headers.length;
        float[] maxContent = new float[cols];
        float[] avgContent = new float[cols];
        int[] wordCounts = new int[cols];

        // measure headers
        for (int i = 0; i < cols; i++) {
            maxContent[i] = textWidth(FONT_BOLD, FONT_SIZE_SMALL, headers[i]) + 20f; // Generous padding
        }

        // measure data and track word counts for wrapping decisions
        for (String[] row : data) {
            for (int c = 0; c < cols && c < row.length; c++) {
                String s = row[c] == null ? "" : row[c];
                float w = textWidth(FONT_REGULAR, FONT_SIZE_SMALL, s) + 20f; // Generous padding
                if (w > maxContent[c]) maxContent[c] = w;
                
                avgContent[c] += w;
                wordCounts[c] += s.split("\\s+").length;
            }
        }

        // Calculate averages
        for (int c = 0; c < cols; c++) {
            if (data.length > 0) avgContent[c] /= data.length;
        }

        float totalNeed = 0f;
        for (float w : maxContent) totalNeed += w;

        float[] result = new float[cols];
        final float MIN_COL = 90f; // Increased minimum column width for better text display
        
        if (totalNeed <= tableWidth) {
            // distribute remaining space proportionally
            float remaining = tableWidth;
            int flexible = 0;
            for (int i = 0; i < cols; i++) {
                if (maxContent[i] < MIN_COL) {
                    result[i] = MIN_COL;
                    remaining -= MIN_COL;
                } else {
                    flexible++;
                }
            }
            if (flexible > 0) {
                float per = remaining / flexible;
                for (int i = 0; i < cols; i++) {
                    if (result[i] == 0) result[i] = Math.max(maxContent[i], per);
                }
            }
        } else {
            // Need to compress - use smart allocation based on content type
            for (int i = 0; i < cols; i++) {
                // If column has long words (likely needs wrapping), give it more space
                float wordLengthFactor = wordCounts[i] > 0 ? (avgContent[i] / wordCounts[i]) : 1.0f;
                float allocation = tableWidth * (maxContent[i] / totalNeed);
                
                // Boost allocation for columns with long words
                if (wordLengthFactor > 30f) {
                    allocation *= 1.2f;
                }
                
                result[i] = Math.max(MIN_COL, allocation);
            }
            
            // Normalize to exactly tableWidth
            float assigned = 0f;
            for (float r : result) assigned += r;
            if (assigned > tableWidth) {
                // Scale down proportionally
                for (int i = 0; i < cols; i++) {
                    result[i] = result[i] * (tableWidth / assigned);
                }
            } else {
                // Distribute extra space
                float diff = tableWidth - assigned;
                for (int i = 0; i < cols; i++) {
                    result[i] += (diff * (result[i] / assigned));
                }
            }
        }
        return result;
    }

    // ------------------------
    //  Utility: detect numeric-like strings (simple)
    // ------------------------
    private static boolean isNumericLike(String s) {
        if (s == null) return false;
        String t = s.trim().replace(",", "").replace("R", "").replace("$", "");
        if (t.isEmpty()) return false;
        try {
            Double.parseDouble(t);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Calculate custom column widths for Annual Budget table with 14 columns.
     * Gives more space to Category column (first column) and Annual Amount,
     * while distributing remaining space among 12 month columns.
     */
    public static float[] calculateAnnualBudgetColumnWidths(float tableWidth) {
        float[] widths = new float[14];
        
        widths[0] = tableWidth * 0.15f;  // Category column - 15%
        widths[1] = tableWidth * 0.10f;  // Annual Amount - 10%
        
        // Remaining 75% divided equally among 12 months = 6.25% each
        float monthColumnWidth = tableWidth * 0.0625f;
        
        for (int i = 2; i < 14; i++) {
            widths[i] = monthColumnWidth;  // Each month gets 6.0%
        }
        
        return widths;
    }

    /**
     * Overloaded drawTable that accepts pre-calculated column widths.
     * Useful for tables that need custom column width allocation.
     */
    public static float drawTable(PDPageContentStream cs, PDDocument doc, String[] headers, String[][] data,
                                  float startY, float rowHeight, float pageContentWidth, float pageMarginLeft,
                                  float[] customColumnWidths) throws IOException {
        float currentY = startY;
        float tableWidth = pageContentWidth;

        float[] columnWidths = customColumnWidths;

        // Calculate table height to draw border (we don't paginate here — keep simple)
        float tableHeight = (data.length + 1) * rowHeight;
        cs.setLineWidth(1f);
        cs.addRect(pageMarginLeft, currentY - tableHeight, tableWidth, tableHeight);
        cs.stroke();

        // Header background
        cs.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        cs.addRect(pageMarginLeft + 1, currentY - rowHeight + 1, tableWidth - 2, rowHeight - 2);
        cs.fill();
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Draw header text with padding
        float x = pageMarginLeft + 6;
        float headerTextY = currentY - rowHeight + (rowHeight - FONT_SIZE_SMALL) / 2f + 2;
        cs.setFont(FONT_BOLD, FONT_SIZE_SMALL);
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(x, headerTextY);
            cs.showText(headers[i]);
            cs.endText();
            x += columnWidths[i];
        }

        currentY -= rowHeight;

        // Draw data rows with wrapping support
        cs.setFont(FONT_REGULAR, FONT_SIZE_SMALL);
        for (String[] row : data) {
            x = pageMarginLeft + 6;
            
            // Calculate max lines needed for this row based on rowHeight
            int maxLines = (int) Math.floor(rowHeight / (FONT_SIZE_SMALL + 2));
            float textStartY = currentY - (rowHeight - FONT_SIZE_SMALL) / 2f;

            for (int col = 0; col < row.length && col < columnWidths.length; col++) {
                String cellText = row[col] == null ? "" : row[col];
                float colWidth = columnWidths[col] - 12; // padding
                
                List<String> wrappedLines = wrapText(cellText, FONT_REGULAR, FONT_SIZE_SMALL, colWidth);
                
                // Only show maxLines, append "..." if truncated AND has content after truncation
                boolean truncated = wrappedLines.size() > maxLines;
                List<String> displayLines = wrappedLines.subList(0, Math.min(maxLines, wrappedLines.size()));
                
                // Check if remaining lines have non-empty content
                boolean hasRemainingContent = false;
                if (truncated) {
                    for (int i = maxLines; i < wrappedLines.size(); i++) {
                        if (!wrappedLines.get(i).trim().isEmpty()) {
                            hasRemainingContent = true;
                            break;
                        }
                    }
                }
                
                if (truncated && hasRemainingContent && !displayLines.isEmpty()) {
                    String lastLine = displayLines.get(displayLines.size() - 1);
                    displayLines.set(displayLines.size() - 1, lastLine + "...");
                }

                float lineY = textStartY;
                for (String line : displayLines) {
                    if (line.trim().isEmpty()) continue; // Skip empty lines
                    
                    cs.beginText();
                    cs.newLineAtOffset(x, lineY);
                    cs.showText(line);
                    cs.endText();
                    lineY -= (FONT_SIZE_SMALL + 2);
                }

                x += columnWidths[col];
            }

            currentY -= rowHeight;

            // Draw horizontal line between rows
            cs.setLineWidth(0.5f);
            cs.moveTo(pageMarginLeft, currentY);
            cs.lineTo(pageMarginLeft + tableWidth, currentY);
            cs.stroke();
            cs.setLineWidth(1f);
        }

        // Draw vertical lines
        float columnX = pageMarginLeft;
        for (float w : columnWidths) {
            cs.moveTo(columnX, startY);
            cs.lineTo(columnX, currentY);
            cs.stroke();
            columnX += w;
        }
        cs.moveTo(columnX, startY);
        cs.lineTo(columnX, currentY);
        cs.stroke();

        return currentY;
    }

    /**
     * Overloaded drawTable with custom font size for narrow tables with many columns.
     * Useful for tables like Annual Budget where values need to fit in narrow month columns.
     * 
     * @param fontSize Custom font size (recommended: 8f for wide tables, 10f for normal)
     */
    public static float drawTable(PDPageContentStream cs, PDDocument doc, String[] headers, String[][] data,
                                  float startY, float rowHeight, float pageContentWidth, float pageMarginLeft,
                                  float[] customColumnWidths, float fontSize) throws IOException {
        float currentY = startY;
        float tableWidth = pageContentWidth;

        float[] columnWidths = customColumnWidths;

        // Calculate table height to draw border
        float tableHeight = (data.length + 1) * rowHeight;
        cs.setLineWidth(1f);
        cs.addRect(pageMarginLeft, currentY - tableHeight, tableWidth, tableHeight);
        cs.stroke();

        // Header background
        cs.setNonStrokingColor(COLOR_LIGHT_GRAY[0], COLOR_LIGHT_GRAY[1], COLOR_LIGHT_GRAY[2]);
        cs.addRect(pageMarginLeft + 1, currentY - rowHeight + 1, tableWidth - 2, rowHeight - 2);
        cs.fill();
        cs.setNonStrokingColor(COLOR_BLACK[0], COLOR_BLACK[1], COLOR_BLACK[2]);

        // Draw header text with padding (using custom font size)
        float x = pageMarginLeft + 6;
        float headerTextY = currentY - rowHeight + (rowHeight - fontSize) / 2f + 2;
        cs.setFont(FONT_BOLD, fontSize);
        for (int i = 0; i < headers.length; i++) {
            cs.beginText();
            cs.newLineAtOffset(x, headerTextY);
            cs.showText(headers[i]);
            cs.endText();
            x += columnWidths[i];
        }

        currentY -= rowHeight;

        // Draw data rows with wrapping support (using custom font size)
        cs.setFont(FONT_REGULAR, fontSize);
        for (String[] row : data) {
            x = pageMarginLeft + 6;
            
            // Calculate max lines needed for this row based on rowHeight and custom fontSize
            int maxLines = (int) Math.floor(rowHeight / (fontSize + 2));
            float textStartY = currentY - (rowHeight - fontSize) / 2f;

            for (int col = 0; col < row.length && col < columnWidths.length; col++) {
                String cellText = row[col] == null ? "" : row[col];
                float colWidth = columnWidths[col] - 12; // padding
                
                List<String> wrappedLines = wrapText(cellText, FONT_REGULAR, fontSize, colWidth);
                
                // Only show maxLines, append "..." if truncated AND has content after truncation
                boolean truncated = wrappedLines.size() > maxLines;
                List<String> displayLines = wrappedLines.subList(0, Math.min(maxLines, wrappedLines.size()));
                
                // Check if remaining lines have non-empty content
                boolean hasRemainingContent = false;
                if (truncated) {
                    for (int i = maxLines; i < wrappedLines.size(); i++) {
                        if (!wrappedLines.get(i).trim().isEmpty()) {
                            hasRemainingContent = true;
                            break;
                        }
                    }
                }
                
                if (truncated && hasRemainingContent && !displayLines.isEmpty()) {
                    String lastLine = displayLines.get(displayLines.size() - 1);
                    displayLines.set(displayLines.size() - 1, lastLine + "...");
                }

                float lineY = textStartY;
                for (String line : displayLines) {
                    if (line.trim().isEmpty()) continue; // Skip empty lines
                    
                    cs.beginText();
                    cs.newLineAtOffset(x, lineY);
                    cs.showText(line);
                    cs.endText();
                    lineY -= (fontSize + 2);
                }

                x += columnWidths[col];
            }

            currentY -= rowHeight;

            // Draw horizontal line between rows
            cs.setLineWidth(0.5f);
            cs.moveTo(pageMarginLeft, currentY);
            cs.lineTo(pageMarginLeft + tableWidth, currentY);
            cs.stroke();
            cs.setLineWidth(1f);
        }

        // Draw vertical lines
        float columnX = pageMarginLeft;
        for (float w : columnWidths) {
            cs.moveTo(columnX, startY);
            cs.lineTo(columnX, currentY);
            cs.stroke();
            columnX += w;
        }
        cs.moveTo(columnX, startY);
        cs.lineTo(columnX, currentY);
        cs.stroke();

        return currentY;
    }

    // ------------------------
    //  Currency formatting utility
    // ------------------------
    public static String formatCurrency(double value, Locale locale) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale == null ? Locale.getDefault() : locale);
        return nf.format(value);
    }
}
