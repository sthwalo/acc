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

package fin.api.util;

import fin.api.dto.requests.LoginRequest;
import fin.api.dto.requests.RegisterRequest;
import fin.api.dto.requests.CompanyRequest;

/**
 * Request Validator utility class
 * Provides static methods for validating request parameters
 */
public class RequestValidator {

    /**
     * Validates that an object is not null
     * @param value the value to check
     * @param fieldName the field name for error messages
     * @throws IllegalArgumentException if value is null
     */
    public static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    /**
     * Validates that a string is not null or empty
     * @param value the string to check
     * @param fieldName the field name for error messages
     * @throws IllegalArgumentException if value is null or empty
     */
    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
    }

    /**
     * Validates that a number is positive
     * @param value the number to check
     * @param fieldName the field name for error messages
     * @throws IllegalArgumentException if value is not positive
     */
    public static void requirePositive(Number value, String fieldName) {
        if (value == null || value.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Validates a login request
     * @param request the login request to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidLoginRequest(LoginRequest request) {
        if (request == null) {
            return false;
        }

        try {
            requireNonEmpty(request.getEmail(), "email");
            requireNonEmpty(request.getPassword(), "password");

            // Basic email format validation
            if (!request.getEmail().contains("@") || !request.getEmail().contains(".")) {
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a registration request
     * @param request the registration request to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidRegisterRequest(RegisterRequest request) {
        if (request == null) {
            return false;
        }

        try {
            requireNonEmpty(request.getEmail(), "email");
            requireNonEmpty(request.getPassword(), "password");
            requireNonEmpty(request.getFirstName(), "firstName");
            requireNonEmpty(request.getLastName(), "lastName");

            // Basic email format validation
            if (!request.getEmail().contains("@") || !request.getEmail().contains(".")) {
                return false;
            }

            // Password strength validation (minimum 6 characters)
            if (request.getPassword().length() < 6) {
                return false;
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates a company request
     * @param request the company request to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidCompanyRequest(CompanyRequest request) {
        if (request == null) {
            return false;
        }

        try {
            requireNonEmpty(request.getName(), "name");

            // Optional fields can be null but if present should be reasonable
            if (request.getContactEmail() != null && !request.getContactEmail().trim().isEmpty()) {
                // Basic email format validation
                String email = request.getContactEmail();
                if (!email.contains("@") || !email.contains(".")) {
                    return false;
                }
            }

            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private RequestValidator() {
        // Utility class, prevent instantiation
    }
}