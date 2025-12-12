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

/**
 * Data Transfer Object for pagination metadata.
 * Contains information about current page, page size, and total results.
 * 
 * Part of TASK_007: Reports View API Integration & Audit Trail Enhancement
 */
public class PaginationMetadata {
    
    private int currentPage;
    private int pageSize;
    private long totalEntries;
    private int totalPages;

    /**
     * Default constructor for JSON deserialization
     */
    public PaginationMetadata() {
    }

    /**
     * Full constructor for creating pagination metadata
     */
    public PaginationMetadata(int currentPage, int pageSize, long totalEntries, int totalPages) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalEntries = totalEntries;
        this.totalPages = totalPages;
    }

    /**
     * Convenience constructor that calculates total pages from total entries
     */
    public PaginationMetadata(int currentPage, int pageSize, long totalEntries) {
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalEntries = totalEntries;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalEntries / pageSize) : 0;
    }

    // Getters and Setters

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(long totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    /**
     * Check if there is a next page
     */
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }

    /**
     * Check if there is a previous page
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }

    @Override
    public String toString() {
        return "PaginationMetadata{" +
                "currentPage=" + currentPage +
                ", pageSize=" + pageSize +
                ", totalEntries=" + totalEntries +
                ", totalPages=" + totalPages +
                ", hasNext=" + hasNext() +
                ", hasPrevious=" + hasPrevious() +
                '}';
    }
}
