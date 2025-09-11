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
package fin.service.parser;

import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ServiceFeeParserTest {
    private ServiceFeeParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new ServiceFeeParser();
        context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
    }

    @Test
    void canParseReturnsTrueForServiceFees() {
        assertTrue(parser.canParse("FEE-ELECTRONIC ACCOUNT PAYMENT 203163753 ## 8.90-", context));
        assertTrue(parser.canParse("FEE - PAYMENT CONFIRM - EMAIL ## 0.80-", context));
        assertTrue(parser.canParse("SERVICE FEE  35.00-", context));
    }

    @Test
    void canParseReturnsFalseForNonFees() {
        assertFalse(parser.canParse("IB PAYMENT TO NTSAKO MAPHOSA", context));
        assertFalse(parser.canParse("CREDIT TRANSFER", context));
        assertFalse(parser.canParse("Fee Debits Credits Date Balance", context)); // Table header
        assertFalse(parser.canParse("Date Details Debit Credit Balance", context)); // Table header
    }

    @Test
    void parseExtractsServiceFeeAmount() {
        String line = "FEE-ELECTRONIC ACCOUNT PAYMENT 203163753 ## 8.90-";
        ParsedTransaction fee = parser.parse(line, context);
        
        assertNotNull(fee);
        assertEquals(TransactionType.SERVICE_FEE, fee.getType());
        assertEquals(new BigDecimal("8.90"), fee.getAmount());
        assertEquals("FEE-ELECTRONIC ACCOUNT PAYMENT 203163753", fee.getDescription().trim());
    }

    @Test
    void parseHandlesEmailConfirmationFee() {
        String line = "FEE: PAYMENT CONFIRM - EMAIL 0.80- ##";
        ParsedTransaction fee = parser.parse(line, context);
        
        assertNotNull(fee);
        assertEquals(TransactionType.SERVICE_FEE, fee.getType());
        assertEquals(new BigDecimal("0.80"), fee.getAmount());
        assertEquals("FEE: PAYMENT CONFIRM - EMAIL", fee.getDescription().trim());
    }
}
