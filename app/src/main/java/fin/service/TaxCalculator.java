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
 */

package fin.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * South African Tax Calculator for 2026 tax year
 * Implements official SARS tax tables and brackets
 * Converted from C implementation to Java
 */
public class TaxCalculator {

    // 2026 Tax Year Constants
    private static final BigDecimal PRIMARY_REBATE = new BigDecimal("15744");
    private static final BigDecimal SECONDARY_REBATE = new BigDecimal("11007"); // 65+
    private static final BigDecimal TERTIARY_REBATE = new BigDecimal("3646");   // 75+

    // UIF Constants for 2026
    private static final BigDecimal UIF_RATE = new BigDecimal("0.01");
    private static final BigDecimal UIF_MAX_EARNINGS = new BigDecimal("22608"); // Monthly max

    // 2026 Monthly Tax Brackets (SARS Monthly Tax Deduction Tables)
    private static final TaxBracket[] MONTHLY_TAX_BRACKETS = {
        new TaxBracket(BigDecimal.ZERO, new BigDecimal("19758.33"), new BigDecimal("0.18"), BigDecimal.ZERO),
        new TaxBracket(new BigDecimal("19758.33"), new BigDecimal("30875.00"), new BigDecimal("0.26"), new BigDecimal("3556.50")),
        new TaxBracket(new BigDecimal("30875.00"), new BigDecimal("42733.33"), new BigDecimal("0.31"), new BigDecimal("6446.83")),
        new TaxBracket(new BigDecimal("42733.33"), new BigDecimal("56083.33"), new BigDecimal("0.36"), new BigDecimal("10122.92")),
        new TaxBracket(new BigDecimal("56083.33"), new BigDecimal("71491.67"), new BigDecimal("0.39"), new BigDecimal("14895.58")),
        new TaxBracket(new BigDecimal("71491.67"), new BigDecimal("151416.67"), new BigDecimal("0.41"), new BigDecimal("20938.17")),
        new TaxBracket(new BigDecimal("151416.67"), null, new BigDecimal("0.45"), new BigDecimal("53707.42"))
    };

    /**
     * Tax bracket definition
     */
    public static class TaxBracket {
        private final BigDecimal minAmount;
        private final BigDecimal maxAmount;
        private final BigDecimal rate;
        private final BigDecimal cumulativeTax;

        public TaxBracket(BigDecimal minAmount, BigDecimal maxAmount, BigDecimal rate, BigDecimal cumulativeTax) {
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.rate = rate;
            this.cumulativeTax = cumulativeTax;
        }

        public BigDecimal getMinAmount() { return minAmount; }
        public BigDecimal getMaxAmount() { return maxAmount; }
        public BigDecimal getRate() { return rate; }
        public BigDecimal getCumulativeTax() { return cumulativeTax; }

        public boolean contains(BigDecimal amount) {
            return amount.compareTo(minAmount) >= 0 &&
                   (maxAmount == null || amount.compareTo(maxAmount) < 0);
        }
    }

    /**
     * Calculate PAYE tax for monthly salary using SARS monthly tax deduction tables
     * @param monthlySalary Monthly gross salary
     * @param age Age of employee for rebate calculation
     * @return Monthly PAYE tax amount
     */
    public static BigDecimal calculatePAYE(BigDecimal monthlySalary, int age) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal tax = BigDecimal.ZERO;

        // Find applicable monthly tax bracket
        for (TaxBracket bracket : MONTHLY_TAX_BRACKETS) {
            if (bracket.contains(monthlySalary)) {
                // Calculate tax for this bracket
                BigDecimal taxableInBracket = monthlySalary.subtract(bracket.getMinAmount());
                tax = bracket.getCumulativeTax().add(
                    taxableInBracket.multiply(bracket.getRate())
                );
                break;
            }
        }

        // Apply monthly tax rebates (annual rebates divided by 12)
        BigDecimal monthlyRebate = getTaxRebate(age).divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        tax = tax.subtract(monthlyRebate);

        // Ensure tax is not negative
        return tax.max(BigDecimal.ZERO);
    }

    /**
     * Calculate annual PAYE tax (for compatibility - converts monthly calculation)
     * @param annualSalary Annual gross salary
     * @param age Age of employee for rebate calculation
     * @return Annual PAYE tax amount
     */
    public static BigDecimal calculateAnnualPAYE(BigDecimal annualSalary, int age) {
        if (annualSalary == null || annualSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // Convert annual to monthly, calculate monthly tax, then convert back
        BigDecimal monthlySalary = annualSalary.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        BigDecimal monthlyTax = calculatePAYE(monthlySalary, age);
        return monthlyTax.multiply(BigDecimal.valueOf(12));
    }

    /**
     * Get tax rebate based on age
     * @param age Employee age
     * @return Tax rebate amount
     */
    private static BigDecimal getTaxRebate(int age) {
        if (age >= 75) {
            return PRIMARY_REBATE.add(SECONDARY_REBATE).add(TERTIARY_REBATE);
        } else if (age >= 65) {
            return PRIMARY_REBATE.add(SECONDARY_REBATE);
        } else {
            return PRIMARY_REBATE;
        }
    }

    /**
     * Calculate UIF employee contribution
     * @param monthlySalary Monthly gross salary
     * @return Monthly UIF contribution
     */
    public static BigDecimal calculateUIFEmployee(BigDecimal monthlySalary) {
        if (monthlySalary == null || monthlySalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal uifableIncome = monthlySalary.min(UIF_MAX_EARNINGS);
        return uifableIncome.multiply(UIF_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate UIF employer contribution (same as employee)
     * @param monthlySalary Monthly gross salary
     * @return Monthly UIF contribution
     */
    public static BigDecimal calculateUIFEmployer(BigDecimal monthlySalary) {
        return calculateUIFEmployee(monthlySalary);
    }

    /**
     * Calculate net pay after tax and deductions
     * @param grossSalary Monthly gross salary
     * @param payeTax Monthly PAYE tax
     * @param uifEmployee Monthly UIF employee contribution
     * @param otherDeductions Other monthly deductions
     * @return Monthly net pay
     */
    public static BigDecimal calculateNetPay(BigDecimal grossSalary, BigDecimal payeTax,
                                           BigDecimal uifEmployee, BigDecimal otherDeductions) {
        if (grossSalary == null) return BigDecimal.ZERO;

        BigDecimal totalDeductions = BigDecimal.ZERO;

        if (payeTax != null) totalDeductions = totalDeductions.add(payeTax);
        if (uifEmployee != null) totalDeductions = totalDeductions.add(uifEmployee);
        if (otherDeductions != null) totalDeductions = totalDeductions.add(otherDeductions);

        return grossSalary.subtract(totalDeductions).max(BigDecimal.ZERO);
    }

    /**
     * Get tax bracket information for a given annual salary
     * @param annualSalary Annual salary
     * @return Tax bracket information
     */
    public static TaxBracket getTaxBracket(BigDecimal annualSalary) {
        // Convert annual to monthly for bracket lookup
        BigDecimal monthlySalary = annualSalary.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        for (TaxBracket bracket : MONTHLY_TAX_BRACKETS) {
            if (bracket.contains(monthlySalary)) {
                return bracket;
            }
        }
        return MONTHLY_TAX_BRACKETS[MONTHLY_TAX_BRACKETS.length - 1]; // Top bracket
    }

    /**
     * Calculate marginal tax rate for a given annual salary
     * @param annualSalary Annual salary
     * @return Marginal tax rate
     */
    public static BigDecimal getMarginalTaxRate(BigDecimal annualSalary) {
        TaxBracket bracket = getTaxBracket(annualSalary);
        return bracket.getRate();
    }

    /**
     * Calculate effective tax rate for a given annual salary
     * @param annualSalary Annual salary
     * @param age Employee age
     * @return Effective tax rate (0.0 to 1.0)
     */
    public static BigDecimal getEffectiveTaxRate(BigDecimal annualSalary, int age) {
        if (annualSalary.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal annualTax = calculateAnnualPAYE(annualSalary, age);
        return annualTax.divide(annualSalary, 4, RoundingMode.HALF_UP);
    }
}