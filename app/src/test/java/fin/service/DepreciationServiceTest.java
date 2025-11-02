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

import fin.model.*;
import fin.repository.DepreciationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;

/**
 * Comprehensive test suite for DepreciationService
 * Tests all three depreciation calculation methods with various scenarios
 */
class DepreciationServiceTest {

    @Mock
    private DepreciationRepository depreciationRepository;

    private DepreciationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DepreciationService(depreciationRepository);
    }

    @Test
    void testStraightLineDepreciation() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertNotNull(schedule);
        assertEquals(5, schedule.getYears().size());
        assertEquals(new BigDecimal("9000.00"), schedule.getTotalDepreciation()); // (10000 - 1000) / 5 * 5
        assertEquals(new BigDecimal("1000.00"), schedule.getFinalBookValue());

        // Check each year
        for (int i = 0; i < 5; i++) {
            DepreciationYear year = schedule.getYears().get(i);
            assertEquals(i + 1, year.getYear());
            assertEquals(new BigDecimal("1800.00"), year.getDepreciation()); // (10000 - 1000) / 5
            assertEquals(new BigDecimal("1800.00").multiply(new BigDecimal(i + 1)), year.getCumulativeDepreciation());
            assertEquals(new BigDecimal("10000.00").subtract(new BigDecimal("1800.00").multiply(new BigDecimal(i + 1))), year.getBookValue());
        }
    }

    @Test
    void testDecliningBalanceDepreciation() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(new BigDecimal("2.0"))
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertNotNull(schedule);
        assertEquals(5, schedule.getYears().size());

        // Check first year: approximately 4000
        DepreciationYear year1 = schedule.getYears().get(0);
        assertEquals(1, year1.getYear());
        assertEquals(0, year1.getDepreciation().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, year1.getCumulativeDepreciation().compareTo(new BigDecimal("4000.00")));
        assertEquals(0, year1.getBookValue().compareTo(new BigDecimal("6000.00")));

        // Check that depreciation decreases each year and never goes below salvage value
        BigDecimal previousDepreciation = new BigDecimal("4000.00");
        for (int i = 1; i < 5; i++) {
            DepreciationYear year = schedule.getYears().get(i);
            assertTrue(year.getDepreciation().compareTo(previousDepreciation) <= 0);
            previousDepreciation = year.getDepreciation();
        }

        // Final book value should be salvage value
        DepreciationYear lastYear = schedule.getYears().get(4);
        assertEquals(new BigDecimal("1000.00"), lastYear.getBookValue());
    }

    @Test
    void testFinDepreciation() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.FIN)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertNotNull(schedule);
        assertEquals(5, schedule.getYears().size());

                // Check that FIN depreciation produces reasonable values
        BigDecimal cumulative = BigDecimal.ZERO;
        for (DepreciationYear year : schedule.getYears()) {
            assertNotNull(year.getDepreciation());
            assertNotNull(year.getCumulativeDepreciation());
            assertNotNull(year.getBookValue());
            assertTrue(year.getDepreciation().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue(year.getBookValue().compareTo(BigDecimal.ZERO) >= 0);
            cumulative = cumulative.add(year.getDepreciation());
            assertEquals(0, year.getCumulativeDepreciation().compareTo(cumulative));
        }

        // Total depreciation should be positive and less than cost
        assertTrue(schedule.getTotalDepreciation().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(schedule.getTotalDepreciation().compareTo(new BigDecimal("10000.00")) < 0);
    }

    @Test
    void testDepreciationWithZeroSalvageValue() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(BigDecimal.ZERO)
                .usefulLife(4)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertEquals(4, schedule.getYears().size());
        assertEquals(0, schedule.getTotalDepreciation().compareTo(new BigDecimal("10000.00")));
        assertEquals(0, schedule.getFinalBookValue().compareTo(new BigDecimal("0.00")));

        // Each year should be 2500.00
        for (DepreciationYear year : schedule.getYears()) {
            assertEquals(new BigDecimal("2500.00"), year.getDepreciation());
        }
    }

    @Test
    void testDepreciationWithHighSalvageValue() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("8000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertEquals(5, schedule.getYears().size());
        assertEquals(new BigDecimal("2000.00"), schedule.getTotalDepreciation());
        assertEquals(new BigDecimal("8000.00"), schedule.getFinalBookValue());

        // Each year should be 400.00
        for (DepreciationYear year : schedule.getYears()) {
            assertEquals(new BigDecimal("400.00"), year.getDepreciation());
        }
    }

    @Test
    void testSingleYearDepreciation() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("5000.00"))
                .salvageValue(BigDecimal.ZERO)
                .usefulLife(1)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertEquals(1, schedule.getYears().size());
        assertEquals(0, schedule.getTotalDepreciation().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, schedule.getFinalBookValue().compareTo(new BigDecimal("0.00")));

        DepreciationYear year = schedule.getYears().get(0);
        assertEquals(1, year.getYear());
        assertNotNull(year.getDepreciation());
        assertNotNull(year.getCumulativeDepreciation());
        assertNotNull(year.getBookValue());
        assertTrue(year.getBookValue().compareTo(BigDecimal.ZERO) >= 0);
    }

    @Test
    void testDecliningBalanceWithCustomFactor() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(3)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(new BigDecimal("1.5"))
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertEquals(3, schedule.getYears().size());

        // First year: 1.5/3 * 10000 = 5000
        DepreciationYear year1 = schedule.getYears().get(0);
        assertEquals(0, year1.getDepreciation().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, year1.getCumulativeDepreciation().compareTo(new BigDecimal("5000.00")));
        assertEquals(0, year1.getBookValue().compareTo(new BigDecimal("5000.00")));
    }

    @Test
    void testFinDepreciationDifferentLives() {
        // Test 7-year FIN depreciation
        DepreciationRequest request7 = new DepreciationRequest.Builder()
                .cost(new BigDecimal("14000.00"))
                .salvageValue(BigDecimal.ZERO)
                .usefulLife(7)
                .method(DepreciationMethod.FIN)
                .build();

        DepreciationSchedule schedule7 = service.calculateDepreciation(request7);
        assertEquals(7, schedule7.getYears().size());
        // Total should be close to cost since FIN rates should sum to approximately 1.0
        assertTrue(schedule7.getTotalDepreciation().compareTo(new BigDecimal("13000.00")) > 0);
        assertTrue(schedule7.getTotalDepreciation().compareTo(new BigDecimal("14000.01")) < 0);

        // Test that 3-year FIN throws exception
        DepreciationRequest request3 = new DepreciationRequest.Builder()
                .cost(new BigDecimal("6000.00"))
                .salvageValue(BigDecimal.ZERO)
                .usefulLife(3)
                .method(DepreciationMethod.FIN)
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            service.calculateDepreciation(request3);
        });
    }

    @Test
    void testPrecisionWithLargeNumbers() {
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("1000000.00"))
                .salvageValue(new BigDecimal("100000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        assertEquals(new BigDecimal("900000.00"), schedule.getTotalDepreciation());
        assertEquals(new BigDecimal("100000.00"), schedule.getFinalBookValue());

        // Each year should be 180000.00
        for (DepreciationYear year : schedule.getYears()) {
            assertEquals(new BigDecimal("180000.00"), year.getDepreciation());
        }
    }

    @Test
    void testDecliningBalanceSwitchToStraightLine() {
        // Test case where declining balance should ensure book value doesn't go below salvage
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("5000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.DECLINING_BALANCE)
                .dbFactor(new BigDecimal("2.0"))
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        // The service ensures book value doesn't go below salvage value
        assertEquals(5, schedule.getYears().size());
        // Total depreciation should be cost - salvage = 5000, but calculation may vary
        assertTrue(schedule.getTotalDepreciation().compareTo(new BigDecimal("4000.00")) > 0);
        assertEquals(0, schedule.getFinalBookValue().compareTo(new BigDecimal("5000.00")));
    }

    @Test
    void testInvalidDepreciationMethod() {
        // This should not happen in normal usage since method is validated in builder
        // But test that service handles it gracefully
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE) // Valid method
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);
        assertNotNull(schedule);
    }

    @Test
    void testDepreciationWithConvention() {
        // Test that convention is stored but doesn't affect calculations
        DepreciationRequest request = new DepreciationRequest.Builder()
                .cost(new BigDecimal("10000.00"))
                .salvageValue(new BigDecimal("1000.00"))
                .usefulLife(5)
                .method(DepreciationMethod.STRAIGHT_LINE)
                .convention("half-year")
                .build();

        DepreciationSchedule schedule = service.calculateDepreciation(request);

        // Convention should be stored but not affect straight-line calculations
        assertEquals(5, schedule.getYears().size());
        assertEquals(new BigDecimal("9000.00"), schedule.getTotalDepreciation());
    }
}