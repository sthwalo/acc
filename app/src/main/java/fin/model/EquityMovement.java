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
import java.time.LocalDateTime;

/**
 * Equity Movement Model
 * Represents movements in equity components for Statement of Changes in Equity
 */
public class EquityMovement {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String equityComponent; // share_capital, retained_earnings, reserves, etc.
    private BigDecimal openingBalance;
    private BigDecimal profitLoss;
    private BigDecimal dividends;
    private BigDecimal shareIssues;
    private BigDecimal shareBuybacks;
    private BigDecimal transfersToReserves;
    private BigDecimal transfersFromReserves;
    private BigDecimal otherMovements;
    private String otherMovementsDescription;
    private BigDecimal closingBalance;
    private int displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public EquityMovement() {
        this.openingBalance = BigDecimal.ZERO;
        this.profitLoss = BigDecimal.ZERO;
        this.dividends = BigDecimal.ZERO;
        this.shareIssues = BigDecimal.ZERO;
        this.shareBuybacks = BigDecimal.ZERO;
        this.transfersToReserves = BigDecimal.ZERO;
        this.transfersFromReserves = BigDecimal.ZERO;
        this.otherMovements = BigDecimal.ZERO;
        this.closingBalance = BigDecimal.ZERO;
    }
    
    public EquityMovement(Long companyId, Long fiscalPeriodId, String equityComponent) {
        this();
        this.companyId = companyId;
        this.fiscalPeriodId = fiscalPeriodId;
        this.equityComponent = equityComponent;
    }
    
    /**
     * Calculate closing balance based on movements
     */
    public void calculateClosingBalance() {
        this.closingBalance = openingBalance
            .add(profitLoss)
            .subtract(dividends)
            .add(shareIssues)
            .subtract(shareBuybacks)
            .subtract(transfersToReserves)
            .add(transfersFromReserves)
            .add(otherMovements);
    }
    
    // Getters and Setters
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
    
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }
    
    public void setFiscalPeriodId(Long fiscalPeriodId) {
        this.fiscalPeriodId = fiscalPeriodId;
    }
    
    public String getEquityComponent() {
        return equityComponent;
    }
    
    public void setEquityComponent(String equityComponent) {
        this.equityComponent = equityComponent;
    }
    
    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }
    
    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }
    
    public BigDecimal getProfitLoss() {
        return profitLoss;
    }
    
    public void setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
    }
    
    public BigDecimal getDividends() {
        return dividends;
    }
    
    public void setDividends(BigDecimal dividends) {
        this.dividends = dividends;
    }
    
    public BigDecimal getShareIssues() {
        return shareIssues;
    }
    
    public void setShareIssues(BigDecimal shareIssues) {
        this.shareIssues = shareIssues;
    }
    
    public BigDecimal getShareBuybacks() {
        return shareBuybacks;
    }
    
    public void setShareBuybacks(BigDecimal shareBuybacks) {
        this.shareBuybacks = shareBuybacks;
    }
    
    public BigDecimal getTransfersToReserves() {
        return transfersToReserves;
    }
    
    public void setTransfersToReserves(BigDecimal transfersToReserves) {
        this.transfersToReserves = transfersToReserves;
    }
    
    public BigDecimal getTransfersFromReserves() {
        return transfersFromReserves;
    }
    
    public void setTransfersFromReserves(BigDecimal transfersFromReserves) {
        this.transfersFromReserves = transfersFromReserves;
    }
    
    public BigDecimal getOtherMovements() {
        return otherMovements;
    }
    
    public void setOtherMovements(BigDecimal otherMovements) {
        this.otherMovements = otherMovements;
    }
    
    public String getOtherMovementsDescription() {
        return otherMovementsDescription;
    }
    
    public void setOtherMovementsDescription(String otherMovementsDescription) {
        this.otherMovementsDescription = otherMovementsDescription;
    }
    
    public BigDecimal getClosingBalance() {
        return closingBalance;
    }
    
    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }
    
    public int getDisplayOrder() {
        return displayOrder;
    }
    
    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
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
}
