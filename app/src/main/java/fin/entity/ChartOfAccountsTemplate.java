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
import java.util.List;

/**
 * Chart of Accounts Template entity.
 * Defines industry-specific account structures that are copied to companies during setup.
 */
@Entity
@Table(name = "chart_of_accounts_templates", indexes = {
    @Index(name = "idx_coa_templates_industry_id", columnList = "industry_id"),
    @Index(name = "idx_coa_templates_parent_id", columnList = "parent_template_id"),
    @Index(name = "idx_coa_templates_level", columnList = "level")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChartOfAccountsTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @NotBlank
    @Size(max = 50)
    @Column(name = "account_code", nullable = false)
    private String accountCode;

    @NotBlank
    @Size(max = 255)
    @Column(name = "account_name", nullable = false)
    private String accountName;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type")
    private AccountType accountType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_template_id")
    private ChartOfAccountsTemplate parentTemplate;

    @NotNull
    @Min(1)
    @Max(4)
    @Column(nullable = false)
    private Integer level;

    @NotNull
    @Builder.Default
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Builder.Default
    @Column(name = "default_balance", precision = 15, scale = 2)
    private BigDecimal defaultBalance = BigDecimal.ZERO;

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

    // Relationships
    @OneToMany(mappedBy = "parentTemplate", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChartOfAccountsTemplate> childTemplates;
}