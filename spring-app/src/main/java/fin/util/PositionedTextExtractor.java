/*
 * Copyright (c) 2024 Immaculate Nyoni <sthwaloe@gmail.com>
 * Licensed under the MIT License. See LICENSE file in the project root.
 */
package fin.util;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts text with X,Y coordinates from both PDFBox (text-based PDFs)
 * and Tesseract OCR (image-based PDFs).
 */
public class PositionedTextExtractor {
    
    private static final Logger logger = LoggerFactory.getLogger(PositionedTextExtractor.class);
    
    /**
     * Extract positioned text from PDF.
     * Tries PDFBox first (for text-based PDFs), falls back to OCR if needed.
     */
    public static List<PositionedText> extractPositionedText(File pdfFile) throws Exception {
        // Try PDFBox first
        List<PositionedText> pdfboxResult = extractWithPDFBox(pdfFile);
        if (!pdfboxResult.isEmpty()) {
            logger.info("Extracted {} positioned elements with PDFBox", pdfboxResult.size());
            return pdfboxResult;
        }
        
        // Fall back to OCR
        logger.info("PDFBox extraction empty, falling back to OCR");
        return extractWithOCR(pdfFile);
    }
    
    /**
     * Extract positioned text using PDFBox TextPosition.
     */
    private static List<PositionedText> extractWithPDFBox(File pdfFile) throws IOException {
        List<PositionedText> results = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PositionCapturingStripper stripper = new PositionCapturingStripper();
            stripper.getText(document);
            results = stripper.getPositionedTexts();
        }
        
        return results;
    }
    
    /**
     * Extract positioned text using Tesseract OCR.
     */
    private static List<PositionedText> extractWithOCR(File pdfFile) throws Exception {
        List<PositionedText> results = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            ITesseract tesseract = new Tesseract();
            
            // Configure Tesseract
            tesseract.setDatapath("/opt/homebrew/share/tessdata");
            tesseract.setLanguage("eng");
            tesseract.setPageSegMode(1); // Auto page segmentation with OSD
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only
            
            // Process each page
            for (int pageNum = 0; pageNum < document.getNumberOfPages(); pageNum++) {
                logger.info("OCR processing page {}", pageNum + 1);
                
                // Render page to image at high DPI for better OCR accuracy
                BufferedImage image = renderer.renderImageWithDPI(pageNum, 300);
                
                // Get words with bounding boxes
                List<Word> words = tesseract.getWords(image, 0);
                
                for (Word word : words) {
                    String text = word.getText().trim();
                    if (!text.isEmpty()) {
                        // Tesseract uses pixels, convert to PDF coordinates (assuming 72 DPI PDF)
                        float x = word.getBoundingBox().x * 72.0f / 300.0f;
                        float y = word.getBoundingBox().y * 72.0f / 300.0f;
                        
                        results.add(new PositionedText(text, x, y));
                    }
                }
            }
            
            logger.info("Extracted {} positioned elements with OCR", results.size());
        }
        
        return results;
    }
    
    /**
     * Custom PDFTextStripper that captures text positions.
     */
    private static class PositionCapturingStripper extends PDFTextStripper {
        private final List<PositionedText> positionedTexts = new ArrayList<>();
        
        PositionCapturingStripper() throws IOException {
            super();
            setSortByPosition(true);
        }
        
        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (textPositions.isEmpty() || text.trim().isEmpty()) {
                return;
            }
            
            // Group consecutive characters into words
            StringBuilder currentWord = new StringBuilder();
            TextPosition firstPos = null;
            float lastX = 0;
            
            for (int i = 0; i < textPositions.size(); i++) {
                TextPosition pos = textPositions.get(i);
                String str = pos.getUnicode();
                
                if (str.trim().isEmpty()) {
                    // Space - end current word
                    if (currentWord.length() > 0 && firstPos != null) {
                        positionedTexts.add(new PositionedText(
                            currentWord.toString(),
                            firstPos.getXDirAdj(),
                            firstPos.getYDirAdj()
                        ));
                        currentWord = new StringBuilder();
                        firstPos = null;
                    }
                } else {
                    // Check if this is a new word (significant gap)
                    if (currentWord.length() > 0 && (pos.getXDirAdj() - lastX) > 5.0f) {
                        positionedTexts.add(new PositionedText(
                            currentWord.toString(),
                            firstPos.getXDirAdj(),
                            firstPos.getYDirAdj()
                        ));
                        currentWord = new StringBuilder();
                        firstPos = null;
                    }
                    
                    if (firstPos == null) {
                        firstPos = pos;
                    }
                    currentWord.append(str);
                    lastX = pos.getXDirAdj() + pos.getWidthDirAdj();
                }
            }
            
            // Add final word
            if (currentWord.length() > 0 && firstPos != null) {
                positionedTexts.add(new PositionedText(
                    currentWord.toString(),
                    firstPos.getXDirAdj(),
                    firstPos.getYDirAdj()
                ));
            }
        }
        
        List<PositionedText> getPositionedTexts() {
            return positionedTexts;
        }
    }
}
