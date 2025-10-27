package fin.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a complete depreciation schedule with all years
 */
public class DepreciationSchedule {
    // Database fields
    private Long id;
    private Long assetId;
    private String scheduleNumber;
    private String scheduleName;
    private String description;
    private BigDecimal cost;
    private BigDecimal salvageValue;
    private int usefulLifeYears;
    private DepreciationMethod depreciationMethod;
    private BigDecimal dbFactor;
    private String convention;
    private String status;
    private String createdBy;
    private LocalDateTime calculationDate;

    // Calculated fields
    private List<DepreciationYear> years;
    private BigDecimal totalDepreciation;
    private BigDecimal finalBookValue;

    public DepreciationSchedule() {
        this.years = new ArrayList<>();
        this.totalDepreciation = BigDecimal.ZERO;
        this.finalBookValue = BigDecimal.ZERO;
        this.dbFactor = BigDecimal.valueOf(2.0); // Default for declining balance
        this.convention = "HALF_YEAR"; // Default convention
        this.status = "CALCULATED";
        this.calculationDate = LocalDateTime.now();
    }

    public DepreciationSchedule(List<DepreciationYear> years) {
        this();
        this.years = years != null ? new ArrayList<>(years) : new ArrayList<>();
        calculateTotals();
    }

    private void calculateTotals() {
        if (years != null && !years.isEmpty()) {
            BigDecimal total = BigDecimal.ZERO;
            for (DepreciationYear year : years) {
                if (year.getDepreciation() != null) {
                    total = total.add(year.getDepreciation());
                }
            }
            this.totalDepreciation = total;

            DepreciationYear lastYear = years.get(years.size() - 1);
            this.finalBookValue = lastYear.getBookValue();
        } else {
            this.totalDepreciation = BigDecimal.ZERO;
            this.finalBookValue = BigDecimal.ZERO;
        }
    }

    // Database field getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getScheduleNumber() {
        return scheduleNumber;
    }

    public void setScheduleNumber(String scheduleNumber) {
        this.scheduleNumber = scheduleNumber;
    }

    public String getScheduleName() {
        return scheduleName;
    }

    public void setScheduleName(String scheduleName) {
        this.scheduleName = scheduleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public BigDecimal getSalvageValue() {
        return salvageValue;
    }

    public void setSalvageValue(BigDecimal salvageValue) {
        this.salvageValue = salvageValue;
    }

    public int getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(int usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }

    public DepreciationMethod getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(DepreciationMethod depreciationMethod) {
        this.depreciationMethod = depreciationMethod;
    }

    public BigDecimal getDbFactor() {
        return dbFactor;
    }

    public void setDbFactor(BigDecimal dbFactor) {
        this.dbFactor = dbFactor;
    }

    public String getConvention() {
        return convention;
    }

    public void setConvention(String convention) {
        this.convention = convention;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCalculationDate() {
        return calculationDate;
    }

    public void setCalculationDate(LocalDateTime calculationDate) {
        this.calculationDate = calculationDate;
    }

    // Calculated field getters
    public List<DepreciationYear> getYears() {
        return years != null ? Collections.unmodifiableList(years) : Collections.emptyList();
    }

    public void setYears(List<DepreciationYear> years) {
        this.years = years != null ? new ArrayList<>(years) : new ArrayList<>();
        calculateTotals();
    }

    public BigDecimal getTotalDepreciation() {
        return totalDepreciation;
    }

    public void setTotalDepreciation(BigDecimal totalDepreciation) {
        this.totalDepreciation = totalDepreciation;
    }

    public BigDecimal getFinalBookValue() {
        return finalBookValue;
    }

    public void setFinalBookValue(BigDecimal finalBookValue) {
        this.finalBookValue = finalBookValue;
    }

    public int getUsefulLife() {
        return years != null ? years.size() : 0;
    }

    public void addYear(DepreciationYear year) {
        if (years == null) {
            years = new ArrayList<>();
        }
        years.add(year);
        calculateTotals();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Depreciation Schedule:\n");
        if (scheduleNumber != null) {
            sb.append("Schedule Number: ").append(scheduleNumber).append("\n");
        }
        if (scheduleName != null) {
            sb.append("Schedule Name: ").append(scheduleName).append("\n");
        }
        sb.append("Cost: ").append(cost).append("\n");
        sb.append("Salvage Value: ").append(salvageValue).append("\n");
        sb.append("Useful Life: ").append(usefulLifeYears).append(" years\n");
        sb.append("Method: ").append(depreciationMethod).append("\n");
        sb.append("Total Depreciation: ").append(totalDepreciation).append("\n");
        sb.append("Final Book Value: ").append(finalBookValue).append("\n");
        sb.append("Yearly Breakdown:\n");

        if (years != null) {
            for (DepreciationYear year : years) {
                sb.append("  ").append(year.toString()).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DepreciationSchedule that = (DepreciationSchedule) o;

        if (usefulLifeYears != that.usefulLifeYears) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (assetId != null ? !assetId.equals(that.assetId) : that.assetId != null) return false;
        if (scheduleNumber != null ? !scheduleNumber.equals(that.scheduleNumber) : that.scheduleNumber != null) return false;
        if (scheduleName != null ? !scheduleName.equals(that.scheduleName) : that.scheduleName != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (cost != null ? !cost.equals(that.cost) : that.cost != null) return false;
        if (salvageValue != null ? !salvageValue.equals(that.salvageValue) : that.salvageValue != null) return false;
        if (depreciationMethod != that.depreciationMethod) return false;
        if (dbFactor != null ? !dbFactor.equals(that.dbFactor) : that.dbFactor != null) return false;
        if (convention != null ? !convention.equals(that.convention) : that.convention != null) return false;
        if (status != null ? !status.equals(that.status) : that.status != null) return false;
        if (createdBy != null ? !createdBy.equals(that.createdBy) : that.createdBy != null) return false;
        if (calculationDate != null ? !calculationDate.equals(that.calculationDate) : that.calculationDate != null) return false;
        if (years != null ? !years.equals(that.years) : that.years != null) return false;
        if (totalDepreciation != null ? !totalDepreciation.equals(that.totalDepreciation) : that.totalDepreciation != null)
            return false;
        return finalBookValue != null ? finalBookValue.equals(that.finalBookValue) : that.finalBookValue == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (assetId != null ? assetId.hashCode() : 0);
        result = 31 * result + (scheduleNumber != null ? scheduleNumber.hashCode() : 0);
        result = 31 * result + (scheduleName != null ? scheduleName.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (cost != null ? cost.hashCode() : 0);
        result = 31 * result + (salvageValue != null ? salvageValue.hashCode() : 0);
        result = 31 * result + usefulLifeYears;
        result = 31 * result + (depreciationMethod != null ? depreciationMethod.hashCode() : 0);
        result = 31 * result + (dbFactor != null ? dbFactor.hashCode() : 0);
        result = 31 * result + (convention != null ? convention.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = 31 * result + (calculationDate != null ? calculationDate.hashCode() : 0);
        result = 31 * result + (years != null ? years.hashCode() : 0);
        result = 31 * result + (totalDepreciation != null ? totalDepreciation.hashCode() : 0);
        result = 31 * result + (finalBookValue != null ? finalBookValue.hashCode() : 0);
        return result;
    }
}