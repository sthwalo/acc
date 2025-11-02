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

import fin.model.Asset;
import fin.model.DepreciationMethod;
import fin.model.DepreciationRequest;
import fin.model.DepreciationSchedule;
import fin.model.DepreciationYear;
import fin.repository.DepreciationRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service for calculating depreciation using various methods
 */
public class DepreciationService {
    private final DepreciationRepository depreciationRepository;

    public DepreciationService(DepreciationRepository repository) {
        this.depreciationRepository = repository;
    }

    private static final Map<Integer, double[]> FIN_RATES = Map.of(
        5, new double[]{0.20, 0.32, 0.192, 0.1152, 0.1152, 0.0576},
        7, new double[]{0.1429, 0.2449, 0.1749, 0.1249, 0.0893, 0.0892, 0.0893, 0.0446}
    );

    private static final BigDecimal DEFAULT_DB_FACTOR = BigDecimal.valueOf(2.0);
    private static final int DEPRECIATION_CALCULATION_SCALE = 4;

    /**
     * Calculate depreciation schedule for the given request and save to database
     */
    public DepreciationSchedule calculateDepreciation(DepreciationRequest request) {
        validateRequest(request);

        List<DepreciationYear> schedule = new ArrayList<>();
        BigDecimal bookValue = request.getCost();
        BigDecimal cumulativeDepreciation = BigDecimal.ZERO;

        for (int year = 1; year <= request.getUsefulLife(); year++) {
            BigDecimal annualDepreciation = calculateAnnualDepreciation(
                request.getMethod(),
                bookValue,
                request.getCost(),
                request.getSalvageValue(),
                request.getUsefulLife(),
                year,
                request.getDbFactor()
            );

            // Ensure we don't depreciate below salvage value for SL method
            if (request.getMethod() == DepreciationMethod.STRAIGHT_LINE) {
                BigDecimal remainingValue = request.getCost().subtract(cumulativeDepreciation);
                BigDecimal minValue = request.getSalvageValue();
                if (remainingValue.subtract(annualDepreciation).compareTo(minValue) < 0) {
                    annualDepreciation = remainingValue.subtract(minValue);
                }
            }

            cumulativeDepreciation = cumulativeDepreciation.add(annualDepreciation);
            bookValue = request.getCost().subtract(cumulativeDepreciation);

            // Ensure book value doesn't go below salvage value
            if (bookValue.compareTo(request.getSalvageValue()) < 0) {
                bookValue = request.getSalvageValue();
            }

            schedule.add(new DepreciationYear(year, annualDepreciation, cumulativeDepreciation, bookValue));
        }

        DepreciationSchedule depreciationSchedule = new DepreciationSchedule(schedule);
        return depreciationSchedule;
    }

    /**
     * Calculate depreciation schedule for the given asset and request, then save to database
     */
    public DepreciationSchedule calculateAndSaveDepreciation(Asset asset, DepreciationRequest request) {
        validateRequest(request);

        // Calculate the depreciation schedule
        List<DepreciationYear> schedule = calculateDepreciationSchedule(request);

        // Create database schedule object
        DepreciationSchedule dbSchedule = new DepreciationSchedule(schedule);
        dbSchedule.setAssetId(asset.getId());
        dbSchedule.setScheduleName(asset.getAssetName() + " - " + request.getMethod().name());
        dbSchedule.setDescription("Depreciation schedule for " + asset.getAssetName());
        dbSchedule.setCost(request.getCost());
        dbSchedule.setSalvageValue(request.getSalvageValue());
        dbSchedule.setUsefulLifeYears(request.getUsefulLife());
        dbSchedule.setDepreciationMethod(request.getMethod());
        dbSchedule.setDbFactor(request.getDbFactor());
        dbSchedule.setConvention(request.getConvention() != null ? request.getConvention() : "HALF_YEAR");
        dbSchedule.setStatus("CALCULATED");
        dbSchedule.setCreatedBy("SYSTEM");

        try {
            // Save to database
            return depreciationRepository.saveDepreciationSchedule(dbSchedule, asset.getCompanyId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save depreciation schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Find depreciation schedule by ID
     */
    public Optional<DepreciationSchedule> getDepreciationSchedule(Long scheduleId) {
        try {
            return depreciationRepository.findScheduleById(scheduleId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find depreciation schedule: " + e.getMessage(), e);
        }
    }

    /**
     * Find depreciation schedule for asset
     */
    public Optional<DepreciationSchedule> getDepreciationScheduleForAsset(Long assetId) {
        // Find the asset first to get company ID, then find schedules
        Optional<Asset> assetOpt = getAssetById(assetId);
        if (assetOpt.isEmpty()) {
            return Optional.empty();
        }
        // For now, return empty - need to implement schedule lookup by asset
        return Optional.empty();
    }

    /**
     * Find asset by ID
     */
    public Optional<Asset> getAssetById(Long assetId) {
        // This method doesn't exist in repository, need to implement
        return Optional.empty();
    }

    /**
     * Get assets for company
     */
    public List<Asset> getAssetsForCompany(Long companyId) {
        try {
            return depreciationRepository.findAssetsByCompany(companyId);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find assets for company: " + e.getMessage(), e);
        }
    }

    /**
     * Save asset
     */
    public Asset saveAsset(Asset asset) {
        try {
            return depreciationRepository.saveAsset(asset, asset.getCompanyId());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save asset: " + e.getMessage(), e);
        }
    }

    /**
     * Delete depreciation schedule
     */
    public void deleteDepreciationSchedule(Long scheduleId) {
        // Repository doesn't have delete method, implement if needed
        throw new UnsupportedOperationException("Delete depreciation schedule not implemented");
    }

    /**
     * Delete asset
     */
    public void deleteAsset(Long assetId) {
        // Repository doesn't have delete method, implement if needed
        throw new UnsupportedOperationException("Delete asset not implemented");
    }

    /**
     * Calculate straight-line depreciation
     */
    public BigDecimal calculateStraightLineDepreciation(BigDecimal cost, BigDecimal salvageValue, int usefulLife) {
        if (usefulLife <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        return cost.subtract(salvageValue).divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate declining balance depreciation for a specific year
     */
    public BigDecimal calculateDecliningBalanceDepreciation(BigDecimal bookValue, BigDecimal rate, int usefulLife) {
        return bookValue.multiply(rate).divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate FIN depreciation for a specific year
     */
    public BigDecimal calculateFINDepreciation(BigDecimal basis, int recoveryPeriod, int year) {
        double[] rates = FIN_RATES.get(recoveryPeriod);
        if (rates == null) {
            throw new IllegalArgumentException("Unsupported recovery period: " + recoveryPeriod +
                                             ". Supported periods: " + FIN_RATES.keySet());
        }
        if (year < 1 || year > rates.length) {
            return BigDecimal.ZERO;
        }
        return basis.multiply(BigDecimal.valueOf(rates[year - 1]));
    }

    /**
     * Get supported FIN recovery periods
     */
    public Set<Integer> getSupportedFINPeriods() {
        return FIN_RATES.keySet();
    }

    /**
     * Get FIN rates for a specific recovery period
     */
    public double[] getFINRates(int recoveryPeriod) {
        double[] rates = FIN_RATES.get(recoveryPeriod);
        if (rates == null) {
            throw new IllegalArgumentException("Unsupported recovery period: " + recoveryPeriod);
        }
        return Arrays.copyOf(rates, rates.length);
    }

    /**
     * Calculate depreciation schedule for the given request
     */
    private List<DepreciationYear> calculateDepreciationSchedule(DepreciationRequest request) {
        List<DepreciationYear> schedule = new ArrayList<>();
        BigDecimal bookValue = request.getCost();
        BigDecimal cumulativeDepreciation = BigDecimal.ZERO;

        for (int year = 1; year <= request.getUsefulLife(); year++) {
            BigDecimal annualDepreciation = calculateAnnualDepreciation(
                request.getMethod(),
                bookValue,
                request.getCost(),
                request.getSalvageValue(),
                request.getUsefulLife(),
                year,
                request.getDbFactor()
            );

            // Ensure we don't depreciate below salvage value for SL method
            if (request.getMethod() == DepreciationMethod.STRAIGHT_LINE) {
                BigDecimal remainingValue = request.getCost().subtract(cumulativeDepreciation);
                BigDecimal minValue = request.getSalvageValue();
                if (remainingValue.subtract(annualDepreciation).compareTo(minValue) < 0) {
                    annualDepreciation = remainingValue.subtract(minValue);
                }
            }

            cumulativeDepreciation = cumulativeDepreciation.add(annualDepreciation);
            bookValue = request.getCost().subtract(cumulativeDepreciation);

            DepreciationYear yearEntry = new DepreciationYear(
                year,
                annualDepreciation,
                cumulativeDepreciation,
                bookValue
            );
            schedule.add(yearEntry);
        }

        return schedule;
    }

    private BigDecimal calculateAnnualDepreciation(
            DepreciationMethod method,
            BigDecimal bookValue,
            BigDecimal cost,
            BigDecimal salvageValue,
            int usefulLife,
            int currentYear,
            BigDecimal dbFactor) {

        switch (method) {
            case STRAIGHT_LINE:
                return calculateStraightLineDepreciation(cost, salvageValue, usefulLife);

            case DECLINING_BALANCE:
                if (dbFactor == null) {
                    dbFactor = DEFAULT_DB_FACTOR; // Default to double declining balance
                }
                BigDecimal rate = dbFactor.divide(BigDecimal.valueOf(usefulLife), DEPRECIATION_CALCULATION_SCALE, RoundingMode.HALF_UP);
                return bookValue.multiply(rate);

            case FIN:
                return calculateFINDepreciation(cost, usefulLife, currentYear);

            default:
                throw new IllegalArgumentException("Unsupported depreciation method: " + method);
        }
    }

    private void validateRequest(DepreciationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Depreciation request cannot be null");
        }
        if (request.getCost() == null || request.getCost().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Cost must be positive");
        }
        if (request.getSalvageValue() == null || request.getSalvageValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Salvage value cannot be negative");
        }
        if (request.getSalvageValue().compareTo(request.getCost()) >= 0) {
            throw new IllegalArgumentException("Salvage value must be less than cost");
        }
        if (request.getUsefulLife() <= 0) {
            throw new IllegalArgumentException("Useful life must be positive");
        }
        if (request.getMethod() == null) {
            throw new IllegalArgumentException("Depreciation method cannot be null");
        }
        if (request.getMethod() == DepreciationMethod.FIN && !FIN_RATES.containsKey(request.getUsefulLife())) {
            throw new IllegalArgumentException("FIN method only supports recovery periods: "
                    + FIN_RATES.keySet());
        }
    }
}
