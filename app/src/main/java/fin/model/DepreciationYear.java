package fin.model;

import java.math.BigDecimal;

/**
 * Represents a single year in a depreciation schedule
 */
public class DepreciationYear {
    private int year;
    private BigDecimal depreciation;
    private BigDecimal cumulativeDepreciation;
    private BigDecimal bookValue;

    public DepreciationYear() {
        // Default constructor
    }

    public DepreciationYear(int year, BigDecimal depreciation,
                           BigDecimal cumulativeDepreciation, BigDecimal bookValue) {
        this.year = year;
        this.depreciation = depreciation;
        this.cumulativeDepreciation = cumulativeDepreciation;
        this.bookValue = bookValue;
    }

    // Getters and setters
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public BigDecimal getDepreciation() {
        return depreciation;
    }

    public void setDepreciation(BigDecimal depreciation) {
        this.depreciation = depreciation;
    }

    public BigDecimal getCumulativeDepreciation() {
        return cumulativeDepreciation;
    }

    public void setCumulativeDepreciation(BigDecimal cumulativeDepreciation) {
        this.cumulativeDepreciation = cumulativeDepreciation;
    }

    public BigDecimal getBookValue() {
        return bookValue;
    }

    public void setBookValue(BigDecimal bookValue) {
        this.bookValue = bookValue;
    }

    @Override
    public String toString() {
        return String.format("Year %d: Depreciation=%.2f, Cumulative=%.2f, Book Value=%.2f",
                year, depreciation, cumulativeDepreciation, bookValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DepreciationYear that = (DepreciationYear) o;

        if (year != that.year) return false;
        if (depreciation != null ? !depreciation.equals(that.depreciation) : that.depreciation != null) return false;
        if (cumulativeDepreciation != null ? !cumulativeDepreciation.equals(that.cumulativeDepreciation) : that.cumulativeDepreciation != null)
            return false;
        return bookValue != null ? bookValue.equals(that.bookValue) : that.bookValue == null;
    }

    @Override
    public int hashCode() {
        int result = year;
        result = 31 * result + (depreciation != null ? depreciation.hashCode() : 0);
        result = 31 * result + (cumulativeDepreciation != null ? cumulativeDepreciation.hashCode() : 0);
        result = 31 * result + (bookValue != null ? bookValue.hashCode() : 0);
        return result;
    }
}