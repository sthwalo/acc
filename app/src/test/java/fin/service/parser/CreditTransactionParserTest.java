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

import fin.context.TransactionParsingContext;
import fin.model.parser.ParsedTransaction;
import fin.model.parser.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class CreditTransactionParserTest {
    private CreditTransactionParser parser;
    private TransactionParsingContext context;

    @BeforeEach
    void setUp() {
        parser = new CreditTransactionParser();
        context = new TransactionParsingContext.Builder()
            .accountNumber("20 316 375 3")
            .statementDate(LocalDate.of(2025, 3, 12))
            .statementPeriod("15 February 2025 to 15 March 2025")
            .sourceFile("xxxxx3753 (14).pdf")
            .build();
    }

    @Test
    void canParseReturnsTrueForCreditTransactions() {
        assertTrue(parser.canParse("CREDIT TRANSFER FROM JOHN DOE 1,500.00", context));
        assertTrue(parser.canParse("DEPOSIT REF 123456 500.00", context));
        assertTrue(parser.canParse("PAYMENT FROM CUSTOMER 750.50", context));
    }

    @Test
    void canParseReturnsFalseForNonCreditTransactions() {
        assertFalse(parser.canParse("IB PAYMENT TO VENDOR", context));
        assertFalse(parser.canParse("FEE-ELECTRONIC ACCOUNT PAYMENT 8.90-", context));
        assertFalse(parser.canParse("DEBIT ORDER PAYMENT", context));
    }

    @Test
    void parseExtractsTransactionAmount() {
        String line = "CREDIT TRANSFER FROM JOHN DOE 1,500.00";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("1500.00"), result.getAmount());
        assertEquals("CREDIT TRANSFER FROM JOHN DOE", result.getDescription().trim());
        assertEquals(context.getStatementDate(), result.getDate());
    }

    @Test
    void parseHandlesSimpleDeposit() {
        String line = "DEPOSIT REF 123456 500.00";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals("DEPOSIT REF 123456", result.getDescription().trim());
    }

    @Test
    void parseHandlesPaymentFrom() {
        String line = "PAYMENT FROM CUSTOMER 750.50";
        ParsedTransaction result = parser.parse(line, context);
        
        assertNotNull(result);
        assertEquals(TransactionType.CREDIT, result.getType());
        assertEquals(new BigDecimal("750.50"), result.getAmount());
        assertEquals("PAYMENT FROM CUSTOMER", result.getDescription().trim());
    }
}
