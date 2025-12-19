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

            // Log JVM / vendor information to help diagnose environment-sensitive font issues
            try {
                logger.info("JVM info: java.version={} java.vendor={} java.vm.name={}",
                        System.getProperty("java.version"), System.getProperty("java.vendor"), System.getProperty("java.vm.name"));
            } catch (Throwable ignore) { }

            // Enumerate available font family names as a lightweight diagnostic
            try {
                String[] fonts = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
                logger.info("Number of available font families: {}", (fonts == null ? 0 : fonts.length));
                if (fonts != null && fonts.length > 0) {
                    for (int i = 0; i < Math.min(10, fonts.length); i++) {
                        logger.debug("Font[{}]: {}", i, fonts[i]);
                    }
                }
            } catch (Throwable t) {
                logger.warn("Unable to enumerate system fonts: {}", t.getMessage());
            }

            // Try to create a minimal PDF document to test full functionality, including rendering which can surface font-provider initialization errors
            try (org.apache.pdfbox.pdmodel.PDDocument testDoc = new org.apache.pdfbox.pdmodel.PDDocument()) {
                // Add a blank page
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                testDoc.addPage(page);

                // Try basic text extraction setup
                org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
                String emptyText = stripper.getText(testDoc);

                // Attempt a lightweight render which often triggers font-provider initialization failures
                try {
                    org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(testDoc);
                    java.awt.image.BufferedImage img = renderer.renderImageWithDPI(0, 72);
                    if (img != null) {
                        logger.debug("PDFRenderer produced a test image ({}x{})", img.getWidth(), img.getHeight());
                    }
                } catch (Throwable renderEx) {
                    logger.error("PDFRenderer test failed: {}", renderEx.getMessage(), renderEx);
                    String root = getRootCauseMessage(renderEx);
                    markPdfBoxUnavailable("RENDER_INIT_ERROR: " + root);
                    return; // abort availability marking
                }

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
     * Mark PDFBox as unavailable at runtime and record a reason. Use this when a specific
     * PDF triggers an initialization or font parsing error so subsequent operations
     * use OCR-only fallback without attempting PDFBox text extraction.
     */
    public void markPdfBoxUnavailable(String reason) {
        this.pdfBoxAvailable = false;
        this.pdfBoxStatus = "UNAVAILABLE: " + reason;
        logger.warn("PDFBox marked unavailable: {}", reason);
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
        health.put("java.version", System.getProperty("java.version"));
        health.put("java.vendor", System.getProperty("java.vendor"));
        return health;
    }

    /**
     * Walk the cause chain and return a compact root-cause message useful for status fields and logs.
     */
    private String getRootCauseMessage(Throwable t) {
        if (t == null) return "<no exception>";
        Throwable cur = t;
        String lastMsg = cur.getClass().getSimpleName() + ": " + (cur.getMessage() == null ? "" : cur.getMessage());
        while (cur.getCause() != null) {
            cur = cur.getCause();
            String m = cur.getClass().getSimpleName() + ": " + (cur.getMessage() == null ? "" : cur.getMessage());
            lastMsg = m;
        }
        // Trim to a reasonable length for health/status fields
        if (lastMsg.length() > 800) {
            return lastMsg.substring(0, 780) + "...";
        }
        return lastMsg;
    }
}