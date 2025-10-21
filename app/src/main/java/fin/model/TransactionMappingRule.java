package fin.model;

import java.time.LocalDateTime;

/**
 * Defines rules for automatically categorizing transactions based on their description.
 * These rules help map bank transactions to the appropriate accounts in the chart of accounts.
 */
public class TransactionMappingRule {
    public enum MatchType {
        CONTAINS,       // Description contains the match value
        STARTS_WITH,    // Description starts with the match value
        ENDS_WITH,      // Description ends with the match value
        EQUALS,         // Description exactly matches the match value
        REGEX           // Description matches the regular expression
    }

    private Long id;
    private Company company;
    private String ruleName;
    private String description;
    private MatchType matchType;
    private String matchValue;
    private Account account;
    private boolean isActive;
    private int priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TransactionMappingRule() {
        this.isActive = true;
        this.priority = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TransactionMappingRule(Company company, String ruleName, MatchType matchType, 
                                String matchValue, Account account) {
        this();
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
        this.ruleName = ruleName;
        this.matchType = matchType;
        this.matchValue = matchValue;
        if (account == null) {
            this.account = null;
        } else {
            this.account = new Account(account.getAccountCode(), account.getAccountName(),
                                     account.getCategory(), account.getCompany(),
                                     account.getDescription());
            this.account.setId(account.getId());
            this.account.setActive(account.isActive());
            this.account.setCreatedAt(account.getCreatedAt());
            this.account.setUpdatedAt(account.getUpdatedAt());
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue;
    }

    public Account getAccount() {
        if (account == null) {
            return null;
        }
        Account copy = new Account(account.getAccountCode(), account.getAccountName(),
                                 account.getCategory(), account.getCompany(),
                                 account.getDescription());
        copy.setId(account.getId());
        copy.setActive(account.isActive());
        copy.setCreatedAt(account.getCreatedAt());
        copy.setUpdatedAt(account.getUpdatedAt());
        return copy;
    }

    public void setAccount(Account account) {
        if (account == null) {
            this.account = null;
        } else {
            this.account = new Account(account.getAccountCode(), account.getAccountName(),
                                     account.getCategory(), account.getCompany(),
                                     account.getDescription());
            this.account.setId(account.getId());
            this.account.setActive(account.isActive());
            this.account.setCreatedAt(account.getCreatedAt());
            this.account.setUpdatedAt(account.getUpdatedAt());
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
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

    /**
     * Checks if the given transaction description matches this rule.
     * @param description The transaction description to check
     * @return true if the description matches the rule, false otherwise
     */
    public boolean matches(String description) {
        if (description == null || !isActive) {
            return false;
        }

        String desc = description.trim().toUpperCase();
        String matchVal = matchValue.trim().toUpperCase();

        switch (matchType) {
            case CONTAINS:
                return desc.contains(matchVal);
            case STARTS_WITH:
                return desc.startsWith(matchVal);
            case ENDS_WITH:
                return desc.endsWith(matchVal);
            case EQUALS:
                return desc.equals(matchVal);
            case REGEX:
                return desc.matches(matchVal);
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s: %s %s -> %s", 
                ruleName, matchType, matchValue, 
                account != null ? account.toString() : "[No account]");
    }
}
