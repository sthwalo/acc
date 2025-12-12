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

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for Audit Trail API response.
 * Contains paginated journal entries with pagination and filter metadata.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
public class AuditTrailResponse {
    
    private List<JournalEntryDTO> entries;
    private PaginationMetadata pagination;
    private FilterMetadata filters;

    /**
     * Default constructor for JSON deserialization
     */
    public AuditTrailResponse() {
        this.entries = new ArrayList<>();
    }

    /**
     * Full constructor for creating audit trail response
     */
    public AuditTrailResponse(List<JournalEntryDTO> entries, PaginationMetadata pagination, FilterMetadata filters) {
        this.entries = entries != null ? entries : new ArrayList<>();
        this.pagination = pagination;
        this.filters = filters;
    }

    /**
     * Constructor for empty result set
     */
    public AuditTrailResponse(PaginationMetadata pagination, FilterMetadata filters) {
        this.entries = new ArrayList<>();
        this.pagination = pagination;
        this.filters = filters;
    }

    // Getters and Setters

    public List<JournalEntryDTO> getEntries() {
        return entries;
    }

    public void setEntries(List<JournalEntryDTO> entries) {
        this.entries = entries != null ? entries : new ArrayList<>();
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }

    public FilterMetadata getFilters() {
        return filters;
    }

    public void setFilters(FilterMetadata filters) {
        this.filters = filters;
    }

    /**
     * Check if response contains any entries
     */
    public boolean isEmpty() {
        return entries == null || entries.isEmpty();
    }

    /**
     * Get the number of entries in this response
     */
    public int getEntryCount() {
        return entries != null ? entries.size() : 0;
    }

    @Override
    public String toString() {
        return "AuditTrailResponse{" +
                "entryCount=" + getEntryCount() +
                ", pagination=" + pagination +
                ", filters=" + filters +
                ", isEmpty=" + isEmpty() +
                '}';
    }
}
