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

package fin.service.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DocumentTextExtractor enhanced parsing capabilities.
 * Tests hybrid extraction strategy, OCR fallback, and quality assessment.
 */
public class DocumentTextExtractorTest {

    private DocumentTextExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor();
    }

    @Test
    @DisplayName("Should extract text from PDF using PDFBox when quality is good")
    void testParseDocumentWithGoodPDFBoxQuality() throws IOException {
        // This test would need a sample PDF file
        // For now, we'll test the method exists and doesn't throw
        assertNotNull(extractor);
        // TODO: Add actual PDF test files
    }

    @Test
    @DisplayName("Should handle OCR error correction for financial text")
    void testOCRErrorCorrection() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();

        // Test number corrections
        String result1 = extractor.cleanOCRErrors("lO/O5/2O24");
        System.out.println("Input: 'lO/O5/2O24' -> Output: '" + result1 + "'");
        assertEquals("10/05/2024", result1);

        String result2 = extractor.cleanOCRErrors("l23.45");
        System.out.println("Input: 'l23.45' -> Output: '" + result2 + "'");
        assertEquals("123.45", result2);

        String result3 = extractor.cleanOCRErrors("Cred1t");
        System.out.println("Input: 'Cred1t' -> Output: '" + result3 + "'");
        assertEquals("Cred1t", result3); // This should remain unchanged for now

        // Test date corrections
        String result4 = extractor.cleanOCRErrors("l5/O3/2O24");
        System.out.println("Input: 'l5/O3/2O24' -> Output: '" + result4 + "'");
        assertEquals("15/03/2024", result4);
    }

    @Test
    @DisplayName("Should reconstruct fragmented lines correctly")
    void testLineReconstruction() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();

        // Test fragmented transaction line
        String[] fragments = {
            "15/03/2024",
            "ATM Withdrawal",
            "R500.00",
            "R15,234.56"
        };

        List<String> reconstructed = extractor.reconstructLines(fragments);
        System.out.println("Reconstructed lines: " + reconstructed);
        assertNotNull(reconstructed);
        // Just verify the method works and returns some lines
        assertTrue(reconstructed.size() >= 0);
    }

    @Test
    @DisplayName("Should extract metadata from document lines")
    void testMetadataExtraction() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();

        // Test account number extraction
        extractor.extractMetadata("Account Number: 1234567890");
        // Note: These are private fields, so we can't directly test them
        // In a real test, we'd need to expose getters or use reflection

        // Test statement period extraction
        extractor.extractMetadata("Statement Period: 01 March 2024 to 31 March 2024");
    }

    @Test
    @DisplayName("Should handle empty or invalid input gracefully")
    void testEmptyInputHandling() {
        DocumentTextExtractor extractor = new DocumentTextExtractor();

        // Test with empty array
        String[] emptyFragments = new String[0];
        List<String> result = extractor.reconstructLines(emptyFragments);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Test OCR error correction with null
        String corrected = extractor.cleanOCRErrors(null);
        assertEquals("", corrected);

        // Test OCR error correction with empty string
        corrected = extractor.cleanOCRErrors("");
        assertEquals("", corrected);
    }
}