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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Budget model representing an annual budget for an organization
 */
public class Budget {
    private Long id;
    private Long companyId;
    private Long fiscalPeriodId;
    private String title;
    private String description;
    private Integer budgetYear;
    private String status; // DRAFT, APPROVED, ACTIVE, ARCHIVED
    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private List<BudgetCategory> categories; // For loading complete budget with categories

    // Constructors
    public Budget() { }

    public Budget(Long newCompanyId, String newTitle, Integer newBudgetYear) {
        this.companyId = newCompanyId;
        this.title = newTitle;
        this.budgetYear = newBudgetYear;
        this.status = "DRAFT";
        this.totalRevenue = BigDecimal.ZERO;
        this.totalExpenses = BigDecimal.ZERO;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    /**
     * Gets the unique identifier for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom identifier retrieval logic, but should maintain the contract
     * of returning a non-negative Long value or null for unsaved budgets.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return the same identifier consistently across multiple calls</li>
     *   <li>Return null only for budgets that haven't been persisted</li>
     *   <li>Handle any custom identifier generation logic appropriately</li>
     *   <li>Document any additional validation or transformation performed</li>
     * </ul>
     * </p>
     * @return the budget's unique identifier, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom identifier assignment logic, such as validation or
     * transformation of the identifier value.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate that the identifier is non-negative if required</li>
     *   <li>Handle null values appropriately (may indicate unsaved state)</li>
     *   <li>Perform any necessary identifier format validation</li>
     *   <li>Document any side effects or additional processing</li>
     * </ul>
     * </p>
     * @param newId the unique identifier to set, or null for unsaved budgets
     * @throws IllegalArgumentException if the identifier format is invalid
     */
    public void setId(Long newId) {
        this.id = newId;
    }

    /**
     * Gets the company identifier this budget belongs to.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom company identifier retrieval, such as resolving company
     * references or applying access control checks.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure the returned company ID corresponds to an existing company</li>
     *   <li>Handle cases where company association might be temporarily unavailable</li>
     *   <li>Consider security implications of exposing company relationships</li>
     *   <li>Document any caching or lazy loading behavior</li>
     * </ul>
     * </p>
     * @return the company identifier, or null if not associated with a company
     */
    public Long getCompanyId() {
        return companyId;
    }

    /**
     * Sets the company identifier this budget belongs to.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom company association logic, including validation of company
     * existence and permission checks.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate that the company exists and is accessible</li>
     *   <li>Check user permissions for associating budgets with companies</li>
     *   <li>Handle null values (may indicate unassigned state)</li>
     *   <li>Update any dependent relationships or caches</li>
     * </ul>
     * </p>
     * @param newCompanyId the company identifier to associate with this budget
     * @throws IllegalArgumentException if the company ID is invalid or inaccessible
     * @throws SecurityException if the user lacks permission to associate with the company
     */
    public void setCompanyId(Long newCompanyId) {
        this.companyId = newCompanyId;
    }

    /**
     * Gets the fiscal period identifier for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom fiscal period resolution or validation logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure the fiscal period is valid and active</li>
     *   <li>Handle cases where fiscal period might be derived from budget year</li>
     *   <li>Consider temporal constraints and period boundaries</li>
     *   <li>Document any automatic fiscal period assignment logic</li>
     * </ul>
     * </p>
     * @return the fiscal period identifier, or null if not assigned
     */
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    /**
     * Sets the fiscal period identifier for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom fiscal period assignment logic, including validation
     * and compatibility checks with the budget year.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate fiscal period exists and is appropriate for the budget year</li>
     *   <li>Check for conflicts with existing budget assignments</li>
     *   <li>Handle automatic fiscal period derivation from budget year</li>
     *   <li>Update any period-dependent calculations or validations</li>
     * </ul>
     * </p>
     * @param newFiscalPeriodId the fiscal period identifier to set
     * @throws IllegalArgumentException if the fiscal period is invalid or incompatible
     */
    public void setFiscalPeriodId(Long newFiscalPeriodId) {
        this.fiscalPeriodId = newFiscalPeriodId;
    }

    /**
     * Gets the title of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom title formatting or localization logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure returned title is meaningful and non-empty when set</li>
     *   <li>Handle localization or internationalization requirements</li>
     *   <li>Consider title length limits or format constraints</li>
     *   <li>Document any automatic title generation logic</li>
     * </ul>
     * </p>
     * @return the budget title, or null if not set
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom title validation, formatting, or normalization logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate title is not null or empty if required</li>
     *   <li>Apply title formatting or length restrictions</li>
     *   <li>Handle duplicate title detection and resolution</li>
     *   <li>Update any title-dependent references or indexes</li>
     * </ul>
     * </p>
     * @param newTitle the title to set for this budget
     * @throws IllegalArgumentException if the title is invalid or violates constraints
     */
    public void setTitle(String newTitle) {
        this.title = newTitle;
    }

    /**
     * Gets the description of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom description processing or formatting logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Handle null descriptions appropriately</li>
     *   <li>Consider description length limits or format requirements</li>
     *   <li>Support rich text or markup processing if needed</li>
     *   <li>Document any automatic description generation</li>
     * </ul>
     * </p>
     * @return the budget description, or null if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom description validation, sanitization, or processing logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate description content and length constraints</li>
     *   <li>Sanitize or filter inappropriate content</li>
     *   <li>Handle rich text or markup validation</li>
     *   <li>Update any description-dependent features</li>
     * </ul>
     * </p>
     * @param newDescription the description to set for this budget
     * @throws IllegalArgumentException if the description violates content rules
     */
    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    /**
     * Gets the budget year for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom year validation or derivation logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure year is within reasonable bounds (e.g., not in distant past/future)</li>
     *   <li>Handle fiscal year vs calendar year distinctions</li>
     *   <li>Consider year format validation and normalization</li>
     *   <li>Document any automatic year assignment logic</li>
     * </ul>
     * </p>
     * @return the budget year, or null if not set
     */
    public Integer getBudgetYear() {
        return budgetYear;
    }

    /**
     * Sets the budget year for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom year validation, fiscal year calculation, or constraint logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate year is within acceptable range</li>
     *   <li>Handle fiscal year vs calendar year conversions</li>
     *   <li>Check for conflicts with existing budgets for the same year</li>
     *   <li>Update any year-dependent calculations or validations</li>
     * </ul>
     * </p>
     * @param newBudgetYear the budget year to set
     * @throws IllegalArgumentException if the year is invalid or out of range
     */
    public void setBudgetYear(Integer newBudgetYear) {
        this.budgetYear = newBudgetYear;
    }

    /**
     * Gets the current status of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom status interpretation or workflow logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return status values from the defined set: DRAFT, APPROVED, ACTIVE, ARCHIVED</li>
     *   <li>Handle status transitions and validation</li>
     *   <li>Consider security implications of status changes</li>
     *   <li>Document any automatic status progression logic</li>
     * </ul>
     * </p>
     * @return the budget status (DRAFT, APPROVED, ACTIVE, ARCHIVED), or null if not set
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom status validation, workflow enforcement, or transition logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate status is one of: DRAFT, APPROVED, ACTIVE, ARCHIVED</li>
     *   <li>Enforce valid status transitions (e.g., DRAFT → APPROVED → ACTIVE)</li>
     *   <li>Check user permissions for status changes</li>
     *   <li>Trigger status-dependent business logic or notifications</li>
     * </ul>
     * </p>
     * @param newStatus the status to set (DRAFT, APPROVED, ACTIVE, ARCHIVED)
     * @throws IllegalArgumentException if the status is invalid
     * @throws IllegalStateException if the status transition is not allowed
     */
    public void setStatus(String newStatus) {
        this.status = newStatus;
    }

    /**
     * Gets the total revenue amount for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom revenue calculation or aggregation logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure revenue amounts are non-negative</li>
     *   <li>Handle currency conversion if needed</li>
     *   <li>Consider aggregation from budget categories or items</li>
     *   <li>Document any automatic calculation logic</li>
     * </ul>
     * </p>
     * @return the total revenue amount, or null if not calculated
     */
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    /**
     * Sets the total revenue amount for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom revenue validation, calculation, or synchronization logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate revenue amount is non-negative</li>
     *   <li>Handle currency validation and conversion</li>
     *   <li>Trigger recalculation of dependent values (net budget)</li>
     *   <li>Update related budget categories or items</li>
     * </ul>
     * </p>
     * @param newTotalRevenue the total revenue amount to set
     * @throws IllegalArgumentException if the revenue amount is negative
     */
    public void setTotalRevenue(BigDecimal newTotalRevenue) {
        this.totalRevenue = newTotalRevenue;
    }

    /**
     * Gets the total expenses amount for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom expense calculation or aggregation logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Ensure expense amounts are non-negative</li>
     *   <li>Handle currency conversion if needed</li>
     *   <li>Consider aggregation from budget categories or items</li>
     *   <li>Document any automatic calculation logic</li>
     * </ul>
     * </p>
     * @return the total expenses amount, or null if not calculated
     */
    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    /**
     * Sets the total expenses amount for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom expense validation, calculation, or synchronization logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate expense amount is non-negative</li>
     *   <li>Handle currency validation and conversion</li>
     *   <li>Trigger recalculation of dependent values (net budget)</li>
     *   <li>Update related budget categories or items</li>
     * </ul>
     * </p>
     * @param newTotalExpenses the total expenses amount to set
     * @throws IllegalArgumentException if the expense amount is negative
     */
    public void setTotalExpenses(BigDecimal newTotalExpenses) {
        this.totalExpenses = newTotalExpenses;
    }

    /**
     * Gets the creation timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom timestamp handling or timezone conversion logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return timestamps in consistent timezone (typically UTC)</li>
     *   <li>Handle null timestamps for unsaved budgets</li>
     *   <li>Consider audit trail and immutability requirements</li>
     *   <li>Document any automatic timestamp assignment</li>
     * </ul>
     * </p>
     * @return the creation timestamp, or null if not yet created
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom timestamp validation, automatic assignment, or audit logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate timestamp is not in the future</li>
     *   <li>Handle automatic timestamp assignment on creation</li>
     *   <li>Consider immutability after initial creation</li>
     *   <li>Update audit trails or change tracking</li>
     * </ul>
     * </p>
     * @param newCreatedAt the creation timestamp to set
     * @throws IllegalArgumentException if the timestamp is invalid
     */
    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }

    /**
     * Gets the last update timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom update timestamp handling or tracking logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return timestamps in consistent timezone (typically UTC)</li>
     *   <li>Handle null timestamps appropriately</li>
     *   <li>Consider automatic update timestamp management</li>
     *   <li>Document any lazy update timestamp logic</li>
     * </ul>
     * </p>
     * @return the last update timestamp, or null if never updated
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom update tracking, validation, or automatic assignment logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate timestamp is not in the future</li>
     *   <li>Handle automatic timestamp assignment on updates</li>
     *   <li>Ensure update timestamp is after creation timestamp</li>
     *   <li>Trigger change tracking or audit logging</li>
     * </ul>
     * </p>
     * @param newUpdatedAt the update timestamp to set
     * @throws IllegalArgumentException if the timestamp is invalid
     */
    public void setUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
    }

    /**
     * Gets the approval timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom approval timestamp handling or workflow logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return null for budgets that haven't been approved</li>
     *   <li>Handle approval workflow and status synchronization</li>
     *   <li>Consider security implications of approval timestamps</li>
     *   <li>Document approval process integration</li>
     * </ul>
     * </p>
     * @return the approval timestamp, or null if not yet approved
     */
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    /**
     * Sets the approval timestamp for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom approval validation, workflow integration, or audit logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate approval timestamp is not in the future</li>
     *   <li>Ensure budget status is appropriate for approval</li>
     *   <li>Check user permissions for approval actions</li>
     *   <li>Trigger approval-related business logic or notifications</li>
     * </ul>
     * </p>
     * @param newApprovedAt the approval timestamp to set
     * @throws IllegalArgumentException if the timestamp is invalid
     * @throws IllegalStateException if the budget is not in an approvable state
     */
    public void setApprovedAt(LocalDateTime newApprovedAt) {
        this.approvedAt = newApprovedAt;
    }

    /**
     * Gets the user who approved this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom approver identification or resolution logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return null for budgets that haven't been approved</li>
     *   <li>Validate approver identity and permissions</li>
     *   <li>Handle user identification formats or references</li>
     *   <li>Document approver resolution or lookup logic</li>
     * </ul>
     * </p>
     * @return the approver's identifier, or null if not yet approved
     */
    public String getApprovedBy() {
        return approvedBy;
    }

    /**
     * Sets the user who approved this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom approver validation, assignment, or audit logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Validate approver exists and has approval permissions</li>
     *   <li>Ensure approval timestamp is also set appropriately</li>
     *   <li>Check approval workflow compliance</li>
     *   <li>Trigger approval-related notifications or logging</li>
     * </ul>
     * </p>
     * @param newApprovedBy the approver's identifier to set
     * @throws IllegalArgumentException if the approver is invalid or lacks permission
     */
    public void setApprovedBy(String newApprovedBy) {
        this.approvedBy = newApprovedBy;
    }

    /**
     * Gets the list of budget categories for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom category loading, filtering, or ordering logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Return a defensive copy to prevent external modification</li>
     *   <li>Handle lazy loading of categories if not already loaded</li>
     *   <li>Consider category ordering or filtering requirements</li>
     *   <li>Document any caching or performance optimizations</li>
     * </ul>
     * </p>
     * @return an unmodifiable list of budget categories, never null
     */
    public List<BudgetCategory> getCategories() {
        return categories != null ? new ArrayList<>(categories) : Collections.emptyList();
    }

    /**
     * Sets the list of budget categories for this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom category validation, synchronization, or processing logic.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Create a defensive copy of the input list</li>
     *   <li>Validate category relationships and constraints</li>
     *   <li>Update any dependent calculations (totals, net budget)</li>
     *   <li>Handle category ordering or hierarchy requirements</li>
     * </ul>
     * </p>
     * @param newCategories the list of budget categories to set
     * @throws IllegalArgumentException if categories violate business rules
     */
    public void setCategories(List<BudgetCategory> newCategories) {
        this.categories = newCategories != null ? new ArrayList<>(newCategories) : null;
    }

    // Helper methods
    /**
     * Calculates the net budget amount (revenue minus expenses).
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom net budget calculation logic, such as including additional
     * factors, applying different calculation rules, or handling special cases.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Handle null revenue or expense values appropriately (defaulting to zero)</li>
     *   <li>Ensure consistent calculation logic across different budget types</li>
     *   <li>Consider currency conversion or rounding requirements</li>
     *   <li>Document any additional factors included in the calculation</li>
     *   <li>Return zero or null appropriately for incomplete budgets</li>
     * </ul>
     * </p>
     * <p>
     * <b>Default Implementation:</b> Returns totalRevenue - totalExpenses,
     * treating null values as zero.
     * </p>
     * @return the net budget amount (revenue - expenses), or null if calculation not possible
     */
    public BigDecimal getNetBudget() {
        return totalRevenue.subtract(totalExpenses);
    }

    /**
     * Returns a string representation of this budget.
     * <p>
     * This method is designed for extension. Subclasses may override this method
     * to provide custom string formatting, additional fields, or localization support.
     * </p>
     * <p>
     * <b>Override Guidelines:</b>
     * <ul>
     *   <li>Include essential identifying information (ID, title, year)</li>
     *   <li>Handle null values gracefully in the output</li>
     *   <li>Consider security implications of exposing sensitive data</li>
     *   <li>Keep output concise but informative for debugging/logging</li>
     *   <li>Use consistent formatting across different budget types</li>
     *   <li>Support internationalization/localization if needed</li>
     * </ul>
     * </p>
     * <p>
     * <b>Default Implementation:</b> Returns format "Budget{id=X, title='Y', year=Z, status='W', net=V}"
     * where net is calculated via getNetBudget().
     * </p>
     * @return a string representation of this budget, never null
     */
    @Override
    public String toString() {
        return String.format("Budget{id=%d, title='%s', year=%d, status='%s', net=%s}",
                           id, title, budgetYear, status, getNetBudget());
    }
}
