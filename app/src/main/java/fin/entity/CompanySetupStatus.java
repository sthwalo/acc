/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Company Setup Status entity.
 * Tracks the progress of company onboarding and template population.
 */
@Entity
@Table(name = "company_setup_status", indexes = {
    @Index(name = "idx_company_setup_status_company_id", columnList = "company_id"),
    @Index(name = "idx_company_setup_status_stage", columnList = "setup_stage"),
    @Index(name = "idx_company_setup_status_complete", columnList = "is_setup_complete")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySetupStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @NotBlank
    @Size(max = 30)
    @Column(name = "setup_stage", nullable = false)
    private String setupStage; // INDUSTRY_SELECTION, ACCOUNTS_SETUP, RULES_SETUP, BUDGET_SETUP, PAYROLL_SETUP, COMPLETE

    @Column(name = "stage_completed_at")
    private LocalDateTime stageCompletedAt;

    @Size(max = 255)
    @Column(name = "stage_completed_by")
    private String stageCompletedBy;

    @NotNull
    @Builder.Default
    @Column(name = "is_setup_complete", nullable = false)
    private Boolean isSetupComplete = false;

    @NotNull
    @Min(0)
    @Max(100)
    @Builder.Default
    @Column(name = "setup_percentage", nullable = false)
    private Integer setupPercentage = 0;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Size(max = 255)
    @Column(name = "created_by")
    private String createdBy;

    @Size(max = 255)
    @Column(name = "updated_by")
    private String updatedBy;
}