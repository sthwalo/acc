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
 * Rule Template entity.
 * Defines industry-specific transaction mapping rules that are copied to companies during setup.
 */
@Entity
@Table(name = "rule_templates", indexes = {
    @Index(name = "idx_rule_templates_industry_id", columnList = "industry_id"),
    @Index(name = "idx_rule_templates_transaction_type_id", columnList = "transaction_type_id"),
    @Index(name = "idx_rule_templates_priority", columnList = "priority")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @NotBlank
    @Size(max = 255)
    @Column(name = "rule_name", nullable = false)
    private String ruleName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String pattern;

    @Size(max = 50)
    @Column(name = "match_type")
    private String matchType;

    @Size(max = 1000)
    @Column(name = "match_value")
    private String matchValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_id")
    private TransactionType transactionType;

    @Size(max = 100)
    private String category;

    @Size(max = 100)
    private String subcategory;

    @Size(max = 255)
    @Column(name = "account_code")
    private String accountCode;

    @NotNull
    @Builder.Default
    @Column(nullable = false)
    private Integer priority = 0;

    @NotNull
    @Builder.Default
    @Column(name = "is_default_enabled", nullable = false)
    private Boolean isDefaultEnabled = true;

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