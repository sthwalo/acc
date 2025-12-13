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

import fin.entity.Plan;
import fin.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing pricing plans
 */
@Service
@Transactional
public class PlanService {

    private final PlanRepository planRepository;

    @Autowired
    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Get all active plans
     */
    @Transactional(readOnly = true)
    public List<Plan> getAllActivePlans() {
        return planRepository.findByIsActiveTrueOrderByPriceAsc();
    }

    /**
     * Get plan by ID
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getPlanById(Long id) {
        return planRepository.findById(id);
    }

    /**
     * Get plan by name
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getPlanByName(String name) {
        return planRepository.findByNameAndIsActiveTrue(name);
    }

    /**
     * Get trial plan
     */
    @Transactional(readOnly = true)
    public Optional<Plan> getTrialPlan() {
        return planRepository.findTrialPlan();
    }

    /**
     * Create a new plan
     */
    public Plan createPlan(Plan plan) {
        if (plan.getCreatedAt() == null) {
            plan.setCreatedAt(LocalDateTime.now());
        }
        if (plan.getUpdatedAt() == null) {
            plan.setUpdatedAt(LocalDateTime.now());
        }
        if (plan.getCreatedBy() == null) {
            plan.setCreatedBy("FIN");
        }
        if (plan.getUpdatedBy() == null) {
            plan.setUpdatedBy("FIN");
        }
        return planRepository.save(plan);
    }

    /**
     * Update an existing plan
     */
    public Plan updatePlan(Plan plan) {
        plan.setUpdatedAt(LocalDateTime.now());
        plan.setUpdatedBy("FIN");
        return planRepository.save(plan);
    }

    /**
     * Deactivate a plan
     */
    public void deactivatePlan(Long planId) {
        Optional<Plan> planOpt = planRepository.findById(planId);
        if (planOpt.isPresent()) {
            Plan plan = planOpt.get();
            plan.setActive(false);
            plan.setUpdatedAt(LocalDateTime.now());
            plan.setUpdatedBy("FIN");
            planRepository.save(plan);
        }
    }

    /**
     * Check if plan allows a specific feature
     */
    @Transactional(readOnly = true)
    public boolean canPlanAccessFeature(Long planId, String feature) {
        Optional<Plan> planOpt = planRepository.findById(planId);
        if (planOpt.isEmpty()) {
            return false;
        }
        return planOpt.get().canUserAccessFeature(feature);
    }

    /**
     * Get maximum companies allowed for plan
     */
    @Transactional(readOnly = true)
    public int getMaxCompaniesForPlan(Long planId) {
        Optional<Plan> planOpt = planRepository.findById(planId);
        return planOpt.map(Plan::getMaxCompanies).orElse(1);
    }

    /**
     * Get maximum users allowed for plan
     */
    @Transactional(readOnly = true)
    public int getMaxUsersForPlan(Long planId) {
        Optional<Plan> planOpt = planRepository.findById(planId);
        return planOpt.map(Plan::getMaxUsers).orElse(1);
    }

    /**
     * Get maximum transactions per month for plan
     */
    @Transactional(readOnly = true)
    public int getMaxTransactionsPerMonthForPlan(Long planId) {
        Optional<Plan> planOpt = planRepository.findById(planId);
        return planOpt.map(Plan::getMaxTransactionsPerMonth).orElse(100);
    }

    /**
     * Get plans by billing cycle
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansByBillingCycle(String billingCycle) {
        return planRepository.findByBillingCycleAndIsActiveTrue(billingCycle);
    }

    /**
     * Get plans with API access
     */
    @Transactional(readOnly = true)
    public List<Plan> getPlansWithApiAccess() {
        return planRepository.findByHasApiAccessTrueAndIsActiveTrue();
    }

    /**
     * Count total active plans
     */
    @Transactional(readOnly = true)
    public long countActivePlans() {
        return planRepository.countByIsActiveTrue();
    }

    /**
     * Initialize default plans if none exist
     */
    public void initializeDefaultPlans() {
        if (countActivePlans() == 0) {
            // Create default plans
            createPlan(Plan.createFreePlan());
            createPlan(Plan.createStarterPlan());
            createPlan(Plan.createProfessionalPlan());
            createPlan(Plan.createEnterprisePlan());
        }
    }
}