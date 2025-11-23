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

/**
 * DTO for fiscal period data with company information
 * Used for frontend display of fiscal periods across all companies
 */
public class FiscalPeriodSummary {
    private String companyName;
    private Long fiscalPeriodId;
    private String fiscalYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isClosed;

    public FiscalPeriodSummary() {}

    public FiscalPeriodSummary(String companyName, Long fiscalPeriodId, String periodName,
                              LocalDate startDate, LocalDate endDate, boolean isClosed) {
        this.companyName = companyName;
        this.fiscalPeriodId = fiscalPeriodId;
        this.fiscalYear = periodName; // periodName maps to fiscalYear field
        this.startDate = startDate;
        this.endDate = endDate;
        this.isClosed = isClosed;
    }

    // Getters and setters
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public Long getFiscalPeriodId() { return fiscalPeriodId; }
    public void setFiscalPeriodId(Long fiscalPeriodId) { this.fiscalPeriodId = fiscalPeriodId; }

    public String getFiscalYear() { return fiscalYear; }
    public void setFiscalYear(String fiscalYear) { this.fiscalYear = fiscalYear; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isClosed() { return isClosed; }
    public void setClosed(boolean isClosed) { this.isClosed = isClosed; }

    @Override
    public String toString() {
        return "FiscalPeriodSummary{" +
                "companyName='" + companyName + '\'' +
                ", fiscalPeriodId=" + fiscalPeriodId +
                ", fiscalYear='" + fiscalYear + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", isClosed=" + isClosed +
                '}';
    }
}