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
import java.time.LocalDateTime;

/**
 * Data correction entity for tracking transaction categorization corrections
 */
@Entity
@Table(name = "data_corrections")
public class DataCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "original_account_id", nullable = false)
    private Long originalAccountId;

    @Column(name = "new_account_id", nullable = false)
    private Long newAccountId;

    @Column(name = "correction_reason", nullable = false)
    private String correctionReason;

    @Column(name = "corrected_by", nullable = false)
    private String correctedBy;

    @Column(name = "corrected_at", nullable = false)
    private LocalDateTime correctedAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

    public Long getOriginalAccountId() { return originalAccountId; }
    public void setOriginalAccountId(Long originalAccountId) { this.originalAccountId = originalAccountId; }

    public Long getNewAccountId() { return newAccountId; }
    public void setNewAccountId(Long newAccountId) { this.newAccountId = newAccountId; }

    public String getCorrectionReason() { return correctionReason; }
    public void setCorrectionReason(String correctionReason) { this.correctionReason = correctionReason; }

    public String getCorrectedBy() { return correctedBy; }
    public void setCorrectedBy(String correctedBy) { this.correctedBy = correctedBy; }

    public LocalDateTime getCorrectedAt() { return correctedAt; }
    public void setCorrectedAt(LocalDateTime correctedAt) { this.correctedAt = correctedAt; }
}