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
