package fin.validation;

import fin.model.Company;

/**
 * Validator for Company entities
 */
public class CompanyValidator implements ModelValidator<Company> {
    @Override
    public ValidationResult validate(Company company) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (company.getName() == null || company.getName().trim().isEmpty()) {
            result.addError("name", "Company name is required");
        }

        // Length validations
        if (company.getName() != null && company.getName().length() > 100) {
            result.addError("name", "Company name cannot exceed 100 characters");
        }

        if (company.getRegistrationNumber() != null && company.getRegistrationNumber().length() > 50) {
            result.addError("registrationNumber", "Registration number cannot exceed 50 characters");
        }

        if (company.getTaxNumber() != null && company.getTaxNumber().length() > 50) {
            result.addError("taxNumber", "Tax number cannot exceed 50 characters");
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
