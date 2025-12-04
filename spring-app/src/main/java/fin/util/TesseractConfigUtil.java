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

package fin.util;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Utility class for configuring Tesseract OCR engine and JNA native library paths.
 * Handles platform-specific library path configuration for macOS Homebrew installations.
 */
public final class TesseractConfigUtil {

    private static final Logger logger = LoggerFactory.getLogger(TesseractConfigUtil.class);
    
    private static final String HOMEBREW_LIB_PATH = "/opt/homebrew/lib";
    private static final String HOMEBREW_TESSDATA_PATH = "/opt/homebrew/share/tessdata";
    
    private static boolean jnaConfigured = false;
    private static Tesseract tesseractInstance = null;

    private TesseractConfigUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Configure JNA library path for Tesseract native library.
     * This method is idempotent - it only configures once per JVM instance.
     */
    public static synchronized void configureJnaLibraryPath() {
        if (jnaConfigured) {
            return;
        }

        String currentLibPath = System.getProperty("jna.library.path");
        
        if (currentLibPath == null || !currentLibPath.contains(HOMEBREW_LIB_PATH)) {
            String newLibPath = HOMEBREW_LIB_PATH + 
                               (currentLibPath != null ? ":" + currentLibPath : "");
            System.setProperty("jna.library.path", newLibPath);
            logger.info("Configured JNA library path for Tesseract: {}", newLibPath);
        }
        
        jnaConfigured = true;
    }

    /**
     * Get or create a configured Tesseract instance (singleton pattern).
     * 
     * @return Configured Tesseract instance ready for OCR operations
     */
    private static synchronized Tesseract getTesseractInstance() {
        if (tesseractInstance == null) {
            // Ensure JNA is configured before creating Tesseract instance
            configureJnaLibraryPath();
            
            tesseractInstance = new Tesseract();
            
            // Set Tesseract data path (Homebrew installation on macOS)
            tesseractInstance.setDatapath(HOMEBREW_TESSDATA_PATH);
            
            // Configure for English language
            tesseractInstance.setLanguage("eng");
            
            // Automatic page segmentation with Orientation and Script Detection
            tesseractInstance.setPageSegMode(1);
            
            // Neural nets LSTM engine for better accuracy
            tesseractInstance.setOcrEngineMode(1);
            
            logger.info("Initialized Tesseract OCR engine with tessdata path: {}", HOMEBREW_TESSDATA_PATH);
        }
        
        return tesseractInstance;
    }

    /**
     * Get the configured tessdata path.
     * 
     * @return Path to Tesseract training data
     */
    public static String getTessdataPath() {
        return HOMEBREW_TESSDATA_PATH;
    }

    /**
     * Get the configured native library path.
     * 
     * @return Path to Tesseract native library
     */
    public static String getLibraryPath() {
        return HOMEBREW_LIB_PATH;
    }

    /**
     * Check if JNA configuration has been applied.
     * 
     * @return true if JNA library path has been configured
     */
    public static boolean isJnaConfigured() {
        return jnaConfigured;
    }

    /**
     * Perform OCR on a buffered image using Tesseract.
     * 
     * @param image The image to perform OCR on
     * @return Extracted text from the image
     * @throws IOException if OCR extraction fails
     */
    public static synchronized String performOCR(BufferedImage image) throws IOException {
        try {
            Tesseract tesseract = getTesseractInstance();
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            logger.error("OCR extraction failed", e);
            throw new IOException("Failed to perform OCR on image: " + e.getMessage(), e);
        }
    }
    
    /**
     * Reset the Tesseract instance (useful for testing or configuration changes).
     */
    static synchronized void resetInstance() {
        tesseractInstance = null;
        jnaConfigured = false;
    }
}
