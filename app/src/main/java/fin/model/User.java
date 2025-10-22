/*
 * FIN Financial Management System - User Model
 *
 * Copyright (c) 2024-2025 Sthwalo Holdings (Pty) Ltd.
 */

package fin.model;

import java.time.LocalDateTime;

/**
 * User entity for authentication and authorization
 */
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String salt;
    private String firstName;
    private String lastName;
    private String role; // ADMIN, USER
    private Long companyId;
    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime lastLoginAt;

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String initialEmail, String initialFirstName, String initialLastName, String initialRole, Long initialCompanyId) {
        this();
        this.email = initialEmail;
        this.firstName = initialFirstName;
        this.lastName = initialLastName;
        this.role = initialRole;
        this.companyId = initialCompanyId;
    }

    /**
     * Copy constructor for defensive copying to prevent EI_EXPOSE_REP vulnerabilities
     */
    public User(User other) {
        if (other != null) {
            this.id = other.id;
            this.email = other.email;
            this.passwordHash = other.passwordHash;
            this.salt = other.salt;
            this.firstName = other.firstName;
            this.lastName = other.lastName;
            this.role = other.role;
            this.companyId = other.companyId;
            this.isActive = other.isActive;
            this.createdAt = other.createdAt;
            this.updatedAt = other.updatedAt;
            this.createdBy = other.createdBy;
            this.updatedBy = other.updatedBy;
            this.lastLoginAt = other.lastLoginAt;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long newId) { this.id = newId; }

    public String getEmail() { return email; }
    public void setEmail(String newEmail) { this.email = newEmail; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String newPasswordHash) { this.passwordHash = newPasswordHash; }

    public String getSalt() { return salt; }
    public void setSalt(String newSalt) { this.salt = newSalt; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String newFirstName) { this.firstName = newFirstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String newLastName) { this.lastName = newLastName; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public String getRole() { return role; }
    public void setRole(String newRole) { this.role = newRole; }

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long newCompanyId) { this.companyId = newCompanyId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime newCreatedAt) { this.createdAt = newCreatedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime newUpdatedAt) { this.updatedAt = newUpdatedAt; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String newCreatedBy) { this.createdBy = newCreatedBy; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String newUpdatedBy) { this.updatedBy = newUpdatedBy; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime newLastLoginAt) { this.lastLoginAt = newLastLoginAt; }

    // Utility methods
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    public boolean isUser() {
        return "USER".equalsIgnoreCase(role);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role='" + role + '\'' +
                ", companyId=" + companyId +
                ", isActive=" + isActive +
                '}';
    }
}