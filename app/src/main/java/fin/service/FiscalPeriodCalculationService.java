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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;

/**
 * Service for calculating fiscal period dates based on year-end month selection.
 *
 * <p>Fiscal periods are calculated as follows:
 * - Start Date: First day of the month following the previous year's year-end month
 * - End Date: Last day of the year-end month in the fiscal year
 *
 * <p>Example: Year-end month = February (2), Fiscal year = 2024
 * - Start Date: March 1, 2023
 * - End Date: February 29, 2024 (leap year)
 */
@Service
public class FiscalPeriodCalculationService {

    private static final Logger logger = LoggerFactory.getLogger(FiscalPeriodCalculationService.class);

    /**
     * Calculates fiscal period dates based on year-end month and fiscal year.
     *
     * @param setupDTO The setup parameters containing year-end month and fiscal year
     * @param companyId The company ID for which to create the fiscal period
     * @return A FiscalPeriod entity with calculated dates
     */
    public FiscalPeriod calculateFiscalPeriod(FiscalPeriodSetupDTO setupDTO, Long companyId) {
        logger.info("Calculating fiscal period for company {} with year-end month {} and fiscal year {}",
                   companyId, setupDTO.getYearEndMonth(), setupDTO.getFiscalYear());

        LocalDate startDate = calculateStartDate(setupDTO.getYearEndMonth(), setupDTO.getFiscalYear());
        LocalDate endDate = calculateEndDate(setupDTO.getYearEndMonth(), setupDTO.getFiscalYear());
        String periodName = generatePeriodName(startDate, endDate);

        FiscalPeriod fiscalPeriod = new FiscalPeriod();
        fiscalPeriod.setCompanyId(companyId);
        fiscalPeriod.setPeriodName(periodName);
        fiscalPeriod.setStartDate(startDate);
        fiscalPeriod.setEndDate(endDate);
        fiscalPeriod.setClosed(false); // New fiscal periods start as open

        logger.info("Calculated fiscal period: {} ({} to {})", periodName, startDate, endDate);
        return fiscalPeriod;
    }

    /**
     * Calculates the start date of a fiscal period.
     *
     * <p>The start date is the first day of the month following the year-end month
     * in the previous year.
     *
     * @param yearEndMonth The year-end month (1-12)
     * @param fiscalYear The fiscal year
     * @return The start date of the fiscal period
     */
    private LocalDate calculateStartDate(int yearEndMonth, int fiscalYear) {
        // Start date is first day of the month after year-end month in previous year
        int startMonth = yearEndMonth % 12 + 1; // Next month after year-end
        int startYear = (yearEndMonth == 12) ? fiscalYear : fiscalYear - 1;

        return LocalDate.of(startYear, startMonth, 1);
    }

    /**
     * Calculates the end date of a fiscal period.
     *
     * <p>The end date is the last day of the year-end month in the fiscal year.
     *
     * @param yearEndMonth The year-end month (1-12)
     * @param fiscalYear The fiscal year
     * @return The end date of the fiscal period
     */
    private LocalDate calculateEndDate(int yearEndMonth, int fiscalYear) {
        // End date is last day of year-end month in fiscal year
        YearMonth yearMonth = YearMonth.of(fiscalYear, yearEndMonth);
        return LocalDate.of(fiscalYear, yearEndMonth, yearMonth.lengthOfMonth());
    }

    /**
     * Generates a human-readable period name from start and end dates.
     *
     * @param startDate The start date of the period
     * @param endDate The end date of the period
     * @return A formatted period name (e.g., "Financial Year 2024")
     */
    private String generatePeriodName(LocalDate startDate, LocalDate endDate) {
        int yearEndYear = endDate.getYear();
        return "Financial Year " + yearEndYear;
    }

    /**
     * Validates that the calculated fiscal period dates are logical.
     *
     * @param fiscalPeriod The fiscal period to validate
     * @throws IllegalArgumentException if the dates are invalid
     */
    public void validateFiscalPeriodDates(FiscalPeriod fiscalPeriod) {
        if (fiscalPeriod.getStartDate().isAfter(fiscalPeriod.getEndDate())) {
            throw new IllegalArgumentException(
                "Fiscal period start date cannot be after end date: " +
                fiscalPeriod.getStartDate() + " > " + fiscalPeriod.getEndDate()
            );
        }

        if (fiscalPeriod.getStartDate().plusMonths(11).isAfter(fiscalPeriod.getEndDate())) {
            throw new IllegalArgumentException(
                "Fiscal period must be at least 12 months long. Start: " +
                fiscalPeriod.getStartDate() + ", End: " + fiscalPeriod.getEndDate()
            );
        }
    }
}