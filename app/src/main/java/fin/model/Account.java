package fin.model;

import java.time.LocalDateTime;

/**
 * Represents an account in the chart of accounts.
 * Each account is part of a category and has a unique account code.
 */
public class Account {
    private Long id;
    private String accountCode;
    private String accountName;
    private AccountCategory category;
    private Account parentAccount;
    private Company company;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Account(String accountCode, String accountName, AccountCategory category, 
                  Company company, String description) {
        this();
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.category = category;
        this.company = company;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public AccountCategory getCategory() {
        return category;
    }

    public void setCategory(AccountCategory category) {
        this.category = category;
    }

    public Account getParentAccount() {
        return parentAccount;
    }

    public void setParentAccount(Account parentAccount) {
        this.parentAccount = parentAccount;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isDebitAccount() {
        return category != null && category.getAccountType() != null && 
               category.getAccountType().getNormalBalance() == 'D';
    }

    public boolean isCreditAccount() {
        return !isDebitAccount();
    }

    @Override
    public String toString() {
        return String.format("%s - %s", accountCode, accountName);
    }
}
