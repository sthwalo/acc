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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Audit Trail report entries.
 * Represents journal entries with their lines for audit purposes.
 */
public class AuditTrailDTO {
    private final String reference;
    private final LocalDateTime entryDate;
    private final String description;
    private final String createdBy;
    private final LocalDateTime createdAt;
    private final List<AuditTrailLineDTO> lines;

    public AuditTrailDTO(String reference, LocalDateTime entryDate, String description,
                        String createdBy, LocalDateTime createdAt, List<AuditTrailLineDTO> lines) {
        this.reference = reference;
        this.entryDate = entryDate;
        this.description = description;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.lines = lines;
    }

    public String getReference() { return reference; }
    public LocalDateTime getEntryDate() { return entryDate; }
    public String getDescription() { return description; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<AuditTrailLineDTO> getLines() { return lines; }
}