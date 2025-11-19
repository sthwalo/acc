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

package fin.service.spring;

import fin.model.*;
import fin.repository.DepreciationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Service for Depreciation operations
 * Provides depreciation calculation and management functionality using JPA repositories
 */
@Service
@Transactional
public class SpringDepreciationService {

    private final DepreciationRepository depreciationRepository;
    private final SpringCompanyService companyService;

    public SpringDepreciationService(DepreciationRepository depreciationRepository,
                                   SpringCompanyService companyService) {
        this.depreciationRepository = depreciationRepository;
        this.companyService = companyService;
    }

    /**
     * Create a depreciation schedule (controller-compatible method)
     */
    @Transactional
    public DepreciationSchedule createDepreciationSchedule(String assetName, BigDecimal assetCost,
                                                         BigDecimal residualValue, Integer usefulLifeYears,
                                                         String depreciationMethod, Long companyId,
                                                         Long accountId, Long accumulatedDepreciationAccountId,
                                                         LocalDate startDate) {
        // Create a basic schedule with the provided parameters
        DepreciationSchedule schedule = new DepreciationSchedule();
        schedule.setScheduleName(assetName);
        schedule.setAssetCost(assetCost);
        schedule.setResidualValue(residualValue);
        schedule.setUsefulLifeYears(usefulLifeYears);
        schedule.setDepreciationMethod(depreciationMethod);
        schedule.setCompanyId(companyId);
        schedule.setAccountId(accountId);
        schedule.setAccumulatedDepreciationAccountId(accumulatedDepreciationAccountId);
        schedule.setStartDate(startDate);
        schedule.setIsActive(true);
        schedule.setStatus("ACTIVE");

        return depreciationRepository.save(schedule);
    }

    /**
     * Calculate straight-line depreciation (controller-compatible method with 3 parameters)
     */
    @Transactional(readOnly = true)
    public static BigDecimal calculateStraightLineDepreciation(BigDecimal assetCost, BigDecimal residualValue,
                                                            Integer usefulLifeYears) {
        if (assetCost == null || residualValue == null || usefulLifeYears == null) {
            throw new IllegalArgumentException("All parameters are required for depreciation calculation");
        }
        if (usefulLifeYears <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        if (assetCost.compareTo(residualValue) <= 0) {
            return BigDecimal.ZERO; // No depreciation needed
        }

        BigDecimal depreciableAmount = assetCost.subtract(residualValue);
        return depreciableAmount.divide(BigDecimal.valueOf(usefulLifeYears), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate straight-line depreciation
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateStraightLineDepreciation(BigDecimal cost, BigDecimal salvageValue,
                                                     Integer usefulLifeYears, Integer currentAgeYears) {
        if (cost == null || salvageValue == null || usefulLifeYears == null || currentAgeYears == null) {
            throw new IllegalArgumentException("All parameters are required for depreciation calculation");
        }
        if (usefulLifeYears <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        if (currentAgeYears < 0) {
            throw new IllegalArgumentException("Current age cannot be negative");
        }
        if (currentAgeYears >= usefulLifeYears) {
            return BigDecimal.ZERO; // Fully depreciated
        }
        if (cost.compareTo(salvageValue) <= 0) {
            return BigDecimal.ZERO; // No depreciation needed
        }

        BigDecimal depreciableAmount = cost.subtract(salvageValue);
        BigDecimal annualDepreciation = depreciableAmount.divide(BigDecimal.valueOf(usefulLifeYears), 2, RoundingMode.HALF_UP);
        BigDecimal remainingLife = BigDecimal.valueOf(usefulLifeYears - currentAgeYears);

        return annualDepreciation.multiply(remainingLife);
    }

    /**
     * Calculate declining balance depreciation
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateDecliningBalanceDepreciation(BigDecimal cost, BigDecimal salvageValue,
                                                          BigDecimal depreciationRate, Integer currentAgeYears) {
        if (cost == null || salvageValue == null || depreciationRate == null || currentAgeYears == null) {
            throw new IllegalArgumentException("All parameters are required for depreciation calculation");
        }
        if (depreciationRate.compareTo(BigDecimal.ZERO) <= 0 || depreciationRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("Depreciation rate must be between 0 and 1");
        }
        if (currentAgeYears < 0) {
            throw new IllegalArgumentException("Current age cannot be negative");
        }

        BigDecimal bookValue = cost;
        BigDecimal totalDepreciation = BigDecimal.ZERO;

        // Calculate accumulated depreciation year by year
        for (int year = 0; year < currentAgeYears; year++) {
            BigDecimal annualDepreciation = bookValue.multiply(depreciationRate);
            // Don't depreciate below salvage value
            if (bookValue.subtract(annualDepreciation).compareTo(salvageValue) < 0) {
                annualDepreciation = bookValue.subtract(salvageValue);
            }
            totalDepreciation = totalDepreciation.add(annualDepreciation);
            bookValue = bookValue.subtract(annualDepreciation);
        }

        return totalDepreciation;
    }

    /**
     * Create a depreciation schedule for an asset
     */
    @Transactional
    public DepreciationSchedule createDepreciationSchedule(Long companyId, Long assetId, String scheduleNumber,
                                                         BigDecimal cost, BigDecimal salvageValue, Integer usefulLifeYears,
                                                         String depreciationMethod) {
        // Validate inputs
        if (companyId == null || assetId == null || scheduleNumber == null || scheduleNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Company ID, asset ID, and schedule number are required");
        }
        if (cost == null || usefulLifeYears == null) {
            throw new IllegalArgumentException("Cost and useful life are required");
        }
        if (depreciationMethod == null || (!depreciationMethod.equals("STRAIGHT_LINE") &&
                                          !depreciationMethod.equals("DECLINING_BALANCE"))) {
            throw new IllegalArgumentException("Valid depreciation method required: STRAIGHT_LINE or DECLINING_BALANCE");
        }

        // Validate company exists
        Company company = companyService.getCompanyById(companyId);
        if (company == null) {
            throw new IllegalArgumentException("Company not found: " + companyId);
        }

        // Check if schedule number already exists for this company
        if (depreciationRepository.findByCompanyIdAndScheduleNumber(companyId, scheduleNumber.trim()).isPresent()) {
            throw new IllegalArgumentException("Schedule number already exists for this company: " + scheduleNumber);
        }

        // Create depreciation schedule
        DepreciationSchedule schedule = new DepreciationSchedule();
        schedule.setCompanyId(companyId);
        schedule.setAssetId(assetId);
        schedule.setScheduleNumber(scheduleNumber.trim());
        schedule.setCost(cost);
        schedule.setSalvageValue(salvageValue != null ? salvageValue : BigDecimal.ZERO);
        schedule.setUsefulLifeYears(usefulLifeYears);
        schedule.setDepreciationMethod(depreciationMethod);
        schedule.setStatus("ACTIVE");

        return depreciationRepository.save(schedule);
    }

    /**
     * Get depreciation schedules for a company
     */
    @Transactional(readOnly = true)
    public List<DepreciationSchedule> getDepreciationSchedulesByCompany(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return depreciationRepository.findByCompanyId(companyId);
    }

    /**
     * Get depreciation schedule by ID
     */
    @Transactional(readOnly = true)
    public Optional<DepreciationSchedule> getDepreciationById(Long id) {
        return depreciationRepository.findById(id);
    }

    /**
     * Update depreciation schedule (this is a simplified version - in practice you'd track depreciation entries)
     */
    @Transactional
    public DepreciationSchedule updateDepreciationSchedule(Long scheduleId, DepreciationSchedule updatedSchedule) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("Schedule ID is required");
        }
        if (updatedSchedule == null) {
            throw new IllegalArgumentException("Updated schedule data is required");
        }

        Optional<DepreciationSchedule> existingScheduleOpt = depreciationRepository.findById(scheduleId);
        if (existingScheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Depreciation schedule not found: " + scheduleId);
        }

        DepreciationSchedule existingSchedule = existingScheduleOpt.get();

        // Update fields
        existingSchedule.setScheduleName(updatedSchedule.getScheduleName());
        existingSchedule.setDescription(updatedSchedule.getDescription());
        existingSchedule.setStatus(updatedSchedule.getStatus());

        return depreciationRepository.save(existingSchedule);
    }

    /**
     * Calculate monthly depreciation for a schedule
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateMonthlyDepreciation(Long scheduleId) {
        Optional<DepreciationSchedule> scheduleOpt = depreciationRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Depreciation schedule not found: " + scheduleId);
        }

        DepreciationSchedule schedule = scheduleOpt.get();

        if ("STRAIGHT_LINE".equals(schedule.getDepreciationMethod())) {
            BigDecimal annualDepreciation = calculateStraightLineDepreciation(
                schedule.getCost(),
                schedule.getSalvageValue(),
                schedule.getUsefulLifeYears(),
                0 // Current age will be calculated based on creation date
            );
            return annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        } else if ("DECLINING_BALANCE".equals(schedule.getDepreciationMethod())) {
            // For declining balance, we need the depreciation rate
            // This would typically be stored or calculated based on company policy
            BigDecimal depreciationRate = schedule.getDbFactor() != null ?
                schedule.getDbFactor() : BigDecimal.valueOf(0.2); // 20% default
            BigDecimal annualDepreciation = calculateDecliningBalanceDepreciation(
                schedule.getCost(),
                schedule.getSalvageValue(),
                depreciationRate,
                0
            );
            return annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }

        throw new IllegalArgumentException("Unsupported depreciation method: " + schedule.getDepreciationMethod());
    }

    /**
     * Get total cost of assets for a company
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalAssetCost(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        List<DepreciationSchedule> schedules = depreciationRepository.findByCompanyId(companyId);
        return schedules.stream()
                .map(DepreciationSchedule::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get total depreciable amount for a company
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalDepreciableAmount(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }

        List<DepreciationSchedule> schedules = depreciationRepository.findByCompanyId(companyId);
        return schedules.stream()
                .map(DepreciationSchedule::getDepreciableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Search depreciation schedules by schedule name
     */
    @Transactional(readOnly = true)
    public List<DepreciationSchedule> searchDepreciationByScheduleName(Long companyId, String scheduleName) {
        if (companyId == null || scheduleName == null) {
            throw new IllegalArgumentException("Company ID and schedule name are required");
        }
        // Use the available method and filter by company
        return depreciationRepository.findByScheduleNameContainingIgnoreCase(scheduleName)
                .stream()
                .filter(schedule -> schedule.getCompanyId().equals(companyId))
                .toList();
    }

    /**
     * Get active depreciation schedules for a company
     */
    @Transactional(readOnly = true)
    public List<DepreciationSchedule> getActiveDepreciationSchedules(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Company ID is required");
        }
        return depreciationRepository.findByCompanyIdAndStatus(companyId, "ACTIVE");
    }

    /**
     * Delete a depreciation schedule
     */
    @Transactional
    public boolean deleteDepreciationSchedule(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Schedule ID is required");
        }

        if (!depreciationRepository.existsById(id)) {
            return false;
        }

        depreciationRepository.deleteById(id);
        return true;
    }

    /**
     * Get depreciation schedule by ID
     */
    @Transactional(readOnly = true)
    public Optional<DepreciationSchedule> getDepreciationScheduleById(Long id) {
        return depreciationRepository.findById(id);
    }

    /**
     * Get active depreciation schedules by company
     */
    @Transactional(readOnly = true)
    public List<DepreciationSchedule> getActiveDepreciationSchedulesByCompany(Long companyId) {
        return depreciationRepository.findByCompanyIdAndStatus(companyId, "ACTIVE");
    }

    /**
     * Calculate depreciation for a schedule
     */
    @Transactional
    public DepreciationCalculation calculateDepreciation(Long scheduleId, java.time.LocalDate calculationDate) {
        Optional<DepreciationSchedule> scheduleOpt = depreciationRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Depreciation schedule not found: " + scheduleId);
        }

        DepreciationSchedule schedule = scheduleOpt.get();
        BigDecimal annualDepreciation = calculateStraightLineDepreciation(
            schedule.getAssetCost(), schedule.getResidualValue(), schedule.getUsefulLifeYears(), 0);

        BigDecimal monthlyDepreciation = annualDepreciation.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        return new DepreciationCalculation(
            scheduleId,
            "STRAIGHT_LINE",
            schedule.getAssetCost(),
            schedule.getResidualValue(),
            schedule.getUsefulLifeYears(),
            annualDepreciation,
            monthlyDepreciation,
            schedule.getAccumulatedDepreciation()
        );
    }

    /**
     * Process depreciation for a company
     */
    @Transactional
    public DepreciationProcessingResult processDepreciation(Long companyId, java.time.LocalDate processingDate, Long fiscalPeriodId) {
        List<DepreciationSchedule> activeSchedules = getActiveDepreciationSchedulesByCompany(companyId);
        BigDecimal totalDepreciation = BigDecimal.ZERO;
        int processedCount = 0;

        for (DepreciationSchedule schedule : activeSchedules) {
            try {
                DepreciationCalculation calc = calculateDepreciation(schedule.getId(), processingDate);
                totalDepreciation = totalDepreciation.add(calc.getMonthlyDepreciation());
                processedCount++;
            } catch (Exception e) {
                // Log error but continue processing other schedules
                System.err.println("Error processing depreciation for schedule " + schedule.getId() + ": " + e.getMessage());
            }
        }

        return new DepreciationProcessingResult(
            null, // No specific schedule ID for batch processing
            "BATCH_" + processingDate,
            true,
            "Processed " + processedCount + " depreciation schedules",
            totalDepreciation,
            processedCount
        );
    }

    /**
     * Update accumulated depreciation
     */
    @Transactional
    public DepreciationSchedule updateAccumulatedDepreciation(Long scheduleId, BigDecimal accumulatedAmount) {
        Optional<DepreciationSchedule> scheduleOpt = depreciationRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Depreciation schedule not found: " + scheduleId);
        }

        DepreciationSchedule schedule = scheduleOpt.get();
        schedule.setAccumulatedDepreciation(accumulatedAmount);
        return depreciationRepository.save(schedule);
    }

    /**
     * Dispose of an asset
     */
    @Transactional
    public DepreciationSchedule disposeAsset(Long scheduleId, java.time.LocalDate disposalDate, BigDecimal disposalProceeds) {
        Optional<DepreciationSchedule> scheduleOpt = depreciationRepository.findById(scheduleId);
        if (scheduleOpt.isEmpty()) {
            throw new IllegalArgumentException("Depreciation schedule not found: " + scheduleId);
        }

        DepreciationSchedule schedule = scheduleOpt.get();
        schedule.setDisposalDate(disposalDate);
        schedule.setDisposalProceeds(disposalProceeds);
        schedule.setIsActive(false);
        return depreciationRepository.save(schedule);
    }

    /**
     * Get depreciation summary for a company and fiscal period
     */
    @Transactional(readOnly = true)
    public DepreciationSummary getDepreciationSummary(Long companyId, Long fiscalPeriodId) {
        List<DepreciationSchedule> schedules = depreciationRepository.findByCompanyId(companyId);

        int totalAssets = schedules.size();
        BigDecimal totalCost = schedules.stream()
            .map(DepreciationSchedule::getAssetCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAccumulated = schedules.stream()
            .map(DepreciationSchedule::getAccumulatedDepreciation)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalCurrent = schedules.stream()
            .map(s -> calculateStraightLineDepreciation(s.getAssetCost(), s.getResidualValue(), s.getUsefulLifeYears(), 0)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netBookValue = totalCost.subtract(totalAccumulated);

        return new DepreciationSummary(companyId, totalAssets, totalCost, totalAccumulated, totalCurrent, netBookValue);
    }

    /**
     * Depreciation calculation result
     */
    public static class DepreciationCalculation {
        private final Long scheduleId;
        private final String depreciationMethod;
        private final BigDecimal cost;
        private final BigDecimal salvageValue;
        private final Integer usefulLifeYears;
        private final BigDecimal annualDepreciation;
        private final BigDecimal monthlyDepreciation;
        private final BigDecimal accumulatedDepreciation;

        public DepreciationCalculation(Long scheduleId, String depreciationMethod, BigDecimal cost,
                                    BigDecimal salvageValue, Integer usefulLifeYears, BigDecimal annualDepreciation,
                                    BigDecimal monthlyDepreciation, BigDecimal accumulatedDepreciation) {
            this.scheduleId = scheduleId;
            this.depreciationMethod = depreciationMethod;
            this.cost = cost;
            this.salvageValue = salvageValue;
            this.usefulLifeYears = usefulLifeYears;
            this.annualDepreciation = annualDepreciation;
            this.monthlyDepreciation = monthlyDepreciation;
            this.accumulatedDepreciation = accumulatedDepreciation;
        }

        public Long getScheduleId() { return scheduleId; }
        public String getDepreciationMethod() { return depreciationMethod; }
        public BigDecimal getCost() { return cost; }
        public BigDecimal getSalvageValue() { return salvageValue; }
        public Integer getUsefulLifeYears() { return usefulLifeYears; }
        public BigDecimal getAnnualDepreciation() { return annualDepreciation; }
        public BigDecimal getMonthlyDepreciation() { return monthlyDepreciation; }
        public BigDecimal getAccumulatedDepreciation() { return accumulatedDepreciation; }
    }

    /**
     * Depreciation processing result
     */
    public static class DepreciationProcessingResult {
        private final Long scheduleId;
        private final String scheduleNumber;
        private final boolean success;
        private final String message;
        private final BigDecimal depreciationAmount;
        private final Integer periodsProcessed;

        public DepreciationProcessingResult(Long scheduleId, String scheduleNumber, boolean success,
                                         String message, BigDecimal depreciationAmount, Integer periodsProcessed) {
            this.scheduleId = scheduleId;
            this.scheduleNumber = scheduleNumber;
            this.success = success;
            this.message = message;
            this.depreciationAmount = depreciationAmount;
            this.periodsProcessed = periodsProcessed;
        }

        public Long getScheduleId() { return scheduleId; }
        public String getScheduleNumber() { return scheduleNumber; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public BigDecimal getDepreciationAmount() { return depreciationAmount; }
        public Integer getPeriodsProcessed() { return periodsProcessed; }
    }

    /**
     * Depreciation summary for reporting
     */
    public static class DepreciationSummary {
        private final Long companyId;
        private final int totalAssets;
        private final BigDecimal totalCost;
        private final BigDecimal totalAccumulatedDepreciation;
        private final BigDecimal totalCurrentDepreciation;
        private final BigDecimal netBookValue;

        public DepreciationSummary(Long companyId, int totalAssets, BigDecimal totalCost,
                                BigDecimal totalAccumulatedDepreciation, BigDecimal totalCurrentDepreciation,
                                BigDecimal netBookValue) {
            this.companyId = companyId;
            this.totalAssets = totalAssets;
            this.totalCost = totalCost;
            this.totalAccumulatedDepreciation = totalAccumulatedDepreciation;
            this.totalCurrentDepreciation = totalCurrentDepreciation;
            this.netBookValue = netBookValue;
        }

        public Long getCompanyId() { return companyId; }
        public int getTotalAssets() { return totalAssets; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getTotalAccumulatedDepreciation() { return totalAccumulatedDepreciation; }
        public BigDecimal getTotalCurrentDepreciation() { return totalCurrentDepreciation; }
        public BigDecimal getNetBookValue() { return netBookValue; }
    }
}