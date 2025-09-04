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

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
