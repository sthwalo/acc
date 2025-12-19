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
    
    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            Process p = pb.start();
            boolean exited = p.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
            return exited && p.exitValue() == 0;
        } catch (Throwable t) {
            return false;
        }
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
            return results;
        } catch (Throwable t) {
            logger.error("PDFRenderer/Tesseract OCR failed ({}) - attempting external rasterizer fallback: {}", t.getClass().getSimpleName(), t.getMessage());

            // External fallback using pdftoppm if available
            if (!isCommandAvailable("pdftoppm")) {
                throw new IOException("OCR fallback failed and pdftoppm is not available on PATH: " + t.getMessage());
            }

            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("pos_text_raster_");
            String outPrefix = tempDir.resolve("page").toString();

            ProcessBuilder pb = new ProcessBuilder("pdftoppm", "-jpeg", "-r", "300", pdfFile.getAbsolutePath(), outPrefix);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try {
                boolean finished = process.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    throw new IOException("pdftoppm timed out");
                }
                if (process.exitValue() != 0) {
                    String out = new String(process.getInputStream().readAllBytes());
                    throw new IOException("pdftoppm failed: " + out);
                }

                java.nio.file.DirectoryStream<java.nio.file.Path> stream = java.nio.file.Files.newDirectoryStream(tempDir, "page*.jpg");
                for (java.nio.file.Path imgPath : stream) {
                    BufferedImage img = javax.imageio.ImageIO.read(imgPath.toFile());
                    if (img == null) continue;
                    ITesseract tesseract2 = new Tesseract();
                    tesseract2.setDatapath("/opt/homebrew/share/tessdata");
                    tesseract2.setLanguage("eng");
                    tesseract2.setPageSegMode(1);
                    tesseract2.setOcrEngineMode(1);

                    List<Word> words = tesseract2.getWords(img, 0);
                    for (Word word : words) {
                        String text = word.getText().trim();
                        if (!text.isEmpty()) {
                            float x = word.getBoundingBox().x * 72.0f / 300.0f;
                            float y = word.getBoundingBox().y * 72.0f / 300.0f;
                            results.add(new PositionedText(text, x, y));
                        }
                    }
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("pdftoppm interrupted", ie);
            } finally {
                try {
                    java.nio.file.Files.walk(tempDir)
                        .map(java.nio.file.Path::toFile)
                        .sorted((a,b) -> -a.getAbsolutePath().compareTo(b.getAbsolutePath()))
                        .forEach(java.io.File::delete);
                } catch (Throwable ignored) {}
            }

            logger.info("Extracted {} positioned elements via external rasterizer OCR", results.size());
            return results;
        }
    }
    
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
        StringBuilder currentWord = new StringBuilder();
        TextPosition firstPos = null;
        float lastX = 0;
        for (int i = 0; i < textPositions.size(); i++) {
            TextPosition pos = textPositions.get(i);
            String str = pos.getUnicode();
            if (str.trim().isEmpty()) {
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

