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

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
public class BankTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "company_id")
    private Long companyId;
    @Column(name = "bank_account_id")
    private Long bankAccountId;
    @Column(name = "fiscal_period_id")
    private Long fiscalPeriodId;
    @Column(name = "account_code")
    private String accountCode;
    @Column(name = "account_number")
    private String accountNumber;
    @Column(name = "source_file")
    private String sourceFile;
    @Column(name = "service_fee")
    private Boolean serviceFee;
    @Column(name = "transaction_date")
    private LocalDate transactionDate;
    @Column(name = "description")
    private String description;
    @Column(name = "debit_amount")
    private BigDecimal debitAmount;
    @Column(name = "credit_amount")
    private BigDecimal creditAmount;
    @Column(name = "balance")
    private BigDecimal balance;
    @Column(name = "reference")
    private String reference;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Additional database fields
    @Column(name = "transaction_type_id")
    private Long transactionTypeId;
    @Column(name = "category")
    private String category;
    @Column(name = "subcategory")
    private String subcategory;
    @Column(name = "is_reconciled")
    private Boolean isReconciled;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    @Column(name = "classification_date")
    private LocalDateTime classificationDate;
    @Column(name = "classified_by")
    private String classifiedBy;
    
    // Constructors, getters, and setters
    public BankTransaction() {
        this.createdAt = LocalDateTime.now();
    }
    
    /**
     * Gets the unique identifier for this bank transaction.
     * <p>
     * Subclasses may override this method to provide custom ID retrieval logic,
     * but must ensure the returned value is non-null for persisted transactions.
     * Any validation or transformation should be documented in the overriding method.
     * </p>
     * @return the transaction ID, or null if not yet persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the unique identifier for this bank transaction.
     * <p>
     * Subclasses may override this method to implement custom ID assignment logic,
     * such as validation or audit logging. The override should call super.setId(id)
     * to maintain the base functionality unless intentionally replacing it.
     * </p>
     * @param newId the new transaction ID to set
     */
    public void setId(Long newId) {
        this.id = newId;
    }
    
    /**
     * Gets the company identifier this transaction belongs to.
     * <p>
     * Subclasses may override this method to provide company-specific logic,
     * but must ensure the returned value corresponds to a valid company in the system.
     * Any additional validation should be clearly documented.
     * </p>
     * @return the company ID associated with this transaction
     */
    public Long getCompanyId() {
        return companyId;
    }

    /**
     * Sets the company identifier for this transaction.
     * <p>
     * Subclasses may override this method to implement company validation or
     * business rule enforcement. The override should validate the company exists
     * and call super.setCompanyId(companyId) to maintain base functionality.
     * </p>
     * @param newCompanyId the company ID to associate with this transaction
     */
    public void setCompanyId(Long newCompanyId) {
        this.companyId = newCompanyId;
    }
    
    /**
     * Gets the bank account identifier this transaction belongs to.
     * <p>
     * Subclasses may override this method for account-specific processing,
     * but must ensure the returned account ID is valid for the associated company.
     * Any account validation logic should be documented in the override.
     * </p>
     * @return the bank account ID for this transaction
     */
    public Long getBankAccountId() {
        return bankAccountId;
    }

    /**
     * Sets the bank account identifier for this transaction.
     * <p>
     * Subclasses may override this method to validate account ownership,
     * check account status, or enforce business rules. The override should
     * verify the account belongs to the transaction's company before calling
     * super.setBankAccountId(bankAccountId).
     * </p>
     * @param newBankAccountId the bank account ID to set
     */
    public void setBankAccountId(Long newBankAccountId) {
        this.bankAccountId = newBankAccountId;
    }
    
    /**
     * Gets the date when this transaction occurred.
     * <p>
     * Subclasses may override this method to provide date formatting or
     * timezone conversion, but must return a valid LocalDate representing
     * the actual transaction date.
     * </p>
     * @return the transaction date
     */
    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    /**
     * Sets the date when this transaction occurred.
     * <p>
     * Subclasses may override this method to validate date ranges,
     * check fiscal period constraints, or perform date normalization.
     * The override should ensure the date is within valid business periods.
     * </p>
     * @param newTransactionDate the transaction date to set
     */
    public void setTransactionDate(LocalDate newTransactionDate) {
        this.transactionDate = newTransactionDate;
    }
    
    /**
     * Gets the transaction details or description.
     * <p>
     * Subclasses may override this method to provide formatted descriptions,
     * truncated details, or localized text, but must preserve the core
     * transaction information for classification and reporting purposes.
     * </p>
     * @return the transaction details/description
     */
    @JsonProperty("description")
    public String getDetails() {
        return description;
    }

    /**
     * Sets the transaction details or description.
     * <p>
     * Subclasses may override this method to validate description length,
     * sanitize input, or extract keywords for classification. The override
     * should ensure the details are meaningful for transaction processing.
     * </p>
     * @param newDetails the transaction details to set
     */
    public void setDetails(String newDetails) {
        this.description = newDetails;
    }
    
    /**
     * Gets the debit amount for this transaction.
     * <p>
     * Subclasses may override this method to provide formatted amounts,
     * currency conversion, or rounding logic, but must return the actual
     * debit amount for accounting calculations.
     * </p>
     * @return the debit amount, or null if not applicable
     */
    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    /**
     * Sets the debit amount for this transaction.
     * <p>
     * Subclasses may override this method to validate amount ranges,
     * enforce business rules, or perform calculations. The override should
     * ensure the amount is non-negative and properly scaled.
     * </p>
     * @param newDebitAmount the debit amount to set
     */
    public void setDebitAmount(BigDecimal newDebitAmount) {
        this.debitAmount = newDebitAmount;
    }
    
    /**
     * Gets the credit amount for this transaction.
     * <p>
     * Subclasses may override this method to provide formatted amounts,
     * currency conversion, or rounding logic, but must return the actual
     * credit amount for accounting calculations.
     * </p>
     * @return the credit amount, or null if not applicable
     */
    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    /**
     * Sets the credit amount for this transaction.
     * <p>
     * Subclasses may override this method to validate amount ranges,
     * enforce business rules, or perform calculations. The override should
     * ensure the amount is non-negative and properly scaled.
     * </p>
     * @param newCreditAmount the credit amount to set
     */
    public void setCreditAmount(BigDecimal newCreditAmount) {
        this.creditAmount = newCreditAmount;
    }
    
    /**
     * Gets the account balance after this transaction.
     * <p>
     * Subclasses may override this method to provide calculated balances,
     * formatted amounts, or balance validation, but must return the
     * mathematically correct balance for the account.
     * </p>
     * @return the account balance after this transaction
     */
    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Sets the account balance after this transaction.
     * <p>
     * Subclasses may override this method to validate balance calculations,
     * check for overdrafts, or perform reconciliation. The override should
     * ensure the balance accurately reflects the transaction's impact.
     * </p>
     * @param newBalance the account balance to set
     */
    public void setBalance(BigDecimal newBalance) {
        this.balance = newBalance;
    }
    
    /**
     * Checks if this transaction represents a service fee.
     * <p>
     * Subclasses may override this method to implement custom fee detection
     * logic based on transaction details, amounts, or classification rules.
     * The override should return true only for genuine service fee transactions.
     * </p>
     * @return true if this is a service fee transaction, false otherwise
     */
    public boolean isServiceFee() {
        return serviceFee != null ? serviceFee : false;
    }

    /**
     * Sets whether this transaction represents a service fee.
     * <p>
     * Subclasses may override this method to implement automatic fee detection
     * or validation logic. The override should analyze transaction characteristics
     * to determine if it represents a service fee.
     * </p>
     * @param newServiceFee true if this is a service fee transaction
     */
    public void setServiceFee(boolean newServiceFee) {
        this.serviceFee = newServiceFee;
    }
    
    /**
     * Gets the statement period this transaction belongs to.
     * <p>
     * Subclasses may override this method to provide formatted period strings,
     * period validation, or period calculation logic, but must return a
     * valid period identifier for the banking statement.
     * </p>
     * @return the statement period identifier
     */
    public String getStatementPeriod() {
        return null; // Not stored in current schema
    }

    /**
     * Sets the statement period for this transaction.
     * <p>
     * Subclasses may override this method to validate period formats,
     * check period ranges, or perform period normalization. The override
     * should ensure the period corresponds to valid banking cycles.
     * </p>
     * @param newStatementPeriod the statement period to set
     */
    public void setStatementPeriod(String newStatementPeriod) {
        // Not stored in current schema
    }
    
    /**
     * Gets the source file this transaction was imported from.
     * <p>
     * Subclasses may override this method to provide file path formatting,
     * security filtering, or audit trail information, but must return
     * the actual source file identifier for traceability.
     * </p>
     * @return the source file path or identifier
     */
    public String getSourceFile() {
        return sourceFile;
    }

    /**
     * Sets the source file this transaction was imported from.
     * <p>
     * Subclasses may override this method to validate file paths,
     * check file permissions, or perform security checks. The override
     * should ensure the source file is accessible and valid.
     * </p>
     * @param newSourceFile the source file path or identifier
     */
    public void setSourceFile(String newSourceFile) {
        this.sourceFile = newSourceFile;
    }
    
    /**
     * Gets the fiscal period identifier for this transaction.
     * <p>
     * Subclasses may override this method to provide period validation,
     * automatic period assignment, or period calculation logic, but must
     * return a valid fiscal period ID for financial reporting.
     * </p>
     * @return the fiscal period ID
     */
    public Long getFiscalPeriodId() {
        return fiscalPeriodId;
    }

    /**
     * Sets the fiscal period identifier for this transaction.
     * <p>
     * Subclasses may override this method to validate fiscal periods,
     * check period status, or perform automatic assignment. The override
     * should ensure the period is open and valid for the transaction date.
     * </p>
     * @param newFiscalPeriodId the fiscal period ID to set
     */
    public void setFiscalPeriodId(Long newFiscalPeriodId) {
        this.fiscalPeriodId = newFiscalPeriodId;
    }
    
    /**
     * Gets the timestamp when this transaction was created.
     * <p>
     * Subclasses may override this method to provide formatted timestamps,
     * timezone conversion, or audit information, but must return the
     * actual creation time for audit trail purposes.
     * </p>
     * @return the creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the timestamp when this transaction was created.
     * <p>
     * Subclasses may override this method to validate timestamps,
     * set creation times, or perform audit logging. The override should
     * ensure the timestamp represents when the transaction was first created.
     * </p>
     * @param newCreatedAt the creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }
    
    /**
     * Gets the transaction reference number.
     * <p>
     * Subclasses may override this method to provide formatted references,
     * generate reference numbers, or perform validation, but must return
     * a unique identifier for this transaction.
     * </p>
     * @return the transaction reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * Sets the transaction reference number.
     * <p>
     * Subclasses may override this method to validate reference formats,
     * generate unique references, or check for duplicates. The override
     * should ensure the reference is unique within the system.
     * </p>
     * @param newReference the transaction reference to set
     */
    public void setReference(String newReference) {
        this.reference = newReference;
    }
    
    /**
     * Gets the user who last updated this transaction.
     * <p>
     * Subclasses may override this method to provide user validation,
     * audit trail information, or access control, but must return
     * the actual user identifier for audit purposes.
     * </p>
     * @return the user who last updated this transaction
     */
    public String getUpdatedBy() {
        return null; // Not stored in current schema
    }

    /**
     * Sets the user who last updated this transaction.
     * <p>
     * Subclasses may override this method to validate user existence,
     * set update timestamps, or perform audit logging. The override
     * should ensure the user has appropriate permissions.
     * </p>
     * @param newUpdatedBy the user who updated this transaction
     */
    public void setUpdatedBy(String newUpdatedBy) {
        // Not stored in current schema
    }
    
    /**
     * Gets the account code this transaction is classified to.
     * <p>
     * Subclasses may override this method to provide formatted codes,
     * validation logic, or automatic classification, but must return
     * a valid chart of accounts code for financial reporting.
     * </p>
     * @return the account code for classification
     */
    public String getAccountCode() {
        return accountCode;
    }

    /**
     * Sets the account code for transaction classification.
     * <p>
     * Subclasses may override this method to validate account codes,
     * check code existence, or perform automatic classification. The override
     * should ensure the code exists in the chart of accounts.
     * </p>
     * @param newAccountCode the account code to set
     */
    public void setAccountCode(String newAccountCode) {
        this.accountCode = newAccountCode;
    }
    
    /**
     * Gets the account number this transaction belongs to.
     * <p>
     * Subclasses may override this method to provide formatted account numbers,
     * validation logic, or account number masking for security.
     * </p>
     * @return the account number
     */
    public String getAccountNumber() {
        return accountNumber;
    }

    /**
     * Sets the account number for this transaction.
     * <p>
     * Subclasses may override this method to validate account number formats,
     * check account existence, or perform security validation.
     * </p>
     * @param newAccountNumber the account number to set
     */
    public void setAccountNumber(String newAccountNumber) {
        this.accountNumber = newAccountNumber;
    }
    
    /**
     * Gets the account name this transaction is classified to.
     * <p>
     * Subclasses may override this method to provide formatted names,
     * localized names, or validation logic, but must return the
     * corresponding account name for the classification code.
     * </p>
     * @return the account name for classification
     */
    public String getAccountName() {
        return null; // Not stored in current schema
    }

    /**
     * Sets the account name for transaction classification.
     * <p>
     * Subclasses may override this method to validate account names,
     * synchronize with account codes, or perform lookup operations.
     * The override should ensure name and code consistency.
     * </p>
     * @param newAccountName the account name to set
     */
    public void setAccountName(String newAccountName) {
        // Not stored in current schema
    }
    
    /**
     * Returns a string representation of this bank transaction.
     * <p>
     * Subclasses may override this method to customize the string format,
     * add additional fields, or implement different serialization approaches.
     * The override should include all essential transaction information
     * for debugging and logging purposes, and should call super.toString()
     * to maintain consistency unless intentionally replacing the format.
     * </p>
     * @return a string representation of the transaction
     */
    public void setAccountId(Long accountId) {
        this.bankAccountId = accountId;
    }
    
    // Additional getters and setters for database fields
    
    public Long getTransactionTypeId() {
        return transactionTypeId;
    }

    public void setTransactionTypeId(Long transactionTypeId) {
        this.transactionTypeId = transactionTypeId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubcategory() {
        return subcategory;
    }

    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    public Boolean getIsReconciled() {
        return isReconciled;
    }

    public void setIsReconciled(Boolean isReconciled) {
        this.isReconciled = isReconciled;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClassificationDate() {
        return classificationDate;
    }

    public void setClassificationDate(LocalDateTime classificationDate) {
        this.classificationDate = classificationDate;
    }

    public String getClassifiedBy() {
        return classifiedBy;
    }

    public void setClassifiedBy(String classifiedBy) {
        this.classifiedBy = classifiedBy;
    }
}
