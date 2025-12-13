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
import fin.repository.FiscalPeriodRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for setting up fiscal periods during company onboarding.
 *
 * <p>Handles the complete fiscal period setup workflow including:
 * - Date calculation based on year-end month selection
 * - Validation to prevent overlapping periods
 * - Persistence of fiscal period data
 */
@Service
public class FiscalPeriodSetupService {

    private static final Logger logger = LoggerFactory.getLogger(FiscalPeriodSetupService.class);

    private final FiscalPeriodCalculationService calculationService;
    private final FiscalPeriodRepository fiscalPeriodRepository;

    public FiscalPeriodSetupService(
            FiscalPeriodCalculationService calculationService,
            FiscalPeriodRepository fiscalPeriodRepository) {
        this.calculationService = calculationService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Sets up a fiscal period for a company based on year-end month selection.
     *
     * @param setupDTO The setup parameters
     * @param companyId The company ID
     * @return The created fiscal period
     * @throws IllegalArgumentException if the setup would create overlapping periods
     */
    @Transactional
    public FiscalPeriod setupFiscalPeriod(FiscalPeriodSetupDTO setupDTO, Long companyId) {
        logger.info("Setting up fiscal period for company {} with parameters: {}", companyId, setupDTO);

        // Calculate the fiscal period dates
        FiscalPeriod fiscalPeriod = calculationService.calculateFiscalPeriod(setupDTO, companyId);

        // Validate the calculated dates
        calculationService.validateFiscalPeriodDates(fiscalPeriod);

        // Check for overlapping periods
        validateNoOverlappingPeriods(fiscalPeriod);

        // Check for duplicate period names
        validateUniquePeriodName(fiscalPeriod);

        // Save the fiscal period
        FiscalPeriod savedPeriod = fiscalPeriodRepository.save(fiscalPeriod);
        logger.info("Successfully created fiscal period: {} for company {}", savedPeriod.getPeriodName(), companyId);

        return savedPeriod;
    }

    /**
     * Validates that the new fiscal period doesn't overlap with existing periods for the company.
     *
     * @param newPeriod The new fiscal period to validate
     * @throws IllegalArgumentException if overlapping periods exist
     */
    private void validateNoOverlappingPeriods(FiscalPeriod newPeriod) {
        List<FiscalPeriod> existingPeriods = fiscalPeriodRepository.findByCompanyId(newPeriod.getCompanyId());

        for (FiscalPeriod existing : existingPeriods) {
            if (periodsOverlap(newPeriod, existing)) {
                throw new IllegalArgumentException(
                    "Cannot create fiscal period '" + newPeriod.getPeriodName() +
                    "' as it overlaps with existing period '" + existing.getPeriodName() +
                    "' (" + existing.getStartDate() + " to " + existing.getEndDate() + ")"
                );
            }
        }
    }

    /**
     * Validates that the period name is unique for the company.
     *
     * @param fiscalPeriod The fiscal period to validate
     * @throws IllegalArgumentException if a period with the same name already exists
     */
    private void validateUniquePeriodName(FiscalPeriod fiscalPeriod) {
        boolean exists = fiscalPeriodRepository.existsByCompanyIdAndPeriodName(
            fiscalPeriod.getCompanyId(),
            fiscalPeriod.getPeriodName()
        );

        if (exists) {
            throw new IllegalArgumentException(
                "A fiscal period with name '" + fiscalPeriod.getPeriodName() +
                "' already exists for this company"
            );
        }
    }

    /**
     * Checks if two fiscal periods overlap.
     *
     * @param period1 The first period
     * @param period2 The second period
     * @return true if the periods overlap, false otherwise
     */
    private boolean periodsOverlap(FiscalPeriod period1, FiscalPeriod period2) {
        return period1.getStartDate().isBefore(period2.getEndDate().plusDays(1)) &&
               period2.getStartDate().isBefore(period1.getEndDate().plusDays(1));
    }

    /**
     * Gets all fiscal periods for a company.
     *
     * @param companyId The company ID
     * @return List of fiscal periods ordered by start date descending
     */
    public List<FiscalPeriod> getFiscalPeriodsForCompany(Long companyId) {
        return fiscalPeriodRepository.findByCompanyIdOrderByStartDateDesc(companyId);
    }

    /**
     * Checks if a company has any fiscal periods set up.
     *
     * @param companyId The company ID
     * @return true if the company has fiscal periods, false otherwise
     */
    public boolean hasFiscalPeriods(Long companyId) {
        return !fiscalPeriodRepository.findByCompanyId(companyId).isEmpty();
    }
}