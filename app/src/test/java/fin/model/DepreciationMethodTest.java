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

package fin.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for DepreciationMethod enum
 * Tests all enum values, descriptions, and behavior
 */
class DepreciationMethodTest {

    @Test
    void testAllEnumValues() {
        // Test that all expected enum values exist
        DepreciationMethod[] methods = DepreciationMethod.values();
        assertEquals(3, methods.length, "Should have exactly 3 depreciation methods");

        // Verify each method exists
        assertNotNull(DepreciationMethod.STRAIGHT_LINE);
        assertNotNull(DepreciationMethod.DECLINING_BALANCE);
        assertNotNull(DepreciationMethod.FIN);
    }

    @Test
    void testStraightLineMethod() {
        DepreciationMethod method = DepreciationMethod.STRAIGHT_LINE;
        assertEquals("STRAIGHT_LINE", method.name());
        assertEquals("Straight-Line Depreciation", method.getDescription());
    }

    @Test
    void testDecliningBalanceMethod() {
        DepreciationMethod method = DepreciationMethod.DECLINING_BALANCE;
        assertEquals("DECLINING_BALANCE", method.name());
        assertEquals("Declining Balance Depreciation", method.getDescription());
    }

    @Test
    void testFinMethod() {
        DepreciationMethod fin = DepreciationMethod.FIN;
        assertEquals("FIN", fin.getCode());
        assertEquals("FIN Depreciation (Half-Year Convention)", fin.getDescription());
        assertEquals("FIN Depreciation (Half-Year Convention)", fin.toString());
    }

    @Test
    void testValueOf() {
        // Test valueOf method works for all enum values
        assertEquals(DepreciationMethod.STRAIGHT_LINE, DepreciationMethod.valueOf("STRAIGHT_LINE"));
        assertEquals(DepreciationMethod.DECLINING_BALANCE, DepreciationMethod.valueOf("DECLINING_BALANCE"));
        assertEquals(DepreciationMethod.FIN, DepreciationMethod.valueOf("FIN"));
    }

    @Test
    void testDescriptionsAreUnique() {
        // Ensure all descriptions are unique
        String slDesc = DepreciationMethod.STRAIGHT_LINE.getDescription();
        String dbDesc = DepreciationMethod.DECLINING_BALANCE.getDescription();
        String finDesc = DepreciationMethod.FIN.getDescription();

        assertNotEquals(slDesc, dbDesc);
        assertNotEquals(slDesc, finDesc);
        assertNotEquals(dbDesc, finDesc);
    }

    @Test
    void testOrdinalValues() {
        // Test ordinal values are sequential starting from 0
        assertEquals(0, DepreciationMethod.STRAIGHT_LINE.ordinal());
        assertEquals(1, DepreciationMethod.DECLINING_BALANCE.ordinal());
        assertEquals(2, DepreciationMethod.FIN.ordinal());
    }
}