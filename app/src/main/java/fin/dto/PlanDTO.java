/*
 * FIN Financial Management System
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 * Owner: Immaculate Nyoni
 * Contact: sthwaloe@gmail.com | +27 61 514 6185
 *
 * Licensed under Apache License 2.0 - Commercial use requires separate licensing
 */

package fin.dto;

import fin.entity.Plan;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * DTO for Plan data sent to frontend
 */
public class PlanDTO {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price_monthly;
    private BigDecimal price_yearly;
    private List<String> features;
    private int max_companies;
    private int max_transactions;
    private String support_level;
    private boolean is_active;

    public PlanDTO() {}

    public PlanDTO(Plan plan) {
        this.id = plan.getId();
        this.name = plan.getName();
        this.description = plan.getDescription();
        this.is_active = plan.isActive();

        // Set prices based on billing cycle
        if ("MONTHLY".equals(plan.getBillingCycle())) {
            this.price_monthly = plan.getPrice();
            this.price_yearly = plan.getPrice().multiply(BigDecimal.valueOf(12));
        } else if ("YEARLY".equals(plan.getBillingCycle())) {
            this.price_yearly = plan.getPrice();
            this.price_monthly = plan.getPrice().divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
        } else {
            this.price_monthly = plan.getPrice();
            this.price_yearly = plan.getPrice().multiply(BigDecimal.valueOf(12));
        }

        // Set max values
        this.max_companies = plan.getMaxCompanies();
        this.max_transactions = plan.getMaxTransactionsPerMonth();

        // Build features list based on plan capabilities
        this.features = Arrays.asList(
            plan.isCanAccessDashboard() ? "Dashboard Access" : null,
            plan.isCanManageCompanies() ? "Company Management" : null,
            plan.isCanProcessBankStatements() ? "Bank Statement Processing" : null,
            plan.isCanGenerateReports() ? "Financial Reports" : null,
            plan.isCanManagePayroll() ? "Payroll Management" : null,
            plan.isCanManageBudgets() ? "Budget Management" : null,
            plan.isCanAccessMultipleCompanies() ? "Multiple Companies" : null,
            plan.isHasApiAccess() ? "API Access" : null,
            plan.isHasPrioritySupport() ? "Priority Support" : null
        ).stream().filter(f -> f != null).toList();

        // Set support level
        if (plan.isHasPrioritySupport()) {
            this.support_level = "enterprise";
        } else if (plan.isHasApiAccess()) {
            this.support_level = "premium";
        } else {
            this.support_level = "basic";
        }
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice_monthly() { return price_monthly; }
    public void setPrice_monthly(BigDecimal price_monthly) { this.price_monthly = price_monthly; }

    public BigDecimal getPrice_yearly() { return price_yearly; }
    public void setPrice_yearly(BigDecimal price_yearly) { this.price_yearly = price_yearly; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }

    public int getMax_companies() { return max_companies; }
    public void setMax_companies(int max_companies) { this.max_companies = max_companies; }

    public int getMax_transactions() { return max_transactions; }
    public void setMax_transactions(int max_transactions) { this.max_transactions = max_transactions; }

    public String getSupport_level() { return support_level; }
    public void setSupport_level(String support_level) { this.support_level = support_level; }

    public boolean isIs_active() { return is_active; }
    public void setIs_active(boolean is_active) { this.is_active = is_active; }
}