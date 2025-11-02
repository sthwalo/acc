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

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FiscalPeriod {
    private Long id;
    private Long companyId;
    private String periodName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isClosed;
    private LocalDateTime createdAt;
    
    // Constructors, getters, and setters
    public FiscalPeriod() {}
    
    public FiscalPeriod(Long initialCompanyId, String initialPeriodName, LocalDate initialStartDate, LocalDate initialEndDate) {
        this.companyId = initialCompanyId;
        this.periodName = initialPeriodName;
        this.startDate = initialStartDate;
        this.endDate = initialEndDate;
        this.isClosed = false;
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Copy constructor for defensive copying.
     * Creates a deep copy of all FiscalPeriod fields to prevent external modification.
     */
    public FiscalPeriod(FiscalPeriod other) {
        if (other == null) {
            return;
        }
        
        this.id = other.id;
        this.companyId = other.companyId;
        this.periodName = other.periodName;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.isClosed = other.isClosed;
        this.createdAt = other.createdAt;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }
    
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }
    
    public String getPeriodName() { return periodName; }
    public void setPeriodName(String newPeriodName) { this.periodName = newPeriodName; }
    
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate newStartDate) { this.startDate = newStartDate; }
    
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate newEndDate) { this.endDate = newEndDate; }
    
    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean closed) { isClosed = closed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }
    
    @Override
    public String toString() {
        return "FiscalPeriod{" +
                "id=" + id +
                ", companyId=" + companyId +
                ", periodName='" + periodName + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isClosed=" + isClosed +
                '}';
    }
}
