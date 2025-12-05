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
    
    // macOS Homebrew paths (for local development)
    private static final String HOMEBREW_LIB_PATH = "/opt/homebrew/lib";
    private static final String HOMEBREW_TESSDATA_PATH = "/opt/homebrew/share/tessdata";
    
    // Linux container paths (for Docker/production)
    private static final String LINUX_LIB_PATH = "/usr/lib/x86_64-linux-gnu";
    private static final String LINUX_TESSDATA_PATH = "/usr/share/tesseract-ocr/5/tessdata";
    private static final String LINUX_TESSDATA_PATH_ALT = "/usr/share/tesseract-ocr/tessdata";
    
    private static boolean jnaConfigured = false;
    private static Tesseract tesseractInstance = null;

    /**
     * Detect if running in a Linux container environment.
     * 
     * @return true if running in Linux container, false if macOS
     */
    private static boolean isLinuxContainer() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("linux");
    }
    
    /**
     * Get the appropriate library path based on the environment.
     * 
     * @return Library path for the current environment
     */
    private static String getLibraryPathForEnvironment() {
        return isLinuxContainer() ? LINUX_LIB_PATH : HOMEBREW_LIB_PATH;
    }
    
    /**
     * Get the appropriate tessdata path based on the environment.
     * 
     * @return Tessdata path for the current environment
     */
    private static String getTessdataPathForEnvironment() {
        if (isLinuxContainer()) {
            // Try the primary Linux path first
            if (new java.io.File(LINUX_TESSDATA_PATH).exists()) {
                return LINUX_TESSDATA_PATH;
            }
            // Fallback to alternative path
            if (new java.io.File(LINUX_TESSDATA_PATH_ALT).exists()) {
                return LINUX_TESSDATA_PATH_ALT;
            }
            // Last resort - try to find tessdata anywhere in /usr/share
            java.io.File tessdataDir = findTessdataDirectory();
            if (tessdataDir != null) {
                return tessdataDir.getAbsolutePath();
            }
            // Default fallback
            return LINUX_TESSDATA_PATH;
        } else {
            return HOMEBREW_TESSDATA_PATH;
        }
    }
    
    /**
     * Find tessdata directory in common Linux locations.
     * 
     * @return File object for tessdata directory, or null if not found
     */
    private static java.io.File findTessdataDirectory() {
        String[] possiblePaths = {
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/tessdata", 
            "/usr/share/tessdata",
            "/usr/local/share/tessdata"
        };
        
        for (String path : possiblePaths) {
            java.io.File dir = new java.io.File(path);
            if (dir.exists() && dir.isDirectory()) {
                java.io.File engFile = new java.io.File(dir, "eng.traineddata");
                if (engFile.exists()) {
                    logger.info("Found tessdata directory: {}", path);
                    return dir;
                }
            }
        }
        return null;
    }

    /**
     * Configure JNA library path for Tesseract native library.
     * This method is idempotent - it only configures once per JVM instance.
     */
    public static synchronized void configureJnaLibraryPath() {
        if (jnaConfigured) {
            return;
        }

        String libraryPath = getLibraryPathForEnvironment();
        String currentLibPath = System.getProperty("jna.library.path");
        
        if (currentLibPath == null || !currentLibPath.contains(libraryPath)) {
            String newLibPath = libraryPath + 
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
            
            // Set Tesseract data path based on environment
            String tessdataPath = getTessdataPathForEnvironment();
            tesseractInstance.setDatapath(tessdataPath);
            
            // Configure for English language
            tesseractInstance.setLanguage("eng");
            
            // Automatic page segmentation with Orientation and Script Detection
            tesseractInstance.setPageSegMode(1);
            
            // Neural nets LSTM engine for better accuracy
            tesseractInstance.setOcrEngineMode(1);
            
            logger.info("Initialized Tesseract OCR engine with tessdata path: {}", tessdataPath);
        }
        
        return tesseractInstance;
    }

    /**
     * Get the configured tessdata path.
     * 
     * @return Path to Tesseract training data for the current environment
     */
    public static String getTessdataPath() {
        return getTessdataPathForEnvironment();
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
