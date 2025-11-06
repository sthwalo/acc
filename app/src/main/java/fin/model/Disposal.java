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

package fin.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents the disposal of a depreciable asset
 * Implements section 11(o) allowance calculations per SARS guidelines
 */
public final class Disposal {
    private Long id;
    private Long assetId;
    private Long companyId;
    private LocalDate disposalDate;
    private String disposalType; // SALE, SCRAP, THEFT, DONATION, etc.
    private BigDecimal proceedsReceived;
    private BigDecimal taxValue; // Cost - Accumulated Depreciation
    private BigDecimal lossOnDisposal; // Tax value - proceeds (section 11(o) allowance)
    private BigDecimal gainOnDisposal; // Proceeds - tax value (recoupment)
    private String reference;
    private String description;
    private Long journalEntryId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Disposal() {
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

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public LocalDate getDisposalDate() {
        return disposalDate;
    }

    public void setDisposalDate(LocalDate disposalDate) {
        this.disposalDate = disposalDate;
    }

    public String getDisposalType() {
        return disposalType;
    }

    public void setDisposalType(String disposalType) {
        this.disposalType = disposalType;
    }

    public BigDecimal getProceedsReceived() {
        return proceedsReceived;
    }

    public void setProceedsReceived(BigDecimal proceedsReceived) {
        this.proceedsReceived = proceedsReceived;
        calculateLossOrGain();
    }

    public BigDecimal getTaxValue() {
        return taxValue;
    }

    public void setTaxValue(BigDecimal taxValue) {
        this.taxValue = taxValue;
        calculateLossOrGain();
    }

    public BigDecimal getLossOnDisposal() {
        return lossOnDisposal;
    }

    public void setLossOnDisposal(BigDecimal lossOnDisposal) {
        this.lossOnDisposal = lossOnDisposal;
    }

    public BigDecimal getGainOnDisposal() {
        return gainOnDisposal;
    }

    public void setGainOnDisposal(BigDecimal gainOnDisposal) {
        this.gainOnDisposal = gainOnDisposal;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getJournalEntryId() {
        return journalEntryId;
    }

    public void setJournalEntryId(Long journalEntryId) {
        this.journalEntryId = journalEntryId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calculate loss or gain on disposal per section 11(o)
     * Loss = Tax Value - Proceeds Received
     * Gain = Proceeds Received - Tax Value
     */
    private void calculateLossOrGain() {
        if (taxValue != null && proceedsReceived != null) {
            BigDecimal difference = taxValue.subtract(proceedsReceived);
            if (difference.compareTo(BigDecimal.ZERO) > 0) {
                // Loss on disposal (section 11(o) allowance)
                this.lossOnDisposal = difference;
                this.gainOnDisposal = BigDecimal.ZERO;
            } else if (difference.compareTo(BigDecimal.ZERO) < 0) {
                // Gain on disposal (recoupment under section 8(4)(a))
                this.gainOnDisposal = difference.negate();
                this.lossOnDisposal = BigDecimal.ZERO;
            } else {
                // No loss or gain
                this.lossOnDisposal = BigDecimal.ZERO;
                this.gainOnDisposal = BigDecimal.ZERO;
            }
        }
    }

    /**
     * Check if this disposal results in a loss (section 11(o) allowance)
     */
    public boolean hasLoss() {
        return lossOnDisposal != null && lossOnDisposal.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Check if this disposal results in a gain (recoupment)
     */
    public boolean hasGain() {
        return gainOnDisposal != null && gainOnDisposal.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Disposal: ").append(reference).append("\n");
        sb.append("Asset ID: ").append(assetId).append("\n");
        sb.append("Disposal Date: ").append(disposalDate).append("\n");
        sb.append("Type: ").append(disposalType).append("\n");
        sb.append("Proceeds: ").append(proceedsReceived).append("\n");
        sb.append("Tax Value: ").append(taxValue).append("\n");
        if (hasLoss()) {
            sb.append("Loss on Disposal (Section 11(o)): ").append(lossOnDisposal).append("\n");
        }
        if (hasGain()) {
            sb.append("Gain on Disposal (Recoupment): ").append(gainOnDisposal).append("\n");
        }
        sb.append("Description: ").append(description);
        return sb.toString();
    }
}