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

package fin.validation;

import fin.entity.Company;
import fin.entity.User;
import fin.service.CompanyService;
import fin.service.PlanService;
import org.springframework.stereotype.Component;

/**
 * Validator for Company entities with plan-based business rules
 */
@Component
public class CompanyValidator implements ModelValidator<Company> {

    private final PlanService planService;
    private final CompanyService companyService;

    public CompanyValidator(PlanService planService, CompanyService companyService) {
        this.planService = planService;
        this.companyService = companyService;
    }

    @Override
    public ValidationResult validate(Company company) {
        ValidationResult result = new ValidationResult();

        // Basic company validation
        validateRequiredFields(company, result);

        return result;
    }

    /**
     * Validate company creation permissions for a user
     */
    public ValidationResult validateCompanyCreation(Company company, User user) {
        ValidationResult result = new ValidationResult();

        // First validate the company itself
        result = validate(company);
        if (!result.isValid()) {
            return result;
        }

        // Check if user has permission to manage companies based on their plan
        if (!planService.canPlanAccessFeature(user.getPlanId(), "companies")) {
            result.addError("plan", "Your current plan does not allow company management");
            return result;
        }

        // Check if user has reached their company limit
        int maxCompanies = planService.getMaxCompaniesForPlan(user.getPlanId());
        int currentCompanies = 0;
        try {
            currentCompanies = companyService.getCompaniesForUser(user.getId()).size();
        } catch (Exception e) {
            result.addError("database", "Unable to verify current company count: " + e.getMessage());
            return result;
        }
        // Only check limit if plan has a positive limit (negative values mean unlimited)
        if (maxCompanies > 0 && currentCompanies >= maxCompanies) {
            result.addError("limit", "Company limit reached: Your plan allows maximum " + maxCompanies + " companies");
            return result;
        }

        return result;
    }

    private void validateRequiredFields(Company company, ValidationResult result) {
        if (company.getName() == null || company.getName().trim().isEmpty()) {
            result.addError("name", "Company name is required");
        }

        if (company.getName() != null && company.getName().length() > 255) {
            result.addError("name", "Company name cannot exceed 255 characters");
        }
    }
}