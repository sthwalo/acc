package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a fixed asset for depreciation tracking
 */
public final class Asset {
    // Magic number constants
    private static final int HASH_PRIME = 31;

    private Long id;
    private Long companyId;
    private String assetCode;
    private String assetName;
    private String description;
    private String assetCategory;
    private LocalDate acquisitionDate;
    private BigDecimal cost;
    private BigDecimal salvageValue;
    private int usefulLifeYears;
    private String location;
    private String department;
    private String status;
    private BigDecimal accumulatedDepreciation;
    private BigDecimal currentBookValue;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Asset() {
        this.status = "ACTIVE";
        this.accumulatedDepreciation = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getAssetCode() {
        return assetCode;
    }

    public void setAssetCode(String assetCode) {
        this.assetCode = assetCode;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssetCategory() {
        return assetCategory;
    }

    public void setAssetCategory(String assetCategoryParam) {
        this.assetCategory = assetCategoryParam;
    }

    public LocalDate getAcquisitionDate() {
        return acquisitionDate;
    }

    public void setAcquisitionDate(LocalDate acquisitionDateParam) {
        this.acquisitionDate = acquisitionDateParam;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal costParam) {
        this.cost = costParam;
        updateBookValue();
    }

    public BigDecimal getSalvageValue() {
        return salvageValue;
    }

    public void setSalvageValue(BigDecimal salvageValueParam) {
        this.salvageValue = salvageValueParam;
        updateBookValue();
    }

    public int getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(int usefulLifeYearsParam) {
        this.usefulLifeYears = usefulLifeYearsParam;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String locationParam) {
        this.location = locationParam;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String departmentParam) {
        this.department = departmentParam;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String statusParam) {
        this.status = statusParam;
    }

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal accumulatedDepreciationParam) {
        this.accumulatedDepreciation = accumulatedDepreciationParam;
        updateBookValue();
    }

    public BigDecimal getCurrentBookValue() {
        return currentBookValue;
    }

    public void setCurrentBookValue(BigDecimal currentBookValueParam) {
        this.currentBookValue = currentBookValueParam;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdByParam) {
        this.createdBy = createdByParam;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAtParam) {
        this.createdAt = createdAtParam;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAtParam) {
        this.updatedAt = updatedAtParam;
    }

    /**
     * Calculate the depreciable amount (cost - salvage value)
     */
    public BigDecimal getDepreciableAmount() {
        if (cost != null && salvageValue != null) {
            return cost.subtract(salvageValue);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Check if asset is fully depreciated
     */
    public boolean isFullyDepreciated() {
        return currentBookValue != null && currentBookValue.compareTo(salvageValue != null ? salvageValue : BigDecimal.ZERO) <= 0;
    }

    /**
     * Update book value based on cost and accumulated depreciation
     */
    private void updateBookValue() {
        if (cost != null && accumulatedDepreciation != null) {
            this.currentBookValue = cost.subtract(accumulatedDepreciation);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Asset: ").append(assetCode).append(" - ").append(assetName).append("\n");
        sb.append("Category: ").append(assetCategory).append("\n");
        sb.append("Cost: ").append(cost).append("\n");
        sb.append("Salvage Value: ").append(salvageValue).append("\n");
        sb.append("Accumulated Depreciation: ").append(accumulatedDepreciation).append("\n");
        sb.append("Current Book Value: ").append(currentBookValue).append("\n");
        sb.append("Useful Life: ").append(usefulLifeYears).append(" years\n");
        sb.append("Location: ").append(location).append("\n");
        sb.append("Department: ").append(department).append("\n");
        sb.append("Status: ").append(status);
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Asset asset = (Asset) o;

        if (usefulLifeYears != asset.usefulLifeYears) {
            return false;
        }
        if (id != null ? !id.equals(asset.id) : asset.id != null) {
            return false;
        }
        if (companyId != null ? !companyId.equals(asset.companyId) : asset.companyId != null) {
            return false;
        }
        if (assetCode != null ? !assetCode.equals(asset.assetCode) : asset.assetCode != null) {
            return false;
        }
        if (assetName != null ? !assetName.equals(asset.assetName) : asset.assetName != null) {
            return false;
        }
        if (description != null ? !description.equals(asset.description) : asset.description != null) {
            return false;
        }
        if (assetCategory != null ? !assetCategory.equals(asset.assetCategory) : asset.assetCategory != null) {
            return false;
        }
        if (acquisitionDate != null ? !acquisitionDate.equals(asset.acquisitionDate) : asset.acquisitionDate != null) {
            return false;
        }
        if (cost != null ? !cost.equals(asset.cost) : asset.cost != null) {
            return false;
        }
        if (salvageValue != null ? !salvageValue.equals(asset.salvageValue) : asset.salvageValue != null) {
            return false;
        }
        if (location != null ? !location.equals(asset.location) : asset.location != null) {
            return false;
        }
        if (department != null ? !department.equals(asset.department) : asset.department != null) {
            return false;
        }
        if (status != null ? !status.equals(asset.status) : asset.status != null) {
            return false;
        }
        if (accumulatedDepreciation != null ? !accumulatedDepreciation.equals(asset.accumulatedDepreciation) : asset.accumulatedDepreciation != null) {
            return false;
        }
        if (currentBookValue != null ? !currentBookValue.equals(asset.currentBookValue) : asset.currentBookValue != null) {
            return false;
        }
        if (createdBy != null ? !createdBy.equals(asset.createdBy) : asset.createdBy != null) {
            return false;
        }
        if (createdAt != null ? !createdAt.equals(asset.createdAt) : asset.createdAt != null) {
            return false;
        }
        return updatedAt != null ? updatedAt.equals(asset.updatedAt) : asset.updatedAt == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = HASH_PRIME * result + (companyId != null ? companyId.hashCode() : 0);
        result = HASH_PRIME * result + (assetCode != null ? assetCode.hashCode() : 0);
        result = HASH_PRIME * result + (assetName != null ? assetName.hashCode() : 0);
        result = HASH_PRIME * result + (description != null ? description.hashCode() : 0);
        result = HASH_PRIME * result + (assetCategory != null ? assetCategory.hashCode() : 0);
        result = HASH_PRIME * result + (acquisitionDate != null ? acquisitionDate.hashCode() : 0);
        result = HASH_PRIME * result + (cost != null ? cost.hashCode() : 0);
        result = HASH_PRIME * result + (salvageValue != null ? salvageValue.hashCode() : 0);
        result = HASH_PRIME * result + usefulLifeYears;
        result = HASH_PRIME * result + (location != null ? location.hashCode() : 0);
        result = HASH_PRIME * result + (department != null ? department.hashCode() : 0);
        result = HASH_PRIME * result + (status != null ? status.hashCode() : 0);
        result = HASH_PRIME * result + (accumulatedDepreciation != null ? accumulatedDepreciation.hashCode() : 0);
        result = HASH_PRIME * result + (currentBookValue != null ? currentBookValue.hashCode() : 0);
        result = HASH_PRIME * result + (createdBy != null ? createdBy.hashCode() : 0);
        result = HASH_PRIME * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = HASH_PRIME * result + (updatedAt != null ? updatedAt.hashCode() : 0);
        return result;
    }
}