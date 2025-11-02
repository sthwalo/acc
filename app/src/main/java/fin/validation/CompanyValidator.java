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

import fin.model.Company;

/**
 * Validator for Company entities
 */
public class CompanyValidator implements ModelValidator<Company> {
    
    // Field length limits
    private static final int MAX_COMPANY_NAME_LENGTH = 100;
    private static final int MAX_REGISTRATION_NUMBER_LENGTH = 50;
    private static final int MAX_TAX_NUMBER_LENGTH = 50;
    @Override
    public ValidationResult validate(Company company) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (company.getName() == null || company.getName().trim().isEmpty()) {
            result.addError("name", "Company name is required");
        }

        // Length validations
        if (company.getName() != null && company.getName().length() > MAX_COMPANY_NAME_LENGTH) {
            result.addError("name", "Company name cannot exceed " + MAX_COMPANY_NAME_LENGTH + " characters");
        }

        if (company.getRegistrationNumber() != null && company.getRegistrationNumber().length() > MAX_REGISTRATION_NUMBER_LENGTH) {
            result.addError("registrationNumber", "Registration number cannot exceed " + MAX_REGISTRATION_NUMBER_LENGTH + " characters");
        }

        if (company.getTaxNumber() != null && company.getTaxNumber().length() > MAX_TAX_NUMBER_LENGTH) {
            result.addError("taxNumber", "Tax number cannot exceed " + MAX_TAX_NUMBER_LENGTH + " characters");
        }

        // Format validations
        if (company.getContactEmail() != null && !company.getContactEmail().isEmpty()) {
            if (!company.getContactEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                result.addError("contactEmail", "Invalid email format");
            }
        }

        if (company.getContactPhone() != null && !company.getContactPhone().isEmpty()) {
            // Allow numbers, spaces, +, -, and ()
            if (!company.getContactPhone().matches("^[0-9+\\-() ]+$")) {
                result.addError("contactPhone", "Invalid phone number format");
            }
        }

        return result;
    }
}
