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

package fin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Configures Apache PDFBox for safe operation in containerized environments.
 * Sets headless mode, font cache paths, and tests PDFBox availability.
 * Provides health status for PDFBox operations.
 *
 * SINGLE RESPONSIBILITY: PDFBox configuration and health monitoring
 */
@Component
public class PdfBoxConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(PdfBoxConfigurator.class);

    private volatile boolean pdfBoxAvailable = false;
    private volatile String pdfBoxStatus = "UNKNOWN";

    /**
     * Configure PDFBox at application startup.
     * Sets system properties and tests PDFBox availability.
     */
    @PostConstruct
    public void configurePdfBox() {
        logger.info("Initializing PDFBox configuration for container compatibility");

        try {
            // Set headless mode to prevent GUI dependencies
            System.setProperty("java.awt.headless", "true");
            logger.info("Set java.awt.headless=true for container compatibility");

            // Configure PDFBox font cache to avoid file system issues
            System.setProperty("pdfbox.fontcache", "/tmp/pdfbox-fontcache");
            logger.info("Set PDFBox font cache to /tmp/pdfbox-fontcache");

            // Test PDFBox availability by attempting to load core classes
            testPdfBoxAvailability();

            if (pdfBoxAvailable) {
                logger.info("PDFBox configuration successful - PDFBox is available for use");
            } else {
                logger.warn("PDFBox configuration completed but PDFBox is not available - OCR fallback will be used");
            }

        } catch (Exception e) {
            logger.error("Failed to configure PDFBox: {}", e.getMessage(), e);
            pdfBoxStatus = "CONFIGURATION_FAILED: " + e.getMessage();
            pdfBoxAvailable = false;
        }
    }

    /**
     * Test PDFBox availability by attempting to load and use core classes.
     * This prevents runtime failures during actual PDF processing.
     */
    private void testPdfBoxAvailability() {
        try {
            // Test basic PDFBox class loading
            Class.forName("org.apache.pdfbox.Loader");
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class.forName("org.apache.pdfbox.text.PDFTextStripper");

            // Test font mapper initialization (the problematic area)
            Class.forName("org.apache.pdfbox.pdmodel.font.FontMapperImpl");

            // Try to create a minimal PDF document to test full functionality
            try (org.apache.pdfbox.pdmodel.PDDocument testDoc = new org.apache.pdfbox.pdmodel.PDDocument()) {
                // Add a blank page
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                testDoc.addPage(page);

                // Try basic text extraction setup
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                String emptyText = stripper.getText(testDoc);

                if (emptyText != null) {
                    pdfBoxAvailable = true;
                    pdfBoxStatus = "AVAILABLE";
                    logger.debug("PDFBox availability test passed");
                } else {
                    throw new RuntimeException("PDFBox text extraction returned null");
                }
            }

        } catch (ClassNotFoundException e) {
            pdfBoxStatus = "DEPENDENCY_MISSING: " + e.getMessage();
            logger.warn("PDFBox classes not found: {}", e.getMessage());
        } catch (NoClassDefFoundError e) {
            pdfBoxStatus = "CLASS_INIT_ERROR: " + e.getMessage();
            logger.warn("PDFBox class initialization failed: {}", e.getMessage());
        } catch (ExceptionInInitializerError e) {
            pdfBoxStatus = "INITIALIZER_ERROR: " + e.getMessage();
            logger.warn("PDFBox static initializer failed: {}", e.getMessage());
        } catch (Exception e) {
            pdfBoxStatus = "TEST_FAILED: " + e.getMessage();
            logger.warn("PDFBox functionality test failed: {}", e.getMessage());
        }
    }

    /**
     * Check if PDFBox is available for use.
     * @return true if PDFBox can be used safely
     */
    public boolean isPdfBoxAvailable() {
        return pdfBoxAvailable;
    }

    /**
     * Get detailed PDFBox status information.
     * @return status string describing PDFBox availability
     */
    public String getPdfBoxStatus() {
        return pdfBoxStatus;
    }

    /**
     * Get health information for monitoring systems.
     * @return health status map
     */
    public java.util.Map<String, Object> getHealthInfo() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        health.put("pdfbox.available", pdfBoxAvailable);
        health.put("pdfbox.status", pdfBoxStatus);
        health.put("java.awt.headless", System.getProperty("java.awt.headless"));
        health.put("pdfbox.fontcache", System.getProperty("pdfbox.fontcache"));
        return health;
    }
}