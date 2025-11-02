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
 * Budget Monthly Allocation model representing monthly budget allocations and actual spending
 */
public class BudgetMonthlyAllocation {
    private Long id;
    private Long budgetItemId;
    private Integer monthNumber; // 1=Jan, 2=Feb, etc.
    private BigDecimal allocatedAmount;
    private BigDecimal actualAmount;
    private BigDecimal varianceAmount; // Calculated: actual - allocated
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public BudgetMonthlyAllocation() {}

    public BudgetMonthlyAllocation(Long budgetItemId, Integer monthNumber, BigDecimal allocatedAmount) {
        this.budgetItemId = budgetItemId;
        this.monthNumber = monthNumber;
        this.allocatedAmount = allocatedAmount;
        this.actualAmount = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBudgetItemId() { return budgetItemId; }
    public void setBudgetItemId(Long budgetItemId) { this.budgetItemId = budgetItemId; }

    public Integer getMonthNumber() { return monthNumber; }
    public void setMonthNumber(Integer monthNumber) { this.monthNumber = monthNumber; }

    public BigDecimal getAllocatedAmount() { return allocatedAmount; }
    public void setAllocatedAmount(BigDecimal allocatedAmount) { this.allocatedAmount = allocatedAmount; }

    public BigDecimal getActualAmount() { return actualAmount; }
    public void setActualAmount(BigDecimal actualAmount) { this.actualAmount = actualAmount; }

    public BigDecimal getVarianceAmount() { return varianceAmount; }
    public void setVarianceAmount(BigDecimal varianceAmount) { this.varianceAmount = varianceAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Helper methods
    public String getMonthName() {
        String[] months = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        return monthNumber != null && monthNumber >= 1 && monthNumber <= 12 ?
               months[monthNumber - 1] : "Unknown";
    }

    public boolean isOverBudget() {
        return actualAmount != null && allocatedAmount != null &&
               actualAmount.compareTo(allocatedAmount) > 0;
    }

    public boolean isUnderBudget() {
        return actualAmount != null && allocatedAmount != null &&
               actualAmount.compareTo(allocatedAmount) < 0;
    }

    @Override
    public String toString() {
        return String.format("BudgetMonthlyAllocation{id=%d, month=%s, allocated=%s, actual=%s, variance=%s}",
                           id, getMonthName(), allocatedAmount, actualAmount, varianceAmount);
    }
}