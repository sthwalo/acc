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
 * Payroll Template entity.
 * Defines industry-specific payroll components and SARS statutory requirements.
 */
@Entity
@Table(name = "payroll_templates", indexes = {
    @Index(name = "idx_payroll_templates_industry_id", columnList = "industry_id"),
    @Index(name = "idx_payroll_templates_component_type", columnList = "component_type"),
    @Index(name = "idx_payroll_templates_sars_code", columnList = "sars_code")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @NotBlank
    @Size(max = 20)
    @Column(name = "component_type", nullable = false)
    private String componentType; // EARNINGS, DEDUCTIONS, CONTRIBUTIONS

    @NotBlank
    @Size(max = 255)
    @Column(name = "component_name", nullable = false)
    private String componentName;

    @Size(max = 20)
    @Column(name = "sars_code")
    private String sarsCode;

    @NotNull
    @Builder.Default
    @Column(name = "is_statutory", nullable = false)
    private Boolean isStatutory = false;

    @DecimalMin(value = "0.0000")
    @DecimalMax(value = "100.0000")
    @Column(name = "default_rate", precision = 8, scale = 4)
    private BigDecimal defaultRate;

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