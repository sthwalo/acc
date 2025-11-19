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
    private Long planId; // Reference to pricing plan
    private boolean isActive = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime lastLoginAt;

    // JWT token for API authentication (not persisted)
    private String token;

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public User(String initialEmail, String initialFirstName, String initialLastName, String initialRole, Long initialPlanId) {
        this();
        this.email = initialEmail;
        this.firstName = initialFirstName;
        this.lastName = initialLastName;
        this.role = initialRole;
        this.planId = initialPlanId;
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
            this.planId = other.planId;
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

    public Long getPlanId() { return planId; }
    public void setPlanId(Long newPlanId) { this.planId = newPlanId; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Alias for Spring Security compatibility
    public boolean getActive() { return isActive(); }

    // Username for Spring Security (email is used as username)
    public String getUsername() { return email; }

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

    public String getToken() { return token; }
    public void setToken(String newToken) { this.token = newToken; }

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
                ", planId=" + planId +
                ", isActive=" + isActive +
                "}";
    }
}