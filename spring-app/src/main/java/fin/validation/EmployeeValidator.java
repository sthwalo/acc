package fin.validation;

import fin.entity.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Validator for Employee entities with comprehensive South African business rules
 */
@Component
public class EmployeeValidator implements ModelValidator<Employee> {

    // South African Tax Number patterns (simplified - actual validation may be more complex)
    private static final Pattern TAX_NUMBER_PATTERN = Pattern.compile("^\\d{10,13}$");

    // South African ID Number pattern (13 digits) - removed as we only validate tax numbers
    // private static final Pattern ID_NUMBER_PATTERN = Pattern.compile("^\\d{13}$");

    // Email pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // South African phone number patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(?:\\+27|27|0)[6-8][0-9]{8}$");

    // Bank account number pattern (South African - typically 8-16 digits)
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^\\d{8,16}$");

    // Bank branch code pattern (South African - typically 6 digits)
    private static final Pattern BRANCH_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    // Postal code pattern (South African - 4 digits)
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^\\d{4}$");

    @Override
    public ValidationResult validate(Employee employee) {
        ValidationResult result = new ValidationResult();

        // Required fields validation
        validateRequiredFields(employee, result);

        // Format validations
        validateFormats(employee, result);

        // Business rule validations
        validateBusinessRules(employee, result);

        return result;
    }

    /**
     * Validate employee for updates (partial validation)
     * Only validates fields that are provided (non-null)
     */
    public ValidationResult validateForUpdate(Employee employee) {
        ValidationResult result = new ValidationResult();

        // Only validate non-null fields for updates
        validateUpdateFields(employee, result);

        // Format validations (only for non-null fields)
        validateFormatsForUpdate(employee, result);

        // Business rule validations (only for non-null fields)
        validateBusinessRulesForUpdate(employee, result);

        return result;
    }

    private void validateRequiredFields(Employee employee, ValidationResult result) {
        // Core required fields for employee creation
        if (employee.getEmployeeNumber() == null || employee.getEmployeeNumber().trim().isEmpty()) {
            result.addError("employeeNumber", "Employee number is required");
        }

        if (employee.getFirstName() == null || employee.getFirstName().trim().isEmpty()) {
            result.addError("firstName", "First name is required");
        }

        if (employee.getLastName() == null || employee.getLastName().trim().isEmpty()) {
            result.addError("lastName", "Last name is required");
        }

        if (employee.getCompanyId() == null) {
            result.addError("companyId", "Company ID is required");
        }

        if (employee.getBasicSalary() == null) {
            result.addError("basicSalary", "Basic salary is required");
        } else if (employee.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
            result.addError("basicSalary", "Basic salary must be greater than zero");
        }

        // Employment details
        if (employee.getHireDate() == null) {
            result.addError("hireDate", "Hire date is required");
        }

        // Banking details - required for payroll processing
        if (employee.getBankName() == null || employee.getBankName().trim().isEmpty()) {
            result.addError("bankName", "Bank name is required for payroll processing");
        }

        if (employee.getAccountNumber() == null || employee.getAccountNumber().trim().isEmpty()) {
            result.addError("accountNumber", "Account number is required for payroll processing");
        }

        if (employee.getBranchCode() == null || employee.getBranchCode().trim().isEmpty()) {
            result.addError("branchCode", "Branch code is required for payroll processing");
        }

        // Tax details - required for SARS compliance
        if (employee.getTaxNumber() == null || employee.getTaxNumber().trim().isEmpty()) {
            result.addError("taxNumber", "Tax number is required for SARS compliance");
        }
    }

    private void validateFormats(Employee employee, ValidationResult result) {
        // Email validation
        if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(employee.getEmail().trim()).matches()) {
                result.addError("email", "Invalid email format");
            }
        }

        // Phone validation
        if (employee.getPhone() != null && !employee.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(employee.getPhone().trim()).matches()) {
                result.addError("phone", "Invalid South African phone number format. Expected format: +27XXXXXXXXX, 27XXXXXXXXX, or 0XXXXXXXXX");
            }
        }

        // Tax number validation (South African)
        if (employee.getTaxNumber() != null && !employee.getTaxNumber().trim().isEmpty()) {
            String taxNumber = employee.getTaxNumber().trim();
            if (!TAX_NUMBER_PATTERN.matcher(taxNumber).matches()) {
                result.addError("taxNumber", "Invalid tax number format. Must be 10-13 digits");
            }
        }

        // Note: ID number validation removed - tax numbers and ID numbers are often the same in South Africa
        // but have different validation rules. Since we only have taxNumber field, we validate as tax number only.

        // Banking details validation
        if (employee.getAccountNumber() != null && !employee.getAccountNumber().trim().isEmpty()) {
            if (!ACCOUNT_NUMBER_PATTERN.matcher(employee.getAccountNumber().trim()).matches()) {
                result.addError("accountNumber", "Invalid account number format. Must be 8-16 digits");
            }
        }

        if (employee.getBranchCode() != null && !employee.getBranchCode().trim().isEmpty()) {
            if (!BRANCH_CODE_PATTERN.matcher(employee.getBranchCode().trim()).matches()) {
                result.addError("branchCode", "Invalid branch code format. Must be exactly 6 digits");
            }
        }

        // Postal code validation
        if (employee.getPostalCode() != null && !employee.getPostalCode().trim().isEmpty()) {
            if (!POSTAL_CODE_PATTERN.matcher(employee.getPostalCode().trim()).matches()) {
                result.addError("postalCode", "Invalid postal code format. Must be exactly 4 digits");
            }
        }
    }

    private void validateBusinessRules(Employee employee, ValidationResult result) {
        // Age validation (must be at least 16 for employment)
        if (employee.getHireDate() != null) {
            LocalDate now = LocalDate.now();
            if (employee.getHireDate().isAfter(now)) {
                result.addError("hireDate", "Hire date cannot be in the future");
            }

            // If date of birth is available, check minimum age
            // Note: Employee model doesn't have dateOfBirth field yet
            // This would be added in future enhancement
        }

        // Termination date validation
        if (employee.getTerminationDate() != null) {
            if (employee.getHireDate() != null && employee.getTerminationDate().isBefore(employee.getHireDate())) {
                result.addError("terminationDate", "Termination date cannot be before hire date");
            }

            if (employee.getTerminationDate().isAfter(LocalDate.now())) {
                result.addError("terminationDate", "Termination date cannot be in the future");
            }
        }

        // Salary validation (reasonable bounds for South African context)
        if (employee.getBasicSalary() != null) {
            BigDecimal minSalary = new BigDecimal("4000.00"); // Minimum wage consideration
            BigDecimal maxSalary = new BigDecimal("500000.00"); // Reasonable upper bound

            if (employee.getBasicSalary().compareTo(minSalary) < 0) {
                result.addError("basicSalary", "Basic salary seems unusually low. Please verify the amount");
            }

            if (employee.getBasicSalary().compareTo(maxSalary) > 0) {
                result.addError("basicSalary", "Basic salary seems unusually high. Please verify the amount");
            }
        }

        // Overtime rate validation
        if (employee.getOvertimeRate() != null) {
            if (employee.getOvertimeRate().compareTo(BigDecimal.ONE) < 0) {
                result.addError("overtimeRate", "Overtime rate cannot be less than 1.0");
            }

            if (employee.getOvertimeRate().compareTo(new BigDecimal("3.0")) > 0) {
                result.addError("overtimeRate", "Overtime rate seems unusually high. Please verify");
            }
        }

        // Account holder name validation (should match employee name if not specified)
        if (employee.getAccountHolderName() != null && !employee.getAccountHolderName().trim().isEmpty()) {
            String fullName = (employee.getFirstName() + " " + employee.getLastName()).toLowerCase();
            String accountHolder = employee.getAccountHolderName().toLowerCase();

            // Basic check - account holder should contain employee name
            if (!accountHolder.contains(employee.getFirstName().toLowerCase()) ||
                !accountHolder.contains(employee.getLastName().toLowerCase())) {
                result.addError("accountHolderName", "Account holder name should match the employee's name for banking security");
            }
        }
    }

    /**
     * Validate only the fields that are being updated (non-null values)
     */
    private void validateUpdateFields(Employee employee, ValidationResult result) {
        // Core required fields for employee creation - but for updates, only validate if the field is being set
        // Note: We don't validate required fields for updates since they might already exist in the database

        // Basic salary validation (if being updated)
        if (employee.getBasicSalary() != null) {
            if (employee.getBasicSalary().compareTo(BigDecimal.ZERO) <= 0) {
                result.addError("basicSalary", "Basic salary must be greater than zero");
            }
        }

        // Hire date validation (if being updated)
        if (employee.getHireDate() != null) {
            LocalDate now = LocalDate.now();
            if (employee.getHireDate().isAfter(now)) {
                result.addError("hireDate", "Hire date cannot be in the future");
            }
        }

        // Banking details - only validate if being updated
        if (employee.getBankName() != null && employee.getBankName().trim().isEmpty()) {
            result.addError("bankName", "Bank name cannot be empty if provided");
        }

        if (employee.getAccountNumber() != null && employee.getAccountNumber().trim().isEmpty()) {
            result.addError("accountNumber", "Account number cannot be empty if provided");
        }

        if (employee.getBranchCode() != null && employee.getBranchCode().trim().isEmpty()) {
            result.addError("branchCode", "Branch code cannot be empty if provided");
        }

        // Tax details - only validate if being updated
        if (employee.getTaxNumber() != null && employee.getTaxNumber().trim().isEmpty()) {
            result.addError("taxNumber", "Tax number cannot be empty if provided");
        }
    }

    /**
     * Validate formats only for non-null fields during updates
     */
    private void validateFormatsForUpdate(Employee employee, ValidationResult result) {
        // Email validation (only if provided)
        if (employee.getEmail() != null && !employee.getEmail().trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(employee.getEmail().trim()).matches()) {
                result.addError("email", "Invalid email format");
            }
        }

        // Phone validation (only if provided)
        if (employee.getPhone() != null && !employee.getPhone().trim().isEmpty()) {
            if (!PHONE_PATTERN.matcher(employee.getPhone().trim()).matches()) {
                result.addError("phone", "Invalid South African phone number format. Expected format: +27XXXXXXXXX, 27XXXXXXXXX, or 0XXXXXXXXX");
            }
        }

        // Tax number validation (only if provided)
        if (employee.getTaxNumber() != null && !employee.getTaxNumber().trim().isEmpty()) {
            String taxNumber = employee.getTaxNumber().trim();
            if (!TAX_NUMBER_PATTERN.matcher(taxNumber).matches()) {
                result.addError("taxNumber", "Invalid tax number format. Must be 10-13 digits");
            }
        }

        // Banking details validation (only if provided)
        if (employee.getAccountNumber() != null && !employee.getAccountNumber().trim().isEmpty()) {
            if (!ACCOUNT_NUMBER_PATTERN.matcher(employee.getAccountNumber().trim()).matches()) {
                result.addError("accountNumber", "Invalid account number format. Must be 8-16 digits");
            }
        }

        if (employee.getBranchCode() != null && !employee.getBranchCode().trim().isEmpty()) {
            if (!BRANCH_CODE_PATTERN.matcher(employee.getBranchCode().trim()).matches()) {
                result.addError("branchCode", "Invalid branch code format. Must be exactly 6 digits");
            }
        }

        // Postal code validation (only if provided)
        if (employee.getPostalCode() != null && !employee.getPostalCode().trim().isEmpty()) {
            if (!POSTAL_CODE_PATTERN.matcher(employee.getPostalCode().trim()).matches()) {
                result.addError("postalCode", "Invalid postal code format. Must be exactly 4 digits");
            }
        }
    }

    /**
     * Validate business rules only for non-null fields during updates
     */
    private void validateBusinessRulesForUpdate(Employee employee, ValidationResult result) {
        // Age validation (only if hire date is being updated)
        if (employee.getHireDate() != null) {
            LocalDate now = LocalDate.now();
            if (employee.getHireDate().isAfter(now)) {
                result.addError("hireDate", "Hire date cannot be in the future");
            }
        }

        // Termination date validation (only if being updated)
        if (employee.getTerminationDate() != null) {
            if (employee.getHireDate() != null && employee.getTerminationDate().isBefore(employee.getHireDate())) {
                result.addError("terminationDate", "Termination date cannot be before hire date");
            }

            if (employee.getTerminationDate().isAfter(LocalDate.now())) {
                result.addError("terminationDate", "Termination date cannot be in the future");
            }
        }

        // Salary validation (only if being updated)
        if (employee.getBasicSalary() != null) {
            BigDecimal minSalary = new BigDecimal("4000.00"); // Minimum wage consideration
            BigDecimal maxSalary = new BigDecimal("500000.00"); // Reasonable upper bound

            if (employee.getBasicSalary().compareTo(minSalary) < 0) {
                result.addError("basicSalary", "Basic salary seems unusually low. Please verify the amount");
            }

            if (employee.getBasicSalary().compareTo(maxSalary) > 0) {
                result.addError("basicSalary", "Basic salary seems unusually high. Please verify the amount");
            }
        }

        // Overtime rate validation (only if being updated)
        if (employee.getOvertimeRate() != null) {
            if (employee.getOvertimeRate().compareTo(BigDecimal.ONE) < 0) {
                result.addError("overtimeRate", "Overtime rate cannot be less than 1.0");
            }

            if (employee.getOvertimeRate().compareTo(new BigDecimal("3.0")) > 0) {
                result.addError("overtimeRate", "Overtime rate seems unusually high. Please verify");
            }
        }

        // Account holder name validation (only if being updated)
        if (employee.getAccountHolderName() != null && !employee.getAccountHolderName().trim().isEmpty()) {
            String fullName = (employee.getFirstName() + " " + employee.getLastName()).toLowerCase();
            String accountHolder = employee.getAccountHolderName().toLowerCase();

            // Basic check - account holder should contain employee name
            if (!accountHolder.contains(employee.getFirstName().toLowerCase()) ||
                !accountHolder.contains(employee.getLastName().toLowerCase())) {
                result.addError("accountHolderName", "Account holder name should match the employee's name for banking security");
            }
        }
    }
}