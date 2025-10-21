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
        if (company == null) {
            this.company = null;
        } else {
            this.company = new Company(company.getName());
            this.company.setId(company.getId());
            this.company.setRegistrationNumber(company.getRegistrationNumber());
            this.company.setTaxNumber(company.getTaxNumber());
            this.company.setAddress(company.getAddress());
            this.company.setContactEmail(company.getContactEmail());
            this.company.setContactPhone(company.getContactPhone());
            this.company.setLogoPath(company.getLogoPath());
            this.company.setCreatedAt(company.getCreatedAt());
        }
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
        if (company == null) {
            return null;
        }
        Company copy = new Company(company.getName());
        copy.setId(company.getId());
        copy.setRegistrationNumber(company.getRegistrationNumber());
        copy.setTaxNumber(company.getTaxNumber());
        copy.setAddress(company.getAddress());
        copy.setContactEmail(company.getContactEmail());
        copy.setContactPhone(company.getContactPhone());
        copy.setLogoPath(company.getLogoPath());
        copy.setCreatedAt(company.getCreatedAt());
        return copy;
    }

    public void setCompany(Company company) {
        if (company == null) {
            this.company = null;
        } else {
            this.company = new Company(company.getName());
            this.company.setId(company.getId());
            this.company.setRegistrationNumber(company.getRegistrationNumber());
            this.company.setTaxNumber(company.getTaxNumber());
            this.company.setAddress(company.getAddress());
            this.company.setContactEmail(company.getContactEmail());
            this.company.setContactPhone(company.getContactPhone());
            this.company.setLogoPath(company.getLogoPath());
            this.company.setCreatedAt(company.getCreatedAt());
        }
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
