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

package fin.model;

/**
 * Value object representing an account code.
 * Follows Domain-Driven Design principles for type safety and expressiveness.
 */
public record AccountCode(String value) {
    public AccountCode {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Account code cannot be null or empty");
        }
        if (!value.matches("^[0-9]{4}(-[0-9]{1,3})?$")) {
            throw new IllegalArgumentException("Account code must be in format XXXX or XXXX-YYY (e.g., 1000 or 1000-001)");
        }
    }

    /**
     * Gets the main account code (first 4 digits).
     */
    public String getMainCode() {
        return value.split("-")[0];
    }

    /**
     * Gets the subaccount code if present.
     */
    public String getSubCode() {
        String[] parts = value.split("-");
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Checks if this is a subaccount.
     */
    public boolean isSubAccount() {
        return value.contains("-");
    }

    @Override
    public String toString() {
        return value;
    }
}