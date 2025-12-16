/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.controller;

import fin.dto.ApiResponse;
import fin.dto.PlanDTO;
import fin.entity.Plan;
import fin.service.PlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing pricing plans
 */
@RestController
@RequestMapping({"/api", "/api/v1"})
public class PlanController {

    private final PlanService planService;

    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Get all active plans
     */
    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<PlanDTO>>> getAllActivePlans() {
        try {
            List<Plan> plans = planService.getAllActivePlans();
            List<PlanDTO> planDTOs = plans.stream()
                .map(PlanDTO::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(new ApiResponse<>(true, planDTOs, "Plans retrieved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, null, "Failed to retrieve plans: " + e.getMessage()));
        }
    }
}