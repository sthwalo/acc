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
package fin.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DocumentTextExtractorTest {
    private DocumentTextExtractor extractor;
    
    @BeforeEach
    void setUp() {
        extractor = new DocumentTextExtractor();
    }
    
    @Test
    void isTransaction_WithTransactionLine_ReturnsTrue() {
        String[] transactions = {
            "BALANCE BROUGHT FORWARD",
            "IB PAYMENT TO NTSAKO MAPHOSA",
            "FEE-ELECTRONIC ACCOUNT PAYMENT ## 8.90-",
            "SERVICE FEE  35.00-"
        };
        
        for (String transaction : transactions) {
            assertTrue(extractor.isTransaction(transaction),
                       "Should identify '" + transaction + "' as transaction");
        }
    }
    
    @Test
    void isTransaction_WithHeaderLine_ReturnsFalse() {
        String[] headers = {
            "BRAAMFONTEIN PO BOX 62325",
            "Statement No: 3",
            "VAT Reg. No: 4640268068",
            "Page 1 of 15",
            "## These fees include VAT"
        };
        
        for (String header : headers) {
            assertFalse(extractor.isTransaction(header), 
                      "Should not identify '" + header + "' as transaction");
        }
    }
}
