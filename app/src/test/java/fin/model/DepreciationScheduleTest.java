package fin.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Comprehensive test suite for DepreciationSchedule model
 * Tests schedule container, totals calculation, and year list management
 */
class DepreciationScheduleTest {

    private DepreciationSchedule schedule;
    private List<DepreciationYear> years;

    @BeforeEach
    void setUp() {
        years = new ArrayList<>();
        years.add(new DepreciationYear(1, new BigDecimal("2000.00"), new BigDecimal("2000.00"), new BigDecimal("8000.00")));
        years.add(new DepreciationYear(2, new BigDecimal("2000.00"), new BigDecimal("4000.00"), new BigDecimal("6000.00")));
        years.add(new DepreciationYear(3, new BigDecimal("2000.00"), new BigDecimal("6000.00"), new BigDecimal("4000.00")));
        years.add(new DepreciationYear(4, new BigDecimal("2000.00"), new BigDecimal("8000.00"), new BigDecimal("2000.00")));
        years.add(new DepreciationYear(5, new BigDecimal("2000.00"), new BigDecimal("10000.00"), new BigDecimal("0.00")));

        schedule = new DepreciationSchedule(years);
    }

    @Test
    void testConstructorWithYears() {
        assertNotNull(schedule);
        assertEquals(5, schedule.getYears().size());
        assertEquals(years, schedule.getYears());
    }

    @Test
    void testDefaultConstructor() {
        DepreciationSchedule emptySchedule = new DepreciationSchedule();
        assertNotNull(emptySchedule);
        assertNotNull(emptySchedule.getYears());
        assertTrue(emptySchedule.getYears().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        List<DepreciationYear> newYears = new ArrayList<>();
        newYears.add(new DepreciationYear(1, new BigDecimal("1000.00"), new BigDecimal("1000.00"), new BigDecimal("9000.00")));

        schedule.setYears(newYears);

        assertEquals(1, schedule.getYears().size());
        assertEquals(newYears, schedule.getYears());
    }

    @Test
    void testTotalDepreciation() {
        BigDecimal totalDepreciation = schedule.getTotalDepreciation();
        assertEquals(new BigDecimal("10000.00"), totalDepreciation);
    }

    @Test
    void testTotalDepreciationWithEmptySchedule() {
        DepreciationSchedule emptySchedule = new DepreciationSchedule();
        BigDecimal totalDepreciation = emptySchedule.getTotalDepreciation();
        assertEquals(BigDecimal.ZERO, totalDepreciation);
    }

    @Test
    void testTotalDepreciationWithNullValues() {
        List<DepreciationYear> yearsWithNulls = new ArrayList<>();
        yearsWithNulls.add(new DepreciationYear(1, null, new BigDecimal("2000.00"), new BigDecimal("8000.00")));
        yearsWithNulls.add(new DepreciationYear(2, new BigDecimal("2000.00"), new BigDecimal("4000.00"), new BigDecimal("6000.00")));

        DepreciationSchedule scheduleWithNulls = new DepreciationSchedule(yearsWithNulls);
        // Should handle null values gracefully
        BigDecimal totalDepreciation = scheduleWithNulls.getTotalDepreciation();
        assertEquals(new BigDecimal("2000.00"), totalDepreciation);
    }

    @Test
    void testYearProgression() {
        List<DepreciationYear> years = schedule.getYears();

        // Verify years are in order
        for (int i = 0; i < years.size(); i++) {
            assertEquals(i + 1, years.get(i).getYear());
        }

        // Verify cumulative depreciation increases
        for (int i = 1; i < years.size(); i++) {
            BigDecimal previousCumulative = years.get(i - 1).getCumulativeDepreciation();
            BigDecimal currentCumulative = years.get(i).getCumulativeDepreciation();
            assertTrue(currentCumulative.compareTo(previousCumulative) >= 0);
        }

        // Verify book value decreases
        for (int i = 1; i < years.size(); i++) {
            BigDecimal previousBookValue = years.get(i - 1).getBookValue();
            BigDecimal currentBookValue = years.get(i).getBookValue();
            assertTrue(previousBookValue.compareTo(currentBookValue) >= 0);
        }
    }

    @Test
    void testScheduleWithDifferentDepreciations() {
        List<DepreciationYear> irregularYears = new ArrayList<>();
        irregularYears.add(new DepreciationYear(1, new BigDecimal("3000.00"), new BigDecimal("3000.00"), new BigDecimal("7000.00")));
        irregularYears.add(new DepreciationYear(2, new BigDecimal("2500.00"), new BigDecimal("5500.00"), new BigDecimal("4500.00")));
        irregularYears.add(new DepreciationYear(3, new BigDecimal("2000.00"), new BigDecimal("7500.00"), new BigDecimal("2500.00")));

        DepreciationSchedule irregularSchedule = new DepreciationSchedule(irregularYears);

        BigDecimal totalDepreciation = irregularSchedule.getTotalDepreciation();
        assertEquals(new BigDecimal("7500.00"), totalDepreciation);
    }

    @Test
    void testScheduleWithZeroDepreciation() {
        List<DepreciationYear> zeroYears = new ArrayList<>();
        zeroYears.add(new DepreciationYear(1, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000.00")));
        zeroYears.add(new DepreciationYear(2, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000.00")));

        DepreciationSchedule zeroSchedule = new DepreciationSchedule(zeroYears);

        BigDecimal totalDepreciation = zeroSchedule.getTotalDepreciation();
        assertEquals(BigDecimal.ZERO, totalDepreciation);
    }

    @Test
    void testLargeNumbers() {
        List<DepreciationYear> largeYears = new ArrayList<>();
        largeYears.add(new DepreciationYear(1, new BigDecimal("1000000.00"), new BigDecimal("1000000.00"), new BigDecimal("9000000.00")));
        largeYears.add(new DepreciationYear(2, new BigDecimal("1000000.00"), new BigDecimal("2000000.00"), new BigDecimal("8000000.00")));

        DepreciationSchedule largeSchedule = new DepreciationSchedule(largeYears);

        BigDecimal totalDepreciation = largeSchedule.getTotalDepreciation();
        assertEquals(new BigDecimal("2000000.00"), totalDepreciation);
    }

    @Test
    void testPrecisionWithDecimals() {
        List<DepreciationYear> preciseYears = new ArrayList<>();
        preciseYears.add(new DepreciationYear(1, new BigDecimal("1666.67"), new BigDecimal("1666.67"), new BigDecimal("8333.33")));
        preciseYears.add(new DepreciationYear(2, new BigDecimal("1666.67"), new BigDecimal("3333.34"), new BigDecimal("6666.66")));

        DepreciationSchedule preciseSchedule = new DepreciationSchedule(preciseYears);

        BigDecimal totalDepreciation = preciseSchedule.getTotalDepreciation();
        assertEquals(new BigDecimal("3333.34"), totalDepreciation);
    }

    @Test
    void testAddYear() {
        DepreciationSchedule schedule = new DepreciationSchedule();
        DepreciationYear year = new DepreciationYear(1, new BigDecimal("2000.00"), new BigDecimal("2000.00"), new BigDecimal("8000.00"));

        schedule.addYear(year);

        assertEquals(1, schedule.getYears().size());
        assertEquals(year, schedule.getYears().get(0));
        assertEquals(new BigDecimal("2000.00"), schedule.getTotalDepreciation());
    }

    @Test
    void testEmptyScheduleOperations() {
        DepreciationSchedule emptySchedule = new DepreciationSchedule();

        assertEquals(BigDecimal.ZERO, emptySchedule.getTotalDepreciation());
        assertNotNull(emptySchedule.getYears());
        assertTrue(emptySchedule.getYears().isEmpty());
    }

    @Test
    void testScheduleImmutability() {
        List<DepreciationYear> originalYears = new ArrayList<>(years);
        DepreciationSchedule schedule = new DepreciationSchedule(originalYears);

        // Modify original list
        originalYears.add(new DepreciationYear(6, new BigDecimal("1000.00"), new BigDecimal("11000.00"), new BigDecimal("-1000.00")));

        // Schedule should not be affected
        assertEquals(5, schedule.getYears().size());
        assertEquals(new BigDecimal("10000.00"), schedule.getTotalDepreciation());
    }

    @Test
    void testEqualsAndHashCode() {
        DepreciationSchedule schedule1 = new DepreciationSchedule(years);
        DepreciationSchedule schedule2 = new DepreciationSchedule(new ArrayList<>(years));
        DepreciationSchedule schedule3 = new DepreciationSchedule();

        // Set same calculation date for comparison
        LocalDateTime testDate = LocalDateTime.of(2024, 1, 1, 12, 0);
        schedule1.setCalculationDate(testDate);
        schedule2.setCalculationDate(testDate);
        schedule3.setCalculationDate(testDate);

        assertEquals(schedule1, schedule2);
        assertEquals(schedule1.hashCode(), schedule2.hashCode());
        assertNotEquals(schedule1, schedule3);
    }
}