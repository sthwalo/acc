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

import java.util.ArrayList;
import java.util.List;

/**
 * Generic validation result container
 */
public class ValidationResult {
    private final List<ValidationError> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    public void addError(String field, String message) {
        errors.add(new ValidationError(field, message));
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return new ArrayList<>(errors);
    }

    public static class ValidationError {
        private final String field;
        private final String message;

        public ValidationError(String valueField, String valueMessage) {
            this.field = valueField;
            this.message = valueMessage;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
