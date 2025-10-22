package fin.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object representing monetary amounts.
 * Follows Domain-Driven Design principles for type safety and currency handling.
 */
public record Money(
    BigDecimal amount,
    Currency currency
) {
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("ZAR");

    public Money(BigDecimal amount) {
        this(amount, DEFAULT_CURRENCY);
    }

    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        // Ensure consistent scale (2 decimal places)
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Creates a Money instance from a double value.
     */
    public static Money of(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    /**
     * Creates a Money instance from a BigDecimal value.
     */
    public static Money of(BigDecimal amount) {
        return new Money(amount);
    }

    /**
     * Creates a Money instance from a double value with specified currency.
     */
    public static Money of(double amount, Currency currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    /**
     * Adds two Money amounts.
     */
    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Subtracts another Money amount.
     */
    public Money subtract(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * Multiplies by a scalar value.
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    /**
     * Multiplies by a double value.
     */
    public Money multiply(double multiplier) {
        return multiply(BigDecimal.valueOf(multiplier));
    }

    /**
     * Checks if this amount is positive.
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this amount is negative.
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Checks if this amount is zero.
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Gets the absolute value.
     */
    public Money abs() {
        return new Money(amount.abs(), currency);
    }

    /**
     * Negates the amount.
     */
    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    @Override
    public String toString() {
        return currency.getSymbol() + amount.toString();
    }
}