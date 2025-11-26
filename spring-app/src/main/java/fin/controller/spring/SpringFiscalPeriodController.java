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

package fin.controller.spring;

import fin.controller.spring.ApiResponse;
import fin.controller.spring.ErrorCode;
import fin.dto.FiscalPeriodPayrollConfigRequest;
import fin.dto.FiscalPeriodPayrollConfigResponse;
import fin.dto.FiscalPeriodPayrollStatusResponse;
import fin.model.FiscalPeriod;
import fin.repository.FiscalPeriodRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Spring REST Controller for fiscal period operations.
 * Handles fiscal period-specific operations that don't require company context.
 */
@RestController
@RequestMapping("/api/v1/fiscal-periods")
public class SpringFiscalPeriodController {

    private final FiscalPeriodRepository fiscalPeriodRepository;

    public SpringFiscalPeriodController(FiscalPeriodRepository fiscalPeriodRepository) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
    }

    /**
     * Get payroll configuration for a fiscal period
     */
    @GetMapping("/{id}/payroll-config")
    public ResponseEntity<ApiResponse<FiscalPeriodPayrollConfigResponse>> getFiscalPeriodPayrollConfig(@PathVariable Long id) {
        try {
            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(id).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.notFound().build();
            }

            FiscalPeriodPayrollConfigResponse response = new FiscalPeriodPayrollConfigResponse(fiscalPeriod);
            return ResponseEntity.ok(ApiResponse.success(
                "Payroll configuration retrieved successfully",
                response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve payroll configuration: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Configure payroll settings for a fiscal period
     */
    @PutMapping("/{id}/payroll-config")
    public ResponseEntity<ApiResponse<FiscalPeriodPayrollConfigResponse>> configureFiscalPeriodPayroll(
            @PathVariable Long id,
            @RequestBody FiscalPeriodPayrollConfigRequest request) {
        try {
            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(id).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.notFound().build();
            }

            // Update payroll configuration fields
            if (request.getPayDate() != null) {
                fiscalPeriod.setPayDate(request.getPayDate());
            }
            if (request.getPeriodType() != null) {
                fiscalPeriod.setPeriodType(request.getPeriodType());
            }
            if (request.getPayrollStatus() != null) {
                fiscalPeriod.setPayrollStatus(request.getPayrollStatus());
            }

            // Update timestamp
            fiscalPeriod.setUpdatedAt(java.time.LocalDateTime.now());

            FiscalPeriod savedPeriod = fiscalPeriodRepository.save(fiscalPeriod);
            FiscalPeriodPayrollConfigResponse response = new FiscalPeriodPayrollConfigResponse(savedPeriod);

            return ResponseEntity.ok(ApiResponse.success(
                "Payroll configuration updated successfully",
                response
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to configure payroll: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Get fiscal periods with payroll status
     */
    @GetMapping("/payroll-status")
    public ResponseEntity<ApiResponse<List<FiscalPeriodPayrollStatusResponse>>> getFiscalPeriodsPayrollStatus(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String status) {
        try {
            List<FiscalPeriod> fiscalPeriods;

            if (companyId != null) {
                fiscalPeriods = fiscalPeriodRepository.findByCompanyId(companyId);
            } else {
                fiscalPeriods = fiscalPeriodRepository.findAll();
            }

            // Filter by payroll status if specified
            if (status != null && !status.isEmpty()) {
                try {
                    FiscalPeriod.PayrollStatus payrollStatus = FiscalPeriod.PayrollStatus.valueOf(status.toUpperCase());
                    fiscalPeriods = fiscalPeriods.stream()
                            .filter(fp -> fp.getPayrollStatus() == payrollStatus)
                            .toList();
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error("Invalid payroll status: " + status, ErrorCode.VALIDATION_ERROR.getCode())
                    );
                }
            }

            List<FiscalPeriodPayrollStatusResponse> responses = fiscalPeriods.stream()
                    .map(FiscalPeriodPayrollStatusResponse::new)
                    .toList();

            return ResponseEntity.ok(ApiResponse.success(
                "Fiscal periods with payroll status retrieved successfully",
                responses,
                responses.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to retrieve fiscal periods payroll status: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }

    /**
     * Reset payroll configuration for a fiscal period
     */
    @DeleteMapping("/{id}/payroll-config")
    public ResponseEntity<ApiResponse<String>> resetFiscalPeriodPayrollConfig(@PathVariable Long id) {
        try {
            FiscalPeriod fiscalPeriod = fiscalPeriodRepository.findById(id).orElse(null);
            if (fiscalPeriod == null) {
                return ResponseEntity.notFound().build();
            }

            // Check if payroll has been processed - don't allow reset if processed
            if (fiscalPeriod.isPayrollProcessed()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error("Cannot reset payroll configuration for processed fiscal period", ErrorCode.VALIDATION_ERROR.getCode())
                );
            }

            // Reset payroll fields to defaults
            fiscalPeriod.setPayDate(null);
            fiscalPeriod.setPeriodType(FiscalPeriod.PeriodType.MONTHLY);
            fiscalPeriod.setPayrollStatus(FiscalPeriod.PayrollStatus.OPEN);
            fiscalPeriod.setTotalGrossPay(java.math.BigDecimal.ZERO);
            fiscalPeriod.setTotalDeductions(java.math.BigDecimal.ZERO);
            fiscalPeriod.setTotalNetPay(java.math.BigDecimal.ZERO);
            fiscalPeriod.setEmployeeCount(0);
            fiscalPeriod.setProcessedAt(null);
            fiscalPeriod.setProcessedBy(null);
            fiscalPeriod.setApprovedAt(null);
            fiscalPeriod.setApprovedBy(null);
            fiscalPeriod.setUpdatedAt(java.time.LocalDateTime.now());

            fiscalPeriodRepository.save(fiscalPeriod);

            return ResponseEntity.ok(ApiResponse.success(
                "Payroll configuration reset successfully",
                "Payroll configuration has been reset for fiscal period " + id
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error("Failed to reset payroll configuration: " + e.getMessage(), ErrorCode.INTERNAL_ERROR.getCode())
            );
        }
    }
}