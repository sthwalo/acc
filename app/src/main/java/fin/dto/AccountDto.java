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

/**
 * Data Transfer Object for Account information.
 * Contains only the essential fields for account selection in dropdowns and UI.
 * Matches the frontend Account interface exactly.
 */
public class AccountDto {
    private Long id;
    private String code;
    private String name;
    private String category;
    private String type;
    private Boolean isActive;
    private Long companyId;
    private Long categoryId;
    private String description;
    private Long parentAccountId;
    private String accountCode;
    private String accountName;

    // Constructors
    public AccountDto() {}

    public AccountDto(Long id, String code, String name, String category, String type,
                     Boolean isActive, Long companyId, Long categoryId, String description,
                     Long parentAccountId, String accountCode, String accountName) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.category = category;
        this.type = type;
        this.isActive = isActive;
        this.companyId = companyId;
        this.categoryId = categoryId;
        this.description = description;
        this.parentAccountId = parentAccountId;
        this.accountCode = accountCode;
        this.accountName = accountName;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getParentAccountId() { return parentAccountId; }
    public void setParentAccountId(Long parentAccountId) { this.parentAccountId = parentAccountId; }

    public String getAccountCode() { return accountCode; }
    public void setAccountCode(String accountCode) { this.accountCode = accountCode; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
}