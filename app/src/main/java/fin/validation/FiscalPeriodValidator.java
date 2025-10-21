package fin.validation;

import fin.model.FiscalPeriod;
import java.time.LocalDate;

/**
 * Validator for FiscalPeriod entities
 */
public class FiscalPeriodValidator implements ModelValidator<FiscalPeriod> {
    private static final int MAX_PERIOD_NAME_LENGTH = 100;
    @Override
    public ValidationResult validate(FiscalPeriod period) {
        ValidationResult result = new ValidationResult();

        // Required fields
        if (period.getCompanyId() == null) {
            result.addError("companyId", "Company ID is required");
        }

        if (period.getPeriodName() == null || period.getPeriodName().trim().isEmpty()) {
            result.addError("periodName", "Period name is required");
        }

        if (period.getStartDate() == null) {
            result.addError("startDate", "Start date is required");
        }

        if (period.getEndDate() == null) {
            result.addError("endDate", "End date is required");
        }

        // Date validations
        if (period.getStartDate() != null && period.getEndDate() != null) {
            if (period.getStartDate().isAfter(period.getEndDate())) {
                result.addError("dateRange", "Start date cannot be after end date");
            }

            // Check if period is in the future
            LocalDate today = LocalDate.now();
            if (period.getStartDate().isAfter(today.plusYears(1))) {
                result.addError("startDate", "Fiscal period cannot start more than 1 year in the future");
            }

            // Check if period length is reasonable (e.g., not more than 2 years)
            if (period.getStartDate().plusYears(2).isBefore(period.getEndDate())) {
                result.addError("dateRange", "Fiscal period cannot be longer than 2 years");
            }
        }

        // Length validations
        if (period.getPeriodName() != null && period.getPeriodName().length() > MAX_PERIOD_NAME_LENGTH) {
            result.addError("periodName", "Period name cannot exceed " + MAX_PERIOD_NAME_LENGTH + " characters");
        }

        return result;
    }
}
