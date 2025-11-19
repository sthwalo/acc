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

package fin.api.controllers;

import fin.api.dto.responses.ApiResponse;
import fin.model.Plan;
import fin.repository.PlanRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Plan Controller
 * Handles plan management operations for user registration
 */
public class PlanController {
    private final PlanRepository planRepository;

    /**
     * Constructor with dependency injection
     * @param planRepository the plan repository
     */
    public PlanController(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    /**
     * Get all active plans formatted for frontend
     * @return API response with plans data
     */
    public ApiResponse<List<PlanResponse>> getAllActivePlans() {
        try {
            List<Plan> plans = planRepository.findAllActive();

            if (plans.isEmpty()) {
                throw new RuntimeException(
                    "No plans found in database. Please add plans through the backend administration. " +
                    "SQL: INSERT INTO plans (name, description, price, currency, billing_cycle, is_active, " +
                    "can_access_dashboard, can_manage_companies, can_process_bank_statements, can_generate_reports, " +
                    "can_manage_payroll, can_manage_budgets, can_access_multiple_companies, max_companies, max_users, " +
                    "max_transactions_per_month, has_api_access, has_priority_support, " +
                    "created_at, updated_at, created_by, updated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
            }

            List<PlanResponse> planResponses = plans.stream()
                .map(this::convertToPlanResponse)
                .collect(Collectors.toList());

            return new ApiResponse<>(true, planResponses, null);

        } catch (Exception e) {
            return new ApiResponse<>(false, null, "Failed to load plans: " + e.getMessage());
        }
    }

    /**
     * Convert Plan model to PlanResponse DTO for frontend
     * @param plan the plan model
     * @return plan response DTO
     */
    private PlanResponse convertToPlanResponse(Plan plan) {
        PlanResponse response = new PlanResponse();
        response.setId(plan.getId().intValue());
        response.setName(plan.getName());
        response.setDescription(plan.getDescription());

        // Convert price based on billing cycle
        BigDecimal price = plan.getPrice() != null ? plan.getPrice() : BigDecimal.ZERO;
        if ("MONTHLY".equals(plan.getBillingCycle())) {
            response.setPriceMonthly(price.doubleValue());
            response.setPriceYearly(price.multiply(BigDecimal.valueOf(12)).doubleValue());
        } else {
            response.setPriceYearly(price.doubleValue());
            response.setPriceMonthly(price.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP).doubleValue());
        }

        // Build features list based on plan capabilities
        List<String> features = new ArrayList<>();
        if (plan.isCanAccessDashboard()) {
            features.add("Dashboard access");
        }
        if (plan.isCanProcessBankStatements()) {
            features.add("Bank statement processing");
        }
        if (plan.isCanGenerateReports()) {
            features.add("Financial reports");
        }
        if (plan.isCanManageCompanies()) {
            features.add("Multi-company management");
        }
        if (plan.isCanManagePayroll()) {
            features.add("Payroll management");
        }
        if (plan.isCanManageBudgets()) {
            features.add("Budget planning");
        }
        if (plan.isHasApiAccess()) {
            features.add("API access");
        }
        if (plan.isHasPrioritySupport()) {
            features.add("Priority support");
        }

        // Add transaction and company limits
        if (plan.getMaxTransactionsPerMonth() > 0) {
            if (plan.getMaxTransactionsPerMonth() >= 10000) {
                features.add("Unlimited transactions");
            } else {
                features.add(plan.getMaxTransactionsPerMonth() + " transactions/month");
            }
        }

        if (plan.getMaxCompanies() > 1) {
            if (plan.getMaxCompanies() >= 10) {
                features.add("Unlimited companies");
            } else {
                features.add("Up to " + plan.getMaxCompanies() + " companies");
            }
        }

        response.setFeatures(features);
        response.setMaxCompanies(plan.getMaxCompanies());
        response.setMaxTransactions(plan.getMaxTransactionsPerMonth());

        // Determine support level based on features
        String supportLevel = "basic";
        if (plan.isHasPrioritySupport()) {
            supportLevel = "enterprise";
        } else if (plan.isHasApiAccess() || plan.getMaxCompanies() > 3) {
            supportLevel = "premium";
        }
        response.setSupportLevel(supportLevel);

        response.setIsActive(plan.isActive());

        return response;
    }

    /**
     * DTO for plan response to match frontend expectations
     */
    public static class PlanResponse {
        private int id;
        private String name;
        private String description;
        private double priceMonthly;
        private double priceYearly;
        private List<String> features;
        private int maxCompanies;
        private int maxTransactions;
        private String supportLevel;
        private boolean isActive;

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getPriceMonthly() { return priceMonthly; }
        public void setPriceMonthly(double priceMonthly) { this.priceMonthly = priceMonthly; }

        public double getPriceYearly() { return priceYearly; }
        public void setPriceYearly(double priceYearly) { this.priceYearly = priceYearly; }

        public List<String> getFeatures() { return features; }
        public void setFeatures(List<String> features) { this.features = features; }

        public int getMaxCompanies() { return maxCompanies; }
        public void setMaxCompanies(int maxCompanies) { this.maxCompanies = maxCompanies; }

        public int getMaxTransactions() { return maxTransactions; }
        public void setMaxTransactions(int maxTransactions) { this.maxTransactions = maxTransactions; }

        public String getSupportLevel() { return supportLevel; }
        public void setSupportLevel(String supportLevel) { this.supportLevel = supportLevel; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean isActive) { this.isActive = isActive; }
        public void setIsActive(boolean isActive) { this.isActive = isActive; }
    }
}