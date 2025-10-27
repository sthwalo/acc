package fin.model;

import java.math.BigDecimal;

/**
 * Request object for depreciation calculations
 */
public class DepreciationRequest {
    private BigDecimal cost;
    private BigDecimal salvageValue;
    private int usefulLife;
    private DepreciationMethod method;
    private BigDecimal dbFactor; // For declining balance method (typically 2.0 for double declining)
    private String convention; // "Half-Year", "Mid-Quarter", etc.

    public DepreciationRequest() {
        // Default constructor
    }

    public DepreciationRequest(BigDecimal cost, BigDecimal salvageValue, int usefulLife,
                             DepreciationMethod method, BigDecimal dbFactor, String convention) {
        this.cost = cost;
        this.salvageValue = salvageValue;
        this.usefulLife = usefulLife;
        this.method = method;
        this.dbFactor = dbFactor;
        this.convention = convention;
    }

    public static class Builder {
        private BigDecimal cost;
        private BigDecimal salvageValue;
        private Integer usefulLife;
        private DepreciationMethod method;
        private BigDecimal dbFactor = new BigDecimal("1.0");
        private String convention;

        public Builder cost(BigDecimal cost) {
            this.cost = cost;
            return this;
        }

        public Builder salvageValue(BigDecimal salvageValue) {
            this.salvageValue = salvageValue;
            return this;
        }

        public Builder usefulLife(int usefulLife) {
            this.usefulLife = usefulLife;
            return this;
        }

        public Builder method(DepreciationMethod method) {
            this.method = method;
            return this;
        }

        public Builder dbFactor(BigDecimal dbFactor) {
            this.dbFactor = dbFactor;
            return this;
        }

        public Builder convention(String convention) {
            this.convention = convention;
            return this;
        }

        public DepreciationRequest build() {
            validate();
            return new DepreciationRequest(cost, salvageValue, usefulLife, method, dbFactor, convention);
        }

        private void validate() {
            if (cost == null) {
                throw new IllegalArgumentException("Cost is required");
            }
            if (salvageValue == null) {
                throw new IllegalArgumentException("Salvage value is required");
            }
            if (usefulLife == null) {
                throw new IllegalArgumentException("Useful life is required");
            }
            if (method == null) {
                throw new IllegalArgumentException("Depreciation method is required");
            }
            if (cost.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Cost must be positive");
            }
            if (salvageValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Salvage value cannot be negative");
            }
            if (salvageValue.compareTo(cost) > 0) {
                throw new IllegalArgumentException("Salvage value cannot be greater than cost");
            }
            if (usefulLife <= 0) {
                throw new IllegalArgumentException("Useful life must be positive");
            }
            if (usefulLife > 50) {
                throw new IllegalArgumentException("Useful life seems unreasonably long (max 50 years)");
            }
            if (dbFactor != null && dbFactor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Declining balance factor must be positive");
            }
        }
    }

    // Getters and setters
    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public DepreciationRequest cost(BigDecimal cost) {
        this.cost = cost;
        return this;
    }

    public BigDecimal getSalvageValue() {
        return salvageValue;
    }

    public void setSalvageValue(BigDecimal salvageValue) {
        this.salvageValue = salvageValue;
    }

    public DepreciationRequest salvageValue(BigDecimal salvageValue) {
        this.salvageValue = salvageValue;
        return this;
    }

    public int getUsefulLife() {
        return usefulLife;
    }

    public void setUsefulLife(int usefulLife) {
        this.usefulLife = usefulLife;
    }

    public DepreciationRequest usefulLife(int usefulLife) {
        this.usefulLife = usefulLife;
        return this;
    }

    public DepreciationMethod getMethod() {
        return method;
    }

    public void setMethod(DepreciationMethod method) {
        this.method = method;
    }

    public DepreciationRequest method(DepreciationMethod method) {
        this.method = method;
        return this;
    }

    public BigDecimal getDbFactor() {
        return dbFactor;
    }

    public void setDbFactor(BigDecimal dbFactor) {
        this.dbFactor = dbFactor;
    }

    public DepreciationRequest dbFactor(BigDecimal dbFactor) {
        this.dbFactor = dbFactor;
        return this;
    }

    public String getConvention() {
        return convention;
    }

    public void setConvention(String convention) {
        this.convention = convention;
    }

    public DepreciationRequest convention(String convention) {
        this.convention = convention;
        return this;
    }

    @Override
    public String toString() {
        return "DepreciationRequest{" +
                "cost=" + cost +
                ", salvageValue=" + salvageValue +
                ", usefulLife=" + usefulLife +
                ", method=" + method +
                ", dbFactor=" + dbFactor +
                ", convention='" + convention + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DepreciationRequest that = (DepreciationRequest) o;

        if (usefulLife != that.usefulLife) return false;
        if (cost != null ? !cost.equals(that.cost) : that.cost != null) return false;
        if (salvageValue != null ? !salvageValue.equals(that.salvageValue) : that.salvageValue != null) return false;
        if (method != that.method) return false;
        if (dbFactor != null ? !dbFactor.equals(that.dbFactor) : that.dbFactor != null) return false;
        return convention != null ? convention.equals(that.convention) : that.convention == null;
    }

    @Override
    public int hashCode() {
        int result = cost != null ? cost.hashCode() : 0;
        result = 31 * result + (salvageValue != null ? salvageValue.hashCode() : 0);
        result = 31 * result + usefulLife;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (dbFactor != null ? dbFactor.hashCode() : 0);
        result = 31 * result + (convention != null ? convention.hashCode() : 0);
        return result;
    }
}