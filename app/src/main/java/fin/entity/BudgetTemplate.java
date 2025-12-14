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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget Template entity.
 * Defines industry-specific budget categories and default percentages for company setup.
 */
@Entity
@Table(name = "budget_templates", indexes = {
    @Index(name = "idx_budget_templates_industry_id", columnList = "industry_id"),
    @Index(name = "idx_budget_templates_budget_type", columnList = "budget_type"),
    @Index(name = "idx_budget_templates_category", columnList = "category")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String category;

    @Size(max = 100)
    private String subcategory;

    @NotBlank
    @Size(max = 20)
    @Column(name = "budget_type", nullable = false)
    private String budgetType; // REVENUE, EXPENSE, CAPITAL

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    @Column(name = "default_percentage", precision = 5, scale = 2)
    private BigDecimal defaultPercentage;

    @NotBlank
    @Size(max = 20)
    @Builder.Default
    @Column(nullable = false)
    private String frequency = "MONTHLY"; // MONTHLY, QUARTERLY, ANNUALLY

    @NotNull
    @Builder.Default
    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory = false;

    @Column(columnDefinition = "TEXT")
    private String description;

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