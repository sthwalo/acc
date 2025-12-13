/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.service;

import fin.dto.FiscalPeriodSetupDTO;
import fin.entity.FiscalPeriod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FiscalPeriodCalculationService.
 * Tests the fiscal period date calculation logic based on year-end month selection.
 */
@SpringBootTest
class FiscalPeriodCalculationServiceTest {

    @Autowired
    private FiscalPeriodCalculationService calculationService;

    @Test
    @DisplayName("Should calculate fiscal period for February year-end (standard case)")
    void shouldCalculateFiscalPeriodForFebruaryYearEnd() {
        // Given: February year-end, fiscal year 2024
        FiscalPeriodSetupDTO setupDTO = new FiscalPeriodSetupDTO(2, 2024);
        Long companyId = 1L;

        // When: Calculate fiscal period
        FiscalPeriod result = calculationService.calculateFiscalPeriod(setupDTO, companyId);

        // Then: Verify dates and properties
        assertEquals(companyId, result.getCompanyId());
        assertEquals("Financial Year 2024", result.getPeriodName());
        assertEquals(LocalDate.of(2023, 3, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 2, 29), result.getEndDate()); // 2024 is leap year
        assertFalse(result.isClosed());
    }

    @Test
    @DisplayName("Should calculate fiscal period for December year-end")
    void shouldCalculateFiscalPeriodForDecemberYearEnd() {
        // Given: December year-end, fiscal year 2024
        FiscalPeriodSetupDTO setupDTO = new FiscalPeriodSetupDTO(12, 2024);
        Long companyId = 1L;

        // When: Calculate fiscal period
        FiscalPeriod result = calculationService.calculateFiscalPeriod(setupDTO, companyId);

        // Then: Verify dates and properties
        assertEquals(companyId, result.getCompanyId());
        assertEquals("Financial Year 2024", result.getPeriodName());
        assertEquals(LocalDate.of(2024, 1, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), result.getEndDate());
        assertFalse(result.isClosed());
    }

    @Test
    @DisplayName("Should calculate fiscal period for June year-end")
    void shouldCalculateFiscalPeriodForJuneYearEnd() {
        // Given: June year-end, fiscal year 2024
        FiscalPeriodSetupDTO setupDTO = new FiscalPeriodSetupDTO(6, 2024);
        Long companyId = 1L;

        // When: Calculate fiscal period
        FiscalPeriod result = calculationService.calculateFiscalPeriod(setupDTO, companyId);

        // Then: Verify dates and properties
        assertEquals(companyId, result.getCompanyId());
        assertEquals("Financial Year 2024", result.getPeriodName());
        assertEquals(LocalDate.of(2023, 7, 1), result.getStartDate());
        assertEquals(LocalDate.of(2024, 6, 30), result.getEndDate());
        assertFalse(result.isClosed());
    }

    @Test
    @DisplayName("Should handle non-leap year correctly")
    void shouldHandleNonLeapYearCorrectly() {
        // Given: February year-end, fiscal year 2023 (non-leap year)
        FiscalPeriodSetupDTO setupDTO = new FiscalPeriodSetupDTO(2, 2023);
        Long companyId = 1L;

        // When: Calculate fiscal period
        FiscalPeriod result = calculationService.calculateFiscalPeriod(setupDTO, companyId);

        // Then: Verify February has 28 days in non-leap year
        assertEquals(LocalDate.of(2023, 2, 28), result.getEndDate());
        assertEquals("Financial Year 2023", result.getPeriodName());
    }

    @Test
    @DisplayName("Should validate fiscal period dates correctly")
    void shouldValidateFiscalPeriodDatesCorrectly() {
        // Given: Valid fiscal period
        FiscalPeriod validPeriod = new FiscalPeriod();
        validPeriod.setStartDate(LocalDate.of(2023, 3, 1));
        validPeriod.setEndDate(LocalDate.of(2024, 2, 29));

        // When & Then: Should not throw exception
        assertDoesNotThrow(() -> calculationService.validateFiscalPeriodDates(validPeriod));
    }

    @Test
    @DisplayName("Should reject fiscal period with start date after end date")
    void shouldRejectFiscalPeriodWithStartAfterEnd() {
        // Given: Invalid fiscal period (start after end)
        FiscalPeriod invalidPeriod = new FiscalPeriod();
        invalidPeriod.setStartDate(LocalDate.of(2024, 2, 29));
        invalidPeriod.setEndDate(LocalDate.of(2023, 3, 1));

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculationService.validateFiscalPeriodDates(invalidPeriod)
        );
        assertTrue(exception.getMessage().contains("start date cannot be after end date"));
    }

    @Test
    @DisplayName("Should reject fiscal period shorter than 12 months")
    void shouldRejectFiscalPeriodShorterThan12Months() {
        // Given: Invalid fiscal period (only 6 months)
        FiscalPeriod invalidPeriod = new FiscalPeriod();
        invalidPeriod.setStartDate(LocalDate.of(2023, 3, 1));
        invalidPeriod.setEndDate(LocalDate.of(2023, 8, 31));

        // When & Then: Should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> calculationService.validateFiscalPeriodDates(invalidPeriod)
        );
        assertTrue(exception.getMessage().contains("must be at least 12 months long"));
    }
}