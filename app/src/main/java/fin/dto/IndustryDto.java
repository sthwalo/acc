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
 * Data Transfer Object for Industry information.
 * Contains only the essential fields for industry selection in company registration/updates.
 */
public class IndustryDto {
    private Long id;
    private String divisionCode;
    private String name;
    private String description;
    private String category;
    private Boolean isActive;
    private Boolean isSarsCompliant;

    // Constructors
    public IndustryDto() {}

    public IndustryDto(Long id, String divisionCode, String name, String description,
                      String category, Boolean isActive, Boolean isSarsCompliant) {
        this.id = id;
        this.divisionCode = divisionCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.isActive = isActive;
        this.isSarsCompliant = isSarsCompliant;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDivisionCode() { return divisionCode; }
    public void setDivisionCode(String divisionCode) { this.divisionCode = divisionCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsSarsCompliant() { return isSarsCompliant; }
    public void setIsSarsCompliant(Boolean isSarsCompliant) { this.isSarsCompliant = isSarsCompliant; }
}