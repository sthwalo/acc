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

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan entity representing pricing plans and feature access levels
 */
@Entity
@Table(name = "plans")
public class Plan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "currency", nullable = false)
    private String currency = "ZAR";

    @Column(name = "billing_cycle")
    private String billingCycle; // MONTHLY, YEARLY

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    // Feature flags
    @Column(name = "can_access_dashboard", nullable = false)
    private boolean canAccessDashboard = true;

    @Column(name = "can_manage_companies", nullable = false)
    private boolean canManageCompanies = false;

    @Column(name = "can_process_bank_statements", nullable = false)
    private boolean canProcessBankStatements = true;

    @Column(name = "can_generate_reports", nullable = false)
    private boolean canGenerateReports = true;

    @Column(name = "can_manage_payroll", nullable = false)
    private boolean canManagePayroll = false;

    @Column(name = "can_manage_budgets", nullable = false)
    private boolean canManageBudgets = false;

    @Column(name = "can_access_multiple_companies", nullable = false)
    private boolean canAccessMultipleCompanies = false;

    @Column(name = "max_companies", nullable = false)
    private int maxCompanies = 1;

    @Column(name = "max_users", nullable = false)
    private int maxUsers = 1;

    @Column(name = "max_transactions_per_month", nullable = false)
    private int maxTransactionsPerMonth = 1000;

    @Column(name = "has_api_access", nullable = false)
    private boolean hasApiAccess = false;

    @Column(name = "has_priority_support", nullable = false)
    private boolean hasPrioritySupport = false;

    // Constructors
    public Plan() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Plan(String name, String description, BigDecimal price, String billingCycle) {
        this();
        this.name = name;
        this.description = description;
        this.price = price;
        this.billingCycle = billingCycle;
        this.createdBy = "FIN";
        this.updatedBy = "FIN";
    }

    /**
     * Copy constructor for defensive copying
     */
    public Plan(Plan other) {
        if (other != null) {
            this.id = other.id;
            this.name = other.name;
            this.description = other.description;
            this.price = other.price;
            this.currency = other.currency;
            this.billingCycle = other.billingCycle;
            this.isActive = other.isActive;
            this.createdAt = other.createdAt;
            this.updatedAt = other.updatedAt;
            this.createdBy = other.createdBy;
            this.updatedBy = other.updatedBy;
            this.canAccessDashboard = other.canAccessDashboard;
            this.canManageCompanies = other.canManageCompanies;
            this.canProcessBankStatements = other.canProcessBankStatements;
            this.canGenerateReports = other.canGenerateReports;
            this.canManagePayroll = other.canManagePayroll;
            this.canManageBudgets = other.canManageBudgets;
            this.canAccessMultipleCompanies = other.canAccessMultipleCompanies;
            this.maxCompanies = other.maxCompanies;
            this.maxUsers = other.maxUsers;
            this.maxTransactionsPerMonth = other.maxTransactionsPerMonth;
            this.hasApiAccess = other.hasApiAccess;
            this.hasPrioritySupport = other.hasPrioritySupport;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBillingCycle() { return billingCycle; }
    public void setBillingCycle(String billingCycle) { this.billingCycle = billingCycle; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    // Feature flag getters and setters
    public boolean isCanAccessDashboard() { return canAccessDashboard; }
    public void setCanAccessDashboard(boolean canAccessDashboard) { this.canAccessDashboard = canAccessDashboard; }

    public boolean isCanManageCompanies() { return canManageCompanies; }
    public void setCanManageCompanies(boolean canManageCompanies) { this.canManageCompanies = canManageCompanies; }

    public boolean isCanProcessBankStatements() { return canProcessBankStatements; }
    public void setCanProcessBankStatements(boolean canProcessBankStatements) { this.canProcessBankStatements = canProcessBankStatements; }

    public boolean isCanGenerateReports() { return canGenerateReports; }
    public void setCanGenerateReports(boolean canGenerateReports) { this.canGenerateReports = canGenerateReports; }

    public boolean isCanManagePayroll() { return canManagePayroll; }
    public void setCanManagePayroll(boolean canManagePayroll) { this.canManagePayroll = canManagePayroll; }

    public boolean isCanManageBudgets() { return canManageBudgets; }
    public void setCanManageBudgets(boolean canManageBudgets) { this.canManageBudgets = canManageBudgets; }

    public boolean isCanAccessMultipleCompanies() { return canAccessMultipleCompanies; }
    public void setCanAccessMultipleCompanies(boolean canAccessMultipleCompanies) { this.canAccessMultipleCompanies = canAccessMultipleCompanies; }

    public int getMaxCompanies() { return maxCompanies; }
    public void setMaxCompanies(int maxCompanies) { this.maxCompanies = maxCompanies; }

    public int getMaxUsers() { return maxUsers; }
    public void setMaxUsers(int maxUsers) { this.maxUsers = maxUsers; }

    public int getMaxTransactionsPerMonth() { return maxTransactionsPerMonth; }
    public void setMaxTransactionsPerMonth(int maxTransactionsPerMonth) { this.maxTransactionsPerMonth = maxTransactionsPerMonth; }

    public boolean isHasApiAccess() { return hasApiAccess; }
    public void setHasApiAccess(boolean hasApiAccess) { this.hasApiAccess = hasApiAccess; }

    public boolean isHasPrioritySupport() { return hasPrioritySupport; }
    public void setHasPrioritySupport(boolean hasPrioritySupport) { this.hasPrioritySupport = hasPrioritySupport; }

    // Utility methods
    public boolean isTrialPlan() {
        return price != null && price.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isPaidPlan() {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean canUserAccessFeature(String feature) {
        switch (feature.toLowerCase()) {
            case "dashboard":
                return canAccessDashboard;
            case "companies":
                return canManageCompanies;
            case "bank_statements":
                return canProcessBankStatements;
            case "reports":
                return canGenerateReports;
            case "payroll":
                return canManagePayroll;
            case "budgets":
                return canManageBudgets;
            case "multiple_companies":
                return canAccessMultipleCompanies;
            case "api":
                return hasApiAccess;
            default:
                return false;
        }
    }

    public static Plan createFreePlan() {
        Plan plan = new Plan("Trial", "1-month trial with basic financial management features", BigDecimal.ZERO, "MONTHLY");
        plan.setCanAccessDashboard(true);
        plan.setCanProcessBankStatements(true);
        plan.setCanGenerateReports(true);
        plan.setMaxCompanies(1);
        plan.setMaxUsers(1);
        plan.setMaxTransactionsPerMonth(100); // Trial limit
        return plan;
    }

    public static Plan createStarterPlan() {
        Plan plan = new Plan("Starter", "Perfect for small businesses getting started", new BigDecimal("29.00"), "MONTHLY");
        plan.setCanAccessDashboard(true);
        plan.setCanManageCompanies(true);
        plan.setCanProcessBankStatements(true);
        plan.setCanGenerateReports(true);
        plan.setCanAccessMultipleCompanies(true);
        plan.setMaxCompanies(3);
        plan.setMaxUsers(5);
        plan.setMaxTransactionsPerMonth(1000);
        return plan;
    }

    public static Plan createProfessionalPlan() {
        Plan plan = new Plan("Professional", "Advanced features for growing businesses", new BigDecimal("99.00"), "MONTHLY");
        plan.setCanAccessDashboard(true);
        plan.setCanManageCompanies(true);
        plan.setCanProcessBankStatements(true);
        plan.setCanGenerateReports(true);
        plan.setCanManagePayroll(true);
        plan.setCanManageBudgets(true);
        plan.setCanAccessMultipleCompanies(true);
        plan.setMaxCompanies(10);
        plan.setMaxUsers(15);
        plan.setMaxTransactionsPerMonth(10000);
        plan.setHasApiAccess(true);
        return plan;
    }

    public static Plan createEnterprisePlan() {
        Plan plan = new Plan("Enterprise", "Complete solution for large enterprises", new BigDecimal("299.00"), "MONTHLY");
        plan.setCanAccessDashboard(true);
        plan.setCanManageCompanies(true);
        plan.setCanProcessBankStatements(true);
        plan.setCanGenerateReports(true);
        plan.setCanManagePayroll(true);
        plan.setCanManageBudgets(true);
        plan.setCanAccessMultipleCompanies(true);
        plan.setMaxCompanies(-1); // unlimited
        plan.setMaxUsers(-1); // unlimited
        plan.setMaxTransactionsPerMonth(-1); // unlimited
        plan.setHasApiAccess(true);
        plan.setHasPrioritySupport(true);
        return plan;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", currency='" + currency + '\'' +
                ", billingCycle='" + billingCycle + '\'' +
                ", maxCompanies=" + maxCompanies +
                ", maxUsers=" + maxUsers +
                '}';
    }
}