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

package fin.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for filter metadata.
 * Contains information about active filters applied to the result set.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
public class FilterMetadata {
    
    private LocalDate startDate;
    private LocalDate endDate;
    private String transactionType;
    private String searchTerm;

    /**
     * Default constructor for JSON deserialization
     */
    public FilterMetadata() {
    }

    /**
     * Full constructor for creating filter metadata
     */
    public FilterMetadata(LocalDate startDate, LocalDate endDate, String transactionType, String searchTerm) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.transactionType = transactionType;
        this.searchTerm = searchTerm;
    }

    // Getters and Setters

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    /**
     * Check if any filters are active
     */
    public boolean hasActiveFilters() {
        return startDate != null || endDate != null || 
               (transactionType != null && !transactionType.isEmpty()) ||
               (searchTerm != null && !searchTerm.isEmpty());
    }

    @Override
    public String toString() {
        return "FilterMetadata{" +
                "startDate=" + startDate +
                ", endDate=" + endDate +
                ", transactionType='" + transactionType + '\'' +
                ", searchTerm='" + searchTerm + '\'' +
                ", hasActiveFilters=" + hasActiveFilters() +
                '}';
    }
}
