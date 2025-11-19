package fin.validation;

/**
 * Base interface for all validators
 */
public interface ModelValidator<T> {
    ValidationResult validate(T model);
}