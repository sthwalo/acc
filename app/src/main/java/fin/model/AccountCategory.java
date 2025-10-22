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

    public AccountCategory(String initialName, String initialDescription, AccountType initialAccountType, Company initialCompany) {
        this.name = initialName;
        this.description = initialDescription;
        this.accountType = initialAccountType;
        if (initialCompany == null) {
            this.company = null;
        } else {
            this.company = new Company(initialCompany.getName());
            this.company.setId(initialCompany.getId());
            this.company.setRegistrationNumber(initialCompany.getRegistrationNumber());
            this.company.setTaxNumber(initialCompany.getTaxNumber());
            this.company.setAddress(initialCompany.getAddress());
            this.company.setContactEmail(initialCompany.getContactEmail());
            this.company.setContactPhone(initialCompany.getContactPhone());
            this.company.setLogoPath(initialCompany.getLogoPath());
            this.company.setCreatedAt(initialCompany.getCreatedAt());
        }
        this.isActive = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long newId) {
        this.id = newId;
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String newDescription) {
        this.description = newDescription;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType newAccountType) {
        this.accountType = newAccountType;
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

    public void setCompany(Company newCompany) {
        if (newCompany == null) {
            this.company = null;
        } else {
            this.company = new Company(newCompany.getName());
            this.company.setId(newCompany.getId());
            this.company.setRegistrationNumber(newCompany.getRegistrationNumber());
            this.company.setTaxNumber(newCompany.getTaxNumber());
            this.company.setAddress(newCompany.getAddress());
            this.company.setContactEmail(newCompany.getContactEmail());
            this.company.setContactPhone(newCompany.getContactPhone());
            this.company.setLogoPath(newCompany.getLogoPath());
            this.company.setCreatedAt(newCompany.getCreatedAt());
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

    public void setCreatedAt(LocalDateTime newCreatedAt) {
        this.createdAt = newCreatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
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
