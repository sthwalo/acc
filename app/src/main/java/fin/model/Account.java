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
        if (category == null) {
            this.category = null;
        } else {
            this.category = new AccountCategory(category.getName(), category.getDescription(), 
                                              category.getAccountType(), category.getCompany());
            this.category.setId(category.getId());
            this.category.setActive(category.isActive());
            this.category.setCreatedAt(category.getCreatedAt());
            this.category.setUpdatedAt(category.getUpdatedAt());
        }
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
        if (category == null) {
            return null;
        }
        return new AccountCategory(category.getName(), category.getDescription(), 
                                 category.getAccountType(), category.getCompany());
    }

    public void setCategory(AccountCategory category) {
        if (category == null) {
            this.category = null;
        } else {
            this.category = new AccountCategory(category.getName(), category.getDescription(), 
                                              category.getAccountType(), category.getCompany());
            this.category.setId(category.getId());
            this.category.setActive(category.isActive());
            this.category.setCreatedAt(category.getCreatedAt());
            this.category.setUpdatedAt(category.getUpdatedAt());
        }
    }

    public Account getParentAccount() {
        if (parentAccount == null) {
            return null;
        }
        Account copy = new Account(parentAccount.getAccountCode(), parentAccount.getAccountName(),
                                 parentAccount.getCategory(), parentAccount.getCompany(),
                                 parentAccount.getDescription());
        copy.setId(parentAccount.getId());
        copy.setActive(parentAccount.isActive());
        copy.setCreatedAt(parentAccount.getCreatedAt());
        copy.setUpdatedAt(parentAccount.getUpdatedAt());
        return copy;
    }

    public void setParentAccount(Account parentAccount) {
        if (parentAccount == null) {
            this.parentAccount = null;
        } else {
            this.parentAccount = new Account(parentAccount.getAccountCode(), parentAccount.getAccountName(),
                                           parentAccount.getCategory(), parentAccount.getCompany(),
                                           parentAccount.getDescription());
            this.parentAccount.setId(parentAccount.getId());
            this.parentAccount.setActive(parentAccount.isActive());
            this.parentAccount.setCreatedAt(parentAccount.getCreatedAt());
            this.parentAccount.setUpdatedAt(parentAccount.getUpdatedAt());
        }
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
