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
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for credit transactions (deposits, transfers in, etc.)
 */
public class CreditTransactionParser implements TransactionParser {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("\\s*(\\d+,?\\d*\\.\\d{2})\\s*$");
    private static final String[] CREDIT_KEYWORDS = {
        "CREDIT TRANSFER",
        "DEPOSIT",
        "PAYMENT FROM",
        "TRANSFER FROM",
        "REAL TIME CREDIT",
        "REVERSAL",
        "REFUND"
    };

    @Override
    public boolean canParse(String line, TransactionParsingContext context) {
        if (line == null) return false;
        String upperLine = line.toUpperCase();
        return !upperLine.contains("SERVICE FEE") && // Not a service fee
               !upperLine.contains("##") && // Not a marked fee
               java.util.Arrays.stream(CREDIT_KEYWORDS)
                   .anyMatch(keyword -> upperLine.contains(keyword));
    }

    @Override
    public ParsedTransaction parse(String line, TransactionParsingContext context) {
        if (!canParse(line, context)) {
            throw new IllegalArgumentException("Cannot parse line: " + line);
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(line);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No amount found in line: " + line);
        }

        String amountStr = matcher.group(1).replace(",", "");
        BigDecimal amount = new BigDecimal(amountStr);

        // Extract description (everything before the amount)
        String description = line.substring(0, matcher.start()).trim();

        return new ParsedTransaction.Builder()
                .type(TransactionType.CREDIT)
                .description(description)
                .amount(amount)
                .date(context.getStatementDate())
                .build();
    }
}
