/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for fiscal period setup during company onboarding.
 * Used to calculate fiscal period dates based on year-end month selection.
 */
public class FiscalPeriodSetupDTO {

    /**
     * The year-end month (1-12, where 1=January, 12=December)
     * This determines the fiscal period boundaries.
     */
    @NotNull(message = "Year-end month is required")
    @Min(value = 1, message = "Year-end month must be between 1 and 12")
    @Max(value = 12, message = "Year-end month must be between 1 and 12")
    private Integer yearEndMonth;

    /**
     * The fiscal year to set up (e.g., 2024)
     * This is the year in which the fiscal period ends.
     */
    @NotNull(message = "Fiscal year is required")
    @Min(value = 2000, message = "Fiscal year must be 2000 or later")
    @Max(value = 2100, message = "Fiscal year must be 2100 or earlier")
    private Integer fiscalYear;

    public FiscalPeriodSetupDTO() {
    }

    public FiscalPeriodSetupDTO(Integer yearEndMonth, Integer fiscalYear) {
        this.yearEndMonth = yearEndMonth;
        this.fiscalYear = fiscalYear;
    }

    public Integer getYearEndMonth() {
        return yearEndMonth;
    }

    public void setYearEndMonth(Integer yearEndMonth) {
        this.yearEndMonth = yearEndMonth;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    @Override
    public String toString() {
        return "FiscalPeriodSetupDTO{" +
                "yearEndMonth=" + yearEndMonth +
                ", fiscalYear=" + fiscalYear +
                '}';
    }
}