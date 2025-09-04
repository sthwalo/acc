package fin.model;

import java.time.LocalDateTime;

/**
 * Represents a category for grouping related accounts in the chart of accounts.
 * Categories help organize accounts by their function (e.g., Current Assets, Operating Expenses).
 */
public class AccountCategory {
    private Long id;
    private String name;
    private String description;
    private AccountType accountType;
    private Company company;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AccountCategory() {
        this.isActive = true;
    }

    public AccountCategory(String name, String description, AccountType accountType, Company company) {
        this.name = name;
        this.description = description;
        this.accountType = accountType;
        this.company = company;
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
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

    @Override
    public String toString() {
        return "AccountCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", accountType=" + accountType +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
