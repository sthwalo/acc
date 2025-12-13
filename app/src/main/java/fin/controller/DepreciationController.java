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

package fin.controller;

import fin.entity.DepreciationSchedule;
import fin.service.DepreciationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring REST Controller for depreciation management operations.
 */
@RestController
@RequestMapping("/api/v1/depreciation")
public class DepreciationController {

    private final DepreciationService depreciationService;

    public DepreciationController(DepreciationService depreciationService) {
        this.depreciationService = depreciationService;
    }

    /**
     * Create a depreciation schedule
     */
    @PostMapping("/schedules")
    public ResponseEntity<DepreciationSchedule> createDepreciationSchedule(@RequestBody DepreciationSchedule schedule) {
        try {
            DepreciationSchedule createdSchedule = depreciationService.createDepreciationSchedule(
                schedule.getAssetName(),
                schedule.getAssetCost(),
                schedule.getResidualValue(),
                schedule.getUsefulLifeYears(),
                schedule.getDepreciationMethod(),
                schedule.getCompanyId(),
                schedule.getAccountId(),
                schedule.getAccumulatedDepreciationAccountId(),
                schedule.getStartDate()
            );
            return ResponseEntity.ok(createdSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get depreciation schedule by ID
     */
    @GetMapping("/schedules/{id}")
    public ResponseEntity<DepreciationSchedule> getDepreciationScheduleById(@PathVariable Long id) {
        Optional<DepreciationSchedule> schedule = depreciationService.getDepreciationScheduleById(id);
        return schedule.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all depreciation schedules for a company
     */
    @GetMapping("/schedules/company/{companyId}")
    public ResponseEntity<List<DepreciationSchedule>> getDepreciationSchedulesByCompany(@PathVariable Long companyId) {
        try {
            List<DepreciationSchedule> schedules = depreciationService.getDepreciationSchedulesByCompany(companyId);
            return ResponseEntity.ok(schedules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active depreciation schedules for a company
     */
    @GetMapping("/schedules/company/{companyId}/active")
    public ResponseEntity<List<DepreciationSchedule>> getActiveDepreciationSchedulesByCompany(@PathVariable Long companyId) {
        try {
            List<DepreciationSchedule> schedules = depreciationService.getActiveDepreciationSchedulesByCompany(companyId);
            return ResponseEntity.ok(schedules);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update a depreciation schedule
     */
    @PutMapping("/schedules/{id}")
    public ResponseEntity<DepreciationSchedule> updateDepreciationSchedule(@PathVariable Long id,
                                                                         @RequestBody DepreciationSchedule schedule) {
        try {
            DepreciationSchedule updatedSchedule = depreciationService.updateDepreciationSchedule(id, schedule);
            return ResponseEntity.ok(updatedSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate depreciation for a specific period
     */
    @PostMapping("/schedules/{id}/calculate")
    public ResponseEntity<DepreciationService.DepreciationCalculation> calculateDepreciation(
            @PathVariable Long id,
            @RequestParam LocalDate calculationDate) {
        try {
            DepreciationService.DepreciationCalculation calculation =
                depreciationService.calculateDepreciation(id, calculationDate);
            return ResponseEntity.ok(calculation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Process depreciation entries for all active schedules
     */
    @PostMapping("/process/company/{companyId}")
    public ResponseEntity<DepreciationService.DepreciationProcessingResult> processDepreciation(
            @PathVariable Long companyId,
            @RequestParam LocalDate processingDate,
            @RequestParam Long fiscalPeriodId) {
        try {
            DepreciationService.DepreciationProcessingResult result =
                depreciationService.processDepreciation(companyId, processingDate, fiscalPeriodId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update accumulated depreciation
     */
    @PostMapping("/schedules/{id}/accumulated")
    public ResponseEntity<DepreciationSchedule> updateAccumulatedDepreciation(@PathVariable Long id,
                                                                            @RequestParam BigDecimal accumulatedAmount) {
        try {
            DepreciationSchedule updatedSchedule = depreciationService.updateAccumulatedDepreciation(id, accumulatedAmount);
            return ResponseEntity.ok(updatedSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Dispose of an asset (end depreciation)
     */
    @PostMapping("/schedules/{id}/dispose")
    public ResponseEntity<DepreciationSchedule> disposeAsset(@PathVariable Long id,
                                                           @RequestParam LocalDate disposalDate,
                                                           @RequestParam BigDecimal disposalProceeds) {
        try {
            DepreciationSchedule disposedSchedule = depreciationService.disposeAsset(id, disposalDate, disposalProceeds);
            return ResponseEntity.ok(disposedSchedule);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get depreciation summary for a company and fiscal period
     */
    @GetMapping("/summary/company/{companyId}/fiscal-period/{fiscalPeriodId}")
    public ResponseEntity<DepreciationService.DepreciationSummary> getDepreciationSummary(
            @PathVariable Long companyId,
            @PathVariable Long fiscalPeriodId) {
        try {
            DepreciationService.DepreciationSummary summary =
                depreciationService.getDepreciationSummary(companyId, fiscalPeriodId);
            return ResponseEntity.ok(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate straight-line depreciation
     */
    @PostMapping("/calculate/straight-line")
    public ResponseEntity<BigDecimal> calculateStraightLineDepreciation(
            @RequestParam BigDecimal assetCost,
            @RequestParam BigDecimal residualValue,
            @RequestParam Integer usefulLifeYears) {
        try {
            BigDecimal depreciation = DepreciationService.calculateStraightLineDepreciation(
                assetCost, residualValue, usefulLifeYears);
            return ResponseEntity.ok(depreciation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Calculate declining balance depreciation
     */
    @PostMapping("/calculate/declining-balance")
    public ResponseEntity<BigDecimal> calculateDecliningBalanceDepreciation(
            @RequestParam BigDecimal assetCost,
            @RequestParam BigDecimal residualValue,
            @RequestParam Integer usefulLifeYears,
            @RequestParam BigDecimal depreciationRate,
            @RequestParam Integer currentAgeYears) {
        try {
            BigDecimal depreciation = depreciationService.calculateDecliningBalanceDepreciation(
                assetCost, residualValue, depreciationRate, currentAgeYears);
            return ResponseEntity.ok(depreciation);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}