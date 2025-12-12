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
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fin.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA entity representing a depreciation schedule for assets.
 */
@Entity
@Table(name = "depreciation_schedules")
@SuppressWarnings("DesignForExtension")
public class DepreciationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private Long assetId;

    @Column(name = "schedule_number", nullable = false)
    private String scheduleNumber;

    @Column(name = "schedule_name")
    private String scheduleName;

    @Column(name = "description")
    private String description;

    @Column(name = "cost", precision = 15, scale = 2, nullable = false)
    private BigDecimal cost;

    @Column(name = "salvage_value", precision = 15, scale = 2)
    private BigDecimal salvageValue = BigDecimal.ZERO;

    @Column(name = "useful_life_years", nullable = false)
    private Integer usefulLifeYears;

    @Column(name = "depreciation_method")
    private String depreciationMethod;

    @Column(name = "db_factor", precision = 5, scale = 2)
    private BigDecimal dbFactor;

    @Column(name = "convention")
    private String convention;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "accumulated_depreciation", precision = 15, scale = 2)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Column(name = "disposal_date")
    private java.time.LocalDate disposalDate;

    @Column(name = "disposal_proceeds", precision = 15, scale = 2)
    private BigDecimal disposalProceeds;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public DepreciationSchedule() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DepreciationSchedule(Long assetId, String scheduleNumber, BigDecimal cost,
                              Integer usefulLifeYears, Long companyId) {
        this();
        this.assetId = assetId;
        this.scheduleNumber = scheduleNumber;
        this.cost = cost;
        this.usefulLifeYears = usefulLifeYears;
        this.companyId = companyId;
    }

    // Getters and Setters
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

    public Integer getUsefulLifeYears() {
        return usefulLifeYears;
    }

    public void setUsefulLifeYears(Integer usefulLifeYears) {
        this.usefulLifeYears = usefulLifeYears;
    }

    public String getDepreciationMethod() {
        return depreciationMethod;
    }

    public void setDepreciationMethod(String depreciationMethod) {
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

    public BigDecimal getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(BigDecimal value) {
        this.accumulatedDepreciation = value;
    }

    public java.time.LocalDate getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(java.time.LocalDate date) {
        this.disposalDate = date;
    }

    public BigDecimal getDisposalProceeds() {
        return disposalProceeds;
    }

    public void setDisposalProceeds(BigDecimal proceeds) {
        this.disposalProceeds = proceeds;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyIdValue) {
        this.companyId = companyIdValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAtValue) {
        this.createdAt = createdAtValue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAtValue) {
        this.updatedAt = updatedAtValue;
    }

    public BigDecimal getAssetCost() {
        return cost;
    }

    public void setAssetCost(BigDecimal assetCostValue) {
        this.cost = assetCostValue;
    }

    public BigDecimal getResidualValue() {
        return salvageValue;
    }

    public void setResidualValue(BigDecimal residualValueAmount) {
        this.salvageValue = residualValueAmount;
    }

    public String getAssetName() {
        return scheduleName;
    }

    public void setAssetName(String assetNameValue) {
        this.scheduleName = assetNameValue;
    }

    public Long getAccountId() {
        return assetId;
    }

    public void setAccountId(Long accountIdValue) {
        this.assetId = accountIdValue;
    }

    public Long getAccumulatedDepreciationAccountId() {
        // TODO: Add accumulated depreciation account ID field
        return null;
    }

    public void setAccumulatedDepreciationAccountId(Long accumulatedDepreciationAccountId) {
        // TODO: Add accumulated depreciation account ID field
    }

    public java.time.LocalDate getStartDate() {
        // TODO: Add start date field
        return null;
    }

    public void setStartDate(java.time.LocalDate startDate) {
        // TODO: Add start date field
    }

    // Utility methods
    public BigDecimal getDepreciableAmount() {
        return cost.subtract(salvageValue);
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public void setIsActive(boolean active) {
        this.status = active ? "ACTIVE" : "INACTIVE";
    }

    @Override
    public String toString() {
        return String.format("DepreciationSchedule{id=%d, scheduleNumber='%s', cost=%s}",
                id, scheduleNumber, cost);
    }
}